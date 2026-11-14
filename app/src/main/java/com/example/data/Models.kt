package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AvatarMode {
    PRESET,
    BUILDER,
    PHOTO
}

data class AvatarSpec(
    val mode: AvatarMode = AvatarMode.PRESET,
    val presetId: String = "vedic_strategist",
    val crestId: String = "yantra",
    val frameId: String = "astrolabe",
    val toneId: String = "saffron",
    val localPhotoPath: String = ""
) {
    companion object {
        fun fromStrings(
            mode: String,
            presetId: String,
            crestId: String,
            frameId: String,
            toneId: String,
            localPhotoPath: String = ""
        ): AvatarSpec {
            val resolvedMode = runCatching { AvatarMode.valueOf(mode.uppercase()) }
                .getOrDefault(AvatarMode.PRESET)
            return AvatarSpec(
                mode = resolvedMode,
                presetId = presetId.ifBlank { "vedic_strategist" },
                crestId = crestId.ifBlank { "yantra" },
                frameId = frameId.ifBlank { "astrolabe" },
                toneId = toneId.ifBlank { "saffron" },
                localPhotoPath = localPhotoPath
            )
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val name: String = "Scholar",
    val email: String = "",
    val photoUrl: String = "",
    val vidyaId: String = "",
    val vidyaHandle: String = "",
    val vidyaDiscriminator: String = "",
    val vidyaKey: String = "",
    val contactCode: String = "",
    val avatarMode: String = "PRESET",
    val avatarPresetId: String = "vedic_strategist",
    val avatarCrestId: String = "yantra",
    val avatarFrameId: String = "astrolabe",
    val avatarToneId: String = "saffron",
    val localAvatarPhotoPath: String = "",
    val focusRealm: String = "MATHEMATICS",
    val onboardingCompleted: Boolean = false,
    val streak: Int = 5,
    val xp: Int = 1250,
    val level: String = "Silver Scholar",
    val hearts: Int = 5,
    val completedLessons: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveVidyaId(): String {
        return vidyaId.ifBlank {
            contactCode.ifBlank { "scholar#1000" }
        }
    }

    fun toAvatarSpec(): AvatarSpec = AvatarSpec.fromStrings(
        mode = avatarMode,
        presetId = avatarPresetId,
        crestId = avatarCrestId,
        frameId = avatarFrameId,
        toneId = avatarToneId,
        localPhotoPath = localAvatarPhotoPath
    )
}

@IgnoreExtraProperties
data class VidyaIdentityCard(
    val uid: String = "",
    val vidyaId: String = "",
    val vidyaHandle: String = "",
    val vidyaDiscriminator: String = "",
    val displayName: String = "",
    val level: String = "Bronze Scholar",
    val avatarMode: String = "PRESET",
    val avatarPresetId: String = "vedic_strategist",
    val avatarCrestId: String = "yantra",
    val avatarFrameId: String = "astrolabe",
    val avatarToneId: String = "saffron",
    val focusRealm: String = "MATHEMATICS",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toAvatarSpec(): AvatarSpec = AvatarSpec.fromStrings(
        mode = avatarMode,
        presetId = avatarPresetId,
        crestId = avatarCrestId,
        frameId = avatarFrameId,
        toneId = avatarToneId
    )
}

object PracticePeersCatalog {
    val sampleScholars: List<VidyaIdentityCard> = listOf(
        VidyaIdentityCard(
            uid = "ananya_01",
            vidyaId = "ananya#2048",
            vidyaHandle = "ananya",
            vidyaDiscriminator = "2048",
            displayName = "Ananya Sharma",
            level = "Gold Master",
            avatarMode = "PRESET",
            avatarPresetId = "quantum_alchemist",
            avatarCrestId = "chakra",
            avatarFrameId = "ashtapada",
            avatarToneId = "cyan",
            focusRealm = "PHYSICS"
        ),
        VidyaIdentityCard(
            uid = "rohan_02",
            vidyaId = "rohan#3904",
            vidyaHandle = "rohan",
            vidyaDiscriminator = "3904",
            displayName = "Rohan Verma",
            level = "Silver Prodigy",
            avatarMode = "PRESET",
            avatarPresetId = "celestial_architect",
            avatarCrestId = "compass",
            avatarFrameId = "astrolabe",
            avatarToneId = "indigo",
            focusRealm = "CODING_AI"
        ),
        VidyaIdentityCard(
            uid = "meera_03",
            vidyaId = "meera#5510",
            vidyaHandle = "meera",
            vidyaDiscriminator = "5510",
            displayName = "Meera Nair",
            level = "Gold Master",
            avatarMode = "PRESET",
            avatarPresetId = "lotus_sage",
            avatarCrestId = "lotus",
            avatarFrameId = "lotus_halo",
            avatarToneId = "emerald",
            focusRealm = "CHEMISTRY"
        ),
        VidyaIdentityCard(
            uid = "kabir_04",
            vidyaId = "kabir#7319",
            vidyaHandle = "kabir",
            vidyaDiscriminator = "7319",
            displayName = "Kabir Sen",
            level = "Platinum Sage",
            avatarMode = "BUILDER",
            avatarPresetId = "vedic_strategist",
            avatarCrestId = "yantra",
            avatarFrameId = "minimal_gold",
            avatarToneId = "gold",
            focusRealm = "MATHEMATICS"
        ),
        VidyaIdentityCard(
            uid = "vidyut_bot_1001",
            vidyaId = "vidyut#1001",
            vidyaHandle = "vidyut",
            vidyaDiscriminator = "1001",
            displayName = "Vidyut Tactician",
            level = "Arena Grandmaster",
            avatarMode = "PRESET",
            avatarPresetId = "vidyut_maverick",
            avatarCrestId = "flame",
            avatarFrameId = "ashtapada",
            avatarToneId = "vermilion",
            focusRealm = "MATHEMATICS"
        )
    )

    fun findByQuery(query: String): List<VidyaIdentityCard> {
        val cleaned = query.trim().removePrefix("@").lowercase()
        if (cleaned.isBlank()) return sampleScholars
        return sampleScholars.filter {
            it.vidyaId.lowercase().contains(cleaned) ||
                it.displayName.lowercase().contains(cleaned) ||
                it.vidyaHandle.lowercase().contains(cleaned)
        }
    }

    fun findExactByVidyaId(vidyaId: String): VidyaIdentityCard? {
        val cleaned = vidyaId.trim().removePrefix("@").lowercase()
        return sampleScholars.firstOrNull { it.vidyaId.lowercase() == cleaned }
    }
}

data class Course(
    val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val iconName: String,
    val totalLessons: Int,
    val completedCount: Int,
    val colorHex: String
)

data class Lesson(
    val id: String,
    val courseId: String,
    val title: String,
    val conceptNotes: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class CommunityProject(
    val id: String = "",
    val title: String,
    val description: String,
    val authorName: String,
    val authorEmail: String,
    val category: String,
    val upvotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val senderVidyaId: String = "",
    val memberUids: List<String> = emptyList(),
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Chat(
    val id: String = "",
    val createdBy: String = "",
    val participants: List<String> = emptyList(),
    val name: String? = null,
    val peerVidyaId: String = "",
    val peerAvatarSpec: AvatarSpec? = null,
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,
    val type: String = "individual",
    val subjectTag: String = "General",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val timestampLabel: String = "Just now"
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Contact(
    val uid: String = "",
    val contactCode: String = "",
    val name: String = ""
)
