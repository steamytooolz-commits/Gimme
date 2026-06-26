package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import com.example.data.model.EvidenceItem
import com.example.data.model.AdmissibilityStatus
import com.example.data.model.GamePhase
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalSurface
import com.example.ui.theme.TextGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EvidenceDossier(
    evidenceList: List<EvidenceItem>,
    currentPhase: GamePhase,
    modifier: Modifier = Modifier
) {
    var selectedEvidence by remember { mutableStateOf<EvidenceItem?>(null) }

    // Auto select first if none selected
    LaunchedEffect(evidenceList) {
        if (selectedEvidence == null && evidenceList.isNotEmpty()) {
            selectedEvidence = evidenceList.first()
        } else if (selectedEvidence != null) {
            // Keep selected item updated with database changes
            selectedEvidence = evidenceList.find { it.id == selectedEvidence?.id } ?: evidenceList.firstOrNull()
        }
    }

    val isDark = currentPhase == GamePhase.INVESTIGATION
    val containerBg = if (isDark) Color(0xFF0F0F11) else MaterialTheme.colorScheme.background

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(containerBg)
    ) {
        // Left side: List of evidence cards (takes 50% width)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            if (evidenceList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) AmberAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No evidence collected yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDark) Color.Gray else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Conduct searches like '/search lord chambers' in the terminal to gather clues.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.DarkGray else MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(evidenceList) { evidence ->
                        val isSelected = selectedEvidence?.id == evidence.id
                        EvidenceCard(
                            evidence = evidence,
                            isSelected = isSelected,
                            currentPhase = currentPhase,
                            onClick = { selectedEvidence = evidence }
                        )
                    }
                }
            }
        }

        // Right side: Detail Panel (takes 50% width)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .background(if (isDark) DarkTerminalSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF2A2A30) else MaterialTheme.colorScheme.outlineVariant
                )
                .padding(16.dp)
        ) {
            selectedEvidence?.let { evidence ->
                EvidenceDetailPanel(evidence = evidence, currentPhase = currentPhase)
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select an item to view dossier details.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun EvidenceCard(
    evidence: EvidenceItem,
    isSelected: Boolean,
    currentPhase: GamePhase,
    onClick: () -> Unit
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION
    
    val cardBg = when {
        isSelected -> if (isDark) Color(0xFF25252D) else MaterialTheme.colorScheme.primaryContainer
        else -> if (isDark) Color(0xFF16161B) else MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isSelected -> if (isDark) AmberAccent else MaterialTheme.colorScheme.primary
        else -> if (isDark) Color(0xFF222226) else MaterialTheme.colorScheme.outlineVariant
    }

    val isAdmissible = evidence.admissibilityStatus == AdmissibilityStatus.ADMISSIBLE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("evidence_card_${evidence.id}")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = evidence.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = if (isSelected) {
                        if (isDark) AmberAccent else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Integrity indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isAdmissible) TextGreen else CrimsonAccent)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = evidence.physicalDescription,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAdmissible) "PROCEDURALLY ADMISSIBLE" else "INADMISSIBLE (PROCEDURAL BREAK)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = if (isAdmissible) TextGreen else CrimsonAccent
            )
        }
    }
}

@Composable
fun EvidenceDetailPanel(
    evidence: EvidenceItem,
    currentPhase: GamePhase
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION
    val scrollState = rememberScrollState()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "EVIDENCE FILE",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = evidence.name.uppercase(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Folder Tabs UI
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Details", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Procedural", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Annotations", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: DETAILS & FORENSICS
                    Text(
                        text = "PHYSICAL DESCRIPTION:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = evidence.physicalDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "FORENSICS REPORT:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E1E22) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isDark) Color(0xFF32323A) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = evidence.forensicReport,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                1 -> {
                    // TAB 1: PROCEDURAL & CUSTODY
                    Text(
                        text = "COLLECTION DATA:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF1A1A1E) else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Location Found:", fontSize = 12.sp, color = Color.Gray)
                            Text(evidence.collectionContext.locationFound, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Collecting Officer:", fontSize = 12.sp, color = Color.Gray)
                            Text(evidence.collectionContext.collectingOfficer, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Timestamp:", fontSize = 12.sp, color = Color.Gray)
                            Text(sdf.format(Date(evidence.collectionContext.timestamp)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Search Warrant Used:", fontSize = 12.sp, color = Color.Gray)
                            Text(evidence.collectionContext.warrantUsed ?: "NONE (Illegal Seizure)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (evidence.collectionContext.warrantUsed != null) TextGreen else CrimsonAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "COURTROOM ADMISSIBILITY STATUS:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val isAdmissible = evidence.admissibilityStatus == AdmissibilityStatus.ADMISSIBLE

                    if (isAdmissible) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF142015) else Color(0xFFE8F5E9))
                                .border(1.dp, TextGreen, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TextGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Procedurally sound. Search warrant verified. Admissible in court.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                color = if (isDark) TextGreen else Color(0xFF2E7D32)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF241416) else Color(0xFFFFEBEE))
                                .border(1.dp, CrimsonAccent, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonAccent,
                                    modifier = Modifier.size(20.dp)
                               )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FRUIT OF THE POISONOUS TREE",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = if (isDark) CrimsonAccent else Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This item was seized without an active Search Warrant. It violates procedural law and is constitutionally inadmissible.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                2 -> {
                    // TAB 2: ANNOTATIONS
                    Text(
                        text = "MAGISTRATE'S PERSONAL NOTES:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E1E22) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isDark) Color(0xFF32323A) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = evidence.userAnnotations.ifEmpty { "No personal notes or arguments recorded for this piece of evidence. Use the Pre-Trial Dossier Builder to structure link justifications." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
