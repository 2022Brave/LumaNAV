package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.backgroundColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionBase
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionColor
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionHeight
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val AUVIONYX_DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val ROUTE_SOURCE_ID = "auvionyx-live-route"
private const val ROUTE_LAYER_ID = "auvionyx-live-route-line"
private const val POSITION_SOURCE_ID = "auvionyx-live-position"
private const val POSITION_LAYER_ID = "auvionyx-live-position-dot"

/**
 * Native MapLibre map shared by the Maps screen and HUD.
 * Map state stays separate from the UI: route and location are GeoJSON overlays.
 * User gestures temporarily pause camera-follow; the location puck continues updating.
 */
@Composable
fun NativeMapLibreView(
    currentLocation: Pair<Double, Double>?,
    routePoints: List<Pair<Double, Double>>,
    headingDegrees: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    show3DBuildings: Boolean = true,
    styleUri: String = AUVIONYX_DARK_STYLE,
    attributionBottomMarginDp: Int = 0,
    wireframeMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }
    var styleError by remember { mutableStateOf<String?>(null) }
    var cameraFollowEnabled by remember { mutableStateOf(true) }
    var retryToken by remember { mutableStateOf(0) }
    var hasCenteredOnFix by remember { mutableStateOf(false) }
    var lastRequestedStyle by remember { mutableStateOf(AUVIONYX_DARK_STYLE) }
    var lastRequestedWireframe by remember { mutableStateOf(false) }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }

    val cameraGestureListener = remember(mapView) {
        MapLibreMap.OnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                cameraFollowEnabled = false
            }
        }
    }
    val mapFailureListener = remember(mapView) {
        MapView.OnDidFailLoadingMapListener { message ->
            // Tile failures can happen after the base style is already visible. Do not
            // replace a working/partially rendered map with the full-screen error state.
            styleError = message.take(180).ifBlank { "Some map resources could not be loaded." }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.addOnDidFailLoadingMapListener(mapFailureListener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.removeOnDidFailLoadingMapListener(mapFailureListener)
            mapLibreMap?.removeOnCameraMoveStartedListener(cameraGestureListener)
            mapView.onDestroy()
            mapLibreMap = null
            loadedStyle = null
        }
    }


    LaunchedEffect(retryToken, mapLibreMap, styleUri, wireframeMode) {
        if (mapLibreMap != null && (retryToken > 0 || styleUri != lastRequestedStyle || wireframeMode != lastRequestedWireframe)) {
            lastRequestedStyle = styleUri
            lastRequestedWireframe = wireframeMode
            styleLoaded = false
            styleError = null
            loadedStyle = null
            mapLibreMap?.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                if (style.getSource(ROUTE_SOURCE_ID) == null) {
                    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, emptyFeatureCollection()))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                            lineColor("#00D9F5"),
                            lineWidth(5.5f),
                            lineCap(Property.LINE_CAP_ROUND),
                            lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    )
                }
                if (style.getSource(POSITION_SOURCE_ID) == null) {
                    style.addSource(GeoJsonSource(POSITION_SOURCE_ID, emptyFeatureCollection()))
                    style.addLayer(
                        CircleLayer(POSITION_LAYER_ID, POSITION_SOURCE_ID).withProperties(
                            circleRadius(7.5f),
                            circleColor("#00D9F5"),
                            circleStrokeColor("#FFFFFF"),
                            circleStrokeWidth(2.2f)
                        )
                    )
                }
                if (show3DBuildings) addLumaNav3DBuildings(style)
                if (wireframeMode) applyLumaNavWireframe(style)
                lastRequestedWireframe = wireframeMode
                loadedStyle = style
                styleLoaded = true
            }
        }
    }

    LaunchedEffect(styleLoaded, styleError, retryToken) {
        if (!styleLoaded && styleError == null) {
            delay(15_000L)
            if (!styleLoaded && styleError == null) {
                styleError = "Map is taking too long. Check your internet and retry."
            }
        }
    }

    LaunchedEffect(loadedStyle, currentLocation, routePoints, headingDegrees, accent, cameraFollowEnabled) {
        val style = loadedStyle ?: return@LaunchedEffect
        val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
        val positionSource = style.getSourceAs<GeoJsonSource>(POSITION_SOURCE_ID)

        val routeCoordinates = routePoints.map { (lat, lon) -> Point.fromLngLat(lon, lat) }
        val routeFeature = if (routeCoordinates.size >= 2) {
            Feature.fromGeometry(LineString.fromLngLats(routeCoordinates))
        } else null
        routeSource?.setGeoJson(
            if (routeFeature != null) FeatureCollection.fromFeatures(arrayOf(routeFeature))
            else emptyFeatureCollection()
        )

        if (currentLocation != null) {
            val (latitude, longitude) = currentLocation
            positionSource?.setGeoJson(
                FeatureCollection.fromFeatures(
                    arrayOf(Feature.fromGeometry(Point.fromLngLat(longitude, latitude)))
                )
            )
            if (cameraFollowEnabled) {
                val camera = CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(16.2)
                    .bearing(headingDegrees.toDouble())
                    .tilt(48.0)
                    .build()
                if (!hasCenteredOnFix) {
                    mapLibreMap?.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
                    hasCenteredOnFix = true
                } else {
                    mapLibreMap?.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 700)
                }
            }
        } else {
            positionSource?.setGeoJson(emptyFeatureCollection())
            if (routeCoordinates.size >= 2 && !hasCenteredOnFix) {
                val first = routeCoordinates.first()
                mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude(), first.longitude()), 12.0))
                hasCenteredOnFix = true
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = {
            mapView.apply {
                setBackgroundColor(Color.Black.toArgb())
                getMapAsync { map ->
                    mapLibreMap = map
                    map.uiSettings.isCompassEnabled = false
                    if (attributionBottomMarginDp > 0) {
                        val bottomMarginPx = (attributionBottomMarginDp * context.resources.displayMetrics.density).toInt()
                        map.uiSettings.setLogoMargins(12, 0, 0, bottomMarginPx)
                        map.uiSettings.setAttributionMargins(12, 0, 0, bottomMarginPx)
                    }
                    map.uiSettings.isRotateGesturesEnabled = true
                    map.uiSettings.isTiltGesturesEnabled = true
                    map.addOnCameraMoveStartedListener(cameraGestureListener)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(28.6139, 77.2090))
                        .zoom(10.5)
                        .build()
                    map.setStyle(Style.Builder().fromUri(AUVIONYX_DARK_STYLE)) { style ->
                        styleError = null
                        if (style.getSource(ROUTE_SOURCE_ID) == null) {
                            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, emptyFeatureCollection()))
                            style.addLayer(
                                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                                    lineColor("#00D9F5"),
                                    lineWidth(5.5f),
                                    lineCap(Property.LINE_CAP_ROUND),
                                    lineJoin(Property.LINE_JOIN_ROUND)
                                )
                            )
                        }
                        if (style.getSource(POSITION_SOURCE_ID) == null) {
                            style.addSource(GeoJsonSource(POSITION_SOURCE_ID, emptyFeatureCollection()))
                            style.addLayer(
                                CircleLayer(POSITION_LAYER_ID, POSITION_SOURCE_ID).withProperties(
                                    circleRadius(7.5f),
                                    circleColor(accent.toHexColor()),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(2.2f)
                                )
                            )
                        }
                        if (show3DBuildings) addLumaNav3DBuildings(style)
                        if (wireframeMode) applyLumaNavWireframe(style)
                        lastRequestedWireframe = wireframeMode
                        loadedStyle = style
                        styleLoaded = true
                    }
                }
            }
        },
            update = { /* Native map view is retained. */ }
        )
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            shape = CircleShape,
            color = Color(0xCC07111D),
            contentColor = accent
        ) {
            IconButton(
                onClick = {
                    cameraFollowEnabled = true
                    currentLocation?.let { (lat, lon) ->
                        hasCenteredOnFix = true
                        mapLibreMap?.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(lat, lon))
                                    .zoom(16.2)
                                    .bearing(headingDegrees.toDouble())
                                    .tilt(32.0)
                                    .build()
                            ),
                            500
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = if (cameraFollowEnabled) Icons.Outlined.GpsFixed else Icons.Outlined.GpsNotFixed,
                    contentDescription = if (cameraFollowEnabled) "Camera following location" else "Recenter and follow location",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (!styleLoaded) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xDD07111D),
                shape = CircleShape
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    if (styleError == null) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = styleError ?: "Map unavailable",
                            color = Color.White
                        )
                    }
                }
            }
            if (styleError != null) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(top = 76.dp).clickable { retryToken += 1; hasCenteredOnFix = false },
                    color = accent.copy(alpha = 0.18f),
                    shape = CircleShape,
                    contentColor = Color.White
                ) {
                    Text(
                        text = "Retry map",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}


/**
 * OpenFreeMap exposes OSM building polygons in the "building" vector source-layer.
 * Extrusion heights are only available where OSM contributors supplied building data.
 */

private fun applyLumaNavWireframe(style: Style) {
    style.layers.forEach { layer ->
        when (layer) {
            is BackgroundLayer -> layer.setProperties(backgroundColor("#000000"))
            is FillLayer -> layer.setProperties(fillOpacity(0f))
            is RasterLayer -> layer.setProperties(visibility(Property.NONE))
            is SymbolLayer -> layer.setProperties(visibility(Property.NONE))
            is LineLayer -> if (layer.id != ROUTE_LAYER_ID) layer.setProperties(
                lineColor("#00D9F5"),
                lineOpacity(0.88f),
                lineWidth(1.15f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND)
            )
            is FillExtrusionLayer -> layer.setProperties(visibility(Property.NONE))
        }
    }
}

private fun addLumaNav3DBuildings(style: Style) {
    if (style.getLayer("lumanav-buildings-3d") != null) return
    if (style.getSource("openmaptiles") == null) return

    val buildings = FillExtrusionLayer("lumanav-buildings-3d", "openmaptiles").apply {
        sourceLayer = "building"
        minZoom = 15f
        setProperties(
            fillExtrusionColor("#26384B"),
            fillExtrusionHeight(Expression.get("render_height")),
            fillExtrusionBase(Expression.get("render_min_height")),
            fillExtrusionOpacity(0.88f)
        )
    }
    style.addLayer(buildings)
}

private fun emptyFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(emptyArray<Feature>())

private fun Color.toHexColor(): String =
    String.format("#%06X", 0xFFFFFF and toArgb())
