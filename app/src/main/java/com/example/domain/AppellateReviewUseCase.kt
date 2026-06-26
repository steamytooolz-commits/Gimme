package com.example.domain

import com.example.api.LlmClient
import com.example.data.repository.GameRepository
import com.example.data.repository.SecureStorageRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import java.util.UUID

class AppellateReviewUseCase(
    private val repository: GameRepository,
    private val secureStorageRepository: SecureStorageRepository,
    private val llmClient: LlmClient
) {
    suspend fun evaluatePerformance(
        caseId: String,
        targetSuspectId: String,
        wasConvicted: Boolean,
        conflictOfInterest: Int,
        selectedEvidenceIds: Set<String>
    ): CaseEvaluationReport {
        // 1. Fetch Ground Truth
        val groundTruth = repository.getGroundTruth(caseId) ?: CaseGroundTruth(
            caseId = caseId,
            archetype = CaseArchetype.HOMICIDE,
            coreCrime = "The assassination of Duke Sterling.",
            absoluteTruth = "Lord Vance poisoned the Duke to seize political power. He blackmailed Lady Beatrice into helping him and framed Gideon the Apothecary.",
            suspectCulpabilities = mapOf(
                "vance" to CulpabilityStatus.PRINCIPAL,
                "beatrice" to CulpabilityStatus.ACCOMPLICE,
                "gideon" to CulpabilityStatus.COMPLETELY_INNOCENT
            ),
            criticalMissableClues = listOf("forged_ledger", "torn_letter")
        )

        val culpabilities = groundTruth.suspectCulpabilities
        val actualCulpability = culpabilities[targetSuspectId] ?: CulpabilityStatus.COMPLETELY_INNOCENT

        // 2. Calculate Justice Metric (0-100)
        // Convicting a PRINCIPAL gives 100.
        // Convicting an ACCOMPLICE or ACCESSORY gives 80.
        // Convicting a COMPLETELY_INNOCENT gives 0, with severe penalties.
        // Convicting an UNAWARE_PAWN gives 30.
        // Let's refine the score based on the action:
        var justiceMetric = 0
        if (wasConvicted) {
            justiceMetric = when (actualCulpability) {
                CulpabilityStatus.PRINCIPAL -> 100
                CulpabilityStatus.ACCOMPLICE -> 80
                CulpabilityStatus.ACCESSORY_AFTER_THE_FACT -> 70
                CulpabilityStatus.UNAWARE_PAWN -> 30
                CulpabilityStatus.GUILTY_OF_UNRELATED_CRIME -> 40
                CulpabilityStatus.COMPLETELY_INNOCENT -> 10
            }
        } else {
            // Acquitted or let go
            justiceMetric = when (actualCulpability) {
                CulpabilityStatus.COMPLETELY_INNOCENT -> 100
                CulpabilityStatus.UNAWARE_PAWN -> 90
                CulpabilityStatus.GUILTY_OF_UNRELATED_CRIME -> 70
                CulpabilityStatus.ACCESSORY_AFTER_THE_FACT -> 40
                CulpabilityStatus.ACCOMPLICE -> 30
                CulpabilityStatus.PRINCIPAL -> 20
            }
        }

        // 3. Calculate Procedural Integrity (0-100)
        // It starts at 100, penalized by conflictOfInterest.
        // Also penalized for each warrantless evidence presented.
        val allEvidence = repository.allEvidence.first()
        var presentedTaintedCount = 0
        allEvidence.forEach { item ->
            if (item.id in selectedEvidenceIds) {
                if (item.collectionContext.warrantUsed == null || item.admissibilityStatus == AdmissibilityStatus.INADMISSIBLE) {
                    presentedTaintedCount++
                }
            }
        }
        var proceduralIntegrity = 100 - conflictOfInterest
        proceduralIntegrity -= (presentedTaintedCount * 15)
        proceduralIntegrity = proceduralIntegrity.coerceIn(0, 100)

        // 4. Calculate Conspiracy Unraveled (0-100)
        // Check what fraction of non-innocent suspects were correctly convicted,
        // and if critical missable clues were found (meaning they exist in the evidence database and were selected).
        val guiltySuspects = culpabilities.filter { it.value == CulpabilityStatus.PRINCIPAL || it.value == CulpabilityStatus.ACCOMPLICE || it.value == CulpabilityStatus.ACCESSORY_AFTER_THE_FACT }
        var guiltyIdentifiedCount = 0
        guiltySuspects.keys.forEach { suspectId ->
            // If the suspect was convicted and actually guilty, or if they were thoroughly investigated
            if (suspectId == targetSuspectId && wasConvicted) {
                guiltyIdentifiedCount++
            }
        }
        val baseConspiracyScore = if (guiltySuspects.isNotEmpty()) {
            (guiltyIdentifiedCount.toFloat() / guiltySuspects.size.toFloat() * 60f).toInt()
        } else {
            60
        }

        val foundCriticalCluesCount = groundTruth.criticalMissableClues.count { clueId ->
            allEvidence.any { it.id == clueId && it.id in selectedEvidenceIds }
        }
        val cluesPercentage = if (groundTruth.criticalMissableClues.isNotEmpty()) {
            (foundCriticalCluesCount.toFloat() / groundTruth.criticalMissableClues.size.toFloat() * 40f).toInt()
        } else {
            40
        }

        val conspiracyUnraveledPercentage = (baseConspiracyScore + cluesPercentage).coerceIn(0, 100)

        // 5. Calculate Overall Grade
        val averageScore = (justiceMetric + proceduralIntegrity + conspiracyUnraveledPercentage) / 3
        val overallGrade = when {
            averageScore >= 95 -> "Supreme Court Justice (S)"
            averageScore >= 85 -> "Appellate Judge (A)"
            averageScore >= 72 -> "Magistrate (B)"
            averageScore >= 60 -> "Justice of the Peace (C)"
            averageScore >= 45 -> "Court Clerk (D)"
            else -> "Disbarred (F)"
        }

        // 6. Generate Appellate Critique via LLM
        val npcList = repository.allNpcs.first()
        val targetNpcName = npcList.find { it.id == targetSuspectId }?.name ?: targetSuspectId

        val prompt = """
            You are the Appellate Review Board of the High Court of Themis.
            Evaluate the Magistrate's performance for this case.
            
            CASE PARAMETERS & GROUND TRUTH:
            - Archetype: ${groundTruth.archetype.name}
            - Core Crime: ${groundTruth.coreCrime}
            - Absolute Truth: ${groundTruth.absoluteTruth}
            - Actual Suspect Roles: ${groundTruth.suspectCulpabilities.entries.joinToString { "${it.key} is ${it.value.name}" }}
            
            MAGISTRATE'S TRIAL DECISION:
            - Target Suspect: $targetNpcName (ID: $targetSuspectId)
            - Verdict Rendered: ${if (wasConvicted) "CONVICTED" else "ACQUITTED"}
            - actual culpability of target: ${actualCulpability.name}
            
            DETERMINISTIC METRICS CALCULATED:
            - Justice Score: $justiceMetric / 100
            - Procedural Integrity: $proceduralIntegrity / 100
            - Conspiracy Unraveled: $conspiracyUnraveledPercentage % (Found $foundCriticalCluesCount of ${groundTruth.criticalMissableClues.size} critical clues)
            - Assigned Grade: $overallGrade
            
            Write a 3-paragraph critique in formal, story-rich legal prose:
            Paragraph 1: Executive Summary. State the correctness of the verdict. Did they punish the true mastermind or condemn an innocent?
            Paragraph 2: Procedural Conduct. Review how they handled evidence. Praise them if procedural integrity was high, or scold them if they relied on tainted, warrantless evidence or suffered high conflict of interest.
            Paragraph 3: Legacy of the Case. Explain how this decision shapes the trust of the local populace and the prestige of the Court of Themis.
            
            Format your response directly as elegant text. Do not include JSON formatting or markdown headers.
        """.trimIndent()

        val critique = try {
            val response = llmClient.generateGameResponse(
                systemInstruction = "You are the Appellate Board of the Court of Themis. Write a 3-paragraph formal evaluation report of a Magistrate's trial verdict.",
                history = listOf("user" to prompt),
                currentPhase = GamePhase.COURTROOM,
                config = secureStorageRepository.getLlmConfig(),
                apiKey = secureStorageRepository.getApiKey()
            )
            response.textResponse
        } catch (e: Exception) {
            // High-quality fallback critique if LLM fails
            buildFallbackCritique(targetNpcName, actualCulpability, wasConvicted, justiceMetric, proceduralIntegrity, conspiracyUnraveledPercentage)
        }

        val report = CaseEvaluationReport(
            caseId = caseId,
            justiceMetric = justiceMetric,
            proceduralIntegrity = proceduralIntegrity,
            conspiracyUnraveledPercentage = conspiracyUnraveledPercentage,
            overallGrade = overallGrade,
            appellateCritique = critique,
            targetSuspectId = targetSuspectId,
            wasConvicted = wasConvicted,
            actualCulpability = actualCulpability
        )

        // Save report to Room
        repository.insertEvaluationReport(report)

        return report
    }

    private fun buildFallbackCritique(
        npcName: String,
        culpability: CulpabilityStatus,
        wasConvicted: Boolean,
        justice: Int,
        procedure: Int,
        conspiracy: Int
    ): String {
        val verdictDesc = if (wasConvicted) "convict" else "acquit"
        val truthPart = when (culpability) {
            CulpabilityStatus.PRINCIPAL -> {
                if (wasConvicted) {
                    "The Magistrate successfully identified and convicted $npcName, the primary perpetrator of this heinous offense. Justice has been served in its purest sense."
                } else {
                    "In a shocking miscarriage of justice, the primary mastermind, $npcName, was allowed to walk free. The court is deeply embarrassed by this oversight."
                }
            }
            CulpabilityStatus.COMPLETELY_INNOCENT -> {
                if (wasConvicted) {
                    "A dark day for the High Court. The completely innocent $npcName has been wrongfully convicted of a crime they had no hand in, while the true culprits remain at large."
                } else {
                    "The Magistrate correctly acquitted the innocent $npcName, shielding them from wrongful condemnation. This shows proper judicial restraint."
                }
            }
            else -> {
                "The trial concluded with a decision to $verdictDesc $npcName, who held the status of ${culpability.name} in this complex affair. While not the sole mastermind, their involvement represents a critical thread of the conspiracy."
            }
        }

        val procedurePart = if (procedure >= 80) {
            "Procedurally, the Magistrate operated with exceptional cleanliness. The dossier was constructed without relying on tainted or unconstitutional searches, and proper warrants were maintained. The Court of Themis commends this rigorous adherence to the Rule of Law."
        } else {
            "The procedural record is severely flawed. Tainted evidence, seized without appropriate search warrants, was permitted to enter the trial, compromising the ethical integrity of our institutions. The high level of personal conflict of interest leaves a stained legacy."
        }

        val conspiracyPart = if (conspiracy >= 75) {
            "Furthermore, the Magistrate thoroughly unraveled the conspiracy, leaving no critical clue undiscovered. This comprehensive investigation sets a new gold standard for judicial inquiry."
        } else {
            "Unfortunately, key conspiracies remain shrouded in mystery. Several critical, missable clues were overlooked or ignored, leaving the full extent of the criminal syndicate unprosecuted."
        }

        return "$truthPart\n\n$procedurePart\n\n$conspiracyPart"
    }
}
