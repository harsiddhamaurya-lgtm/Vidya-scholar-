package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AcademicStream
import com.example.data.BatchType
import com.example.data.QuestLevel
import com.example.data.RealmId
import com.example.data.StandardGrade
import com.example.data.SubjectRealm
import com.example.data.VidyaRepository
import com.example.ui.components.RealmCardGeometryWatermark
import com.example.ui.components.RealmMedallionWithProgress
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.IndoMotion
import com.example.utils.SoundHelper
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.VidyaVaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class VidyaDestination {
    data object VaultGrid : VidyaDestination()
    data class QuestMap(val realmId: RealmId) : VidyaDestination()
    data class PlayLevel(val realmId: RealmId, val level: QuestLevel) : VidyaDestination()
    data class SavedNotes(val realmId: RealmId) : VidyaDestination()
    data class QuestionsLog(val realmId: RealmId) : VidyaDestination()
}

@Composable
fun CoursesScreen(
    onNavigateToCourse: (String) -> Unit = {},
    vidyaViewModel: VidyaVaultViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val profile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val standardId = profile?.standardId ?: "class_11"
    val streamId = profile?.streamId ?: "science_pcm"
    val batchType = profile?.batchType ?: "live"

    var destination by remember { mutableStateOf<VidyaDestination>(VidyaDestination.VaultGrid) }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (fadeIn(tween(260, easing = IndoMotion.OrganicDeceleration)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(260, easing = IndoMotion.OrganicDeceleration)))
                .togetherWith(
                    fadeOut(tween(180, easing = IndoMotion.OrganicDeceleration)) +
                        scaleOut(targetScale = 1.03f, animationSpec = tween(180, easing = IndoMotion.OrganicDeceleration))
                )
        },
        label = "vidya_realm_transition"
    ) { dest ->
        when (dest) {
            VidyaDestination.VaultGrid -> {
                VidyaRealmPortalVault(
                    vidyaViewModel = vidyaViewModel,
                    standardId = standardId,
                    streamId = streamId,
                    batchType = batchType,
                    onEnterRealm = { realm -> destination = VidyaDestination.QuestMap(realm.realmId) },
                    onOpenNotes = { realm -> destination = VidyaDestination.SavedNotes(realm.realmId) },
                    onOpenLog = { realm -> destination = VidyaDestination.QuestionsLog(realm.realmId) }
                )
            }
            is VidyaDestination.QuestMap -> {
                val realm = vidyaViewModel.getRealm(dest.realmId, standardId, streamId, batchType)
                WindingQuestMapView(
                    realm = realm,
                    vidyaViewModel = vidyaViewModel,
                    onBack = { destination = VidyaDestination.VaultGrid },
                    onLaunchLevel = { level -> destination = VidyaDestination.PlayLevel(dest.realmId, level) },
                    onOpenNotesArchive = { destination = VidyaDestination.SavedNotes(dest.realmId) },
                    onOpenQuestionsLog = { destination = VidyaDestination.QuestionsLog(dest.realmId) }
                )
            }
            is VidyaDestination.PlayLevel -> {
                val realm = vidyaViewModel.getRealm(dest.realmId, standardId, streamId, batchType)
                if (dest.level.isBossBattle) {
                    BossBattleArenaScreen(
                        level = dest.level,
                        realm = realm,
                        vidyaViewModel = vidyaViewModel,
                        authViewModel = authViewModel,
                        onBack = { destination = VidyaDestination.QuestMap(dest.realmId) }
                    )
                } else {
                    LevelQuestRunnerScreen(
                        level = dest.level,
                        realm = realm,
                        vidyaViewModel = vidyaViewModel,
                        authViewModel = authViewModel,
                        onBack = { destination = VidyaDestination.QuestMap(dest.realmId) }
                    )
                }
            }
            is VidyaDestination.SavedNotes -> {
                val realm = vidyaViewModel.getRealm(dest.realmId, standardId, streamId, batchType)
                SavedNotesArchiveView(
                    realm = realm,
                    vidyaViewModel = vidyaViewModel,
                    onBack = { destination = VidyaDestination.QuestMap(dest.realmId) }
                )
            }
            is VidyaDestination.QuestionsLog -> {
                val realm = vidyaViewModel.getRealm(dest.realmId, standardId, streamId, batchType)
                AnsweredQuestionsLogView(
                    realm = realm,
                    vidyaViewModel = vidyaViewModel,
                    onBack = { destination = VidyaDestination.QuestMap(dest.realmId) }
                )
            }
        }
    }
}

