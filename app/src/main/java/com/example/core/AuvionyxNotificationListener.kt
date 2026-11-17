package com.example.core

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleMapsLiveNavigation(
    val headline: String,
    val turnInstruction: String,
    val nextTurnDistanceLabel: String,
    val remainingSummary: String,
    val remainingMinutes: Int?,
    val remainingDistanceKm: Float?,
    val etaTimeLabel: String?,
    val updatedAtMs: Long = System.currentTimeMillis()
)

object LiveGoogleMapsSyncBridge {
    private val MAPS_PACKAGES = setOf(
        "com.google.android.apps.maps",
        "com.google.android.apps.gmm",
        "com.google.android.apps.gmm.dev",
        "com.google.android.apps.gmm.qp"
    )

    private val _isListenerConnected = MutableStateFlow(false)
    val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

    private val _activeNavigation = MutableStateFlow<GoogleMapsLiveNavigation?>(null)
    val activeNavigation: StateFlow<GoogleMapsLiveNavigation?> = _activeNavigation.asStateFlow()

    private val _mediaUpdatePulse = MutableStateFlow(0L)
    val mediaUpdatePulse: StateFlow<Long> = _mediaUpdatePulse.asStateFlow()

    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabled = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        return enabled.contains(context.packageName) || _isListenerConnected.value
    }

    internal fun setListenerConnected(connected: Boolean) {
        _isListenerConnected.value = connected
        if (!connected) {
            _activeNavigation.value = null
        }
    }

    internal fun processActiveNotifications(notifications: Array<StatusBarNotification>?) {
        if (notifications == null) return
        val mapsSbn = notifications
            .filter { it.packageName in MAPS_PACKAGES && it.isOngoing }
            .maxByOrNull { it.postTime }

        if (mapsSbn == null) {
            _activeNavigation.value = null
        } else {
            val parsed = parseNotification(mapsSbn.notification)
            if (parsed != null) {
                _activeNavigation.value = parsed
            }
        }
        _mediaUpdatePulse.value = System.currentTimeMillis()
    }

    internal fun parseNotification(notification: Notification?): GoogleMapsLiveNavigation? {
        val extras = notification?.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()

        return parseGoogleMapsNotificationText(
            title = title,
            text = text,
            subText = subText,
            bigText = bigText
        )
    }

    /**
     * Pure parser for Google Maps ongoing navigation notifications across Android OS versions/locales.
     */
    fun parseGoogleMapsNotificationText(
        title: String,
        text: String,
        subText: String = "",
        bigText: String = ""
    ): GoogleMapsLiveNavigation? {
        val combinedParts = listOf(title, text, subText, bigText).filter { it.isNotBlank() }
        if (combinedParts.isEmpty()) return null

        val joined = combinedParts.joinToString(" · ")
        val distanceRegex = Regex("""\b(\d+(?:[.,]\d+)?\s*(?:km|m|mi|ft))\b""", RegexOption.IGNORE_CASE)
        val distanceMatch = distanceRegex.find(title) ?: distanceRegex.find(text)
        val distanceLabel = distanceMatch?.groupValues?.get(1)?.trim() ?: "--"

        val instruction = when {
            text.isNotBlank() && !text.equals(distanceLabel, ignoreCase = true) -> text
            bigText.isNotBlank() -> bigText
            title.isNotBlank() -> title
            else -> "Navigating via Google Maps"
        }

        val headline = when {
            title.isNotBlank() && !title.equals(distanceLabel, ignoreCase = true) -> title
            else -> instruction
                .replace(Regex("""^(Turn\s+(left|right)|Keep\s+(left|right)|Head\s+\w+|Merge|Continue|Exit|Take\s+exit)\s*(onto|on|toward|towards)?\s*""", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { instruction }
        }

        val etaSource = listOf(subText, text, bigText, joined).firstOrNull {
            it.contains("min", ignoreCase = true) ||
                it.contains("hr", ignoreCase = true) ||
                it.contains("ETA", ignoreCase = true) ||
                it.contains(":")
        }.orEmpty()

        val minMatch = Regex("""(\d+)\s*min""", RegexOption.IGNORE_CASE).find(etaSource)
        val hrMatch = Regex("""(\d+)\s*hr""", RegexOption.IGNORE_CASE).find(etaSource)
        val remainingMinutes = when {
            hrMatch != null && minMatch != null -> hrMatch.groupValues[1].toInt() * 60 + minMatch.groupValues[1].toInt()
            minMatch != null -> minMatch.groupValues[1].toInt()
            hrMatch != null -> hrMatch.groupValues[1].toInt() * 60
            else -> null
        }

        val remKmMatch = Regex("""(\d+(?:[.,]\d+)?)\s*km""", RegexOption.IGNORE_CASE).find(etaSource)
        val remainingKm = remKmMatch?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull()

        val etaTimeMatch = Regex("""\b(\d{1,2}:\d{2}(?:\s*[APap][Mm])?)\b""").find(etaSource)
        val etaLabel = etaTimeMatch?.groupValues?.get(1)?.trim()

        return GoogleMapsLiveNavigation(
            headline = headline,
            turnInstruction = instruction,
            nextTurnDistanceLabel = distanceLabel,
            remainingSummary = subText.ifBlank { etaSource },
            remainingMinutes = remainingMinutes,
            remainingDistanceKm = remainingKm,
            etaTimeLabel = etaLabel
        )
    }
}

class AuvionyxNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        LiveGoogleMapsSyncBridge.setListenerConnected(true)
        runCatching {
            LiveGoogleMapsSyncBridge.processActiveNotifications(activeNotifications)
        }
    }

    override fun onListenerDisconnected() {
        LiveGoogleMapsSyncBridge.setListenerConnected(false)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        runCatching {
            LiveGoogleMapsSyncBridge.processActiveNotifications(activeNotifications)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        runCatching {
            LiveGoogleMapsSyncBridge.processActiveNotifications(activeNotifications)
        }
    }
}
