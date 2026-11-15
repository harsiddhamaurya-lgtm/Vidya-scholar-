package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.SecurePrefsFactory
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

@Database(
    entities = [GameSessionEntity::class, GameStatsEntity::class, UserBadgeEntity::class, MatchHistoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        private const val TAG = "AppDatabase"
        const val ENCRYPTED_DB_NAME = "vidhya_scholar_enc.db"
        const val LEGACY_DB_NAME = "vidhya_scholar.db"
        private const val DB_KEY_PREFS = "vidya_sqlcipher_key_v1"
        private const val KEY_DB_PASSPHRASE = "sqlcipher_passphrase_b64"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var sqlCipherEnabled: Boolean = false

        fun isSqlCipherActive(): Boolean = sqlCipherEnabled

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            val legacyDbFile = appContext.getDatabasePath(LEGACY_DB_NAME)
            val encDbFile = appContext.getDatabasePath(ENCRYPTED_DB_NAME)
            val shouldMigrateLegacy = legacyDbFile.exists() && !encDbFile.exists()

            val builder = Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                ENCRYPTED_DB_NAME
            ).fallbackToDestructiveMigration()

            val factory = createSqlCipherFactoryOrNull(appContext)
            if (factory != null) {
                builder.openHelperFactory(factory)
                sqlCipherEnabled = true
            } else {
                sqlCipherEnabled = false
            }

            val instance = builder.build()

            if (shouldMigrateLegacy) {
                migrateLegacyPlaintextDatabase(appContext, legacyDbFile.path, instance)
            } else if (legacyDbFile.exists()) {
                // Clean up any leftover legacy plaintext database once encrypted DB exists
                runCatching { appContext.deleteDatabase(LEGACY_DB_NAME) }
            }

            return instance
        }

        private fun createSqlCipherFactoryOrNull(appContext: Context): SupportOpenHelperFactory? {
            if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
                return null
            }
            return try {
                System.loadLibrary("sqlcipher")
                val passphrase = getOrCreateDatabasePassphrase(appContext)
                SupportOpenHelperFactory(passphrase)
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "SQLCipher native library unavailable in current runtime: ${e.message}")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SQLCipher factory: ${e.message}")
                null
            }
        }

        internal fun getOrCreateDatabasePassphrase(appContext: Context): ByteArray {
            val prefs = SecurePrefsFactory.getEncryptedPrefs(appContext, DB_KEY_PREFS)
            val existingB64 = prefs.getString(KEY_DB_PASSPHRASE, null)
            if (!existingB64.isNullOrBlank()) {
                val decoded = runCatching { Base64.decode(existingB64, Base64.NO_WRAP) }.getOrNull()
                if (decoded != null && decoded.size >= 32) {
                    return decoded
                }
            }
            val keyBytes = ByteArray(32)
            SecureRandom().nextBytes(keyBytes)
            prefs.edit()
                .putString(KEY_DB_PASSPHRASE, Base64.encodeToString(keyBytes, Base64.NO_WRAP))
                .commit()
            return keyBytes
        }

        private fun migrateLegacyPlaintextDatabase(
            appContext: Context,
            legacyDbPath: String,
            targetDb: AppDatabase
        ) {
            var legacySqlite: SQLiteDatabase? = null
            try {
                legacySqlite = SQLiteDatabase.openDatabase(legacyDbPath, null, SQLiteDatabase.OPEN_READONLY)
                val writable = targetDb.openHelper.writableDatabase
                writable.beginTransaction()
                try {
                    copyTableRows(legacySqlite, writable, "game_sessions")
                    copyTableRows(legacySqlite, writable, "game_stats")
                    copyTableRows(legacySqlite, writable, "user_badges")
                    copyTableRows(legacySqlite, writable, "match_history")
                    writable.setTransactionSuccessful()
                } finally {
                    writable.endTransaction()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Legacy SQLite migration skipped or partial: ${e.message}")
            } finally {
                runCatching { legacySqlite?.close() }
                runCatching { appContext.deleteDatabase(LEGACY_DB_NAME) }
            }
        }

        private fun copyTableRows(
            source: SQLiteDatabase,
            target: androidx.sqlite.db.SupportSQLiteDatabase,
            tableName: String
        ) {
            val tableCheck = source.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(tableName)
            )
            val exists = tableCheck.use { it.moveToFirst() }
            if (!exists) return

            source.rawQuery("SELECT * FROM $tableName", null).use { cursor ->
                val columnNames = cursor.columnNames
                while (cursor.moveToNext()) {
                    val values = ContentValues()
                    for (idx in columnNames.indices) {
                        val col = columnNames[idx]
                        when (cursor.getType(idx)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> values.putNull(col)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(col, cursor.getLong(idx))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(col, cursor.getDouble(idx))
                            android.database.Cursor.FIELD_TYPE_STRING -> values.put(col, cursor.getString(idx))
                            android.database.Cursor.FIELD_TYPE_BLOB -> values.put(col, cursor.getBlob(idx))
                        }
                    }
                    target.insert(tableName, SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }
        }
    }
}
