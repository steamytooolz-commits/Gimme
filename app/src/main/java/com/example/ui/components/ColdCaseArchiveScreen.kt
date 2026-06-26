package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaseProgress
import com.example.data.model.CaseStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalBg
import com.example.ui.theme.ParchmentBg
import com.example.ui.theme.WarmWoodBrown
import com.example.ui.viewmodel.ThemisIntent
import com.example.ui.viewmodel.ThemisUiState

@Composable
fun ColdCaseArchiveScreen(
    uiState: ThemisUiState,
    onIntent: (ThemisIntent) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val archive = uiState.coldCaseArchive
    var selectedCaseId by remember { mutableStateOf<String?>(null) }
    var showReopenConfirmDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkTerminalBg else ParchmentBg)
            .padding(16.dp)
    ) {
        // Title block
        Text(
            text = "📁 COLD CASE ARCHIVE",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp
            ),
            color = if (isDark) AmberAccent else WarmWoodBrown,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Magistrate records of unresolved, expired, or lead-exhausted cases. Suspended pending reopening protocols.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) Color.LightGray else Color.DarkGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (archive.isEmpty()) {
            // Elegant empty state
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Archive Empty",
                        tint = if (isDark) Color.DarkGray else Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "THE ARCHIVE IS VACANT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isDark) Color.Gray else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Maintain active leads and execute timely warrants to ensure no investigation goes cold.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.DarkGray else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(archive) { case ->
                    val isExpanded = selectedCaseId == case.caseId
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1C1C24) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isExpanded) (if (isDark) AmberAccent else WarmWoodBrown) else (if (isDark) Color(0xFF2A2A30) else Color.LightGray),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCaseId = if (isExpanded) null else case.caseId }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "CASE FILE: ${case.caseId.take(8).uppercase()}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${case.status.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CrimsonAccent
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isDark) Color(0xFF2A2A30) else Color(0xFFF0F0F0))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "EXP: DAY ${case.daysElapsed}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    HorizontalDivider(color = if (isDark) Color(0xFF2A2A30) else Color.LightGray)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "ARCHIVE AUDIT DETAILS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isDark) AmberAccent else WarmWoodBrown
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    DetailRow(label = "Days Elapsed:", value = "${case.daysElapsed} days", isDark = isDark)
                                    DetailRow(label = "Max Days Allocated:", value = "${case.maxDaysBeforeCold} days", isDark = isDark)
                                    DetailRow(label = "Remaining Leads:", value = "${case.activeLeadsRemaining} leads", isDark = isDark)
                                    DetailRow(label = "Socioeconomic Pressure:", value = "${(case.publicPressure * 100).toInt()}%", isDark = isDark)
                                    DetailRow(label = "Prior Degradation Level:", value = "Lvl ${case.degradationLevel}", isDark = isDark)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showReopenConfirmDialog = case.caseId },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CrimsonAccent,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reopen_case_btn_${case.caseId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reopen",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "REOPEN INVESTIGATION",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Alert Dialog with detailed warning
    showReopenConfirmDialog?.let { caseId ->
        AlertDialog(
            onDismissRequest = { showReopenConfirmDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = CrimsonAccent,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "REOPEN INVESTIGATION?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = CrimsonAccent
                )
            },
            text = {
                Text(
                    text = "⚠️ PROCEDURAL DECAY WARNING ⚠️\n\n" +
                            "Reopening a cold case triggers immediate degradation. " +
                            "Physical evidence left in the archives becomes compromised (unverified admissibility), " +
                            "witnesses suffer memory amnesia (foggy memories), and the principal suspect may flee " +
                            "and become a fugitive if too much time has passed.\n\n" +
                            "Are you prepared to face these severe investigative handicaps?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.White else Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onIntent(ThemisIntent.ReopenColdCase(caseId))
                        showReopenConfirmDialog = null
                        selectedCaseId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                ) {
                    Text("PROCEED", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenConfirmDialog = null }) {
                    Text("ABORT", color = if (isDark) Color.White else Color.Black)
                }
            },
            containerColor = if (isDark) Color(0xFF1E1E24) else Color.White
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) Color.Gray else Color.DarkGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isDark) Color.White else Color.Black
        )
    }
}
