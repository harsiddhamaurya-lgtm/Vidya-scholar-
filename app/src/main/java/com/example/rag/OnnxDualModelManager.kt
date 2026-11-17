package com.example.rag

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * Dual-Model ONNX Lifecycle Manager for Cosmium:
 * 1. `all-MiniLM-L6-v2` Quantized Semantic Embedding Encoder (~23 MB) -> 384-dim query/chapter vectors.
 * 2. `Qwen 2.5 3B Instruct` 4-Bit INT4 Medium Language Model (MLM, ~1.9 GB) -> Step-by-step reasoning over retrieved passages.
 *
 * Enforces:
 * - Zero startup model allocation (strict lazy loading).
 * - Hardware free-RAM guardrail via `ActivityManager.MemoryInfo`.
 * - 60-second idle auto-unload watchdog closing ONNX sessions to reclaim RAM cleanly alongside native TTS/GL.
 */
class OnnxDualModelManager(context: Context) {

    companion object {
        private const val TAG = "OnnxDualModelManager"
        private const val VECTOR_DIM = EncryptedRagDatabase.VECTOR_DIM // 384
        const val IDLE_UNLOAD_TIMEOUT_MS = 60_000L
    }

    private val appContext = context.applicationContext
    private val modelsRootDir = File(appContext.filesDir, "cosmium_models")
    private val sessionMutex = Mutex()
    private val watchdogScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var idleWatchdogJob: Job? = null

    private var ortEnv: OrtEnvironment? = null
    private var miniLmSession: OrtSession? = null
    private var miniLmVocab: Map<String, Int>? = null

    private var qwenSession: OrtSession? = null
    private var qwenVocab: Map<String, Int>? = null
    private var qwenReverseVocab: Map<Int, String>? = null

    private val _lifecycleState = MutableStateFlow(ModelLifecycleState.UNLOADED)
    val lifecycleState: StateFlow<ModelLifecycleState> = _lifecycleState.asStateFlow()

