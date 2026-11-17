package com.example.utils

import com.example.rag.MemoryMappedKeywordIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Unified JNI bridge to `libcosmium_native.so` (C++ core engines for math formatting,
 * RAG dense vector operations, voice quiz evaluation, board-game AI, and acoustic PCM DSP).
 *
 * All production Android runs execute natively in C++ for smooth frame times and zero-GC heap usage.
 * Host-JVM fallbacks are encapsulated exclusively here so Robolectric unit tests execute identically
 * without native `.so` files.
 */
object NativeCoreBridge {
    private val loaded: Boolean by lazy {
        runCatching {
            System.loadLibrary("cosmium_native")
            true
        }.getOrDefault(false)
    }

    fun isAvailable(): Boolean = loaded

    // --- Raw JNI Bindings ---

    external fun nativeComputeChessBotMove(
        boardArray: IntArray,
        turnSide: Int,
        difficulty: Int
    ): Int

    external fun nativeChooseLudoBestToken(
        tokenPositionsArr: IntArray,
        tokenStartTrackArr: IntArray,
        enemyPositionsArr: IntArray,
        dice: Int,
        safeZonesArr: IntArray
    ): Int

    external fun nativeChooseChaturangaBestMove(
        capturedPointsArr: IntArray,
        capturedIsRajaArr: BooleanArray,
        toRowsArr: IntArray,
        toColsArr: IntArray
    ): Int

    external fun nativeSynthesizeWoodyTock(
        sampleRate: Int,
        durationSec: Double,
        baseFreq: Double,
        sweep: Double,
        volume: Double
    ): ShortArray

    external fun nativeSynthesizeTempleChime(
        sampleRate: Int,
        durationSec: Double,
        fundamental: Double
    ): ShortArray

    external fun nativeSynthesizeVictorySwell(
        sampleRate: Int,
        durationSec: Double
    ): ShortArray

    external fun nativeSynthesizeMusicNote(
        outBuffer: ShortArray,
        sampleRate: Int,
        melodyFreq: Double,
        harmonicFreq: Double,
        droneFreq: Double,
        isJoyful: Boolean,
        globalSampleStart: Long
    )

    external fun nativeSanitizeAndFormatMath(
        rawText: String,
        standardGrade: Int,
        streamId: String
    ): String

    external fun nativeCalibrateCosmosPrompt(
        userQuery: String,
        standardGrade: Int,
        streamId: String
    ): String

    external fun nativeCosineSimilarity(
        aArray: FloatArray,
        bArray: FloatArray
    ): Float

    external fun nativeEncodeConceptVector(
        text: String,
        dim: Int
    ): FloatArray

    external fun nativeEvaluateQuizAnswer(
        studentAnswer: String,
        canonicalAnswer: String,
        keywordsPipe: String
    ): Boolean

