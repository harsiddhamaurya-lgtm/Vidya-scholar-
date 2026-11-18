package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.UserProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encrypted at-rest local persistence for [UserProfile] and user preferences using
 * [androidx.security.crypto.EncryptedSharedPreferences] (AES256-SIV keys + AES256-GCM values).
 */
class LocalProfileStore(context: Context) {
    private val appContext = context.applicationContext

    private val profilePrefs: SharedPreferences by lazy {
        SecurePrefsFactory.getEncryptedPrefs(appContext, PREFS_PROFILE_FILE)
    }

    private val appPrefs: SharedPreferences by lazy {
        SecurePrefsFactory.getEncryptedPrefs(appContext, PREFS_SETTINGS_FILE)
    }

    fun saveProfile(profile: UserProfile) {
        if (profile.uid.isBlank()) return
        val json = serializeProfile(profile)
        profilePrefs.edit()
            .putString(KEY_PROFILE_PREFIX + profile.uid, json)
            .putString(KEY_LAST_UID, profile.uid)
            .apply()
    }

    fun getProfile(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        val raw = profilePrefs.getString(KEY_PROFILE_PREFIX + uid, null) ?: return null
        return deserializeProfile(raw)
    }

    fun clearProfile(uid: String) {
        val editor = profilePrefs.edit()
        if (uid.isNotBlank()) {
            editor.remove(KEY_PROFILE_PREFIX + uid)
            editor.remove(KEY_PENDING_PROGRESS_PREFIX + uid)
        }
        if (profilePrefs.getString(KEY_LAST_UID, null) == uid) {
            editor.remove(KEY_LAST_UID)
        }
        editor.apply()
    }

    fun clearAllProfiles() {
        profilePrefs.edit().clear().apply()
    }

    fun queuePendingProfileProgress(profile: UserProfile) {
        if (profile.uid.isBlank() || profile.uid.startsWith("demo_") || profile.uid.startsWith("test_acc_")) {
            return
        }
        val obj = JSONObject().apply {
            put("uid", profile.uid)
            put("xp", profile.xp)
            put("level", profile.level)
            put("completedLessons", JSONArray(profile.completedLessons))
            put("queuedAt", System.currentTimeMillis())
        }
        profilePrefs.edit()
            .putString(KEY_PENDING_PROGRESS_PREFIX + profile.uid, obj.toString())
            .apply()
    }

