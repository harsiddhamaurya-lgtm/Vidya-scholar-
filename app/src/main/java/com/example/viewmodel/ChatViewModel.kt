package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Chat
import com.example.data.ChatMessage
import com.example.data.OperationType
import com.example.data.PracticePeersCatalog
import com.example.data.UserProfile
import com.example.data.VidyaIdentityCard
import com.example.data.handleFirestoreError
import com.example.data.repository.ScholarIdentityRepository
import com.example.data.repository.VidyaIdFormatter
import com.example.security.KeystoreCryptoManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

data class LiveStudySabha(
    val id: String,
    val title: String,
    val subjectTag: String,
    val activePeersCount: Int,
    val peerInitials: List<String>,
    val hostSummary: String
)

class ChatViewModel(private val db: FirebaseFirestore) : ViewModel() {

    private val ananyaCard = PracticePeersCatalog.sampleScholars[0]
    private val rohanCard = PracticePeersCatalog.sampleScholars[1]

    private val defaultStudyThreads = listOf(
        Chat(
            id = "peer_ananya",
            createdBy = "ananya_01",
            participants = listOf("demo_student_123", "ananya_01"),
            name = ananyaCard.displayName,
            peerVidyaId = ananyaCard.vidyaId,
            peerAvatarSpec = ananyaCard.toAvatarSpec(),
            lastMessage = "Shared the Lagrangian derivation sheet for tonight's quiz!",
            lastTimestamp = System.currentTimeMillis() - 180_000L,
            type = "individual",
            subjectTag = "Physics",
            unreadCount = 2,
            isOnline = true,
            timestampLabel = "3m"
        ),
        Chat(
            id = "peer_rohan",
            createdBy = "rohan_02",
            participants = listOf("demo_student_123", "rohan_02"),
            name = rohanCard.displayName,
            peerVidyaId = rohanCard.vidyaId,
            peerAvatarSpec = rohanCard.toAvatarSpec(),
            lastMessage = "Let's test the Compose Canvas mandala animation together.",
            lastTimestamp = System.currentTimeMillis() - 900_000L,
            type = "individual",
            subjectTag = "Coding & AI",
            unreadCount = 1,
            isOnline = true,
            timestampLabel = "15m"
        ),
        Chat(
            id = "room_chem_synthesis",
            createdBy = "meera_03",
            participants = listOf("demo_student_123"),
            name = "Organic Synthesis & Mechanisms",
            peerVidyaId = "meera#5510",
            lastMessage = "Meera: Notice how the nucleophile attacks the less hindered carbon.",
            lastTimestamp = System.currentTimeMillis() - 120_000L,
            type = "group",
            subjectTag = "Chemistry",
            unreadCount = 4,
            isOnline = true,
            timestampLabel = "2m"
        ),
        Chat(
            id = "room_indo_arch",
            createdBy = "kabir_04",
            participants = listOf("demo_student_123"),
            name = "Sacred Geometry & Jali Proportions",
            peerVidyaId = "kabir#7319",
            lastMessage = "Kabir: Uploaded the 60-30-10 temple courtyard symmetry grid.",
            lastTimestamp = System.currentTimeMillis() - 1_500_000L,
            type = "group",
            subjectTag = "Architecture",
            unreadCount = 0,
            isOnline = true,
            timestampLabel = "25m"
        ),
        Chat(
            id = "room_general_sabha",
            createdBy = "kabir_04",
            participants = listOf("demo_student_123"),
            name = "Evening Pomodoro Study Sabha",
            peerVidyaId = "sabha#0108",
            lastMessage = "Round 2 focus sprint starts in 5 mins — drop your goal!",
            lastTimestamp = System.currentTimeMillis() - 3_600_000L,
            type = "group",
            subjectTag = "General",
            unreadCount = 1,
            isOnline = false,
            timestampLabel = "1h"
        )
    )

