package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AodStyle
import com.example.data.model.DashboardSplitRatio
import com.example.data.model.HudAccentChoice
import com.example.data.model.HudConfig
import com.example.data.model.HudCockpitStyle
import com.example.data.model.HudLayoutMode
import com.example.data.model.HudPosition
import com.example.data.model.MediaAppSource
import com.example.data.model.SavedPlace
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.auvionyxDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auvionyx_preferences"
)

data class AuvionyxSettingsState(
    val useAdaptiveAlbumColors: Boolean = false,
    val daylightHighContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val dashboardRatio: DashboardSplitRatio = DashboardSplitRatio.RATIO_70_30,
    val hudConfig: HudConfig = HudConfig(),
    val aodStyle: AodStyle = AodStyle.MINIMAL,
    val defaultMediaSource: MediaAppSource = MediaAppSource.LASTWAVE,
    val useMetricUnits: Boolean = true,
    val showTrafficOverlay: Boolean = true,
    val showSatelliteGrid: Boolean = false,
    val optionalCarLauncherEnabled: Boolean = false,
)

class AuvionyxPreferencesRepository(private val context: Context) {

    private object Keys {
        val ADAPTIVE_ALBUM_COLORS = booleanPreferencesKey("adaptive_album_colors")
        val DAYLIGHT_HIGH_CONTRAST = booleanPreferencesKey("daylight_high_contrast")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val DASHBOARD_RATIO = stringPreferencesKey("dashboard_ratio")
        val HUD_LAYOUT = stringPreferencesKey("hud_layout")
        val HUD_ACCENT = stringPreferencesKey("hud_accent")
        val HUD_FONT_SCALE = floatPreferencesKey("hud_font_scale")
        val HUD_POSITION = stringPreferencesKey("hud_position")
        val HUD_MIRROR = booleanPreferencesKey("hud_mirror")
        val HUD_SHOW_SPEED = booleanPreferencesKey("hud_show_speed")
        val HUD_SHOW_LIMIT = booleanPreferencesKey("hud_show_limit")
        val HUD_SHOW_TURN = booleanPreferencesKey("hud_show_turn")
        val HUD_SHOW_COMPASS = booleanPreferencesKey("hud_show_compass")
        val HUD_SHOW_PROGRESS = booleanPreferencesKey("hud_show_progress")
        val HUD_SHOW_ETA = booleanPreferencesKey("hud_show_eta")
        val HUD_SHOW_MEDIA = booleanPreferencesKey("hud_show_media")
        val HUD_SPEED_CALIBRATION = floatPreferencesKey("hud_speed_calibration")
        val HUD_LAY_FLAT_START = booleanPreferencesKey("hud_lay_flat_start")
        val HUD_COCKPIT_STYLE = stringPreferencesKey("hud_cockpit_style")
        val AOD_STYLE = stringPreferencesKey("aod_style")
        val MEDIA_SOURCE = stringPreferencesKey("media_source")
        val METRIC_UNITS = booleanPreferencesKey("metric_units")
        val TRAFFIC_OVERLAY = booleanPreferencesKey("traffic_overlay")
        val SATELLITE_GRID = booleanPreferencesKey("satellite_grid")
        val CAR_LAUNCHER = booleanPreferencesKey("car_launcher")
        val SAVED_PLACES = stringPreferencesKey("saved_places")
    }

