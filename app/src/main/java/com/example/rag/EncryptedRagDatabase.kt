package com.example.rag

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.security.SqlCipherFactoryProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encrypted SQLCipher flat vector & textbook chunk cache scoped by (standardId, streamId).
 *
 * Uses `net.zetetic:sqlcipher-android` backed by a 32-byte Keystore-encrypted passphrase on
 * Android devices, and automatically falls back to Framework SQLite strictly in host JVM /
 * Robolectric test environments where native `libsqlcipher.so` is absent.
 */
class EncryptedRagDatabase(context: Context) {

    companion object {
        const val DB_NAME = "cosmium_rag_vault_enc.db"
        private const val DB_VERSION = 1
        private const val KEY_PREFS = "cosmium_rag_sqlcipher_key_v1"
        private const val KEY_PASSPHRASE = "rag_passphrase_b64"
        private const val TABLE_CHUNKS = "rag_chunks"
        const val VECTOR_DIM = 384

        fun encodeFloatArray(vector: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (v in vector) {
                buffer.putFloat(v)
            }
            return buffer.array()
        }

        fun decodeFloatArray(bytes: ByteArray): FloatArray {
            if (bytes.isEmpty() || bytes.size % 4 != 0) return FloatArray(0)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(bytes.size / 4) { buffer.float }
        }
    }

    private val appContext = context.applicationContext
    @Volatile
    var isSqlCipherActive: Boolean = false
        private set

    private val helper: SupportSQLiteOpenHelper by lazy { createOpenHelper() }

    private fun createOpenHelper(): SupportSQLiteOpenHelper {
        val callback = object : SupportSQLiteOpenHelper.Callback(DB_VERSION) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_CHUNKS (
                        chunk_id TEXT PRIMARY KEY NOT NULL,
                        standard_id TEXT NOT NULL,
                        stream_id TEXT NOT NULL,
                        subject_id TEXT NOT NULL,
                        subject_name TEXT NOT NULL,
                        chapter_number INTEGER NOT NULL,
                        level_id TEXT NOT NULL,
                        topic_title TEXT NOT NULL,
                        content_text TEXT NOT NULL,
                        formula_tags TEXT NOT NULL,
                        worked_example TEXT NOT NULL,
                        embedding_blob BLOB NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_rag_scope ON $TABLE_CHUNKS(standard_id, stream_id)"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_CHUNKS")
                onCreate(db)
            }
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(appContext)
            .name(DB_NAME)
            .callback(callback)
            .build()

        val cipherFactory = SqlCipherFactoryProvider.createFactoryOrNull(
            appContext = appContext,
            prefsName = KEY_PREFS,
            passphraseKey = KEY_PASSPHRASE
        )
        return if (cipherFactory != null) {
            isSqlCipherActive = true
            cipherFactory.create(config)
        } else {
            isSqlCipherActive = false
            FrameworkSQLiteOpenHelperFactory().create(config)
        }
    }

    fun upsertChunks(items: List<VectorizedChunk>) {
        if (items.isEmpty()) return
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            for (item in items) {
                val c = item.chunk
                val values = ContentValues().apply {
                    put("chunk_id", c.chunkId)
                    put("standard_id", c.standardId)
                    put("stream_id", c.streamId)
                    put("subject_id", c.subjectId)
                    put("subject_name", c.subjectName)
                    put("chapter_number", c.chapterNumber)
                    put("level_id", c.levelId)
                    put("topic_title", c.topicTitle)
                    put("content_text", c.contentText)
                    put("formula_tags", c.formulaTags)
                    put("worked_example", c.workedExample)
                    put("embedding_blob", encodeFloatArray(item.embedding))
                }
                db.insert(TABLE_CHUNKS, SQLiteDatabase.CONFLICT_REPLACE, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getChunksForScope(standardId: String, streamId: String): List<VectorizedChunk> {
        val db = helper.readableDatabase
        val results = mutableListOf<VectorizedChunk>()
        db.query(
            """
            SELECT chunk_id, standard_id, stream_id, subject_id, subject_name,
                   chapter_number, level_id, topic_title, content_text,
                   formula_tags, worked_example, embedding_blob
            FROM $TABLE_CHUNKS
            WHERE standard_id = ? AND stream_id = ?
            ORDER BY subject_name ASC, chapter_number ASC
            """.trimIndent(),
            arrayOf(standardId, streamId)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val chunk = TextbookChunk(
                    chunkId = cursor.getString(0),
                    standardId = cursor.getString(1),
                    streamId = cursor.getString(2),
                    subjectId = cursor.getString(3),
                    subjectName = cursor.getString(4),
                    chapterNumber = cursor.getInt(5),
                    levelId = cursor.getString(6),
                    topicTitle = cursor.getString(7),
                    contentText = cursor.getString(8),
                    formulaTags = cursor.getString(9),
                    workedExample = cursor.getString(10)
                )
                val vector = decodeFloatArray(cursor.getBlob(11))
                results += VectorizedChunk(chunk = chunk, embedding = vector)
            }
        }
        return results
    }

    fun getChunkCountForScope(standardId: String, streamId: String): Int {
        val db = helper.readableDatabase
        db.query(
            "SELECT COUNT(*) FROM $TABLE_CHUNKS WHERE standard_id = ? AND stream_id = ?",
            arrayOf(standardId, streamId)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun deleteScope(standardId: String, streamId: String) {
        val db = helper.writableDatabase
        db.delete(
            TABLE_CHUNKS,
            "standard_id = ? AND stream_id = ?",
            arrayOf(standardId, streamId)
        )
    }

    fun close() {
        runCatching { helper.close() }
    }
}
