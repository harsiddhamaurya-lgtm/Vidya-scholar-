package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.data.AnsweredQuestionRecord
import com.example.data.QuestLevel
import com.example.data.QuestNodeState
import com.example.data.RealmId
import com.example.data.SubjectRealm
import com.example.data.VidyaQuizQuestion
import com.example.data.VidyaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class VidyaVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("vidya_vault_state_v1", Context.MODE_PRIVATE)

    private val _completedLevelIds = MutableStateFlow<Set<String>>(emptySet())
    val completedLevelIds: StateFlow<Set<String>> = _completedLevelIds.asStateFlow()

    private val _savedNoteLevelIds = MutableStateFlow<Set<String>>(emptySet())
    val savedNoteLevelIds: StateFlow<Set<String>> = _savedNoteLevelIds.asStateFlow()

    private val _answeredLogs = MutableStateFlow<List<AnsweredQuestionRecord>>(emptyList())
    val answeredLogs: StateFlow<List<AnsweredQuestionRecord>> = _answeredLogs.asStateFlow()

    init {
        loadOrSeedState()
    }

    private fun loadOrSeedState() {
        val hasSaved = prefs.getBoolean("initialized", false)
        if (!hasSaved) {
            val initialCompleted = setOf(
                "math_l1", "math_l2",
                "phys_l1",
                "ai_l1", "ai_l2", "ai_l3", "ai_l4",
                "eng_l1"
            )
            val initialNotes = setOf(
                "math_l1", "math_l2",
                "phys_l1",
                "ai_l1", "ai_l2", "ai_l3", "ai_l4",
                "eng_l1"
            )
            val now = System.currentTimeMillis()
            val initialLogs = listOf(
                AnsweredQuestionRecord(
                    id = "seed_m1",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 1,
                    levelTitle = "Limits & Continuity",
                    questionSource = "Quick Test",
                    questionPrompt = "Evaluate lim(x->3) (x^2 - 9) / (x - 3).",
                    selectedAnswer = "6",
                    correctAnswer = "6",
                    isCorrect = true,
                    explanation = "Factor numerator as (x-3)(x+3); cancel (x-3) and substitute x=3 to get 6.",
                    timestampMillis = now - 86_400_000L
                ),
                AnsweredQuestionRecord(
                    id = "seed_m2",
                    realmId = RealmId.MATHEMATICS,
                    levelNumber = 2,
                    levelTitle = "Derivatives & Power Rule",
                    questionSource = "Video Checkpoint",
                    questionPrompt = "Geometrically, what does f'(x) represent at a point?",
                    selectedAnswer = "Slope of the tangent line",
                    correctAnswer = "Slope of the tangent line",
                    isCorrect = true,
                    explanation = "The derivative at x equals the instantaneous slope of the tangent touching the curve.",
                    timestampMillis = now - 43_200_000L
                ),
                AnsweredQuestionRecord(
                    id = "seed_p1",
                    realmId = RealmId.PHYSICS,
                    levelNumber = 1,
                    titleOrFallback = "Newton's Laws of Motion",
                    questionSource = "Quick Test",
                    questionPrompt = "A 10 kg mass accelerates at 2 m/s^2. Net force?",
                    selectedAnswer = "20 N",
                    correctAnswer = "20 N",
                    isCorrect = true,
                    explanation = "F = m*a = 10 * 2 = 20 N.",
                    timestampMillis = now - 64_800_000L
                ),
                AnsweredQuestionRecord(
                    id = "seed_a4",
                    realmId = RealmId.CODING_AI,
                    levelNumber = 4,
                    levelTitle = "Transformers & Prompt Engineering",
                    questionSource = "Quick Test",
                    questionPrompt = "RAG improves accuracy by:",
                    selectedAnswer = "Retrieving grounded external docs into context",
                    correctAnswer = "Retrieving grounded external docs into context",
                    isCorrect = true,
                    explanation = "Retrieval-Augmented Generation grounds answers in verified facts.",
                    timestampMillis = now - 21_600_000L
                )
            )
            _completedLevelIds.value = initialCompleted
            _savedNoteLevelIds.value = initialNotes
            _answeredLogs.value = initialLogs
            persistState()
            prefs.edit().putBoolean("initialized", true).apply()
        } else {
            val compSet = prefs.getStringSet("completed_levels", emptySet())?.toSet() ?: emptySet()
            val noteSet = prefs.getStringSet("saved_notes", emptySet())?.toSet() ?: emptySet()
            val logsJson = prefs.getString("answered_logs", "[]") ?: "[]"
            _completedLevelIds.value = compSet
            _savedNoteLevelIds.value = noteSet
            _answeredLogs.value = parseLogs(logsJson)
        }
    }

    private fun AnsweredQuestionRecord(
        id: String,
        realmId: RealmId,
        levelNumber: Int,
        titleOrFallback: String,
        questionSource: String,
        questionPrompt: String,
        selectedAnswer: String,
        correctAnswer: String,
        isCorrect: Boolean,
        explanation: String,
        timestampMillis: Long
    ) = AnsweredQuestionRecord(
        id = id,
        realmId = realmId,
        levelNumber = levelNumber,
        levelTitle = titleOrFallback,
        questionSource = questionSource,
        questionPrompt = questionPrompt,
        selectedAnswer = selectedAnswer,
        correctAnswer = correctAnswer,
        isCorrect = isCorrect,
        explanation = explanation,
        timestampMillis = timestampMillis
    )

    fun getNodeState(realm: SubjectRealm, level: QuestLevel, completed: Set<String> = _completedLevelIds.value): QuestNodeState {
        if (completed.contains(level.id)) return QuestNodeState.COMPLETED
        val prevLevel = realm.levels.firstOrNull { it.levelNumber == level.levelNumber - 1 }
        return if (level.levelNumber == 1 || (prevLevel != null && completed.contains(prevLevel.id))) {
            QuestNodeState.ACTIVE
        } else {
            QuestNodeState.LOCKED
        }
    }

    fun getRealmProgress(realm: SubjectRealm, completed: Set<String> = _completedLevelIds.value): Pair<Int, Float> {
        val doneCount = realm.levels.count { completed.contains(it.id) }
        val total = realm.levels.size.coerceAtLeast(1)
        val activeLevelNum = realm.levels
            .firstOrNull { getNodeState(realm, it, completed) == QuestNodeState.ACTIVE }
            ?.levelNumber ?: total
        return activeLevelNum to (doneCount.toFloat() / total.toFloat())
    }

    fun markLevelCompleted(level: QuestLevel) {
        val updatedCompleted = _completedLevelIds.value + level.id
        val updatedSaved = _savedNoteLevelIds.value + level.id
        _completedLevelIds.value = updatedCompleted
        _savedNoteLevelIds.value = updatedSaved
        persistState()
    }

    fun toggleNoteSaved(levelId: String) {
        val current = _savedNoteLevelIds.value
        _savedNoteLevelIds.value = if (current.contains(levelId)) current - levelId else current + levelId
        persistState()
    }

    fun logAnsweredQuestion(
        level: QuestLevel,
        question: VidyaQuizQuestion,
        selectedIndex: Int,
        sourceOverride: String? = null
    ) {
        val selectedText = question.options.getOrElse(selectedIndex) { "Skipped" }
        val correctText = question.options.getOrElse(question.correctIndex) { "" }
        val record = AnsweredQuestionRecord(
            id = "${level.id}_${question.id}_${System.currentTimeMillis()}",
            realmId = level.realmId,
            levelNumber = level.levelNumber,
            levelTitle = level.title,
            questionSource = sourceOverride ?: question.sourceLabel,
            questionPrompt = question.prompt,
            selectedAnswer = selectedText,
            correctAnswer = correctText,
            isCorrect = selectedIndex == question.correctIndex,
            explanation = question.explanation,
            timestampMillis = System.currentTimeMillis()
        )
        _answeredLogs.value = listOf(record) + _answeredLogs.value.take(149)
        persistState()
    }

    fun getSavedScrollsForRealm(realmId: RealmId): List<QuestLevel> {
        val realm = VidyaRepository.getRealm(realmId)
        val savedIds = _savedNoteLevelIds.value
        val completed = _completedLevelIds.value
        return realm.levels.filter { savedIds.contains(it.id) || completed.contains(it.id) }
    }

    fun getLogsForRealm(realmId: RealmId): List<AnsweredQuestionRecord> {
        return _answeredLogs.value.filter { it.realmId == realmId }
    }

    private fun persistState() {
        val arr = JSONArray()
        _answeredLogs.value.forEach { rec ->
            val obj = JSONObject().apply {
                put("id", rec.id)
                put("realmId", rec.realmId.name)
                put("levelNumber", rec.levelNumber)
                put("levelTitle", rec.levelTitle)
                put("questionSource", rec.questionSource)
                put("questionPrompt", rec.questionPrompt)
                put("selectedAnswer", rec.selectedAnswer)
                put("correctAnswer", rec.correctAnswer)
                put("isCorrect", rec.isCorrect)
                put("explanation", rec.explanation)
                put("timestampMillis", rec.timestampMillis)
            }
            arr.put(obj)
        }
        prefs.edit()
            .putStringSet("completed_levels", _completedLevelIds.value)
            .putStringSet("saved_notes", _savedNoteLevelIds.value)
            .putString("answered_logs", arr.toString())
            .apply()
    }

    private fun parseLogs(raw: String): List<AnsweredQuestionRecord> {
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val realm = runCatching { RealmId.valueOf(obj.getString("realmId")) }
                        .getOrDefault(RealmId.MATHEMATICS)
                    add(
                        AnsweredQuestionRecord(
                            id = obj.optString("id"),
                            realmId = realm,
                            levelNumber = obj.optInt("levelNumber", 1),
                            levelTitle = obj.optString("levelTitle", "Quest"),
                            questionSource = obj.optString("questionSource", "Quick Test"),
                            questionPrompt = obj.optString("questionPrompt", ""),
                            selectedAnswer = obj.optString("selectedAnswer", ""),
                            correctAnswer = obj.optString("correctAnswer", ""),
                            isCorrect = obj.optBoolean("isCorrect", false),
                            explanation = obj.optString("explanation", ""),
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
