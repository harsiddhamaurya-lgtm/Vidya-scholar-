package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnsweredQuestionRecord
import com.example.data.QuestLevel
import com.example.data.QuestNodeState
import com.example.data.SubjectRealm
import com.example.ui.components.QuestMilestoneNode
import com.example.ui.components.RealmCardGeometryWatermark
import com.example.ui.components.RealmMedallionWithProgress
import com.example.ui.components.VidyaLecturePlayer
import com.example.ui.theme.IndoAtmosphericBackdrop
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.MandalaUnfoldContainer
import com.example.utils.SoundHelper
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.VidyaVaultViewModel

@Composable
fun WindingQuestMapView(
    realm: SubjectRealm,
    vidyaViewModel: VidyaVaultViewModel,
    onBack: () -> Unit,
    onLaunchLevel: (QuestLevel) -> Unit,
    onOpenNotesArchive: () -> Unit,
    onOpenQuestionsLog: () -> Unit
) {
    BackHandler { onBack() }
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val completedIds by vidyaViewModel.completedLevelIds.collectAsStateWithLifecycle()
    val (currentLevelNum, masteryFrac) = vidyaViewModel.getRealmProgress(realm, completedIds)
    var showMenu by remember { mutableStateOf(false) }
    var lockedNotice by remember { mutableStateOf<String?>(null) }

    IndoAtmosphericBackdrop(ambientGlowColor = realm.accentPrimary) {
        MandalaUnfoldContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Realm Badge + Corner Triple-Dot Menu
                item {
                    IndoToneSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        containerColor = if (palette.isDark) realm.darkSurfaceTop else if (palette.isEInk) palette.cardSurface else realm.lightSurfaceTop,
                        hairlineColor = palette.accentGold.copy(alpha = 0.45f)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            RealmCardGeometryWatermark(
                                visualKind = realm.visualKind,
                                tint = realm.accentPrimary,
                                isEInk = palette.isEInk,
                                modifier = Modifier.matchParentSize()
                            )
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            SoundHelper.playClick(context)
                                            onBack()
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .minimumInteractiveComponentSize()
                                            .clip(CircleShape)
                                            .background(palette.cardSurface.copy(alpha = 0.72f))
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to Vault",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    ) {
                                        RealmMedallionWithProgress(
                                            visualKind = realm.visualKind,
                                            accentPrimary = realm.accentPrimary,
                                            accentSecondary = realm.accentSecondary,
                                            progressFraction = masteryFrac,
                                            size = 46.dp
                                        )
                                        Column {
                                            Text(
                                                text = realm.realmId.subjectName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Level $currentLevelNum • ${(masteryFrac * 100).toInt()}% Mastered",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (palette.isEInk) palette.accentSaffron else realm.accentPrimary
                                            )
                                        }
                                    }

                                    Box {
                                        IconButton(
                                            onClick = { showMenu = true },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .minimumInteractiveComponentSize()
                                                .clip(CircleShape)
                                                .background(palette.cardSurface.copy(alpha = 0.78f))
                                                .border(1.dp, palette.goldHairline, CircleShape)
                                                .testTag("quest_header_more_menu")
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = "Realm Options",
                                                tint = MaterialTheme.colorScheme.onSurface
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
                                                    onOpenNotesArchive()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Answered Questions Log") },
                                                leadingIcon = { Icon(Icons.Default.HistoryEdu, contentDescription = null) },
                                                onClick = {
                                                    showMenu = false
                                                    onOpenQuestionsLog()
                                                }
                                            )
                                        }
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { masteryFrac.coerceIn(0.03f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (palette.isEInk) palette.accentSaffron else realm.accentPrimary,
                                    trackColor = palette.cardSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }

                if (lockedNotice != null) {
                    item {
                        IndoToneSurface(
                            shape = RoundedCornerShape(12.dp),
                            containerColor = palette.elevatedSurface,
                            hairlineColor = Color(0xFFE88D14)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lockedNotice ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { lockedNotice = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Candy Crush-style Vertical Winding Quest Path
                item {
                    val levels = realm.levels
                    val rowHeight = 144.dp
                    val totalHeight = rowHeight * levels.size

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalHeight)
                            .padding(vertical = 8.dp)
                    ) {
                        val areaWidth = maxWidth
                        // S-curve horizontal fractions for nodes 1..5
                        val xFractions = remember(levels.size) {
                            listOf(0.26f, 0.72f, 0.30f, 0.72f, 0.50f)
                        }

                        // Glowing Golden-Cyan SVG Winding Trail
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val rowPx = size.height / levels.size
                            val points = levels.indices.map { i ->
                                val fx = xFractions[i % xFractions.size]
                                Offset(size.width * fx, rowPx * i + rowPx * 0.42f)
                            }
                            val trailPath = Path()
                            points.forEachIndexed { idx, pt ->
                                if (idx == 0) {
                                    trailPath.moveTo(pt.x, pt.y)
                                } else {
                                    val prev = points[idx - 1]
                                    val midY = (prev.y + pt.y) / 2f
                                    trailPath.cubicTo(prev.x, midY, pt.x, midY, pt.x, pt.y)
                                }
                            }

                            if (!palette.isEInk) {
                                drawPath(
                                    path = trailPath,
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFD4AF37).copy(alpha = 0.35f), Color(0xFF00D2E6).copy(alpha = 0.35f))
                                    ),
                                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            drawPath(
                                path = trailPath,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        if (palette.isEInk) palette.accentSaffron else Color(0xFFD4AF37),
                                        if (palette.isEInk) palette.accentGold else Color(0xFF00D2E6),
                                        if (palette.isEInk) palette.accentSaffron else Color(0xFFE88D14)
                                    )
                                ),
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Circular Embossed Milestone Nodes positioned cleanly on the trail
                        levels.forEachIndexed { idx, level ->
                            val state = vidyaViewModel.getNodeState(realm, level, completedIds)
                            val fx = xFractions[idx % xFractions.size]
                            val nodeWidth = 176.dp
                            val centerX = areaWidth * fx
                            val xOffset = (centerX - nodeWidth / 2).coerceIn(0.dp, (areaWidth - nodeWidth).coerceAtLeast(0.dp))
                            val yOffset = rowHeight * idx + 12.dp

                            QuestMilestoneNode(
                                level = level,
                                state = state,
                                accentPrimary = realm.accentPrimary,
                                onClick = {
                                    if (state == QuestNodeState.LOCKED) {
                                        SoundHelper.playWoodyTock(context)
                                        lockedNotice = "Complete Level ${level.levelNumber - 1} first to unseal ${level.title}."
                                    } else {
                                        SoundHelper.playTempleChime(context)
                                        onLaunchLevel(level)
                                    }
                                },
                                modifier = Modifier
                                    .width(nodeWidth)
                                    .offset(x = xOffset, y = yOffset)
                                    .testTag("quest_node_${level.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelQuestRunnerScreen(
    level: QuestLevel,
    realm: SubjectRealm,
    vidyaViewModel: VidyaVaultViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val completedIds by vidyaViewModel.completedLevelIds.collectAsStateWithLifecycle()
    val savedNoteIds by vidyaViewModel.savedNoteLevelIds.collectAsStateWithLifecycle()
    val alreadyCompleted = completedIds.contains(level.id)
    val isSaved = savedNoteIds.contains(level.id)

    // 0 = Video Lecture, 1 = Quick Test (4 Qs), 2 = Scroll of Knowledge
    var currentStage by remember(level.id) { mutableIntStateOf(0) }
    var videoCompleted by remember(level.id) { mutableStateOf(alreadyCompleted) }
    var testCompleted by remember(level.id) { mutableStateOf(alreadyCompleted) }

    // Quick test state
    var quizIndex by remember(level.id) { mutableIntStateOf(0) }
    var selectedOpt by remember(level.id) { mutableStateOf<Int?>(null) }
    var submittedOpt by remember(level.id) { mutableStateOf(false) }
    var correctCount by remember(level.id) { mutableIntStateOf(0) }

    IndoAtmosphericBackdrop(ambientGlowColor = realm.accentPrimary) {
        MandalaUnfoldContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Level ${level.levelNumber} • ${level.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = level.chapterSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Sequential 3-Stage Stepper Pills
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StagePill("1. Video", currentStage == 0, videoCompleted, Modifier.weight(1f)) {
                            currentStage = 0
                        }
                        StagePill("2. Quick Test", currentStage == 1, testCompleted, Modifier.weight(1f)) {
                            if (videoCompleted || alreadyCompleted) currentStage = 1
                        }
                        StagePill("3. Notes", currentStage == 2, alreadyCompleted, Modifier.weight(1f)) {
                            if (testCompleted || alreadyCompleted) currentStage = 2
                        }
                    }
                }

                when (currentStage) {
                    0 -> {
                        item {
                            VidyaLecturePlayer(
                                level = level,
                                accentColor = realm.accentPrimary,
                                onCheckpointAnswered = { q, chosen ->
                                    vidyaViewModel.logAnsweredQuestion(level, q, chosen, "Video Checkpoint")
                                },
                                onLectureCompleted = {
                                    videoCompleted = true
                                    currentStage = 1
                                }
                            )
                        }
                    }
                    1 -> {
                        val questions = level.quickTest
                        val q = questions[quizIndex]
                        item {
                            IndoToneSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = palette.cardSurface,
                                hairlineColor = palette.goldHairline
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "QUICK TEST • Q${quizIndex + 1}/${questions.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE88D14)
                                        )
                                        Text(
                                            text = "Score: $correctCount/${questions.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    Text(
                                        text = q.prompt,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    q.options.forEachIndexed { idx, opt ->
                                        val isSelected = selectedOpt == idx
                                        val isRight = idx == q.correctIndex
                                        val bg = when {
                                            submittedOpt && isRight -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            submittedOpt && isSelected && !isRight -> palette.accentVermilion.copy(alpha = 0.2f)
                                            isSelected -> palette.secondaryIndigo
                                            else -> palette.elevatedSurface
                                        }
                                        IndoToneSurface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable(!submittedOpt) { selectedOpt = idx },
                                            shape = RoundedCornerShape(14.dp),
                                            containerColor = bg,
                                            hairlineColor = if (isSelected) Color(0xFFE88D14) else palette.goldHairline
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { if (!submittedOpt) selectedOpt = idx },
                                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE88D14))
                                                )
                                                Text(opt, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }

                                    if (submittedOpt) {
                                        Text(
                                            text = q.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (!submittedOpt) {
                                                val chosen = selectedOpt ?: return@Button
                                                submittedOpt = true
                                                if (chosen == q.correctIndex) {
                                                    correctCount++
                                                    SoundHelper.playTempleChime(context)
                                                } else {
                                                    SoundHelper.playWoodyTock(context)
                                                }
                                                vidyaViewModel.logAnsweredQuestion(level, q, chosen, "Quick Test")
                                            } else {
                                                submittedOpt = false
                                                selectedOpt = null
                                                if (quizIndex < questions.lastIndex) {
                                                    quizIndex++
                                                } else {
                                                    testCompleted = true
                                                    currentStage = 2
                                                }
                                            }
                                        },
                                        enabled = selectedOpt != null,
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (submittedOpt) Color(0xFF10B981) else Color(0xFFE88D14),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = when {
                                                !submittedOpt -> "Submit Answer"
                                                quizIndex < questions.lastIndex -> "Next Question →"
                                                else -> "Unlock Scroll of Knowledge →"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Stage 3: Scroll of Knowledge Study Notes
                        item {
                            IndoToneSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = palette.cardSurface,
                                hairlineColor = palette.accentGold.copy(alpha = 0.6f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SCROLL OF KNOWLEDGE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE88D14)
                                        )
                                        IconButton(onClick = { vidyaViewModel.toggleNoteSaved(level.id) }) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Save Note",
                                                tint = Color(0xFFE88D14)
                                            )
                                        }
                                    }
                                    Text(
                                        text = level.scrollTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    HorizontalDivider(color = palette.goldHairline)
                                    Text(
                                        text = level.scrollSummary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Key Formulas & Takeaways",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = realm.accentPrimary
                                    )
                                    level.keyFormulas.forEach { formula ->
                                        IndoToneSurface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            containerColor = palette.elevatedSurface,
                                            hairlineColor = palette.goldHairline
                                        ) {
                                            Text(
                                                text = "• $formula",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            SoundHelper.playArenaVictorySwell(context)
                                            vidyaViewModel.markLevelCompleted(level)
                                            if (!alreadyCompleted) {
                                                authViewModel.updateProgress(50, level.id)
                                            }
                                            onBack()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (alreadyCompleted) "Return to Quest Path" else "Complete Level (+50 XP)",
                                            fontWeight = FontWeight.Bold
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
}

@Composable
private fun StagePill(
    label: String,
    active: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    IndoToneSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        containerColor = when {
            active -> Color(0xFFE88D14)
            done -> Color(0xFF10B981).copy(alpha = 0.22f)
            else -> palette.elevatedSurface
        },
        hairlineColor = if (active) Color(0xFFE88D14) else palette.goldHairline
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BossBattleArenaScreen(
    level: QuestLevel,
    realm: SubjectRealm,
    vidyaViewModel: VidyaVaultViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val questions = level.bossQuestions

    var qIdx by remember(level.id) { mutableIntStateOf(0) }
    var selected by remember(level.id) { mutableStateOf<Int?>(null) }
    var submitted by remember(level.id) { mutableStateOf(false) }
    var marksEarned by remember(level.id) { mutableIntStateOf(0) }
    var finished by remember(level.id) { mutableStateOf(false) }

    val bossHpFraction = ((questions.size - (marksEarned / 5f)) / questions.size.coerceAtLeast(1)).coerceIn(0f, 1f)

    IndoAtmosphericBackdrop(ambientGlowColor = palette.accentVermilion) {
        MandalaUnfoldContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CROWNED BOSS ARENA • 50 MARKS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accentVermilion
                            )
                            Text(
                                text = level.bossTitle.ifBlank { level.title },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accentGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$marksEarned / 50",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accentSaffron
                            )
                        }
                    }
                }

                // Boss HP & Mastery Meter
                item {
                    IndoToneSurface(
                        shape = RoundedCornerShape(18.dp),
                        containerColor = palette.cardSurface,
                        hairlineColor = palette.accentGold.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Guardian Shield Remaining", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${(bossHpFraction * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accentVermilion)
                            }
                            LinearProgressIndicator(
                                progress = { bossHpFraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = palette.accentVermilion,
                                trackColor = palette.elevatedSurface
                            )
                        }
                    }
                }

                if (!finished && questions.isNotEmpty()) {
                    val q = questions[qIdx]
                    item {
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.goldHairline
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "TRIAL ${qIdx + 1} OF ${questions.size} • +5 MARKS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = realm.accentPrimary
                                )
                                Text(
                                    text = q.prompt,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                q.options.forEachIndexed { idx, opt ->
                                    val isSel = selected == idx
                                    val isRight = idx == q.correctIndex
                                    val bg = when {
                                        submitted && isRight -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        submitted && isSel && !isRight -> palette.accentVermilion.copy(alpha = 0.2f)
                                        isSel -> palette.secondaryIndigo
                                        else -> palette.elevatedSurface
                                    }
                                    IndoToneSurface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(!submitted) { selected = idx },
                                        shape = RoundedCornerShape(14.dp),
                                        containerColor = bg,
                                        hairlineColor = if (isSel) palette.accentGold else palette.goldHairline
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSel,
                                                onClick = { if (!submitted) selected = idx },
                                                colors = RadioButtonDefaults.colors(selectedColor = palette.accentSaffron)
                                            )
                                            Text(opt, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                if (submitted) {
                                    Text(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!submitted) {
                                            val chosen = selected ?: return@Button
                                            submitted = true
                                            if (chosen == q.correctIndex) {
                                                marksEarned += 5
                                                SoundHelper.playTempleChime(context)
                                            } else {
                                                SoundHelper.playWoodyTock(context)
                                            }
                                            vidyaViewModel.logAnsweredQuestion(level, q, chosen, "Boss Battle (50M)")
                                        } else {
                                            submitted = false
                                            selected = null
                                            if (qIdx < questions.lastIndex) {
                                                qIdx++
                                            } else {
                                                finished = true
                                                if (marksEarned >= 30) {
                                                    SoundHelper.playArenaVictorySwell(context)
                                                    vidyaViewModel.markLevelCompleted(level)
                                                    authViewModel.updateProgress(250, level.id)
                                                }
                                            }
                                        }
                                    },
                                    enabled = selected != null,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (submitted) Color(0xFF10B981) else palette.accentVermilion,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            !submitted -> "Strike Answer (5 Marks)"
                                            qIdx < questions.lastIndex -> "Next Trial →"
                                            else -> "Reveal Boss Verdict"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val passed = marksEarned >= 30
                    item {
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.accentGold
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (passed) palette.accentGold else palette.accentVermilion,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = if (passed) "Boss Realm Conquered!" else "Trial Complete — Keep Training",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Final Score: $marksEarned / 50 Marks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (passed) Color(0xFF10B981) else palette.accentSaffron
                                )
                                Text(
                                    text = if (passed) level.scrollSummary else "Score at least 30/50 marks to claim the ${realm.rankTitle} Crown Scroll.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE88D14),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Return to Winding Quest Map", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedNotesArchiveView(
    realm: SubjectRealm,
    vidyaViewModel: VidyaVaultViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val palette = LocalIndoPalette.current
    val savedIds by vidyaViewModel.savedNoteLevelIds.collectAsStateWithLifecycle()
    val completedIds by vidyaViewModel.completedLevelIds.collectAsStateWithLifecycle()
    val scrolls = remember(realm, savedIds, completedIds) {
        realm.levels.filter { savedIds.contains(it.id) || completedIds.contains(it.id) }
    }

    IndoAtmosphericBackdrop(ambientGlowColor = realm.accentPrimary) {
        MandalaUnfoldContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = "${realm.realmId.subjectName} • Saved Notes Archive",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${scrolls.size} Scrolls of Knowledge unlocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (scrolls.isEmpty()) {
                    item {
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.goldHairline
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = realm.accentPrimary, modifier = Modifier.size(40.dp))
                                Text("No Scrolls Saved Yet", fontWeight = FontWeight.Bold)
                                Text(
                                    "Complete a chapter quest in ${realm.realmId.subjectName} to unlock and archive its Scroll of Knowledge.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(scrolls, key = { it.id }) { lvl ->
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.accentGold.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LEVEL ${lvl.levelNumber} • ${lvl.scrollTitle}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = realm.accentPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { vidyaViewModel.toggleNoteSaved(lvl.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            imageVector = if (savedIds.contains(lvl.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Toggle Bookmark",
                                            tint = Color(0xFFE88D14)
                                        )
                                    }
                                }
                                Text(lvl.scrollSummary, style = MaterialTheme.typography.bodyMedium)
                                lvl.keyFormulas.forEach { formula ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.elevatedSurface
                                    ) {
                                        Text(
                                            text = "• $formula",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
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
}

@Composable
fun AnsweredQuestionsLogView(
    realm: SubjectRealm,
    vidyaViewModel: VidyaVaultViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val palette = LocalIndoPalette.current
    val allLogs by vidyaViewModel.answeredLogs.collectAsStateWithLifecycle()
    val realmLogs = remember(allLogs, realm.realmId) {
        allLogs.filter { it.realmId == realm.realmId }
    }
    val correctTotal = realmLogs.count { it.isCorrect }

    IndoAtmosphericBackdrop(ambientGlowColor = realm.accentPrimary) {
        MandalaUnfoldContainer(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${realm.realmId.subjectName} • Answered Questions Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${realmLogs.size} logged • $correctTotal correct",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (realmLogs.isEmpty()) {
                    item {
                        IndoToneSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = palette.cardSurface,
                            hairlineColor = palette.goldHairline
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = realm.accentPrimary, modifier = Modifier.size(40.dp))
                                Text("No Questions Answered Yet", fontWeight = FontWeight.Bold)
                                Text(
                                    "Watch a lecture checkpoint or attempt a Quick Test in ${realm.realmId.subjectName} to record your mastery log.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(realmLogs, key = { it.id }) { rec ->
                        AnsweredRecordCard(rec)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnsweredRecordCard(record: AnsweredQuestionRecord) {
    val palette = LocalIndoPalette.current
    val statusColor = if (record.isCorrect) Color(0xFF10B981) else palette.accentVermilion
    IndoToneSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        containerColor = palette.cardSurface,
        hairlineColor = palette.goldHairline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LVL ${record.levelNumber} • ${record.questionSource}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE88D14)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (record.isCorrect) "Correct" else "Review",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            Text(record.questionPrompt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = "Your Answer: ${record.selectedAnswer}",
                fontSize = 12.sp,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
            if (!record.isCorrect) {
                Text(
                    text = "Correct Answer: ${record.correctAnswer}",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = record.explanation,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
