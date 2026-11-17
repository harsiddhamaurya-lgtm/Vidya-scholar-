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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.games.chess.*
import com.example.games.config.PlayMode
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.GameMusicPlayer
import com.example.viewmodel.GameType
import com.example.viewmodel.GamesViewModel
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ChessGameView(
    viewModel: GamesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val state by viewModel.chessState.collectAsStateWithLifecycle()
    val selectedPos = viewModel.selectedChessPos
    val validMoves = viewModel.validChessDestinations
    var isMuted by remember { mutableStateOf(GameMusicPlayer.isMuted) }

    DisposableEffect(Unit) {
        GameMusicPlayer.startMusic(GameType.SHATRANJ)
        onDispose {
            GameMusicPlayer.stopMusic()
        }
    }

    val whiteName = viewModel.getPlayerName(0, "White (You)")
    val blackName = viewModel.getPlayerName(1, if (viewModel.chessMode == "BOT") "Vidyut AI" else "Black")
    val config = viewModel.activeGameConfig.collectAsStateWithLifecycle().value

    val statusColor = when {
        state.isCheckmate -> palette.accentVermilion
        state.isCheck -> palette.accentSaffron
        else -> palette.cardSurface
    }
    val statusTextColor = if (state.isCheck || state.isCheckmate) Color.White else MaterialTheme.colorScheme.onSurface

    GameZoneBackdrop(ambientGlowColor = palette.accentSaffron) {
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
                // COMPACT EDGE-ANCHORED TOP HUD
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
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chess_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Shatranj • Chess",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (config?.mode) {
                                    PlayMode.ONLINE_RANKED -> "Ranked (+24 / -14 ELO)"
                                    PlayMode.TOURNAMENT -> "Tournament: ${config.tournamentRound}"
                                    else -> "$whiteName vs $blackName"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(
                                onClick = { isMuted = GameMusicPlayer.toggleMute(GameType.SHATRANJ) },
                                modifier = Modifier.testTag("chess_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute Arena" else "Mute Arena",
                                    tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else palette.accentSaffron
                                )
                            }
                            IconButton(onClick = { viewModel.showRulebook(GameType.SHATRANJ) }) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Rulebook", tint = palette.accentGold)
                            }
                            IconButton(onClick = { viewModel.resetChess() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // SLIM TURN + OPPONENT CAPTURED TRAY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IndoToneSurface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = statusColor,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.currentTurn == PieceColor.WHITE) Color.White
                                        else palette.accentSaffron
                                    )
                            )
                            Text(
                                text = when {
                                    state.isCheckmate -> "Checkmate! ${if (state.currentTurn == PieceColor.WHITE) blackName else whiteName}"
                                    state.isCheck -> "Check! ${if (state.currentTurn == PieceColor.WHITE) whiteName else blackName}"
                                    state.currentTurn == PieceColor.WHITE -> "Turn: $whiteName"
                                    viewModel.chessMode == "BOT" -> "$blackName thinking..."
                                    else -> "Turn: $blackName"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = statusTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ChessModePill(
                            label = "Bot",
                            selected = viewModel.chessMode == "BOT",
                            accent = palette.accentSaffron,
                            onClick = {
                                viewModel.chessMode = "BOT"
                                viewModel.resetChess()
                            }
                        )
                        ChessModePill(
                            label = "2P",
                            selected = viewModel.chessMode == "PASS_AND_PLAY",
                            accent = palette.accentSaffron,
                            onClick = {
                                viewModel.chessMode = "PASS_AND_PLAY"
                                viewModel.resetChess()
                            }
                        )
                    }
                }

                // MAXIMIZED 8x8 SHATRANJ BOARD WITH MATTE SURFACES, METALLIC GOLD FRAME & GPU GLIDE PHYSICS
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShatranjBoardCanvasAndPieces(
                        state = state,
                        selectedPos = selectedPos,
                        validMoves = validMoves,
                        onSquareClick = { pos -> viewModel.onChessSquareClick(pos, context) }
                    )
                }

                // COMPACT BOTTOM CAPTURED PIECES & DIFFICULTY DOCK
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = palette.cardSurface.copy(alpha = 0.92f),
                    hairlineColor = palette.goldHairline
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Captured: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.accentSaffron)
                                val blackLost = state.capturedBlack.takeLast(8).joinToString("") { getPieceSymbol(it) }
                                Text(
                                    text = blackLost.ifEmpty { "—" },
                                    fontSize = 14.sp,
                                    color = palette.accentSaffron,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lost: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val whiteLost = state.capturedWhite.takeLast(8).joinToString("") { getPieceSymbol(it) }
                                Text(
                                    text = whiteLost.ifEmpty { "—" },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (viewModel.chessMode == "BOT") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vidyut Engine",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ChessModePill(
                                        label = "Easy",
                                        selected = viewModel.chessDifficulty == "EASY",
                                        accent = palette.accentSaffron,
                                        onClick = { viewModel.chessDifficulty = "EASY" }
                                    )
                                    ChessModePill(
                                        label = "Master",
                                        selected = viewModel.chessDifficulty == "MASTER",
                                        accent = palette.accentSaffron,
                                        onClick = { viewModel.chessDifficulty = "MASTER" }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GEOMETRIC MANDALA VICTORY CELEBRATION OVERLAY
            val winnerName = if (state.currentTurn == PieceColor.WHITE) blackName else whiteName
            VictoryMandalaOverlay(
                visible = state.isCheckmate,
                winnerTitle = "Checkmate • $winnerName",
                subtitle = "Royal victory on the Shatranj board",
                accentColor = palette.accentSaffron,
                onRematch = { viewModel.resetChess() },
                onExit = onBack
            )
        }
    }
}

