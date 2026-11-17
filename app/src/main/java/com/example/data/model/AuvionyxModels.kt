package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue

enum class AuvionyxModule(val route: String, val label: String) {
    DRIVE("drive", "Home"),
    MAPS("maps", "Maps"),
    MUSIC("music", "Music"),
    HUD("hud", "HUD"),
    PULSE("pulse", "Traffic Pulse"),
    VOICE("voice", "Auvionyx Voice"),
    AOD("aod", "AOD"),
    VEHICLE("vehicle", "Vehicle"),
    SETTINGS("settings", "Settings"),
    APPS("apps", "Apps")
}

enum class DashboardSplitRatio(val label: String, val mapWeight: Float, val mediaWeight: Float) {
    RATIO_70_30("70 : 30", 0.70f, 0.30f),
    RATIO_60_40("60 : 40", 0.60f, 0.40f),
    RATIO_50_50("50 : 50", 0.50f, 0.50f),
    FULL_MAPS("Full Maps", 1.0f, 0.0f),
    FULL_MUSIC("Full Music", 0.0f, 1.0f)
}

enum class HudLayoutMode(val label: String) {
    MINIMAL("Minimal HUD"),
    NAVIGATION("Navigation HUD"),
    ADVANCED("Advanced HUD")
}

enum class HudAccentChoice(val label: String, val color: Color) {
    ELECTRIC_CYAN("Electric Cyan", ElectricCyan),
    ICE_BLUE("Ice Blue", IceBlue),
    DEEP_BLUE("Deep Blue", DeepBlue),
    GLASS_WHITE("Glass White", GlassWhite)
}

enum class HudPosition(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

data class HudConfig(
    val layoutMode: HudLayoutMode = HudLayoutMode.NAVIGATION,
    val accentChoice: HudAccentChoice = HudAccentChoice.ELECTRIC_CYAN,
    val fontScale: Float = 1.0f,
    val speedPosition: HudPosition = HudPosition.LEFT,
    val mirrorFlipForWindshield: Boolean = false,
    val showSpeedometer: Boolean = true,
    val showSpeedLimit: Boolean = true,
    val showNextTurn: Boolean = true,
    val showCompass: Boolean = true,
    val showJourneyProgress: Boolean = true,
    val showEta: Boolean = true,
    val showMediaInfo: Boolean = true
)

enum class AodStyle(val label: String, val description: String) {
    MINIMAL("Minimal AOD", "Pure obsidian clock, speed & ambient cyan arc"),
    MAP("Map AOD", "Low-power vector path & next turn indicator"),
    JOURNEY("Journey AOD", "ETA, remaining distance & minimal progress bar")
}

enum class MediaAppSource(val label: String, val packageHint: String) {
    LASTWAVE("LastWave", "Native Stream"),
    SPOTIFY("Spotify", "MediaSession Bridge"),
    YOUTUBE_MUSIC("YouTube Music", "MediaSession Bridge"),
    LOCAL_PLAYER("Local Audio", "On-Device Library")
}

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val source: MediaAppSource,
    val adaptiveColor: Color,
    val artVariant: Int
)

enum class TrafficSeverity(val label: String) {
    HIGH("High Congestion"),
    SLOW("Slow Moving"),
    CLEAR("Clear")
}

data class TrafficIncident(
    val id: String,
    val title: String,
    val corridor: String,
    val delayMinutes: Int,
    val severity: TrafficSeverity,
    val distanceKm: Float,
    val reportedBy: String,
    val timestampLabel: String,
    val isUserReported: Boolean = false
)

data class RouteAlternative(
    val id: String,
    val name: String,
    val viaRoad: String,
    val durationMin: Int,
    val distanceKm: Float,
    val etaLabel: String,
    val trafficState: TrafficSeverity,
    val isSelected: Boolean = false
)

data class MapPlaceMarker(
    val id: String,
    val name: String,
    val category: String,
    val roadName: String,
    val distanceKm: Float,
    val etaMinutes: Int,
    val relX: Float,
    val relY: Float
)

/**
 * Contract describing Google Maps Platform readiness vs Prototype Demo Map rendering.
 */
data class GoogleMapsIntegrationStatus(
    val isMapsSdkConfigured: Boolean = false,
    val providerLabel: String = "Google Maps Platform (API Key Pending)",
    val renderModeLabel: String = "DEMO VECTOR PREVIEW"
)

enum class DriveMode(val label: String) {
    ECO("Eco"),
    TOUR("Tour"),
    SPORT("Sport")
}

data class VehicleTelemetry(
    val speedKmh: Int = 0,
    val speedLimitKmh: Int? = null, // Null unless verified road speed-limit feed is connected
    val demoSpeedLimitKmh: Int = 100,
    val headingDegrees: Int = 0,
    val headingCardinal: String = "N",
    val gpsAccuracyMeters: Float? = null,
    val nextTurnDistanceLabel: String = "--",
    val nextTurnRoad: String = "No Active Route",
    val nextTurnInstruction: String = "Set a destination in Google Maps or via Voice",
    val etaTimeLabel: String = "--:--",
    val remainingMinutes: Int = 0,
    val remainingDistanceKm: Float = 0f,
    val journeyProgress: Float = 0f,
    val currentLocationLabel: String = "Acquiring GPS…",
    val weatherTempC: Int? = null,
    val weatherCondition: String = "",
    val cabinDriverTempC: Float = 24.0f,
    val cabinPassengerTempC: Float = 22.5f,
    val rangeKm: Int = 0,
    val batteryPercent: Int = 0,
    val driveMode: DriveMode = DriveMode.TOUR,
    val tirePsi: List<Int> = emptyList(),
    val isLiveGpsActive: Boolean = false,
    val isSimulationRunning: Boolean = false,
    val isLiveWeather: Boolean = false
)

