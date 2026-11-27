package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.math.sin
import com.example.ui.components.LivingFlow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Bluetooth
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
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MediaPlayerState

@OptIn(ExperimentalFoundationApi::class)
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

    Box(modifier = modifier.fillMaxSize().then(if (isLandscapeWide) Modifier.statusBarsPadding() else Modifier)) {
        if (isLandscapeWide) {
            val liveArtwork = mediaState.externalArtwork
            if (liveArtwork != null) {
                Image(
                    bitmap = liveArtwork.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(34.dp)
                )
            } else if (mediaState.isPlaying) {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(track.adaptiveColor.copy(alpha = 0.42f), Color.Black))))
            } else {
                AlbumArtworkArt(
                    artVariant = track.artVariant,
                    accentColor = track.adaptiveColor,
                    cornerRadius = 0.dp,
                    modifier = Modifier.fillMaxSize().blur(28.dp)
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0xB8000000), Color(0xE6000000), Color(0xF2000000))
                    )
                )
            )
        }
    GlassCard(
        modifier = Modifier.fillMaxSize(),
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
                        MusicArtwork(track, mediaState, Modifier.fillMaxSize(), landscape = true)
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Surface(
                                color = Color(0x553B4652),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.16f))
                            ) {
                                Text(
                                    "AUDIO · SYSTEM",
                                    color = accent,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
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
                    MusicProgress(mediaState, duration, track.adaptiveColor, onSeek, animatedWave = true)
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Full-bleed square artwork, allowed to extend behind the transparent status bar.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.56f)
                ) {
                    val artwork = mediaState.externalArtwork
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
                            cornerRadius = 0.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.52f to Color.Black.copy(alpha = 0.10f),
                                    1f to Color.Black
                                )
                            )
                    )
                    Text(
                        text = if (mediaState.isExternalSessionActive) mediaState.externalAppLabel ?: "NOW PLAYING" else "LUMANAV PLAYER",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassWhite.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 24.dp, top = 12.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.44f)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp
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
                    }

                    MusicProgress(mediaState, duration, accent, onSeek, animatedWave = true)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onPrevTrack,
                            modifier = Modifier.requiredSize(56.dp),
                            shape = CircleShape,
                            color = Color(0x553B4652),
                            border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.16f))
                        ) {
                            Box(Modifier.requiredSize(56.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SkipPrevious, "Previous", tint = GlassWhite, modifier = Modifier.size(28.dp))
                            }
                        }
                        Surface(
                            onClick = onPlayPause,
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = ElectricCyan,
                            contentColor = Color.Black
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (mediaState.isPlaying) "PAUSE" else "PLAY", fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            onClick = onNextTrack,
                            modifier = Modifier.requiredSize(56.dp),
                            shape = CircleShape,
                            color = Color(0x553B4652),
                            border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.16f))
                        ) {
                            Box(Modifier.requiredSize(56.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SkipNext, "Next", tint = GlassWhite, modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onToggleShuffle,
                            shape = RoundedCornerShape(18.dp),
                            color = if (mediaState.isShuffle) ElectricCyan.copy(alpha = 0.20f) else Color(0x553B4652),
                            border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.14f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Shuffle, null, tint = if (mediaState.isShuffle) ElectricCyan else TextSecondary, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Shuffle", color = GlassWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0x553B4652),
                            border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.14f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Bluetooth, null, tint = ElectricCyan, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Audio · System", color = GlassWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Surface(
                            onClick = onToggleRepeat,
                            shape = RoundedCornerShape(18.dp),
                            color = if (mediaState.isRepeat) ElectricCyan.copy(alpha = 0.20f) else Color(0x553B4652),
                            border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.14f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Repeat, null, tint = if (mediaState.isRepeat) ElectricCyan else TextSecondary, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Repeat", color = GlassWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MusicArtwork(track: MediaTrack, state: MediaPlayerState, modifier: Modifier = Modifier, landscape: Boolean = false) {
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
        } else if (landscape && state.isPlaying) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(track.adaptiveColor.copy(alpha = 0.42f), Color(0xFF030609)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = track.adaptiveColor.copy(alpha = 0.7f), modifier = Modifier.size(72.dp))
            }
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
    onSeek: (Int) -> Unit,
    animatedWave: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (animatedWave) {
            WavyMusicProgressBar(state, duration, accent, onSeek)
        } else Slider(
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
private fun WavyMusicProgressBar(
    state: MediaPlayerState,
    duration: Int,
    accent: Color,
    onSeek: (Int) -> Unit
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val targetAmplitude = if (state.isPlaying) 1f else 0.72f
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(480, easing = LivingFlow.LaminarEasing),
        label = "musicWaveAmplitude"
    )
    val transition = rememberInfiniteTransition(label = "musicWavePhase")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "musicWavePhaseValue"
    )
    val amplitude = if (reducedMotion) targetAmplitude else animatedAmplitude
    val phase = if (reducedMotion) 0f else animatedPhase
    val progress = (state.effectivePositionSec.toFloat() / duration.coerceAtLeast(1)).coerceIn(0f, 1f)

    Canvas(
        modifier = Modifier.fillMaxWidth().height(42.dp)
            .pointerInput(duration) {
                detectTapGestures { point ->
                    onSeek((point.x / size.width.coerceAtLeast(1).toFloat() * duration.toFloat()).toInt().coerceIn(0, duration))
                }
            }
            .pointerInput(duration) {
                detectDragGestures { change, _ ->
                    onSeek((change.position.x / size.width.coerceAtLeast(1).toFloat() * duration.toFloat()).toInt().coerceIn(0, duration))
                    change.consume()
                }
            }
    ) {
        val centerY = size.height / 2f
        val progressX = size.width * progress
        val step = 3.dp.toPx()
        drawLine(
            color = GlassWhite.copy(alpha = 0.16f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        val layers = listOf(
            Triple(10.dp.toPx(), 4.dp.toPx(), 0.16f),
            Triple(6.dp.toPx(), 2.8.dp.toPx(), 0.38f),
            Triple(2.2.dp.toPx(), 1.8.dp.toPx(), 0.96f)
        )
        layers.forEachIndexed { index, layer ->
            fun waveY(x: Float): Float {
                val angle = (x / size.width.coerceAtLeast(1f) * 5.5f * 6.2831855f) + phase + index * 0.7f
                return centerY + sin(angle) * layer.first * amplitude
            }

            // Keep a dim waveform visible across the entire track, not just the played segment.
            val fullWave = Path()
            var x = 0f
            while (x <= size.width) {
                val y = waveY(x)
                if (x == 0f) fullWave.moveTo(x, y) else fullWave.lineTo(x, y)
                x += step
            }
            fullWave.lineTo(size.width, waveY(size.width))
            drawPath(
                fullWave,
                accent.copy(alpha = 0.22f),
                style = Stroke(width = layer.second, cap = StrokeCap.Round)
            )

            // Brighten the played section while retaining the same animated wave geometry.
            val activeWave = Path()
            x = 0f
            while (x <= progressX) {
                val y = waveY(x)
                if (x == 0f) activeWave.moveTo(x, y) else activeWave.lineTo(x, y)
                x += step
            }
            if (progressX > 0f) activeWave.lineTo(progressX, waveY(progressX))
            if (progressX > 0f) {
                drawPath(
                    activeWave,
                    accent.copy(alpha = layer.third),
                    style = Stroke(width = layer.second, cap = StrokeCap.Round)
                )
            }
        }
        drawCircle(
            color = GlassWhite.copy(alpha = 0.95f),
            radius = 8.dp.toPx(),
            center = Offset(progressX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(color = accent, radius = 5.dp.toPx(), center = Offset(progressX, centerY))
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
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(buttonSize).background(if (large) Color(0x553B4652) else Color.Transparent, CircleShape)) {
            Icon(Icons.Outlined.Shuffle, "Shuffle", tint = if (mediaState.isShuffle) accent else TextSecondary, modifier = Modifier.size(large.let { if (it) 26.dp else 22.dp }))
        }
        IconButton(onClick = onPrevTrack, modifier = Modifier.size(buttonSize).background(if (large) Color(0x553B4652) else Color.Transparent, CircleShape)) {
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
        IconButton(onClick = onNextTrack, modifier = Modifier.size(buttonSize).background(Color(0x553B4652), CircleShape)) {
            Icon(Icons.Rounded.SkipNext, "Next", tint = GlassWhite, modifier = Modifier.size(if (large) 34.dp else 28.dp))
        }
        IconButton(onClick = onToggleRepeat, modifier = Modifier.size(buttonSize).background(Color(0x553B4652), CircleShape)) {
            Icon(Icons.Outlined.Repeat, "Repeat", tint = if (mediaState.isRepeat) accent else TextSecondary, modifier = Modifier.size(large.let { if (it) 26.dp else 22.dp }))
        }
    }
}
