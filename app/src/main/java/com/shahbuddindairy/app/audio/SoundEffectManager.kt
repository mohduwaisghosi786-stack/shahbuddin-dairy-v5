package com.shahbuddindairy.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.shahbuddindairy.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class SoundEvent(val idKey: String, val label: String, val defaultRawRes: Int) {
    DELIVERED("snd_delivered", "Delivered Successfully", R.raw.snd_delivered),
    NAGA("snd_naga", "Naaga Marked", R.raw.snd_naga),
    PENDING("snd_pending", "Pending / Later", R.raw.snd_pending),
    NEXT_CUSTOMER("snd_next", "Next Customer", R.raw.snd_next),
    PREV_CUSTOMER("snd_prev", "Previous Customer", R.raw.snd_prev),
    CASH_INPUT("snd_cash_input", "Cash Amount Entered", R.raw.snd_cash_input),
    CASH_SAVED("snd_cash_saved", "Cash Received & Saved", R.raw.snd_cash_saved),
    ONLINE_SAVED("snd_online_saved", "Online Payment Saved", R.raw.snd_online_saved),
    SAVE_COMPLETED("snd_save", "Save Completed", R.raw.snd_save),
    UNDO_COMPLETED("snd_undo", "Undo / Correction", R.raw.snd_undo),
    NOTE_SAVED("snd_note_saved", "Note Saved", R.raw.snd_note_saved),
    ATTACHMENT_ADDED("snd_attachment", "Attachment Added", R.raw.snd_attachment),
    ERROR_WARNING("snd_error", "Error / Warning", R.raw.snd_error),
    MIC_TOGGLE("snd_mic", "AI / Mic Toggle", R.raw.snd_mic)
}

enum class SoundProfile(val label: String, val description: String, val volumeMultiplier: Float) {
    DAIRY_PRO("Dairy Pro", "Balanced crisp cues for daily delivery", 1.0f),
    SOFT_QUIET("Soft & Quiet", "Gentle, non-intrusive sound cues", 0.45f),
    OUTDOOR_SUNLIGHT("Outdoor / Sunlight", "Maximum clarity and loudness in traffic", 1.0f),
    CLASSIC_BELL("Classic Bell", "Traditional dairy chime experience", 0.9f),
    CUSTOM("Custom", "Personalized sound file mappings", 1.0f)
}

class SoundEffectManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SoundEffectManager"
        private const val PREFS_NAME = "sd_sound_engine_prefs"

        @Volatile
        private var instance: SoundEffectManager? = null

        fun getInstance(context: Context): SoundEffectManager {
            return instance ?: synchronized(this) {
                instance ?: SoundEffectManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var soundPool: SoundPool? = null
    private val soundIdMap = mutableMapOf<SoundEvent, Int>()
    private val loadedSounds = mutableSetOf<Int>()

    // Master Settings
    var isMasterSoundEnabled: Boolean
        get() = prefs.getBoolean("master_sound", true)
        set(v) = prefs.edit().putBoolean("master_sound", v).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(v) = prefs.edit().putBoolean("haptic_feedback", v).apply()

    var soundVolume: Float
        get() = prefs.getFloat("sound_volume", 1.0f)
        set(v) = prefs.edit().putFloat("sound_volume", v.coerceIn(0.1f, 1.0f)).apply()

    var activeProfile: SoundProfile
        get() {
            val name = prefs.getString("sound_profile", SoundProfile.DAIRY_PRO.name)
            return try {
                SoundProfile.valueOf(name ?: SoundProfile.DAIRY_PRO.name)
            } catch (e: Exception) {
                SoundProfile.DAIRY_PRO
            }
        }
        set(p) = prefs.edit().putString("sound_profile", p.name).apply()

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val sp = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()

            sp.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSounds.add(sampleId)
                } else {
                    Log.w(TAG, "Failed to load sample ID: $sampleId, status: $status")
                }
            }

            soundPool = sp
            loadAllSounds()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SoundPool: ${e.message}", e)
        }
    }

    fun loadAllSounds() {
        val sp = soundPool ?: return
        soundIdMap.clear()
        loadedSounds.clear()

        val customDir = File(context.filesDir, "custom_sounds")

        for (event in SoundEvent.values()) {
            val customFile = File(customDir, "${event.idKey}.wav")
            val soundId = if (customFile.exists() && customFile.length() > 0) {
                try {
                    sp.load(customFile.absolutePath, 1)
                } catch (e: Exception) {
                    sp.load(context, event.defaultRawRes, 1)
                }
            } else {
                sp.load(context, event.defaultRawRes, 1)
            }
            soundIdMap[event] = soundId
        }
    }

    fun isEventEnabled(event: SoundEvent): Boolean {
        return prefs.getBoolean("event_${event.idKey}", true)
    }

    fun setEventEnabled(event: SoundEvent, enabled: Boolean) {
        prefs.edit().putBoolean("event_${event.idKey}", enabled).apply()
    }

    fun play(event: SoundEvent) {
        if (!isMasterSoundEnabled) return
        if (!isEventEnabled(event)) return

        // Respect system silent mode
        val ringer = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        if (ringer == AudioManager.RINGER_MODE_SILENT) return

        val sp = soundPool ?: return
        val soundId = soundIdMap[event] ?: return

        val vol = (soundVolume * activeProfile.volumeMultiplier).coerceIn(0.05f, 1.0f)
        try {
            sp.play(soundId, vol, vol, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "Error playing sound for $event: ${e.message}")
        }
    }

    fun playPreview(event: SoundEvent) {
        val sp = soundPool ?: return
        val soundId = soundIdMap[event] ?: return
        val vol = (soundVolume * activeProfile.volumeMultiplier).coerceIn(0.05f, 1.0f)
        sp.play(soundId, vol, vol, 1, 0, 1.0f)
    }

    fun vibrate(durationMs: Long = 40) {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error vibrating: ${e.message}")
        }
    }

    fun vibratePattern(pattern: LongArray) {
        if (!isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error vibrating pattern: ${e.message}")
        }
    }

    /**
     * Sequentially tests all enabled sounds with 300ms pause.
     */
    fun testAllSounds(scope: CoroutineScope, onSoundTested: (SoundEvent) -> Unit) {
        scope.launch {
            for (event in SoundEvent.values()) {
                if (isEventEnabled(event)) {
                    playPreview(event)
                    vibrate(30)
                    onSoundTested(event)
                    delay(380)
                }
            }
        }
    }

    fun setCustomSound(event: SoundEvent, uri: Uri): Boolean {
        return try {
            val customDir = File(context.filesDir, "custom_sounds")
            if (!customDir.exists()) customDir.mkdirs()

            val targetFile = File(customDir, "${event.idKey}.wav")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Reload SoundPool
            soundPool?.unload(soundIdMap[event] ?: 0)
            val newId = soundPool?.load(targetFile.absolutePath, 1) ?: 0
            soundIdMap[event] = newId
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom sound: ${e.message}", e)
            false
        }
    }

    fun resetToDefaultSound(event: SoundEvent) {
        try {
            val customDir = File(context.filesDir, "custom_sounds")
            val targetFile = File(customDir, "${event.idKey}.wav")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            soundPool?.unload(soundIdMap[event] ?: 0)
            val newId = soundPool?.load(context, event.defaultRawRes, 1) ?: 0
            soundIdMap[event] = newId
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting sound: ${e.message}", e)
        }
    }

    fun resetAllSoundsToDefault() {
        try {
            val customDir = File(context.filesDir, "custom_sounds")
            if (customDir.exists()) {
                customDir.deleteRecursively()
            }
            activeProfile = SoundProfile.DAIRY_PRO
            soundVolume = 1.0f
            loadAllSounds()
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting all sounds: ${e.message}", e)
        }
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing SoundPool: ${e.message}")
        }
    }
}
