package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AuvionyxModule
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.TrafficSeverity
import com.example.ui.theme.DarkNeonFill
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.GlassSurfaceLow
import com.example.ui.theme.GlassSurfaceMedium
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.PulseClear
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.PulseSlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tactile Laminar Elastic + Photon Luminous Press interaction modifier.
 */
fun Modifier.auvionyxBounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = photonLuminousTouch(
    enabled = enabled,
    pressScale = 0.95f,
    onClick = onClick
)

/**
 * Authentic Liquid Glass Card:
 * - Frosted translucent surface on pure AMOLED black
 * - Diagonal white/ice specular reflection
 * - Crisp top-edge white hairline reflection
 * - Subtle Spotlight radial aura when highlighted
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    highlighted: Boolean = false,
    accentOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    LiquidGlassCockpitCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        highlighted = highlighted,
        bottomCyanBloom = highlighted,
        translucentFloating = true,
        accentOverride = accentOverride,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * High-Fidelity Liquid Glass Surface matching Image 06/07 & Active/Idle Cockpit blueprints:
 * - Frosted translucent obsidian backing (lets 3D map/backdrop shine through in floating mode)
 * - Diagonal top-left white/ice frosted reflection sheen
 * - Crisp top horizontal specular hairline + subtle bottom cyan illumination bloom
 */
@Composable
fun LiquidGlassCockpitCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    highlighted: Boolean = false,
    bottomCyanBloom: Boolean = true,
    translucentFloating: Boolean = false,
    accentOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAuvionyxPalette.current
    val accent = accentOverride ?: palette.accentPrimary
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            GlassWhite.copy(alpha = if (highlighted) 0.55f else 0.34f),
            accent.copy(alpha = if (highlighted) 0.72f else 0.36f),
            DeepBlue.copy(alpha = 0.28f),
            accent.copy(alpha = 0.42f)
        )
    )

    val topSurface = if (translucentFloating) Color(0x9E0A1320) else Color(0xCC0A121D)
    val midSurface = if (translucentFloating) Color(0x8804080F) else Color(0xD9050910)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topSurface, midSurface)
                )
            )
            .drawBehind {
                // Upper-left diagonal white-ice frosted glass reflection
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassWhite.copy(alpha = 0.14f),
                            IceBlue.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.14f, size.height * 0.06f),
                        radius = size.maxDimension * 0.65f
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
                // Crisp top edge white specular hairline
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            GlassWhite.copy(alpha = 0.45f),
                            IceBlue.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(cornerRadius.toPx() * 0.55f, 0.9.dp.toPx()),
                    end = Offset(size.width - cornerRadius.toPx() * 0.55f, 0.9.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                // Restrained bottom-center cyan illumination bloom
                if (bottomCyanBloom) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.26f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height),
                            radius = size.width * 0.42f
                        ),
                        radius = size.width * 0.42f,
                        center = Offset(size.width * 0.5f, size.height)
                    )
                }
            }
            .border(BorderStroke(0.9.dp, borderBrush), shape)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Animated Liquid Glass / Dark Neon Selection Lens:
 * - Selected state uses Deep Blue (#1267D5) depth + Electric Cyan (#00D9F5) illuminated rim + top white specular arc.
 * - Unselected state remains translucent frosted glass.
 */
@Composable
fun LiquidGlassLens(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalAuvionyxPalette.current
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val accent = palette.accentPrimary
    val shape = RoundedCornerShape(cornerRadius)

    val lensScale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.97f,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = LivingFlow.CompactDurationMs,
            easing = LivingFlow.LaminarEasing
        ),
        label = "lensScale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 0.34f else 0.07f,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = LivingFlow.CompactDurationMs,
            easing = LivingFlow.LaminarEasing
        ),
        label = "lensAlpha"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else GlassWhite.copy(alpha = 0.18f),
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = LivingFlow.CompactDurationMs,
            easing = LivingFlow.LaminarEasing
        ),
        label = "lensBorder"
    )

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = lensScale
                scaleY = lensScale
            }
            .drawBehind {
                if (selected) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.24f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.72f
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = if (selected) {
                        listOf(
                            GlassWhite.copy(alpha = 0.14f),
                            accent.copy(alpha = bgAlpha),
                            DeepBlue.copy(alpha = 0.52f)
                        )
                    } else {
                        listOf(GlassSurfaceLow, DarkNeonFill)
                    }
                )
            )
            .border(
                width = if (selected) 1.3.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (selected) {
                        listOf(GlassWhite.copy(alpha = 0.65f), borderColor, IceBlue.copy(alpha = 0.7f))
                    } else {
                        listOf(GlassWhite.copy(alpha = 0.20f), GlassWhite.copy(alpha = 0.06f))
                    }
                ),
                shape = shape
            )
            .auvionyxBounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Illuminated Liquid Glass Floating Action Button.
 */
