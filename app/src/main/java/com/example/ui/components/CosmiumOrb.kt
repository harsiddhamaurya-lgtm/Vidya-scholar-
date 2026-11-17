package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIndoPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class CosmiumMood(
    val stateKey: String,
    val badgeLabel: String,
    val colorHex: Int
) {
    IDLE("idle", "READY", 0xFFE066),
    SPEAKING("speaking", "SPEAKING", 0xFFE86E),
    THINKING("thinking", "THINKING", 0xFFC107),
    CURIOUS("curious", "LISTENING", 0xFFD54F),
    CELEBRATE("celebrate", "INSPIRED", 0x34D399),
    ALERT("alert", "RETRYING", 0xFF8A65)
}

/**
 * Pure Jetpack Compose 60/120fps fluid lava-lamp oil-blob mascot ("Cosmium").
 * Renders an organic closed Catmull-Rom spline silhouette with multi-harmonic
 * surface-tension wobbles, Zero-G buoyant drift, internal plasma convection
 * pockets, soft specular oil sheen, and state-driven speaking/emotion pulses.
 */
@Composable
fun CosmiumOrb(
    size: Dp,
    mood: CosmiumMood = CosmiumMood.IDLE,
    interactionIntensity: Float = 0f,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val palette = LocalIndoPalette.current
    var tapBurst by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(tapBurst) {
        if (tapBurst > 0f) {
            delay(650L)
            tapBurst = 0f
        }
    }

    val activeBurst by animateFloatAsState(
        targetValue = tapBurst,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.55f),
        label = "cosmium_tap_burst"
    )

    val combinedInteraction = (interactionIntensity + activeBurst).coerceIn(0f, 1.5f)

    // Emotion-driven neon glowing plasma color transitions
    val targetBodyColor = when {
        palette.isEInk -> palette.accentSaffron
        mood == CosmiumMood.CELEBRATE -> Color(0xFF00FF88) // Neon Emerald/Mint
        mood == CosmiumMood.ALERT -> Color(0xFFFF2A6D)     // Neon Magenta/Coral
        mood == CosmiumMood.THINKING -> Color(0xFF00F0FF)  // Neon Cyan
        mood == CosmiumMood.SPEAKING -> Color(0xFFFFEA00)  // Neon Electric Yellow
        else -> Color(0xFFFFFF33)                          // Luminous Neon Gold
    }
    val targetDeepColor = when {
        palette.isEInk -> palette.accentGold
        mood == CosmiumMood.CELEBRATE -> Color(0xFF00B0FF) // Neon Blue Glow
        mood == CosmiumMood.ALERT -> Color(0xFFFF5252)     // Neon Red Glow
        mood == CosmiumMood.THINKING -> Color(0xFF7C4DFF)  // Neon Purple Glow
        mood == CosmiumMood.SPEAKING -> Color(0xFFFF9100)  // Neon Orange Glow
        else -> Color(0xFFFFC400)                          // Neon Amber Glow
    }
    val targetHighlightColor = when {
        palette.isEInk -> Color.White
        mood == CosmiumMood.CELEBRATE -> Color(0xFFB9F6CA)
        mood == CosmiumMood.THINKING -> Color(0xFF84FFFF)
        else -> Color(0xFFFFFF8D)
    }

    val bodyColor by animateColorAsState(targetBodyColor, tween(450), label = "cosmium_body_col")
    val deepColor by animateColorAsState(targetDeepColor, tween(450), label = "cosmium_deep_col")
    val highlightColor by animateColorAsState(targetHighlightColor, tween(450), label = "cosmium_hi_col")

    val infiniteTransition = rememberInfiniteTransition(label = "cosmium_lava_loop")
    // Continuous phase [0 .. 2*PI*12] to prevent harsh loop seams across harmonics
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI * 12.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cosmium_phase"
    )

    val tapInteractionSource = remember { MutableInteractionSource() }
    val tapScale by animateFloatAsState(
        targetValue = if (tapBurst > 0f) 1.09f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.45f),
        label = "cosmium_tap_scale"
    )

    val clickModifier = if (onTap != null) {
        Modifier.clickable(
            interactionSource = tapInteractionSource,
            indication = null
        ) {
            tapBurst = 1f
            onTap()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = tapScale
                scaleY = tapScale
            }
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val baseR = minOf(w, h) * 0.38f
            val isEInk = palette.isEInk

            val speedMul = when (mood) {
                CosmiumMood.SPEAKING -> 1.85f
                CosmiumMood.THINKING -> 1.65f
                CosmiumMood.CELEBRATE -> 1.95f
                CosmiumMood.CURIOUS -> 1.35f
                CosmiumMood.ALERT -> 1.25f
                CosmiumMood.IDLE -> 1.0f
            } + combinedInteraction * 0.45f

            val t = if (isEInk) 0.4f else phase * speedMul

            // Buoyant Zero-G floating offset inside canvas bounds
            val floatX = if (isEInk) 0f else sin(t * 0.833f) * baseR * 0.055f
            val floatY = if (isEInk) 0f else cos(t * 1.166f) * baseR * 0.075f -
                (if (mood == CosmiumMood.CELEBRATE) abs(sin(t * 2.5f)) * baseR * 0.08f else 0f)

            val center = Offset(w * 0.5f + floatX, h * 0.5f + floatY)

            // Volume-preserving gelatinous squash & stretch + speech cadence
            val speechCadence = if (mood == CosmiumMood.SPEAKING && !isEInk) {
                0.045f * sin(t * 5.0f) + 0.032f * sin(t * 8.0f + 0.7f)
            } else {
                0f
            }
            val breathAmp = when (mood) {
                CosmiumMood.SPEAKING -> 0.055f
                CosmiumMood.THINKING -> 0.048f
                CosmiumMood.CELEBRATE -> 0.07f
                CosmiumMood.CURIOUS -> 0.038f
                else -> 0.025f
            } + combinedInteraction * 0.03f
            val squishY = if (isEInk) 0f else sin(t * 1.5f) * breathAmp + speechCadence
            val scaleY = 1f + squishY
            val scaleX = 1f - squishY * 0.5f

            // Layer 1: Soft Atmospheric Neon Halo Glow
            if (!isEInk) {
                val auraPulse = 1f + 0.15f * sin(t * 2.0f) + combinedInteraction * 0.15f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            bodyColor.copy(alpha = if (mood == CosmiumMood.SPEAKING) 0.55f else 0.38f),
                            deepColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseR * 1.85f * auraPulse
                    ),
                    radius = baseR * 1.85f * auraPulse,
                    center = center
                )
            }

            // Layer 2: Speaking Sound-Pulse Halo Rings
            if (mood == CosmiumMood.SPEAKING && !isEInk) {
                val p1 = (t * 0.85f) % 1f
                val p2 = ((t * 0.85f) + 0.5f) % 1f
                drawCircle(
                    color = bodyColor.copy(alpha = 0.36f * (1f - p1)),
                    radius = baseR * (1.02f + 0.42f * p1),
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
                drawCircle(
                    color = highlightColor.copy(alpha = 0.26f * (1f - p2)),
                    radius = baseR * (1.02f + 0.42f * p2),
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Organic fluid contour builder (smooth closed Catmull-Rom -> cubic Bezier)
            val lobeIntensity = when (mood) {
                CosmiumMood.SPEAKING -> 0.095f
                CosmiumMood.THINKING -> 0.085f
                CosmiumMood.CELEBRATE -> 0.10f
                CosmiumMood.CURIOUS -> 0.072f
                CosmiumMood.ALERT -> 0.06f
                CosmiumMood.IDLE -> 0.055f
            } + combinedInteraction * 0.045f

            fun buildOilBlobPath(radiusScale: Float, phaseLag: Float): Path {
                val count = 32
                val pts = Array(count) { i ->
                    val theta = (i.toFloat() / count.toFloat()) * (2f * PI.toFloat())
                    val lt = t - phaseLag
                    // Low-order organic lava-lamp harmonics (2, 3, 4 lobes)
                    val h2 = cos(2f * theta + lt * 1.166f) * 0.50f
                    val h3 = sin(3f * theta - lt * 1.50f + 0.8f) * 0.34f
                    val h4 = cos(4f * theta + lt * 2.0f - 0.4f) * 0.16f
                    val vocalRipple = if (mood == CosmiumMood.SPEAKING) {
                        sin(3f * theta + lt * 5.0f) * 0.28f
                    } else 0f

                    val deform = 1f + (h2 + h3 + h4 + vocalRipple) * lobeIntensity
                    val rx = baseR * radiusScale * deform * scaleX
                    val ry = baseR * radiusScale * deform * scaleY
                    Offset(
                        x = center.x + cos(theta) * rx,
                        y = center.y + sin(theta) * ry
                    )
                }

                return Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    val tension = 1f / 6f
                    for (i in 0 until count) {
                        val p0 = pts[(i - 1 + count) % count]
                        val p1 = pts[i]
                        val p2 = pts[(i + 1) % count]
                        val p3 = pts[(i + 2) % count]

                        val cp1x = p1.x + (p2.x - p0.x) * tension
                        val cp1y = p1.y + (p2.y - p0.y) * tension
                        val cp2x = p2.x - (p3.x - p1.x) * tension
                        val cp2y = p2.y - (p3.y - p1.y) * tension
                        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                    }
                    close()
                }
            }

            // Outer translucent membrane envelope
            if (!isEInk) {
                val outerPath = buildOilBlobPath(radiusScale = 1.10f, phaseLag = 0.55f)
                drawPath(
                    path = outerPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            bodyColor.copy(alpha = 0.22f),
                            deepColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseR * 1.18f
                    )
                )
            }

            // Main viscous golden oil-blob body
            val mainPath = buildOilBlobPath(radiusScale = 1.0f, phaseLag = 0f)
            val coreShift = Offset(
                center.x - baseR * 0.16f + sin(t * 1.333f) * baseR * 0.10f,
                center.y - baseR * 0.18f + cos(t * 1.0f) * baseR * 0.10f
            )

            drawPath(
                path = mainPath,
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to highlightColor,
                        0.45f to bodyColor,
                        0.86f to deepColor,
                        1.0f to deepColor.copy(alpha = 0.92f)
                    ),
                    center = coreShift,
                    radius = baseR * 1.28f
                )
            )

            // Internal lava-lamp convection plasma pockets clipped inside the blob silhouette
            if (!isEInk) {
                clipPath(mainPath) {
                    val pocket1Center = Offset(
                        center.x + sin(t * 1.25f) * baseR * 0.32f,
                        center.y + cos(t * 0.92f) * baseR * 0.28f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0.65f),
                                bodyColor.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = pocket1Center,
                            radius = baseR * 0.56f
                        ),
                        radius = baseR * 0.56f,
                        center = pocket1Center
                    )

                    val pocket2Center = Offset(
                        center.x - cos(t * 1.08f) * baseR * 0.28f,
                        center.y + sin(t * 1.42f) * baseR * 0.30f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                deepColor.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = pocket2Center,
                            radius = baseR * 0.50f
                        ),
                        radius = baseR * 0.50f,
                        center = pocket2Center
                    )

                    // Top-left liquid-oil specular sheen
                    val sheenCenter = Offset(
                        center.x - baseR * 0.28f,
                        center.y - baseR * 0.30f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.44f),
                                Color.Transparent
                            ),
                            center = sheenCenter,
                            radius = baseR * 0.42f
                        ),
                        radius = baseR * 0.42f,
                        center = sheenCenter
                    )
                }
            } else {
                drawPath(
                    path = mainPath,
                    color = palette.goldHairline,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun CosmiumStatusPill(
    mood: CosmiumMood,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    val pillAccent = when {
        palette.isEInk -> palette.accentSaffron
        mood == CosmiumMood.CELEBRATE -> Color(0xFF34D399)
        mood == CosmiumMood.ALERT -> Color(0xFFFF8A65)
        mood == CosmiumMood.THINKING -> Color(0xFFFFCA28)
        else -> Color(0xFFFFD54F)
    }
    Surface(
        modifier = modifier.testTag("cosmium_status_pill"),
        shape = RoundedCornerShape(50),
        color = if (palette.isEInk) palette.elevatedSurface else pillAccent.copy(alpha = 0.15f),
        border = BorderStroke(0.8.dp, pillAccent.copy(alpha = 0.55f))
    ) {
        Text(
            text = mood.badgeLabel,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.7.sp,
            color = pillAccent
        )
    }
}
