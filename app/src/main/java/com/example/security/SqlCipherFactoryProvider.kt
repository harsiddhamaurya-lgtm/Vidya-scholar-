package com.example.security

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

/**
 * Unified SQLCipher open-helper factory and passphrase manager shared across all encrypted
 * Room databases ([com.example.data.local.AppDatabase] and [com.example.rag.RagRoomDatabase]).
 */
object SqlCipherFactoryProvider {
    private const val TAG = "SqlCipherFactoryProvider"

    fun createFactoryOrNull(
        appContext: Context,
        prefsName: String,
        passphraseKey: String
    ): SupportOpenHelperFactory? {
        if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
            return null
        }
        return try {
            System.loadLibrary("sqlcipher")
            val passphrase = getOrCreatePassphrase(appContext, prefsName, passphraseKey)
            SupportOpenHelperFactory(passphrase)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "libsqlcipher unavailable in current runtime: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SQLCipher factory: ${e.message}")
            null
        }
    }

    internal fun getOrCreatePassphrase(
        appContext: Context,
        prefsName: String,
        passphraseKey: String
    ): ByteArray {
        val prefs = SecurePrefsFactory.getEncryptedPrefs(appContext, prefsName)
        val existingB64 = prefs.getString(passphraseKey, null)
        if (!existingB64.isNullOrBlank()) {
            val decoded = runCatching { Base64.decode(existingB64, Base64.NO_WRAP) }.getOrNull()
            if (decoded != null && decoded.size >= 32) {
                return decoded
            }
        }
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        prefs.edit()
            .putString(passphraseKey, Base64.encodeToString(keyBytes, Base64.NO_WRAP))
            .commit()
        return keyBytes
    }
}
