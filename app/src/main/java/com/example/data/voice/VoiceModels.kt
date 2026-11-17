package com.example.data.voice

import com.example.data.model.DashboardSplitRatio
import com.example.data.model.DestinationCandidate
import com.example.data.model.MediaAppSource
import com.example.data.model.ServiceAvailabilityState

/**
 * Six explicit visual and operational states for Auvionyx Voice.
 */
enum class VoiceVisualState(val label: String) {
    IDLE("Idle"),
    LISTENING("Listening"),
    PROCESSING("Processing"),
    COMMAND_RECOGNISED("Command recognised"),
    CONFIRMATION_REQUIRED("Confirmation required"),
    ERROR("Error")
}

enum class VoiceCommandCategory(val label: String) {
    NAVIGATION("Navigation"),
    MEDIA("Media"),
    VEHICLE("Vehicle"),
    INTERFACE("Interface")
}

enum class VoiceCommandAction {
    NAVIGATE_TO,
    FIND_NEARBY,
    SEARCH_RESTAURANTS_OR_FUEL,
    TRAFFIC_CONDITIONS,
    PLAY_MUSIC,
    PAUSE_MUSIC,
    RESUME_MUSIC,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    CHANGE_VOLUME,
    OPEN_MEDIA_APP,
    PLAY_PLAYLIST,
    OPEN_HUD,
    SHOW_JOURNEY_INFO,
    DISPLAY_VEHICLE_DASHBOARD,
    OPEN_MAPS,
    OPEN_MUSIC,
    CHANGE_DASHBOARD_LAYOUT,
    ACTIVATE_FULLSCREEN
}

data class ParsedVoiceCommand(
    val category: VoiceCommandCategory,
    val action: VoiceCommandAction,
    val queryOrTarget: String = "",
    val mediaSource: MediaAppSource? = null,
    val targetRatio: DashboardSplitRatio? = null,
    val targetVolumePercent: Int? = null,
    val volumeDeltaPercent: Int? = null,
    val playlistHint: String? = null
)

data class VoiceCommandOutcome(
    val category: VoiceCommandCategory,
    val action: VoiceCommandAction,
    val recognizedPhrase: String,
    val confirmationTitle: String,
    val confirmationDetail: String,
    val serviceState: ServiceAvailabilityState = ServiceAvailabilityState.LIVE,
    val usedGeminiInterpreter: Boolean = false,
    val nativeIntegrationNote: String? = null
)

data class PendingDestinationConfirmation(
    val spokenQuery: String,
    val candidates: List<DestinationCandidate>,
    val serviceState: ServiceAvailabilityState
)

data class NativeVoiceBridgeStatus(
    val speechServiceState: ServiceAvailabilityState = ServiceAvailabilityState.API_UNAVAILABLE,
    val speechRecognizerAvailable: Boolean = false,
    val recordAudioGranted: Boolean = false,
    val textToSpeechReady: Boolean = false,
    val mediaSessionState: ServiceAvailabilityState = ServiceAvailabilityState.DEMO,
    val locationServiceState: ServiceAvailabilityState = ServiceAvailabilityState.PERMISSION_DENIED,
    val geocoderAvailable: Boolean = false,
    val networkOnline: Boolean = false,
    val googleMapsConfigured: Boolean = false,
    val wakeWordSupported: Boolean = false
)

data class VoiceUiState(
    val visualState: VoiceVisualState = VoiceVisualState.IDLE,
    val partialTranscript: String = "",
    val rmsNormalized: Float = 0.25f,
    val lastOutcome: VoiceCommandOutcome? = null,
    val pendingConfirmation: PendingDestinationConfirmation? = null,
    val errorTitle: String? = null,
    val errorDetail: String? = null,
    val errorServiceState: ServiceAvailabilityState? = null,
    val needsAudioPermission: Boolean = false,
    val ttsConfirmationsEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val geminiComplexFallbackEnabled: Boolean = true,
    val bridgeStatus: NativeVoiceBridgeStatus = NativeVoiceBridgeStatus()
)
