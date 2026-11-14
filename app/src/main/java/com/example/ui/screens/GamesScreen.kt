package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AvatarSpec
import com.example.data.UserProfile
import com.example.ui.components.ScholarAvatarBadge
import com.example.ui.components.VidyaIdChip
import com.example.ui.screens.games.AbstractGameArtBanner
import com.example.ui.screens.games.BadgeSectionView
import com.example.ui.screens.games.ChaturangaGameView
import com.example.ui.screens.games.ChessGameView
import com.example.ui.screens.games.GameHubScreen
import com.example.ui.screens.games.LudoGameView
import com.example.ui.screens.games.RulebookDialog
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.MandalaUnfoldContainer
import com.example.utils.SoundHelper
import com.example.utils.indoTactileMotion
import com.example.viewmodel.GameType
import com.example.viewmodel.GamesViewModel
import androidx.compose.ui.graphics.Path

@Composable
fun GamesScreen(
    userProfile: UserProfile? = null,
    gamesViewModel: GamesViewModel = viewModel()
) {
    val activeGame by gamesViewModel.activeGame.collectAsState()
    val selectedGameForSetup by gamesViewModel.selectedGameForSetup.collectAsState()
    val rulebookGameType by gamesViewModel.rulebookGameType.collectAsState()
    var showBadgeSection by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler(enabled = activeGame != null || selectedGameForSetup != null || showBadgeSection) {
        when {
            activeGame != null -> gamesViewModel.exitToLobby()
            selectedGameForSetup != null -> gamesViewModel.closeGameHub()
            showBadgeSection -> showBadgeSection = false
        }
    }

    if (rulebookGameType != null) {
        RulebookDialog(
            gameType = rulebookGameType!!,
            onDismiss = { gamesViewModel.dismissRulebook() }
        )
    }

    when {
        activeGame == GameType.SHATRANJ -> {
            ChessGameView(
                viewModel = gamesViewModel,
                onBack = {
                    SoundHelper.playClick(context)
                    gamesViewModel.exitToLobby()
                }
            )
        }
        activeGame == GameType.LUDO -> {
            LudoGameView(
                viewModel = gamesViewModel,
                onBack = {
                    SoundHelper.playClick(context)
                    gamesViewModel.exitToLobby()
                }
            )
        }
        activeGame == GameType.CHATURANGA -> {
            ChaturangaGameView(
                viewModel = gamesViewModel,
                onBack = {
                    SoundHelper.playClick(context)
                    gamesViewModel.exitToLobby()
                }
            )
        }
        selectedGameForSetup != null -> {
            MandalaUnfoldContainer {
                GameHubScreen(
                    gameType = selectedGameForSetup!!,
                    userProfile = userProfile,
                    viewModel = gamesViewModel,
                    onBack = {
                        SoundHelper.playClick(context)
                        gamesViewModel.closeGameHub()
                    },
                    onStartGame = { config ->
                        gamesViewModel.startConfiguredGame(config)
                    },
                    onOpenRulebook = {
                        gamesViewModel.showRulebook(selectedGameForSetup!!)
                    }
                )
            }
        }
        showBadgeSection -> {
            MandalaUnfoldContainer {
                BadgeSectionView(
                    viewModel = gamesViewModel,
                    onBack = {
                        SoundHelper.playClick(context)
                        showBadgeSection = false
                    }
                )
            }
        }
        else -> {
            KhelLobbyScreen(
                userProfile = userProfile,
                gamesViewModel = gamesViewModel,
                onSelectGame = { gameType ->
                    SoundHelper.playFolderOpen(context)
                    gamesViewModel.openGameHub(gameType)
                },
                onOpenBadges = {
                    SoundHelper.playFolderOpen(context)
                    showBadgeSection = true
                }
            )
        }
    }
}

private data class GameCardSpec(
    val type: GameType,
    val wellColor: Color,
    val accentColor: Color,
    val secondaryColor: Color,
    val playerBadge: String,
    val icon: ImageVector
)