    val savedPlacesFlow: Flow<List<SavedPlace>> = context.auvionyxDataStore.data.map { prefs ->
        val json = runCatching { JSONArray(prefs[Keys.SAVED_PLACES].orEmpty()) }.getOrNull() ?: JSONArray()
        buildList {
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: continue
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                if (!lat.isFinite() || lat !in -90.0..90.0 || !lon.isFinite() || lon !in -180.0..180.0) continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                add(SavedPlace(
                    id = item.optString("id").ifBlank { "saved_$index" },
                    name = name,
                    addressSummary = item.optString("addressSummary"),
                    latitude = lat,
                    longitude = lon,
                    category = item.optString("category", "Saved place")
                ))
            }
        }
    }

    val settingsFlow: Flow<AuvionyxSettingsState> = context.auvionyxDataStore.data.map { prefs ->
        AuvionyxSettingsState(
            useAdaptiveAlbumColors = prefs[Keys.ADAPTIVE_ALBUM_COLORS] ?: false,
            daylightHighContrast = prefs[Keys.DAYLIGHT_HIGH_CONTRAST] ?: false,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            dashboardRatio = prefs[Keys.DASHBOARD_RATIO]?.let {
                runCatching { DashboardSplitRatio.valueOf(it) }.getOrNull()
            } ?: DashboardSplitRatio.RATIO_70_30,
            hudConfig = HudConfig(
                layoutMode = prefs[Keys.HUD_LAYOUT]?.let {
                    runCatching { HudLayoutMode.valueOf(it) }.getOrNull()
                } ?: HudLayoutMode.NAVIGATION,
                accentChoice = prefs[Keys.HUD_ACCENT]?.let {
                    runCatching { HudAccentChoice.valueOf(it) }.getOrNull()
                } ?: HudAccentChoice.ELECTRIC_CYAN,
                fontScale = prefs[Keys.HUD_FONT_SCALE] ?: 1.0f,
                speedPosition = prefs[Keys.HUD_POSITION]?.let {
                    runCatching { HudPosition.valueOf(it) }.getOrNull()
                } ?: HudPosition.LEFT,
                mirrorFlipForWindshield = prefs[Keys.HUD_MIRROR] ?: false,
                showSpeedometer = prefs[Keys.HUD_SHOW_SPEED] ?: true,
                showSpeedLimit = prefs[Keys.HUD_SHOW_LIMIT] ?: true,
                showNextTurn = prefs[Keys.HUD_SHOW_TURN] ?: true,
                showCompass = prefs[Keys.HUD_SHOW_COMPASS] ?: true,
                showJourneyProgress = prefs[Keys.HUD_SHOW_PROGRESS] ?: true,
                showEta = prefs[Keys.HUD_SHOW_ETA] ?: true,
                showMediaInfo = prefs[Keys.HUD_SHOW_MEDIA] ?: true,
                speedometerMatchPercent = (prefs[Keys.HUD_SPEED_CALIBRATION] ?: 4f).coerceIn(0f, 10f),
                layFlatToStartHud = prefs[Keys.HUD_LAY_FLAT_START] ?: false,
                cockpitStyle = prefs[Keys.HUD_COCKPIT_STYLE]?.let {
                    runCatching { HudCockpitStyle.valueOf(it) }.getOrNull()
                } ?: HudCockpitStyle.DIGITAL_MINIMAL
            ),
            aodStyle = prefs[Keys.AOD_STYLE]?.let {
                runCatching { AodStyle.valueOf(it) }.getOrNull()
            } ?: AodStyle.MINIMAL,
            defaultMediaSource = prefs[Keys.MEDIA_SOURCE]?.let {
                runCatching { MediaAppSource.valueOf(it) }.getOrNull()
            } ?: MediaAppSource.LASTWAVE,
            useMetricUnits = prefs[Keys.METRIC_UNITS] ?: true,
            showTrafficOverlay = prefs[Keys.TRAFFIC_OVERLAY] ?: true,
            showSatelliteGrid = prefs[Keys.SATELLITE_GRID] ?: false,
            optionalCarLauncherEnabled = prefs[Keys.CAR_LAUNCHER] ?: false,
        )
    }

    suspend fun setAdaptiveAlbumColors(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.ADAPTIVE_ALBUM_COLORS] = enabled }
    }

    suspend fun setDaylightHighContrast(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.DAYLIGHT_HIGH_CONTRAST] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setDashboardRatio(ratio: DashboardSplitRatio) {
        context.auvionyxDataStore.edit { it[Keys.DASHBOARD_RATIO] = ratio.name }
    }

    suspend fun updateHudConfig(config: HudConfig) {
        context.auvionyxDataStore.edit { prefs ->
            prefs[Keys.HUD_LAYOUT] = config.layoutMode.name
            prefs[Keys.HUD_ACCENT] = config.accentChoice.name
            prefs[Keys.HUD_FONT_SCALE] = config.fontScale
            prefs[Keys.HUD_POSITION] = config.speedPosition.name
            prefs[Keys.HUD_MIRROR] = config.mirrorFlipForWindshield
            prefs[Keys.HUD_SHOW_SPEED] = config.showSpeedometer
            prefs[Keys.HUD_SHOW_LIMIT] = config.showSpeedLimit
            prefs[Keys.HUD_SHOW_TURN] = config.showNextTurn
            prefs[Keys.HUD_SHOW_COMPASS] = config.showCompass
            prefs[Keys.HUD_SHOW_PROGRESS] = config.showJourneyProgress
            prefs[Keys.HUD_SHOW_ETA] = config.showEta
            prefs[Keys.HUD_SHOW_MEDIA] = config.showMediaInfo
            prefs[Keys.HUD_SPEED_CALIBRATION] = config.speedometerMatchPercent.coerceIn(0f, 10f)
            prefs[Keys.HUD_LAY_FLAT_START] = config.layFlatToStartHud
            prefs[Keys.HUD_COCKPIT_STYLE] = config.cockpitStyle.name
        }
    }

    suspend fun setAodStyle(style: AodStyle) {
        context.auvionyxDataStore.edit { it[Keys.AOD_STYLE] = style.name }
    }

    suspend fun setDefaultMediaSource(source: MediaAppSource) {
        context.auvionyxDataStore.edit { it[Keys.MEDIA_SOURCE] = source.name }
    }

    suspend fun setMetricUnits(metric: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.METRIC_UNITS] = metric }
    }

    suspend fun setTrafficOverlay(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.TRAFFIC_OVERLAY] = enabled }
    }

    suspend fun setSatelliteGrid(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.SATELLITE_GRID] = enabled }
    }

    suspend fun setOptionalCarLauncher(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.CAR_LAUNCHER] = enabled }
    }

    suspend fun savePlace(place: SavedPlace) {
        context.auvionyxDataStore.edit { prefs ->
            val current = runCatching { JSONArray(prefs[Keys.SAVED_PLACES].orEmpty()) }.getOrNull() ?: JSONArray()
            val updated = JSONArray()
            for (index in 0 until current.length()) {
                val item = current.optJSONObject(index) ?: continue
                if (item.optString("id") != place.id) updated.put(item)
            }
            updated.put(JSONObject()
                .put("id", place.id)
                .put("name", place.name)
                .put("addressSummary", place.addressSummary)
                .put("latitude", place.latitude)
                .put("longitude", place.longitude)
                .put("category", place.category))
            prefs[Keys.SAVED_PLACES] = updated.toString()
        }
    }

    suspend fun removeSavedPlace(id: String) {
        context.auvionyxDataStore.edit { prefs ->
            val current = runCatching { JSONArray(prefs[Keys.SAVED_PLACES].orEmpty()) }.getOrNull() ?: JSONArray()
            val updated = JSONArray()
            for (index in 0 until current.length()) {
                val item = current.optJSONObject(index) ?: continue
                if (item.optString("id") != id) updated.put(item)
            }
            prefs[Keys.SAVED_PLACES] = updated.toString()
        }
    }
}
