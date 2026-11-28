package com.example

import android.Manifest
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.UpdateManager
import com.example.core.configureAppCheck
import com.example.data.model.AuvionyxModule
import com.example.ui.components.GlassNavigationRail
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.LumaMotion
import com.example.ui.screens.AodScreen
import com.example.ui.screens.BootSplashScreen
import com.example.ui.screens.DriveDashboardScreen
import com.example.ui.screens.HudScreen
import com.example.ui.screens.MapsScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AuvionyxTheme
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AuvionyxViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var appViewModel: AuvionyxViewModel? = null

    @Suppress("DEPRECATION")
    private fun requestHighestSupportedRefreshRate() {
        val display = windowManager.defaultDisplay
        val requestedRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            display.supportedModes.maxByOrNull { it.refreshRate }?.refreshRate ?: display.refreshRate
        } else {
            display.refreshRate
        }
        window.attributes = window.attributes.apply {
            preferredRefreshRate = requestedRate
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateManager.check(applicationContext)

        // Configure Firebase App Check with launch intent before rendering UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        requestHighestSupportedRefreshRate()

        setContent {
            val viewModel: AuvionyxViewModel = viewModel()
            appViewModel = viewModel
            LaunchedEffect(Unit) {
                viewModel.handleExternalMapsIntent(intent)
            }
            AuvionyxRootApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel?.handleExternalMapsIntent(intent)
    }
}

