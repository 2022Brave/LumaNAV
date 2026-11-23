package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AuvionyxModule
import com.example.data.model.DashboardSplitRatio
import com.example.data.model.HudLayoutMode
import com.example.data.model.MediaAppSource
import com.example.data.model.SyncDeviceProfile
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.ExpandableGlassPanel
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.LiquidGlassToggle
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PermissionsSnapshot

@Composable
fun SettingsScreen(
    settings: AuvionyxSettingsState,
    syncDevices: List<SyncDeviceProfile>,
    permissions: PermissionsSnapshot,
    onRefreshPermissions: () -> Unit,
    onSetAdaptiveAlbumColors: (Boolean) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetDashboardRatio: (DashboardSplitRatio) -> Unit,
    onSetHudLayout: (HudLayoutMode) -> Unit,
    onOpenHudDesigner: () -> Unit,
    onSetMetricUnits: (Boolean) -> Unit,
    onSetTrafficOverlay: (Boolean) -> Unit,
    onSetSatelliteGrid: (Boolean) -> Unit,
    onSetDemoSimulation: (Boolean) -> Unit,
    onSetMediaSource: (MediaAppSource) -> Unit,
    onSetOptionalLauncher: (Boolean) -> Unit,
    onToggleDeviceSync: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = LocalAuvionyxPalette.current.accentPrimary

    var appearanceExpanded by remember { mutableStateOf(true) }
    var dashboardExpanded by remember { mutableStateOf(true) }
    var mapsMediaExpanded by remember { mutableStateOf(false) }
    var launcherSyncExpanded by remember { mutableStateOf(false) }
    var permissionsExpanded by remember { mutableStateOf(true) }

    val locLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onRefreshPermissions() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LumaNav Settings", style = MaterialTheme.typography.headlineMedium, color = GlassWhite)
                    Text(
                        "Personalisation, Dashboard Designer & Native Capability Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 1. Appearance & Motion
        item {
            ExpandableGlassPanel(
                title = "Appearance & Motion",
                subtitle = if (settings.useAdaptiveAlbumColors) "Adaptive Album Accent · AMOLED Black" else "Official Electric Cyan · AMOLED Black",
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                SettingsToggleRow(
                    title = "Official Default Palette Lock",
                    subtitle = "Pure #000000 AMOLED black with Electric Cyan #00D9F5",
                    checked = !settings.useAdaptiveAlbumColors,
                    testTag = "settings_official_theme_toggle",
                    onCheckedChange = { locked -> onSetAdaptiveAlbumColors(!locked) }
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Adaptive Album-Art Colours",
                    subtitle = "Optional dynamic accent tint derived from active track",
                    checked = settings.useAdaptiveAlbumColors,
                    testTag = "settings_adaptive_album_toggle",
                    onCheckedChange = onSetAdaptiveAlbumColors
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Reduced Motion",
                    subtitle = "Minimizes spring bounce & ambient animations for 4GB RAM / focus",
                    checked = settings.reducedMotion,
                    testTag = "settings_reduced_motion_toggle",
                    onCheckedChange = onSetReducedMotion
                )
            }
        }

        // 2. Dashboard Designer & HUD Customisation
        item {
            ExpandableGlassPanel(
                title = "Dashboard Designer & HUD",
                subtitle = "Blaupunkt 1280×800 split (${settings.dashboardRatio.label}) · ${settings.hudConfig.layoutMode.label}",
                expanded = dashboardExpanded,
                onToggle = { dashboardExpanded = !dashboardExpanded }
            ) {
                Text("Default Maps-to-Media Split Ratio", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardSplitRatio.entries.forEach { ratio ->
                        LiquidGlassLens(
                            selected = settings.dashboardRatio == ratio,
                            onClick = { onSetDashboardRatio(ratio) },
                            cornerRadius = 12.dp,
                            testTag = "designer_ratio_${ratio.name}",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = ratio.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (settings.dashboardRatio == ratio) GlassWhite else TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Default HUD Layout", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudLayoutMode.entries.forEach { mode ->
                        LiquidGlassLens(
                            selected = settings.hudConfig.layoutMode == mode,
                            onClick = { onSetHudLayout(mode) },
                            cornerRadius = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = mode.label.removeSuffix(" HUD"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (settings.hudConfig.layoutMode == mode) GlassWhite else TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onOpenHudDesigner,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("settings_open_hud")
                ) {
                    Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Full HUD Designer")
                }
            }
        }

        // 3. Maps & Media Preferences
        item {
            ExpandableGlassPanel(
                title = "Maps & Media Preferences",
                subtitle = "${if (settings.useMetricUnits) "Metric (km/h)" else "Imperial (mph)"} · Default Media: ${settings.defaultMediaSource.label}",
                expanded = mapsMediaExpanded,
                onToggle = { mapsMediaExpanded = !mapsMediaExpanded }
            ) {
                SettingsToggleRow(
                    title = "Metric Units (km/h & km)",
                    subtitle = "Toggle between kilometres and miles",
                    checked = settings.useMetricUnits,
                    testTag = "settings_metric_toggle",
                    onCheckedChange = onSetMetricUnits
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Traffic Pulse Overlay Default",
                    subtitle = "Highlight corridor congestion on active navigation maps",
                    checked = settings.showTrafficOverlay,
                    onCheckedChange = onSetTrafficOverlay
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Satellite Relief Grid",
                    subtitle = "Show subtle terrain grid on map canvas",
                    checked = settings.showSatelliteGrid,
                    onCheckedChange = onSetSatelliteGrid
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Demo Sensor Simulation",
                    subtitle = "Prototype mode: simulates speed oscillation when stationary",
                    checked = settings.demoSimulationEnabled,
                    onCheckedChange = onSetDemoSimulation
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = IceBlue.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maps Integration Status: Demo Vector Preview active. Add a Google Maps Platform API key in AndroidManifest & build configuration to enable live Google Maps tiles, Geocoding, and Routes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceBlue,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Preferred Media Hub App", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaAppSource.entries.forEach { src ->
                        LiquidGlassLens(
                            selected = settings.defaultMediaSource == src,
                            onClick = { onSetMediaSource(src) },
                            cornerRadius = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = src.label.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassWhite
                            )
                        }
                    }
                }
            }
        }

        // 4. Optional Launcher Selection & Device Synchronisation
        item {
            ExpandableGlassPanel(
                title = "Launcher & Device Synchronisation",
                subtitle = "Optional Car Launcher · Blaupunkt, OPPO Find X10 & Galaxy S24+",
                expanded = launcherSyncExpanded,
                onToggle = { launcherSyncExpanded = !launcherSyncExpanded }
            ) {
                Surface(
                    color = IceBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Native Requirement Note: Replacing the Android Home Launcher requires declaring CATEGORY_HOME / ROLE_HOME in AndroidManifest & user selection in Android System Settings. Device Sync below is a prototype configuration profile.",
                        style = MaterialTheme.typography.labelSmall,
                        color = IceBlue,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Optional Car Home Launcher Mode",
                    subtitle = "Remain optional; boot directly into LumaNav Drive when enabled",
                    checked = settings.optionalCarLauncherEnabled,
                    testTag = "settings_launcher_toggle",
                    onCheckedChange = onSetOptionalLauncher
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassWhite.copy(alpha = 0.12f),
                        contentColor = GlassWhite
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Android Default Home Settings")
                }

                Spacer(Modifier.height(14.dp))
                Text("Synced Ecosystem Devices (Prototype)", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                Spacer(Modifier.height(8.dp))
                syncDevices.forEach { device ->
                    Surface(
                        color = GlassWhite.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (device.isSynced) accent.copy(alpha = 0.55f) else GlassWhite.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Devices, contentDescription = null, tint = accent)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(device.name, style = MaterialTheme.typography.titleMedium, color = GlassWhite)
                                    Text(
                                        "${device.formFactor} · ${device.osVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            LiquidGlassToggle(
                                checked = device.isSynced,
                                onCheckedChange = { onToggleDeviceSync(device.id) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Permission Manager (Real Android Runtime Permissions)
        item {
            ExpandableGlassPanel(
                title = "Permission Manager",
                subtitle = "Location and navigation notification access",
                expanded = permissionsExpanded,
                onToggle = { permissionsExpanded = !permissionsExpanded }
            ) {
                PermissionRow(
                    title = "Precise GPS Location (ACCESS_FINE_LOCATION)",
                    granted = permissions.locationGranted,
                    testTag = "perm_request_location",
                    onRequest = { locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
                Spacer(Modifier.height(10.dp))
                PermissionRow(
                    title = "Navigation Notifications (Maps, Waze & Mappls)",
                    granted = permissions.notificationAccessGranted,
                    testTag = "perm_request_notification_sync",
                    onRequest = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Notification Access lets LumaNav read active navigation notifications from supported apps to show turn instructions, distance and ETA. Android may require App info → ⋮ → Allow restricted settings before this can be enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = LocalAuvionyxPalette.current.accentPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, LocalAuvionyxPalette.current.accentPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    }.testTag("open_app_info_restricted_settings")
                ) {
                    Text(
                        text = "Open LumaNav App Info",
                        color = GlassWhite,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GlassWhite)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        LiquidGlassToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            testTag = testTag
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    testTag: String,
    onRequest: () -> Unit
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.Security, contentDescription = null, tint = if (granted) accent else TextMuted)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = GlassWhite)
                Text(
                    text = if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) accent else TextMuted
                )
            }
        }
        if (!granted) {
            Surface(
                color = accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent),
                modifier = Modifier
                    .testTag(testTag)
                    .clickable(onClick = onRequest)
            ) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
