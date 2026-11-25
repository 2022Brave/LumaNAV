package com.example.data.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMatcherTest {

    @Test
    fun projectsLocationOntoRouteAndCalculatesRemainingDistance() {
        val route = listOf(
            28.0000 to 77.0000,
            28.0000 to 77.0100,
            28.0000 to 77.0200
        )
        val match = RouteMatcher.matchLocationToRoute(28.0002, 77.0100, route)
        assertNotNull(match)
        assertTrue(match!!.distanceToRouteMeters < 30.0)
        assertTrue(match.remainingDistanceMeters in 900.0..1_300.0)
        assertEquals(0.5, match.progressFraction, 0.08)
    }

    @Test
    fun locationFarFromRouteRetainsCrossTrackDistance() {
        val route = listOf(28.0000 to 77.0000, 28.0000 to 77.0100)
        val match = RouteMatcher.matchLocationToRoute(28.01, 77.005, route)
        assertNotNull(match)
        assertTrue(match!!.distanceToRouteMeters > 900.0)
    }

    @Test
    fun invalidOrInsufficientRouteReturnsNull() {
        assertNull(RouteMatcher.matchLocationToRoute(28.0, 77.0, emptyList()))
        assertNull(RouteMatcher.matchLocationToRoute(28.0, 77.0, listOf(28.0 to 77.0)))
        assertNull(RouteMatcher.matchLocationToRoute(91.0, 77.0, listOf(28.0 to 77.0, 28.1 to 77.1)))
    }
}
