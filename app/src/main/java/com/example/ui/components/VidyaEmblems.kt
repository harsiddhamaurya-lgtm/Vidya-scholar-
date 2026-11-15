package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuestLevel
import com.example.data.QuestNodeState
import com.example.data.RealmVisualKind
import com.example.ui.theme.LocalIndoPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 5% opacity glowing sacred geometry background watermark inside each Realm Portal card.
 */
@Composable
fun RealmCardGeometryWatermark(
    visualKind: RealmVisualKind,
    tint: Color,
    isEInk: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.78f, size.height * 0.36f)
        val radius = size.minDimension * 0.56f
        val lineAlpha = if (isEInk) 0.08f else 0.06f
        val strokeColor = tint.copy(alpha = lineAlpha)
        val stroke = Stroke(width = 1.4.dp.toPx())

        // Outer concentric mandala aura
        drawCircle(strokeColor, radius = radius, center = center, style = stroke)
        drawCircle(strokeColor, radius = radius * 0.68f, center = center, style = stroke)

        when (visualKind) {
            RealmVisualKind.SRI_YANTRA_COMPASS -> {
                for (i in 0..2) {
                    val rot = i * 30f
                    rotate(rot, pivot = center) {
                        val tri = Path().apply {
                            for (v in 0..2) {
                                val a = (-90.0 + v * 120.0) * PI / 180.0
                                val px = center.x + (radius * 0.85f * cos(a)).toFloat()
                                val py = center.y + (radius * 0.85f * sin(a)).toFloat()
                                if (v == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(tri, strokeColor, style = stroke)
                    }
                }
            }
            RealmVisualKind.ATOMIC_CHAKRA -> {
                for (angle in listOf(0f, 60f, 120f)) {
                    rotate(angle, pivot = center) {
                        drawOval(
                            color = strokeColor,
                            topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.32f),
                            size = Size(radius * 1.8f, radius * 0.64f),
                            style = stroke
                        )
                    }
                }
            }
            RealmVisualKind.ALCHEMICAL_LOTUS -> {
                for (k in 0 until 6) {
                    val a1 = k * PI / 3.0
                    val a2 = (k + 1) * PI / 3.0
                    drawLine(
                        color = strokeColor,
                        start = Offset(center.x + (radius * 0.82f * cos(a1)).toFloat(), center.y + (radius * 0.82f * sin(a1)).toFloat()),
                        end = Offset(center.x + (radius * 0.82f * cos(a2)).toFloat(), center.y + (radius * 0.82f * sin(a2)).toFloat()),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }
            }
            RealmVisualKind.NEURAL_MANDALA -> {
                for (i in 0 until 8) {
                    val a = i * PI / 4.0
                    val tip = Offset(center.x + (radius * cos(a)).toFloat(), center.y + (radius * sin(a)).toFloat())
                    drawLine(strokeColor, center, tip, strokeWidth = 1.2.dp.toPx())
                    drawCircle(strokeColor, radius = 3.dp.toPx(), center = tip)
                }
            }
            RealmVisualKind.KAVYA_QUILL -> {
                for (i in 0 until 10) {
                    val a = i * 2.0 * PI / 10.0
                    val tip = Offset(center.x + (radius * 0.92f * cos(a)).toFloat(), center.y + (radius * 0.92f * sin(a)).toFloat())
                    drawLine(strokeColor, center, tip, strokeWidth = 1.1.dp.toPx())
                }
            }
            RealmVisualKind.PRANA_CHAKRA, RealmVisualKind.GOLDEN_SCALES,
            RealmVisualKind.DHANUR_VAULT, RealmVisualKind.ARTHA_WHEEL,
            RealmVisualKind.KALACHAKRA, RealmVisualKind.RAJASIC_SHIELD -> {
                for (i in 0 until 12) {
                    val a = i * 2.0 * PI / 12.0
                    val tip = Offset(center.x + (radius * 0.88f * cos(a)).toFloat(), center.y + (radius * 0.88f * sin(a)).toFloat())
                    drawLine(strokeColor, center, tip, strokeWidth = 1.1.dp.toPx())
                }
            }
        }
    }
}

/**
 * 3D-styled Indo-Futuristic Emblem Medallion ringed by a Radial XP Progress Ring.
 */
@Composable
fun RealmMedallionWithProgress(
    visualKind: RealmVisualKind,
    accentPrimary: Color,
    accentSecondary: Color,
    progressFraction: Float,
    size: Dp = 66.dp
) {
    val palette = LocalIndoPalette.current
    val ringColor = if (palette.isEInk) palette.accentSaffron else accentPrimary
    val trackColor = if (palette.isEInk) Color(0xFF171717).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.14f)

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerR = this.size.minDimension / 2f - 3.dp.toPx()
            val medalR = outerR - 6.dp.toPx()

            // Radial XP Ring track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(c.x - outerR, c.y - outerR),
                size = Size(outerR * 2f, outerR * 2f),
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Radial XP active sweep
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ringColor, accentSecondary, ringColor),
                    center = c
                ),
                startAngle = -90f,
                sweepAngle = (progressFraction.coerceIn(0.05f, 1f)) * 360f,
                useCenter = false,
                topLeft = Offset(c.x - outerR, c.y - outerR),
                size = Size(outerR * 2f, outerR * 2f),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3D Medallion base disc with radial bevel gradient
            if (!palette.isEInk) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.34f),
                            palette.elevatedSurface.copy(alpha = 0.95f),
                            Color(0xFF0A0D12).copy(alpha = 0.92f)
                        ),
                        center = Offset(c.x - medalR * 0.25f, c.y - medalR * 0.25f),
                        radius = medalR * 1.45f
                    ),
                    radius = medalR,
                    center = c
                )
            } else {
                drawCircle(
                    color = palette.cardSurface,
                    radius = medalR,
                    center = c
                )
            }

            // Metallic gold inner rim
            drawCircle(
                color = palette.accentGold.copy(alpha = if (palette.isEInk) 0.7f else 0.65f),
                radius = medalR,
                center = c,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Custom Emblem Vector Art
            drawEmblemGlyph(visualKind, c, medalR * 0.62f, if (palette.isEInk) palette.accentSaffron else ringColor, accentSecondary)
        }
    }
}

