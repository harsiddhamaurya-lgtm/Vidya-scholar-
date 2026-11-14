package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // Session state queries (for save & resume)
    @Query("SELECT * FROM game_sessions WHERE gameType = :gameType LIMIT 1")
    fun getSessionFlow(gameType: String): Flow<GameSessionEntity?>

    @Query("SELECT * FROM game_sessions WHERE gameType = :gameType LIMIT 1")
    suspend fun getSession(gameType: String): GameSessionEntity?

    @Query("SELECT * FROM game_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<GameSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE gameType = :gameType")
    suspend fun clearSession(gameType: String)

    // Stats queries
    @Query("SELECT * FROM game_stats WHERE gameType = :gameType LIMIT 1")
    fun getStatsFlow(gameType: String): Flow<GameStatsEntity?>

    @Query("SELECT * FROM game_stats")
    fun getAllStats(): Flow<List<GameStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: GameStatsEntity)

    // Badge queries
    @Query("SELECT * FROM user_badges")
    fun getAllBadges(): Flow<List<UserBadgeEntity>>

    @Query("SELECT * FROM user_badges WHERE gameType = :gameType")
    fun getBadgesForGame(gameType: String): Flow<List<UserBadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: UserBadgeEntity)

    // Match History queries
    @Query("SELECT * FROM match_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentMatches(): Flow<List<MatchHistoryEntity>>

    @Query("SELECT * FROM match_history WHERE gameType = :gameType ORDER BY timestamp DESC")
    fun getMatchHistoryForGame(gameType: String): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(history: MatchHistoryEntity)
}
