package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaAppSource
import com.example.data.model.MediaTrack
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PulseClear
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Active Cockpit Top-Right Speedometer Card:
 * Matches reference Active Cockpit blueprint:
 * - Left: Segmented tick horseshoe gauge (135° -> 405°), Electric Cyan sweep, bold speed & unit
 * - Right: Red-ring Speed Limit sign, Heading chevron + cardinal, GPS status dot
 */
@Composable
fun ActiveSpeedometerCard(
    speedValue: Int,
    maxSpeed: Int,
    unitLabel: String,
    speedLimitValue: Int?,
    headingCardinal: String,
    isLiveGps: Boolean,
    isDemoPreview: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary

    LiquidGlassCockpitCard(
        modifier = modifier
            .testTag("active_speedometer_card")
            .clip(RoundedCornerShape(24.dp))
            .photonLuminousTouch(pressScale = 0.98f, onClick = onClick),
        cornerRadius = 24.dp,
        bottomCyanBloom = true,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Dial Arc + Numeric Readout
            BoxWithConstraints(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val dialDiameter = minOf(maxWidth, maxHeight).coerceAtMost(192.dp)
                Box(
                    modifier = Modifier.size(dialDiameter),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 9.dp.toPx()
                        val tickRingInset = stroke + 6.dp.toPx()
                        val arcSize = Size(size.width - tickRingInset * 2, size.height - tickRingInset * 2)
                        val startAngle = 135f
                        val sweepTotal = 270f
                        val fraction = (speedValue.toFloat() / maxSpeed.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerR = size.minDimension / 2f - 3.dp.toPx()
                        val innerTickR = outerR - 9.dp.toPx()

                        // Background dark track
                        drawArc(
                            color = GlassWhite.copy(alpha = 0.09f),
                            startAngle = startAngle,
                            sweepAngle = sweepTotal,
                            useCenter = false,
                            topLeft = Offset(tickRingInset, tickRingInset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )

                        // Radial peripheral ticks matching Active Cockpit gauge
                        val totalTicks = 28
                        val activeTickCutoff = (fraction * totalTicks).toInt()
                        for (i in 0..totalTicks) {
                            val angleDeg = startAngle + (sweepTotal * i / totalTicks)
                            val rad = angleDeg * (PI / 180.0)
                            val cosA = cos(rad).toFloat()
                            val sinA = sin(rad).toFloat()
                            val isLit = i <= activeTickCutoff && speedValue > 0
                            drawLine(
                                color = if (isLit) ElectricCyan.copy(alpha = 0.85f) else GlassWhite.copy(alpha = 0.22f),
                                start = Offset(cx + cosA * innerTickR, cy + sinA * innerTickR),
                                end = Offset(cx + cosA * outerR, cy + sinA * outerR),
                                strokeWidth = if (i % 4 == 0) 1.8.dp.toPx() else 1.1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Swept cyan/ice illuminated arc
                        if (fraction > 0f) {
                            drawArc(
                                color = accent.copy(alpha = 0.24f),
                                startAngle = startAngle,
                                sweepAngle = sweepTotal * fraction,
                                useCenter = false,
                                topLeft = Offset(tickRingInset, tickRingInset),
                                size = arcSize,
                                style = Stroke(width = stroke + 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(DeepBlue, accent, IceBlue, GlassWhite)
                                ),
                                startAngle = startAngle,
                                sweepAngle = sweepTotal * fraction,
                                useCenter = false,
                                topLeft = Offset(tickRingInset, tickRingInset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )

                            val tipRad = (startAngle + sweepTotal * fraction) * (PI / 180.0)
                            val arcRadius = arcSize.width / 2f
                            val tipCenter = Offset(
                                cx + cos(tipRad).toFloat() * arcRadius,
                                cy + sin(tipRad).toFloat() * arcRadius
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(GlassWhite, accent.copy(alpha = 0.5f), Color.Transparent),
                                    center = tipCenter,
                                    radius = 12.dp.toPx()
                                ),
                                radius = 12.dp.toPx(),
                                center = tipCenter
                            )
                            drawCircle(color = GlassWhite, radius = 3.2.dp.toPx(), center = tipCenter)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = "$speedValue",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (dialDiameter < 150.dp) 38.sp else 48.sp,
                            lineHeight = 50.sp,
                            color = GlassWhite
                        )
                        Text(
                            text = unitLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Right Column: Speed Limit Circle + Heading + GPS Status
            Column(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Conventional Speed Limit Sign
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (speedLimitValue != null) GlassWhite else Color(0xFF0A1019))
                        .border(
                            width = 4.dp,
                            color = if (speedLimitValue != null) PulseCongestion else GlassWhite.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = speedLimitValue?.toString() ?: "--",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (speedLimitValue != null) ObsidianBlack else TextMuted
                    )
                }

                // Heading Arrow + Cardinal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        val arrow = Path().apply {
                            moveTo(size.width * 0.5f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(size.width * 0.5f, size.height * 0.72f)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(arrow, color = GlassWhite)
                    }
                    Text(
                        text = headingCardinal,
                        style = MaterialTheme.typography.labelLarge,
                        color = GlassWhite
                    )
                }

                // GPS Status Indicator (honestly distinguishes Live GPS vs Demo Placeholder)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLiveGps -> PulseClear
                                    isDemoPreview -> IceBlue
                                    else -> TextMuted
                                }
                            )
                    )
                    Text(
                        text = if (isDemoPreview && !isLiveGps) "DEMO" else "GPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassWhite
                    )
                }
            }
        }
    }
}

