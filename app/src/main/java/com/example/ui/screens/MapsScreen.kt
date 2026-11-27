package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.data.model.DestinationCandidate
import com.example.data.model.MapPlaceMarker
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.DraggableLaminarSheet
import com.example.ui.components.GlassCard
import com.example.ui.components.NativeMapLibreView
import com.example.ui.components.LivingFlow
import com.example.ui.components.ServiceStateBadge
import com.example.ui.components.TrafficPulseBadge
import com.example.ui.components.photonLuminousTouch
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.components.formatNavigationDistance
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
    onSaveDestination: (DestinationCandidate) -> Unit,
    onRemoveSavedPlace: (String) -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onSearchDestination: (String) -> Unit,
    onClearDestinationSearch: () -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onSelectRoute: (String) -> Unit,
    onToggleNavigation: () -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    onMapFullscreenChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapFullscreen by remember { mutableStateOf(false) }
    var showSavedPlaces by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var imeWasVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottom) {
        val visible = imeBottom > 0
        if (imeWasVisible && !visible) {
            searchActive = false
            focusManager.clearFocus()
        }
        imeWasVisible = visible
    }
    LaunchedEffect(mapFullscreen) { onMapFullscreenChanged(mapFullscreen) }
    val mapStyles = listOf(
        "DARK" to "https://tiles.openfreemap.org/styles/dark",
        "WIREFRAME" to "https://tiles.openfreemap.org/styles/dark",
        "STREETS" to "https://tiles.openfreemap.org/styles/liberty"
    )
    var mapStyleIndex by remember { mutableStateOf(0) }
    val activeMapStyle = mapStyles[mapStyleIndex]
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionUpdated()
    }

    val routeContent: @Composable (Modifier) -> Unit = { contentMod ->
        Column(
            modifier = contentMod.animateContentSize(
                animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLandscapeWide) {
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
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showSavedPlaces = true },
                    color = DeepBlue.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("maps_saved_places_chip")
                ) {
                    Text("SAVED · ${mapsPulse.savedPlaces.size}", color = accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
                listOf("HOME", "WORK").forEach { quickPlace ->
                    Surface(
                        onClick = {
                            val saved = mapsPulse.savedPlaces.firstOrNull { place ->
                                place.name.equals(quickPlace, ignoreCase = true) ||
                                    place.category.equals(quickPlace, ignoreCase = true)
                            }
                            if (saved != null) {
                                onConfirmDestination(saved.toDestinationCandidate())
                            } else {
                                showSavedPlaces = true
                            }
                        },
                        color = Color(0xFF0E1522),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.12f))
                    ) {
                        Text(quickPlace, color = GlassWhite, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                    }
                }
                Surface(
                    onClick = {
                        onDestinationQueryChange("fuel station")
                        onSearchDestination("fuel station")
                    },
                    color = Color(0xFF0E1522),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.12f))
                ) {
                    Text("FUEL", color = GlassWhite, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
                Surface(
                    onClick = {
                        val recent = mapsPulse.confirmedDestination ?: mapsPulse.destinationCandidates.firstOrNull()
                        if (recent != null) onConfirmDestination(recent) else showSavedPlaces = true
                    },
                    color = Color(0xFF0E1522),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.12f))
                ) {
                    Text("RECENT", color = GlassWhite, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
                Spacer(Modifier.width(16.dp))
            }
            Text(
                text = "Map data © OpenStreetMap contributors · Tiles by OpenFreeMap · Search by Photon",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
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
                                    if (target.latitude != null && target.longitude != null) {
                                        IconButton(onClick = { onSaveDestination(target) }) {
                                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save destination", tint = accent)
                                        }
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
                                    ServiceStateBadge(state = status, labelPrefix = "Place Search")
                                    Text(
                                        text = mapsPulse.destinationSearchMessage
                                            ?: "No matching places found, or place search is temporarily unavailable.",
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
            if (!searchActive) AnimatedContent(
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
                                    if (candidate.latitude != null && candidate.longitude != null) {
                                        IconButton(onClick = { onSaveDestination(candidate) }) {
                                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save place", tint = accent)
                                        }
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
                                        text = "${route.viaRoad} · ${route.durationMin} min (${formatNavigationDistance(route.distanceKm)})",
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

    if (showSavedPlaces) {
        AlertDialog(
            onDismissRequest = { showSavedPlaces = false },
            title = { Text("Saved Places", color = GlassWhite) },
            text = {
                if (mapsPulse.savedPlaces.isEmpty()) {
                    Text("No saved places yet. Search for a destination and tap the bookmark icon to save it.", color = TextSecondary)
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mapsPulse.savedPlaces.forEach { place ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .photonLuminousTouch(pressScale = 0.97f, onClick = {
                                            onConfirmDestination(place.toDestinationCandidate())
                                            showSavedPlaces = false
                                        })
                                        .padding(10.dp)
                                ) {
                                    Text(place.name, color = GlassWhite, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(place.addressSummary, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            onSaveDestination(place.toDestinationCandidate().copy(name = "Home", category = "Home"))
                                            showSavedPlaces = false
                                        },
                                        color = DeepBlue.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("SET HOME", color = accent, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                    }
                                    Surface(
                                        onClick = {
                                            onSaveDestination(place.toDestinationCandidate().copy(name = "Work", category = "Work"))
                                            showSavedPlaces = false
                                        },
                                        color = DeepBlue.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("SET WORK", color = accent, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                    }
                                    IconButton(onClick = { onRemoveSavedPlace(place.id) }) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove saved place", tint = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSavedPlaces = false }) { Text("Close") }
            },
            containerColor = Color(0xFF101722),
            titleContentColor = GlassWhite,
            textContentColor = TextSecondary
        )
    }

    if (isLandscapeWide && mapFullscreen) {
        Box(modifier = modifier.fillMaxSize()) {
            NativeMapLibreView(
                currentLocation = mapsPulse.currentLatLon,
                routePoints = mapsPulse.routePolylineGeo,
                headingDegrees = telemetry.headingDegrees,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
                styleUri = activeMapStyle.second,
                    show3DBuildings = activeMapStyle.first != "WIREFRAME",
                wireframeMode = activeMapStyle.first == "WIREFRAME"
            )
            Surface(
                onClick = { mapStyleIndex = (mapStyleIndex + 1) % mapStyles.size },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(14.dp),
                color = Color(0xE609111B), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) { Text("MAP · ${activeMapStyle.first}", color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall) }
            Surface(
                onClick = { mapFullscreen = false },
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                color = Color(0xE609111B),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) { Text("EXIT FULLSCREEN", color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall) }
        }
    } else if (isLandscapeWide) {
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
            Box(modifier = Modifier.weight(1f).fillMaxHeight().zIndex(0f)) {
                NativeMapLibreView(
                    currentLocation = mapsPulse.currentLatLon,
                    routePoints = mapsPulse.routePolylineGeo,
                    headingDegrees = telemetry.headingDegrees,
                    accent = accent,
                    modifier = Modifier.fillMaxSize(),
                    styleUri = activeMapStyle.second,
                    show3DBuildings = activeMapStyle.first != "WIREFRAME",
                    wireframeMode = activeMapStyle.first == "WIREFRAME"
                )
                Surface(
                    onClick = { mapStyleIndex = (mapStyleIndex + 1) % mapStyles.size },
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(14.dp),
                    color = Color(0xE609111B), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                ) { Text("MAP · ${activeMapStyle.first}", color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall) }
                Surface(
                    onClick = { mapFullscreen = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                    color = Color(0xE609111B), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                ) { Text("FULLSCREEN", color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall) }
                if (mapsPulse.confirmedDestination != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp),
                        color = Color(0xE609111B),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                            Text("NAVIGATING TO", style = MaterialTheme.typography.labelSmall, color = accent)
                            Text(mapsPulse.confirmedDestination.name, style = MaterialTheme.typography.titleSmall, color = GlassWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    } else {
        // Smartphone Portrait: Full-bleed interactive map beneath a velocity-responsive Draggable Laminar Sheet
        Box(modifier = modifier.fillMaxSize()) {
            NativeMapLibreView(
                currentLocation = mapsPulse.currentLatLon,
                routePoints = mapsPulse.routePolylineGeo,
                headingDegrees = telemetry.headingDegrees,
                accent = accent,
                modifier = Modifier.fillMaxSize().zIndex(0f),
                styleUri = activeMapStyle.second,
                show3DBuildings = activeMapStyle.first != "WIREFRAME",
                wireframeMode = activeMapStyle.first == "WIREFRAME"
            )
            if (!isLandscapeWide) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().zIndex(4f)
            ) {
                val searchBarY by animateDpAsState(
                    targetValue = when {
                        searchActive -> 0.dp
                        mapsPulse.confirmedDestination != null -> (maxHeight - 318.dp).coerceAtLeast(80.dp)
                        else -> (maxHeight - 152.dp).coerceAtLeast(0.dp)
                    },
                    animationSpec = tween(
                        durationMillis = if (reducedMotion) 0 else LivingFlow.StandardDurationMs,
                        easing = LivingFlow.LaminarEasing
                    ),
                    label = "mapsFloatingSearchPosition"
                )
                LaunchedEffect(searchActive) {
                    if (searchActive) focusRequester.requestFocus()
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = searchBarY)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .statusBarsPadding()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mapsPulse.confirmedDestination != null && !searchActive) {
                        Surface(
                            onClick = { searchActive = true },
                            modifier = Modifier.align(Alignment.End),
                            color = Color(0xF20A1019),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                                Text("SEARCH DESTINATION", color = GlassWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mapsPulse.destinationQuery,
                            onValueChange = onDestinationQueryChange,
                            modifier = Modifier.weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) searchActive = true }
                                .testTag("maps_destination_search"),
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
                            keyboardActions = KeyboardActions(onSearch = {
                                onSearchDestination(mapsPulse.destinationQuery)
                                keyboardController?.hide()
                                searchActive = false
                                focusManager.clearFocus()
                            })
                        )
                        IconButton(
                            onClick = {
                                onSearchDestination(mapsPulse.destinationQuery)
                                keyboardController?.hide()
                                searchActive = false
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.testTag("maps_search_submit")
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = accent)
                        }
                    }
                    }
                    if (searchActive && mapsPulse.destinationCandidates.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                            color = Color(0xF20A1019),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(mapsPulse.destinationCandidates, key = { it.id }) { candidate ->
                                    Surface(
                                        color = Color(0xFF111B28),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                            .photonLuminousTouch(pressScale = 0.97f, onClick = {
                                                onConfirmDestination(candidate)
                                                keyboardController?.hide()
                                                searchActive = false
                                                focusManager.clearFocus()
                                            })
                                    ) {
                                        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                                            Text(candidate.name, color = GlassWhite, style = MaterialTheme.typography.titleSmall)
                                            Text(candidate.addressSummary, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            if (!searchActive) Surface(
                onClick = { mapStyleIndex = (mapStyleIndex + 1) % mapStyles.size },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(14.dp).zIndex(2f),
                color = Color(0xE609111B), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) { Text("MAP · ${activeMapStyle.first}", color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall) }
            Surface(
                onClick = { mapFullscreen = !mapFullscreen },
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).zIndex(2f),
                color = Color(0xE609111B), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
            ) { Text(if (mapFullscreen) "EXIT FULLSCREEN" else "FULLSCREEN", color = accent, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall) }
            if (mapsPulse.confirmedDestination != null && !searchActive) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp).zIndex(0.5f),
                    color = Color(0xE609111B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.48f))
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text("NAVIGATING TO", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(mapsPulse.confirmedDestination.name, style = MaterialTheme.typography.titleSmall, color = GlassWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (!mapFullscreen) DraggableLaminarSheet(
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