private fun DrawScope.drawEmblemGlyph(
    kind: RealmVisualKind,
    c: Offset,
    r: Float,
    primary: Color,
    secondary: Color
) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    when (kind) {
        RealmVisualKind.SRI_YANTRA_COMPASS -> {
            val upTri = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.86f, c.y + r * 0.62f)
                lineTo(c.x - r * 0.86f, c.y + r * 0.62f)
                close()
            }
            val downTri = Path().apply {
                moveTo(c.x, c.y + r)
                lineTo(c.x + r * 0.86f, c.y - r * 0.62f)
                lineTo(c.x - r * 0.86f, c.y - r * 0.62f)
                close()
            }
            drawPath(upTri, primary, style = stroke)
            drawPath(downTri, secondary.copy(alpha = 0.85f), style = stroke)
            drawCircle(primary, radius = r * 0.16f, center = c)
        }
        RealmVisualKind.ATOMIC_CHAKRA -> {
            for (angle in listOf(0f, 60f, 120f)) {
                rotate(angle, pivot = c) {
                    drawOval(
                        color = primary,
                        topLeft = Offset(c.x - r, c.y - r * 0.38f),
                        size = Size(r * 2f, r * 0.76f),
                        style = stroke
                    )
                }
            }
            drawCircle(secondary, radius = r * 0.22f, center = c)
        }
        RealmVisualKind.ALCHEMICAL_LOTUS -> {
            val hex = Path().apply {
                for (k in 0 until 6) {
                    val a = k * PI / 3.0 - PI / 6.0
                    val px = c.x + (r * 0.85f * cos(a)).toFloat()
                    val py = c.y + (r * 0.85f * sin(a)).toFloat()
                    if (k == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(hex, primary, style = stroke)
            drawCircle(secondary, radius = r * 0.42f, center = c, style = stroke)
            drawCircle(primary, radius = r * 0.16f, center = c)
        }
        RealmVisualKind.NEURAL_MANDALA -> {
            val nodes = (0 until 6).map { k ->
                val a = k * PI / 3.0
                Offset(c.x + (r * 0.82f * cos(a)).toFloat(), c.y + (r * 0.82f * sin(a)).toFloat())
            }
            nodes.forEach { pt ->
                drawLine(primary.copy(alpha = 0.75f), c, pt, strokeWidth = 1.4.dp.toPx())
                drawCircle(secondary, radius = 2.8.dp.toPx(), center = pt)
            }
            drawCircle(primary, radius = r * 0.26f, center = c)
        }
        RealmVisualKind.KAVYA_QUILL -> {
            val nib = Path().apply {
                moveTo(c.x + r * 0.65f, c.y - r * 0.85f)
                lineTo(c.x + r * 0.25f, c.y + r * 0.35f)
                lineTo(c.x - r * 0.45f, c.y + r * 0.8f)
                lineTo(c.x - r * 0.2f, c.y - r * 0.1f)
                close()
            }
            drawPath(nib, primary, style = stroke)
            drawLine(secondary, Offset(c.x - r * 0.45f, c.y + r * 0.8f), Offset(c.x + r * 0.15f, c.y - r * 0.25f), strokeWidth = 1.5.dp.toPx())
            drawCircle(secondary, radius = r * 0.22f, center = Offset(c.x - r * 0.55f, c.y + r * 0.85f))
        }
        RealmVisualKind.PRANA_CHAKRA -> {
            drawCircle(primary, radius = r * 0.72f, center = c, style = stroke)
            for (i in 0 until 8) {
                val a = i * PI / 4.0
                val pt = Offset(c.x + (r * 0.6f * cos(a)).toFloat(), c.y + (r * 0.6f * sin(a)).toFloat())
                drawCircle(secondary, radius = 2.5.dp.toPx(), center = pt)
            }
        }
        RealmVisualKind.GOLDEN_SCALES, RealmVisualKind.ARTHA_WHEEL -> {
            drawLine(primary, Offset(c.x - r * 0.7f, c.y), Offset(c.x + r * 0.7f, c.y), strokeWidth = 1.6.dp.toPx())
            drawLine(secondary, Offset(c.x, c.y - r * 0.6f), Offset(c.x, c.y + r * 0.6f), strokeWidth = 1.6.dp.toPx())
            drawCircle(primary, radius = r * 0.25f, center = Offset(c.x - r * 0.5f, c.y + r * 0.3f), style = stroke)
            drawCircle(primary, radius = r * 0.25f, center = Offset(c.x + r * 0.5f, c.y + r * 0.3f), style = stroke)
        }
        RealmVisualKind.DHANUR_VAULT, RealmVisualKind.KALACHAKRA, RealmVisualKind.RAJASIC_SHIELD -> {
            drawCircle(primary, radius = r * 0.65f, center = c, style = stroke)
            drawLine(secondary, Offset(c.x, c.y - r * 0.75f), Offset(c.x, c.y + r * 0.75f), strokeWidth = 1.5.dp.toPx())
            drawLine(secondary, Offset(c.x - r * 0.75f, c.y), Offset(c.x + r * 0.75f, c.y), strokeWidth = 1.5.dp.toPx())
        }
    }
}

/**
 * Circular embossed Quest Milestone Node for the vertical Candy Crush-style winding path.
 */
@Composable
fun QuestMilestoneNode(
    level: QuestLevel,
    state: QuestNodeState,
    accentPrimary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse_${level.id}")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val nodeSize = if (level.isBossBattle) 86.dp else 68.dp
    val pressScale = if (pressed) 0.94f else 1f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(nodeSize + 20.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            contentAlignment = Alignment.Center
        ) {
            // Pulsing Saffron Amber halo for ACTIVE node
            if (state == QuestNodeState.ACTIVE && !palette.isEInk) {
                Box(
                    modifier = Modifier
                        .size(nodeSize)
                        .graphicsLayer {
                            scaleX = haloScale
                            scaleY = haloScale
                            alpha = haloAlpha
                        }
                        .clip(CircleShape)
                        .background(if (level.isBossBattle) palette.accentVermilion else Color(0xFFE88D14))
                )
            }

            // Embossed Circular Pedestal
            Box(
                modifier = Modifier
                    .size(nodeSize)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(
                        brush = when {
                            palette.isEInk && state == QuestNodeState.COMPLETED ->
                                Brush.radialGradient(listOf(palette.accentSaffron, palette.accentSaffron))
                            palette.isEInk && state == QuestNodeState.ACTIVE ->
                                Brush.radialGradient(listOf(palette.cardSurface, palette.cardSurface))
                            palette.isEInk ->
                                Brush.radialGradient(listOf(palette.elevatedSurface, palette.elevatedSurface))
                            level.isBossBattle && state != QuestNodeState.LOCKED ->
                                Brush.radialGradient(listOf(Color(0xFFD6335C), Color(0xFF5A162C)))
                            state == QuestNodeState.COMPLETED ->
                                Brush.radialGradient(listOf(Color(0xFFE8B948), Color(0xFF8C6214)))
                            state == QuestNodeState.ACTIVE ->
                                Brush.radialGradient(listOf(Color(0xFFF59E0B), accentPrimary))
                            else ->
                                Brush.radialGradient(listOf(palette.elevatedSurface, palette.secondarySlate))
                        }
                    )
                    .border(
                        width = if (state == QuestNodeState.ACTIVE || level.isBossBattle) 2.5.dp else 1.5.dp,
                        color = when {
                            palette.isEInk -> palette.accentGold
                            state == QuestNodeState.COMPLETED -> palette.accentGold
                            state == QuestNodeState.ACTIVE -> Color(0xFFFFD166)
                            else -> palette.goldHairline
                        },
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner Lotus / Boss Crown / Seal artwork
                when {
                    level.isBossBattle && state != QuestNodeState.LOCKED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Boss Battle Arena",
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "BOSS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    state == QuestNodeState.COMPLETED -> {
                        // Gold Lotus Badge
                        Canvas(modifier = Modifier.size(34.dp)) {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val r = size.minDimension * 0.42f
                            for (i in 0 until 8) {
                                val a = i * PI / 4.0
                                val p1 = Offset(c.x + (r * 0.35f * cos(a - 0.35)).toFloat(), c.y + (r * 0.35f * sin(a - 0.35)).toFloat())
                                val tip = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
                                val p2 = Offset(c.x + (r * 0.35f * cos(a + 0.35)).toFloat(), c.y + (r * 0.35f * sin(a + 0.35)).toFloat())
                                val path = Path().apply {
                                    moveTo(c.x, c.y); lineTo(p1.x, p1.y); lineTo(tip.x, tip.y); lineTo(p2.x, p2.y); close()
                                }
                                drawPath(path, color = Color(0xFFFFF8E1))
                            }
                            drawCircle(Color(0xFFD4AF37), radius = r * 0.24f, center = c)
                        }
                    }
                    state == QuestNodeState.ACTIVE -> {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start Level ${level.levelNumber}",
                            tint = if (palette.isEInk) palette.accentSaffron else Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    else -> {
                        // Carved Stone Seal
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked Level ${level.levelNumber}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Compact Pill Caption under Circular Node
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = palette.cardSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (level.isBossBattle) palette.accentGold else palette.goldHairline
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (level.isBossBattle) "LVL ${level.levelNumber} • BOSS ARENA (50M)" else "LVL ${level.levelNumber} • ${level.title}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (level.isBossBattle) palette.accentVermilion else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
