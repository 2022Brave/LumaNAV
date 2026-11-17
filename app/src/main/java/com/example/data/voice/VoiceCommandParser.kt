package com.example.data.voice

import com.example.data.model.DashboardSplitRatio
import com.example.data.model.MediaAppSource

/**
 * Fast, deterministic local command parser for Auvionyx Voice.
 * Handles direct Navigation, Media, Vehicle, and Interface commands without invoking Gemini.
 */
object VoiceCommandParser {

    private fun hasWord(text: String, word: String): Boolean =
        Regex("""\b${Regex.escape(word)}\b""").containsMatchIn(text)

    fun parseDeterministic(rawUtterance: String): ParsedVoiceCommand? {
        val text = rawUtterance.trim().lowercase()
        if (text.isEmpty()) return null

        // 1. INTERFACE commands
        if (text.contains("full screen") || text.contains("fullscreen")) {
            val ratio = if (hasWord(text, "music") || hasWord(text, "media")) {
                DashboardSplitRatio.FULL_MUSIC
            } else {
                DashboardSplitRatio.FULL_MAPS
            }
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.INTERFACE,
                action = VoiceCommandAction.ACTIVATE_FULLSCREEN,
                targetRatio = ratio
            )
        }

        if (hasWord(text, "layout") || hasWord(text, "split") || text.contains("70 30") ||
            text.contains("70:30") || text.contains("60 40") || text.contains("60:40") ||
            text.contains("50 50") || text.contains("50:50")
        ) {
            val ratio = parseRatio(text) ?: DashboardSplitRatio.RATIO_60_40
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.INTERFACE,
                action = VoiceCommandAction.CHANGE_DASHBOARD_LAYOUT,
                targetRatio = ratio
            )
        }

        if (text == "open maps" || text == "show maps" || text == "go to maps") {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.INTERFACE,
                action = VoiceCommandAction.OPEN_MAPS
            )
        }

        if (text == "open music" || text == "show music" || text == "open media") {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.INTERFACE,
                action = VoiceCommandAction.OPEN_MUSIC
            )
        }

        // 2. VEHICLE commands
        if (hasWord(text, "hud") || text.contains("heads up") || text.contains("head-up")) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.VEHICLE,
                action = VoiceCommandAction.OPEN_HUD
            )
        }

        if (hasWord(text, "journey") || hasWord(text, "eta") || text.contains("how far") ||
            text.contains("remaining distance")
        ) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.VEHICLE,
                action = VoiceCommandAction.SHOW_JOURNEY_INFO
            )
        }

        if (hasWord(text, "vehicle") || text.contains("car status") || hasWord(text, "battery") ||
            hasWord(text, "range") || hasWord(text, "tire") || hasWord(text, "tires") || hasWord(text, "climate")
        ) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.VEHICLE,
                action = VoiceCommandAction.DISPLAY_VEHICLE_DASHBOARD
            )
        }

        // 3. MEDIA commands
        if (hasWord(text, "volume") || hasWord(text, "louder") || hasWord(text, "quieter") ||
            text == "mute" || text == "unmute"
        ) {
            val explicitPct = Regex("""\b(\d{1,3})\b""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            return when {
                text == "mute" -> ParsedVoiceCommand(
                    category = VoiceCommandCategory.MEDIA,
                    action = VoiceCommandAction.CHANGE_VOLUME,
                    targetVolumePercent = 0
                )
                explicitPct != null -> ParsedVoiceCommand(
                    category = VoiceCommandCategory.MEDIA,
                    action = VoiceCommandAction.CHANGE_VOLUME,
                    targetVolumePercent = explicitPct.coerceIn(0, 100)
                )
                hasWord(text, "down") || hasWord(text, "lower") || hasWord(text, "quieter") -> ParsedVoiceCommand(
                    category = VoiceCommandCategory.MEDIA,
                    action = VoiceCommandAction.CHANGE_VOLUME,
                    volumeDeltaPercent = -15
                )
                else -> ParsedVoiceCommand(
                    category = VoiceCommandCategory.MEDIA,
                    action = VoiceCommandAction.CHANGE_VOLUME,
                    volumeDeltaPercent = 15
                )
            }
        }

        if (text.contains("next track") || text.contains("next song") || text == "next" || text == "skip") {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.NEXT_TRACK
            )
        }

        if (text.contains("previous track") || text.contains("previous song") ||
            text.contains("last track") || text == "previous"
        ) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.PREVIOUS_TRACK
            )
        }

        if (hasWord(text, "pause") || text.contains("stop music") || text.contains("stop playing")) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.PAUSE_MUSIC
            )
        }

        if (hasWord(text, "resume") || text == "unpause" || text == "continue music") {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.RESUME_MUSIC
            )
        }

        val detectedMediaApp = parseMediaApp(text)
        if (detectedMediaApp != null && (hasWord(text, "open") || hasWord(text, "switch") || text.contains("play on"))) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.OPEN_MEDIA_APP,
                mediaSource = detectedMediaApp
            )
        }

        if (hasWord(text, "playlist") || hasWord(text, "lamentis") || hasWord(text, "echelon") ||
            hasWord(text, "synthwave")
        ) {
            val playlist = text.removePrefix("play playlist").removePrefix("play").trim()
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.PLAY_PLAYLIST,
                playlistHint = playlist.ifBlank { "Night Synthwave" }
            )
        }

        if (text.startsWith("play music") || text == "play" || text == "start music") {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.MEDIA,
                action = VoiceCommandAction.PLAY_MUSIC
            )
        }

        // 4. NAVIGATION commands
        if (hasWord(text, "traffic") || hasWord(text, "congestion") || hasWord(text, "pulse")) {
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.NAVIGATION,
                action = VoiceCommandAction.TRAFFIC_CONDITIONS
            )
        }

        if (hasWord(text, "restaurant") || hasWord(text, "restaurants") || hasWord(text, "cafe") ||
            hasWord(text, "petrol") || text.contains("gas station") || hasWord(text, "fuel") ||
            hasWord(text, "charger") || hasWord(text, "chargers") || hasWord(text, "ev")
        ) {
            val query = when {
                hasWord(text, "restaurant") || hasWord(text, "restaurants") || hasWord(text, "cafe") -> "Restaurants"
                hasWord(text, "petrol") || text.contains("gas station") || hasWord(text, "fuel") -> "Petrol Pumps"
                else -> "EV Fast Charger"
            }
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.NAVIGATION,
                action = VoiceCommandAction.SEARCH_RESTAURANTS_OR_FUEL,
                queryOrTarget = query
            )
        }

        if (hasWord(text, "nearby") || text.startsWith("find ")) {
            val query = text.substringAfter("nearby").substringAfter("find ").trim().ifBlank { "Nearby Places" }
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.NAVIGATION,
                action = VoiceCommandAction.FIND_NEARBY,
                queryOrTarget = query
            )
        }

        if (text.startsWith("navigate to") || text.startsWith("route to") ||
            text.startsWith("directions to") || text.startsWith("drive to")
        ) {
            val destination = text
                .removePrefix("navigate to")
                .removePrefix("route to")
                .removePrefix("directions to")
                .removePrefix("drive to")
                .trim()
                .ifBlank { "Rajpur Road" }
            return ParsedVoiceCommand(
                category = VoiceCommandCategory.NAVIGATION,
                action = VoiceCommandAction.NAVIGATE_TO,
                queryOrTarget = destination
            )
        }

        return null
    }

    fun parseRatio(text: String): DashboardSplitRatio? {
        val norm = text.lowercase()
        return when {
            norm.contains("70") && norm.contains("30") -> DashboardSplitRatio.RATIO_70_30
            norm.contains("60") && norm.contains("40") -> DashboardSplitRatio.RATIO_60_40
            norm.contains("50") && norm.contains("50") -> DashboardSplitRatio.RATIO_50_50
            norm.contains("full_maps") || norm.contains("full maps") -> DashboardSplitRatio.FULL_MAPS
            norm.contains("full_music") || norm.contains("full music") -> DashboardSplitRatio.FULL_MUSIC
            else -> null
        }
    }

    fun parseMediaApp(text: String): MediaAppSource? {
        val norm = text.lowercase()
        return when {
            norm.contains("spotify") -> MediaAppSource.SPOTIFY
            norm.contains("youtube") -> MediaAppSource.YOUTUBE_MUSIC
            norm.contains("lastwave") || norm.contains("last wave") -> MediaAppSource.LASTWAVE
            norm.contains("local") -> MediaAppSource.LOCAL_PLAYER
            else -> null
        }
    }

    /**
     * Resolves spoken disambiguation responses during CONFIRMATION_REQUIRED state.
     * Returns 0-based index for selection, -1 for cancel, or null if unrelated.
     */
    fun parseConfirmationReply(utterance: String, candidateCount: Int): Int? {
        val norm = utterance.trim().lowercase()
        if (norm.isEmpty()) return null
        if (hasWord(norm, "cancel") || hasWord(norm, "stop") || hasWord(norm, "no") || hasWord(norm, "nevermind")) {
            return -1
        }
        if (hasWord(norm, "confirm") || hasWord(norm, "yes") || hasWord(norm, "ok") || hasWord(norm, "okay") ||
            hasWord(norm, "first") || hasWord(norm, "one") || hasWord(norm, "1")
        ) {
            return if (candidateCount > 0) 0 else null
        }
        if ((hasWord(norm, "second") || hasWord(norm, "two") || hasWord(norm, "2")) && candidateCount >= 2) return 1
        if ((hasWord(norm, "third") || hasWord(norm, "three") || hasWord(norm, "3")) && candidateCount >= 3) return 2
        if ((hasWord(norm, "fourth") || hasWord(norm, "four") || hasWord(norm, "4")) && candidateCount >= 4) return 3
        if ((hasWord(norm, "fifth") || hasWord(norm, "five") || hasWord(norm, "5")) && candidateCount >= 5) return 4
        return null
    }
}
