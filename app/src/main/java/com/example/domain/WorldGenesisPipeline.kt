package com.example.domain

import android.util.Log
import com.example.api.LlmClient
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

class WorldBibleValidator {
    fun validate(bible: WorldBible): Boolean {
        try {
            val locationIds = bible.locations.keys
            val evidenceIds = (bible.physicalEvidenceMap.keys + bible.digitalEvidenceMap.keys).toSet()

            // 1. Alibi Check: Check if characters' alibis point to valid locations (if specified)
            for ((_, character) in bible.characters) {
                val actualLoc = character.alibi.actualWhereabouts
                // Standardizing some check or making sure it's valid if non-empty
                if (actualLoc.isNotEmpty() && !locationIds.any { it.equals(actualLoc, ignoreCase = true) || actualLoc.contains(it, ignoreCase = true) }) {
                    Log.w("WorldBibleValidator", "Alibi check failed: actual location '$actualLoc' for '${character.name}' not found in locations list.")
                }
            }

            // 2. Lie Check: Every contradictoryEvidenceId should be a valid evidence ID in the maps
            for ((_, character) in bible.characters) {
                for (lie in character.lies) {
                    val evId = lie.contradictoryEvidenceId
                    if (evId.isNotEmpty() && !evidenceIds.contains(evId)) {
                        Log.w("WorldBibleValidator", "Lie check warning: evidence ID '$evId' not in physical/digital evidence maps.")
                    }
                }
            }

            // 3. Culpability Check: Verify we have at least one PRINCIPAL
            val principals = bible.characters.values.count { it.culpability == CulpabilityStatus.PRINCIPAL }
            if (principals < 1) {
                Log.w("WorldBibleValidator", "Culpability check failed: No character marked as PRINCIPAL.")
                return false
            }

            return true
        } catch (e: Exception) {
            Log.e("WorldBibleValidator", "Exception during validation: ${e.message}", e)
            return false
        }
    }
}

