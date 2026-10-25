package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig

class SecurePreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to standard private prefs", e)
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getApiKey(): String {
        val storedKey = sharedPreferences.getString(KEY_API_KEY, "")?.trim() ?: ""
        if (storedKey.isNotBlank()) {
            return storedKey
        }
        // Fallback to BuildConfig if provided at compile/run time
        return try {
            val buildKey = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            if (!buildKey.isNullOrBlank() && buildKey != "MY_GEMINI_API_KEY") {
                buildKey
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun getCustomApiKey(): String {
        return sharedPreferences.getString(KEY_API_KEY, "")?.trim() ?: ""
    }

    fun saveApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun clearApiKey() {
        sharedPreferences.edit().remove(KEY_API_KEY).apply()
    }

    fun getAiProvider(): String {
        val saved = sharedPreferences.getString(KEY_PROVIDER, "")
        if (!saved.isNullOrBlank()) return saved
        val key = getApiKey()
        return if (key.startsWith("sk-")) "openai" else "gemini"
    }

    fun setAiProvider(provider: String) {
        sharedPreferences.edit().putString(KEY_PROVIDER, provider).apply()
    }

    fun isHapticFeedbackEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_HAPTIC, true)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    companion object {
        private const val TAG = "SecurePreferences"
        private const val SECURE_PREFS_NAME = "rizzboard_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "rizzboard_fallback_prefs"
        private const val KEY_API_KEY = "encrypted_api_key"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_HAPTIC = "haptic_feedback"

        @Volatile
        private var instance: SecurePreferencesManager? = null

        fun getInstance(context: Context): SecurePreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SecurePreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