@Composable
private fun ShatranjBoardCanvasAndPieces(
    state: ChessGameState,
    selectedPos: Position?,
    validMoves: List<Position>,
    onSquareClick: (Position) -> Unit
) {
    val palette = LocalIndoPalette.current
    val density = LocalDensity.current
    val lastMove = state.moveHistory.lastOrNull()
    val moveCount = state.moveHistory.size

    // Smooth weighted glide (CubicBezier(0.22, 1, 0.36, 1), 300ms)
    val glideAnim = remember { Animatable(1f) }
    var captureBurstKey by remember { mutableStateOf(0) }
    var burstPos by remember { mutableStateOf(Position(0, 0)) }

    LaunchedEffect(moveCount) {
        if (moveCount > 0 && lastMove != null) {
            glideAnim.snapTo(0f)
            glideAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
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

    // Pulsing legal target indicator
    val infiniteTransition = rememberInfiniteTransition(label = "chess_targets")
    val targetPulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "target_ring"
    )

    // 30% Matte Board Colors + Metallic Gold Frame
    val frameOuterColor = if (palette.isDark) Color(0xFF1A1512) else Color(0xFF3A2C22)
    val lightSq = when {
        palette.isEInk -> Color(0xFFFFFFFF)
        palette.isDark -> Color(0xFF4A423E)
        else -> Color(0xFFF6F0E6)           // High contrast bright ivory / sandstone light
    }
    val darkSq = when {
        palette.isEInk -> Color(0xFF888888)
        palette.isDark -> Color(0xFF181D26) // Deep contrast slate dark
        else -> Color(0xFF6B4423)           // High contrast rich walnut brown
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

            // LAYER 1: 8x8 Matte Grid + Legal Move Target Rings + Tap Hitboxes
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0..7) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..7) {
                            val pos = Position(r, c)
                            val piece = state.board[r][c]
                            val isSelected = selectedPos == pos
                            val isValidMove = validMoves.contains(pos)
                            val isLastEndpoint = lastMove != null && (lastMove.from == pos || lastMove.to == pos)
                            val isLight = (r + c) % 2 == 0

                            val baseSquare = if (isLight) lightSq else darkSq

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(baseSquare)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onSquareClick(pos)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Subtle last-move tint
                                if (isLastEndpoint && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(palette.accentGold.copy(alpha = 0.14f))
                                    )
                                }
                                // Selected square glow under piece
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(palette.accentGold.copy(alpha = 0.25f))
                                            .border(1.5.dp, palette.accentGold.copy(alpha = 0.8f))
                                    )
                                }
                                // Pulsing geometric target ring for valid destination or capture target
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
                                                radius = size.minDimension * 0.46f,
                                                style = Stroke(width = 2.4.dp.toPx())
                                            )
                                        } else {
                                            drawCircle(
                                                color = ringColor.copy(alpha = 0.35f),
                                                radius = size.minDimension * 0.32f,
                                                style = Stroke(width = 1.6.dp.toPx())
                                            )
                                            drawCircle(
                                                color = ringColor.copy(alpha = 0.92f),
                                                radius = size.minDimension * 0.14f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // LAYER 2: GPU-Animated Chess Pieces (Selection Lift + Weighted Bezier Glide)
            Box(modifier = Modifier.fillMaxSize()) {
                for (r in 0..7) {
                    for (c in 0..7) {
                        val piece = state.board[r][c] ?: continue
                        val pos = Position(r, c)
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
                            label = "chess_select_scale"
                        )
                        val selectionLift by animateFloatAsState(
                            targetValue = if (isSelected) liftPx else 0f,
                            animationSpec = tween(160, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
                            label = "chess_select_lift"
                        )

                        val glidePulse = if (isJustMoved) 1f + 0.12f * sin(PI * glideAnim.value).toFloat() else 1f
                        val pieceTint = if (piece.color == PieceColor.WHITE) {
                            if (palette.isDark) Color(0xFFFFFFFF) else Color(0xFF140D08)
                        } else {
                            Color(0xFFF59E0B)
                        }

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
                                text = getPieceSymbol(piece),
                                fontSize = 29.sp,
                                fontWeight = FontWeight.Bold,
                                color = pieceTint
                            )
                        }
                    }
                }
            }

            // LAYER 3: Capture Particle Burst & Shockwave Ripple
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
private fun ChessModePill(
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
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getPieceSymbol(piece: ChessPiece): String {
    return when (piece.type) {
        PieceType.KING -> if (piece.color == PieceColor.WHITE) "♔" else "♚"
        PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♕" else "♛"
        PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♖" else "♜"
        PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♗" else "♝"
        PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♘" else "♞"
        PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♙" else "♟"
    }
}
