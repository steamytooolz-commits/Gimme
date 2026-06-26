package com.example.domain

import com.example.data.repository.GameRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.first

class ContextAssemblerUseCase(
    private val repository: GameRepository
) {
    suspend fun assembleTrialContext(selectedEvidenceIds: List<String>): String {
        val allEvidence = repository.allEvidence.first()
        val allLinks = repository.allLinks.first()

        val selectedEvidence = allEvidence.filter { it.id in selectedEvidenceIds }
        // Filter out INADMISSIBLE evidence for the official trial context
        val admittedEvidence = selectedEvidence.filter { it.admissibilityStatus == AdmissibilityStatus.ADMISSIBLE }
        val admittedIds = admittedEvidence.map { it.id }.toSet()

        // Filter links where both source and target are admitted
        val relevantLinks = allLinks.filter { it.sourceEvidenceId in admittedIds && it.targetEvidenceId in admittedIds }

        return buildString {
            appendLine("# OFFICIAL CASE DOSSIER")
            appendLine()
            appendLine("## ADMITTED EVIDENCE")
            if (admittedEvidence.isEmpty()) {
                appendLine("No admitted evidence has been introduced into the record.")
            } else {
                admittedEvidence.forEach { item ->
                    appendLine("### Item ID: ${item.id}")
                    appendLine("- **Name**: ${item.name}")
                    appendLine("- **Physical Description**: ${item.physicalDescription}")
                    appendLine("- **Forensic Report**: ${item.forensicReport}")
                    appendLine("- **Location Found**: ${item.collectionContext.locationFound}")
                    appendLine("- **Collecting Officer**: ${item.collectionContext.collectingOfficer}")
                    appendLine("- **Seizure Warrant**: ${item.collectionContext.warrantUsed ?: "NONE"}")
                    if (item.userAnnotations.isNotEmpty()) {
                        appendLine("- **Magistrate's Annotations**: ${item.userAnnotations}")
                    }
                    appendLine()
                }
            }

            appendLine("## SEMANTIC RELATIONSHIPS & MAGISTRATE NOTES")
            if (relevantLinks.isEmpty()) {
                appendLine("No semantic links have been formally established by the Magistrate.")
            } else {
                relevantLinks.forEach { link ->
                    val sourceName = admittedEvidence.find { it.id == link.sourceEvidenceId }?.name ?: link.sourceEvidenceId
                    val targetName = admittedEvidence.find { it.id == link.targetEvidenceId }?.name ?: link.targetEvidenceId
                    appendLine("- **[$sourceName]** ${link.relationshipType.name} **[$targetName]**")
                    appendLine("  - *Magistrate's Justification*: ${link.magistrateJustification}")
                    appendLine()
                }
            }

            appendLine("## KNOWN PROCEDURAL VIOLATIONS")
            // Explicitly list chain-of-custody breaks or warrantless seizures so the LLM Defense can attack them
            val violations = selectedEvidence.filter { it.collectionContext.warrantUsed == null || it.admissibilityStatus == AdmissibilityStatus.INADMISSIBLE }
            if (violations.isEmpty()) {
                appendLine("No procedural violations or warrantless searches were logged for the selected evidence.")
            } else {
                violations.forEach { item ->
                    appendLine("- **Warrantless seizure detected** for item **${item.name}** (ID: ${item.id}).")
                    appendLine("  - *Location*: ${item.collectionContext.locationFound}")
                    appendLine("  - *Officer*: ${item.collectionContext.collectingOfficer}")
                    appendLine("  - *Status*: Marked as ${item.admissibilityStatus.name} in official logs.")
                    appendLine()
                }
            }
        }
    }
}
