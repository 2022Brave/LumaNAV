package com.example.auto

import android.content.Intent
import androidx.car.app.AppManager
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LumaNavCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val sharedRuntime = LumaNavCarRuntime.get(carContext)
        val screen = LumaNavNavigationScreen(
            carContext = carContext,
            runtime = sharedRuntime
        )
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(sharedRuntime.surfaceRenderer)
        val navigationManager = carContext.getCarService(NavigationManager::class.java)
        navigationManager.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onStopNavigation() {
                sharedRuntime.stopRoute()
            }
        })
        sharedRuntime.attachNavigationManager(navigationManager)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                sharedRuntime.startLocationUpdates()
            }

            override fun onStop(owner: LifecycleOwner) {
                if (!sharedRuntime.state.value.isNavigating) {
                    sharedRuntime.stopLocationUpdates()
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                sharedRuntime.detachNavigationManager()
                sharedRuntime.surfaceRenderer.release()
                sharedRuntime.stopLocationUpdates()
            }
        })
        return screen
    }
}
