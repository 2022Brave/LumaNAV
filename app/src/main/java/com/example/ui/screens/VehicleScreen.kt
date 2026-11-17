package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DriveMode
import com.example.data.model.VehicleTelemetry
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassLens
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun VehicleScreen(
    isLandscapeWide: Boolean,
    telemetry: VehicleTelemetry,
    onAdjustDriverTemp: (Float) -> Unit,
    onAdjustPassengerTemp: (Float) -> Unit,
    onSelectDriveMode: (DriveMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Prototype telemetry disclosure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auvionyx Vehicle", style = MaterialTheme.typography.headlineMedium, color = GlassWhite)
            Surface(
                color = IceBlue.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.45f))
            ) {
                Text(
                    text = if (telemetry.isSimulationRunning) {
                        "Demo Vehicle Telemetry"
                    } else {
                        "Device Battery Live · Car Bus Pending"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = IceBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (isLandscapeWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PowertrainAndModeCard(
                    telemetry = telemetry,
                    accent = accent,
                    onSelectDriveMode = onSelectDriveMode,
                    modifier = Modifier.weight(1f)
                )
                ClimateControlCard(
                    telemetry = telemetry,
                    accent = accent,
                    onAdjustDriverTemp = onAdjustDriverTemp,
                    onAdjustPassengerTemp = onAdjustPassengerTemp,
                    modifier = Modifier.weight(1f)
                )
            }
            TireAndDiagnosticsCard(telemetry = telemetry, accent = accent)
        } else {
            PowertrainAndModeCard(
                telemetry = telemetry,
                accent = accent,
                onSelectDriveMode = onSelectDriveMode,
                modifier = Modifier.fillMaxWidth()
            )
            ClimateControlCard(
                telemetry = telemetry,
                accent = accent,
                onAdjustDriverTemp = onAdjustDriverTemp,
                onAdjustPassengerTemp = onAdjustPassengerTemp,
                modifier = Modifier.fillMaxWidth()
            )
            TireAndDiagnosticsCard(telemetry = telemetry, accent = accent)
        }
    }
}

@Composable
private fun PowertrainAndModeCard(
    telemetry: VehicleTelemetry,
    accent: androidx.compose.ui.graphics.Color,
    onSelectDriveMode: (DriveMode) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        highlighted = true,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = "Battery & Range",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = if (telemetry.rangeKm > 0) "${telemetry.rangeKm} km Range" else "${telemetry.batteryPercent}% Device Battery",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GlassWhite
                    )
                    Text(
                        text = if (telemetry.rangeKm > 0) "${telemetry.batteryPercent}% Battery · Demo Powertrain" else "Car range requires OBD-II / CarPropertyManager",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { telemetry.batteryPercent / 100f },
            color = accent,
            trackColor = GlassWhite.copy(alpha = 0.14f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
        Spacer(Modifier.height(16.dp))
        Text("Drive Mode", style = MaterialTheme.typography.titleMedium, color = GlassWhite)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DriveMode.entries.forEach { mode ->
                LiquidGlassLens(
                    selected = telemetry.driveMode == mode,
                    onClick = { onSelectDriveMode(mode) },
                    cornerRadius = 14.dp,
                    testTag = "drive_mode_${mode.name}",
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (telemetry.driveMode == mode) GlassWhite else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ClimateControlCard(
    telemetry: VehicleTelemetry,
    accent: androidx.compose.ui.graphics.Color,
    onAdjustDriverTemp: (Float) -> Unit,
    onAdjustPassengerTemp: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Thermostat,
                contentDescription = "Dual Zone Climate",
                tint = accent
            )
            Text("Dual-Zone Cabin Climate", style = MaterialTheme.typography.titleLarge, color = GlassWhite)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ZoneTempStepper(
                zoneLabel = "Driver",
                tempC = telemetry.cabinDriverTempC,
                accent = accent,
                onDec = { onAdjustDriverTemp(-0.5f) },
                onInc = { onAdjustDriverTemp(0.5f) },
                modifier = Modifier.weight(1f)
            )
            ZoneTempStepper(
                zoneLabel = "Passenger",
                tempC = telemetry.cabinPassengerTempC,
                accent = accent,
                onDec = { onAdjustPassengerTemp(-0.5f) },
                onInc = { onAdjustPassengerTemp(0.5f) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ZoneTempStepper(
    zoneLabel: String,
    tempC: Float,
    accent: androidx.compose.ui.graphics.Color,
    onDec: () -> Unit,
    onInc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = GlassWhite.copy(alpha = 0.05f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(zoneLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "%.1f°C", tempC),
                style = MaterialTheme.typography.headlineMedium,
                color = GlassWhite
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDec, modifier = Modifier.testTag("climate_dec_$zoneLabel")) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease $zoneLabel temp", tint = accent)
                }
                IconButton(onClick = onInc, modifier = Modifier.testTag("climate_inc_$zoneLabel")) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase $zoneLabel temp", tint = accent)
                }
            }
        }
    }
}

@Composable
private fun TireAndDiagnosticsCard(
    telemetry: VehicleTelemetry,
    accent: androidx.compose.ui.graphics.Color
) {
    val tireLabels = listOf("Front Left", "Front Right", "Rear Left", "Rear Right")
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.TireRepair, contentDescription = "Tire Pressure", tint = accent)
                Text("Tire Pressure & Diagnostics", style = MaterialTheme.typography.titleLarge, color = GlassWhite)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text("All Systems Nominal", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (telemetry.tirePsi.isEmpty()) {
                Text(
                    text = "TPMS sensors unavailable without vehicle CAN/OBD-II interface.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                telemetry.tirePsi.forEachIndexed { idx, psi ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = GlassWhite.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(tireLabels.getOrElse(idx) { "Tire" }, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(Modifier.height(4.dp))
                            Text("$psi PSI", style = MaterialTheme.typography.titleLarge, color = GlassWhite)
                        }
                    }
                }
            }
        }
    }
}
