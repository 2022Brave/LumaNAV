package com.example.data.maps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import com.example.data.model.DestinationCandidate
import com.example.data.model.DestinationSearchOutcome
import com.example.data.model.ServiceAvailabilityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
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
                reason = "Enter a destination name to search."
            )
        }

        if (!isNetworkOnline()) {
            return@withContext DestinationSearchOutcome.Unavailable(
                query = clean,
                serviceState = ServiceAvailabilityState.OFFLINE,
                reason = "Device is offline. Live destination search requires an internet connection."
            )
        }

        val cacheKey = buildString {
            append(clean.lowercase(Locale.ROOT))
            append('|')
            append(nearLat?.let { String.format(Locale.US, "%.2f", it) } ?: "global")
            append('|')
            append(nearLon?.let { String.format(Locale.US, "%.2f", it) } ?: "global")
        }
        searchCache[cacheKey]?.let { cached ->
            return@withContext DestinationSearchOutcome.Matches(
                query = clean,
                serviceState = ServiceAvailabilityState.LIVE,
                candidates = cached
            )
        }

        // Photon is an independent OpenStreetMap geocoder, so destination search does not
        // depend on Google Play services or the Android system Geocoder being installed.
        val photonCandidates = withTimeoutOrNull(9_000L) {
            runCatching { fetchPhotonCandidates(clean, nearLat, nearLon) }.getOrNull()
        }
        if (photonCandidates != null) {
            cacheSearch(cacheKey, photonCandidates)
            return@withContext DestinationSearchOutcome.Matches(
                query = clean,
                serviceState = ServiceAvailabilityState.LIVE,
                candidates = photonCandidates
            )
        }

        // Keep Android Geocoder as a fallback for devices where its provider is available.
        if (!isGeocoderAvailable()) {
            return@withContext DestinationSearchOutcome.Unavailable(
                query = clean,
                serviceState = ServiceAvailabilityState.API_UNAVAILABLE,
                reason = "OpenStreetMap place search is temporarily unavailable and this device has no Android Geocoder provider."
            )
        }

        try {
            val addresses = withTimeout(8_000L) {
                fetchAddressesFromGeocoder(clean, nearLat, nearLon)
            }
            val mapped = addresses.mapIndexed { index, addr ->
                val title = addr.featureName?.takeIf { it.isNotBlank() }
                    ?: addr.locality?.takeIf { it.isNotBlank() }
                    ?: addr.subAdminArea?.takeIf { it.isNotBlank() }
                    ?: clean
                val line = listOfNotNull(
                    addr.thoroughfare,
                    addr.locality,
                    addr.adminArea,
                    addr.countryName
                ).distinct().joinToString(", ").ifBlank { addr.getAddressLine(0) ?: clean }

                DestinationCandidate(
                    id = "geo_${index}_${addr.latitude}_${addr.longitude}",
                    name = title,
                    addressSummary = line,
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    category = "Android Geocoder",
                    isDemo = false
                )
            }
            cacheSearch(cacheKey, mapped)
            DestinationSearchOutcome.Matches(
                query = clean,
                serviceState = ServiceAvailabilityState.LIVE,
                candidates = mapped
            )
        } catch (_: Exception) {
            DestinationSearchOutcome.Unavailable(
                query = clean,
                serviceState = ServiceAvailabilityState.API_UNAVAILABLE,
                reason = "OpenStreetMap place search and the Android Geocoder fallback are unavailable."
            )
        }
    }

    private suspend fun fetchPhotonCandidates(
        query: String,
        nearLat: Double?,
        nearLon: Double?
    ): List<DestinationCandidate> = photonRequestMutex.withLock {
        // The hosted Photon demo is a shared community service. Keep requests serial and
        // at least one second apart; searches are only issued on explicit user submission.
        val elapsed = SystemClock.elapsedRealtime() - lastPhotonRequestAt
        if (lastPhotonRequestAt > 0L && elapsed < PHOTON_MIN_INTERVAL_MS) {
            delay(PHOTON_MIN_INTERVAL_MS - elapsed)
        }
        lastPhotonRequestAt = SystemClock.elapsedRealtime()

        val builder = Uri.parse("https://photon.komoot.io/api/").buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("limit", "5")
            .appendQueryParameter("lang", "en")
        if (nearLat != null && nearLon != null &&
            nearLat.isFinite() && nearLon.isFinite() &&
            nearLat in -90.0..90.0 && nearLon in -180.0..180.0
        ) {
            builder.appendQueryParameter("lat", nearLat.toString())
            builder.appendQueryParameter("lon", nearLon.toString())
            builder.appendQueryParameter("zoom", "12")
        }

        val connection = (URL(builder.build().toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("Accept", "application/geo+json, application/json")
            setRequestProperty("User-Agent", "LumaNAV-Android/1.0 (personal navigation app)")
        }
        val body = try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Photon HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val features = JSONObject(body).optJSONArray("features") ?: return@withLock emptyList()
        buildList {
            for (index in 0 until features.length()) {
                val feature = features.optJSONObject(index) ?: continue
                val coordinates = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                if (coordinates.length() < 2) continue
                val lon = coordinates.optDouble(0, Double.NaN)
                val lat = coordinates.optDouble(1, Double.NaN)
                if (!lat.isFinite() || lat !in -90.0..90.0 ||
                    !lon.isFinite() || lon !in -180.0..180.0
                ) continue

                val properties = feature.optJSONObject("properties") ?: JSONObject()
                val name = properties.optString("name")
                    .takeIf { it.isNotBlank() }
                    ?: properties.optString("street").takeIf { it.isNotBlank() }
                    ?: properties.optString("city").takeIf { it.isNotBlank() }
                    ?: query
                val address = listOf(
                    properties.optString("street"),
                    properties.optString("district"),
                    properties.optString("city"),
                    properties.optString("state"),
                    properties.optString("postcode"),
                    properties.optString("country")
                ).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { name }
                val osmId = properties.optString("osm_id").ifBlank { "${lat}_${lon}" }
                val category = properties.optString("osm_value")
                    .ifBlank { properties.optString("osm_key") }
                    .ifBlank { "OpenStreetMap place" }

                add(
                    DestinationCandidate(
                        id = "photon_${properties.optString("osm_type")}_$osmId",
                        name = name,
                        addressSummary = address,
                        latitude = lat,
                        longitude = lon,
                        category = category,
                        isDemo = false
                    )
                )
            }
        }
    }

    private fun cacheSearch(key: String, candidates: List<DestinationCandidate>) {
        if (searchCache.size >= MAX_SEARCH_CACHE_ENTRIES) searchCache.clear()
        searchCache[key] = candidates
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

        // Valhalla is attempted first. Its public OSM endpoint is keyless; OSRM remains
        // an independent fallback when Valhalla is unavailable or returns an unusable route.
        val valhalla = withTimeoutOrNull(9_000L) {
            runCatching {
                val request = JSONObject().apply {
                    put("locations", org.json.JSONArray().put(
                        JSONObject().put("lat", startLat).put("lon", startLon)
                    ).put(JSONObject().put("lat", endLat).put("lon", endLon)))
                    put("costing", "auto")
                    put("units", "kilometers")
                    put("directions_options", JSONObject().put("units", "kilometers"))
                }
                val endpoint = "https://valhalla1.openstreetmap.de/route?json=" +
                    Uri.encode(request.toString())
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "LumaNAV-Android/1.0")
                }
                val body = try {
                    val code = connection.responseCode
                    if (code !in 200..299) error("Valhalla HTTP $code")
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
                parseValhallaRoute(JSONObject(body), destinationFallbackName)
            }.getOrNull()
        }

        valhalla ?: runCatching {
            val urlStr = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true",
                startLon, startLat, endLon, endLat
            )
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "LumaNAV-Android/1.0")
            }
            val body = try {
                val code = conn.responseCode
                if (code !in 200..299) error("OSRM HTTP $code")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            parseOsrmRoute(JSONObject(body), destinationFallbackName)
        }.getOrNull()
    }

    private fun parseValhallaRoute(json: JSONObject, destinationFallbackName: String): LiveDrivingRoute? {
        val trip = json.optJSONObject("trip") ?: return null
        val summary = trip.optJSONObject("summary") ?: return null
        val legs = trip.optJSONArray("legs") ?: return null
        val leg = legs.optJSONObject(0) ?: return null
        val shape = leg.optString("shape").takeIf { it.isNotBlank() } ?: return null
        val points = decodePolyline6(shape)
        if (points.size < 2) return null

        val distanceKm = summary.optDouble("length", 0.0).toFloat()
        val durationMinutes = (summary.optDouble("time", 0.0) / 60.0).roundToInt().coerceAtLeast(1)
        val maneuvers = leg.optJSONArray("maneuvers")
        val next = when {
            maneuvers != null && maneuvers.length() > 1 -> maneuvers.optJSONObject(1)
            maneuvers != null && maneuvers.length() > 0 -> maneuvers.optJSONObject(0)
            else -> null
        }
        val road = next?.optJSONArray("street_names")?.optString(0)
            ?.takeIf { it.isNotBlank() } ?: destinationFallbackName
        val instruction = next?.optString("instruction")?.takeIf { it.isNotBlank() }
            ?: "Continue to $destinationFallbackName"
        val turnDistance = (next?.optDouble("length", 0.0) ?: 0.0) * 1000.0
        val eta = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
            Date(System.currentTimeMillis() + durationMinutes * 60_000L)
        )
        return LiveDrivingRoute(
            polylinePoints = points,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            etaTimeLabel = eta,
            nextTurnDistanceLabel = formatDistanceMeters(turnDistance),
            nextTurnRoad = road,
            nextTurnInstruction = instruction
        )
    }

    private fun parseOsrmRoute(json: JSONObject, destinationFallbackName: String): LiveDrivingRoute? {
        val route = json.optJSONArray("routes")?.optJSONObject(0) ?: return null
        val distanceMeters = route.optDouble("distance", 0.0)
        val durationSeconds = route.optDouble("duration", 0.0)
        val distanceKm = (distanceMeters / 1000.0).toFloat()
        val durationMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
        val coordinates = route.optJSONObject("geometry")?.optJSONArray("coordinates")
        val points = mutableListOf<Pair<Double, Double>>()
        if (coordinates != null) {
            for (i in 0 until coordinates.length()) {
                val pair = coordinates.optJSONArray(i) ?: continue
                points.add(pair.optDouble(1) to pair.optDouble(0))
            }
        }
        if (points.size < 2) return null
        val steps = route.optJSONArray("legs")?.optJSONObject(0)?.optJSONArray("steps")
        val next = when {
            steps != null && steps.length() > 1 -> steps.optJSONObject(1)
            steps != null && steps.length() > 0 -> steps.optJSONObject(0)
            else -> null
        }
        val first = steps?.optJSONObject(0)
        val turnDistance = first?.optDouble("distance", distanceMeters) ?: distanceMeters
        val road = next?.optString("name")?.takeIf { it.isNotBlank() } ?: destinationFallbackName
        val maneuver = next?.optJSONObject("maneuver")
        val instruction = listOf(
            maneuver?.optString("type").orEmpty(),
            maneuver?.optString("modifier").orEmpty(),
            if (road.isNotBlank()) "onto $road" else ""
        ).filter { it.isNotBlank() }.joinToString(" ")
            .replaceFirstChar { it.uppercase() }.ifBlank { "Continue to $destinationFallbackName" }
        val eta = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
            Date(System.currentTimeMillis() + durationMinutes * 60_000L)
        )
        return LiveDrivingRoute(
            polylinePoints = points,
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            etaTimeLabel = eta,
            nextTurnDistanceLabel = formatDistanceMeters(turnDistance),
            nextTurnRoad = road,
            nextTurnInstruction = instruction
        )
    }

    private fun decodePolyline6(encoded: String): List<Pair<Double, Double>> {
        val result = ArrayList<Pair<Double, Double>>()
        var index = 0
        var latitude = 0
        var longitude = 0
        while (index < encoded.length) {
            var shift = 0
            var value = 0
            var byte: Int
            do {
                if (index >= encoded.length) return result
                byte = encoded[index++].code - 63
                value = value or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            latitude += if ((value and 1) != 0) (value shr 1).inv() else value shr 1

            shift = 0
            value = 0
            do {
                if (index >= encoded.length) return result
                byte = encoded[index++].code - 63
                value = value or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            longitude += if ((value and 1) != 0) (value shr 1).inv() else value shr 1
            result.add(latitude / 1_000_000.0 to longitude / 1_000_000.0)
        }
        return result
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
        private const val PHOTON_MIN_INTERVAL_MS = 1_000L
        private const val MAX_SEARCH_CACHE_ENTRIES = 64
        private val photonRequestMutex = Mutex()
        @Volatile private var lastPhotonRequestAt = 0L
        private val searchCache = ConcurrentHashMap<String, List<DestinationCandidate>>()

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