@Composable
fun AuvionyxRootApp(viewModel: AuvionyxViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showBootSplash by viewModel.showBootSplash.collectAsStateWithLifecycle()
    val activeModule by viewModel.activeModule.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val mapsPulse by viewModel.mapsPulse.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val updateState by UpdateManager.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val isHudMode = activeModule == AuvionyxModule.HUD
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    SideEffect {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            when {
                isHudMode || showBootSplash -> controller.hide(WindowInsetsCompat.Type.systemBars())
                isLandscape -> {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                }
                else -> controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dismissedUpdateVersion by remember { mutableStateOf<String?>(null) }
    var askedInitialLocation by remember { mutableStateOf(false) }
    var isMapsFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(activeModule) {
        if (activeModule != AuvionyxModule.MAPS) isMapsFullscreen = false
    }

    val startupLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsAndGps()
    }

    // Automatically refresh GPS, Google Maps Sync, and MediaSession when returning from Settings or Maps
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsAndGps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showBootSplash) {
        if (!showBootSplash && !permissions.locationGranted && !askedInitialLocation) {
            askedInitialLocation = true
            startupLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun openPhoneDialer() {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:")
        }
        runCatching { context.startActivity(dialIntent) }.onFailure {
            scope.launch {
                snackbarHostState.showSnackbar("No system phone dialer available on this head unit.")
            }
        }
    }

    AuvionyxTheme(
        adaptiveAccentColor = mediaState.currentTrack.adaptiveColor,
        useAdaptiveAlbumColors = settings.useAdaptiveAlbumColors,
        reducedMotion = settings.reducedMotion
    ) {
        if (showBootSplash) {
            BootSplashScreen(onBootComplete = { viewModel.finishBootSplash() })
            return@AuvionyxTheme
        }

        if (updateState.updateAvailable && dismissedUpdateVersion != updateState.latestVersion) {
            AlertDialog(
                onDismissRequest = { dismissedUpdateVersion = updateState.latestVersion },
                containerColor = Color(0xFF0A0D12),
                titleContentColor = GlassWhite,
                textContentColor = TextSecondary,
                title = { Text("LumaNAV update available · ${updateState.latestVersion}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(updateState.releaseNotes, style = MaterialTheme.typography.bodySmall)
                        if (updateState.downloading) {
                            LinearProgressIndicator(
                                progress = { updateState.downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${updateState.downloadProgress}% downloaded")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { UpdateManager.downloadAndInstall(context.applicationContext) },
                        enabled = !updateState.downloading
                    ) {
                        Text(if (updateState.downloading) "Downloading…" else "Download & Install")
                    }
                },
                dismissButton = {
                    Button(onClick = { dismissedUpdateVersion = updateState.latestVersion }) {
                        Text("Later")
                    }
                }
            )
        }

        if (activeModule == AuvionyxModule.AOD) {
            BackHandler { viewModel.navigateTo(AuvionyxModule.DRIVE) }
            AodScreen(
                aodStyle = settings.aodStyle,
                telemetry = telemetry,
                useMetricUnits = settings.useMetricUnits,
                currentLocation = mapsPulse.currentLatLon,
                routePoints = mapsPulse.routePolylineGeo,
                headingDegrees = telemetry.headingDegrees,
                isNavigating = mapsPulse.isNavigating,
                nowPlayingTitle = mediaState.currentTrack.title.takeIf { mediaState.isExternalSessionActive || mediaState.userStartedLocalPlayback },
                nowPlayingArtist = mediaState.currentTrack.artist,
                hasActiveMedia = mediaState.isExternalSessionActive || mediaState.userStartedLocalPlayback,
                onSelectAodStyle = { viewModel.setAodStyle(it) },
                onExitAod = { viewModel.navigateTo(AuvionyxModule.DRIVE) }
            )
            return@AuvionyxTheme
        }

        BackHandler(enabled = activeModule != AuvionyxModule.DRIVE) {
            viewModel.navigateTo(AuvionyxModule.DRIVE)
        }

        // Visual Reconstruction: 6-second inactivity timer for Immersive Idle Cockpit
        var lastInteractionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var isIdleCockpit by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            containerColor = ObsidianBlack,
            snackbarHost = { if (!isHudMode) SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack)
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.pressed }) {
                                    lastInteractionTimeMs = System.currentTimeMillis()
                                    if (isIdleCockpit) {
                                        isIdleCockpit = false
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Covers 1280x800 Blaupunkt across all DPI tiers (560dp+ landscape) as well as wide tablets
                val isLandscapeWide = (maxWidth > maxHeight) && (maxWidth >= 560.dp)

                // Automatically enter Immersive Idle Cockpit after 6s untouched on landscape Drive Cockpit
                LaunchedEffect(lastInteractionTimeMs, activeModule, isLandscapeWide) {
                    if (activeModule != AuvionyxModule.DRIVE || !isLandscapeWide) {
                        isIdleCockpit = false
                    } else {
                        val interactionAtStart = lastInteractionTimeMs
                        delay(6_000L)
                        if (
                            lastInteractionTimeMs == interactionAtStart &&
                            activeModule == AuvionyxModule.DRIVE &&
                            isLandscapeWide
                        ) {
                            isIdleCockpit = true
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLandscapeWide) {
                        val cockpitIdleActive = isIdleCockpit && activeModule == AuvionyxModule.DRIVE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    horizontal = if (cockpitIdleActive || isHudMode || isMapsFullscreen) 0.dp else 14.dp,
                                    vertical = if (cockpitIdleActive || isHudMode || isMapsFullscreen) 0.dp else 4.dp
                                ),
                            horizontalArrangement = Arrangement.spacedBy(if (cockpitIdleActive || isHudMode || isMapsFullscreen) 0.dp else 12.dp)
                        ) {
                            AnimatedVisibility(
                                visible = !cockpitIdleActive && !isHudMode && !isMapsFullscreen,
                                enter = fadeIn(tween(LumaMotion.StandardDurationMs, easing = LumaMotion.LaminarEasing)) +
                                    expandHorizontally(tween(LumaMotion.StandardDurationMs, easing = LumaMotion.LaminarEasing)) +
                                    slideInHorizontally(LumaMotion.LaminarIntOffsetSpec) { -it / 3 },
                                exit = fadeOut(tween(LumaMotion.CompactDurationMs, easing = LumaMotion.LaminarEasing)) +
                                    shrinkHorizontally(tween(LumaMotion.StandardDurationMs, easing = LumaMotion.LaminarEasing)) +
                                    slideOutHorizontally(LumaMotion.LaminarIntOffsetSpec) { -it / 3 }
                            ) {
                                GlassNavigationRail(
                                    activeModule = activeModule,
                                    onSelectModule = { viewModel.navigateTo(it) },
                                    modifier = Modifier.width(132.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                ModuleContentRouter(
                                    activeModule = activeModule,
                                    isLandscapeWide = true,
                                    isIdleCockpit = cockpitIdleActive,
                                    viewModel = viewModel,
                                    onMapsFullscreenChanged = { isMapsFullscreen = it },
                                    )
                            }
                        }

                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            ModuleContentRouter(
                                activeModule = activeModule,
                                isLandscapeWide = false,
                                isIdleCockpit = false,
                                viewModel = viewModel,
                                onMapsFullscreenChanged = { isMapsFullscreen = it },
                            )
                        }

                        if (!isHudMode) {
                        SmartphoneBottomNavBar(
                            activeModule = activeModule,
                            onSelectModule = { viewModel.navigateTo(it) }
                        )
                        }
                    }
                }


            }
        }
    }
}

@Composable
private fun ModuleContentRouter(
    activeModule: AuvionyxModule,
    isLandscapeWide: Boolean,
    isIdleCockpit: Boolean,
    viewModel: AuvionyxViewModel,
    onMapsFullscreenChanged: (Boolean) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val mapsPulse by viewModel.mapsPulse.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion

    AnimatedContent(
        targetState = activeModule,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(tween(LumaMotion.StandardDurationMs, easing = LumaMotion.LaminarEasing)) +
                    slideInHorizontally(animationSpec = LumaMotion.LaminarIntOffsetSpec) { it / 16 }) togetherWith
                    (fadeOut(tween(LumaMotion.CompactDurationMs, easing = LumaMotion.LaminarEasing)) +
                        slideOutHorizontally(animationSpec = LumaMotion.LaminarIntOffsetSpec) { -it / 16 })
            }
        },
        label = "moduleRouter"
    ) { module ->
        when (module) {
            AuvionyxModule.DRIVE -> DriveDashboardScreen(
                isLandscapeWide = isLandscapeWide,
                isIdleCockpit = isIdleCockpit,
                settings = settings,
                telemetry = telemetry,
                mediaState = mediaState,
                mapsPulse = mapsPulse,
                notificationAccessGranted = permissions.notificationAccessGranted,
                onSelectRatio = { viewModel.setDashboardRatio(it) },
                onSelectModule = { viewModel.navigateTo(it) },
                onSelectMarker = { viewModel.selectMarker(it) },
                onToggleTraffic = { viewModel.setTrafficOverlay(!settings.showTrafficOverlay) },
                onToggleSatellite = { viewModel.setSatelliteGrid(!settings.showSatelliteGrid) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNextTrack = { viewModel.nextTrack() },
                onPrevTrack = { viewModel.previousTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onSeek = { viewModel.seekTo(it) }
            )

            AuvionyxModule.MAPS -> MapsScreen(
                isLandscapeWide = isLandscapeWide,
                settings = settings,
                telemetry = telemetry,
                mapsPulse = mapsPulse,
                locationGranted = permissions.locationGranted,
                notificationAccessGranted = permissions.notificationAccessGranted,
                onPermissionUpdated = { viewModel.refreshPermissionsAndGps() },
                onConfirmDestination = { viewModel.confirmDestinationCandidate(it) },
                onSaveDestination = { viewModel.saveDestination(it) },
                onRemoveSavedPlace = { viewModel.removeSavedDestination(it) },
                onDestinationQueryChange = { viewModel.updateMapsDestinationQuery(it) },
                onSearchDestination = { viewModel.searchDestinationFromMaps(it) },
                onClearDestinationSearch = { viewModel.clearDestinationSearch() },
                onSelectMarker = { viewModel.selectMarker(it) },
                onSelectRoute = { viewModel.selectRoute(it) },
                onToggleNavigation = { viewModel.toggleNavigation() },
                onToggleTraffic = { viewModel.setTrafficOverlay(!settings.showTrafficOverlay) },
                onToggleSatellite = { viewModel.setSatelliteGrid(!settings.showSatelliteGrid) },
                onMapFullscreenChanged = onMapsFullscreenChanged
            )

            AuvionyxModule.MUSIC -> MusicScreen(
                isLandscapeWide = isLandscapeWide,
                settings = settings,
                mediaState = mediaState,
                onSelectSource = { viewModel.selectMediaSource(it) },
                onSelectTrack = { viewModel.selectTrack(it) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNextTrack = { viewModel.nextTrack() },
                onPrevTrack = { viewModel.previousTrack() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSeek = { viewModel.seekTo(it) },
                onToggleAdaptiveColors = { viewModel.setAdaptiveAlbumColors(it) }
            )

            AuvionyxModule.HUD -> HudScreen(
                isLandscapeWide = isLandscapeWide,
                hudConfig = settings.hudConfig,
                settings = settings,
                telemetry = telemetry,
                mediaState = mediaState,
                currentLocation = mapsPulse.currentLatLon,
                routePoints = mapsPulse.routePolylineGeo,
                headingDegrees = telemetry.headingDegrees,
                onSelectModule = { viewModel.navigateTo(it) },
                onUpdateHudConfig = { viewModel.updateHudConfig(it) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNextTrack = { viewModel.nextTrack() },
                onPrevTrack = { viewModel.previousTrack() }
            )

            AuvionyxModule.SETTINGS -> SettingsScreen(
                settings = settings,
                permissions = permissions,
                onRefreshPermissions = { viewModel.refreshPermissionsAndGps() },
                onSetAdaptiveAlbumColors = { viewModel.setAdaptiveAlbumColors(it) },
                onSetDaylightHighContrast = { viewModel.setDaylightHighContrast(it) },
                onSetReducedMotion = { viewModel.setReducedMotion(it) },
                onSetDashboardRatio = { viewModel.setDashboardRatio(it) },
                onSetHudConfig = { viewModel.updateHudConfig(it) },
                onSetMetricUnits = { viewModel.setMetricUnits(it) },
                onSetTrafficOverlay = { viewModel.setTrafficOverlay(it) },
                onSetSatelliteGrid = { viewModel.setSatelliteGrid(it) },
                onSetMediaSource = { viewModel.selectMediaSource(it) },
                onSetOptionalLauncher = { viewModel.setOptionalCarLauncher(it) }
            )

            AuvionyxModule.AOD -> Unit
        }
    }
}

@Composable
private fun SmartphoneBottomNavBar(
    activeModule: AuvionyxModule,
    onSelectModule: (AuvionyxModule) -> Unit
) {
    val navItems = listOf(
        Triple(AuvionyxModule.DRIVE, Icons.Outlined.Home, "Drive"),
        Triple(AuvionyxModule.MAPS, Icons.Outlined.Navigation, "Maps"),
        Triple(AuvionyxModule.MUSIC, Icons.Outlined.MusicNote, "Music"),
        Triple(AuvionyxModule.HUD, Icons.Outlined.Speed, "HUD"),
        Triple(AuvionyxModule.AOD, Icons.Outlined.Nightlight, "AOD"),
        Triple(AuvionyxModule.SETTINGS, Icons.Outlined.Settings, "Settings")
    )
    Surface(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color(0xE60B0F15),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.8.dp, GlassWhite.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (module, icon, label) ->
                val selected = activeModule == module
                LiquidGlassLens(
                    selected = selected,
                    onClick = { onSelectModule(module) },
                    cornerRadius = 18.dp,
                    testTag = "phone_nav_${module.route}",
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(imageVector = icon, contentDescription = label, tint = if (selected) GlassWhite else TextSecondary, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(text = label, style = MaterialTheme.typography.labelSmall, color = if (selected) GlassWhite else TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}
