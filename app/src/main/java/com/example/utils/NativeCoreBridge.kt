package com.example.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Single authoritative JNI loader & bridge to `libcosmium_native.so` (`cosmium::math`,
 * `cosmium::rag`, `cosmium::games`, `cosmium::audio`, and OpenGL renderers).
 *
 * Production Android execution runs in native C++17. Host-JVM parity fallbacks live solely inside
 * this boundary module so Robolectric/JUnit tests execute identically without `libcosmium_native.so`.
 */
object NativeCoreBridge {
    private val loaded: Boolean by lazy {
        runCatching {
            System.loadLibrary("cosmium_native")
            true
        }.getOrDefault(false)
    }

    fun ensureLoaded(): Boolean = loaded

    fun isAvailable(): Boolean = loaded

    // --- Raw JNI Bindings ---

    @JvmStatic external fun nativeCosmosSystemInstruction(): String

    @JvmStatic external fun nativeSanitizeAndFormatMath(
        rawText: String,
        standardGrade: Int,
        streamId: String
    ): String

    @JvmStatic external fun nativeCalibrateCosmosPrompt(
        userQuery: String,
        standardGrade: Int,
        streamId: String
    ): String

    @JvmStatic external fun nativeBuildMarkdownWithProtectedMathHtml(rawText: String): String

    @JvmStatic external fun nativeFormatReadableMathFallback(rawText: String): String

    @JvmStatic external fun nativeFormatAsciiFormulaToLatex(rawFormula: String): String

    @JvmStatic external fun nativeTokenizeRagText(rawText: String): Array<String>

    @JvmStatic external fun nativeBuildSafeFtsMatchQuery(rawQuery: String): String

    @JvmStatic external fun nativeCosineSimilarity(aArray: FloatArray, bArray: FloatArray): Float

    @JvmStatic external fun nativeEncodeConceptVector(text: String, dim: Int): FloatArray

    @JvmStatic external fun nativeEncodeWeightedDocumentVector(
        topicTitle: String,
        subjectName: String,
        formulaTags: String,
        contentText: String,
        dim: Int
    ): FloatArray

    @JvmStatic external fun nativeComputeTopicSpecificityScore(
        query: String,
        topicTitle: String,
        subjectName: String,
        formulaTags: String,
        contentText: String,
        denseSim: Float,
        ftsScore: Float,
        bm25Score: Float
    ): Float

    @JvmStatic external fun nativeComposeFallbackTextbookResponse(
        topicTitle: String,
        subjectName: String,
        chapterNumber: Int,
        contentText: String,
        formulaTags: String,
        workedExample: String,
        lowRamNotice: Boolean
    ): String

    @JvmStatic external fun nativeEvaluateQuizAnswer(
        studentAnswer: String,
        canonicalAnswer: String,
        keywordsPipe: String
    ): Boolean

    @JvmStatic external fun nativeIsChessKingAttacked(boardArray: IntArray, side: Int): Boolean

    @JvmStatic external fun nativeHasAnyChessLegalMoves(boardArray: IntArray, side: Int): Boolean

    @JvmStatic external fun nativeGetChessValidMoves(
        boardArray: IntArray,
        turnSide: Int,
        fromRow: Int,
        fromCol: Int
    ): IntArray

    @JvmStatic external fun nativeComputeChessBotMove(
        boardArray: IntArray,
        turnSide: Int,
        difficulty: Int
    ): Int

    @JvmStatic external fun nativeGetLudoDestinationPosition(
        position: Int,
        startTrackIndex: Int,
        dice: Int
    ): Int

    @JvmStatic external fun nativeChooseLudoBestToken(
        tokenPositionsArr: IntArray,
        tokenStartTrackArr: IntArray,
        enemyPositionsArr: IntArray,
        dice: Int,
        safeZonesArr: IntArray
    ): Int

    @JvmStatic external fun nativeChooseChaturangaBestMove(
        capturedPointsArr: IntArray,
        capturedIsRajaArr: BooleanArray,
        toRowsArr: IntArray,
        toColsArr: IntArray
    ): Int

    @JvmStatic external fun nativeSynthesizeWoodyTock(
        sampleRate: Int,
        durationSec: Double,
        baseFreq: Double,
        sweep: Double,
        volume: Double
    ): ShortArray

    @JvmStatic external fun nativeSynthesizeTempleChime(
        sampleRate: Int,
        durationSec: Double,
        fundamental: Double
    ): ShortArray

    @JvmStatic external fun nativeSynthesizeVictorySwell(
        sampleRate: Int,
        durationSec: Double
    ): ShortArray

    @JvmStatic external fun nativeSynthesizeMusicNote(
        outBuffer: ShortArray,
        sampleRate: Int,
        melodyFreq: Double,
        harmonicFreq: Double,
        droneFreq: Double,
        isJoyful: Boolean,
        globalSampleStart: Long
    )

    @JvmStatic external fun nativeSynthesizeGameMusicStep(
        gameTypeOrdinal: Int,
        noteIndex: Int,
        sampleRate: Int,
        globalSampleStart: Long
    ): ShortArray

    // =========================================================================
    // 1. Math, Markdown & Prompt Calibration Facade (`cosmium::math`)
    // =========================================================================

    fun cosmosSystemInstruction(): String {
        if (isAvailable()) return nativeCosmosSystemInstruction()
        return "You are Cosmium, the AI study mentor for Vidhya Scholar aligned with the latest rationalized NCERT & NCF-SE competency-based syllabus.\n" +
            "Follow these strict pedagogical, anti-hallucination, and formatting rules for every answer:\n" +
            "1. Strict Topic Purity & Latest NCERT Fidelity: Focus strictly on the exact concept, theorem, historical date, or problem the student asked about. " +
            "Follow the latest rationalized NCERT syllabus (never confuse students with dropped chapters, and give deep conceptual clarity suited to modern competency/application questions). Never mix formulas or examples from unrelated neighboring topics.\n" +
            "2. Interleaved Textbook Structure (Never Dump Formulas Together): Alternate explanation sentences with equations:\n" +
            "   - Open with a clear definition and 2-3 intuitive takeaways.\n" +
            "   - '### 1. Mathematical Formulation' (or Core Rules / Milestones): Introduce the core equation or rule in prose, show it alone in \$\$...\$\$, add a '**where:**' bullet list explaining every symbol with \$...\$, then explain any general form in prose + \$\$...\$\$.\n" +
            "   - '### 2. Why It Works' (or Derivation / Cause & Logic): Walk through each logical step alternating prose and \$\$...\$\$.\n" +
            "   - '### 3. Crucial Nuances & Common Pitfalls': 3-4 concept-specific bullets on scope limits, boundary conditions, and exam traps.\n" +
            "   - '### 4. Quick Worked Example': Solve one focused competency-style example step by step with prose + \$\$...\$\$.\n" +
            "3. KaTeX Delimiters: Always wrap inline variables and symbols in \$...\$ (e.g. \$W_{\\text{net}}\$, \$\\vec{v}\$). " +
            "Place every standalone equation, derivation step, and final result on its own \$\$...\$\$ block with blank lines before and after. Never wrap math inside ``` code fences."
    }

