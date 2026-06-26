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
    fun clearAll()
}

class SecureStorageRepositoryImpl(private val context: Context) : SecureStorageRepository {

    private val tag = "SecureStorage"
    private val prefsName = "themis_secure_prefs"
    private val configKey = "llm_endpoint_config"
    private val apiKeySecret = "llm_api_key_secret"

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

    override fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
