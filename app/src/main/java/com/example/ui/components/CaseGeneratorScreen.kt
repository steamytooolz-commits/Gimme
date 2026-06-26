package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaseArchetype
import com.example.data.model.CaseGenerationParameters
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkTerminalBg
import com.example.ui.theme.ParchmentBg
import com.example.ui.theme.WarmWoodBrown
import com.example.ui.viewmodel.ThemisIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseGeneratorScreen(
    isDark: Boolean,
    onBack: () -> Unit,
    onGenerate: (CaseGenerationParameters) -> Unit,
    isGenerating: Boolean,
    progressText: String,
    modifier: Modifier = Modifier
) {
    var archetype by remember { mutableStateOf(CaseArchetype.HOMICIDE) }
    var complexityLevel by remember { mutableStateOf(3) }
    var allowConspiracies by remember { mutableStateOf(true) }
    var setting by remember { mutableStateOf("Renaissance Palace") }
    var tone by remember { mutableStateOf("Grim Noir") }

    val scrollState = rememberScrollState()

    val cardBg = if (isDark) Color(0xFF16161B) else Color.White
    val borderCol = if (isDark) Color(0xFF2A2A30) else Color.LightGray
    val textColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color.Gray else Color.DarkGray
    val primaryAccent = if (isDark) AmberAccent else WarmWoodBrown

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkTerminalBg else ParchmentBg)
    ) {
        if (isGenerating) {
            // Loading Overlay with progress state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp)
            ) {
                CircularProgressIndicator(color = AmberAccent)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "GENERATING TRIAL GROUND TRUTH",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = AmberAccent
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.LightGray,
                    modifier = Modifier.testTag("generation_progress_text")
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Top Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_to_dashboard_btn")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Text(
                        text = "NEW MYSTERY SETUP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = primaryAccent
                    )
                }

                // Archetype Selection
                Text(
                    text = "CASE ARCHETYPE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = subtitleColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        CaseArchetype.values().forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { archetype = type }
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                                    .testTag("archetype_${type.name}")
                            ) {
                                RadioButton(
                                    selected = archetype == type,
                                    onClick = { archetype = type },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = type.name.replace("_", " "),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textColor
                                    )
                                }
                            }
                            if (type != CaseArchetype.values().last()) {
                                HorizontalDivider(color = borderCol.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                // Complexity Level Slider
                Text(
                    text = "COMPLEXITY LEVEL: $complexityLevel / 5",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = subtitleColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Slider(
                            value = complexityLevel.toFloat(),
                            onValueChange = { complexityLevel = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryAccent,
                                activeTrackColor = primaryAccent,
                                inactiveTrackColor = borderCol
                            ),
                            modifier = Modifier.testTag("complexity_slider")
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simple Trial", style = MaterialTheme.typography.labelSmall, color = subtitleColor)
                            Text("Intricate Web", style = MaterialTheme.typography.labelSmall, color = subtitleColor)
                        }
                    }
                }

                // Conspiracies Toggle
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Multi-Culprit Conspiracies",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                            Text(
                                text = "Enables accomplices, accessories, and unaware pawns in addition to the principal.",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor
                            )
                        }
                        Switch(
                            checked = allowConspiracies,
                            onCheckedChange = { allowConspiracies = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryAccent,
                                checkedTrackColor = primaryAccent.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("conspiracies_switch")
                        )
                    }
                }

                // Custom Setting Input
                Text(
                    text = "MYSTERY SETTING",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = subtitleColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = setting,
                    onValueChange = { setting = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedIndicatorColor = primaryAccent,
                        unfocusedIndicatorColor = borderCol
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("setting_input_field")
                )

                // Custom Tone Input
                Text(
                    text = "MYSTERY TONE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = subtitleColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = tone,
                    onValueChange = { tone = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg,
                        focusedIndicatorColor = primaryAccent,
                        unfocusedIndicatorColor = borderCol
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("tone_input_field")
                )

                // Submit/Generate Button
                Button(
                    onClick = {
                        onGenerate(
                            CaseGenerationParameters(
                                archetype = archetype,
                                complexityLevel = complexityLevel,
                                allowConspiracies = allowConspiracies,
                                setting = setting,
                                tone = tone
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAccent,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("initiate_generation_btn")
                ) {
                    Text(
                        text = "GENERATE GAME GROUND TRUTH",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}
