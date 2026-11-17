package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MapPlaceMarker
import com.example.data.model.VehicleTelemetry
import com.example.ui.theme.DarkNeonFill
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Interactive 3D Obsidian Automotive Map Canvas matching Active & Idle Cockpit blueprints:
 * - Renders actual live GPS + OSRM road route polylines when configured
 * - Uses a clearly identified 3D perspective city-block vector placeholder when unconfigured/demo
 */
@Composable
fun InteractiveMapCanvas(
    telemetry: VehicleTelemetry,
    markers: List<MapPlaceMarker>,
    selectedMarkerId: String?,
    showTrafficOverlay: Boolean,
    showSatelliteGrid: Boolean,
    isNavigating: Boolean,
    onSelectMarker: (MapPlaceMarker) -> Unit,
    onToggleTraffic: () -> Unit,
    onToggleSatellite: () -> Unit,
    modifier: Modifier = Modifier,
    currentLatLon: Pair<Double, Double>? = null,
    destinationLatLon: Pair<Double, Double>? = null,
    routePolylineGeo: List<Pair<Double, Double>> = emptyList(),
    navigationSourceLabel: String? = null,
    isDemoMode: Boolean = false,
    showMapControls: Boolean = true,
    compactHudMode: Boolean = false,
    showFullScreenBorder: Boolean = true,
    displayNextTurnDistance: String = telemetry.nextTurnDistanceLabel,
    displayNextTurnRoad: String = telemetry.nextTurnRoad,
    displayRemainingMinutes: Int = telemetry.remainingMinutes,
    displayRemainingKm: Float = telemetry.remainingDistanceKm,
    displayEtaLabel: String = telemetry.etaTimeLabel,
    onExpandMap: (() -> Unit)? = null
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 500)) }

    val cornerShape = if (showFullScreenBorder) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp)

    Box(
        modifier = modifier
            .clip(cornerShape)
            .background(ObsidianBlack)
            .then(
                if (showFullScreenBorder) {
                    Modifier.border(
                        width = 0.85.dp,
                        brush = Brush.linearGradient(
                            listOf(GlassWhite.copy(alpha = 0.22f), accent.copy(alpha = 0.28f), GlassBorderSubtle)
                        ),
                        shape = cornerShape
                    )
                } else {
                    Modifier
                }
            )
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(0.65f, 2.8f)
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-260f, 260f),
                        y = (panOffset.y + pan.y).coerceIn(-200f, 200f)
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.16f
            val vanishX = w * 0.50f + panOffset.x * 0.35f

            // 1. Atmospheric AMOLED Night Sky -> Subtle Deep Obsidian City Mist
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBlack,
                        Color(0xFF020710),
                        Color(0xFF040D1A),
                        Color(0xFF02050A)
                    )
                )
            )

            // Distant horizon mountain ridge silhouette (matches Idle & Active map horizons)
            val ridge = Path().apply {
                moveTo(0f, horizonY)
                lineTo(w * 0.14f, horizonY - h * 0.035f)
                lineTo(w * 0.31f, horizonY - h * 0.015f)
                lineTo(w * 0.48f, horizonY - h * 0.042f)
                lineTo(w * 0.67f, horizonY - h * 0.020f)
                lineTo(w * 0.86f, horizonY - h * 0.038f)
                lineTo(w, horizonY - h * 0.01f)
                lineTo(w, horizonY + 8.dp.toPx())
                lineTo(0f, horizonY + 8.dp.toPx())
                close()
            }
            drawPath(ridge, color = Color(0xFF010408))

            // Horizon mist glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, IceBlue.copy(alpha = 0.07f), Color.Transparent),
                    startY = horizonY - h * 0.03f,
                    endY = horizonY + h * 0.16f
                ),
                topLeft = Offset(0f, horizonY - h * 0.03f),
                size = Size(w, h * 0.19f)
            )

            // 2. 3D Perspective Street Grid & Obsidian City Building Blocks
            drawPerspectiveCityGrid(
                w = w,
                h = h,
                horizonY = horizonY,
                vanishX = vanishX,
                panOffset = panOffset,
                accent = accent,
                showGridBoost = showSatelliteGrid
            )

            // 3. Route Spline + Navigation Puck
            if (routePolylineGeo.size >= 2) {
                val allPts = buildList {
                    addAll(routePolylineGeo)
                    currentLatLon?.let { add(it) }
                    destinationLatLon?.let { add(it) }
                }
                val minLat = allPts.minOf { it.first }
                val maxLat = allPts.maxOf { it.first }
                val minLon = allPts.minOf { it.second }
                val maxLon = allPts.maxOf { it.second }
                val midLat = (minLat + maxLat) / 2.0
                val midLon = (minLon + maxLon) / 2.0
                val cosLat = cos(Math.toRadians(midLat)).coerceAtLeast(0.2)
                val latSpan = max(maxLat - minLat, 0.004)
                val lonSpan = max((maxLon - minLon) * cosLat, 0.004)
                val scale = (minOf(w * 0.72f, h * 0.66f) / max(latSpan, lonSpan).toFloat()) * zoom

                fun projectGeo(lat: Double, lon: Double): Offset {
                    val x = w * 0.5f + panOffset.x + ((lon - midLon) * cosLat).toFloat() * scale
                    val y = h * 0.54f + panOffset.y - (lat - midLat).toFloat() * scale
                    return Offset(x, y)
                }

                val path = Path()
                routePolylineGeo.forEachIndexed { idx, (lat, lon) ->
                    val p = projectGeo(lat, lon)
                    if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }

                drawPath(
                    path = path,
                    color = accent.copy(alpha = 0.24f),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(listOf(GlassWhite, accent, DeepBlue)),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val destPt = projectGeo(routePolylineGeo.last().first, routePolylineGeo.last().second)
                drawCircle(color = accent.copy(alpha = 0.35f), radius = 14.dp.toPx(), center = destPt)
                drawCircle(color = GlassWhite, radius = 5.dp.toPx(), center = destPt)

                val carGeo = currentLatLon ?: routePolylineGeo.first()
                drawNavigationPuck(projectGeo(carGeo.first, carGeo.second), telemetry.headingDegrees.toFloat(), accent)
            } else {
                // Perspective Glowing Electric Cyan Highway Ribbon (matches Active & Idle Cockpit blueprints)
                val puckCenter = Offset(w * 0.50f + panOffset.x * 0.5f, h * 0.66f + panOffset.y * 0.4f)
                val highwayPath = Path().apply {
                    moveTo(puckCenter.x - w * 0.015f, h * 1.02f)
                    lineTo(puckCenter.x, puckCenter.y)
                    cubicTo(
                        puckCenter.x + w * 0.01f, h * 0.48f,
                        w * 0.68f, h * 0.38f,
                        w * 0.62f, h * 0.25f
                    )
                    cubicTo(
                        w * 0.58f, h * 0.21f,
                        w * 0.49f, h * 0.19f,
                        w * 0.45f, horizonY + 2.dp.toPx()
                    )
                }

                // Wide atmospheric cyan highway glow
                drawPath(
                    path = highwayPath,
                    color = accent.copy(alpha = 0.20f),
                    style = Stroke(width = 24.dp.toPx() * zoom.coerceIn(0.8f, 1.3f), cap = StrokeCap.Round)
                )
                drawPath(
                    path = highwayPath,
                    color = accent.copy(alpha = 0.45f),
                    style = Stroke(width = 11.dp.toPx() * zoom.coerceIn(0.8f, 1.3f), cap = StrokeCap.Round)
                )
                drawPath(
                    path = highwayPath,
                    brush = Brush.verticalGradient(
                        listOf(IceBlue.copy(alpha = 0.85f), accent, GlassWhite)
                    ),
                    style = Stroke(width = 4.5.dp.toPx() * zoom.coerceIn(0.8f, 1.3f), cap = StrokeCap.Round)
                )

                drawNavigationPuck(
                    puckCenter,
                    if (isNavigating) 0f else telemetry.headingDegrees.toFloat(),
                    accent
                )
            }
        }

        // Interactive Markers (for Maps/Pulse screens when present)
        markers.forEach { marker ->
            val isSelected = marker.id == selectedMarkerId
            val xPx = (canvasSize.width * 0.5f + panOffset.x + (marker.relX - 0.5f) * canvasSize.width * zoom).roundToInt()
            val yPx = (canvasSize.height * 0.55f + panOffset.y + (marker.relY - 0.55f) * canvasSize.height * zoom).roundToInt()

            Surface(
                modifier = Modifier
                    .offset { IntOffset(xPx - 12, yPx - 12) }
                    .clickable { onSelectMarker(marker) },
                color = if (isSelected) accent else DarkNeonFill,
                shape = CircleShape,
                border = BorderStroke(1.2.dp, if (isSelected) GlassWhite else accent)
            ) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ObsidianBlack else accent)
                    )
                }
            }
        }

        // Active Cockpit In-Map Overlays (Fluid Cloud Navigation Pill + Photon Touch ETA)
        if (!compactHudMode) {
            if (isNavigating || isDemoMode) {
                FluidCloudNavigationPill(
                    nextTurnDistance = displayNextTurnDistance,
                    nextTurnRoad = displayNextTurnRoad,
                    nextTurnInstruction = telemetry.nextTurnInstruction,
                    remainingMinutes = displayRemainingMinutes,
                    remainingKm = displayRemainingKm,
                    speedAlertActive = telemetry.speedLimitKmh != null && telemetry.speedKmh > telemetry.speedLimitKmh,
                    speedLimitKmh = telemetry.speedLimitKmh,
                    accent = accent,
                    onOpenFullMaps = { onExpandMap?.invoke() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp, start = 16.dp, end = 16.dp)
                )
            }

            // Bottom-Center ETA Capsule (`19 min | 12.6 km | ETA 10:43`)
            if ((isNavigating || isDemoMode) && displayRemainingMinutes > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .photonLuminousTouch(pressScale = 0.97f)
                        .testTag("map_eta_strip"),
                    color = Color(0xBD040911),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        0.85.dp,
                        Brush.horizontalGradient(
                            listOf(GlassWhite.copy(alpha = 0.20f), accent.copy(alpha = 0.45f), GlassWhite.copy(alpha = 0.20f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            Text("$displayRemainingMinutes min", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                        }
                        Box(modifier = Modifier.width(1.dp).height(14.dp).background(GlassWhite.copy(alpha = 0.22f)))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            Text("$displayRemainingKm km", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                        }
                        Box(modifier = Modifier.width(1.dp).height(14.dp).background(GlassWhite.copy(alpha = 0.22f)))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.Flag, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            Text("ETA $displayEtaLabel", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                        }
                    }
                }
            }

            // Honest Placeholder / Live Source Indicator in bottom-left
            val sourceTag = when {
                navigationSourceLabel != null -> navigationSourceLabel.uppercase()
                telemetry.isLiveGpsActive -> "LIVE GPS"
                else -> "MAP PLACEHOLDER · DEMO"
            }
            Text(
                text = sourceTag,
                style = MaterialTheme.typography.labelSmall,
                color = IceBlue.copy(alpha = 0.58f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // Right-Edge Circular Map Tools matching Image 3: Compass (N), Layers, Zoom (+/-)
        if (showMapControls && !compactHudMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapToolPillButton(
                    icon = Icons.Outlined.Explore,
                    active = false,
                    contentDescription = "Recenter compass",
                    testTag = "map_recenter",
                    onClick = {
                        zoom = 1.0f
                        panOffset = Offset.Zero
                    }
                )
                MapToolPillButton(
                    icon = Icons.Outlined.Layers,
                    active = showSatelliteGrid,
                    contentDescription = "Toggle map layers",
                    testTag = "map_toggle_satellite",
                    onClick = onToggleSatellite
                )
                // Stacked + / - Zoom Capsule
                Surface(
                    color = Color(0xBD060D17),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(0.85.dp, GlassWhite.copy(alpha = 0.24f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("map_zoom_in")
                                .photonLuminousTouch(
                                    pressScale = 0.90f,
                                    onClick = { zoom = (zoom + 0.2f).coerceAtMost(2.5f) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Add, "Zoom in", tint = GlassWhite, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(0.8.dp)
                                .background(GlassWhite.copy(alpha = 0.16f))
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("map_zoom_out")
                                .photonLuminousTouch(
                                    pressScale = 0.90f,
                                    onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.7f) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Remove, "Zoom out", tint = GlassWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPerspectiveCityGrid(
    w: Float,
    h: Float,
    horizonY: Float,
    vanishX: Float,
    panOffset: Offset,
    accent: Color,
    showGridBoost: Boolean
) {
    val gridAlpha = if (showGridBoost) 0.16f else 0.09f

    // Radial perspective street avenues converging toward horizon vanishing point
    val rayCount = 12
    for (i in -rayCount..rayCount) {
        val bottomX = vanishX + i * (w * 0.11f) + panOffset.x * 0.45f
        val topX = vanishX + i * (w * 0.025f)
        drawLine(
            color = IceBlue.copy(alpha = gridAlpha),
            start = Offset(topX, horizonY),
            end = Offset(bottomX, h),
            strokeWidth = if (i % 3 == 0) 1.4.dp.toPx() else 0.8.dp.toPx()
        )
    }

    // Perspective horizontal cross-streets (quadratic depth spacing)
    val rows = 9
    for (r in 1..rows) {
        val t = (r.toFloat() / rows).pow(1.65f)
        val y = horizonY + (h - horizonY) * t
        drawLine(
            color = IceBlue.copy(alpha = gridAlpha * (0.4f + 0.6f * t)),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = (0.7f + t * 0.9f).dp.toPx()
        )
    }

    // Geometric extruded dark-obsidian city blocks for 3D visual depth
    val blocks = listOf(
        floatArrayOf(0.12f, 0.34f, 0.09f, 0.07f),
        floatArrayOf(0.25f, 0.44f, 0.11f, 0.09f),
        floatArrayOf(0.08f, 0.60f, 0.14f, 0.12f),
        floatArrayOf(0.28f, 0.68f, 0.11f, 0.10f),
        floatArrayOf(0.70f, 0.36f, 0.10f, 0.08f),
        floatArrayOf(0.78f, 0.50f, 0.13f, 0.11f),
        floatArrayOf(0.66f, 0.64f, 0.12f, 0.10f),
        floatArrayOf(0.84f, 0.72f, 0.11f, 0.11f)
    )
    blocks.forEach { b ->
        val bx = w * b[0] + panOffset.x * 0.25f
        val by = h * b[1] + panOffset.y * 0.25f
        val bw = w * b[2]
        val bh = h * b[3]
        drawRect(
            color = Color(0xFF07111E),
            topLeft = Offset(bx, by),
            size = Size(bw, bh)
        )
        drawRect(
            color = accent.copy(alpha = 0.07f),
            topLeft = Offset(bx, by),
            size = Size(bw, bh),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}

private fun DrawScope.drawNavigationPuck(
    carPos: Offset,
    headingDegrees: Float,
    accent: Color
) {
    // Cyan outer illumination aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.52f), Color.Transparent),
            center = carPos,
            radius = 36.dp.toPx()
        ),
        center = carPos,
        radius = 36.dp.toPx()
    )
    // Glossy Deep Blue Circular Navigation Puck
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(IceBlue.copy(alpha = 0.45f), DeepBlue, Color(0xFF071F4A)),
            center = carPos,
            radius = 22.dp.toPx()
        ),
        radius = 22.dp.toPx(),
        center = carPos
    )
    drawCircle(
        color = GlassWhite.copy(alpha = 0.85f),
        radius = 22.dp.toPx(),
        center = carPos,
        style = Stroke(width = 1.4.dp.toPx())
    )
    rotate(degrees = headingDegrees, pivot = carPos) {
        val r = 11.dp.toPx()
        val chevron = Path().apply {
            moveTo(carPos.x, carPos.y - r * 1.15f)
            lineTo(carPos.x + r * 0.85f, carPos.y + r * 0.85f)
            lineTo(carPos.x, carPos.y + r * 0.40f)
            lineTo(carPos.x - r * 0.85f, carPos.y + r * 0.85f)
            close()
        }
        drawPath(chevron, color = GlassWhite)
    }
}

@Composable
private fun FluidCloudNavigationPill(
    nextTurnDistance: String,
    nextTurnRoad: String,
    nextTurnInstruction: String,
    remainingMinutes: Int,
    remainingKm: Float,
    speedAlertActive: Boolean,
    speedLimitKmh: Int?,
    accent: Color,
    onOpenFullMaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    var userExpanded by remember { mutableStateOf(false) }
    val imminentManeuver = nextTurnDistance.trim().endsWith(" m", ignoreCase = true)
    val isExpanded = userExpanded || imminentManeuver || speedAlertActive

    val pillCorner by animateDpAsState(
        targetValue = if (isExpanded) 22.dp else 30.dp,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = LivingFlow.CompactDurationMs,
            easing = LivingFlow.LaminarEasing
        ),
        label = "fluidCloudCorner"
    )
    val borderAccent = if (speedAlertActive) PulseCongestion else accent

    Surface(
        modifier = modifier
            .testTag("map_next_turn_pill")
            .clip(RoundedCornerShape(pillCorner))
            .animateContentSize(
                animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
            )
            .photonLuminousTouch(
                pressScale = 0.96f,
                glowColor = borderAccent,
                onClick = {
                    if (userExpanded) {
                        userExpanded = false
                        onOpenFullMaps()
                    } else {
                        userExpanded = true
                    }
                }
            ),
        color = Color(0xC2050C16),
        shape = RoundedCornerShape(pillCorner),
        border = BorderStroke(
            0.95.dp,
            Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.40f), borderAccent.copy(alpha = 0.68f)))
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isExpanded) 22.dp else 20.dp,
                vertical = if (isExpanded) 12.dp else 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TurnRight,
                    contentDescription = "Next turn",
                    tint = borderAccent,
                    modifier = Modifier.size(28.dp)
                )
                // Zero-bounce crisp transition on critical distance/road readout
                AnimatedContent(
                    targetState = nextTurnDistance to nextTurnRoad,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            fadeIn(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) togetherWith
                                fadeOut(tween(180, easing = LivingFlow.LaminarEasing))
                        }
                    },
                    label = "fluidTurnText"
                ) { (dist, road) ->
                    Column {
                        Text(
                            text = dist,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = GlassWhite
                        )
                        Text(
                            text = road,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) +
                    fadeIn(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)),
                exit = shrinkVertically(tween(220, easing = LivingFlow.LaminarEasing)) +
                    fadeOut(tween(180, easing = LivingFlow.LaminarEasing))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(0.8.dp)
                            .background(GlassWhite.copy(alpha = 0.14f))
                    )
                    Text(
                        text = if (speedAlertActive && speedLimitKmh != null) {
                            "Speed Alert · Limit $speedLimitKmh km/h"
                        } else {
                            nextTurnInstruction
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (speedAlertActive) PulseCongestion else IceBlue
                    )
                    if (remainingMinutes > 0) {
                        Text(
                            text = "$remainingMinutes min remaining · $remainingKm km · Tap again for full route",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapToolPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    Surface(
        modifier = Modifier
            .testTag(testTag)
            .size(40.dp)
            .clip(CircleShape)
            .photonLuminousTouch(pressScale = 0.90f, onClick = onClick),
        color = if (active) DeepBlue.copy(alpha = 0.58f) else Color(0xBD060D17),
        shape = CircleShape,
        border = BorderStroke(0.9.dp, if (active) accent else GlassWhite.copy(alpha = 0.24f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) accent else GlassWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
