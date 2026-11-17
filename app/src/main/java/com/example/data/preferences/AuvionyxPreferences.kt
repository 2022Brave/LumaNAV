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
import com.example.data.model.HudLayoutMode
import com.example.data.model.HudPosition
import com.example.data.model.MediaAppSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.auvionyxDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auvionyx_preferences"
)

data class AuvionyxSettingsState(
    val useAdaptiveAlbumColors: Boolean = false,
    val reducedMotion: Boolean = false,
    val dashboardRatio: DashboardSplitRatio = DashboardSplitRatio.RATIO_70_30,
    val hudConfig: HudConfig = HudConfig(),
    val aodStyle: AodStyle = AodStyle.MINIMAL,
    val defaultMediaSource: MediaAppSource = MediaAppSource.LASTWAVE,
    val useMetricUnits: Boolean = true,
    val showTrafficOverlay: Boolean = true,
    val showSatelliteGrid: Boolean = false,
    val optionalCarLauncherEnabled: Boolean = false,
    val demoSimulationEnabled: Boolean = false
)

class AuvionyxPreferencesRepository(private val context: Context) {

    private object Keys {
        val ADAPTIVE_ALBUM_COLORS = booleanPreferencesKey("adaptive_album_colors")
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
        val AOD_STYLE = stringPreferencesKey("aod_style")
        val MEDIA_SOURCE = stringPreferencesKey("media_source")
        val METRIC_UNITS = booleanPreferencesKey("metric_units")
        val TRAFFIC_OVERLAY = booleanPreferencesKey("traffic_overlay")
        val SATELLITE_GRID = booleanPreferencesKey("satellite_grid")
        val CAR_LAUNCHER = booleanPreferencesKey("car_launcher")
        val DEMO_SIMULATION = booleanPreferencesKey("demo_simulation")
    }

    val settingsFlow: Flow<AuvionyxSettingsState> = context.auvionyxDataStore.data.map { prefs ->
        AuvionyxSettingsState(
            useAdaptiveAlbumColors = prefs[Keys.ADAPTIVE_ALBUM_COLORS] ?: false,
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
                showMediaInfo = prefs[Keys.HUD_SHOW_MEDIA] ?: true
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
            demoSimulationEnabled = prefs[Keys.DEMO_SIMULATION] ?: false
        )
    }

    suspend fun setAdaptiveAlbumColors(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.ADAPTIVE_ALBUM_COLORS] = enabled }
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

    suspend fun setDemoSimulation(enabled: Boolean) {
        context.auvionyxDataStore.edit { it[Keys.DEMO_SIMULATION] = enabled }
    }
}
