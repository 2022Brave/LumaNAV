package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.components.NativeMapLibreView
import com.example.ui.components.formatNavigationDistance
import com.example.data.model.AuvionyxModule
import com.example.data.model.HudConfig
import com.example.data.model.HudLayoutMode
import com.example.data.model.MediaTrack
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.AlbumArtworkArt
import com.example.ui.components.GlassCard
import com.example.ui.components.photonLuminousTouch
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaPlayerState

/**
 * Minimal driving HUD: speedometer, live navigation map, and media controls only.
 */
@Composable
fun HudScreen(
    isLandscapeWide: Boolean,
    hudConfig: HudConfig,
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mediaState: MediaPlayerState,
    currentLocation: Pair<Double, Double>?,
    routePoints: List<Pair<Double, Double>>,
    headingDegrees: Int,
    onSelectModule: (AuvionyxModule) -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mirrorForWindshield by remember { mutableStateOf(hudConfig.mirrorFlipForWindshield) }
    val hudAccent = hudConfig.accentChoice.color
    val speedDisplay = if (settings.useMetricUnits) telemetry.speedKmh
        else (telemetry.speedKmh * 0.621371f).toInt()
    val unitLabel = if (settings.useMetricUnits) "KM/H" else "MPH"

    Column(
        modifier = modifier.fillMaxSize().background(ObsidianBlack).graphicsLayer {
            if (mirrorForWindshield) scaleX = -1f
        },
        verticalArrangement = Arrangement.spacedBy(if (isLandscapeWide) 8.dp else 10.dp)
    ) {
        Surface(
            onClick = { mirrorForWindshield = !mirrorForWindshield },
            color = if (mirrorForWindshield) hudAccent.copy(alpha = 0.22f) else Color(0xFF101722),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, hudAccent.copy(alpha = 0.42f))
        ) {
            Text(
                text = if (mirrorForWindshield) "WINDSHIELD MIRROR · ON" else "WINDSHIELD MIRROR · OFF",
                color = GlassWhite,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
        if (isLandscapeWide) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    modifier = Modifier.weight(0.27f).fillMaxHeight(),
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        HudSpeedometerGauge(
                            speed = speedDisplay,
                            maxSpeed = if (settings.useMetricUnits) 160 else 100,
                            unitLabel = unitLabel,
                            accent = hudAccent
                        )
                    }
                }
                if (hudConfig.layoutMode == HudLayoutMode.MINIMAL) {
                    HudMinimalTurnCard(
                        distance = telemetry.nextTurnDistanceLabel,
                        instruction = telemetry.nextTurnInstruction,
                        accent = hudAccent,
                        modifier = Modifier.weight(0.73f).fillMaxHeight()
                    )
                } else {
                    AuvionyxNativeMapPanel(
                        currentLocation = currentLocation,
                        routePoints = routePoints,
                        headingDegrees = headingDegrees,
                        accent = hudAccent,
                        isNavigating = routePoints.size >= 2,
                        nextTurnDistance = telemetry.nextTurnDistanceLabel,
                        nextTurnRoad = telemetry.nextTurnRoad,
                        nextTurnInstruction = telemetry.nextTurnInstruction,
                        telemetryDestinationLabel = telemetry.destinationLabel,
                        modifier = Modifier.weight(0.73f).fillMaxHeight()
                    )
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.fillMaxWidth().height(126.dp),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(6.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    HudSpeedometerGauge(
                        speed = speedDisplay,
                        maxSpeed = if (settings.useMetricUnits) 160 else 100,
                        unitLabel = unitLabel,
                        accent = hudAccent,
                        compact = true
                    )
                }
            }
            if (hudConfig.layoutMode == HudLayoutMode.MINIMAL) {
                HudMinimalTurnCard(
                    distance = telemetry.nextTurnDistanceLabel,
                    instruction = telemetry.nextTurnInstruction,
                    accent = hudAccent,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                AuvionyxNativeMapPanel(
                    currentLocation = currentLocation,
                    routePoints = routePoints,
                    headingDegrees = headingDegrees,
                    accent = ElectricCyan,
                    isNavigating = routePoints.size >= 2,
                    nextTurnDistance = telemetry.nextTurnDistanceLabel,
                    nextTurnRoad = telemetry.nextTurnRoad,
                    nextTurnInstruction = telemetry.nextTurnInstruction,
                    telemetryDestinationLabel = telemetry.destinationLabel,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }

        if (hudConfig.layoutMode == HudLayoutMode.ADVANCED) {
            Surface(
                color = Color(0xFF0A0D12),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, hudAccent.copy(alpha = 0.32f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ETA ${telemetry.etaTimeLabel}", color = GlassWhite, style = MaterialTheme.typography.labelMedium)
                    Text(formatNavigationDistance(telemetry.remainingDistanceKm), color = hudAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
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
}

@Composable
private fun AuvionyxNativeMapPanel(
    currentLocation: Pair<Double, Double>?,
    routePoints: List<Pair<Double, Double>>,
    headingDegrees: Int,
    accent: Color,
    isNavigating: Boolean,
    nextTurnDistance: String,
    nextTurnRoad: String,
    nextTurnInstruction: String,
    telemetryDestinationLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF03070C))
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .testTag("hud_live_map_panel")
    ) {
        NativeMapLibreView(
            currentLocation = currentLocation,
            routePoints = routePoints,
            headingDegrees = headingDegrees,
            accent = accent,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = Color(0xCC03070C),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.72f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = "Next maneuver",
                    tint = accent,
                    modifier = Modifier.size(58.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isNavigating) nextTurnDistance else "READY",
                        color = GlassWhite,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isNavigating) nextTurnInstruction.ifBlank { nextTurnRoad } else "NO ACTIVE ROUTE",
                        color = GlassWhite.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isNavigating && telemetryDestinationLabel.isNotBlank()) {
                        Text(
                            text = "TO · $telemetryDestinationLabel",
                            color = accent,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (currentLocation == null) {
            Text(
                text = "WAITING FOR GPS",
                color = GlassWhite.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color(0xCC03070C), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
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

@Composable
private fun HudMinimalTurnCard(
    distance: String,
    instruction: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0A0D12),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Navigation,
                contentDescription = "Next turn",
                tint = accent,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = distance,
                color = GlassWhite,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = instruction.ifBlank { "No active route" },
                color = TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