data class SyncDeviceProfile(
    val id: String,
    val name: String,
    val formFactor: String,
    val osVersion: String,
    val resolution: String,
    val isCurrentTarget: Boolean,
    val isSynced: Boolean
)

object AuvionyxSampleData {
    // Official brand cyan/ice/deep blue palette used across all default media tracks (no purple/magenta)
    val tracks = listOf(
        MediaTrack(
            id = "lamentis_2077",
            title = "Lamentis 2077",
            artist = "Synthwave Collective",
            album = "Neon Horizon OST",
            durationSec = 324,
            source = MediaAppSource.LASTWAVE,
            adaptiveColor = ElectricCyan,
            artVariant = 0
        ),
        MediaTrack(
            id = "echelon_drift",
            title = "Lamentis 2077 (Echelon Mix)",
            artist = "Echelon",
            album = "Midnight Autobahn",
            durationSec = 266,
            source = MediaAppSource.SPOTIFY,
            adaptiveColor = IceBlue,
            artVariant = 1
        ),
        MediaTrack(
            id = "obsidian_pulse",
            title = "Obsidian Velocity",
            artist = "Kairo & The Grid",
            album = "Afterburner",
            durationSec = 248,
            source = MediaAppSource.YOUTUBE_MUSIC,
            adaptiveColor = DeepBlue,
            artVariant = 2
        ),
        MediaTrack(
            id = "rajpur_rain",
            title = "Cybernetic Rain",
            artist = "Aether Drive",
            album = "AMOLED Sessions",
            durationSec = 295,
            source = MediaAppSource.LOCAL_PLAYER,
            adaptiveColor = IceBlue,
            artVariant = 3
        )
    )

    val routes = listOf(
        RouteAlternative(
            id = "route_rajpur",
            name = "Rajpur Corridor (Demo)",
            viaRoad = "Rajpur Road -> Canal Bypass",
            durationMin = 19,
            distanceKm = 12.6f,
            etaLabel = "10:43",
            trafficState = TrafficSeverity.CLEAR,
            isSelected = true
        ),
        RouteAlternative(
            id = "route_isbt",
            name = "ISBT Junction Route (Demo)",
            viaRoad = "ISBT Junction -> Ring Road",
            durationMin = 25,
            distanceKm = 14.1f,
            etaLabel = "10:49",
            trafficState = TrafficSeverity.SLOW
        ),
        RouteAlternative(
            id = "route_expy",
            name = "Delhi-Dehradun Expy (Demo)",
            viaRoad = "Expressway Toll Link",
            durationMin = 22,
            distanceKm = 16.4f,
            etaLabel = "10:46",
            trafficState = TrafficSeverity.CLEAR
        )
    )

    val incidents = listOf(
        TrafficIncident(
            id = "inc_nh58",
            title = "NH-58 Bottleneck",
            corridor = "NH-58 (7.2 km)",
            delayMinutes = 18,
            severity = TrafficSeverity.HIGH,
            distanceKm = 7.2f,
            reportedBy = "Demo Telemetry",
            timestampLabel = "Sample · 2m"
        ),
        TrafficIncident(
            id = "inc_isbt",
            title = "ISBT Junction Signal Queue",
            corridor = "ISBT Junction",
            delayMinutes = 6,
            severity = TrafficSeverity.SLOW,
            distanceKm = 3.8f,
            reportedBy = "Demo Probe",
            timestampLabel = "Sample · 5m"
        ),
        TrafficIncident(
            id = "inc_expy",
            title = "Delhi-Dehradun Expy Flow",
            corridor = "Delhi-Dehradun Expy",
            delayMinutes = 0,
            severity = TrafficSeverity.CLEAR,
            distanceKm = 5.4f,
            reportedBy = "Demo Corridor",
            timestampLabel = "Sample"
        )
    )

    val placeMarkers = listOf(
        MapPlaceMarker("mk_1", "Rajpur Tech Plaza", "EV Fast Charger", "Rajpur Road", 2.4f, 5, 0.58f, 0.34f),
        MapPlaceMarker("mk_2", "ISBT Mobility Hub", "Transit Junction", "ISBT Bypass", 3.8f, 9, 0.30f, 0.62f),
        MapPlaceMarker("mk_3", "Clock Tower Circle", "City Center", "Haridwar Rd", 6.1f, 14, 0.72f, 0.68f),
        MapPlaceMarker("mk_4", "Pacific Sky Lounge", "Destination", "Mussoorie Div", 12.6f, 19, 0.52f, 0.18f)
    )

    val syncDevices = listOf(
        SyncDeviceProfile(
            id = "dev_blaupunkt",
            name = "Blaupunkt Palm Bay 1000",
            formFactor = "Head Unit · 1280×800 Landscape",
            osVersion = "Android 11 · 4GB RAM",
            resolution = "1280×800",
            isCurrentTarget = true,
            isSynced = true
        ),
        SyncDeviceProfile(
            id = "dev_oppo",
            name = "OPPO Find X10",
            formFactor = "Smartphone · Portrait & Landscape",
            osVersion = "Android 15 · Companion",
            resolution = "1440×3168",
            isCurrentTarget = false,
            isSynced = true
        ),
        SyncDeviceProfile(
            id = "dev_samsung",
            name = "Samsung Galaxy S24+",
            formFactor = "Smartphone · Portrait & Landscape",
            osVersion = "Android 15 · Companion",
            resolution = "1440×3120",
            isCurrentTarget = false,
            isSynced = false
        )
    )
}
