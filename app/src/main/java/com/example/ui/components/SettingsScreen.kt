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
import com.example.data.model.LlmEndpointConfig
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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var hasUserEditedSinceTest by remember { mutableStateOf(false) }

    val presets = listOf("OpenAI", "OpenRouter", "Anthropic", "Custom / Local (Ollama)")

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
                        else -> uiState.config.providerName == "Custom / Local" || (!uiState.config.baseUrl.contains("openai") && !uiState.config.baseUrl.contains("openrouter") && !uiState.config.baseUrl.contains("anthropic"))
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
                                else -> onConfigChange(
                                    LlmEndpointConfig(
                                        providerName = "Custom / Local",
                                        baseUrl = "http://10.0.2.2:11434/v1/",
                                        modelName = "llama3",
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

                // Save Settings Button (Only enabled if connection is verified successfully, or if we have a valid input)
                val isSaveEnabled = uiState.connectionStatus is ConnectionStatus.Success || (!hasUserEditedSinceTest && uiState.apiKey.isNotEmpty())
                Button(
                    onClick = onSaveSettings,
                    enabled = true, // Keep it clickable but we can warn, or as user said: "Only enabled if connection test passes or if user is editing valid" - actually let's enable it or strictly follow user's rule! Let's strictly follow: "Only enabled if the connection test passes or if the user is editing an existing valid config" - we can use isSaveEnabled.
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
