package com.example.data.maps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffRouteGuardTest {

    @Test
    fun requiresThreeAccurateOffRouteFixes() {
        val guard = OffRouteGuard()
        assertFalse(guard.shouldReroute(180.0, 20f, 1_000L))
        assertFalse(guard.shouldReroute(180.0, 20f, 2_500L))
        assertTrue(guard.shouldReroute(180.0, 20f, 4_000L))
    }

    @Test
    fun ignoresSingleGpsJumpAndPoorAccuracy() {
        val guard = OffRouteGuard()
        assertFalse(guard.shouldReroute(500.0, 20f, 1_000L))
        assertFalse(guard.shouldReroute(500.0, 180f, 2_000L))
        assertFalse(guard.shouldReroute(500.0, 20f, 3_000L))
        assertFalse(guard.shouldReroute(500.0, 20f, 4_000L))
        assertTrue(guard.shouldReroute(500.0, 20f, 5_000L))
    }

    @Test
    fun enforcesCooldownAndResetsWhenBackOnRoute() {
        val guard = OffRouteGuard()
        assertFalse(guard.shouldReroute(200.0, 15f, 1_000L))
        assertFalse(guard.shouldReroute(200.0, 15f, 2_000L))
        assertTrue(guard.shouldReroute(200.0, 15f, 3_000L))
        assertFalse(guard.shouldReroute(200.0, 15f, 4_000L))
        assertFalse(guard.shouldReroute(20.0, 15f, 5_000L))
        assertFalse(guard.shouldReroute(200.0, 15f, 6_000L))
        assertFalse(guard.shouldReroute(200.0, 15f, 7_000L))
        assertFalse(guard.shouldReroute(200.0, 15f, 8_000L))
        assertTrue(guard.shouldReroute(200.0, 15f, 50_000L))
    }
}