@Composable
fun LiquidFloatingButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    isPrimary: Boolean = true,
    testTag: String? = null
) {
    val palette = LocalAuvionyxPalette.current
    val accent = palette.accentPrimary

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .size(size)
            .minimumInteractiveComponentSize()
            .drawBehind {
                if (isPrimary) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.42f), Color.Transparent),
                            radius = this.size.maxDimension * 0.80f
                        )
                    )
                }
            }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = if (isPrimary) {
                        listOf(
                            GlassWhite.copy(alpha = 0.22f),
                            accent.copy(alpha = 0.38f),
                            DeepBlue.copy(alpha = 0.72f)
                        )
                    } else {
                        listOf(GlassSurfaceHigh, DarkNeonFill)
                    }
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(GlassWhite.copy(alpha = 0.65f), accent, DeepBlue)
                ),
                shape = CircleShape
            )
            .auvionyxBounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) GlassWhite else accent,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Liquid Glass Toggle matching the Design System sheet.
 */
@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val palette = LocalAuvionyxPalette.current
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val accent = palette.accentPrimary

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 22f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toggleThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) accent.copy(alpha = 0.85f) else GlassSurfaceMedium,
        label = "toggleTrack"
    )

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .minimumInteractiveComponentSize()
            .width(54.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(
                1.dp,
                if (checked) GlassWhite.copy(alpha = 0.55f) else GlassBorderSubtle,
                CircleShape
            )
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(GlassWhite)
        )
    }
}

/**
 * Minimalist Halo-style runtime service indicator: clean 7.dp status dot + concise label,
 * keeping Live, Demo, Offline, Permission Denied, and API Unavailable states honest without visual noise.
 */
@Composable
fun ServiceStateBadge(
    state: ServiceAvailabilityState,
    modifier: Modifier = Modifier,
    labelPrefix: String? = null
) {
    val dotColor = when (state) {
        ServiceAvailabilityState.LIVE -> PulseClear
        ServiceAvailabilityState.DEMO -> IceBlue
        ServiceAvailabilityState.OFFLINE -> PulseSlow
        ServiceAvailabilityState.PERMISSION_DENIED,
        ServiceAvailabilityState.API_UNAVAILABLE -> PulseCongestion
    }
    val text = if (labelPrefix != null) "$labelPrefix · ${state.label}" else state.label

    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

/**
 * Expandable Glass Panel with spring reveal.
 */
@Composable
fun ExpandableGlassPanel(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAuvionyxPalette.current
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        highlighted = expanded,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = GlassWhite)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Text(
                text = if (expanded) "Hide" else "Configure",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accentPrimary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp), content = content)
        }
    }
}

/**
 * GPU-conscious Neon Wave Visualizer:
 * Only runs infinite transition when `isActive && !reducedMotion` so Blaupunkt 4GB RAM stays cool & snappy.
 */
