package com.example.data.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import com.example.core.AuvionyxNotificationListener
import com.example.data.model.MediaAppSource
import com.example.data.model.MediaTrack
import com.example.data.model.ServiceAvailabilityState

private const val DEFAULT_ACCENT_ARGB = -16721419 // 0xFF00D9F5 (Electric Cyan)

data class MediaSessionBridgeStatus(
    val serviceState: ServiceAvailabilityState = ServiceAvailabilityState.API_UNAVAILABLE,
    val activeControllerPackage: String? = null,
    val hasNotificationListenerAccess: Boolean = false,
    val localSessionActive: Boolean = false
)

data class ExternalMediaSnapshot(
    val packageName: String,
    val source: MediaAppSource,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val positionSec: Int,
    val isPlaying: Boolean,
    val appLabel: String,
    val artwork: Bitmap?,
    val artworkAccentArgb: Int
)

/**
 * Genuine Android MediaSession integration layer backed by AuvionyxNotificationListener.
 * - Reads active external MediaControllers (Spotify, YouTube Music, system media players)
 *   via MediaSessionManager without mutating the user's selected source tab.
 * - Maintains an internal MediaSession for Auvionyx local playback when active.
 */
class MediaSessionBridge(private val context: Context) {

    private val appLabelCache = ConcurrentHashMap<String, String>()
    private var localSession: MediaSession? = null

    init {
        runCatching {
            localSession = MediaSession(context, "AuvionyxMediaSession").apply {
                isActive = true
            }
        }
    }

    fun hasNotificationListenerAccess(): Boolean {
        val enabled = runCatching {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
        }.getOrNull().orEmpty()
        return enabled.contains(context.packageName)
    }

    fun getActiveExternalController(): MediaController? {
        if (!hasNotificationListenerAccess()) return null
        return runCatching {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val controllers = manager?.getActiveSessions(
                ComponentName(context, AuvionyxNotificationListener::class.java)
            )?.filter { it.packageName != context.packageName }.orEmpty()

            // Prefer currently playing controller, else most recent external session
            controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: controllers.firstOrNull()
        }.getOrNull()
    }

    fun getExternalMediaSnapshot(): ExternalMediaSnapshot? {
        val controller = getActiveExternalController() ?: return null
        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        if (title.isBlank()) return null

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim()
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim()
            ?: controller.packageName
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty()
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val durationSec = (durationMs / 1000L).toInt().coerceAtLeast(1)

        val pb = controller.playbackState
        val isPlaying = pb?.state == PlaybackState.STATE_PLAYING ||
            pb?.state == PlaybackState.STATE_BUFFERING
        val basePosMs = pb?.position ?: 0L
        val effectivePosMs = if (isPlaying && pb != null && pb.lastPositionUpdateTime > 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime).coerceAtLeast(0L)
            basePosMs + (elapsed * pb.playbackSpeed).toLong()
        } else {
            basePosMs
        }
        val positionSec = (effectivePosMs / 1000L).toInt().coerceIn(0, durationSec)

        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val appLabel = appLabelCache.getOrPut(controller.packageName) {
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(controller.packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            }.getOrElse {
                controller.packageName.substringAfterLast('.')
                    .replaceFirstChar { character -> character.uppercase() }
            }
        }
        val artworkAccentArgb = artwork?.let(::sampleArtworkAccent) ?: DEFAULT_ACCENT_ARGB

        val source = when {
            controller.packageName.contains("spotify", ignoreCase = true) -> MediaAppSource.SPOTIFY
            controller.packageName.contains("youtube", ignoreCase = true) -> MediaAppSource.YOUTUBE_MUSIC
            else -> MediaAppSource.LOCAL_PLAYER
        }

        return ExternalMediaSnapshot(
            packageName = controller.packageName,
            source = source,
            title = title,
            artist = artist,
            album = album,
            durationSec = durationSec,
            positionSec = positionSec,
            isPlaying = isPlaying,
            appLabel = appLabel,
            artwork = artwork,
            artworkAccentArgb = artworkAccentArgb
        )
    }

    private fun sampleArtworkAccent(bitmap: Bitmap): Int {
        if (bitmap.width <= 0 || bitmap.height <= 0) return DEFAULT_ACCENT_ARGB
        val sample = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val pixel = sample.getPixel(0, 0)
        if (sample !== bitmap) sample.recycle()
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(pixel, hsv)
        // Preserve the album's hue while keeping the accent vivid enough for glass UI.
        hsv[1] = hsv[1].coerceIn(0.38f, 0.88f)
        hsv[2] = hsv[2].coerceIn(0.58f, 0.96f)
        return AndroidColor.HSVToColor(hsv)
    }

    fun getBridgeStatus(): MediaSessionBridgeStatus {
        val hasListener = hasNotificationListenerAccess()
        val external = getActiveExternalController()
        val state = when {
            external != null -> ServiceAvailabilityState.LIVE
            !hasListener -> ServiceAvailabilityState.PERMISSION_DENIED
            else -> ServiceAvailabilityState.LIVE
        }
        return MediaSessionBridgeStatus(
            serviceState = state,
            activeControllerPackage = external?.packageName,
            hasNotificationListenerAccess = hasListener,
            localSessionActive = localSession?.isActive == true
        )
    }

    fun publishLocalSessionState(track: MediaTrack, isPlaying: Boolean, positionSec: Int) {
        runCatching {
            val session = localSession ?: return
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationSec * 1000L)
                .build()
            session.setMetadata(metadata)

            val stateCode = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO
            val playbackState = PlaybackState.Builder()
                .setActions(actions)
                .setState(stateCode, positionSec * 1000L, if (isPlaying) 1f else 0f)
                .build()
            session.setPlaybackState(playbackState)
        }
    }

    fun dispatchTransportPlayPause(currentlyPlaying: Boolean): Boolean {
        val external = getActiveExternalController() ?: return false
        return runCatching {
            if (currentlyPlaying) external.transportControls.pause() else external.transportControls.play()
            true
        }.getOrDefault(false)
    }

    fun dispatchTransportNext(): Boolean {
        val external = getActiveExternalController() ?: return false
        return runCatching {
            external.transportControls.skipToNext()
            true
        }.getOrDefault(false)
    }

    fun dispatchTransportPrevious(): Boolean {
        val external = getActiveExternalController() ?: return false
        return runCatching {
            external.transportControls.skipToPrevious()
            true
        }.getOrDefault(false)
    }

    fun dispatchSeek(positionMs: Long): Boolean {
        val external = getActiveExternalController() ?: return false
        return runCatching {
            external.transportControls.seekTo(positionMs)
            true
        }.getOrDefault(false)
    }

    fun tryLaunchSourceApp(source: MediaAppSource): Boolean {
        val pkg = when (source) {
            MediaAppSource.SPOTIFY -> "com.spotify.music"
            MediaAppSource.YOUTUBE_MUSIC -> "com.google.android.apps.youtube.music"
            MediaAppSource.LASTWAVE, MediaAppSource.LOCAL_PLAYER -> return true
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    fun release() {
        runCatching {
            localSession?.isActive = false
            localSession?.release()
            localSession = null
        }
    }
}
