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
import com.example.games.matchmaking.OnlineMatchRepository
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

enum class GameType(val title: String, val subtitle: String) {
    SHATRANJ("Shatranj (Chess)", "2-Player master tactical duel against Vidyut Bot"),
    LUDO("Pachisi (Ludo)", "4-Player race game with animated dice"),
    CHATURANGA("Chaturanga (Chaturaji)", "Ancient 4-player dice battle on 8x8 Ashtapada board")
}

class GamesViewModel(application: Application) : AndroidViewModel(application) {
    private val database by lazy { AppDatabase.getInstance(application) }
    private val repository by lazy { GameRepository(database.gameDao()) }
    private val onlineMatchRepo by lazy { OnlineMatchRepository(application) }

    private val _activeGame = MutableStateFlow<GameType?>(null)
    val activeGame: StateFlow<GameType?> = _activeGame.asStateFlow()

    private val _selectedGameForSetup = MutableStateFlow<GameType?>(null)
    val selectedGameForSetup: StateFlow<GameType?> = _selectedGameForSetup.asStateFlow()

    private val _activeGameConfig = MutableStateFlow<GameSessionConfig?>(null)
    val activeGameConfig: StateFlow<GameSessionConfig?> = _activeGameConfig.asStateFlow()

    private val _rulebookGameType = MutableStateFlow<GameType?>(null)
    val rulebookGameType: StateFlow<GameType?> = _rulebookGameType.asStateFlow()

    private var botTurnJob: kotlinx.coroutines.Job? = null
    private var onlineMatchListenerJob: kotlinx.coroutines.Job? = null
    private var activeOnlineMatchId: String? = null
    private var activeOnlineSessionKey: SecretKey? = null
    private var activeOnlineParticipants: List<String> = emptyList()
    private var activeLocalUid: String = ""
    private var activeLocalPlayerIndex: Int = 0
    private var currentOnlineMoveSeq: Int = 0

    // 1. SHATRANJ (2-PLAYER CHESS) - Lazy loaded only when opened
    private var _chessStateFlow: MutableStateFlow<ChessGameState>? = null
    private val _chessState: MutableStateFlow<ChessGameState>
        get() = _chessStateFlow ?: MutableStateFlow(ChessGameState.initialBoard()).also { _chessStateFlow = it }
    val chessState: StateFlow<ChessGameState>
        get() = _chessState.asStateFlow()
    var selectedChessPos by mutableStateOf<Position?>(null)
        private set
    var validChessDestinations by mutableStateOf<List<Position>>(emptyList())
        private set
    var chessMode by mutableStateOf("BOT") // "BOT", "PASS_AND_PLAY", or "ONLINE"
    var chessDifficulty by mutableStateOf("MASTER") // "EASY", "MASTER"
    private var isBotThinking = false

    // 2. PACHISI (LUDO) - Lazy loaded only when opened
    private var _ludoStateFlow: MutableStateFlow<LudoGameState>? = null
    private val _ludoState: MutableStateFlow<LudoGameState>
        get() = _ludoStateFlow ?: MutableStateFlow(LudoGameState.initialGame()).also { _ludoStateFlow = it }
    val ludoState: StateFlow<LudoGameState>
        get() = _ludoState.asStateFlow()
    var ludoMode by mutableStateOf("BOT") // "BOT", "PASS_AND_PLAY", or "ONLINE"
    var isDiceRolling by mutableStateOf(false)
        private set

    // 3. CHATURANGA (4-PLAYER DICE CHESS) - Lazy loaded only when opened
    private var _chaturangaStateFlow: MutableStateFlow<ChaturangaState>? = null
    private val _chaturangaState: MutableStateFlow<ChaturangaState>
        get() = _chaturangaStateFlow ?: MutableStateFlow(ChaturangaState.initialBoard()).also { _chaturangaStateFlow = it }
    val chaturangaState: StateFlow<ChaturangaState>
        get() = _chaturangaState.asStateFlow()
    var selectedChaturangaPos by mutableStateOf<BoardPos?>(null)
        private set
    var validChaturangaDestinations by mutableStateOf<List<BoardPos>>(emptyList())
        private set
    var chaturangaMode by mutableStateOf("BOT") // "BOT", "PASS_AND_PLAY", or "ONLINE"
    var isChaturangaDiceRolling by mutableStateOf(false)
        private set