/**
 * Immersive Idle Cockpit Prominent Left Floating Speedometer Ring:
 * Matches reference Idle Cockpit blueprint (floating large glossy frosted ring on left side of map):
 * - Thick frosted translucent circular bezel with top-left bright white specular glare
 * - Glowing segmented cyan radial speed arc
 * - Large bold italic-styled speed numeral + km/h
 */
@Composable
fun IdleProminentSpeedometer(
    speedValue: Int,
    maxSpeed: Int,
    unitLabel: String,
    isDemoPreview: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary

    Box(
        modifier = modifier
            .testTag("idle_prominent_speedometer")
            .size(252.dp)
            .drawBehind {
                // Soft ambient outer cyan rim bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.35f, size.height * 0.25f),
                        radius = size.maxDimension * 0.72f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val outerR = minOf(w, h) / 2f - 4.dp.toPx()

            // Frosted translucent obsidian disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x9E040A12),
                        Color(0xC202060D),
                        Color(0xD9050F1C)
                    ),
                    center = Offset(cx, cy),
                    radius = outerR
                ),
                radius = outerR,
                center = Offset(cx, cy)
            )

            // Subtle inner dark glass bezel shadow
            drawCircle(
                color = GlassWhite.copy(alpha = 0.14f),
                radius = outerR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Segmented cyan speed arc (matches left illuminated crescent of Idle blueprint)
            val strokeW = 14.dp.toPx()
            val arcInset = 18.dp.toPx()
            val arcSize = Size(w - arcInset * 2, h - arcInset * 2)
            val startAngle = 142f
            val sweepTotal = 256f
            val fraction = (speedValue.toFloat() / maxSpeed.coerceAtLeast(1)).coerceIn(0f, 1f)

            // Faint recessed track
            drawArc(
                color = GlassWhite.copy(alpha = 0.08f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(arcInset, arcInset),
                size = arcSize,
                style = Stroke(width = strokeW)
            )

            // Illuminated cyan progression arc
            if (fraction > 0f) {
                drawArc(
                    color = accent.copy(alpha = 0.24f),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * fraction,
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = arcSize,
                    style = Stroke(width = strokeW + 8.dp.toPx())
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(DeepBlue, accent, GlassWhite, IceBlue)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * fraction,
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = arcSize,
                    style = Stroke(width = strokeW)
                )
            }

            // Segmented dividers cleanly cutting across the arc
            val segments = 16
            val arcR = arcSize.width / 2f
            for (i in 0..segments) {
                val deg = startAngle + (sweepTotal * i / segments)
                val rad = deg * (PI / 180.0)
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                drawLine(
                    color = Color(0xCC02060C),
                    start = Offset(cx + cosA * (arcR - strokeW * 0.65f), cy + sinA * (arcR - strokeW * 0.65f)),
                    end = Offset(cx + cosA * (arcR + strokeW * 0.65f), cy + sinA * (arcR + strokeW * 0.65f)),
                    strokeWidth = 1.6.dp.toPx()
                )
            }

            // Top-left bright white-cyan glass specular rim arc
            drawArc(
                brush = Brush.linearGradient(
                    listOf(GlassWhite.copy(alpha = 0.85f), accent.copy(alpha = 0.65f), Color.Transparent)
                ),
                startAngle = 175f,
                sweepAngle = 115f,
                useCenter = false,
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$speedValue",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 72.sp,
                lineHeight = 72.sp,
                color = GlassWhite
            )
            Text(
                text = unitLabel,
                fontFamily = SpaceGroteskFontFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = TextSecondary
            )
            if (isDemoPreview) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "DEMO",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceBlue.copy(alpha = 0.72f)
                )
            }
        }
    }
}

