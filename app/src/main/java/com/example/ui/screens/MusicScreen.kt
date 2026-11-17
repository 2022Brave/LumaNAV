package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaAppSource
import com.example.data.preferences.AuvionyxSettingsState
import com.example.ui.components.AlbumArtworkArt
import com.example.ui.components.GlassCard
import com.example.ui.components.LiquidGlassLens
import com.example.ui.components.MediaControlCluster
import com.example.ui.components.NeonWaveVisualizer
import com.example.ui.components.ServiceStateBadge
import com.example.ui.components.formatDuration
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaPlayerState

/**
 * Halo HUD Minimalist Media Hub:
 * Clean source switcher bar, spacious artwork & playback cluster, and decoupled queue list.
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
    val current = mediaState.currentTrack

    val nowPlayingPanel: @Composable (Modifier) -> Unit = { panelMod ->
        GlassCard(
            modifier = panelMod,
            cornerRadius = 24.dp,
            highlighted = mediaState.isPlaying,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Clean Single-Row Source Switcher & Adaptive Toggle
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ServiceStateBadge(
                            state = mediaState.bridgeStatus.serviceState,
                            labelPrefix = "MediaSession"
                        )
                        Surface(
                            color = if (settings.useAdaptiveAlbumColors) DeepBlue.copy(alpha = 0.45f) else Color(0xFF0B0F15),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, if (settings.useAdaptiveAlbumColors) accent else GlassWhite.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .testTag("music_adaptive_color_toggle")
                                .clickable { onToggleAdaptiveColors(!settings.useAdaptiveAlbumColors) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Toggle adaptive album colors",
                                    tint = accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (settings.useAdaptiveAlbumColors) "Adaptive" else "Brand Cyan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassWhite
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0B0F15))
                            .border(0.8.dp, GlassWhite.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MediaAppSource.entries.forEach { source ->
                            LiquidGlassLens(
                                selected = mediaState.selectedSource == source,
                                onClick = { onSelectSource(source) },
                                cornerRadius = 12.dp,
                                testTag = "media_source_${source.name}",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Text(
                                    text = source.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (mediaState.selectedSource == source) GlassWhite else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Clean Artwork + Track Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscapeWide) 150.dp else 104.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0B0F15))
                            .border(0.8.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                    ) {
                        AlbumArtworkArt(
                            artVariant = current.artVariant,
                            accentColor = current.adaptiveColor,
                            cornerRadius = 18.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaState.bridgeStatus.activeControllerPackage ?: mediaState.activeSessionSource.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = GlassWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${current.artist} · ${current.album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        NeonWaveVisualizer(
                            isActive = mediaState.isPlaying,
                            accentOverride = if (settings.useAdaptiveAlbumColors) current.adaptiveColor else accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        )
                    }
                }

                // Scrubber + Playback Controls
                Column {
                    Slider(
                        value = mediaState.positionSec.toFloat(),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..current.durationSec.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = GlassWhite,
                            activeTrackColor = accent,
                            inactiveTrackColor = GlassWhite.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(mediaState.positionSec), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(formatDuration(current.durationSec), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Spacer(Modifier.height(6.dp))
                    MediaControlCluster(
                        isPlaying = mediaState.isPlaying,
                        isShuffle = mediaState.isShuffle,
                        isRepeat = mediaState.isRepeat,
                        isFavorite = current.id in mediaState.favoriteIds,
                        onToggleShuffle = onToggleShuffle,
                        onPrevious = onPrevTrack,
                        onPlayPause = onPlayPause,
                        onNext = onNextTrack,
                        onToggleRepeatOrFav = onToggleRepeat,
                        showRepeatInsteadOfFav = true
                    )
                }
            }
        }
    }

    val queuePanel: @Composable (Modifier) -> Unit = { panelMod ->
        GlassCard(
            modifier = panelMod,
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlassWhite
                    )
                    ServiceStateBadge(state = mediaState.bridgeStatus.serviceState)
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(mediaState.tracks, key = { _, item -> item.id }) { idx, track ->
                        val active = idx == mediaState.currentTrackIndex
                        Surface(
                            color = if (active) DeepBlue.copy(alpha = 0.28f) else Color(0xFF0A0E14),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                0.8.dp,
                                if (active) accent else GlassWhite.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("queue_track_${track.id}")
                                .clickable { onSelectTrack(idx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtworkArt(
                                    artVariant = track.artVariant,
                                    accentColor = track.adaptiveColor,
                                    cornerRadius = 10.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GlassWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} · ${track.source.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Outlined.GraphicEq,
                                        contentDescription = "Now playing",
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = formatDuration(track.durationSec),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
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
            nowPlayingPanel(Modifier.weight(0.56f).fillMaxHeight())
            queuePanel(Modifier.weight(0.44f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            nowPlayingPanel(Modifier.fillMaxWidth().weight(0.60f))
            queuePanel(Modifier.fillMaxWidth().weight(0.40f))
        }
    }
}
