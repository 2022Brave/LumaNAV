package com.example.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

/**
 * Android Auto entry point. Navigation logic and location tracking remain phone-owned;
 * the car host receives driver-optimized templates and standardized trip metadata.
 */
class LumaNavCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = LumaNavCarSession()
}

private class LumaNavCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = LumaNavCarHomeScreen(carContext)
}

/** Live phone-navigation status plus Android Auto cluster/HUD trip synchronization. */
private class LumaNavCarHomeScreen(carContext: CarContext) : Screen(carContext) {
    private val navigationManager = carContext.getCarService(NavigationManager::class.java)
    private val mapRenderer = CarMapSurfaceRenderer(carContext, lifecycle)
    private var hostNavigationStarted = false

    init {
        navigationManager.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onStopNavigation() {
                // The phone ViewModel remains the authority and performs the actual state change.
                CarNavigationBridge.requestStopNavigation()
            }

            override fun onAutoDriveEnabled() {
                // Simulation mode intentionally does not fabricate a route or GPS position.
            }
        })

        lifecycleScope.launch {
            CarNavigationBridge.snapshot.collect { state ->
                    mapRenderer.update(state)
                    syncNavigationManager(state)
                    invalidate()
                }
        }
    }

    private fun syncNavigationManager(state: CarNavigationSnapshot) {
        if (!state.isNavigating) {
            if (hostNavigationStarted) {
                runCatching { navigationManager.navigationEnded() }
                hostNavigationStarted = false
            }
            return
        }

        if (!hostNavigationStarted) {
            runCatching { navigationManager.navigationStarted() }
                .onSuccess { hostNavigationStarted = true }
                .onFailure { return }
        }

        val distanceKm = state.remainingDistanceKm
        val minutes = state.remainingMinutes
        val destinationName = state.destinationName
        if (destinationName.isBlank() || distanceKm <= 0f || minutes <= 0) return

        val arrivalTimeMillis = System.currentTimeMillis() + minutes * 60_000L
        val estimate = TravelEstimate.Builder(
            Distance.create(distanceKm.toDouble(), Distance.UNIT_KILOMETERS_P1),
            DateTimeWithZone.create(arrivalTimeMillis, TimeZone.getDefault())
        )
            .setRemainingTimeSeconds(minutes * 60L)
            .build()

        val trip = Trip.Builder()
            .addDestination(Destination.Builder().setName(destinationName).build(), estimate)
            .build()
        runCatching { navigationManager.updateTrip(trip) }
    }

    override fun onGetTemplate(): Template {
        val state = CarNavigationBridge.snapshot.value
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Stop")
                    .setOnClickListener { CarNavigationBridge.requestStopNavigation() }
                    .build()
            )
            .build()

        val builder = NavigationTemplate.Builder()
            .setActionStrip(actionStrip)

        if (carContext.carAppApiLevel >= 2) {
            builder.setMapActionStrip(
                ActionStrip.Builder().addAction(Action.PAN).build()
            )
        }

        if (state.isNavigating) {
            val turnDistance = parseTurnDistance(state.nextTurnDistance)
            if (turnDistance != null && state.nextTurnInstruction.isNotBlank()) {
                val stepBuilder = Step.Builder(state.nextTurnInstruction)
                if (state.nextTurnRoad.isNotBlank()) stepBuilder.setRoad(state.nextTurnRoad)
                builder.setNavigationInfo(
                    RoutingInfo.Builder()
                        .setCurrentStep(stepBuilder.build(), turnDistance)
                        .build()
                )
            }
        }

        if (state.isNavigating && state.remainingDistanceKm > 0f && state.remainingMinutes > 0) {
            val arrivalTimeMillis = System.currentTimeMillis() + state.remainingMinutes * 60_000L
            val estimate = TravelEstimate.Builder(
                Distance.create(state.remainingDistanceKm.toDouble(), Distance.UNIT_KILOMETERS_P1),
                DateTimeWithZone.create(arrivalTimeMillis, TimeZone.getDefault())
            )
                .setRemainingTimeSeconds(state.remainingMinutes * 60L)
                .build()
            builder.setDestinationTravelEstimate(estimate)
        }

        return builder.build()
    }

    private fun parseTurnDistance(label: String): Distance? {
        val amount = Regex("""\\d+(?:[.,]\\d+)?""")
            .find(label)?.value?.replace(',', '.')?.toDoubleOrNull() ?: return null
        val unit = if (label.contains("km", ignoreCase = true)) {
            Distance.UNIT_KILOMETERS_P1
        } else {
            Distance.UNIT_METERS
        }
        return Distance.create(amount, unit)
    }
}
