package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cyber_api_keys", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GROQ = "groq_api_key"
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_CUSTOM = "custom_api_key"
        private const val KEY_CUSTOM_URL = "custom_api_url"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isHapticsEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    fun getGroqKey(): String {
        return prefs.getString(KEY_GROQ, "") ?: ""
    }

    fun saveGroqKey(key: String) {
        prefs.edit().putString(KEY_GROQ, key.trim()).apply()
    }

    fun getOpenRouterKey(): String {
        return prefs.getString(KEY_OPENROUTER, "") ?: ""
    }

    fun saveOpenRouterKey(key: String) {
        prefs.edit().putString(KEY_OPENROUTER, key.trim()).apply()
    }

    fun getCustomKey(): String {
        return prefs.getString(KEY_CUSTOM, "") ?: ""
    }

    fun saveCustomKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM, key.trim()).apply()
    }

    fun getCustomUrl(): String {
        return prefs.getString(KEY_CUSTOM_URL, "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions"
    }

    fun saveCustomUrl(url: String) {
        prefs.edit().putString(KEY_CUSTOM_URL, url.trim()).apply()
    }

    fun getGeminiKey(): String {
        // Fall back to BuildConfig.GEMINI_API_KEY if user-defined key is empty
        val userKey = prefs.getString(KEY_GEMINI, "") ?: ""
        if (userKey.isNotEmpty()) return userKey
        
        // Return system key from BuildConfig if available
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun getRawUserGeminiKey(): String {
        return prefs.getString(KEY_GEMINI, "") ?: ""
    }

    fun saveGeminiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun clearAllKeys() {
        prefs.edit().clear().apply()
    }
}
