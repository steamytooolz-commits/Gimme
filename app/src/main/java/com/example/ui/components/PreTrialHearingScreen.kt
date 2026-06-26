package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.data.model.PreTrialMotion
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.TextGreen

@Composable
fun PreTrialHearingScreen(
    motions: List<PreTrialMotion>,
    onRuleOnMotion: (String, Boolean) -> Unit,
    onProceedToCourtroom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val allRuled = motions.all { it.ruled }

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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRE-TRIAL HEARING IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "DEFENSE'S OBJECTIONS & MOTIONS TO SUPPRESS",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Button(
                onClick = onProceedToCourtroom,
                enabled = allRuled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberAccent,
                    disabledContainerColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("proceed_to_trial_button")
            ) {
                Text("Proceed to Courtroom Trial", fontSize = 12.sp, color = if (allRuled) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance of Power Warning Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E1416))
                .border(1.dp, CrimsonAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = CrimsonAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "THE MAGISTRATE'S CRUCIBLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonAccent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "As both Lead Investigator and Presiding Judge, your judicial integrity is on the line. Sustaining a motion suppresses illegally-obtained evidence, keeping your hands clean but weakening your case. Overruling a motion admits the evidence, but the defense will appeal, spiking your Conflict of Interest meter.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Motions List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (motions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No motions filed by the Defense. Your dossier stands unchallenged.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                motions.forEach { motion ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (motion.ruled) Color(0xFF2A2A30) else AmberAccent, RoundedCornerShape(8.dp))
                            .testTag("motion_card_${motion.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Motion Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "MOTION TO SUPPRESS: ${motion.targetEvidenceName.uppercase()}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                
                                // Status Indicator
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (motion.sustained) {
                                                true -> Color(0xFF241416)
                                                false -> Color(0xFF142015)
                                                null -> Color(0xFF2A2A35)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = when (motion.sustained) {
                                            true -> "SUPPRESSED (EVIDENCE REMOVED)"
                                            false -> "ADMITTED (MOTION OVERRULED)"
                                            null -> "PENDING RULING"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (motion.sustained) {
                                            true -> CrimsonAccent
                                            false -> TextGreen
                                            null -> Color.Gray
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Argument Text Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1A1A22))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "DEFENSE'S LEGAL ARGUMENT:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = motion.argument,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Ruling Buttons
                            if (!motion.ruled) {
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onRuleOnMotion(motion.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1E22)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.testTag("sustain_motion_button_${motion.id}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SUSTAIN (Exclude Evidence)", color = CrimsonAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        onClick = { onRuleOnMotion(motion.id, false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3524)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.testTag("overrule_motion_button_${motion.id}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TextGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("OVERRULE (Admit Evidence)", color = TextGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (motion.sustained == true) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (motion.sustained == true) CrimsonAccent else TextGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (motion.sustained == true) {
                                            "You ruled to SUSTAIN. This evidence is excluded from the trial record to preserve judicial procedure."
                                        } else {
                                            "You ruled to OVERRULE. The evidence is admitted over the defense's fierce objection."
                                        },
                                        fontSize = 11.sp,
                                        color = Color.Gray
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
