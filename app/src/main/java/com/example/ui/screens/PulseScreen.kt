package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MapPlaceMarker
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.TrafficIncident
import com.example.data.model.TrafficSeverity
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.LivingFlow
import com.example.ui.components.ServiceStateBadge
import com.example.ui.components.TrafficPulseBadge
import com.example.ui.components.photonLuminousTouch
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PulseClear
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.PulseSlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MapsAndPulseState
import com.example.viewmodel.PulseFeedTab

@Composable
fun PulseScreen(
    isLandscapeWide: Boolean,
    settings: AuvionyxSettingsState,
    telemetry: VehicleTelemetry,
    mapsPulse: MapsAndPulseState,
    onSelectPulseTab: (PulseFeedTab) -> Unit,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    onReportIncident: (String, String, TrafficSeverity) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val hazardPresets = listOf("Lane Closure", "Road Work", "Slow Queue", "Hazard Ahead")
    var showReportComposer by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf(hazardPresets.first()) }
    var draftCorridor by remember { mutableStateOf(telemetry.currentLocationLabel) }
    var draftSeverity by remember { mutableStateOf(TrafficSeverity.SLOW) }

    val feedPanel: @Composable (Modifier) -> Unit = { panelMod ->
        GlassCard(
            modifier = panelMod,
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(
                        animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Corridors / Forecast / Incidents Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseFeedTab.entries.forEach { tab ->
                        LiquidGlassLens(
                            selected = mapsPulse.activePulseTab == tab,
                            onClick = { onSelectPulseTab(tab) },
                            cornerRadius = 14.dp,
                            testTag = "pulse_tab_${tab.name}",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (mapsPulse.activePulseTab == tab) GlassWhite else TextSecondary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Corridor Traffic Feed",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    ServiceStateBadge(state = ServiceAvailabilityState.DEMO)
                }

                if (mapsPulse.activePulseTab == PulseFeedTab.FORECAST) {
                    TrafficTimelineForecastCard(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Incident & Corridor Cards
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mapsPulse.incidents, key = { it.id }) { incident ->
                        PulseIncidentCard(incident = incident)
                    }
                }

                // Smooth Container Morphing: CTA Pill <-> Hands-Free Incident Composer
                AnimatedContent(
                    targetState = showReportComposer,
                    transitionSpec = {
                        if (reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                                scaleIn(initialScale = 0.94f, animationSpec = tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing))) togetherWith
                                (fadeOut(tween(220, easing = LivingFlow.LaminarEasing)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = tween(220, easing = LivingFlow.LaminarEasing)))
                        }
                    },
                    label = "pulseReportMorph"
                ) { composerOpen ->
                    if (composerOpen) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                hazardPresets.forEach { preset ->
                                    LiquidGlassLens(
                                        selected = draftTitle == preset,
                                        onClick = { draftTitle = preset },
                                        cornerRadius = 12.dp,
                                        testTag = "pulse_preset_${preset.lowercase().replace(' ', '_')}",
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GlassWhite
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TrafficSeverity.entries.forEach { sev ->
                                    LiquidGlassLens(
                                        selected = draftSeverity == sev,
                                        onClick = { draftSeverity = sev },
                                        cornerRadius = 12.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = sev.label.substringBefore(" "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GlassWhite
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showReportComposer = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlassWhite.copy(alpha = 0.12f),
                                        contentColor = GlassWhite
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .photonLuminousTouch(pressScale = 0.95f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        onReportIncident(draftTitle, draftCorridor, draftSeverity)
                                        draftTitle = hazardPresets.first()
                                        showReportComposer = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = ObsidianBlack
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .photonLuminousTouch(pressScale = 0.95f, glowColor = GlassWhite)
                                        .testTag("pulse_submit_report")
                                ) {
                                    Text("Submit Report")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { showReportComposer = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = ObsidianBlack
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .photonLuminousTouch(pressScale = 0.96f, glowColor = GlassWhite)
                                .testTag("pulse_open_report")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddAlert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Report Road Incident", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (isLandscapeWide) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InteractiveMapCanvas(
                telemetry = telemetry,
                markers = mapsPulse.markers,
                selectedMarkerId = mapsPulse.selectedMarkerId,
                showTrafficOverlay = true,
                showSatelliteGrid = settings.showSatelliteGrid,
                isNavigating = mapsPulse.isNavigating,
                onSelectMarker = onSelectMarker,
                onToggleTraffic = onToggleTraffic,
                onToggleSatellite = onToggleSatellite,
                currentLatLon = mapsPulse.currentLatLon,
                destinationLatLon = mapsPulse.destinationLatLon,
                routePolylineGeo = mapsPulse.routePolylineGeo,
                navigationSourceLabel = mapsPulse.navigationSourceLabel,
                isDemoMode = settings.demoSimulationEnabled,
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
            )
            feedPanel(Modifier.weight(0.44f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InteractiveMapCanvas(
                telemetry = telemetry,
                markers = mapsPulse.markers,
                selectedMarkerId = mapsPulse.selectedMarkerId,
                showTrafficOverlay = true,
                showSatelliteGrid = settings.showSatelliteGrid,
                isNavigating = mapsPulse.isNavigating,
                onSelectMarker = onSelectMarker,
                onToggleTraffic = onToggleTraffic,
                onToggleSatellite = onToggleSatellite,
                currentLatLon = mapsPulse.currentLatLon,
                destinationLatLon = mapsPulse.destinationLatLon,
                routePolylineGeo = mapsPulse.routePolylineGeo,
                navigationSourceLabel = mapsPulse.navigationSourceLabel,
                isDemoMode = settings.demoSimulationEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            )
            feedPanel(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun PulseIncidentCard(incident: TrafficIncident) {
    val (icon, tint) = when (incident.severity) {
        TrafficSeverity.HIGH -> Icons.Outlined.ReportProblem to PulseCongestion
        TrafficSeverity.SLOW -> Icons.Outlined.WarningAmber to PulseSlow
        TrafficSeverity.CLEAR -> Icons.Outlined.CheckCircle to PulseClear
    }
    val delayText = if (incident.delayMinutes > 0) "+${incident.delayMinutes} min" else "0 min"

    Surface(
        color = tint.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .photonLuminousTouch(pressScale = 0.98f, glowColor = tint)
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
                Icon(
                    imageVector = icon,
                    contentDescription = incident.severity.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(incident.title, style = MaterialTheme.typography.titleMedium, color = GlassWhite)
                    Text(
                        text = "${incident.corridor} · ${incident.reportedBy} · ${incident.timestampLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                TrafficPulseBadge(severity = incident.severity)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = delayText,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint
                )
            }
        }
    }
}

@Composable
private fun TrafficTimelineForecastCard(modifier: Modifier = Modifier) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val slots = listOf(
        "Now" to 0.72f,
        "+1h" to 0.85f,
        "+2h" to 0.56f,
        "+3h" to 0.34f,
        "+4h" to 0.25f,
        "+5h" to 0.20f
    )
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text("Traffic Timeline Outlook (Demo)", style = MaterialTheme.typography.titleMedium, color = GlassWhite)
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val barWidth = (size.width / slots.size) * 0.52f
            val step = size.width / slots.size
            slots.forEachIndexed { idx, (_, load) ->
                val barHeight = size.height * load
                val x = idx * step + (step - barWidth) / 2f
                val color = when {
                    load > 0.75f -> PulseCongestion
                    load > 0.45f -> PulseSlow
                    else -> accent
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            slots.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
