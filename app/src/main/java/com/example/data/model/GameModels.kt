package com.example.data.model

import com.squareup.moshi.JsonClass


data class LlmEndpointConfig(
    val providerName: String, // e.g., "OpenAI", "Ollama (Local)", "Custom Proxy"
    val baseUrl: String,      // e.g., "https://api.openai.com/v1/" or "http://10.0.2.2:11434/v1/"
    val modelName: String,    // e.g., "gpt-4o", "llama3", "mistral"
    val requiresApiKey: Boolean,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f
)

enum class GamePhase {
    INVESTIGATION, // "Detective Hat" - Dark Theme, monospace, terminal chat
    COURTROOM,     // "Judge Hat" - Light Theme, serif, formal courtroom layout
    COLD           // Case suspended - Cold Case view
}

enum class CaseStatus {
    ACTIVE,      // Currently investigating
    COLD,        // Investigation stalled/failed, moved to archive
    IN_TRIAL,    // Courtroom phase active
    CLOSED_JUST, // Successfully convicted
    CLOSED_FAILED // Acquitted or dismissed
}

@JsonClass(generateAdapter = true)
data class CaseProgress(
    val caseId: String,
    val status: CaseStatus,
    val daysElapsed: Int,
    val maxDaysBeforeCold: Int, // e.g., 30 days for murder, 14 for theft
    val activeLeadsRemaining: Int, // Decreases as leads are pursued/dead-ended
    val publicPressure: Float, // 0.0 to 1.0. Increases with time. Affects witness cooperation.
    val degradationLevel: Int // 0 to 3. Increases when a case goes COLD and is revisited.
)

enum class ObjectionType {
    HEARSAY,
    LEADING,
    IRRELEVANT,
    SPECULATION,
    COERCED_CONFESSION
}

data class CustodyEvent(
    val timestamp: Long,
    val officer: String,
    val action: String
)

enum class AdmissibilityStatus {
    ADMISSIBLE, INADMISSIBLE, UNVERIFIED
}

data class CollectionContext(
    val locationFound: String,
    val collectingOfficer: String,
    val timestamp: Long,
    val warrantUsed: String? // Null if warrantless (triggers procedural checks)
)

data class EvidenceItem(
    val id: String,
    val name: String,
    val physicalDescription: String,
    val forensicReport: String, // The raw text from the Forensics Agent
    val collectionContext: CollectionContext, // Who, when, how it was found
    val userAnnotations: String, // The Magistrate's personal notes
    val admissibilityStatus: AdmissibilityStatus
)

enum class RelationshipType {
    CORROBORATES, CONTRADICTS, ESTABLISHES_MOTIVE, PLACES_AT_SCENE, TIMELINE_ANCHOR
}

data class EvidenceLink(
    val id: Long = 0L,
    val sourceEvidenceId: String,
    val targetEvidenceId: String,
    val relationshipType: RelationshipType,
    val magistrateJustification: String // Crucial: The user must explain the link in text
)

data class PreTrialMotion(
    val id: String,
    val targetEvidenceId: String,
    val targetEvidenceName: String,
    val argument: String, // LLM's reason for suppression
    val ruled: Boolean = false,
    val sustained: Boolean? = null // null = unruled, true = sustained, false = overruled
)

data class NPC(
    val id: String,
    val name: String,
    val role: String, // Witness, Suspect, Victim
    val stress: Int, // 0 to 100
    val statement: String,
    val profile: String,
    val hiddenMotive: String
)

data class ChatMessage(
    val id: String,
    val phase: GamePhase,
    val sender: String, // "Player", "Lady Beatrice", "Gideon", "System", etc.
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isToolCall: Boolean = false,
    val toolName: String? = null
)

enum class CaseArchetype {
    HOMICIDE, FINANCIAL_FRAUD, CORPORATE_ESPIONAGE, CYBERCRIME, CRIMES_OF_PASSION, ORGANIZED_CRIME
}

enum class CulpabilityStatus {
    PRINCIPAL, // The main perpetrator
    ACCOMPLICE, // Actively helped, knew about it
    ACCESSORY_AFTER_THE_FACT, // Helped hide it after
    UNAWARE_PAWN, // Used by the guilty, but innocent
    COMPLETELY_INNOCENT,
    GUILTY_OF_UNRELATED_CRIME // A fun twist: they didn't do this murder, but they are hiding drugs
}

enum class ConnectionType {
    SAME_PERPETRATOR,
    COPYCAT,
    REVENGE,
    CONTINUING_ENTERPRISE,
    COINCIDENCE
}

@JsonClass(generateAdapter = true)
data class ColdCaseDigest(
    val originalCaseId: String,
    val summary: String,
    val keyPlayers: List<String>,
    val unresolvedThreads: List<String>
)

@JsonClass(generateAdapter = true)
data class CaseLinkage(
    val activeCaseId: String,
    val coldCaseId: String,
    val connectionType: ConnectionType,
    val description: String
)

@JsonClass(generateAdapter = true)
data class CaseGroundTruth(
    val caseId: String,
    val archetype: CaseArchetype,
    val coreCrime: String,
    val absoluteTruth: String, // The unredacted, full story of what actually happened
    val suspectCulpabilities: Map<String, CulpabilityStatus>, // Maps Suspect ID to their actual role
    val criticalMissableClues: List<String> // Clues that, if missed, result in a score penalty
)

data class CaseGenerationParameters(
    val archetype: CaseArchetype,
    val complexityLevel: Int, // 1 to 5 (determines number of suspects and red herrings)
    val allowConspiracies: Boolean, // If true, allows multiple PRINCIPAL/ACCOMPLICE nodes
    val setting: String, // e.g., "1920s Manor", "Cyberpunk Megacorp", "Modern Suburb"
    val tone: String // e.g., "Gritty Noir", "Whodunit Mystery", "Legal Thriller"
)

data class CaseEvaluationReport(
    val caseId: String,
    val justiceMetric: Int,
    val proceduralIntegrity: Int,
    val conspiracyUnraveledPercentage: Int,
    val overallGrade: String,
    val appellateCritique: String,
    val targetSuspectId: String, // who was prosecuted/convicted
    val wasConvicted: Boolean,
    val actualCulpability: CulpabilityStatus
)