@Composable
private fun KhelLobbyScreen(
    userProfile: UserProfile?,
    gamesViewModel: GamesViewModel,
    onSelectGame: (GameType) -> Unit,
    onOpenBadges: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val allStats by gamesViewModel.allStats.collectAsState()
    val allBadges by gamesViewModel.allBadges.collectAsState()

    val totalWins = allStats.sumOf { it.matchesWon }
    val totalPlayed = allStats.sumOf { it.matchesPlayed }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentSaffron) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ABSTRACT FLOWING COLOR HERO BANNER + 30% SECONDARY STRUCTURE + 10% GOLD/SAFFRON BADGES CTA
            item {
                IndoToneSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    containerColor = if (palette.isEInk) palette.cardSurface else palette.secondaryIndigo,
                    hairlineColor = palette.goldHairline
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (palette.isEInk) {
                                    Brush.linearGradient(colors = listOf(palette.cardSurface, palette.cardSurface))
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            palette.secondaryIndigo,
                                            palette.secondaryCopper,
                                            palette.secondaryTerracotta
                                        )
                                    )
                                }
                            )
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            if (palette.isEInk) {
                                val ink = Color(0xFF171717)
                                val contour = Path().apply {
                                    moveTo(0f, h * 0.78f)
                                    cubicTo(w * 0.30f, h * 0.35f, w * 0.65f, h * 0.98f, w, h * 0.25f)
                                }
                                drawPath(
                                    path = contour,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ink.copy(alpha = 0.30f), ink.copy(alpha = 0.12f), ink.copy(alpha = 0.03f))
                                    ),
                                    style = Stroke(1.1.dp.toPx())
                                )
                                drawCircle(
                                    color = ink.copy(alpha = 0.18f),
                                    radius = h * 0.45f,
                                    center = Offset(w * 0.90f, h * 0.25f),
                                    style = Stroke(1.dp.toPx())
                                )
                            } else {
                                val ribbon1 = Path().apply {
                                    moveTo(0f, h * 0.78f)
                                    cubicTo(w * 0.30f, h * 0.35f, w * 0.65f, h * 0.98f, w, h * 0.25f)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(
                                    path = ribbon1,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            palette.accentSaffron.copy(alpha = if (palette.isDark) 0.24f else 0.16f),
                                            palette.accentCyan.copy(alpha = if (palette.isDark) 0.24f else 0.16f),
                                            palette.accentVermilion.copy(alpha = if (palette.isDark) 0.18f else 0.12f)
                                        )
                                    )
                                )
                                val ribbon2 = Path().apply {
                                    moveTo(0f, h * 0.22f)
                                    cubicTo(w * 0.28f, h * 0.62f, w * 0.72f, h * 0.05f, w, h * 0.42f)
                                    lineTo(w, 0f)
                                    lineTo(0f, 0f)
                                    close()
                                }
                                drawPath(
                                    path = ribbon2,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            palette.accentCyan.copy(alpha = if (palette.isDark) 0.16f else 0.11f),
                                            palette.accentGold.copy(alpha = if (palette.isDark) 0.16f else 0.11f),
                                            palette.accentSaffron.copy(alpha = if (palette.isDark) 0.18f else 0.12f)
                                        )
                                    )
                                )
                                val c = Offset(w * 0.90f, h * 0.25f)
                                drawCircle(
                                    color = palette.accentGold.copy(alpha = 0.13f),
                                    radius = h * 0.52f,
                                    center = c,
                                    style = Stroke(1.2.dp.toPx())
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ScholarAvatarBadge(
                                        avatarSpec = userProfile?.toAvatarSpec() ?: AvatarSpec(),
                                        displayName = userProfile?.name ?: "Scholar",
                                        size = 46.dp
                                    )
                                    Column {
                                        Text(
                                            text = "Khel • Strategy Arena",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        VidyaIdChip(
                                            vidyaId = userProfile?.effectiveVidyaId() ?: "scholar#1000",
                                            compact = true,
                                            showCopyIcon = false
                                        )
                                    }
                                }
                                Button(
                                    onClick = onOpenBadges,
                                    modifier = Modifier.testTag("open_badges_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accentSaffron,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MilitaryTech,
                                        contentDescription = "Badges",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${allBadges.size} Badges",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatHeroPill(
                                    value = "$totalWins",
                                    label = "Victories",
                                    icon = Icons.Default.EmojiEvents,
                                    iconTint = palette.accentGold,
                                    modifier = Modifier.weight(1f)
                                )
                                StatHeroPill(
                                    value = "$totalPlayed",
                                    label = "Matches",
                                    icon = Icons.Default.SportsEsports,
                                    iconTint = palette.accentCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                StatHeroPill(
                                    value = "Local",
                                    label = "SQLite DB",
                                    icon = Icons.Default.Verified,
                                    iconTint = palette.accentSaffron,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Strategy Games",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val gameCards = listOf(
                GameCardSpec(
                    type = GameType.SHATRANJ,
                    wellColor = palette.secondaryTerracotta,
                    accentColor = palette.accentSaffron,
                    secondaryColor = palette.accentVermilion,
                    playerBadge = "2 Players",
                    icon = Icons.Default.Shield
                ),
                GameCardSpec(
                    type = GameType.LUDO,
                    wellColor = palette.secondaryIndigo,
                    accentColor = palette.accentVermilion,
                    secondaryColor = palette.accentCyan,
                    playerBadge = "2–4 Players",
                    icon = Icons.Default.Casino
                ),
                GameCardSpec(
                    type = GameType.CHATURANGA,
                    wellColor = palette.secondaryCopper,
                    accentColor = palette.accentCyan,
                    secondaryColor = palette.accentGold,
                    playerBadge = "4 Players",
                    icon = Icons.Default.Extension
                )
            )

            items(gameCards) { spec ->
                val stats = allStats.find { it.gameType == spec.type.name }
                IndoToneGameCard(
                    spec = spec,
                    wins = stats?.matchesWon ?: 0,
                    played = stats?.matchesPlayed ?: 0,
                    elo = stats?.eloRating ?: 400,
                    onClick = { onSelectGame(spec.type) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StatHeroPill(
    value: String,
    label: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    IndoToneSurface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        containerColor = palette.cardSurface.copy(alpha = 0.85f),
        hairlineColor = palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IndoToneGameCard(
    spec: GameCardSpec,
    wins: Int,
    played: Int,
    elo: Int,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_card_${spec.type.name.lowercase()}")
            .indoTactileMotion(interactionSource, sweepTint = spec.accentColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() },
        shape = RoundedCornerShape(22.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // RESTORED ABSTRACT ART-TYPE BANNER WITH FLOWING COLOR WAVES & INDO-FUTURISTIC EMBLEM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp)
            ) {
                AbstractGameArtBanner(
                    gameType = spec.type,
                    primaryColor = spec.accentColor,
                    secondaryColor = spec.secondaryColor,
                    accentGold = palette.accentGold,
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bannerTextColor = if (palette.isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    val bannerBadgeBg = when {
                        palette.isEInk -> Color.Transparent
                        palette.isDark -> Color.Black.copy(alpha = 0.34f)
                        else -> Color.White.copy(alpha = 0.58f)
                    }
                    val badgeIconTint = if (palette.isEInk) MaterialTheme.colorScheme.onSurface else palette.accentGold

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bannerBadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = null,
                            tint = bannerTextColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = spec.type.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = bannerTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bannerBadgeBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = badgeIconTint,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = spec.playerBadge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerTextColor
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = spec.type.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.elevatedSurface
                        ) {
                            Text(
                                text = "${wins}W / ${played}P",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = spec.wellColor
                        ) {
                            Text(
                                text = "$elo ELO",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentGold
                            )
                        }
                    }

                    val ctaContentColor = when {
                        palette.isEInk -> MaterialTheme.colorScheme.onPrimary
                        palette.isDark && spec.accentColor != palette.accentVermilion -> Color(0xFF0E1116)
                        else -> Color.White
                    }

                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.accentColor,
                            contentColor = ctaContentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp)
                    ) {
                        Text("Play", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
