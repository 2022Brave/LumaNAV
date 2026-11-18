package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.configureAppCheck
import com.example.data.model.AuvionyxModule
import com.example.data.voice.VoiceVisualState
import com.example.ui.components.FloatingVoiceMicOverlay
import com.example.ui.components.GlassBottomDock
import com.example.ui.components.GlassNavigationRail
import com.example.ui.components.GlassTopStatusBar
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.LivingFlow
import com.example.ui.screens.AodScreen
import com.example.ui.screens.AppsDrawerScreen
import com.example.ui.screens.BootSplashScreen
import com.example.ui.screens.DriveDashboardScreen
import com.example.ui.screens.HudScreen
import com.example.ui.screens.MapsScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PulseScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VehicleScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.theme.AuvionyxTheme
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AuvionyxViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var appViewModel: AuvionyxViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

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
    val syncDevices by viewModel.syncDevices.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var askedInitialLocation by remember { mutableStateOf(false) }

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

    val voiceAudioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionsAndGps()
        if (granted) {
            viewModel.activateVoiceListening()
        }
    }

    fun triggerVoiceCapture() {
        if (permissions.audioGranted) {
            viewModel.activateVoiceListening()
        } else {
            voiceAudioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

        if (activeModule == AuvionyxModule.AOD) {
            BackHandler { viewModel.navigateTo(AuvionyxModule.DRIVE) }
            AodScreen(
                aodStyle = settings.aodStyle,
                telemetry = telemetry,
                useMetricUnits = settings.useMetricUnits,
                onSelectAodStyle = { viewModel.setAodStyle(it) },
                onExitAod = { viewModel.navigateTo(AuvionyxModule.DRIVE) }
            )
            return@AuvionyxTheme
        }

        BackHandler(enabled = activeModule != AuvionyxModule.DRIVE) {
            viewModel.navigateTo(AuvionyxModule.DRIVE)
        }

        // Visual Reconstruction: 7-second inactivity timer for Immersive Idle Cockpit
        var lastInteractionTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var isIdleCockpit by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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

                // Automatically enter Immersive Idle Cockpit after 7s untouched on landscape Drive Cockpit
                LaunchedEffect(lastInteractionTimeMs, activeModule, isLandscapeWide, voiceState.visualState) {
                    if (activeModule == AuvionyxModule.DRIVE &&
                        isLandscapeWide &&
                        voiceState.visualState == VoiceVisualState.IDLE
                    ) {
                        delay(7_000L)
                        isIdleCockpit = true
                    } else {
                        isIdleCockpit = false
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    GlassTopStatusBar(
                        currentLocation = telemetry.currentLocationLabel,
                        weatherTempC = telemetry.weatherTempC,
                        isLiveGps = telemetry.isLiveGpsActive,
                        gpsAccuracyMeters = telemetry.gpsAccuracyMeters,
                        onOpenAod = { viewModel.navigateTo(AuvionyxModule.AOD) },
                        onOpenSettings = { viewModel.navigateTo(AuvionyxModule.SETTINGS) },
                        isIdleCockpit = isIdleCockpit && activeModule == AuvionyxModule.DRIVE && isLandscapeWide
                    )

                    if (isLandscapeWide) {
                        val cockpitIdleActive = isIdleCockpit && activeModule == AuvionyxModule.DRIVE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(
                                    horizontal = if (cockpitIdleActive) 0.dp else 14.dp,
                                    vertical = if (cockpitIdleActive) 0.dp else 4.dp
                                ),
                            horizontalArrangement = Arrangement.spacedBy(if (cockpitIdleActive) 0.dp else 12.dp)
                        ) {
                            AnimatedVisibility(
                                visible = !cockpitIdleActive,
                                enter = fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                    expandHorizontally(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                    slideInHorizontally(LivingFlow.LaminarIntOffsetSpec) { -it / 3 },
                                exit = fadeOut(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) +
                                    shrinkHorizontally(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                    slideOutHorizontally(LivingFlow.LaminarIntOffsetSpec) { -it / 3 }
                            ) {
                                GlassNavigationRail(
                                    activeModule = activeModule,
                                    onSelectModule = { viewModel.navigateTo(it) },
                                    modifier = Modifier.width(64.dp)
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
                                    onActivateVoiceCapture = ::triggerVoiceCapture,
                                    onOpenPhoneDialer = ::openPhoneDialer
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = !cockpitIdleActive,
                            enter = fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                expandVertically(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                slideInVertically(LivingFlow.LaminarIntOffsetSpec) { it / 3 },
                            exit = fadeOut(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) +
                                shrinkVertically(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                slideOutVertically(LivingFlow.LaminarIntOffsetSpec) { it / 3 }
                        ) {
                            GlassBottomDock(
                                activeModule = activeModule,
                                onSelectModule = { viewModel.navigateTo(it) },
                                onOpenPhoneDialer = ::openPhoneDialer,
                                onActivateVoice = ::triggerVoiceCapture,
                                isVoiceActive = voiceState.visualState != VoiceVisualState.IDLE
                            )
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
                                onActivateVoiceCapture = ::triggerVoiceCapture,
                                onOpenPhoneDialer = ::openPhoneDialer
                            )
                        }

                        SmartphoneBottomNavBar(
                            activeModule = activeModule,
                            onSelectModule = { viewModel.navigateTo(it) }
                        )
                    }
                }

                // Voice Overlay: In landscape, dock's center glowing Mic Orb launches Voice cleanly;
                // FloatingVoiceMicOverlay expands when active/listening/processing/confirming or in portrait.
                val showVoiceOverlay = activeModule != AuvionyxModule.VOICE &&
                    (!isLandscapeWide || voiceState.visualState != VoiceVisualState.IDLE)
                if (showVoiceOverlay) {
                    val alignment = if (voiceState.visualState == VoiceVisualState.IDLE) {
                        Alignment.BottomEnd
                    } else if (isLandscapeWide) {
                        Alignment.BottomCenter
                    } else {
                        Alignment.BottomCenter
                    }
                    FloatingVoiceMicOverlay(
                        voiceState = voiceState,
                        onActivateMic = ::triggerVoiceCapture,
                        onStopMic = { viewModel.stopVoiceListening() },
                        onDismiss = { viewModel.resetVoiceStateToIdle() },
                        onRequestAudioPermission = {
                            voiceAudioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onConfirmCandidate = { viewModel.confirmDestinationCandidate(it) },
                        onOpenVoiceConsole = { viewModel.navigateTo(AuvionyxModule.VOICE) },
                        modifier = Modifier
                            .align(alignment)
                            .padding(
                                start = 16.dp,
                                end = if (isLandscapeWide) 22.dp else 16.dp,
                                bottom = if (isLandscapeWide) 84.dp else 84.dp
                            )
                    )
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
    onActivateVoiceCapture: () -> Unit,
    onOpenPhoneDialer: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val mapsPulse by viewModel.mapsPulse.collectAsStateWithLifecycle()
    val syncDevices by viewModel.syncDevices.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion

    AnimatedContent(
        targetState = activeModule,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                    slideInHorizontally(animationSpec = LivingFlow.LaminarIntOffsetSpec) { it / 16 }) togetherWith
                    (fadeOut(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) +
                        slideOutHorizontally(animationSpec = LivingFlow.LaminarIntOffsetSpec) { -it / 16 })
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
                onActivateVoiceDestinationSearch = onActivateVoiceCapture,
                onSearchDestinationText = { viewModel.searchDestinationFromText(it) },
                onConfirmDestination = { viewModel.confirmDestinationCandidate(it) },
                onClearDestinationSearch = { viewModel.clearDestinationSearch() },
                onSelectMarker = { viewModel.selectMarker(it) },
                onSelectRoute = { viewModel.selectRoute(it) },
                onToggleNavigation = { viewModel.toggleNavigation() },
                onToggleTraffic = { viewModel.setTrafficOverlay(!settings.showTrafficOverlay) },
                onToggleSatellite = { viewModel.setSatelliteGrid(!settings.showSatelliteGrid) }
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
                mapsPulse = mapsPulse,
                mediaState = mediaState,
                onSelectModule = { viewModel.navigateTo(it) },
                onSelectMarker = { viewModel.selectMarker(it) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNextTrack = { viewModel.nextTrack() },
                onPrevTrack = { viewModel.previousTrack() }
            )

            AuvionyxModule.PULSE -> PulseScreen(
                isLandscapeWide = isLandscapeWide,
                settings = settings,
                telemetry = telemetry,
                mapsPulse = mapsPulse,
                onSelectPulseTab = { viewModel.selectPulseTab(it) },
                onSelectMarker = { viewModel.selectMarker(it) },
                onToggleTraffic = { viewModel.setTrafficOverlay(!settings.showTrafficOverlay) },
                onToggleSatellite = { viewModel.setSatelliteGrid(!settings.showSatelliteGrid) },
                onReportIncident = { title, corridor, sev ->
                    viewModel.reportTrafficIncident(title, corridor, sev)
                }
            )

            AuvionyxModule.VOICE -> VoiceScreen(
                isLandscapeWide = isLandscapeWide,
                voiceState = voiceState,
                onPermissionUpdated = { viewModel.refreshPermissionsAndGps() },
                onActivateMic = { viewModel.activateVoiceListening() },
                onStopMic = { viewModel.stopVoiceListening() },
                onResetToIdle = { viewModel.resetVoiceStateToIdle() },
                onConfirmCandidate = { viewModel.confirmDestinationCandidate(it) },
                onExecuteVoiceCommand = { viewModel.executeVoiceUtterance(it) },
                onToggleTts = { viewModel.setTtsConfirmationsEnabled(it) },
                onToggleWakeWord = { viewModel.setWakeWordEnabled(it) },
                onToggleGeminiInterpreter = { viewModel.setGeminiComplexFallbackEnabled(it) }
            )

            AuvionyxModule.VEHICLE -> VehicleScreen(
                isLandscapeWide = isLandscapeWide,
                telemetry = telemetry,
                onAdjustDriverTemp = { viewModel.adjustDriverTemp(it) },
                onAdjustPassengerTemp = { viewModel.adjustPassengerTemp(it) },
                onSelectDriveMode = { viewModel.setDriveMode(it) }
            )

            AuvionyxModule.SETTINGS -> SettingsScreen(
                settings = settings,
                syncDevices = syncDevices,
                permissions = permissions,
                onRefreshPermissions = { viewModel.refreshPermissionsAndGps() },
                onSetAdaptiveAlbumColors = { viewModel.setAdaptiveAlbumColors(it) },
                onSetReducedMotion = { viewModel.setReducedMotion(it) },
                onSetDashboardRatio = { viewModel.setDashboardRatio(it) },
                onSetHudLayout = { viewModel.updateHudConfig(settings.hudConfig.copy(layoutMode = it)) },
                onOpenHudDesigner = { viewModel.navigateTo(AuvionyxModule.HUD) },
                onSetMetricUnits = { viewModel.setMetricUnits(it) },
                onSetTrafficOverlay = { viewModel.setTrafficOverlay(it) },
                onSetSatelliteGrid = { viewModel.setSatelliteGrid(it) },
                onSetDemoSimulation = { viewModel.setDemoSimulation(it) },
                onSetMediaSource = { viewModel.selectMediaSource(it) },
                onSetOptionalLauncher = { viewModel.setOptionalCarLauncher(it) },
                onToggleDeviceSync = { viewModel.toggleDeviceSync(it) }
            )

            AuvionyxModule.APPS -> AppsDrawerScreen(
                isLandscapeWide = isLandscapeWide,
                onSelectModule = { viewModel.navigateTo(it) },
                onOpenPhoneDialer = onOpenPhoneDialer
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
        Triple(AuvionyxModule.VOICE, Icons.Outlined.Mic, "Voice"),
        Triple(AuvionyxModule.APPS, Icons.Outlined.Apps, "Apps")
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color(0xFF0B0F15),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.8.dp, GlassWhite.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (module, icon, label) ->
                val selected = activeModule == module
                LiquidGlassLens(
                    selected = selected,
                    onClick = { onSelectModule(module) },
                    cornerRadius = 18.dp,
                    testTag = "phone_nav_${module.route}",
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) GlassWhite else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) GlassWhite else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
