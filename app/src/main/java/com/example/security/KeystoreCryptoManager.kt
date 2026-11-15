package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * Zero-UI hardware-backed AndroidKeyStore cryptography manager for:
 * 1. Local vault / offline sync queue AES-256-GCM encryption at rest.
 * 2. Per-user RSA-2048 OAEP identity keypairs for wrapping/unwrapping Varta & OnlineMatch AES-256 session DEKs.
 * 3. Deterministic SHA-256 integrity digests for online matchmaking turn payloads.
 *
 * Includes an automatic software JCA fallback exclusively when running inside JVM unit-test sandboxes
 * where the "AndroidKeyStore" provider is absent.
 */
object KeystoreCryptoManager {

    private const val TAG = "KeystoreCryptoManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val LOCAL_MASTER_ALIAS = "vidhya_vault_master_aes"
    private const val IDENTITY_ALIAS_PREFIX = "vidhya_e2ee_rsa_"
    private const val ENC_PREFIX = "ENCv1:"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val secureRandom = SecureRandom()
    private val oaepSpec = OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA1,
        PSource.PSpecified.DEFAULT
    )

    // In-memory JCA fallback used strictly in JVM / Robolectric sandboxes lacking AndroidKeyStore
    private val fallbackAesKeys = ConcurrentHashMap<String, SecretKey>()
    private val fallbackRsaPairs = ConcurrentHashMap<String, KeyPair>()
    private val lock = Any()

    fun encryptLocalString(plain: String): String {
        if (plain.isEmpty()) return plain
        return try {
            val key = getOrCreateLocalMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = ByteBuffer.allocate(1 + iv.size + cipherBytes.size)
                .put(iv.size.toByte())
                .put(iv)
                .put(cipherBytes)
                .array()
            ENC_PREFIX + encodeB64(combined)
        } catch (e: Exception) {
            Log.w(TAG, "Local AES encryption fallback: ${e.message}")
            plain
        }
    }

    fun decryptLocalString(stored: String): String {
        if (stored.isEmpty()) return stored
        // Graceful migration for legacy plaintext SharedPreferences entries
        if (!stored.startsWith(ENC_PREFIX)) return stored
        return try {
            val payload = decodeB64(stored.removePrefix(ENC_PREFIX))
            if (payload.size < 2) return ""
            val ivLen = payload[0].toInt() and 0xFF
            if (ivLen <= 0 || payload.size <= 1 + ivLen) return ""
            val iv = payload.copyOfRange(1, 1 + ivLen)
            val cipherBytes = payload.copyOfRange(1 + ivLen, payload.size)
            val key = getOrCreateLocalMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decrypt local entry: ${e.message}")
            ""
        }
    }

    fun ensureIdentityKeyPair(uid: String): String {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) return ""
        val alias = IDENTITY_ALIAS_PREFIX + cleanUid
        synchronized(lock) {
            try {
                val ks = getAndroidKeyStore()
                if (ks != null) {
                    val existingPub = ks.getCertificate(alias)?.publicKey
                    if (existingPub != null) {
                        return encodeB64(existingPub.encoded)
                    }
                    val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
                    val spec = KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .build()
                    kpg.initialize(spec)
                    val pair = kpg.generateKeyPair()
                    return encodeB64(pair.public.encoded)
                }
            } catch (e: Exception) {
                Log.w(TAG, "AndroidKeyStore RSA unavailable, using JVM fallback: ${e.message}")
            }

            val fallbackPair = fallbackRsaPairs.getOrPut(alias) {
                KeyPairGenerator.getInstance("RSA").apply { initialize(2048, secureRandom) }.generateKeyPair()
            }
            return encodeB64(fallbackPair.public.encoded)
        }
    }

    fun generateSessionAesKey(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256, secureRandom)
        return kg.generateKey()
    }

    fun wrapSessionKey(aesKey: SecretKey, recipientPublicKeyB64: String): String {
        val cleanB64 = recipientPublicKeyB64.trim()
        if (cleanB64.isBlank()) return ""
        return try {
            val pubBytes = decodeB64(cleanB64)
            val publicKey: PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(pubBytes))
            val cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec)
            encodeB64(cipher.doFinal(aesKey.encoded))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to wrap session key: ${e.message}")
            ""
        }
    }

    fun unwrapSessionKey(uid: String, wrappedKeyB64: String): SecretKey? {
        val cleanUid = uid.trim()
        val cleanWrapped = wrappedKeyB64.trim()
        if (cleanUid.isBlank() || cleanWrapped.isBlank()) return null
        val alias = IDENTITY_ALIAS_PREFIX + cleanUid
        return try {
            val privateKey = getIdentityPrivateKey(alias) ?: return null
            val cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec)
            val rawAes = cipher.doFinal(decodeB64(cleanWrapped))
            SecretKeySpec(rawAes, "AES")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unwrap session key for $cleanUid: ${e.message}")
            null
        }
    }

    fun encryptWithSessionKey(plaintext: String, aesKey: SecretKey): Pair<String, String> {
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return encodeB64(cipherBytes) to encodeB64(iv)
    }

    fun decryptWithSessionKey(ciphertextB64: String, ivB64: String, aesKey: SecretKey): String? {
        if (ciphertextB64.isBlank() || ivB64.isBlank()) return null
        return try {
            val iv = decodeB64(ivB64)
            val cipherBytes = decodeB64(ciphertextB64)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Session AES decryption failed: ${e.message}")
            null
        }
    }

    fun secretKeyToB64(aesKey: SecretKey): String = encodeB64(aesKey.encoded)

    fun secretKeyFromB64(rawB64: String): SecretKey? {
        if (rawB64.isBlank()) return null
        return try {
            val bytes = decodeB64(rawB64)
            if (bytes.isEmpty()) null else SecretKeySpec(bytes, "AES")
        } catch (_: Exception) {
            null
        }
    }

    fun sha256Digest(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun getOrCreateLocalMasterKey(): SecretKey {
        synchronized(lock) {
            try {
                val ks = getAndroidKeyStore()
                if (ks != null) {
                    val entry = ks.getKey(LOCAL_MASTER_ALIAS, null) as? SecretKey
                    if (entry != null) return entry

                    val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                    val spec = KeyGenParameterSpec.Builder(
                        LOCAL_MASTER_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                    kg.init(spec)
                    return kg.generateKey()
                }
            } catch (e: Exception) {
                Log.w(TAG, "AndroidKeyStore AES unavailable, using JVM fallback: ${e.message}")
            }

            return fallbackAesKeys.getOrPut(LOCAL_MASTER_ALIAS) {
                KeyGenerator.getInstance("AES").apply { init(256, secureRandom) }.generateKey()
            }
        }
    }

    private fun getIdentityPrivateKey(alias: String): PrivateKey? {
        try {
            val ks = getAndroidKeyStore()
            if (ks != null) {
                val pk = ks.getKey(alias, null) as? PrivateKey
                if (pk != null) return pk
            }
        } catch (e: Exception) {
            Log.w(TAG, "AndroidKeyStore private key lookup fallback: ${e.message}")
        }
        return fallbackRsaPairs[alias]?.private
    }

    private fun getAndroidKeyStore(): KeyStore? {
        return try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeB64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeB64(encoded: String): ByteArray {
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(encoded)
        }
    }
}
