package com.shahbuddindairy.app.gesture

import android.content.Context

class GestureSettingsManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "sd_gesture_prefs"

        @Volatile
        private var instance: GestureSettingsManager? = null

        fun getInstance(context: Context): GestureSettingsManager {
            return instance ?: synchronized(this) {
                instance ?: GestureSettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMasterGestureEnabled: Boolean
        get() = prefs.getBoolean("master_gesture_enabled", true)
        set(v) = prefs.edit().putBoolean("master_gesture_enabled", v).apply()

    var isSwipeDownDeliverEnabled: Boolean
        get() = prefs.getBoolean("swipe_down_deliver", true)
        set(v) = prefs.edit().putBoolean("swipe_down_deliver", v).apply()

    var isSwipeUpNaagaEnabled: Boolean
        get() = prefs.getBoolean("swipe_up_naaga", true)
        set(v) = prefs.edit().putBoolean("swipe_up_naaga", v).apply()

    var isSwipeLeftPrevEnabled: Boolean
        get() = prefs.getBoolean("swipe_left_prev", true)
        set(v) = prefs.edit().putBoolean("swipe_left_prev", v).apply()

    var isSwipeRightNextEnabled: Boolean
        get() = prefs.getBoolean("swipe_right_next", true)
        set(v) = prefs.edit().putBoolean("swipe_right_next", v).apply()

    var sensitivityThresholdPx: Float
        get() = prefs.getFloat("gesture_sensitivity_px", 75f)
        set(v) = prefs.edit().putFloat("gesture_sensitivity_px", v.coerceIn(35f, 150f)).apply()

    var isGestureSoundEnabled: Boolean
        get() = prefs.getBoolean("gesture_sound", true)
        set(v) = prefs.edit().putBoolean("gesture_sound", v).apply()

    var isGestureHapticEnabled: Boolean
        get() = prefs.getBoolean("gesture_haptic", true)
        set(v) = prefs.edit().putBoolean("gesture_haptic", v).apply()

    var showGestureGuide: Boolean
        get() = prefs.getBoolean("show_gesture_guide", true)
        set(v) = prefs.edit().putBoolean("show_gesture_guide", v).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
