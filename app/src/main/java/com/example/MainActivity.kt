package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.example.api.OpenAiCompatibleLlmClient
import com.example.data.local.ThemisDatabase
import com.example.data.repository.GameRepository
import com.example.data.repository.SecureStorageRepositoryImpl
import com.example.ui.components.ThemisGameLayout
import com.example.ui.theme.ThemisTheme
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SettingsViewModelFactory
import com.example.ui.viewmodel.ThemisViewModel
import com.example.ui.viewmodel.ThemisViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var database: ThemisDatabase
    private lateinit var repository: GameRepository
    private lateinit var secureStorageRepository: SecureStorageRepositoryImpl
    private lateinit var llmClient: OpenAiCompatibleLlmClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve singletons from custom Application class (Clean Service Locator pattern)
        val app = application as ThemisApplication
        database = app.database
        repository = app.repository
        secureStorageRepository = app.secureStorageRepository
        llmClient = app.llmClient

        // 2. Instantiate ViewModels using their Factories
        val themisFactory = ThemisViewModelFactory(application, repository, secureStorageRepository, llmClient)
        val viewModel: ThemisViewModel by viewModels { themisFactory }

        val settingsFactory = SettingsViewModelFactory(secureStorageRepository, llmClient)
        val settingsViewModel: SettingsViewModel by viewModels { settingsFactory }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val settingsUiState by settingsViewModel.uiState.collectAsState()

            ThemisTheme(
                phase = uiState.currentPhase,
                fontSizeMultiplier = settingsUiState.fontSizeMultiplier,
                customTheme = settingsUiState.customTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ThemisGameLayout(
                        uiState = uiState,
                        onIntent = { intent -> viewModel.processIntent(intent) },
                        settingsUiState = settingsUiState,
                        onConfigChange = { settingsViewModel.updateConfig(it) },
                        onApiKeyChange = { settingsViewModel.updateApiKey(it) },
                        onTestConnection = { settingsViewModel.testConnection() },
                        onSaveSettings = { settingsViewModel.saveSettings() },
                        onFontSizeChange = { settingsViewModel.updateFontSizeMultiplier(it) },
                        onThemeChange = { settingsViewModel.updateCustomTheme(it) },
                        onAdUnitIdChange = { settingsViewModel.updateAdUnitId(it) },
                        onUseSimulatedAdsChange = { settingsViewModel.updateUseSimulatedAds(it) },
                        onAdsEnabledChange = { settingsViewModel.updateAdsEnabled(it) }
                    )
                }
            }
        }
    }
}
