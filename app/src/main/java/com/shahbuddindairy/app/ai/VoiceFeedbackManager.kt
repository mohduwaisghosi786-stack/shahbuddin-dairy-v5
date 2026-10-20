package com.shahbuddindairy.app.ai

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.shahbuddindairy.app.audio.SoundEffectManager
import java.util.Locale

/**
 * Native Android Text-To-Speech & Event Sound Manager for Shahbuddin Dairy V8.5.
 * - Provides zero-dependency, instantaneous audio confirmations in Hindi/Hinglish.
 * - Event sound chimes for Delivery, Naga, Save, and WhatsApp Share.
 * - Strictly respects Android system silent / vibrate mode.
 * - SharedPreferences persistence for user-configurable audio preferences.
 */
class VoiceFeedbackManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceFeedbackManager"
        private const val PREFS_NAME = "sd_sound_prefs"

        @Volatile
        private var instance: VoiceFeedbackManager? = null

        fun getInstance(context: Context): VoiceFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceFeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Configurable Preferences
    var isMasterSoundEnabled: Boolean
        get() = prefs.getBoolean("master_sound", true)
        set(v) = prefs.edit().putBoolean("master_sound", v).apply()

    var isDeliverySoundEnabled: Boolean
        get() = prefs.getBoolean("delivery_sound", true)
        set(v) = prefs.edit().putBoolean("delivery_sound", v).apply()

    var isNagaSoundEnabled: Boolean
        get() = prefs.getBoolean("naga_sound", true)
        set(v) = prefs.edit().putBoolean("naga_sound", v).apply()

    var isSaveSoundEnabled: Boolean
        get() = prefs.getBoolean("save_sound", true)
        set(v) = prefs.edit().putBoolean("save_sound", v).apply()

    var isShareSoundEnabled: Boolean
        get() = prefs.getBoolean("share_sound", true)
        set(v) = prefs.edit().putBoolean("share_sound", v).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean("voice_tts", true)
        set(v) = prefs.edit().putBoolean("voice_tts", v).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(v) = prefs.edit().putBoolean("haptic_feedback", v).apply()

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
            tts?.setSpeechRate(1.05f) // Brisk cadence suited for dairy runners
            tts?.setPitch(1.0f)
            isInitialized = true
            Log.d(TAG, "TextToSpeech initialized successfully")
        } else {
            Log.w(TAG, "TextToSpeech initialization failed with status $status")
            isInitialized = false
        }
    }

    private fun isSystemSilent(): Boolean {
        val mode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        return mode != AudioManager.RINGER_MODE_NORMAL
    }

    private val soundEffectManager = SoundEffectManager.getInstance(context)

    fun playDeliverySound() {
        if (isDeliverySoundEnabled) {
            soundEffectManager.play(com.shahbuddindairy.app.audio.SoundEvent.DELIVERED)
            soundEffectManager.vibrate(50)
        }
    }

    fun playNagaSound() {
        if (isNagaSoundEnabled) {
            soundEffectManager.play(com.shahbuddindairy.app.audio.SoundEvent.NAGA)
            soundEffectManager.vibrate(80)
        }
    }

    fun playSaveSound() {
        if (isSaveSoundEnabled) {
            soundEffectManager.play(com.shahbuddindairy.app.audio.SoundEvent.SAVE_COMPLETED)
            soundEffectManager.vibrate(40)
        }
    }

    fun playShareSound() {
        if (isShareSoundEnabled) {
            soundEffectManager.play(com.shahbuddindairy.app.audio.SoundEvent.ATTACHMENT_ADDED)
            soundEffectManager.vibrate(30)
        }
    }

    fun speak(text: String) {
        if (!isMasterSoundEnabled || !isVoiceEnabled || !isInitialized || isSystemSilent() || text.isBlank()) return
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
        playDeliverySound()
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
        playNagaSound()
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
