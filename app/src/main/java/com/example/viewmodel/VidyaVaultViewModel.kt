package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AnsweredQuestionRecord
import com.example.data.QuestLevel
import com.example.data.QuestNodeState
import com.example.data.RealmId
import com.example.data.SubjectRealm
import com.example.data.VidyaQuizQuestion
import com.example.data.VidyaRepository
import com.example.data.repository.ScholarIdentityRepository
import com.example.security.KeystoreCryptoManager
import com.example.security.SecurePrefsFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.LevelPlaybackProgress
import com.example.data.StandardGrade
import com.example.data.repository.CurriculumRepository

class VidyaVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val prefs by lazy { SecurePrefsFactory.getEncryptedPrefs(appContext, ENCRYPTED_PREFS_NAME) }
    val curriculumRepository by lazy { CurriculumRepository(appContext) }

    private val _completedLevelIds = MutableStateFlow<Set<String>>(DEFAULT_COMPLETED_LEVELS)
    val completedLevelIds: StateFlow<Set<String>> = _completedLevelIds.asStateFlow()

    private val _savedNoteLevelIds = MutableStateFlow<Set<String>>(DEFAULT_COMPLETED_LEVELS)
    val savedNoteLevelIds: StateFlow<Set<String>> = _savedNoteLevelIds.asStateFlow()

    private val _answeredLogs = MutableStateFlow<List<AnsweredQuestionRecord>>(emptyList())
    val answeredLogs: StateFlow<List<AnsweredQuestionRecord>> = _answeredLogs.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun getRealmsForStudent(standardId: String, streamId: String, batchType: String): List<SubjectRealm> {
        return curriculumRepository.getSubjectsForStudent(standardId, streamId, batchType)
    }

    fun getRealm(
        realmId: RealmId,
        standardId: String = "class_11",
        streamId: String = "science_pcm",
        batchType: String = "live"
    ): SubjectRealm {
        val studentRealms = getRealmsForStudent(standardId, streamId, batchType)
        return studentRealms.firstOrNull { it.realmId == realmId }
            ?: VidyaRepository.realms.firstOrNull { it.realmId == realmId }
            ?: studentRealms.first()
    }

    fun isStandardCompleted(standardId: String, streamId: String, batchType: String): Boolean {
        return curriculumRepository.isStandardCompleted(standardId, streamId, batchType, _completedLevelIds.value)
    }

    fun getPlaybackProgress(vidyaId: String, levelId: String): LevelPlaybackProgress? {
        return curriculumRepository.getPlaybackProgress(vidyaId, levelId)
    }

    fun savePlaybackProgress(vidyaId: String, levelId: String, positionSec: Int, totalDurationSec: Int, completed: Boolean) {
        curriculumRepository.savePlaybackProgress(vidyaId, levelId, positionSec, totalDurationSec, completed, viewModelScope)
    }

    fun getNextStandard(standardId: String): StandardGrade? {
        return curriculumRepository.getNextStandard(standardId)
    }

    init {
        loadOrSeedStateAsync()
        registerConnectivitySyncCallback()
    }

    private fun loadOrSeedStateAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            migrateLegacyPrefsIfNeeded()

            val hasSaved = prefs.getBoolean(KEY_INITIALIZED, false)
            if (!hasSaved) {
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
                _completedLevelIds.value = DEFAULT_COMPLETED_LEVELS
                _savedNoteLevelIds.value = DEFAULT_COMPLETED_LEVELS
                _answeredLogs.value = initialLogs
                writeSnapshotToPrefs(DEFAULT_COMPLETED_LEVELS, DEFAULT_COMPLETED_LEVELS, initialLogs, markInitialized = true)
            } else {
                val compSet = readStringSetFromPrefs(KEY_COMPLETED)
                val noteSet = readStringSetFromPrefs(KEY_NOTES)
                val rawLogs = prefs.getString(KEY_ANSWERED_LOGS, "[]") ?: "[]"
                _completedLevelIds.value = compSet
                _savedNoteLevelIds.value = noteSet
                _answeredLogs.value = parseLogs(rawLogs)
            }
            tryFlushPendingSync()
        }
    }

    private fun migrateLegacyPrefsIfNeeded() {
        val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        if (!legacyPrefs.getBoolean(KEY_INITIALIZED, false)) {
            if (legacyPrefs.all.isNotEmpty()) {
                SecurePrefsFactory.deletePrefsFile(appContext, LEGACY_PREFS_NAME)
            }
            return
        }

        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            val compSet = readLegacyEncryptedStringSet(legacyPrefs, KEY_COMPLETED, LEGACY_KEY_COMPLETED)
            val noteSet = readLegacyEncryptedStringSet(legacyPrefs, KEY_NOTES, LEGACY_KEY_NOTES)
            val rawLegacyLogs = legacyPrefs.getString(KEY_ANSWERED_LOGS, "[]") ?: "[]"
            val logsJson = if (rawLegacyLogs == "[]") "[]" else KeystoreCryptoManager.decryptLocalString(rawLegacyLogs).ifBlank { "[]" }
            val logs = parseLogs(logsJson)

            writeSnapshotToPrefs(compSet, noteSet, logs, markInitialized = true)

            val rawPending = legacyPrefs.getString(KEY_PENDING_QUEUE, null)
            if (!rawPending.isNullOrBlank()) {
                val decryptedPending = KeystoreCryptoManager.decryptLocalString(rawPending).ifBlank { "[]" }
                prefs.edit().putString(KEY_PENDING_QUEUE, decryptedPending).commit()
            }
        }

        SecurePrefsFactory.deletePrefsFile(appContext, LEGACY_PREFS_NAME)
    }

    private fun readStringSetFromPrefs(key: String): Set<String> {
        val raw = prefs.getString(key, null) ?: return emptySet()
        return runCatching {
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) {
                    add(arr.getString(i))
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun readLegacyEncryptedStringSet(
        legacyPrefs: android.content.SharedPreferences,
        encKey: String,
        legacyKey: String
    ): Set<String> {
        val encRaw = legacyPrefs.getString(encKey, null)
        if (!encRaw.isNullOrBlank()) {
            val jsonStr = KeystoreCryptoManager.decryptLocalString(encRaw)
            if (jsonStr.isNotBlank()) {
                return runCatching {
                    val arr = JSONArray(jsonStr)
                    buildSet {
                        for (i in 0 until arr.length()) {
                            add(arr.getString(i))
                        }
                    }
                }.getOrDefault(emptySet())
            }
        }
        return legacyPrefs.getStringSet(legacyKey, emptySet())?.toSet() ?: emptySet()
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
        persistStateAsync()
    }

    fun toggleNoteSaved(levelId: String) {
        val current = _savedNoteLevelIds.value
        _savedNoteLevelIds.value = if (current.contains(levelId)) current - levelId else current + levelId
        persistStateAsync()
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
        persistStateAsync()
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

    private fun persistStateAsync() {
        val completedSnapshot = _completedLevelIds.value
        val savedSnapshot = _savedNoteLevelIds.value
        val logsSnapshot = _answeredLogs.value
        viewModelScope.launch(Dispatchers.IO) {
            writeSnapshotToPrefs(completedSnapshot, savedSnapshot, logsSnapshot, markInitialized = false)
            enqueuePendingSync(
                "progress_update",
                mapOf(
                    "completed" to completedSnapshot.toList(),
                    "saved" to savedSnapshot.toList(),
                    "timestamp" to System.currentTimeMillis()
                )
            )
            flushPendingSyncInternal()
        }
    }

    private fun enqueuePendingSync(actionType: String, payloadMap: Map<String, Any>) {
        try {
            val rawStored = prefs.getString(KEY_PENDING_QUEUE, null) ?: "[]"
            val arr = JSONArray(rawStored.ifBlank { "[]" })
            val item = JSONObject().apply {
                put("type", actionType)
                val dataObj = JSONObject()
                payloadMap.forEach { (k, v) ->
                    if (v is List<*>) {
                        dataObj.put(k, JSONArray(v))
                    } else {
                        dataObj.put(k, v)
                    }
                }
                put("payload", dataObj)
                put("queuedAt", System.currentTimeMillis())
            }
            arr.put(item)
            prefs.edit().putString(KEY_PENDING_QUEUE, arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun tryFlushPendingSync() {
        viewModelScope.launch(Dispatchers.IO) {
            flushPendingSyncInternal()
        }
    }

    private suspend fun flushPendingSyncInternal() {
        try {
            val rawStored = prefs.getString(KEY_PENDING_QUEUE, null) ?: return
            val arr = JSONArray(rawStored.ifBlank { "[]" })
            if (arr.length() == 0) return

            val currentUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
            if (currentUid.isNullOrBlank() || currentUid.startsWith("demo_")) {
                return
            }
            if (!isNetworkAvailable()) {
                return
            }

            val repo = runCatching {
                val dbId = appContext.getString(R.string.firestore_database_id)
                ScholarIdentityRepository(FirebaseFirestore.getInstance(dbId))
            }.getOrNull() ?: return

            val mergedCompleted = mutableSetOf<String>()
            val remainingArr = JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val payload = item.optJSONObject("payload")
                val compArr = payload?.optJSONArray("completed")
                if (compArr != null) {
                    for (j in 0 until compArr.length()) {
                        mergedCompleted.add(compArr.optString(j))
                    }
                }
            }

            val result = repo.syncCompletedLessonsSnapshot(currentUid, mergedCompleted.filter { it.isNotBlank() })
            if (result.isFailure) {
                for (i in 0 until arr.length()) {
                    remainingArr.put(arr.getJSONObject(i))
                }
            }

            prefs.edit().putString(
                KEY_PENDING_QUEUE,
                if (remainingArr.length() == 0) "[]" else remainingArr.toString()
            ).apply()
        } catch (_: Exception) {}
    }

    private fun isNetworkAvailable(): Boolean {
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching true
            val network = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    private fun registerConnectivitySyncCallback() {
        runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    tryFlushPendingSync()
                }
            }
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
        }
    }

    private fun writeSnapshotToPrefs(
        completedSnapshot: Set<String>,
        savedSnapshot: Set<String>,
        logsSnapshot: List<AnsweredQuestionRecord>,
        markInitialized: Boolean
    ) {
        val arr = JSONArray()
        logsSnapshot.forEach { rec ->
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
        val completedJson = JSONArray(completedSnapshot.toList()).toString()
        val savedJson = JSONArray(savedSnapshot.toList()).toString()

        val editor = prefs.edit()
            .putString(KEY_COMPLETED, completedJson)
            .putString(KEY_NOTES, savedJson)
            .putString(KEY_ANSWERED_LOGS, arr.toString())
        if (markInitialized) {
            editor.putBoolean(KEY_INITIALIZED, true)
        }
        editor.commit()
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

    override fun onCleared() {
        super.onCleared()
        networkCallback?.let { cb ->
            runCatching {
                val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(cb)
            }
        }
        networkCallback = null
    }

    companion object {
        const val LEGACY_PREFS_NAME = "vidya_vault_state_v1"
        const val ENCRYPTED_PREFS_NAME = "vidya_vault_encrypted_v2"
        private const val KEY_INITIALIZED = "initialized"
        private const val LEGACY_KEY_COMPLETED = "completed_levels"
        private const val LEGACY_KEY_NOTES = "saved_notes"
        private const val KEY_COMPLETED = "completed_levels_enc"
        private const val KEY_NOTES = "saved_notes_enc"
        private const val KEY_ANSWERED_LOGS = "answered_logs"
        private const val KEY_PENDING_QUEUE = "pending_sync_queue"

        private val DEFAULT_COMPLETED_LEVELS = setOf(
            "math_l1", "math_l2",
            "phys_l1",
            "ai_l1", "ai_l2", "ai_l3", "ai_l4",
            "eng_l1"
        )
    }
}
