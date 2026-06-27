package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvidenceItem
import com.example.data.model.AdmissibilityStatus
import com.example.data.model.GamePhase
import com.example.data.model.CollectionContext
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalSurface
import com.example.ui.theme.TextGreen
import com.example.ui.viewmodel.ThemisIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceDossier(
    evidenceList: List<EvidenceItem>,
    currentPhase: GamePhase,
    onIntent: (ThemisIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEvidence by remember { mutableStateOf<EvidenceItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<AdmissibilityStatus?>(null) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    val filteredList = evidenceList.filter { evidence ->
        val matchesSearch = evidence.name.contains(searchQuery, ignoreCase = true) ||
                evidence.physicalDescription.contains(searchQuery, ignoreCase = true) ||
                evidence.forensicReport.contains(searchQuery, ignoreCase = true) ||
                evidence.collectionContext.locationFound.contains(searchQuery, ignoreCase = true)
        
        val matchesStatus = statusFilter == null || evidence.admissibilityStatus == statusFilter
        
        matchesSearch && matchesStatus
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(containerBg)
    ) {
        // Left side: List of evidence cards & controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header with Log Manual Clue Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DOSSIER REGISTRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Collected Clues (${evidenceList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF25252D) else MaterialTheme.colorScheme.primaryContainer)
                        .testTag("log_manual_clue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Manual Clue",
                        tint = if (isDark) AmberAccent else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search clue dossier...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                    focusedLabelColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color(0xFF2E2E35) else MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("All", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = statusFilter == AdmissibilityStatus.ADMISSIBLE,
                    onClick = { statusFilter = AdmissibilityStatus.ADMISSIBLE },
                    label = { Text("Admissible", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = statusFilter == AdmissibilityStatus.INADMISSIBLE,
                    onClick = { statusFilter = AdmissibilityStatus.INADMISSIBLE },
                    label = { Text("Fruit of Poison", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clue List
            Box(modifier = Modifier.weight(1f)) {
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isDark) AmberAccent.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (evidenceList.isEmpty()) "No evidence collected yet." else "No matching results found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color.Gray else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredList) { evidence ->
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        EvidenceDetailPanel(
                            evidence = evidence,
                            currentPhase = currentPhase,
                            onIntent = onIntent
                        )
                    }

                    Divider(color = if (isDark) Color(0xFF2E2E35) else MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Discard Button
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = CrimsonAccent),
                        modifier = Modifier.align(Alignment.End).testTag("delete_clue_${evidence.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Discard Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
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

    // Dialogs
    if (showAddDialog) {
        AddManualEvidenceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc, forensic, loc, admissible ->
                onIntent(ThemisIntent.AddManualEvidence(name, desc, forensic, loc, admissible))
                showAddDialog = false
            }
        )
    }

    if (showDeleteDialog && selectedEvidence != null) {
        DeleteConfirmationDialog(
            itemName = selectedEvidence!!.name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onIntent(ThemisIntent.DeleteEvidence(selectedEvidence!!.id))
                selectedEvidence = null
                showDeleteDialog = false
            }
        )
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
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = if (isSelected) {
                        if (isDark) AmberAccent else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Integrity indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isAdmissible) TextGreen else CrimsonAccent)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = evidence.physicalDescription,
                maxLines = 2,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAdmissible) "PROCEDURALLY ADMISSIBLE" else "INADMISSIBLE FRUIT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (isAdmissible) TextGreen else CrimsonAccent
                )

                Text(
                    text = "ID: ${evidence.id.uppercase().take(8)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceDetailPanel(
    evidence: EvidenceItem,
    currentPhase: GamePhase,
    onIntent: (ThemisIntent) -> Unit
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION
    val scrollState = rememberScrollState()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    var activeTab by remember { mutableStateOf(0) }
    
    // Notes state for current selected evidence
    var noteText by remember(evidence.id) { mutableStateOf(evidence.userAnnotations) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "EVIDENCE REGISTRY FILE",
            style = MaterialTheme.typography.labelSmall,
            color = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = evidence.name.uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

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
                text = { Text("Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "FORENSICS / INVESTIGATION ANALYSIS:",
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
                        text = "COLLECTION DATA METADATA:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

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
                            Text("Collecting Authority:", fontSize = 12.sp, color = Color.Gray)
                            Text(evidence.collectionContext.collectingOfficer, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Timestamp Logged:", fontSize = 12.sp, color = Color.Gray)
                            Text(sdf.format(Date(evidence.collectionContext.timestamp)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Search Warrant Used:", fontSize = 12.sp, color = Color.Gray)
                            Text(evidence.collectionContext.warrantUsed ?: "NONE (Procedural Breach)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (evidence.collectionContext.warrantUsed != null) TextGreen else CrimsonAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "LEGAL ADMISSIBILITY EVALUATION:",
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
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TextGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Procedurally verified. Admissible in court of law.",
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
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CrimsonAccent,
                                    modifier = Modifier.size(18.dp)
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
                                text = "Seized without a valid warrant. Subject to exclusionary rules of evidence.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                2 -> {
                    // TAB 2: EDITABLE ANNOTATIONS
                    Text(
                        text = "MAGISTRATE'S IN-LINE INVESTIGATION NOTES:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("evidence_notes_input"),
                        placeholder = { Text("Log observations, links, or suspect contradictions...", fontSize = 13.sp) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                            focusedLabelColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (isDark) Color(0xFF2E2E35) else MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val isModified = noteText != evidence.userAnnotations

                    Button(
                        onClick = {
                            onIntent(ThemisIntent.UpdateEvidenceAnnotations(evidence.id, noteText))
                        },
                        enabled = isModified,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) AmberAccent else MaterialTheme.colorScheme.primary,
                            contentColor = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.align(Alignment.End).testTag("save_evidence_notes_button")
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualEvidenceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, forensicReport: String, location: String, isAdmissible: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var forensicReport by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isAdmissible by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "LOG CLUE/TESTIMONY",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmberAccent
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Clue Name") },
                    placeholder = { Text("e.g. Secret Alchemical Recipe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_clue_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        focusedLabelColor = AmberAccent
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Physical Appearance") },
                    placeholder = { Text("A leather-bound ledger found under...") },
                    modifier = Modifier.fillMaxWidth().testTag("manual_clue_description"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        focusedLabelColor = AmberAccent
                    )
                )

                OutlinedTextField(
                    value = forensicReport,
                    onValueChange = { forensicReport = it },
                    label = { Text("Analysis Notes / Testimonial Details") },
                    placeholder = { Text("Examination reveals traces of arsenic...") },
                    modifier = Modifier.fillMaxWidth().testTag("manual_clue_analysis"),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        focusedLabelColor = AmberAccent
                    )
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location Found") },
                    placeholder = { Text("e.g. Duke's Private Library") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_clue_location"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberAccent,
                        focusedLabelColor = AmberAccent
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Search Warrant Utilized", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(
                            "Warrantless logging flags this item as legally inadmissible.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAdmissible,
                        onCheckedChange = { isAdmissible = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextGreen,
                            checkedTrackColor = TextGreen.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("manual_clue_admissibility_switch")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description, forensicReport, location, isAdmissible)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = AmberAccent),
                modifier = Modifier.testTag("manual_clue_submit_button")
            ) {
                Text("LOG CLUE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("manual_clue_cancel_button")) {
                Text("CANCEL", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF16161B)
    )
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "DISCARD EVIDENCE?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonAccent
            )
        },
        text = {
            Text(
                "Are you sure you want to permanently discard '$itemName' from your official case files? This action is irreversible.",
                color = Color.LightGray
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = CrimsonAccent),
                modifier = Modifier.testTag("confirm_delete_clue_button")
            ) {
                Text("DISCARD", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("KEEP", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF16161B)
    )
}