    private val defaultLiveSabhas = listOf(
        LiveStudySabha(
            id = "room_chem_synthesis",
            title = "Organic Synthesis Lab",
            subjectTag = "Chemistry",
            activePeersCount = 4,
            peerInitials = listOf("M", "A", "K"),
            hostSummary = "Reaction pathways & stereochemistry"
        ),
        LiveStudySabha(
            id = "room_indo_arch",
            title = "Ashtapada & Jali Design",
            subjectTag = "Architecture",
            activePeersCount = 6,
            peerInitials = listOf("K", "R", "S"),
            hostSummary = "Sacred ratio & lattice sketching"
        ),
        LiveStudySabha(
            id = "room_general_sabha",
            title = "Pomodoro Deep Focus",
            subjectTag = "General",
            activePeersCount = 9,
            peerInitials = listOf("P", "V", "D"),
            hostSummary = "50/10 silent peer accountability"
        )
    )

    private val localMessagesMap = mutableMapOf(
        "peer_ananya" to mutableListOf(
            ChatMessage(
                senderId = "ananya_01",
                senderName = ananyaCard.displayName,
                senderVidyaId = ananyaCard.vidyaId,
                text = "Hey! Have you reviewed the Euler-Lagrange problem set yet?",
                isUser = false,
                timestamp = System.currentTimeMillis() - 600_000L
            ),
            ChatMessage(
                senderId = "ananya_01",
                senderName = ananyaCard.displayName,
                senderVidyaId = ananyaCard.vidyaId,
                text = "Shared the Lagrangian derivation sheet for tonight's quiz!",
                isUser = false,
                timestamp = System.currentTimeMillis() - 180_000L
            )
        ),
        "peer_rohan" to mutableListOf(
            ChatMessage(
                senderId = "rohan_02",
                senderName = rohanCard.displayName,
                senderVidyaId = rohanCard.vidyaId,
                text = "Let's test the Compose Canvas mandala animation together.",
                isUser = false,
                timestamp = System.currentTimeMillis() - 900_000L
            )
        ),
        "room_chem_synthesis" to mutableListOf(
            ChatMessage(
                senderId = "meera_03",
                senderName = "Meera Nair",
                senderVidyaId = "meera#5510",
                text = "Notice how the nucleophile attacks the less hindered carbon.",
                isUser = false,
                timestamp = System.currentTimeMillis() - 120_000L
            )
        ),
        "room_indo_arch" to mutableListOf(
            ChatMessage(
                senderId = "kabir_04",
                senderName = "Kabir Sen",
                senderVidyaId = "kabir#7319",
                text = "Uploaded the 60-30-10 temple courtyard symmetry grid.",
                isUser = false,
                timestamp = System.currentTimeMillis() - 1_500_000L
            )
        ),
        "room_general_sabha" to mutableListOf(
            ChatMessage(
                senderId = "priya_05",
                senderName = "Priya Patel",
                senderVidyaId = "priya#1204",
                text = "Round 2 focus sprint starts in 5 mins — drop your goal!",
                isUser = false,
                timestamp = System.currentTimeMillis() - 3_600_000L
            )
        )
    )

    private val _chats = MutableStateFlow<List<Chat>>(defaultStudyThreads)
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _liveSabhas = MutableStateFlow<List<LiveStudySabha>>(defaultLiveSabhas)
    val liveSabhas: StateFlow<List<LiveStudySabha>> = _liveSabhas.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // In-memory cache of unwrapped AES-256 thread session DEKs: key = "${chatId}:${keyVersion}"
    private val sessionKeyCache = ConcurrentHashMap<String, SecretKey>()

