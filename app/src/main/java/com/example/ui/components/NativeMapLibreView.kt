package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
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
 * A real vector map rendered in-process by MapLibre. No MediaProjection, screenshots,
 * simulated road canvas, or dependency on another app's visible window.
 *
 * Coordinates are latitude/longitude pairs in app state; GeoJSON expects longitude/latitude.
 */
@Composable
fun NativeMapLibreView(
    currentLocation: Pair<Double, Double>?,
    routePoints: List<Pair<Double, Double>>,
    headingDegrees: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var hasCenteredOnFix by remember { mutableStateOf(false) }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
            mapLibreMap = null
            loadedStyle = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            mapView.apply {
                getMapAsync { map ->
                    mapLibreMap = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isRotateGesturesEnabled = true
                    map.uiSettings.isTiltGesturesEnabled = false
                    map.uiSettings.isZoomControlsEnabled = false
                    map.setStyle(Style.Builder().fromUri(AUVIONYX_DARK_STYLE)) { style ->
                        if (style.getSource(ROUTE_SOURCE_ID) == null) {
                            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, emptyFeatureCollection()))
                            style.addLayer(
                                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                                    lineColor(accent.toHexColor()),
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
                        loadedStyle = style
                    }
                }
            }
        },
        update = { /* Map updates are driven below without recreating the native view. */ }
    )

    LaunchedEffect(loadedStyle, currentLocation, routePoints, headingDegrees, accent) {
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
            val camera = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(16.2)
                .bearing(headingDegrees.toDouble())
                .tilt(32.0)
                .build()
            if (!hasCenteredOnFix) {
                mapLibreMap?.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
                hasCenteredOnFix = true
            } else {
                mapLibreMap?.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 450)
            }
        } else {
            positionSource?.setGeoJson(emptyFeatureCollection())
        }
    }
}

private fun emptyFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(emptyArray<Feature>())

private fun Color.toHexColor(): String =
    String.format("#%06X", 0xFFFFFF and toArgb())
