package com.example.data.model

/**
 * Explicit runtime service state separating real live integrations from Demo, Offline,
 * Permission Denied, and API Unavailable states. Never silently substitutes demo data for live failures.
 */
enum class ServiceAvailabilityState(val label: String) {
    LIVE("Live"),
    DEMO("Demo"),
    OFFLINE("Offline"),
    PERMISSION_DENIED("Permission Denied"),
    API_UNAVAILABLE("API Unavailable")
}

data class SavedPlace(
    val id: String,
    val name: String,
    val addressSummary: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "Saved place"
) {
    fun toDestinationCandidate() = DestinationCandidate(
        id = id,
        name = name,
        addressSummary = addressSummary,
        latitude = latitude,
        longitude = longitude,
        category = category,
        isDemo = false
    )
}

data class DestinationCandidate(
    val id: String,
    val name: String,
    val addressSummary: String,
    val latitude: Double?,
    val longitude: Double?,
    val category: String = "Geocoded Place",
    val isDemo: Boolean = false
)

sealed interface DestinationSearchOutcome {
    data class Matches(
        val query: String,
        val serviceState: ServiceAvailabilityState,
        val candidates: List<DestinationCandidate>
    ) : DestinationSearchOutcome

    data class Unavailable(
        val query: String,
        val serviceState: ServiceAvailabilityState,
        val reason: String
    ) : DestinationSearchOutcome
}
