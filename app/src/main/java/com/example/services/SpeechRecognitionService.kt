package com.example.services

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Success(val transcript: String) : VoiceState()
    data class Error(val message: String, val rawPartial: String? = null) : VoiceState()
    object PermissionDenied : VoiceState()
}

class SpeechRecognitionService(private val context: Context) {

    companion object {
        private const val TAG = "VoiceBookingSpeech"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    fun isPermissionGranted(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "isPermissionGranted: $granted")
        return granted
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "SpeechRecognizer.isRecognitionAvailable(context): $available")
        return available
    }

    fun isRecognitionServiceAvailable(): Boolean {
        val intent = Intent(RecognitionService.SERVICE_INTERFACE)
        val services = context.packageManager.queryIntentServices(intent, 0)
        val available = services.isNotEmpty()
        Log.d(TAG, "isRecognitionServiceAvailable: $available (found ${services.size} services)")
        return available
    }

    fun startListening(
        languageCode: String = "en-IN",
        onStateChange: (VoiceState) -> Unit
    ) {
        Log.d(TAG, "startListening invoked with languageCode: $languageCode")

        if (!isPermissionGranted()) {
            Log.w(TAG, "RECORD_AUDIO permission not granted")
            onStateChange(VoiceState.PermissionDenied)
            return
        }

        val recogAvailable = isSpeechRecognitionAvailable()
        val serviceAvailable = isRecognitionServiceAvailable()

        if (!recogAvailable || !serviceAvailable) {
            Log.e(TAG, "Speech recognition service unavailable on device (recogAvailable=$recogAvailable, serviceAvailable=$serviceAvailable)")
            onStateChange(
                VoiceState.Error("Voice recognition is unavailable on this device. Please try Voice Booking on a real Android device.")
            )
            return
        }

        try {
            cleanupRecognizer()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d(TAG, "SpeechRecognizer created successfully using default provider")

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your transport request...")
            }

            var partialTranscript = ""

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Callback: onReadyForSpeech")
                    onStateChange(VoiceState.Listening)
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Callback: onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level meter signal
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    Log.d(TAG, "Callback: onBufferReceived (size=${buffer?.size ?: 0})")
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Callback: onEndOfSpeech")
                }

                override fun onError(error: Int) {
                    val errCodeName = getErrorCodeName(error)
                    Log.e(TAG, "Callback: onError code=$error ($errCodeName)")

                    val errMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech matching your request was detected. Please try speaking clearly."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected. Tap the mic and speak your transport details."
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection issue during speech recognition. Please check internet connection."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during speech recognition. Please try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again in a moment."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice booking."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone device."
                        SpeechRecognizer.ERROR_SERVER -> "Voice recognition is unavailable on this device. Please try Voice Booking on a real Android device."
                        SpeechRecognizer.ERROR_CLIENT -> "Voice recognition is unavailable on this device. Please try Voice Booking on a real Android device."
                        11 -> "Voice recognition is unavailable on this device. Please try Voice Booking on a real Android device."
                        else -> "Voice recognition is unavailable on this device. Please try Voice Booking on a real Android device."
                    }

                    if (partialTranscript.isNotBlank()) {
                        Log.d(TAG, "Partial transcript recovered on error: '$partialTranscript'")
                        onStateChange(VoiceState.Success(partialTranscript))
                    } else {
                        onStateChange(VoiceState.Error(errMsg, partialPartialText(partialTranscript)))
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d(TAG, "Callback: onResults matches=${matches?.joinToString(" | ")}")

                    if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                        val transcript = matches[0].trim()
                        Log.d(TAG, "Recognized transcript successfully: '$transcript'")
                        onStateChange(VoiceState.Success(transcript))
                    } else if (partialTranscript.isNotBlank()) {
                        Log.d(TAG, "Using partial transcript as final result: '$partialTranscript'")
                        onStateChange(VoiceState.Success(partialTranscript))
                    } else {
                        Log.w(TAG, "onResults produced empty matches list")
                        onStateChange(VoiceState.Error("No speech input recognized. Please speak clearly or type your request below."))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                        partialTranscript = matches[0].trim()
                        Log.d(TAG, "Callback: onPartialResults: '$partialTranscript'")
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d(TAG, "Callback: onEvent eventType=$eventType")
                }
            })

            speechRecognizer?.startListening(intent)
            onStateChange(VoiceState.Listening)

        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing or starting SpeechRecognizer: ${e.localizedMessage}", e)
            onStateChange(
                VoiceState.Error("Voice recognition is unavailable on this device. Please try Voice Booking on a real Android device.")
            )
        }
    }

    private fun partialPartialText(text: String): String? {
        return text.ifBlank { null }
    }

    private fun getErrorCodeName(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO (3)"
            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT (5)"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS (9)"
            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK (2)"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT (1)"
            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH (7)"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY (8)"
            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER (4)"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT (6)"
            11 -> "ERROR_SERVER_DISCONNECTED_OR_UNAVAILABLE (11)"
            else -> "ERROR_UNKNOWN ($error)"
        }
    }

    fun stopListening() {
        try {
            Log.d(TAG, "stopListening requested")
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening: ${e.localizedMessage}")
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "Cleaned up old SpeechRecognizer instance")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up SpeechRecognizer: ${e.localizedMessage}")
        }
    }

    fun destroy() {
        cleanupRecognizer()
    }
}

