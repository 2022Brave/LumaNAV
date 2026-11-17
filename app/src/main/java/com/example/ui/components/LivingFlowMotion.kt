package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import kotlinx.coroutines.launch

/**
 * ColorOS "Living Flow" (Laminar Flow & Photon Engine) Motion & Material Specs.
 * Safety-calibrated for automotive head units (250ms–400ms, zero overshoot on critical nav text/arrows).
 */
object LivingFlow {
    val LaminarEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    const val StandardDurationMs = 320
    const val CompactDurationMs = 260
    const val PanelDurationMs = 360

    val LaminarIntOffsetSpec = tween<IntOffset>(durationMillis = StandardDurationMs, easing = LaminarEasing)
    val LaminarSizeSpec = tween<IntSize>(durationMillis = StandardDurationMs, easing = LaminarEasing)
    val LaminarBoundsSpec = tween<Rect>(durationMillis = PanelDurationMs, easing = LaminarEasing)
    val NavSafetyFloatSpec = tween<Float>(durationMillis = CompactDurationMs, easing = LaminarEasing)

    // Tactile micro-elastic press physics (for buttons/controls only, never critical nav text)
    val ElasticButtonSpring = spring<Float>(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMedium
    )

    // Interruptible velocity-driven sheet spring without harsh snapping
    val DraggableSheetSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 380f
    )
}

/**
 * Photon Engine Luminous Touch Modifier:
 * - Captures exact press coordinates (x, y) non-destructively
 * - Emits a radial Electric Cyan / Glass White luminous bloom expanding from the touch point
 * - Applies elastic tactile press feedback (0.95f scale -> spring release)
 */
fun Modifier.photonLuminousTouch(
    enabled: Boolean = true,
    pressScale: Float = 0.95f,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val accent = glowColor ?: LocalAuvionyxPalette.current.accentPrimary
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var touchOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var isPointerDown by remember { mutableStateOf(false) }
    val photonProgress = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if ((isPressed || isPointerDown) && enabled && !reducedMotion) pressScale else 1f,
        animationSpec = if (reducedMotion) tween(0) else LivingFlow.ElasticButtonSpring,
        label = "photonElasticScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val anyPressed = event.changes.any { it.pressed }
                    val newDown = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                    isPointerDown = anyPressed
                    if (newDown != null) {
                        touchOrigin = newDown.position
                        if (!reducedMotion) {
                            scope.launch {
                                photonProgress.snapTo(0.05f)
                                photonProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 360, easing = LivingFlow.LaminarEasing)
                                )
                                photonProgress.snapTo(0f)
                            }
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            val p = photonProgress.value
            if (p > 0f && touchOrigin != Offset.Unspecified) {
                val radius = size.maxDimension * (0.28f + 0.92f * p)
                val alpha = (1f - p).coerceIn(0f, 1f) * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassWhite.copy(alpha = alpha * 0.85f),
                            accent.copy(alpha = alpha),
                            Color.Transparent
                        ),
                        center = touchOrigin,
                        radius = radius
                    ),
                    radius = radius,
                    center = touchOrigin
                )
            }
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

/**
 * Velocity-responsive Draggable Laminar Frosted Glass Sheet for Phone & Route drawers:
 * Responds immediately to touch & drag velocity with interruptible spring physics.
 */
@Composable
fun DraggableLaminarSheet(
    minHeight: Dp = 180.dp,
    maxHeight: Dp = 440.dp,
    initialFraction: Float = 0.42f,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val scope = rememberCoroutineScope()
    val expandFraction = remember { Animatable(initialFraction.coerceIn(0f, 1f)) }
    val dragRangePx = remember(minHeight, maxHeight) { (maxHeight - minHeight).value.coerceAtLeast(80f) * 2.5f }

    val targetHeight = minHeight + (maxHeight - minHeight) * expandFraction.value
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .height(targetHeight)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xBA0A1322), Color(0xD904080F))
                )
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassWhite.copy(alpha = 0.12f), IceBlue.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(size.width * 0.2f, 0f),
                        radius = size.maxDimension * 0.65f
                    ),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, GlassWhite.copy(alpha = 0.42f), accent.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    start = Offset(28.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 28.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(
                BorderStroke(
                    width = 0.9.dp,
                    brush = Brush.linearGradient(
                        listOf(GlassWhite.copy(alpha = 0.22f), ElectricCyan.copy(alpha = 0.35f), DeepBlue.copy(alpha = 0.25f))
                    )
                ),
                shape = shape
            )
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        val next = (expandFraction.value - delta / dragRangePx).coerceIn(0f, 1f)
                        expandFraction.snapTo(next)
                    }
                },
                onDragStopped = { velocity ->
                    val target = when {
                        velocity < -450f -> 1f
                        velocity > 450f -> 0f
                        expandFraction.value >= 0.52f -> 1f
                        else -> 0f
                    }
                    scope.launch {
                        if (reducedMotion) {
                            expandFraction.snapTo(target)
                        } else {
                            expandFraction.animateTo(
                                targetValue = target,
                                initialVelocity = (-velocity / dragRangePx),
                                animationSpec = LivingFlow.DraggableSheetSpring
                            )
                        }
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tactile Laminar Grabber Handle
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(GlassWhite.copy(alpha = 0.32f))
            )
            content()
        }
    }
}
