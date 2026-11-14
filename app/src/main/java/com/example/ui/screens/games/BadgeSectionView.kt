package com.example.ui.screens.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserBadgeEntity
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.viewmodel.GamesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeSectionView(
    viewModel: GamesViewModel,
    onBack: () -> Unit
) {
    val earnedBadges by viewModel.allBadges.collectAsState()
    val palette = LocalIndoPalette.current

    val showcaseBadges = if (earnedBadges.isNotEmpty()) {
        earnedBadges
    } else {
        listOf(
            UserBadgeEntity(1, "SHATRANJ", "Bronze", 1, "Bronze I", 0xFFCD7F32L),
            UserBadgeEntity(2, "LUDO", "Silver", 2, "Silver II", 0xFF94A3B8L),
            UserBadgeEntity(3, "CHATURANGA", "Gold", 3, "Gold III", 0xFFD4AF37L),
            UserBadgeEntity(4, "SHATRANJ", "Platinum", 1, "Platinum I", 0xFF00D2E6L)
        )
    }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentGold) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Badge Sanctuary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ranked Crests (Bronze I – Platinum VII)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        containerColor = palette.secondaryIndigo,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(palette.cardSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = palette.accentGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Divisional Honor Roll",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (earnedBadges.isNotEmpty())
                                        "${earnedBadges.size} ranked badges stored in your Vidya profile."
                                    else
                                        "Earn unique crests per game from Bronze I up to Platinum VII.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = if (earnedBadges.isNotEmpty()) "Earned Profile Badges" else "Tier Crest Preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(showcaseBadges) { badge ->
                    val tierAccent = when (badge.tierName.uppercase()) {
                        "BRONZE" -> Color(0xFFB45309)
                        "SILVER" -> Color(0xFF64748B)
                        "GOLD" -> palette.accentGold
                        else -> palette.accentCyan
                    }
                    val wellBg = when (badge.gameType) {
                        "SHATRANJ" -> palette.secondaryTerracotta
                        "LUDO" -> palette.secondaryIndigo
                        else -> palette.secondaryCopper
                    }

                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (!palette.isEInk) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    drawCircle(
                                        color = tierAccent.copy(alpha = 0.06f),
                                        radius = size.height * 0.6f,
                                        center = Offset(size.width * 0.9f, size.height * 0.5f),
                                        style = Stroke(1.dp.toPx())
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(wellBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (badge.gameType) {
                                                "SHATRANJ" -> Icons.Default.Shield
                                                "LUDO" -> Icons.Default.Casino
                                                else -> Icons.Default.Extension
                                            },
                                            contentDescription = null,
                                            tint = tierAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = badge.badgeTitle,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${badge.gameType} • ${badge.tierName} Tier",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.elevatedSurface
                                ) {
                                    Text(
                                        text = if (earnedBadges.isNotEmpty()) "Earned" else "Lv ${badge.division}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tierAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
