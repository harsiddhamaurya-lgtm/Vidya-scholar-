package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndoToneSurface
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.IndoMotion
import com.example.viewmodel.GameType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 60% Distraction-Free Game Arena Backdrop:
 * - Dark mode: Deep Temple Basalt (#0B0E14)
 * - Light mode: Warm Matte Sandstone (#EAE3D5)
 * - Ultra-subtle sacred geometry / Ashtapada watermark capped at <=4% opacity.
 */
@Composable
fun GameZoneBackdrop(
    ambientGlowColor: Color,
    isAshtapada: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalIndoPalette.current
    val arenaBg = when {
        palette.isEInk -> Color(0xFFF2EFE9)
        palette.isDark -> Color(0xFF0B0E14)
        else -> Color(0xFFEAE3D5)
    }
    val watermarkBase = when {
        palette.isEInk -> Color.Transparent
        palette.isDark -> Color(0xFFD4AF37).copy(alpha = 0.038f)
        else -> Color(0xFF5C3A16).copy(alpha = 0.038f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(arenaBg)
    ) {
        if (!palette.isEInk) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Soft ambient focal halo centered behind the board
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientGlowColor.copy(alpha = if (palette.isDark) 0.10f else 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.52f),
                        radius = size.width * 0.78f
                    ),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * 0.5f, size.height * 0.52f)
                )

                val stroke = Stroke(width = 1.dp.toPx())
                val center = Offset(size.width * 0.5f, size.height * 0.52f)

                // Sacred Concentric Mandala rings (<= 4% opacity)
                for (rFactor in listOf(0.32f, 0.44f, 0.58f)) {
                    drawCircle(
                        color = watermarkBase,
                        radius = size.width * rFactor,
                        center = center,
                        style = stroke
                    )
                }

                // 8-ray lotus axis lines
                for (i in 0 until 8) {
                    val angle = i * PI / 4.0
                    val rMax = size.width * 0.58f
                    drawLine(
                        color = watermarkBase,
                        start = center,
                        end = Offset(
                            center.x + (rMax * cos(angle)).toFloat(),
                            center.y + (rMax * sin(angle)).toFloat()
                        ),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }

                // Extra Ashtapada architectural corner framing hints
                if (isAshtapada) {
                    val pad = 18.dp.toPx()
                    val step = 28.dp.toPx()
                    drawRect(
                        color = watermarkBase,
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - pad * 2, size.height - pad * 2),
                        style = stroke
                    )
                    drawLine(watermarkBase, Offset(pad, pad), Offset(pad + step, pad + step), strokeWidth = 1.dp.toPx())
                    drawLine(watermarkBase, Offset(size.width - pad, pad), Offset(size.width - pad - step, pad + step), strokeWidth = 1.dp.toPx())
                    drawLine(watermarkBase, Offset(pad, size.height - pad), Offset(pad + step, size.height - pad - step), strokeWidth = 1.dp.toPx())
                    drawLine(watermarkBase, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - step, size.height - pad - step), strokeWidth = 1.dp.toPx())
                }
            }
        }
        content()
    }
}

/**
 * Multi-axis 3D GPU-accelerated Pasa / Dice control with bounce, tumble rotation,
 * mid-air face cycling, and crisp settle onto final value.
 */
