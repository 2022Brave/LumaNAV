package com.example.auto

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Gravity
import android.view.Surface
import android.view.ViewGroup
import android.app.Presentation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.example.data.maps.LiveDrivingRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.backgroundColor
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineBlur
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.ln

internal class LumaNavCarSurfaceRenderer(
    private val context: Context,
    private val runtime: LumaNavCarRuntime
) : SurfaceCallback {
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: LumaNavMapPresentation? = null
    private var hostSurface: Surface? = null
    private var visibleArea = Rect()
    private var stableArea = Rect()
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var stateJob: Job? = null

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        releaseDisplay()
        val surface = surfaceContainer.surface ?: return
        if (!surface.isValid || surfaceContainer.width <= 0 || surfaceContainer.height <= 0) return
        hostSurface = surface
        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        virtualDisplay = manager.createVirtualDisplay(
            "LumaNAV-AndroidAuto-Map",
            surfaceContainer.width,
            surfaceContainer.height,
            surfaceContainer.dpi.coerceAtLeast(120),
            surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        val display = virtualDisplay?.display ?: return
        presentation = LumaNavMapPresentation(context, display, runtime).also {
            it.setCancelable(false)
            it.show()
            val safe = when {
                !stableArea.isEmpty -> stableArea
                !visibleArea.isEmpty -> visibleArea
                else -> Rect(0, 0, surfaceContainer.width, surfaceContainer.height)
            }
            it.updateSafeArea(safe)
        }
        stateJob?.cancel()
        stateJob = runtime.scope.launch {
            runtime.state.collect { state ->
                presentation?.renderState(state)
            }
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        stateJob?.cancel()
        stateJob = null
        releaseDisplay()
    }

    override fun onVisibleAreaChanged(area: Rect) {
        visibleArea = Rect(area)
        presentation?.updateSafeArea(
            when {
                !stableArea.isEmpty -> stableArea
                !visibleArea.isEmpty -> visibleArea
                else -> Rect(0, 0, surfaceWidth, surfaceHeight)
            }
        )
    }

    override fun onStableAreaChanged(area: Rect) {
        stableArea = Rect(area)
        presentation?.updateSafeArea(
            when {
                !stableArea.isEmpty -> stableArea
                !visibleArea.isEmpty -> visibleArea
                else -> Rect(0, 0, surfaceWidth, surfaceHeight)
            }
        )
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        presentation?.pan(distanceX, distanceY)
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        presentation?.pan(velocityX * 0.12f, velocityY * 0.12f)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        presentation?.scale(focusX, focusY, scaleFactor)
    }

    fun recenter() {
        presentation?.recenter()
    }

    fun zoomBy(factor: Float) {
        presentation?.scale(-1f, -1f, factor)
    }

    fun release() {
        stateJob?.cancel()
        stateJob = null
        releaseDisplay()
    }

    private fun releaseDisplay() {
        presentation?.dispose()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
        hostSurface?.release()
        hostSurface = null
        surfaceWidth = 0
        surfaceHeight = 0
    }
}

private class LumaNavMapPresentation(
    hostContext: Context,
    display: android.view.Display,
    private val runtime: LumaNavCarRuntime
) : Presentation(hostContext, display) {
    private lateinit var root: FrameLayout
    private lateinit var mapView: MapView
    private lateinit var speedPill: TextView
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var styleLoading = false
    private var latestState: LumaNavCarState = runtime.state.value
    private var safeArea = Rect()
    private var lastCenteredLocation: Pair<Double, Double>? = null
    private var disposed = false

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val presentationContext = context
        MapLibre.getInstance(presentationContext.applicationContext)
        root = FrameLayout(presentationContext)
        mapView = MapView(presentationContext)
        speedPill = TextView(presentationContext)
        mapView.onCreate(savedInstanceState)
        root.setBackgroundColor(android.graphics.Color.BLACK)
        root.addView(
            mapView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        configureSpeedPill()
        root.addView(speedPill)
        setContentView(root)
        mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap
            mapLibreMap.uiSettings.isCompassEnabled = false
            mapLibreMap.uiSettings.isLogoEnabled = false
            mapLibreMap.uiSettings.isAttributionEnabled = true
            loadStyle(latestState.wireframeMode)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapView.onResume()
    }

    override fun onStop() {
        mapView.onPause()
        mapView.onStop()
        super.onStop()
    }

    fun renderState(state: LumaNavCarState) {
        latestState = state
        speedPill.text = "${state.location?.speedKmh ?: 0}  km/h"
        if (map != null && !styleLoading &&
            (style == null || state.wireframeMode != wireframeApplied)
        ) {
            loadStyle(state.wireframeMode)
        } else if (style != null) {
            updateOverlays(state.route)
            state.location?.let { reading ->
                val point = reading.latitude to reading.longitude
                if (followLocation && point != lastCenteredLocation) {
                    map?.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(reading.latitude, reading.longitude))
                                .zoom(15.5)
                                .bearing(reading.bearingDeg.toDouble())
                                .build()
                        )
                    )
                    lastCenteredLocation = point
                }
            }
        }
    }

    fun updateSafeArea(area: Rect) {
        safeArea = Rect(area)
        if (root.width == 0 || root.height == 0) {
            root.post { updateSafeArea(safeArea) }
            return
        }
        val density = resources.displayMetrics.density
        val margin = (16f * density).toInt()
        val params = speedPill.layoutParams as? FrameLayout.LayoutParams ?: return
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.setMargins(
            margin,
            margin,
            (root.width - safeArea.right).coerceAtLeast(0) + margin,
            (root.height - safeArea.bottom).coerceAtLeast(0) + margin
        )
        speedPill.layoutParams = params
    }

    fun pan(dx: Float, dy: Float) {
        followLocation = false
        map?.scrollBy(dx, dy)
    }

    fun scale(focusX: Float, focusY: Float, factor: Float) {
        if (factor <= 0f) return
        followLocation = false
        val zoomDelta = ln(factor.toDouble()) / ln(2.0)
        val point = if (focusX >= 0f && focusY >= 0f) {
            Point(focusX.toInt(), focusY.toInt())
        } else {
            null
        }
        val update = if (point != null) {
            CameraUpdateFactory.zoomBy(zoomDelta, point)
        } else {
            CameraUpdateFactory.zoomBy(zoomDelta)
        }
        map?.animateCamera(update)
    }

    private var followLocation = true

    fun recenter() {
        followLocation = true
        val location = latestState.location ?: return
        map?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(15.5)
                    .bearing(location.bearingDeg.toDouble())
                    .build()
            )
        )
        lastCenteredLocation = location.latitude to location.longitude
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        if (isShowing) dismiss()
        runCatching { mapView.onDestroy() }
        map = null
        style = null
    }

    private var wireframeApplied = false

    private fun loadStyle(wireframe: Boolean) {
        val mapLibreMap = map ?: return
        styleLoading = true
        style = null
        wireframeApplied = wireframe
        mapLibreMap.setStyle(Style.Builder().fromUri(DARK_STYLE_URL)) { loaded ->
            style = loaded
            styleLoading = false
            if (wireframe) {
                loaded.layers.filterIsInstance<BackgroundLayer>().firstOrNull()?.setProperties(
                    backgroundColor("#000000")
                )
                loaded.layers.filterIsInstance<LineLayer>().forEach { layer ->
                    layer.setProperties(
                        lineColor("#00D9F5"),
                        lineWidth(1.2f),
                        lineOpacity(0.92f)
                    )
                }
            }
            addNavigationLayers(loaded)
            updateOverlays(latestState.route)
            latestState.location?.let { reading ->
                mapLibreMap.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(reading.latitude, reading.longitude))
                            .zoom(15.5)
                            .bearing(reading.bearingDeg.toDouble())
                            .build()
                    )
                )
                lastCenteredLocation = reading.latitude to reading.longitude
            }
        }
    }

    private fun addNavigationLayers(loaded: Style) {
        if (loaded.getSource(ROUTE_SOURCE) == null) {
            loaded.addSource(GeoJsonSource(ROUTE_SOURCE, emptyCollection()))
            loaded.addLayer(
                LineLayer(ROUTE_GLOW_LAYER, ROUTE_SOURCE).withProperties(
                    lineColor("#00D9F5"),
                    lineWidth(10f),
                    lineBlur(4f),
                    lineOpacity(0.55f),
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
            loaded.addLayer(
                LineLayer(ROUTE_LINE_LAYER, ROUTE_SOURCE).withProperties(
                    lineColor("#00D9F5"),
                    lineWidth(4.5f),
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
        }
        if (loaded.getSource(PUCK_SOURCE) == null) {
            loaded.addSource(GeoJsonSource(PUCK_SOURCE, emptyCollection()))
            loaded.addLayer(
                CircleLayer(PUCK_LAYER, PUCK_SOURCE).withProperties(
                    circleRadius(7f),
                    circleColor("#00D9F5"),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(2f)
                )
            )
        }
    }

    private fun updateOverlays(route: LiveDrivingRoute?) {
        val loaded = style ?: return
        val routeSource = loaded.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)
        val routeCoordinates = route?.polylinePoints?.map { (lat, lon) ->
            Point.fromLngLat(lon, lat)
        }.orEmpty()
        val routeFeature = if (routeCoordinates.size >= 2) {
            Feature.fromGeometry(LineString.fromLngLats(routeCoordinates))
        } else null
        routeSource?.setGeoJson(
            if (routeFeature != null) FeatureCollection.fromFeatures(arrayOf(routeFeature))
            else emptyCollection()
        )

        val location = latestState.location
        val puckSource = loaded.getSourceAs<GeoJsonSource>(PUCK_SOURCE)
        val puck = location?.let {
            FeatureCollection.fromFeatures(
                arrayOf(Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)))
            )
        } ?: emptyCollection()
        puckSource?.setGeoJson(puck)
    }

    private fun configureSpeedPill() {
        val density = resources.displayMetrics.density
        speedPill.apply {
            text = "0  km/h"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(
                (18f * density).toInt(),
                (10f * density).toInt(),
                (18f * density).toInt(),
                (10f * density).toInt()
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density
                setColor(0xD9000000.toInt())
                setStroke((1f * density).toInt(), android.graphics.Color.rgb(0, 217, 245))
            }
            elevation = 8f * density
        }
        speedPill.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )
    }

    companion object {
        private const val DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
        private const val ROUTE_SOURCE = "lumanav-aa-route-source"
        private const val ROUTE_GLOW_LAYER = "lumanav-aa-route-glow"
        private const val ROUTE_LINE_LAYER = "lumanav-aa-route-line"
        private const val PUCK_SOURCE = "lumanav-aa-puck-source"
        private const val PUCK_LAYER = "lumanav-aa-puck-layer"

        private fun emptyCollection() = FeatureCollection.fromFeatures(emptyArray<Feature>())
    }
}
