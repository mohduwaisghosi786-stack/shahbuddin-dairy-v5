package com.shahbuddindairy.app.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Native Android Text-To-Speech Manager for Shahbuddin Dairy Runner Mode.
 * - Provides zero-dependency, instantaneous audio confirmations in Hindi/Hinglish.
 * - Confirms deliveries, nagas, and customer skips without requiring the runner to glance at the screen.
 * - Completely offline and gracefully silent if TTS is not available on the device.
 */
class VoiceFeedbackManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceFeedbackManager"
        @Volatile
        private var instance: VoiceFeedbackManager? = null

        fun getInstance(context: Context): VoiceFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceFeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isVoiceEnabled: Boolean = true

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize TextToSpeech: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hindiLocale = Locale("hi", "IN")
            val result = tts?.setLanguage(hindiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Hindi TTS not fully supported, falling back to default locale")
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(1.05f) // Slightly brisk cadence suited for fast dairy runners
            tts?.setPitch(1.0f)
            isInitialized = true
            Log.d(TAG, "TextToSpeech initialized successfully")
        } else {
            Log.w(TAG, "TextToSpeech initialization failed with status $status")
            isInitialized = false
        }
    }

    fun speak(text: String) {
        if (!isVoiceEnabled || !isInitialized || text.isBlank()) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dairy_voice_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w(TAG, "Error speaking text: ${e.message}")
        }
    }

    /**
     * Announces a completed milk delivery:
     * e.g., "भोलानाथ, 2.5 लीटर डिलीवर, बिल ₹175"
     */
    fun speakDelivery(customerName: String, liters: Double, billAmount: Double) {
        val litStr = if (liters == liters.toLong().toDouble()) {
            "${liters.toLong()}"
        } else {
            "%.1f".format(Locale.US, liters)
        }
        val billStr = "%.0f".format(Locale.US, billAmount)
        speak("$customerName, $litStr लीटर डिलीवर, बिल ₹$billStr")
    }

    /**
     * Announces a Naga (no-delivery) mark.
     */
    fun speakNaga(customerName: String) {
        speak("$customerName, आज का नागा दर्ज हुआ")
    }

    /**
     * Announces skipping a customer to the end of the queue.
     */
    fun speakSkip(customerName: String) {
        speak("$customerName को बाद के लिए छोड़ा")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS: ${e.message}")
        } finally {
            tts = null
            isInitialized = false
        }
    }
}
