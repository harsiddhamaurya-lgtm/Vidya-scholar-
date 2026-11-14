package com.example.ui.screens.games

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.games.rulebook.RulebookData
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.MandalaUnfoldContainer
import com.example.viewmodel.GameType

@Composable
fun RulebookDialog(
    gameType: GameType,
    onDismiss: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val rulebook = remember(gameType) { RulebookData.getRulebook(gameType) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Pieces", "Turns & Dice", "Draw Rules", "Loopholes", "Pro Tips")

    val accentColor = when (gameType) {
        GameType.CHATURANGA -> palette.accentCyan
        GameType.SHATRANJ -> palette.accentSaffron
        GameType.LUDO -> palette.accentVermilion
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        MandalaUnfoldContainer {
            IndoToneSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(28.dp),
                containerColor = palette.cardSurface,
                hairlineColor = palette.goldHairline
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.secondaryIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Ultimate Rulebook",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                            Text(
                                text = rulebook.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.elevatedSurface)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = palette.goldHairline
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item {
                                RuleCard(
                                    title = "Historical Origin",
                                    icon = Icons.Default.HistoryEdu,
                                    accentColor = accentColor,
                                    content = rulebook.historicalOrigin
                                )
                            }
                            item {
                                RuleCard(
                                    title = "Core Objective",
                                    icon = Icons.Default.EmojiEvents,
                                    accentColor = accentColor,
                                    content = rulebook.coreObjective
                                )
                            }
                            item {
                                Text(
                                    text = "Setup Overview",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(rulebook.setupOverview) { pointer ->
                                PointerItem(pointer = pointer, accentColor = accentColor)
                            }
                        }

                        1 -> {
                            items(rulebook.pieces) { piece ->
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = palette.cardSurface,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = piece.symbol,
                                                    fontSize = 26.sp,
                                                    color = accentColor
                                                )
                                                Text(
                                                    text = piece.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                color = palette.elevatedSurface,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${piece.points} Pts",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Movement: ${piece.movement}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (piece.specialNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Special: ${piece.specialNotes}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            item {
                                RuleCard(
                                    title = "Turn Mechanics & Play Order",
                                    icon = Icons.Default.Casino,
                                    accentColor = accentColor,
                                    content = "Sequential moves dictated by classic rules and dice logic."
                                )
                            }
                            items(rulebook.turnAndDiceRules) { pointer ->
                                PointerItem(pointer = pointer, accentColor = accentColor)
                            }
                        }

                        3 -> {
                            item {
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = palette.secondaryIndigo,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Balance, contentDescription = null, tint = palette.accentCyan)
                                        Column {
                                            Text(
                                                text = "Complete Draw & Stalemate Rules",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "All conditions leading to a tie, draw, or stalemate.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            items(rulebook.drawRules) { pointer ->
                                PointerItem(pointer = pointer, accentColor = palette.accentCyan)
                            }
                        }

                        4 -> {
                            item {
                                IndoToneSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = palette.secondaryTerracotta,
                                    hairlineColor = palette.goldHairline
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = palette.accentGold)
                                        Column {
                                            Text(
                                                text = "Loopholes & Tactical Nuances",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Advanced rules, safe-zone timing, and positional traps.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            items(rulebook.loopholesAndExploits) { pointer ->
                                PointerItem(pointer = pointer, accentColor = palette.accentGold)
                            }
                        }

                        5 -> {
                            item {
                                RuleCard(
                                    title = "Mastery & High-Level Tactics",
                                    icon = Icons.Default.Psychology,
                                    accentColor = accentColor,
                                    content = "Proven strategic principles utilized by grandmasters and historians."
                                )
                            }
                            items(rulebook.proStrategies) { pointer ->
                                PointerItem(pointer = pointer, accentColor = accentColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Got It • Return to Game", fontWeight = FontWeight.Bold)
                }
            }
        }
        }
    }
}

@Composable
private fun RuleCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: String
) {
    val palette = LocalIndoPalette.current
    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PointerItem(
    pointer: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = pointer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        )
    }
}