@Composable
fun NeonWaveVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    accentOverride: Color? = null
) {
    val palette = LocalAuvionyxPalette.current
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val primary = accentOverride ?: palette.accentPrimary
    val secondary = palette.accentSecondary

    val phase = if (isActive && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "neonWave")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase"
        )
        animatedPhase
    } else {
        0.65f
    }

    val amplitudeScale by animateFloatAsState(
        targetValue = if (isActive && !reducedMotion) 1f else if (isActive) 0.45f else 0.18f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "waveAmp"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h * 0.5f
        val maxAmp = h * 0.36f * amplitudeScale

        fun buildWavePath(phaseOffset: Float, freq: Float, ampFactor: Float): Path {
            val path = Path()
            val steps = 36
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val x = t * w
                val envelope = sin(t * PI).toFloat()
                val y = midY + sin(t * freq * PI + phase + phaseOffset).toFloat() * maxAmp * ampFactor * envelope
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        val wave1 = buildWavePath(0f, 3.2f, 1.0f)
        val wave2 = buildWavePath(1.4f, 4.4f, 0.70f)

        drawPath(
            path = wave2,
            color = secondary.copy(alpha = 0.55f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = wave1,
            brush = Brush.horizontalGradient(listOf(DeepBlue, primary, IceBlue)),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Conventional Traffic Pulse status pill indicator (Green / Amber / Red).
 */
@Composable
fun TrafficPulseBadge(
    severity: TrafficSeverity,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (severity) {
        TrafficSeverity.HIGH -> PulseCongestion to "High Congestion"
        TrafficSeverity.SLOW -> PulseSlow to "Slow Moving"
        TrafficSeverity.CLEAR -> PulseClear to "Clear"
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * Authoritative Top Branding & Status Bar matching Active & Idle Cockpit blueprints:
 * - Left: Velocity A + AUVIONYX wordmark
 * - Bottom center: Subtle Electric Cyan horizon shimmer hairline
 * - Right: Stacked Time/Date, Divider, Weather Icon + Temp + Locality, Signal, Wi-Fi, Bluetooth, Settings gear
 */
@Composable
fun GlassTopStatusBar(
    currentLocation: String,
    weatherTempC: Int?,
    isLiveGps: Boolean,
    gpsAccuracyMeters: Float?,
    onOpenAod: () -> Unit,
    onOpenSettings: () -> Unit,
    isIdleCockpit: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalAuvionyxPalette.current
    val accent = palette.accentPrimary
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(15_000L)
        }
    }
    val timeText = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val dateText = remember(now) { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now) }
    val localityDisplay = when {
        isLiveGps && !currentLocation.contains("Acquiring", ignoreCase = true) ->
            currentLocation.substringBefore(",").take(18)
        else -> "Noida (Demo)"
    }
    val tempDisplay = weatherTempC ?: 24

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Restrained horizontal cyan shimmer line under header
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            accent.copy(alpha = if (isIdleCockpit) 0.20f else 0.55f),
                            GlassWhite.copy(alpha = if (isIdleCockpit) 0.15f else 0.45f),
                            accent.copy(alpha = if (isIdleCockpit) 0.20f else 0.55f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Official Velocity A + AUVIONYX wordmark (tap for AOD)
            VelocityACompactWordmark(
                modifier = Modifier
                    .testTag("top_aod_button")
                    .clickable(onClick = onOpenAod)
            )

            // Right: Time/Date | Weather | Connectivity | Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleLarge,
                        color = GlassWhite
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(GlassWhite.copy(alpha = 0.22f))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Weather",
                        tint = IceBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "${tempDisplay}°C",
                            style = MaterialTheme.typography.titleMedium,
                            color = GlassWhite
                        )
                        Text(
                            text = localityDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SignalCellularAlt,
                        contentDescription = if (isLiveGps && gpsAccuracyMeters != null) "Cellular & GPS ±${gpsAccuracyMeters.toInt()}m" else "Signal",
                        tint = GlassWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = GlassWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = GlassWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!isIdleCockpit) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = GlassWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slim Left Navigation Rail matching Image 3 blueprint:
 * Home | Maps | Music | Vehicle | Apps | Settings inside a frosted vertical rounded capsule.
 */
@Composable
fun GlassNavigationRail(
    activeModule: AuvionyxModule,
    onSelectModule: (AuvionyxModule) -> Unit,
    modifier: Modifier = Modifier
) {
    val railItems = listOf(
        Triple(AuvionyxModule.DRIVE, Icons.Outlined.Home, "Home"),
        Triple(AuvionyxModule.MAPS, Icons.Outlined.Navigation, "Maps"),
        Triple(AuvionyxModule.MUSIC, Icons.Outlined.MusicNote, "Music"),
        Triple(AuvionyxModule.VEHICLE, Icons.Outlined.DirectionsCar, "Vehicle"),
        Triple(AuvionyxModule.APPS, Icons.Outlined.Apps, "Apps"),
        Triple(AuvionyxModule.SETTINGS, Icons.Outlined.Settings, "Settings")
    )

    LiquidGlassCockpitCard(
        modifier = modifier.fillMaxHeight(),
        cornerRadius = 30.dp,
        bottomCyanBloom = false,
        translucentFloating = true,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            railItems.forEach { (module, icon, label) ->
                val selected = activeModule == module
                LiquidGlassLens(
                    selected = selected,
                    onClick = { onSelectModule(module) },
                    cornerRadius = 18.dp,
                    modifier = Modifier.size(48.dp),
                    testTag = "rail_${module.route}"
                ) {
                    Icon(
                        imageVector = if (module == AuvionyxModule.DRIVE && selected) Icons.Filled.Home else icon,
                        contentDescription = label,
                        tint = if (selected) GlassWhite else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-Width Floating Bottom Navigation Dock matching Image 3 Active Cockpit blueprint:
 * [ Phone ] [ Maps ] [ Music ] [ HUD ] [ Apps ]
 */
@Composable
fun GlassBottomDock(
    activeModule: AuvionyxModule,
    onSelectModule: (AuvionyxModule) -> Unit,
    onOpenPhoneDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAuvionyxPalette.current
    val accent = palette.accentPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xB80A1422), Color(0xD404080F))
                    )
                )
                .drawBehind {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                GlassWhite.copy(alpha = 0.42f),
                                accent.copy(alpha = 0.35f),
                                GlassWhite.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(40.dp.toPx(), 1.dp.toPx()),
                        end = Offset(size.width - 40.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .border(
                    width = 0.9.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            GlassWhite.copy(alpha = 0.22f),
                            accent.copy(alpha = 0.48f),
                            GlassWhite.copy(alpha = 0.22f)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockShortcutItem(
                icon = Icons.Outlined.Phone,
                label = "Phone",
                selected = false,
                testTag = "dock_phone",
                onClick = onOpenPhoneDialer,
                modifier = Modifier.weight(1f)
            )
            DockShortcutItem(
                icon = Icons.Outlined.Navigation,
                label = "Maps",
                selected = activeModule == AuvionyxModule.MAPS || activeModule == AuvionyxModule.DRIVE,
                testTag = "dock_maps",
                onClick = { onSelectModule(AuvionyxModule.MAPS) },
                modifier = Modifier.weight(1f)
            )
            DockShortcutItem(
                icon = Icons.Outlined.MusicNote,
                label = "Music",
                selected = activeModule == AuvionyxModule.MUSIC,
                testTag = "dock_music",
                onClick = { onSelectModule(AuvionyxModule.MUSIC) },
                modifier = Modifier.weight(1f)
            )

            DockShortcutItem(
                icon = Icons.Outlined.Speed,
                label = "HUD",
                selected = activeModule == AuvionyxModule.HUD,
                testTag = "dock_hud",
                onClick = { onSelectModule(AuvionyxModule.HUD) },
                modifier = Modifier.weight(1f)
            )
            DockShortcutItem(
                icon = Icons.Outlined.Apps,
                label = "Apps",
                selected = activeModule == AuvionyxModule.APPS,
                testTag = "dock_apps",
                onClick = { onSelectModule(AuvionyxModule.APPS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DockShortcutItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    Box(
        modifier = modifier
            .then(Modifier.testTag(testTag))
            .height(52.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.24f), DeepBlue.copy(alpha = 0.38f))
                            )
                        )
                        .border(
                            0.9.dp,
                            Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.55f), accent)),
                            RoundedCornerShape(22.dp)
                        )
                } else {
                    Modifier
                }
            )
            .auvionyxBounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else GlassWhite.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) GlassWhite else TextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Reusable Liquid Glass Media Control Cluster.
 */
@Composable
fun MediaControlCluster(
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isFavorite: Boolean,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeatOrFav: () -> Unit,
    showRepeatInsteadOfFav: Boolean = false,
    accentOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAuvionyxPalette.current
    val accent = accentOverride ?: palette.accentPrimary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier
                .clip(CircleShape)
                .photonLuminousTouch()
                .testTag("media_shuffle")
        ) {
            Icon(
                imageVector = Icons.Outlined.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffle) accent else IceBlue.copy(alpha = 0.7f)
            )
        }
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .clip(CircleShape)
                .photonLuminousTouch()
                .testTag("media_prev")
        ) {
            Icon(
                imageVector = Icons.Outlined.SkipPrevious,
                contentDescription = "Previous track",
                tint = GlassWhite,
                modifier = Modifier.size(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .testTag("media_play_pause")
                .size(56.dp)
                .minimumInteractiveComponentSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                            radius = size.maxDimension * 0.85f
                        )
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            GlassWhite.copy(alpha = 0.22f),
                            accent.copy(alpha = 0.42f),
                            DeepBlue.copy(alpha = 0.72f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.7f), accent)),
                    shape = CircleShape
                )
                .auvionyxBounceClick(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = GlassWhite,
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .clip(CircleShape)
                .photonLuminousTouch()
                .testTag("media_next")
        ) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = "Next track",
                tint = GlassWhite,
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(
            onClick = onToggleRepeatOrFav,
            modifier = Modifier
                .clip(CircleShape)
                .photonLuminousTouch()
                .testTag("media_fav_repeat")
        ) {
            if (showRepeatInsteadOfFav) {
                Icon(
                    imageVector = Icons.Outlined.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeat) accent else IceBlue.copy(alpha = 0.7f)
                )
            } else {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite track",
                    tint = if (isFavorite) accent else IceBlue.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
