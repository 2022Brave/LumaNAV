package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DestinationCandidate
import com.example.data.voice.VoiceUiState
import com.example.data.voice.VoiceVisualState
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassSurfaceHigh
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PulseCongestion
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * Floating Liquid Glass Microphone & Expanded Hands-Free Voice Overlay.
 * Supports all 6 explicit visual states:
 * IDLE, LISTENING, PROCESSING, COMMAND_RECOGNISED, CONFIRMATION_REQUIRED, ERROR.
 */
@Composable
fun FloatingVoiceMicOverlay(
    voiceState: VoiceUiState,
    onActivateMic: () -> Unit,
    onStopMic: () -> Unit,
    onDismiss: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onConfirmCandidate: (DestinationCandidate) -> Unit,
    onOpenVoiceConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion

    AnimatedContent(
        targetState = voiceState.visualState == VoiceVisualState.IDLE,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing)) +
                    scaleIn(initialScale = 0.88f, animationSpec = tween(LivingFlow.StandardDurationMs, easing = LivingFlow.LaminarEasing))) togetherWith
                    (fadeOut(tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)) +
                        scaleOut(targetScale = 0.90f, animationSpec = tween(LivingFlow.CompactDurationMs, easing = LivingFlow.LaminarEasing)))
            }
        },
        label = "floatingVoiceOverlay",
        modifier = modifier
    ) { isIdle ->
        if (isIdle) {
            FloatingIdleMicOrb(
                onClick = onActivateMic,
                testTag = "floating_voice_mic_idle"
            )
        } else {
            val isHighlighted = voiceState.visualState == VoiceVisualState.LISTENING ||
                voiceState.visualState == VoiceVisualState.COMMAND_RECOGNISED ||
                voiceState.visualState == VoiceVisualState.CONFIRMATION_REQUIRED

            GlassCard(
                modifier = Modifier
                    .widthIn(max = 450.dp)
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = if (reducedMotion) tween(0) else LivingFlow.LaminarSizeSpec
                    )
                    .testTag("floating_voice_expanded_${voiceState.visualState.name}"),
                cornerRadius = 26.dp,
                highlighted = isHighlighted,
                accentOverride = if (voiceState.visualState == VoiceVisualState.ERROR) {
                    PulseCongestion
                } else {
                    ElectricCyan
                },
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                contentDescription = null,
                                tint = if (voiceState.visualState == VoiceVisualState.ERROR) {
                                    PulseCongestion
                                } else {
                                    ElectricCyan
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LumaNav Voice · ${voiceState.visualState.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = GlassWhite
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onOpenVoiceConsole,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .photonLuminousTouch(pressScale = 0.90f)
                                    .testTag("voice_overlay_expand_console")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Open LumaNav Voice console",
                                    tint = IceBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .photonLuminousTouch(pressScale = 0.90f)
                                    .testTag("voice_overlay_dismiss")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Dismiss voice panel",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    when (voiceState.visualState) {
                        VoiceVisualState.LISTENING -> {
                            val pulseScale = if (!reducedMotion) {
                                val transition = rememberInfiniteTransition(label = "voicePulse")
                                val s by transition.animateFloat(
                                    initialValue = 0.94f,
                                    targetValue = 1.06f,
                                    animationSpec = infiniteRepeatable(
                                        tween(850, easing = FastOutSlowInEasing),
                                        RepeatMode.Reverse
                                    ),
                                    label = "micPulseScale"
                                )
                                s
                            } else 1f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LiquidFloatingButton(
                                    icon = Icons.Outlined.Stop,
                                    contentDescription = "Stop listening",
                                    onClick = onStopMic,
                                    size = (52 * pulseScale).dp,
                                    isPrimary = true,
                                    testTag = "voice_overlay_stop"
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    NeonWaveVisualizer(
                                        isActive = true,
                                        accentOverride = ElectricCyan,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    )
                                    Text(
                                        text = voiceState.partialTranscript.ifBlank { "Listening for command…" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = IceBlue,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        VoiceVisualState.PROCESSING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = ElectricCyan,
                                    trackColor = GlassSurfaceHigh,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(34.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Processing voice request…",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GlassWhite
                                    )
                                    if (voiceState.partialTranscript.isNotBlank()) {
                                        Text(
                                            text = "\"${voiceState.partialTranscript}\"",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceBlue,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        VoiceVisualState.COMMAND_RECOGNISED -> {
                            val outcome = voiceState.lastOutcome
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Command recognised",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = outcome?.confirmationTitle ?: "Command recognised",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GlassWhite
                                    )
                                    Text(
                                        text = outcome?.confirmationDetail.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                if (outcome != null) {
                                    ServiceStateBadge(state = outcome.serviceState)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiquidGlassLens(
                                    selected = true,
                                    onClick = onActivateMic,
                                    cornerRadius = 14.dp,
                                    testTag = "voice_overlay_retry_after_recognised",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Mic,
                                            contentDescription = null,
                                            tint = GlassWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Speak Command", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                                    }
                                }
                            }
                        }

                        VoiceVisualState.CONFIRMATION_REQUIRED -> {
                            val pending = voiceState.pendingConfirmation
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
                                    Icon(
                                        imageVector = Icons.Outlined.HelpOutline,
                                        contentDescription = "Confirmation required",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Confirm \"${pending?.spokenQuery.orEmpty()}\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GlassWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (pending != null) {
                                    ServiceStateBadge(state = pending.serviceState)
                                }
                            }

                            pending?.candidates?.take(3)?.forEachIndexed { idx, candidate ->
                                LiquidGlassLens(
                                    selected = idx == 0,
                                    onClick = { onConfirmCandidate(candidate) },
                                    cornerRadius = 14.dp,
                                    testTag = "voice_confirm_candidate_$idx",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Place,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${candidate.name}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = GlassWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = candidate.addressSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "Confirm",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiquidGlassLens(
                                    selected = true,
                                    onClick = onActivateMic,
                                    cornerRadius = 14.dp,
                                    testTag = "voice_confirm_speak_reply",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Say 'First' / 'Second'", style = MaterialTheme.typography.labelMedium, color = GlassWhite)
                                }
                                LiquidGlassLens(
                                    selected = false,
                                    onClick = onDismiss,
                                    cornerRadius = 14.dp,
                                    testTag = "voice_confirm_cancel",
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .height(48.dp)
                                ) {
                                    Text("Cancel", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                }
                            }
                        }

                        VoiceVisualState.ERROR -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = "Voice error",
                                    tint = PulseCongestion,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = voiceState.errorTitle ?: "Voice Service Unavailable",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GlassWhite,
                                            modifier = Modifier.weight(1f)
                                        )
                                        voiceState.errorServiceState?.let { state ->
                                            ServiceStateBadge(state = state)
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = voiceState.errorDetail ?: "Unable to recognize speech on this device.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (voiceState.needsAudioPermission) {
                                    LiquidGlassLens(
                                        selected = true,
                                        onClick = onRequestAudioPermission,
                                        cornerRadius = 14.dp,
                                        testTag = "voice_overlay_grant_mic",
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("Grant Mic Permission", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                                    }
                                }
                                LiquidGlassLens(
                                    selected = !voiceState.needsAudioPermission,
                                    onClick = onOpenVoiceConsole,
                                    cornerRadius = 14.dp,
                                    testTag = "voice_overlay_open_triggers",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("Voice Console", style = MaterialTheme.typography.labelLarge, color = GlassWhite)
                                }
                            }
                        }

                        VoiceVisualState.IDLE -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingIdleMicOrb(
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .testTag(testTag)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricCyan.copy(alpha = 0.28f), Color.Transparent),
                        radius = size.maxDimension * 0.85f
                    )
                )
            }
            .size(54.dp)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(GlassWhite.copy(alpha = 0.65f), ElectricCyan, DeepBlue)
                ),
                shape = CircleShape
            )
            .photonLuminousTouch(pressScale = 0.92f, onClick = onClick),
        color = ObsidianBlack,
        shape = CircleShape,
        border = BorderStroke(0.8.dp, ElectricCyan.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        GlassWhite.copy(alpha = 0.14f),
                        ElectricCyan.copy(alpha = 0.20f),
                        DeepBlue.copy(alpha = 0.58f)
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Activate LumaNav Voice",
                tint = GlassWhite,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
