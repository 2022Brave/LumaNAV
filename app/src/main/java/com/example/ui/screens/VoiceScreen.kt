package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DestinationCandidate
import com.example.data.model.ServiceAvailabilityState
import com.example.data.voice.VoiceCommandCategory
import com.example.data.voice.VoiceUiState
import com.example.data.voice.VoiceVisualState
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.LiquidGlassToggle
import com.example.ui.components.NeonWaveVisualizer
import com.example.ui.components.ServiceStateBadge
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * Halo HUD Auvionyx Voice Screen:
 * Minimalist voice orb left + tabbed single-category voice triggers & quiet service telemetry right.
 * Zero keyboard/text inputs, zero chat history, strict native SpeechRecognizer/Geocoder honesty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceScreen(
    isLandscapeWide: Boolean,
    voiceState: VoiceUiState,
    onPermissionUpdated: () -> Unit,
    onActivateMic: () -> Unit,
    onStopMic: () -> Unit,
    onResetToIdle: () -> Unit,
    onConfirmCandidate: (DestinationCandidate) -> Unit,
    onExecuteVoiceCommand: (String) -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleGeminiInterpreter: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(VoiceCommandCategory.NAVIGATION) }
    var showVoiceSettings by remember { mutableStateOf(false) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionUpdated()
        if (granted) {
            onActivateMic()
        }
    }

    val commandsByCategory = remember {
        mapOf(
            VoiceCommandCategory.NAVIGATION to listOf(
                "Navigate to Haridwar" to "Navigate to Haridwar",
                "Navigate to Rajpur Road" to "Navigate to Rajpur Road",
                "Search Petrol Pumps" to "Search petrol pumps",
                "Search Restaurants" to "Search restaurants",
                "Traffic Conditions" to "How is traffic"
            ),
            VoiceCommandCategory.MEDIA to listOf(
                "Play Music" to "Play music",
                "Pause Music" to "Pause music",
                "Next Track" to "Next track",
                "Previous Track" to "Previous track",
                "Volume 80%" to "Set volume to 80",
                "Open Spotify" to "Open Spotify",
                "Play Lamentis" to "Play playlist Lamentis 2077"
            ),
            VoiceCommandCategory.VEHICLE to listOf(
                "Open HUD" to "Open HUD",
                "Journey Info" to "Show journey info",
                "Vehicle Dashboard" to "Display vehicle dashboard"
            ),
            VoiceCommandCategory.INTERFACE to listOf(
                "Open Maps" to "Open Maps",
                "Open Music" to "Open Music",
                "Layout 70:30" to "Change dashboard layout 70 30",
                "Layout 60:40" to "Change dashboard layout 60 40",
                "Fullscreen Maps" to "Activate fullscreen maps"
            )
        )
    }

    val isHighlighted = voiceState.visualState == VoiceVisualState.LISTENING ||
        voiceState.visualState == VoiceVisualState.COMMAND_RECOGNISED ||
        voiceState.visualState == VoiceVisualState.CONFIRMATION_REQUIRED

    val voiceOrbPanel: @Composable (Modifier) -> Unit = { panelMod ->
        GlassCard(
            modifier = panelMod,
            cornerRadius = 24.dp,
            highlighted = isHighlighted,
            accentOverride = if (voiceState.visualState == VoiceVisualState.ERROR) PulseCongestion else ElectricCyan,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
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
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = "Auvionyx Voice",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Auvionyx Voice", style = MaterialTheme.typography.titleLarge, color = GlassWhite)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = voiceState.visualState.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (voiceState.visualState == VoiceVisualState.ERROR) PulseCongestion else ElectricCyan
                        )
                        Surface(
                            color = if (showVoiceSettings) DeepBlue.copy(alpha = 0.45f) else Color(0xFF0B0F15),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, GlassWhite.copy(alpha = 0.14f)),
                            modifier = Modifier.clickable { showVoiceSettings = !showVoiceSettings }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Voice options",
                                tint = IceBlue,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }

                // Clean Centered Halo Voice Orb & Waveform
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NeonWaveVisualizer(
                        isActive = voiceState.visualState == VoiceVisualState.LISTENING,
                        accentOverride = if (voiceState.visualState == VoiceVisualState.ERROR) PulseCongestion else ElectricCyan,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(52.dp)
                    )

                    val isListening = voiceState.visualState == VoiceVisualState.LISTENING
                    val isProcessing = voiceState.visualState == VoiceVisualState.PROCESSING
                    Box(
                        modifier = Modifier
                            .testTag("voice_screen_mic_orb")
                            .size(90.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            (if (voiceState.visualState == VoiceVisualState.ERROR) PulseCongestion else ElectricCyan)
                                                .copy(alpha = if (isListening) 0.48f else 0.24f),
                                            Color.Transparent
                                        ),
                                        radius = size.maxDimension * 0.85f
                                    )
                                )
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        GlassWhite.copy(alpha = 0.16f),
                                        ElectricCyan.copy(alpha = if (isListening) 0.38f else 0.22f),
                                        DeepBlue.copy(alpha = 0.68f)
                                    )
                                )
                            )
                            .border(
                                width = 1.6.dp,
                                brush = Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.7f), ElectricCyan, DeepBlue)),
                                shape = CircleShape
                            )
                            .clickable {
                                when {
                                    isListening -> onStopMic()
                                    !voiceState.bridgeStatus.recordAudioGranted ->
                                        audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    else -> onActivateMic()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = GlassWhite,
                                trackColor = GlassSurfaceHigh,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isListening) Icons.Outlined.Stop else Icons.Outlined.Mic,
                                contentDescription = stringResource(id = R.string.cd_voice_assistant),
                                tint = GlassWhite,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    VoiceStateStatusBanner(
                        voiceState = voiceState,
                        onGrantMicPermission = { audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onConfirmCandidate = onConfirmCandidate,
                        onResetToIdle = onResetToIdle
                    )
                }

                AnimatedVisibility(visible = showVoiceSettings) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        VoiceOptionRow("TextToSpeech Confirmations", voiceState.ttsConfirmationsEnabled, onToggleTts)
                        VoiceOptionRow("Gemini Complex Intent Fallback", voiceState.geminiComplexFallbackEnabled, onToggleGeminiInterpreter)
                        VoiceOptionRow("Wake-Word (OS Voice Service)", voiceState.wakeWordEnabled, onToggleWakeWord)
                    }
                }
            }
        }
    }

    val commandConsolePanel: @Composable (Modifier) -> Unit = { consoleMod ->
        GlassCard(
            modifier = consoleMod,
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(18.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Commands",
                            style = MaterialTheme.typography.titleMedium,
                            color = GlassWhite
                        )
                        ServiceStateBadge(state = ServiceAvailabilityState.DEMO)
                    }
                }

                // Clean Category Filter Tabs (only 1 category visible at a time -> Halo minimalism)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0B0F15))
                            .border(0.8.dp, GlassWhite.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        VoiceCommandCategory.entries.forEach { category ->
                            val selected = selectedCategory == category
                            LiquidGlassLens(
                                selected = selected,
                                onClick = { selectedCategory = category },
                                cornerRadius = 12.dp,
                                testTag = "voice_cat_${category.name}",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) GlassWhite else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Triggers for Selected Category Only
                item {
                    val activeCommands = commandsByCategory[selectedCategory].orEmpty()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activeCommands.forEach { (label, utterance) ->
                            LiquidGlassLens(
                                selected = voiceState.lastOutcome?.recognizedPhrase.equals(utterance, ignoreCase = true),
                                onClick = { onExecuteVoiceCommand(utterance) },
                                cornerRadius = 14.dp,
                                testTag = "voice_cmd_${utterance.lowercase().replace(' ', '_')}",
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GlassWhite,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }

                // Quiet Halo Service Telemetry Readouts
                item {
                    val b = voiceState.bridgeStatus
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0A0E14))
                            .border(0.8.dp, GlassWhite.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Service Availability",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextMuted
                        )
                        NativeServiceStateRow("SpeechRecognizer", b.speechServiceState)
                        NativeServiceStateRow(
                            "Microphone Permission",
                            if (b.recordAudioGranted) ServiceAvailabilityState.LIVE else ServiceAvailabilityState.PERMISSION_DENIED
                        )
                        NativeServiceStateRow(
                            "TextToSpeech",
                            if (b.textToSpeechReady) ServiceAvailabilityState.LIVE else ServiceAvailabilityState.API_UNAVAILABLE
                        )
                        NativeServiceStateRow(
                            "Geocoder",
                            when {
                                !b.networkOnline -> ServiceAvailabilityState.OFFLINE
                                b.geocoderAvailable -> ServiceAvailabilityState.LIVE
                                else -> ServiceAvailabilityState.API_UNAVAILABLE
                            }
                        )
                        NativeServiceStateRow("GPS Location", b.locationServiceState)
                        NativeServiceStateRow("MediaSession", b.mediaSessionState)
                    }
                }
            }
        }
    }

    if (isLandscapeWide) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            voiceOrbPanel(Modifier.weight(0.45f).fillMaxHeight())
            commandConsolePanel(Modifier.weight(0.55f).fillMaxHeight())
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                voiceOrbPanel(Modifier.fillMaxWidth().height(400.dp))
            }
            item {
                commandConsolePanel(Modifier.fillMaxWidth().height(460.dp))
            }
        }
    }
}

