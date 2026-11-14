package com.example.games.config

import com.example.viewmodel.GameType

data class RankBadge(
    val tierName: String, // "Bronze", "Silver", "Gold", "Platinum"
    val division: Int,    // 1 to 4 (or up to 7 for Platinum)
    val title: String,    // "Bronze III", "Gold I", "Platinum VII"
    val colorHex: Long,
    val description: String
)

object RankBadgeSystem {
    fun getRankBadge(elo: Int): RankBadge {
        return when {
            elo < 600 -> {
                val div = when {
                    elo < 450 -> 4
                    elo < 500 -> 3
                    elo < 550 -> 2
                    else -> 1
                }
                RankBadge("Bronze", div, "Bronze $div", 0xFFCD7F32, "Apprentice combatant exploring the basics.")
            }
            elo < 1000 -> {
                val div = when {
                    elo < 700 -> 4
                    elo < 800 -> 3
                    elo < 900 -> 2
                    else -> 1
                }
                RankBadge("Silver", div, "Silver $div", 0xFFC0C0C0, "Skilled tactician mastering foundational openings.")
            }
            elo < 1500 -> {
                val div = when {
                    elo < 1125 -> 4
                    elo < 1250 -> 3
                    elo < 1375 -> 2
                    else -> 1
                }
                RankBadge("Gold", div, "Gold $div", 0xFFFFD700, "Elite strategist dominating board positioning.")
            }
            else -> {
                // Platinum / Grandmaster with 7 divisions (1 to 7)
                val diff = elo - 1500
                val div = (diff / 100).coerceIn(0, 6) + 1
                RankBadge("Platinum", div, "Platinum $div", 0xFF00E5FF, "Grandmaster legend of legendary tactical mastery.")
            }
        }
    }

    fun getTierNameFromElo(elo: Int): String {
        return getRankBadge(elo).tierName
    }
}
