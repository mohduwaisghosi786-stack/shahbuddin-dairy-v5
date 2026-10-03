package com.shahbuddindairy.app.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

/**
 * AppLockManager provides a secure, lightweight local PIN App Lock.
 *
 * CRITICAL RULE:
 * App Lock is strictly OFF by default.
 * First launch on V8.0 opens normally without blocking or forcing PIN setup.
 *
 * PIN is never stored in plain text. A salted SHA-256 hash is used.
 */
class AppLockManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isAppLockEnabled = MutableStateFlow(isLockConfigured())
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    // Session unlock state: if lock is enabled, app starts locked; if lock is disabled, app starts unlocked.
    private val _isUnlocked = MutableStateFlow(!isLockConfigured())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L

    fun isLockConfigured(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ENABLED, false) && prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val inputHash = hashPin(inputPin, storedSalt)
        return storedHash == inputHash
    }

    fun unlockWithPin(inputPin: String): Boolean {
        if (verifyPin(inputPin)) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun setPin(newPin: String): Boolean {
        if (newPin.length < 4) return false
        val salt = UUID.randomUUID().toString()
        val hash = hashPin(newPin, salt)
        prefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, true)
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
        _isAppLockEnabled.value = true
        _isUnlocked.value = true
        return true
    }

    fun disableLock(currentPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        prefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
        _isAppLockEnabled.value = false
        _isUnlocked.value = true
        return true
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        return setPin(newPin)
    }

    fun onAppMovedToBackground() {
        if (isLockConfigured()) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    fun onAppMovedToForeground() {
        if (isLockConfigured()) {
            // Lock if backgrounded for more than 15 seconds
            val now = System.currentTimeMillis()
            if (now - backgroundTimestamp > LOCK_TIMEOUT_MS) {
                _isUnlocked.value = false
            }
        }
    }

    fun lockNow() {
        if (isLockConfigured()) {
            _isUnlocked.value = false
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = "$salt:$pin:shahbuddin_v8"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "shahbuddin_security_prefs"
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_PIN_HASH = "app_pin_hash"
        private const val KEY_PIN_SALT = "app_pin_salt"
        private const val LOCK_TIMEOUT_MS = 15_000L // 15 seconds grace period

        @Volatile
        private var INSTANCE: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLockManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
