package com.example.rag

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.utils.CosmiumVoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceQuizStep {
    INACTIVE,
    ASKING_QUESTION,
    LISTENING,
    EVALUATING,
    FEEDBACK_SPEAKING,
    ROUND_SUMMARY
}

data class VoiceQuizQuestion(
    val id: String,
    val subjectName: String,
    val chapterNumber: Int,
    val topicTitle: String,
    val questionPrompt: String,
    val expectedKeywords: List<String>,
    val canonicalAnswer: String,
    val formulaLatex: String,
    val explanation: String
)

data class VoiceQuizUiState(
    val step: VoiceQuizStep = VoiceQuizStep.INACTIVE,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 5,
    val currentQuestion: VoiceQuizQuestion? = null,
    val transcribedAnswer: String = "",
    val feedbackMessage: String = "",
    val isAnswerCorrect: Boolean? = null,
    val score: Int = 0,
    val micRms: Float = 0f,
    val isMicAvailable: Boolean = true,
    val statusText: String = ""
)

/**
 * Interactive voice-based quiz engine for Cosmos:
 * - Generates tailored conceptual challenge questions from the active textbook syllabus packs.
 * - Reads questions verbally using [CosmiumVoiceManager].
 * - Listens for student spoken answers via Android's [SpeechRecognizer] with real-time waveform RMS.
 * - Evaluates answers and delivers verbal textbook feedback.
 */