    fun getPendingProfileProgress(uid: String): PendingProfileProgress? {
        if (uid.isBlank()) return null
        val raw = profilePrefs.getString(KEY_PENDING_PROGRESS_PREFIX + uid, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val lessonsArr = obj.optJSONArray("completedLessons") ?: JSONArray()
            val lessons = buildList {
                for (i in 0 until lessonsArr.length()) {
                    val item = lessonsArr.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
            PendingProfileProgress(
                uid = obj.getString("uid"),
                xp = obj.getInt("xp"),
                level = obj.getString("level"),
                completedLessons = lessons
            )
        }.getOrNull()
    }

    fun clearPendingProfileProgress(uid: String) {
        if (uid.isBlank()) return
        profilePrefs.edit().remove(KEY_PENDING_PROGRESS_PREFIX + uid).apply()
    }

    fun saveThemeMode(modeName: String) {
        appPrefs.edit().putString(KEY_THEME_MODE, modeName).apply()
    }

    fun getThemeMode(defaultMode: String): String {
        return appPrefs.getString(KEY_THEME_MODE, defaultMode) ?: defaultMode
    }

    @Synchronized
    fun nextQuoteIndex(deckSize: Int): Int {
        if (deckSize <= 1) return 0
        val lastIndex = appPrefs.getInt(KEY_LAST_QUOTE_INDEX, -1)
        val rawDeck = appPrefs.getString(KEY_QUOTE_DECK, "").orEmpty()
        val remaining = rawDeck
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0 until deckSize }
            .toMutableList()

        if (remaining.isEmpty()) {
            val freshDeck = (0 until deckSize).shuffled().toMutableList()
            if (freshDeck.first() == lastIndex && freshDeck.size > 1) {
                val swapIdx = 1 + (freshDeck.size - 1).let { count ->
                    if (count <= 1) 0 else kotlin.random.Random.nextInt(count)
                }
                val tmp = freshDeck[0]
                freshDeck[0] = freshDeck[swapIdx]
                freshDeck[swapIdx] = tmp
            }
            remaining.addAll(freshDeck)
        }

        val chosen = remaining.removeAt(0)
        appPrefs.edit()
            .putInt(KEY_LAST_QUOTE_INDEX, chosen)
            .putString(KEY_QUOTE_DECK, remaining.joinToString(","))
            .apply()
        return chosen
    }

    private fun serializeProfile(profile: UserProfile): String {
        val obj = JSONObject().apply {
            put("uid", profile.uid)
            put("name", profile.name)
            put("email", profile.email)
            put("photoUrl", profile.photoUrl)
            put("vidyaId", profile.vidyaId)
            put("vidyaHandle", profile.vidyaHandle)
            put("vidyaDiscriminator", profile.vidyaDiscriminator)
            put("vidyaKey", profile.vidyaKey)
            put("contactCode", profile.contactCode)
            put("avatarMode", profile.avatarMode)
            put("avatarPresetId", profile.avatarPresetId)
            put("avatarCrestId", profile.avatarCrestId)
            put("avatarFrameId", profile.avatarFrameId)
            put("avatarToneId", profile.avatarToneId)
            put("localAvatarPhotoPath", profile.localAvatarPhotoPath)
            put("focusRealm", profile.focusRealm)
            put("standardId", profile.standardId)
            put("streamId", profile.streamId)
            put("batchType", profile.batchType)
            put("preferredLanguage", profile.preferredLanguage)
            put("completedStandards", JSONArray(profile.completedStandards))
            put("onboardingCompleted", profile.onboardingCompleted)
            put("streak", profile.streak)
            put("xp", profile.xp)
            put("level", profile.level)
            put("hearts", profile.hearts)
            put("completedLessons", JSONArray(profile.completedLessons))
            put("publicKey", profile.publicKey)
            put("keyVersion", profile.keyVersion)
        }
        return obj.toString()
    }

    private fun deserializeProfile(raw: String): UserProfile? {
        return runCatching {
            val obj = JSONObject(raw)
            val lessonsArr = obj.optJSONArray("completedLessons") ?: JSONArray()
            val lessons = buildList {
                for (i in 0 until lessonsArr.length()) {
                    val item = lessonsArr.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
            val completedStandardsArr = obj.optJSONArray("completedStandards") ?: JSONArray()
            val completedStandards = buildList {
                for (i in 0 until completedStandardsArr.length()) {
                    val item = completedStandardsArr.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
            UserProfile(
                uid = obj.optString("uid"),
                name = obj.optString("name", "Scholar"),
                email = obj.optString("email"),
                photoUrl = obj.optString("photoUrl"),
                vidyaId = obj.optString("vidyaId"),
                vidyaHandle = obj.optString("vidyaHandle"),
                vidyaDiscriminator = obj.optString("vidyaDiscriminator"),
                vidyaKey = obj.optString("vidyaKey"),
                contactCode = obj.optString("contactCode"),
                avatarMode = obj.optString("avatarMode", "PRESET"),
                avatarPresetId = obj.optString("avatarPresetId", "vedic_strategist"),
                avatarCrestId = obj.optString("avatarCrestId", "yantra"),
                avatarFrameId = obj.optString("avatarFrameId", "astrolabe"),
                avatarToneId = obj.optString("avatarToneId", "saffron"),
                localAvatarPhotoPath = obj.optString("localAvatarPhotoPath"),
                focusRealm = obj.optString("focusRealm", "MATHEMATICS"),
                standardId = obj.optString("standardId", "class_11"),
                streamId = obj.optString("streamId", "science_pcm"),
                batchType = obj.optString("batchType", "live"),
                preferredLanguage = obj.optString("preferredLanguage", "en"),
                completedStandards = completedStandards,
                onboardingCompleted = obj.optBoolean("onboardingCompleted", false),
                streak = obj.optInt("streak", 5),
                xp = obj.optInt("xp", 1250),
                level = obj.optString("level", "Silver Scholar"),
                hearts = obj.optInt("hearts", 5),
                completedLessons = lessons,
                publicKey = obj.optString("publicKey"),
                keyVersion = obj.optInt("keyVersion", 1)
            )
        }.getOrNull()
    }

    data class PendingProfileProgress(
        val uid: String,
        val xp: Int,
        val level: String,
        val completedLessons: List<String>
    )

    companion object {
        const val PREFS_PROFILE_FILE = "vidya_profile_encrypted_v1"
        const val PREFS_SETTINGS_FILE = "vidya_prefs_encrypted_v1"
        private const val KEY_PROFILE_PREFIX = "profile_"
        private const val KEY_PENDING_PROGRESS_PREFIX = "pending_progress_"
        private const val KEY_LAST_UID = "last_active_uid"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_QUOTE_INDEX = "signin_last_quote_index"
        private const val KEY_QUOTE_DECK = "signin_quote_deck"
    }
}