@Composable
fun AnimatedPasaDice(
    diceValue: Int,
    isRolling: Boolean,
    accentColor: Color,
    enabled: Boolean,
    testTag: String,
    pieceHint: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalIndoPalette.current
    var displayFace by remember { mutableStateOf(diceValue) }

    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val rotZ = remember { Animatable(0f) }
    val bounceY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            // Upward jump & 3D tumble loop while rolling
            bounceY.animateTo(-18f, tween(140, easing = IndoMotion.OrganicDeceleration))
            scale.animateTo(1.16f, tween(120, easing = IndoMotion.OrganicDeceleration))
            var tick = 0
            while (isRolling) {
                displayFace = ((tick * 2 + 1) % 6) + 1
                rotX.snapTo((rotX.value + 115f) % 360f)
                rotY.snapTo((rotY.value + 145f) % 360f)
                rotZ.snapTo((rotZ.value + 65f) % 360f)
                tick++
                delay(55)
            }
        } else {
            displayFace = diceValue
            // Crisp settle back onto surface
            rotX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
            rotY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
            rotZ.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
            bounceY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
            scale.animateTo(1.06f, tween(80))
            scale.animateTo(1f, tween(140, easing = IndoMotion.OrganicDeceleration))
        }
    }

    LaunchedEffect(diceValue) {
        if (!isRolling) {
            displayFace = diceValue
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(62.dp)
                .testTag(testTag)
                .graphicsLayer {
                    rotationX = rotX.value
                    rotationY = rotY.value
                    rotationZ = rotZ.value
                    translationY = bounceY.value
                    scaleX = scale.value
                    scaleY = scale.value
                    cameraDistance = 12f * density
                }
                .clickable(enabled = enabled, onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = accentColor,
            border = BorderStroke(1.dp, if (enabled) palette.accentGold.copy(alpha = 0.78f) else palette.goldHairline),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    drawDiePips(displayFace, Color.White)
                }
            }
        }

        if (pieceHint != null) {
            Text(
                text = pieceHint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

private fun DrawScope.drawDiePips(value: Int, pipColor: Color) {
    val r = size.minDimension * 0.11f
    val c1 = size.width * 0.22f
    val c2 = size.width * 0.50f
    val c3 = size.width * 0.78f
    val r1 = size.height * 0.22f
    val r2 = size.height * 0.50f
    val r3 = size.height * 0.78f

    val pts = when (value) {
        1 -> listOf(Offset(c2, r2))
        2 -> listOf(Offset(c1, r1), Offset(c3, r3))
        3 -> listOf(Offset(c1, r1), Offset(c2, r2), Offset(c3, r3))
        4 -> listOf(Offset(c1, r1), Offset(c3, r1), Offset(c1, r3), Offset(c3, r3))
        5 -> listOf(Offset(c1, r1), Offset(c3, r1), Offset(c2, r2), Offset(c1, r3), Offset(c3, r3))
        else -> listOf(
            Offset(c1, r1), Offset(c3, r1),
            Offset(c1, r2), Offset(c3, r2),
            Offset(c1, r3), Offset(c3, r3)
        )
    }

    pts.forEach { p ->
        drawCircle(color = pipColor.copy(alpha = 0.25f), radius = r * 1.35f, center = p)
        drawCircle(color = pipColor, radius = r, center = p)
    }
}

/**
 * Golden / Cyan shockwave ripple + 12 radial geometric diamond sparks emitted on piece capture.
 */
@Composable
fun CaptureParticleBurst(
    burstKey: Int,
    gridX: Float,
    gridY: Float,
    gridDimension: Float,
    burstColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(burstKey) {
        if (burstKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 440, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
            )
        }
    }

    if (burstKey > 0 && progress.value < 0.99f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = progress.value
            val cellW = size.width / gridDimension
            val cellH = size.height / gridDimension
            val cx = gridX * cellW
            val cy = gridY * cellH
            val alpha = (1f - t).coerceIn(0f, 1f)

            // Dual concentric shockwave rings (Gold + Accent)
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = alpha * 0.85f),
                radius = cellW * (0.35f + t * 1.45f),
                center = Offset(cx, cy),
                style = Stroke(width = (3.5f * (1f - t) + 0.8f).dp.toPx())
            )
            drawCircle(
                color = burstColor.copy(alpha = alpha * 0.7f),
                radius = cellW * (0.2f + t * 1.0f),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // 12 radial spark diamonds
            val numParticles = 12
            for (i in 0 until numParticles) {
                val angle = (i * (2.0 * PI / numParticles)) + t * 0.45
                val dist = cellW * (0.25f + t * 1.55f)
                val px = cx + (dist * cos(angle)).toFloat()
                val py = cy + (dist * sin(angle)).toFloat()
                val pColor = if (i % 2 == 0) Color(0xFFD4AF37) else Color(0xFF00D2E6)
                val pSize = (4.5f * (1f - t * 0.6f)).dp.toPx()

                drawCircle(
                    color = pColor.copy(alpha = alpha),
                    radius = pSize,
                    center = Offset(px, py)
                )
            }
        }
    }
}