    // --- Unified Engine Facade ---

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (isAvailable()) {
            return nativeCosineSimilarity(a, b)
        }
        val n = minOf(a.size, b.size)
        if (n == 0) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in 0 until n) {
            val va = a[i].toDouble()
            val vb = b[i].toDouble()
            dot += va * vb
            normA += va * va
            normB += vb * vb
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom <= 1e-6) 0f else (dot / denom).toFloat().coerceIn(0f, 1f)
    }

    fun encodeConceptVector(text: String, dim: Int = 384): FloatArray {
        val clean = text.trim()
        if (clean.isEmpty()) return FloatArray(dim)
        if (isAvailable()) {
            return nativeEncodeConceptVector(clean, dim)
        }
        return jvmEncodeConceptFallback(clean, dim)
    }

    fun evaluateQuizAnswer(
        studentAnswer: String,
        canonicalAnswer: String,
        expectedKeywords: List<String>
    ): Boolean {
        val clean = studentAnswer.trim()
        if (clean.isEmpty()) return false
        val joinedKeywords = expectedKeywords.joinToString("|")
        if (isAvailable()) {
            return nativeEvaluateQuizAnswer(clean, canonicalAnswer, joinedKeywords)
        }
        val studentNorm = normalizeSpeechAndMathJvm(clean)
        val studentCompact = studentNorm.replace(" ", "").replace("(", "").replace(")", "")
        for (kw in expectedKeywords) {
            val kwNorm = normalizeSpeechAndMathJvm(kw)
            if (kwNorm.isNotEmpty() && studentNorm.contains(kwNorm)) return true
            val kwCompact = kwNorm.replace(" ", "").replace("(", "").replace(")", "")
            if (kwCompact.length >= 2 && studentCompact.contains(kwCompact)) return true
        }
        val canonCompact = normalizeSpeechAndMathJvm(canonicalAnswer)
            .replace(" ", "").replace("(", "").replace(")", "")
        if (canonCompact.isNotEmpty() && studentCompact.contains(canonCompact)) return true
        return expectedKeywords.isEmpty() && studentNorm.length >= 4
    }

    fun computeChessBotMove(boardArray: IntArray, turnSide: Int, difficulty: Int): Int? {
        if (!isAvailable()) return null
        val encoded = nativeComputeChessBotMove(boardArray, turnSide, difficulty)
        return encoded.takeIf { it != -1 }
    }

    fun chooseLudoBestToken(
        tokenPositions: IntArray,
        tokenStartTrackIndices: IntArray,
        enemyPositions: IntArray,
        dice: Int,
        safeZones: IntArray
    ): Int {
        if (tokenPositions.isEmpty()) return -1
        if (tokenPositions.size == 1) return 0
        if (isAvailable()) {
            return nativeChooseLudoBestToken(
                tokenPositions,
                tokenStartTrackIndices,
                enemyPositions,
                dice,
                safeZones
            )
        }
        val safeSet = safeZones.toHashSet()
        val enemySet = enemyPositions.toHashSet()
        fun destFor(p: Int, start: Int): Int? {
            if (p == 57) return null
            if (p == -1) return if (dice == 6) start else null
            val rel = if (p in 52..56) p - 1 else (p - start + 52) % 52
            val nextRel = rel + dice
            return when {
                nextRel > 56 -> null
                nextRel == 56 -> 57
                nextRel in 51..55 -> nextRel + 1
                else -> (start + nextRel) % 52
            }
        }
        var bestIdx = 0
        var bestScore = Int.MIN_VALUE
        for (i in tokenPositions.indices) {
            val pos = tokenPositions[i]
            val start = tokenStartTrackIndices[i]
            val dest = destFor(pos, start) ?: continue
            var score = 0
            if (dest in 0..51 && dest !in safeSet && dest in enemySet) score += 10000
            if (dest == 57) score += 5000
            if (dice == 6 && pos == -1) score += 2500
            if (dest in 52..56 || dest in safeSet) score += 1200
            score += when {
                pos in 52..56 -> pos
                pos == -1 -> 0
                else -> (pos - start + 52) % 52
            }
            if (score > bestScore) {
                bestScore = score
                bestIdx = i
            }
        }
        return bestIdx
    }

    fun chooseChaturangaBestMove(
        capturedPoints: IntArray,
        capturedIsRaja: BooleanArray,
        toRows: IntArray,
        toCols: IntArray
    ): Int {
        if (capturedPoints.isEmpty()) return -1
        if (capturedPoints.size == 1) return 0
        if (isAvailable()) {
            return nativeChooseChaturangaBestMove(capturedPoints, capturedIsRaja, toRows, toCols)
        }
        var bestScore = Int.MIN_VALUE
        val bestIndices = mutableListOf<Int>()
        for (i in capturedPoints.indices) {
            var score = 0
            if (capturedIsRaja[i]) score += 50000 else if (capturedPoints[i] > 0) score += capturedPoints[i] * 100
            val distCenter = abs(toRows[i] * 2 - 7) + abs(toCols[i] * 2 - 7)
            score += (14 - distCenter)
            if (score > bestScore) {
                bestScore = score
                bestIndices.clear()
                bestIndices.add(i)
            } else if (score == bestScore) {
                bestIndices.add(i)
            }
        }
        return if (bestIndices.isEmpty()) 0 else bestIndices[Random.nextInt(bestIndices.size)]
    }

    fun synthesizeWoodyTock(
        sampleRate: Int,
        durationSec: Double,
        baseFreq: Double,
        sweep: Double,
        volume: Double
    ): ShortArray {
        if (isAvailable()) {
            return nativeSynthesizeWoodyTock(sampleRate, durationSec, baseFreq, sweep, volume)
        }
        val numSamples = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = baseFreq + sweep * exp(-55.0 * t)
            val attack = (t / 0.004).coerceIn(0.0, 1.0)
            val decay = exp(-42.0 * t)
            val body = sin(2.0 * PI * freq * t) * 0.8 + sin(2.0 * PI * (freq * 1.48) * t) * 0.2
            val sample = (body * attack * decay * volume).coerceIn(-0.9, 0.9)
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    fun synthesizeTempleChime(
        sampleRate: Int,
        durationSec: Double,
        fundamental: Double
    ): ShortArray {
        if (isAvailable()) {
            return nativeSynthesizeTempleChime(sampleRate, durationSec, fundamental)
        }
        val numSamples = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.008).coerceIn(0.0, 1.0)
            val env = attack * exp(-14.5 * t)
            val overtoneEnv = attack * exp(-22.0 * t)
            val wave = sin(2.0 * PI * fundamental * t) * 0.65 * env +
                sin(2.0 * PI * (fundamental * 2.0) * t) * 0.22 * overtoneEnv +
                sin(2.0 * PI * (fundamental * 2.76) * t) * 0.13 * overtoneEnv
            val sample = (wave * 0.26).coerceIn(-0.9, 0.9)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun synthesizeVictorySwell(sampleRate: Int, durationSec: Double): ShortArray {
        if (isAvailable()) {
            return nativeSynthesizeVictorySwell(sampleRate, durationSec)
        }
        val numSamples = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(numSamples)
        val freqs = doubleArrayOf(261.63, 392.00, 523.25, 659.25)
        val entryTimes = doubleArrayOf(0.0, 0.06, 0.13, 0.21)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var mix = 0.0
            for (k in freqs.indices) {
                val dt = t - entryTimes[k]
                if (dt > 0.0) {
                    val env = (dt / 0.028).coerceIn(0.0, 1.0) * exp(-3.8 * dt)
                    val f = freqs[k]
                    val voice = sin(2.0 * PI * f * dt) * 0.58 +
                        sin(2.0 * PI * (f * 2.0) * dt) * 0.26 +
                        sin(2.0 * PI * (f * 3.0) * dt) * 0.16
                    mix += voice * env
                }
            }
            val sample = (mix * 0.22).coerceIn(-0.92, 0.92)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun synthesizeMusicNote(
        outBuffer: ShortArray,
        sampleRate: Int,
        melodyFreq: Double,
        harmonicFreq: Double,
        droneFreq: Double,
        isJoyful: Boolean,
        globalSampleStart: Long
    ) {
        if (isAvailable()) {
            nativeSynthesizeMusicNote(
                outBuffer,
                sampleRate,
                melodyFreq,
                harmonicFreq,
                droneFreq,
                isJoyful,
                globalSampleStart
            )
            return
        }
        val decayRate = if (isJoyful) 5.5 else 2.2
        val harmonicWeight = if (isJoyful) 0.18 else 0.12
        val droneEnv = if (isJoyful) 0.25 else 0.42
        for (i in outBuffer.indices) {
            val tLocal = i.toDouble() / sampleRate
            val tGlobal = (globalSampleStart + i).toDouble() / sampleRate
            val attack = (tLocal / 0.025).coerceIn(0.0, 1.0)
            val env = attack * exp(-decayRate * tLocal)
            val melodyWave = sin(2.0 * PI * melodyFreq * tLocal) * 0.65 +
                sin(2.0 * PI * harmonicFreq * tLocal) * harmonicWeight
            val droneWave = sin(2.0 * PI * droneFreq * tGlobal) * droneEnv +
                sin(2.0 * PI * (droneFreq * 1.5) * tGlobal) * (droneEnv * 0.45)
            val mixed = (melodyWave * env + droneWave) * 0.14
            outBuffer[i] = (mixed.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun sanitizeAndFormatMath(rawText: String, standardGrade: Int, streamId: String): String {
        if (isAvailable()) {
            return nativeSanitizeAndFormatMath(rawText, standardGrade, streamId)
        }
        return rawText
            .replace("###/", "")
            .replace("##/", "")
            .replace("#/", "")
            .replace("\\tfrac", "\\frac")
            .replace("\\dfrac", "\\frac")
            .replace("\\cfrac", "\\frac")
            .replace("\\degree", "^\\circ")
    }

    fun calibrateCosmosPrompt(
        userQuery: String,
        standardGrade: Int,
        streamId: String,
        scopeTitle: String = "Vidhya Scholar"
    ): String {
        if (isAvailable()) {
            return nativeCalibrateCosmosPrompt(userQuery, standardGrade, streamId)
        }
        return "You are Cosmium—an encouraging AI study sanctuary for $scopeTitle (Standard $standardGrade • $streamId). " +
            "Give crystal-clear step-by-step explanations with LaTeX/KaTeX math formatting.\n\nStudent Question: $userQuery"
    }

    // --- Encapsulated JVM Test Fallbacks ---

    private val CONCEPT_SYNONYMS_JVM: Map<String, List<String>> = mapOf(
        "derivative" to listOf("differentiation", "slope", "tangent", "calculus", "rate", "dx"),
        "integral" to listOf("integration", "antiderivative", "area", "substitution", "calculus"),
        "limit" to listOf("continuity", "approaches", "calculus", "indeterminate"),
        "newton" to listOf("force", "mass", "acceleration", "inertia", "momentum", "friction"),
        "kirchhoff" to listOf("circuit", "current", "voltage", "loop", "junction", "wheatstone", "resistor"),
        "ohm" to listOf("voltage", "current", "resistance", "circuit", "joule", "power"),
        "mirror" to listOf("lens", "optics", "refraction", "snell", "focal", "reflection"),
        "thermo" to listOf("thermodynamics", "heat", "entropy", "internal", "isothermal", "adiabatic"),
        "redox" to listOf("oxidation", "reduction", "electron", "nernst", "electrochemistry"),
        "quantum" to listOf("orbital", "bohr", "electron", "pauli", "spin", "atomic"),
        "mitosis" to listOf("meiosis", "cell", "chromosome", "diploid", "haploid", "genetics"),
        "dna" to listOf("rna", "replication", "transcription", "genetics", "mendel", "operon"),
        "debit" to listOf("credit", "ledger", "journal", "accounting", "assets", "liabilities"),
        "elasticity" to listOf("demand", "price", "marginal", "utility", "microeconomics"),
        "gdp" to listOf("macroeconomics", "multiplier", "mpc", "national", "income"),
        "python" to listOf("function", "recursion", "list", "algorithm", "complexity", "code")
    )

    private fun jvmEncodeConceptFallback(text: String, dim: Int): FloatArray {
        val vec = FloatArray(dim)
        val tokens = MemoryMappedKeywordIndex.tokenize(text)
        if (tokens.isEmpty()) return vec

        fun addToken(token: String, weight: Float) {
            val h1 = token.hashCode()
            val idx1 = ((h1 and 0x7FFFFFFF) % dim)
            val sign1 = if ((h1 ushr 31) == 0) 1f else -1f
            vec[idx1] += sign1 * weight

            val h2 = (token + "_cosmium_b").hashCode()
            val idx2 = ((h2 and 0x7FFFFFFF) % dim)
            val sign2 = if ((h2 ushr 31) == 0) 1f else -1f
            vec[idx2] += sign2 * weight * 0.7f
        }

        for (tok in tokens) {
            addToken(tok, 1.0f)
            for ((root, syns) in CONCEPT_SYNONYMS_JVM) {
                if (tok.contains(root) || syns.any { tok.contains(it) }) {
                    addToken("syn_$root", 0.65f)
                }
            }
        }
        for (i in 0 until tokens.lastIndex) {
            addToken("${tokens[i]}_${tokens[i + 1]}", 0.55f)
        }
        var sumSq = 0.0
        for (v in vec) sumSq += v * v
        val norm = sqrt(sumSq).toFloat()
        if (norm <= 1e-6f) return vec
        return FloatArray(dim) { idx -> vec[idx] / norm }
    }

    private fun normalizeSpeechAndMathJvm(raw: String): String {
        return raw.lowercase()
            .replace("equal to", " = ")
            .replace("equals", " = ")
            .replace("plus", " + ")
            .replace("minus", " - ")
            .replace("multiplied by", " * ")
            .replace("times", " * ")
            .replace("divided by", " / ")
            .replace("squared", "^2")
            .replace("cubed", "^3")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
