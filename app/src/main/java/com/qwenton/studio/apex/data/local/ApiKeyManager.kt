package com.qwenton.studio.apex.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class ApiKeyManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, using fallback storage", e)
        context.getSharedPreferences("${PREFS_FILENAME}_fallback", Context.MODE_PRIVATE)
    }

    /**
     * Retrieves the current API key or generates a new secure one if not present.
     */
    fun getOrCreateApiKey(): String {
        val existing = prefs.getString(KEY_API_KEY, null)
        if (!existing.isNullOrBlank() && isValidKey(existing)) {
            return existing
        }
        val newKey = generateSecureApiKey()
        saveApiKey(newKey)
        return newKey
    }

    /**
     * Forces generation and storage of a new API key.
     */
    fun rotateApiKey(): String {
        val newKey = generateSecureApiKey()
        saveApiKey(newKey)
        return newKey
    }

    private fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    /**
     * Generates an API key formatted as: sk-qwenton-apex-<24-hex-chars>
     */
    private fun generateSecureApiKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(12) // 12 bytes = 24 hex chars
        random.nextBytes(bytes)
        val hexString = bytes.joinToString("") { "%02x".format(it) }
        return "$KEY_PREFIX$hexString"
    }

    /**
     * Validates that an API key matches the expected format sk-qwenton-apex-[0-9a-f]{24}
     */
    fun isValidKey(key: String): Boolean {
        val regex = Regex("^sk-qwenton-apex-[0-9a-fA-F]{24}$")
        return regex.matches(key)
    }

    companion object {
        private const val TAG = "ApiKeyManager"
        private const val PREFS_FILENAME = "qwenton_secure_prefs"
        private const val KEY_API_KEY = "current_api_key"
        private const val KEY_PREFIX = "sk-qwenton-apex-"
    }
}
