package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

class GameRepository(val db: ThemisDatabase) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun insertWorldBible(caseId: String, bible: WorldBible) {
        val adapter = moshi.adapter(WorldBible::class.java)
        val json = adapter.toJson(bible)
        db.worldBibleDao().insertWorldBible(WorldBibleEntity(caseId, json))
    }

    suspend fun getWorldBible(caseId: String): WorldBible? {
        val entity = db.worldBibleDao().getWorldBible(caseId) ?: return null
        val adapter = moshi.adapter(WorldBible::class.java)
        return try {
            adapter.fromJson(entity.bibleJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insertCaseProgress(progress: CaseProgress) {
        db.caseProgressDao().insertCaseProgress(
            CaseProgressEntity(
                caseId = progress.caseId,
                status = progress.status.name,
                daysElapsed = progress.daysElapsed,
                maxDaysBeforeCold = progress.maxDaysBeforeCold,
                activeLeadsRemaining = progress.activeLeadsRemaining,
                publicPressure = progress.publicPressure,
                degradationLevel = progress.degradationLevel
            )
        )
    }

    suspend fun getCaseProgress(caseId: String): CaseProgress? {
        val entity = db.caseProgressDao().getCaseProgress(caseId) ?: return null
        return CaseProgress(
            caseId = entity.caseId,
            status = try { CaseStatus.valueOf(entity.status) } catch (e: Exception) { CaseStatus.ACTIVE },
            daysElapsed = entity.daysElapsed,
            maxDaysBeforeCold = entity.maxDaysBeforeCold,
            activeLeadsRemaining = entity.activeLeadsRemaining,
            publicPressure = entity.publicPressure,
            degradationLevel = entity.degradationLevel
        )
    }

    suspend fun getAllCaseProgress(): List<CaseProgress> {
        return db.caseProgressDao().getAllCaseProgress().map { entity ->
            CaseProgress(
                caseId = entity.caseId,
                status = try { CaseStatus.valueOf(entity.status) } catch (e: Exception) { CaseStatus.ACTIVE },
                daysElapsed = entity.daysElapsed,
                maxDaysBeforeCold = entity.maxDaysBeforeCold,
                activeLeadsRemaining = entity.activeLeadsRemaining,
                publicPressure = entity.publicPressure,
                degradationLevel = entity.degradationLevel
            )
        }
    }

    // --- Flows of Domain Models ---

    val allEvidence: Flow<List<EvidenceItem>> = db.evidenceDao().getAllEvidence().map { entities ->
        entities.map { entity ->
            EvidenceItem(
                id = entity.id,
                name = entity.name,
                physicalDescription = entity.physicalDescription,
                forensicReport = entity.forensicReport,
                collectionContext = CollectionContext(
                    locationFound = entity.locationFound,
                    collectingOfficer = entity.collectingOfficer,
                    timestamp = entity.timestamp,
                    warrantUsed = entity.warrantUsed
                ),
                userAnnotations = entity.userAnnotations,
                admissibilityStatus = try {
                    AdmissibilityStatus.valueOf(entity.admissibilityStatus)
                } catch (e: Exception) {
                    AdmissibilityStatus.UNVERIFIED
                }
            )
        }
    }

    val allLinks: Flow<List<EvidenceLink>> = db.evidenceLinkDao().getAllLinks().map { entities ->
        entities.map { entity ->
            EvidenceLink(
                id = entity.id,
                sourceEvidenceId = entity.sourceEvidenceId,
                targetEvidenceId = entity.targetEvidenceId,
                relationshipType = try {
                    RelationshipType.valueOf(entity.relationshipType)
                } catch (e: Exception) {
                    RelationshipType.CORROBORATES
                },
                magistrateJustification = entity.magistrateJustification
            )
        }
    }

    val allNpcs: Flow<List<NPC>> = db.npcDao().getAllNpcs().map { entities ->
        entities.map { entity ->
            NPC(
                id = entity.id,
                name = entity.name,
                role = entity.role,
                stress = entity.stress,
                statement = entity.statement,
                profile = entity.profile,
                hiddenMotive = entity.hiddenMotive
            )
        }
    }

    val chatMessages: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessages().map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                phase = if (entity.phaseName == GamePhase.COURTROOM.name) GamePhase.COURTROOM else GamePhase.INVESTIGATION,
                sender = entity.sender,
                text = entity.text,
                timestamp = entity.timestamp,
                isSystem = entity.isSystem,
                isToolCall = entity.isToolCall,
                toolName = entity.toolName
            )
        }
    }

    suspend fun getAllMessagesDirect(): List<ChatMessage> {
        return db.chatMessageDao().getAllMessagesDirect().map { entity ->
            ChatMessage(
                id = entity.id,
                phase = if (entity.phaseName == GamePhase.COURTROOM.name) GamePhase.COURTROOM else GamePhase.INVESTIGATION,
                sender = entity.sender,
                text = entity.text,
                timestamp = entity.timestamp,
                isSystem = entity.isSystem,
                isToolCall = entity.isToolCall,
                toolName = entity.toolName
            )
        }
    }

    suspend fun getAllEvidenceDirect(): List<EvidenceItem> {
        return db.evidenceDao().getEvidenceListDirect().map { entity ->
            EvidenceItem(
                id = entity.id,
                name = entity.name,
                physicalDescription = entity.physicalDescription,
                forensicReport = entity.forensicReport,
                collectionContext = CollectionContext(
                    locationFound = entity.locationFound,
                    collectingOfficer = entity.collectingOfficer,
                    timestamp = entity.timestamp,
                    warrantUsed = entity.warrantUsed
                ),
                userAnnotations = entity.userAnnotations,
                admissibilityStatus = try { AdmissibilityStatus.valueOf(entity.admissibilityStatus) } catch (e: Exception) { AdmissibilityStatus.UNVERIFIED }
            )
        }
    }

    suspend fun getNpcsDirect(): List<NPC> {
        return db.npcDao().getNpcListDirect().map { entity ->
            NPC(
                id = entity.id,
                name = entity.name,
                role = entity.role,
                stress = entity.stress,
                statement = entity.statement,
                profile = entity.profile,
                hiddenMotive = entity.hiddenMotive
            )
        }
    }

    // --- Write Operations ---

    suspend fun addEvidence(evidence: EvidenceItem) {
        db.evidenceDao().insertEvidence(
            EvidenceEntity(
                id = evidence.id,
                name = evidence.name,
                physicalDescription = evidence.physicalDescription,
                forensicReport = evidence.forensicReport,
                locationFound = evidence.collectionContext.locationFound,
                collectingOfficer = evidence.collectionContext.collectingOfficer,
                timestamp = evidence.collectionContext.timestamp,
                warrantUsed = evidence.collectionContext.warrantUsed,
                userAnnotations = evidence.userAnnotations,
                admissibilityStatus = evidence.admissibilityStatus.name
            )
        )
    }

    suspend fun addLink(link: EvidenceLink) {
        db.evidenceLinkDao().insertLink(
            EvidenceLinkEntity(
                sourceEvidenceId = link.sourceEvidenceId,
                targetEvidenceId = link.targetEvidenceId,
                relationshipType = link.relationshipType.name,
                magistrateJustification = link.magistrateJustification
            )
        )
    }

    suspend fun updateNpcStress(id: String, stress: Int) {
        val current = db.npcDao().getNpcById(id) ?: return
        val newStress = (current.stress + stress).coerceIn(0, 100)
        db.npcDao().updateNpcStress(id, newStress)
    }

    suspend fun updateNpcStatement(id: String, statement: String) {
        val current = db.npcDao().getNpcById(id) ?: return
        db.npcDao().insertNpc(current.copy(statement = statement))
    }

    suspend fun insertMessage(message: ChatMessage) {
        db.chatMessageDao().insertMessage(
            ChatMessageEntity(
                id = message.id,
                phaseName = message.phase.name,
                sender = message.sender,
                text = message.text,
                timestamp = message.timestamp,
                isSystem = message.isSystem,
                isToolCall = message.isToolCall,
                toolName = message.toolName
            )
        )
    }

    suspend fun getGamePhase(): GamePhase {
        val phaseStr = db.worldStateDao().getValue("game_phase") ?: GamePhase.INVESTIGATION.name
        return if (phaseStr == GamePhase.COURTROOM.name) GamePhase.COURTROOM else GamePhase.INVESTIGATION
    }

    suspend fun setGamePhase(phase: GamePhase) {
        db.worldStateDao().insertState(WorldStateEntity("game_phase", phase.name))
    }

    suspend fun getGameTime(): String {
        return db.worldStateDao().getValue("game_time") ?: "Day 1, 10:00 AM"
    }

    suspend fun setGameTime(time: String) {
        db.worldStateDao().insertState(WorldStateEntity("game_time", time))
    }

    suspend fun getWorldState(key: String): String? {
        return db.worldStateDao().getValue(key)
    }

    suspend fun updateWorldState(key: String, value: String) {
        db.worldStateDao().insertState(WorldStateEntity(key, value))
    }

    // --- Seeding ---

    suspend fun seedInitialData() {
        // Clear all first
        db.evidenceDao().clearAll()
        db.evidenceLinkDao().clearAll()
        db.npcDao().clearAll()
        db.chatMessageDao().clearAll()
        db.worldStateDao().clearAll()

        // Set state variables
        setGamePhase(GamePhase.INVESTIGATION)
        setGameTime("Day 1, 10:00 AM")
        updateWorldState("conflict_of_interest", "15") // starts low

        // Seed NPCs
        val npcs = listOf(
            NpcEntity(
                id = "beatrice",
                name = "Lady Beatrice",
                role = "Suspect (Widow)",
                stress = 25,
                statement = "I was walking in the rose garden when my husband collapsed. He has been under so much political pressure lately. I... I can't believe he's gone.",
                profile = "The Duke's elegant but guarded widow. Speaks with measured poise, but becomes defensive if asked about money.",
                hiddenMotive = "Stands to inherit the entire Duchy. Secretly in debt to Lord Vance, she was blackmailed into slipping Mandrake toxin into the Duke's goblet. She pretends to be devastated."
            ),
            NpcEntity(
                id = "gideon",
                name = "Gideon the Apothecary",
                role = "Witness (Apothecary)",
                stress = 45,
                statement = "I only prepare his daily sleeping draughts! I would never poison him. My chest of Mandrake extract is kept locked, but Lord Vance had access to it!",
                profile = "The nervous court apothecary. Fidgety, highly knowledgeable about rare toxic agents, terrifyingly aware that he is an easy scapegoat.",
                hiddenMotive = "Gideon left his cabinet unlocked. He knows Lord Vance stole the Mandrake extract, but is terrified Lord Vance will execute him or frame his daughter if he tells the truth."
            ),
            NpcEntity(
                id = "vance",
                name = "Lord Vance",
                role = "Suspect (Chancellor)",
                stress = 15,
                statement = "As the Chancellor, I am deeply shocked. We must find the killer immediately! I saw Gideon lingering near the wine serving cellar just before the banquet.",
                profile = "The Duke's arrogant, politically ambitious Chancellor. Coldly logical, smugly self-assured, and holds extreme sway in the kingdom.",
                hiddenMotive = "He orchestrated the entire assassination to seize political power. He poisoned the Duke's goblet himself and fabricated Lady Beatrice's signature in a poison purchasing ledger."
            )
        )
        db.npcDao().insertNpcs(npcs)

        // Seed initial Evidence
        addEvidence(
            EvidenceItem(
                id = "golden_goblet",
                name = "The Golden Goblet",
                physicalDescription = "The Duke's ceremonial goblet. Leftover wine is dark crimson, smelling faintly of Mandrake root toxin.",
                forensicReport = "Alchemical analysis reveals active Mandrake root toxin concentrated at 400ppm. Chemical signature matches Gideon's proprietary synthesis.",
                collectionContext = CollectionContext(
                    locationFound = "The Grand Banquet Hall",
                    collectingOfficer = "Magistrate",
                    timestamp = System.currentTimeMillis(),
                    warrantUsed = "WARRANT_001"
                ),
                userAnnotations = "Found at the center of the crime scene. The chemical traces suggest the poison was placed directly into his drink rather than cooked in the kitchen.",
                admissibilityStatus = AdmissibilityStatus.ADMISSIBLE
            )
        )

        // Seed introductory system message
        insertMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                phase = GamePhase.INVESTIGATION,
                sender = "System",
                text = "Welcome, Investigating Magistrate. Duke Sterling has been assassinated by poison at the state banquet. As Magistrate, you must search the Palace, interrogate the three prime suspects, gather admissible evidence, and try them in the Court of Themis. Your actions will define justice.",
                timestamp = System.currentTimeMillis(),
                isSystem = true
            )
        )
    }

    suspend fun getGroundTruth(caseId: String): CaseGroundTruth? {
        val entity = db.groundTruthDao().getGroundTruth(caseId) ?: return null
        return CaseGroundTruth(
            caseId = entity.caseId,
            archetype = try { CaseArchetype.valueOf(entity.archetype) } catch (e: Exception) { CaseArchetype.HOMICIDE },
            coreCrime = entity.coreCrime,
            absoluteTruth = entity.absoluteTruth,
            suspectCulpabilities = deserializeCulpabilities(entity.suspectCulpabilitiesJson),
            criticalMissableClues = deserializeList(entity.criticalMissableCluesJson)
        )
    }

    suspend fun insertGroundTruth(groundTruth: CaseGroundTruth) {
        val entity = com.example.data.local.CaseGroundTruthEntity(
            caseId = groundTruth.caseId,
            archetype = groundTruth.archetype.name,
            coreCrime = groundTruth.coreCrime,
            absoluteTruth = groundTruth.absoluteTruth,
            suspectCulpabilitiesJson = serializeCulpabilities(groundTruth.suspectCulpabilities),
            criticalMissableCluesJson = serializeList(groundTruth.criticalMissableClues)
        )
        db.groundTruthDao().insertGroundTruth(entity)
    }

    suspend fun getEvaluationReport(caseId: String): CaseEvaluationReport? {
        val entity = db.caseEvaluationDao().getEvaluationReport(caseId) ?: return null
        return CaseEvaluationReport(
            caseId = entity.caseId,
            justiceMetric = entity.justiceMetric,
            proceduralIntegrity = entity.proceduralIntegrity,
            conspiracyUnraveledPercentage = entity.conspiracyUnraveledPercentage,
            overallGrade = entity.overallGrade,
            appellateCritique = entity.appellateCritique,
            targetSuspectId = entity.targetSuspectId,
            wasConvicted = entity.wasConvicted,
            actualCulpability = try { CulpabilityStatus.valueOf(entity.actualCulpability) } catch (e: Exception) { CulpabilityStatus.COMPLETELY_INNOCENT }
        )
    }

    suspend fun insertEvaluationReport(report: CaseEvaluationReport) {
        val entity = com.example.data.local.CaseEvaluationReportEntity(
            caseId = report.caseId,
            justiceMetric = report.justiceMetric,
            proceduralIntegrity = report.proceduralIntegrity,
            conspiracyUnraveledPercentage = report.conspiracyUnraveledPercentage,
            overallGrade = report.overallGrade,
            appellateCritique = report.appellateCritique,
            targetSuspectId = report.targetSuspectId,
            wasConvicted = report.wasConvicted,
            actualCulpability = report.actualCulpability.name
        )
        db.caseEvaluationDao().insertEvaluationReport(entity)
    }

    suspend fun clearCaseData() {
        db.evidenceDao().clearAll()
        db.evidenceLinkDao().clearAll()
        db.npcDao().clearAll()
        db.chatMessageDao().clearAll()
        db.worldStateDao().clearAll()
        db.groundTruthDao().clearAll()
        db.caseEvaluationDao().clearAll()
        db.worldBibleDao().clearAll()
        db.caseProgressDao().clearAll()
    }

    suspend fun insertNpcs(npcs: List<NpcEntity>) {
        db.npcDao().insertNpcs(npcs)
    }

    // --- Linkage & Cold Case Digests ---
    suspend fun getLinkagesForCase(activeCaseId: String): List<com.example.data.model.CaseLinkage> {
        return db.caseLinkageDao().getLinkagesForCase(activeCaseId).map {
            com.example.data.model.CaseLinkage(
                activeCaseId = it.activeCaseId,
                coldCaseId = it.coldCaseId,
                connectionType = try { com.example.data.model.ConnectionType.valueOf(it.connectionType) } catch (e: Exception) { com.example.data.model.ConnectionType.COINCIDENCE },
                description = it.description
            )
        }
    }

    suspend fun insertLinkage(linkage: com.example.data.model.CaseLinkage) {
        val entity = com.example.data.local.CaseLinkageEntity(
            activeCaseId = linkage.activeCaseId,
            coldCaseId = linkage.coldCaseId,
            connectionType = linkage.connectionType.name,
            description = linkage.description
        )
        db.caseLinkageDao().insertLinkage(entity)
    }

    suspend fun getColdCaseDigest(caseId: String): com.example.data.model.ColdCaseDigest? {
        val entity = db.coldCaseDigestDao().getDigest(caseId) ?: return null
        return com.example.data.model.ColdCaseDigest(
            originalCaseId = entity.originalCaseId,
            summary = entity.summary,
            keyPlayers = deserializeList(entity.keyPlayersJson),
            unresolvedThreads = deserializeList(entity.unresolvedThreadsJson)
        )
    }

    suspend fun getAllColdCaseDigests(): List<com.example.data.model.ColdCaseDigest> {
        return db.coldCaseDigestDao().getAllDigests().map { entity ->
            com.example.data.model.ColdCaseDigest(
                originalCaseId = entity.originalCaseId,
                summary = entity.summary,
                keyPlayers = deserializeList(entity.keyPlayersJson),
                unresolvedThreads = deserializeList(entity.unresolvedThreadsJson)
            )
        }
    }

    suspend fun insertColdCaseDigest(digest: com.example.data.model.ColdCaseDigest) {
        val entity = com.example.data.local.ColdCaseDigestEntity(
            originalCaseId = digest.originalCaseId,
            summary = digest.summary,
            keyPlayersJson = serializeList(digest.keyPlayers),
            unresolvedThreadsJson = serializeList(digest.unresolvedThreads)
        )
        db.coldCaseDigestDao().insertDigest(entity)
    }

    private fun serializeCulpabilities(map: Map<String, CulpabilityStatus>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value.name}" }
    }

    private fun deserializeCulpabilities(str: String): Map<String, CulpabilityStatus> {
        if (str.isEmpty()) return emptyMap()
        return str.split(",").associate {
            val parts = it.split(":")
            if (parts.size >= 2) {
                val id = parts[0]
                val status = try {
                    CulpabilityStatus.valueOf(parts[1])
                } catch (e: Exception) {
                    CulpabilityStatus.COMPLETELY_INNOCENT
                }
                id to status
            } else {
                "" to CulpabilityStatus.COMPLETELY_INNOCENT
            }
        }.filterKeys { it.isNotEmpty() }
    }

    private fun serializeList(list: List<String>): String {
        return list.joinToString(",")
    }

    private fun deserializeList(str: String): List<String> {
        if (str.isEmpty()) return emptyList()
        return str.split(",")
    }
}

