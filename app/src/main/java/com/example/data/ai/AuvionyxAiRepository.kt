package com.example.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AuvionyxVoiceAi"

data class ParsedNaturalVoiceIntent(
    val category: String,
    val action: String,
    val parameter: String,
    val confirmationMessage: String,
    val requiresNativeBridge: Boolean
)

/**
 * Optional Gemini natural-language voice command interpreter powered by Firebase AI Logic.
 * Only invoked for complex or ambiguous spoken commands when local deterministic matching
 * does not match directly. Uses structured JSON output and App Check (no exposed API keys).
 */
class AuvionyxAiRepository {

    private val intentSchema = Schema.obj(
        properties = mapOf(
            "category" to Schema.enumeration(
                values = listOf("NAVIGATION", "MEDIA", "VEHICLE", "INTERFACE", "UNSUPPORTED"),
                description = "Infotainment domain category for the voice command"
            ),
            "action" to Schema.enumeration(
                values = listOf(
                    "NAVIGATE_TO",
                    "SEARCH_PLACES",
                    "CHECK_TRAFFIC",
                    "PLAY_MEDIA",
                    "PAUSE_MEDIA",
                    "RESUME_MEDIA",
                    "NEXT_TRACK",
                    "PREV_TRACK",
                    "SET_VOLUME",
                    "OPEN_MEDIA_APP",
                    "PLAY_PLAYLIST",
                    "OPEN_HUD",
                    "SHOW_JOURNEY",
                    "OPEN_VEHICLE",
                    "OPEN_MAPS",
                    "OPEN_MUSIC",
                    "SET_LAYOUT_RATIO",
                    "ACTIVATE_FULLSCREEN",
                    "UNSUPPORTED"
                ),
                description = "Exact action identifier"
            ),
            "parameter" to Schema.string(
                description = "Target destination, place category, media app, ratio (70_30, 60_40, 50_50, FULL_MAPS, FULL_MUSIC), or numeric volume 0-100. Empty string if none."
            ),
            "confirmationMessage" to Schema.string(
                description = "Short, hands-free spoken confirmation under 14 words"
            ),
            "requiresNativeBridge" to Schema.boolean(
                description = "True if executing against external hardware/Google Maps/Spotify requires native SDK integration"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = intentSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are the natural-language command parser for Auvionyx Voice.
                    Convert spoken driver utterances strictly into a structured command JSON.
                    Never generate conversational chit-chat. Keep confirmationMessage crisp, factual, and under 14 words.
                    
                    NAVIGATION INTENT RULES:
                    - Treat "drive me to", "take me to", "get me to", "navigate to", "directions to", "I want to go to", and natural equivalents as NAVIGATION / NAVIGATE_TO.
                    - Extract the actual destination, not the command words. Example: "Hey Skoda, drive me to my home in Haridwar" -> parameter "Haridwar, Uttarakhand, India".
                    - Preserve meaningful place context such as city, state, country, neighbourhood, landmark or business. Do not invent a street address or coordinates.
                    - If the driver says "home" or "work" without a location and no saved place is supplied in Vehicle Context, do not guess. Return UNSUPPORTED and ask a short clarification in confirmationMessage.
                    - If the utterance names a city after "home in" or "work in", use that named city as the geocoding query. Do not claim it resolved a private saved address.
                    - Nearby requests such as "find the nearest petrol pump" should be NAVIGATION / SEARCH_PLACES with the place category as parameter.
                    
                    Keep parameter concise and suitable for a real geocoder. Never fabricate coordinates, route details, search results, or a destination.
                    If the user asks for something outside Navigation, Media, Vehicle, or Interface control, return category UNSUPPORTED and action UNSUPPORTED.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun interpretComplexVoiceCommand(
        utterance: String,
        contextSummary: String
    ): Result<ParsedNaturalVoiceIntent> {
        val prompt = buildString {
            appendLine("Vehicle Context:")
            appendLine(contextSummary)
            append("Driver Spoken Command: ")
            append(utterance)
        }

        return try {
            val response = withTimeout(30_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text?.trim()
            if (rawJson.isNullOrEmpty()) {
                Result.failure(IllegalStateException("Voice AI interpreter returned an empty intent."))
            } else {
                val json = JSONObject(rawJson)
                Result.success(
                    ParsedNaturalVoiceIntent(
                        category = json.optString("category", "UNSUPPORTED"),
                        action = json.optString("action", "UNSUPPORTED"),
                        parameter = json.optString("parameter", ""),
                        confirmationMessage = json.optString("confirmationMessage", "Command recognized."),
                        requiresNativeBridge = json.optBoolean("requiresNativeBridge", false)
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini voice intent interpretation timed out", e)
            Result.failure(IllegalStateException("Voice interpretation timed out. Try a shorter command."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic voice interpretation failed", e)
            Result.failure(
                IllegalStateException(
                    e.localizedMessage ?: "Natural-language interpreter unavailable."
                )
            )
        }
    }
}
