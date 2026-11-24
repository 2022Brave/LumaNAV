package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaAppSource
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.AlbumArtworkArt
import com.example.ui.components.GlassCard
import com.example.ui.components.ServiceStateBadge
import com.example.ui.components.formatDuration
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaPlayerState

/**
 * Minimal universal media surface. Playback is delegated to the active Android MediaSession;
 * Auvionyx only mirrors metadata/artwork and dispatches transport controls.
 */
@Composable
fun MusicScreen(
    isLandscapeWide: Boolean,
    settings: AuvionyxSettingsState,
    mediaState: MediaPlayerState,
    onSelectSource: (MediaAppSource) -> Unit,
    onSelectTrack: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleAdaptiveColors: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val track = mediaState.currentTrack
    val artwork = mediaState.externalArtwork
    val isExternal = mediaState.isExternalSessionActive
    val duration = track.durationSec.coerceAtLeast(1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 28.dp,
            highlighted = mediaState.isPlaying,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = if (isLandscapeWide) 34.dp else 22.dp,
                vertical = if (isLandscapeWide) 26.dp else 20.dp
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (isLandscapeWide) 22.dp else 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isExternal) mediaState.externalAppLabel ?: "Now Playing" else "LUMANAV PLAYER",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isExternal) "Connected media session" else "Play music in any supported app",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    ServiceStateBadge(
                        state = mediaState.bridgeStatus.serviceState,
                        labelPrefix = "MEDIA"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscapeWide) 260.dp else 220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF090E16))
                            .border(
                                BorderStroke(1.dp, Brush.linearGradient(
                                    listOf(GlassWhite.copy(alpha = 0.52f), accent.copy(alpha = 0.62f), Color.Transparent)
                                )),
                                RoundedCornerShape(20.dp)
                            )
                    ) {
                        if (artwork != null) {
                            Image(
                                bitmap = artwork.asImageBitmap(),
                                contentDescription = "Album artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AlbumArtworkArt(
                                artVariant = track.artVariant,
                                accentColor = track.adaptiveColor,
                                cornerRadius = 18.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = GlassWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.album.isNotBlank()) {
                            Text(
                                text = track.album,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Slider(
                        value = mediaState.effectivePositionSec.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = GlassWhite,
                            activeTrackColor = accent,
                            inactiveTrackColor = GlassWhite.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(mediaState.effectivePositionSec), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(Icons.Outlined.Shuffle, "Shuffle", tint = if (mediaState.isShuffle) accent else TextSecondary)
                    }
                    IconButton(onClick = onPrevTrack) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", tint = GlassWhite, modifier = Modifier.size(30.dp))
                    }
                    Surface(
                        onClick = onPlayPause,
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.72f))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), Color.Transparent))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                tint = GlassWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNextTrack) {
                        Icon(Icons.Rounded.SkipNext, "Next", tint = GlassWhite, modifier = Modifier.size(30.dp))
                    }
                    IconButton(onClick = onToggleRepeat) {
                        Icon(Icons.Outlined.Repeat, "Repeat", tint = if (mediaState.isRepeat) accent else TextSecondary)
                    }
                }
            }
        }
    }
}
