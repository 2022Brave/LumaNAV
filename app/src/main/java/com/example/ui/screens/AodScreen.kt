package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AodStyle
import com.example.data.model.VehicleTelemetry
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.NativeMapLibreView
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure AMOLED #000000 Always-On Navigation Display.
 * Uses a low-frequency 20-second discrete pixel-shift for burn-in protection without 60fps GPU wakeups.
 */
@Composable
fun AodScreen(
    aodStyle: AodStyle,
    telemetry: VehicleTelemetry,
    useMetricUnits: Boolean,
    currentLocation: Pair<Double, Double>?,
    routePoints: List<Pair<Double, Double>>,
    headingDegrees: Int,
    isNavigating: Boolean,
    onSelectAodStyle: (AodStyle) -> Unit,
    onExitAod: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    var shiftStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            while (true) {
                delay(20_000L)
                shiftStep = (shiftStep + 1) % 4
            }
        } else {
            shiftStep = 0
        }
    }

    val (shiftX, shiftY) = when (shiftStep) {
        1 -> 6 to 4
        2 -> -6 to 4
        3 -> -6 to -4
        else -> 6 to -4
    }

    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val speedVal = if (useMetricUnits) telemetry.speedKmh else (telemetry.speedKmh * 0.621371f).toInt()
    val unit = if (useMetricUnits) "km/h" else "mph"
    val speedSourceTag = when {
        telemetry.isLiveGpsActive -> ""
        telemetry.isSimulationRunning -> " (Demo)"
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .systemBarsPadding()
            .padding(if (aodStyle == AodStyle.MAP) 0.dp else 24.dp)
    ) {
        if (aodStyle == AodStyle.MAP) {
            NativeMapLibreView(
                currentLocation = currentLocation,
                routePoints = routePoints,
                headingDegrees = headingDegrees,
                accent = ElectricCyan,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color(0x55000000)))
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(shiftX, shiftY) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "LUMANAV AOD · ${aodStyle.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.5.sp),
                color = ElectricCyan.copy(alpha = 0.72f)
            )

            when (aodStyle) {
                AodStyle.MINIMAL -> {
                    Text(
                        text = timeLabel,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 68.sp,
                        color = GlassWhite.copy(alpha = 0.84f)
                    )
                    Text(
                        text = "$speedVal $unit$speedSourceTag · ${telemetry.headingCardinal}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricCyan.copy(alpha = 0.88f)
                    )
                }

                AodStyle.MAP -> {
                    Text(
                        text = if (isNavigating && routePoints.size >= 2) telemetry.destinationLabel.ifBlank { "ACTIVE NAVIGATION" } else "NO ACTIVE ROUTE",
                        style = MaterialTheme.typography.titleLarge,
                        color = GlassWhite,
                        modifier = Modifier.padding(top = 56.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TurnRight,
                            contentDescription = "Next turn",
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (isNavigating && routePoints.size >= 2) "${telemetry.nextTurnDistanceLabel} · ${telemetry.nextTurnInstruction}" else "No active route",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GlassWhite.copy(alpha = 0.85f)
                        )
                    }
                }

                AodStyle.JOURNEY -> {
                    Text(
                        text = "ETA ${telemetry.etaTimeLabel}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 48.sp,
                        color = GlassWhite.copy(alpha = 0.86f)
                    )
                    Text(
                        text = if (telemetry.remainingMinutes > 0) {
                            "${if (telemetry.isSimulationRunning) "Demo Route" else "Route"}: ${telemetry.remainingMinutes} min · ${telemetry.remainingDistanceKm} km"
                        } else {
                            "No active route"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = IceBlue.copy(alpha = 0.86f)
                    )
                    LinearProgressIndicator(
                        progress = { telemetry.journeyProgress },
                        color = ElectricCyan.copy(alpha = 0.85f),
                        trackColor = Color(0xFF101824),
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(5.dp)
                    )
                    Text(
                        text = "$speedVal $unit$speedSourceTag · Next: ${telemetry.nextTurnRoad}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "AMOLED Low-Power Pixel-Shift Active",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        // Bottom Style Switcher & Wake Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AodStyle.entries.forEach { style ->
                    LiquidGlassLens(
                        selected = aodStyle == style,
                        onClick = { onSelectAodStyle(style) },
                        cornerRadius = 14.dp,
                        testTag = "aod_style_${style.name}",
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (aodStyle == style) GlassWhite else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            Surface(
                color = DeepBlue.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ElectricCyan),
                modifier = Modifier
                    .testTag("aod_wake_button")
                    .clickable(onClick = onExitAod)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = "Wake LumaNav Drive",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Wake LumaNav", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                }
            }
        }
    }
}