    fun modelDirectory(spec: OfflineModelSpec): File {
        val dir = File(modelsRootDir, spec.folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isModelDownloaded(spec: OfflineModelSpec): Boolean {
        val dir = modelDirectory(spec)
        val modelFile = File(dir, spec.primaryFileName)
        val tokenizerFile = File(dir, spec.tokenizerFileName)
        return modelFile.exists() && modelFile.length() > 1024L && tokenizerFile.exists() && tokenizerFile.length() > 128L
    }

    fun getMemoryStatsMb(): Pair<Long, Long> {
        return try {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo()
            if (am != null) {
                am.getMemoryInfo(info)
                val freeMb = info.availMem / (1024L * 1024L)
                val totalMb = info.totalMem / (1024L * 1024L)
                freeMb to totalMb
            } else {
                2048L to 4096L
            }
        } catch (_: Exception) {
            2048L to 4096L
        }
    }

    fun canSafelyLoadQwen4Bit(): Boolean {
        val (freeMb, totalMb) = getMemoryStatsMb()
        return freeMb >= OfflineModelSpec.QWEN_2_5_3B_INT4.minFreeRamMb || totalMb >= 6_000L && freeMb >= 1_800L
    }

    /**
     * Generates a normalized 384-dim embedding for either a textbook passage or a student query.
     * Uses downloaded `all-MiniLM-L6-v2` ONNX session when available, falling back seamlessly
     * to a deterministic 384-dim concept-hashed semantic projection when weights are absent.
     */
    suspend fun encodeText(text: String): FloatArray {
        val clean = text.trim()
        if (clean.isEmpty()) return FloatArray(VECTOR_DIM)

        if (isModelDownloaded(OfflineModelSpec.MINILM_ENCODER)) {
            val onnxVec = runCatching { encodeWithMiniLmOnnx(clean) }.getOrNull()
            if (onnxVec != null && onnxVec.size == VECTOR_DIM) {
                scheduleIdleUnload()
                return onnxVec
            }
        }
        return encodeConceptProjection(clean)
    }

    /**
     * Generates a step-by-step grounded answer using Qwen 2.5 3B INT4 ONNX when downloaded
     * and memory permits; otherwise synthesizes a crisp, structured explanation directly from
     * the top retrieved curriculum passages.
     */
    suspend fun synthesizeGroundedAnswer(
        query: String,
        retrieved: List<RetrievedChunk>,
        standardTitle: String
    ): Pair<String, BrainMode> {
        val qwenReady = isModelDownloaded(OfflineModelSpec.QWEN_2_5_3B_INT4)
        if (qwenReady && !canSafelyLoadQwen4Bit()) {
            _lifecycleState.value = ModelLifecycleState.LOW_RAM_FALLBACK
            return synthesizeStructuredRagFallback(query, retrieved, standardTitle, lowRamNotice = true) to BrainMode.OFFLINE_HYBRID_RAG
        }

        if (qwenReady) {
            val generated = runCatching { runQwen4BitInference(query, retrieved, standardTitle) }.getOrNull()
            if (!generated.isNullOrBlank()) {
                scheduleIdleUnload()
                return generated to BrainMode.OFFLINE_QWEN_4BIT_RAG
            }
        }

        return synthesizeStructuredRagFallback(query, retrieved, standardTitle, lowRamNotice = false) to BrainMode.OFFLINE_HYBRID_RAG
    }

    private suspend fun encodeWithMiniLmOnnx(text: String): FloatArray? = sessionMutex.withLock {
        val env = ortEnv ?: OrtEnvironment.getEnvironment().also { ortEnv = it }
        val dir = modelDirectory(OfflineModelSpec.MINILM_ENCODER)
        val session = miniLmSession ?: run {
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            env.createSession(File(dir, OfflineModelSpec.MINILM_ENCODER.primaryFileName).absolutePath, opts)
                .also { miniLmSession = it }
        }
        val vocab = miniLmVocab ?: loadTokenizerVocab(File(dir, OfflineModelSpec.MINILM_ENCODER.tokenizerFileName))
            .also { miniLmVocab = it }

        val tokenIds = tokenizeWordPiece(text, vocab, maxLen = 96)
        val seqLen = tokenIds.size.toLong()
        val attentionMask = LongArray(tokenIds.size) { 1L }
        val tokenTypeIds = LongArray(tokenIds.size) { 0L }

        val inputNames = session.inputNames
        val inputs = linkedMapOf<String, OnnxTensor>()
        try {
            if ("input_ids" in inputNames) {
                inputs["input_ids"] = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenIds), longArrayOf(1, seqLen))
            }
            if ("attention_mask" in inputNames) {
                inputs["attention_mask"] = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), longArrayOf(1, seqLen))
            }
            if ("token_type_ids" in inputNames) {
                inputs["token_type_ids"] = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), longArrayOf(1, seqLen))
            }

            session.run(inputs).use { result ->
                val rawOut = result.get(0)?.value
                val pooled = extractMeanPooledVector(rawOut)
                return if (pooled != null && pooled.size == VECTOR_DIM) normalizeVector(pooled) else null
            }
        } finally {
            inputs.values.forEach { runCatching { it.close() } }
        }
    }

    private suspend fun runQwen4BitInference(
        query: String,
        retrieved: List<RetrievedChunk>,
        standardTitle: String
    ): String? = sessionMutex.withLock {
        _lifecycleState.value = ModelLifecycleState.LOADING
        val env = ortEnv ?: OrtEnvironment.getEnvironment().also { ortEnv = it }
        val dir = modelDirectory(OfflineModelSpec.QWEN_2_5_3B_INT4)
        val session = qwenSession ?: run {
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setMemoryPatternOptimization(true)
            }
            env.createSession(File(dir, OfflineModelSpec.QWEN_2_5_3B_INT4.primaryFileName).absolutePath, opts)
                .also { qwenSession = it }
        }
        if (qwenVocab == null) {
            val loaded = loadTokenizerVocab(File(dir, OfflineModelSpec.QWEN_2_5_3B_INT4.tokenizerFileName))
            qwenVocab = loaded
            qwenReverseVocab = loaded.entries.associate { (k, v) -> v to k }
        }
        _lifecycleState.value = ModelLifecycleState.READY

        val vocab = qwenVocab ?: return@withLock null
        val revVocab = qwenReverseVocab ?: return@withLock null
        val contextSummary = retrieved.take(3).joinToString("\n") { hit ->
            "[${hit.chunk.subjectName} Ch.${hit.chunk.chapterNumber}: ${hit.chunk.topicTitle}] " +
                "${hit.chunk.contentText} | Formulas: ${hit.chunk.formulaTags} | Example: ${hit.chunk.workedExample}"
        }
        val prompt = buildString {
            append("<|im_start|>system\n")
            append("You are Cosmium, an encouraging study tutor for $standardTitle. ")
            append("Answer strictly using these verified syllabus passages:\n")
            append(contextSummary)
            append("\n<|im_end|>\n<|im_start|>user\n")
            append(query)
            append("\n<|im_end|>\n<|im_start|>assistant\n")
        }

        // Autoregressive greedy decoding loop (supports standard causal LM ONNX exports expecting input_ids & attention_mask)
        val inputNames = session.inputNames
        if ("input_ids" !in inputNames) return@withLock null

        val promptIds = tokenizeWordPiece(prompt, vocab, maxLen = 384).toMutableList()
        val generatedTokens = mutableListOf<Int>()
        val eosId = vocab["<|im_end|>"] ?: vocab["<|endoftext|>"] ?: 151645

        val maxNewTokens = 96
        for (step in 0 until maxNewTokens) {
            val seqArr = promptIds.toLongArray()
            val maskArr = LongArray(seqArr.size) { 1L }
            val inputs = linkedMapOf<String, OnnxTensor>()
            try {
                inputs["input_ids"] = OnnxTensor.createTensor(env, LongBuffer.wrap(seqArr), longArrayOf(1, seqArr.size.toLong()))
                if ("attention_mask" in inputNames) {
                    inputs["attention_mask"] = OnnxTensor.createTensor(env, LongBuffer.wrap(maskArr), longArrayOf(1, maskArr.size.toLong()))
                }
                val nextId = session.run(inputs).use { res ->
                    argmaxLastLogit(res.get(0)?.value)
                } ?: break
                if (nextId == eosId) break
                generatedTokens += nextId
                promptIds += nextId.toLong()
            } catch (e: Exception) {
                Log.w(TAG, "Qwen ONNX step aborted at step $step: ${e.message}")
                break
            } finally {
                inputs.values.forEach { runCatching { it.close() } }
            }
        }

        if (generatedTokens.isEmpty()) return@withLock null
        val decoded = generatedTokens.joinToString("") { id ->
            revVocab[id]?.replace("Ġ", " ")?.replace("Ċ", "\n")?.replace("##", "") ?: ""
        }.trim()

        return@withLock decoded.takeIf { it.length >= 24 }
    }

    private fun synthesizeStructuredRagFallback(
        query: String,
        retrieved: List<RetrievedChunk>,
        standardTitle: String,
        lowRamNotice: Boolean
    ): String {
        if (retrieved.isEmpty()) {
            return buildString {
                append("Cosmium Study Guide ($standardTitle)\n\n")
                append("I couldn't find an exact chapter match in your study guide for \"$query\".\n\n")
                append("Try including key chapter terms or formulas such as Newton, Derivative, Kirchhoff, Mole, Genetics, or Accounting Equation.")
            }
        }

        val primary = retrieved.first()
        val chunk = primary.chunk

        return buildString {
            append("[${chunk.subjectName} • Ch. ${chunk.chapterNumber}: ${chunk.topicTitle}]\n\n")
            append("1. Core Concept:\n")
            append(chunk.contentText)
            append("\n\n")
            if (chunk.formulaTags.isNotBlank()) {
                append("2. Key Formulas & Rules:\n\n")
                val tags = chunk.formulaTags.split(", ").filter { it.isNotBlank() }
                for (tag in tags) {
                    append("$$ $tag $$\n")
                }
                append("\n")
            }
            if (chunk.workedExample.isNotBlank()) {
                append("3. Worked Example:\n")
                append(chunk.workedExample)
                append("\n\n")
            }
            if (retrieved.size > 1) {
                val related = retrieved.drop(1).joinToString(" • ") {
                    "${it.chunk.subjectName}: ${it.chunk.topicTitle}"
                }
                append("Related Textbook Sections: $related")
            }
        }.trim()
    }

    fun scheduleIdleUnload() {
        idleWatchdogJob?.cancel()
        idleWatchdogJob = watchdogScope.launch {
            delay(IDLE_UNLOAD_TIMEOUT_MS)
            unloadAllModelsFromRam()
        }
    }

    suspend fun unloadAllModelsFromRam() {
        idleWatchdogJob?.cancel()
        sessionMutex.withLock {
            runCatching { miniLmSession?.close() }
            runCatching { qwenSession?.close() }
            miniLmSession = null
            qwenSession = null
            _lifecycleState.value = ModelLifecycleState.UNLOADED
        }
    }

    /**
     * Deterministic 384-dim semantic + synonym-expanded feature-hashing projection routed through C++.
     */
    fun encodeConceptProjection(text: String): FloatArray =
        com.example.utils.NativeCoreBridge.encodeConceptVector(text, VECTOR_DIM)

    private fun normalizeVector(vec: FloatArray): FloatArray {
        var sumSq = 0.0
        for (v in vec) sumSq += v * v
        val norm = sqrt(sumSq).toFloat()
        if (norm <= 1e-6f) return vec
        return FloatArray(vec.size) { idx -> vec[idx] / norm }
    }

    private fun loadTokenizerVocab(tokenizerJsonFile: File): Map<String, Int> {
        val json = JSONObject(tokenizerJsonFile.readText())
        val modelObj = json.optJSONObject("model") ?: return emptyMap()
        val vocabObj = modelObj.optJSONObject("vocab") ?: return emptyMap()
        val map = HashMap<String, Int>(vocabObj.length())
        val keys = vocabObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = vocabObj.optInt(k, 0)
        }
        return map
    }

    private fun tokenizeWordPiece(text: String, vocab: Map<String, Int>, maxLen: Int): LongArray {
        val unkId = (vocab["[UNK]"] ?: vocab["<unk>"] ?: 100).toLong()
        val clsId = vocab["[CLS]"]?.toLong()
        val sepId = vocab["[SEP]"]?.toLong()
        val out = ArrayList<Long>(maxLen)
        if (clsId != null) out += clsId

        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        for (word in words) {
            if (out.size >= maxLen - 1) break
            val direct = vocab[word] ?: vocab["Ġ$word"]
            if (direct != null) {
                out += direct.toLong()
            } else {
                var start = 0
                while (start < word.length && out.size < maxLen - 1) {
                    var end = word.length
                    var foundId: Int? = null
                    while (start < end) {
                        val sub = word.substring(start, end)
                        val candidate = if (start == 0) sub else "##$sub"
                        val id = vocab[candidate]
                        if (id != null) {
                            foundId = id
                            break
                        }
                        end--
                    }
                    if (foundId != null) {
                        out += foundId.toLong()
                        start = end
                    } else {
                        out += unkId
                        break
                    }
                }
            }
        }
        if (sepId != null && out.size < maxLen) out += sepId
        return out.toLongArray()
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractMeanPooledVector(rawValue: Any?): FloatArray? {
        val batch = rawValue as? Array<*> ?: return null
        val seq = batch.firstOrNull() as? Array<*> ?: return null
        if (seq.isEmpty()) return null
        val out = FloatArray(VECTOR_DIM)
        var count = 0
        for (tokObj in seq) {
            val vec = tokObj as? FloatArray ?: continue
            if (vec.size == VECTOR_DIM) {
                for (i in 0 until VECTOR_DIM) out[i] += vec[i]
                count++
            }
        }
        if (count == 0) return null
        for (i in 0 until VECTOR_DIM) out[i] /= count.toFloat()
        return out
    }

    private fun argmaxLastLogit(rawValue: Any?): Int? {
        val batch = rawValue as? Array<*> ?: return null
        val seq = batch.firstOrNull() as? Array<*> ?: return null
        val lastLogits = seq.lastOrNull() as? FloatArray ?: return null
        if (lastLogits.isEmpty()) return null
        var bestIdx = 0
        var bestVal = Float.NEGATIVE_INFINITY
        for (i in lastLogits.indices) {
            if (lastLogits[i] > bestVal) {
                bestVal = lastLogits[i]
                bestIdx = i
            }
        }
        return bestIdx
    }
}
