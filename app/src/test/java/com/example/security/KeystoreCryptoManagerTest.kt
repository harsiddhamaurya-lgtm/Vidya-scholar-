package com.example.security

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.RealmId
import com.example.data.UserProfile
import com.example.data.local.AppDatabase
import com.example.viewmodel.VidyaVaultViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class KeystoreCryptoManagerTest {

    @Test
    fun localAesGcm_roundTripsAndMigratesLegacyPlaintext() {
        val secretJson = """[{"id":"math_l1","isCorrect":true}]"""
        val encrypted = KeystoreCryptoManager.encryptLocalString(secretJson)

        assertTrue(encrypted.startsWith("ENCv1:"))
        assertNotEquals(secretJson, encrypted)
        assertEquals(secretJson, KeystoreCryptoManager.decryptLocalString(encrypted))

        // Legacy plaintext passes through cleanly
        assertEquals("legacy_plain_value", KeystoreCryptoManager.decryptLocalString("legacy_plain_value"))
    }

    @Test
    fun e2eeRsaOaep_wrapsUnwrapsAndIsolatesUsers() {
        val alicePub = KeystoreCryptoManager.ensureIdentityKeyPair("alice_uid")
        val bobPub = KeystoreCryptoManager.ensureIdentityKeyPair("bob_uid")
        val evePub = KeystoreCryptoManager.ensureIdentityKeyPair("eve_uid")

        assertTrue(alicePub.isNotBlank())
        assertTrue(bobPub.isNotBlank())
        assertTrue(evePub.isNotBlank())
        assertNotEquals(alicePub, bobPub)

        val sessionDek = KeystoreCryptoManager.generateSessionAesKey()
        val wrappedForAlice = KeystoreCryptoManager.wrapSessionKey(sessionDek, alicePub)
        val wrappedForBob = KeystoreCryptoManager.wrapSessionKey(sessionDek, bobPub)

        assertTrue(wrappedForAlice.isNotBlank())
        assertTrue(wrappedForBob.isNotBlank())

        val (ciphertext, iv) = KeystoreCryptoManager.encryptWithSessionKey("Top secret Sanskrit sutra proof!", sessionDek)
        assertFalse(ciphertext.contains("Sanskrit"))

        // Bob unwraps and decrypts
        val bobDek = KeystoreCryptoManager.unwrapSessionKey("bob_uid", wrappedForBob)
        assertNotNull(bobDek)
        assertEquals("Top secret Sanskrit sutra proof!", KeystoreCryptoManager.decryptWithSessionKey(ciphertext, iv, bobDek!!))

        // Alice unwraps and decrypts
        val aliceDek = KeystoreCryptoManager.unwrapSessionKey("alice_uid", wrappedForAlice)
        assertNotNull(aliceDek)
        assertEquals("Top secret Sanskrit sutra proof!", KeystoreCryptoManager.decryptWithSessionKey(ciphertext, iv, aliceDek!!))

        // Eve cannot unwrap Bob's key
        assertNull(KeystoreCryptoManager.unwrapSessionKey("eve_uid", wrappedForBob))
    }

    @Test
    fun sha256Digest_isDeterministicAndSensitive() {
        val d1 = KeystoreCryptoManager.sha256Digest("match_1|1|abc")
        val d2 = KeystoreCryptoManager.sha256Digest("match_1|1|abc")
        val d3 = KeystoreCryptoManager.sha256Digest("match_1|2|abc")
        assertEquals(64, d1.length)
        assertEquals(d1, d2)
        assertNotEquals(d1, d3)
    }

    @Test
    fun vidyaVaultViewModel_migratesLegacyPlaintextPrefsToEncryptedPrefs() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SecurePrefsFactory.deletePrefsFile(app, VidyaVaultViewModel.ENCRYPTED_PREFS_NAME)

        val legacyPrefs = app.getSharedPreferences(VidyaVaultViewModel.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        legacyPrefs.edit().clear()
            .putBoolean("initialized", true)
            .putStringSet("completed_levels", setOf("math_l1", "phys_l1"))
            .putStringSet("saved_notes", setOf("math_l1"))
            .putString("answered_logs", "[]")
            .commit()

        val vm = VidyaVaultViewModel(app)
        val encryptedStore = SecurePrefsFactory.getEncryptedPrefs(app, VidyaVaultViewModel.ENCRYPTED_PREFS_NAME)
        val deadline = System.currentTimeMillis() + 3000L
        while (System.currentTimeMillis() < deadline && !encryptedStore.getBoolean("initialized", false)) {
            Thread.sleep(25)
        }

        assertTrue(vm.completedLevelIds.value.contains("phys_l1"))
        assertTrue(vm.savedNoteLevelIds.value.contains("math_l1"))
        assertTrue(vm.getLogsForRealm(RealmId.MATHEMATICS).isEmpty())

        // Legacy plaintext file was wiped after migration
        assertTrue(legacyPrefs.all.isEmpty())

        // Raw underlying SharedPreferences contains zero plaintext keys or values
        val rawUnderlying = app.getSharedPreferences(VidyaVaultViewModel.ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        assertFalse(rawUnderlying.contains("completed_levels_enc"))
        assertFalse(rawUnderlying.contains("initialized"))
        val rawDump = rawUnderlying.all.entries.joinToString { "${it.key}=${it.value}" }
        assertFalse(rawDump.contains("phys_l1"))
        assertFalse(rawDump.contains("math_l1"))
    }

    @Test
    fun localProfileStore_encryptsProfileAndThemeAtRest() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SecurePrefsFactory.deletePrefsFile(app, LocalProfileStore.PREFS_PROFILE_FILE)
        SecurePrefsFactory.deletePrefsFile(app, LocalProfileStore.PREFS_SETTINGS_FILE)

        val store = LocalProfileStore(app)
        val profile = UserProfile(
            uid = "uid_secret_42",
            name = "Arya Vidyarthi",
            email = "arya@example.com",
            vidyaId = "arya#4242",
            vidyaHandle = "arya",
            vidyaDiscriminator = "4242",
            vidyaKey = "arya_4242",
            onboardingCompleted = true,
            xp = 2450,
            level = "Gold Master",
            completedLessons = listOf("m_1", "p_1")
        )

        store.saveProfile(profile)
        store.queuePendingProfileProgress(profile.copy(xp = 2600))
        store.saveThemeMode("DARK")

        val loaded = store.getProfile("uid_secret_42")
        assertNotNull(loaded)
        assertEquals("Arya Vidyarthi", loaded?.name)
        assertEquals("arya@example.com", loaded?.email)
        assertEquals(2450, loaded?.xp)
        assertEquals(listOf("m_1", "p_1"), loaded?.completedLessons)
        assertEquals(2600, store.getPendingProfileProgress("uid_secret_42")?.xp)
        assertEquals("DARK", store.getThemeMode("LIGHT"))

        // Verify zero plaintext leakage in raw underlying SharedPreferences
        val rawProfileUnderlying = app.getSharedPreferences(LocalProfileStore.PREFS_PROFILE_FILE, Context.MODE_PRIVATE)
        val rawDump = rawProfileUnderlying.all.entries.joinToString { "${it.key}=${it.value}" }
        assertFalse(rawDump.contains("Arya Vidyarthi"))
        assertFalse(rawDump.contains("arya@example.com"))
        assertFalse(rawDump.contains("uid_secret_42"))

        store.clearProfile("uid_secret_42")
        assertNull(store.getProfile("uid_secret_42"))
        assertNull(store.getPendingProfileProgress("uid_secret_42"))
    }

    @Test
    fun appDatabase_persists32ByteKeyAndMigratesLegacySqlite() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val key1 = AppDatabase.getOrCreateDatabasePassphrase(app)
        val key2 = AppDatabase.getOrCreateDatabasePassphrase(app)
        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)

        // Reset Room singleton and seed a legacy unencrypted vidhya_scholar.db
        val legacyFile = app.getDatabasePath(AppDatabase.LEGACY_DB_NAME)
        legacyFile.parentFile?.mkdirs()
        app.deleteDatabase(AppDatabase.LEGACY_DB_NAME)
        app.deleteDatabase(AppDatabase.ENCRYPTED_DB_NAME)

        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        val legacyDb = SQLiteDatabase.openOrCreateDatabase(legacyFile, null)
        legacyDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS game_stats (
                gameType TEXT NOT NULL PRIMARY KEY,
                matchesPlayed INTEGER NOT NULL,
                matchesWon INTEGER NOT NULL,
                highScore INTEGER NOT NULL,
                bestTimeSeconds INTEGER NOT NULL,
                eloRating INTEGER NOT NULL,
                rankTier TEXT NOT NULL,
                lastPlayedTimestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        legacyDb.execSQL(
            """
            INSERT INTO game_stats VALUES ('SHATRANJ', 5, 4, 320, 95, 1420, 'Tactician', 1700000000)
            """.trimIndent()
        )
        legacyDb.close()
        assertTrue(legacyFile.exists())

        val db = AppDatabase.getInstance(app)
        val stats = db.gameDao().getAllStats().first()
        assertEquals(1, stats.size)
        assertEquals("SHATRANJ", stats.first().gameType)
        assertEquals(1420, stats.first().eloRating)
        assertFalse(legacyFile.exists())
        db.close()
        field.set(null, null)
    }
}
