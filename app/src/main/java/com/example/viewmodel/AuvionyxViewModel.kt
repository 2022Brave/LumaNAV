package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.GoogleMapsLiveNavigation
import com.example.core.LiveGoogleMapsSyncBridge
import com.example.data.ai.AuvionyxAiRepository
import com.example.data.ai.ParsedNaturalVoiceIntent
import com.example.data.maps.DestinationSearchRepository
import com.example.data.maps.OffRouteGuard
import com.example.data.maps.RouteMatcher
import com.example.data.media.MediaSessionBridge
import com.example.data.media.MediaSessionBridgeStatus
import com.example.data.model.AodStyle
import com.example.data.model.AuvionyxModule
import com.example.data.model.AuvionyxSampleData
import com.example.data.model.DashboardSplitRatio
import com.example.data.model.DestinationCandidate
import com.example.data.model.DestinationSearchOutcome
import com.example.data.model.DriveMode
import com.example.data.model.HudConfig
import com.example.data.model.HudLayoutMode
import com.example.data.model.MapPlaceMarker
import com.example.data.model.MediaAppSource
import com.example.data.model.MediaTrack
import com.example.data.model.RouteAlternative
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.SyncDeviceProfile
import com.example.data.model.TrafficIncident
import com.example.data.model.TrafficSeverity
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxPreferencesRepository
import com.example.data.preferences.AuvionyxSettingsState
import com.example.data.sensor.GpsReadingPolicy
import com.example.data.sensor.TelemetryProvider
import com.example.data.voice.AuvionyxVoiceEngine
import com.example.data.voice.NativeVoiceBridgeStatus
import com.example.data.voice.ParsedVoiceCommand
import com.example.data.voice.PendingDestinationConfirmation
import com.example.data.voice.VoiceCommandAction
import com.example.data.voice.VoiceCommandCategory
import com.example.data.voice.VoiceCommandOutcome
import com.example.data.voice.VoiceCommandParser
import com.example.data.voice.VoiceEngineCallbacks
import com.example.data.voice.VoiceUiState
import com.example.data.voice.VoiceVisualState
import com.example.ui.theme.ElectricCyan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private const val DEFAULT_ACCENT_ARGB = -16721419 // 0xFF00D9F5 (Electric Cyan)

enum class PulseFeedTab(val label: String) {
    LIVE("Corridors"),
    FORECAST("Forecast"),
    INCIDENTS("Incidents")
}

data class MediaPlayerState(
    val tracks: List<MediaTrack> = AuvionyxSampleData.tracks,
    val currentTrackIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionSec: Int = 0,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val selectedSource: MediaAppSource = MediaAppSource.SPOTIFY,
    val activeSessionSource: MediaAppSource = MediaAppSource.LASTWAVE,
    val bridgeStatus: MediaSessionBridgeStatus = MediaSessionBridgeStatus(),
    val volumePercent: Int = 68,
    val isExternalSessionActive: Boolean = false,
    val externalTitle: String? = null,
    val externalArtist: String? = null,
    val externalAlbum: String? = null,
    val externalDurationSec: Int? = null,
    val externalPositionSec: Int? = null,
    val externalAppLabel: String? = null,
    val externalArtwork: Bitmap? = null,
    val externalAccentArgb: Int = DEFAULT_ACCENT_ARGB,
    val userStartedLocalPlayback: Boolean = false
) {
    val currentTrack: MediaTrack
        get() = if (isExternalSessionActive && !externalTitle.isNullOrBlank()) {
            MediaTrack(
                id = "external_live",
                title = externalTitle,
                artist = externalArtist.orEmpty().ifBlank { activeSessionSource.label },
                album = externalAlbum.orEmpty(),
                durationSec = (externalDurationSec ?: 1).coerceAtLeast(1),
                source = activeSessionSource,
                adaptiveColor = androidx.compose.ui.graphics.Color(externalAccentArgb),
                artVariant = 0
            )
        } else if (userStartedLocalPlayback) {
            tracks.getOrElse(currentTrackIndex) { tracks.first() }
        } else {
            MediaTrack(
                id = "idle_none",
                title = "No Active Media",
                artist = if (bridgeStatus.hasNotificationListenerAccess) {
                    "Play Spotify, YouTube Music, or pick a track"
                } else {
                    "Enable Media Sync to mirror active player"
                },
                album = "",
                durationSec = 1,
                source = activeSessionSource,
                adaptiveColor = ElectricCyan,
                artVariant = 0
            )
        }

    val effectivePositionSec: Int
        get() = if (isExternalSessionActive && externalPositionSec != null) {
            externalPositionSec
        } else {
            positionSec
        }
}

/**
 * Pure media transport helper verifying playback actions operate on the active MediaSession
 * without ever mutating the user's separately selected source tab/filter.
 */
object MediaPlaybackReducer {
    fun nextTrack(state: MediaPlayerState): MediaPlayerState {
        val nextIdx = if (state.isShuffle && state.tracks.size > 1) {
            (state.currentTrackIndex + 2) % state.tracks.size
        } else {
            (state.currentTrackIndex + 1) % state.tracks.size
        }
        return state.copy(
            currentTrackIndex = nextIdx,
            positionSec = 0,
            userStartedLocalPlayback = true,
            activeSessionSource = state.tracks[nextIdx].source
            // selectedSource is intentionally preserved
        )
    }

    fun previousTrack(state: MediaPlayerState): MediaPlayerState {
        return if (state.positionSec > 5) {
            state.copy(positionSec = 0)
        } else {
            val prevIdx = if (state.currentTrackIndex - 1 < 0) {
                state.tracks.lastIndex
            } else {
                state.currentTrackIndex - 1
            }
            state.copy(
                currentTrackIndex = prevIdx,
                positionSec = 0,
                userStartedLocalPlayback = true,
                activeSessionSource = state.tracks[prevIdx].source
                // selectedSource is intentionally preserved
            )
        }
    }

    fun selectTrack(state: MediaPlayerState, index: Int): MediaPlayerState {
        val safe = index.coerceIn(0, state.tracks.lastIndex)
        return state.copy(
            currentTrackIndex = safe,
            positionSec = 0,
            isPlaying = true,
            userStartedLocalPlayback = true,
            isExternalSessionActive = false,
            activeSessionSource = state.tracks[safe].source
            // selectedSource is intentionally preserved
        )
    }

    fun selectSource(state: MediaPlayerState, source: MediaAppSource): MediaPlayerState {
        return state.copy(selectedSource = source)
    }
}

data class MapsAndPulseState(
    val destinationQuery: String = "",
    val destinationSearchState: ServiceAvailabilityState? = null,
    val destinationSearchMessage: String? = null,
    val destinationCandidates: List<DestinationCandidate> = emptyList(),
    val confirmedDestination: DestinationCandidate? = null,
    val routes: List<RouteAlternative> = emptyList(),
    val markers: List<MapPlaceMarker> = emptyList(),
    val selectedMarkerId: String? = null,
    val incidents: List<TrafficIncident> = emptyList(),
    val activePulseTab: PulseFeedTab = PulseFeedTab.LIVE,
    val isNavigating: Boolean = false,
    val isExternalGoogleMapsSyncActive: Boolean = false,
    val navigationSourceLabel: String? = null,
    val currentLatLon: Pair<Double, Double>? = null,
    val destinationLatLon: Pair<Double, Double>? = null,
    val routePolylineGeo: List<Pair<Double, Double>> = emptyList(),
    val gpsServiceState: ServiceAvailabilityState = ServiceAvailabilityState.PERMISSION_DENIED,
    val mapsServiceState: ServiceAvailabilityState = ServiceAvailabilityState.API_UNAVAILABLE
)

data class PermissionsSnapshot(
    val locationGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val notificationAccessGranted: Boolean = false
)

class AuvionyxViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = AuvionyxPreferencesRepository(application)
    private val aiRepo = AuvionyxAiRepository()
    private val telemetryProvider = TelemetryProvider(application)
    private val destinationSearchRepo = DestinationSearchRepository(application)
    private val offRouteGuard = OffRouteGuard()
    private val mediaSessionBridge = MediaSessionBridge(application)

    val settings: StateFlow<AuvionyxSettingsState> = prefsRepo.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AuvionyxSettingsState()
        )

    private val _showBootSplash = MutableStateFlow(true)
    val showBootSplash: StateFlow<Boolean> = _showBootSplash.asStateFlow()

    private val _activeModule = MutableStateFlow(AuvionyxModule.DRIVE)
    val activeModule: StateFlow<AuvionyxModule> = _activeModule.asStateFlow()

    private val _mediaState = MutableStateFlow(
        MediaPlayerState(bridgeStatus = mediaSessionBridge.getBridgeStatus())
    )
    val mediaState: StateFlow<MediaPlayerState> = _mediaState.asStateFlow()

    private val _telemetry = MutableStateFlow(VehicleTelemetry())
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    private val _mapsPulse = MutableStateFlow(MapsAndPulseState())
    val mapsPulse: StateFlow<MapsAndPulseState> = _mapsPulse.asStateFlow()

    private val _syncDevices = MutableStateFlow(AuvionyxSampleData.syncDevices)
    val syncDevices: StateFlow<List<SyncDeviceProfile>> = _syncDevices.asStateFlow()

    private val _permissions = MutableStateFlow(
        PermissionsSnapshot(
            locationGranted = telemetryProvider.hasLocationPermission(),
            audioGranted = telemetryProvider.hasAudioPermission(),
            notificationAccessGranted = LiveGoogleMapsSyncBridge.isNotificationAccessEnabled(application)
        )
    )
    val permissions: StateFlow<PermissionsSnapshot> = _permissions.asStateFlow()

    private val _voiceState = MutableStateFlow(VoiceUiState())
    val voiceState: StateFlow<VoiceUiState> = _voiceState.asStateFlow()

    private val voiceEngine = AuvionyxVoiceEngine(
        context = application,
        callbacks = object : VoiceEngineCallbacks {
            override fun onPartialTranscript(text: String) {
                _voiceState.update {
                    it.copy(
                        visualState = VoiceVisualState.LISTENING,
                        partialTranscript = text,
                        errorTitle = null,
                        errorDetail = null,
                        errorServiceState = null,
                        needsAudioPermission = false
                    )
                }
            }

            override fun onRmsChanged(normalizedLevel: Float) {
                _voiceState.update { it.copy(rmsNormalized = normalizedLevel) }
            }

            override fun onFinalTranscript(text: String) {
                executeVoiceUtterance(text)
            }

            override fun onRecognitionError(
                title: String,
                detail: String,
                serviceState: ServiceAvailabilityState,
                missingPermission: Boolean
            ) {
                _voiceState.update {
                    it.copy(
                        visualState = VoiceVisualState.ERROR,
                        errorTitle = title,
                        errorDetail = detail,
                        errorServiceState = serviceState,
                        needsAudioPermission = missingPermission,
                        rmsNormalized = 0.15f
                    )
                }
            }

            override fun onTtsStatusChanged(ready: Boolean) {
                _voiceState.update {
                    it.copy(bridgeStatus = it.bridgeStatus.copy(textToSpeechReady = ready))
                }
            }
        }
    )

    private var gpsJob: Job? = null
    private var voiceJob: Job? = null
    private var routeJob: Job? = null
    private var suggestionJob: Job? = null
    private var lastReverseGeocodeLatLon: Pair<Double, Double>? = null
    private var lastWeatherFetchMs: Long = 0L

    init {
        observeLiveGoogleMapsNotifications()
        startMediaTicker()
        observeDemoSimulationSetting()
        refreshDeviceBattery()
        refreshPermissionsAndGps()
    }

    fun finishBootSplash() {
        _showBootSplash.value = false
    }

    fun navigateTo(module: AuvionyxModule) {
        _activeModule.value = module
    }

    private fun refreshDeviceBattery() {
        val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (pct in 0..100) {
            _telemetry.update { it.copy(batteryPercent = pct) }
        }
    }

    private fun observeLiveGoogleMapsNotifications() {
        viewModelScope.launch {
            LiveGoogleMapsSyncBridge.activeNavigation.collect { liveNav ->
                applyGoogleMapsLiveNavigation(liveNav)
            }
        }
        viewModelScope.launch {
            LiveGoogleMapsSyncBridge.mediaUpdatePulse.collect {
                syncExternalMediaSnapshot()
            }
        }
    }

    private fun applyGoogleMapsLiveNavigation(liveNav: GoogleMapsLiveNavigation?) {
        if (liveNav != null) {
            _telemetry.update { current ->
                current.copy(
                    nextTurnDistanceLabel = liveNav.nextTurnDistanceLabel,
                    nextTurnRoad = liveNav.headline,
                    nextTurnInstruction = liveNav.turnInstruction,
                    remainingMinutes = liveNav.remainingMinutes ?: current.remainingMinutes,
                    remainingDistanceKm = liveNav.remainingDistanceKm ?: current.remainingDistanceKm,
                    etaTimeLabel = liveNav.etaTimeLabel ?: current.etaTimeLabel
                )
            }
            _mapsPulse.update { state ->
                val updatedRoutes = listOf(
                    RouteAlternative(
                        id = "external_live_route",
                        name = liveNav.headline,
                        viaRoad = liveNav.turnInstruction,
                        durationMin = liveNav.remainingMinutes ?: 0,
                        distanceKm = liveNav.remainingDistanceKm ?: 0f,
                        etaLabel = liveNav.etaTimeLabel ?: "--:--",
                        trafficState = TrafficSeverity.CLEAR,
                        isSelected = true
                    )
                )
                state.copy(
                    destinationQuery = liveNav.headline,
                    isNavigating = true,
                    isExternalGoogleMapsSyncActive = true,
                    navigationSourceLabel = "${liveNav.providerLabel} Sync",
                    mapsServiceState = ServiceAvailabilityState.LIVE,
                    routes = updatedRoutes,
                    destinationSearchMessage = liveNav.remainingSummary.ifBlank { liveNav.turnInstruction }
                )
            }
        } else if (_mapsPulse.value.isExternalGoogleMapsSyncActive) {
            // External navigation provider stopped publishing an active navigation notification
            val hasCustomRoute = _mapsPulse.value.confirmedDestination != null
            _mapsPulse.update { state ->
                state.copy(
                    isExternalGoogleMapsSyncActive = false,
                    isNavigating = hasCustomRoute,
                    navigationSourceLabel = if (hasCustomRoute) state.navigationSourceLabel else null,
                    routes = if (hasCustomRoute) state.routes else emptyList(),
                    destinationQuery = if (hasCustomRoute) state.destinationQuery else ""
                )
            }
            if (!hasCustomRoute) {
                _telemetry.update {
                    it.copy(
                        nextTurnDistanceLabel = "--",
                        nextTurnRoad = "No Active Route",
                        nextTurnInstruction = "Start navigation in Google Maps, Waze or Mappls",
                        etaTimeLabel = "--:--",
                        remainingMinutes = 0,
                        remainingDistanceKm = 0f,
                        journeyProgress = 0f
                    )
                }
            }
        }
    }

    private fun syncExternalMediaSnapshot() {
        val status = mediaSessionBridge.getBridgeStatus()
        val snap = mediaSessionBridge.getExternalMediaSnapshot()
        if (snap != null) {
            _mediaState.update { state ->
                state.copy(
                    isPlaying = snap.isPlaying,
                    positionSec = snap.positionSec,
                    activeSessionSource = snap.source,
                    bridgeStatus = status,
                    isExternalSessionActive = true,
                    externalTitle = snap.title,
                    externalArtist = snap.artist,
                    externalAlbum = snap.album,
                    externalDurationSec = snap.durationSec,
                    externalPositionSec = snap.positionSec,
                    externalAppLabel = snap.appLabel,
                    externalArtwork = snap.artwork,
                    externalAccentArgb = snap.artworkAccentArgb
                )
            }
        } else {
            _mediaState.update { state ->
                state.copy(
                    bridgeStatus = status,
                    isExternalSessionActive = false,
                    externalTitle = null,
                    externalArtist = null,
                    externalAlbum = null,
                    externalDurationSec = null,
                    externalPositionSec = null,
                    externalAppLabel = null,
                    externalArtwork = null,
                    externalAccentArgb = DEFAULT_ACCENT_ARGB
                )
            }
        }
    }

    fun handleExternalMapsIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return

        val dataStr = intent.dataString
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val parsed = DestinationSearchRepository.parseExternalMapsIntent(dataStr, sharedText) ?: return

        if (parsed.latitude != null && parsed.longitude != null) {
            val candidate = DestinationCandidate(
                id = "intent_${parsed.latitude}_${parsed.longitude}",
                name = parsed.queryLabel,
                addressSummary = String.format(Locale.US, "%.4f, %.4f", parsed.latitude, parsed.longitude),
                latitude = parsed.latitude,
                longitude = parsed.longitude,
                category = "Google Maps Target",
                isDemo = false
            )
            confirmDestinationCandidate(candidate)
        } else if (parsed.queryLabel.isNotBlank()) {
            performRealDestinationSearch(
                query = parsed.queryLabel,
                action = VoiceCommandAction.NAVIGATE_TO,
                recognizedPhrase = parsed.queryLabel,
                usedGemini = false
            )
        }
    }

    fun refreshPermissionsAndGps() {
        val app = getApplication<Application>()
        val locGranted = telemetryProvider.hasLocationPermission()
        val audGranted = telemetryProvider.hasAudioPermission()
        val notifGranted = LiveGoogleMapsSyncBridge.isNotificationAccessEnabled(app)
        val speechAvailable = voiceEngine.isSpeechRecognitionAvailable()
        val geocoderAvailable = destinationSearchRepo.isGeocoderAvailable()
        val networkOnline = destinationSearchRepo.isNetworkOnline()
        val bridge = mediaSessionBridge.getBridgeStatus()

        _permissions.value = PermissionsSnapshot(
            locationGranted = locGranted,
            audioGranted = audGranted,
            notificationAccessGranted = notifGranted
        )
        refreshDeviceBattery()
        syncExternalMediaSnapshot()
        applyGoogleMapsLiveNavigation(LiveGoogleMapsSyncBridge.activeNavigation.value)

        val speechState = when {
            !audGranted -> ServiceAvailabilityState.PERMISSION_DENIED
            !speechAvailable -> ServiceAvailabilityState.API_UNAVAILABLE
            else -> ServiceAvailabilityState.LIVE
        }
        val locState = if (locGranted) ServiceAvailabilityState.LIVE else ServiceAvailabilityState.PERMISSION_DENIED

        _mediaState.update { it.copy(bridgeStatus = bridge) }
        _mapsPulse.update {
            val hasLiveMaps = it.isExternalGoogleMapsSyncActive ||
                it.routePolylineGeo.isNotEmpty() ||
                (locGranted && _telemetry.value.isLiveGpsActive)
            it.copy(
                gpsServiceState = if (locGranted) {
                    if (_telemetry.value.isLiveGpsActive) ServiceAvailabilityState.LIVE else ServiceAvailabilityState.API_UNAVAILABLE
                } else {
                    ServiceAvailabilityState.PERMISSION_DENIED
                },
                mapsServiceState = when {
                    hasLiveMaps -> ServiceAvailabilityState.LIVE
                    settings.value.demoSimulationEnabled -> ServiceAvailabilityState.DEMO
                    !networkOnline -> ServiceAvailabilityState.OFFLINE
                    else -> ServiceAvailabilityState.API_UNAVAILABLE
                }
            )
        }

        _voiceState.update { state ->
            state.copy(
                needsAudioPermission = if (audGranted) false else state.needsAudioPermission,
                bridgeStatus = NativeVoiceBridgeStatus(
                    speechServiceState = speechState,
                    speechRecognizerAvailable = speechAvailable,
                    recordAudioGranted = audGranted,
                    textToSpeechReady = voiceEngine.isTtsReady(),
                    mediaSessionState = bridge.serviceState,
                    locationServiceState = locState,
                    geocoderAvailable = geocoderAvailable,
                    networkOnline = networkOnline,
                    googleMapsConfigured = notifGranted || networkOnline,
                    wakeWordSupported = false
                )
            )
        }

        gpsJob?.cancel()
        if (locGranted) {
            gpsJob = viewModelScope.launch {
                telemetryProvider.observeGpsLocation().collect { reading ->
                    if (reading != null) {
                        val isFreshLiveFix = GpsReadingPolicy.isFreshLiveFix(
                            isLastKnown = reading.isLastKnown,
                            ageMillis = reading.ageMillis
                        )
                        val latLon = reading.latitude to reading.longitude
                        _telemetry.update { current ->
                            val stableHeading = if (reading.hasBearing) {
                                GpsReadingPolicy.stabilizedHeading(
                                    previousDegrees = current.headingDegrees,
                                    targetDegrees = reading.bearingDeg,
                                    speedKmh = reading.speedKmh
                                )
                            } else {
                                current.headingDegrees
                            }
                            current.copy(
                                speedKmh = if (isFreshLiveFix) reading.speedKmh else 0,
                                headingDegrees = stableHeading,
                                headingCardinal = bearingToCardinal(stableHeading),
                                gpsAccuracyMeters = reading.accuracyMeters,
                                isLiveGpsActive = isFreshLiveFix,
                                isLocationFallback = !isFreshLiveFix,
                                isSimulationRunning = false,
                                currentLocationLabel = if (isFreshLiveFix) current.currentLocationLabel else "GPS pending"
                            )
                        }
                        _mapsPulse.update { state ->
                            val confirmed = state.confirmedDestination
                            val cLat = confirmed?.latitude
                            val cLon = confirmed?.longitude
                            val routeMatch = if (state.routePolylineGeo.size >= 2) {
                                RouteMatcher.matchLocationToRoute(
                                    latitude = reading.latitude,
                                    longitude = reading.longitude,
                                    route = state.routePolylineGeo
                                )
                            } else {
                                null
                            }
                            val acceptableRouteMatch = routeMatch?.takeIf {
                                val accuracyAllowance = (reading.accuracyMeters ?: 0f) * 2.0
                                it.distanceToRouteMeters <= maxOf(75.0, accuracyAllowance)
                            }
                            val updatedDist = when {
                                acceptableRouteMatch != null -> {
                                    (acceptableRouteMatch.remainingDistanceMeters / 1000.0).toFloat()
                                }
                                cLat != null && cLon != null && !state.isExternalGoogleMapsSyncActive -> {
                                    DestinationSearchRepository.haversineKm(
                                        reading.latitude,
                                        reading.longitude,
                                        cLat,
                                        cLon
                                    )
                                }
                                else -> null
                            }
                            if (updatedDist != null) {
                                _telemetry.update { t ->
                                    t.copy(remainingDistanceKm = updatedDist)
                                }
                            }
                            state.copy(
                                currentLatLon = latLon,
                                gpsServiceState = if (isFreshLiveFix) {
                                    ServiceAvailabilityState.LIVE
                                } else {
                                    ServiceAvailabilityState.API_UNAVAILABLE
                                },
                                mapsServiceState = if (state.routePolylineGeo.isNotEmpty() || state.isExternalGoogleMapsSyncActive || destinationSearchRepo.isNetworkOnline()) {
                                    ServiceAvailabilityState.LIVE
                                } else {
                                    state.mapsServiceState
                                }
                            )
                        }
                        val routeState = _mapsPulse.value
                        val routeMatchForGuard = if (
                            isFreshLiveFix &&
                            routeState.isNavigating &&
                            !routeState.isExternalGoogleMapsSyncActive &&
                            routeState.routePolylineGeo.size >= 2
                        ) {
                            RouteMatcher.matchLocationToRoute(
                                latitude = reading.latitude,
                                longitude = reading.longitude,
                                route = routeState.routePolylineGeo
                            )
                        } else {
                            null
                        }
                        val shouldAutoReroute = if (routeMatchForGuard != null) {
                            offRouteGuard.shouldReroute(
                                distanceToRouteMeters = routeMatchForGuard.distanceToRouteMeters,
                                accuracyMeters = reading.accuracyMeters,
                                nowMillis = System.currentTimeMillis()
                            )
                        } else {
                            offRouteGuard.reset()
                            false
                        }
                        if (shouldAutoReroute) {
                            routeState.confirmedDestination?.let { computeOsrmRouteToDestination(it) }
                        }
                        if (isFreshLiveFix) {
                            val pendingDestination = _mapsPulse.value.confirmedDestination
                            if (pendingDestination != null && _mapsPulse.value.routePolylineGeo.size < 2 && routeJob?.isActive != true) {
                                computeOsrmRouteToDestination(pendingDestination)
                            }
                            maybeReverseGeocodeAndWeather(reading.latitude, reading.longitude)
                        }
                    } else {
                        _telemetry.update {
                            it.copy(isLiveGpsActive = false, isLocationFallback = false, speedKmh = 0)
                        }
                        _mapsPulse.update {
                            it.copy(
                                gpsServiceState = if (settings.value.demoSimulationEnabled) {
                                    ServiceAvailabilityState.DEMO
                                } else {
                                    ServiceAvailabilityState.API_UNAVAILABLE
                                }
                            )
                        }
                    }
                }
            }
        } else {
            _telemetry.update {
                it.copy(
                    isLiveGpsActive = false,
                    currentLocationLabel = if (settings.value.demoSimulationEnabled) "Demo Street" else "Location Permission Required"
                )
            }
        }
    }

    private fun maybeReverseGeocodeAndWeather(lat: Double, lon: Double) {
        val last = lastReverseGeocodeLatLon
        val movedEnough = last == null ||
            DestinationSearchRepository.haversineKm(last.first, last.second, lat, lon) >= 0.15f
        if (movedEnough) {
            lastReverseGeocodeLatLon = lat to lon
            viewModelScope.launch {
                val placeName = destinationSearchRepo.reverseGeocodeLocation(lat, lon)
                _telemetry.update { current ->
                    current.copy(
                        currentLocationLabel = placeName ?: String.format(Locale.US, "%.4f, %.4f", lat, lon)
                    )
                }
                // Recompute the live Valhalla/OSRM route if destination is set without a polyline yet
                val dest = _mapsPulse.value.confirmedDestination
                if (dest != null && _mapsPulse.value.routePolylineGeo.isEmpty() && !_mapsPulse.value.isExternalGoogleMapsSyncActive) {
                    computeOsrmRouteToDestination(dest)
                }
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastWeatherFetchMs > 600_000L) { // 10 min cache
            lastWeatherFetchMs = now
            viewModelScope.launch {
                val weather = destinationSearchRepo.fetchCurrentWeather(lat, lon)
                if (weather != null) {
                    _telemetry.update {
                        it.copy(
                            weatherTempC = weather.tempC,
                            weatherCondition = weather.condition,
                            isLiveWeather = true
                        )
                    }
                }
            }
        }
    }

    private fun startMediaTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                // Always prioritize live external MediaSession if connected
                val externalSnap = mediaSessionBridge.getExternalMediaSnapshot()
                if (externalSnap != null) {
                    val status = mediaSessionBridge.getBridgeStatus()
                    _mediaState.update { state ->
                        state.copy(
                            isPlaying = externalSnap.isPlaying,
                            positionSec = externalSnap.positionSec,
                            activeSessionSource = externalSnap.source,
                            bridgeStatus = status,
                            isExternalSessionActive = true,
                            externalTitle = externalSnap.title,
                            externalArtist = externalSnap.artist,
                            externalAlbum = externalSnap.album,
                            externalDurationSec = externalSnap.durationSec,
                            externalPositionSec = externalSnap.positionSec
                        )
                    }
                } else {
                    _mediaState.update { state ->
                        if (state.isExternalSessionActive) {
                            state.copy(
                                isExternalSessionActive = false,
                                externalTitle = null,
                                externalArtist = null,
                                externalAlbum = null,
                                externalDurationSec = null,
                                externalPositionSec = null,
                                externalAppLabel = null,
                                externalArtwork = null,
                                externalAccentArgb = DEFAULT_ACCENT_ARGB,
                                isPlaying = false
                            )
                        } else if (!state.isPlaying || !state.userStartedLocalPlayback) {
                            state
                        } else {
                            val maxSec = state.currentTrack.durationSec
                            if (state.positionSec + 1 >= maxSec) {
                                if (state.isRepeat) {
                                    state.copy(positionSec = 0)
                                } else {
                                    MediaPlaybackReducer.nextTrack(state)
                                }
                            } else {
                                state.copy(positionSec = state.positionSec + 1)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeDemoSimulationSetting() {
        viewModelScope.launch {
            var step = 0
            while (isActive) {
                delay(2200L)
                val demoEnabled = settings.value.demoSimulationEnabled
                val hasLiveFix = _telemetry.value.isLiveGpsActive
                if (demoEnabled && !hasLiveFix) {
                    step++
                    val delta = listOf(0, 1, 2, 1, 0, -1, -2, -1)[step % 8]
                    val nextSpeed = (86 + delta).coerceIn(72, 98)
                    _telemetry.update { current ->
                        current.copy(
                            speedKmh = nextSpeed,
                            rangeKm = if (current.rangeKm == 0) 418 else current.rangeKm,
                            batteryPercent = if (current.batteryPercent == 0) 84 else current.batteryPercent,
                            tirePsi = if (current.tirePsi.isEmpty()) listOf(34, 34, 33, 33) else current.tirePsi,
                            isSimulationRunning = true
                        )
                    }
                } else {
                    _telemetry.update { current ->
                        current.copy(
                            speedKmh = if (hasLiveFix) current.speedKmh else 0,
                            isSimulationRunning = false
                        )
                    }
                }
            }
        }
    }

    private fun publishMediaSession() {
        if (_mediaState.value.isExternalSessionActive) return
        val state = _mediaState.value
        if (!state.userStartedLocalPlayback) return
        mediaSessionBridge.publishLocalSessionState(
            track = state.currentTrack,
            isPlaying = state.isPlaying,
            positionSec = state.positionSec
        )
    }

    // Media Playback Controls (strictly operate on active session; NEVER change selectedSource)
    fun togglePlayPause() {
        val state = _mediaState.value
        if (state.isExternalSessionActive) {
            mediaSessionBridge.dispatchTransportPlayPause(state.isPlaying)
            _mediaState.update { it.copy(isPlaying = !state.isPlaying) }
            return
        }
        val nextPlaying = !state.isPlaying
        _mediaState.update {
            it.copy(
                isPlaying = nextPlaying,
                userStartedLocalPlayback = true
            )
        }
        publishMediaSession()
    }

    fun nextTrack() {
        if (_mediaState.value.isExternalSessionActive) {
            mediaSessionBridge.dispatchTransportNext()
            return
        }
        _mediaState.update { MediaPlaybackReducer.nextTrack(it) }
        publishMediaSession()
    }

    fun previousTrack() {
        if (_mediaState.value.isExternalSessionActive) {
            mediaSessionBridge.dispatchTransportPrevious()
            return
        }
        _mediaState.update { MediaPlaybackReducer.previousTrack(it) }
        publishMediaSession()
    }

    fun selectTrack(index: Int) {
        _mediaState.update { MediaPlaybackReducer.selectTrack(it, index) }
        publishMediaSession()
    }

    fun seekTo(seconds: Int) {
        val state = _mediaState.value
        val clamped = seconds.coerceIn(0, state.currentTrack.durationSec)
        if (state.isExternalSessionActive) {
            mediaSessionBridge.dispatchSeek(clamped * 1000L)
            _mediaState.update { it.copy(positionSec = clamped, externalPositionSec = clamped) }
            return
        }
        _mediaState.update { it.copy(positionSec = clamped) }
        publishMediaSession()
    }

    fun toggleShuffle() {
        _mediaState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _mediaState.update { it.copy(isRepeat = !it.isRepeat) }
    }

    fun toggleFavorite(trackId: String) {
        _mediaState.update { state ->
            val updated = if (trackId in state.favoriteIds) {
                state.favoriteIds - trackId
            } else {
                state.favoriteIds + trackId
            }
            state.copy(favoriteIds = updated)
        }
    }

    fun selectMediaSource(source: MediaAppSource) {
        mediaSessionBridge.tryLaunchSourceApp(source)
        val updatedBridge = mediaSessionBridge.getBridgeStatus()
        _mediaState.update { state ->
            MediaPlaybackReducer.selectSource(state, source).copy(
                bridgeStatus = updatedBridge
            )
        }
        viewModelScope.launch {
            prefsRepo.setDefaultMediaSource(source)
        }
    }

    fun setVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        _mediaState.update { it.copy(volumePercent = clamped) }
        voiceEngine.applySystemMediaVolumePercent(clamped)
    }

    // Maps & Traffic Pulse Controls
    fun selectMarker(marker: MapPlaceMarker) {
        _mapsPulse.update { it.copy(selectedMarkerId = marker.id) }
        _telemetry.update {
            it.copy(
                nextTurnDistanceLabel = "${marker.distanceKm} km",
                nextTurnRoad = marker.roadName,
                nextTurnInstruction = marker.name,
                remainingMinutes = marker.etaMinutes,
                remainingDistanceKm = marker.distanceKm
            )
        }
    }

    fun selectRoute(routeId: String) {
        _mapsPulse.update { state ->
            val updated = state.routes.map { it.copy(isSelected = it.id == routeId) }
            val chosen = updated.firstOrNull { it.isSelected }
            if (chosen != null) {
                _telemetry.update {
                    it.copy(
                        remainingMinutes = chosen.durationMin,
                        remainingDistanceKm = chosen.distanceKm,
                        etaTimeLabel = chosen.etaLabel,
                        nextTurnRoad = chosen.viaRoad.substringBefore("->").trim()
                    )
                }
            }
            state.copy(routes = updated, isNavigating = true)
        }
    }

    fun toggleNavigation() {
        _mapsPulse.update { it.copy(isNavigating = !it.isNavigating) }
    }

    fun confirmDestinationCandidate(candidate: DestinationCandidate) {
        val currentGeo = _mapsPulse.value.currentLatLon
        val cLat = candidate.latitude
        val cLon = candidate.longitude
        val approxKm = if (currentGeo != null && cLat != null && cLon != null) {
            DestinationSearchRepository.haversineKm(
                currentGeo.first,
                currentGeo.second,
                cLat,
                cLon
            )
        } else {
            0f
        }
        val approxMin = if (approxKm > 0f) (approxKm * 1.6f).roundToInt().coerceAtLeast(1) else 0
        val etaStr = if (approxMin > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + approxMin * 60_000L))
        } else {
            "--:--"
        }

        _mapsPulse.update {
            it.copy(
                confirmedDestination = candidate,
                destinationLatLon = if (cLat != null && cLon != null) cLat to cLon else null,
                destinationQuery = candidate.name,
                destinationCandidates = emptyList(),
                destinationSearchState = ServiceAvailabilityState.LIVE,
                destinationSearchMessage = candidate.addressSummary,
                isNavigating = false,
                isExternalGoogleMapsSyncActive = false,
                navigationSourceLabel = "Waiting for GPS fix",
                routePolylineGeo = emptyList(),
                routes = emptyList(),
                mapsServiceState = ServiceAvailabilityState.LIVE
            )
        }
        _telemetry.update {
            it.copy(
                nextTurnDistanceLabel = if (approxKm > 0f) String.format(Locale.US, "%.1f km", approxKm) else "--",
                nextTurnRoad = candidate.name,
                nextTurnInstruction = candidate.addressSummary,
                destinationLabel = candidate.name,
                remainingDistanceKm = approxKm,
                remainingMinutes = approxMin,
                etaTimeLabel = etaStr
            )
        }

        computeOsrmRouteToDestination(candidate)

        val outcome = VoiceCommandOutcome(
            category = VoiceCommandCategory.NAVIGATION,
            action = VoiceCommandAction.NAVIGATE_TO,
            recognizedPhrase = candidate.name,
            confirmationTitle = "Navigating to ${candidate.name}",
            confirmationDetail = candidate.addressSummary,
            serviceState = ServiceAvailabilityState.LIVE
        )
        _voiceState.update {
            it.copy(
                visualState = VoiceVisualState.COMMAND_RECOGNISED,
                pendingConfirmation = null,
                lastOutcome = outcome,
                errorTitle = null,
                errorDetail = null,
                errorServiceState = null
            )
        }
        if (_voiceState.value.ttsConfirmationsEnabled) {
            voiceEngine.speakConfirmation("Navigating to ${candidate.name}")
        }
        navigateTo(AuvionyxModule.MAPS)
    }

    private fun computeOsrmRouteToDestination(candidate: DestinationCandidate) {
        val endLat = candidate.latitude
        val endLon = candidate.longitude
        if (endLat == null || endLon == null) {
            _mapsPulse.update { it.copy(isNavigating = false, navigationSourceLabel = "Route unavailable", destinationSearchMessage = "This search result has no usable coordinates. Choose another result.") }
            return
        }
        val currentGeo = _mapsPulse.value.currentLatLon
        if (currentGeo == null || !_telemetry.value.isLiveGpsActive || _telemetry.value.isLocationFallback) {
            _mapsPulse.update { it.copy(isNavigating = false, navigationSourceLabel = "Waiting for GPS fix", destinationSearchMessage = "Destination selected. Waiting for a fresh GPS fix before calculating your route.") }
            return
        }
        if (routeJob?.isActive == true) return
        routeJob = viewModelScope.launch {
            _mapsPulse.update { it.copy(isNavigating = false, navigationSourceLabel = "Calculating route", destinationSearchMessage = "Calculating a driving route…") }
            try {
                val liveRoute = destinationSearchRepo.fetchDrivingRoute(
                    startLat = currentGeo.first, startLon = currentGeo.second,
                    endLat = endLat, endLon = endLon, destinationFallbackName = candidate.name
                )
                if (liveRoute == null || liveRoute.polylinePoints.size < 2) {
                    _mapsPulse.update { it.copy(isNavigating = false, navigationSourceLabel = "Route unavailable", mapsServiceState = ServiceAvailabilityState.API_UNAVAILABLE, destinationSearchMessage = "The routing service did not return a drivable route. Check your internet connection and try again.") }
                    return@launch
                }
                val routeItem = RouteAlternative(
                    id = "osrm_primary", name = candidate.name, viaRoad = liveRoute.nextTurnRoad,
                    durationMin = liveRoute.durationMinutes, distanceKm = liveRoute.distanceKm,
                    etaLabel = liveRoute.etaTimeLabel, trafficState = TrafficSeverity.CLEAR, isSelected = true
                )
                _mapsPulse.update { it.copy(routePolylineGeo = liveRoute.polylinePoints, routes = listOf(routeItem), isNavigating = true, mapsServiceState = ServiceAvailabilityState.LIVE, navigationSourceLabel = "LumaNav · ${liveRoute.routingProvider}", destinationSearchMessage = "Route ready · " + liveRoute.distanceKm + " km · " + liveRoute.durationMinutes + " min") }
                _telemetry.update { it.copy(nextTurnDistanceLabel = liveRoute.nextTurnDistanceLabel, nextTurnRoad = liveRoute.nextTurnRoad, nextTurnInstruction = liveRoute.nextTurnInstruction, remainingDistanceKm = liveRoute.distanceKm, remainingMinutes = liveRoute.durationMinutes, etaTimeLabel = liveRoute.etaTimeLabel, journeyProgress = 0f) }
            } finally {
                routeJob = null
            }
        }
    }

    fun dismissDestinationConfirmation() {
        _voiceState.update {
            it.copy(
                visualState = VoiceVisualState.IDLE,
                pendingConfirmation = null,
                partialTranscript = ""
            )
        }
    }

    fun clearDestinationSearch() {
        routeJob?.cancel()
        _mapsPulse.update {
            it.copy(
                destinationQuery = "",
                destinationSearchState = null,
                destinationSearchMessage = null,
                destinationCandidates = emptyList(),
                confirmedDestination = null,
                destinationLatLon = null,
                routePolylineGeo = emptyList(),
                isNavigating = false,
                isExternalGoogleMapsSyncActive = false,
                navigationSourceLabel = null,
                routes = emptyList()
            )
        }
        _telemetry.update {
            it.copy(
                nextTurnDistanceLabel = "--",
                nextTurnRoad = "No Active Route",
                nextTurnInstruction = "Set a destination in Google Maps or via Voice",
                etaTimeLabel = "--:--",
                remainingMinutes = 0,
                remainingDistanceKm = 0f,
                journeyProgress = 0f
            )
        }
    }

    fun selectPulseTab(tab: PulseFeedTab) {
        _mapsPulse.update { it.copy(activePulseTab = tab) }
    }

    fun reportTrafficIncident(title: String, corridor: String, severity: TrafficSeverity) {
        val delay = when (severity) {
            TrafficSeverity.HIGH -> 14
            TrafficSeverity.SLOW -> 6
            TrafficSeverity.CLEAR -> 0
        }
        val newIncident = TrafficIncident(
            id = "user_${UUID.randomUUID()}",
            title = title.ifBlank { "Driver Hazard Report" },
            corridor = corridor.ifBlank { _telemetry.value.currentLocationLabel },
            delayMinutes = delay,
            severity = severity,
            distanceKm = 0.5f,
            reportedBy = "Driver Report",
            timestampLabel = "Just now",
            isUserReported = true
        )
        _mapsPulse.update { state ->
            state.copy(incidents = listOf(newIncident) + state.incidents)
        }
    }

    // Vehicle Controls
    fun adjustDriverTemp(deltaC: Float) {
        _telemetry.update {
            val next = ((it.cabinDriverTempC + deltaC) * 2f).roundToInt() / 2f
            it.copy(cabinDriverTempC = next.coerceIn(16.0f, 28.0f))
        }
    }

    fun adjustPassengerTemp(deltaC: Float) {
        _telemetry.update {
            val next = ((it.cabinPassengerTempC + deltaC) * 2f).roundToInt() / 2f
            it.copy(cabinPassengerTempC = next.coerceIn(16.0f, 28.0f))
        }
    }

    fun setDriveMode(mode: DriveMode) {
        _telemetry.update { it.copy(driveMode = mode) }
    }

    fun toggleDeviceSync(deviceId: String) {
        _syncDevices.update { list ->
            list.map { if (it.id == deviceId) it.copy(isSynced = !it.isSynced) else it }
        }
    }

    // Settings Persistence
    fun setAdaptiveAlbumColors(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAdaptiveAlbumColors(enabled) }
    }

    fun setDaylightHighContrast(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDaylightHighContrast(enabled) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setReducedMotion(enabled) }
    }

    fun setDashboardRatio(ratio: DashboardSplitRatio) {
        viewModelScope.launch { prefsRepo.setDashboardRatio(ratio) }
    }

    fun updateHudConfig(config: HudConfig) {
        viewModelScope.launch { prefsRepo.updateHudConfig(config) }
    }

    fun setAodStyle(style: AodStyle) {
        viewModelScope.launch { prefsRepo.setAodStyle(style) }
    }

    fun setMetricUnits(metric: Boolean) {
        viewModelScope.launch { prefsRepo.setMetricUnits(metric) }
    }

    fun setTrafficOverlay(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setTrafficOverlay(enabled) }
    }

    fun setSatelliteGrid(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSatelliteGrid(enabled) }
    }

    fun setOptionalCarLauncher(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setOptionalCarLauncher(enabled) }
    }

    fun setDemoSimulation(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDemoSimulation(enabled)
            if (!enabled) {
                _mapsPulse.update {
                    if (it.confirmedDestination == null && !it.isExternalGoogleMapsSyncActive) {
                        it.copy(
                            routes = emptyList(),
                            markers = emptyList(),
                            incidents = it.incidents.filter { inc -> inc.isUserReported },
                            navigationSourceLabel = null
                        )
                    } else it
                }
            }
            refreshPermissionsAndGps()
        }
    }

    // Auvionyx Voice Control
    fun activateVoiceListening() {
        voiceJob?.cancel()
        refreshPermissionsAndGps()
        _voiceState.update {
            it.copy(
                visualState = VoiceVisualState.LISTENING,
                partialTranscript = "Activating microphone…",
                errorTitle = null,
                errorDetail = null,
                errorServiceState = null
            )
        }
        voiceEngine.startListening()
    }

    fun stopVoiceListening() {
        voiceEngine.stopListening()
        _voiceState.update {
            if (it.visualState == VoiceVisualState.LISTENING) {
                it.copy(visualState = VoiceVisualState.IDLE, partialTranscript = "")
            } else it
        }
    }

    fun resetVoiceStateToIdle() {
        voiceJob?.cancel()
        voiceEngine.cancelListening()
        _voiceState.update {
            it.copy(
                visualState = VoiceVisualState.IDLE,
                partialTranscript = "",
                pendingConfirmation = null,
                errorTitle = null,
                errorDetail = null,
                errorServiceState = null,
                needsAudioPermission = false
            )
        }
    }

    fun setTtsConfirmationsEnabled(enabled: Boolean) {
        _voiceState.update { it.copy(ttsConfirmationsEnabled = enabled) }
    }

    fun setGeminiComplexFallbackEnabled(enabled: Boolean) {
        _voiceState.update { it.copy(geminiComplexFallbackEnabled = enabled) }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        if (enabled) {
            _voiceState.update {
                it.copy(
                    wakeWordEnabled = false,
                    visualState = VoiceVisualState.ERROR,
                    errorServiceState = ServiceAvailabilityState.API_UNAVAILABLE,
                    errorTitle = "Wake-Word Requires Native OS Service",
                    errorDetail = "Always-on hotword detection requires Android VoiceInteractionService / DSP integration. Tap the Floating Mic to listen hands-free."
                )
            }
        } else {
            _voiceState.update { it.copy(wakeWordEnabled = false) }
        }
    }

    fun executeVoiceUtterance(utterance: String) {
        val clean = utterance.trim()
        if (clean.isEmpty()) return

        val currentState = _voiceState.value
        if (currentState.visualState == VoiceVisualState.CONFIRMATION_REQUIRED &&
            currentState.pendingConfirmation != null
        ) {
            val replyIdx = VoiceCommandParser.parseConfirmationReply(
                clean,
                currentState.pendingConfirmation.candidates.size
            )
            if (replyIdx == -1) {
                dismissDestinationConfirmation()
                return
            } else if (replyIdx != null) {
                confirmDestinationCandidate(currentState.pendingConfirmation.candidates[replyIdx])
                return
            }
        }

        voiceJob?.cancel()
        _voiceState.update {
            it.copy(
                visualState = VoiceVisualState.PROCESSING,
                partialTranscript = clean,
                pendingConfirmation = null,
                errorTitle = null,
                errorDetail = null,
                errorServiceState = null
            )
        }

        val deterministic = VoiceCommandParser.parseDeterministic(clean)
        if (deterministic != null) {
            routeParsedVoiceCommand(
                command = deterministic,
                recognizedPhrase = clean,
                usedGemini = false
            )
            return
        }

        if (!_voiceState.value.geminiComplexFallbackEnabled) {
            _voiceState.update {
                it.copy(
                    visualState = VoiceVisualState.ERROR,
                    errorServiceState = ServiceAvailabilityState.API_UNAVAILABLE,
                    errorTitle = "Command Not Recognized",
                    errorDetail = "\"$clean\" did not match a direct voice command."
                )
            }
            return
        }

        voiceJob = viewModelScope.launch {
            val tel = _telemetry.value
            val track = _mediaState.value.currentTrack
            val contextSummary = buildString {
                appendLine("Location: ${tel.currentLocationLabel}, Speed: ${tel.speedKmh} km/h, Heading: ${tel.headingCardinal}")
                appendLine("Media: ${track.title} via ${track.source.label}, Volume: ${_mediaState.value.volumePercent}%")
                append("Dashboard Ratio: ${settings.value.dashboardRatio.label}, Active Module: ${_activeModule.value.label}")
            }

            val result = aiRepo.interpretComplexVoiceCommand(clean, contextSummary)
            result.onSuccess { intent ->
                val mapped = mapNaturalIntentToParsedCommand(intent)
                if (mapped != null) {
                    routeParsedVoiceCommand(
                        command = mapped,
                        recognizedPhrase = clean,
                        usedGemini = true,
                        customConfirmation = intent.confirmationMessage
                    )
                } else {
                    _voiceState.update {
                        it.copy(
                            visualState = VoiceVisualState.ERROR,
                            errorServiceState = ServiceAvailabilityState.API_UNAVAILABLE,
                            errorTitle = "Unsupported Voice Command",
                            errorDetail = intent.confirmationMessage.ifBlank {
                                "Try a Navigation, Media, Vehicle, or Interface command."
                            }
                        )
                    }
                }
            }.onFailure { err ->
                _voiceState.update {
                    it.copy(
                        visualState = VoiceVisualState.ERROR,
                        errorServiceState = ServiceAvailabilityState.API_UNAVAILABLE,
                        errorTitle = "Voice Interpreter Unavailable",
                        errorDetail = err.localizedMessage ?: "Unable to interpret natural-language command."
                    )
                }
            }
        }
    }

    private fun routeParsedVoiceCommand(
        command: ParsedVoiceCommand,
        recognizedPhrase: String,
        usedGemini: Boolean,
        customConfirmation: String? = null
    ) {
        if (command.action == VoiceCommandAction.NAVIGATE_TO ||
            command.action == VoiceCommandAction.FIND_NEARBY ||
            command.action == VoiceCommandAction.SEARCH_RESTAURANTS_OR_FUEL
        ) {
            performRealDestinationSearch(
                query = command.queryOrTarget,
                action = command.action,
                recognizedPhrase = recognizedPhrase,
                usedGemini = usedGemini
            )
            return
        }

        val outcome = applyImmediateVoiceCommand(
            command = command,
            recognizedPhrase = recognizedPhrase,
            usedGemini = usedGemini,
            customConfirmation = customConfirmation
        )
        publishVoiceCommandRecognised(outcome)
    }

    fun updateMapsDestinationQuery(query: String) {
        suggestionJob?.cancel()
        _mapsPulse.update {
            it.copy(
                destinationQuery = query,
                destinationCandidates = emptyList(),
                destinationSearchState = null,
                destinationSearchMessage = null
            )
        }
        val clean = query.trim()
        if (clean.length < 3) return
        suggestionJob = viewModelScope.launch {
            kotlinx.coroutines.delay(450L)
            val origin = _mapsPulse.value.currentLatLon
            when (val outcome = destinationSearchRepo.searchDestination(clean, origin?.first, origin?.second)) {
                is DestinationSearchOutcome.Matches -> {
                    if (_mapsPulse.value.destinationQuery.trim() == clean) {
                        _mapsPulse.update {
                            it.copy(
                                destinationCandidates = outcome.candidates,
                                destinationSearchState = outcome.serviceState,
                                destinationSearchMessage = if (outcome.candidates.isEmpty()) "No matching places yet." else "Suggestions · select a destination"
                            )
                        }
                    }
                }
                is DestinationSearchOutcome.Unavailable -> {
                    if (_mapsPulse.value.destinationQuery.trim() == clean) {
                        _mapsPulse.update {
                            it.copy(
                                destinationCandidates = emptyList(),
                                destinationSearchState = outcome.serviceState,
                                destinationSearchMessage = outcome.reason
                            )
                        }
                    }
                }
            }
        }
    }

    fun searchDestinationFromMaps(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _mapsPulse.update {
                it.copy(
                    destinationSearchState = ServiceAvailabilityState.API_UNAVAILABLE,
                    destinationSearchMessage = "Enter a destination to search.",
                    destinationCandidates = emptyList()
                )
            }
            return
        }
        performRealDestinationSearch(
            query = clean,
            action = VoiceCommandAction.NAVIGATE_TO,
            recognizedPhrase = clean,
            usedGemini = false
        )
    }

    private fun performRealDestinationSearch(
        query: String,
        action: VoiceCommandAction,
        recognizedPhrase: String,
        usedGemini: Boolean
    ) {
        val currentGeo = _mapsPulse.value.currentLatLon
        voiceJob = viewModelScope.launch {
            _mapsPulse.update {
                it.copy(
                    destinationQuery = query,
                    destinationSearchState = null,
                    destinationSearchMessage = "Looking up \"$query\" via Android Geocoder…",
                    destinationCandidates = emptyList()
                )
            }
            when (val outcome = destinationSearchRepo.searchDestination(query, currentGeo?.first, currentGeo?.second)) {
                is DestinationSearchOutcome.Unavailable -> {
                    _mapsPulse.update {
                        it.copy(
                            destinationQuery = query,
                            destinationSearchState = outcome.serviceState,
                            destinationSearchMessage = outcome.reason,
                            destinationCandidates = emptyList(),
                            confirmedDestination = null
                        )
                    }
                    _voiceState.update {
                        it.copy(
                            visualState = VoiceVisualState.ERROR,
                            errorServiceState = outcome.serviceState,
                            errorTitle = "${outcome.serviceState.label}: Destination Lookup Failed",
                            errorDetail = outcome.reason,
                            rmsNormalized = 0.15f
                        )
                    }
                    if (_voiceState.value.ttsConfirmationsEnabled) {
                        voiceEngine.speakConfirmation("Destination lookup unavailable for $query")
                    }
                }

                is DestinationSearchOutcome.Matches -> {
                    val candidates = outcome.candidates
                    if (candidates.isEmpty()) {
                        val msg = "No geocoded places found for \"$query\"."
                        _mapsPulse.update {
                            it.copy(
                                destinationQuery = query,
                                destinationSearchState = ServiceAvailabilityState.LIVE,
                                destinationSearchMessage = msg,
                                destinationCandidates = emptyList()
                            )
                        }
                        _voiceState.update {
                            it.copy(
                                visualState = VoiceVisualState.ERROR,
                                errorServiceState = ServiceAvailabilityState.LIVE,
                                errorTitle = "No Matching Destinations",
                                errorDetail = msg
                            )
                        }
                    } else if (candidates.size == 1) {
                        confirmDestinationCandidate(candidates.first())
                    } else {
                        val pending = PendingDestinationConfirmation(
                            spokenQuery = query,
                            candidates = candidates,
                            serviceState = outcome.serviceState
                        )
                        _mapsPulse.update {
                            it.copy(
                                destinationQuery = query,
                                destinationSearchState = outcome.serviceState,
                                destinationSearchMessage = "${candidates.size} places matched \"$query\".",
                                destinationCandidates = candidates
                            )
                        }
                        _voiceState.update {
                            it.copy(
                                visualState = VoiceVisualState.CONFIRMATION_REQUIRED,
                                pendingConfirmation = pending,
                                errorTitle = null,
                                errorDetail = null,
                                errorServiceState = null,
                                lastOutcome = VoiceCommandOutcome(
                                    category = VoiceCommandCategory.NAVIGATION,
                                    action = action,
                                    recognizedPhrase = recognizedPhrase,
                                    confirmationTitle = "Multiple matches for $query",
                                    confirmationDetail = "Tap a destination or say 'Confirm first' / 'Second'.",
                                    serviceState = outcome.serviceState,
                                    usedGeminiInterpreter = usedGemini
                                )
                            )
                        }
                        if (_voiceState.value.ttsConfirmationsEnabled) {
                            voiceEngine.speakConfirmation(
                                "Found ${candidates.size} matches for $query. Say confirm first, second, or tap to navigate."
                            )
                        }
                        navigateTo(AuvionyxModule.MAPS)
                    }
                }
            }
        }
    }

    private fun publishVoiceCommandRecognised(outcome: VoiceCommandOutcome) {
        _voiceState.update {
            it.copy(
                visualState = VoiceVisualState.COMMAND_RECOGNISED,
                pendingConfirmation = null,
                lastOutcome = outcome,
                errorTitle = null,
                errorDetail = null,
                errorServiceState = null,
                rmsNormalized = 0.25f
            )
        }
        if (_voiceState.value.ttsConfirmationsEnabled) {
            voiceEngine.speakConfirmation(outcome.confirmationTitle)
        }
    }

    private fun mapNaturalIntentToParsedCommand(intent: ParsedNaturalVoiceIntent): ParsedVoiceCommand? {
        val category = runCatching { VoiceCommandCategory.valueOf(intent.category) }.getOrNull() ?: return null
        val param = intent.parameter
        return when (intent.action) {
            "NAVIGATE_TO" -> ParsedVoiceCommand(category, VoiceCommandAction.NAVIGATE_TO, queryOrTarget = param)
            "SEARCH_PLACES" -> ParsedVoiceCommand(category, VoiceCommandAction.SEARCH_RESTAURANTS_OR_FUEL, queryOrTarget = param)
            "CHECK_TRAFFIC" -> ParsedVoiceCommand(category, VoiceCommandAction.TRAFFIC_CONDITIONS)
            "PLAY_MEDIA" -> ParsedVoiceCommand(category, VoiceCommandAction.PLAY_MUSIC)
            "PAUSE_MEDIA" -> ParsedVoiceCommand(category, VoiceCommandAction.PAUSE_MUSIC)
            "RESUME_MEDIA" -> ParsedVoiceCommand(category, VoiceCommandAction.RESUME_MUSIC)
            "NEXT_TRACK" -> ParsedVoiceCommand(category, VoiceCommandAction.NEXT_TRACK)
            "PREV_TRACK" -> ParsedVoiceCommand(category, VoiceCommandAction.PREVIOUS_TRACK)
            "SET_VOLUME" -> ParsedVoiceCommand(
                category,
                VoiceCommandAction.CHANGE_VOLUME,
                targetVolumePercent = param.toIntOrNull()?.coerceIn(0, 100) ?: 65
            )
            "OPEN_MEDIA_APP" -> ParsedVoiceCommand(
                category,
                VoiceCommandAction.OPEN_MEDIA_APP,
                mediaSource = VoiceCommandParser.parseMediaApp(param) ?: MediaAppSource.SPOTIFY
            )
            "PLAY_PLAYLIST" -> ParsedVoiceCommand(category, VoiceCommandAction.PLAY_PLAYLIST, playlistHint = param.ifBlank { "Lamentis 2077" })
            "OPEN_HUD" -> ParsedVoiceCommand(category, VoiceCommandAction.OPEN_HUD)
            "SHOW_JOURNEY" -> ParsedVoiceCommand(category, VoiceCommandAction.SHOW_JOURNEY_INFO)
            "OPEN_VEHICLE" -> ParsedVoiceCommand(category, VoiceCommandAction.DISPLAY_VEHICLE_DASHBOARD)
            "OPEN_MAPS" -> ParsedVoiceCommand(category, VoiceCommandAction.OPEN_MAPS)
            "OPEN_MUSIC" -> ParsedVoiceCommand(category, VoiceCommandAction.OPEN_MUSIC)
            "SET_LAYOUT_RATIO" -> ParsedVoiceCommand(
                category,
                VoiceCommandAction.CHANGE_DASHBOARD_LAYOUT,
                targetRatio = VoiceCommandParser.parseRatio(param) ?: DashboardSplitRatio.RATIO_60_40
            )
            "ACTIVATE_FULLSCREEN" -> ParsedVoiceCommand(
                category,
                VoiceCommandAction.ACTIVATE_FULLSCREEN,
                targetRatio = VoiceCommandParser.parseRatio(param) ?: DashboardSplitRatio.FULL_MAPS
            )
            else -> null
        }
    }

    private fun applyImmediateVoiceCommand(
        command: ParsedVoiceCommand,
        recognizedPhrase: String,
        usedGemini: Boolean,
        customConfirmation: String? = null
    ): VoiceCommandOutcome {
        return when (command.action) {
            VoiceCommandAction.TRAFFIC_CONDITIONS -> {
                selectPulseTab(PulseFeedTab.LIVE)
                navigateTo(AuvionyxModule.PULSE)
                val count = _mapsPulse.value.incidents.count { it.severity == TrafficSeverity.HIGH }
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.NAVIGATION,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Opening Traffic Pulse ($count alerts)",
                    confirmationDetail = if (_mapsPulse.value.incidents.isEmpty()) "No active corridor reports." else "Displaying active incidents.",
                    serviceState = _mapsPulse.value.mapsServiceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.PLAY_MUSIC,
            VoiceCommandAction.RESUME_MUSIC -> {
                if (!_mediaState.value.isPlaying) togglePlayPause()
                val track = _mediaState.value.currentTrack
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Playing ${track.title}",
                    confirmationDetail = "${track.artist} · Session: ${_mediaState.value.activeSessionSource.label}",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.PAUSE_MUSIC -> {
                if (_mediaState.value.isPlaying) togglePlayPause()
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Music paused",
                    confirmationDetail = "Active MediaSession paused.",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.NEXT_TRACK -> {
                nextTrack()
                val track = _mediaState.value.currentTrack
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Next track: ${track.title}",
                    confirmationDetail = "${track.artist} (source selection unchanged)",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.PREVIOUS_TRACK -> {
                previousTrack()
                val track = _mediaState.value.currentTrack
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Previous track: ${track.title}",
                    confirmationDetail = "${track.artist} (source selection unchanged)",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.CHANGE_VOLUME -> {
                val current = _mediaState.value.volumePercent
                val next = command.targetVolumePercent ?: (current + (command.volumeDeltaPercent ?: 0))
                setVolume(next)
                val applied = _mediaState.value.volumePercent
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Volume set to $applied%",
                    confirmationDetail = "Synced with Android AudioManager MUSIC stream.",
                    serviceState = ServiceAvailabilityState.LIVE,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.OPEN_MEDIA_APP -> {
                val source = command.mediaSource ?: MediaAppSource.SPOTIFY
                selectMediaSource(source)
                navigateTo(AuvionyxModule.MUSIC)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Selected source ${source.label}",
                    confirmationDetail = "Source selection updated independently of playback transport.",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.PLAY_PLAYLIST -> {
                val hint = command.playlistHint ?: "Lamentis 2077"
                val trackIdx = _mediaState.value.tracks.indexOfFirst {
                    it.title.contains(hint, ignoreCase = true) ||
                        it.album.contains(hint, ignoreCase = true) ||
                        it.artist.contains(hint, ignoreCase = true)
                }.let { if (it >= 0) it else 0 }
                selectTrack(trackIdx)
                val active = _mediaState.value.currentTrack
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.MEDIA,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Playing ${active.title}",
                    confirmationDetail = "${active.artist}",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.OPEN_HUD -> {
                navigateTo(AuvionyxModule.HUD)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.VEHICLE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Opening LumaNav HUD",
                    confirmationDetail = "Minimalist HUD active.",
                    serviceState = _mapsPulse.value.gpsServiceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.SHOW_JOURNEY_INFO -> {
                val tel = _telemetry.value
                setHudLayoutMode(HudLayoutMode.ADVANCED)
                navigateTo(AuvionyxModule.HUD)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.VEHICLE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Journey: ${tel.remainingMinutes} min (${tel.etaTimeLabel})",
                    confirmationDetail = "${tel.remainingDistanceKm} km · ${tel.nextTurnRoad}",
                    serviceState = _mapsPulse.value.mapsServiceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.DISPLAY_VEHICLE_DASHBOARD -> {
                val tel = _telemetry.value
                navigateTo(AuvionyxModule.VEHICLE)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.VEHICLE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Vehicle Dashboard",
                    confirmationDetail = "${tel.batteryPercent}% device battery · ${tel.driveMode.label} Mode.",
                    serviceState = if (tel.isSimulationRunning) ServiceAvailabilityState.DEMO else ServiceAvailabilityState.LIVE,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.OPEN_MAPS -> {
                navigateTo(AuvionyxModule.MAPS)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.INTERFACE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Opening LumaNav Maps",
                    confirmationDetail = "Navigation ready.",
                    serviceState = _mapsPulse.value.mapsServiceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.OPEN_MUSIC -> {
                navigateTo(AuvionyxModule.MUSIC)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.INTERFACE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Opening LumaNav Music",
                    confirmationDetail = "Unified Media Hub displayed.",
                    serviceState = _mediaState.value.bridgeStatus.serviceState,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.CHANGE_DASHBOARD_LAYOUT -> {
                val ratio = command.targetRatio ?: DashboardSplitRatio.RATIO_60_40
                setDashboardRatio(ratio)
                navigateTo(AuvionyxModule.DRIVE)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.INTERFACE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Layout set to ${ratio.label}",
                    confirmationDetail = "Updated cockpit split ratio.",
                    serviceState = ServiceAvailabilityState.LIVE,
                    usedGeminiInterpreter = usedGemini
                )
            }

            VoiceCommandAction.ACTIVATE_FULLSCREEN -> {
                val ratio = command.targetRatio ?: DashboardSplitRatio.FULL_MAPS
                setDashboardRatio(ratio)
                navigateTo(AuvionyxModule.DRIVE)
                VoiceCommandOutcome(
                    category = VoiceCommandCategory.INTERFACE,
                    action = command.action,
                    recognizedPhrase = recognizedPhrase,
                    confirmationTitle = customConfirmation ?: "Fullscreen ${ratio.label} active",
                    confirmationDetail = "Expanded primary panel.",
                    serviceState = ServiceAvailabilityState.LIVE,
                    usedGeminiInterpreter = usedGemini
                )
            }

            else -> VoiceCommandOutcome(
                category = command.category,
                action = command.action,
                recognizedPhrase = recognizedPhrase,
                confirmationTitle = "Handled",
                confirmationDetail = recognizedPhrase
            )
        }
    }

    private fun setHudLayoutMode(mode: HudLayoutMode) {
        updateHudConfig(settings.value.hudConfig.copy(layoutMode = mode))
    }

    private fun bearingToCardinal(deg: Int): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val normalized = ((deg % 360) + 360) % 360
        return dirs[((normalized + 22) / 45) % 8]
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
        mediaSessionBridge.release()
    }
}
