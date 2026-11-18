package com.example.core

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Best-effort snapshot of navigation information exposed by a third-party navigation
 * app through its ongoing Android notification. Values are nullable when the provider
 * does not publish them; the app must never fill missing values with demo data.
 */
data class GoogleMapsLiveNavigation(
    val headline: String,
    val turnInstruction: String,
    val nextTurnDistanceLabel: String,
    val remainingSummary: String,
    val remainingMinutes: Int?,
    val remainingDistanceKm: Float?,
    val etaTimeLabel: String?,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val providerLabel: String = "Google Maps",
    val packageName: String = "com.google.android.apps.maps"
)

object LiveGoogleMapsSyncBridge {
    private val PROVIDERS = mapOf(
        "com.google.android.apps.maps" to "Google Maps",
        "com.waze" to "Waze",
        "com.mmi.maps" to "Mappls"
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
        if (!connected) _activeNavigation.value = null
    }

    internal fun processActiveNotifications(notifications: Array<StatusBarNotification>?) {
        if (notifications == null) return
        val active = notifications
            .asSequence()
            .filter { it.isOngoing && PROVIDERS.containsKey(it.packageName) }
            .sortedByDescending { it.postTime }
            .mapNotNull { sbn ->
                parseNotification(sbn.packageName, sbn.notification)
            }
            .firstOrNull()

        _activeNavigation.value = active
        _mediaUpdatePulse.value = System.currentTimeMillis()
    }

    internal fun parseNotification(
        packageName: String,
        notification: Notification?
    ): GoogleMapsLiveNavigation? {
        val extras = notification?.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val textLines = extras.getCharSequenceArray("android.textLines")
            ?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
            .orEmpty()

        return parseNavigationNotification(
            packageName = packageName,
            title = title,
            text = text,
            subText = subText,
            bigText = bigText,
            extraLines = textLines
        )
    }

    /**
     * Provider-agnostic parser for the navigation text exposed by Maps, Waze and Mappls.
     * Notification formats vary by app version and device language, so this intentionally
     * preserves raw text and only extracts fields that are clearly present.
     */
    fun parseNavigationNotification(
        packageName: String,
        title: String,
        text: String,
        subText: String = "",
        bigText: String = "",
        extraLines: List<String> = emptyList()
    ): GoogleMapsLiveNavigation? {
        val provider = PROVIDERS[packageName] ?: return null
        val parts = (listOf(title, text, subText, bigText) + extraLines)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        if (parts.isEmpty()) return null

        val joined = parts.joinToString(" · ")
        val distanceRegex = Regex(
            """\b(\d+(?:[.,]\d+)?\s*(?:km|m|mi|ft))\b""",
            RegexOption.IGNORE_CASE
        )
        val distanceMatch = distanceRegex.find(joined)
        val distanceLabel = distanceMatch?.groupValues?.get(1)?.trim() ?: "--"

        val maneuverRegex = Regex(
            """\b(turn|keep|bear|merge|exit|take exit|continue|head|roundabout|u-turn|make a)\b""",
            RegexOption.IGNORE_CASE
        )
        val hasNavigationSignal = maneuverRegex.containsMatchIn(joined) ||
            distanceRegex.containsMatchIn(joined) ||
            Regex("""\\b(navigat|destination|arrival|arriving|eta)\\b""", RegexOption.IGNORE_CASE).containsMatchIn(joined)
        if (!hasNavigationSignal) return null

        val instruction = parts.firstOrNull { maneuverRegex.containsMatchIn(it) }
            ?: text.takeIf(String::isNotBlank)
            ?: bigText.takeIf(String::isNotBlank)
            ?: title.takeIf(String::isNotBlank)
            ?: joined

        val roadMatch = Regex(
            """(?:onto|on|towards?|to)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        ).find(instruction)
        val headline = roadMatch?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
            ?: title.takeIf { it.isNotBlank() && !distanceRegex.matches(it) }
            ?: instruction

        val timeSource = (listOf(subText, bigText) + extraLines + parts)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" · ")
        val minuteMatches = Regex(
            """(\d+)\s*(?:min|mins|minutes)\b""",
            RegexOption.IGNORE_CASE
        ).findAll(timeSource).toList()
        val hourMatch = Regex(
            """(\d+)\s*(?:hr|hrs|hours)\b""",
            RegexOption.IGNORE_CASE
        ).find(timeSource)
        val remainingMinutes = when {
            hourMatch != null && minuteMatches.isNotEmpty() ->
                hourMatch.groupValues[1].toInt() * 60 + minuteMatches.last().groupValues[1].toInt()
            hourMatch != null -> hourMatch.groupValues[1].toInt() * 60
            minuteMatches.isNotEmpty() -> minuteMatches.last().groupValues[1].toInt()
            else -> null
        }

        // Do not mistake the next-turn distance for the whole-trip distance.
        val remainingSource = (listOf(subText, bigText) + extraLines)
            .filter(String::isNotBlank)
            .joinToString(" · ")
        val remainingKm = Regex(
            """(\d+(?:[.,]\d+)?)\s*km""",
            RegexOption.IGNORE_CASE
        ).find(remainingSource)
            ?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toFloatOrNull()

        val etaSource = (listOf(subText, bigText) + extraLines)
            .firstOrNull {
                it.contains("ETA", ignoreCase = true) ||
                    Regex("""\b\d{1,2}:\d{2}(?:\s*[APap][Mm])?\b""").containsMatchIn(it)
            }.orEmpty()
        val etaTimeLabel = Regex(
            """\b(\d{1,2}:\d{2}(?:\s*[APap][Mm])?)\b"""
        ).find(etaSource)?.groupValues?.get(1)

        val summary = listOf(subText, bigText)
            .firstOrNull(String::isNotBlank)
            ?: extraLines.firstOrNull(String::isNotBlank)
            ?: ""

        return GoogleMapsLiveNavigation(
            headline = headline,
            turnInstruction = instruction,
            nextTurnDistanceLabel = distanceLabel,
            remainingSummary = summary,
            remainingMinutes = remainingMinutes,
            remainingDistanceKm = remainingKm,
            etaTimeLabel = etaTimeLabel,
            providerLabel = provider,
            packageName = packageName
        )
    }

    /** Kept for existing unit tests and callers that specifically parse Google Maps. */
    fun parseGoogleMapsNotificationText(
        title: String,
        text: String,
        subText: String = "",
        bigText: String = ""
    ): GoogleMapsLiveNavigation? = parseNavigationNotification(
        packageName = "com.google.android.apps.maps",
        title = title,
        text = text,
        subText = subText,
        bigText = bigText
    )
}

class AuvionyxNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        LiveGoogleMapsSyncBridge.setListenerConnected(true)
        runCatching { LiveGoogleMapsSyncBridge.processActiveNotifications(activeNotifications) }
    }

    override fun onListenerDisconnected() {
        LiveGoogleMapsSyncBridge.setListenerConnected(false)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        runCatching { LiveGoogleMapsSyncBridge.processActiveNotifications(activeNotifications) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        runCatching { LiveGoogleMapsSyncBridge.processActiveNotifications(activeNotifications) }
    }
}