class CosmosVoiceQuizManager(
    private val context: Context,
    private val voiceManager: CosmiumVoiceManager,
    private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "CosmosVoiceQuizManager"
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiState = MutableStateFlow(VoiceQuizUiState())
    val uiState: StateFlow<VoiceQuizUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var activeQuestions: List<VoiceQuizQuestion> = emptyList()
    private var isListeningActive = false
    private var listenTimeoutJob: Job? = null

    init {
        checkSpeechRecognizerSupport()
    }

    private fun checkSpeechRecognizerSupport() {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        _uiState.value = _uiState.value.copy(isMicAvailable = available)
    }

    /**
     * Starts a voice-based quiz session for the specified standard and stream.
     */
    fun startQuizSession(
        standardId: String,
        streamId: String,
        numQuestions: Int = 5
    ) {
        val chunks = SyllabusPackCatalog.getChunksForScope(standardId, streamId)
        val generated = generateQuestionsFromChunks(chunks, numQuestions)
        if (generated.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                step = VoiceQuizStep.INACTIVE,
                statusText = "No textbook chunks found for this standard."
            )
            return
        }

        activeQuestions = generated
        _uiState.value = VoiceQuizUiState(
            step = VoiceQuizStep.ASKING_QUESTION,
            currentQuestionIndex = 0,
            totalQuestions = generated.size,
            currentQuestion = generated[0],
            transcribedAnswer = "",
            feedbackMessage = "",
            isAnswerCorrect = null,
            score = 0,
            statusText = "Cosmium is asking question 1 of ${generated.size}..."
        )

        askCurrentQuestion()
    }

    private fun askCurrentQuestion() {
        val state = _uiState.value
        val q = state.currentQuestion ?: return

        _uiState.value = state.copy(
            step = VoiceQuizStep.ASKING_QUESTION,
            transcribedAnswer = "",
            feedbackMessage = "",
            isAnswerCorrect = null,
            statusText = "Cosmium is asking question ${state.currentQuestionIndex + 1}..."
        )

        val spokenText = "${q.subjectName}, Chapter ${q.chapterNumber}: ${q.topicTitle}. Here is your question: ${q.questionPrompt}"
        voiceManager.speak(spokenText)

        // Wait for voice to finish speaking, then start listening
        scope.launch {
            delay(1500L)
            while (voiceManager.isSpeaking.value) {
                delay(300L)
            }
            delay(400L)
            if (_uiState.value.step == VoiceQuizStep.ASKING_QUESTION) {
                startListeningForAnswer()
            }
        }
    }

    /**
     * Activates speech recognition to capture the student's spoken response.
     */
    fun startListeningForAnswer() {
        if (_uiState.value.step == VoiceQuizStep.INACTIVE) return

        val hasMicPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPerm) {
            _uiState.value = _uiState.value.copy(
                step = VoiceQuizStep.LISTENING,
                isMicAvailable = false,
                statusText = "Microphone access not granted. Type your answer below!"
            )
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.value = _uiState.value.copy(
                step = VoiceQuizStep.LISTENING,
                isMicAvailable = false,
                statusText = "Speech recognition unavailable on this device. Type your answer below!"
            )
            return
        }

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
                isListeningActive = true
                _uiState.value = _uiState.value.copy(
                    step = VoiceQuizStep.LISTENING,
                    statusText = "Listening for your answer... Speak now!"
                )

                // 12s safety timeout in case student stays silent
                listenTimeoutJob?.cancel()
                listenTimeoutJob = scope.launch {
                    delay(12_000L)
                    if (isListeningActive && _uiState.value.step == VoiceQuizStep.LISTENING) {
                        stopListening()
                        if (_uiState.value.transcribedAnswer.isNotBlank()) {
                            evaluateAnswer(_uiState.value.transcribedAnswer)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                statusText = "No speech detected. Tap 'Retry Mic' or type your answer below."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    step = VoiceQuizStep.LISTENING,
                    statusText = "Microphone listening paused. You can speak again or submit below."
                )
            }
        }
    }

    fun stopListening() {
        listenTimeoutJob?.cancel()
        if (isListeningActive) {
            isListeningActive = false
            mainHandler.post {
                runCatching { speechRecognizer?.stopListening() }
            }
        }
    }

    /**
     * Allows student to submit text directly as a fallback if mic is silent or unavailable.
     */
    fun submitAnswer(answerText: String) {
        stopListening()
        evaluateAnswer(answerText)
    }

    private fun evaluateAnswer(rawAnswer: String) {
        val cleanAnswer = rawAnswer.trim()
        val q = _uiState.value.currentQuestion ?: return

        _uiState.value = _uiState.value.copy(
            step = VoiceQuizStep.EVALUATING,
            transcribedAnswer = cleanAnswer,
            statusText = "Evaluating your response..."
        )

        val isCorrect = com.example.utils.NativeCoreBridge.evaluateQuizAnswer(
            studentAnswer = cleanAnswer,
            canonicalAnswer = q.canonicalAnswer,
            expectedKeywords = q.expectedKeywords
        )
        val newScore = if (isCorrect) _uiState.value.score + 1 else _uiState.value.score

        val feedback = when {
            isCorrect -> "Correct! Well done! ${q.explanation}"
            cleanAnswer.isBlank() -> "Time's up! The textbook concept is: ${q.canonicalAnswer}. ${q.explanation}"
            else -> "Good attempt! The textbook concept is: ${q.canonicalAnswer}. ${q.explanation}"
        }

        _uiState.value = _uiState.value.copy(
            step = VoiceQuizStep.FEEDBACK_SPEAKING,
            isAnswerCorrect = isCorrect,
            score = newScore,
            feedbackMessage = feedback,
            statusText = if (isCorrect) "Spot on! (+1 mark)" else "Reviewing key concept"
        )

        voiceManager.speak(feedback)

        // Wait for explanation voice to complete, then advance to next question
        scope.launch {
            delay(1500L)
            while (voiceManager.isSpeaking.value) {
                delay(300L)
            }
            delay(800L)
            if (_uiState.value.step == VoiceQuizStep.FEEDBACK_SPEAKING) {
                advanceToNextQuestion()
            }
        }
    }

    fun nextQuestionNow() {
        voiceManager.stop()
        stopListening()
        advanceToNextQuestion()
    }

    private fun advanceToNextQuestion() {
        val nextIdx = _uiState.value.currentQuestionIndex + 1
        if (nextIdx < activeQuestions.size) {
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = nextIdx,
                currentQuestion = activeQuestions[nextIdx],
                transcribedAnswer = "",
                feedbackMessage = "",
                isAnswerCorrect = null,
                statusText = "Question ${nextIdx + 1} of ${activeQuestions.size}"
            )
            askCurrentQuestion()
        } else {
            // Quiz completed
            val total = activeQuestions.size
            val finalScore = _uiState.value.score
            val completionMsg = "Quiz complete! You scored $finalScore out of $total. Excellent work in the Cosmos study sanctuary!"
            _uiState.value = _uiState.value.copy(
                step = VoiceQuizStep.ROUND_SUMMARY,
                statusText = completionMsg
            )
            voiceManager.speak(completionMsg)
        }
    }

    fun stopQuiz() {
        listenTimeoutJob?.cancel()
        stopListening()
        voiceManager.stop()
        _uiState.value = VoiceQuizUiState(step = VoiceQuizStep.INACTIVE)
    }

    fun release() {
        stopQuiz()
        mainHandler.post {
            runCatching {
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.value = _uiState.value.copy(
                    step = VoiceQuizStep.LISTENING,
                    statusText = "Listening... Speak your answer now!"
                )
            }

            override fun onBeginningOfSpeech() {
                _uiState.value = _uiState.value.copy(statusText = "Hearing your voice...")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _uiState.value = _uiState.value.copy(micRms = normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListeningActive = false
                _uiState.value = _uiState.value.copy(
                    micRms = 0f,
                    statusText = "Processing spoken answer..."
                )
            }

            override fun onError(error: Int) {
                isListeningActive = false
                _uiState.value = _uiState.value.copy(micRms = 0f)
                if (_uiState.value.transcribedAnswer.isNotBlank()) {
                    evaluateAnswer(_uiState.value.transcribedAnswer)
                } else if (_uiState.value.step == VoiceQuizStep.LISTENING) {
                    _uiState.value = _uiState.value.copy(
                        statusText = "Didn't catch that. Tap 'Retry Mic' or type your answer below."
                    )
                }
            }

            override fun onResults(results: Bundle?) {
                isListeningActive = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topMatch = matches?.firstOrNull() ?: ""
                _uiState.value = _uiState.value.copy(
                    transcribedAnswer = topMatch,
                    micRms = 0f
                )
                if (topMatch.isNotBlank()) {
                    evaluateAnswer(topMatch)
                } else {
                    _uiState.value = _uiState.value.copy(
                        statusText = "No words recognized. Tap 'Retry Mic' to speak again."
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(transcribedAnswer = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun generateQuestionsFromChunks(
        chunks: List<TextbookChunk>,
        maxCount: Int
    ): List<VoiceQuizQuestion> {
        val questions = mutableListOf<VoiceQuizQuestion>()

        for ((idx, chunk) in chunks.take(maxCount * 2).withIndex()) {
            val q = buildQuestionFromChunk(chunk, "vq_$idx")
            if (q != null) {
                questions.add(q)
                if (questions.size >= maxCount) break
            }
        }

        // Second pass: generate formula / application follow-ups from the same chunks if needed
        if (questions.size < maxCount) {
            for ((idx, chunk) in chunks.withIndex()) {
                val followUp = buildFollowUpQuestionFromChunk(chunk, "vq_followup_$idx")
                if (followUp != null) {
                    questions.add(followUp)
                    if (questions.size >= maxCount) break
                }
            }
        }

        // Third pass: curated NCERT foundation bank if still below maxCount
        if (questions.size < maxCount) {
            val bank = listOf(
                VoiceQuizQuestion(
                    id = "vq_fallback_1",
                    subjectName = "Physics",
                    chapterNumber = 1,
                    topicTitle = "Units and Measurement",
                    questionPrompt = "What is the SI unit of electric current?",
                    expectedKeywords = listOf("ampere", "amp"),
                    canonicalAnswer = "Ampere (A)",
                    formulaLatex = "1\\text{ A} = 1\\text{ C/s}",
                    explanation = "Electric current is a fundamental SI quantity measured in Amperes."
                ),
                VoiceQuizQuestion(
                    id = "vq_fallback_2",
                    subjectName = "Accountancy",
                    chapterNumber = 3,
                    topicTitle = "Golden Rules of Accounting",
                    questionPrompt = "According to the Golden Rule for a Real Account, what should be debited?",
                    expectedKeywords = listOf("what comes in", "comes in"),
                    canonicalAnswer = "Debit what comes in, Credit what goes out",
                    formulaLatex = "\\text{Real A/c: Debit what comes in}",
                    explanation = "Under double-entry bookkeeping, tangible assets coming into the business are debited."
                ),
                VoiceQuizQuestion(
                    id = "vq_fallback_3",
                    subjectName = "Economics",
                    chapterNumber = 2,
                    topicTitle = "Consumer Equilibrium",
                    questionPrompt = "According to the Law of Diminishing Marginal Utility, what happens to additional satisfaction as consumption increases?",
                    expectedKeywords = listOf("declines", "decreases", "falls", "diminishes"),
                    canonicalAnswer = "Marginal utility declines as consumption increases",
                    formulaLatex = "MU_n = TU_n - TU_{n-1}",
                    explanation = "Each successive unit consumed yields less additional utility than the previous unit."
                )
            )
            for (item in bank) {
                if (questions.size >= maxCount) break
                if (questions.none { it.topicTitle == item.topicTitle }) {
                    questions.add(item)
                }
            }
        }

        return questions.shuffled().take(maxCount)
    }

    private fun buildFollowUpQuestionFromChunk(chunk: TextbookChunk, id: String): VoiceQuizQuestion? {
        val tags = chunk.formulaTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val secondaryFormula = tags.getOrNull(1) ?: tags.firstOrNull()
        return when {
            secondaryFormula != null -> {
                val keywords = secondaryFormula
                    .split("=", "+", "-", "*", "/", "(", ")")
                    .map { it.trim().lowercase() }
                    .filter { it.length >= 2 }
                    .distinct()
                    .take(4)
                    .ifEmpty { listOf(secondaryFormula.lowercase()) }
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = chunk.topicTitle,
                    questionPrompt = "From ${chunk.subjectName} Chapter ${chunk.chapterNumber}, state the key formula or rule from ${chunk.topicTitle}: ${chunk.workedExample.take(90)}",
                    expectedKeywords = keywords,
                    canonicalAnswer = secondaryFormula,
                    formulaLatex = secondaryFormula,
                    explanation = chunk.contentText.take(160)
                )
            }
            chunk.workedExample.isNotBlank() -> {
                val words = chunk.topicTitle.split(" ").filter { it.length > 3 }.take(3)
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = chunk.topicTitle,
                    questionPrompt = "Apply the core rule of ${chunk.topicTitle} in ${chunk.subjectName}: ${chunk.workedExample.take(100)}",
                    expectedKeywords = words.ifEmpty { listOf(chunk.subjectName) },
                    canonicalAnswer = chunk.workedExample.take(100),
                    formulaLatex = "",
                    explanation = chunk.contentText.take(160)
                )
            }
            else -> null
        }
    }

    private fun buildQuestionFromChunk(chunk: TextbookChunk, id: String): VoiceQuizQuestion? {
        val topic = chunk.topicTitle
        val content = chunk.contentText
        val formula = chunk.formulaTags

        return when {
            topic.contains("Motion", ignoreCase = true) || topic.contains("Kinematics", ignoreCase = true) -> {
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "Under uniform acceleration, what formula connects final velocity, initial velocity, acceleration, and time?",
                    expectedKeywords = listOf("u + at", "v = u", "plus at", "u+at"),
                    canonicalAnswer = "v = u + at",
                    formulaLatex = "v = u + at",
                    explanation = "The first equation of motion relates velocity, acceleration, and time under constant acceleration."
                )
            }
            topic.contains("Stoichiometry", ignoreCase = true) || topic.contains("Chemical", ignoreCase = true) -> {
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "In a balanced chemical reaction, what fundamental law states that matter cannot be created or destroyed?",
                    expectedKeywords = listOf("conservation of mass", "conservation", "mass"),
                    canonicalAnswer = "Law of Conservation of Mass",
                    formulaLatex = "\\sum m_{\\text{reactants}} = \\sum m_{\\text{products}}",
                    explanation = "Lavoisier's Law of Conservation of Mass governs all chemical balancing."
                )
            }
            topic.contains("Derivative", ignoreCase = true) || topic.contains("Calculus", ignoreCase = true) -> {
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "What is the derivative of the sine of x with respect to x?",
                    expectedKeywords = listOf("cos", "cosine"),
                    canonicalAnswer = "cos(x)",
                    formulaLatex = "\\frac{d}{dx}(\\sin x) = \\cos x",
                    explanation = "The rate of change of the sine function is the cosine function."
                )
            }
            topic.contains("Accounting", ignoreCase = true) || topic.contains("Ledger", ignoreCase = true) -> {
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "What fundamental accounting equation connects Assets, Liabilities, and Capital?",
                    expectedKeywords = listOf("liabilities + capital", "liabilities plus capital", "assets equal", "liabilities"),
                    canonicalAnswer = "Assets = Liabilities + Capital",
                    formulaLatex = "\\text{Assets} = \\text{Liabilities} + \\text{Capital}",
                    explanation = "Every economic resource in a firm is financed either by creditor claims (Liabilities) or owner claims (Capital)."
                )
            }
            topic.contains("Demand", ignoreCase = true) || topic.contains("Economics", ignoreCase = true) -> {
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "According to the Law of Demand, what happens to the quantity demanded when price increases, other things remaining equal?",
                    expectedKeywords = listOf("decrease", "falls", "drops", "decreases", "declines"),
                    canonicalAnswer = "Quantity demanded decreases",
                    formulaLatex = "\\frac{\\Delta Q_d}{\\Delta P} < 0",
                    explanation = "The Law of Demand states an inverse relationship between price and quantity demanded."
                )
            }
            formula.isNotBlank() -> {
                val cleanFormula = formula.split(",").firstOrNull()?.trim() ?: formula
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "In $topic, state the primary formula or relationship associated with this concept.",
                    expectedKeywords = cleanFormula.split("=").map { it.trim().take(12) }.filter { it.isNotBlank() },
                    canonicalAnswer = cleanFormula,
                    formulaLatex = cleanFormula,
                    explanation = content.take(150)
                )
            }
            else -> {
                VoiceQuizQuestion(
                    id = id,
                    subjectName = chunk.subjectName,
                    chapterNumber = chunk.chapterNumber,
                    topicTitle = topic,
                    questionPrompt = "In ${chunk.subjectName}, Chapter ${chunk.chapterNumber}: Explain the primary principle behind $topic.",
                    expectedKeywords = topic.split(" ").filter { it.length > 3 },
                    canonicalAnswer = topic,
                    formulaLatex = "",
                    explanation = content.take(160)
                )
            }
        }
    }
}
