package com.example.data.maps

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RouteMatch(
    val distanceToRouteMeters: Double,
    val remainingDistanceMeters: Double,
    val progressFraction: Double,
    val segmentIndex: Int
)

/**
 * Lightweight route projection for navigation progress. It uses a local equirectangular
 * projection for segment snapping and haversine distances for route length.
 */
object RouteMatcher {

    fun matchLocationToRoute(
        latitude: Double,
        longitude: Double,
        route: List<Pair<Double, Double>>
    ): RouteMatch? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        if (route.size < 2 || route.any { (lat, lon) ->
                !lat.isFinite() || lat !in -90.0..90.0 ||
                    !lon.isFinite() || lon !in -180.0..180.0
            }
        ) return null

        val cosLatitude = cos(Math.toRadians(latitude)).coerceAtLeast(0.000001)
        fun xy(point: Pair<Double, Double>): Pair<Double, Double> =
            (Math.toRadians(point.second - longitude) * cosLatitude) to
                Math.toRadians(point.first - latitude)

        var cumulativeMeters = 0.0
        var totalMeters = 0.0
        val segmentLengths = DoubleArray(route.size - 1) { index ->
            haversineMeters(route[index], route[index + 1])
        }
        totalMeters = segmentLengths.sum()

        var bestDistance = Double.POSITIVE_INFINITY
        var bestAlong = 0.0
        var bestSegment = 0

        for (index in 0 until route.lastIndex) {
            val a = xy(route[index])
            val b = xy(route[index + 1])
            val dx = b.first - a.first
            val dy = b.second - a.second
            val lengthSquared = dx * dx + dy * dy
            val t = if (lengthSquared <= 1e-18) {
                0.0
            } else {
                ((-(a.first * dx + a.second * dy) / lengthSquared)).coerceIn(0.0, 1.0)
            }
            val projected = (a.first + t * dx) to (a.second + t * dy)
            val distanceMeters = sqrt(projected.first * projected.first + projected.second * projected.second) * 6_371_000.0
            if (distanceMeters < bestDistance) {
                bestDistance = distanceMeters
                bestAlong = cumulativeMeters + segmentLengths[index] * t
                bestSegment = index
            }
            cumulativeMeters += segmentLengths[index]
        }

        val remaining = max(totalMeters - bestAlong, 0.0)
        return RouteMatch(
            distanceToRouteMeters = bestDistance,
            remainingDistanceMeters = remaining,
            progressFraction = if (totalMeters > 0.0) (bestAlong / totalMeters).coerceIn(0.0, 1.0) else 0.0,
            segmentIndex = bestSegment
        )
    }

    private fun haversineMeters(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val lat1 = Math.toRadians(a.first)
        val lat2 = Math.toRadians(b.first)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.second - a.second)
        val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            cos(lat1) * cos(lat2) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2.0 * 6_371_000.0 * kotlin.math.atan2(sqrt(h), sqrt(max(1.0 - h, 0.0)))
    }
}