    private var activeLoadedUserId: String? = null
    private var chatsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var messagesListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadChats(userId: String) {
        if (userId.isBlank() || userId.startsWith("demo_") || userId == activeLoadedUserId) return
        activeLoadedUserId = userId
        KeystoreCryptoManager.ensureIdentityKeyPair(userId)

        chatsListener?.remove()
        chatsListener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "chats")
                    return@addSnapshotListener
                }
                val remote = snapshot?.documents?.mapNotNull { doc ->
                    val rawChat = doc.toObject(Chat::class.java)?.copy(id = doc.id) ?: return@mapNotNull null
                    decryptChatSummary(rawChat, userId)
                }.orEmpty()

                if (remote.isNotEmpty()) {
                    val merged = (remote + _chats.value.filter { local -> remote.none { it.id == local.id } })
                        .sortedByDescending { it.lastTimestamp }
                    _chats.value = merged
                }
            }
    }

    private fun decryptChatSummary(chat: Chat, uid: String): Chat {
        val sessionKey = resolveChatSessionKey(chat, uid)
        if (!chat.isEncrypted || chat.lastMessageIv.isBlank()) {
            return chat
        }
        val plainPreview = if (sessionKey != null) {
            KeystoreCryptoManager.decryptWithSessionKey(chat.lastMessage, chat.lastMessageIv, sessionKey)
                ?: "[Encrypted message]"
        } else {
            "[Encrypted message]"
        }
        return chat.copy(lastMessage = plainPreview)
    }

    private fun resolveChatSessionKey(chat: Chat, uid: String): SecretKey? {
        val cacheKey = "${chat.id}:${chat.keyVersion}"
        sessionKeyCache[cacheKey]?.let { return it }

        val wrappedForMe = chat.wrappedKeys[uid] ?: return null
        val unwrapped = KeystoreCryptoManager.unwrapSessionKey(uid, wrappedForMe) ?: return null
        sessionKeyCache[cacheKey] = unwrapped
        return unwrapped
    }

    fun markChatAsRead(chatId: String) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) chat.copy(unreadCount = 0) else chat
        }
    }

    fun getChatById(chatId: String): Chat? = _chats.value.find { it.id == chatId }

    fun createThreadFromPeer(
        currentProfile: UserProfile?,
        peerCard: VidyaIdentityCard,
        subjectTag: String
    ): String {
        val currentUid = currentProfile?.uid?.ifBlank { "demo_student_123" } ?: "demo_student_123"
        val existing = _chats.value.firstOrNull {
            it.type == "individual" && it.peerVidyaId.equals(peerCard.vidyaId, ignoreCase = true)
        }
        if (existing != null) return existing.id

        val newId = "peer_${peerCard.vidyaHandle}_${System.currentTimeMillis()}"
        val participants = listOf(currentUid, peerCard.uid).distinct()
        val openingMsg = "Namaste ${peerCard.displayName} (${peerCard.vidyaId})! Let's collaborate on $subjectTag."
        val now = System.currentTimeMillis()
        val newChat = Chat(
            id = newId,
            createdBy = currentUid,
            participants = participants,
            name = peerCard.displayName,
            peerVidyaId = peerCard.vidyaId,
            peerAvatarSpec = peerCard.toAvatarSpec(),
            lastMessage = openingMsg,
            lastTimestamp = now,
            type = "individual",
            subjectTag = subjectTag,
            unreadCount = 0,
            isOnline = true,
            timestampLabel = "Just now"
        )
        _chats.value = listOf(newChat) + _chats.value
        val openingChatMessage = ChatMessage(
            senderId = currentUid,
            senderName = currentProfile?.name ?: "Scholar",
            senderVidyaId = currentProfile?.effectiveVidyaId() ?: "",
            memberUids = participants,
            text = openingMsg,
            isUser = true,
            timestamp = now
        )
        localMessagesMap[newId] = mutableListOf(openingChatMessage)

        persistEncryptedChatToFirestore(
            uid = currentUid,
            chat = newChat,
            openingMessage = openingChatMessage,
            peerPublicKeyHint = peerCard.publicKey
        )
        return newId
    }

    fun createThread(
        userId: String,
        title: String,
        subjectTag: String,
        isGroupRoom: Boolean,
        currentProfile: UserProfile? = null
    ): String {
        val cleanTitle = title.trim().ifBlank { if (isGroupRoom) "New Study Sabha" else "Scholar Peer" }
        val newId = (if (isGroupRoom) "room_" else "peer_") + System.currentTimeMillis()
        val effectiveUid = userId.ifBlank { "demo_student_123" }
        val openingMsg = if (isGroupRoom) {
            "Sabha opened for $subjectTag — share notes and questions!"
        } else {
            "Namaste! Started a $subjectTag study thread."
        }
        val now = System.currentTimeMillis()
        val newChat = Chat(
            id = newId,
            createdBy = effectiveUid,
            participants = listOf(effectiveUid),
            name = cleanTitle,
            peerVidyaId = if (isGroupRoom) "sabha#${(1000..9999).random()}" else currentProfile?.effectiveVidyaId().orEmpty(),
            lastMessage = openingMsg,
            lastTimestamp = now,
            type = if (isGroupRoom) "group" else "individual",
            subjectTag = subjectTag,
            unreadCount = 0,
            isOnline = true,
            timestampLabel = "Just now"
        )
        _chats.value = listOf(newChat) + _chats.value
        val openingChatMessage = ChatMessage(
            senderId = effectiveUid,
            senderName = currentProfile?.name ?: "Scholar",
            senderVidyaId = currentProfile?.effectiveVidyaId().orEmpty(),
            memberUids = listOf(effectiveUid),
            text = openingMsg,
            isUser = true,
            timestamp = now
        )
        localMessagesMap[newId] = mutableListOf(openingChatMessage)

        if (isGroupRoom) {
            _liveSabhas.value = listOf(
                LiveStudySabha(
                    id = newId,
                    title = cleanTitle,
                    subjectTag = subjectTag,
                    activePeersCount = 1,
                    peerInitials = listOf(cleanTitle.take(1).uppercase()),
                    hostSummary = "Live peer study session"
                )
            ) + _liveSabhas.value
        }

        persistEncryptedChatToFirestore(
            uid = effectiveUid,
            chat = newChat,
            openingMessage = openingChatMessage
        )
        return newId
    }

    private fun persistEncryptedChatToFirestore(
        uid: String,
        chat: Chat,
        openingMessage: ChatMessage,
        peerPublicKeyHint: String = ""
    ) {
        if (uid.isBlank() || uid.startsWith("demo_")) return
        viewModelScope.launch {
            try {
                val myPubKey = KeystoreCryptoManager.ensureIdentityKeyPair(uid)
                val sessionKey = KeystoreCryptoManager.generateSessionAesKey()
                sessionKeyCache["${chat.id}:1"] = sessionKey

                val wrappedKeys = mutableMapOf<String, String>()
                if (myPubKey.isNotBlank()) {
                    val wrappedSelf = KeystoreCryptoManager.wrapSessionKey(sessionKey, myPubKey)
                    if (wrappedSelf.isNotBlank()) wrappedKeys[uid] = wrappedSelf
                }

                // Resolve peer public key from hint or Vidya registry if missing
                val peerUid = chat.participants.firstOrNull { it != uid }
                var resolvedPeerPub = peerPublicKeyHint
                if (resolvedPeerPub.isBlank() && peerUid != null && chat.peerVidyaId.contains("#")) {
                    val parsed = VidyaIdFormatter.parseVidyaId(chat.peerVidyaId)
                    if (parsed != null) {
                        val keyDoc = VidyaIdFormatter.toDocumentKey(parsed.first, parsed.second)
                        resolvedPeerPub = runCatching {
                            db.collection(ScholarIdentityRepository.COLLECTION_VIDYA_IDS)
                                .document(keyDoc)
                                .get()
                                .await()
                                .getString("publicKey")
                                .orEmpty()
                        }.getOrDefault("")
                    }
                }
                if (peerUid != null && resolvedPeerPub.isNotBlank()) {
                    val wrappedPeer = KeystoreCryptoManager.wrapSessionKey(sessionKey, resolvedPeerPub)
                    if (wrappedPeer.isNotBlank()) wrappedKeys[peerUid] = wrappedPeer
                }

                val (encPreview, previewIv) = KeystoreCryptoManager.encryptWithSessionKey(chat.lastMessage, sessionKey)
                val chatPayload = mapOf(
                    "createdBy" to chat.createdBy,
                    "participants" to chat.participants,
                    "name" to (chat.name ?: "Varta Thread"),
                    "peerVidyaId" to chat.peerVidyaId,
                    "lastMessage" to encPreview,
                    "lastMessageIv" to previewIv,
                    "lastTimestamp" to chat.lastTimestamp,
                    "type" to chat.type,
                    "subjectTag" to chat.subjectTag,
                    "unreadCount" to chat.unreadCount,
                    "isOnline" to chat.isOnline,
                    "timestampLabel" to chat.timestampLabel,
                    "wrappedKeys" to wrappedKeys,
                    "keyVersion" to 1,
                    "isEncrypted" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("chats").document(chat.id).set(chatPayload).await()

                val (msgCipher, msgIv) = KeystoreCryptoManager.encryptWithSessionKey(openingMessage.text, sessionKey)
                val msgPayload = mapOf(
                    "senderId" to uid,
                    "senderName" to openingMessage.senderName.take(60),
                    "senderVidyaId" to openingMessage.senderVidyaId.take(24),
                    "memberUids" to chat.participants,
                    "text" to "[Encrypted]",
                    "ciphertext" to msgCipher,
                    "iv" to msgIv,
                    "keyVersion" to 1,
                    "isEncrypted" to true,
                    "timestamp" to openingMessage.timestamp,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("chats").document(chat.id)
                    .collection("messages").document(openingMessage.id)
                    .set(msgPayload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "chats/${chat.id}")
            }
        }
    }

    fun loadMessages(chatId: String, currentUserId: String = "") {
        markChatAsRead(chatId)
        _messages.value = localMessagesMap[chatId]?.toList() ?: emptyList()

        if (currentUserId.isBlank() || currentUserId.startsWith("demo_")) return
        KeystoreCryptoManager.ensureIdentityKeyPair(currentUserId)

        messagesListener?.remove()
        messagesListener = db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("memberUids", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "chats/$chatId/messages")
                    return@addSnapshotListener
                }
                val rawDocs = snapshot?.documents.orEmpty()
                if (rawDocs.isEmpty()) return@addSnapshotListener

                viewModelScope.launch {
                    val chat = ensureChatSessionKeyLoaded(chatId, currentUserId)
                    val remote = rawDocs.mapNotNull { doc ->
                        val msg = doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                            ?: return@mapNotNull null
                        val isMe = msg.senderId == currentUserId
                        if (msg.isEncrypted && msg.ciphertext.isNotBlank() && msg.iv.isNotBlank()) {
                            val key = sessionKeyCache["$chatId:${msg.keyVersion}"]
                                ?: chat?.let { resolveChatSessionKey(it, currentUserId) }
                            val decrypted = if (key != null) {
                                KeystoreCryptoManager.decryptWithSessionKey(msg.ciphertext, msg.iv, key)
                            } else {
                                null
                            }
                            msg.copy(text = decrypted ?: "[Encrypted message]", isUser = isMe)
                        } else {
                            msg.copy(isUser = isMe)
                        }
                    }.sortedBy { it.timestamp }

                    if (remote.isNotEmpty()) {
                        _messages.value = remote
                    }
                }
            }
    }

    private suspend fun ensureChatSessionKeyLoaded(chatId: String, uid: String): Chat? {
        val cachedChat = getChatById(chatId)
        if (cachedChat != null && sessionKeyCache.containsKey("$chatId:${cachedChat.keyVersion}")) {
            return cachedChat
        }
        return try {
            val snap = db.collection("chats").document(chatId).get().await()
            val remoteChat = snap.toObject(Chat::class.java)?.copy(id = snap.id) ?: return cachedChat
            resolveChatSessionKey(remoteChat, uid)
            remoteChat
        } catch (_: Exception) {
            cachedChat
        }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        text: String,
        senderName: String = "Scholar",
        senderVidyaId: String = ""
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val effectiveSender = senderId.ifBlank { "demo_student_123" }
        val chatObj = getChatById(chatId)
        val participants = chatObj?.participants?.ifEmpty { listOf(effectiveSender) }
            ?: listOf(effectiveSender)
        val now = System.currentTimeMillis()
        val message = ChatMessage(
            senderId = effectiveSender,
            senderName = senderName,
            senderVidyaId = senderVidyaId,
            memberUids = participants,
            text = trimmed,
            isUser = true,
            timestamp = now
        )

        val currentList = localMessagesMap.getOrPut(chatId) { mutableListOf() }
        currentList.add(message)
        _messages.value = currentList.toList()

        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(
                    lastMessage = trimmed,
                    lastTimestamp = now,
                    timestampLabel = "Just now",
                    unreadCount = 0
                )
            } else chat
        }

        if (!effectiveSender.startsWith("demo_")) {
            viewModelScope.launch {
                val msgPath = "chats/$chatId/messages/${message.id}"
                try {
                    val remoteChat = ensureChatSessionKeyLoaded(chatId, effectiveSender)
                    var keyVersion = remoteChat?.keyVersion ?: 1
                    var sessionKey = sessionKeyCache["$chatId:$keyVersion"]
                        ?: remoteChat?.let { resolveChatSessionKey(it, effectiveSender) }

                    var newWrappedKeys: Map<String, String>? = null
                    if (sessionKey == null) {
                        sessionKey = KeystoreCryptoManager.generateSessionAesKey()
                        keyVersion = (remoteChat?.keyVersion ?: 0).coerceAtLeast(1)
                        sessionKeyCache["$chatId:$keyVersion"] = sessionKey
                        val myPub = KeystoreCryptoManager.ensureIdentityKeyPair(effectiveSender)
                        if (myPub.isNotBlank()) {
                            val wrappedSelf = KeystoreCryptoManager.wrapSessionKey(sessionKey, myPub)
                            if (wrappedSelf.isNotBlank()) {
                                val merged = remoteChat?.wrappedKeys.orEmpty().toMutableMap()
                                merged[effectiveSender] = wrappedSelf
                                newWrappedKeys = merged
                            }
                        }
                    }

                    val (msgCipher, msgIv) = KeystoreCryptoManager.encryptWithSessionKey(trimmed.take(2000), sessionKey)
                    val (previewCipher, previewIv) = KeystoreCryptoManager.encryptWithSessionKey(trimmed.take(1000), sessionKey)

                    val msgPayload = mapOf(
                        "senderId" to effectiveSender,
                        "senderName" to senderName.take(60),
                        "senderVidyaId" to senderVidyaId.take(24),
                        "memberUids" to participants,
                        "text" to "[Encrypted]",
                        "ciphertext" to msgCipher,
                        "iv" to msgIv,
                        "keyVersion" to keyVersion,
                        "isEncrypted" to true,
                        "timestamp" to now,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("chats").document(chatId)
                        .collection("messages").document(message.id)
                        .set(msgPayload).await()

                    val chatUpdate = mutableMapOf<String, Any>(
                        "lastMessage" to previewCipher,
                        "lastMessageIv" to previewIv,
                        "keyVersion" to keyVersion,
                        "isEncrypted" to true,
                        "lastTimestamp" to now,
                        "timestampLabel" to "Just now",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (newWrappedKeys != null) {
                        chatUpdate["wrappedKeys"] = newWrappedKeys
                    }
                    db.collection("chats").document(chatId)
                        .update(chatUpdate).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, msgPath)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        messagesListener?.remove()
    }
}
