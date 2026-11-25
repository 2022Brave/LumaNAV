package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaAppSource
import com.example.data.model.MediaTrack
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.AlbumArtworkArt
import com.example.ui.components.GlassCard
import com.example.ui.components.ServiceStateBadge
import com.example.ui.components.formatDuration
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaPlayerState

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
    val duration = track.durationSec.coerceAtLeast(1)

    GlassCard(
        modifier = modifier.fillMaxSize(),
        cornerRadius = 28.dp,
        highlighted = mediaState.isPlaying,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = if (isLandscapeWide) 28.dp else 22.dp,
            vertical = if (isLandscapeWide) 18.dp else 20.dp
        )
    ) {
        if (isLandscapeWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.38f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .background(
                                Brush.radialGradient(
                                    listOf(accent.copy(alpha = 0.18f), Color.Transparent)
                                ),
                                RoundedCornerShape(28.dp)
                            )
                            .padding(10.dp)
                    ) {
                        MusicArtwork(track, mediaState, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (mediaState.isExternalSessionActive) {
                            mediaState.externalAppLabel ?: "Connected media session"
                        } else track.source.label,
                        color = accent,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ServiceStateBadge(
                        state = mediaState.bridgeStatus.serviceState,
                        labelPrefix = "MEDIA"
                    )
                }

                Column(
                    modifier = Modifier.weight(0.62f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp
                            ),
                            color = GlassWhite,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().basicMarquee()
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.album.isNotBlank()) {
                            Text(track.album, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        }
                    }
                    MusicProgress(mediaState, duration, accent, onSeek)
                    MusicTransport(
                        mediaState = mediaState,
                        accent = accent,
                        onToggleShuffle = onToggleShuffle,
                        onPrevTrack = onPrevTrack,
                        onPlayPause = onPlayPause,
                        onNextTrack = onNextTrack,
                        onToggleRepeat = onToggleRepeat,
                        large = true
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (mediaState.isExternalSessionActive) mediaState.externalAppLabel ?: "Now Playing" else "LUMANAV PLAYER",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (mediaState.isExternalSessionActive) "Connected media session" else "Play music in any supported app",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    ServiceStateBadge(state = mediaState.bridgeStatus.serviceState, labelPrefix = "MEDIA")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MusicArtwork(track, mediaState, Modifier.size(220.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleLarge, color = GlassWhite, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (track.album.isNotBlank()) Text(track.album, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
                    }
                }
                MusicProgress(mediaState, duration, accent, onSeek)
                MusicTransport(
                    mediaState = mediaState,
                    accent = accent,
                    onToggleShuffle = onToggleShuffle,
                    onPrevTrack = onPrevTrack,
                    onPlayPause = onPlayPause,
                    onNextTrack = onNextTrack,
                    onToggleRepeat = onToggleRepeat,
                    large = false
                )
            }
        }
    }
}

@Composable
private fun MusicArtwork(track: MediaTrack, state: MediaPlayerState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF090E16))
            .then(
                Modifier.background(
                    Brush.linearGradient(
                        listOf(GlassWhite.copy(alpha = 0.06f), ElectricCyan.copy(alpha = 0.04f))
                    )
                )
            )
    ) {
        val artwork = state.externalArtwork
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
                cornerRadius = 20.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MusicProgress(
    state: MediaPlayerState,
    duration: Int,
    accent: Color,
    onSeek: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Slider(
            value = state.effectivePositionSec.toFloat().coerceIn(0f, duration.toFloat()),
            onValueChange = { onSeek(it.toInt()) },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = GlassWhite,
                activeTrackColor = accent,
                inactiveTrackColor = GlassWhite.copy(alpha = 0.18f)
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.effectivePositionSec), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun MusicTransport(
    mediaState: MediaPlayerState,
    accent: Color,
    onToggleShuffle: () -> Unit,
    onPrevTrack: () -> Unit,
    onPlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onToggleRepeat: () -> Unit,
    large: Boolean
) {
    val buttonSize = if (large) 60.dp else 52.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Outlined.Shuffle, "Shuffle", tint = if (mediaState.isShuffle) accent else TextSecondary, modifier = Modifier.size(large.let { if (it) 26.dp else 22.dp }))
        }
        IconButton(onClick = onPrevTrack, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = GlassWhite, modifier = Modifier.size(if (large) 34.dp else 28.dp))
        }
        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = accent.copy(alpha = 0.20f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.82f))
        ) {
            Box(
                modifier = Modifier.size(if (large) 64.dp else 58.dp)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.34f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                    tint = GlassWhite,
                    modifier = Modifier.size(if (large) 36.dp else 32.dp)
                )
            }
        }
        IconButton(onClick = onNextTrack, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Rounded.SkipNext, "Next", tint = GlassWhite, modifier = Modifier.size(if (large) 34.dp else 28.dp))
        }
        IconButton(onClick = onToggleRepeat, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Outlined.Repeat, "Repeat", tint = if (mediaState.isRepeat) accent else TextSecondary, modifier = Modifier.size(large.let { if (it) 26.dp else 22.dp }))
        }
    }
}
