package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AuvionyxModule
import com.example.data.model.DashboardSplitRatio
import com.example.data.model.MapPlaceMarker
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.ActiveSpeedometerCard
import com.example.ui.components.CockpitMediaPlayerCard
import com.example.ui.components.AlbumArtworkArt
import com.example.ui.components.GlassCard
import com.example.ui.components.IdleNextTurnPill
import com.example.ui.components.IdleProminentSpeedometer
import com.example.ui.components.NativeMapLibreView
import com.example.ui.components.LumaMotion
import com.example.ui.components.LivingFlow
import com.example.ui.components.photonLuminousTouch
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.PulseClear
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.PulseSlow
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MapsAndPulseState
import com.example.viewmodel.MediaPlayerState


@Composable
private fun CockpitNativeMapPanel(
    currentLocation: Pair<Double, Double>?,
    routePoints: List<Pair<Double, Double>>,
    headingDegrees: Int,
    accent: Color,
    isNavigating: Boolean,
    nextTurnDistance: String,
    nextTurnRoad: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clip(RoundedCornerShape(24.dp))) {
        NativeMapLibreView(
            currentLocation = currentLocation,
            routePoints = routePoints,
            headingDegrees = headingDegrees,
            accent = accent,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp),
            color = Color(0xCC07111D),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isNavigating && routePoints.size >= 2) {
                    "$nextTurnDistance · $nextTurnRoad"
                } else if (currentLocation != null) {
                    "GPS FIXED · NO ACTIVE ROUTE"
                } else {
                    "WAITING FOR GPS"
                },
                color = GlassWhite,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private data class ResolvedCockpitDisplay(
    val speedDisplay: Int,
    val unitLabel: String,
    val maxSpeed: Int,
    val speedLimitDisplay: Int?,
    val headingCardinal: String,
    val nextTurnDistance: String,
    val nextTurnRoad: String,
    val remainingMinutes: Int,
    val remainingKm: Float,
    val etaLabel: String,
)

@Composable
private fun resolveCockpitDisplay(
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mapsPulse: MapsAndPulseState
): ResolvedCockpitDisplay {
    val rawSpeedKmh = (telemetry.speedKmh *
        (1f + settings.hudConfig.speedometerMatchPercent.coerceIn(0f, 10f) / 100f)).toInt()
    val speedDisplay = if (settings.useMetricUnits) {
        rawSpeedKmh
    } else {
        (rawSpeedKmh * 0.621371f).toInt()
    }
    val maxSpeed = if (settings.useMetricUnits) 160 else 100
    val unitLabel = if (settings.useMetricUnits) "km/h" else "mph"

    val nextTurnDist = if (mapsPulse.isNavigating && telemetry.nextTurnDistanceLabel != "--") {
        telemetry.nextTurnDistanceLabel
    } else {
        "--"
    }
    val nextTurnRoad = if (mapsPulse.isNavigating && telemetry.nextTurnRoad != "No Active Route") {
        telemetry.nextTurnRoad
    } else {
        "No active route"
    }
    val remMinutes = if (mapsPulse.isNavigating && telemetry.remainingMinutes > 0) telemetry.remainingMinutes else 0
    val remKm = if (mapsPulse.isNavigating && telemetry.remainingDistanceKm > 0f) telemetry.remainingDistanceKm else 0f
    val eta = if (mapsPulse.isNavigating && telemetry.etaTimeLabel != "--:--") telemetry.etaTimeLabel else "--:--"
    val limit = telemetry.speedLimitKmh

    return ResolvedCockpitDisplay(
        speedDisplay = speedDisplay,
        unitLabel = unitLabel,
        maxSpeed = maxSpeed,
        speedLimitDisplay = limit,
        headingCardinal = telemetry.headingCardinal,
        nextTurnDistance = nextTurnDist,
        nextTurnRoad = nextTurnRoad,
        remainingMinutes = remMinutes,
        remainingKm = remKm,
        etaLabel = eta,
    )
}