    // STATS & BADGES (Lazy initialized on collection)
    val allStats: StateFlow<List<GameStatsEntity>> by lazy {
        repository.allStats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val allBadges: StateFlow<List<UserBadgeEntity>> by lazy {
        repository.allBadges.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val recentMatches: StateFlow<List<com.example.data.local.MatchHistoryEntity>> by lazy {
        repository.recentMatches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

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
        botTurnJob?.cancel()
        botTurnJob = null
        onlineMatchListenerJob?.cancel()
        onlineMatchListenerJob = null
        isBotThinking = false
        isDiceRolling = false
        isChaturangaDiceRolling = false
        _activeGameConfig.value = config
        _selectedGameForSetup.value = null

        val isOnlineActive = config.mode == PlayMode.ONLINE_RANKED &&
            config.onlineMatchId.isNotBlank() &&
            config.localParticipantUid.isNotBlank()

        activeOnlineMatchId = if (isOnlineActive) config.onlineMatchId else null
        activeOnlineSessionKey = null
        activeOnlineParticipants = emptyList()
        activeLocalUid = config.localParticipantUid
        activeLocalPlayerIndex = config.localPlayerIndex
        currentOnlineMoveSeq = 0

        val type = try { GameType.valueOf(config.gameTypeName) } catch (_: Exception) { GameType.SHATRANJ }
        val modeTag = when {
            isOnlineActive -> "ONLINE"
            config.mode == PlayMode.PASS_AND_PLAY -> "PASS_AND_PLAY"
            else -> "BOT"
        }
        chessMode = modeTag
        chessDifficulty = config.difficulty
        ludoMode = modeTag
        chaturangaMode = modeTag

        when (type) {
            GameType.SHATRANJ -> {
                _chessState.value = ChessGameState.initialBoard()
                selectedChessPos = null
                validChessDestinations = emptyList()
            }
            GameType.LUDO -> {
                val activeColors = if (isOnlineActive) {
                    listOf(LudoColor.RED, LudoColor.YELLOW)
                } else {
                    when (config.playerCount) {
                        2 -> listOf(LudoColor.RED, LudoColor.YELLOW)
                        3 -> listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW)
                        else -> LudoColor.entries
                    }
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

        if (isOnlineActive) {
            startOnlineMatchSync(type, config.onlineMatchId, config.localParticipantUid)
        }
    }

    private fun startOnlineMatchSync(type: GameType, matchId: String, uid: String) {
        onlineMatchListenerJob?.cancel()
        onlineMatchListenerJob = viewModelScope.launch {
            onlineMatchRepo.observeMatch(matchId).collect { match ->
                if (match == null) return@collect
                activeOnlineParticipants = match.participantUids
                if (activeOnlineSessionKey == null) {
                    activeOnlineSessionKey = onlineMatchRepo.resolveMatchKey(match, uid)
                }
                val key = activeOnlineSessionKey
                if (match.moveSeq > currentOnlineMoveSeq && key != null) {
                    currentOnlineMoveSeq = match.moveSeq
                    val decrypted = onlineMatchRepo.verifyAndDecryptMatchState(match, key)
                    if (!decrypted.isNullOrBlank()) {
                        when (type) {
                            GameType.SHATRANJ -> {
                                ChessGameState.deserialize(decrypted)?.let { parsed ->
                                    val check = ChessEngine.isKingAttacked(parsed.board, parsed.currentTurn)
                                    val mate = check && !ChessEngine.hasAnyLegalMoves(parsed.board, parsed.currentTurn)
                                    _chessState.value = parsed.copy(isCheck = check, isCheckmate = mate)
                                }
                            }
                            GameType.LUDO -> {
                                LudoGameState.deserialize(decrypted)?.let { parsed ->
                                    _ludoState.value = parsed
                                }
                            }
                            GameType.CHATURANGA -> {
                                ChaturangaState.deserialize(decrypted)?.let { parsed ->
                                    _chaturangaState.value = parsed
                                }
                            }
                        }
                    }
                    if (match.status == "completed" && match.winnerUid.isNotBlank()) {
                        val won = match.winnerUid == uid
                        repository.recordMatchResult(
                            gameType = type.name,
                            won = won,
                            scoreEarned = if (won) 150 else 30,
                            timeSeconds = 180L,
                            isRanked = true
                        )
                    }
                }
            }
        }
    }

    private fun publishOnlineTurn(
        serializedState: String,
        keepSameTurn: Boolean = false,
        winnerUid: String = ""
    ) {
        val matchId = activeOnlineMatchId ?: return
        val key = activeOnlineSessionKey ?: return
        val nextSeq = currentOnlineMoveSeq + 1
        val nextTurnUid = if (keepSameTurn) {
            activeLocalUid
        } else {
            activeOnlineParticipants.firstOrNull { it != activeLocalUid } ?: activeLocalUid
        }
        currentOnlineMoveSeq = nextSeq
        viewModelScope.launch {
            onlineMatchRepo.submitEncryptedTurn(
                matchId = matchId,
                nextTurnUid = nextTurnUid,
                nextMoveSeq = nextSeq,
                serializedState = serializedState,
                sessionKey = key,
                winnerUid = winnerUid
            )
        }
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
        botTurnJob?.cancel()
        botTurnJob = null
        onlineMatchListenerJob?.cancel()
        onlineMatchListenerJob = null
        activeOnlineMatchId = null
        activeOnlineSessionKey = null
        activeOnlineParticipants = emptyList()
        currentOnlineMoveSeq = 0
        isBotThinking = false
        isDiceRolling = false
        isChaturangaDiceRolling = false
        selectedChessPos = null
        validChessDestinations = emptyList()
        selectedChaturangaPos = null
        validChaturangaDestinations = emptyList()
        _activeGame.value = null
        _activeGameConfig.value = null
        _chessStateFlow = null
        _ludoStateFlow = null
        _chaturangaStateFlow = null
    }

    // --- SHATRANJ (2-PLAYER CHESS) INTERACTIONS ---
    fun onChessSquareClick(pos: Position, context: Context) {
        if (isBotThinking || _chessState.value.isCheckmate) return
        val currentTurn = _chessState.value.currentTurn

        if (chessMode == "BOT" && currentTurn == PieceColor.BLACK) return
        if (chessMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) PieceColor.WHITE else PieceColor.BLACK
            if (currentTurn != myColor) return
        }

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

                if (chessMode == "ONLINE") {
                    publishOnlineTurn(
                        serializedState = nextState.serialize(),
                        keepSameTurn = false,
                        winnerUid = if (nextState.isCheckmate) activeLocalUid else ""
                    )
                }

                if (nextState.isCheckmate) {
                    SoundHelper.playArenaVictorySwell(context)
                    if (chessMode != "ONLINE") {
                        viewModelScope.launch {
                            val isRanked = _activeGameConfig.value?.mode == PlayMode.ONLINE_RANKED
                            repository.recordMatchResult(GameType.SHATRANJ.name, won = true, scoreEarned = 100, timeSeconds = 120L, isRanked = isRanked)
                        }
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
        botTurnJob?.cancel()
        botTurnJob = viewModelScope.launch {
            delay(500)
            if (_activeGame.value != GameType.SHATRANJ) {
                isBotThinking = false
                return@launch
            }
            val botMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                ChessEngine.computeBotMove(_chessState.value, chessDifficulty)
            }
            if (_activeGame.value != GameType.SHATRANJ) {
                isBotThinking = false
                return@launch
            }
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
        botTurnJob?.cancel()
        botTurnJob = null
        isBotThinking = false
        _chessState.value = ChessGameState.initialBoard()
        selectedChessPos = null
        validChessDestinations = emptyList()
    }

    // --- PACHISI (LUDO) INTERACTIONS ---
    fun onRollLudoDice(context: Context) {
        if (isDiceRolling || _ludoState.value.hasRolled || _ludoState.value.isGameOver) return
        if (ludoMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) LudoColor.RED else LudoColor.YELLOW
            if (_ludoState.value.currentTurn != myColor) return
        }
        isDiceRolling = true
        SoundHelper.playArenaDiceRattle(context)

        botTurnJob?.cancel()
        botTurnJob = viewModelScope.launch {
            delay(420)
            if (_activeGame.value != GameType.LUDO) {
                isDiceRolling = false
                return@launch
            }
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
                if (_activeGame.value != GameType.LUDO) return@launch
                val activeColors = updated.tokens.map { it.color }.distinct()
                val nextPlayer = LudoEngine.nextPlayer(updated.currentTurn, activeColors)
                val passedState = updated.copy(
                    currentTurn = nextPlayer,
                    hasRolled = false,
                    logMessage = "No moves for ${updated.currentTurn.title}. ${nextPlayer.title}'s turn."
                )
                _ludoState.value = passedState
                if (ludoMode == "ONLINE") {
                    publishOnlineTurn(passedState.serialize(), keepSameTurn = false)
                } else {
                    checkAndTriggerLudoBot(context)
                }
            } else if (movable.size == 1 && (ludoMode == "BOT" && updated.currentTurn != LudoColor.RED)) {
                delay(400)
                if (_activeGame.value != GameType.LUDO) return@launch
                onLudoTokenClick(movable.first(), context)
            } else if (ludoMode == "BOT" && updated.currentTurn != LudoColor.RED) {
                delay(400)
                if (_activeGame.value != GameType.LUDO) return@launch
                val botToken = LudoEngine.chooseBotToken(movable, updated.tokens, dice)
                if (botToken != null) onLudoTokenClick(botToken, context)
            }
        }
    }

    fun onLudoTokenClick(token: LudoToken, context: Context) {
        val state = _ludoState.value
        if (!state.hasRolled || token.color != state.currentTurn) return
        if (ludoMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) LudoColor.RED else LudoColor.YELLOW
            if (state.currentTurn != myColor) return
        }
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

        if (ludoMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) LudoColor.RED else LudoColor.YELLOW
            val wonNow = nextState.winners.contains(myColor)
            publishOnlineTurn(
                serializedState = nextState.serialize(),
                keepSameTurn = !wonNow && nextState.currentTurn == myColor,
                winnerUid = if (wonNow) activeLocalUid else ""
            )
        } else {
            if (nextState.winners.contains(LudoColor.RED) && !state.winners.contains(LudoColor.RED)) {
                viewModelScope.launch {
                    val isRanked = _activeGameConfig.value?.mode == PlayMode.ONLINE_RANKED
                    repository.recordMatchResult(GameType.LUDO.name, won = true, scoreEarned = 250, timeSeconds = 300L, isRanked = isRanked)
                }
            }
            checkAndTriggerLudoBot(context)
        }
    }

    private fun checkAndTriggerLudoBot(context: Context) {
        if (_activeGame.value != GameType.LUDO) return
        val state = _ludoState.value
        if (ludoMode == "BOT" && state.currentTurn != LudoColor.RED && !state.isGameOver) {
            botTurnJob?.cancel()
            botTurnJob = viewModelScope.launch {
                delay(700)
                if (_activeGame.value == GameType.LUDO) {
                    onRollLudoDice(context)
                }
            }
        }
    }

    fun resetLudo() {
        botTurnJob?.cancel()
        botTurnJob = null
        isDiceRolling = false
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
        if (chaturangaMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) ChaturangaPlayer.RED else ChaturangaPlayer.GREEN
            if (_chaturangaState.value.currentTurn != myColor) return
        }
        isChaturangaDiceRolling = true
        SoundHelper.playArenaDiceRattle(context)

        botTurnJob?.cancel()
        botTurnJob = viewModelScope.launch {
            delay(420)
            if (_activeGame.value != GameType.CHATURANGA) {
                isChaturangaDiceRolling = false
                return@launch
            }
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
                if (_activeGame.value != GameType.CHATURANGA) return@launch
                val next = ChaturangaEngine.getNextActivePlayer(updated.currentTurn, updated.eliminatedPlayers)
                val passedState = updated.copy(
                    currentTurn = next,
                    hasRolled = false,
                    logMessage = "No moves for ${updated.currentTurn.title}. ${next.title}'s turn."
                )
                _chaturangaState.value = passedState
                if (chaturangaMode == "ONLINE") {
                    publishOnlineTurn(passedState.serialize(), keepSameTurn = false)
                } else {
                    checkAndTriggerChaturangaBot(context)
                }
            } else if (chaturangaMode == "BOT" && updated.currentTurn != ChaturangaPlayer.RED) {
                delay(500)
                if (_activeGame.value != GameType.CHATURANGA) return@launch
                val botMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    ChaturangaEngine.chooseBotMove(updated)
                }
                if (_activeGame.value != GameType.CHATURANGA) return@launch
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
        if (chaturangaMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) ChaturangaPlayer.RED else ChaturangaPlayer.GREEN
            if (state.currentTurn != myColor) return
        }

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

