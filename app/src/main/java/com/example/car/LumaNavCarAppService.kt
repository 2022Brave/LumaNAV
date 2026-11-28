package com.example.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Android Auto entry point. Navigation logic and location tracking remain phone-owned;
 * the car host receives only driver-optimized templates from this service.
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

/** Live phone-navigation status. The map surface is the next layer; no values are fabricated. */
private class LumaNavCarHomeScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    init {
        lifecycleScope.launch {
            CarNavigationBridge.snapshot.collect { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        val state = CarNavigationBridge.snapshot.value
        val status = when {
            state.isNavigating -> "Navigation active"
            state.destinationName.isNotBlank() -> "Destination selected · waiting for route"
            else -> "No active route"
        }
        val destination = state.destinationName.ifBlank { "Choose a destination on your phone" }
        val manoeuvre = if (state.isNavigating) {
            listOf(state.nextTurnInstruction, state.nextTurnRoad)
                .filter { it.isNotBlank() }.distinct().joinToString(" · ")
                .ifBlank { "Follow the highlighted route" }
        } else {
            "Turn-by-turn guidance will appear when a route is active"
        }
        val remaining = if (state.remainingDistanceKm > 0f)
            String.format(Locale.getDefault(), "%.1f km", state.remainingDistanceKm)
        else "Not available"
        val duration = if (state.remainingMinutes > 0) "${state.remainingMinutes} min" else "Not available"
        val eta = state.etaTimeLabel.takeIf { state.isNavigating && it.isNotBlank() && it != "--:--" }
            ?: "Not available"

        val items = ItemList.Builder()
            .addItem(Row.Builder().setTitle("STATUS").addText(status).build())
            .addItem(Row.Builder().setTitle("DESTINATION").addText(destination).build())
            .addItem(Row.Builder().setTitle("NEXT MANOEUVRE").addText(manoeuvre).build())
            .addItem(Row.Builder().setTitle("DISTANCE REMAINING").addText(remaining).build())
            .addItem(Row.Builder().setTitle("TIME · ETA").addText("$duration · $eta").build())
            .addItem(Row.Builder().setTitle("PHONE GPS").addText(
                if (state.gpsAvailable) "Live location · ${state.locationLabel}"
                else "Waiting for a live phone GPS fix"
            ).build())
            .addItem(Row.Builder().setTitle("NAVIGATION SOURCE").addText(
                state.navigationSource.ifBlank {
                    if (state.isExternalNavigation) "External navigation sync" else "LumaNAV on phone"
                }
            ).build())
            .build()

        return ListTemplate.Builder().setTitle("LumaNAV").setSingleList(items).build()
    }
}
