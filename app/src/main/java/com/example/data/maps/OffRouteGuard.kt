package com.example.data.maps

/**
 * Requires repeated, reasonably accurate off-route observations before requesting a reroute.
 * This avoids rerouting on a single GPS jump or a temporarily poor fix.
 */
class OffRouteGuard(
    private val requiredStrikes: Int = 3,
    private val rerouteCooldownMillis: Long = 45_000L,
    private val maxTrustedAccuracyMeters: Float = 100f,
    private val minimumOffRouteMeters: Double = 75.0,
    private val accuracyMultiplier: Double = 2.0
) {
    private var strikes = 0
    private var lastRerouteAtMillis = Long.MIN_VALUE

    fun shouldReroute(
        distanceToRouteMeters: Double,
        accuracyMeters: Float?,
        nowMillis: Long
    ): Boolean {
        if (!distanceToRouteMeters.isFinite() ||
            accuracyMeters == null ||
            !accuracyMeters.isFinite() ||
            accuracyMeters < 0f ||
            accuracyMeters > maxTrustedAccuracyMeters
        ) {
            strikes = 0
            return false
        }

        val threshold = maxOf(minimumOffRouteMeters, accuracyMeters * accuracyMultiplier)
        if (distanceToRouteMeters <= threshold) {
            strikes = 0
            return false
        }

        strikes += 1
        if (strikes < requiredStrikes) return false
        if (lastRerouteAtMillis != Long.MIN_VALUE &&
            nowMillis - lastRerouteAtMillis < rerouteCooldownMillis
        ) return false

        strikes = 0
        lastRerouteAtMillis = nowMillis
        return true
    }

    fun reset() {
        strikes = 0
    }
}
