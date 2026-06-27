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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NPC
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AntiqueBrassGold
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalBg
import com.example.ui.theme.ParchmentBg
import com.example.ui.theme.WarmWoodBrown
import com.example.ui.viewmodel.ThemisIntent
import com.example.ui.viewmodel.ThemisUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawAndOrderScreen(
    uiState: ThemisUiState,
    onIntent: (ThemisIntent) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840

    var isDraftingActive by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkTerminalBg else ParchmentBg)
    ) {
        if (isTablet) {
            // Left Pane: Codex and Drafting
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                if (isDraftingActive) {
                    DraftStatuteForm(
                        onDraft = { title, desc, clauses ->
                            onIntent(ThemisIntent.DraftStatute(title, desc, clauses))
                            isDraftingActive = false
                        },
                        onCancel = { isDraftingActive = false },
                        isDark = isDark
                    )
                } else {
                    StatuteCodex(
                        statutes = uiState.legalStatutes,
                        onStartDrafting = { isDraftingActive = true },
                        isDark = isDark
                    )
                }
            }

            // Right Pane: Newspapers & Bribes
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                BribesAndShadowProposals(
                    npcs = uiState.npcList,
                    onAcceptBribe = { suspectId, amount, desc ->
                        onIntent(ThemisIntent.AcceptBribe(suspectId, amount, desc))
                    },
                    onRefuseBribe = { suspectId ->
                        onIntent(ThemisIntent.RefuseBribe(suspectId))
                    },
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                ProvincialGazette(
                    articles = uiState.newspaperArticles,
                    isDark = isDark
                )
            }
        } else {
            // Single-pane scrollable for mobile viewports
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                if (isDraftingActive) {
                    DraftStatuteForm(
                        onDraft = { title, desc, clauses ->
                            onIntent(ThemisIntent.DraftStatute(title, desc, clauses))
                            isDraftingActive = false
                        },
                        onCancel = { isDraftingActive = false },
                        isDark = isDark
                    )
                } else {
                    StatuteCodex(
                        statutes = uiState.legalStatutes,
                        onStartDrafting = { isDraftingActive = true },
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                BribesAndShadowProposals(
                    npcs = uiState.npcList,
                    onAcceptBribe = { suspectId, amount, desc ->
                        onIntent(ThemisIntent.AcceptBribe(suspectId, amount, desc))
                    },
                    onRefuseBribe = { suspectId ->
                        onIntent(ThemisIntent.RefuseBribe(suspectId))
                    },
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(24.dp))

                ProvincialGazette(
                    articles = uiState.newspaperArticles,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
fun StatuteCodex(
    statutes: List<com.example.data.model.LegalStatute>,
    onStartDrafting: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text(
                text = "⚖️ LEGAL STATUTE CODEX",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDark) AntiqueBrassGold else WarmWoodBrown
            )
            Button(
                onClick = onStartDrafting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                    contentColor = if (isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("start_drafting_statute_btn")
            ) {
                Text("Draft Statute 📝", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        if (statutes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, if (isDark) Color.DarkGray else Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No legal statutes codified in the province yet.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            statutes.forEach { statute ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF16161B) else Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statute.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                                color = if (isDark) AmberAccent else WarmWoodBrown
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDark) Color(0xFF2A2A30) else Color(0xFFE8E8E8))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statute.id,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color.LightGray else Color.DarkGray
                                )
                            }
                        }
                        Text(
                            text = statute.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Divider(color = if (isDark) Color(0xFF222226) else Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        statute.clauses.forEach { clause ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "Clause ${clause.id}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = if (isDark) AntiqueBrassGold else WarmWoodBrown
                                )
                                Text(
                                    text = clause.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DraftStatuteForm(
    onDraft: (String, String, List<String>) -> Unit,
    onCancel: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Maintain list of clauses
    var clauses = remember { mutableStateListOf("") }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B1B22) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🏛️ DRAFT NEW PROVINCIAL STATUTE",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                color = if (isDark) AmberAccent else WarmWoodBrown,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Statute Title (e.g. Alchemical Safety Decree)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                    unfocusedBorderColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("General Description") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Text(
                text = "STATUTE CLAUSES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = if (isDark) AntiqueBrassGold else WarmWoodBrown,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            clauses.forEachIndexed { index, clauseText ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Clause ${(index + 65).toChar()}:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        modifier = Modifier.width(75.dp)
                    )
                    OutlinedTextField(
                        value = clauseText,
                        onValueChange = { clauses[index] = it },
                        placeholder = { Text("e.g. Possession of mandrake root without license is treason.") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) AmberAccent else WarmWoodBrown,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    IconButton(
                        onClick = { if (clauses.size > 1) clauses.removeAt(index) },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Clause", tint = CrimsonAccent)
                    }
                }
            }

            Button(
                onClick = { clauses.add("") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF2A2A30) else Color.LightGray,
                    contentColor = if (isDark) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("+ Add Another Clause", style = MaterialTheme.typography.labelSmall)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = if (isDark) Color.White else Color.Black)
                }

                Button(
                    onClick = {
                        val validClauses = clauses.filter { it.trim().isNotEmpty() }
                        if (title.trim().isNotEmpty() && validClauses.isNotEmpty()) {
                            onDraft(title, description, validClauses)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("proclaim_statute_btn")
                ) {
                    Text("Proclaim Decree", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun BribesAndShadowProposals(
    npcs: List<NPC>,
    onAcceptBribe: (String, Int, String) -> Unit,
    onRefuseBribe: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // Generate bribery offers dynamically based on NPC stress levels
    val offers = npcs.filter { it.stress >= 50 }.map { npc ->
        val amount = when (npc.role.uppercase()) {
            "CHANCELLOR", "DUKE" -> 1500
            "ALCHEMIST", "PHYSICIAN" -> 800
            "GUARD", "OFFICER" -> 300
            else -> 500
        }
        val description = when (npc.id) {
            "gideon" -> "Offers 800 guilders and a signed, sealed confession from a random guard to completely drop the Mandrake possession inquiries."
            "beatrice" -> "Offers 1500 sterling gold coins and a deed to a vineyard in the Southern Valleys to secure an instant acquittal."
            "vance" -> "Offers a pouch containing 300 guilders to dismiss the Palace entry violation charges."
            else -> "Offers $amount gold coins to look the other way regarding specific forensic clues."
        }
        BriberyOffer(npcId = npc.id, npcName = npc.name, npcRole = npc.role, amount = amount, description = description)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "⏳ ACTIVE SHADOW OFFERS (BRIBERY)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = if (isDark) AntiqueBrassGold else WarmWoodBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (offers.isEmpty()) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF16161B) else Color(0xFFF9F9F9)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF222226) else Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Suspects are too cautious. Elevate their stress level to 50%+ during interrogations to force secret bribery propositions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            offers.forEach { offer ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF261A1E) else Color(0xFFFFF0F2) // Light red tint for illegal actions
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Proffered by: ${offer.npcName} (${offer.npcRole})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "💰 ${offer.amount} Gold Coins",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF81C784)
                            )
                        }
                        Text(
                            text = offer.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onRefuseBribe(offer.npcId) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Refuse & Interrogate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onAcceptBribe(offer.npcId, offer.amount, offer.description) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C), contentColor = Color.White),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Accept Bribe 🤝", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BriberyOffer(
    val npcId: String,
    val npcName: String,
    val npcRole: String,
    val amount: Int,
    val description: String
)

@Composable
fun ProvincialGazette(
    articles: List<com.example.data.model.NewspaperArticle>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "📰 THE PROVINCIAL GAZETTE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = if (isDark) AntiqueBrassGold else WarmWoodBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (articles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(1.dp, if (isDark) Color.DarkGray else Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No news published yet.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            articles.forEach { article ->
                Card(
                    shape = RoundedCornerShape(0.dp), // Retro newspaper straight edges
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1E24) else Color(0xFFFAF6EE) // Retro yellow paper
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.DarkGray else Color.LightGray),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "THE DAILY PROVINCIAL GAZETTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Text(
                                text = "Day ${article.dayPublished} • Edition I",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                        
                        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = article.headline,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Serif),
                            color = if (isDark) Color.White else Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        
                        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

                        Text(
                            text = article.content,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, lineHeight = 16.sp),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isShiftPositive = article.publicSentimentShift >= 0
                            val shiftText = if (isShiftPositive) "+${article.publicSentimentShift}%" else "${article.publicSentimentShift}%"
                            val shiftColor = if (isShiftPositive) Color(0xFF81C784) else CrimsonAccent
                            
                            Icon(
                                imageVector = if (isShiftPositive) Icons.Default.ThumbUp else Icons.Default.Warning,
                                contentDescription = "Sentiment Shift",
                                tint = shiftColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Populace Sentiment Shift: $shiftText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = shiftColor
                            )
                        }
                    }
                }
            }
        }
    }
}
