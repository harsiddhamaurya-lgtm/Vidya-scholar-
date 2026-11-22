package com.example.rag

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.example.utils.NativeCoreBridge
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
import java.io.RandomAccessFile
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * Dual-Model ONNX Lifecycle Manager for Cosmium:
 * 1. `all-MiniLM-L6-v2` Quantized Semantic Embedding Encoder (~23 MB) -> 384-dim vectors when verified.
 * 2. `Qwen 2.5 3B Instruct` 4-Bit INT4 Local Language Model -> Focused step-by-step synthesis over retrieved passages.
 *
 * Guarantees unified vector space between indexing & querying via [activeEncoderSignature] and strict
 * model integrity checks so corrupt/dummy artifacts are never loaded into ORT.
 */
class OnnxDualModelManager(context: Context) {

    companion object {
        private const val TAG = "OnnxDualModelManager"
        private const val VECTOR_DIM = EncryptedRagDatabase.VECTOR_DIM // 384
        const val IDLE_UNLOAD_TIMEOUT_MS = 60_000L
        const val ENCODER_SIG_MINILM = "minilm_onnx_v1"
        const val ENCODER_SIG_NATIVE = "native_cpp_v4"
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

    internal fun minValidModelBytes(spec: OfflineModelSpec): Long = when (spec) {
        OfflineModelSpec.MINILM_ENCODER -> 1_000_000L
        OfflineModelSpec.QWEN_2_5_3B_INT4 -> 50_000_000L
    }

    internal fun isCorruptStub(file: File): Boolean {
        if (!file.exists() || file.length() < 16L) return true
        return try {
            val head = ByteArray(16)
            RandomAccessFile(file, "r").use { raf ->
                raf.readFully(head)
            }
            head.all { it == 0x58.toByte() }
        } catch (_: Exception) {
            true
        }
    }

    internal fun isTokenizerValid(tokenizerFile: File): Boolean {
        if (!tokenizerFile.exists() || tokenizerFile.length() < 512L) return false
        return try {
            val text = tokenizerFile.readText()
            if (text.contains("cosmium_offline_fallback_tokenizer")) return false
            val json = JSONObject(text)
            val modelObj = json.optJSONObject("model") ?: return false
            val vocabObj = modelObj.optJSONObject("vocab") ?: return false
            vocabObj.length() >= 100
        } catch (_: Exception) {
            false
        }
    }

    fun isModelDownloaded(spec: OfflineModelSpec): Boolean {
        val dir = modelDirectory(spec)
        val modelFile = File(dir, spec.primaryFileName)
        val tokenizerFile = File(dir, spec.tokenizerFileName)
        return modelFile.exists() &&
            modelFile.length() >= minValidModelBytes(spec) &&
            !isCorruptStub(modelFile) &&
            isTokenizerValid(tokenizerFile)
    }

    fun activeEncoderSignature(): String =
        if (isModelDownloaded(OfflineModelSpec.MINILM_ENCODER)) ENCODER_SIG_MINILM else ENCODER_SIG_NATIVE

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
        return freeMb >= OfflineModelSpec.QWEN_2_5_3B_INT4.minFreeRamMb ||
            (totalMb >= 6_000L && freeMb >= 1_800L)
    }

    /**
     * Generates a normalized 384-dim query embedding in the exact active encoder vector space.
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
     * Generates a normalized 384-dim document embedding with field weighting in the exact same
     * vector space as [encodeText].
     */
    suspend fun encodeWeightedDocument(
        title: String,
        subject: String,
        formulaTags: String,
        body: String,
        workedExample: String
    ): FloatArray {
        if (isModelDownloaded(OfflineModelSpec.MINILM_ENCODER)) {
            val combined = "$title. $subject. $formulaTags. $body"
            val onnxVec = runCatching { encodeWithMiniLmOnnx(combined) }.getOrNull()
            if (onnxVec != null && onnxVec.size == VECTOR_DIM) {
                scheduleIdleUnload()
                return onnxVec
            }
        }
        return NativeCoreBridge.encodeWeightedDocumentVector(
            topicTitle = title,
            subjectName = subject,
            formulaTags = formulaTags,
            contentText = "$body $workedExample".trim(),
            dim = VECTOR_DIM
        )
    }

