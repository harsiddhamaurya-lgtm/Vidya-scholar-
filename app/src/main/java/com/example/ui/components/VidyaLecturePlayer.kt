package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MidVideoCheckpoint
import com.example.data.QuestLevel
import com.example.data.RealmId
import com.example.data.VidyaQuizQuestion
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VidyaLecturePlayer(
    level: QuestLevel,
    accentColor: Color,
    initialPositionSec: Int = 0,
    onProgressUpdate: (positionSec: Int, totalDurationSec: Int) -> Unit = { _, _ -> },
    onCheckpointAnswered: (VidyaQuizQuestion, Int) -> Unit,
    onLectureCompleted: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current

    val startingProgress = remember(level.id, initialPositionSec) {
        if (level.videoDurationSeconds > 0 && initialPositionSec > 0) {
            (initialPositionSec.toFloat() / level.videoDurationSeconds).coerceIn(0f, 0.95f)
        } else {
            0f
        }
    }

    var isPlaying by remember(level.id) { mutableStateOf(true) }
    var progress by remember(level.id) { mutableFloatStateOf(startingProgress) }
    var playbackSpeed by remember(level.id) { mutableFloatStateOf(1f) }
    var clearedCheckpointIds by remember(level.id) {
        mutableStateOf(
            if (startingProgress > 0f) {
                level.checkpoints.filter { it.triggerProgress <= startingProgress }.map { it.id }.toSet()
            } else {
                emptySet()
            }
        )
    }
    var activeCheckpoint by remember(level.id) { mutableStateOf<MidVideoCheckpoint?>(null) }
    var selectedCheckpointOption by remember(level.id) { mutableStateOf<Int?>(null) }
    var checkpointSubmitted by remember(level.id) { mutableStateOf(false) }
    var videoFinished by remember(level.id) { mutableStateOf(startingProgress >= 0.99f) }

    // Deterministic lecture loop with auto-pausing checkpoint detection
    LaunchedEffect(level.id, isPlaying, playbackSpeed, activeCheckpoint, videoFinished) {
        var tickCount = 0
        while (isPlaying && activeCheckpoint == null && !videoFinished) {
            delay(80L)
            tickCount++
            val step = (0.08f * playbackSpeed) / level.videoDurationSeconds.coerceAtLeast(10).toFloat()
            val next = (progress + step).coerceAtMost(1f)

            val triggered = level.checkpoints.firstOrNull { cp ->
                !clearedCheckpointIds.contains(cp.id) && next >= cp.triggerProgress
            }
            if (triggered != null) {
                progress = triggered.triggerProgress
                isPlaying = false
                activeCheckpoint = triggered
                selectedCheckpointOption = null
                checkpointSubmitted = false
                SoundHelper.playWoodyTock(context)
                onProgressUpdate((progress * level.videoDurationSeconds).toInt(), level.videoDurationSeconds)
            } else {
                progress = next
                if (tickCount % 12 == 0) {
                    onProgressUpdate((progress * level.videoDurationSeconds).toInt(), level.videoDurationSeconds)
                }
                if (next >= 1f) {
                    isPlaying = false
                    videoFinished = true
                    SoundHelper.playTempleChime(context)
                    onProgressUpdate(level.videoDurationSeconds, level.videoDurationSeconds)
                    onLectureCompleted()
                }
            }
        }
    }

    val captions = level.lectureCaptions.ifEmpty { listOf("Exploring ${level.title}") }
    val captionIndex = ((progress * captions.size).toInt()).coerceIn(0, captions.lastIndex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 16:9 Interactive Animated Concept Video Canvas
        IndoToneSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            containerColor = if (palette.isEInk) palette.cardSurface else Color(0xFF0E131D),
            hairlineColor = palette.goldHairline
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(
                            if (palette.isEInk) palette.elevatedSurface
                            else Color(0xFF0C1018)
                        )
                ) {
                    // Animated Subject-Specific Concept Chalkboard Visual
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val c = Offset(w * 0.5f, h * 0.48f)
                        val lineColor = if (palette.isEInk) Color(0xFF171717) else accentColor
                        val goldColor = if (palette.isEInk) Color(0xFF44403C) else Color(0xFFD4AF37)

                        // Subtle grid lines
                        for (gx in 1..5) {
                            drawLine(
                                lineColor.copy(alpha = 0.08f),
                                Offset(w * gx / 6f, 0f),
                                Offset(w * gx / 6f, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        for (gy in 1..3) {
                            drawLine(
                                lineColor.copy(alpha = 0.08f),
                                Offset(0f, h * gy / 4f),
                                Offset(w, h * gy / 4f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        when (level.realmId) {
                            RealmId.MATHEMATICS -> {
                                // Animated curve + moving tangent line
                                val path = Path()
                                val samples = 48
                                for (i in 0..samples) {
                                    val t = i.toFloat() / samples
                                    val x = w * 0.14f + t * w * 0.72f
                                    val y = h * 0.68f - sin(t * PI).toFloat() * h * 0.42f
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, lineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                                val pt = progress.coerceIn(0.1f, 0.9f)
                                val px = w * 0.14f + pt * w * 0.72f
                                val py = h * 0.68f - sin(pt * PI).toFloat() * h * 0.42f
                                val slope = -cos(pt * PI).toFloat() * 0.65f
                                drawLine(
                                    goldColor,
                                    Offset(px - 46.dp.toPx(), py - slope * 46.dp.toPx()),
                                    Offset(px + 46.dp.toPx(), py + slope * 46.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawCircle(goldColor, radius = 5.5.dp.toPx(), center = Offset(px, py))
                            }
                            RealmId.PHYSICS -> {
                                // Oscillating wave & orbiting particle
                                val wave = Path()
                                for (i in 0..60) {
                                    val t = i / 60f
                                    val x = w * 0.1f + t * w * 0.8f
                                    val y = c.y + sin((t * 4f + progress * 3f) * PI).toFloat() * h * 0.2f
                                    if (i == 0) wave.moveTo(x, y) else wave.lineTo(x, y)
                                }
                                drawPath(wave, lineColor, style = Stroke(2.4.dp.toPx()))
                                val px = w * 0.1f + progress * w * 0.8f
                                val py = c.y + sin((progress * 4f + progress * 3f) * PI).toFloat() * h * 0.2f
                                drawCircle(goldColor, radius = 6.dp.toPx(), center = Offset(px, py))
                            }
                            RealmId.CHEMISTRY -> {
                                // Dual bonded atoms sharing electrons
                                val left = Offset(w * 0.36f, c.y)
                                val right = Offset(w * 0.64f, c.y)
                                drawLine(goldColor, left, right, strokeWidth = 3.dp.toPx())
                                drawCircle(lineColor.copy(alpha = 0.25f), radius = 34.dp.toPx(), center = left)
                                drawCircle(lineColor, radius = 34.dp.toPx(), center = left, style = Stroke(2.dp.toPx()))
                                drawCircle(lineColor.copy(alpha = 0.25f), radius = 34.dp.toPx(), center = right)
                                drawCircle(lineColor, radius = 34.dp.toPx(), center = right, style = Stroke(2.dp.toPx()))
                                val angle = progress * 6f * PI
                                val ePos = Offset(
                                    c.x + (48.dp.toPx() * cos(angle)).toFloat(),
                                    c.y + (22.dp.toPx() * sin(angle)).toFloat()
                                )
                                drawCircle(goldColor, radius = 5.dp.toPx(), center = ePos)
                            }
                            RealmId.CODING_AI -> {
                                // Neural network pulsing edges
                                val layers = listOf(0.24f, 0.5f, 0.76f)
                                val ys = listOf(0.28f, 0.5f, 0.72f)
                                for (l in 0 until 2) {
                                    ys.forEach { y1 ->
                                        ys.forEach { y2 ->
                                            drawLine(
                                                lineColor.copy(alpha = 0.35f),
                                                Offset(w * layers[l], h * y1),
                                                Offset(w * layers[l + 1], h * y2),
                                                strokeWidth = 1.4.dp.toPx()
                                            )
                                        }
                                    }
                                }
                                layers.forEachIndexed { idx, lx ->
                                    ys.forEach { ly ->
                                        val activeNode = ((progress * 3f).toInt() == idx)
                                        drawCircle(
                                            if (activeNode) goldColor else lineColor,
                                            radius = if (activeNode) 8.dp.toPx() else 6.dp.toPx(),
                                            center = Offset(w * lx, h * ly)
                                        )
                                    }
                                }
                            }
                            RealmId.ENGLISH -> {
                                // Aristotle Rhetorical Triangle
                                val top = Offset(w * 0.5f, h * 0.22f)
                                val bl = Offset(w * 0.28f, h * 0.74f)
                                val br = Offset(w * 0.72f, h * 0.74f)
                                val tri = Path().apply {
                                    moveTo(top.x, top.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
                                }
                                drawPath(tri, lineColor, style = Stroke(2.4.dp.toPx()))
                                val pts = listOf(top, bl, br)
                                val activePt = pts[(progress * 3f).toInt().coerceIn(0, 2)]
                                pts.forEach { pt ->
                                    drawCircle(
                                        if (pt == activePt) goldColor else lineColor,
                                        radius = if (pt == activePt) 9.dp.toPx() else 6.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                            else -> {
                                val r = w * 0.22f * (0.85f + 0.15f * sin(progress * PI * 4).toFloat())
                                drawCircle(goldColor, radius = r.coerceAtLeast(12f), center = c, style = Stroke(2.5.dp.toPx()))
                                drawCircle(lineColor, radius = 8f, center = c)
                            }
                        }
                    }

                    // Top Video Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = level.videoTitle,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Live Subtitles Overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.68f)
                    ) {
                        Text(
                            text = captions[captionIndex],
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFF8E1)
                        )
                    }

                    // Center Pause/Completed Badge if stopped cleanly
                    if (!isPlaying && activeCheckpoint == null) {
                        IconButton(
                            onClick = {
                                if (videoFinished) {
                                    progress = 0f
                                    clearedCheckpointIds = emptySet()
                                    videoFinished = false
                                }
                                isPlaying = true
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = if (videoFinished) Icons.Default.Replay else Icons.Default.PlayArrow,
                                contentDescription = if (videoFinished) "Replay Video" else "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Player Control Bar with Checkpoint Markers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.cardSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Custom Progress Rail with Checkpoint Diamonds
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.elevatedSurface)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(accentColor, Color(0xFFE88D14)))
                                )
                        )
                        // Checkpoint markers along timeline
                        Row(modifier = Modifier.fillMaxWidth()) {
                            level.checkpoints.forEach { cp ->
                                val cleared = clearedCheckpointIds.contains(cp.id)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(cp.triggerProgress)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(if (cleared) Color(0xFF10B981) else Color(0xFFE88D14))
                                            .border(1.5.dp, palette.cardSurface, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (activeCheckpoint == null) {
                                        if (videoFinished) {
                                            progress = 0f
                                            clearedCheckpointIds = emptySet()
                                            videoFinished = false
                                        }
                                        isPlaying = !isPlaying
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = accentColor
                                )
                            }
                            val currentSec = (progress * level.videoDurationSeconds).toInt()
                            Text(
                                text = "0:${currentSec.toString().padStart(2, '0')} / 0:${level.videoDurationSeconds.toString().padStart(2, '0')}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1f, 1.5f, 2f).forEach { spd ->
                                Surface(
                                    onClick = { playbackSpeed = spd },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (playbackSpeed == spd) accentColor else palette.elevatedSurface
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (playbackSpeed == spd) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // AUTO-PAUSED MID-VIDEO CHECKPOINT GATE
        AnimatedVisibility(
            visible = activeCheckpoint != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            activeCheckpoint?.let { cp ->
                IndoToneSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mid_video_checkpoint_card"),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = palette.cardSurface,
                    hairlineColor = Color(0xFFE88D14)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE88D14).copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "AUTO-PAUSED CHECKPOINT • ${cp.timestampLabel}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFC2590B)
                            )
                        }
                        Text(
                            text = cp.question.prompt,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        cp.question.options.forEachIndexed { idx, opt ->
                            val isSelected = selectedCheckpointOption == idx
                            val isCorrect = idx == cp.question.correctIndex
                            val bg = when {
                                checkpointSubmitted && isCorrect -> Color(0xFF10B981).copy(alpha = 0.18f)
                                checkpointSubmitted && isSelected && !isCorrect -> palette.accentVermilion.copy(alpha = 0.18f)
                                isSelected -> palette.secondaryIndigo
                                else -> palette.elevatedSurface
                            }
                            IndoToneSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(!checkpointSubmitted) { selectedCheckpointOption = idx },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = bg,
                                hairlineColor = if (isSelected) Color(0xFFE88D14) else palette.goldHairline
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (!checkpointSubmitted) selectedCheckpointOption = idx },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE88D14))
                                    )
                                    Text(
                                        text = opt,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (checkpointSubmitted) {
                            Text(
                                text = cp.question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                if (!checkpointSubmitted) {
                                    val chosen = selectedCheckpointOption ?: return@Button
                                    checkpointSubmitted = true
                                    onCheckpointAnswered(cp.question, chosen)
                                    if (chosen == cp.question.correctIndex) {
                                        SoundHelper.playTempleChime(context)
                                    } else {
                                        SoundHelper.playWoodyTock(context)
                                    }
                                } else {
                                    clearedCheckpointIds = clearedCheckpointIds + cp.id
                                    activeCheckpoint = null
                                    isPlaying = true
                                }
                            },
                            enabled = selectedCheckpointOption != null,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (checkpointSubmitted) Color(0xFF10B981) else Color(0xFFE88D14),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (checkpointSubmitted) "Resume Lecture →" else "Verify Checkpoint",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (videoFinished) {
            Button(
                onClick = onLectureCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("proceed_to_quick_test_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE88D14),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proceed to Quick Test (4 Questions) →", fontWeight = FontWeight.Bold)
            }
        }
    }
}
