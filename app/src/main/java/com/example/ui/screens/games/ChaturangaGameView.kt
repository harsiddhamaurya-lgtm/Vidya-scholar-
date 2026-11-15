package com.example.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.chaturanga.*
import com.example.games.config.PlayMode
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.GameMusicPlayer
import com.example.viewmodel.GameType
import com.example.viewmodel.GamesViewModel
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ChaturangaGameView(
    viewModel: GamesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val state by viewModel.chaturangaState.collectAsState()
    val selectedPos = viewModel.selectedChaturangaPos
    val validMoves = viewModel.validChaturangaDestinations
    var isMuted by remember { mutableStateOf(GameMusicPlayer.isMuted) }

    DisposableEffect(Unit) {
        GameMusicPlayer.startMusic(GameType.CHATURANGA)
        onDispose {
            GameMusicPlayer.stopMusic()
        }
    }

    val redName = viewModel.getPlayerName(0, "Red")
    val greenName = viewModel.getPlayerName(1, "Green")
    val yellowName = viewModel.getPlayerName(2, "Yellow")
    val blackName = viewModel.getPlayerName(3, "Blue")
    val config = viewModel.activeGameConfig.collectAsState().value

    fun getPlayerTitle(p: ChaturangaPlayer): String {
        return when (p) {
            ChaturangaPlayer.RED -> redName
            ChaturangaPlayer.GREEN -> greenName
            ChaturangaPlayer.YELLOW -> yellowName
            ChaturangaPlayer.BLACK -> blackName
        }
    }

    val activeColor = getChaturangaColor(state.currentTurn)

    GameZoneBackdrop(ambientGlowColor = activeColor, isAshtapada = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // COMPACT TOP ARENA HUD
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = palette.cardSurface.copy(alpha = 0.92f),
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chaturanga_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Chaturanga • Ashtapada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (config?.mode) {
                                    PlayMode.ONLINE_RANKED -> "Online Ranked (+24 / -14 ELO)"
                                    PlayMode.TOURNAMENT -> "Tournament: ${config.tournamentRound}"
                                    else -> state.logMessage
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = activeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(
                                onClick = { isMuted = GameMusicPlayer.toggleMute(GameType.CHATURANGA) },
                                modifier = Modifier.testTag("chaturanga_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute Arena" else "Mute Arena",
                                    tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else palette.accentCyan
                                )
                            }
                            IconButton(onClick = { viewModel.showRulebook(GameType.CHATURANGA) }) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Rulebook", tint = palette.accentGold)
                            }
                            IconButton(onClick = { viewModel.resetChaturanga() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // COMPACT 4-ARMY SCOREBOARD STRIP
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = palette.cardSurface.copy(alpha = 0.88f),
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChaturangaPlayer.entries.forEach { player ->
                            val color = getChaturangaColor(player)
                            val isTurn = state.currentTurn == player
                            val isEliminated = state.eliminatedPlayers.contains(player)
                            val score = state.playerScores[player] ?: 0

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isTurn) color else palette.elevatedSurface.copy(alpha = 0.7f))
                                    .padding(vertical = 5.dp, horizontal = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isEliminated) Color.Gray else if (isTurn) Color.White else color)
                                    )
                                    Text(
                                        text = getPlayerTitle(player),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isEliminated -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            isTurn -> Color.White
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (isEliminated) "Out" else "${score}p",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTurn) Color.White.copy(alpha = 0.92f) else color
                                )
                            }
                        }
                    }
                }

                // MAXIMIZED 8x8 ASHTAPADA BOARD WITH GPU WEIGHTED GLIDE & CAPTURE SHOCKWAVE
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AshtapadaBoardCanvasAndPieces(
                        state = state,
                        selectedPos = selectedPos,
                        validMoves = validMoves,
                        onSquareClick = { pos -> viewModel.onChaturangaSquareClick(pos, context) }
                    )
                }

                // COMPACT BOTTOM PASA DICE & MODE DOCK
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = palette.cardSurface.copy(alpha = 0.92f),
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = if (!state.hasRolled) "Roll Pasa • ${getPlayerTitle(state.currentTurn)}"
                                       else "Move: ${state.allowedPieceType.joinToString { it.title.split(" ")[0] }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ChaturangaModePill(
                                    label = "vs 3 Bots",
                                    selected = viewModel.chaturangaMode == "BOT",
                                    accent = palette.accentCyan,
                                    onClick = {
                                        viewModel.chaturangaMode = "BOT"
                                        viewModel.resetChaturanga()
                                    }
                                )
                                ChaturangaModePill(
                                    label = "4P Local",
                                    selected = viewModel.chaturangaMode == "PASS_AND_PLAY",
                                    accent = palette.accentCyan,
                                    onClick = {
                                        viewModel.chaturangaMode = "PASS_AND_PLAY"
                                        viewModel.resetChaturanga()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        val pieceHint = if (state.hasRolled) {
                            state.allowedPieceType.firstOrNull()?.title?.split(" ")?.firstOrNull()
                        } else null

                        val rulebookOpen = viewModel.rulebookGameType.collectAsState().value != null
                        AnimatedPasaDice(
                            diceValue = state.lastDiceValue,
                            isRolling = viewModel.isChaturangaDiceRolling,
                            accentColor = activeColor,
                            enabled = !state.hasRolled && !viewModel.isChaturangaDiceRolling && !state.isGameOver,
                            testTag = "chaturanga_dice_button",
                            isActive = !state.isGameOver && !rulebookOpen,
                            pieceHint = pieceHint,
                            onClick = { viewModel.onRollChaturangaDice(context) }
                        )
                    }
                }
            }

            // GEOMETRIC MANDALA VICTORY OVERLAY
            val topScorer = state.playerScores.maxByOrNull { it.value }?.key ?: ChaturangaPlayer.RED
            val winnerTitle = "${getPlayerTitle(topScorer)} Prevails!"
            VictoryMandalaOverlay(
                visible = state.isGameOver,
                winnerTitle = winnerTitle,
                subtitle = "Ashtapada conquest complete • ${state.playerScores[topScorer] ?: 0} points",
                accentColor = getChaturangaColor(topScorer),
                onRematch = { viewModel.resetChaturanga() },
                onExit = onBack
            )
        }
    }
}

