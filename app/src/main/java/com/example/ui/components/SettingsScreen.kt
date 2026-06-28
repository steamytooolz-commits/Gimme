package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.LiquidOnDeviceSdk
import com.example.data.model.LlmEndpointConfig
import kotlinx.coroutines.launch
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkTerminalBg
import com.example.ui.theme.ParchmentBg
import com.example.ui.theme.WarmWoodBrown
import com.example.ui.viewmodel.ConnectionStatus
import com.example.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onConfigChange: (LlmEndpointConfig) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveSettings: () -> Unit,
    isDark: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onThemeChange: (String) -> Unit,
    onAdUnitIdChange: (String) -> Unit,
    onUseSimulatedAdsChange: (Boolean) -> Unit,
    onAdsEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var hasUserEditedSinceTest by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val lfmStatus by LiquidOnDeviceSdk.status.collectAsState()
    val lfmProgress by LiquidOnDeviceSdk.downloadProgress.collectAsState()
    val lfmSpeed by LiquidOnDeviceSdk.downloadSpeed.collectAsState()
    val lfmEta by LiquidOnDeviceSdk.downloadEta.collectAsState()
    val downloadedFiles by LiquidOnDeviceSdk.downloadedFiles.collectAsState()
    val activeModelFileName by LiquidOnDeviceSdk.activeModelFileName.collectAsState()
    val activeModelInfo by LiquidOnDeviceSdk.activeModelInfo.collectAsState()
    val verificationStates by LiquidOnDeviceSdk.verificationStates.collectAsState()

    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var useCustomUrl by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }
    var customFileNameInput by remember { mutableStateOf("") }
    var customSizeInput by remember { mutableStateOf("175") }

    var hfRepoInput by remember { mutableStateOf("mradermacher/LFM2.5-1.2B-Instruct-GGUF") }
    var hfGgufFiles by remember { mutableStateOf<List<LiquidOnDeviceSdk.ModelMetadata>>(emptyList()) }
    var isSearchingHf by remember { mutableStateOf(false) }
    var hfSearchError by remember { mutableStateOf("") }
    var showHfDropdown by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var localTestPrompt by remember { mutableStateOf("Verify if suspect Jax has a valid alibi for central magistrate breach.") }
    var localTestResponse by remember { mutableStateOf("") }
    var isLocalTesting by remember { mutableStateOf(false) }

    LaunchedEffect(downloadedFiles) {
        downloadedFiles.forEach { file ->
            if (!verificationStates.containsKey(file.name)) {
                scope.launch {
                    LiquidOnDeviceSdk.verifyFileIntegrity(context, file)
                }
            }
        }
    }

    LaunchedEffect(uiState.config.providerName) {
        if (uiState.config.providerName == "Liquid LFM On-Device SDK") {
            LiquidOnDeviceSdk.checkStatus(context)
        }
    }

    val presets = listOf(
        "OpenAI",
        "OpenRouter",
        "Anthropic",
        "G4F",
        "Liquid AI Developer API",
        "Liquid LFM (Local Ollama)",
        "Liquid LFM On-Device SDK",
        "Custom / Local (Ollama)"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkTerminalBg else ParchmentBg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Screen Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isDark) AmberAccent else WarmWoodBrown,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BYOK LLM CONFIGURATION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }

            Text(
                text = "Configure your own API keys or custom endpoints (Ollama, LM Studio, etc.) for local or private LLM routing.",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Preset Provider Selector
            Text(
                text = "Preset LLM Provider:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) AmberAccent else WarmWoodBrown,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Segmented or List Preset Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = when (preset) {
                        "OpenAI" -> uiState.config.providerName == "OpenAI" && uiState.config.baseUrl.contains("openai")
                        "OpenRouter" -> uiState.config.providerName == "OpenRouter" && uiState.config.baseUrl.contains("openrouter")
                        "Anthropic" -> uiState.config.providerName == "Anthropic" && uiState.config.baseUrl.contains("anthropic")
                        "G4F" -> uiState.config.providerName == "G4F" && uiState.config.baseUrl.contains("g4f")
                        "Liquid AI Developer API" -> uiState.config.providerName == "Liquid AI Developer API"
                        "Liquid LFM (Local Ollama)" -> uiState.config.providerName == "Liquid LFM (Local Ollama)"
                        "Liquid LFM On-Device SDK" -> uiState.config.providerName == "Liquid LFM On-Device SDK"
                        else -> uiState.config.providerName == "Custom / Local (Ollama)" || (!uiState.config.baseUrl.contains("openai") && !uiState.config.baseUrl.contains("openrouter") && !uiState.config.baseUrl.contains("anthropic") && !uiState.config.baseUrl.contains("g4f") && !uiState.config.baseUrl.contains("liquid") && uiState.config.providerName != "Liquid LFM (Local Ollama)" && uiState.config.providerName != "Liquid LFM On-Device SDK")
                    }

                    OutlinedButton(
                        onClick = {
                            hasUserEditedSinceTest = true
                            when (preset) {
                                "OpenAI" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "OpenAI",
                                        baseUrl = "https://api.openai.com/v1/",
                                        modelName = "gpt-4o-mini",
                                        requiresApiKey = true
                                    )
                                )
                                "OpenRouter" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "OpenRouter",
                                        baseUrl = "https://openrouter.ai/api/v1/",
                                        modelName = "google/gemini-flash-1.5",
                                        requiresApiKey = true
                                    )
                                )
                                "Anthropic" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "Anthropic",
                                        baseUrl = "https://api.anthropic.com/v1/",
                                        modelName = "claude-3-5-sonnet-20241022",
                                        requiresApiKey = true
                                    )
                                )
                                "G4F" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "G4F",
                                        baseUrl = "https://g4f.space/v1/",
                                        modelName = "gpt-4o",
                                        requiresApiKey = false
                                    )
                                )
                                "Liquid AI Developer API" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "Liquid AI Developer API",
                                        baseUrl = "https://api.liquid.ai/v1/",
                                        modelName = "liquid-lfm-3b",
                                        requiresApiKey = true
                                    )
                                )
                                "Liquid LFM (Local Ollama)" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "Liquid LFM (Local Ollama)",
                                        baseUrl = "http://10.0.2.2:11434/v1/",
                                        modelName = "lfm2.5:350m",
                                        requiresApiKey = false
                                    )
                                )
                                "Liquid LFM On-Device SDK" -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "Liquid LFM On-Device SDK",
                                        baseUrl = "liquid://on-device",
                                        modelName = "lfm2.5:350m",
                                        requiresApiKey = false
                                    )
                                )
                                else -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "Custom / Local (Ollama)",
                                        baseUrl = "http://10.0.2.2:11434/v1/",
                                        modelName = "lfm2.5",
                                        requiresApiKey = false
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) {
                                if (isDark) Color(0xFF222226) else WarmWoodBrown.copy(alpha = 0.1f)
                            } else Color.Transparent,
                            contentColor = if (isDark) Color.White else Color.Black
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, if (isDark) AmberAccent else WarmWoodBrown)
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.DarkGray else Color.LightGray)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("preset_${preset.replace(" ", "_").lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = preset, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isDark) AmberAccent else WarmWoodBrown,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.config.providerName == "Liquid LFM On-Device SDK") {
                // Render advanced Liquid LFM Model Controller & Downloader UI
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF16161B) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF2A2A30) else Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Model Info",
                                tint = if (isDark) AmberAccent else WarmWoodBrown,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIQUID LFM LOCAL PORTAL",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isDark) AmberAccent else WarmWoodBrown
                            )
                        }

                        Text(
                            text = "Download any LFM model directly into your device's public Downloads directory or app sandbox. You can manage multiple weights on disk and load any model into RAM for local offline game-logic evaluation.",
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        HorizontalDivider(
                            color = if (isDark) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // SECTION 1: DOWNLOAD A NEW MODEL
                        Text(
                            text = "1. DOWNLOAD & ADD NEW WEIGHTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) AmberAccent else WarmWoodBrown,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Preset Selection Rows
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Checkbox(
                                checked = useCustomUrl,
                                onCheckedChange = { useCustomUrl = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isDark) AmberAccent else WarmWoodBrown
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Use Custom HuggingFace / HTTP URL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }

                        if (!useCustomUrl) {
                            // Render presets dropdown
                            Text(
                                text = "Select Preset Model:",
                                fontSize = 11.sp,
                                color = if (isDark) Color.Gray else Color.DarkGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LiquidOnDeviceSdk.PRESETS.forEachIndexed { idx, preset ->
                                val isPresetSelected = idx == selectedPresetIndex
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPresetSelected) {
                                            if (isDark) Color(0xFF23232A) else Color(0xFFF1F1F1)
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isPresetSelected) (if (isDark) AmberAccent else WarmWoodBrown) else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clickable { selectedPresetIndex = idx }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isPresetSelected,
                                            onClick = { selectedPresetIndex = idx },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = if (isDark) AmberAccent else WarmWoodBrown
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = preset.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            Text(
                                                text = preset.description,
                                                fontSize = 11.sp,
                                                color = if (isDark) Color.LightGray else Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // HuggingFace Repo Search / Direct Sideload Selector
                            Text(
                                text = "HUGGING FACE MODEL HUB DIRECT LOOKUP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) AmberAccent else WarmWoodBrown,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = hfRepoInput,
                                    onValueChange = { hfRepoInput = it },
                                    singleLine = true,
                                    label = { Text("HF Repo ID (e.g. liquidai/LFM2.5-230M-Instruct)", fontSize = 10.sp) },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                                        unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                                        focusedTextColor = if (isDark) Color.White else Color.Black,
                                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        isSearchingHf = true
                                        hfSearchError = ""
                                        scope.launch {
                                            try {
                                                val files = LiquidOnDeviceSdk.fetchRepoGgufFiles(hfRepoInput.trim())
                                                if (files.isNotEmpty()) {
                                                    hfGgufFiles = files
                                                    showHfDropdown = true
                                                } else {
                                                    hfSearchError = "No GGUF models found in this repo."
                                                }
                                            } catch (e: Exception) {
                                                hfSearchError = "Error: ${e.localizedMessage}"
                                            } finally {
                                                isSearchingHf = false
                                            }
                                        }
                                    },
                                    enabled = !isSearchingHf && hfRepoInput.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                                        contentColor = if (isDark) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    if (isSearchingHf) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = if (isDark) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text("Search Hub", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (hfSearchError.isNotEmpty()) {
                                Text(
                                    text = hfSearchError,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }

                            if (showHfDropdown && hfGgufFiles.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF141419) else Color(0xFFF0F0F5)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Select GGUF file from HuggingFace:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color.Black,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        hfGgufFiles.forEach { fileInfo ->
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        customUrlInput = fileInfo.downloadUrl
                                                        customFileNameInput = fileInfo.fileName
                                                        customSizeInput = (fileInfo.sizeBytes / (1024 * 1024)).toString()
                                                        showHfDropdown = false
                                                    }
                                                    .padding(vertical = 6.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(fileInfo.fileName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)
                                                    Text("SHA-256: " + if (fileInfo.sha256.isNotEmpty()) fileInfo.sha256.take(16) + "..." else "N/A", fontSize = 9.sp, color = Color.Gray)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = String.format("%.1f MB", fileInfo.sizeBytes / (1024f * 1024f)),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) AmberAccent else WarmWoodBrown
                                                )
                                            }
                                            HorizontalDivider(color = if (isDark) Color.DarkGray else Color.LightGray)
                                        }
                                    }
                                }
                            }

                            // Custom input fields
                            Text(
                                text = "Enter Custom Download URL:",
                                fontSize = 11.sp,
                                color = if (isDark) Color.Gray else Color.DarkGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                singleLine = true,
                                placeholder = { Text("https://example.com/lfm.gguf") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                                    unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                                    focusedTextColor = if (isDark) Color.White else Color.Black,
                                    unfocusedTextColor = if (isDark) Color.White else Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = "File Name (e.g. custom.gguf):",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.Gray else Color.DarkGray,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = customFileNameInput,
                                        onValueChange = { customFileNameInput = it },
                                        singleLine = true,
                                        placeholder = { Text("model.gguf") },
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                                            focusedTextColor = if (isDark) Color.White else Color.Black
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Model Size (MB):",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.Gray else Color.DarkGray,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    OutlinedTextField(
                                        value = customSizeInput,
                                        onValueChange = { customSizeInput = it },
                                        singleLine = true,
                                        placeholder = { Text("175") },
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                                            focusedTextColor = if (isDark) Color.White else Color.Black
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Download trigger / Progress area
                        if (lfmStatus == LiquidOnDeviceSdk.ModelStatus.DOWNLOADING) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "Downloading Model Layer...",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                    Text(
                                        text = "${(lfmProgress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) AmberAccent else WarmWoodBrown
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { lfmProgress },
                                    color = if (isDark) AmberAccent else WarmWoodBrown,
                                    trackColor = if (isDark) Color(0xFF2A2A30) else Color.LightGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Speed: $lfmSpeed",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.Gray else Color.DarkGray
                                    )
                                    Text(
                                        text = lfmEta,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.Gray else Color.DarkGray
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { LiquidOnDeviceSdk().cancelDownload() },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("cancel_lfm_download_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Cancel Transfer", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val sdk = LiquidOnDeviceSdk()
                                        if (useCustomUrl) {
                                            if (customUrlInput.isNotEmpty()) {
                                                val size = customSizeInput.toIntOrNull() ?: 175
                                                sdk.startDownload(
                                                    context = context,
                                                    modelUrl = customUrlInput,
                                                    customFileName = customFileNameInput,
                                                    sizeMb = size
                                                )
                                            }
                                        } else {
                                            val preset = LiquidOnDeviceSdk.PRESETS[selectedPresetIndex]
                                            sdk.startDownload(
                                                context = context,
                                                modelUrl = preset.url,
                                                customFileName = preset.fileName,
                                                sizeMb = preset.sizeMb
                                            )
                                        }
                                    }
                                },
                                enabled = !useCustomUrl || customUrlInput.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                                    contentColor = if (isDark) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .padding(top = 10.dp)
                                    .testTag("download_lfm_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Model Weights Download",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(
                            color = if (isDark) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        // SECTION 2: WEIGHTS STORED ON LOCAL DEVICE
                        Text(
                            text = "2. LOCAL WEIGHTS ON STORAGE DISK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) AmberAccent else WarmWoodBrown,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        if (downloadedFiles.isEmpty()) {
                            Surface(
                                color = if (isDark) Color(0xFF0F0F12) else Color(0xFFF9F9F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.DarkGray else Color.LightGray),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "No weights detected on disk. Select a model preset or custom URL above to download weights (weights will be saved directly to your device's Downloads folder).",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.Gray else Color.DarkGray,
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        } else {
                            downloadedFiles.forEach { file ->
                                val isActive = file.name == activeModelFileName
                                val isLoaded = lfmStatus == LiquidOnDeviceSdk.ModelStatus.LOADED && isActive
                                val isThisLoading = lfmStatus == LiquidOnDeviceSdk.ModelStatus.LOADING && isActive

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) {
                                            if (isDark) Color(0xFF1B2F20) else Color(0xFFE8F5E9)
                                        } else {
                                            if (isDark) Color(0xFF0D0D11) else Color(0xFFFAFAFA)
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isActive) Color.Green else (if (isDark) Color.DarkGray else Color.LightGray)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = file.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color.White else Color.Black
                                                )
                                                Text(
                                                    text = "Path: ${file.absolutePath}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isDark) Color.Gray else Color.DarkGray,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Text(
                                                    text = "Size: ${String.format("%.1f MB", file.length() / (1024f * 1024f).coerceAtLeast(1.0f))}",
                                                    fontSize = 10.sp,
                                                    color = if (isDark) Color.LightGray else Color.DarkGray,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )

                                                val vState = verificationStates[file.name]
                                                if (vState != null) {
                                                    Column(modifier = Modifier.padding(top = 4.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "HF Status: ",
                                                                fontSize = 10.sp,
                                                                color = if (isDark) Color.Gray else Color.DarkGray
                                                            )
                                                            Text(
                                                                text = vState.status,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = when {
                                                                    vState.status.contains("Verified ✅") -> Color.Green
                                                                    vState.status.contains("Verified ⚠️") -> if (isDark) AmberAccent else WarmWoodBrown
                                                                    vState.status.contains("Corrupt ❌") -> Color.Red
                                                                    vState.status.contains("Verifying") || vState.status.contains("Computing") -> if (isDark) AmberAccent else WarmWoodBrown
                                                                    else -> Color.Gray
                                                                }
                                                            )
                                                        }
                                                        if (vState.progress in 0.01f..0.99f) {
                                                            LinearProgressIndicator(
                                                                progress = { vState.progress },
                                                                color = if (isDark) AmberAccent else WarmWoodBrown,
                                                                trackColor = Color.DarkGray,
                                                                modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp)
                                                            )
                                                        }
                                                        if (vState.details.isNotEmpty()) {
                                                            Text(
                                                                text = vState.details,
                                                                fontSize = 9.sp,
                                                                color = if (isDark) Color.LightGray else Color.DarkGray,
                                                                modifier = Modifier.padding(top = 2.dp),
                                                                lineHeight = 12.sp
                                                            )
                                                        }
                                                        if (vState.status.contains("Unverified ℹ️")) {
                                                            var manualVerifyRepoId by remember { mutableStateOf("") }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = manualVerifyRepoId,
                                                                    onValueChange = { manualVerifyRepoId = it },
                                                                    singleLine = true,
                                                                    placeholder = { Text("repo/name (e.g. liquidai/LFM2.5-230M-Instruct)", fontSize = 8.sp) },
                                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp),
                                                                    colors = OutlinedTextFieldDefaults.colors(
                                                                        focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                                                                        unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                                                                        focusedTextColor = if (isDark) Color.White else Color.Black,
                                                                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                                                                    ),
                                                                    modifier = Modifier.weight(1f).height(32.dp)
                                                                )
                                                                Button(
                                                                    onClick = {
                                                                        if (manualVerifyRepoId.isNotEmpty()) {
                                                                            scope.launch {
                                                                                LiquidOnDeviceSdk.verifyFileIntegrity(context, file, manualVerifyRepoId.trim())
                                                                            }
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                                                                        contentColor = if (isDark) Color.Black else Color.White
                                                                    ),
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                                                    modifier = Modifier.height(32.dp)
                                                                ) {
                                                                    Text("Verify", fontSize = 8.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                LiquidOnDeviceSdk.verifyFileIntegrity(context, file)
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color.Transparent,
                                                            contentColor = if (isDark) AmberAccent else WarmWoodBrown
                                                        ),
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("Verify with HuggingFace Hub", fontSize = 9.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    LiquidOnDeviceSdk().deleteModel(context, file.name)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Model Weights",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isLoaded) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Active",
                                                        tint = Color.Green,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Active & Loaded in RAM",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Green
                                                    )
                                                }
                                            } else if (isThisLoading) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        color = if (isDark) AmberAccent else WarmWoodBrown,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Loading neural weights...",
                                                        fontSize = 11.sp,
                                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                                    )
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            LiquidOnDeviceSdk.setActiveModel(context, file.name)
                                                            LiquidOnDeviceSdk().initialize(context, file.name)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isDark) Color(0xFF2A2A30) else Color.LightGray,
                                                        contentColor = if (isDark) Color.White else Color.Black
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .height(28.dp)
                                                        .testTag("load_model_${file.name}")
                                                ) {
                                                    Text(text = "Load weights into system RAM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Playgrounds & Active state Info
                        if (lfmStatus == LiquidOnDeviceSdk.ModelStatus.LOADED && activeModelFileName.isNotEmpty()) {
                            HorizontalDivider(
                                color = if (isDark) Color.DarkGray else Color.LightGray,
                                modifier = Modifier.padding(vertical = 14.dp)
                            )

                            // Local Testing Area / Playground!
                            Text(
                                text = "Test Local Inference Playground ($activeModelFileName):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) AmberAccent else WarmWoodBrown,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = localTestPrompt,
                                onValueChange = { localTestPrompt = it },
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                                    unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                                    focusedTextColor = if (isDark) Color.White else Color.Black,
                                    unfocusedTextColor = if (isDark) Color.White else Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    isLocalTesting = true
                                    localTestResponse = ""
                                    scope.launch {
                                        try {
                                            LiquidOnDeviceSdk().streamCompletion(context, localTestPrompt).collect { chunk ->
                                                localTestResponse += chunk
                                            }
                                        } catch (e: Exception) {
                                            localTestResponse = "Error: ${e.localizedMessage}"
                                        } finally {
                                            isLocalTesting = false
                                        }
                                    }
                                },
                                enabled = !isLocalTesting && localTestPrompt.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                                    contentColor = if (isDark) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                if (isLocalTesting) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = if (isDark) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Processing neural layers...", fontSize = 12.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Test",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Run On-Device Inference", fontSize = 12.sp)
                                }
                            }

                            if (localTestResponse.isNotEmpty()) {
                                Surface(
                                    color = if (isDark) Color(0xFF0C0C0E) else Color(0xFFF1F1F1),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isDark) Color(0xFF1C1C22) else Color.LightGray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Text(
                                        text = localTestResponse,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color.Green else Color.Black,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Endpoint Base URL
                Text(
                    text = "API Base URL:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) AmberAccent else WarmWoodBrown,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = uiState.config.baseUrl,
                    onValueChange = {
                        hasUserEditedSinceTest = true
                        onConfigChange(uiState.config.copy(baseUrl = it))
                    },
                    singleLine = true,
                    placeholder = { Text("https://api.openai.com/v1/") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                        unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("input_base_url")
                )

                // Model Name
                Text(
                    text = "Model Name / ID:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) AmberAccent else WarmWoodBrown,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = uiState.config.modelName,
                    onValueChange = {
                        hasUserEditedSinceTest = true
                        onConfigChange(uiState.config.copy(modelName = it))
                    },
                    singleLine = true,
                    placeholder = { Text("gpt-4o-mini") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                        unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("input_model_name")
                )

                // Requires API Key Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Checkbox(
                        checked = uiState.config.requiresApiKey,
                        onCheckedChange = {
                            hasUserEditedSinceTest = true
                            onConfigChange(uiState.config.copy(requiresApiKey = it))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (isDark) AmberAccent else WarmWoodBrown,
                            checkmarkColor = if (isDark) Color.Black else Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Requires API Key Verification",
                        fontSize = 13.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                // API Key Input
                if (uiState.config.requiresApiKey) {
                    Text(
                        text = "API Key / Auth Token:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) AmberAccent else WarmWoodBrown,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = {
                            hasUserEditedSinceTest = true
                            onApiKeyChange(it)
                        },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        placeholder = { Text("sk-proj-...") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                            unfocusedBorderColor = if (isDark) Color.DarkGray else Color.Gray,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.Info else Icons.Default.Lock,
                                    contentDescription = if (isApiKeyVisible) "Hide Password" else "Show Password",
                                    tint = if (isDark) Color.Gray else Color.DarkGray
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .testTag("input_api_key")
                    )
                }

                // Test Connection Status Card
                Surface(
                    color = if (isDark) Color(0xFF16161B) else Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Connection Status",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.Gray else Color.DarkGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        when (val status = uiState.connectionStatus) {
                            is ConnectionStatus.Idle -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Untested",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Not tested yet. Please trigger a connection check.",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                            is ConnectionStatus.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = if (isDark) AmberAccent else WarmWoodBrown,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Contacting remote host endpoint...",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                            is ConnectionStatus.Success -> {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = Color.Green,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Success! Handshake verified.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Green
                                        )
                                        if (status.models.isNotEmpty()) {
                                            Text(
                                                text = "Available Models: ${status.models.take(5).joinToString(", ")}",
                                                fontSize = 11.sp,
                                                color = if (isDark) Color.Gray else Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                            is ConnectionStatus.Error -> {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Failed",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Handshake Failed",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                        Text(
                                            text = status.message,
                                            fontSize = 11.sp,
                                            color = if (isDark) Color.Gray else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Custom UI & Resizable Font Section ---
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 24.dp),
                color = if (isDark) Color.DarkGray else Color.LightGray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "UI Customizer",
                    tint = if (isDark) AmberAccent else WarmWoodBrown,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AESTHETICS & ACCESSIBILITY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }

            // 1. Theme Preferences
            Text(
                text = "Select Custom Theme:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) AmberAccent else WarmWoodBrown,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val themes = listOf(
                "Auto" to "Phase-Based adaptive theme",
                "Classic Terminal" to "Amber & Dark Terminal (Monospace)",
                "Warm Parchment" to "Serif Legal Parchment (Warm light)",
                "Cyberpunk Neon" to "Glowing Cyan & Obsidian dark",
                "Royal Court" to "Imperial Gold & Deep Navy blue"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                themes.forEach { (themeName, themeDesc) ->
                    val isThemeSelected = uiState.customTheme == themeName
                    val themeColor = when (themeName) {
                        "Classic Terminal" -> AmberAccent
                        "Warm Parchment" -> WarmWoodBrown
                        "Cyberpunk Neon" -> Color(0xFF00E5FF)
                        "Royal Court" -> Color(0xFFF1D483)
                        else -> if (isDark) AmberAccent else WarmWoodBrown
                    }

                    OutlinedButton(
                        onClick = { onThemeChange(themeName) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isThemeSelected) {
                                themeColor.copy(alpha = 0.15f)
                            } else Color.Transparent,
                            contentColor = if (isDark) Color.White else Color.Black
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isThemeSelected) 2.dp else 1.dp,
                            color = if (isThemeSelected) themeColor else if (isDark) Color.DarkGray else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_button_${themeName.replace(" ", "_").lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = themeName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    text = themeDesc,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color.Gray else Color.DarkGray
                                )
                            }
                            if (isThemeSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected Theme",
                                    tint = themeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Resizable Font Size Slider
            Text(
                text = "Resize Font Scale:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) AmberAccent else WarmWoodBrown,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val currentScale = uiState.fontSizeMultiplier
            val scaleLabel = when {
                currentScale < 0.9f -> "Small (85%)"
                currentScale < 1.1f -> "Normal (100%)"
                currentScale < 1.25f -> "Large (115%)"
                else -> "Extra Large (130%)"
            }

            Text(
                text = "Current scale: $scaleLabel",
                fontSize = 13.sp,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = currentScale,
                onValueChange = {
                    val snappedValue = when {
                        it < 0.92f -> 0.85f
                        it < 1.08f -> 1.0f
                        it < 1.22f -> 1.15f
                        else -> 1.3f
                    }
                    onFontSizeChange(snappedValue)
                },
                valueRange = 0.85f..1.3f,
                steps = 2,
                colors = SliderDefaults.colors(
                    thumbColor = if (isDark) AmberAccent else WarmWoodBrown,
                    activeTrackColor = if (isDark) AmberAccent else WarmWoodBrown,
                    inactiveTrackColor = if (isDark) Color.DarkGray else Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("font_size_slider")
            )

            // Live Font Scaling Preview Box
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF0EBE1)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2C2C35) else Color.LightGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIVE TYPOGRAPHY PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.Gray else Color.DarkGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "The justice system of Project Themis requires prompt and meticulous adjudication of all evidence dockets.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            }

            // --- Monetization & Sustainability Section ---
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 24.dp),
                color = if (isDark) Color.DarkGray else Color.LightGray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Monetization",
                    tint = Color(0xFFFFD700), // Imperial Gold
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MONETIZATION & SUSTAINABILITY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isDark) Color.White else Color.Black
                )
            }

            // Explanatory Card about revenue & credit usage
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF13131A) else Color(0xFFFBF8F3)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2C2C35) else Color.LightGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Ads generate active revenue to sustain your AI credit usage and keep servers/LLMs operational. Disabling ads will halt local developer monetization support.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.LightGray else Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }
            }

            // 1. Show Bottom Ads Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Bottom Google Ads",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Displays banner ads at the bottom of the screens",
                        fontSize = 11.sp,
                        color = if (isDark) Color.Gray else Color.DarkGray
                    )
                }
                Switch(
                    checked = uiState.adsEnabled,
                    onCheckedChange = onAdsEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDark) AmberAccent else WarmWoodBrown,
                        checkedTrackColor = (if (isDark) AmberAccent else WarmWoodBrown).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("ads_enabled_switch")
                )
            }

            // 2. Simulated Ads Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use Simulated Sponsor Ads",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "Force simulation when offline/in dev sandbox",
                        fontSize = 11.sp,
                        color = if (isDark) Color.Gray else Color.DarkGray
                    )
                }
                Switch(
                    checked = uiState.useSimulatedAds,
                    onCheckedChange = onUseSimulatedAdsChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDark) AmberAccent else WarmWoodBrown,
                        checkedTrackColor = (if (isDark) AmberAccent else WarmWoodBrown).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("simulated_ads_switch")
                )
            }

            // 3. Ad Unit ID Customization
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Ad Unit ID (Banner):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) AmberAccent else WarmWoodBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = uiState.adUnitId,
                    onValueChange = onAdUnitIdChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ad_unit_id_input"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color.White else Color.Black
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { onAdUnitIdChange("ca-app-pub-3940256099942544/6300978111") },
                            modifier = Modifier.testTag("reset_ad_id_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset default test banner ID",
                                tint = if (isDark) Color.Gray else Color.DarkGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                        unfocusedBorderColor = if (isDark) Color.DarkGray else Color.LightGray,
                        cursorColor = if (isDark) AmberAccent else WarmWoodBrown
                    )
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                // Test Connection Button
                Button(
                    onClick = {
                        hasUserEditedSinceTest = false
                        onTestConnection()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF222226) else Color.LightGray,
                        contentColor = if (isDark) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_connection_btn")
                ) {
                    Text("Test Connection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Save Settings Button
                Button(
                    onClick = {
                        onSaveSettings()
                        android.widget.Toast.makeText(context, "Configuration saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                        contentColor = if (isDark) Color.Black else Color.White,
                        disabledContainerColor = if (isDark) Color.DarkGray else Color.LightGray,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_settings_btn")
                ) {
                    Text("Save Config", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
