package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.GameRepository
import com.example.data.local.GameStatsEntity
import com.example.data.local.UserBadgeEntity
import com.example.games.chaturanga.*
import com.example.games.chess.*
import com.example.games.config.*
import com.example.games.ludo.*
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class GameType(val title: String, val subtitle: String) {
    SHATRANJ("Shatranj (Chess)", "2-Player master tactical duel against Vidyut Bot"),
    LUDO("Pachisi (Ludo)", "4-Player race game with animated dice"),
    CHATURANGA("Chaturanga (Chaturaji)", "Ancient 4-player dice battle on 8x8 Ashtapada board")
}

class GamesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = GameRepository(database.gameDao())

    private val _activeGame = MutableStateFlow<GameType?>(null)
    val activeGame: StateFlow<GameType?> = _activeGame.asStateFlow()

    private val _selectedGameForSetup = MutableStateFlow<GameType?>(null)
    val selectedGameForSetup: StateFlow<GameType?> = _selectedGameForSetup.asStateFlow()

    private val _activeGameConfig = MutableStateFlow<GameSessionConfig?>(null)
    val activeGameConfig: StateFlow<GameSessionConfig?> = _activeGameConfig.asStateFlow()

    private val _rulebookGameType = MutableStateFlow<GameType?>(null)
    val rulebookGameType: StateFlow<GameType?> = _rulebookGameType.asStateFlow()

    // 1. SHATRANJ (2-PLAYER CHESS) STATE
    private val _chessState = MutableStateFlow(ChessGameState.initialBoard())
    val chessState: StateFlow<ChessGameState> = _chessState.asStateFlow()
    var selectedChessPos by mutableStateOf<Position?>(null)
        private set
    var validChessDestinations by mutableStateOf<List<Position>>(emptyList())
        private set
    var chessMode by mutableStateOf("BOT") // "BOT" or "PASS_AND_PLAY"
    var chessDifficulty by mutableStateOf("MASTER") // "EASY", "MASTER"
    private var isBotThinking = false

    // 2. PACHISI (LUDO) STATE
    private val _ludoState = MutableStateFlow(LudoGameState.initialGame())
    val ludoState: StateFlow<LudoGameState> = _ludoState.asStateFlow()
    var ludoMode by mutableStateOf("BOT") // "BOT" or "PASS_AND_PLAY"
    var isDiceRolling by mutableStateOf(false)
        private set

    // 3. CHATURANGA (4-PLAYER DICE CHESS) STATE
    private val _chaturangaState = MutableStateFlow(ChaturangaState.initialBoard())
    val chaturangaState: StateFlow<ChaturangaState> = _chaturangaState.asStateFlow()
    var selectedChaturangaPos by mutableStateOf<BoardPos?>(null)
        private set
    var validChaturangaDestinations by mutableStateOf<List<BoardPos>>(emptyList())
        private set
    var chaturangaMode by mutableStateOf("BOT") // "BOT" (Red vs 3 Bots) or "PASS_AND_PLAY"
    var isChaturangaDiceRolling by mutableStateOf(false)
        private set

    // STATS & BADGES
    val allStats: StateFlow<List<GameStatsEntity>> = repository.allStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBadges: StateFlow<List<UserBadgeEntity>> = repository.allBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMatches: StateFlow<List<com.example.data.local.MatchHistoryEntity>> = repository.recentMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMatchHistoryForGame(gameType: String): Flow<List<com.example.data.local.MatchHistoryEntity>> {
        return repository.getMatchHistoryForGame(gameType)
    }

    fun openGameHub(type: GameType) {
        _selectedGameForSetup.value = type
    }

    fun closeGameHub() {
        _selectedGameForSetup.value = null
    }

    fun showRulebook(type: GameType) {
        _rulebookGameType.value = type
    }

    fun dismissRulebook() {
        _rulebookGameType.value = null
    }

    fun startConfiguredGame(config: GameSessionConfig) {
        _activeGameConfig.value = config
        _selectedGameForSetup.value = null

        val type = try { GameType.valueOf(config.gameTypeName) } catch (_: Exception) { GameType.SHATRANJ }
        chessMode = if (config.mode == PlayMode.PASS_AND_PLAY) "PASS_AND_PLAY" else "BOT"
        chessDifficulty = config.difficulty
        ludoMode = if (config.mode == PlayMode.PASS_AND_PLAY) "PASS_AND_PLAY" else "BOT"
        chaturangaMode = if (config.mode == PlayMode.PASS_AND_PLAY) "PASS_AND_PLAY" else "BOT"

        // Reset to a clean configured board when starting from hub
        when (type) {
            GameType.SHATRANJ -> {
                _chessState.value = ChessGameState.initialBoard()
                selectedChessPos = null
                validChessDestinations = emptyList()
            }
            GameType.LUDO -> {
                val activeColors = when (config.playerCount) {
                    2 -> listOf(LudoColor.RED, LudoColor.YELLOW) // Opposite corners
                    3 -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW)
                    else -> LudoColor.entries
                }
                _ludoState.value = LudoGameState.initialGame(activeColors)
            }
            GameType.CHATURANGA -> {
                _chaturangaState.value = ChaturangaState.initialBoard()
                selectedChaturangaPos = null
                validChaturangaDestinations = emptyList()
            }
        }
        _activeGame.value = type
    }

    fun getPlayerName(index: Int, fallback: String): String {
        return _activeGameConfig.value?.players?.getOrNull(index)?.name?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun launchGame(type: GameType) {
        when (type) {
            GameType.SHATRANJ -> resetChess()
            GameType.LUDO -> resetLudo()
            GameType.CHATURANGA -> resetChaturanga()
        }
        _activeGame.value = type
    }

    fun exitToLobby() {
        _activeGame.value = null
        _activeGameConfig.value = null
    }

    // --- SHATRANJ (2-PLAYER CHESS) INTERACTIONS ---
    fun onChessSquareClick(pos: Position, context: Context) {
        if (isBotThinking || _chessState.value.isCheckmate) return
        val currentTurn = _chessState.value.currentTurn

        if (chessMode == "BOT" && currentTurn == PieceColor.BLACK) return

        val selected = selectedChessPos
        if (selected == null) {
            val piece = _chessState.value.board[pos.row][pos.col]
            if (piece?.color == currentTurn) {
                selectedChessPos = pos
                validChessDestinations = ChessEngine.getValidMoves(_chessState.value, pos)
                SoundHelper.playClick(context)
            }
        } else {
            if (validChessDestinations.contains(pos)) {
                val move = ChessMove(selected, pos, _chessState.value.board[pos.row][pos.col])
                val nextState = ChessEngine.makeMove(_chessState.value, move)
                _chessState.value = nextState
                selectedChessPos = null
                validChessDestinations = emptyList()

                if (nextState.isCheckmate) {
                    SoundHelper.playArenaVictorySwell(context)
                    viewModelScope.launch {
                        val isRanked = _activeGameConfig.value?.mode == PlayMode.ONLINE_RANKED
                        repository.recordMatchResult(GameType.SHATRANJ.name, won = true, scoreEarned = 100, timeSeconds = 120L, isRanked = isRanked)
                    }
                } else if (move.captured != null) {
                    SoundHelper.playArenaCaptureOrSafeChime(context, isCapture = true)
                    if (chessMode == "BOT" && nextState.currentTurn == PieceColor.BLACK) {
                        triggerVidyutBotChessMove(context)
                    }
                } else {
                    SoundHelper.playArenaMove(context)
                    if (chessMode == "BOT" && nextState.currentTurn == PieceColor.BLACK) {
                        triggerVidyutBotChessMove(context)
                    }
                }
            } else {
                val piece = _chessState.value.board[pos.row][pos.col]
                if (piece?.color == currentTurn) {
                    selectedChessPos = pos
                    validChessDestinations = ChessEngine.getValidMoves(_chessState.value, pos)
                    SoundHelper.playClick(context)
                } else {
                    selectedChessPos = null
                    validChessDestinations = emptyList()
                }
            }
        }
    }

    private fun triggerVidyutBotChessMove(context: Context) {
        isBotThinking = true
        viewModelScope.launch {
            delay(500)
            val botMove = ChessEngine.computeBotMove(_chessState.value, chessDifficulty)
            if (botMove != null) {
                val nextState = ChessEngine.makeMove(_chessState.value, botMove)
                _chessState.value = nextState
                when {
                    nextState.isCheckmate -> SoundHelper.playArenaVictorySwell(context)
                    botMove.captured != null -> SoundHelper.playArenaCaptureOrSafeChime(context, isCapture = true)
                    else -> SoundHelper.playArenaMove(context)
                }

                if (nextState.isCheckmate) {
                    repository.recordMatchResult(GameType.SHATRANJ.name, won = false, scoreEarned = 25, timeSeconds = 120L)
                }
            }
            isBotThinking = false
        }
    }

    fun resetChess() {
        _chessState.value = ChessGameState.initialBoard()
        selectedChessPos = null
        validChessDestinations = emptyList()
    }

    // --- PACHISI (LUDO) INTERACTIONS ---
    fun onRollLudoDice(context: Context) {
        if (isDiceRolling || _ludoState.value.hasRolled || _ludoState.value.isGameOver) return
        isDiceRolling = true
        SoundHelper.playArenaDiceRattle(context)

        viewModelScope.launch {
            delay(420)
            val dice = LudoEngine.rollDice()
            val updated = _ludoState.value.copy(
                lastDiceValue = dice,
                hasRolled = true,
                logMessage = "${_ludoState.value.currentTurn.title} rolled a $dice!"
            )
            _ludoState.value = updated
            isDiceRolling = false
            SoundHelper.playClick(context)

            val movable = LudoEngine.getMovableTokens(updated)
            if (movable.isEmpty()) {
                delay(600)
                val activeColors = updated.tokens.map { it.color }.distinct()
                val nextPlayer = LudoEngine.nextPlayer(updated.currentTurn, activeColors)
                _ludoState.value = updated.copy(
                    currentTurn = nextPlayer,
                    hasRolled = false,
                    logMessage = "No moves for ${updated.currentTurn.title}. ${nextPlayer.title}'s turn."
                )
                checkAndTriggerLudoBot(context)
            } else if (movable.size == 1 && (ludoMode == "BOT" && updated.currentTurn != LudoColor.RED)) {
                delay(400)
                onLudoTokenClick(movable.first(), context)
            } else if (ludoMode == "BOT" && updated.currentTurn != LudoColor.RED) {
                delay(400)
                val botToken = LudoEngine.chooseBotToken(movable, updated.tokens, dice)
                if (botToken != null) onLudoTokenClick(botToken, context)
            }
        }
    }

    fun onLudoTokenClick(token: LudoToken, context: Context) {
        val state = _ludoState.value
        if (!state.hasRolled || token.color != state.currentTurn) return
        if (!LudoEngine.canMoveToken(token, state.lastDiceValue)) return

        val nextState = LudoEngine.moveToken(state, token)
        _ludoState.value = nextState

        val movedToken = nextState.tokens.find { it.id == token.id && it.color == token.color }
        val reachedHome = movedToken?.isHome == true && !token.isHome
        val capturedOpponent = nextState.tokens.count { it.isInYard && it.color != token.color } >
            state.tokens.count { it.isInYard && it.color != token.color }
        val landedSafeZone = movedToken != null && movedToken.position in LudoEngine.SAFE_ZONES

        when {
            nextState.isGameOver -> SoundHelper.playArenaVictorySwell(context)
            capturedOpponent -> SoundHelper.playArenaCaptureOrSafeChime(context, isCapture = true)
            reachedHome || landedSafeZone -> SoundHelper.playArenaCaptureOrSafeChime(context, isCapture = false)
            else -> SoundHelper.playArenaMove(context)
        }

        if (nextState.winners.contains(LudoColor.RED) && !state.winners.contains(LudoColor.RED)) {
            viewModelScope.launch {
                val isRanked = _activeGameConfig.value?.mode == PlayMode.ONLINE_RANKED
                repository.recordMatchResult(GameType.LUDO.name, won = true, scoreEarned = 250, timeSeconds = 300L, isRanked = isRanked)
            }
        }

        checkAndTriggerLudoBot(context)
    }

    private fun checkAndTriggerLudoBot(context: Context) {
        val state = _ludoState.value
        if (ludoMode == "BOT" && state.currentTurn != LudoColor.RED && !state.isGameOver) {
            viewModelScope.launch {
                delay(700)
                onRollLudoDice(context)
            }
        }
    }

    fun resetLudo() {
        val count = _activeGameConfig.value?.playerCount ?: 4
        val activeColors = when (count) {
            2 -> listOf(LudoColor.RED, LudoColor.YELLOW)
            3 -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW)
            else -> LudoColor.entries
        }
        _ludoState.value = LudoGameState.initialGame(activeColors)
    }

    // --- CHATURANGA (4-PLAYER DICE CHESS) INTERACTIONS ---
    fun onRollChaturangaDice(context: Context) {
        if (isChaturangaDiceRolling || _chaturangaState.value.hasRolled || _chaturangaState.value.isGameOver) return
        isChaturangaDiceRolling = true
        SoundHelper.playArenaDiceRattle(context)

        viewModelScope.launch {
            delay(420)
            val dice = ChaturangaEngine.rollDice()
            val allowed = ChaturangaEngine.diceToPieceTypes(dice)
            val allowedTitles = allowed.joinToString { it.title.split(" ")[0] }
            val updated = _chaturangaState.value.copy(
                lastDiceValue = dice,
                allowedPieceType = allowed,
                hasRolled = true,
                logMessage = "${_chaturangaState.value.currentTurn.title} rolled $dice! Move: $allowedTitles"
            )
            _chaturangaState.value = updated
            isChaturangaDiceRolling = false
            selectedChaturangaPos = null
            validChaturangaDestinations = emptyList()
            SoundHelper.playClick(context)

            val movable = ChaturangaEngine.getMovablePieces(updated)
            if (movable.isEmpty()) {
                delay(700)
                val next = ChaturangaEngine.getNextActivePlayer(updated.currentTurn, updated.eliminatedPlayers)
                _chaturangaState.value = updated.copy(
                    currentTurn = next,
                    hasRolled = false,
                    logMessage = "No moves for ${updated.currentTurn.title}. ${next.title}'s turn."
                )
                checkAndTriggerChaturangaBot(context)
            } else if (chaturangaMode == "BOT" && updated.currentTurn != ChaturangaPlayer.RED) {
                delay(500)
                val botMove = ChaturangaEngine.chooseBotMove(updated)
                if (botMove != null) {
                    executeChaturangaMove(botMove, context)
                }
            }
        }
    }

    fun onChaturangaSquareClick(pos: BoardPos, context: Context) {
        val state = _chaturangaState.value
        if (!state.hasRolled || isChaturangaDiceRolling || state.isGameOver) return

        if (chaturangaMode == "BOT" && state.currentTurn != ChaturangaPlayer.RED) return

        val selected = selectedChaturangaPos
        if (selected == null) {
            val piece = state.board[pos.row][pos.col]
            if (piece?.player == state.currentTurn && !piece.isDead && state.allowedPieceType.contains(piece.type)) {
                val valid = ChaturangaEngine.getValidMoves(state, pos)
                if (valid.isNotEmpty()) {
                    selectedChaturangaPos = pos
                    validChaturangaDestinations = valid
                    SoundHelper.playClick(context)
                }
            }
        } else {
            if (validChaturangaDestinations.contains(pos)) {
                val move = ChaturangaMove(selected, pos, state.board[pos.row][pos.col])
                executeChaturangaMove(move, context)
            } else {
                val piece = state.board[pos.row][pos.col]
                if (piece?.player == state.currentTurn && !piece.isDead && state.allowedPieceType.contains(piece.type)) {
                    val valid = ChaturangaEngine.getValidMoves(state, pos)
                    if (valid.isNotEmpty()) {
                        selectedChaturangaPos = pos
                        validChaturangaDestinations = valid
                        SoundHelper.playClick(context)
                    }
                } else {
                    selectedChaturangaPos = null
                    validChaturangaDestinations = emptyList()
                }
            }
        }
    }

    private fun executeChaturangaMove(move: ChaturangaMove, context: Context) {
        val nextState = ChaturangaEngine.makeMove(_chaturangaState.value, move)
        _chaturangaState.value = nextState
        selectedChaturangaPos = null
        validChaturangaDestinations = emptyList()

        when {
            nextState.isGameOver -> SoundHelper.playArenaVictorySwell(context)
            move.captured != null -> SoundHelper.playArenaCaptureOrSafeChime(context, isCapture = true)
            else -> SoundHelper.playArenaMove(context)
        }

        if (nextState.isGameOver) {
            val won = !nextState.eliminatedPlayers.contains(ChaturangaPlayer.RED)
            viewModelScope.launch {
                val redScore = nextState.playerScores[ChaturangaPlayer.RED] ?: 0
                val isRanked = _activeGameConfig.value?.mode == PlayMode.ONLINE_RANKED
                repository.recordMatchResult(GameType.CHATURANGA.name, won = won, scoreEarned = redScore, timeSeconds = 240L, isRanked = isRanked)
            }
        } else {
            checkAndTriggerChaturangaBot(context)
        }
    }

    private fun checkAndTriggerChaturangaBot(context: Context) {
        val state = _chaturangaState.value
        if (chaturangaMode == "BOT" && state.currentTurn != ChaturangaPlayer.RED && !state.isGameOver) {
            viewModelScope.launch {
                delay(700)
                onRollChaturangaDice(context)
            }
        }
    }

    fun resetChaturanga() {
        _chaturangaState.value = ChaturangaState.initialBoard()
        selectedChaturangaPos = null
        validChaturangaDestinations = emptyList()
    }
}
