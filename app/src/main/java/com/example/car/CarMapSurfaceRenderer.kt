package com.example.car

import android.app.Presentation
import android.graphics.Paint
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.View
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
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
import kotlin.math.*

/**
 * Renders the phone-owned MapLibre navigation state into Android Auto's host surface.
 * The virtual display is only a rendering target; GPS and routing remain on the phone.
 */
internal class CarMapSurfaceRenderer(
    private val carContext: CarContext,
    lifecycle: Lifecycle
) : SurfaceCallback, DefaultLifecycleObserver {

    private var surfaceContainer: SurfaceContainer? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var routeSource: GeoJsonSource? = null
    private var positionSource: GeoJsonSource? = null
    private var latestSnapshot = CarNavigationBridge.snapshot.value
    private var hasCenteredOnFix = false
    private var lastCameraLocation: Pair<Double, Double>? = null
    private var lastCameraHeading = 0

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        runCatching { carContext.getCarService(AppManager::class.java).setSurfaceCallback(this) }
    }

    fun update(snapshot: CarNavigationSnapshot) {
        latestSnapshot = snapshot
        renderSnapshot()
    }

    override fun onSurfaceAvailable(container: SurfaceContainer) {
        releaseSurfaceResources(releaseHostSurface = true)
        surfaceContainer = container
        try {
            MapLibre.getInstance(carContext.applicationContext)
            val displayManager = carContext.getSystemService(DisplayManager::class.java)
            val display = displayManager.createVirtualDisplay(
                "LumaNAV-AndroidAuto-Map",
                container.width,
                container.height,
                container.dpi,
                container.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            ) ?: throw IllegalStateException("Unable to create Android Auto map display")
            virtualDisplay = display

            val mapPresentation = Presentation(carContext, display.display)
            presentation = mapPresentation
            val view = MapView(carContext).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, Paint())
                onCreate(null)
                onStart()
                onResume()
            }
            mapView = view
            mapPresentation.setContentView(view)
            mapPresentation.show()

            view.getMapAsync { mapLibreMap ->
                map = mapLibreMap
                mapLibreMap.uiSettings.isCompassEnabled = false
                mapLibreMap.uiSettings.isRotateGesturesEnabled = true
                mapLibreMap.uiSettings.isTiltGesturesEnabled = false
                mapLibreMap.setStyle(Style.Builder().fromUri(DARK_STYLE_URI)) { style ->
                    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, emptyFeatureCollection()))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                            lineColor("#00D9F5"),
                            lineWidth(6f),
                            lineCap(Property.LINE_CAP_ROUND),
                            lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    )
                    style.addSource(GeoJsonSource(POSITION_SOURCE_ID, emptyFeatureCollection()))
                    style.addLayer(
                        CircleLayer(POSITION_LAYER_ID, POSITION_SOURCE_ID).withProperties(
                            circleRadius(8f),
                            circleColor("#00D9F5"),
                            circleStrokeColor("#FFFFFF"),
                            circleStrokeWidth(2f)
                        )
                    )
                    routeSource = style.getSourceAs(ROUTE_SOURCE_ID)
                    positionSource = style.getSourceAs(POSITION_SOURCE_ID)
                    mapLibreMap.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(22.5, 78.9))
                                .zoom(4.3)
                                .build()
                        )
                    )
                    renderSnapshot()
                }
            }
        } catch (error: Exception) {
            releaseSurfaceResources(releaseHostSurface = true)
        }
    }

    private fun renderSnapshot() {
        val activeMap = map ?: return
        val route = routeSource
        val position = positionSource
        val routeCoordinates = latestSnapshot.routePoints
            .filter { (lat, lon) -> lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0 }
            .map { (lat, lon) -> Point.fromLngLat(lon, lat) }

        val routeFeature = if (routeCoordinates.size >= 2) {
            Feature.fromGeometry(LineString.fromLngLats(routeCoordinates))
        } else null
        route?.setGeoJson(
            routeFeature?.let { FeatureCollection.fromFeatures(arrayOf(it)) } ?: emptyFeatureCollection()
        )

        val location = latestSnapshot.currentLocation
        if (location != null &&
            location.first.isFinite() && location.first in -90.0..90.0 &&
            location.second.isFinite() && location.second in -180.0..180.0
        ) {
            position?.setGeoJson(
                FeatureCollection.fromFeatures(
                    arrayOf(Feature.fromGeometry(Point.fromLngLat(location.second, location.first)))
                )
            )
            val previous = lastCameraLocation
            val movedEnough = previous == null || distanceMeters(previous, location) >= 8.0
            val headingChanged = abs(((latestSnapshot.headingDegrees - lastCameraHeading + 540) % 360) - 180) >= 8
            if (latestSnapshot.gpsAvailable && (previous == null || movedEnough || headingChanged)) {
                val camera = CameraPosition.Builder()
                    .target(LatLng(location.first, location.second))
                    .zoom(16.2)
                    .bearing(latestSnapshot.headingDegrees.toDouble())
                    .tilt(42.0)
                    .build()
                if (!hasCenteredOnFix) {
                    activeMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
                    hasCenteredOnFix = true
                } else {
                    activeMap.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 650)
                }
                lastCameraLocation = location
                lastCameraHeading = latestSnapshot.headingDegrees
            }
        } else {
            position?.setGeoJson(emptyFeatureCollection())
        }
    }

    override fun onVisibleAreaChanged(visibleArea: android.graphics.Rect) = Unit
    override fun onStableAreaChanged(stableArea: android.graphics.Rect) = Unit

    override fun onScroll(distanceX: Float, distanceY: Float) {
        map?.scrollBy(-distanceX, -distanceY, 0)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        val activeMap = map ?: return
        val currentZoom = activeMap.cameraPosition.zoom
        val zoomDelta = ln(scaleFactor.toDouble()) / ln(Math.PI / 2.0) * 1.5
        activeMap.setZoom((currentZoom + zoomDelta).coerceIn(3.0, 19.0), android.graphics.PointF(focusX, focusY), 0)
    }

    override fun onSurfaceDestroyed(container: SurfaceContainer) {
        releaseSurfaceResources(releaseHostSurface = true)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        runCatching { carContext.getCarService(AppManager::class.java).setSurfaceCallback(null) }
        releaseSurfaceResources(releaseHostSurface = true)
        owner.lifecycle.removeObserver(this)
    }

    private fun releaseSurfaceResources(releaseHostSurface: Boolean) {
        runCatching { presentation?.dismiss() }
        presentation = null
        map = null
        routeSource = null
        positionSource = null
        mapView?.let { view ->
            runCatching { view.onPause() }
            runCatching { view.onStop() }
            runCatching { view.onDestroy() }
        }
        mapView = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        if (releaseHostSurface) runCatching { surfaceContainer?.surface?.release() }
        surfaceContainer = null
        hasCenteredOnFix = false
        lastCameraLocation = null
    }

    private fun distanceMeters(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.first - a.first)
        val dLon = Math.toRadians(b.second - a.second)
        val lat1 = Math.toRadians(a.first)
        val lat2 = Math.toRadians(b.first)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return earthRadius * 2 * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun emptyFeatureCollection() = FeatureCollection.fromFeatures(emptyArray<Feature>())

    private companion object {
        const val DARK_STYLE_URI = "https://tiles.openfreemap.org/styles/dark"
        const val ROUTE_SOURCE_ID = "lumanav-car-route"
        const val ROUTE_LAYER_ID = "lumanav-car-route-line"
        const val POSITION_SOURCE_ID = "lumanav-car-position"
        const val POSITION_LAYER_ID = "lumanav-car-position-dot"
    }
}
