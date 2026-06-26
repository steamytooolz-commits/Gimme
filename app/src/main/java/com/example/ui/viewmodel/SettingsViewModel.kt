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
    val connectionStatus: ConnectionStatus = ConnectionStatus.Idle
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
        if (savedConfig != null) {
            _uiState.update {
                it.copy(
                    config = savedConfig,
                    apiKey = savedApiKey,
                    connectionStatus = ConnectionStatus.Idle
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    apiKey = savedApiKey,
                    connectionStatus = ConnectionStatus.Idle
                )
            }
        }
    }

    fun updateConfig(config: LlmEndpointConfig) {
        _uiState.update { it.copy(config = config) }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
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
