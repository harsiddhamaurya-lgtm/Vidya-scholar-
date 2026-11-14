package com.example.games.matchmaking

import com.example.data.PracticePeersCatalog
import com.example.data.VidyaIdentityCard
import com.example.viewmodel.GameType

data class MatchmakingTicket(
    val ticketId: String = "",
    val uid: String = "",
    val vidyaId: String = "",
    val gameType: String = "",
    val eloRating: Int = 1200,
    val requiredPlayers: Int = 2,
    val timestamp: Long = System.currentTimeMillis()
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
