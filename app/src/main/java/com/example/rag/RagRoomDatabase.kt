package com.example.rag

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.SqlCipherFactoryProvider
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity storing downloaded textbook chapter chunks and their 384-dim `all-MiniLM-L6-v2`
 * float32 embedding vectors inside an encrypted SQLCipher Room database.
 */
@Entity(
    tableName = "rag_chapters",
    indices = [
        Index(value = ["chunk_id"], unique = true),
        Index(value = ["standard_id", "stream_id"])
    ]
)
data class RagChapterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    val rowId: Int = 0,
    @ColumnInfo(name = "chunk_id")
    val chunkId: String,
    @ColumnInfo(name = "standard_id")
    val standardId: String,
    @ColumnInfo(name = "stream_id")
    val streamId: String,
    @ColumnInfo(name = "subject_id")
    val subjectId: String,
    @ColumnInfo(name = "subject_name")
    val subjectName: String,
    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int,
    @ColumnInfo(name = "level_id")
    val levelId: String,
    @ColumnInfo(name = "topic_title")
    val topicTitle: String,
    @ColumnInfo(name = "content_text")
    val contentText: String,
    @ColumnInfo(name = "formula_tags")
    val formulaTags: String,
    @ColumnInfo(name = "worked_example")
    val workedExample: String,
    @ColumnInfo(name = "embedding_blob")
    val embeddingBlob: ByteArray,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toTextbookChunk(): TextbookChunk = TextbookChunk(
        chunkId = chunkId,
        standardId = standardId,
        streamId = streamId,
        subjectId = subjectId,
        subjectName = subjectName,
        chapterNumber = chapterNumber,
        levelId = levelId,
        topicTitle = topicTitle,
        contentText = contentText,
        formulaTags = formulaTags,
        workedExample = workedExample
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RagChapterEntity) return false
        return rowId == other.rowId &&
            chunkId == other.chunkId &&
            standardId == other.standardId &&
            streamId == other.streamId &&
            embeddingBlob.contentEquals(other.embeddingBlob)
    }

    override fun hashCode(): Int {
        var result = rowId
        result = 31 * result + chunkId.hashCode()
        result = 31 * result + embeddingBlob.contentHashCode()
        return result
    }
}

/**
 * Room FTS4 Virtual Table synchronized with [RagChapterEntity] for instant SQLite Full-Text Search
 * across textbook chapter titles, subjects, formulas, body concepts, and worked examples.
 */
@Fts4(contentEntity = RagChapterEntity::class)
@Entity(tableName = "rag_chapters_fts")
data class RagChapterFtsEntity(
    @ColumnInfo(name = "topic_title")
    val topicTitle: String,
    @ColumnInfo(name = "subject_name")
    val subjectName: String,
    @ColumnInfo(name = "formula_tags")
    val formulaTags: String,
    @ColumnInfo(name = "content_text")
    val contentText: String,
    @ColumnInfo(name = "worked_example")
    val workedExample: String
)

@Dao
interface RagChapterDao {

    @Query(
        """
        SELECT * FROM rag_chapters
        WHERE standard_id = :standardId AND stream_id = :streamId
        ORDER BY subject_name ASC, chapter_number ASC
        """
    )
    fun observeChaptersForScope(standardId: String, streamId: String): Flow<List<RagChapterEntity>>

    @Query(
        """
        SELECT * FROM rag_chapters
        WHERE standard_id = :standardId AND stream_id = :streamId
        ORDER BY subject_name ASC, chapter_number ASC
        """
    )
    suspend fun getChaptersForScope(standardId: String, streamId: String): List<RagChapterEntity>

    @Query(
        """
        SELECT COUNT(*) FROM rag_chapters
        WHERE standard_id = :standardId AND stream_id = :streamId
        """
    )
    suspend fun getCountForScope(standardId: String, streamId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(chapters: List<RagChapterEntity>)

    @Query("DELETE FROM rag_chapters WHERE standard_id = :standardId AND stream_id = :streamId")
    suspend fun deleteScope(standardId: String, streamId: String)

    @Query(
        """
        SELECT c.* FROM rag_chapters AS c
        INNER JOIN rag_chapters_fts ON c.rowid = rag_chapters_fts.rowid
        WHERE rag_chapters_fts MATCH :ftsQuery
          AND c.standard_id = :standardId
          AND c.stream_id = :streamId
        LIMIT :limit
        """
    )
    suspend fun searchFtsInScope(
        ftsQuery: String,
        standardId: String,
        streamId: String,
        limit: Int = 12
    ): List<RagChapterEntity>
}

@Database(
    entities = [
        RagChapterEntity::class,
        RagChapterFtsEntity::class,
        CosmosChatSessionEntity::class,
        CosmosChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RagRoomDatabase : RoomDatabase() {
    abstract fun ragChapterDao(): RagChapterDao
    abstract fun cosmosChatDao(): CosmosChatDao

    companion object {
        private const val TAG = "RagRoomDatabase"
        const val ENCRYPTED_DB_NAME = "rag_room_vault_enc.db"
        private const val KEY_PREFS = "cosmium_rag_room_key_v1"
        private const val KEY_PASSPHRASE = "rag_room_passphrase_b64"

        @Volatile
        private var INSTANCE: RagRoomDatabase? = null

        @Volatile
        var isSqlCipherActive: Boolean = false
            private set

        fun getInstance(context: Context): RagRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        internal fun resetInstanceForTesting() {
            synchronized(this) {
                runCatching { INSTANCE?.close() }
                INSTANCE = null
            }
        }

        private fun buildDatabase(appContext: Context): RagRoomDatabase {
            val builder = Room.databaseBuilder(
                appContext,
                RagRoomDatabase::class.java,
                ENCRYPTED_DB_NAME
            ).fallbackToDestructiveMigration()

            val factory = SqlCipherFactoryProvider.createFactoryOrNull(
                appContext = appContext,
                prefsName = KEY_PREFS,
                passphraseKey = KEY_PASSPHRASE
            )
            if (factory != null) {
                builder.openHelperFactory(factory)
                isSqlCipherActive = true
            } else {
                isSqlCipherActive = false
            }

            return builder.build()
        }

        internal fun getOrCreateDatabasePassphrase(appContext: Context): ByteArray =
            SqlCipherFactoryProvider.getOrCreatePassphrase(
                appContext = appContext,
                prefsName = KEY_PREFS,
                passphraseKey = KEY_PASSPHRASE
            )
    }
}
