package com.example.car

import com.example.viewmodel.MapsAndPulseState
import com.example.data.model.VehicleTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local, read-only projection of the phone navigation state for Android Auto.
 * The phone ViewModel remains the single owner of GPS, search and route calculation.
 */
data class CarNavigationSnapshot(
    val isNavigating: Boolean = false,
    val isExternalNavigation: Boolean = false,
    val destinationName: String = "",
    val navigationSource: String = "",
    val currentLocation: Pair<Double, Double>? = null,
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val headingDegrees: Int = 0,
    val nextTurnInstruction: String = "No active navigation",
    val nextTurnRoad: String = "",
    val nextTurnDistance: String = "--",
    val remainingDistanceKm: Float = 0f,
    val remainingMinutes: Int = 0,
    val etaTimeLabel: String = "--:--",
    val gpsAvailable: Boolean = false,
    val locationLabel: String = "Waiting for phone GPS"
) {
    companion object {
        fun from(
            maps: MapsAndPulseState,
            telemetry: VehicleTelemetry
        ): CarNavigationSnapshot {
            val destination = maps.confirmedDestination?.name
                ?: telemetry.destinationLabel
                .takeIf { it.isNotBlank() }
                ?: maps.destinationQuery

            return CarNavigationSnapshot(
                isNavigating = maps.isNavigating,
                isExternalNavigation = maps.isExternalGoogleMapsSyncActive,
                destinationName = destination,
                navigationSource = maps.navigationSourceLabel.orEmpty(),
                currentLocation = maps.currentLatLon,
                routePoints = maps.routePolylineGeo.toList(),
                headingDegrees = telemetry.headingDegrees,
                nextTurnInstruction = telemetry.nextTurnInstruction,
                nextTurnRoad = telemetry.nextTurnRoad,
                nextTurnDistance = telemetry.nextTurnDistanceLabel,
                remainingDistanceKm = telemetry.remainingDistanceKm,
                remainingMinutes = telemetry.remainingMinutes,
                etaTimeLabel = telemetry.etaTimeLabel,
                gpsAvailable = telemetry.isLiveGpsActive && !telemetry.isLocationFallback,
                locationLabel = telemetry.currentLocationLabel
            )
        }
    }
}

/**
 * A tiny in-process state bridge. It deliberately does not create another ViewModel,
 * GPS provider, location subscription or routing client inside the car service.
 */
object CarNavigationBridge {
    private val _snapshot = MutableStateFlow(CarNavigationSnapshot())
    val snapshot: StateFlow<CarNavigationSnapshot> = _snapshot.asStateFlow()

    fun publish(snapshot: CarNavigationSnapshot) {
        _snapshot.value = snapshot
    }
}