        if (chaturangaMode == "ONLINE") {
            val myColor = if (activeLocalPlayerIndex == 0) ChaturangaPlayer.RED else ChaturangaPlayer.GREEN
            val won = nextState.isGameOver && !nextState.eliminatedPlayers.contains(myColor)
            publishOnlineTurn(
                serializedState = nextState.serialize(),
                keepSameTurn = false,
                winnerUid = if (nextState.isGameOver && won) activeLocalUid else ""
            )
        } else if (nextState.isGameOver) {
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
        if (_activeGame.value != GameType.CHATURANGA) return
        val state = _chaturangaState.value
        if (chaturangaMode == "BOT" && state.currentTurn != ChaturangaPlayer.RED && !state.isGameOver) {
            botTurnJob?.cancel()
            botTurnJob = viewModelScope.launch {
                delay(700)
                if (_activeGame.value == GameType.CHATURANGA) {
                    onRollChaturangaDice(context)
                }
            }
        }
    }

    fun resetChaturanga() {
        botTurnJob?.cancel()
        botTurnJob = null
        isChaturangaDiceRolling = false
        _chaturangaState.value = ChaturangaState.initialBoard()
        selectedChaturangaPos = null
        validChaturangaDestinations = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        botTurnJob?.cancel()
        onlineMatchListenerJob?.cancel()
    }
}