/**
 * Geometric Mandala Celebration Overlay shown when a match reaches victory state.
 */
@Composable
fun VictoryMandalaOverlay(
    visible: Boolean,
    winnerTitle: String,
    subtitle: String,
    accentColor: Color,
    onRematch: () -> Unit,
    onExit: () -> Unit
) {
    val palette = LocalIndoPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "victory_mandala")
    val mandalaAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mandala_spin"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260, easing = IndoMotion.OrganicDeceleration)) +
            scaleIn(initialScale = 0.88f, animationSpec = tween(280, easing = IndoMotion.OrganicDeceleration)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.64f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            if (!palette.isEInk) {
                Canvas(modifier = Modifier.size(340.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    rotate(mandalaAngle, pivot = center) {
                        for (ring in 1..3) {
                            drawCircle(
                                color = palette.accentGold.copy(alpha = 0.24f),
                                radius = size.minDimension * (0.22f + ring * 0.09f),
                                center = center,
                                style = Stroke(1.5.dp.toPx())
                            )
                        }
                        for (i in 0 until 12) {
                            val angle = i * PI / 6.0
                            val r1 = size.minDimension * 0.20f
                            val r2 = size.minDimension * 0.48f
                            drawLine(
                                color = accentColor.copy(alpha = 0.30f),
                                start = Offset(center.x + (r1 * cos(angle)).toFloat(), center.y + (r1 * sin(angle)).toFloat()),
                                end = Offset(center.x + (r2 * cos(angle)).toFloat(), center.y + (r2 * sin(angle)).toFloat()),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }
            }

            IndoToneSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                shape = RoundedCornerShape(26.dp),
                containerColor = palette.cardSurface,
                hairlineColor = palette.accentGold.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Victory",
                            tint = palette.accentGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = winnerTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExit,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.goldHairline)
                        ) {
                            Text("Exit Arena", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onRematch,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Abstract Art-Type Flowing Banner & Indo-Futuristic Emblem Art:
 * - Light Mode: Warm airy sandstone/terracotta/teal light washes + luminous soft ribbons
 * - Dark Mode: Deep obsidian/indigo/copper rich jewel washes + glowing ribbons
 * - E-Ink Mode: Pure paper surface with fading monochrome contour boundaries only
 */
@Composable
fun AbstractGameArtBanner(
    gameType: GameType,
    primaryColor: Color,
    secondaryColor: Color,
    accentGold: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (palette.isEInk) {
            // Pure paper base with only fading geometric boundaries and contour strokes
            drawRect(color = palette.cardSurface)
            val ink = Color(0xFF171717)

            val contour1 = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.24f, h * 0.26f, w * 0.56f, h * 1.06f, w, h * 0.36f)
            }
            drawPath(
                path = contour1,
                brush = Brush.horizontalGradient(
                    colors = listOf(ink.copy(alpha = 0.36f), ink.copy(alpha = 0.14f), ink.copy(alpha = 0.03f))
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )

            val contour2 = Path().apply {
                moveTo(0f, h * 0.42f)
                cubicTo(w * 0.32f, h * 0.84f, w * 0.68f, h * 0.08f, w, h * 0.56f)
            }
            drawPath(
                path = contour2,
                brush = Brush.horizontalGradient(
                    colors = listOf(ink.copy(alpha = 0.05f), ink.copy(alpha = 0.26f), ink.copy(alpha = 0.04f))
                ),
                style = Stroke(width = 1.dp.toPx())
            )

            // Fading bottom divider boundary
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(ink.copy(alpha = 0.28f), ink.copy(alpha = 0.12f), Color.Transparent)
                ),
                start = Offset(0f, h - 0.5.dp.toPx()),
                end = Offset(w, h - 0.5.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Minimal fading emblem boundary rings
            val emblemCenter = Offset(w * 0.84f, h * 0.50f)
            val r = h * 0.35f
            drawCircle(
                color = ink.copy(alpha = 0.28f),
                radius = r,
                center = emblemCenter,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = ink.copy(alpha = 0.12f),
                radius = r * 1.24f,
                center = emblemCenter,
                style = Stroke(width = 0.8.dp.toPx())
            )
            return@Canvas
        }

        // Theme-adaptive 30% Base Gradient: Light airy tones in Light Mode, deep jewel tones in Dark Mode
        val baseStops = if (palette.isDark) {
            when (gameType) {
                GameType.SHATRANJ -> listOf(Color(0xFF28171C), Color(0xFF4B211B), Color(0xFF70301C))
                GameType.LUDO -> listOf(Color(0xFF16223C), Color(0xFF163D4D), Color(0xFF3E2247))
                GameType.CHATURANGA -> listOf(Color(0xFF132631), Color(0xFF1B344D), Color(0xFF2E274A))
            }
        } else {
            when (gameType) {
                GameType.SHATRANJ -> listOf(Color(0xFFF3E4DA), Color(0xFFECD2C2), Color(0xFFE7C5B0))
                GameType.LUDO -> listOf(Color(0xFFE1E8F4), Color(0xFFD7EAE7), Color(0xFFEBE1EF))
                GameType.CHATURANGA -> listOf(Color(0xFFD9EAE8), Color(0xFFDDE4F1), Color(0xFFEBE3D3))
            }
        }

        drawRect(
            brush = Brush.linearGradient(
                colors = baseStops,
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        val waveAlpha1 = if (palette.isDark) 0.48f else 0.24f
        val waveAlpha2 = if (palette.isDark) 0.36f else 0.18f
        val waveAlpha3 = if (palette.isDark) 0.28f else 0.15f

        // Flowing Organic Bezier Color Waves (3 layered ribbons)
        val wave1 = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.24f, h * 0.26f, w * 0.56f, h * 1.08f, w, h * 0.36f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = wave1,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = waveAlpha1),
                    secondaryColor.copy(alpha = waveAlpha1 * 0.88f),
                    accentGold.copy(alpha = waveAlpha1 * 0.72f)
                )
            )
        )

        val wave2 = Path().apply {
            moveTo(0f, h * 0.44f)
            cubicTo(w * 0.32f, h * 0.86f, w * 0.68f, h * 0.06f, w, h * 0.56f)
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(
            path = wave2,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = waveAlpha2),
                    accentGold.copy(alpha = waveAlpha2 * 0.75f),
                    primaryColor.copy(alpha = waveAlpha2)
                )
            )
        )

        val wave3 = Path().apply {
            moveTo(0f, h * 0.92f)
            cubicTo(w * 0.36f, h * 0.52f, w * 0.72f, h * 0.96f, w, h * 0.68f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = wave3,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentGold.copy(alpha = waveAlpha3),
                    primaryColor.copy(alpha = waveAlpha3 * 1.15f),
                    secondaryColor.copy(alpha = waveAlpha3)
                )
            )
        )

        // Floating translucent abstract spheres
        drawCircle(
            color = accentGold.copy(alpha = if (palette.isDark) 0.14f else 0.12f),
            radius = h * 0.46f,
            center = Offset(w * 0.20f, h * 0.24f)
        )
        drawCircle(
            color = secondaryColor.copy(alpha = if (palette.isDark) 0.18f else 0.11f),
            radius = h * 0.34f,
            center = Offset(w * 0.62f, h * 0.80f)
        )

        // Subtle molten gold geometric arc sweep
        drawArc(
            color = accentGold.copy(alpha = if (palette.isDark) 0.22f else 0.30f),
            startAngle = -35f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.35f, -h * 0.3f),
            size = Size(w * 0.5f, h * 1.6f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Custom Indo-Futuristic Emblem Art (10% luminous accent focal point)
        val emblemCenter = Offset(w * 0.84f, h * 0.50f)
        val r = h * 0.36f
        val emblemCore = if (palette.isDark) Color.White else Color(0xFF1C1917)

        drawCircle(
            color = accentGold.copy(alpha = if (palette.isDark) 0.16f else 0.14f),
            radius = r * 1.28f,
            center = emblemCenter
        )
        drawCircle(
            color = accentGold.copy(alpha = 0.42f),
            radius = r * 1.14f,
            center = emblemCenter,
            style = Stroke(width = 0.9.dp.toPx())
        )
        drawCircle(
            color = accentGold.copy(alpha = 0.86f),
            radius = r,
            center = emblemCenter,
            style = Stroke(width = 1.8.dp.toPx())
        )

        when (gameType) {
            GameType.SHATRANJ -> {
                val crown = Path().apply {
                    moveTo(emblemCenter.x - r * 0.54f, emblemCenter.y + r * 0.34f)
                    lineTo(emblemCenter.x - r * 0.64f, emblemCenter.y - r * 0.20f)
                    lineTo(emblemCenter.x - r * 0.24f, emblemCenter.y + r * 0.05f)
                    lineTo(emblemCenter.x, emblemCenter.y - r * 0.50f)
                    lineTo(emblemCenter.x + r * 0.24f, emblemCenter.y + r * 0.05f)
                    lineTo(emblemCenter.x + r * 0.64f, emblemCenter.y - r * 0.20f)
                    lineTo(emblemCenter.x + r * 0.54f, emblemCenter.y + r * 0.34f)
                    close()
                }
                drawPath(crown, color = accentGold.copy(alpha = 0.92f))
                drawCircle(
                    emblemCore.copy(alpha = 0.92f),
                    radius = 2.8.dp.toPx(),
                    center = Offset(emblemCenter.x, emblemCenter.y - r * 0.52f)
                )
            }
            GameType.LUDO -> {
                val colors = listOf(
                    Color(0xFFE11D48),
                    Color(0xFF10B981),
                    Color(0xFFF59E0B),
                    Color(0xFF00D2E6)
                )
                val offsets = listOf(
                    Offset(-r * 0.38f, -r * 0.38f),
                    Offset(r * 0.06f, -r * 0.38f),
                    Offset(r * 0.06f, r * 0.06f),
                    Offset(-r * 0.38f, r * 0.06f)
                )
                offsets.forEachIndexed { idx, off ->
                    drawRoundRect(
                        color = colors[idx].copy(alpha = if (palette.isDark) 0.92f else 0.86f),
                        topLeft = Offset(emblemCenter.x + off.x, emblemCenter.y + off.y),
                        size = Size(r * 0.32f, r * 0.32f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                drawCircle(emblemCore, radius = r * 0.14f, center = emblemCenter)
            }
            GameType.CHATURANGA -> {
                for (i in 0 until 8) {
                    val a = i * PI / 4.0
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.88f),
                        start = emblemCenter,
                        end = Offset(
                            emblemCenter.x + (r * 0.82f * cos(a)).toFloat(),
                            emblemCenter.y + (r * 0.82f * sin(a)).toFloat()
                        ),
                        strokeWidth = 1.6.dp.toPx()
                    )
                }
                val diamond = Path().apply {
                    moveTo(emblemCenter.x, emblemCenter.y - r * 0.54f)
                    lineTo(emblemCenter.x + r * 0.54f, emblemCenter.y)
                    lineTo(emblemCenter.x, emblemCenter.y + r * 0.54f)
                    lineTo(emblemCenter.x - r * 0.54f, emblemCenter.y)
                    close()
                }
                drawPath(diamond, color = accentGold.copy(alpha = 0.34f))
                drawPath(diamond, color = accentGold, style = Stroke(1.6.dp.toPx()))
                drawCircle(emblemCore, radius = r * 0.14f, center = emblemCenter)
            }
        }
    }
}

