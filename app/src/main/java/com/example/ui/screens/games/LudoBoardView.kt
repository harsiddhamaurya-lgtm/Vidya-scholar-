package com.example.ui.screens.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.ludo.*
import com.example.ui.theme.LocalIndoPalette
import com.example.utils.SoundHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class LudoHopAnimation(
    val tokenKey: String,
    val pathPositions: List<Int> // includes startPos at index 0 -> ... -> destPos
)

@Composable
fun LudoBoardView(
    state: LudoGameState,
    pulseScale: Float,
    onTokenClick: (LudoToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIndoPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val track = remember { getPachisiTrackCoordinates() }

    // Track previous positions to animate step-by-step tile hopping (120ms/square) & captures
    val prevPositions = remember { mutableStateMapOf<String, Int>() }
    var activeHop by remember { mutableStateOf<LudoHopAnimation?>(null) }
    var currentHopStepIdx by remember { mutableStateOf(0) }
    val hopFraction = remember { Animatable(1f) }

    var captureBurstKey by remember { mutableStateOf(0) }
    var captureBurstGrid by remember { mutableStateOf(Pair(7.5f, 7.5f)) }
    var captureBurstColor by remember { mutableStateOf(Color(0xFFD4AF37)) }

    LaunchedEffect(state.tokens) {
        if (state.tokens.all { it.isInYard } && prevPositions.isNotEmpty()) {
            prevPositions.clear()
            activeHop = null
            return@LaunchedEffect
        }

        if (prevPositions.isEmpty()) {
            state.tokens.forEach { prevPositions["${it.color.name}_${it.id}"] = it.position }
            return@LaunchedEffect
        }

        var foundAdvance: LudoHopAnimation? = null
        state.tokens.forEach { token ->
            val key = "${token.color.name}_${token.id}"
            val oldPos = prevPositions[key] ?: -1
            val newPos = token.position
            if (oldPos != newPos) {
                if (oldPos >= 0 && newPos == -1) {
                    // Captured back to yard -> emit particle burst at captured tile!
                    val (gx, gy) = getPachisiGridForPosition(token.color, token.id, oldPos, track)
                    captureBurstGrid = Pair(gx, gy)
                    captureBurstColor = getSharedLudoColor(token.color)
                    captureBurstKey++
                } else if (newPos >= 0) {
                    val steps = buildLudoStepPath(token.color, oldPos, newPos)
                    if (steps.size >= 2) {
                        foundAdvance = LudoHopAnimation(key, steps)
                    }
                }
                prevPositions[key] = newPos
            }
        }

        if (foundAdvance != null) {
            val anim = foundAdvance!!
            activeHop = anim
            for (step in 0 until anim.pathPositions.lastIndex) {
                currentHopStepIdx = step
                hopFraction.snapTo(0f)
                if (step > 0) SoundHelper.playArenaHopTick(context)
                hopFraction.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 120, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
                )
            }
            activeHop = null
        }
    }

    val frameOuterColor = if (palette.isDark) Color(0xFF14111D) else Color(0xFF362921)
    val trackSurface = when {
        palette.isEInk -> Color(0xFFF6F3EB)
        palette.isDark -> Color(0xFF232938) // Matte temple slate track (30% structure)
        else -> Color(0xFFF3EDE1)           // Warm sandstone track (30% structure)
    }
    val gridLineColor = if (palette.isDark) Color(0xFF475569).copy(alpha = 0.55f) else Color(0xFF78685A).copy(alpha = 0.38f)

    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        color = frameOuterColor,
        border = BorderStroke(1.2.dp, palette.accentGold.copy(alpha = 0.55f)),
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(7.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(0.8.dp, palette.accentGold.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                .background(trackSurface)
        ) {
            val boardWidthPx = constraints.maxWidth.toFloat()
            val boardHeightPx = constraints.maxHeight.toFloat()
            val cellW = boardWidthPx / 15f
            val cellH = boardHeightPx / 15f
            val cellDp = with(density) { cellW.toDp() }
            val liftPx = with(density) { (-4).dp.toPx() }

            // 15x15 Matte Pachisi Board Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cw = w / 15f
                val ch = h / 15f

                val redColor = getSharedLudoColor(LudoColor.RED)
                val greenColor = getSharedLudoColor(LudoColor.GREEN)
                val yellowColor = getSharedLudoColor(LudoColor.YELLOW)
                val blueColor = getSharedLudoColor(LudoColor.BLUE)

                // Matte quadrant structure colors (muted so tokens pop as 10% highlights)
                val redPad = if (palette.isDark) Color(0xFF5D2131) else Color(0xFFC23656)
                val greenPad = if (palette.isDark) Color(0xFF164A3B) else Color(0xFF1E7E60)
                val yellowPad = if (palette.isDark) Color(0xFF5D4216) else Color(0xFFC7861B)
                val bluePad = if (palette.isDark) Color(0xFF173D5C) else Color(0xFF1C6BA0)

                // 1. Matte track surface
                drawRect(trackSurface, topLeft = Offset.Zero, size = size)

                // 2. Muted Home Columns & Start Squares (toned down for crisp 60-30-10 balance)
                val homeAlpha = if (palette.isDark) 0.72f else 0.85f
                for (c in 1..5) drawRect(redColor.copy(alpha = homeAlpha), Offset(c * cw, 7 * ch), Size(cw, ch))
                drawRect(redColor.copy(alpha = homeAlpha), Offset(1 * cw, 6 * ch), Size(cw, ch))

                for (r in 1..5) drawRect(greenColor.copy(alpha = homeAlpha), Offset(7 * cw, r * ch), Size(cw, ch))
                drawRect(greenColor.copy(alpha = homeAlpha), Offset(8 * cw, 1 * ch), Size(cw, ch))

                for (c in 9..13) drawRect(yellowColor.copy(alpha = homeAlpha), Offset(c * cw, 7 * ch), Size(cw, ch))
                drawRect(yellowColor.copy(alpha = homeAlpha), Offset(13 * cw, 8 * ch), Size(cw, ch))

                for (r in 9..13) drawRect(blueColor.copy(alpha = homeAlpha), Offset(7 * cw, r * ch), Size(cw, ch))
                drawRect(blueColor.copy(alpha = homeAlpha), Offset(6 * cw, 13 * ch), Size(cw, ch))

                // 3. Safe Star / Lotus Tiles
                val safeStars = listOf(Pair(6, 2), Pair(12, 6), Pair(8, 12), Pair(2, 8))
                val starTileTint = if (palette.isDark) Color(0xFF2F3850) else Color(0xFFE5DFCE)
                for ((col, row) in safeStars) {
                    drawRect(starTileTint, Offset(col * cw, row * ch), Size(cw, ch))
                    drawFivePointStar(
                        center = Offset((col + 0.5f) * cw, (row + 0.5f) * ch),
                        outerRadius = cw * 0.34f,
                        innerRadius = cw * 0.15f,
                        color = palette.accentGold
                    )
                }

                // Start square crisp stars
                val startStars = listOf(Pair(1, 6), Pair(8, 1), Pair(13, 8), Pair(6, 13))
                for ((col, row) in startStars) {
                    drawFivePointStar(
                        center = Offset((col + 0.5f) * cw, (row + 0.5f) * ch),
                        outerRadius = cw * 0.31f,
                        innerRadius = cw * 0.14f,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                // 4. Track Grid Lines
                for (i in 0..15) {
                    drawLine(gridLineColor, Offset(i * cw, 0f), Offset(i * cw, h), strokeWidth = 0.9.dp.toPx())
                    drawLine(gridLineColor, Offset(0f, i * ch), Offset(w, i * ch), strokeWidth = 0.9.dp.toPx())
                }

                // 5. Four 6x6 Corner Yards (Matte Framed Pads + Gold Active Highlight)
                drawYardQuadrant(0f, 0f, cw, ch, redPad, redColor, trackSurface, palette.accentGold, state.currentTurn == LudoColor.RED)
                drawYardQuadrant(9 * cw, 0f, cw, ch, greenPad, greenColor, trackSurface, palette.accentGold, state.currentTurn == LudoColor.GREEN)
                drawYardQuadrant(9 * cw, 9 * ch, cw, ch, yellowPad, yellowColor, trackSurface, palette.accentGold, state.currentTurn == LudoColor.YELLOW)
                drawYardQuadrant(0f, 9 * ch, cw, ch, bluePad, blueColor, trackSurface, palette.accentGold, state.currentTurn == LudoColor.BLUE)

                // 6. Central 3x3 Home Sanctuary
                val cx = 7.5f * cw
                val cy = 7.5f * ch
                val leftX = 6f * cw
                val rightX = 9f * cw
                val topY = 6f * ch
                val bottomY = 9f * ch

                drawPath(Path().apply { moveTo(leftX, topY); lineTo(cx, cy); lineTo(leftX, bottomY); close() }, color = redPad)
                drawPath(Path().apply { moveTo(leftX, topY); lineTo(rightX, topY); lineTo(cx, cy); close() }, color = greenPad)
                drawPath(Path().apply { moveTo(rightX, topY); lineTo(rightX, bottomY); lineTo(cx, cy); close() }, color = yellowPad)
                drawPath(Path().apply { moveTo(leftX, bottomY); lineTo(rightX, bottomY); lineTo(cx, cy); close() }, color = bluePad)

                val trim = palette.accentGold.copy(alpha = 0.85f)
                drawLine(trim, Offset(leftX, topY), Offset(rightX, bottomY), strokeWidth = 1.6.dp.toPx())
                drawLine(trim, Offset(rightX, topY), Offset(leftX, bottomY), strokeWidth = 1.6.dp.toPx())
                drawRect(trim, Offset(leftX, topY), Size(3 * cw, 3 * ch), style = Stroke(1.8.dp.toPx()))
                drawCircle(palette.accentGold, radius = cw * 0.42f, center = Offset(cx, cy))
                drawCircle(frameOuterColor, radius = cw * 0.42f, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
            }

            // 7. GPU-Animated Interactive Token Layer (Step-by-Step Parabolic Hopping + Stacking + Lift)
            val groupedTokens = state.tokens.groupBy { token ->
                val key = "${token.color.name}_${token.id}"
                if (activeHop?.tokenKey == key) "HOPPING_$key"
                else if (token.isInYard) "YARD_${token.color.name}_${token.id}"
                else if (token.isHome) "HOME_${token.color.name}"
                else if (token.isInHomeStretch) "STRETCH_${token.color.name}_${token.position}"
                else "TRACK_${token.position}"
            }

            Box(modifier = Modifier.fillMaxSize()) {
                groupedTokens.forEach { (_, group) ->
                    val count = group.size
                    group.forEachIndexed { index, token ->
                        val key = "${token.color.name}_${token.id}"
                        val isHopping = activeHop?.tokenKey == key
                        val canMove = state.hasRolled &&
                            token.color == state.currentTurn &&
                            LudoEngine.canMoveToken(token, state.lastDiceValue)

                        val (gridX, gridY, hopScale, hopLift) = if (isHopping && activeHop != null) {
                            val steps = activeHop!!.pathPositions
                            val fromPos = steps[currentHopStepIdx.coerceIn(0, steps.lastIndex)]
                            val toPos = steps[(currentHopStepIdx + 1).coerceIn(0, steps.lastIndex)]
                            val (fx, fy) = getPachisiGridForPosition(token.color, token.id, fromPos, track)
                            val (tx, ty) = getPachisiGridForPosition(token.color, token.id, toPos, track)
                            val f = hopFraction.value
                            val arc = sin(PI * f).toFloat()
                            QuadFloat(
                                fx + (tx - fx) * f,
                                fy + (ty - fy) * f,
                                1f + 0.30f * arc,
                                -cellH * 0.42f * arc
                            )
                        } else {
                            val (gx, gy) = getPachisiTokenCenterGrid(token, track)
                            val sx = if (count > 1) ((index % 2) - 0.5f) * 0.28f else 0f
                            val sy = if (count > 2) ((index / 2) - 0.5f) * 0.28f else 0f
                            QuadFloat(gx + sx, gy + sy, 1f, 0f)
                        }

                        val tokenSizeDp = if (count > 1 && !isHopping) cellDp * 0.72f else cellDp * 0.86f
                        val tokenSizePx = with(density) { tokenSizeDp.toPx() }
                        val posX = cellW * gridX - tokenSizePx / 2f
                        val posY = cellH * gridY - tokenSizePx / 2f + hopLift + (if (canMove) liftPx else 0f)
                        val tokenColor = getSharedLudoColor(token.color)

                        Box(
                            modifier = Modifier
                                .size(tokenSizeDp)
                                .graphicsLayer {
                                    translationX = posX
                                    translationY = posY
                                    val finalScale = if (isHopping) hopScale else if (canMove) pulseScale else 1f
                                    scaleX = finalScale
                                    scaleY = finalScale
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(tokenColor.copy(alpha = 0.92f), tokenColor)
                                    )
                                )
                                .border(
                                    width = if (canMove || isHopping) 2.dp else 1.dp,
                                    color = if (canMove || isHopping) palette.accentGold else palette.goldHairline,
                                    shape = CircleShape
                                )
                                .clickable(enabled = canMove && activeHop == null) {
                                    onTokenClick(token)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(tokenSizeDp * 0.56f)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.26f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (token.isHome) "★" else "${token.id + 1}",
                                    color = Color.White,
                                    fontSize = if (count > 1) 8.sp else 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // 8. Capture / Elimination Golden-Cyan Particle Burst
            CaptureParticleBurst(
                burstKey = captureBurstKey,
                gridX = captureBurstGrid.first,
                gridY = captureBurstGrid.second,
                gridDimension = 15f,
                burstColor = captureBurstColor
            )
        }
    }
}

private data class QuadFloat(val x: Float, val y: Float, val s: Float, val lift: Float)

/**
 * Reconstructs exact step-by-step cell positions between fromPos and toPos for hopping animation.
 */
private fun buildLudoStepPath(color: LudoColor, fromPos: Int, toPos: Int): List<Int> {
    if (fromPos == -1) return listOf(-1, toPos)
    val path = mutableListOf(fromPos)
    var cur = LudoToken(0, color, fromPos)
    for (step in 1..6) {
        val next = LudoEngine.getDestinationPosition(cur, 1) ?: break
        path.add(next)
        if (next == toPos) break
        cur = cur.copy(position = next)
    }
    if (path.last() != toPos) {
        return listOf(fromPos, toPos)
    }
    return path
}

private fun getPachisiGridForPosition(
    color: LudoColor,
    tokenId: Int,
    position: Int,
    track: List<Pair<Float, Float>>
): Pair<Float, Float> {
    return getPachisiTokenCenterGrid(LudoToken(tokenId, color, position), track)
}

private fun DrawScope.drawYardQuadrant(
    originX: Float,
    originY: Float,
    cw: Float,
    ch: Float,
    padColor: Color,
    tokenAccent: Color,
    wellSurface: Color,
    goldTrim: Color,
    isActiveTurn: Boolean
) {
    drawRect(
        color = padColor,
        topLeft = Offset(originX, originY),
        size = Size(6 * cw, 6 * ch)
    )
    drawRect(
        color = if (isActiveTurn) goldTrim else Color(0xFF1F1638).copy(alpha = 0.65f),
        topLeft = Offset(originX, originY),
        size = Size(6 * cw, 6 * ch),
        style = Stroke(width = if (isActiveTurn) 2.6.dp.toPx() else 1.2.dp.toPx())
    )

    drawRoundRect(
        color = wellSurface,
        topLeft = Offset(originX + 0.85f * cw, originY + 0.85f * ch),
        size = Size(4.3f * cw, 4.3f * ch),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    )

    val wellOffsets = listOf(
        Offset(originX + 2.0f * cw, originY + 2.0f * ch),
        Offset(originX + 4.0f * cw, originY + 2.0f * ch),
        Offset(originX + 2.0f * cw, originY + 4.0f * ch),
        Offset(originX + 4.0f * cw, originY + 4.0f * ch)
    )
    for (center in wellOffsets) {
        drawCircle(tokenAccent.copy(alpha = 0.20f), radius = cw * 0.62f, center = center)
        drawCircle(tokenAccent.copy(alpha = 0.85f), radius = cw * 0.62f, center = center, style = Stroke(1.4.dp.toPx()))
    }
}

private fun DrawScope.drawFivePointStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    val angleStep = PI / 5.0
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + i * angleStep
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}

private fun getPachisiTrackCoordinates(): List<Pair<Float, Float>> {
    return listOf(
        Pair(1f, 6f), Pair(2f, 6f), Pair(3f, 6f), Pair(4f, 6f), Pair(5f, 6f),
        Pair(6f, 5f), Pair(6f, 4f), Pair(6f, 3f), Pair(6f, 2f), Pair(6f, 1f), Pair(6f, 0f),
        Pair(7f, 0f), Pair(8f, 0f),
        Pair(8f, 1f), Pair(8f, 2f), Pair(8f, 3f), Pair(8f, 4f), Pair(8f, 5f),
        Pair(9f, 6f), Pair(10f, 6f), Pair(11f, 6f), Pair(12f, 6f), Pair(13f, 6f), Pair(14f, 6f),
        Pair(14f, 7f), Pair(14f, 8f),
        Pair(13f, 8f), Pair(12f, 8f), Pair(11f, 8f), Pair(10f, 8f), Pair(9f, 8f),
        Pair(8f, 9f), Pair(8f, 10f), Pair(8f, 11f), Pair(8f, 12f), Pair(8f, 13f), Pair(8f, 14f),
        Pair(7f, 14f), Pair(6f, 14f),
        Pair(6f, 13f), Pair(6f, 12f), Pair(6f, 11f), Pair(6f, 10f), Pair(6f, 9f),
        Pair(5f, 8f), Pair(4f, 8f), Pair(3f, 8f), Pair(2f, 8f), Pair(1f, 8f), Pair(0f, 8f),
        Pair(0f, 7f), Pair(0f, 6f)
    )
}

private fun getPachisiTokenCenterGrid(token: LudoToken, track: List<Pair<Float, Float>>): Pair<Float, Float> {
    return when {
        token.isInYard -> {
            val colOffset = if (token.id % 2 == 0) 2.0f else 4.0f
            val rowOffset = if (token.id < 2) 2.0f else 4.0f
            when (token.color) {
                LudoColor.RED -> Pair(colOffset, rowOffset)
                LudoColor.GREEN -> Pair(9.0f + colOffset, rowOffset)
                LudoColor.YELLOW -> Pair(9.0f + colOffset, 9.0f + rowOffset)
                LudoColor.BLUE -> Pair(colOffset, 9.0f + rowOffset)
            }
        }
        token.isHome -> {
            when (token.color) {
                LudoColor.RED -> Pair(6.65f, 7.5f)
                LudoColor.GREEN -> Pair(7.5f, 6.65f)
                LudoColor.YELLOW -> Pair(8.35f, 7.5f)
                LudoColor.BLUE -> Pair(7.5f, 8.35f)
            }
        }
        token.isInHomeStretch -> {
            val step = (token.position - 52).coerceIn(0, 4)
            when (token.color) {
                LudoColor.RED -> Pair(1.5f + step, 7.5f)
                LudoColor.GREEN -> Pair(7.5f, 1.5f + step)
                LudoColor.YELLOW -> Pair(13.5f - step, 7.5f)
                LudoColor.BLUE -> Pair(7.5f, 13.5f - step)
            }
        }
        token.position in 0..51 -> {
            val cell = track[token.position]
            Pair(cell.first + 0.5f, cell.second + 0.5f)
        }
        else -> Pair(7.5f, 7.5f)
    }
}

fun getSharedLudoColor(color: LudoColor): Color {
    return when (color) {
        LudoColor.RED -> Color(0xFFE11D48)    // Crimson Vermilion
        LudoColor.GREEN -> Color(0xFF10B981)  // Emerald
        LudoColor.YELLOW -> Color(0xFFF59E0B) // Saffron Gold
        LudoColor.BLUE -> Color(0xFF00B4D8)   // Electric Cyan-Sapphire
    }
}
