package com.example.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Motion Design System adhering to snappy 150ms–300ms organic deceleration
 * (`cubic-bezier(0.16, 1, 0.3, 1)`) and GPU-only transform/opacity animations.
 */
object IndoMotion {
    val OrganicDeceleration = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    const val MICRO_DURATION_MS = 180
    const val UNFOLD_DURATION_MS = 250

    fun mandalaEnterTransition(): EnterTransition =
        fadeIn(tween(UNFOLD_DURATION_MS, easing = OrganicDeceleration)) +
            scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(UNFOLD_DURATION_MS, easing = OrganicDeceleration)
            )

    fun mandalaExitTransition(): ExitTransition =
        fadeOut(tween(MICRO_DURATION_MS, easing = OrganicDeceleration)) +
            scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(MICRO_DURATION_MS, easing = OrganicDeceleration)
            )
}

/**
 * Mandala-inspired geometric unfolding container using strictly GPU scale + alpha transforms.
 */
@Composable
fun MandalaUnfoldContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = IndoMotion.mandalaEnterTransition(),
        exit = IndoMotion.mandalaExitTransition()
    ) {
        content()
    }
}

/**
 * Snappy GPU-accelerated micro-interaction feedback (transform scale + warm light-sweep sheen)
 * tuned at 180ms with organic deceleration for cards and interactive tiles.
 */
fun Modifier.indoTactileMotion(
    interactionSource: MutableInteractionSource,
    sweepTint: Color = Color(0xFFD4AF37)
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.975f
            isHovered -> 1.012f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = IndoMotion.MICRO_DURATION_MS,
            easing = IndoMotion.OrganicDeceleration
        ),
        label = "indo_scale"
    )

    val sheenProgress by animateFloatAsState(
        targetValue = if (isHovered || isPressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 260,
            easing = IndoMotion.OrganicDeceleration
        ),
        label = "indo_sheen"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (sheenProgress > 0.01f) {
                val sweepX = size.width * (-0.3f + 1.6f * sheenProgress)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            sweepTint.copy(alpha = 0.14f * (1f - kotlin.math.abs(sheenProgress - 0.5f))),
                            Color.Transparent
                        ),
                        start = Offset(sweepX - size.width * 0.35f, 0f),
                        end = Offset(sweepX + size.width * 0.35f, size.height)
                    )
                )
            }
        }
}