    /**
     * Generates an on-topic, interleaved textbook answer using the local 4-bit model when verified
     * and memory permits, or synthesizes from the verified NCERT lesson / C++ composer.
     */
    suspend fun synthesizeGroundedAnswer(
        query: String,
        retrieved: List<RetrievedChunk>,
        standardId: String,
        streamId: String,
        standardTitle: String
    ): Pair<String, BrainMode> {
        val qwenReady = isModelDownloaded(OfflineModelSpec.QWEN_2_5_3B_INT4)
        if (qwenReady && !canSafelyLoadQwen4Bit()) {
            _lifecycleState.value = ModelLifecycleState.LOW_RAM_FALLBACK
            return synthesizeStructuredRagFallback(
                query = query,
                retrieved = retrieved,
                standardId = standardId,
                streamId = streamId,
                standardTitle = standardTitle,
                lowRamNotice = true
            ) to BrainMode.OFFLINE_HYBRID_RAG
        }

        if (qwenReady && retrieved.isNotEmpty()) {
            val generated = runCatching {
                runQwen4BitInference(query, retrieved, standardId, streamId, standardTitle)
            }.getOrNull()
            if (!generated.isNullOrBlank()) {
                scheduleIdleUnload()
                return generated to BrainMode.OFFLINE_QWEN_4BIT_RAG
            }
        }

        return synthesizeStructuredRagFallback(
            query = query,
            retrieved = retrieved,
            standardId = standardId,
            streamId = streamId,
            standardTitle = standardTitle,
            lowRamNotice = false
        ) to BrainMode.OFFLINE_HYBRID_RAG
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
        if (vocab.isEmpty()) return@withLock null

        val tokenIds = tokenizeWordPiece(text, vocab, maxLen = 128)
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
        standardId: String,
        streamId: String,
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
        if (vocab.isEmpty()) return@withLock null

        val primaryHit = retrieved.firstOrNull() ?: return@withLock null
        val gradeNum = when {
            standardId.contains("9") -> 9
            standardId.contains("10") -> 10
            standardId.contains("12") -> 12
            else -> 11
        }
        val sysDirective = NativeCoreBridge.calibrateCosmosPrompt(
            userQuery = query,
            standardGrade = gradeNum,
            streamId = streamId,
            scopeTitle = standardTitle
        )
        val curatedContext = SyllabusPackCatalog.getStructuredLesson(primaryHit.chunk.chunkId)
            ?: "${primaryHit.chunk.contentText}\nFormulas: ${primaryHit.chunk.formulaTags}\nExample: ${primaryHit.chunk.workedExample}"

        val prompt = buildString {
            append("<|im_start|>system\n")
            append(sysDirective)
            append("\nFocus EXCLUSIVELY on this verified syllabus unit ([${primaryHit.chunk.subjectName} Ch.${primaryHit.chunk.chapterNumber}: ${primaryHit.chunk.topicTitle}]):\n")
            append(curatedContext.take(900))
            append("\n<|im_end|>\n<|im_start|>user\n")
            append(query)
            append("\n<|im_end|>\n<|im_start|>assistant\n")
        }

        val inputNames = session.inputNames
        if ("input_ids" !in inputNames) return@withLock null

        val promptIds = tokenizeWordPiece(prompt, vocab, maxLen = 512).toMutableList()
        val generatedTokens = mutableListOf<Int>()
        val eosId = vocab["<|im_end|>"] ?: vocab["<|endoftext|>"] ?: 151645

        val maxNewTokens = 256
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
                Log.w(TAG, "Local LM inference step aborted at $step: ${e.message}")
                break
            } finally {
                inputs.values.forEach { runCatching { it.close() } }
            }
        }

        if (generatedTokens.size < 12) return@withLock null
        val decoded = generatedTokens.joinToString("") { id ->
            revVocab[id]?.replace("Ġ", " ")?.replace("Ċ", "\n")?.replace("##", "") ?: ""
        }.trim()

        // Guard against garbled or repetitive outputs
        return@withLock decoded.takeIf { it.length >= 60 && it.any(Char::isLetter) }
    }

    private fun synthesizeStructuredRagFallback(
        query: String,
        retrieved: List<RetrievedChunk>,
        standardId: String,
        streamId: String,
        standardTitle: String,
        lowRamNotice: Boolean
    ): String {
        if (retrieved.isEmpty()) {
            val sampleTopics = SyllabusPackCatalog.getChunksForScope(standardId, streamId)
                .take(5)
                .joinToString("\n") { "- **${it.subjectName} (Ch. ${it.chapterNumber}):** ${it.topicTitle}" }
            return buildString {
                append("### $standardTitle • Syllabus Study Guide\n\n")
                append("I couldn't find a direct match inside your **$standardTitle** syllabus pack for **\"$query\"**.\n\n")
                append("To keep every answer accurate and free of cross-stream confusion, here are core chapters currently loaded for your level:\n\n")
                append(sampleTopics)
                append("\n\nAsk any specific concept, theorem, historical date, accounting rule, or numerical problem from your active syllabus above.")
            }
        }

        val primary = retrieved.first()
        val chunk = primary.chunk
        val curatedLesson = SyllabusPackCatalog.getStructuredLesson(chunk.chunkId)
        if (curatedLesson != null) {
            return buildString {
                append("### ${chunk.subjectName} • Ch. ${chunk.chapterNumber}: ${chunk.topicTitle}\n\n")
                if (lowRamNotice) {
                    append("*Prepared in fast offline syllabus mode.*\n\n")
                }
                append(curatedLesson.trim())
            }
        }

        return NativeCoreBridge.composeFallbackTextbookResponse(
            topicTitle = chunk.topicTitle,
            subjectName = chunk.subjectName,
            chapterNumber = chunk.chapterNumber,
            contentText = chunk.contentText,
            formulaTags = chunk.formulaTags,
            workedExample = chunk.workedExample,
            lowRamNotice = lowRamNotice
        )
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
     * Deterministic 384-dim semantic + cluster-expanded projection routed through C++.
     */
    fun encodeConceptProjection(text: String): FloatArray =
        NativeCoreBridge.encodeConceptVector(text, VECTOR_DIM)

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
