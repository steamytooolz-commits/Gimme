package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.GamePhase
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalBg
import com.example.ui.theme.TextGreen
import kotlinx.coroutines.launch

@Composable
fun MagistrateTerminal(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    currentPhase: GamePhase,
    activeNpcName: String?,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (currentPhase == GamePhase.INVESTIGATION) DarkTerminalBg else MaterialTheme.colorScheme.background)
    ) {
        // Active target banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (currentPhase == GamePhase.INVESTIGATION) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeNpcName != null) "INTERROGATING: $activeNpcName" else "GENERAL INQUIRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (currentPhase == GamePhase.INVESTIGATION) "MODE: MAGISTRATE_DECT" else "MODE: MAGISTRATE_JUDGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentPhase == GamePhase.INVESTIGATION) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                TerminalMessageItem(message = message, currentPhase = currentPhase)
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentPhase == GamePhase.INVESTIGATION) "> INQUISITOR_LLM thinking..." else "Defense attorney formulating response...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentPhase == GamePhase.INVESTIGATION) AmberAccent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Auto-complete chips for easy typing / game discoverability
        val commands = if (currentPhase == GamePhase.INVESTIGATION) {
            listOf(
                "/search vance desk",
                "/search beatrice chambers",
                "/search banquet hall",
                "/interrogate beatrice",
                "/interrogate gideon",
                "/interrogate vance"
            )
        } else {
            listOf(
                "/present golden_goblet",
                "/present forged_ledger",
                "/present torn_letter",
                "/interrogate gideon",
                "/interrogate vance"
            )
        }

        ScrollableTabRow(
            selectedTabIndex = 0,
            indicator = {},
            divider = {},
            edgePadding = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (currentPhase == GamePhase.INVESTIGATION) Color.Black else Color.Transparent)
        ) {
            commands.forEach { command ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentPhase == GamePhase.INVESTIGATION) Color(0xFF1E1E24) else MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            inputText = command
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = command,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Input Field
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Issue command or ask suspect...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("magistrate_input"),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.testTag("submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalMessageItem(message: ChatMessage, currentPhase: GamePhase) {
    if (message.isSystem) {
        // Render system/tool messages uniquely
        val isWarning = message.text.contains("Warning") || message.text.contains("⚠️") || message.text.contains("objection")
        val isTool = message.isToolCall
        val bgColor = when {
            isWarning -> if (currentPhase == GamePhase.INVESTIGATION) CrimsonAccent.copy(alpha = 0.15f) else Color(0xFFFFEBEE)
            isTool -> if (currentPhase == GamePhase.INVESTIGATION) AmberAccent.copy(alpha = 0.08f) else Color(0xFFFFF8E1)
            else -> if (currentPhase == GamePhase.INVESTIGATION) TextGreen.copy(alpha = 0.12f) else Color(0xFFE8F5E9)
        }
        val borderColor = when {
            isWarning -> if (currentPhase == GamePhase.INVESTIGATION) CrimsonAccent else Color.Red
            isTool -> if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else Color(0xFFFFD54F)
            else -> if (currentPhase == GamePhase.INVESTIGATION) TextGreen else Color(0xFF81C784)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = when {
                    isWarning -> if (currentPhase == GamePhase.INVESTIGATION) CrimsonAccent else Color(0xFFB71C1C)
                    isTool -> if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else Color(0xFFF57F17)
                    else -> if (currentPhase == GamePhase.INVESTIGATION) TextGreen else Color(0xFF2E7D32)
                }
            )
        }
    } else {
        // standard chat bubble
        val isPlayer = message.sender == "Player"
        
        val bubbleBg = if (isPlayer) {
            if (currentPhase == GamePhase.INVESTIGATION) Color(0xFF222226) else MaterialTheme.colorScheme.primaryContainer
        } else {
            if (currentPhase == GamePhase.INVESTIGATION) Color(0xFF16161B) else MaterialTheme.colorScheme.surfaceVariant
        }
        
        val textColor = if (isPlayer) {
            if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            if (currentPhase == GamePhase.INVESTIGATION) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(
            horizontalAlignment = if (isPlayer) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sender name
            Text(
                text = message.sender.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlayer) {
                    if (currentPhase == GamePhase.INVESTIGATION) AmberAccent else MaterialTheme.colorScheme.primary
                } else {
                    if (currentPhase == GamePhase.INVESTIGATION) Color.Gray else MaterialTheme.colorScheme.secondary
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = if (isPlayer) 8.dp else 0.dp,
                            bottomEnd = if (isPlayer) 0.dp else 8.dp
                        )
                    )
                    .background(bubbleBg)
                    .padding(12.dp)
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }
        }
    }
}
