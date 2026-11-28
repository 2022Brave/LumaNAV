package com.example.auto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.car.app.navigation.NavigationManager
import androidx.core.content.ContextCompat
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import com.example.data.maps.DestinationSearchRepository
import com.example.data.maps.LiveDrivingRoute
import com.example.data.model.DestinationCandidate
import com.example.data.model.DestinationSearchOutcome
import com.example.data.model.SavedPlace
import com.example.data.preferences.AuvionyxPreferencesRepository
import com.example.data.sensor.LiveGpsReading
import com.example.data.sensor.TelemetryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt

internal data class LumaNavCarState(
    val location: LiveGpsReading? = null,
    val destination: DestinationCandidate? = null,
    val route: LiveDrivingRoute? = null,
    val isNavigating: Boolean = false,
    val wireframeMode: Boolean = false,
    val isSearching: Boolean = false,
    val searchResults: List<DestinationCandidate> = emptyList(),
    val searchMessage: String? = null
)

internal class LumaNavCarRuntime private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val telemetry = TelemetryProvider(appContext)
    private val destinations = DestinationSearchRepository(appContext)
    private val preferences = AuvionyxPreferencesRepository(appContext)
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(LumaNavCarState())
    val state: StateFlow<LumaNavCarState> = mutableState.asStateFlow()
    val surfaceRenderer = LumaNavCarSurfaceRenderer(appContext, this)

    private var gpsJob: Job? = null
    private var navigationManager: NavigationManager? = null
    private var navigationStarted = false
    private var foregroundServiceStarted = false
    private var furthestRouteProgress = 0.0

    companion object {
        @Volatile private var instance: LumaNavCarRuntime? = null

        fun get(context: Context): LumaNavCarRuntime =
            instance ?: synchronized(this) {
                instance ?: LumaNavCarRuntime(context).also { instance = it }
            }
    }

    fun attachNavigationManager(manager: NavigationManager) {
        navigationManager = manager
        if (mutableState.value.isNavigating) {
            startHostNavigation()
        }
    }

    fun detachNavigationManager() {
        if (navigationStarted) {
            runCatching { navigationManager?.navigationEnded() }
        }
        navigationStarted = false
        navigationManager = null
    }

    fun startLocationUpdates() {
        if (gpsJob?.isActive == true) return
        gpsJob = scope.launch {
            telemetry.observeGpsLocation().collect { reading ->
                mutableState.value = mutableState.value.copy(location = reading)
                if (mutableState.value.isNavigating) publishTrip()
            }
        }
    }

    fun stopLocationUpdates() {
        gpsJob?.cancel()
        gpsJob = null
    }

    fun hasLocationPermission(): Boolean {
        if (!telemetry.hasLocationPermission()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun savedPlaces(): List<SavedPlace> = preferences.savedPlacesFlow.first()

    suspend fun search(query: String): DestinationSearchOutcome {
        val location = mutableState.value.location
        return destinations.searchDestination(
            query = query,
            nearLat = location?.latitude,
            nearLon = location?.longitude
        )
    }

    suspend fun startRoute(candidate: DestinationCandidate): Boolean {
        if (!hasLocationPermission()) return false
        val latitude = candidate.latitude ?: return false
        val longitude = candidate.longitude ?: return false
        var origin = mutableState.value.location
        if (origin == null) {
            origin = runCatching {
                withTimeoutOrNull(12_000L) {
                    telemetry.observeGpsLocation().filterNotNull().first()
                }
            }.getOrNull()
        }
        if (origin == null) return false

        val route = withContext(Dispatchers.IO) {
            destinations.fetchDrivingRoute(
                startLat = origin.latitude,
                startLon = origin.longitude,
                endLat = latitude,
                endLon = longitude,
                destinationFallbackName = candidate.name
            )
        } ?: return false

        if (!startForegroundNavigation(candidate.name)) return false
        foregroundServiceStarted = true
        furthestRouteProgress = 0.0
        mutableState.value = mutableState.value.copy(
            destination = candidate,
            route = route,
            isNavigating = true,
            searchMessage = null
        )
        rememberRecent(candidate)
        startHostNavigation()
        publishTrip()
        return true
    }

    fun stopRoute() {
        mutableState.value = mutableState.value.copy(
            destination = null,
            route = null,
            isNavigating = false
        )
        if (navigationStarted) {
            runCatching { navigationManager?.navigationEnded() }
        }
        navigationStarted = false
        furthestRouteProgress = 0.0
        stopForegroundNavigation()
    }

    fun distanceLabel(candidate: DestinationCandidate): String {
        val origin = mutableState.value.location ?: return ""
        val latitude = candidate.latitude ?: return ""
        val longitude = candidate.longitude ?: return ""
        val distance = distanceKm(origin.latitude, origin.longitude, latitude, longitude)
        return if (distance < 1.0) {
            "${(distance * 1000.0).roundToInt()} m"
        } else {
            String.format(Locale.getDefault(), "%.1f km", distance)
        }
    }

    fun remainingDistanceKm(route: LiveDrivingRoute): Double =
        route.distanceKm.toDouble() * remainingRouteFraction(route)

    fun remainingDurationMinutes(route: LiveDrivingRoute): Int =
        ceil(route.durationMinutes * remainingRouteFraction(route))
            .toInt()
            .coerceAtLeast(1)

    private fun remainingRouteFraction(route: LiveDrivingRoute): Double {
        val location = mutableState.value.location ?: return 1.0
        val points = route.polylinePoints
        if (points.size < 2) return 1.0

        val cumulativeDistance = DoubleArray(points.size)
        var totalDistance = 0.0
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            totalDistance += distanceKm(previous.first, previous.second, current.first, current.second)
            cumulativeDistance[index] = totalDistance
        }
        if (totalDistance <= 0.0) return 1.0

        var closestIndex = 0
        var closestDistance = Double.MAX_VALUE
        points.forEachIndexed { index, point ->
            val distance = distanceKm(
                location.latitude,
                location.longitude,
                point.first,
                point.second
            )
            if (distance < closestDistance) {
                closestDistance = distance
                closestIndex = index
            }
        }
        val progress = (cumulativeDistance[closestIndex] / totalDistance).coerceIn(0.0, 1.0)
        furthestRouteProgress = maxOf(furthestRouteProgress, progress)
        return (1.0 - furthestRouteProgress).coerceIn(0.01, 1.0)
    }

    private fun distanceKm(
        latitudeA: Double,
        longitudeA: Double,
        latitudeB: Double,
        longitudeB: Double
    ): Double {
        val earthRadiusKm = 6371.0088
        val latitudeDelta = Math.toRadians(latitudeB - latitudeA)
        val longitudeDelta = Math.toRadians(longitudeB - longitudeA)
        val startLatitude = Math.toRadians(latitudeA)
        val endLatitude = Math.toRadians(latitudeB)
        val haversine = kotlin.math.sin(latitudeDelta / 2.0) * kotlin.math.sin(latitudeDelta / 2.0) +
            kotlin.math.cos(startLatitude) * kotlin.math.cos(endLatitude) *
            kotlin.math.sin(longitudeDelta / 2.0) * kotlin.math.sin(longitudeDelta / 2.0)
        return 2.0 * earthRadiusKm * kotlin.math.asin(kotlin.math.sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    fun toggleMapStyle() {
        mutableState.value = mutableState.value.copy(
            wireframeMode = !mutableState.value.wireframeMode
        )
    }

    fun updateSearchState(
        loading: Boolean,
        results: List<DestinationCandidate>,
        message: String? = null
    ) {
        mutableState.value = mutableState.value.copy(
            isSearching = loading,
            searchResults = results,
            searchMessage = message
        )
    }

    suspend fun recentPlaces(): List<SavedPlace> = withContext(Dispatchers.IO) {
        val json = appContext.getSharedPreferences("lumanav_car_recent", Context.MODE_PRIVATE)
            .getString("places", "[]")
        val array = runCatching { JSONArray(json.orEmpty()) }.getOrNull() ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                if (!lat.isFinite() || !lon.isFinite()) continue
                add(
                    SavedPlace(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        addressSummary = item.optString("address"),
                        latitude = lat,
                        longitude = lon,
                        category = "Recent"
                    )
                )
            }
        }
    }

    private fun rememberRecent(candidate: DestinationCandidate) {
        val latitude = candidate.latitude ?: return
        val longitude = candidate.longitude ?: return
        val prefs = appContext.getSharedPreferences("lumanav_car_recent", Context.MODE_PRIVATE)
        val current = runCatching { JSONArray(prefs.getString("places", "[]").orEmpty()) }.getOrNull() ?: JSONArray()
        val updated = JSONArray()
        updated.put(
            JSONObject()
                .put("id", candidate.id)
                .put("name", candidate.name)
                .put("address", candidate.addressSummary)
                .put("latitude", latitude)
                .put("longitude", longitude)
        )
        for (index in 0 until current.length()) {
            val item = current.optJSONObject(index) ?: continue
            if (item.optString("id") != candidate.id && updated.length() < 8) updated.put(item)
        }
        prefs.edit().putString("places", updated.toString()).apply()
    }

    private fun startForegroundNavigation(destinationName: String): Boolean {
        val intent = Intent(appContext, LumaNavCarAppService::class.java)
            .setAction(LumaNavCarAppService.ACTION_START_NAVIGATION)
            .putExtra(LumaNavCarAppService.EXTRA_DESTINATION, destinationName)
        return runCatching { ContextCompat.startForegroundService(appContext, intent) }.isSuccess
    }

    private fun stopForegroundNavigation() {
        if (!foregroundServiceStarted) return
        val intent = Intent(appContext, LumaNavCarAppService::class.java)
            .setAction(LumaNavCarAppService.ACTION_STOP_NAVIGATION)
        runCatching { appContext.startService(intent) }
        foregroundServiceStarted = false
    }

    private fun startHostNavigation() {
        val manager = navigationManager ?: return
        if (navigationStarted) return
        runCatching { manager.navigationStarted() }
            .onSuccess { navigationStarted = true }
    }

    private fun publishTrip() {
        val manager = navigationManager ?: return
        val current = mutableState.value
        val route = current.route ?: return
        val destinationCandidate = current.destination ?: return
        if (!navigationStarted) return

        val remainingDistance = remainingDistanceKm(route)
        val remainingMinutes = remainingDurationMinutes(route)
        val arrivalMillis = System.currentTimeMillis() + remainingMinutes * 60_000L
        val arrival = DateTimeWithZone.create(arrivalMillis, TimeZone.getDefault())
        val destinationEstimate = TravelEstimate.Builder(
            Distance.create(remainingDistance, Distance.UNIT_KILOMETERS),
            arrival
        ).setRemainingTimeSeconds(remainingMinutes * 60L).build()

        val stepDistanceMeters = route.nextTurnDistanceLabel
            .replace(",", "")
            .substringBefore(" ")
            .toDoubleOrNull()
            ?.let { value ->
                if (route.nextTurnDistanceLabel.contains("km", ignoreCase = true)) value * 1000.0 else value
            } ?: 500.0
        val stepEstimate = TravelEstimate.Builder(
            Distance.create(stepDistanceMeters.coerceAtLeast(1.0), Distance.UNIT_METERS),
            arrival
        ).setRemainingTimeSeconds(remainingMinutes * 60L).build()

        val maneuverType = when {
            route.nextTurnInstruction.contains("left", ignoreCase = true) -> Maneuver.TYPE_TURN_NORMAL_LEFT
            route.nextTurnInstruction.contains("right", ignoreCase = true) -> Maneuver.TYPE_TURN_NORMAL_RIGHT
            route.nextTurnInstruction.contains("u-turn", ignoreCase = true) -> Maneuver.TYPE_U_TURN_LEFT
            else -> Maneuver.TYPE_STRAIGHT
        }
        val step = Step.Builder(route.nextTurnInstruction)
            .setManeuver(Maneuver.Builder(maneuverType).build())
            .setRoad(route.nextTurnRoad)
            .build()
        val destination = Destination.Builder()
            .setName(destinationCandidate.name)
            .setAddress(destinationCandidate.addressSummary)
            .build()
        val trip = Trip.Builder()
            .addDestination(destination, destinationEstimate)
            .addStep(step, stepEstimate)
            .setCurrentRoad(route.nextTurnRoad)
            .build()
        runCatching { manager.updateTrip(trip) }
    }
}
