package com.example.data.sensor

import kotlin.math.roundToInt

/**
 * Small, deterministic GPS quality and heading policies shared by the location pipeline.
 * Location age and accuracy are kept explicit so stale fixes are never presented as live.
 */
object GpsReadingPolicy {
    const val MAX_ACCURACY_METERS = 250f
    const val MAX_LAST_KNOWN_AGE_MILLIS = 30 * 60 * 1000L
    const val MAX_LIVE_FIX_AGE_MILLIS = 2 * 60 * 1000L
    const val LIVE_FIX_FRESHNESS_MILLIS = 15_000L
    private const val MIN_BEARING_SPEED_KMH = 5

    fun isAcceptableFix(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        ageMillis: Long,
        isLastKnown: Boolean
    ): Boolean {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return false
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        if (ageMillis < 0L) return false
        val maxAge = if (isLastKnown) MAX_LAST_KNOWN_AGE_MILLIS else MAX_LIVE_FIX_AGE_MILLIS
        if (ageMillis > maxAge) return false
        return accuracyMeters == null ||
            (accuracyMeters.isFinite() && accuracyMeters >= 0f && accuracyMeters <= MAX_ACCURACY_METERS)
    }

    fun isFreshLiveFix(isLastKnown: Boolean, ageMillis: Long): Boolean =
        !isLastKnown && ageMillis in 0..LIVE_FIX_FRESHNESS_MILLIS

    fun stabilizedHeading(previousDegrees: Int, targetDegrees: Int, speedKmh: Int): Int {
        val previous = normalizeBearing(previousDegrees.toFloat())
        if (speedKmh < MIN_BEARING_SPEED_KMH) return previous.roundToInt() % 360
        val target = normalizeBearing(targetDegrees.toFloat())
        val shortestDelta = ((target - previous + 540f) % 360f) - 180f
        return normalizeBearing(previous + shortestDelta * 0.28f).roundToInt() % 360
    }

    private fun normalizeBearing(value: Float): Float = ((value % 360f) + 360f) % 360f
}
