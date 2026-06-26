package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.CaseEvaluationReport
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AntiqueBrassGold
import com.example.ui.theme.WarmWoodBrown

@Composable
fun AppellateReportOverlay(
    report: CaseEvaluationReport,
    onRestart: () -> Unit,
    onNavigateToGenerator: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .border(2.dp, AntiqueBrassGold, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E24) // Immersive deep slate
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                // Header
                Text(
                    text = "APPELLATE REVIEW OF THEMIS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = AmberAccent,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "JUDICIAL DECREE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Numerical Metrics Row (Justice, Procedure, Conspiracy)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Justice Card
                    MetricCard(
                        title = "JUSTICE",
                        value = "${report.justiceMetric}/100",
                        progress = report.justiceMetric / 100f,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f).testTag("justice_score_card")
                    )

                    // Procedure Card
                    MetricCard(
                        title = "PROCEDURE",
                        value = "${report.proceduralIntegrity}/100",
                        progress = report.proceduralIntegrity / 100f,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f).testTag("procedure_score_card")
                    )

                    // Conspiracy Card
                    MetricCard(
                        title = "UNRAVELED",
                        value = "${report.conspiracyUnraveledPercentage}%",
                        progress = report.conspiracyUnraveledPercentage / 100f,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f).testTag("conspiracy_score_card")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assigned Title & Grade banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A35)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AntiqueBrassGold),
                    modifier = Modifier.fillMaxWidth().testTag("grade_title_card")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "OFFICIAL STANDING GRADE",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.overallGrade.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Serif
                            ),
                            color = AmberAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Critique Text box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF15151A))
                        .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = "APPELLATE COURT OPINION:",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = report.appellateCritique,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                lineHeight = 22.sp,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("appellate_critique_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("restart_game_report_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E2E35),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("REINVESTIGATE", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onNavigateToGenerator,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("new_custom_mystery_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AntiqueBrassGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("NEW CUSTOM CASE", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24242C)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF32323C)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                color = Color.Gray,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = color,
                trackColor = Color(0xFF1B1B20),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
