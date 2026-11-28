package com.example.car

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local projection of phone navigation state for Android Auto.
 * The phone ViewModel remains the single owner of GPS, search and route calculation.
 */
data class CarNavigationSnapshot(
    val isNavigating: Boolean = false,
    val isExternalNavigation: Boolean = false,
    val destinationName: String = "",
    val destinationLatLon: Pair<Double, Double>? = null,
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
)

/** In-process bridge; it never creates a second GPS or routing owner. */
object CarNavigationBridge {
    private val _snapshot = MutableStateFlow(CarNavigationSnapshot())
    val snapshot: StateFlow<CarNavigationSnapshot> = _snapshot.asStateFlow()

    private val _stopNavigationRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stopNavigationRequests: SharedFlow<Unit> = _stopNavigationRequests.asSharedFlow()

    fun publish(snapshot: CarNavigationSnapshot) {
        _snapshot.value = snapshot
    }

    fun requestStopNavigation() {
        _stopNavigationRequests.tryEmit(Unit)
    }
}
