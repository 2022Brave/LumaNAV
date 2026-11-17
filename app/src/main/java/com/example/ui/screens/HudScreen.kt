package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuvionyxModule
import com.example.data.model.HudConfig
import com.example.data.model.MapPlaceMarker
import com.example.data.model.MediaTrack
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.AlbumArtworkArt
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.LivingFlow
import com.example.ui.components.photonLuminousTouch
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MapsAndPulseState
import com.example.viewmodel.MediaPlayerState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Focused, Clutter-Free Halo HUD Screen:
 * Strictly displays ONLY the 4 requested driving elements:
 * 1. Next Turn
 * 2. Speedometer
 * 3. Map
 * 4. Music player at the bottom
 */
@Composable
fun HudScreen(
    isLandscapeWide: Boolean,
    hudConfig: HudConfig,
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mapsPulse: MapsAndPulseState,
    mediaState: MediaPlayerState,
    onSelectModule: (AuvionyxModule) -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hudAccent = hudConfig.accentChoice.color
    val speedDisplay = if (settings.useMetricUnits) {
        telemetry.speedKmh
    } else {
        (telemetry.speedKmh * 0.621371f).toInt()
    }
    val unitLabel = if (settings.useMetricUnits) "KM/H" else "MPH"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .graphicsLayer {
                if (hudConfig.mirrorFlipForWindshield) {
                    scaleY = -1f
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLandscapeWide) {
            // Landscape HUD: Left (Next Turn + Speedometer) | Right (Clean Live Map)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HudNextTurnCard(
                        telemetry = telemetry,
                        isNavigating = mapsPulse.isNavigating,
                        accent = hudAccent,
                        onClick = { onSelectModule(AuvionyxModule.MAPS) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            HudSpeedometerGauge(
                                speed = speedDisplay,
                                maxSpeed = if (settings.useMetricUnits) 160 else 100,
                                unitLabel = unitLabel,
                                accent = hudAccent
                            )
                        }
                    }
                }

                InteractiveMapCanvas(
                    telemetry = telemetry,
                    markers = mapsPulse.markers,
                    selectedMarkerId = mapsPulse.selectedMarkerId,
                    showTrafficOverlay = settings.showTrafficOverlay,
                    showSatelliteGrid = settings.showSatelliteGrid,
                    isNavigating = mapsPulse.isNavigating,
                    onSelectMarker = onSelectMarker,
                    onToggleTraffic = {},
                    onToggleSatellite = {},
                    currentLatLon = mapsPulse.currentLatLon,
                    destinationLatLon = mapsPulse.destinationLatLon,
                    routePolylineGeo = mapsPulse.routePolylineGeo,
                    navigationSourceLabel = mapsPulse.navigationSourceLabel,
                    isDemoMode = settings.demoSimulationEnabled,
                    showMapControls = false,
                    compactHudMode = true,
                    onExpandMap = { onSelectModule(AuvionyxModule.MAPS) },
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Portrait HUD: Top Row (Next Turn + Speedometer), Middle (Map)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudNextTurnCard(
                    telemetry = telemetry,
                    isNavigating = mapsPulse.isNavigating,
                    accent = hudAccent,
                    onClick = { onSelectModule(AuvionyxModule.MAPS) },
                    modifier = Modifier.weight(1f)
                )

                GlassCard(
                    modifier = Modifier.size(134.dp),
                    cornerRadius = 22.dp,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HudSpeedometerGauge(
                            speed = speedDisplay,
                            maxSpeed = if (settings.useMetricUnits) 160 else 100,
                            unitLabel = unitLabel,
                            accent = hudAccent,
                            compact = true
                        )
                    }
                }
            }

            InteractiveMapCanvas(
                telemetry = telemetry,
                markers = mapsPulse.markers,
                selectedMarkerId = mapsPulse.selectedMarkerId,
                showTrafficOverlay = settings.showTrafficOverlay,
                showSatelliteGrid = settings.showSatelliteGrid,
                isNavigating = mapsPulse.isNavigating,
                onSelectMarker = onSelectMarker,
                onToggleTraffic = {},
                onToggleSatellite = {},
                currentLatLon = mapsPulse.currentLatLon,
                destinationLatLon = mapsPulse.destinationLatLon,
                routePolylineGeo = mapsPulse.routePolylineGeo,
                navigationSourceLabel = mapsPulse.navigationSourceLabel,
                isDemoMode = settings.demoSimulationEnabled,
                showMapControls = false,
                compactHudMode = true,
                onExpandMap = { onSelectModule(AuvionyxModule.MAPS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Bottom: Clean Music Player Bar
        HudBottomMusicBar(
            track = mediaState.currentTrack,
            isPlaying = mediaState.isPlaying,
            accent = hudAccent,
            onOpenMusic = { onSelectModule(AuvionyxModule.MUSIC) },
            onPrevious = onPrevTrack,
            onPlayPause = onPlayPause,
            onNext = onNextTrack
        )
    }
}

@Composable
private fun HudNextTurnCard(
    telemetry: VehicleTelemetry,
    isNavigating: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val turnIcon = when {
        telemetry.nextTurnInstruction.contains("left", ignoreCase = true) -> Icons.Outlined.TurnLeft
        telemetry.nextTurnInstruction.contains("right", ignoreCase = true) -> Icons.Outlined.TurnRight
        else -> Icons.Outlined.Navigation
    }

    GlassCard(
        modifier = modifier
            .testTag("hud_next_turn_card")
            .clip(RoundedCornerShape(22.dp))
            .animateContentSize(
                animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
            )
            .photonLuminousTouch(pressScale = 0.97f, glowColor = accent, onClick = onClick),
        cornerRadius = 22.dp,
        highlighted = isNavigating,
        accentOverride = accent,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = turnIcon,
                contentDescription = "Next turn",
                tint = accent,
                modifier = Modifier.size(34.dp)
            )
            AnimatedContent(
                targetState = (if (isNavigating) telemetry.nextTurnDistanceLabel else "Next Turn") to telemetry.nextTurnRoad,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        fadeIn(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) togetherWith
                            fadeOut(tween(180, easing = LivingFlow.LaminarEasing))
                    }
                },
                label = "hudNextTurnText",
                modifier = Modifier.weight(1f)
            ) { (dist, road) ->
                Column {
                    Text(
                        text = dist,
                        style = MaterialTheme.typography.titleLarge,
                        color = GlassWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = road,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IceBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HudSpeedometerGauge(
    speed: Int,
    maxSpeed: Int,
    unitLabel: String,
    accent: Color,
    compact: Boolean = false
) {
    val boxSize = if (compact) 114.dp else 188.dp
    val fontSize = if (compact) 34.sp else 56.sp
    Box(
        modifier = Modifier
            .testTag("hud_speedometer")
            .size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = if (compact) 6.dp.toPx() else 9.dp.toPx()
            val inset = stroke / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val sweepTotal = 250f
            val startAngle = 145f
            val fraction = (speed.toFloat() / maxSpeed.coerceAtLeast(1)).coerceIn(0f, 1f)
            val radius = arcSize.width / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawArc(
                color = GlassWhite.copy(alpha = 0.10f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(DeepBlue, accent, IceBlue)),
                startAngle = startAngle,
                sweepAngle = sweepTotal * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val tipAngleRad = (startAngle + sweepTotal * fraction) * (PI / 180.0)
            val tipCenter = Offset(
                cx + cos(tipAngleRad).toFloat() * radius,
                cy + sin(tipAngleRad).toFloat() * radius
            )
            drawCircle(color = GlassWhite, radius = 3.dp.toPx(), center = tipCenter)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$speed",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = GlassWhite
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
    }
}

@Composable
private fun HudBottomMusicBar(
    track: MediaTrack,
    isPlaying: Boolean,
    accent: Color,
    onOpenMusic: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hud_bottom_music_bar")
            .clip(RoundedCornerShape(20.dp))
            .photonLuminousTouch(pressScale = 0.98f, glowColor = accent, onClick = onOpenMusic),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtworkArt(
                artVariant = track.artVariant,
                accentColor = accent,
                cornerRadius = 10.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GlassWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .clip(CircleShape)
                        .photonLuminousTouch(pressScale = 0.90f, glowColor = accent)
                        .testTag("hud_media_prev")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = GlassWhite
                    )
                }
                Surface(
                    color = accent.copy(alpha = 0.20f),
                    shape = CircleShape,
                    border = BorderStroke(0.8.dp, accent),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .testTag("hud_media_play_pause")
                        .photonLuminousTouch(pressScale = 0.92f, glowColor = accent, onClick = onPlayPause)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = GlassWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .clip(CircleShape)
                        .photonLuminousTouch(pressScale = 0.90f, glowColor = accent)
                        .testTag("hud_media_next")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = "Next track",
                        tint = GlassWhite
                    )
                }
            }
        }
    }
}
