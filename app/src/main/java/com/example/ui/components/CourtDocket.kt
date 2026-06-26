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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GamePhase
import com.example.data.model.NPC
import com.example.ui.theme.AntiqueBrassGold
import com.example.ui.theme.CourtSurface
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.LegalRed
import com.example.ui.theme.TextGreen
import com.example.ui.theme.WarmWoodBrown
import com.example.ui.viewmodel.ObjectionState

@Composable
fun CourtDocket(
    npcs: List<NPC>,
    currentPhase: GamePhase,
    selectedNpcId: String?,
    onSelectNpc: (String?) -> Unit,
    onIssueVerdict: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var npcToJudge by remember { mutableStateOf<NPC?>(null) }

    val isDark = currentPhase == GamePhase.INVESTIGATION
    val containerBg = if (isDark) Color(0xFF0F0F11) else MaterialTheme.colorScheme.background

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(containerBg)
            .padding(16.dp)
    ) {
        // Docket Banner
        Text(
            text = if (currentPhase == GamePhase.INVESTIGATION) "SUSPECT PROFILES & INTERROGATOR" else "THE HIGH COURT DOCKET & WITNESS STAND",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = if (isDark) AntiqueBrassGold else WarmWoodBrown
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (currentPhase == GamePhase.INVESTIGATION) "Select a suspect below to focus your interrogation in the Terminal tab." else "Observe witness stress and press 'VERDICT GAVEL' to issue your judgment once trial has reached clarity.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(npcs) { npc ->
                val isSelected = selectedNpcId == npc.id
                NpcDocketCard(
                    npc = npc,
                    currentPhase = currentPhase,
                    isSelected = isSelected,
                    onSelect = { onSelectNpc(if (isSelected) null else npc.id) },
                    onOpenVerdict = { npcToJudge = npc }
                )
            }
        }
    }

    // Verdict Judgment Dialog
    npcToJudge?.let { npc ->
        VerdictDialog(
            npc = npc,
            currentPhase = currentPhase,
            onDismiss = { npcToJudge = null },
            onConfirmVerdict = { convict ->
                onIssueVerdict(npc.id, convict)
                npcToJudge = null
            }
        )
    }
}

@Composable
fun NpcDocketCard(
    npc: NPC,
    currentPhase: GamePhase,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenVerdict: () -> Unit
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION
    
    val cardBg = when {
        isSelected -> if (isDark) Color(0xFF22222A) else MaterialTheme.colorScheme.primaryContainer
        else -> if (isDark) Color(0xFF16161B) else MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isSelected -> if (isDark) AntiqueBrassGold else WarmWoodBrown
        else -> if (isDark) Color(0xFF222226) else MaterialTheme.colorScheme.outlineVariant
    }

    val stressColor = when {
        npc.stress < 40 -> TextGreen
        npc.stress < 75 -> AntiqueBrassGold
        else -> CrimsonAccent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .padding(14.dp)
            .testTag("npc_card_${npc.id}")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = npc.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = npc.role.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isDark) AntiqueBrassGold else WarmWoodBrown,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Selection / Interrogate Button indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) (if (isDark) AntiqueBrassGold else WarmWoodBrown) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isDark) AntiqueBrassGold else WarmWoodBrown,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSelected) "ACTIVE FOCUS" else "SET FOCUS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isSelected) (if (isDark) Color.Black else Color.White) else (if (isDark) AntiqueBrassGold else WarmWoodBrown)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stress level progress bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ANXIETY LEVEL: ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                LinearProgressIndicator(
                    progress = { npc.stress / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = stressColor,
                    trackColor = if (isDark) Color(0xFF2A2A30) else Color(0xFFE0E0E0)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${npc.stress}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = stressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Statement Summary
            Text(
                text = "TESTIMONY:",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "\"${npc.statement}\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )

            // Hidden background summary when selected
            if (isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = if (isDark) Color(0xFF2E2E35) else MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DOSSIER BRIEFING:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (isDark) AntiqueBrassGold else WarmWoodBrown
                )
                Text(
                    text = npc.profile,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = if (isDark) Color.LightGray else MaterialTheme.colorScheme.onSurface
                )
            }

            // Gavel Gavel button in Courtroom Phase
            if (currentPhase == GamePhase.COURTROOM) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenVerdict,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("gavel_button_${npc.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) AntiqueBrassGold else WarmWoodBrown,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "DELIVER COURT VERDICT (GAVEL)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun VerdictDialog(
    npc: NPC,
    currentPhase: GamePhase,
    onDismiss: () -> Unit,
    onConfirmVerdict: (Boolean) -> Unit
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    2.dp,
                    if (isDark) AntiqueBrassGold else WarmWoodBrown,
                    RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1E1E24) else CourtSurface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "THE COURT OF THEMIS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = if (isDark) AntiqueBrassGold else WarmWoodBrown
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PRONOUNCING JUDGMENT",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else LegalRed
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "You hold the Golden Gavel over ${npc.name}. As the Investigating Magistrate, you must formally pronounce your final, legally binding decree:",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.LightGray else Color.Black
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CONVICT Button
                    Button(
                        onClick = { onConfirmVerdict(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("confirm_convict_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) CrimsonAccent else LegalRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CONVICT GUILTY", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // ACQUIT Button
                    Button(
                        onClick = { onConfirmVerdict(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("confirm_acquit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) TextGreen else Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("ACQUIT INNOCENT", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(onClick = onDismiss) {
                    Text(
                        "ABSTAIN / RETURN TO TRIAL",
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ObjectionOverlay(
    objection: ObjectionState,
    currentPhase: GamePhase,
    onResolve: (Boolean) -> Unit
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    3.dp,
                    Brush.verticalGradient(listOf(CrimsonAccent, AntiqueBrassGold)),
                    RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1E1E24) else CourtSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CrimsonAccent)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "DEFENSE ATTORNEY SECURED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "⚠️ OBJECTION RAISED!",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) CrimsonAccent else LegalRed
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Witness: ${objection.witnessName}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "Grounds: ${objection.type}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueBrassGold
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = objection.description + "\n\nAs Presiding Magistrate, you must rule immediately. Sparing procedural rules may compromise judicial integrity.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SUSTAIN (exclude)
                    Button(
                        onClick = { onResolve(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sustain_objection_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) AntiqueBrassGold else WarmWoodBrown,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SUSTAIN", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    // OVERRULE (allow but raise COI)
                    Button(
                        onClick = { onResolve(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("overrule_objection_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C35) else Color(0xFFE0D8CC),
                            contentColor = if (isDark) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = borderStrokeHelper(isDark)
                    ) {
                        Text("OVERRULE", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun VerdictResultOverlay(
    verdictText: String,
    currentPhase: GamePhase,
    onRestart: () -> Unit
) {
    val isDark = currentPhase == GamePhase.INVESTIGATION
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, AntiqueBrassGold, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = CourtSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "THE SUPREME DECREE OF THEMIS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = WarmWoodBrown,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CASE CONCLUDED",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
                    color = LegalRed,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ParchmentBgHelper())
                        .border(1.dp, AntiqueBrassGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                        Text(
                            text = verdictText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 22.sp),
                            color = LegalOnBgHelper()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restart_game_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmWoodBrown,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("BEGIN A NEW INVESTIGATION", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun borderStrokeHelper(isDark: Boolean): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isDark) AntiqueBrassGold else WarmWoodBrown
    )
}

fun ParchmentBgHelper(): Color = Color(0xFFFBF8F3)
fun LegalOnBgHelper(): Color = Color(0xFF2C2520)