@Composable
private fun VoiceStateStatusBanner(
    voiceState: VoiceUiState,
    onGrantMicPermission: () -> Unit,
    onConfirmCandidate: (DestinationCandidate) -> Unit,
    onResetToIdle: () -> Unit
) {
    when (voiceState.visualState) {
        VoiceVisualState.IDLE -> {
            Text(
                text = "Tap the mic to speak",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        VoiceVisualState.LISTENING -> {
            Text(
                text = voiceState.partialTranscript.ifBlank { "Listening…" },
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan
            )
        }

        VoiceVisualState.PROCESSING -> {
            Text(
                text = "Processing \"${voiceState.partialTranscript}\"…",
                style = MaterialTheme.typography.titleMedium,
                color = IceBlue
            )
        }

        VoiceVisualState.COMMAND_RECOGNISED -> {
            val outcome = voiceState.lastOutcome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricCyan.copy(alpha = 0.08f))
                    .border(0.8.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outcome?.confirmationTitle ?: "Command recognised",
                        style = MaterialTheme.typography.titleSmall,
                        color = GlassWhite
                    )
                    Text(
                        text = outcome?.confirmationDetail.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                if (outcome != null) {
                    ServiceStateBadge(state = outcome.serviceState)
                }
            }
        }

        VoiceVisualState.CONFIRMATION_REQUIRED -> {
            val pending = voiceState.pendingConfirmation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E1522))
                    .border(0.8.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Confirm \"${pending?.spokenQuery.orEmpty()}\"",
                            style = MaterialTheme.typography.titleSmall,
                            color = GlassWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (pending != null) {
                        ServiceStateBadge(state = pending.serviceState)
                    }
                }
                pending?.candidates?.take(2)?.forEachIndexed { idx, candidate ->
                    LiquidGlassLens(
                        selected = idx == 0,
                        onClick = { onConfirmCandidate(candidate) },
                        cornerRadius = 12.dp,
                        testTag = "voice_screen_confirm_$idx",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${idx + 1}. ${candidate.name} · ${candidate.addressSummary}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlassWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        VoiceVisualState.ERROR -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PulseCongestion.copy(alpha = 0.08f))
                    .border(0.8.dp, PulseCongestion.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = PulseCongestion, modifier = Modifier.size(18.dp))
                        Text(
                            text = voiceState.errorTitle ?: "Speech Unavailable",
                            style = MaterialTheme.typography.titleSmall,
                            color = GlassWhite
                        )
                    }
                    voiceState.errorServiceState?.let { ServiceStateBadge(state = it) }
                }
                Text(
                    text = voiceState.errorDetail.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (voiceState.needsAudioPermission) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, ElectricCyan),
                            modifier = Modifier.clickable(onClick = onGrantMicPermission)
                        ) {
                            Text(
                                text = "Grant Mic",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlassWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        color = ObsidianBlack,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, GlassBorderSubtle),
                        modifier = Modifier.clickable(onClick = onResetToIdle)
                    ) {
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        LiquidGlassToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NativeServiceStateRow(
    name: String,
    state: ServiceAvailabilityState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = GlassWhite)
        ServiceStateBadge(state = state)
    }
}
