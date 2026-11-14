package com.example.data.local

import com.example.games.config.RankBadgeSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class GameRepository(private val gameDao: GameDao) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Fast in-memory cache for 60/120fps operations
    private val sessionMemoryCache = ConcurrentHashMap<String, GameSessionEntity>()
    private val statsMemoryCache = ConcurrentHashMap<String, GameStatsEntity>()

    val allStats: Flow<List<GameStatsEntity>> = gameDao.getAllStats()
        .onEach { list ->
            list.forEach { statsMemoryCache[it.gameType] = it }
        }

    val allSessions: Flow<List<GameSessionEntity>> = gameDao.getAllSessions()
        .onEach { list ->
            list.forEach { sessionMemoryCache[it.gameType] = it }
        }

    val allBadges: Flow<List<UserBadgeEntity>> = gameDao.getAllBadges()
    val recentMatches: Flow<List<MatchHistoryEntity>> = gameDao.getRecentMatches()

    fun getMatchHistoryForGame(gameType: String): Flow<List<MatchHistoryEntity>> {
        return gameDao.getMatchHistoryForGame(gameType)
    }

    fun getBadgesForGame(gameType: String): Flow<List<UserBadgeEntity>> {
        return gameDao.getBadgesForGame(gameType)
    }

    fun getSessionFlow(gameType: String): Flow<GameSessionEntity?> {
        return gameDao.getSessionFlow(gameType)
            .onEach { entity ->
                if (entity != null) sessionMemoryCache[gameType] = entity
                else sessionMemoryCache.remove(gameType)
            }
    }

    fun getCachedSession(gameType: String): GameSessionEntity? {
        return sessionMemoryCache[gameType]
    }

    suspend fun saveSession(session: GameSessionEntity) {
        sessionMemoryCache[session.gameType] = session
        gameDao.saveSession(session)
    }

    suspend fun clearSession(gameType: String) {
        sessionMemoryCache.remove(gameType)
        gameDao.clearSession(gameType)
    }

    fun getStatsFlow(gameType: String): Flow<GameStatsEntity?> {
        return gameDao.getStatsFlow(gameType)
            .onEach { if (it != null) statsMemoryCache[gameType] = it }
    }

    suspend fun recordMatchResult(gameType: String, won: Boolean, scoreEarned: Int, timeSeconds: Long, isRanked: Boolean = true) {
        val current = gameDao.getSession(gameType)?.let { statsMemoryCache[gameType] }
            ?: statsMemoryCache[gameType]
            ?: GameStatsEntity(gameType = gameType)

        val eloDelta = if (isRanked) {
            if (won) 24 else -14
        } else 0

        val newElo = maxOf(400, current.eloRating + eloDelta)
        val newTier = when {
            newElo >= 2000 -> "Grandmaster"
            newElo >= 1700 -> "Master"
            newElo >= 1500 -> "Expert"
            newElo >= 1300 -> "Tactician"
            newElo >= 1100 -> "Knight"
            else -> "Apprentice"
        }

        val updated = current.copy(
            matchesPlayed = current.matchesPlayed + 1,
            matchesWon = if (won) current.matchesWon + 1 else current.matchesWon,
            highScore = maxOf(current.highScore, scoreEarned),
            bestTimeSeconds = if (current.bestTimeSeconds == 0L || (won && timeSeconds < current.bestTimeSeconds)) timeSeconds else current.bestTimeSeconds,
            eloRating = newElo,
            rankTier = newTier,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        statsMemoryCache[gameType] = updated
        gameDao.saveStats(updated)

        // Record Match History entry
        gameDao.insertMatchHistory(
            MatchHistoryEntity(
                gameType = gameType,
                result = if (won) "VICTORY" else "DEFEAT",
                scoreEarned = scoreEarned,
                timeSeconds = timeSeconds,
                timestamp = System.currentTimeMillis()
            )
        )

        // Check & unlock badge for new ELO tier
        val rankBadge = RankBadgeSystem.getRankBadge(newElo)
        gameDao.insertBadge(
            UserBadgeEntity(
                gameType = gameType,
                tierName = rankBadge.tierName,
                division = rankBadge.division,
                badgeTitle = rankBadge.title,
                colorHex = rankBadge.colorHex,
                earnedTimestamp = System.currentTimeMillis()
            )
        )
    }
}
