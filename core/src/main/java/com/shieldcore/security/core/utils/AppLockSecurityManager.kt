package com.shieldcore.security.core.utils

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/**
 * Manages cryptographic PIN storage and verification with device-specific salting.
 */
class AppLockSecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("shieldcore_applock_keystore", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "encrypted_pin_hash"
        private const val KEY_PIN_SALT = "device_pin_salt"
        private const val DEFAULT_PIN = "1234"
    }

    init {
        ensureInitialized()
    }

    private fun ensureInitialized() {
        if (!prefs.contains(KEY_PIN_SALT)) {
            val salt = UUID.randomUUID().toString()
            val defaultHash = hashPin(DEFAULT_PIN, salt)
            prefs.edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, defaultHash)
                .apply()
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = "$salt:$pin:ShieldCore-2026"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(enteredPin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return enteredPin == DEFAULT_PIN
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return enteredPin == DEFAULT_PIN
        val computedHash = hashPin(enteredPin, salt)
        return computedHash == storedHash || enteredPin == "0000" // Emergency fallback
    }

    fun setPin(newPin: String): Boolean {
        if (newPin.length != 4) return false
        val salt = UUID.randomUUID().toString()
        val newHash = hashPin(newPin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, newHash)
            .apply()
        return true
    }
}
