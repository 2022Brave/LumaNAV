package com.example.data.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt
import java.util.ArrayDeque

data class LiveGpsReading(
    val speedKmh: Int,
    val bearingDeg: Int,
    val accuracyMeters: Float?,
    val latitude: Double,
    val longitude: Double,
    val isLastKnown: Boolean = false,
    val ageMillis: Long = 0L,
    val hasBearing: Boolean = false
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
        var lastAcceptedTimestampMillis = 0L
        val acceptedSpeedSamples = ArrayDeque<Float>(3)
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

        fun emitLocation(location: Location, isLastKnown: Boolean) {
            val ageMillis = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
            val accuracy = if (location.hasAccuracy()) location.accuracy else null
            if (!isLastKnown && location.time < lastAcceptedTimestampMillis) return
            if (!GpsReadingPolicy.isAcceptableFix(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = accuracy,
                    ageMillis = ageMillis,
                    isLastKnown = isLastKnown
                )
            ) return

            lastAcceptedTimestampMillis = maxOf(lastAcceptedTimestampMillis, location.time)
            val measuredSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
            val speedAccuracyPoor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                location.hasSpeedAccuracy() && location.speedAccuracyMetersPerSecond > 2.5f
            val speedKmh = if (
                !location.hasSpeed() ||
                accuracy == null ||
                accuracy > 20f ||
                measuredSpeedKmh < 3.0f ||
                speedAccuracyPoor
            ) {
                acceptedSpeedSamples.clear()
                0
            } else {
                acceptedSpeedSamples.addLast(measuredSpeedKmh)
                while (acceptedSpeedSamples.size > 3) acceptedSpeedSamples.removeFirst()
                acceptedSpeedSamples.average().roundToInt().coerceAtLeast(0)
            }
            val hasBearing = location.hasBearing()
            trySend(
                LiveGpsReading(
                    speedKmh = speedKmh,
                    bearingDeg = if (hasBearing) location.bearing.roundToInt() else 0,
                    accuracyMeters = accuracy,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isLastKnown = isLastKnown,
                    ageMillis = ageMillis,
                    hasBearing = hasBearing
                )
            )
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                emitLocation(location, isLastKnown = false)
            }
        }

        val providers = listOfNotNull(
            LocationManager.GPS_PROVIDER.takeIf {
                runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
            },
            LocationManager.NETWORK_PROVIDER.takeIf {
                runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
            }
        )

        if (providers.isNotEmpty()) {
            // A recent system-cached fix can center the map while fresh GPS is acquired.
            // It is explicitly marked as fallback and never reported as a live fix.
            val bestLastKnown = providers
                .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            bestLastKnown?.let { emitLocation(it, isLastKnown = true) }

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
