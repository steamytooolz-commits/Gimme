package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.LlmClient
import com.example.data.model.LlmEndpointConfig
import com.example.data.repository.SecureStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ConnectionStatus {
    object Idle : ConnectionStatus
    object Loading : ConnectionStatus
    data class Success(val models: List<String>) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

data class SettingsUiState(
    val config: LlmEndpointConfig = LlmEndpointConfig(
        providerName = "OpenAI",
        baseUrl = "https://api.openai.com/v1/",
        modelName = "gpt-4o",
        requiresApiKey = true
    ),
    val apiKey: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle,
    val fontSizeMultiplier: Float = 1.0f,
    val customTheme: String = "Auto",
    val adUnitId: String = "ca-app-pub-3940256099942544/6300978111",
    val useSimulatedAds: Boolean = false,
    val adsEnabled: Boolean = true
)

class SettingsViewModel(
    private val secureStorageRepository: SecureStorageRepository,
    private val llmClient: LlmClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val savedConfig = secureStorageRepository.getLlmConfig()
        val savedApiKey = secureStorageRepository.getApiKey()
        val savedFontSize = secureStorageRepository.getFontSizeMultiplier()
        val savedTheme = secureStorageRepository.getUiThemePreference()
        val savedAdUnitId = secureStorageRepository.getAdUnitId()
        val savedUseSimulatedAds = secureStorageRepository.getUseSimulatedAds()
        val savedAdsEnabled = secureStorageRepository.getAdsEnabled()
        _uiState.update {
            it.copy(
                config = savedConfig ?: it.config,
                apiKey = savedApiKey,
                fontSizeMultiplier = savedFontSize,
                customTheme = savedTheme,
                adUnitId = savedAdUnitId,
                useSimulatedAds = savedUseSimulatedAds,
                adsEnabled = savedAdsEnabled,
                connectionStatus = ConnectionStatus.Idle
            )
        }
    }

    fun updateConfig(config: LlmEndpointConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
    }

    fun updateFontSizeMultiplier(multiplier: Float) {
        _uiState.update { it.copy(fontSizeMultiplier = multiplier) }
        secureStorageRepository.saveFontSizeMultiplier(multiplier)
    }

    fun updateCustomTheme(theme: String) {
        _uiState.update { it.copy(customTheme = theme) }
        secureStorageRepository.saveUiThemePreference(theme)
    }

    fun updateAdUnitId(id: String) {
        _uiState.update { it.copy(adUnitId = id) }
        secureStorageRepository.saveAdUnitId(id)
    }

    fun updateUseSimulatedAds(use: Boolean) {
        _uiState.update { it.copy(useSimulatedAds = use) }
        secureStorageRepository.saveUseSimulatedAds(use)
    }

    fun updateAdsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(adsEnabled = enabled) }
        secureStorageRepository.saveAdsEnabled(enabled)
    }

    fun testConnection() {
        val currentState = _uiState.value
        _uiState.update { it.copy(connectionStatus = ConnectionStatus.Loading) }

        viewModelScope.launch {
            val result = llmClient.validateConnection(currentState.config, currentState.apiKey)
            result.onSuccess { models ->
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.Success(models)) }
            }
            result.onFailure { exception ->
                _uiState.update { 
                    it.copy(connectionStatus = ConnectionStatus.Error(exception.localizedMessage ?: "Unknown error occurred")) 
                }
            }
        }
    }

    fun saveSettings() {
        val currentState = _uiState.value
        secureStorageRepository.saveLlmConfig(currentState.config)
        secureStorageRepository.saveApiKey(currentState.apiKey)
        secureStorageRepository.saveFontSizeMultiplier(currentState.fontSizeMultiplier)
        secureStorageRepository.saveUiThemePreference(currentState.customTheme)
        secureStorageRepository.saveAdUnitId(currentState.adUnitId)
        secureStorageRepository.saveUseSimulatedAds(currentState.useSimulatedAds)
        secureStorageRepository.saveAdsEnabled(currentState.adsEnabled)
    }
}

class SettingsViewModelFactory(
    private val secureStorageRepository: SecureStorageRepository,
    private val llmClient: LlmClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(secureStorageRepository, llmClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
