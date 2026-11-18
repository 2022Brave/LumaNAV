package com.example

import com.example.core.LiveGoogleMapsSyncBridge
import com.example.data.model.AuvionyxModule
import com.example.data.model.AuvionyxSampleData
import com.example.data.model.DashboardSplitRatio
import com.example.data.model.GoogleMapsIntegrationStatus
import com.example.data.model.MediaAppSource
import com.example.data.model.ServiceAvailabilityState
import com.example.data.model.VehicleTelemetry
import com.example.data.preferences.AuvionyxSettingsState
import com.example.data.voice.VoiceCommandAction
import com.example.data.voice.VoiceCommandCategory
import com.example.data.voice.VoiceCommandParser
import com.example.data.voice.VoiceVisualState
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.MediaPlaybackReducer
import com.example.viewmodel.MediaPlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun officialBrandPalette_matchesFrozenSpec() {
    assertEquals(0xFF00D9F5.toInt(), ElectricCyan.value.let { (it shr 32).toInt() })
    assertEquals(0xFF63BFFF.toInt(), IceBlue.value.let { (it shr 32).toInt() })
    assertEquals(0xFF1267D5.toInt(), DeepBlue.value.let { (it shr 32).toInt() })
    assertEquals(0xFF000000.toInt(), ObsidianBlack.value.let { (it shr 32).toInt() })
    assertEquals(0xFFFFFFFF.toInt(), GlassWhite.value.let { (it shr 32).toInt() })
  }

  @Test
  fun ecosystemModules_includeAllNineCoreModules() {
    val routes = AuvionyxModule.entries.map { it.route }.toSet()
    assertTrue(
      routes.containsAll(
        listOf("drive", "maps", "music", "hud", "pulse", "voice", "aod", "vehicle", "settings")
      )
    )
  }

  @Test
  fun dashboardSplitRatios_coverAllRequiredLayouts() {
    assertEquals(0.70f, DashboardSplitRatio.RATIO_70_30.mapWeight, 0.001f)
    assertEquals(0.30f, DashboardSplitRatio.RATIO_70_30.mediaWeight, 0.001f)
    assertEquals(0.60f, DashboardSplitRatio.RATIO_60_40.mapWeight, 0.001f)
    assertEquals(0.50f, DashboardSplitRatio.RATIO_50_50.mapWeight, 0.001f)
    assertEquals(1.0f, DashboardSplitRatio.FULL_MAPS.mapWeight, 0.001f)
    assertEquals(1.0f, DashboardSplitRatio.FULL_MUSIC.mediaWeight, 0.001f)
  }

  @Test
  fun telemetryAndMapsDefaults_honestlyDiscloseDemoAndUnverifiedData() {
    val telemetry = VehicleTelemetry()
    assertEquals(0, telemetry.speedKmh)
    assertFalse("Demo simulation must stay off by default", telemetry.isSimulationRunning)
    assertNull("Speed limit must be null unless verified", telemetry.speedLimitKmh)
    assertNull("Weather must remain null until live weather loads", telemetry.weatherTempC)
    assertFalse("GPS should not claim live fix by default", telemetry.isLiveGpsActive)
    assertTrue("No fake tire telemetry when CAN/OBD-II is disconnected", telemetry.tirePsi.isEmpty())

    val settings = AuvionyxSettingsState()
    assertFalse("Demo simulation toggle must default to false", settings.demoSimulationEnabled)

    val mapsStatus = GoogleMapsIntegrationStatus()
    assertFalse(mapsStatus.isMapsSdkConfigured)
    assertTrue(mapsStatus.renderModeLabel.contains("DEMO", ignoreCase = true))

    assertTrue(AuvionyxSampleData.routes.all { it.name.contains("Demo", ignoreCase = true) })
    assertEquals("Lamentis 2077", AuvionyxSampleData.tracks.first().title)
  }

  @Test
  fun googleMapsNotificationParser_extractsTurnDistanceInstructionAndEta() {
    val parsed = LiveGoogleMapsSyncBridge.parseGoogleMapsNotificationText(
      title = "350 m",
      text = "Turn right onto MG Road",
      subText = "14 min (6.8 km) · 11:24"
    )
    assertNotNull(parsed)
    assertEquals("350 m", parsed?.nextTurnDistanceLabel)
    assertEquals("Turn right onto MG Road", parsed?.turnInstruction)
    assertEquals("MG Road", parsed?.headline)
    assertEquals(14, parsed?.remainingMinutes)
    assertEquals(6.8f, parsed?.remainingDistanceKm ?: 0f, 0.01f)
    assertEquals("11:24", parsed?.etaTimeLabel)

    val multiHour = LiveGoogleMapsSyncBridge.parseGoogleMapsNotificationText(
      title = "2.4 km",
      text = "Continue onto NH 44",
      subText = "1 hr 18 min (62.5 km) · 13:45 ETA"
    )
    assertNotNull(multiHour)
    assertEquals(78, multiHour?.remainingMinutes)
    assertEquals(62.5f, multiHour?.remainingDistanceKm ?: 0f, 0.01f)
    assertEquals("13:45", multiHour?.etaTimeLabel)
  }

  @Test
  fun externalNavigationParser_supportsWazeAndMapplsWithoutInventingMissingFields() {
    val waze = LiveGoogleMapsSyncBridge.parseNavigationNotification(
      packageName = "com.waze",
      title = "200 m",
      text = "Turn left onto Outer Ring Road",
      subText = "18 min · 7.4 km · 6:42 PM"
    )
    assertNotNull(waze)
    assertEquals("Waze", waze?.providerLabel)
    assertEquals("200 m", waze?.nextTurnDistanceLabel)
    assertEquals("Turn left onto Outer Ring Road", waze?.turnInstruction)
    assertEquals("Outer Ring Road", waze?.headline)
    assertEquals(18, waze?.remainingMinutes)
    assertEquals(7.4f, waze?.remainingDistanceKm ?: 0f, 0.01f)
    assertEquals("6:42 PM", waze?.etaTimeLabel)

    val mappls = LiveGoogleMapsSyncBridge.parseNavigationNotification(
      packageName = "com.mmi.maps",
      title = "In 500 m",
      text = "Keep right",
      subText = "Continue on NH 48"
    )
    assertNotNull(mappls)
    assertEquals("Mappls", mappls?.providerLabel)
    assertNull("Do not invent a trip duration", mappls?.remainingMinutes)
    assertNull("Do not treat the next-turn distance as trip distance", mappls?.remainingDistanceKm)

    assertNull(
      LiveGoogleMapsSyncBridge.parseNavigationNotification(
        packageName = "com.example.unknown",
        title = "Turn left",
        text = "In 200 m"
      )
    )
  }

  @Test
  fun serviceStatesAndVoiceStates_coverAllPhase013Contracts() {
    assertEquals(
      setOf(
        VoiceVisualState.IDLE,
        VoiceVisualState.LISTENING,
        VoiceVisualState.PROCESSING,
        VoiceVisualState.COMMAND_RECOGNISED,
        VoiceVisualState.CONFIRMATION_REQUIRED,
        VoiceVisualState.ERROR
      ),
      VoiceVisualState.entries.toSet()
    )

    assertEquals(
      setOf(
        ServiceAvailabilityState.LIVE,
        ServiceAvailabilityState.DEMO,
        ServiceAvailabilityState.OFFLINE,
        ServiceAvailabilityState.PERMISSION_DENIED,
        ServiceAvailabilityState.API_UNAVAILABLE
      ),
      ServiceAvailabilityState.entries.toSet()
    )
  }

  @Test
  fun voiceCommandParser_handlesHaridwarAndAllCategoriesAndConfirmation() {
    val haridwar = VoiceCommandParser.parseDeterministic("Navigate to Haridwar")
    assertEquals(VoiceCommandCategory.NAVIGATION, haridwar?.category)
    assertEquals(VoiceCommandAction.NAVIGATE_TO, haridwar?.action)
    assertEquals("haridwar", haridwar?.queryOrTarget)

    val fuel = VoiceCommandParser.parseDeterministic("Search petrol pumps")
    assertEquals(VoiceCommandCategory.NAVIGATION, fuel?.category)
    assertEquals(VoiceCommandAction.SEARCH_RESTAURANTS_OR_FUEL, fuel?.action)

    val media = VoiceCommandParser.parseDeterministic("Open Spotify")
    assertEquals(VoiceCommandCategory.MEDIA, media?.category)
    assertEquals(MediaAppSource.SPOTIFY, media?.mediaSource)

    val vol = VoiceCommandParser.parseDeterministic("Set volume to 80")
    assertEquals(VoiceCommandAction.CHANGE_VOLUME, vol?.action)
    assertEquals(80, vol?.targetVolumePercent)

    val vehicle = VoiceCommandParser.parseDeterministic("Open HUD")
    assertEquals(VoiceCommandCategory.VEHICLE, vehicle?.category)
    assertEquals(VoiceCommandAction.OPEN_HUD, vehicle?.action)

    val ui = VoiceCommandParser.parseDeterministic("Change dashboard layout 60 40")
    assertEquals(VoiceCommandCategory.INTERFACE, ui?.category)
    assertEquals(DashboardSplitRatio.RATIO_60_40, ui?.targetRatio)

    // Confirmation disambiguation replies
    assertEquals(0, VoiceCommandParser.parseConfirmationReply("confirm first", 3))
    assertEquals(1, VoiceCommandParser.parseConfirmationReply("second", 3))
    assertEquals(-1, VoiceCommandParser.parseConfirmationReply("cancel", 3))
    assertNull(VoiceCommandParser.parseConfirmationReply("fifth", 2))

    // Complex open-ended request returns null so optional Gemini fallback handles it cleanly
    assertNull(VoiceCommandParser.parseDeterministic("I am tired of rain take me somewhere quiet for coffee"))
  }

  @Test
  fun mediaPlayback_nextAndPreviousNeverMutateSelectedSource() {
    val initial = MediaPlayerState(
      selectedSource = MediaAppSource.YOUTUBE_MUSIC,
      activeSessionSource = MediaAppSource.LASTWAVE,
      currentTrackIndex = 0,
      positionSec = 2
    )
    assertEquals(0xFF00D9F5.toInt(), initial.externalAccentArgb)
    val afterNext = MediaPlaybackReducer.nextTrack(initial)
    assertEquals(1, afterNext.currentTrackIndex)
    assertEquals(MediaAppSource.YOUTUBE_MUSIC, afterNext.selectedSource)

    val afterPrev = MediaPlaybackReducer.previousTrack(afterNext)
    assertEquals(0, afterPrev.currentTrackIndex)
    assertEquals(MediaAppSource.YOUTUBE_MUSIC, afterPrev.selectedSource)

    val afterSourceSwitch = MediaPlaybackReducer.selectSource(afterPrev, MediaAppSource.SPOTIFY)
    assertEquals(MediaAppSource.SPOTIFY, afterSourceSwitch.selectedSource)
    assertEquals(0, afterSourceSwitch.currentTrackIndex)
  }
}
