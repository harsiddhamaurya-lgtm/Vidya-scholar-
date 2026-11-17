package com.example.rag

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room entity representing an offline Cosmos chat conversation session.
 * Stores metadata, pinning state, deletion flag, and cloud sync status.
 */
@Entity(
    tableName = "cosmos_chat_sessions",
    indices = [
        Index(value = ["session_id"], unique = true),
        Index(value = ["user_id", "is_deleted", "is_pinned", "updated_at"]),
        Index(value = ["is_synced"])
    ]
)
data class CosmosChatSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "standard_id")
    val standardId: String,

    @ColumnInfo(name = "stream_id")
    val streamId: String,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_message_text")
    val lastMessageText: String = "",

    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)

/**
 * Room entity representing an individual chat message in a Cosmos session.
 */
@Entity(
    tableName = "cosmos_chat_messages",
    indices = [
        Index(value = ["message_id"], unique = true),
        Index(value = ["session_id", "timestamp"]),
        Index(value = ["is_synced"])
    ]
)
data class CosmosChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "is_user")
    val isUser: Boolean,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "rag_sources_json")
    val ragSourcesJson: String = "",

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)

@Dao
interface CosmosChatDao {

    @Query(
        """
        SELECT * FROM cosmos_chat_sessions
        WHERE user_id = :userId AND is_deleted = 0
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    fun observeSessions(userId: String): Flow<List<CosmosChatSessionEntity>>

    @Query(
        """
        SELECT * FROM cosmos_chat_sessions
        WHERE user_id = :userId AND is_deleted = 0
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    suspend fun getSessions(userId: String): List<CosmosChatSessionEntity>

    @Query(
        """
        SELECT * FROM cosmos_chat_sessions
        WHERE session_id = :sessionId LIMIT 1
        """
    )
    suspend fun getSessionById(sessionId: String): CosmosChatSessionEntity?

    @Query(
        """
        SELECT * FROM cosmos_chat_messages
        WHERE session_id = :sessionId
        ORDER BY timestamp ASC
        """
    )
    fun observeMessagesForSession(sessionId: String): Flow<List<CosmosChatMessageEntity>>

    @Query(
        """
        SELECT * FROM cosmos_chat_messages
        WHERE session_id = :sessionId
        ORDER BY timestamp ASC
        """
    )
    suspend fun getMessagesForSession(sessionId: String): List<CosmosChatMessageEntity>

    @Query(
        """
        SELECT * FROM cosmos_chat_messages
        WHERE session_id = :sessionId
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getMessagesPaged(sessionId: String, limit: Int, offset: Int): List<CosmosChatMessageEntity>

    @Query("SELECT COUNT(*) FROM cosmos_chat_messages WHERE session_id = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: CosmosChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<CosmosChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: CosmosChatMessageEntity)

    @Query(
        """
        UPDATE cosmos_chat_sessions
        SET is_pinned = :isPinned, is_synced = 0, updated_at = :updatedAt
        WHERE session_id = :sessionId
        """
    )
    suspend fun updatePinStatus(
        sessionId: String,
        isPinned: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE cosmos_chat_sessions
        SET is_deleted = 1, is_synced = 0, updated_at = :updatedAt
        WHERE session_id = :sessionId
        """
    )
    suspend fun markSessionDeleted(
        sessionId: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM cosmos_chat_sessions WHERE session_id = :sessionId")
    suspend fun hardDeleteSession(sessionId: String)

    @Query("DELETE FROM cosmos_chat_messages WHERE session_id = :sessionId")
    suspend fun hardDeleteMessages(sessionId: String)

    @Query(
        """
        SELECT * FROM cosmos_chat_sessions
        WHERE is_synced = 0 AND user_id = :userId
        """
    )
    suspend fun getUnsyncedSessions(userId: String): List<CosmosChatSessionEntity>

    @Query(
        """
        SELECT * FROM cosmos_chat_messages
        WHERE is_synced = 0
        """
    )
    suspend fun getUnsyncedMessages(): List<CosmosChatMessageEntity>

    @Query("UPDATE cosmos_chat_sessions SET is_synced = 1 WHERE session_id = :sessionId")
    suspend fun markSessionSynced(sessionId: String)

    @Query("UPDATE cosmos_chat_messages SET is_synced = 1 WHERE message_id = :messageId")
    suspend fun markMessageSynced(messageId: String)
}