/**
 * Reusable Liquid Glass Media Player Card:
 * - Active Cockpit mode (`compactMode = false`): Art + Title/Artist + Source Icon + Cyan Slider + 5-button row (Shuffle, Prev, Glowing Play/Pause, Next, Favorite)
 * - Idle Cockpit mode (`compactMode = true`): Floating frosted card + Art + Title/Artist + Cyan Slider + Compact 3-button row (Prev, Glowing Play/Pause, Next)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CockpitMediaPlayerCard(
    track: MediaTrack,
    source: MediaAppSource,
    isPlaying: Boolean,
    isShuffle: Boolean,
    isFavorite: Boolean,
    positionSec: Int,
    compactMode: Boolean,
    externalArtwork: Bitmap? = null,
    onOpenMusic: () -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary

    Box(modifier = modifier) {
        if (!compactMode) {
            if (externalArtwork != null) {
                Image(
                    bitmap = externalArtwork.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(30.dp)
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(track.adaptiveColor.copy(alpha = 0.34f), Color.Black))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xA8000000), Color(0xE8000000)))))
        }
    LiquidGlassCockpitCard(
        modifier = Modifier.fillMaxSize()
            .testTag(if (compactMode) "idle_media_card" else "active_media_card")
            .clip(RoundedCornerShape(24.dp))
            .photonLuminousTouch(pressScale = 0.98f, onClick = onOpenMusic),
        cornerRadius = 24.dp,
        bottomCyanBloom = true,
        translucentFloating = compactMode,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Album Art + Track Info + Source Badge (active mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (externalArtwork != null) {
                    Image(
                        bitmap = externalArtwork.asImageBitmap(),
                        contentDescription = "Current track album artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (compactMode) 58.dp else 66.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else if (isPlaying) {
                    Box(
                        modifier = Modifier.size(if (compactMode) 58.dp else 66.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.radialGradient(listOf(track.adaptiveColor.copy(alpha = 0.48f), Color.Black))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = track.adaptiveColor, modifier = Modifier.size(28.dp))
                    }
                } else {
                    AlbumArtworkArt(
                        artVariant = track.artVariant,
                        accentColor = track.adaptiveColor,
                        cornerRadius = 14.dp,
                        modifier = Modifier.size(if (compactMode) 58.dp else 66.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = GlassWhite,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!compactMode) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f))
                            .border(0.8.dp, accent.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (source) {
                                MediaAppSource.SPOTIFY -> Icons.Outlined.GraphicEq
                                else -> Icons.Outlined.MusicNote
                            },
                            contentDescription = source.label,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Middle: Sleek Cyan Progress Scrubber + Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!compactMode) {
                    CockpitWavyProgressBar(positionSec, track.durationSec, isPlaying, track.adaptiveColor, onSeek)
                } else Slider(
                    value = positionSec.coerceIn(0, track.durationSec.coerceAtLeast(1)).toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..track.durationSec.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = GlassWhite.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(positionSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formatDuration(track.durationSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Bottom Row: Transport Buttons
            if (compactMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevTrack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .photonLuminousTouch(pressScale = 0.90f)
                            .testTag("idle_media_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = GlassWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    GlowPlayPauseOrb(
                        isPlaying = isPlaying,
                        accent = accent,
                        onClick = onPlayPause,
                        testTag = "idle_media_play_pause",
                        size = 54.dp
                    )
                    IconButton(
                        onClick = onNextTrack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .photonLuminousTouch(pressScale = 0.90f)
                            .testTag("idle_media_next")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = "Next track",
                            tint = GlassWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .clip(CircleShape)
                            .photonLuminousTouch(pressScale = 0.90f)
                            .testTag("active_media_shuffle")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) accent else GlassWhite.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onPrevTrack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .photonLuminousTouch(pressScale = 0.90f)
                            .testTag("active_media_prev")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = GlassWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    GlowPlayPauseOrb(
                        isPlaying = isPlaying,
                        accent = accent,
                        onClick = onPlayPause,
                        testTag = "active_media_play_pause",
                        size = 54.dp
                    )
                    IconButton(
                        onClick = onNextTrack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .photonLuminousTouch(pressScale = 0.90f)
                            .testTag("active_media_next")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = "Next track",
                            tint = GlassWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .clip(CircleShape)
                            .photonLuminousTouch(pressScale = 0.90f)
                            .testTag("active_media_fav")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) accent else GlassWhite.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CockpitWavyProgressBar(
    positionSec: Int,
    durationSec: Int,
    isPlaying: Boolean,
    accent: Color,
    onSeek: (Int) -> Unit
) {
    val amplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(480, easing = LivingFlow.LaminarEasing),
        label = "cockpitWaveAmplitude"
    )
    val transition = rememberInfiniteTransition(label = "cockpitWavePhase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "cockpitWavePhaseValue"
    )
    val progress = (positionSec.toFloat() / durationSec.coerceAtLeast(1)).coerceIn(0f, 1f)
    Canvas(
        modifier = Modifier.fillMaxWidth().height(42.dp)
            .pointerInput(durationSec) {
                detectTapGestures { point ->
                    onSeek((point.x / size.width.coerceAtLeast(1).toFloat() * durationSec.toFloat()).toInt().coerceIn(0, durationSec))
                }
            }
            .pointerInput(durationSec) {
                detectDragGestures { change, _ ->
                    onSeek((change.position.x / size.width.coerceAtLeast(1).toFloat() * durationSec.toFloat()).toInt().coerceIn(0, durationSec))
                    change.consume()
                }
            }
    ) {
        val centerY = size.height / 2f
        val progressX = size.width * progress
        drawLine(GlassWhite.copy(alpha = 0.18f), Offset(0f, centerY), Offset(size.width, centerY), 3.dp.toPx(), cap = StrokeCap.Round)
        val layers = listOf(Triple(9.dp.toPx(), 5.dp.toPx(), 0.16f), Triple(5.dp.toPx(), 3.dp.toPx(), 0.40f), Triple(2.dp.toPx(), 2.dp.toPx(), 0.96f))
        layers.forEachIndexed { index, layer ->
            val path = Path()
            var x = 0f
            val step = 3.dp.toPx()
            while (x <= progressX) {
                val angle = (x / size.width.coerceAtLeast(1f) * 5.5f * 2f * PI).toFloat() + phase + index * 0.65f
                val y = centerY + sin(angle) * layer.first * amplitude
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += step
            }
            drawPath(path, accent.copy(alpha = layer.third), style = Stroke(width = layer.second, cap = StrokeCap.Round))
        }
        drawCircle(GlassWhite, 8.dp.toPx(), Offset(progressX, centerY), style = Stroke(width = 2.dp.toPx()))
        drawCircle(accent, 5.dp.toPx(), Offset(progressX, centerY))
    }
}

@Composable
private fun GlowPlayPauseOrb(
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    size: androidx.compose.ui.unit.Dp = 54.dp
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(size)
            .minimumInteractiveComponentSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                        radius = this.size.maxDimension * 0.90f
                    )
                )
            }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        GlassWhite.copy(alpha = 0.24f),
                        accent.copy(alpha = 0.36f),
                        DeepBlue.copy(alpha = 0.75f)
                    )
                )
            )
            .border(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    listOf(GlassWhite.copy(alpha = 0.80f), accent, IceBlue)
                ),
                shape = CircleShape
            )
            .auvionyxBounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = GlassWhite,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Immersive Idle Cockpit Bottom-Center Floating Next-Turn & ETA Fluid Cloud Pill:
 * Contracts smoothly when cruising and expands cleanly for imminent maneuvers.
 */
