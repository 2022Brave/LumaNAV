package com.example.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsReadingPolicyTest {

    @Test
    fun rejectsInvalidCoordinatesAndPoorAccuracy() {
        assertFalse(GpsReadingPolicy.isAcceptableFix(91.0, 10.0, 5f, 0L, false))
        assertFalse(GpsReadingPolicy.isAcceptableFix(28.6, 181.0, 5f, 0L, false))
        assertFalse(GpsReadingPolicy.isAcceptableFix(0.0, 0.0, 5f, 0L, false))
        assertFalse(GpsReadingPolicy.isAcceptableFix(28.6, 77.2, 400f, 0L, false))
        assertTrue(GpsReadingPolicy.isAcceptableFix(28.6, 77.2, 25f, 0L, false))
    }

    @Test
    fun cachedFixHasShorterTrustWindowThanLiveProviderFix() {
        assertTrue(
            GpsReadingPolicy.isAcceptableFix(
                28.6, 77.2, 40f,
                GpsReadingPolicy.MAX_LAST_KNOWN_AGE_MILLIS,
                true
            )
        )
        assertFalse(
            GpsReadingPolicy.isAcceptableFix(
                28.6, 77.2, 40f,
                GpsReadingPolicy.MAX_LAST_KNOWN_AGE_MILLIS + 1L,
                true
            )
        )
        assertFalse(GpsReadingPolicy.isFreshLiveFix(isLastKnown = true, ageMillis = 0L))
        assertTrue(GpsReadingPolicy.isFreshLiveFix(isLastKnown = false, ageMillis = 2_000L))
        assertFalse(GpsReadingPolicy.isFreshLiveFix(isLastKnown = false, ageMillis = 20_000L))
    }

    @Test
    fun headingSmoothingUsesShortestArcAndHoldsWhileStationary() {
        assertEquals(356, GpsReadingPolicy.stabilizedHeading(350, 10, 60))
        assertEquals(350, GpsReadingPolicy.stabilizedHeading(350, 90, 0))
        assertEquals(4, GpsReadingPolicy.stabilizedHeading(10, 350, 60))
    }
}
