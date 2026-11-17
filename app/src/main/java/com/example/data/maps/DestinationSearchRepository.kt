package com.example.data.maps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import com.example.data.model.DestinationCandidate
import com.example.data.model.DestinationSearchOutcome
import com.example.data.model.ServiceAvailabilityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LiveDrivingRoute(
    val polylinePoints: List<Pair<Double, Double>>,
    val distanceKm: Float,
    val durationMinutes: Int,
    val etaTimeLabel: String,
    val nextTurnDistanceLabel: String,
    val nextTurnRoad: String,
    val nextTurnInstruction: String
)

data class LiveWeatherReading(
    val tempC: Int,
    val condition: String
)

data class ExternalMapsTarget(
    val queryLabel: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Genuine Android Geocoder + Reverse Geocoder + OSRM road routing + Open-Meteo weather repository.
 * Honestly distinguishes LIVE lookups from OFFLINE and API_UNAVAILABLE.
 * Never fabricates search results when geocoding fails or returns empty.
 */
class DestinationSearchRepository(private val context: Context) {

    fun isNetworkOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isGeocoderAvailable(): Boolean {
        return runCatching { Geocoder.isPresent() }.getOrDefault(false)
    }

    suspend fun searchDestination(
        query: String,
        nearLat: Double? = null,
        nearLon: Double? = null
    ): DestinationSearchOutcome = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            return@withContext DestinationSearchOutcome.Unavailable(
                query = query,
                serviceState = ServiceAvailabilityState.API_UNAVAILABLE,
                reason = "Speak a destination name to search."
            )
        }

        if (!isNetworkOnline()) {
            return@withContext DestinationSearchOutcome.Unavailable(
                query = clean,
                serviceState = ServiceAvailabilityState.OFFLINE,
                reason = "Device is offline. Network connectivity is required for live destination search."
            )
        }

        if (!isGeocoderAvailable()) {
            return@withContext DestinationSearchOutcome.Unavailable(
                query = clean,
                serviceState = ServiceAvailabilityState.API_UNAVAILABLE,
                reason = "Android Geocoder backend is unavailable on this device."
            )
        }

        try {
            val addresses = withTimeout(10_000L) {
                fetchAddressesFromGeocoder(clean, nearLat, nearLon)
            }
            if (addresses.isEmpty()) {
                DestinationSearchOutcome.Matches(
                    query = clean,
                    serviceState = ServiceAvailabilityState.LIVE,
                    candidates = emptyList()
                )
            } else {
                val mapped = addresses.mapIndexed { index, addr ->
                    val title = addr.featureName?.takeIf { it.isNotBlank() }
                        ?: addr.locality?.takeIf { it.isNotBlank() }
                        ?: addr.subAdminArea?.takeIf { it.isNotBlank() }
                        ?: clean
                    val line = buildString {
                        listOfNotNull(addr.thoroughfare, addr.locality, addr.adminArea, addr.countryName)
                            .distinct()
                            .joinToString(", ")
                            .let { append(it) }
                    }.ifBlank { addr.getAddressLine(0) ?: clean }

                    DestinationCandidate(
                        id = "geo_${index}_${addr.latitude}_${addr.longitude}",
                        name = title,
                        addressSummary = line,
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        category = "Geocoded Place",
                        isDemo = false
                    )
                }
                DestinationSearchOutcome.Matches(
                    query = clean,
                    serviceState = ServiceAvailabilityState.LIVE,
                    candidates = mapped
                )
            }
        } catch (_: Exception) {
            DestinationSearchOutcome.Unavailable(
                query = clean,
                serviceState = ServiceAvailabilityState.API_UNAVAILABLE,
                reason = "Geocoder service request failed."
            )
        }
    }

    suspend fun reverseGeocodeLocation(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        if (!isGeocoderAvailable()) return@withContext null
        runCatching {
            withTimeout(5_000L) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(emptyList())
                            }
                        })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1).orEmpty()
                }
                val first = results.firstOrNull() ?: return@withTimeout null
                listOfNotNull(
                    first.thoroughfare?.takeIf { it.isNotBlank() },
                    first.subLocality?.takeIf { it.isNotBlank() },
                    first.locality?.takeIf { it.isNotBlank() }
                ).distinct().take(2).joinToString(", ")
                    .ifBlank { first.adminArea ?: first.featureName }
            }
        }.getOrNull()
    }

    suspend fun fetchDrivingRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        destinationFallbackName: String
    ): LiveDrivingRoute? = withContext(Dispatchers.IO) {
        if (!isNetworkOnline()) return@withContext null
        runCatching {
            val urlStr = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true",
                startLon,
                startLat,
                endLon,
                endLat
            )
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "AuvionyxAndroid/1.0")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val routeObj = json.optJSONArray("routes")?.optJSONObject(0) ?: return@withContext null
            val distanceMeters = routeObj.optDouble("distance", 0.0)
            val durationSec = routeObj.optDouble("duration", 0.0)
            val distKm = ((distanceMeters / 100.0).roundToInt() / 10f).coerceAtLeast(0.1f)
            val durationMin = (durationSec / 60.0).roundToInt().coerceAtLeast(1)

            val coordsArray = routeObj.optJSONObject("geometry")?.optJSONArray("coordinates")
            val polyline = mutableListOf<Pair<Double, Double>>()
            if (coordsArray != null) {
                for (i in 0 until coordsArray.length()) {
                    val pair = coordsArray.optJSONArray(i) ?: continue
                    val lon = pair.optDouble(0)
                    val lat = pair.optDouble(1)
                    polyline.add(lat to lon)
                }
            }

            // Extract first upcoming turn maneuver (prefer step[1] if step[0] is initial depart)
            val steps = routeObj.optJSONArray("legs")?.optJSONObject(0)?.optJSONArray("steps")
            val nextStep = when {
                steps != null && steps.length() > 1 -> steps.optJSONObject(1)
                steps != null && steps.length() > 0 -> steps.optJSONObject(0)
                else -> null
            }
            val firstStep = steps?.optJSONObject(0)
            val stepDistanceMeters = firstStep?.optDouble("distance", distanceMeters) ?: distanceMeters
            val stepRoad = nextStep?.optString("name")?.takeIf { it.isNotBlank() }
                ?: destinationFallbackName
            val maneuverType = nextStep?.optJSONObject("maneuver")?.optString("type").orEmpty()
            val maneuverMod = nextStep?.optJSONObject("maneuver")?.optString("modifier").orEmpty()
            val instruction = listOf(maneuverType, maneuverMod, if (stepRoad.isNotBlank()) "onto $stepRoad" else "")
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Continue to $destinationFallbackName" }

            val etaMs = System.currentTimeMillis() + durationMin * 60_000L
            val etaStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(etaMs))

            LiveDrivingRoute(
                polylinePoints = polyline,
                distanceKm = distKm,
                durationMinutes = durationMin,
                etaTimeLabel = etaStr,
                nextTurnDistanceLabel = formatDistanceMeters(stepDistanceMeters),
                nextTurnRoad = stepRoad,
                nextTurnInstruction = instruction
            )
        }.getOrNull()
    }

    suspend fun fetchCurrentWeather(lat: Double, lon: Double): LiveWeatherReading? = withContext(Dispatchers.IO) {
        if (!isNetworkOnline()) return@withContext null
        runCatching {
            val urlStr = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                lat,
                lon
            )
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "AuvionyxAndroid/1.0")
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val current = JSONObject(body).optJSONObject("current") ?: return@withContext null
            val tempC = current.optDouble("temperature_2m", 24.0).roundToInt()
            val wmoCode = current.optInt("weather_code", 0)
            LiveWeatherReading(tempC = tempC, condition = mapWmoCode(wmoCode))
        }.getOrNull()
    }

    private suspend fun fetchAddressesFromGeocoder(
        query: String,
        nearLat: Double?,
        nearLon: Double?
    ): List<Address> {
        val geocoder = Geocoder(context, Locale.getDefault())
        val useBounded = nearLat != null && nearLon != null
        val minLat = ((nearLat ?: 0.0) - 0.45).coerceIn(-90.0, 90.0)
        val maxLat = ((nearLat ?: 0.0) + 0.45).coerceIn(-90.0, 90.0)
        val minLon = ((nearLon ?: 0.0) - 0.45).coerceIn(-180.0, 180.0)
        val maxLon = ((nearLon ?: 0.0) + 0.45).coerceIn(-180.0, 180.0)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val boundedResults = if (useBounded) {
                suspendCancellableCoroutine<List<Address>> { cont ->
                    geocoder.getFromLocationName(query, 5, minLat, minLon, maxLat, maxLon, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    })
                }
            } else emptyList()

            boundedResults.ifEmpty {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    })
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val bounded = if (useBounded) {
                geocoder.getFromLocationName(query, 5, minLat, minLon, maxLat, maxLon).orEmpty()
            } else emptyList()
            @Suppress("DEPRECATION")
            bounded.ifEmpty { geocoder.getFromLocationName(query, 5).orEmpty() }
        }
    }

    companion object {
        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return (((r * c) * 10.0).roundToInt() / 10f).coerceAtLeast(0.1f)
        }

        fun formatDistanceMeters(meters: Double): String {
            return if (meters >= 1000.0) {
                String.format(Locale.US, "%.1f km", meters / 1000.0)
            } else {
                "${meters.roundToInt().coerceAtLeast(10)} m"
            }
        }

        fun parseExternalMapsIntent(dataString: String?, sharedText: String?): ExternalMapsTarget? {
            if (!dataString.isNullOrBlank()) {
                val uri = runCatching { Uri.parse(dataString) }.getOrNull()
                if (uri != null) {
                    val scheme = uri.scheme?.lowercase(Locale.ROOT)
                    if (scheme == "google.navigation") {
                        val rawQ = if (uri.isHierarchical) {
                            uri.getQueryParameter("q")
                        } else {
                            dataString.substringAfter("q=", "").substringBefore("&")
                        }
                        if (!rawQ.isNullOrBlank()) {
                            val decoded = runCatching { URLDecoder.decode(rawQ, "UTF-8") }.getOrDefault(Uri.decode(rawQ))
                            return parseCoordOrQuery(decoded)
                        }
                    }
                    if (scheme == "geo") {
                        val ssp = uri.schemeSpecificPart.orEmpty()
                        val rawQueryQ = if (uri.isHierarchical) {
                            uri.getQueryParameter("q")
                        } else {
                            ssp.substringAfter("q=", "").substringBefore("&")
                        }
                        if (!rawQueryQ.isNullOrBlank()) {
                            val decoded = runCatching { URLDecoder.decode(rawQueryQ, "UTF-8") }.getOrDefault(Uri.decode(rawQueryQ))
                            return parseCoordOrQuery(decoded)
                        }
                        val coordPart = ssp.substringBefore("?")
                        parseCoordPair(coordPart)?.let { (lat, lon) ->
                            return ExternalMapsTarget(
                                queryLabel = String.format(Locale.US, "%.4f, %.4f", lat, lon),
                                latitude = lat,
                                longitude = lon
                            )
                        }
                    }
                }
            }
            if (!sharedText.isNullOrBlank()) {
                val clean = sharedText.lines().firstOrNull { !it.trim().startsWith("http") }?.trim()
                    ?: sharedText.trim()
                val coord = Regex("""(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)""").find(sharedText)
                if (coord != null) {
                    val lat = coord.groupValues[1].toDoubleOrNull()
                    val lon = coord.groupValues[2].toDoubleOrNull()
                    if (lat != null && lon != null) {
                        return ExternalMapsTarget(queryLabel = clean.take(48), latitude = lat, longitude = lon)
                    }
                }
                if (clean.isNotBlank()) {
                    return ExternalMapsTarget(queryLabel = clean.take(64))
                }
            }
            return null
        }

        private fun parseCoordOrQuery(raw: String): ExternalMapsTarget {
            val cleaned = raw.trim()
            val coord = parseCoordPair(cleaned.substringBefore("(").trim())
            val label = cleaned.substringAfter("(", "").substringBefore(")").ifBlank { cleaned }
            return if (coord != null) {
                ExternalMapsTarget(queryLabel = label, latitude = coord.first, longitude = coord.second)
            } else {
                ExternalMapsTarget(queryLabel = cleaned)
            }
        }

        private fun parseCoordPair(raw: String): Pair<Double, Double>? {
            val parts = raw.split(",").map { it.trim() }
            if (parts.size != 2) return null
            val lat = parts[0].toDoubleOrNull() ?: return null
            val lon = parts[1].toDoubleOrNull() ?: return null
            if (lat == 0.0 && lon == 0.0) return null
            return lat to lon
        }

        private fun mapWmoCode(code: Int): String = when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55, 61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95, 96, 99 -> "Storm"
            else -> "Live"
        }
    }
}
