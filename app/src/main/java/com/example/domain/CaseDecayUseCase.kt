package com.example.domain

import com.example.data.model.*
import com.example.data.repository.GameRepository
import com.example.data.local.NpcEntity
import android.util.Log

class CaseDecayUseCase(
    private val repository: GameRepository
) {
    suspend fun applyDecayToCase(caseId: String, daysCold: Int): CaseProgress? {
        val currentProgress = repository.getCaseProgress(caseId) ?: return null
        val nextDegradation = (currentProgress.degradationLevel + 1).coerceAtMost(3)
        
        // 1. Evidence Degradation
        val discoveredEvidence = repository.db.evidenceDao().getEvidenceListDirect()
        for (evidence in discoveredEvidence) {
            val degradedReport = "[DEGRADED - COMPROMISED CHAIN OF CUSTODY]\n" +
                    "Due to the case being cold for $daysCold days (Degradation Level $nextDegradation), " +
                    "the sample has degraded. Forensic analysis is highly ambiguous: ${evidence.forensicReport}"
            
            repository.db.evidenceDao().insertEvidence(
                evidence.copy(
                    forensicReport = degradedReport,
                    admissibilityStatus = AdmissibilityStatus.UNVERIFIED.name
                )
            )
        }

        // 2. NPC/Witness Amnesia and Suspect Escape
        val npcEntities = repository.db.npcDao().getNpcListDirect()
        val updatedNpcs = npcEntities.map { npc ->
            val culpabilityMap = repository.getGroundTruth(caseId)?.suspectCulpabilities ?: emptyMap()
            val npcId = npc.id
            val actualCulp = culpabilityMap[npcId] ?: CulpabilityStatus.COMPLETELY_INNOCENT
            
            if (actualCulp == CulpabilityStatus.PRINCIPAL && daysCold >= 60) {
                // Fugitive Suspect
                npc.copy(
                    role = "FUGITIVE (Escaped Suspect)",
                    statement = "[FUGITIVE - FLOWN JURISDICTION]\nThis suspect has fled the Court's jurisdiction after the case went cold. They cannot be interrogated directly.",
                    profile = "STATUS: FUGITIVE. ${npc.profile}"
                )
            } else {
                // Witness Amnesia / Relocation
                npc.copy(
                    statement = "[MEMORY FADE ACTIVE] I... I don't recall those details clearly anymore. It was so many days ago...",
                    profile = "STATUS: RELOCATED / FORGETFUL. ${npc.profile}"
                )
            }
        }
        repository.insertNpcs(updatedNpcs)

        // Update database progress
        val updatedProgress = currentProgress.copy(
            status = CaseStatus.ACTIVE,
            daysElapsed = 1, // Reset active days on reopen
            activeLeadsRemaining = 12, // Reduced starting leads for cold case
            publicPressure = 0.4f * nextDegradation,
            degradationLevel = nextDegradation
        )
        repository.insertCaseProgress(updatedProgress)
        return updatedProgress
    }
}
