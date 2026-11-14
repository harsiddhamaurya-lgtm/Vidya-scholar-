package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 60% Dominant Low-Strain Atmospheric Backdrop with subtle 3%–7% Sacred Geometry / Jali
 * watermarks and soft cinematic focal glow (disabled cleanly in E-Ink mode).
 */
@Composable
fun IndoAtmosphericBackdrop(
    modifier: Modifier = Modifier,
    ambientGlowColor: Color? = null,
    baseColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalIndoPalette.current
    val glowColor = ambientGlowColor ?: palette.accentSaffron

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor ?: palette.baseBg)
    ) {
        if (!palette.isEInk) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Soft cinematic ambient top-right glow (kept at low opacity for calm eyes)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (palette.isDark) 0.11f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f)
                )

                // Soft opposing gold warmth behind lower-left lattice
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentGold.copy(alpha = if (palette.isDark) 0.07f else 0.055f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.88f),
                        radius = size.width * 0.58f
                    ),
                    radius = size.width * 0.58f,
                    center = Offset(size.width * 0.12f, size.height * 0.88f)
                )

                // Subtle 4%–6% Sacred Mandala Watermark in top corner
                val cx = size.width * 0.88f
                val cy = size.height * 0.14f
                val mandalaColor = palette.watermarkTint
                val stroke = Stroke(width = 1.dp.toPx())

                for (ring in 1..3) {
                    drawCircle(
                        color = mandalaColor,
                        radius = ring * 44.dp.toPx(),
                        center = Offset(cx, cy),
                        style = stroke
                    )
                }

                // 8-petal lotus / star geometric spokes
                for (i in 0 until 8) {
                    val angle = (i * PI / 4.0)
                    val endX = cx + (132.dp.toPx() * cos(angle)).toFloat()
                    val endY = cy + (132.dp.toPx() * sin(angle)).toFloat()
                    drawLine(
                        color = mandalaColor,
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }

                // Subtle bottom-left architectural jali lattice diamond accents (3.5% opacity)
                val step = 34.dp.toPx()
                val startX = -step
                val startY = size.height * 0.76f
                for (r in 0..3) {
                    for (c in 0..3) {
                        val px = startX + c * step + (if (r % 2 == 1) step * 0.5f else 0f)
                        val py = startY + r * (step * 0.65f)
                        val diamond = Path().apply {
                            moveTo(px, py - step * 0.28f)
                            lineTo(px + step * 0.28f, py)
                            lineTo(px, py + step * 0.28f)
                            lineTo(px - step * 0.28f, py)
                            close()
                        }
                        drawPath(diamond, color = mandalaColor, style = stroke)
                    }
                }
            }
        }
        content()
    }
}

/**
 * Tone-on-tone elevated container using deliberate tonal surface shifts (cardSurface /
 * elevatedSurface / highlightSurface) with a crisp 1px (1.dp) gold hairline border
 * (rgba(212, 175, 55, 0.2)) and zero drop shadows.
 */
@Composable
fun IndoToneSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = LocalIndoPalette.current.cardSurface,
    hairlineColor: Color = LocalIndoPalette.current.goldHairline,
    borderWidth: Dp = 1.dp,
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = 0.dp,
        border = BorderStroke(borderWidth, hairlineColor),
        content = content
    )
}

@Composable
fun IndoToneSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = LocalIndoPalette.current.cardSurface,
    hairlineColor: Color = LocalIndoPalette.current.goldHairline,
    borderWidth: Dp = 1.dp,
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = 0.dp,
        border = BorderStroke(borderWidth, hairlineColor),
        content = content
    )
}