@Composable
fun DriveDashboardScreen(
    isLandscapeWide: Boolean,
    isIdleCockpit: Boolean,
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mediaState: MediaPlayerState,
    mapsPulse: MapsAndPulseState,
    notificationAccessGranted: Boolean,
    onSelectRatio: (DashboardSplitRatio) -> Unit,
    onSelectModule: (AuvionyxModule) -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var flatLaunchConsumed by remember { mutableStateOf(false) }
    DisposableEffect(settings.hudConfig.layFlatToStartHud) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val screenUpAndFlat = kotlin.math.abs(x) < 2.0f &&
                    kotlin.math.abs(y) < 2.0f && z > 8.5f
                if (screenUpAndFlat && !flatLaunchConsumed && settings.hudConfig.layFlatToStartHud) {
                    flatLaunchConsumed = true
                    onSelectModule(AuvionyxModule.HUD)
                } else if (!screenUpAndFlat) {
                    flatLaunchConsumed = false
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (settings.hudConfig.layFlatToStartHud && sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose { manager.unregisterListener(listener) }
    }

    if (isLandscapeWide) {
        LandscapeCockpitSurface(
            isIdleCockpit = isIdleCockpit,
            settings = settings,
            telemetry = telemetry,
            mediaState = mediaState,
            mapsPulse = mapsPulse,
            onSelectModule = onSelectModule,
            onSelectMarker = onSelectMarker,
            onToggleTraffic = onToggleTraffic,
            onToggleSatellite = onToggleSatellite,
            onPlayPause = onPlayPause,
            onNextTrack = onNextTrack,
            onPrevTrack = onPrevTrack,
            onToggleShuffle = onToggleShuffle,
            onToggleFavorite = onToggleFavorite,
            onSeek = onSeek,
            modifier = modifier
        )
    } else {
        SmartphonePortraitDriveDashboard(
            settings = settings,
            telemetry = telemetry,
            mediaState = mediaState,
            mapsPulse = mapsPulse,
            notificationAccessGranted = notificationAccessGranted,
            onSelectModule = onSelectModule,
            onSelectMarker = onSelectMarker,
            onToggleTraffic = onToggleTraffic,
            onToggleSatellite = onToggleSatellite,
            onPlayPause = onPlayPause,
            onNextTrack = onNextTrack,
            onPrevTrack = onPrevTrack,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onToggleFavorite = onToggleFavorite,
            onSeek = onSeek,
            modifier = modifier
        )
    }
}

/**
 * Authoritative 1280x800 Landscape Cockpit Surface:
 * Supports both:
 * 1) Active Cockpit (Image 3 blueprint): 64% Map Left | 36% Stacked Speedometer + Media Right
 * 2) Immersive Idle Cockpit (Image 2 blueprint, after 7s untouched):
 *    Full-bleed Expanded Map + Left Prominent Speedometer + Right Compact Media + Bottom Turn Pill
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LandscapeCockpitSurface(
    isIdleCockpit: Boolean,
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mediaState: MediaPlayerState,
    mapsPulse: MapsAndPulseState,
    onSelectModule: (AuvionyxModule) -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val resolved = resolveCockpitDisplay(settings = settings, telemetry = telemetry, mapsPulse = mapsPulse)
    val track = mediaState.currentTrack
    val isFavorite = track.id in mediaState.favoriteIds

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isIdleCockpit,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(
                        tween(durationMillis = LumaMotion.PanelDurationMs, easing = LumaMotion.LaminarEasing)
                    ) togetherWith fadeOut(
                        tween(durationMillis = LumaMotion.StandardDurationMs, easing = LumaMotion.LaminarEasing)
                    )
                }
            },
            label = "cockpitLumaMotionMorph",
            modifier = Modifier.fillMaxSize()
        ) { idle ->
            if (!idle) {
                // ACTIVE COCKPIT COMPOSITION (Image 3 Blueprint)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("active_cockpit_root"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Persisted dashboard split controls the real map/media panel widths.
                    if (settings.dashboardRatio != DashboardSplitRatio.FULL_MUSIC) {
                        CockpitNativeMapPanel(
                            currentLocation = mapsPulse.currentLatLon,
                            routePoints = mapsPulse.routePolylineGeo,
                            headingDegrees = telemetry.headingDegrees,
                            accent = ElectricCyan,
                            isNavigating = mapsPulse.isNavigating,
                            nextTurnDistance = telemetry.nextTurnDistanceLabel,
                            nextTurnRoad = telemetry.nextTurnRoad,
                            modifier = Modifier
                                .weight(if (settings.dashboardRatio == DashboardSplitRatio.FULL_MAPS) 1f else settings.dashboardRatio.mapWeight)
                                .fillMaxHeight()
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "cockpit_map"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = { _, _ -> LumaMotion.LaminarBoundsSpec }
                                )
                        )
                    }

                    if (settings.dashboardRatio != DashboardSplitRatio.FULL_MAPS) {
                    // Right Stacked Speedometer + Media Player Column
                    Column(
                        modifier = Modifier
                            .weight(if (settings.dashboardRatio == DashboardSplitRatio.FULL_MUSIC) 1f else settings.dashboardRatio.mediaWeight)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (settings.dashboardRatio != DashboardSplitRatio.FULL_MUSIC) {
                            ActiveSpeedometerCard(
                                speedValue = resolved.speedDisplay,
                                maxSpeed = resolved.maxSpeed,
                                unitLabel = resolved.unitLabel,
                                speedLimitValue = resolved.speedLimitDisplay,
                                headingCardinal = resolved.headingCardinal,
                                isLiveGps = telemetry.isLiveGpsActive,
                                onClick = { onSelectModule(AuvionyxModule.HUD) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.45f)
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "cockpit_speedometer"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        boundsTransform = { _, _ -> LumaMotion.LaminarBoundsSpec }
                                    )
                            )
                        }

                        CockpitMediaPlayerCard(
                            track = track,
                            source = mediaState.activeSessionSource,
                            isPlaying = mediaState.isPlaying,
                            isShuffle = mediaState.isShuffle,
                            isFavorite = isFavorite,
                            positionSec = mediaState.positionSec,
                            compactMode = false,
                            externalArtwork = mediaState.externalArtwork,
                            onOpenMusic = { onSelectModule(AuvionyxModule.MUSIC) },
                            onPlayPause = onPlayPause,
                            onNextTrack = onNextTrack,
                            onPrevTrack = onPrevTrack,
                            onToggleShuffle = onToggleShuffle,
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onSeek = onSeek,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(if (settings.dashboardRatio == DashboardSplitRatio.FULL_MUSIC) 1f else 0.55f)
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "cockpit_media"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = { _, _ -> LumaMotion.LaminarBoundsSpec }
                                )
                        )
                    }
                    }
                }
            } else {
                // IMMERSIVE IDLE COCKPIT COMPOSITION (Image 2 Blueprint — 7s untouched)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("idle_cockpit_root")
                ) {
                    // Expanded Full-Bleed Map (secondary controls & card borders hidden)
                    CockpitNativeMapPanel(
                        currentLocation = mapsPulse.currentLatLon,
                        routePoints = mapsPulse.routePolylineGeo,
                        headingDegrees = telemetry.headingDegrees,
                        accent = ElectricCyan,
                        isNavigating = mapsPulse.isNavigating,
                        nextTurnDistance = telemetry.nextTurnDistanceLabel,
                        nextTurnRoad = telemetry.nextTurnRoad,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "cockpit_map"),
                                animatedVisibilityScope = this@AnimatedContent,
                                boundsTransform = { _, _ -> LumaMotion.LaminarBoundsSpec }
                            )
                    )

                    // Prominent Left Floating Speedometer Ring
                    IdleProminentSpeedometer(
                        speedValue = resolved.speedDisplay,
                        maxSpeed = resolved.maxSpeed,
                        unitLabel = resolved.unitLabel,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 28.dp, bottom = 12.dp)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "cockpit_speedometer"),
                                animatedVisibilityScope = this@AnimatedContent,
                                boundsTransform = { _, _ -> LumaMotion.LaminarBoundsSpec }
                            )
                    )

                    // Right Floating Compact Media Player Card (Prev / Play-Pause / Next)
                    CockpitMediaPlayerCard(
                        track = track,
                        source = mediaState.activeSessionSource,
                        isPlaying = mediaState.isPlaying,
                        isShuffle = mediaState.isShuffle,
                        isFavorite = isFavorite,
                        positionSec = mediaState.positionSec,
                        compactMode = true,
                        externalArtwork = mediaState.externalArtwork,
                        onOpenMusic = { onSelectModule(AuvionyxModule.MUSIC) },
                        onPlayPause = onPlayPause,
                        onNextTrack = onNextTrack,
                        onPrevTrack = onPrevTrack,
                        onToggleShuffle = onToggleShuffle,
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onSeek = onSeek,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 28.dp, bottom = 18.dp)
                            .width(298.dp)
                            .height(238.dp)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "cockpit_media"),
                                animatedVisibilityScope = this@AnimatedContent,
                                boundsTransform = { _, _ -> LumaMotion.LaminarBoundsSpec }
                            )
                    )

                    // Bottom-Center Floating Next-Turn & Remaining Trip Fluid Cloud Pill
                    IdleNextTurnPill(
                        nextTurnDistance = resolved.nextTurnDistance,
                        remainingMinutes = resolved.remainingMinutes,
                        remainingDistanceKm = resolved.remainingKm,
                        nextTurnRoad = resolved.nextTurnRoad,
                        onClick = { onSelectModule(AuvionyxModule.MAPS) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                            .animateEnterExit(
                                enter = slideInVertically(LumaMotion.LaminarIntOffsetSpec) { it / 2 } +
                                    fadeIn(tween(LumaMotion.StandardDurationMs, easing = LumaMotion.LaminarEasing)),
                                exit = slideOutVertically(LumaMotion.LaminarIntOffsetSpec) { it / 2 } +
                                    fadeOut(tween(LumaMotion.CompactDurationMs, easing = LumaMotion.LaminarEasing))
                            )
                    )
                }
            }
        }
    }
}

/**
 * Clean Portrait Phone Companion Layout (OPPO Find X10 / Galaxy S24+).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SmartphonePortraitDriveDashboard(
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mediaState: MediaPlayerState,
    mapsPulse: MapsAndPulseState,
    notificationAccessGranted: Boolean,
    onSelectModule: (AuvionyxModule) -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val track = mediaState.currentTrack
    val resolved = resolveCockpitDisplay(settings = settings, telemetry = telemetry, mapsPulse = mapsPulse)
    var mapFraction by remember { mutableStateOf(0.60f) }
    val animatedMapFraction by animateFloatAsState(
        targetValue = mapFraction,
        animationSpec = tween(
            durationMillis = if (LocalAuvionyxMotion.current.reducedMotion) 0 else 360,
            easing = LivingFlow.LaminarEasing
        ),
        label = "portraitDriveMapSplit"
    )
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().navigationBarsPadding()
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(animatedMapFraction.coerceIn(0.25f, 0.88f))
            ) {
                NativeMapLibreView(
                    currentLocation = mapsPulse.currentLatLon,
                    routePoints = mapsPulse.routePolylineGeo,
                    headingDegrees = telemetry.headingDegrees,
                    accent = accent,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
                        .fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC07111D),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f))
                    ) {
                        Text(
                            "GPS · ${resolved.speedDisplay} ${resolved.unitLabel}",
                            color = GlassWhite,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        )
                    }
                    Surface(
                        color = Color(0xCC07111D),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                    ) {
                        Text(
                            if (mapsPulse.isNavigating) "NAV · ${telemetry.nextTurnDistanceLabel} · ${telemetry.nextTurnRoad}" else "NAV READY",
                            color = if (mapsPulse.isNavigating) accent else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        )
                    }
                    Surface(
                        onClick = { onSelectModule(AuvionyxModule.HUD) },
                        color = accent.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.24f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.Speed, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Text("HUD", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(32.dp)
                    .pointerInput(totalHeightPx) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (totalHeightPx > 0f) {
                                    mapFraction = (mapFraction + dragAmount / totalHeightPx).coerceIn(0.25f, 0.88f)
                                }
                            },
                            onDragEnd = {
                                mapFraction = when {
                                    mapFraction >= 0.73f -> 0.85f
                                    mapFraction <= 0.45f -> 0.30f
                                    else -> 0.60f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.82f))
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().weight((1f - animatedMapFraction).coerceIn(0.12f, 0.75f))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                if (animatedMapFraction >= 0.75f) {
                    Row(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                            .background(Color(0xEE0A1019)).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when {
                            mediaState.externalArtwork != null -> Image(
                                bitmap = mediaState.externalArtwork.asImageBitmap(),
                                contentDescription = "Current track album artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp))
                            )
                            mediaState.isExternalSessionActive -> Surface(
                                modifier = Modifier.size(40.dp),
                                color = track.adaptiveColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(9.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = track.adaptiveColor)
                                }
                            }
                            else -> AlbumArtworkArt(
                                artVariant = track.artVariant,
                                accentColor = track.adaptiveColor,
                                cornerRadius = 9.dp,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(
                                track.title,
                                color = GlassWhite,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.fillMaxWidth().basicMarquee()
                            )
                            Text(
                                track.artist,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.fillMaxWidth().basicMarquee()
                            )
                        }
                        Surface(
                            onClick = onPlayPause,
                            shape = CircleShape,
                            color = accent.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                    tint = accent
                                )
                            }
                        }
                        Surface(
                            onClick = onNextTrack,
                            shape = CircleShape,
                            color = Color(0x553B4652),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.SkipNext, contentDescription = "Next track", tint = GlassWhite)
                            }
                        }
                    }
                } else {
                    CockpitMediaPlayerCard(
                        track = track,
                        source = mediaState.activeSessionSource,
                        isPlaying = mediaState.isPlaying,
                        isShuffle = mediaState.isShuffle,
                        isRepeat = mediaState.isRepeat,
                        isFavorite = track.id in mediaState.favoriteIds,
                        positionSec = mediaState.effectivePositionSec,
                        compactMode = false,
                        externalArtwork = mediaState.externalArtwork,
                        onOpenMusic = { onSelectModule(AuvionyxModule.MUSIC) },
                        onPlayPause = onPlayPause,
                        onNextTrack = onNextTrack,
                        onPrevTrack = onPrevTrack,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onSeek = onSeek,
                        artworkSize = if (animatedMapFraction < 0.45f) 124.dp else 66.dp,
                        showRepeat = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun HaloGlanceTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    statusState: ServiceAvailabilityState,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val dotColor = when (statusState) {
        ServiceAvailabilityState.LIVE -> PulseClear
        ServiceAvailabilityState.DEMO -> accent
        ServiceAvailabilityState.OFFLINE -> PulseSlow
        ServiceAvailabilityState.PERMISSION_DENIED,
        ServiceAvailabilityState.API_UNAVAILABLE -> PulseCongestion
    }

    GlassCard(
        modifier = modifier
            .testTag(testTag)
            .height(94.dp)
            .clip(RoundedCornerShape(20.dp))
            .photonLuminousTouch(pressScale = 0.96f, onClick = onClick),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = GlassWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
