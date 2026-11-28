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

/**
 * Android Auto entry point. Navigation logic and location tracking remain phone-owned;
 * the car host receives only driver-optimized templates from this service.
 */
class LumaNavCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session = LumaNavCarSession()
}

private class LumaNavCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = LumaNavCarHomeScreen(carContext)
}

/**
 * First host-compatible landing screen. Live map projection and shared navigation state
 * are added in the next integration step; this screen intentionally contains no fake
 * location, route, ETA, or telemetry values.
 */
private class LumaNavCarHomeScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val items = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Phone-powered navigation")
                    .addText("LumaNAV runs GPS and route calculation on your phone.")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Navigation engine")
                    .addText("OpenFreeMap · Valhalla with OSRM fallback")
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Next step")
                    .addText("Live MapLibre map projection and route controls are being connected.")
                    .build()
            )
            .build()

        return ListTemplate.Builder()
            .setTitle("LumaNAV")
            .setSingleList(items)
            .build()
    }
}
