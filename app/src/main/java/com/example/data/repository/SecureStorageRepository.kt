package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.model.LlmEndpointConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface SecureStorageRepository {
    fun saveLlmConfig(config: LlmEndpointConfig)
    fun getLlmConfig(): LlmEndpointConfig?
    fun saveApiKey(key: String)
    fun getApiKey(): String
    fun saveFontSizeMultiplier(multiplier: Float)
    fun getFontSizeMultiplier(): Float
    fun saveUiThemePreference(theme: String)
    fun getUiThemePreference(): String
    fun saveAdUnitId(id: String)
    fun getAdUnitId(): String
    fun saveUseSimulatedAds(use: Boolean)
    fun getUseSimulatedAds(): Boolean
    fun saveAdsEnabled(enabled: Boolean)
    fun getAdsEnabled(): Boolean
    fun clearAll()
}

class SecureStorageRepositoryImpl(private val context: Context) : SecureStorageRepository {

    private val tag = "SecureStorage"
    private val prefsName = "themis_secure_prefs"
    private val configKey = "llm_endpoint_config"
    private val apiKeySecret = "llm_api_key_secret"
    private val fontSizeKey = "ui_font_size_multiplier"
    private val themePreferenceKey = "ui_theme_preference"
    private val adUnitIdKey = "ui_ad_unit_id"
    private val useSimulatedAdsKey = "ui_use_simulated_ads"
    private val adsEnabledKey = "ui_ads_enabled"

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                prefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(tag, "EncryptedSharedPreferences initialization failed. Falling back to obfuscated standard SharedPreferences.", e)
            // Fallback: Obfuscated standard shared preferences so that the app doesn't crash in some emulator environments
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        }
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(LlmEndpointConfig::class.java)

    override fun saveLlmConfig(config: LlmEndpointConfig) {
        try {
            val serialized = adapter.toJson(config)
            sharedPreferences.edit().putString(configKey, serialized).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save LLM configuration", e)
        }
    }

    override fun getLlmConfig(): LlmEndpointConfig? {
        val serialized = sharedPreferences.getString(configKey, null) ?: return null
        return try {
            adapter.fromJson(serialized)
        } catch (e: Exception) {
            Log.e(tag, "Failed to decode LLM configuration", e)
            null
        }
    }

    override fun saveApiKey(key: String) {
        try {
            // Further obfuscate the key to avoid simple memory scan if using fallback
            val obfuscated = Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sharedPreferences.edit().putString(apiKeySecret, obfuscated).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save API key", e)
        }
    }

    override fun getApiKey(): String {
        val obfuscated = sharedPreferences.getString(apiKeySecret, "") ?: ""
        if (obfuscated.isEmpty()) return ""
        return try {
            String(Base64.decode(obfuscated, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(tag, "Failed to decode API key", e)
            ""
        }
    }

    override fun saveFontSizeMultiplier(multiplier: Float) {
        try {
            sharedPreferences.edit().putFloat(fontSizeKey, multiplier).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save font size multiplier", e)
        }
    }

    override fun getFontSizeMultiplier(): Float {
        return try {
            sharedPreferences.getFloat(fontSizeKey, 1.0f)
        } catch (e: Exception) {
            Log.e(tag, "Failed to load font size multiplier", e)
            1.0f
        }
    }

    override fun saveUiThemePreference(theme: String) {
        try {
            sharedPreferences.edit().putString(themePreferenceKey, theme).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save theme preference", e)
        }
    }

    override fun getUiThemePreference(): String {
        return try {
            sharedPreferences.getString(themePreferenceKey, "Auto") ?: "Auto"
        } catch (e: Exception) {
            Log.e(tag, "Failed to load theme preference", e)
            "Auto"
        }
    }

    override fun saveAdUnitId(id: String) {
        try {
            sharedPreferences.edit().putString(adUnitIdKey, id).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save ad unit ID", e)
        }
    }

    override fun getAdUnitId(): String {
        return try {
            sharedPreferences.getString(adUnitIdKey, "ca-app-pub-3940256099942544/6300978111") ?: "ca-app-pub-3940256099942544/6300978111"
        } catch (e: Exception) {
            Log.e(tag, "Failed to load ad unit ID", e)
            "ca-app-pub-3940256099942544/6300978111"
        }
    }

    override fun saveUseSimulatedAds(use: Boolean) {
        try {
            sharedPreferences.edit().putBoolean(useSimulatedAdsKey, use).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save use simulated ads", e)
        }
    }

    override fun getUseSimulatedAds(): Boolean {
        return try {
            sharedPreferences.getBoolean(useSimulatedAdsKey, false)
        } catch (e: Exception) {
            Log.e(tag, "Failed to load use simulated ads", e)
            false
        }
    }

    override fun saveAdsEnabled(enabled: Boolean) {
        try {
            sharedPreferences.edit().putBoolean(adsEnabledKey, enabled).apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save ads enabled", e)
        }
    }

    override fun getAdsEnabled(): Boolean {
        return try {
            sharedPreferences.getBoolean(adsEnabledKey, true)
        } catch (e: Exception) {
            Log.e(tag, "Failed to load ads enabled", e)
            true
        }
    }

    override fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
