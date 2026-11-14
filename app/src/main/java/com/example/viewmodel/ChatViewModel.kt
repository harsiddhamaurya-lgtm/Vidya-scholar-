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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private var activeLoadedUserId: String? = null
    private var chatsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var messagesListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadChats(userId: String) {
        if (userId.isBlank() || userId.startsWith("demo_") || userId == activeLoadedUserId) return
        activeLoadedUserId = userId
        chatsListener?.remove()
        chatsListener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "chats")
                    return@addSnapshotListener
                }
                val remote = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(id = doc.id)
                }.orEmpty()
                if (remote.isNotEmpty()) {
                    val merged = (remote + _chats.value.filter { local -> remote.none { it.id == local.id } })
                        .sortedByDescending { it.lastTimestamp }
                    _chats.value = merged
                }
            }
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
        localMessagesMap[newId] = mutableListOf(
            ChatMessage(
                senderId = currentUid,
                senderName = currentProfile?.name ?: "Scholar",
                senderVidyaId = currentProfile?.effectiveVidyaId() ?: "",
                memberUids = participants,
                text = openingMsg,
                isUser = true,
                timestamp = now
            )
        )

        persistChatToFirestore(currentUid, newChat)
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
        localMessagesMap[newId] = mutableListOf(
            ChatMessage(
                senderId = effectiveUid,
                senderName = currentProfile?.name ?: "Scholar",
                senderVidyaId = currentProfile?.effectiveVidyaId().orEmpty(),
                memberUids = listOf(effectiveUid),
                text = openingMsg,
                isUser = true,
                timestamp = now
            )
        )

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

        persistChatToFirestore(effectiveUid, newChat)
        return newId
    }

    private fun persistChatToFirestore(uid: String, chat: Chat) {
        if (uid.isBlank() || uid.startsWith("demo_")) return
        viewModelScope.launch {
            try {
                val payload = mapOf(
                    "createdBy" to chat.createdBy,
                    "participants" to chat.participants,
                    "name" to (chat.name ?: "Varta Thread"),
                    "peerVidyaId" to chat.peerVidyaId,
                    "lastMessage" to chat.lastMessage,
                    "lastTimestamp" to chat.lastTimestamp,
                    "type" to chat.type,
                    "subjectTag" to chat.subjectTag,
                    "unreadCount" to chat.unreadCount,
                    "isOnline" to chat.isOnline,
                    "timestampLabel" to chat.timestampLabel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("chats").document(chat.id).set(payload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "chats/${chat.id}")
            }
        }
    }

    fun loadMessages(chatId: String, currentUserId: String = "") {
        markChatAsRead(chatId)
        _messages.value = localMessagesMap[chatId]?.toList() ?: emptyList()

        if (currentUserId.isBlank() || currentUserId.startsWith("demo_")) return
        messagesListener?.remove()
        messagesListener = db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("memberUids", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "chats/$chatId/messages")
                    return@addSnapshotListener
                }
                val remote = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.timestamp }.orEmpty()
                if (remote.isNotEmpty()) {
                    _messages.value = remote
                }
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
        val participants = getChatById(chatId)?.participants?.ifEmpty { listOf(effectiveSender) }
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
                    val msgPayload = mapOf(
                        "senderId" to effectiveSender,
                        "senderName" to senderName.take(60),
                        "senderVidyaId" to senderVidyaId.take(24),
                        "memberUids" to participants,
                        "text" to trimmed.take(2000),
                        "timestamp" to now,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("chats").document(chatId)
                        .collection("messages").document(message.id)
                        .set(msgPayload).await()
                    db.collection("chats").document(chatId)
                        .update(
                            mapOf(
                                "lastMessage" to trimmed.take(1000),
                                "lastTimestamp" to now,
                                "timestampLabel" to "Just now",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
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
