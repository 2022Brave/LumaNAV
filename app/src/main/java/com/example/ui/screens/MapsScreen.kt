package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.core.LiveGoogleMapsSyncBridge
import com.example.core.NavigationCaptureService
import com.example.data.model.DestinationCandidate
import com.example.data.model.MapPlaceMarker
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.DraggableLaminarSheet
import com.example.ui.components.GlassCard
import com.example.ui.components.NativeMapLibreView
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.LivingFlow
import com.example.ui.components.ServiceStateBadge
import com.example.ui.components.TrafficPulseBadge
import com.example.ui.components.photonLuminousTouch
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PulseClear
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MapsAndPulseState

/**
 * Halo HUD Minimalist Voice-First Maps Screen:
 * Clean destination search button, subtle dot service readouts, airy route list, and distraction-free map canvas.
 */
@Composable
fun MapsScreen(
    isLandscapeWide: Boolean,
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mapsPulse: MapsAndPulseState,
    locationGranted: Boolean,
    notificationAccessGranted: Boolean,
    onPermissionUpdated: () -> Unit,
    onConfirmDestination: (DestinationCandidate) -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onSearchDestination: (String) -> Unit,
    onClearDestinationSearch: () -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onSelectRoute: (String) -> Unit,
    onToggleNavigation: () -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionUpdated()
    }

    val captureActive by NavigationCaptureService.captureActive.collectAsState()
    val latestCaptureFrame by NavigationCaptureService.latestFrame.collectAsState()
    val captureError by NavigationCaptureService.captureError.collectAsState()
    val externalNavigation by LiveGoogleMapsSyncBridge.activeNavigation.collectAsState()

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val captureData = result.data
        if (result.resultCode == Activity.RESULT_OK && captureData != null) {
            NavigationCaptureService.start(context, result.resultCode, captureData)
        }
    }

    val routeContent: @Composable (Modifier) -> Unit = { contentMod ->
        Column(
            modifier = contentMod.animateContentSize(
                animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = mapsPulse.destinationQuery,
                    onValueChange = onDestinationQueryChange,
                    modifier = Modifier.weight(1f).testTag("maps_destination_search"),
                    singleLine = true,
                    placeholder = { Text("Search destination…", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search destination", tint = accent) },
                    trailingIcon = {
                        if (mapsPulse.destinationQuery.isNotBlank()) {
                            IconButton(onClick = onClearDestinationSearch) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear destination", tint = TextSecondary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchDestination(mapsPulse.destinationQuery) })
                )
                IconButton(
                    onClick = { onSearchDestination(mapsPulse.destinationQuery) },
                    modifier = Modifier.testTag("maps_search_submit")
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = accent)
                }
            }
            // Minimal Service Indicators + External Navigation Sync Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServiceStateBadge(
                        state = mapsPulse.gpsServiceState,
                        labelPrefix = if (telemetry.isLocationFallback) "GPS · LAST KNOWN" else "GPS"
                    )
                    ServiceStateBadge(
                        state = if (notificationAccessGranted) ServiceAvailabilityState.LIVE else ServiceAvailabilityState.PERMISSION_DENIED,
                        labelPrefix = "Nav Sync"
                    )
                }
                if (!locationGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("maps_enable_gps")
                            .photonLuminousTouch(
                                pressScale = 0.94f,
                                onClick = {
                                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GpsFixed,
                            contentDescription = "Enable GPS",
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text("Enable GPS", style = MaterialTheme.typography.labelSmall, color = IceBlue)
                    }
                } else if (!notificationAccessGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("maps_enable_sync")
                            .photonLuminousTouch(
                                pressScale = 0.94f,
                                onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Enable navigation sync",
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text("Sync Nav", style = MaterialTheme.typography.labelSmall, color = IceBlue)
                    }
                }
            }

            // In-app mirror for user-selected Google Maps, Waze or Mappls app windows.
            Surface(
                color = Color(0xFF08111C),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.8.dp, accent.copy(alpha = 0.38f)),
                modifier = Modifier.fillMaxWidth().testTag("maps_navigation_mirror_panel")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "NAVIGATION MIRROR",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                            Text(
                                when {
                                    captureActive -> "Selected app window is being mirrored"
                                    externalNavigation != null -> "${externalNavigation!!.providerLabel} guidance detected"
                                    else -> "Mirror a navigation app inside LumaNav"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                if (captureActive) {
                                    NavigationCaptureService.stop(context)
                                } else {
                                    val manager = context.getSystemService(
                                        android.content.Context.MEDIA_PROJECTION_SERVICE
                                    ) as MediaProjectionManager
                                    captureLauncher.launch(manager.createScreenCaptureIntent())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (captureActive) Color(0xFF7A2632) else accent,
                                contentColor = if (captureActive) GlassWhite else ObsidianBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("maps_navigation_mirror_toggle")
                        ) {
                            Text(if (captureActive) "Stop" else "Mirror")
                        }
                    }

                    if (captureActive) {
                        val frame = latestCaptureFrame
                        if (frame != null && !frame.isRecycled) {
                            Image(
                                bitmap = frame.asImageBitmap(),
                                contentDescription = "Live mirrored navigation app window",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isLandscapeWide) 150.dp else 112.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("maps_navigation_mirror_frame")
                            )
                        } else {
                            Text(
                                "Waiting for the selected app window…",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    externalNavigation?.let { navigation ->
                        Text(
                            "${navigation.providerLabel} · ${navigation.turnInstruction}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            buildList {
                                if (navigation.nextTurnDistanceLabel != "--") add("Next: ${navigation.nextTurnDistanceLabel}")
                                navigation.remainingSummary.takeIf(String::isNotBlank)?.let { add(it) }
                                navigation.etaTimeLabel?.let { add("ETA $it") }
                            }.joinToString(" · ").ifBlank { "Live guidance notification detected" },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    captureError?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF8A80)
                        )
                    }

                    if (!captureActive) {
                        Text(
                            "Start navigation in Maps, Waze or Mappls, tap Mirror, then choose that app's window in Android's sharing picker. Avoid Entire screen if you don't want LumaNav to mirror itself.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Continuous Container Morphing between Destination Confirmation / Search Status / None
            val bannerState = when {
                mapsPulse.isExternalGoogleMapsSyncActive -> 3
                mapsPulse.confirmedDestination != null -> 1
                mapsPulse.destinationSearchState != null && mapsPulse.destinationCandidates.isEmpty() -> 2
                else -> 0
            }
            AnimatedContent(
                targetState = bannerState,
                transitionSpec = {
                    if (reducedMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        (fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing))) togetherWith
                            (fadeOut(tween(220, easing = LivingFlow.LaminarEasing)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(220, easing = LivingFlow.LaminarEasing)))
                    }
                },
                label = "mapsDestinationBannerMorph"
            ) { state ->
                when (state) {
                    1 -> {
                        val target = mapsPulse.confirmedDestination
                        if (target != null) {
                            Surface(
                                color = PulseClear.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.8.dp, PulseClear.copy(alpha = 0.40f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = PulseClear, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(target.name, style = MaterialTheme.typography.titleSmall, color = GlassWhite)
                                        Text(
                                            text = target.addressSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    ServiceStateBadge(state = ServiceAvailabilityState.LIVE)
                                }
                            }
                        }
                    }
                    2 -> {
                        val status = mapsPulse.destinationSearchState
                        if (status != null) {
                            Surface(
                                color = PulseCongestion.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.8.dp, PulseCongestion.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ServiceStateBadge(state = status, labelPrefix = "Geocoder")
                                    Text(
                                        text = mapsPulse.destinationSearchMessage
                                            ?: "No results when live Geocoder is unavailable.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        Surface(
                            color = DeepBlue.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = mapsPulse.navigationSourceLabel ?: "External navigation active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                                Text(
                                    text = telemetry.nextTurnInstruction,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GlassWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${telemetry.nextTurnDistanceLabel} · ${mapsPulse.destinationSearchMessage.orEmpty()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    else -> Spacer(Modifier.height(0.dp))
                }
            }

            // Continuous Morph between Candidate Disambiguation List & Route Alternatives
            AnimatedContent(
                targetState = mapsPulse.destinationCandidates.isNotEmpty(),
                transitionSpec = {
                    if (reducedMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) togetherWith
                            fadeOut(tween(220, easing = LivingFlow.LaminarEasing))
                    }
                },
                label = "mapsCandidateVsRoutesMorph",
                modifier = Modifier.weight(1f, fill = false)
            ) { showingCandidates ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showingCandidates) {
                        item {
                            Text(
                                text = "Select Matching Destination",
                                style = MaterialTheme.typography.labelLarge,
                                color = IceBlue
                            )
                        }
                        items(mapsPulse.destinationCandidates, key = { it.id }) { candidate ->
                            Surface(
                                color = Color(0xFF0E1522),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.8.dp, accent.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("maps_candidate_${candidate.id}")
                                    .photonLuminousTouch(
                                        pressScale = 0.97f,
                                        onClick = { onConfirmDestination(candidate) }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Place, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(candidate.name, style = MaterialTheme.typography.titleSmall, color = GlassWhite)
                                        Text(
                                            text = candidate.addressSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text("Select", style = MaterialTheme.typography.labelMedium, color = accent)
                                }
                            }
                        }
                    } else {
                        items(mapsPulse.routes, key = { it.id }) { route ->
                            Surface(
                                color = if (route.isSelected) DeepBlue.copy(alpha = 0.30f) else Color(0xFF0A0E14),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (route.isSelected) accent else GlassWhite.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("route_${route.id}")
                                    .photonLuminousTouch(
                                        pressScale = 0.97f,
                                        onClick = { onSelectRoute(route.id) }
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AltRoute,
                                                contentDescription = null,
                                                tint = accent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = route.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = GlassWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TrafficPulseBadge(severity = route.trafficState)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${route.viaRoad} · ${route.durationMin} min (${route.distanceKm} km)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onToggleNavigation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = ObsidianBlack
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .photonLuminousTouch(pressScale = 0.96f, glowColor = GlassWhite)
                    .testTag("maps_nav_toggle")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (mapsPulse.isNavigating) "Pause Guidance" else "Start Navigation",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (isLandscapeWide) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .width(350.dp)
                    .fillMaxHeight(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                routeContent(Modifier.fillMaxSize())
            }
            NativeMapLibreView(
                currentLocation = mapsPulse.currentLatLon,
                routePoints = mapsPulse.routePolylineGeo,
                headingDegrees = telemetry.headingDegrees,
                accent = accent,
                modifier = Modifier.weight(1f).fillMaxHeight().zIndex(0f)
            )
        }
    } else {
        // Smartphone Portrait: Full-bleed interactive map beneath a velocity-responsive Draggable Laminar Sheet
        Box(modifier = modifier.fillMaxSize()) {
            NativeMapLibreView(
                currentLocation = mapsPulse.currentLatLon,
                routePoints = mapsPulse.routePolylineGeo,
                headingDegrees = telemetry.headingDegrees,
                accent = accent,
                modifier = Modifier.fillMaxSize().zIndex(0f)
            )

            DraggableLaminarSheet(
                minHeight = 250.dp,
                maxHeight = 520.dp,
                initialFraction = 0.42f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag("maps_portrait_laminar_sheet")
                    .zIndex(1f)
            ) {
                routeContent(Modifier.fillMaxSize())
            }
        }
    }
}
