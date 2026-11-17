package com.example.rag

import android.content.Context
import android.util.Log
import com.example.auth.AuthRepository
import com.example.data.ChatMessage
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository orchestrating offline-first Cosmos AI chat history:
 * 1. Immediate local storage and lazy loading via encrypted Room database ([RagRoomDatabase]).
 * 2. Pinning, unpinning, and deletion of conversation sessions.
 * 3. Bidirectional background cloud synchronization to Firebase Firestore when online.
 */
class CosmosChatRepository(
    private val context: Context,
    private val roomDb: RagRoomDatabase = RagRoomDatabase.getInstance(context),
    private val authRepo: AuthRepository = AuthRepository(),
    private val firestoreProvider: () -> FirebaseFirestore? = {
        runCatching {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }.getOrNull()
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val TAG = "CosmosChatRepository"
    private val chatDao: CosmosChatDao = roomDb.cosmosChatDao()

    private val _activeSessionId = MutableStateFlow<String>(DEFAULT_SESSION_ID)
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        const val DEFAULT_SESSION_ID = "cosmos_active_session"
        const val GUEST_USER_ID = "guest_scholar"
    }

    fun getCurrentUserId(): String {
        return authRepo.currentUser?.uid ?: GUEST_USER_ID
    }

    fun setActiveSession(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun observeUserSessions(userId: String = getCurrentUserId()): Flow<List<CosmosChatSessionEntity>> {
        return chatDao.observeSessions(userId)
    }

    suspend fun getUserSessions(userId: String = getCurrentUserId()): List<CosmosChatSessionEntity> {
        return withContext(ioDispatcher) {
            chatDao.getSessions(userId)
        }
    }

    fun observeSessionMessages(sessionId: String): Flow<List<CosmosChatMessageEntity>> {
        return chatDao.observeMessagesForSession(sessionId)
    }

    suspend fun getSessionMessages(sessionId: String): List<CosmosChatMessageEntity> {
        return withContext(ioDispatcher) {
            chatDao.getMessagesForSession(sessionId)
        }
    }

    /**
     * Lazy-loads messages for a specific session with pagination.
     */
    suspend fun getMessagesPaged(sessionId: String, limit: Int = 30, offset: Int = 0): List<CosmosChatMessageEntity> {
        return withContext(ioDispatcher) {
            chatDao.getMessagesPaged(sessionId, limit, offset)
        }
    }

    suspend fun getMessageCount(sessionId: String): Int {
        return withContext(ioDispatcher) {
            chatDao.getMessageCount(sessionId)
        }
    }

    /**
     * Creates a new conversation session, saving it to local Room database.
     */
    suspend fun createNewSession(
        standardId: String,
        streamId: String,
        title: String = "Cosmos Conversation",
        userId: String = getCurrentUserId()
    ): CosmosChatSessionEntity {
        return withContext(ioDispatcher) {
            val newSession = CosmosChatSessionEntity(
                sessionId = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                standardId = standardId,
                streamId = streamId,
                isPinned = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessageText = "",
                messageCount = 0,
                isSynced = false,
                isDeleted = false
            )
            chatDao.upsertSession(newSession)
            _activeSessionId.value = newSession.sessionId
            newSession
        }
    }

    /**
     * Appends a message to the active or target session and updates last message preview.
     */
    suspend fun saveMessage(
        sessionId: String,
        message: ChatMessage,
        standardId: String,
        streamId: String,
        userId: String = getCurrentUserId()
    ): CosmosChatMessageEntity {
        return withContext(ioDispatcher) {
            val sourcesJoined = message.ragSources.joinToString("||")
            val entity = CosmosChatMessageEntity(
                messageId = message.id,
                sessionId = sessionId,
                text = message.text,
                isUser = message.isUser,
                timestamp = System.currentTimeMillis(),
                ragSourcesJson = sourcesJoined,
                isSynced = false
            )
            chatDao.upsertMessage(entity)

            // Update or create parent session
            val existing = chatDao.getSessionById(sessionId)
            val updatedTitle = if (existing == null || existing.title == "Cosmos Conversation" || existing.title.isBlank()) {
                if (message.isUser) {
                    message.text.take(45).let { if (message.text.length > 45) "$it..." else it }
                } else {
                    existing?.title ?: "Cosmos Study Session"
                }
            } else {
                existing.title
            }

            val sessionToSave = existing?.copy(
                title = updatedTitle,
                updatedAt = System.currentTimeMillis(),
                lastMessageText = message.text.take(120),
                messageCount = existing.messageCount + 1,
                isSynced = false
            ) ?: CosmosChatSessionEntity(
                sessionId = sessionId,
                userId = userId,
                title = updatedTitle,
                standardId = standardId,
                streamId = streamId,
                isPinned = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessageText = message.text.take(120),
                messageCount = 1,
                isSynced = false,
                isDeleted = false
            )
            chatDao.upsertSession(sessionToSave)

            entity
        }
    }

    /**
     * Pins or unpins a chat session, moving pinned sessions to the top of the history list.
     */
    suspend fun setChatPinned(sessionId: String, isPinned: Boolean) {
        withContext(ioDispatcher) {
            chatDao.updatePinStatus(sessionId, isPinned)
        }
    }

    /**
     * Soft-deletes a chat session locally and queues it for deletion on the server.
     */
    suspend fun deleteSession(sessionId: String) {
        withContext(ioDispatcher) {
            chatDao.markSessionDeleted(sessionId)
            chatDao.hardDeleteMessages(sessionId)
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = UUID.randomUUID().toString()
            }
        }
    }

    /**
     * Synchronizes pending unsynced sessions and messages to Firestore when online.
     */
    suspend fun syncPendingToCloud(userId: String = getCurrentUserId()): Boolean {
        if (userId == GUEST_USER_ID) return false
        val firestore = firestoreProvider() ?: return false

        return withContext(ioDispatcher) {
            if (_isSyncing.value) return@withContext false
            _isSyncing.value = true
            try {
                // 1. Sync pending sessions
                val unsyncedSessions = chatDao.getUnsyncedSessions(userId)
                val deletedIds = mutableSetOf<String>()
                for (session in unsyncedSessions) {
                    val sessionDocRef = firestore.collection("users")
                        .document(userId)
                        .collection("cosmos_chats")
                        .document(session.sessionId)

                    if (session.isDeleted) {
                        deletedIds.add(session.sessionId)
                        // Clean up subcollection messages on remote before deleting parent
                        runCatching {
                            val subMsgs = sessionDocRef.collection("messages").get().await()
                            for (doc in subMsgs.documents) {
                                doc.reference.delete().await()
                            }
                        }
                        sessionDocRef.delete().await()
                        chatDao.hardDeleteMessages(session.sessionId)
                        chatDao.hardDeleteSession(session.sessionId)
                    } else {
                        val sessionData = hashMapOf(
                            "sessionId" to session.sessionId,
                            "userId" to userId,
                            "title" to session.title,
                            "standardId" to session.standardId,
                            "streamId" to session.streamId,
                            "isPinned" to session.isPinned,
                            "createdAt" to session.createdAt,
                            "updatedAt" to session.updatedAt,
                            "lastMessageText" to session.lastMessageText,
                            "messageCount" to session.messageCount,
                            "isDeleted" to session.isDeleted
                        )
                        sessionDocRef.set(sessionData, SetOptions.merge()).await()
                        chatDao.markSessionSynced(session.sessionId)
                    }
                }

                // 2. Sync pending messages (excluding any from deleted sessions)
                val unsyncedMessages = chatDao.getUnsyncedMessages()
                    .filter { it.sessionId !in deletedIds }
                for (msg in unsyncedMessages) {
                    val msgDocRef = firestore.collection("users")
                        .document(userId)
                        .collection("cosmos_chats")
                        .document(msg.sessionId)
                        .collection("messages")
                        .document(msg.messageId)

                    val sourcesList = msg.ragSourcesJson
                        .split("||")
                        .filter { it.isNotBlank() }

                    val msgData = hashMapOf(
                        "messageId" to msg.messageId,
                        "sessionId" to msg.sessionId,
                        "text" to msg.text,
                        "isUser" to msg.isUser,
                        "timestamp" to msg.timestamp,
                        "ragSources" to sourcesList
                    )
                    msgDocRef.set(msgData, SetOptions.merge()).await()
                    chatDao.markMessageSynced(msg.messageId)
                }

                // 3. Download any remote sessions not present locally (e.g. multi-device sync)
                restoreRemoteSessions(userId, firestore)

                Log.d(TAG, "Successfully synced ${unsyncedSessions.size} sessions and ${unsyncedMessages.size} messages to cloud.")
                true
            } catch (e: Exception) {
                val jsonError = handleFirestoreError(e, OperationType.WRITE, "users/$userId/cosmos_chats")
                Log.w(TAG, "Cosmos chat sync paused or failed: $jsonError")
                false
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun restoreRemoteSessions(userId: String, firestore: FirebaseFirestore) {
        runCatching {
            val remoteDocs = firestore.collection("users")
                .document(userId)
                .collection("cosmos_chats")
                .get()
                .await()

            for (doc in remoteDocs.documents) {
                val sid = doc.getString("sessionId") ?: doc.id
                val isDel = doc.getBoolean("isDeleted") ?: false
                if (isDel) continue

                val local = chatDao.getSessionById(sid)
                if (local == null) {
                    val remoteSession = CosmosChatSessionEntity(
                        sessionId = sid,
                        userId = userId,
                        title = doc.getString("title") ?: "Saved Session",
                        standardId = doc.getString("standardId") ?: "class_11",
                        streamId = doc.getString("streamId") ?: "science_pcm",
                        isPinned = doc.getBoolean("isPinned") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        lastMessageText = doc.getString("lastMessageText") ?: "",
                        messageCount = (doc.getLong("messageCount") ?: 0L).toInt(),
                        isSynced = true,
                        isDeleted = false
                    )
                    chatDao.upsertSession(remoteSession)

                    // Fetch messages for this remote session
                    val msgDocs = doc.reference.collection("messages").get().await()
                    val remoteMessages = msgDocs.documents.mapNotNull { mDoc ->
                        val mid = mDoc.getString("messageId") ?: mDoc.id
                        val txt = mDoc.getString("text") ?: return@mapNotNull null
                        val isUser = mDoc.getBoolean("isUser") ?: false
                        val ts = mDoc.getLong("timestamp") ?: System.currentTimeMillis()
                        @Suppress("UNCHECKED_CAST")
                        val srcList = (mDoc.get("ragSources") as? List<String>) ?: emptyList()
                        CosmosChatMessageEntity(
                            messageId = mid,
                            sessionId = sid,
                            text = txt,
                            isUser = isUser,
                            timestamp = ts,
                            ragSourcesJson = srcList.joinToString("||"),
                            isSynced = true
                        )
                    }
                    if (remoteMessages.isNotEmpty()) {
                        chatDao.upsertMessages(remoteMessages)
                    }
                }
            }
        }
    }
}