@Composable
fun IdleNextTurnPill(
    nextTurnDistance: String,
    remainingMinutes: Int,
    remainingDistanceKm: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nextTurnRoad: String? = null
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val imminentManeuver = nextTurnDistance.trim().endsWith(" m", ignoreCase = true)
    var userExpanded by remember { mutableStateOf(false) }
    val expanded = (userExpanded || imminentManeuver) && !nextTurnRoad.isNullOrBlank()
    val pillRadius by animateDpAsState(
        targetValue = if (expanded) 24.dp else 34.dp,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = LivingFlow.CompactDurationMs,
            easing = LivingFlow.LaminarEasing
        ),
        label = "idlePillCorner"
    )

    Surface(
        modifier = modifier
            .testTag("idle_next_turn_pill")
            .clip(RoundedCornerShape(pillRadius))
            .animateContentSize(
                animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height),
                        radius = size.width * 0.55f
                    ),
                    cornerRadius = CornerRadius(pillRadius.toPx(), pillRadius.toPx())
                )
            }
            .photonLuminousTouch(
                pressScale = 0.96f,
                onClick = {
                    if (!nextTurnRoad.isNullOrBlank() && !userExpanded) {
                        userExpanded = true
                    } else {
                        onClick()
                    }
                }
            ),
        color = Color(0xBA040911),
        shape = RoundedCornerShape(pillRadius),
        border = BorderStroke(
            0.9.dp,
            Brush.horizontalGradient(
                listOf(
                    GlassWhite.copy(alpha = 0.24f),
                    accent.copy(alpha = 0.65f),
                    GlassWhite.copy(alpha = 0.24f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TurnRight,
                        contentDescription = "Next turn",
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                    AnimatedContent(
                        targetState = nextTurnDistance,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            } else {
                                fadeIn(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) togetherWith
                                    fadeOut(tween(180, easing = LivingFlow.LaminarEasing))
                            }
                        },
                        label = "idleTurnDistance"
                    ) { dist ->
                        Text(
                            text = dist,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = GlassWhite
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(GlassWhite.copy(alpha = 0.22f))
                )

                Text(
                    text = if (remainingMinutes > 0) {
                        "$remainingMinutes min  ·  ${formatNavigationDistance(remainingDistanceKm)}"
                    } else {
                        "Route Ready"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) +
                    fadeIn(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)),
                exit = shrinkVertically(tween(200, easing = LivingFlow.LaminarEasing)) +
                    fadeOut(tween(160, easing = LivingFlow.LaminarEasing))
            ) {
                Text(
                    text = "Turn onto ${nextTurnRoad.orEmpty()} · Tap again for Maps",
                    style = MaterialTheme.typography.labelMedium,
                    color = IceBlue
                )
            }
        }
    }
}
