package com.example.games.matchmaking

import com.example.data.PracticePeersCatalog
import com.example.data.VidyaIdentityCard
import com.example.viewmodel.GameType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MatchmakingTicket(
    val ticketId: String = "",
    val uid: String = "",
    val vidyaId: String = "",
    val displayName: String = "",
    val gameType: String = "",
    val eloRating: Int = 1200,
    val requiredPlayers: Int = 2,
    val publicKey: String = "",
    val status: String = "searching",
    val matchId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OnlineMatch(
    val matchId: String = "",
    val createdBy: String = "",
    val participantUids: List<String> = emptyList(),
    val gameType: String = "",
    val status: String = "active",
    val currentTurnUid: String = "",
    val moveSeq: Int = 0,
    val wrappedKeys: Map<String, String> = emptyMap(),
    val keyVersion: Int = 1,
    val encryptedState: String = "",
    val stateIv: String = "",
    val stateDigest: String = "",
    val winnerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object MatchmakingEngine {
    fun pickPracticeOpponent(gameType: GameType, excludeUid: String): VidyaIdentityCard {
        val candidates = PracticePeersCatalog.sampleScholars.filter { it.uid != excludeUid }
        return if (gameType == GameType.SHATRANJ) {
            candidates.firstOrNull { it.vidyaId == "vidyut#1001" } ?: candidates.first()
        } else {
            candidates.random()
        }
    }
}
