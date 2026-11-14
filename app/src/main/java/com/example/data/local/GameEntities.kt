package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey
    val gameType: String, // "SHATRANJ", "LUDO", "CHATURANGA"
    val stateData: String, // JSON payload representing board, turn, tokens
    val mode: String, // "BOT", "PASS_AND_PLAY"
    val difficulty: String = "NORMAL", // "EASY", "MASTER"
    val moveCount: Int = 0,
    val timeElapsedSeconds: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey
    val gameType: String,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val highScore: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val eloRating: Int = 1200,
    val rankTier: String = "Knight",
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_badges")
data class UserBadgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val gameType: String,     // "SHATRANJ", "LUDO", "CHATURANGA"
    val tierName: String,     // "Bronze", "Silver", "Gold", "Platinum"
    val division: Int,        // 1 to 7
    val badgeTitle: String,   // e.g. "Bronze III", "Gold I"
    val colorHex: Long,       // Hex color representation
    val earnedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val gameType: String,     // "SHATRANJ", "LUDO", "CHATURANGA"
    val result: String,       // "VICTORY" or "DEFEAT"
    val scoreEarned: Int,
    val timeSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)
