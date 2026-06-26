package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorldBible(
    val meta: CaseMeta,
    val timeline: List<TimelineEvent>,
    val locations: Map<String, LocationProfile>,
    val characters: Map<String, CharacterProfile>,
    val physicalEvidenceMap: Map<String, PhysicalEvidenceProfile>,
    val digitalEvidenceMap: Map<String, DigitalEvidenceProfile>,
    val legalFramework: LegalFramework,
    val groundTruth: CaseGroundTruth
)

@JsonClass(generateAdapter = true)
data class CaseMeta(
    val caseTitle: String,
    val archetype: CaseArchetype,
    val setting: SettingProfile,
    val tone: String,
    val complexityLevel: Int,
    val generatedAt: Long
)

@JsonClass(generateAdapter = true)
data class SettingProfile(
    val name: String,
    val description: String,
    val era: String,
    val socioeconomicContext: String
)

@JsonClass(generateAdapter = true)
data class TimelineEvent(
    val timestamp: Long,
    val timeDisplay: String,
    val eventDescription: String,
    val keyInvolvedNpcIds: List<String>
)

@JsonClass(generateAdapter = true)
data class CharacterProfile(
    val id: String,
    val name: String,
    val role: String,
    val personality: PersonalityMatrix,
    val culpability: CulpabilityStatus,
    val hiddenKnowledge: List<String>,
    val publicKnowledge: List<String>,
    val lies: List<LieProfile>,
    val stressTriggers: List<String>,
    val alibi: AlibiProfile,
    val relationships: Map<String, RelationshipDetail>
)

@JsonClass(generateAdapter = true)
data class PersonalityMatrix(
    val cooperativeness: Float,
    val deceptionSkill: Float,
    val emotionalStability: Float,
    val verbosity: Float,
    val speechPattern: String
)

@JsonClass(generateAdapter = true)
data class LieProfile(
    val lieContent: String,
    val triggerCondition: String,
    val tellIndicator: String,
    val contradictoryEvidenceId: String
)

@JsonClass(generateAdapter = true)
data class AlibiProfile(
    val claimedWhereabouts: String,
    val actualWhereabouts: String,
    val corroboratingNpcId: String?
)

@JsonClass(generateAdapter = true)
data class RelationshipDetail(
    val sentiment: String,
    val trustLevel: Float,
    val backStory: String
)

@JsonClass(generateAdapter = true)
data class LocationProfile(
    val id: String,
    val name: String,
    val description: String,
    val interactiveElements: List<InteractiveElement>,
    val hiddenElements: List<HiddenElement>,
    val ambientDetails: List<String>
)

@JsonClass(generateAdapter = true)
data class InteractiveElement(
    val name: String,
    val description: String,
    val keyEvidenceId: String?
)

@JsonClass(generateAdapter = true)
data class HiddenElement(
    val id: String,
    val description: String,
    val revealPrompt: String,
    val hiddenEvidenceId: String
)

@JsonClass(generateAdapter = true)
data class PhysicalEvidenceProfile(
    val id: String,
    val locationId: String,
    val discoveryCondition: DiscoveryCondition,
    val forensicAnalysis: ForensicResult,
    val chainOfCustodyRequirements: List<String>
)

@JsonClass(generateAdapter = true)
data class DiscoveryCondition(
    val searchActionRequired: String,
    val warrantRequired: Boolean,
    val baseDifficulty: Int
)

@JsonClass(generateAdapter = true)
data class ForensicResult(
    val text: String,
    val analyticalDetails: Map<String, String>,
    val chemicalAlchemicalResidues: List<String>
)

@JsonClass(generateAdapter = true)
data class DigitalEvidenceProfile(
    val id: String,
    val type: String,
    val ownerNpcId: String,
    val sourceDeviceName: String,
    val decryptedContent: String,
    val associatedMetadata: Map<String, String>
)

@JsonClass(generateAdapter = true)
data class LegalFramework(
    val jurisdiction: String,
    val applicableStatutes: List<Statute>,
    val bindingPrecedents: List<LegalPrecedent>,
    val rulesOfEvidence: List<EvidenceRule>
)

@JsonClass(generateAdapter = true)
data class Statute(
    val code: String,
    val title: String,
    val elements: List<String>,
    val sentencingRange: String
)

@JsonClass(generateAdapter = true)
data class LegalPrecedent(
    val citation: String,
    val rulingDetail: String,
    val legalPrinciple: String
)

@JsonClass(generateAdapter = true)
data class EvidenceRule(
    val ruleName: String,
    val description: String,
    val triggerException: String?
)
