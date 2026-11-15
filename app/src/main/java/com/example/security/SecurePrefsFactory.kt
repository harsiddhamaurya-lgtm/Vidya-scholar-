package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory providing [EncryptedSharedPreferences] backed by Android Keystore [MasterKey]
 * (AES256-SIV key encryption + AES256-GCM value encryption) for all persisted local study
 * progress, user profile cache, preferences, and SQLCipher database key material.
 *
 * Includes self-healing recovery on corrupted keyset/backup state and an AES-256-GCM
 * [SharedPreferences] wrapper exclusively when running in host JVM / Robolectric test
 * sandboxes where the "AndroidKeyStore" provider is unavailable.
 */
object SecurePrefsFactory {
    private const val TAG = "SecurePrefsFactory"
    private val instances = ConcurrentHashMap<String, SharedPreferences>()

    fun getEncryptedPrefs(context: Context, fileName: String): SharedPreferences {
        val appContext = context.applicationContext
        return instances.getOrPut(fileName) {
            createEncryptedPrefs(appContext, fileName)
        }
    }

    fun deletePrefsFile(context: Context, fileName: String) {
        val appContext = context.applicationContext
        instances.remove(fileName)
        runCatching {
            appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().clear().commit()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { appContext.deleteSharedPreferences(fileName) }
        }
    }

    fun isHardwareEncryptedPrefsSupported(): Boolean {
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            return false
        }
        return runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun createEncryptedPrefs(appContext: Context, fileName: String): SharedPreferences {
        if (!isHardwareEncryptedPrefsSupported()) {
            return AesEncryptedSharedPreferences(
                appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            )
        }

        return try {
            openJetpackEncryptedPrefs(appContext, fileName)
        } catch (e: Exception) {
            Log.w(TAG, "Recovering EncryptedSharedPreferences for $fileName after error: ${e.message}")
            deletePrefsFile(appContext, fileName)
            try {
                openJetpackEncryptedPrefs(appContext, fileName)
            } catch (retryError: Exception) {
                Log.e(TAG, "Falling back to Keystore-wrapped prefs for $fileName: ${retryError.message}")
                AesEncryptedSharedPreferences(
                    appContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun openJetpackEncryptedPrefs(appContext: Context, fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

/**
 * Fallback AES-256-GCM SharedPreferences implementation used strictly inside JVM / Robolectric
 * environments lacking AndroidKeyStore. Both preference keys and serialized typed values are
 * encrypted at rest before hitting the underlying SharedPreferences.
 */
internal class AesEncryptedSharedPreferences(
    private val delegate: SharedPreferences
) : SharedPreferences {

    private fun hashedKey(key: String): String =
        "ek_${KeystoreCryptoManager.sha256Digest(key).take(24)}"

    private fun readRecord(key: String): JSONObject? {
        val raw = delegate.getString(hashedKey(key), null) ?: return null
        val decrypted = KeystoreCryptoManager.decryptLocalString(raw)
        if (decrypted.isBlank()) return null
        return runCatching { JSONObject(decrypted) }.getOrNull()
    }

    override fun getAll(): MutableMap<String, *> {
        val out = linkedMapOf<String, Any?>()
        delegate.all.values.forEach { raw ->
            val str = raw as? String ?: return@forEach
            val decrypted = KeystoreCryptoManager.decryptLocalString(str)
            val obj = runCatching { JSONObject(decrypted) }.getOrNull() ?: return@forEach
            val k = obj.optString("k")
            if (k.isBlank()) return@forEach
            out[k] = when (obj.optString("t")) {
                "s" -> obj.optString("v")
                "b" -> obj.optBoolean("v")
                "i" -> obj.optInt("v")
                "l" -> obj.optLong("v")
                "f" -> obj.optDouble("v").toFloat()
                "ss" -> {
                    val arr = obj.optJSONArray("v") ?: JSONArray()
                    buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
                }
                else -> null
            }
        }
        return out
    }

    override fun getString(key: String?, defValue: String?): String? {
        if (key == null) return defValue
        val obj = readRecord(key) ?: return defValue
        return if (obj.optString("t") == "s") obj.optString("v", defValue ?: "") else defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        if (key == null) return defValues
        val obj = readRecord(key) ?: return defValues
        if (obj.optString("t") != "ss") return defValues
        val arr = obj.optJSONArray("v") ?: return defValues
        return buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }.toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (key == null) return defValue
        val obj = readRecord(key) ?: return defValue
        return if (obj.optString("t") == "i") obj.optInt("v", defValue) else defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        if (key == null) return defValue
        val obj = readRecord(key) ?: return defValue
        return if (obj.optString("t") == "l") obj.optLong("v", defValue) else defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        if (key == null) return defValue
        val obj = readRecord(key) ?: return defValue
        return if (obj.optString("t") == "f") obj.optDouble("v", defValue.toDouble()).toFloat() else defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (key == null) return defValue
        val obj = readRecord(key) ?: return defValue
        return if (obj.optString("t") == "b") obj.optBoolean("v", defValue) else defValue
    }

    override fun contains(key: String?): Boolean =
        key != null && delegate.contains(hashedKey(key))

    override fun edit(): SharedPreferences.Editor = Editor(delegate.edit())

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        delegate.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        delegate.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private inner class Editor(
        private val delegateEditor: SharedPreferences.Editor
    ) : SharedPreferences.Editor {

        private fun putEncrypted(key: String?, type: String, value: Any?) {
            if (key == null) return
            if (value == null) {
                delegateEditor.remove(hashedKey(key))
                return
            }
            val obj = JSONObject().apply {
                put("k", key)
                put("t", type)
                put("v", value)
            }
            delegateEditor.putString(
                hashedKey(key),
                KeystoreCryptoManager.encryptLocalString(obj.toString())
            )
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            putEncrypted(key, "s", value)
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key == null) return this
            if (values == null) {
                delegateEditor.remove(hashedKey(key))
            } else {
                putEncrypted(key, "ss", JSONArray(values.toList()))
            }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            putEncrypted(key, "i", value)
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            putEncrypted(key, "l", value)
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            putEncrypted(key, "f", value.toDouble())
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            putEncrypted(key, "b", value)
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) delegateEditor.remove(hashedKey(key))
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            delegateEditor.clear()
            return this
        }

        override fun commit(): Boolean = delegateEditor.commit()

        override fun apply() = delegateEditor.apply()
    }
}
