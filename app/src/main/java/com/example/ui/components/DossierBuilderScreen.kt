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
import com.example.data.model.EvidenceLink
import com.example.data.model.RelationshipType
import com.example.data.model.AdmissibilityStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalSurface
import com.example.ui.theme.TextGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DossierBuilderScreen(
    evidenceList: List<EvidenceItem>,
    selectedIds: Set<String>,
    evidenceLinks: List<EvidenceLink>,
    onToggleSelection: (String) -> Unit,
    onCreateLink: (String, String, RelationshipType, String) -> Unit,
    onGenerateMotions: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sourceIdForLink by remember { mutableStateOf<String?>(null) }
    var targetIdForLink by remember { mutableStateOf<String?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }

    var relationshipType by remember { mutableStateOf(RelationshipType.CORROBORATES) }
    var justificationText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CASE DOSSIER BUILDER",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "CURATE THE OFFICIAL CHARGES",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Row {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25252D)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("close_dossier_builder_button")
                ) {
                    Text("Resume Investigation", fontSize = 12.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onGenerateMotions,
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("submit_dossier_button")
                ) {
                    Text("Proceed to Pre-Trial Hearing", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link builder control panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF16161B))
                .border(1.dp, Color(0xFF2A2A32), RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "ESTABLISH SEMANTIC RELATIONSHIP",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Source selection indicator
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E1E24))
                            .border(1.dp, if (sourceIdForLink != null) AmberAccent else Color.DarkGray, RoundedCornerShape(4.dp))
                            .clickable { sourceIdForLink = null }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val sourceName = evidenceList.find { it.id == sourceIdForLink }?.name ?: "Select Source Evidence..."
                        Text(sourceName, fontSize = 11.sp, color = if (sourceIdForLink != null) Color.White else Color.Gray, maxLines = 1)
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Target selection indicator
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E1E24))
                            .border(1.dp, if (targetIdForLink != null) AmberAccent else Color.DarkGray, RoundedCornerShape(4.dp))
                            .clickable { targetIdForLink = null }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val targetName = evidenceList.find { it.id == targetIdForLink }?.name ?: "Select Target Evidence..."
                        Text(targetName, fontSize = 11.sp, color = if (targetIdForLink != null) Color.White else Color.Gray, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showLinkDialog = true },
                        enabled = sourceIdForLink != null && targetIdForLink != null && sourceIdForLink != targetIdForLink,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, disabledContainerColor = Color.DarkGray),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Link Clues", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Main Corkboard: Grid of gathered evidence
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "EVIDENCE CORKBOARD",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(evidenceList) { item ->
                        val isSelected = item.id in selectedIds
                        val isSource = item.id == sourceIdForLink
                        val isTarget = item.id == targetIdForLink
                        
                        val isAdmissible = item.admissibilityStatus == AdmissibilityStatus.ADMISSIBLE

                        val itemBorderColor = when {
                            isSource -> AmberAccent
                            isTarget -> Color(0xFF64B5F6)
                            isSelected -> TextGreen
                            else -> Color(0xFF2A2A30)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, itemBorderColor, RoundedCornerShape(8.dp))
                                .testTag("corkboard_card_${item.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelection(item.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = TextGreen,
                                            uncheckedColor = Color.DarkGray
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = item.physicalDescription,
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    maxLines = 2,
                                    modifier = Modifier.height(34.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Admissibility badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isAdmissible) Color(0xFF142015) else Color(0xFF241416))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAdmissible) "ADMISSIBLE" else "POISONED FRUIT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdmissible) TextGreen else CrimsonAccent
                                        )
                                    }

                                    // Quick Linking Buttons
                                    Row {
                                        IconButton(
                                            onClick = { sourceIdForLink = item.id },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Set as Source",
                                                tint = if (isSource) AmberAccent else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { targetIdForLink = item.id },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = "Set as Target",
                                                tint = if (isTarget) Color(0xFF64B5F6) else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right sidebar: List of connections/links & selections
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .background(Color(0xFF16161B))
                    .border(1.dp, Color(0xFF2A2A30), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "ESTABLISHED LINKS (${evidenceLinks.size})",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (evidenceLinks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No logical connections established yet. Select Source (▶) and Target (✔) clues above to map relationship links.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        evidenceLinks.forEach { link ->
                            val sourceName = evidenceList.find { it.id == link.sourceEvidenceId }?.name ?: "Unknown"
                            val targetName = evidenceList.find { it.id == link.targetEvidenceId }?.name ?: "Unknown"

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF212128))
                                    .border(1.dp, Color(0xFF2A2A34), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = link.relationshipType.name.replace("_", " "),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active link",
                                        tint = TextGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$sourceName ➔ $targetName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = link.magistrateJustification,
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for configuring a new logical Link
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = {
                Text(
                    text = "ESTABLISH LEGAL NEXUS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sName = evidenceList.find { it.id == sourceIdForLink }?.name ?: ""
                    val tName = evidenceList.find { it.id == targetIdForLink }?.name ?: ""
                    Text(
                        text = "Source: $sName\nTarget: $tName",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Relationship Type",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Relationship Type select options
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RelationshipType.values().forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { relationshipType = type }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = relationshipType == type,
                                    onClick = { relationshipType = type },
                                    colors = RadioButtonDefaults.colors(selectedColor = AmberAccent, unselectedColor = Color.DarkGray)
                                )
                                Text(
                                    text = type.name.replace("_", " "),
                                    fontSize = 12.sp,
                                    color = if (relationshipType == type) Color.White else Color.LightGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Magistrate's Justification (Required)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = justificationText,
                        onValueChange = { justificationText = it },
                        placeholder = { Text("Describe the logical or forensic link between these two clues...", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("magistrate_justification_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sId = sourceIdForLink
                        val tId = targetIdForLink
                        if (sId != null && tId != null && justificationText.trim().length >= 10) {
                            onCreateLink(sId, tId, relationshipType, justificationText.trim())
                            justificationText = ""
                            sourceIdForLink = null
                            targetIdForLink = null
                            showLinkDialog = false
                        }
                    },
                    enabled = justificationText.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, disabledContainerColor = Color.DarkGray)
                ) {
                    Text("Apply Link", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF16161B)
        )
    }
}