@Composable
private fun VidyaRealmPortalVault(
    vidyaViewModel: VidyaVaultViewModel,
    standardId: String,
    streamId: String,
    batchType: String,
    onEnterRealm: (SubjectRealm) -> Unit,
    onOpenNotes: (SubjectRealm) -> Unit,
    onOpenLog: (SubjectRealm) -> Unit
) {
    val palette = LocalIndoPalette.current
    val completedIds by vidyaViewModel.completedLevelIds.collectAsStateWithLifecycle()
    val standardGrade = StandardGrade.fromId(standardId)
    val academicStream = AcademicStream.fromId(streamId)
    val batch = BatchType.fromId(batchType)

    val realms = remember(standardId, streamId, batchType) {
        vidyaViewModel.getRealmsForStudent(standardId, streamId, batchType)
    }

    val totalQuests = realms.sumOf { it.levels.size }
    val completedInRealms = realms.flatMap { it.levels }.count { it.id in completedIds }

    val leftRealms = realms.filterIndexed { index, _ -> index % 2 == 0 }
    val rightRealms = realms.filterIndexed { index, _ -> index % 2 != 0 }

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentSaffron) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vidya • Subject Vault",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Knowledge realms calibrated to your enrolled stream & batch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IndoToneSurface(
                        shape = RoundedCornerShape(14.dp),
                        containerColor = palette.elevatedSurface,
                        hairlineColor = palette.goldHairline
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = palette.accentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$completedInRealms/$totalQuests Quests",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Academic Batch Pill
            item {
                IndoToneSurface(
                    shape = RoundedCornerShape(12.dp),
                    containerColor = palette.elevatedSurface.copy(alpha = 0.85f),
                    hairlineColor = palette.goldHairline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = palette.accentGold.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, palette.accentGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = batch.badgeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${standardGrade.shortName} • ${if (!standardGrade.hasStreams) "Foundation" else academicStream.tagLabel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${realms.size} Dynamic Subjects",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dynamic 2-Column Staggered Bento Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        leftRealms.forEachIndexed { i, realm ->
                            val (lvl, frac) = vidyaViewModel.getRealmProgress(realm, completedIds)
                            val cardMinHeight = if (i % 2 == 0) 244.dp else 220.dp
                            RealmPortalCard(
                                realm = realm,
                                currentLevel = lvl,
                                masteryFraction = frac,
                                staggerIndex = i * 2,
                                minHeight = cardMinHeight,
                                onEnterRealm = { onEnterRealm(realm) },
                                onOpenNotes = { onOpenNotes(realm) },
                                onOpenLog = { onOpenLog(realm) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rightRealms.forEachIndexed { i, realm ->
                            val (lvl, frac) = vidyaViewModel.getRealmProgress(realm, completedIds)
                            val cardMinHeight = if (i % 2 == 0) 220.dp else 244.dp
                            RealmPortalCard(
                                realm = realm,
                                currentLevel = lvl,
                                masteryFraction = frac,
                                staggerIndex = i * 2 + 1,
                                minHeight = cardMinHeight,
                                onEnterRealm = { onEnterRealm(realm) },
                                onOpenNotes = { onOpenNotes(realm) },
                                onOpenLog = { onOpenLog(realm) }
                            )
                        }

                        // Compact Boss Trials Summary Bento Tile balancing the right column bottom
                        BossTrialsSummaryTile(
                            completedBosses = realms.count { realm -> completedIds.contains(realm.levels.lastOrNull()?.id) },
                            totalRealms = realms.size,
                            staggerIndex = realms.size + 1,
                            onEnterCodingBoss = {
                                if (realms.isNotEmpty()) onEnterRealm(realms.last())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealmPortalCard(
    realm: SubjectRealm,
    currentLevel: Int,
    masteryFraction: Float,
    staggerIndex: Int,
    minHeight: Dp,
    onEnterRealm: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenLog: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val isHovered by interaction.collectIsHoveredAsState()

    var showMenu by remember { mutableStateOf(false) }
    var isFlashing by remember { mutableStateOf(false) }

    // Staggered float-up entrance (50ms delay between cards)
    val entryAlpha = remember(realm.realmId) { Animatable(0f) }
    val entryTranslateY = remember(realm.realmId) { Animatable(28f) }
    LaunchedEffect(realm.realmId) {
        delay(staggerIndex * 50L)
        entryAlpha.animateTo(1f, tween(260, easing = IndoMotion.OrganicDeceleration))
    }
    LaunchedEffect(realm.realmId) {
        delay(staggerIndex * 50L)
        entryTranslateY.animateTo(0f, tween(280, easing = IndoMotion.OrganicDeceleration))
    }

    // Crisp spring press (scale 0.96f) + warm light-sweep hover/press sheen
    val pressScale by animateFloatAsState(
        targetValue = when {
            isPressed || isFlashing -> 0.96f
            isHovered -> 1.015f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.72f),
        label = "portal_press_scale"
    )

    val sheenProgress by animateFloatAsState(
        targetValue = if (isHovered || isPressed || isFlashing) 1f else 0f,
        animationSpec = tween(260, easing = IndoMotion.OrganicDeceleration),
        label = "portal_sheen"
    )

    val surfaceTop = when {
        palette.isEInk -> palette.cardSurface
        palette.isDark -> realm.darkSurfaceTop
        else -> realm.lightSurfaceTop
    }
    val surfaceBottom = when {
        palette.isEInk -> palette.cardSurface
        palette.isDark -> realm.darkSurfaceBottom
        else -> realm.lightSurfaceBottom
    }
    val accentInk = if (palette.isEInk) palette.accentSaffron else realm.accentPrimary
    val goldHairline = if (isFlashing) palette.accentGold else palette.goldHairline
    val masteryPct = (masteryFraction * 100).toInt()

    IndoToneSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .graphicsLayer {
                alpha = entryAlpha.value
                translationY = entryTranslateY.value
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current
            ) {
                if (!isFlashing) {
                    isFlashing = true
                    SoundHelper.playWoodyTock(context)
                    scope.launch {
                        delay(110L)
                        isFlashing = false
                        onEnterRealm()
                    }
                }
            }
            .testTag("realm_portal_${realm.realmId.name.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.Transparent,
        hairlineColor = goldHairline,
        borderWidth = if (isFlashing) 1.5.dp else 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(Brush.verticalGradient(listOf(surfaceTop, surfaceBottom)))
                .drawWithContent {
                    drawContent()
                    if (sheenProgress > 0.01f && !palette.isEInk) {
                        val sweepX = size.width * (-0.3f + 1.6f * sheenProgress)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFD4AF37).copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepX - size.width * 0.35f, 0f),
                                end = Offset(sweepX + size.width * 0.35f, size.height)
                            )
                        )
                    }
                }
        ) {
            // 5% Sacred Geometry Watermark
            RealmCardGeometryWatermark(
                visualKind = realm.visualKind,
                tint = accentInk,
                isEInk = palette.isEInk,
                modifier = Modifier.matchParentSize()
            )

            // Corner Glassmorphic Triple-Dot Menu (⋮)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        SoundHelper.playClick(context)
                        showMenu = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            if (palette.isEInk) palette.elevatedSurface
                            else if (palette.isDark) Color.White.copy(alpha = 0.12f)
                            else Color.White.copy(alpha = 0.56f)
                        )
                        .border(1.dp, palette.goldHairline, CircleShape)
                        .testTag("portal_menu_${realm.realmId.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "${realm.realmId.subjectName} Archive Options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Saved Notes Archive") },
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenNotes()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Answered Questions Log") },
                        leadingIcon = { Icon(Icons.Default.HistoryEdu, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenLog()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Medallion + Radial XP Ring
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RealmMedallionWithProgress(
                        visualKind = realm.visualKind,
                        accentPrimary = realm.accentPrimary,
                        accentSecondary = realm.accentSecondary,
                        progressFraction = masteryFraction,
                        size = 62.dp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = realm.realmId.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = realm.realmId.subtitle,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom Progress Rank Bar + "Enter Realm →" Pill Trigger
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Level $currentLevel • $masteryPct% Mastered",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentInk
                        )
                        LinearProgressIndicator(
                            progress = { masteryFraction.coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentInk,
                            trackColor = if (palette.isEInk) palette.elevatedSurface else Color.Black.copy(alpha = 0.22f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHovered || isPressed || isFlashing) Color(0xFFE88D14) else accentInk.copy(alpha = if (palette.isEInk) 0.14f else 0.22f),
                        border = BorderStroke(0.8.dp, if (palette.isEInk) palette.goldHairline else accentInk.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Realm →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isHovered || isPressed || isFlashing) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BossTrialsSummaryTile(
    completedBosses: Int,
    totalRealms: Int,
    staggerIndex: Int,
    onEnterCodingBoss: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val entryAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(staggerIndex * 50L)
        entryAlpha.animateTo(1f, tween(260, easing = IndoMotion.OrganicDeceleration))
    }

    IndoToneSurface(
        onClick = onEnterCodingBoss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 194.dp)
            .graphicsLayer { alpha = entryAlpha.value },
        shape = RoundedCornerShape(22.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.accentVermilion.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "BOSS ARENAS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accentVermilion
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "50-Mark Trial Ready",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Coding & AI Level 5 Boss is unsealed ($completedBosses/$totalRealms crowns earned).",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFE88D14)
            ) {
                Text(
                    text = "Challenge Arena →",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
