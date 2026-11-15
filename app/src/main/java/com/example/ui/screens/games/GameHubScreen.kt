package com.example.ui.screens.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AvatarSpec
import com.example.data.UserProfile
import com.example.data.local.GameStatsEntity
import com.example.games.chaturanga.ChaturangaState
import com.example.games.chess.ChessGameState
import com.example.games.config.GameSessionConfig
import com.example.games.config.PlayMode
import com.example.games.config.PlayerSetup
import com.example.games.config.RankBadgeSystem
import com.example.games.ludo.LudoColor
import com.example.games.ludo.LudoGameState
import com.example.games.matchmaking.MatchmakingEngine
import com.example.games.matchmaking.MatchmakingQueueResult
import com.example.games.matchmaking.OnlineMatchRepository
import com.example.ui.components.ScholarAvatarBadge
import com.example.ui.components.VidyaIdChip
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.SoundHelper
import com.example.viewmodel.GameType
import com.example.viewmodel.GamesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubScreen(
    gameType: GameType,
    userProfile: UserProfile? = null,
    viewModel: GamesViewModel,
    onBack: () -> Unit,
    onStartGame: (GameSessionConfig) -> Unit,
    onOpenRulebook: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val coroutineScope = rememberCoroutineScope()
    val allStats by viewModel.allStats.collectAsState()
    val stats = allStats.find { it.gameType == gameType.name } ?: GameStatsEntity(gameType = gameType.name)

    val (secondaryBg, accentColor) = when (gameType) {
        GameType.SHATRANJ -> Pair(palette.secondaryTerracotta, palette.accentSaffron)
        GameType.LUDO -> Pair(palette.secondaryIndigo, palette.accentVermilion)
        GameType.CHATURANGA -> Pair(palette.secondaryCopper, palette.accentCyan)
    }

    var selectedMode by remember { mutableStateOf(PlayMode.BOT) }
    var selectedDifficulty by remember { mutableStateOf("TACTICIAN") }
    var selectedTimeControl by remember { mutableStateOf(10) }
    var playerCount by remember {
        mutableStateOf(
            when (gameType) {
                GameType.SHATRANJ -> 2
                GameType.LUDO -> 4
                GameType.CHATURANGA -> 4
            }
        )
    }

    val playerOneName = userProfile?.name?.ifBlank { "You" } ?: "You"
    val playerVidyaId = userProfile?.effectiveVidyaId() ?: "scholar#1000"

    val defaultPlayers = remember(gameType, playerCount, selectedMode, playerOneName) {
        createDefaultPlayerSetups(gameType, playerCount, selectedMode, playerOneName)
    }
    val players = remember(gameType, playerCount) {
        mutableStateListOf<PlayerSetup>().apply { addAll(defaultPlayers) }
    }

    var isMatchmaking by remember { mutableStateOf(false) }
    var matchmakingSecondsLeft by remember { mutableStateOf(45) }
    var matchedOpponentName by remember { mutableStateOf<String?>(null) }
    var matchedOpponentElo by remember { mutableStateOf(1200) }
    var activeTicketId by remember { mutableStateOf<String?>(null) }
    val onlineMatchRepo = remember(context) { OnlineMatchRepository(context) }

    DisposableEffect(Unit) {
        onDispose {
            val pendingTicket = activeTicketId
            if (!pendingTicket.isNullOrBlank()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    onlineMatchRepo.cancelTicket(pendingTicket)
                }
            }
        }
    }

    val rankTier = stats.rankTier
    val elo = stats.eloRating
    val nextTierThreshold = when {
        elo < 600 -> 600
        elo < 1000 -> 1000
        elo < 1500 -> 1500
        else -> 2200
    }
    val prevTierThreshold = when {
        elo < 600 -> 400
        elo < 1000 -> 600
        elo < 1500 -> 1000
        else -> 1500
    }
    val tierProgress = ((elo - prevTierThreshold).toFloat() / (nextTierThreshold - prevTierThreshold).toFloat()).coerceIn(0f, 1f)

    IndoAtmosphericBackdrop(ambientGlowColor = accentColor) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = gameType.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Arena Lobby & Setup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("hub_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        var showHistoryDialog by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = {
                                SoundHelper.playClick(context)
                                showHistoryDialog = true
                            },
                            modifier = Modifier.testTag("hub_history_button")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = "Match History", tint = accentColor)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = secondaryBg,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .clickable {
                                    SoundHelper.playFolderOpen(context)
                                    onOpenRulebook()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(16.dp))
                                Text("Rules", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (showHistoryDialog) {
                            MatchHistoryDialog(
                                gameType = gameType,
                                viewModel = viewModel,
                                onDismiss = { showHistoryDialog = false }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. ELO RATING & BADGE HERO CARD (30% Secondary Tone + Gold Hairline + 10% Accent Meter)
                item {
                    val badge = RankBadgeSystem.getRankBadge(elo)
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        containerColor = secondaryBg,
                        hairlineColor = palette.goldHairline
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (!palette.isEInk) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    drawCircle(
                                        color = palette.accentGold.copy(alpha = 0.06f),
                                        radius = size.height * 0.58f,
                                        center = Offset(size.width * 0.90f, size.height * 0.25f),
                                        style = Stroke(1.dp.toPx())
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.cardSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (gameType) {
                                                        GameType.SHATRANJ -> Icons.Default.Shield
                                                        GameType.LUDO -> Icons.Default.Casino
                                                        GameType.CHATURANGA -> Icons.Default.Extension
                                                    },
                                                    contentDescription = null,
                                                    tint = Color(badge.colorHex),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Text(
                                                text = badge.title,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$elo ELO • Division: $rankTier",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.cardSurface
                                    ) {
                                        Text(
                                            text = "${stats.matchesWon}W - ${stats.matchesPlayed - stats.matchesWon}L",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                LinearProgressIndicator(
                                    progress = { tierProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(CircleShape),
                                    color = accentColor,
                                    trackColor = palette.cardSurface.copy(alpha = 0.65f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = badge.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${(nextTierThreshold - elo).coerceAtLeast(0)} pts left",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentGold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. PLAY MODE SELECTOR (30% Secondary Wells, 10% Accent Selection)
                item {
                    Text(
                        text = "Select Play Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isTournamentUnlocked = elo >= 1300
                        var showLockedNotice by remember { mutableStateOf(false) }

                        PlayMode.entries.forEach { mode ->
                            val isSelected = selectedMode == mode
                            val isLocked = mode == PlayMode.TOURNAMENT && !isTournamentUnlocked

                            val container = when {
                                isLocked -> palette.elevatedSurface.copy(alpha = 0.65f)
                                isSelected -> accentColor
                                else -> palette.cardSurface
                            }

                            IndoToneSurface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (isLocked) {
                                            SoundHelper.playClick(context)
                                            showLockedNotice = true
                                        } else {
                                            SoundHelper.playClick(context)
                                            selectedMode = mode
                                            players.clear()
                                            players.addAll(createDefaultPlayerSetups(gameType, playerCount, mode, playerOneName))
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = container,
                                hairlineColor = if (isSelected) Color.Transparent else palette.goldHairline
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 13.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Icon(
                                            imageVector = when (mode) {
                                                PlayMode.BOT -> Icons.Default.SmartToy
                                                PlayMode.PASS_AND_PLAY -> Icons.Default.People
                                                PlayMode.ONLINE_RANKED -> Icons.Default.Public
                                                PlayMode.TOURNAMENT -> Icons.Default.EmojiEvents
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                isSelected -> Color.White
                                                else -> accentColor
                                            },
                                            modifier = Modifier.size(21.dp)
                                        )
                                        if (isLocked) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = palette.accentGold,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .offset(x = 6.dp, y = (-4).dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = mode.title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            isSelected -> Color.White
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (showLockedNotice) {
                            AlertDialog(
                                onDismissRequest = { showLockedNotice = false },
                                confirmButton = {
                                    TextButton(onClick = { showLockedNotice = false }) {
                                        Text("Got It", color = accentColor, fontWeight = FontWeight.Bold)
                                    }
                                },
                                title = { Text("Championship Locked") },
                                text = { Text("Championship mode opens when hosted and unlocked by reaching Tactician (1300+ ELO) in Ranked play.") }
                            )
                        }
                    }
                }

                // 3. PLAYER COUNT (LUDO)
                if (gameType == GameType.LUDO) {
                    item {
                        Text(
                            text = "Number of Players",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(2, 3, 4).forEach { count ->
                                val isSelected = playerCount == count
                                IndoToneSurface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            SoundHelper.playClick(context)
                                            playerCount = count
                                            players.clear()
                                            players.addAll(createDefaultPlayerSetups(gameType, count, selectedMode, playerOneName))
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    containerColor = if (isSelected) accentColor else palette.cardSurface,
                                    hairlineColor = if (isSelected) Color.Transparent else palette.goldHairline
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$count Players",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. PLAYER NAMES ONLY FOR PASS & PLAY; VIDYA ID / BOT OTHERWISE
                if (selectedMode == PlayMode.PASS_AND_PLAY) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pass & Play Roster",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = secondaryBg
                            ) {
                                Text(
                                    text = "${players.size} Players",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    itemsIndexed(players) { index, player ->
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.goldHairline
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(player.colorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (player.colorHex == 0xFFFFFFFFL) Color.Black else Color.White
                                    )
                                }

                                OutlinedTextField(
                                    value = player.name,
                                    onValueChange = { newName ->
                                        players[index] = player.copy(name = newName)
                                    },
                                    label = { Text(player.roleTitle) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.goldHairline
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ScholarAvatarBadge(
                                    avatarSpec = userProfile?.toAvatarSpec() ?: AvatarSpec(),
                                    displayName = playerOneName,
                                    size = 48.dp,
                                    isOnline = selectedMode == PlayMode.ONLINE_RANKED
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (selectedMode == PlayMode.ONLINE_RANKED) "Vidya ID Ranked Queue" else "Vidyut AI Bot Practice",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    VidyaIdChip(
                                        vidyaId = playerVidyaId,
                                        compact = true,
                                        showCopyIcon = false
                                    )
                                    Text(
                                        text = if (selectedMode == PlayMode.ONLINE_RANKED)
                                            "Competing as $playerOneName ($playerVidyaId). Auto Vidyut Bot fallback if unmatched."
                                        else
                                            "Playing as $playerOneName ($playerVidyaId) against adaptive AI.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. DIFFICULTY PILLS
                item {
                    Text(
                        text = "Bot Difficulty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("NOVICE", "TACTICIAN", "GRANDMASTER").forEach { diff ->
                            val isSelected = selectedDifficulty == diff
                            IndoToneSurface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        SoundHelper.playClick(context)
                                        selectedDifficulty = diff
                                    },
                                shape = RoundedCornerShape(14.dp),
                                containerColor = if (isSelected) accentColor else palette.cardSurface,
                                hairlineColor = if (isSelected) Color.Transparent else palette.goldHairline
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. PRIMARY CTA BUTTON (10% Accent Reserved Here)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            SoundHelper.playFolderOpen(context)
                            if (selectedMode == PlayMode.ONLINE_RANKED) {
                                isMatchmaking = true
                                matchmakingSecondsLeft = 45
                                matchedOpponentName = null
                                activeTicketId = null
                                coroutineScope.launch {
                                    val authUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull().orEmpty()
                                    val initialBoardSerialized = when (gameType) {
                                        GameType.SHATRANJ -> ChessGameState.initialBoard().serialize()
                                        GameType.LUDO -> LudoGameState.initialGame(listOf(LudoColor.RED, LudoColor.YELLOW)).serialize()
                                        GameType.CHATURANGA -> ChaturangaState.initialBoard().serialize()
                                    }

                                    var liveMatchId = ""
                                    var localIndex = 0
                                    var foundOpponent = false
                                    var opponentLabel = "Vidyut AI (vidyut#1001)"
                                    var opponentElo = elo

                                    if (authUid.isNotBlank() && !authUid.startsWith("demo_")) {
                                        val queueResult = onlineMatchRepo.findOrQueueMatch(
                                            uid = authUid,
                                            vidyaId = playerVidyaId,
                                            displayName = playerOneName,
                                            gameType = gameType,
                                            eloRating = elo,
                                            requiredPlayers = if (gameType == GameType.SHATRANJ) 2 else playerCount,
                                            initialSerializedState = initialBoardSerialized
                                        )

                                        when (queueResult) {
                                            is MatchmakingQueueResult.Matched -> {
                                                foundOpponent = true
                                                liveMatchId = queueResult.matchId
                                                localIndex = queueResult.localPlayerIndex
                                                opponentLabel = "${queueResult.opponentName} (${queueResult.opponentVidyaId})"
                                                opponentElo = queueResult.opponentElo
                                            }
                                            is MatchmakingQueueResult.WaitingOnTicket -> {
                                                activeTicketId = queueResult.ticketId
                                                val countdownJob = launch {
                                                    for (sec in 44 downTo 0) {
                                                        if (!isMatchmaking) break
                                                        delay(1000)
                                                        matchmakingSecondsLeft = sec
                                                    }
                                                }
                                                val matchedResult = onlineMatchRepo.awaitLiveMatch(
                                                    myTicketId = queueResult.ticketId,
                                                    uid = authUid,
                                                    gameType = gameType,
                                                    eloRating = elo,
                                                    initialSerializedState = initialBoardSerialized,
                                                    timeoutMs = OnlineMatchRepository.DEFAULT_QUEUE_TIMEOUT_MS
                                                )
                                                countdownJob.cancel()
                                                if (!isMatchmaking) {
                                                    onlineMatchRepo.cancelTicket(queueResult.ticketId)
                                                    activeTicketId = null
                                                    return@launch
                                                }
                                                if (matchedResult != null) {
                                                    foundOpponent = true
                                                    liveMatchId = matchedResult.matchId
                                                    localIndex = matchedResult.localPlayerIndex
                                                    opponentLabel = "${matchedResult.opponentName} (${matchedResult.opponentVidyaId})"
                                                    opponentElo = matchedResult.opponentElo
                                                } else {
                                                    onlineMatchRepo.cancelTicket(queueResult.ticketId)
                                                }
                                                activeTicketId = null
                                            }
                                            MatchmakingQueueResult.FallbackToBot -> {
                                                delay(1200)
                                            }
                                        }
                                    } else {
                                        // Unauthenticated / offline preview waits briefly then falls back cleanly to Vidyut AI
                                        for (sec in 3 downTo 0) {
                                            if (!isMatchmaking) return@launch
                                            matchmakingSecondsLeft = sec
                                            delay(650)
                                        }
                                    }

                                    if (!isMatchmaking) return@launch
                                    matchedOpponentName = opponentLabel
                                    matchedOpponentElo = opponentElo
                                    delay(450)
                                    isMatchmaking = false

                                    val updatedPlayers = players.mapIndexed { i, p ->
                                        if (i == 0) p.copy(name = "$playerOneName ($playerVidyaId)")
                                        else p.copy(name = "$opponentLabel • $opponentElo", isBot = !foundOpponent)
                                    }
                                    onStartGame(
                                        GameSessionConfig(
                                            gameTypeName = gameType.name,
                                            mode = selectedMode,
                                            playerCount = playerCount,
                                            players = updatedPlayers,
                                            difficulty = selectedDifficulty,
                                            timeControlMinutes = selectedTimeControl,
                                            onlineMatchId = liveMatchId,
                                            localParticipantUid = authUid,
                                            localPlayerIndex = localIndex
                                        )
                                    )
                                }
                            } else {
                                val finalPlayers = if (selectedMode == PlayMode.BOT) {
                                    players.mapIndexed { i, p ->
                                        if (i == 0) p.copy(name = "$playerOneName ($playerVidyaId)") else p
                                    }
                                } else players.toList()

                                onStartGame(
                                    GameSessionConfig(
                                        gameTypeName = gameType.name,
                                        mode = selectedMode,
                                        playerCount = playerCount,
                                        players = finalPlayers,
                                        difficulty = selectedDifficulty,
                                        timeControlMinutes = selectedTimeControl,
                                        tournamentRound = if (selectedMode == PlayMode.TOURNAMENT) "Quarter-Finals" else ""
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_game_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (selectedMode == PlayMode.ONLINE_RANKED) Icons.Default.Public else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedMode == PlayMode.ONLINE_RANKED) "Find Ranked Match" else "Start Match",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    IndoToneSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                SoundHelper.playClick(context)
                                onOpenRulebook()
                            },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = palette.accentGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Read Ultimate Rulebook", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (isMatchmaking) {
        AlertDialog(
            onDismissRequest = {
                isMatchmaking = false
                val pending = activeTicketId
                activeTicketId = null
                if (!pending.isNullOrBlank()) {
                    coroutineScope.launch { onlineMatchRepo.cancelTicket(pending) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isMatchmaking = false
                        val pending = activeTicketId
                        activeTicketId = null
                        if (!pending.isNullOrBlank()) {
                            coroutineScope.launch { onlineMatchRepo.cancelTicket(pending) }
                        }
                    }
                ) {
                    Text("Cancel", color = accentColor)
                }
            },
            title = { Text("Searching Ranked Queue...", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = accentColor)
                    Text(
                        text = "Matching $rankTier (${elo - 50}..${elo + 50} ELO)\nAuto bot fallback in ${matchmakingSecondsLeft}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (matchedOpponentName != null) {
                        IndoToneSurface(
                            shape = RoundedCornerShape(12.dp),
                            containerColor = palette.secondaryCopper,
                            hairlineColor = palette.goldHairline
                        ) {
                            Text(
                                text = "Connected: $matchedOpponentName ($matchedOpponentElo ELO)",
                                modifier = Modifier.padding(10.dp),
                                fontWeight = FontWeight.Bold,
                                color = palette.accentCyan
                            )
                        }
                    }
                }
            }
        )
    }
}

private fun createDefaultPlayerSetups(
    gameType: GameType,
    count: Int,
    mode: PlayMode,
    playerOneName: String = "You"
): List<PlayerSetup> {
    return when (gameType) {
        GameType.SHATRANJ -> listOf(
            PlayerSetup(0, playerOneName, "Player 1", false, 0xFFFFFFFFL),
            PlayerSetup(1, "Black", if (mode == PlayMode.BOT) "Vidyut AI" else "Player 2", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFF1E293BL)
        )
        GameType.CHATURANGA -> listOf(
            PlayerSetup(0, playerOneName, "Player 1", false, 0xFFE11D48L),
            PlayerSetup(1, "Green Army", if (mode == PlayMode.BOT) "Vidyut Green" else "Player 2", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFF059669L),
            PlayerSetup(2, "Yellow Army", if (mode == PlayMode.BOT) "Vidyut Yellow" else "Player 3", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFFF59E0BL),
            PlayerSetup(3, "Blue Army", if (mode == PlayMode.BOT) "Vidyut Blue" else "Player 4", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFF0284C7L)
        )
        GameType.LUDO -> {
            val full = listOf(
                PlayerSetup(0, playerOneName, "Player 1", false, 0xFFE11D48L),
                PlayerSetup(1, "Green Quadrant", if (mode == PlayMode.BOT) "Vidyut Green" else "Player 2", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFF059669L),
                PlayerSetup(2, "Yellow Quadrant", if (mode == PlayMode.BOT) "Vidyut Yellow" else "Player 3", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFFF59E0BL),
                PlayerSetup(3, "Blue Quadrant", if (mode == PlayMode.BOT) "Vidyut Blue" else "Player 4", mode == PlayMode.BOT || mode == PlayMode.ONLINE_RANKED, 0xFF0284C7L)
            )
            full.take(count)
        }
    }
}
