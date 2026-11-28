package com.example.auto

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.model.DestinationCandidate
import com.example.data.model.DestinationSearchOutcome
import com.example.data.model.SavedPlace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.TimeZone

internal class LumaNavNavigationScreen(
    carContext: CarContext,
    private val runtime: LumaNavCarRuntime
) : Screen(carContext) {
    private var panMode = false

    init {
        lifecycleScope.launch {
            runtime.state.collect { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        if (!runtime.hasLocationPermission()) {
            val messageBuilder = MessageTemplate.Builder(
                CarText.create("Allow location access on your phone to use LumaNAV navigation.")
            )
            applyMessageHeader(messageBuilder)
            return messageBuilder.addAction(
                Action.Builder()
                    .setTitle("Open phone settings")
                    .setOnClickListener {
                        val settingsIntent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${carContext.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { carContext.startActivity(settingsIntent) }
                    }
                    .build()
            ).build()
        }

        val state = runtime.state.value
        val actions = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Search")
                    .setIcon(icon(android.R.drawable.ic_menu_search))
                    .setOnClickListener { screenManager.push(LumaNavSearchScreen(carContext, runtime)) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Saved")
                    .setIcon(icon(android.R.drawable.ic_menu_myplaces))
                    .setOnClickListener { screenManager.push(LumaNavSavedPlacesScreen(carContext, runtime)) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (state.wireframeMode) "Dark Map" else "Wireframe")
                    .setIcon(icon(android.R.drawable.ic_menu_mapmode))
                    .setOnClickListener { runtime.toggleMapStyle(); invalidate() }
                    .build()
            )
        if (state.isNavigating) {
            actions.addAction(
                Action.Builder()
                    .setTitle("End")
                    .setIcon(icon(android.R.drawable.ic_menu_close_clear_cancel))
                    .setOnClickListener {
                        runtime.stopRoute()
                        CarToast.makeText(carContext, "Navigation ended", CarToast.LENGTH_SHORT).show()
                        invalidate()
                    }
                    .build()
            )
        }

        val builder = NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.createCustom(0xFF0B1219.toInt()))
            .setActionStrip(actions.build())

        if (carContext.carAppApiLevel >= 2) {
            builder.setMapActionStrip(
                ActionStrip.Builder()
                    .addAction(Action.Builder(Action.PAN).setIcon(icon(android.R.drawable.ic_menu_crop, panMode)).build())
                    .addAction(
                        Action.Builder()
                            .setIcon(icon(android.R.drawable.ic_menu_mylocation))
                            .setOnClickListener { runtime.surfaceRenderer.recenter() }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setIcon(icon(android.R.drawable.ic_input_add))
                            .setOnClickListener { runtime.surfaceRenderer.zoomBy(1.25f) }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setIcon(icon(android.R.drawable.ic_delete))
                            .setOnClickListener { runtime.surfaceRenderer.zoomBy(0.8f) }
                            .build()
                    )
                    .build()
            )
            builder.setPanModeListener { inPanMode ->
                panMode = inPanMode
                invalidate()
            }
        }

        if (state.isNavigating) {
            val route = state.route
            if (route != null) {
                val step = Step.Builder(route.nextTurnInstruction)
                    .setManeuver(Maneuver.Builder(maneuverType(route.nextTurnInstruction)).build())
                    .setRoad(route.nextTurnRoad)
                    .build()
                val distanceMeters = parseDistanceMeters(route.nextTurnDistanceLabel)
                builder.setNavigationInfo(
                    RoutingInfo.Builder()
                        .setCurrentStep(step, Distance.create(distanceMeters, Distance.UNIT_METERS))
                        .build()
                )
                val arrival = DateTimeWithZone.create(
                    System.currentTimeMillis() + route.durationMinutes * 60_000L,
                    TimeZone.getDefault()
                )
                builder.setDestinationTravelEstimate(
                    TravelEstimate.Builder(
                        Distance.create(route.distanceKm.toDouble(), Distance.UNIT_KILOMETERS),
                        arrival
                    ).setRemainingTimeSeconds(route.durationMinutes * 60L).build()
                )
            }
        }
        return builder.build()
    }

    private fun icon(resource: Int, active: Boolean = false): CarIcon = CarIcon.Builder(
        IconCompat.createWithResource(carContext, resource)
    ).setTint(if (active) CarColor.createCustom(0xFFB8F6FF.toInt()) else CYAN).build()

    private fun maneuverType(instruction: String): Int = when {
        instruction.contains("left", true) -> Maneuver.TYPE_TURN_LEFT
        instruction.contains("right", true) -> Maneuver.TYPE_TURN_RIGHT
        instruction.contains("u-turn", true) -> Maneuver.TYPE_U_TURN_LEFT
        else -> Maneuver.TYPE_STRAIGHT
    }

    private fun parseDistanceMeters(label: String): Double {
        val value = label.replace(",", "").substringBefore(" ").toDoubleOrNull() ?: 500.0
        return if (label.contains("km", true)) value * 1000.0 else value
    }

    companion object {
        private val CYAN = CarColor.createCustom(0xFF00D9F5.toInt())
    }
}

internal class LumaNavSearchScreen(
    carContext: CarContext,
    private val runtime: LumaNavCarRuntime,
    initialQuery: String = ""
) : Screen(carContext) {
    private var results: List<DestinationCandidate> = emptyList()
    private var loading = false
    private var message = "Search for a destination."
    private var searchJob: Job? = null

    init {
        if (initialQuery.isNotBlank()) search(initialQuery, immediate = true)
    }

    override fun onGetTemplate(): Template {
        val rows = ItemList.Builder()
        results.forEach { candidate ->
            rows.addItem(
                Row.Builder()
                    .setTitle(candidate.name)
                    .addText(candidate.addressSummary)
                    .addText(candidate.category)
                    .setOnClickListener {
                        runtime.scope.launch {
                            val started = runtime.startRoute(candidate)
                            if (started) {
                                CarToast.makeText(carContext, "Route started", CarToast.LENGTH_SHORT).show()
                                screenManager.popToRoot()
                            } else {
                                CarToast.makeText(
                                    carContext,
                                    "Unable to calculate route. Check GPS and network.",
                                    CarToast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .build()
            )
        }
        rows.setNoItemsMessage(CarText.create(message))
        val searchBuilder = SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    search(searchText)
                }

                override fun onSearchSubmitted(searchText: String) {
                    search(searchText, immediate = true)
                }
            }
        )
            .setSearchHint("Search places")
            .setHeaderAction(Action.BACK)
        if (initialQuery.isNotBlank()) searchBuilder.setInitialSearchText(initialQuery)
        if (loading) {
            searchBuilder.setLoading(true)
        } else {
            searchBuilder.setItemList(rows.build())
        }
        return searchBuilder.build()
    }

    private fun search(text: String, immediate: Boolean = false) {
        searchJob?.cancel()
        if (text.isBlank()) {
            results = emptyList()
            loading = false
            message = "Enter a destination to search."
            invalidate()
            return
        }
        loading = true
        message = "Searching OpenStreetMap places…"
        invalidate()
        searchJob = runtime.scope.launch {
            if (!immediate) delay(450L)
            when (val outcome = runtime.search(text)) {
                is DestinationSearchOutcome.Matches -> {
                    results = outcome.candidates
                    message = if (results.isEmpty()) "No matching places found." else ""
                }
                is DestinationSearchOutcome.Unavailable -> {
                    results = emptyList()
                    message = outcome.reason
                }
            }
            loading = false
            invalidate()
        }
    }
}

internal class LumaNavSavedPlacesScreen(
    carContext: CarContext,
    private val runtime: LumaNavCarRuntime
) : Screen(carContext) {
    private var savedPlaces: List<SavedPlace> = emptyList()
    private var recentPlaces: List<SavedPlace> = emptyList()
    private var loaded = false

    init {
        lifecycleScope.launch {
            savedPlaces = runtime.savedPlaces()
            recentPlaces = runtime.recentPlaces()
            loaded = true
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        if (!loaded) {
            list.setNoItemsMessage(CarText.create("Loading saved places…"))
        } else {
            val homeWork = savedPlaces.filter {
                it.category.contains("home", true) || it.category.contains("work", true) ||
                    it.name.equals("Home", true) || it.name.equals("Work", true)
            }
            homeWork.forEach { place -> list.addItem(placeRow(place)) }

            list.addItem(
                Row.Builder()
                    .setTitle("Fuel nearby")
                    .addText("Find nearby fuel stations")
                    .setOnClickListener {
                        screenManager.push(LumaNavSearchScreen(carContext, runtime, "fuel station"))
                    }
                    .build()
            )

            if (recentPlaces.isNotEmpty()) {
                list.addItem(
                    Row.Builder()
                        .setTitle("Recent destinations")
                        .addText("Your recently selected places")
                        .setOnClickListener {
                            screenManager.push(LumaNavRecentPlacesScreen(carContext, runtime, recentPlaces))
                        }
                        .build()
                )
            }

            savedPlaces.filterNot { it in homeWork }.forEach { place -> list.addItem(placeRow(place)) }
            if (savedPlaces.isEmpty() && recentPlaces.isEmpty()) {
                list.setNoItemsMessage(CarText.create("No saved places yet. Save Home or Work in LumaNAV on your phone."))
            }
        }

        val builder = ListTemplate.Builder().setSingleList(list.build())
        applyListHeader(builder, "Saved Places")
        return builder.build()
    }

    private fun placeRow(place: SavedPlace): Row = Row.Builder()
        .setTitle(place.name)
        .addText(place.addressSummary)
        .setOnClickListener {
            runtime.scope.launch {
                val started = runtime.startRoute(place.toDestinationCandidate())
                if (started) screenManager.popToRoot()
                else CarToast.makeText(carContext, "Unable to calculate route. Check GPS and network.", CarToast.LENGTH_LONG).show()
            }
        }
        .build()
}

internal class LumaNavRecentPlacesScreen(
    carContext: CarContext,
    private val runtime: LumaNavCarRuntime,
    private val recentPlaces: List<SavedPlace>
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        recentPlaces.forEach { place ->
            list.addItem(
                Row.Builder()
                    .setTitle(place.name)
                    .addText(place.addressSummary)
                    .setOnClickListener {
                        runtime.scope.launch {
                            val started = runtime.startRoute(place.toDestinationCandidate())
                            if (started) screenManager.popToRoot()
                            else CarToast.makeText(carContext, "Unable to calculate route.", CarToast.LENGTH_LONG).show()
                        }
                    }
                    .build()
            )
        }
        if (recentPlaces.isEmpty()) list.setNoItemsMessage(CarText.create("No recent destinations."))
        val builder = ListTemplate.Builder().setSingleList(list.build())
        applyListHeader(builder, "Recent Destinations")
        return builder.build()
    }
}

@Suppress("DEPRECATION")
private fun LumaNavSavedPlacesScreen.applyListHeader(
    builder: ListTemplate.Builder,
    title: String
) {
    if (carContext.carAppApiLevel >= 7) {
        builder.setHeader(
            androidx.car.app.model.Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .build()
        )
    } else {
        builder.setTitle(title)
        builder.setHeaderAction(Action.BACK)
    }
}

@Suppress("DEPRECATION")
private fun LumaNavRecentPlacesScreen.applyListHeader(
    builder: ListTemplate.Builder,
    title: String
) {
    if (carContext.carAppApiLevel >= 7) {
        builder.setHeader(
            androidx.car.app.model.Header.Builder()
                .setTitle(title)
                .setStartHeaderAction(Action.BACK)
                .build()
        )
    } else {
        builder.setTitle(title)
        builder.setHeaderAction(Action.BACK)
    }
}

@Suppress("DEPRECATION")
private fun LumaNavNavigationScreen.applyMessageHeader(
    builder: MessageTemplate.Builder
) {
    if (carContext.carAppApiLevel >= 7) {
        builder.setHeader(
            androidx.car.app.model.Header.Builder()
                .setTitle("Location permission required")
                .setStartHeaderAction(Action.APP_ICON)
                .build()
        )
    } else {
        builder.setTitle("Location permission required")
        builder.setHeaderAction(Action.APP_ICON)
    }
}