class WorldGenesisPipeline(
    private val llmClient: LlmClient,
    private val validator: WorldBibleValidator = WorldBibleValidator()
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun generateWorldBible(
        params: CaseGenerationParameters,
        apiKey: String,
        config: LlmEndpointConfig?,
        coldCaseDigest: com.example.data.model.ColdCaseDigest? = null,
        onProgress: (String) -> Unit
    ): Result<WorldBible> = withContext(Dispatchers.Default) {
        val needsApiKey = config?.requiresApiKey ?: true
        val isApiKeyPresent = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
        if (needsApiKey && !isApiKeyPresent) {
            onProgress("No API Key detected. Generating high-fidelity procedural offline mystery...")
            return@withContext Result.success(generateOfflineWorldBible(params))
        }

        try {
            onProgress("PASS 1: Formulating core crime narrative and timeline...")
            
            val digestText = if (coldCaseDigest != null) {
                """
                
                IMPORTANT META-NARRATIVE LINKAGE:
                This new case MUST be narratively linked to the following Cold Case:
                Cold Case Summary: ${coldCaseDigest.summary}
                Key Players from Cold Case: ${coldCaseDigest.keyPlayers.joinToString()}
                Unresolved Threads: ${coldCaseDigest.unresolvedThreads.joinToString()}
                
                Ensure the core crime, motives, or suspects relate back to these unresolved threads.
                """
            } else ""

            val narrativePrompt = """
                You are PASS 1 of Project Themis World Genesis.
                Generate a complete crime narrative JSON for the following:
                Archetype: ${params.archetype.name}
                Complexity Level: ${params.complexityLevel}
                Allow Conspiracies: ${params.allowConspiracies}
                Setting: ${params.setting}
                Tone: ${params.tone}
                $digestText
                
                Your response must be JSON containing "caseTitle", "tone", "settingProfile" (name, description, era, socioeconomicContext), "coreCrime", "absoluteTruth", "suspectCulpabilities" (Map from suspect ID string to PRINCIPAL/ACCOMPLICE/COMPLETELY_INNOCENT), and "timeline" (List of JSON objects with timestamp, timeDisplay, eventDescription, keyInvolvedNpcIds).
                
                Respond ONLY with a valid raw JSON block. No markdown wrappers.
            """.trimIndent()
            
            val pass1Response = generateLlmJson(narrativePrompt, apiKey, config)
            val coreNarrative = parsePass1(pass1Response) ?: throw Exception("Failed to parse Pass 1 JSON")
            
            onProgress("PASS 2: Generating deeply profiled suspects and witnesses...")
            val charactersPrompt = """
                You are PASS 2 of Project Themis.
                Based on this narrative and timeline:
                $pass1Response
                
                Generate a JSON map of CharacterProfile. Keys are character IDs (e.g. "suspect_john").
                Each profile must have "id", "name", "role", "personality" (cooperativeness (0.0-1.0), deceptionSkill, emotionalStability, verbosity, speechPattern), "culpability" (from the list of CulpabilityStatus: PRINCIPAL, ACCOMPLICE, ACCESSORY_AFTER_THE_FACT, COMPLETELY_INNOCENT), "hiddenKnowledge" (List of strings), "publicKnowledge" (List of strings), "lies" (List of LieProfile: lieContent, triggerCondition, tellIndicator, contradictoryEvidenceId), "stressTriggers" (List of strings), "alibi" (claimedWhereabouts, actualWhereabouts, corroboratingNpcId), "relationships" (Map of other Character IDs to RelationshipDetail: sentiment, trustLevel (0.0-1.0), backStory).
                
                Respond ONLY with valid raw JSON.
            """.trimIndent()
            
            val pass2Response = generateLlmJson(charactersPrompt, apiKey, config)
            val characters = parsePass2(pass2Response) ?: throw Exception("Failed to parse Pass 2 JSON")

            onProgress("PASS 3: Building interactive crime scenes and locales...")
            val locationsPrompt = """
                You are PASS 3 of Project Themis.
                Based on characters:
                $pass2Response
                
                Generate a JSON map of LocationProfile. Keys are location IDs (e.g. "grand_ballroom").
                Each location must have "id", "name", "description" (rich details), "interactiveElements" (List of name, description, keyEvidenceId), "hiddenElements" (List of id, description, revealPrompt, hiddenEvidenceId), and "ambientDetails" (List of strings).
                
                Respond ONLY with valid raw JSON.
            """.trimIndent()
            
            val pass3Response = generateLlmJson(locationsPrompt, apiKey, config)
            val locations = parsePass3(pass3Response) ?: throw Exception("Failed to parse Pass 3 JSON")

            onProgress("PASS 4: Fabricating physical and digital forensic evidence...")
            val evidencePrompt = """
                You are PASS 4 of Project Themis.
                Based on timeline, locations, and characters:
                $pass1Response
                $pass3Response
                
                Generate a JSON object containing two maps: "physicalEvidenceMap" (keys are evidence IDs, values are PhysicalEvidenceProfile: id, locationId, discoveryCondition (searchActionRequired, warrantRequired, baseDifficulty), forensicAnalysis (text, analyticalDetails: Map, chemicalAlchemicalResidues: List), chainOfCustodyRequirements: List) and "digitalEvidenceMap" (keys are digital IDs, values are DigitalEvidenceProfile: id, type, ownerNpcId, sourceDeviceName, decryptedContent, associatedMetadata: Map).
                
                Respond ONLY with valid raw JSON.
            """.trimIndent()
            
            val pass4Response = generateLlmJson(evidencePrompt, apiKey, config)
            val evidence = parsePass4(pass4Response) ?: throw Exception("Failed to parse Pass 4 JSON")

            onProgress("PASS 5: Formulating statutes and legal precedents...")
            val legalPrompt = """
                You are PASS 5 of Project Themis.
                Based on archetype ${params.archetype.name} and core crime:
                ${coreNarrative.coreCrime}
                
                Generate a JSON LegalFramework object containing "jurisdiction", "applicableStatutes" (List of code, title, elements, sentencingRange), "bindingPrecedents" (List of citation, rulingDetail, legalPrinciple), "rulesOfEvidence" (List of ruleName, description, triggerException).
                
                Respond ONLY with valid raw JSON.
            """.trimIndent()
            
            val pass5Response = generateLlmJson(legalPrompt, apiKey, config)
            val legal = parsePass5(pass5Response) ?: throw Exception("Failed to parse Pass 5 JSON")

            val bible = WorldBible(
                meta = CaseMeta(
                    caseTitle = coreNarrative.caseTitle,
                    archetype = params.archetype,
                    setting = SettingProfile(
                        name = coreNarrative.settingProfile.name,
                        description = coreNarrative.settingProfile.description,
                        era = coreNarrative.settingProfile.era,
                        socioeconomicContext = coreNarrative.settingProfile.socioeconomicContext
                    ),
                    tone = coreNarrative.tone,
                    complexityLevel = params.complexityLevel,
                    generatedAt = System.currentTimeMillis()
                ),
                timeline = coreNarrative.timeline,
                locations = locations,
                characters = characters,
                physicalEvidenceMap = evidence.physicalEvidenceMap,
                digitalEvidenceMap = evidence.digitalEvidenceMap,
                legalFramework = legal,
                groundTruth = CaseGroundTruth(
                    caseId = UUID.randomUUID().toString(),
                    archetype = params.archetype,
                    coreCrime = coreNarrative.coreCrime,
                    absoluteTruth = coreNarrative.absoluteTruth,
                    suspectCulpabilities = coreNarrative.suspectCulpabilities,
                    criticalMissableClues = evidence.physicalEvidenceMap.keys.toList()
                )
            )

            if (validator.validate(bible)) {
                onProgress("World Bible validated successfully!")
                Result.success(bible)
            } else {
                onProgress("Validation discrepancy found! Re-generating with offline high-fidelity backup...")
                Result.success(generateOfflineWorldBible(params))
            }

        } catch (e: Exception) {
            Log.e("WorldGenesisPipeline", "LLM Genesis failed: ${e.message}", e)
            onProgress("Connection stalled. Running deterministic high-fidelity case generator...")
            Result.success(generateOfflineWorldBible(params))
        }
    }

    private suspend fun generateLlmJson(prompt: String, apiKey: String, config: LlmEndpointConfig?): String {
        val resp = llmClient.generateGameResponse(prompt, emptyList(), GamePhase.INVESTIGATION, config, apiKey)
        var cleanText = resp.textResponse.trim()
        if (cleanText.contains("```json")) {
            cleanText = cleanText.substringAfter("```json").substringBeforeLast("```")
        } else if (cleanText.contains("```")) {
            cleanText = cleanText.substringAfter("```").substringBeforeLast("```")
        }
        cleanText = extractJsonBlock(cleanText)
        return cleanText.trim()
    }

    private fun extractJsonBlock(text: String): String {
        val trimmed = text.trim()
        
        // Find first occurrence of '{' or '['
        val firstBrace = trimmed.indexOf('{')
        val firstBracket = trimmed.indexOf('[')
        
        val startIndex = when {
            firstBrace == -1 -> firstBracket
            firstBracket == -1 -> firstBrace
            else -> minOf(firstBrace, firstBracket)
        }
        
        if (startIndex == -1) return trimmed // Fallback to original
        
        // Find last occurrence of '}' or ']'
        val lastBrace = trimmed.lastIndexOf('}')
        val lastBracket = trimmed.lastIndexOf(']')
        
        val endIndex = maxOf(lastBrace, lastBracket)
        
        if (endIndex == -1 || endIndex <= startIndex) return trimmed
        
        return trimmed.substring(startIndex, endIndex + 1)
    }

    // --- Parsing Helpers ---
    private fun parsePass1(json: String): CoreNarrativeResponse? {
        return try {
            moshi.adapter(CoreNarrativeResponse::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePass2(json: String): Map<String, CharacterProfile>? {
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, CharacterProfile::class.java)
            moshi.adapter<Map<String, CharacterProfile>>(type).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePass3(json: String): Map<String, LocationProfile>? {
        return try {
            val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, LocationProfile::class.java)
            moshi.adapter<Map<String, LocationProfile>>(type).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePass4(json: String): EvidenceMapsResponse? {
        return try {
            moshi.adapter(EvidenceMapsResponse::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePass5(json: String): LegalFramework? {
        return try {
            moshi.adapter(LegalFramework::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    // --- JSON Support Classes ---
    @com.squareup.moshi.JsonClass(generateAdapter = true)
    data class CoreNarrativeResponse(
        val caseTitle: String,
        val tone: String,
        val settingProfile: SettingProfileJson,
        val coreCrime: String,
        val absoluteTruth: String,
        val suspectCulpabilities: Map<String, CulpabilityStatus>,
        val timeline: List<TimelineEvent>
    )

    @com.squareup.moshi.JsonClass(generateAdapter = true)
    data class SettingProfileJson(
        val name: String,
        val description: String,
        val era: String,
        val socioeconomicContext: String
    )

    @com.squareup.moshi.JsonClass(generateAdapter = true)
    data class EvidenceMapsResponse(
        val physicalEvidenceMap: Map<String, PhysicalEvidenceProfile>,
        val digitalEvidenceMap: Map<String, DigitalEvidenceProfile>
    )

    // --- Deterministic Offline Generator ---
    fun generateOfflineWorldBible(params: CaseGenerationParameters): WorldBible {
        val rand = Random(params.hashCode() + System.currentTimeMillis().toInt())
        val caseId = UUID.randomUUID().toString()

        val settingName = params.setting
        val tone = params.tone
        val title = "The Mystery of ${params.setting.substringBefore(" ")}: " + when (params.archetype) {
            CaseArchetype.HOMICIDE -> "A Fatal Miscalculation"
            CaseArchetype.FINANCIAL_FRAUD -> "The Embezzled Ledger"
            CaseArchetype.CORPORATE_ESPIONAGE -> "Silent Protocol Breach"
            CaseArchetype.CYBERCRIME -> "The Encrypted Ledger Ransom"
            CaseArchetype.CRIMES_OF_PASSION -> "The Broken Alchemical Ring"
            CaseArchetype.ORGANIZED_CRIME -> "The Syndicate's Syndicate"
        }

        val primarySuspectName = when (params.archetype) {
            CaseArchetype.HOMICIDE -> "Cassius Vance"
            CaseArchetype.FINANCIAL_FRAUD -> "Sterling Croft"
            else -> "Aurelia Sterling"
        }
        val innocentSuspectName = "Lady Genevieve"
        val accompliceName = "Silas Thorne"

        val coreCrime = when (params.archetype) {
            CaseArchetype.HOMICIDE -> "A prominent figure was found dead of poisoning at the local banquet. Traces of arsenic and rare alchemical powder remain on the drinking goblets."
            CaseArchetype.FINANCIAL_FRAUD -> "Over two million credits went missing from the Royal Treasury. The encrypted vault logs indicate a specialized override key was used."
            else -> "A secure vault has been raided, with several key ledgers and critical artifacts missing. The alarm system was deactivated from an internal console."
        }

        val absoluteTruth = "The crime was executed by $primarySuspectName, who acted as the primary mastermind (PRINCIPAL). " +
                "They planned it meticulously over weeks due to old rivalries. " +
                "They were assisted by $accompliceName (ACCOMPLICE), who provided the mechanical schematics and disabled the security nodes at 11:15 PM. " +
                "$innocentSuspectName had absolutely no knowledge of this and is completely innocent, but was framed with a planted piece of evidence in their personal quarters."

        val timeline = listOf(
            TimelineEvent(System.currentTimeMillis() - 3600000 * 4, "09:00 PM", "Victim hosts a private greeting in the library. Witnesses note minor disagreements.", listOf(primarySuspectName, innocentSuspectName)),
            TimelineEvent(System.currentTimeMillis() - 3600000 * 2, "11:00 PM", "The power momentarily flickers. Accomplice Thorne is spotted near the breaker panel.", listOf(accompliceName)),
            TimelineEvent(System.currentTimeMillis() - 3600000, "11:15 PM", "The security vault log registers an authorized bypass using an override key.", listOf(primarySuspectName)),
            TimelineEvent(System.currentTimeMillis(), "12:00 AM", "The crime is discovered. The victim is found incapacitated and the alarm is sounded.", listOf(innocentSuspectName))
        )

        val characters = mapOf(
            "suspect_1" to CharacterProfile(
                id = "suspect_1",
                name = primarySuspectName,
                role = "Primary Business Associate",
                personality = PersonalityMatrix(0.3f, 0.9f, 0.8f, 0.4f, "Cold, logical, precise legal speech"),
                culpability = CulpabilityStatus.PRINCIPAL,
                hiddenKnowledge = listOf("Possesses a custom forged override key", "Had financial motivations to ruin the victim"),
                publicKnowledge = listOf("Attended the banquet briefly", "Spoke with the victim about standard contracts"),
                lies = listOf(
                    LieProfile(
                        "I was never near the secondary study or the vault console all evening.",
                        "When asked about their whereabouts at 11:15 PM",
                        "Briefly adjusts collar, pauses before answering in a highly clinical tone",
                        "ev_override_key"
                    )
                ),
                stressTriggers = listOf("override key", "study", "the alchemical ledger", "forgery"),
                alibi = AlibiProfile("In the grand lounge talking with guests", "Inside the private study bypassing the vault", null),
                relationships = mapOf(
                    "suspect_2" to RelationshipDetail("Arrogant suspicion", 0.2f, "Considers Genevieve soft and easily framed"),
                    "suspect_3" to RelationshipDetail("Cooperative transactional alliance", 0.7f, "Hired Thorne to disable the local power nodes")
                )
            ),
            "suspect_2" to CharacterProfile(
                id = "suspect_2",
                name = innocentSuspectName,
                role = "Chief Scholar",
                personality = PersonalityMatrix(0.8f, 0.2f, 0.4f, 0.7f, "Nervous, high-register trembling stutter"),
                culpability = CulpabilityStatus.COMPLETELY_INNOCENT,
                hiddenKnowledge = listOf("Saw Cassius near the study with a silver mechanical key"),
                publicKnowledge = listOf("Arrived late", "Found a suspicious vial planted in her desk"),
                lies = emptyList(),
                stressTriggers = listOf("poison", "the silver vial", "murder"),
                alibi = AlibiProfile("In the library studying regional ledgers", "In the library studying regional ledgers", "suspect_3"),
                relationships = mapOf(
                    "suspect_1" to RelationshipDetail("Petrified fear", 0.1f, "Suspects Cassius of extreme malice"),
                    "suspect_3" to RelationshipDetail("Indifferent", 0.5f, "Sees Thorne only as a maintenance technician")
                )
            ),
            "suspect_3" to CharacterProfile(
                id = "suspect_3",
                name = accompliceName,
                role = "Lead Maintenance Technician",
                personality = PersonalityMatrix(0.5f, 0.6f, 0.5f, 0.6f, "Slightly informal street slang, defensive"),
                culpability = CulpabilityStatus.ACCOMPLICE,
                hiddenKnowledge = listOf("Received 50,000 credits to trigger the power blackout at 11:00 PM"),
                publicKnowledge = listOf("Was fixing the heating conduits in the basement"),
                lies = listOf(
                    LieProfile(
                        "The power outage was a simple transformer blowup in the boiler room. Pure accident.",
                        "When asked about the 11:00 PM blackout",
                        "Avoids direct eye contact, bites lip, claims technical error",
                        "ev_burnt_wire"
                    )
                ),
                stressTriggers = listOf("breaker panel", "wirecutter", "bank ledger", "blackout"),
                alibi = AlibiProfile("Basement maintenance room", "Basement electrical junction room cutting the main wire", null),
                relationships = mapOf(
                    "suspect_1" to RelationshipDetail("Subservient caution", 0.4f, "Owes Cassius immense debt"),
                    "suspect_2" to RelationshipDetail("Pity", 0.6f, "Knows she is innocent and feels slightly guilty")
                )
            )
        )

        val locations = mapOf(
            "study" to LocationProfile(
                id = "study",
                name = "Magistrate's Grand Study",
                description = "A richly decorated office adorned with oak shelves, antique parchments, and a central bronze desk where official records are stamped.",
                interactiveElements = listOf(
                    InteractiveElement("Central Mahogany Desk", "Sturdy and orderly, save for a small scratch near the top drawer lock.", "ev_override_key")
                ),
                hiddenElements = listOf(
                    HiddenElement("secret_safe", "A small, wall-mounted steel safe hidden behind a painting of the First Tribunal.", "Reveal secret compartment", "ev_burnt_wire")
                ),
                ambientDetails = listOf("Smells of ancient beeswax and dust", "The fireplace emits a soft, rhythmic crackle")
            ),
            "library" to LocationProfile(
                id = "library",
                name = "Royal Archive Library",
                description = "Towering shelves packed with centuries of binding law, alchemical catalogs, and municipal ledgers. Extremely quiet.",
                interactiveElements = listOf(
                    InteractiveElement("Victim's Personal Desk", "A workspace cluttered with handwritten correspondence and a glass decanter.", "ev_silver_vial")
                ),
                hiddenElements = emptyList(),
                ambientDetails = listOf("Deep ticking from a pendulum grandfather clock", "Cold drafts coming through stained-glass windows")
            )
        )

        val physicalEvidence = mapOf(
            "ev_override_key" to PhysicalEvidenceProfile(
                id = "ev_override_key",
                locationId = "study",
                discoveryCondition = DiscoveryCondition("Inspect desk scratch and search hidden groove", false, 15),
                forensicAnalysis = ForensicResult(
                    "A customized alchemical silver key with micro-machined tumblers. Fingerprints of Cassius Vance are cleanly lifted from the grip.",
                    mapOf("Composition" to "Silver-Palladium Alloy", "Match Rating" to "99.4% to Cassius"),
                    listOf("Mechanical lubricants", "Traces of alchemical silver-lead powder")
                ),
                chainOfCustodyRequirements = listOf("Bagged in protective velvet", "Authenticated by high magistrate signet")
            ),
            "ev_burnt_wire" to PhysicalEvidenceProfile(
                id = "ev_burnt_wire",
                locationId = "study",
                discoveryCondition = DiscoveryCondition("Search secret safe behind First Tribunal painting", true, 20),
                forensicAnalysis = ForensicResult(
                    "A cleanly snipped insulated wire showing signs of rapid electrical arching. Copper residues match the basement main fuse box exactly.",
                    mapOf("Cutting Tool" to "High-tension shear", "Arc temperature" to "Approx 1200C"),
                    listOf("Polymer plastic insulation", "Basement sulfur-coal soot")
                ),
                chainOfCustodyRequirements = listOf("Stored in sealed dry vial")
            ),
            "ev_silver_vial" to PhysicalEvidenceProfile(
                id = "ev_silver_vial",
                locationId = "library",
                discoveryCondition = DiscoveryCondition("Search personal desk center drawer", false, 10),
                forensicAnalysis = ForensicResult(
                    "A small alchemical flask containing a trace element of arsenic poison. Curiously, it was found wrapped in a handkerchief belonging to Cassius Vance, but placed in Lady Genevieve's drawer.",
                    mapOf("Poison Type" to "Refined Arsenic Trioxide", "Vial Material" to "Sterling Silver"),
                    listOf("Arsenic residue", "Cotton-silk blend threads matching Cassius")
                ),
                chainOfCustodyRequirements = listOf("Double-sealed in glass tubes")
            )
        )

        val digitalEvidence = mapOf(
            "dig_bank_records" to DigitalEvidenceProfile(
                id = "dig_bank_records",
                type = "Financial ledger",
                ownerNpcId = accompliceName,
                sourceDeviceName = "Treasury Terminal 04",
                decryptedContent = "TRANSACTION ALERT: Deposit of 50,000 credits to Silas Thorne from account marked 'Vance Estates'. Timestamp: 11:05 PM, on the night of the crime.",
                associatedMetadata = mapOf("Originating Router" to "SecureNode_09", "Encryption" to "AES-256")
            )
        )

        val statutes = listOf(
            Statute("Magisterial Code §101.2", "High Treason and Homicide", listOf("Intentionally caused death of a peer", "Utilized alchemical or chemical poisons", "Bypassed royal seal"), "Life imprisonment or public exile"),
            Statute("Penal Code §404.1", "Procedural Sabotage", listOf("Willfully disabled municipal utility breakers", "Aided a felon in bypassing a secure lock"), "5 to 15 years in regional quarries"),
            Statute("Financial Code §902.3", "Illicit Syndicate Funding", listOf("Transferred credits exceeding 10,000 without state seal", "Aided primary perpetrator under a contract"), "Asset forfeiture and 10 years confinement")
        )

        val precedents = listOf(
            LegalPrecedent("State v. Bartholomew (1894)", "Unsigned override keys do not constitute admissible proof of intent unless paired with unique forensic identifiers or matching witness testimony.", "Corroboration of Digital/Physical Marks"),
            LegalPrecedent("In re Alchemical Toxins (1912)", "The possession of alchemical compounds classified as highly lethal is a strict liability offence if found outside certified apothecary premises.", "Strict Liability for Poison Possession")
        )

        val evidenceRules = listOf(
            EvidenceRule("Rule of Sound Custody", "Evidence collected without a valid search warrant when a warrant is required is deemed inadmissible in peer-level courts.", "Warrant Exemption during Active Blackout"),
            EvidenceRule("Hearsay Exclusion Rule", "Statements repeating third-party claims without an active corroborating source cannot be entered as primary proof of guilt.", "Dying Declarations")
        )

        val legalFramework = LegalFramework("High Court of Themis Jurisdiction", statutes, precedents, evidenceRules)

        return WorldBible(
            meta = CaseMeta(
                caseTitle = title,
                archetype = params.archetype,
                setting = SettingProfile(settingName, "An immersive setting optimized for $tone play.", "Late 19th Century Magisterial Era", "Extreme stratification between high council and low technicians"),
                tone = tone,
                complexityLevel = params.complexityLevel,
                generatedAt = System.currentTimeMillis()
            ),
            timeline = timeline,
            locations = locations,
            characters = characters,
            physicalEvidenceMap = physicalEvidence,
            digitalEvidenceMap = digitalEvidence,
            legalFramework = legalFramework,
            groundTruth = CaseGroundTruth(
                caseId = caseId,
                archetype = params.archetype,
                coreCrime = coreCrime,
                absoluteTruth = absoluteTruth,
                suspectCulpabilities = mapOf(
                    "suspect_1" to CulpabilityStatus.PRINCIPAL,
                    "suspect_2" to CulpabilityStatus.COMPLETELY_INNOCENT,
                    "suspect_3" to CulpabilityStatus.ACCOMPLICE
                ),
                criticalMissableClues = listOf("ev_override_key", "ev_burnt_wire")
            )
        )
    }
}
