package com.example.data.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.example.data.model.ServiceAvailabilityState
import java.util.Locale

interface VoiceEngineCallbacks {
    fun onPartialTranscript(text: String)
    fun onRmsChanged(normalizedLevel: Float)
    fun onFinalTranscript(text: String)
    fun onRecognitionError(
        title: String,
        detail: String,
        serviceState: ServiceAvailabilityState,
        missingPermission: Boolean = false
    )
    fun onTtsStatusChanged(ready: Boolean)
}

/**
 * Native Android voice integration wrapper for SpeechRecognizer, TextToSpeech, and AudioManager.
 * Strictly listens only when explicitly activated via startListening().
 */
class AuvionyxVoiceEngine(
    private val context: Context,
    private val callbacks: VoiceEngineCallbacks
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        runCatching {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                val ok = status == TextToSpeech.SUCCESS &&
                    (textToSpeech?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= 0
                ttsInitialized = ok
                callbacks.onTtsStatusChanged(ok)
            }
        }.onFailure {
            ttsInitialized = false
            callbacks.onTtsStatusChanged(false)
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
    }

    fun isTtsReady(): Boolean = ttsInitialized

    /**
     * Adjusts native Android MUSIC stream volume proportionally to 0..100%.
     */
    fun applySystemMediaVolumePercent(percent: Int) {
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) {
                val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt().coerceIn(0, max)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            }
        }
    }

    fun startListening() {
        if (!hasRecordAudioPermission()) {
            callbacks.onRecognitionError(
                title = "Microphone Permission Denied",
                detail = "Grant RECORD_AUDIO permission to activate native Android SpeechRecognizer.",
                serviceState = ServiceAvailabilityState.PERMISSION_DENIED,
                missingPermission = true
            )
            return
        }

        if (!isSpeechRecognitionAvailable()) {
            callbacks.onRecognitionError(
                title = "Speech Recognition API Unavailable",
                detail = "Android SpeechRecognizer service is not installed or enabled on this device/emulator. Speech recognition cannot run natively here.",
                serviceState = ServiceAvailabilityState.API_UNAVAILABLE,
                missingPermission = false
            )
            return
        }

        mainHandler.post {
            runCatching {
                speechRecognizer?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        callbacks.onPartialTranscript("Listening… speak a command")
                    }

                    override fun onBeginningOfSpeech() = Unit

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.15f, 1.0f)
                        callbacks.onRmsChanged(normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        callbacks.onRmsChanged(0.25f)
                    }

                    override fun onError(error: Int) {
                        val mapped = mapSpeechError(error)
                        callbacks.onRecognitionError(
                            title = mapped.first,
                            detail = mapped.second,
                            serviceState = mapped.third,
                            missingPermission = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                        )
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim().orEmpty()
                        if (best.isNotEmpty()) {
                            callbacks.onFinalTranscript(best)
                        } else {
                            callbacks.onRecognitionError(
                                title = "No Speech Detected",
                                detail = "Tap the mic again and speak clearly.",
                                serviceState = ServiceAvailabilityState.LIVE
                            )
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                        if (!partial.isNullOrEmpty()) {
                            callbacks.onPartialTranscript(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }
                recognizer.startListening(intent)
            }.onFailure { err ->
                callbacks.onRecognitionError(
                    title = "Speech Recognizer API Unavailable",
                    detail = err.localizedMessage ?: "Unable to start Android SpeechRecognizer.",
                    serviceState = ServiceAvailabilityState.API_UNAVAILABLE
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            runCatching { speechRecognizer?.stopListening() }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            runCatching { speechRecognizer?.cancel() }
        }
    }

    fun speakConfirmation(message: String) {
        if (!ttsInitialized || message.isBlank()) return
        runCatching {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "auvionyx_voice_confirm")
        }
    }

    fun release() {
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    private fun mapSpeechError(code: Int): Triple<String, String, ServiceAvailabilityState> = when (code) {
        SpeechRecognizer.ERROR_AUDIO ->
            Triple("Audio Capture Error", "Microphone audio could not be recorded.", ServiceAvailabilityState.API_UNAVAILABLE)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            Triple("Permission Denied", "RECORD_AUDIO permission is required for voice commands.", ServiceAvailabilityState.PERMISSION_DENIED)
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            Triple("Offline / Network Error", "Speech recognition requires internet or an offline ASR language pack.", ServiceAvailabilityState.OFFLINE)
        SpeechRecognizer.ERROR_NO_MATCH ->
            Triple("Command Not Recognized", "No matching speech detected. Try speaking again.", ServiceAvailabilityState.LIVE)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            Triple("Recognizer Busy", "Speech service is busy; try again shortly.", ServiceAvailabilityState.LIVE)
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            Triple("Listening Timed Out", "No voice input detected while listening.", ServiceAvailabilityState.LIVE)
        else ->
            Triple("Speech Recognition Unavailable", "Android SpeechRecognizer reported error code $code.", ServiceAvailabilityState.API_UNAVAILABLE)
    }
}
