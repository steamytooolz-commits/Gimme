package com.example.ui.components

import androidx.compose.foundation.background
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
    val scope = rememberCoroutineScope()
    var localTestPrompt by remember { mutableStateOf("Verify if suspect Jax has a valid alibi for central magistrate breach.") }
    var localTestResponse by remember { mutableStateOf("") }
    var isLocalTesting by remember { mutableStateOf(false) }

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
                // Render custom Liquid LFM Model Downloader UI
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
                            text = "Model: Liquid LFM 2.5 (350M parameters)\n" +
                                    "Quantization: 4-bit (GGUF file signature)\n" +
                                    "Memory Requirement: ~512 MB RAM\n" +
                                    "Local Disk Footprint: 175 MB compressed",
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        HorizontalDivider(
                            color = if (isDark) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Current Status:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.Gray else Color.DarkGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Render status-based visuals
                        when (lfmStatus) {
                            LiquidOnDeviceSdk.ModelStatus.NOT_DOWNLOADED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Missing",
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Model weights not found on local disk.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            LiquidOnDeviceSdk().startDownload(context)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                                        contentColor = if (isDark) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("download_lfm_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Download",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download LFM Model Weights",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            LiquidOnDeviceSdk.ModelStatus.DOWNLOADING -> {
                                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = "Downloading weights...",
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

                                    Spacer(modifier = Modifier.height(8.dp))

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
                                }

                                OutlinedButton(
                                    onClick = {
                                        LiquidOnDeviceSdk().cancelDownload()
                                    },
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
                                    Text(text = "Cancel Download", fontSize = 12.sp)
                                }
                            }
                            LiquidOnDeviceSdk.ModelStatus.DOWNLOADED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Downloaded",
                                        tint = Color.Green,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Model weights are downloaded.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                LiquidOnDeviceSdk().initialize(context)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                                            contentColor = if (isDark) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(44.dp)
                                            .testTag("load_lfm_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = "Load",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Load Model to RAM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            LiquidOnDeviceSdk().deleteModel(context)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("delete_lfm_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Delete", fontSize = 12.sp)
                                    }
                                }
                            }
                            LiquidOnDeviceSdk.ModelStatus.LOADING -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.5.dp,
                                        color = if (isDark) AmberAccent else WarmWoodBrown,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Loading weights into RAM allocation...",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                            LiquidOnDeviceSdk.ModelStatus.LOADED -> {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = Color.Green,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Active & Ready (Running entirely offline)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Green
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            LiquidOnDeviceSdk().deleteModel(context)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .padding(bottom = 12.dp)
                                            .testTag("delete_lfm_btn_active")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Unload/Delete",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Unload & Delete Weights from Storage", fontSize = 11.sp)
                                    }

                                    HorizontalDivider(
                                        color = if (isDark) Color.DarkGray else Color.LightGray,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )

                                    // Local Testing Area / Playground!
                                    Text(
                                        text = "Test Local Inference Playground:",
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