@Composable
private fun AshtapadaBoardCanvasAndPieces(
    state: ChaturangaState,
    selectedPos: BoardPos?,
    validMoves: List<BoardPos>,
    onSquareClick: (BoardPos) -> Unit
) {
    val palette = LocalIndoPalette.current
    val density = LocalDensity.current
    val lastMove = state.lastMove

    // Weighted glide animation (280-320ms CubicBezier(0.22, 1, 0.36, 1))
    val glideAnim = remember { Animatable(1f) }
    var captureBurstKey by remember { mutableStateOf(0) }
    var burstPos by remember { mutableStateOf(BoardPos(0, 0)) }

    LaunchedEffect(lastMove, state.playerScores, state.currentTurn) {
        if (lastMove != null) {
            glideAnim.snapTo(0f)
            glideAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 290,
                    easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                )
            )
            if (lastMove.captured != null) {
                burstPos = lastMove.to
                captureBurstKey++
            }
        } else {
            glideAnim.snapTo(1f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "chaturanga_targets")
    val targetPulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "target_pulse"
    )

    val frameOuterColor = if (palette.isDark) Color(0xFF101624) else Color(0xFF332920)
    val lightSq = when {
        palette.isEInk -> Color(0xFFF6F3EB)
        palette.isDark -> Color(0xFF31394A)
        else -> Color(0xFFECE3D3)
    }
    val darkSq = when {
        palette.isEInk -> Color(0xFFD9D3C5)
        palette.isDark -> Color(0xFF1C2332)
        else -> Color(0xFFC5B19A)
    }

    // Traditional Ashtapada sacred marked squares (4 corners & 4 center squares)
    val ashtapadaMarked = remember {
        setOf(
            BoardPos(0, 0), BoardPos(0, 7), BoardPos(7, 0), BoardPos(7, 7),
            BoardPos(3, 3), BoardPos(3, 4), BoardPos(4, 3), BoardPos(4, 4)
        )
    }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = frameOuterColor,
        border = BorderStroke(1.2.dp, palette.accentGold.copy(alpha = 0.55f)),
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(0.8.dp, palette.accentGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
        ) {
            val boardPx = constraints.maxWidth.toFloat()
            val cellPx = boardPx / 8f
            val cellDp = with(density) { cellPx.toDp() }
            val liftPx = with(density) { (-6).dp.toPx() }

            // LAYER 1: 8x8 Matte Ashtapada Grid + Etched X Marks + Target Rings
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0..7) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..7) {
                            val pos = BoardPos(r, c)
                            val piece = state.board[r][c]
                            val isSelected = selectedPos == pos
                            val isValidMove = validMoves.contains(pos)
                            val isMovablePiece = state.hasRolled && piece?.player == state.currentTurn &&
                                !piece.isDead && state.allowedPieceType.contains(piece.type)
                            val isLight = (r + c) % 2 == 0
                            val isSacredMark = ashtapadaMarked.contains(pos)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isLight) lightSq else darkSq)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSquareClick(pos)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Traditional Ashtapada etched cross on sacred squares (subtle 12% gold)
                                if (isSacredMark && !palette.isEInk) {
                                    Canvas(modifier = Modifier.fillMaxSize(0.75f)) {
                                        val markColor = palette.accentGold.copy(alpha = 0.14f)
                                        drawLine(markColor, Offset.Zero, Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                                        drawLine(markColor, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx())
                                    }
                                }

                                if (isMovablePiece && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(palette.accentGold.copy(alpha = 0.16f))
                                            .border(1.dp, palette.accentGold.copy(alpha = 0.45f))
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(palette.accentGold.copy(alpha = 0.28f))
                                            .border(1.5.dp, palette.accentGold.copy(alpha = 0.85f))
                                    )
                                }

                                if (isValidMove) {
                                    val ringColor = if (piece != null) palette.accentVermilion else palette.accentCyan
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize(0.76f)
                                            .graphicsLayer {
                                                scaleX = targetPulse
                                                scaleY = targetPulse
                                            }
                                    ) {
                                        if (piece != null) {
                                            drawCircle(
                                                color = ringColor.copy(alpha = 0.9f),
                                                radius = size.minDimension * 0.45f,
                                                style = Stroke(width = 2.2.dp.toPx())
                                            )
                                        } else {
                                            drawCircle(
                                                color = ringColor.copy(alpha = 0.35f),
                                                radius = size.minDimension * 0.30f,
                                                style = Stroke(width = 1.5.dp.toPx())
                                            )
                                            drawCircle(
                                                color = ringColor.copy(alpha = 0.92f),
                                                radius = size.minDimension * 0.13f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LAYER 2: GPU-Animated Chaturanga Pieces (Lift & Glow + Weighted Glide)
            Box(modifier = Modifier.fillMaxSize()) {
                for (r in 0..7) {
                    for (c in 0..7) {
                        val piece = state.board[r][c] ?: continue
                        val pos = BoardPos(r, c)
                        val isSelected = selectedPos == pos
                        val isJustMoved = lastMove != null && lastMove.to == pos && glideAnim.value < 0.999f

                        val renderCol = if (isJustMoved && lastMove != null) {
                            lastMove.from.col + (lastMove.to.col - lastMove.from.col) * glideAnim.value
                        } else {
                            c.toFloat()
                        }
                        val renderRow = if (isJustMoved && lastMove != null) {
                            lastMove.from.row + (lastMove.to.row - lastMove.from.row) * glideAnim.value
                        } else {
                            r.toFloat()
                        }

                        val selectionScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1f,
                            animationSpec = tween(160, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                            label = "chat_select_scale"
                        )
                        val selectionLift by animateFloatAsState(
                            targetValue = if (isSelected) liftPx else 0f,
                            animationSpec = tween(160, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                            label = "chat_select_lift"
                        )

                        val glidePulse = if (isJustMoved) 1f + 0.12f * sin(PI * glideAnim.value).toFloat() else 1f
                        val pieceColor = if (piece.isDead) Color.Gray else getChaturangaColor(piece.player)

                        Box(
                            modifier = Modifier
                                .size(cellDp)
                                .graphicsLayer {
                                    translationX = renderCol * cellPx
                                    translationY = renderRow * cellPx + selectionLift
                                    scaleX = selectionScale * glidePulse
                                    scaleY = selectionScale * glidePulse
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(cellDp * 0.72f)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.28f))
                                )
                            }
                            Text(
                                text = piece.type.symbol,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = pieceColor
                            )
                        }
                    }
                }
            }

            // LAYER 3: Capture Particle Burst
            CaptureParticleBurst(
                burstKey = captureBurstKey,
                gridX = burstPos.col + 0.5f,
                gridY = burstPos.row + 0.5f,
                gridDimension = 8f,
                burstColor = palette.accentCyan
            )
        }
    }
}

@Composable
private fun ChaturangaModePill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent else palette.elevatedSurface,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

fun getChaturangaColor(player: ChaturangaPlayer): Color {
    return when (player) {
        ChaturangaPlayer.RED -> Color(0xFFE11D48)
        ChaturangaPlayer.GREEN -> Color(0xFF059669)
        ChaturangaPlayer.YELLOW -> Color(0xFFF59E0B)
        ChaturangaPlayer.BLACK -> Color(0xFF00B4D8)
    }
}
