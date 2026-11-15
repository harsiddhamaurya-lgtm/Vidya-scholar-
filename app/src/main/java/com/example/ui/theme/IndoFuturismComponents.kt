package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val topCenter = Offset(size.width * 0.85f, size.height * 0.12f)
                        val topRadius = size.width * 0.72f
                        val topBrush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = if (palette.isDark) 0.11f else 0.08f),
                                Color.Transparent
                            ),
                            center = topCenter,
                            radius = topRadius.coerceAtLeast(1f)
                        )

                        val botCenter = Offset(size.width * 0.12f, size.height * 0.88f)
                        val botRadius = size.width * 0.58f
                        val botBrush = Brush.radialGradient(
                            colors = listOf(
                                palette.accentGold.copy(alpha = if (palette.isDark) 0.07f else 0.055f),
                                Color.Transparent
                            ),
                            center = botCenter,
                            radius = botRadius.coerceAtLeast(1f)
                        )

                        val mandalaColor = palette.watermarkTint
                        val stroke = Stroke(width = 1.dp.toPx())
                        val spokeStroke = 0.8.dp.toPx()
                        val cx = size.width * 0.88f
                        val cy = size.height * 0.14f
                        val ring1 = 44.dp.toPx()
                        val ring2 = 88.dp.toPx()
                        val ring3 = 132.dp.toPx()

                        val combinedSpokesAndLattice = Path().apply {
                            for (i in 0 until 8) {
                                val angle = i * PI / 4.0
                                moveTo(cx, cy)
                                lineTo(
                                    cx + (ring3 * cos(angle)).toFloat(),
                                    cy + (ring3 * sin(angle)).toFloat()
                                )
                            }
                            val step = 34.dp.toPx()
                            val halfDiamond = step * 0.28f
                            val startX = -step
                            val startY = size.height * 0.76f
                            for (r in 0..3) {
                                for (c in 0..3) {
                                    val px = startX + c * step + (if (r % 2 == 1) step * 0.5f else 0f)
                                    val py = startY + r * (step * 0.65f)
                                    moveTo(px, py - halfDiamond)
                                    lineTo(px + halfDiamond, py)
                                    lineTo(px, py + halfDiamond)
                                    lineTo(px - halfDiamond, py)
                                    close()
                                }
                            }
                        }

                        onDrawBehind {
                            drawCircle(brush = topBrush, radius = topRadius, center = topCenter)
                            drawCircle(brush = botBrush, radius = botRadius, center = botCenter)
                            drawCircle(color = mandalaColor, radius = ring1, center = Offset(cx, cy), style = stroke)
                            drawCircle(color = mandalaColor, radius = ring2, center = Offset(cx, cy), style = stroke)
                            drawCircle(color = mandalaColor, radius = ring3, center = Offset(cx, cy), style = stroke)
                            drawPath(combinedSpokesAndLattice, color = mandalaColor, style = Stroke(width = spokeStroke))
                        }
                    }
            )
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