    fun sanitizeAndFormatMath(rawText: String, standardGrade: Int, streamId: String): String {
        if (isAvailable()) return nativeSanitizeAndFormatMath(rawText, standardGrade, streamId)
        return rawText
            .replace("###/", "")
            .replace("##/", "")
            .replace("#/", "")
            .replace("###$$", "###\n\n$$")
            .replace("##$$", "##\n\n$$")
            .replace("#$$", "#\n\n$$")
            .replace("$$###", "$$\n\n###")
            .replace("$$##", "$$\n\n##")
            .replace("$$#", "$$\n\n#")
            .replace("\\tfrac", "\\frac")
            .replace("\\dfrac", "\\frac")
            .replace("\\cfrac", "\\frac")
            .replace("\\degree", "^\\circ")
            .replace("\\[", "\n\n$$\n")
            .replace("\\]", "\n$$\n\n")
            .replace("\\(", "$")
            .replace("\\)", "$")
            .replace(Regex("```(?:latex|tex|math)\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)) { m ->
                "\n\n$$\n${m.groupValues[1].trim()}\n$$\n\n"
            }
    }

    fun calibrateCosmosPrompt(
        userQuery: String,
        standardGrade: Int,
        streamId: String,
        scopeTitle: String = "Vidhya Scholar"
    ): String {
        if (isAvailable()) return nativeCalibrateCosmosPrompt(userQuery, standardGrade, streamId)
        return "You are Cosmium—an encouraging AI study sanctuary for $scopeTitle (Latest Rationalized NCERT Standard $standardGrade • $streamId).\n" +
            "[Strict Pedagogical & Anti-Hallucination Rules]\n" +
            "1. Stay 100% On Topic & Latest NCERT Aligned: Answer only what the student asked using latest rationalized NCERT concepts and competency depth. Never mix unrelated chapter formulas (e.g. friction/inclined planes inside Work-Energy Theorem).\n" +
            "2. Never Group Equations Into One Dump: Explain a line in prose, place its equation in \$\$...\$\$, and explain variables under **where:** using \$...\$ before continuing.\n" +
            "3. Consistent Textbook Sections: Use ### headings (1. Mathematical Formulation, 2. Why It Works / Derivation, 3. Crucial Nuances & Common Pitfalls, 4. Quick Worked Example).\n" +
            "4. Clean KaTeX Delimiters: Always wrap inline variables in \$...\$ and place every major equation on its own \$\$...\$\$ line with blank lines before and after.\n\n" +
            "Student Question: $userQuery"
    }

    fun buildMarkdownWithProtectedMathHtml(rawText: String): String {
        if (isAvailable()) return nativeBuildMarkdownWithProtectedMathHtml(rawText)
        return JvmParity.buildMarkdownWithProtectedMathHtml(rawText)
    }

    fun formatReadableMathFallback(rawText: String): String {
        if (isAvailable()) return nativeFormatReadableMathFallback(rawText)
        return JvmParity.formatReadableMathFallback(rawText)
    }

    fun formatAsciiFormulaToLatex(rawFormula: String): String {
        if (isAvailable()) return nativeFormatAsciiFormulaToLatex(rawFormula)
        return JvmParity.formatAsciiFormulaToLatex(rawFormula)
    }

    // =========================================================================
    // 2. RAG Tokenization, Dense Vectors & Quiz Evaluation (`cosmium::rag`)
    // =========================================================================

    fun tokenizeRagText(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()
        if (isAvailable()) return nativeTokenizeRagText(rawText).toList()
        return JvmParity.tokenizeRagText(rawText)
    }

    fun buildSafeFtsMatchQuery(rawQuery: String): String {
        if (rawQuery.isBlank()) return ""
        if (isAvailable()) return nativeBuildSafeFtsMatchQuery(rawQuery)
        return JvmParity.tokenizeRagText(rawQuery)
            .map { it.replace(Regex("[^a-z0-9_]"), "") }
            .filter { it.length >= 2 }
            .distinct()
            .take(8)
            .joinToString(" OR ") { "${it}*" }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (isAvailable()) return nativeCosineSimilarity(a, b)
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
        if (isAvailable()) return nativeEncodeConceptVector(clean, dim)
        return JvmParity.encodeConceptVector(clean, dim)
    }

    fun encodeWeightedDocumentVector(
        topicTitle: String,
        subjectName: String,
        formulaTags: String,
        contentText: String,
        dim: Int = 384
    ): FloatArray {
        if (isAvailable()) {
            return nativeEncodeWeightedDocumentVector(topicTitle, subjectName, formulaTags, contentText, dim)
        }
        return JvmParity.encodeWeightedDocumentVector(topicTitle, subjectName, formulaTags, contentText, dim)
    }

    fun computeTopicSpecificityScore(
        query: String,
        topicTitle: String,
        subjectName: String,
        formulaTags: String,
        contentText: String,
        denseSim: Float,
        ftsScore: Float,
        bm25Score: Float
    ): Float {
        if (isAvailable()) {
            return nativeComputeTopicSpecificityScore(
                query, topicTitle, subjectName, formulaTags, contentText, denseSim, ftsScore, bm25Score
            )
        }
        return JvmParity.computeTopicSpecificityScore(
            query, topicTitle, subjectName, formulaTags, contentText, denseSim, ftsScore, bm25Score
        )
    }

    fun composeFallbackTextbookResponse(
        topicTitle: String,
        subjectName: String,
        chapterNumber: Int,
        contentText: String,
        formulaTags: String,
        workedExample: String,
        lowRamNotice: Boolean
    ): String {
        if (isAvailable()) {
            return nativeComposeFallbackTextbookResponse(
                topicTitle, subjectName, chapterNumber, contentText, formulaTags, workedExample, lowRamNotice
            )
        }
        return JvmParity.composeFallbackTextbookResponse(
            topicTitle, subjectName, chapterNumber, contentText, formulaTags, workedExample, lowRamNotice
        )
    }

    fun evaluateQuizAnswer(
        studentAnswer: String,
        canonicalAnswer: String,
        expectedKeywords: List<String>
    ): Boolean {
        val clean = studentAnswer.trim()
        if (clean.isEmpty()) return false
        val joinedKeywords = expectedKeywords.joinToString("|")
        if (isAvailable()) return nativeEvaluateQuizAnswer(clean, canonicalAnswer, joinedKeywords)
        return JvmParity.evaluateQuizAnswer(clean, canonicalAnswer, expectedKeywords)
    }

    // =========================================================================
    // 3. Strategy Game Engines (`cosmium::games`)
    // =========================================================================

    fun isChessKingAttacked(boardArray: IntArray, side: Int): Boolean {
        if (boardArray.size != 64) return false
        if (isAvailable()) return nativeIsChessKingAttacked(boardArray, side)
        return JvmParity.isChessKingInCheck(boardArray, side)
    }

    fun hasAnyChessLegalMoves(boardArray: IntArray, side: Int): Boolean {
        if (boardArray.size != 64) return false
        if (isAvailable()) return nativeHasAnyChessLegalMoves(boardArray, side)
        return JvmParity.hasAnyChessLegalMoves(boardArray, side)
    }

    fun getChessValidMoves(boardArray: IntArray, turnSide: Int, fromRow: Int, fromCol: Int): IntArray {
        if (boardArray.size != 64) return IntArray(0)
        if (isAvailable()) return nativeGetChessValidMoves(boardArray, turnSide, fromRow, fromCol)
        return JvmParity.getChessValidMoves(boardArray, turnSide, fromRow, fromCol)
    }

    fun computeChessBotMove(boardArray: IntArray, turnSide: Int, difficulty: Int): Int? {
        if (boardArray.size != 64) return null
        if (isAvailable()) {
            val encoded = nativeComputeChessBotMove(boardArray, turnSide, difficulty)
            return encoded.takeIf { it != -1 }
        }
        return JvmParity.computeChessBotMove(boardArray, turnSide, difficulty)
    }

    fun getLudoDestinationPosition(position: Int, startTrackIndex: Int, dice: Int): Int? {
        if (isAvailable()) {
            val dest = nativeGetLudoDestinationPosition(position, startTrackIndex, dice)
            return dest.takeIf { it != -1 }
        }
        return JvmParity.getLudoDestinationPosition(position, startTrackIndex, dice)
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
        return JvmParity.chooseLudoBestToken(
            tokenPositions,
            tokenStartTrackIndices,
            enemyPositions,
            dice,
            safeZones
        )
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

    // =========================================================================
    // 4. Procedural Audio & Music DSP (`cosmium::audio`)
    // =========================================================================

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

    fun synthesizeTempleChime(sampleRate: Int, durationSec: Double, fundamental: Double): ShortArray {
        if (isAvailable()) return nativeSynthesizeTempleChime(sampleRate, durationSec, fundamental)
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
        if (isAvailable()) return nativeSynthesizeVictorySwell(sampleRate, durationSec)
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

    fun synthesizeGameMusicStep(
        gameTypeOrdinal: Int,
        noteIndex: Int,
        sampleRate: Int,
        globalSampleStart: Long
    ): ShortArray {
        if (isAvailable()) {
            return nativeSynthesizeGameMusicStep(gameTypeOrdinal, noteIndex, sampleRate, globalSampleStart)
        }
        return JvmParity.synthesizeGameMusicStep(gameTypeOrdinal, noteIndex, sampleRate, globalSampleStart)
    }

    // =========================================================================
    // Host-JVM Parity Engine (Executed ONLY when libcosmium_native.so is absent)
    // =========================================================================

    private object JvmParity {
        private val STOP_WORDS = setOf(
            "the", "is", "are", "was", "were", "a", "an", "and", "or", "of", "to", "in",
            "for", "on", "with", "by", "from", "at", "as", "that", "this", "what", "how",
            "why", "when", "which", "who", "please", "explain", "solve", "tell", "me", "my",
            "can", "you", "it", "be", "into", "using", "about", "define", "state", "derive",
            "give", "example", "describe", "does"
        )

        private val GENERIC_MODIFIERS = setOf(
            "law", "laws", "theorem", "rule", "rules", "equation", "equations", "formula",
            "formulas", "principle", "principles", "concept", "theory", "method", "methods",
            "effect", "process", "processes", "model", "first", "second", "third", "general",
            "basic", "work", "energy", "force", "rate", "change", "value", "system", "physics",
            "chemistry", "math", "mathematics", "biology", "science", "economics", "accountancy",
            "business", "history", "political", "social", "chapter", "topic", "class", "question"
        )

        private val BARE_LATEX_REGEX = Regex(
            """\\(?:frac|sqrt|vec|overrightarrow|overline|hat|bar|mathbf|mathrm|text)\s*(?:\{[^{}]*\}|\[[^\[\]]*\])+(?:\s*(?:[=+\-*/^_]\s*(?:\{[^{}]*\}|[A-Za-z0-9\\]+))+)?|\\(?:int|sum|prod|lim|partial|nabla|infty|alpha|beta|gamma|delta|theta|phi|omega|lambda|sigma|pi|mu|rho|rightarrow|leftarrow|rightleftharpoons|leftrightarrow|approx|equiv|neq|le|ge|pm|times|cdot|div)\b"""
        )

        // Exact parity with cosmium_rag.cpp (58 non-overlapping NCERT topic clusters)
        private val CONCEPT_SYNONYMS = mapOf(
            // Mechanics & Physics
            "work_energy" to listOf("work_energy", "workenergy", "kinetic", "w_net", "wnet", "delta_k"),
            "friction" to listOf("friction", "mu_s", "mu_k", "repose", "limiting", "static_friction", "rough"),
            "impulse_momentum" to listOf("impulse", "momentum", "collision", "delta_p", "recoil", "conservation_momentum"),
            "circular_motion" to listOf("centripetal", "centrifugal", "banking", "angular", "a_c"),
            "newton_laws" to listOf("newton", "inertia", "fbd", "free_body", "action_reaction", "f_eq_ma"),
            "kinematics" to listOf("kinematics", "uniformly", "accelerated", "displacement", "velocity", "v=u+at"),
            "projectile_motion" to listOf("projectile", "trajectory", "parabolic", "h_max", "horizontal_range"),
            "gravitation" to listOf("gravitation", "gravity", "kepler", "escape_velocity", "orbital_velocity", "buoyancy", "archimedes"),
            "thermodynamics" to listOf("thermodynamics", "thermo", "entropy", "enthalpy", "isothermal", "adiabatic", "pv_eq_nrt", "internal_energy"),
            // Optics & Electromagnetism
            "optics_mirror" to listOf("mirror", "concave", "convex_mirror", "magnification", "pole", "focal"),
            "optics_refraction" to listOf("refraction", "snell", "lens", "dioptre", "refractive", "refractive_index"),
            "ohm_circuits" to listOf("ohm", "resistor", "resistance", "resistivity", "v_eq_ir", "series", "parallel"),
            "joule_heating" to listOf("joule_heating", "electric_power", "heat_dissipated", "watt", "i2rt"),
            "kirchhoff_wheatstone" to listOf("kirchhoff", "kcl", "kvl", "wheatstone", "junction", "loop", "drift"),
            "emi_faraday" to listOf("faraday", "lenz", "flux", "induced", "emf", "magnetic_flux"),
            "ac_impedance" to listOf("impedance", "lcr", "resonance", "inductive", "capacitive", "phasor", "alternating"),
            "modern_photoelectric" to listOf("photoelectric", "work_function", "stopping", "photon", "einstein", "threshold_frequency"),
            "bohr_spectrum" to listOf("rydberg", "balmer", "lyman", "paschen", "hydrogen_spectrum"),
            // Mathematics (Class 9-12)
            "real_numbers" to listOf("rational", "irrational", "surd", "rationalize", "conjugate", "hcf", "lcm", "fundamental_theorem"),
            "exponents_surds" to listOf("exponent", "exponents", "nth_root", "power_law"),
            "polynomials_identities" to listOf("polynomial", "identity", "identities", "factorization", "remainder", "factor_theorem"),
            "linear_equations" to listOf("linear_equations", "substitution", "elimination", "intersecting", "coincident", "consistent"),
            "coordinate_geometry" to listOf("coordinate", "distance_formula", "midpoint", "section_formula", "cartesian", "collinear", "abscissa"),
            "heron_triangles" to listOf("heron", "semiperimeter", "triangle", "congruence", "thales", "bpt", "proportionality"),
            "quadratic" to listOf("quadratic", "discriminant", "roots", "parabola", "vieta"),
            "ap_series" to listOf("arithmetic_progression", "progression", "common_difference", "nth_term", "s_n", "a_n"),
            "trigonometry" to listOf("trigonometry", "trigonometric", "sine", "cosine", "pythagorean", "heights", "elevation"),
            "derivative" to listOf("derivative", "differentiation", "power_rule", "product_rule", "tangent", "slope", "d_dx"),
            "limit" to listOf("limit", "continuity", "approaches", "indeterminate", "squeeze"),
            "integral" to listOf("integral", "integration", "antiderivative", "substitution", "ilate", "definite"),
            "matrices_determinants" to listOf("matrix", "matrices", "determinant", "inverse_matrix", "cofactor", "adjoint"),
            "probability_bayes" to listOf("probability", "bayes", "conditional", "independent_events", "random_variable"),
            // Chemistry & Biology
            "chemical_reactions" to listOf("balancing", "combination", "decomposition", "displacement", "rancidity", "corrosion", "exothermic"),
            "mole_concept" to listOf("mole", "avogadro", "molar", "stoichiometry", "6.022"),
            "atomic_orbital" to listOf("quantum", "orbital", "bohr", "pauli", "spin", "broglie", "subshell"),
            "redox" to listOf("redox", "oxidation", "reduction", "oxidizing", "reducing"),
            "bonding_vsepr" to listOf("vsepr", "hybridization", "tetrahedral", "sp3", "sp2", "lone_pair"),
            "chemical_equilibrium" to listOf("equilibrium", "chatelier", "kp", "kc", "ph", "buffer"),
            "electrochemistry" to listOf("nernst", "electrode", "galvanic", "e0_cell", "gibbs"),
            "kinetics_chem" to listOf("kinetics", "rate_constant", "first_order", "half_life", "arrhenius"),
            "life_processes" to listOf("nutrition", "respiration", "aerobic", "anaerobic", "pyruvate", "nephron", "xylem", "phloem"),
            "cell_mitosis" to listOf("mitosis", "meiosis", "cell_cycle", "fluid_mosaic", "chromosome", "ploidy"),
            "genetics_dna" to listOf("dna", "rna", "replication", "okazaki", "mendel", "hardy", "operon"),
            "photosynthesis_bio" to listOf("photosynthesis", "chloroplast", "calvin", "atp", "nadph", "light_reaction"),
            // Coding & CS
            "python_algo" to listOf("python", "recursion", "binary_search", "complexity", "memoization", "stack"),
            // Commerce (Accountancy, Economics, Business Studies)
            "accounting_rules" to listOf("debit", "credit", "ledger", "journal", "assets", "liabilities", "double_entry", "drawings"),
            "depreciation_acc" to listOf("depreciation", "straight_line", "written_down", "slm", "wdv", "salvage"),
            "accounting_ratios" to listOf("liquidity", "current_ratio", "quick_ratio", "solvency", "acid_test"),
            "goodwill_valuation" to listOf("goodwill", "super_profit", "capital_employed", "normal_profit", "years_purchase"),
            "micro_utility" to listOf("marginal_utility", "diminishing", "consumer_equilibrium", "satiety", "total_utility"),
            "micro_elasticity" to listOf("elasticity", "price_elasticity", "inelastic", "unitary", "e_d"),
            "macro_gdp" to listOf("gdp", "nnp", "nfia", "national_income", "expenditure_method", "factor_cost"),
            "macro_multiplier" to listOf("multiplier", "mpc", "mps", "keynes", "autonomous_investment", "paradox_thrift"),
            "business_mgmt" to listOf("fayol", "taylor", "management", "planning", "organizing", "controlling", "unity_command", "scalar_chain"),
            // Social Science, History & Political Science
            "constitution_rights" to listOf("constitution", "fundamental_rights", "habeas", "mandamus", "article", "writ", "preamble"),
            "indian_history" to listOf("champaran", "satyagraha", "dandi", "non_cooperation", "constituent", "gandhi", "quit_india", "rowlatt"),
            "french_revolution_civics" to listOf("french_revolution", "bastille", "robespierre", "jacobean", "estates_general"),
            "federalism_polity" to listOf("federalism", "concurrent_list", "union_list", "state_list", "panchayati", "decentralisation")
        )

        fun tokenizeRagText(rawText: String): List<String> {
            val normalized = rawText.lowercase()
                .replace("work-energy", " work_energy work energy kinetic ")
                .replace("work energy", " work_energy work energy kinetic ")
                .replace("arithmetic progression", " arithmetic_progression ap common_difference ")
                .replace("fundamental theorem of arithmetic", " fundamental_theorem real_numbers hcf lcm ")
                .replace("linear equations", " linear_equations substitution elimination consistent ")
                .replace("distance formula", " distance_formula coordinate_geometry ")
                .replace("section formula", " section_formula coordinate_geometry ")
                .replace("heron's formula", " heron semiperimeter triangle ")
                .replace("heron formula", " heron semiperimeter triangle ")
                .replace("bayes theorem", " bayes conditional probability ")
                .replace("bayes' theorem", " bayes conditional probability ")
                .replace("fundamental rights", " fundamental_rights constitution article writs ")
                .replace("french revolution", " french_revolution bastille estates_general ")
                .replace("estates general", " estates_general french_revolution ")
                .replace("concurrent list", " concurrent_list federalism union_list state_list ")
                .replace("current ratio", " current_ratio liquidity quick_ratio ")
                .replace("quick ratio", " quick_ratio liquidity acid_test ")
                .replace("super profit", " super_profit goodwill capital_employed ")
                .replace("price elasticity", " price_elasticity elasticity demand ")
                .replace("marginal utility", " marginal_utility consumer_equilibrium ")
                .replace("investment multiplier", " multiplier mpc mps macroeconomics ")
                .replace("straight line", " straight_line depreciation slm ")
                .replace("written down", " written_down depreciation wdv ")
                .replace("w_net", " w_net work_energy ")
                .replace("f=ma", " f_eq_ma force mass acceleration newton ")
                .replace("v=ir", " v_eq_ir ohm voltage current resistance ")
                .replace("e=mc^2", " e_eq_mc2 energy relativity ")
                .replace("d/dx", " d_dx derivative differentiation calculus ")
                .replace("pv=nrt", " pv_eq_nrt ideal gas constant thermodynamics ")
                .replace("a=l+c", " accounting equation assets liabilities capital ")

            return Regex("[a-z0-9_]+(?:[+^=-][a-z0-9_]+)?").findAll(normalized)
                .map { it.value.trim() }
                .filter { it.length >= 2 && it !in STOP_WORDS }
                .toList()
        }

        private fun projectTextIntoVector(vec: FloatArray, dim: Int, text: String, fieldWeight: Float) {
            if (text.isBlank() || fieldWeight <= 0f) return
            val tokens = tokenizeRagText(text)
            if (tokens.isEmpty()) return

            fun addFeature(feature: String, weight: Float) {
                val h1 = feature.hashCode()
                vec[(h1 and 0x7FFFFFFF) % dim] += (if ((h1 ushr 31) == 0) 1f else -1f) * weight
                val h2 = (feature + "_c2").hashCode()
                vec[(h2 and 0x7FFFFFFF) % dim] += (if ((h2 ushr 31) == 0) 1f else -1f) * weight * 0.65f
                val h3 = (feature + "_r3").hashCode()
                vec[(h3 and 0x7FFFFFFF) % dim] += (if ((h3 ushr 31) == 0) 1f else -1f) * weight * 0.40f
            }

            for (tok in tokens) {
                val idfFactor = if (tok in GENERIC_MODIFIERS) 0.25f else 1.35f
                addFeature(tok, idfFactor * fieldWeight)
                if (tok.length >= 5) {
                    val limit = minOf(tok.length, 16)
                    for (i in 0..(limit - 3)) {
                        addFeature("c3_${tok.substring(i, i + 3)}", 0.20f * idfFactor * fieldWeight)
                    }
                    for (i in 0..(limit - 4)) {
                        addFeature("c4_${tok.substring(i, i + 4)}", 0.26f * idfFactor * fieldWeight)
                    }
                }
                for ((root, anchors) in CONCEPT_SYNONYMS) {
                    val exact = tok == root || anchors.any { tok == it }
                    val subHit = !exact && (tok.contains(root) || anchors.any { it.length >= 4 && tok.contains(it) })
                    if (exact) {
                        addFeature("cpt_$root", 1.45f * fieldWeight)
                    } else if (subHit) {
                        addFeature("cpt_$root", 0.65f * fieldWeight)
                    }
                }
            }
            for (i in 0 until tokens.lastIndex) {
                addFeature("bg_${tokens[i]}_${tokens[i + 1]}", 0.95f * fieldWeight)
            }
        }

        private fun l2Normalize(vec: FloatArray): FloatArray {
            var sumSq = 0.0
            for (v in vec) sumSq += v * v
            val norm = sqrt(sumSq).toFloat()
            if (norm <= 1e-6f) return vec
            return FloatArray(vec.size) { idx -> vec[idx] / norm }
        }

        fun encodeConceptVector(text: String, dim: Int): FloatArray {
            val vec = FloatArray(dim)
            projectTextIntoVector(vec, dim, text, 1.0f)
            return l2Normalize(vec)
        }

        fun encodeWeightedDocumentVector(
            topicTitle: String,
            subjectName: String,
            formulaTags: String,
            contentText: String,
            dim: Int
        ): FloatArray {
            val vec = FloatArray(dim)
            projectTextIntoVector(vec, dim, topicTitle, 3.6f)
            projectTextIntoVector(vec, dim, formulaTags, 2.3f)
            projectTextIntoVector(vec, dim, subjectName, 1.3f)
            projectTextIntoVector(vec, dim, contentText, 0.85f)
            return l2Normalize(vec)
        }

        private fun detectActiveConcepts(tokens: List<String>): Set<String> {
            val active = mutableSetOf<String>()
            for (tok in tokens) {
                for ((root, anchors) in CONCEPT_SYNONYMS) {
                    if (tok == root || tok.contains(root) ||
                        anchors.any { tok == it || (it.length >= 4 && tok.contains(it)) }
                    ) {
                        active += root
                    }
                }
            }
            return active
        }

        fun computeTopicSpecificityScore(
            query: String,
            topicTitle: String,
            subjectName: String,
            formulaTags: String,
            contentText: String,
            denseSim: Float,
            ftsScore: Float,
            bm25Score: Float
        ): Float {
            val qTokens = tokenizeRagText(query)
            if (qTokens.isEmpty()) return 0f

            val titleTokens = tokenizeRagText(topicTitle)
            val titleSet = titleTokens.toSet()
            val tagSet = tokenizeRagText("$formulaTags $subjectName").toSet()
            val bodySet = tokenizeRagText(contentText).toSet()
            val uniqueQuery = qTokens.distinct()

            fun matchesSet(qt: String, set: Set<String>): Boolean {
                if (qt in set) return true
                return qt.length >= 4 && set.any { it.length >= 4 && (it.startsWith(qt) || qt.startsWith(it)) }
            }

            var querySpecificCount = 0
            var titleAllHits = 0
            var titleSpecificHits = 0
            var tagSpecificHits = 0
            var bodySpecificHits = 0

            for (qt in uniqueQuery) {
                val isGen = qt in GENERIC_MODIFIERS
                if (!isGen) querySpecificCount++
                when {
                    matchesSet(qt, titleSet) -> {
                        titleAllHits++
                        if (!isGen) titleSpecificHits++
                    }
                    matchesSet(qt, tagSet) -> if (!isGen) tagSpecificHits++
                    matchesSet(qt, bodySet) -> if (!isGen) bodySpecificHits++
                }
            }

            val qConcepts = detectActiveConcepts(qTokens)
            val titleConcepts = detectActiveConcepts(titleTokens)
            val tagConcepts = detectActiveConcepts(tokenizeRagText("$formulaTags $subjectName"))
            var sharedTitleConcepts = 0
            var sharedTagConcepts = 0
            for (qc in qConcepts) {
                if (qc in titleConcepts) sharedTitleConcepts++
                else if (qc in tagConcepts) sharedTagConcepts++
            }

            val unionConcepts = qConcepts + titleConcepts
            val clusterJaccard = if (unionConcepts.isEmpty()) 0f else sharedTitleConcepts.toFloat() / unionConcepts.size.toFloat()

            val qLower = query.lowercase()
            val tLower = topicTitle.lowercase()
            val phraseBonus = when {
                tLower.length >= 6 && qLower.contains(tLower) -> 0.30f
                qTokens.size >= 2 && (0 until qTokens.lastIndex).any { i -> tLower.contains("${qTokens[i]} ${qTokens[i + 1]}") } -> 0.20f
                else -> 0f
            }

            val titleHitRatio = titleAllHits.toFloat() / uniqueQuery.size.coerceAtLeast(1)
            val specificRecall = if (querySpecificCount > 0) {
                ((titleSpecificHits + 0.60f * tagSpecificHits + 0.22f * bodySpecificHits) / querySpecificCount).coerceIn(0f, 1f)
            } else {
                titleHitRatio
            }
            val conceptCoverage = if (qConcepts.isNotEmpty()) {
                ((sharedTitleConcepts + 0.45f * sharedTagConcepts) / qConcepts.size).coerceIn(0f, 1f)
            } else 0f

            val mismatchMultiplier = when {
                qConcepts.isNotEmpty() && sharedTitleConcepts == 0 && sharedTagConcepts == 0 ->
                    if (titleSpecificHits == 0 && tagSpecificHits == 0) 0.18f else 0.40f
                qConcepts.isNotEmpty() && titleConcepts.isNotEmpty() && sharedTitleConcepts == 0 -> 0.42f
                querySpecificCount > 0 && titleSpecificHits == 0 && tagSpecificHits == 0 && bodySpecificHits == 0 -> 0.20f
                else -> 1f
            }

            val linearScore = (0.34f * denseSim.coerceIn(0f, 1f)) +
                (0.12f * ftsScore.coerceIn(0f, 1f)) +
                (0.14f * bm25Score.coerceIn(0f, 1f)) +
                (0.18f * specificRecall) +
                (0.12f * maxOf(titleHitRatio, conceptCoverage)) +
                (0.10f * clusterJaccard) +
                phraseBonus

            val centered = (linearScore * mismatchMultiplier) - 0.36f
            val logistic = (1.0 / (1.0 + exp(-5.2 * centered.toDouble()))).toFloat()
            val calibrated = 0.55f * (linearScore * mismatchMultiplier) + 0.45f * logistic * mismatchMultiplier
            return calibrated.coerceIn(0f, 1f)
        }

        fun composeFallbackTextbookResponse(
            topicTitle: String,
            subjectName: String,
            chapterNumber: Int,
            contentText: String,
            formulaTags: String,
            workedExample: String,
            lowRamNotice: Boolean
        ): String = buildString {
            append("### $subjectName • Ch. $chapterNumber: $topicTitle\n\n")
            if (lowRamNotice) {
                append("*Prepared using compact offline study mode.*\n\n")
            }
            append(contentText.trim())
            append("\n\n")
            val formulas = formulaTags.split(", ").map { it.trim() }.filter { it.isNotBlank() }
            if (formulas.isNotEmpty()) {
                append("### 1. Mathematical Formulation\n\n")
                append("The core relationship for **$topicTitle** is expressed as:\n\n")
                append("$$\n${formatAsciiFormulaToLatex(formulas.first())}\n$$\n\n")
                for (extra in formulas.drop(1)) {
                    append("Related mathematical form for **$topicTitle**:\n\n")
                    append("$$\n${formatAsciiFormulaToLatex(extra)}\n$$\n\n")
                }
            }
            if (workedExample.isNotBlank()) {
                append("### 2. Worked Example\n\n")
                append(workedExample.trim())
                append("\n\n")
            }
        }.trim()

        fun evaluateQuizAnswer(
            studentAnswer: String,
            canonicalAnswer: String,
            expectedKeywords: List<String>
        ): Boolean {
            val studentNorm = normalizeSpeechAndMath(studentAnswer)
            val studentCompact = studentNorm.replace(" ", "").replace("(", "").replace(")", "")
            for (kw in expectedKeywords) {
                val kwNorm = normalizeSpeechAndMath(kw)
                if (kwNorm.isNotEmpty() && studentNorm.contains(kwNorm)) return true
                val kwCompact = kwNorm.replace(" ", "").replace("(", "").replace(")", "")
                if (kwCompact.length >= 2 && studentCompact.contains(kwCompact)) return true
            }
            val canonCompact = normalizeSpeechAndMath(canonicalAnswer)
                .replace(" ", "").replace("(", "").replace(")", "")
            if (canonCompact.isNotEmpty() && studentCompact.contains(canonCompact)) return true
            return expectedKeywords.isEmpty() && studentNorm.length >= 4
        }

        private fun normalizeSpeechAndMath(raw: String): String = raw.lowercase()
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

        fun buildMarkdownWithProtectedMathHtml(rawText: String): String {
            val normalized = rawText
                .replace("\r\n", "\n")
                .replace("\\[", "\n\n$$\n")
                .replace("\\]", "\n$$\n\n")
                .replace("\\(", "$")
                .replace("\\)", "$")
                .replace(Regex("```(?:latex|tex|math)\\s*\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)) { m ->
                    "\n\n$$\n${m.groupValues[1].trim()}\n$$\n\n"
                }

            val mathSlots = mutableListOf<Pair<Boolean, String>>()
            val placeholderBuilder = StringBuilder()
            var i = 0
            while (i < normalized.length) {
                if (normalized[i] == '$' && (i == 0 || normalized[i - 1] != '\\')) {
                    if (i + 1 < normalized.length && normalized[i + 1] == '$') {
                        val close = findClosingDoubleDollar(normalized, i + 2)
                        if (close != -1) {
                            val slotIdx = mathSlots.size
                            mathSlots += true to normalized.substring(i + 2, close).trim()
                            placeholderBuilder.append("\n@@COSMOS_DISP_${slotIdx}@@\n")
                            i = close + 2
                            continue
                        }
                    } else {
                        val close = findClosingSingleDollar(normalized, i + 1)
                        if (close != -1) {
                            val slotIdx = mathSlots.size
                            mathSlots += false to normalized.substring(i + 1, close).trim()
                            placeholderBuilder.append("@@COSMOS_INL_${slotIdx}@@")
                            i = close + 1
                            continue
                        }
                    }
                }
                placeholderBuilder.append(normalized[i])
                i++
            }

            val withAutoWrapped = BARE_LATEX_REGEX.replace(placeholderBuilder.toString()) { m ->
                val slotIdx = mathSlots.size
                mathSlots += false to m.value.trim()
                "@@COSMOS_INL_${slotIdx}@@"
            }

            val htmlOut = StringBuilder()
            var inUl = false
            var inOl = false
            val paragraphLines = mutableListOf<String>()

            fun flushParagraph() {
                if (paragraphLines.isNotEmpty()) {
                    htmlOut.append("<p>").append(paragraphLines.joinToString("<br/>") { formatInlineMd(it) }).append("</p>\n")
                    paragraphLines.clear()
                }
            }

            fun closeLists() {
                if (inUl) { htmlOut.append("</ul>\n"); inUl = false }
                if (inOl) { htmlOut.append("</ol>\n"); inOl = false }
            }

            for (rawLine in withAutoWrapped.split("\n")) {
                val trimmed = rawLine.trim()
                when {
                    trimmed.isEmpty() -> { flushParagraph(); closeLists() }
                    trimmed.matches(Regex("^@@COSMOS_DISP_\\d+@@$")) -> {
                        flushParagraph(); closeLists()
                        htmlOut.append("<div class=\"math-display-block\">").append(trimmed).append("</div>\n")
                    }
                    trimmed.startsWith("#### ") -> { flushParagraph(); closeLists(); htmlOut.append("<h4>").append(formatInlineMd(trimmed.removePrefix("#### ").trim())).append("</h4>\n") }
                    trimmed.startsWith("### ") -> { flushParagraph(); closeLists(); htmlOut.append("<h3>").append(formatInlineMd(trimmed.removePrefix("### ").trim())).append("</h3>\n") }
                    trimmed.startsWith("## ") -> { flushParagraph(); closeLists(); htmlOut.append("<h2>").append(formatInlineMd(trimmed.removePrefix("## ").trim())).append("</h2>\n") }
                    trimmed.startsWith("# ") -> { flushParagraph(); closeLists(); htmlOut.append("<h1>").append(formatInlineMd(trimmed.removePrefix("# ").trim())).append("</h1>\n") }
                    trimmed.startsWith("> ") -> { flushParagraph(); closeLists(); htmlOut.append("<blockquote>").append(formatInlineMd(trimmed.removePrefix("> ").trim())).append("</blockquote>\n") }
                    trimmed.matches(Regex("^[-*•]\\s+.*")) -> {
                        flushParagraph()
                        if (inOl) { htmlOut.append("</ol>\n"); inOl = false }
                        if (!inUl) { htmlOut.append("<ul>\n"); inUl = true }
                        htmlOut.append("<li>").append(formatInlineMd(trimmed.replaceFirst(Regex("^[-*•]\\s+"), ""))).append("</li>\n")
                    }
                    trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                        flushParagraph()
                        if (inUl) { htmlOut.append("</ul>\n"); inUl = false }
                        if (!inOl) { htmlOut.append("<ol>\n"); inOl = true }
                        htmlOut.append("<li>").append(formatInlineMd(trimmed.replaceFirst(Regex("^\\d+\\.\\s+"), ""))).append("</li>\n")
                    }
                    else -> { closeLists(); paragraphLines += trimmed }
                }
            }
            flushParagraph()
            closeLists()

            var finalHtml = htmlOut.toString()
            mathSlots.forEachIndexed { idx, (isDisplay, latex) ->
                val safeLatex = escapeHtml(latex)
                if (isDisplay) {
                    finalHtml = finalHtml.replace("@@COSMOS_DISP_${idx}@@", "\$\$\n$safeLatex\n\$\$")
                } else {
                    finalHtml = finalHtml.replace("@@COSMOS_INL_${idx}@@", "\$$safeLatex\$")
                }
            }
            return finalHtml
        }

        private fun findClosingDoubleDollar(text: String, startIndex: Int): Int {
            var idx = startIndex
            while (idx < text.length) {
                val found = text.indexOf("$$", idx)
                if (found == -1) return -1
                if (found == 0 || text[found - 1] != '\\') return found
                idx = found + 2
            }
            return -1
        }

        private fun findClosingSingleDollar(text: String, startIndex: Int): Int {
            var idx = startIndex
            while (idx < text.length) {
                val ch = text[idx]
                if (ch == '\n') return -1
                if (ch == '$' && (idx == 0 || text[idx - 1] != '\\')) {
                    return if (idx + 1 < text.length && text[idx + 1] == '$') -1 else idx
                }
                idx++
            }
            return -1
        }

        private fun escapeHtml(input: String): String =
            input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        private fun formatInlineMd(rawLine: String): String = escapeHtml(rawLine)
            .replace(Regex("`([^`]+)`")) { "<code>${it.groupValues[1]}</code>" }
            .replace(Regex("\\*\\*([^*]+)\\*\\*")) { "<strong>${it.groupValues[1]}</strong>" }
            .replace(Regex("\\*([^*\\n]+)\\*")) { "<em>${it.groupValues[1]}</em>" }

        fun formatReadableMathFallback(rawText: String): String {
            var out = rawText
                .replace(Regex("```(?:latex|tex|math)?\\s*")) { "" }
                .replace("```", "")
                .replace("$$", "")
                .replace("$", "")
                .replace("\\[", "")
                .replace("\\]", "")
                .replace("\\(", "")
                .replace("\\)", "")
                .replace("\\left", "")
                .replace("\\right", "")
                .replace("\\quad", "  ")
                .replace("\\,", " ")
                .replace(Regex("\\\\(?:text|mathrm|mathbf)\\{([^{}]+)\\}")) { it.groupValues[1] }
                .replace(Regex("\\\\vec\\{([^{}]+)\\}")) { "${it.groupValues[1]}⃗" }
                .replace(Regex("\\\\sqrt\\{([^{}]+)\\}")) { "√(${it.groupValues[1]})" }

            val fracRegex = Regex("\\\\(?:frac|tfrac|dfrac)\\{([^{}]+)\\}\\{([^{}]+)\\}")
            repeat(3) {
                out = fracRegex.replace(out) { "(${it.groupValues[1]}) / (${it.groupValues[2]})" }
            }

            return out
                .replace("\\rightarrow", "→")
                .replace("\\rightleftharpoons", "⇌")
                .replace("\\leftrightarrow", "↔")
                .replace("\\times", "×")
                .replace("\\cdot", "·")
                .replace("\\pm", "±")
                .replace("\\le", "≤")
                .replace("\\ge", "≥")
                .replace("\\neq", "≠")
                .replace("\\approx", "≈")
                .replace("\\infty", "∞")
                .replace("\\int", "∫")
                .replace("\\sum", "∑")
                .replace("\\pi", "π")
                .replace("\\theta", "θ")
                .replace("\\alpha", "α")
                .replace("\\beta", "β")
                .replace("\\delta", "Δ")
                .replace("\\Omega", "Ω")
                .replace("**", "")
                .replace(Regex("(?m)^\\s*#{1,4}\\s+"), "")
                .trim()
        }

        fun formatAsciiFormulaToLatex(rawFormula: String): String {
            var f = rawFormula.trim()
            if (f.startsWith("$$") && f.endsWith("$$")) return f.removeSurrounding("$$").trim()
            if (f.startsWith("$") && f.endsWith("$")) return f.removeSurrounding("$").trim()

            var trailingNote = ""
            val noteMatch = Regex("\\s*\\((mirror|lens|constant|at STP|per unit)\\)\\s*$", RegexOption.IGNORE_CASE).find(f)
            if (noteMatch != null) {
                trailingNote = " \\quad \\text{(${noteMatch.groupValues[1]})}"
                f = f.removeRange(noteMatch.range).trim()
            }

            f = f.replace("+-", "\\pm ")
                .replace(">=", "\\ge ")
                .replace("<=", "\\le ")
                .replace("!=", "\\neq ")
                .replace("->", "\\rightarrow ")
                .replace("<->", "\\rightleftharpoons ")
                .replace("(1/2)", "\\frac{1}{2}")
                .replace("(1)/(2)", "\\frac{1}{2}")
                .replace("(1) / (2)", "\\frac{1}{2}")
                .replace("*", " \\cdot ")
                .replace(Regex("sqrt\\(([^()]+)\\)")) { "\\sqrt{${it.groupValues[1]}}" }
                .replace(Regex("nth_root\\(([^()]+)\\)")) { "\\sqrt[n]{${it.groupValues[1]}}" }
                .replace("Delta_G0", "\\Delta G^\\circ")
                .replace("Delta_ng", "\\Delta n_g")
                .replace("Delta_", "\\Delta ")
                .replace("E0_cell", "E^\\circ_{\\text{cell}}")
                .replace("W_net", "W_{\\text{net}}")
                .replace("F_net", "F_{\\text{net}}")
                .replace("f_max", "f_{\\max}")
                .replace("H_max", "H_{\\max}")
                .replace("K_max", "K_{\\max}")
                .replace("f_res", "f_{\\text{res}}")
                .replace("mu_s", "\\mu_s")
                .replace("mu_k", "\\mu_k")
                .replace("phi_0", "\\phi_0")
                .replace("t_1/2", "t_{1/2}")
                .replace(Regex("\\b(rho|theta|phi|alpha|beta|gamma|delta|omega|lambda|sigma|epsilon|mu|pi)\\b")) {
                    "\\${it.groupValues[1]}"
                }
                .replace(Regex("\\b([a-zA-Z]{2,}_[a-zA-Z0-9_]{2,})\\b")) { m ->
                    "\\text{${m.groupValues[1].replace('_', ' ')}}"
                }
                .replace(Regex("\\((-?b\\s*\\\\pm\\s*\\\\sqrt\\{[^}]+\\})\\)\\s*/\\s*(2a)")) { m ->
                    "\\frac{${m.groupValues[1]}}{${m.groupValues[2]}}"
                }

            return (f + trailingNote).trim()
        }

        // --- Strategy Game Rules Parity ---

        fun getLudoDestinationPosition(pos: Int, startTrack: Int, dice: Int): Int? {
            if (pos == 57) return null
            if (pos == -1) return if (dice == 6) startTrack else null
            val rel = if (pos in 52..56) pos - 1 else (pos - startTrack + 52) % 52
            val nextRel = rel + dice
            return when {
                nextRel > 56 -> null
                nextRel == 56 -> 57
                nextRel in 51..55 -> nextRel + 1
                else -> (startTrack + nextRel) % 52
            }
        }

        fun chooseLudoBestToken(
            positions: IntArray,
            startTracks: IntArray,
            enemies: IntArray,
            dice: Int,
            safeZones: IntArray
        ): Int {
            val safeSet = safeZones.toHashSet()
            val enemySet = enemies.toHashSet()
            var bestIdx = 0
            var bestScore = Int.MIN_VALUE
            for (i in positions.indices) {
                val pos = positions[i]
                val start = startTracks[i]
                val dest = getLudoDestinationPosition(pos, start, dice) ?: continue
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

        private val PIECE_VALUES = intArrayOf(0, 100, 320, 330, 500, 900, 20000)

        private fun pieceSide(p: Int): Int = if (p > 0) 1 else if (p < 0) -1 else 0

        fun isChessKingInCheck(board: IntArray, side: Int): Boolean {
            val kingIdx = board.indexOfFirst { it == side * 6 }
            if (kingIdx < 0) return false
            val kr = kingIdx / 8
            val kc = kingIdx % 8
            val attacker = -side
            val pawnRow = kr + attacker
            if (pawnRow in 0..7) {
                if (kc - 1 >= 0 && board[pawnRow * 8 + (kc - 1)] == attacker) return true
                if (kc + 1 < 8 && board[pawnRow * 8 + (kc + 1)] == attacker) return true
            }
            val kDr = intArrayOf(-2, -2, -1, -1, 1, 1, 2, 2)
            val kDc = intArrayOf(-1, 1, -2, 2, -2, 2, -1, 1)
            for (i in 0..7) {
                val nr = kr + kDr[i]; val nc = kc + kDc[i]
                if (nr in 0..7 && nc in 0..7 && board[nr * 8 + nc] == attacker * 2) return true
            }
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = kr + dr; val nc = kc + dc
                if (nr in 0..7 && nc in 0..7 && board[nr * 8 + nc] == attacker * 6) return true
            }
            val dirs = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)
            for (d in dirs.indices) {
                var r = kr + dirs[d].first; var c = kc + dirs[d].second
                while (r in 0..7 && c in 0..7) {
                    val p = board[r * 8 + c]
                    if (p != 0) {
                        val t = abs(p)
                        val diag = d >= 4
                        if (pieceSide(p) == attacker && (t == 5 || (!diag && t == 4) || (diag && t == 3))) return true
                        break
                    }
                    r += dirs[d].first; c += dirs[d].second
                }
            }
            return false
        }

        fun getChessValidMoves(board: IntArray, side: Int, r: Int, c: Int): IntArray {
            if (r !in 0..7 || c !in 0..7) return IntArray(0)
            val p = board[r * 8 + c]
            if (pieceSide(p) != side) return IntArray(0)
            val type = abs(p)
            val valid = mutableListOf<Int>()
            val work = board.copyOf()

            fun tryPush(tr: Int, tc: Int) {
                val origFrom = work[r * 8 + c]
                val origTo = work[tr * 8 + tc]
                work[tr * 8 + tc] = if (type == 1 && (tr == 0 || tr == 7)) side * 5 else origFrom
                work[r * 8 + c] = 0
                if (!isChessKingInCheck(work, side)) valid += tr * 8 + tc
                work[r * 8 + c] = origFrom
                work[tr * 8 + tc] = origTo
            }

            when (type) {
                1 -> {
                    val dir = if (side == 1) -1 else 1
                    val startRow = if (side == 1) 6 else 1
                    if (r + dir in 0..7 && board[(r + dir) * 8 + c] == 0) {
                        tryPush(r + dir, c)
                        if (r == startRow && r + 2 * dir in 0..7 && board[(r + 2 * dir) * 8 + c] == 0) tryPush(r + 2 * dir, c)
                    }
                    for (dc in intArrayOf(-1, 1)) {
                        val nr = r + dir; val nc = c + dc
                        if (nr in 0..7 && nc in 0..7 && pieceSide(board[nr * 8 + nc]) == -side) tryPush(nr, nc)
                    }
                }
                2 -> {
                    val kDr = intArrayOf(-2, -2, -1, -1, 1, 1, 2, 2)
                    val kDc = intArrayOf(-1, 1, -2, 2, -2, 2, -1, 1)
                    for (i in 0..7) {
                        val nr = r + kDr[i]; val nc = c + kDc[i]
                        if (nr in 0..7 && nc in 0..7 && pieceSide(board[nr * 8 + nc]) != side) tryPush(nr, nc)
                    }
                }
                6 -> {
                    for (dr in -1..1) for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = r + dr; val nc = c + dc
                        if (nr in 0..7 && nc in 0..7 && pieceSide(board[nr * 8 + nc]) != side) tryPush(nr, nc)
                    }
                }
                else -> {
                    val dirs = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)
                    val startD = if (type == 3) 4 else 0
                    val endD = if (type == 4) 4 else 8
                    for (d in startD until endD) {
                        var nr = r + dirs[d].first; var nc = c + dirs[d].second
                        while (nr in 0..7 && nc in 0..7) {
                            val t = board[nr * 8 + nc]
                            if (t == 0) tryPush(nr, nc)
                            else {
                                if (pieceSide(t) == -side) tryPush(nr, nc)
                                break
                            }
                            nr += dirs[d].first; nc += dirs[d].second
                        }
                    }
                }
            }
            return valid.toIntArray()
        }

        fun hasAnyChessLegalMoves(board: IntArray, side: Int): Boolean {
            for (r in 0..7) for (c in 0..7) {
                if (pieceSide(board[r * 8 + c]) == side && getChessValidMoves(board, side, r, c).isNotEmpty()) {
                    return true
                }
            }
            return false
        }

        fun computeChessBotMove(board: IntArray, turnSide: Int, difficulty: Int): Int? {
            val moves = mutableListOf<IntArray>()
            for (r in 0..7) for (c in 0..7) {
                if (pieceSide(board[r * 8 + c]) == turnSide) {
                    for (dest in getChessValidMoves(board, turnSide, r, c)) {
                        moves += intArrayOf(r, c, dest / 8, dest % 8)
                    }
                }
            }
            if (moves.isEmpty()) return null
            if (difficulty == 0) {
                val captures = moves.filter { board[it[2] * 8 + it[3]] != 0 }
                val m = if (captures.isNotEmpty() && Random.nextFloat() > 0.4f) captures.random() else moves.random()
                return ((m[0] and 0xFF) shl 24) or ((m[1] and 0xFF) shl 16) or ((m[2] and 0xFF) shl 8) or (m[3] and 0xFF)
            }
            var bestScore = Int.MIN_VALUE
            var best = moves.first()
            val work = board.copyOf()
            for (m in moves) {
                val origFrom = work[m[0] * 8 + m[1]]
                val origTo = work[m[2] * 8 + m[3]]
                work[m[2] * 8 + m[3]] = if (abs(origFrom) == 1 && (m[2] == 0 || m[2] == 7)) turnSide * 5 else origFrom
                work[m[0] * 8 + m[1]] = 0
                var score = 0
                for (i in 0..63) {
                    val p = work[i]
                    if (p != 0) score += if (pieceSide(p) == turnSide) PIECE_VALUES[abs(p)] else -PIECE_VALUES[abs(p)]
                }
                work[m[0] * 8 + m[1]] = origFrom
                work[m[2] * 8 + m[3]] = origTo
                if (score > bestScore) { bestScore = score; best = m }
            }
            return ((best[0] and 0xFF) shl 24) or ((best[1] and 0xFF) shl 16) or ((best[2] and 0xFF) shl 8) or (best[3] and 0xFF)
        }

        fun synthesizeGameMusicStep(
            gameTypeOrdinal: Int,
            noteIndex: Int,
            sampleRate: Int,
            globalSampleStart: Long
        ): ShortArray {
            val ludo = doubleArrayOf(523.25, 659.25, 783.99, 880.00, 783.99, 659.25, 587.33, 523.25, 698.46, 880.00, 1046.50, 783.99)
            val chaturanga = doubleArrayOf(261.63, 277.18, 329.63, 349.23, 392.00, 415.30, 392.00, 349.23, 329.63, 277.18, 261.63, 196.00)
            val shatranj = doubleArrayOf(293.66, 349.23, 440.00, 523.25, 493.88, 440.00, 392.00, 329.63)
            val (pattern, drone, dur, joyful) = when (gameTypeOrdinal) {
                1 -> Quadruple(ludo, 130.81, 0.34, true)
                2 -> Quadruple(chaturanga, 130.81, 0.68, false)
                else -> Quadruple(shatranj, 146.83, 0.72, false)
            }
            val melody = pattern[((noteIndex % pattern.size) + pattern.size) % pattern.size]
            val harmonic = if (joyful) melody * 2.0 else drone * 1.5
            val count = (sampleRate * dur).toInt().coerceAtLeast(1)
            val out = ShortArray(count)
            val decay = if (joyful) 5.5 else 2.2
            val hWeight = if (joyful) 0.18 else 0.12
            val dEnv = if (joyful) 0.25 else 0.42
            for (i in 0 until count) {
                val tl = i.toDouble() / sampleRate
                val tg = (globalSampleStart + i).toDouble() / sampleRate
                val env = (tl / 0.025).coerceIn(0.0, 1.0) * exp(-decay * tl)
                val mWave = sin(2.0 * PI * melody * tl) * 0.65 + sin(2.0 * PI * harmonic * tl) * hWeight
                val dWave = sin(2.0 * PI * drone * tg) * dEnv + sin(2.0 * PI * (drone * 1.5) * tg) * (dEnv * 0.45)
                out[i] = (((mWave * env + dWave) * 0.14).coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
            }
            return out
        }

        private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    }
}
