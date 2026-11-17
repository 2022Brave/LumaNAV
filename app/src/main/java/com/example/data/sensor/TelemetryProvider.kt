package com.example.data.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

data class LiveGpsReading(
    val speedKmh: Int,
    val bearingDeg: Int,
    val accuracyMeters: Float?,
    val latitude: Double,
    val longitude: Double
)

class TelemetryProvider(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun observeGpsLocation(): Flow<LiveGpsReading?> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val speedKmh = if (location.hasSpeed()) {
                    (location.speed * 3.6f).roundToInt().coerceAtLeast(0)
                } else {
                    0
                }
                val bearing = if (location.hasBearing()) {
                    location.bearing.roundToInt()
                } else {
                    0
                }
                val accuracy = if (location.hasAccuracy()) location.accuracy else null
                trySend(
                    LiveGpsReading(
                        speedKmh = speedKmh,
                        bearingDeg = bearing,
                        accuracyMeters = accuracy,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            }
        }

        val providers = listOfNotNull(
            LocationManager.GPS_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) },
            LocationManager.NETWORK_PROVIDER.takeIf { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        )

        if (providers.isNotEmpty()) {
            // Emit freshest lastKnownLocation across all enabled providers immediately
            val bestLast = providers
                .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            if (bestLast != null) {
                listener.onLocationChanged(bestLast)
            }

            providers.forEach { provider ->
                runCatching {
                    locationManager.requestLocationUpdates(
                        provider,
                        1500L,
                        1f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        } else {
            trySend(null)
        }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }
}
