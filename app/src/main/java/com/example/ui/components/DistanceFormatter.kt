package com.example.ui.components

import java.util.Locale

/** Formats route distances consistently for glanceable in-car UI. */
fun formatNavigationDistance(distanceKm: Float): String {
    if (!distanceKm.isFinite() || distanceKm < 0f) return "--"
    return when {
        distanceKm < 1f -> "${(distanceKm * 1000f).toInt().coerceAtLeast(0)} m"
        distanceKm < 100f -> String.format(Locale.getDefault(), "%.1f km", distanceKm)
        else -> String.format(Locale.getDefault(), "%.0f km", distanceKm)
    }
}
