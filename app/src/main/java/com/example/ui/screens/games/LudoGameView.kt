package com.example.ui.screens.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.config.PlayMode
import com.example.games.ludo.*
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.GameMusicPlayer
import com.example.viewmodel.GameType
import com.example.viewmodel.GamesViewModel

@Composable
fun LudoGameView(
    viewModel: GamesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalIndoPalette.current
    val state by viewModel.ludoState.collectAsState()
    val isRolling = viewModel.isDiceRolling
    var isMuted by remember { mutableStateOf(GameMusicPlayer.isMuted) }

    DisposableEffect(Unit) {
        GameMusicPlayer.startMusic(GameType.LUDO)
        onDispose {
            GameMusicPlayer.stopMusic()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ludo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val redName = viewModel.getPlayerName(0, "Red")
    val greenName = viewModel.getPlayerName(1, "Green")
    val yellowName = viewModel.getPlayerName(2, "Yellow")
    val blueName = viewModel.getPlayerName(3, "Blue")
    val config = viewModel.activeGameConfig.collectAsState().value

    fun getPlayerTitle(color: LudoColor): String {
        return when (color) {
            LudoColor.RED -> redName
            LudoColor.GREEN -> greenName
            LudoColor.YELLOW -> yellowName
            LudoColor.BLUE -> blueName
        }
    }

    val activeColor = getSharedLudoColor(state.currentTurn)

    GameZoneBackdrop(ambientGlowColor = activeColor) {
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
                // COMPACT TOP HUD
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
                        IconButton(onClick = onBack, modifier = Modifier.testTag("ludo_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Pachisi • Ludo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (config?.mode) {
                                    PlayMode.ONLINE_RANKED -> "Ranked (+24 / -14 ELO)"
                                    PlayMode.TOURNAMENT -> "Tournament: ${config.tournamentRound}"
                                    else -> if (viewModel.ludoMode == "BOT") "$redName vs Bots" else "${config?.playerCount ?: 4}P Pass & Play"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(
                                onClick = { isMuted = GameMusicPlayer.toggleMute(GameType.LUDO) },
                                modifier = Modifier.testTag("ludo_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute Arena" else "Mute Arena",
                                    tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else palette.accentCyan
                                )
                            }
                            IconButton(onClick = { viewModel.showRulebook(GameType.LUDO) }) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Rulebook", tint = palette.accentGold)
                            }
                            IconButton(onClick = { viewModel.resetLudo() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // SLIM TURN & PLAYER CHIP BANNER
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = palette.cardSurface.copy(alpha = 0.90f),
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.currentTurn.title.take(1),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "Turn: ${getPlayerTitle(state.currentTurn)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.logMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.elevatedSurface,
                            modifier = Modifier.clickable {
                                viewModel.ludoMode = if (viewModel.ludoMode == "BOT") "PASS_AND_PLAY" else "BOT"
                                viewModel.resetLudo()
                            }
                        ) {
                            Text(
                                text = if (viewModel.ludoMode == "BOT") "vs Bots" else "Local",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentSaffron
                            )
                        }
                    }
                }

                // MAXIMIZED 15x15 PACHISI BOARD
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LudoBoardView(
                        state = state,
                        pulseScale = pulseScale,
                        onTokenClick = { token -> viewModel.onLudoTokenClick(token, context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // COMPACT DICE ACTION DOCK WITH 3D PASA ROLL PHYSICS
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    containerColor = palette.cardSurface.copy(alpha = 0.92f),
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${getPlayerTitle(state.currentTurn)} • ${state.currentTurn.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.isGameOver) "Match Completed"
                                       else if (!state.hasRolled) "Tap Pasa to Roll"
                                       else "Tap a pulsing token to hop",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val rulebookOpen = viewModel.rulebookGameType.collectAsState().value != null
                        AnimatedPasaDice(
                            diceValue = state.lastDiceValue,
                            isRolling = isRolling,
                            accentColor = activeColor,
                            enabled = !state.hasRolled && !isRolling && !state.isGameOver,
                            testTag = "ludo_dice_button",
                            isActive = !state.isGameOver && !rulebookOpen,
                            onClick = { viewModel.onRollLudoDice(context) }
                        )
                    }
                }
            }

            // GEOMETRIC MANDALA VICTORY OVERLAY
            val firstWinner = state.winners.firstOrNull() ?: state.currentTurn
            VictoryMandalaOverlay(
                visible = state.isGameOver,
                winnerTitle = "${getPlayerTitle(firstWinner)} Wins!",
                subtitle = "All 4 tokens entered the Pachisi Home Sanctuary",
                accentColor = getSharedLudoColor(firstWinner),
                onRematch = { viewModel.resetLudo() },
                onExit = onBack
            )
        }
    }
}
