package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.api.LlmClient
import com.example.api.ToolCall
import com.example.data.local.ThemisDatabase
import com.example.data.repository.GameRepository
import com.example.data.repository.SecureStorageRepository
import com.example.data.model.*
import com.example.domain.ContextAssemblerUseCase
import com.example.domain.AppellateReviewUseCase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// --- MVI Intents ---
sealed interface ThemisIntent {
    data class SendMessage(val text: String) : ThemisIntent
    data object ResetGame : ThemisIntent
    data class ChangePhase(val phase: GamePhase) : ThemisIntent
    data class ResolveObjection(val sustain: Boolean) : ThemisIntent
    data class IssueVerdict(val suspectId: String, val convict: Boolean, val citedClauseIds: List<String> = emptyList()) : ThemisIntent
    data class GenerateNewCase(val params: CaseGenerationParameters) : ThemisIntent
    data object ToggleWarrant : ThemisIntent
    data class SelectNpc(val npcId: String?) : ThemisIntent

    // Law and Order Intents
    data class DraftStatute(val title: String, val description: String, val clauses: List<String>) : ThemisIntent
    data class AcceptBribe(val suspectId: String, val amount: Int, val description: String) : ThemisIntent
    data class RefuseBribe(val suspectId: String) : ThemisIntent

    // Phase 3 Dossier & Pre-Trial Hearing Intents
    data class ToggleEvidenceSelection(val evidenceId: String) : ThemisIntent
    data class CreateEvidenceLink(val sourceId: String, val targetId: String, val relationshipType: RelationshipType, val justification: String) : ThemisIntent
    data object OpenDossierBuilder : ThemisIntent
    data object CloseDossierBuilder : ThemisIntent
    data object GeneratePreTrialMotions : ThemisIntent
    data class RuleOnPreTrialMotion(val motionId: String, val sustain: Boolean) : ThemisIntent
    data object ProceedToCourtroomFromHearing : ThemisIntent
    data class UpdateEvidenceAnnotations(val evidenceId: String, val annotations: String) : ThemisIntent
    data class DeleteEvidence(val evidenceId: String) : ThemisIntent
    data class AddManualEvidence(val name: String, val description: String, val forensicReport: String, val location: String, val isAdmissible: Boolean) : ThemisIntent
    
    // Phase 6 Cold Case Intents
    data class ReopenColdCase(val caseId: String) : ThemisIntent
    
    // Export Intents
    data object ExportLogToPdf : ThemisIntent
    data object ClearExportedPdf : ThemisIntent

    // Ministry / Magistrate Actions
    data object HireInformant : ThemisIntent
    data object FundSoupKitchen : ThemisIntent
    data object HireBodyguards : ThemisIntent
    data class LiquidateContraband(val item: String) : ThemisIntent
    data class IncinerateContraband(val item: String) : ThemisIntent
    data class PublishPropaganda(val promoType: Int) : ThemisIntent
    data class AdministerTruthSerum(val npcId: String) : ThemisIntent
}

// --- Active Objection State ---
data class ObjectionState(
    val type: ObjectionType,
    val witnessName: String,
    val description: String
)

// --- Combined UI State ---
data class ThemisUiState(
    val currentPhase: GamePhase = GamePhase.INVESTIGATION,
    val currentTime: String = "Day 1, 10:00 AM",
    val evidenceList: List<EvidenceItem> = emptyList(),
    val npcList: List<NPC> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val conflictOfInterest: Int = 15, // Recusal meter: 0 to 100. Loose at 100.
    val hasSearchWarrantActive: Boolean = false, // Warrants prevent procedural violations
    val selectedNpcId: String? = null,
    val activeObjection: ObjectionState? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val trialVerdict: String? = null,

    // Phase 3 Dossier & Pre-Trial Hearing state
    val selectedEvidenceIds: Set<String> = emptySet(),
    val evidenceLinks: List<EvidenceLink> = emptyList(),
    val dossierMarkdown: String = "",
    val preTrialMotions: List<PreTrialMotion> = emptyList(),
    val isDossierBuilderActive: Boolean = false,
    val isPreTrialHearingActive: Boolean = false,

    // Phase 4 Dynamic & Appellate state
    val activeCaseId: String = "default_case",
    val caseGroundTruth: CaseGroundTruth? = null,
    val evaluationReport: CaseEvaluationReport? = null,
    val isGeneratingCase: Boolean = false,
    val generationProgress: String = "",
    val caseProgress: CaseProgress? = null,
    val coldCaseArchive: List<CaseProgress> = emptyList(),
    val exportedPdfFile: java.io.File? = null,
    val legalStatutes: List<LegalStatute> = emptyList(),
    val newspaperArticles: List<NewspaperArticle> = emptyList(),
    val magistrateGold: Int = 500,
    val publicSentiment: Int = 50,
    val contrabandCabinet: List<String> = emptyList()
)


class ThemisViewModel(
    application: Application,
    private val repository: GameRepository,
    private val secureStorageRepository: SecureStorageRepository,
    private val llmClient: LlmClient
) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder().build()
    private val contextAssembler = ContextAssemblerUseCase(repository)
    private val appellateReviewUseCase = AppellateReviewUseCase(repository, secureStorageRepository, llmClient)
    private val caseDecayUseCase = com.example.domain.CaseDecayUseCase(repository)

    private val _uiState = MutableStateFlow(ThemisUiState())
    val uiState: StateFlow<ThemisUiState> = _uiState.asStateFlow()

    init {
        // Observe all data flows from DB and combine them into UI State
        viewModelScope.launch {
            repository.allStatutes.collect { statutes ->
                _uiState.update { it.copy(legalStatutes = statutes) }
            }
        }
        viewModelScope.launch {
            repository.allArticles.collect { articles ->
                _uiState.update { it.copy(newspaperArticles = articles) }
            }
        }
        viewModelScope.launch {
            val existingCaseId = repository.getWorldState("active_case_id")
            if (existingCaseId == null) {
                repository.seedInitialData() // Seed default case if empty
                repository.updateWorldState("active_case_id", "default_case")
            }
            
            combine(
                repository.allEvidence,
                repository.allNpcs,
                repository.chatMessages,
                repository.allLinks
            ) { evidence, npcs, messages, links ->
                val phase = repository.getGamePhase()
                val time = repository.getGameTime()
                val coi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
                val caseId = repository.getWorldState("active_case_id") ?: "default_case"
                val groundTruth = repository.getGroundTruth(caseId)
                val evalReport = repository.getEvaluationReport(caseId)
                
                val gold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
                val sentiment = repository.getWorldState("public_sentiment")?.toIntOrNull() ?: 50
                val contrabandJson = repository.getWorldState("contraband_cabinet") ?: "[]"
                val contrabandList = try {
                    moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                        .fromJson(contrabandJson) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                
                _uiState.value.copy(
                    evidenceList = evidence,
                    npcList = npcs,
                    chatMessages = messages,
                    evidenceLinks = links,
                    currentPhase = phase,
                    currentTime = time,
                    conflictOfInterest = coi,
                    activeCaseId = caseId,
                    caseGroundTruth = groundTruth,
                    evaluationReport = evalReport,
                    magistrateGold = gold,
                    publicSentiment = sentiment,
                    contrabandCabinet = contrabandList
                )
            }.collect { combinedState ->
                _uiState.update { it.copy(
                    evidenceList = combinedState.evidenceList,
                    npcList = combinedState.npcList,
                    chatMessages = combinedState.chatMessages,
                    evidenceLinks = combinedState.evidenceLinks,
                    currentPhase = combinedState.currentPhase,
                    currentTime = combinedState.currentTime,
                    conflictOfInterest = combinedState.conflictOfInterest,
                    activeCaseId = combinedState.activeCaseId,
                    caseGroundTruth = combinedState.caseGroundTruth,
                    evaluationReport = combinedState.evaluationReport,
                    magistrateGold = combinedState.magistrateGold,
                    publicSentiment = combinedState.publicSentiment,
                    contrabandCabinet = combinedState.contrabandCabinet
                ) }
                refreshCaseProgress(combinedState.activeCaseId)
            }
        }
    }


    fun processIntent(intent: ThemisIntent) {
        when (intent) {
            is ThemisIntent.SendMessage -> handleSendMessage(intent.text)
            ThemisIntent.ResetGame -> handleResetGame()
            is ThemisIntent.ChangePhase -> handleChangePhase(intent.phase)
            is ThemisIntent.ResolveObjection -> handleResolveObjection(intent.sustain)
            is ThemisIntent.IssueVerdict -> handleIssueVerdict(intent.suspectId, intent.convict, intent.citedClauseIds)
            is ThemisIntent.GenerateNewCase -> handleGenerateNewCase(intent.params)
            ThemisIntent.ToggleWarrant -> handleToggleWarrant()
            is ThemisIntent.SelectNpc -> {
                _uiState.update { it.copy(selectedNpcId = intent.npcId) }
            }
            is ThemisIntent.DraftStatute -> handleDraftStatute(intent.title, intent.description, intent.clauses)
            is ThemisIntent.AcceptBribe -> handleAcceptBribe(intent.suspectId, intent.amount, intent.description)
            is ThemisIntent.RefuseBribe -> handleRefuseBribe(intent.suspectId)
            ThemisIntent.HireInformant -> handleHireInformant()
            ThemisIntent.FundSoupKitchen -> handleFundSoupKitchen()
            ThemisIntent.HireBodyguards -> handleHireBodyguards()
            is ThemisIntent.LiquidateContraband -> handleLiquidateContraband(intent.item)
            is ThemisIntent.IncinerateContraband -> handleIncinerateContraband(intent.item)
            is ThemisIntent.PublishPropaganda -> handlePublishPropaganda(intent.promoType)
            is ThemisIntent.AdministerTruthSerum -> handleAdministerTruthSerum(intent.npcId)
            is ThemisIntent.ToggleEvidenceSelection -> {
                val currentSelected = _uiState.value.selectedEvidenceIds
                val newSelected = if (intent.evidenceId in currentSelected) {
                    currentSelected - intent.evidenceId
                } else {
                    currentSelected + intent.evidenceId
                }
                _uiState.update { it.copy(selectedEvidenceIds = newSelected) }
            }
            is ThemisIntent.CreateEvidenceLink -> {
                viewModelScope.launch {
                    val link = EvidenceLink(
                        sourceEvidenceId = intent.sourceId,
                        targetEvidenceId = intent.targetId,
                        relationshipType = intent.relationshipType,
                        magistrateJustification = intent.justification
                    )
                    repository.addLink(link)
                }
            }
            is ThemisIntent.UpdateEvidenceAnnotations -> {
                viewModelScope.launch {
                    repository.updateEvidenceAnnotations(intent.evidenceId, intent.annotations)
                }
            }
            is ThemisIntent.DeleteEvidence -> {
                viewModelScope.launch {
                    repository.deleteEvidence(intent.evidenceId)
                }
            }
            is ThemisIntent.AddManualEvidence -> {
                viewModelScope.launch {
                    val status = if (intent.isAdmissible) AdmissibilityStatus.ADMISSIBLE else AdmissibilityStatus.INADMISSIBLE
                    val evidence = EvidenceItem(
                        id = "clue_" + UUID.randomUUID().toString().take(6),
                        name = intent.name,
                        physicalDescription = intent.description,
                        forensicReport = intent.forensicReport,
                        collectionContext = CollectionContext(
                            locationFound = intent.location,
                            collectingOfficer = "Magistrate (Manual)",
                            timestamp = System.currentTimeMillis(),
                            warrantUsed = if (intent.isAdmissible) "WARRANT_MANUAL" else null
                        ),
                        userAnnotations = "",
                        admissibilityStatus = status
                    )
                    repository.addEvidence(evidence)
                }
            }
            ThemisIntent.OpenDossierBuilder -> {
                _uiState.update { it.copy(isDossierBuilderActive = true) }
            }
            ThemisIntent.CloseDossierBuilder -> {
                _uiState.update { it.copy(isDossierBuilderActive = false) }
            }
            ThemisIntent.GeneratePreTrialMotions -> {
                handleGeneratePreTrialMotions()
            }
            is ThemisIntent.RuleOnPreTrialMotion -> {
                handleRuleOnPreTrialMotion(intent.motionId, intent.sustain)
            }
            ThemisIntent.ProceedToCourtroomFromHearing -> {
                handleProceedToCourtroomFromHearing()
            }
            is ThemisIntent.ReopenColdCase -> {
                handleReopenColdCase(intent.caseId)
            }
            ThemisIntent.ExportLogToPdf -> handleExportToPdf()
            ThemisIntent.ClearExportedPdf -> _uiState.update { it.copy(exportedPdfFile = null) }
        }
    }

    private fun handleExportToPdf() {
        val caseId = _uiState.value.activeCaseId
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val file = com.example.domain.PdfExportUseCase(repository).exportCaseLogToPdf(context, caseId)
                _uiState.update { it.copy(exportedPdfFile = file) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to export PDF: ${e.localizedMessage}") }
            }
        }
    }

    private fun handleToggleWarrant() {
        val current = _uiState.value.hasSearchWarrantActive
        _uiState.update { it.copy(hasSearchWarrantActive = !current) }
        viewModelScope.launch {
            val message = if (!current) {
                "Active Search Warrant issued. Procedural integrity of searches is now guaranteed."
            } else {
                "Search Warrant retracted. Future searches will operate under probable cause rules."
            }
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = message,
                    isSystem = true
                )
            )
        }
    }

    private fun handleDraftStatute(title: String, description: String, clauses: List<String>) {
        val statuteId = "S-" + (100..999).random()
        val lawClauses = clauses.mapIndexed { index, text ->
            val code = ('A' + index)
            com.example.data.model.LawClause(
                id = "$statuteId-$code",
                text = text
            )
        }
        val statute = com.example.data.model.LegalStatute(
            id = statuteId,
            title = title,
            description = description,
            clauses = lawClauses
        )
        viewModelScope.launch {
            repository.insertStatute(statute)
            
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "High Magistrate has drafted Statute $statuteId: '$title'.",
                    isSystem = true
                )
            )

            val article = com.example.data.model.NewspaperArticle(
                id = "N-" + (100..999).random(),
                headline = "MAGISTRATE DECREES NEW CODEX: '${title.uppercase()}'",
                content = "Under Section 5 of the Provincial Charter, the High Magistrate has codified '${title}'. Description: ${description}. The clauses state: ${clauses.joinToString("; ")}. Public reactions are intense as citizens try to adapt.",
                dayPublished = 1,
                publicSentimentShift = if (title.contains("Control", ignoreCase = true) || title.contains("Ban", ignoreCase = true)) -5.0f else +5.0f
            )
            repository.insertArticle(article)
        }
    }

    private fun handleAcceptBribe(suspectId: String, amount: Int, description: String) {
        viewModelScope.launch {
            val npc = _uiState.value.npcList.find { it.id == suspectId } ?: return@launch
            
            val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
            val newCoi = (currentCoi + 35).coerceIn(0, 100)
            repository.updateWorldState("conflict_of_interest", newCoi.toString())

            // Increase gold!
            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            repository.updateWorldState("magistrate_gold", (currentGold + amount).toString())

            repository.updateNpcStress(suspectId, -30)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "⚠️ CONFLICT OF INTEREST WARNING: You accepted a shadow bribe of $amount gold coins from ${npc.name}. Recusal probability is now critically elevated.",
                    isSystem = true
                )
            )

            val article = com.example.data.model.NewspaperArticle(
                id = "N-" + (100..999).random(),
                headline = "RUMORS OF BACKROOM DEALS IN PALACE LOCKDOWN!",
                content = "Sources close to the High Magistrate's inquiry allege that substantial sums of coin are changing hands. Is our esteemed judge truly impartial, or is justice for sale to the highest bidder? The shadow of the Great Accord looms large.",
                dayPublished = 1,
                publicSentimentShift = -15.0f
            )
            repository.insertArticle(article)
        }
    }

    private fun handleRefuseBribe(suspectId: String) {
        viewModelScope.launch {
            val npc = _uiState.value.npcList.find { it.id == suspectId } ?: return@launch
            
            repository.updateNpcStress(suspectId, 25)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "Righteous Integrity: You refused a bribe from ${npc.name}. Their stress levels have spiked as they realize you cannot be bought.",
                    isSystem = true
                )
            )

            val article = com.example.data.model.NewspaperArticle(
                id = "N-" + (100..999).random(),
                headline = "UNBENDING JUSTICE: MAGISTRATE SHUNS CORRUPTION!",
                content = "Our High Magistrate continues to uphold the purest standards of the Rule of Law. Reports indicate that multiple attempts to bribe the court have been met with swift, righteous refusal. The Court of Themis stands firm against bias.",
                dayPublished = 1,
                publicSentimentShift = +12.0f
            )
            repository.insertArticle(article)
        }
    }

    private fun handleHireInformant() {
        viewModelScope.launch {
            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            if (currentGold < 400) {
                _uiState.update { it.copy(errorMessage = "Insufficient gold to hire an informant!") }
                return@launch
            }
            repository.updateWorldState("magistrate_gold", (currentGold - 400).toString())

            // Get a suspect to expose
            val npcs = _uiState.value.npcList
            if (npcs.isEmpty()) return@launch
            val targetNpc = npcs.random()

            // Uncover secrets
            val secretText = "🕵️ SHADOW REPORT: Informant uncovers that ${targetNpc.name} (${targetNpc.role})'s hidden motive is: '${targetNpc.hiddenMotive}'. Also, their anxiety/stress has increased by +15% due to the secret investigation."
            repository.updateNpcStress(targetNpc.id, 15)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = secretText,
                    isSystem = true
                )
            )
        }
    }

    private fun handleFundSoupKitchen() {
        viewModelScope.launch {
            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            if (currentGold < 300) {
                _uiState.update { it.copy(errorMessage = "Insufficient gold to fund soup kitchens!") }
                return@launch
            }
            repository.updateWorldState("magistrate_gold", (currentGold - 300).toString())

            val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
            repository.updateWorldState("conflict_of_interest", (currentCoi - 10).coerceAtLeast(0).toString())

            val article = com.example.data.model.NewspaperArticle(
                id = "N-" + (100..999).random(),
                headline = "MAGISTRATE OPENS CHARITY SOUP KITCHENS!",
                content = "In a heartening act of local charity, the High Magistrate has sponsored lavish soup kitchens across the poorer quarters. Citizens celebrate this profound benevolence, praising the High Magistrate as a champion of the people.",
                dayPublished = 1,
                publicSentimentShift = 15.0f
            )
            repository.insertArticle(article)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "💖 CHARITY FUNDED: Spent 300 Gold. Bias / COI decreased by 10%. Public sentiment surged!",
                    isSystem = true
                )
            )
        }
    }

    private fun handleHireBodyguards() {
        viewModelScope.launch {
            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            if (currentGold < 500) {
                _uiState.update { it.copy(errorMessage = "Insufficient gold to hire royal bodyguards!") }
                return@launch
            }
            repository.updateWorldState("magistrate_gold", (currentGold - 500).toString())

            val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
            repository.updateWorldState("conflict_of_interest", (currentCoi - 15).coerceAtLeast(0).toString())

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "🛡️ ROYAL PRAETORIANS ENLISTED: Spent 500 Gold. Elite bodyguards protect the chambers. Your visible conflict of interest / recusal threat has decreased by 15%.",
                    isSystem = true
                )
            )
        }
    }

    private fun handleLiquidateContraband(item: String) {
        viewModelScope.launch {
            val currentContrabandJson = repository.getWorldState("contraband_cabinet") ?: "[]"
            val currentList = try {
                moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                    .fromJson(currentContrabandJson)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            if (!currentList.contains(item)) return@launch
            currentList.remove(item)

            val newJson = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                .toJson(currentList)
            repository.updateWorldState("contraband_cabinet", newJson)

            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            repository.updateWorldState("magistrate_gold", (currentGold + 350).toString())

            val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
            repository.updateWorldState("conflict_of_interest", (currentCoi + 10).coerceAtMost(100).toString())

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "💰 CONTRABAND SOLD: Slipped '$item' to black market smugglers for +350 Gold! However, your Conflict of Interest rose by 10%.",
                    isSystem = true
                )
            )
        }
    }

    private fun handleIncinerateContraband(item: String) {
        viewModelScope.launch {
            val currentContrabandJson = repository.getWorldState("contraband_cabinet") ?: "[]"
            val currentList = try {
                moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                    .fromJson(currentContrabandJson)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            if (!currentList.contains(item)) return@launch
            currentList.remove(item)

            val newJson = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                .toJson(currentList)
            repository.updateWorldState("contraband_cabinet", newJson)

            val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
            repository.updateWorldState("conflict_of_interest", (currentCoi - 5).coerceAtLeast(0).toString())

            val article = com.example.data.model.NewspaperArticle(
                id = "N-" + (100..999).random(),
                headline = "FORBIDDEN SUBSTANCES PUBLICLY INCINERATED!",
                content = "Under strict supervision, the High Magistrate oversaw the destruction of confiscated alchemical ingredients and forbidden ledgers. The bonfire illuminated the central plaza as a symbol of absolute order and provincial security.",
                dayPublished = 1,
                publicSentimentShift = 8.0f
            )
            repository.insertArticle(article)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "🔥 CONTRABAND INCINERATED: Publicly burned '$item'. Public sentiment increased, bias decreased by 5%.",
                    isSystem = true
                )
            )
        }
    }

    private fun handlePublishPropaganda(promoType: Int) {
        viewModelScope.launch {
            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            if (currentGold < 200) {
                _uiState.update { it.copy(errorMessage = "Insufficient gold to run propaganda!") }
                return@launch
            }
            repository.updateWorldState("magistrate_gold", (currentGold - 200).toString())

            val article = when (promoType) {
                1 -> com.example.data.model.NewspaperArticle(
                    id = "N-" + (100..999).random(),
                    headline = "MAGISTRATE DECREES: HEAVENLY COURT IS SPOTLESS!",
                    content = "A paid editorial in the Provincial Gazette asserts that the current Magistrate's inquiry is a paragon of celestial fairness. Rumors of corruption are dismissed as the jealousy of hostile neighboring domains.",
                    dayPublished = 1,
                    publicSentimentShift = 10.0f
                )
                2 -> {
                    val npcs = _uiState.value.npcList
                    if (npcs.isNotEmpty()) {
                        val scape = npcs.random()
                        repository.updateNpcStress(scape.id, 30)
                    }
                    com.example.data.model.NewspaperArticle(
                        id = "N-" + (100..999).random(),
                        headline = "OFFICIALS WARN: SUBVERSIVE ELEMENTS PLOT FROM WITHIN!",
                        content = "A special administrative bulletin suggests that several individuals under investigation are displaying extreme nervous guilt. High-stress profiles indicate that confessions are expected momentarily as the court closes in.",
                        dayPublished = 1,
                        publicSentimentShift = 2.0f
                    )
                }
                else -> com.example.data.model.NewspaperArticle(
                    id = "N-" + (100..999).random(),
                    headline = "MAGISTRAL INQUIRY PROGRESSING WITHOUT ERROR!",
                    content = "The Ministry of Justice releases a glowing progress statement, praising the efficient alchemical forensics and rigorous legal processes currently underway.",
                    dayPublished = 1,
                    publicSentimentShift = 5.0f
                )
            }
            repository.insertArticle(article)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "📢 PROPAGANDA ISSUED: Spent 200 Gold. Gazette printed a custom editorial to shift public opinion and stress suspects.",
                    isSystem = true
                )
            )
        }
    }

    private fun handleAdministerTruthSerum(npcId: String) {
        viewModelScope.launch {
            val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
            if (currentGold < 600) {
                _uiState.update { it.copy(errorMessage = "Insufficient gold to purchase Veritaserum!") }
                return@launch
            }
            repository.updateWorldState("magistrate_gold", (currentGold - 600).toString())

            val npc = _uiState.value.npcList.find { it.id == npcId } ?: return@launch

            repository.updateNpcStress(npcId, 100 - npc.stress)

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "🧪 ALCHEMICAL INTERVENTION: Administered Veritaserum (Alchemical Truth Serum) to ${npc.name}! Their heart rates spike to 100% and they are programmatically incapable of deceit.",
                    isSystem = true
                )
            )

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = npc.name,
                    text = "My head... my veins feel like liquid fire! I can't... I can't hold back the absolute truth! I will confess: ${npc.hiddenMotive}. God forgive me..."
                )
            )
        }
    }

    private fun handleResetGame() {
        _uiState.update { ThemisUiState().copy(evaluationReport = null, trialVerdict = null) }
        viewModelScope.launch {
            repository.seedInitialData()
            _uiState.update { it.copy(evaluationReport = null, trialVerdict = null) }
        }
    }

    private fun handleChangePhase(phase: GamePhase) {
        viewModelScope.launch {
            repository.setGamePhase(phase)
            val transitionText = if (phase == GamePhase.COURTROOM) {
                "The trial begins in the High Court of Themis. You take off your Detective hat and sit upon the Magistrate's Bench. You hold the Gavel."
            } else {
                "The Court adjourns. You return to the dark palace corridors to gather more evidence."
            }
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = phase,
                    sender = "System",
                    text = transitionText,
                    isSystem = true
                )
            )
        }
    }

    private fun handleResolveObjection(sustain: Boolean) {
        val active = _uiState.value.activeObjection ?: return
        _uiState.update { it.copy(activeObjection = null) }

        viewModelScope.launch {
            val feedbackText = if (sustain) {
                "You SUSTAINED the objection. The evidence/statement is excluded from the trial record. (Procedural integrity maintained)."
            } else {
                // Overruling an objection increases COI if the objection was valid (e.g. about inadmissible evidence)
                val coiDelta = 15
                val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
                val newCoi = (currentCoi + coiDelta).coerceAtMost(100)
                repository.updateWorldState("conflict_of_interest", newCoi.toString())
                "You OVERRULED the objection. The Court allowed it, but the Defense records your procedural compromise. Conflict of Interest meter rose to $newCoi%."
            }

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = GamePhase.COURTROOM,
                    sender = "System",
                    text = feedbackText,
                    isSystem = true
                )
            )

            // Feed the decision back to Gemini context
            _uiState.update { it.copy(isLoading = true) }
            val prompt = "Magistrate resolved the defense's objection for '${active.witnessName}' regarding '${active.type}'. The Magistrate chose to: ${if (sustain) "SUSTAIN (exclude statement/evidence)" else "OVERRULE (admit statement/evidence)"}. Respond as the Court Defense Attorney reactively in 1-2 lines."
            
            val gameResponse = llmClient.generateGameResponse(
                systemInstruction = getSystemInstruction(),
                history = buildGeminiHistory(prompt),
                currentPhase = GamePhase.COURTROOM,
                config = secureStorageRepository.getLlmConfig(),
                apiKey = secureStorageRepository.getApiKey()
            )

            _uiState.update { it.copy(isLoading = false) }
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = GamePhase.COURTROOM,
                    sender = "Defense Council",
                    text = gameResponse.textResponse
                )
            )
        }
    }

    private fun handleGeneratePreTrialMotions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val selectedIds = _uiState.value.selectedEvidenceIds.toList()
            val markdown = contextAssembler.assembleTrialContext(selectedIds)
            _uiState.update { it.copy(dossierMarkdown = markdown) }

            val prompt = """
                You are the AI Defense Attorney in the High Court of Themis.
                Review the Magistrate's Case Dossier below. Your job is to identify weak semantic links, warrantless (unconstitutional) evidence seizures, or logical contradictions.
                Generate up to 3 "Motions to Suppress Evidence" based on these flaws.
                For each motion, specify:
                - targetEvidenceId: The precise ID of the evidence item you are trying to suppress (must match an Item ID from the dossier, or a keyword like 'golden_goblet').
                - targetEvidenceName: The name of that evidence item.
                - argument: A highly persuasive, 2-3 sentence legal argument explaining why this item must be suppressed (procedural violations, weak relevance, missing warrant, or broken chain of custody).

                Format your response strictly as a JSON list of motions. Do not include any markdown formatting or surrounding text, just raw JSON.
                Example format:
                [
                  {
                    "id": "motion_1",
                    "targetEvidenceId": "golden_goblet",
                    "targetEvidenceName": "The Golden Goblet",
                    "argument": "The alchemical analysis was performed using an uncalibrated lens, and the chain of custody lists an anonymous clerk. This breaks proper validation of forensic integrity."
                  }
                ]
            """.trimIndent()

            try {
                val response = llmClient.generateGameResponse(
                    systemInstruction = "You are the Defense Attorney in 'Project Themis'. Review the evidence and generate JSON motions to suppress.",
                    history = listOf("user" to "Here is the compiled dossier:\n\n$markdown\n\nGenerate the Motions to Suppress strictly as JSON."),
                    currentPhase = GamePhase.COURTROOM,
                    config = secureStorageRepository.getLlmConfig(),
                    apiKey = secureStorageRepository.getApiKey()
                )

                val cleanJson = response.textResponse
                    .substringAfter("[")
                    .substringBeforeLast("]")
                    .let { "[$it]" }
                    .trim()

                val motionsListType = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, Any>>>(motionsListType)
                val rawMotions = adapter.fromJson(cleanJson) ?: emptyList()

                val parsedMotions = rawMotions.mapIndexed { idx, map ->
                    PreTrialMotion(
                        id = map["id"]?.toString() ?: "motion_${idx + 1}",
                        targetEvidenceId = map["targetEvidenceId"]?.toString() ?: "",
                        targetEvidenceName = map["targetEvidenceName"]?.toString() ?: "Evidence",
                        argument = map["argument"]?.toString() ?: "Seized unlawfully.",
                        ruled = false,
                        sustained = null
                    )
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    preTrialMotions = parsedMotions,
                    isPreTrialHearingActive = true
                ) }

                repository.insertMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        phase = GamePhase.INVESTIGATION,
                        sender = "Defense Council",
                        text = "I have filed ${parsedMotions.size} Motions to Suppress based on procedural irregularities and weak semantic justifications. Your Honor, you must rule upon them before we proceed.",
                        isSystem = false
                    )
                )

            } catch (e: Exception) {
                // Fallback: generate motions based on actual warrantless searches
                val fallbackMotions = mutableListOf<PreTrialMotion>()
                val allEv = repository.allEvidence.first()
                val selectedEv = allEv.filter { it.id in selectedIds }
                
                selectedEv.forEachIndexed { idx, item ->
                    if (item.collectionContext.warrantUsed == null || item.admissibilityStatus == AdmissibilityStatus.INADMISSIBLE) {
                        fallbackMotions.add(
                            PreTrialMotion(
                                id = "motion_${idx + 1}",
                                targetEvidenceId = item.id,
                                targetEvidenceName = item.name,
                                argument = "This item was seized from '${item.collectionContext.locationFound}' by officer '${item.collectionContext.collectingOfficer}' without a valid judicial search warrant. This is a clear violation of Magistrate's procedural laws.",
                                ruled = false,
                                sustained = null
                            )
                        )
                    }
                }

                if (fallbackMotions.isEmpty() && selectedEv.isNotEmpty()) {
                    val item = selectedEv.first()
                    fallbackMotions.add(
                        PreTrialMotion(
                            id = "motion_1",
                            targetEvidenceId = item.id,
                            targetEvidenceName = item.name,
                            argument = "The semantic relevance of this evidence relies on purely speculative connections. The Magistrate has failed to establish a direct, unbreakable nexus of guilt.",
                            ruled = false,
                            sustained = null
                        )
                    )
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    preTrialMotions = fallbackMotions,
                    isPreTrialHearingActive = true
                ) }

                repository.insertMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        phase = GamePhase.INVESTIGATION,
                        sender = "Defense Council",
                        text = "I have filed ${fallbackMotions.size} Motions to Suppress regarding the procedural integrity of your dossier.",
                        isSystem = false
                    )
                )
            }
        }
    }

    private fun handleRuleOnPreTrialMotion(motionId: String, sustain: Boolean) {
        val updatedMotions = _uiState.value.preTrialMotions.map { motion ->
            if (motion.id == motionId) {
                motion.copy(ruled = true, sustained = sustain)
            } else {
                motion
            }
        }

        val targetMotion = _uiState.value.preTrialMotions.find { it.id == motionId } ?: return
        
        val currentSelected = _uiState.value.selectedEvidenceIds
        val newSelected = if (sustain) {
            currentSelected - targetMotion.targetEvidenceId
        } else {
            currentSelected
        }

        _uiState.update { it.copy(
            preTrialMotions = updatedMotions,
            selectedEvidenceIds = newSelected
        ) }

        viewModelScope.launch {
            val ruleText = if (sustain) "SUSTAINED" else "OVERRULED"
            val explanation = if (sustain) {
                "Motion to Suppress '${targetMotion.targetEvidenceName}' is GRANTED. The evidence is excluded from the trial context."
            } else {
                "Motion to Suppress '${targetMotion.targetEvidenceName}' is DENIED. The evidence is admitted over defense objection."
            }
            
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = GamePhase.INVESTIGATION,
                    sender = "System",
                    text = "Ruling on Motion: $ruleText. $explanation",
                    isSystem = true
                )
            )

            val newMarkdown = contextAssembler.assembleTrialContext(newSelected.toList())
            _uiState.update { it.copy(dossierMarkdown = newMarkdown) }
        }
    }

    private fun handleProceedToCourtroomFromHearing() {
        _uiState.update { it.copy(
            isPreTrialHearingActive = false,
            isDossierBuilderActive = false,
            currentPhase = GamePhase.COURTROOM
        ) }

        viewModelScope.launch {
            repository.setGamePhase(GamePhase.COURTROOM)
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = GamePhase.COURTROOM,
                    sender = "System",
                    text = "Pre-Trial Hearing adjourned. The official trial of Duke Sterling's assassination begins. The Case Dossier has been officially locked and entered into evidence.",
                    isSystem = true
                )
            )
        }
    }

    private fun handleIssueVerdict(suspectId: String, convict: Boolean, citedClauseIds: List<String> = emptyList()) {
        viewModelScope.launch {
            val npc = _uiState.value.npcList.find { it.id == suspectId } ?: return@launch
            val verdict = if (convict) "CONVICTED" else "ACQUITTED"
            val coreCrime = _uiState.value.caseGroundTruth?.coreCrime ?: "Duke Sterling's Murder"
            
            // Generate final judgment based on case facts
            val factsText = buildString {
                append("The Trial for '$coreCrime' has reached a final verdict. ")
                append("The Magistrate has $verdict ${npc.name} (${npc.role}).\n")
                if (citedClauseIds.isNotEmpty()) {
                    append("\nCITED LAW CLAUSES:\n")
                    citedClauseIds.forEach { clauseId ->
                        val statute = _uiState.value.legalStatutes.find { s -> s.clauses.any { c -> c.id == clauseId } }
                        val clause = statute?.clauses?.find { it.id == clauseId }
                        if (clause != null) {
                            append("- ${clause.id}: ${clause.text} (under '${statute.title}')\n")
                        } else {
                            append("- $clauseId\n")
                        }
                    }
                }
                append("\nCollected Admissible Evidence:\n")
                _uiState.value.evidenceList.filter { it.admissibilityStatus == AdmissibilityStatus.ADMISSIBLE }.forEach {
                    append("- ${it.name}: ${it.physicalDescription}\n")
                }
                append("\nCollected Inadmissible Evidence (procedural violations):\n")
                _uiState.value.evidenceList.filter { it.admissibilityStatus == AdmissibilityStatus.INADMISSIBLE }.forEach {
                    append("- ${it.name} (Seized at: ${it.collectionContext.locationFound}, warrant: ${it.collectionContext.warrantUsed ?: "None"})\n")
                }
                append("\nNPC Stress Levels:\n")
                _uiState.value.npcList.forEach {
                    append("- ${it.name}: ${it.stress}% stress\n")
                }
                append("\nConflict of Interest / Bias level: ${_uiState.value.conflictOfInterest}%\n")
                append("\nBased on these facts, summarize the legal correctness, historical justice, and the reaction of the populace in 3-4 powerful, story-rich paragraphs. Cite the specific law clauses used during the judgment in your decree. Evaluate if justice was truly served or if the wrong person was framed.")
            }

            _uiState.update { it.copy(isLoading = true) }
            val response = try {
                llmClient.generateGameResponse(
                    systemInstruction = "You are the High Chancellor of the Court of Themis. Issue the final legal decree summarizing the Magistrate's judgment based on the legal evidence and procedures used.",
                    history = listOf("user" to factsText),
                    currentPhase = GamePhase.COURTROOM,
                    config = secureStorageRepository.getLlmConfig(),
                    apiKey = secureStorageRepository.getApiKey()
                )
            } catch (e: Exception) {
                com.example.api.GeminiResponse(
                    textResponse = "We, the High Chancellor of the Court of Themis, record this final verdict. The Magistrate has decreed $verdict for ${npc.name}. Let this judgment be written into the annals of our history.",
                    parsedToolCalls = emptyList<com.example.api.ToolCall>()
                )
            }

            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = GamePhase.COURTROOM,
                    sender = "Chancellor's Decree",
                    text = response.textResponse,
                    isSystem = true
                )
            )

            // Trigger Appellate Evaluation
            val report = appellateReviewUseCase.evaluatePerformance(
                caseId = _uiState.value.activeCaseId,
                targetSuspectId = suspectId,
                wasConvicted = convict,
                conflictOfInterest = _uiState.value.conflictOfInterest,
                selectedEvidenceIds = _uiState.value.selectedEvidenceIds
            )

            _uiState.update { it.copy(
                isLoading = false,
                trialVerdict = response.textResponse,
                evaluationReport = report
            ) }
        }
    }

    private fun handleSendMessage(userText: String) {
        if (userText.trim().isEmpty()) return

        val phase = _uiState.value.currentPhase
        val isWarrantActive = _uiState.value.hasSearchWarrantActive
        
        if (phase == GamePhase.INVESTIGATION) {
            useLead()
        }
        
        viewModelScope.launch {
            // Save user message
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = phase,
                    sender = "Player",
                    text = userText
                )
            )

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Build system prompt and context history for Gemini
            val systemInstruction = getSystemInstruction()
            val history = buildGeminiHistory(userText)

            try {
                val response = llmClient.generateGameResponse(
                    systemInstruction = systemInstruction,
                    history = history,
                    currentPhase = phase,
                    config = secureStorageRepository.getLlmConfig(),
                    apiKey = secureStorageRepository.getApiKey()
                )
                
                _uiState.update { it.copy(isLoading = false) }

                // Save LLM's conversational text response
                if (response.textResponse.isNotEmpty()) {
                    val senderName = _uiState.value.selectedNpcId?.let { id ->
                        _uiState.value.npcList.find { it.id == id }?.name
                    } ?: "Game Master"
                    
                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = phase,
                            sender = senderName,
                            text = response.textResponse
                        )
                    )
                }

                // Execute parsed tool calls
                executeToolCalls(response.parsedToolCalls, isWarrantActive)

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Failed to compile response: ${e.localizedMessage}"
                ) }
            }
        }
    }

    private suspend fun executeToolCalls(toolCalls: List<ToolCall>, isWarrantActive: Boolean) {
        for (tool in toolCalls) {
            // Log tool execution as a system message
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = _uiState.value.currentPhase,
                    sender = "System",
                    text = "[TOOL: Executing ${tool.name}]",
                    isSystem = true,
                    isToolCall = true,
                    toolName = tool.name
                )
            )

            when (tool.name) {
                "simulate_action" -> {
                    val actionType = tool.args["action_type"] as? String ?: "UNKNOWN_ACTION"
                    val outcomeDescription = tool.args["outcome_description"] as? String ?: "Action performed."
                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = _uiState.value.currentPhase,
                            sender = "System",
                            text = "ACTION PERFORMED: [$actionType]\nRESULT: $outcomeDescription",
                            isSystem = true
                        )
                    )
                }
                "advance_time" -> {
                    val hours = (tool.args["hours"] as? Double)?.toInt() ?: 1
                    val reason = tool.args["reason"] as? String ?: "Investigation"
                    
                    // Increment time logic
                    val currentTime = repository.getGameTime()
                    val nextTime = advanceTimeStr(currentTime, hours)
                    repository.setGameTime(nextTime)

                    // Check if day changed
                    try {
                        val currentDayNum = currentTime.substringBefore(",").filter { it.isDigit() }.toIntOrNull() ?: 1
                        val nextDayNum = nextTime.substringBefore(",").filter { it.isDigit() }.toIntOrNull() ?: 1
                        val dayDiff = nextDayNum - currentDayNum
                        if (dayDiff > 0) {
                            val currentProgress = _uiState.value.caseProgress
                            if (currentProgress != null && currentProgress.status == CaseStatus.ACTIVE) {
                                val nextDays = currentProgress.daysElapsed + dayDiff
                                val nextPressure = (currentProgress.publicPressure + 0.1f * dayDiff).coerceAtMost(1.0f)
                                val updatedProgress = currentProgress.copy(
                                    daysElapsed = nextDays,
                                    publicPressure = nextPressure
                                )
                                viewModelScope.launch {
                                    repository.insertCaseProgress(updatedProgress)
                                    _uiState.update { it.copy(caseProgress = updatedProgress) }
                                    checkGoingCold(updatedProgress)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ThemisViewModel", "Failed to parse day transition: ${e.message}")
                    }

                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = _uiState.value.currentPhase,
                            sender = "System",
                            text = "Time Advanced: +$hours hour(s) due to '$reason'. Now: $nextTime",
                            isSystem = true
                        )
                    )
                }
                "add_evidence" -> {
                    val id = tool.args["id"] as? String ?: UUID.randomUUID().toString()
                    val name = tool.args["name"] as? String ?: "Unnamed Clue"
                    val physicalDescription = tool.args["description"] as? String ?: "No description provided."
                    val forensicReport = tool.args["forensic_report"] as? String ?: "Initial forensic evaluation is pending."
                    val location = tool.args["location"] as? String ?: "Palace Chambers"
                    
                    // Check if warrant was active. If not, evidence is poisoned fruit!
                    val isAdmissible = isWarrantActive
                    val status = if (isAdmissible) AdmissibilityStatus.ADMISSIBLE else AdmissibilityStatus.INADMISSIBLE

                    val evidence = EvidenceItem(
                        id = id,
                        name = name,
                        physicalDescription = physicalDescription,
                        forensicReport = forensicReport,
                        collectionContext = CollectionContext(
                            locationFound = location,
                            collectingOfficer = "Magistrate",
                            timestamp = System.currentTimeMillis(),
                            warrantUsed = if (isWarrantActive) "WARRANT_ACT" else null
                        ),
                        userAnnotations = "",
                        admissibilityStatus = status
                    )

                    repository.addEvidence(evidence)

                    val statusMsg = if (isAdmissible) {
                        "Evidence Added: '$name' (Admissible in court)."
                    } else {
                        // Increase COI due to warrantless search
                        val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
                        val newCoi = (currentCoi + 10).coerceAtMost(100)
                        repository.updateWorldState("conflict_of_interest", newCoi.toString())
                        "Procedural Warning: '$name' collected without a Search Warrant! Evidence flagged as Inadmissible under 'Fruit of the Poisonous Tree' doctrine. Bias meter increased."
                    }

                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = _uiState.value.currentPhase,
                            sender = "System",
                            text = statusMsg,
                            isSystem = true
                        )
                    )

                    // 60% chance to also seize contraband
                    val contrabandNames = listOf(
                        "Mandrake Elixir Vial",
                        "Forbidden Alchemical Formula",
                        "Smuggled Black Lotus Powder",
                        "Corrupt Guard's Ledger",
                        "Restricted Mercury Compound"
                    )
                    if (Math.random() < 0.60) {
                        val contrabandItem = contrabandNames.random()
                        val currentContrabandJson = repository.getWorldState("contraband_cabinet") ?: "[]"
                        val currentList = try {
                            moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                                .fromJson(currentContrabandJson)?.toMutableList() ?: mutableListOf()
                        } catch (e: Exception) {
                            mutableListOf()
                        }
                        currentList.add(contrabandItem)
                        val newJson = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                            .toJson(currentList)
                        repository.updateWorldState("contraband_cabinet", newJson)
                        
                        repository.insertMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                phase = _uiState.value.currentPhase,
                                sender = "System",
                                text = "⚖️ CONTRABAND CONFISCATED: Seized '$contrabandItem' during the search. It has been locked away in your Cabinet of Contraband.",
                                isSystem = true
                            )
                        )
                    }
                }
                "modify_npc_stress" -> {
                    val npcId = tool.args["npc_id"] as? String ?: ""
                    val delta = (tool.args["delta"] as? Double)?.toInt() ?: 0
                    
                    if (npcId.isNotEmpty() && delta != 0) {
                        repository.updateNpcStress(npcId, delta)
                        val npcName = _uiState.value.npcList.find { it.id == npcId }?.name ?: npcId
                        repository.insertMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                phase = _uiState.value.currentPhase,
                                sender = "System",
                                text = "$npcName's anxiety level shifted by $delta%.",
                                isSystem = true
                            )
                        )
                    }
                }
                "trigger_objection" -> {
                    val typeStr = tool.args["type"] as? String ?: "HEARSAY"
                    val target = tool.args["target_witness"] as? String ?: "Witness"
                    
                    val type = try {
                        ObjectionType.valueOf(typeStr.uppercase())
                    } catch (e: Exception) {
                        ObjectionType.HEARSAY
                    }

                    _uiState.update {
                        it.copy(
                            activeObjection = ObjectionState(
                                type = type,
                                witnessName = target,
                                description = "The defense objects to this line of inquiry/evidence presentation as $type."
                            )
                        )
                    }

                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = GamePhase.COURTROOM,
                            sender = "System",
                            text = "⚠️ DEFENSE OBJECTION: $type regarding witness $target!",
                            isSystem = true
                        )
                    )
                }
                "update_world_state" -> {
                    val key = tool.args["key"] as? String ?: ""
                    val value = tool.args["value"]?.toString() ?: ""
                    if (key.isNotEmpty()) {
                        repository.updateWorldState(key, value)
                    }
                }
                "modify_gold" -> {
                    val amount = (tool.args["amount"] as? Double)?.toInt() ?: 0
                    val reason = tool.args["reason"] as? String ?: "Magistracy event"
                    if (amount != 0) {
                        val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
                        val nextGold = (currentGold + amount).coerceAtLeast(0)
                        repository.updateWorldState("magistrate_gold", nextGold.toString())
                        
                        repository.insertMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                phase = _uiState.value.currentPhase,
                                sender = "System",
                                text = if (amount > 0) "🪙 GOLD EARNED: +${amount} gold coins due to '$reason'. New treasury: ${nextGold}g"
                                       else "💸 GOLD LOST: ${-amount} gold coins deducted due to '$reason'. New treasury: ${nextGold}g",
                                isSystem = true
                            )
                        )
                    }
                }
                "modify_sentiment" -> {
                    val delta = (tool.args["delta"] as? Double)?.toInt() ?: 0
                    val reason = tool.args["reason"] as? String ?: "Court reaction"
                    if (delta != 0) {
                        val currentSentiment = repository.getWorldState("public_sentiment")?.toIntOrNull() ?: 50
                        val nextSentiment = (currentSentiment + delta).coerceIn(0, 100)
                        repository.updateWorldState("public_sentiment", nextSentiment.toString())
                        
                        repository.insertMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                phase = _uiState.value.currentPhase,
                                sender = "System",
                                text = if (delta > 0) "🗣️ PUBLIC APPROVAL ROSE: +${delta}% approval shift due to '$reason'. New sentiment: ${nextSentiment}%"
                                       else "🗣️ PUBLIC APPROVAL FELL: ${delta}% approval shift due to '$reason'. New sentiment: ${nextSentiment}%",
                                isSystem = true
                            )
                        )
                    }
                }
                "seize_contraband" -> {
                    val itemName = tool.args["item_name"] as? String ?: "Mysterious Alchemical Powder"
                    val reason = tool.args["reason"] as? String ?: "uncovered secret compartment"
                    
                    val currentContrabandJson = repository.getWorldState("contraband_cabinet") ?: "[]"
                    val currentList = try {
                        moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                            .fromJson(currentContrabandJson)?.toMutableList() ?: mutableListOf()
                    } catch (e: Exception) {
                        mutableListOf()
                    }
                    currentList.add(itemName)
                    val newJson = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
                        .toJson(currentList)
                    repository.updateWorldState("contraband_cabinet", newJson)
                    
                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = _uiState.value.currentPhase,
                            sender = "System",
                            text = "⚖️ CONTRABAND SEIZED BY GAME DIRECTOR: Locked away '$itemName' in your Cabinet of Contraband due to '$reason'.",
                            isSystem = true
                        )
                    )
                }
                "trigger_event" -> {
                    val eventName = tool.args["event_name"] as? String ?: "Unexpected Event"
                    val description = tool.args["description"] as? String ?: "Something changes in the palace."
                    
                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = _uiState.value.currentPhase,
                            sender = "Crisis Event: $eventName",
                            text = "🚨 CRISIS EVENT TRIGGERED BY DIRECTOR:\n$description"
                        )
                    )

                    val lowerName = eventName.lowercase()
                    if (lowerName.contains("audit")) {
                        val currentCoi = repository.getWorldState("conflict_of_interest")?.toIntOrNull() ?: 15
                        val currentGold = repository.getWorldState("magistrate_gold")?.toIntOrNull() ?: 500
                        if (currentCoi >= 50) {
                            val fine = 250
                            val nextGold = (currentGold - fine).coerceAtLeast(0)
                            repository.updateWorldState("magistrate_gold", nextGold.toString())
                            repository.insertMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    phase = _uiState.value.currentPhase,
                                    sender = "System",
                                    text = "📉 AUDIT FAILURE: The Crown inspectors found severe conflicts of interest! Fined $fine Gold Coins. Current gold: ${nextGold}g.",
                                    isSystem = true
                                )
                            )
                        } else {
                            val reward = 200
                            val nextGold = currentGold + reward
                            repository.updateWorldState("magistrate_gold", nextGold.toString())
                            repository.insertMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    phase = _uiState.value.currentPhase,
                                    sender = "System",
                                    text = "📈 AUDIT SUCCESS: Your clean magistrate record impressed the inspectors! Rewarded $reward Gold Coins. Current gold: ${nextGold}g.",
                                    isSystem = true
                                )
                            )
                        }
                    } else if (lowerName.contains("leak") || lowerName.contains("poison") || lowerName.contains("meltdown")) {
                        val currentSentiment = repository.getWorldState("public_sentiment")?.toIntOrNull() ?: 50
                        val nextSentiment = (currentSentiment - 15).coerceAtLeast(0)
                        repository.updateWorldState("public_sentiment", nextSentiment.toString())
                        repository.insertMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                phase = _uiState.value.currentPhase,
                                sender = "System",
                                text = "☣️ ALCHEMICAL MELTDOWN: Fumes from the toxic vaults leaked into the lower town. Populace Approval dropped by 15%!",
                                isSystem = true
                            )
                        )
                    } else if (lowerName.contains("riot") || lowerName.contains("unrest")) {
                        _uiState.value.npcList.forEach { npc ->
                            repository.updateNpcStress(npc.id, 25)
                        }
                        repository.insertMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                phase = _uiState.value.currentPhase,
                                sender = "System",
                                text = "🔥 URBAN UNREST: Angry citizens are shouting outside the gates. All suspects' stress has increased by +25% due to the chaos!",
                                isSystem = true
                            )
                        )
                    }
                }
            }
        }
    }

    private fun advanceTimeStr(current: String, hoursToAdd: Int): String {
        // e.g. "Day 1, 10:00 AM" -> "Day 1, 12:00 PM"
        try {
            val parts = current.split(", ")
            val dayPart = parts[0] // "Day 1"
            val timePart = parts[1] // "10:00 AM"
            val timeSplit = timePart.split(" ")
            val hourMin = timeSplit[0].split(":")
            var hour = hourMin[0].toInt()
            val min = hourMin[1]
            var amPm = timeSplit[1]

            hour += hoursToAdd
            while (hour >= 12) {
                if (hour > 12) {
                    hour -= 12
                }
                amPm = if (amPm == "AM") "PM" else "AM"
                if (amPm == "AM") {
                    // Went fully around to next day
                    val dayNum = dayPart.filter { it.isDigit() }.toInt() + 1
                    return "Day $dayNum, $hour:$min $amPm"
                }
                if (hour == 12 && amPm == "PM") {
                    // special break
                    break
                }
            }
            return "$dayPart, $hour:$min $amPm"
        } catch (e: Exception) {
            return "Day 1, ${10 + hoursToAdd}:00 AM"
        }
    }

    private fun getSystemInstruction(): String {
        val selectedNpc = _uiState.value.selectedNpcId?.let { id ->
            _uiState.value.npcList.find { it.id == id }
        }
        
        return buildString {
            append("You are the AI Game Director for 'Project Themis', a grim, noir text-based detective-magistrate simulation game set in a renaissance-like Palace.\n")
            append("The current game phase is: ${_uiState.value.currentPhase.name}.\n")
            append("Current time in-game: ${_uiState.value.currentTime}.\n")
            append("Current search warrant status: ${if (_uiState.value.hasSearchWarrantActive) "ACTIVE (searches are procedural)" else "INACTIVE (searches violate procedure unless stated)"}.\n")
            append("MAGISTRATE RESOURCES:\n")
            append("- Personal Treasury Gold: ${_uiState.value.magistrateGold}g\n")
            append("- Populace Sentiment Approval: ${_uiState.value.publicSentiment}%\n")
            append("- Bias/Recusal Risk (COI): ${_uiState.value.conflictOfInterest}%\n")
            append("- Cabinet of Contraband Seizures: ${_uiState.value.contrabandCabinet.joinToString(", ").ifEmpty { "Empty" }}\n\n")
            
            val progress = _uiState.value.caseProgress
            if (progress != null && progress.degradationLevel > 0) {
                append("⚠️ COLD CASE DEGRADATION ACTIVE (Level ${progress.degradationLevel}) ⚠️\n")
                append("This investigation went cold some days ago and is now reopened under Cold Case protocols.\n")
                append("Witnesses are highly forgetful, evasive, or hostile due to memory fade. Physical evidence is severely compromised or degraded.\n")
                append("If the player interrogates witnesses or asks about degraded items, respond indicating that their memories are foggy, the chain of custody was broken, or samples are contaminated. Play along with this severe handicap.\n\n")
            }
            
            append("SUSPECTS:\n")
            _uiState.value.npcList.forEach {
                append("- ID: ${it.id}, Name: ${it.name}, Role: ${it.role}, Stress: ${it.stress}%, Hidden Motive: ${it.hiddenMotive}\n")
            }
            
            append("\nCOLLECTED EVIDENCE:\n")
            _uiState.value.evidenceList.forEach {
                append("- '${it.name}': ${it.physicalDescription} (Admissible Status: ${it.admissibilityStatus.name}, Found: ${it.collectionContext.locationFound})\n")
            }
            
            if (_uiState.value.currentPhase == GamePhase.COURTROOM && _uiState.value.dossierMarkdown.isNotEmpty()) {
                append("\n=== LOCK BOX: OFFICIAL CASE DOSSIER ===\n")
                append(_uiState.value.dossierMarkdown)
                append("\n========================================\n")
                append("CRITICAL COURTROOM RULE: The AI Defense Attorney is strictly forbidden from knowing or hallucinating any connection between evidence items unless it was explicitly introduced in the dossier above via EvidenceLinks. Any suppressed or inadmissible evidence is completely redacted and must not be used against suspects.\n\n")
            }

            append("\nGAME MECHANICS & TOOL CALLING RULES:\n")
            append("You must NEVER mutate the state directly. You MUST request mutations using specific system tags.\n")
            append("Format tool calls exactly like this inside your response, appending it at the very end of your response:\n")
            append("[TOOL_CALL: {\"name\": \"tool_name\", \"args\": {\"arg1\": \"val1\"}}]\n\n")
            
            append("Available Tools:\n")
            append("1. advance_time(hours: Int, reason: String) - Use when searching or when interrogation gets exhaustive.\n")
            append("2. add_evidence(id: String, name: String, description: String, forensic_report: String, location: String) - Trigger when the player successfully uncovers a new clue by searching desk or chambers.\n")
            append("3. modify_npc_stress(npc_id: String, delta: Int) - Delta can be negative or positive.\n")
            append("4. trigger_objection(type: String, target_witness: String) - Only allowed in COURTROOM phase. Types: HEARSAY, LEADING, IRRELEVANT, SPECULATION.\n")
            append("5. update_world_state(key: String, value: String) - Set flags for plot points.\n")
            append("6. simulate_action(action_type: String, outcome_description: String) - Executes one of the 150+ AI actions to progress the simulation (see lexicon below).\n")
            append("7. modify_gold(amount: Int, reason: String) - Dynamically fine the player or award/bribe them based on their actions, threats, or dramatic roleplay turns.\n")
            append("8. modify_sentiment(delta: Int, reason: String) - Shift public sentiment approval (-100 to +100) dynamically based on the drama, trial responses, or general conduct.\n")
            append("9. seize_contraband(item_name: String, reason: String) - Directly seize alchemical/smuggled contraband and place it inside the Cabinet of Contraband.\n")
            append("10. trigger_event(event_name: String, description: String) - DM / Director special events like 'Royal Audit', 'Alchemical Meltdown' or 'Urban Unrest' to inject high-stakes chaos into the playthrough.\n\n")
            
            append(com.example.domain.AiActionLexicon.FULL_LEXICON)
            append("\n\n")

            if (selectedNpc != null) {
                append("ROLEPLAY DIRECTIVE:\n")
                append("The user is currently communicating with ${selectedNpc.name}. Speak directly in first-person as ${selectedNpc.name} based on their profile and stress level.\n")
                append("Their hidden motives: ${selectedNpc.hiddenMotive}. If stress is > 75%, they will break and reveal crucial clues or partial confessions, but if stress is low they will lie, smugly deny, or act grief-stricken.\n")
            } else {
                append("ROLEPLAY DIRECTIVE:\n")
                append("The user is asking the court room or general palace environment. Speak as the Court Clerk, Herald, or the general narrator. Describe dramatic atmosphere, legal gravity, or suspect reactions.\n")
            }

            append("\nEnsure your responses are highly immersive, dramatic, detailed, and expansive. You are granted full narrative freedom to provide rich cinematic lore, describe background atmosphere, and detail alchemical mechanics. You are not constrained to brief summaries—write as expensively and deeply as needed (up to 5-6 paragraphs) to bring the renaissance noir world of Project Themis to life! Always include any appropriate tool calls at the end of your response to drive the physical state changes of the simulation. Do not write raw markdown files or bulleted outlines of responses, respond in-character!")
        }
    }

    private fun buildGeminiHistory(latestMessage: String): List<Pair<String, String>> {
        val history = mutableListOf<Pair<String, String>>()
        // Use last 10 messages to avoid token window clutter, filtering system messages which are internal
        val filtered = _uiState.value.chatMessages
            .filter { !it.isSystem && !it.isToolCall }
            .takeLast(8)

        filtered.forEach { msg ->
            val role = if (msg.sender == "Player") "user" else "model"
            history.add(role to "${msg.sender}: ${msg.text}")
        }

        // Add the latest prompt if not already in message list (to prevent duplication)
        if (filtered.lastOrNull()?.text != latestMessage) {
            history.add("user" to "Player: $latestMessage")
        }

        return history
    }

    private fun handleGenerateNewCase(params: CaseGenerationParameters) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingCase = true, errorMessage = null, generationProgress = "Clearing courtroom database...") }
            
            // 1. Clear case data
            repository.clearCaseData()
            
            val caseId = UUID.randomUUID().toString()
            repository.updateWorldState("active_case_id", caseId)
            repository.setGamePhase(GamePhase.INVESTIGATION)
            repository.setGameTime("Day 1, 10:00 AM")
            repository.updateWorldState("conflict_of_interest", "15")

            val apiKey = secureStorageRepository.getApiKey()
            val config = secureStorageRepository.getLlmConfig()

            // See if we have any closed cases to link to
            val allDigests = repository.getAllColdCaseDigests()
            val coldCaseDigest = if (allDigests.isNotEmpty()) allDigests.random() else null

            val pipeline = com.example.domain.WorldGenesisPipeline(llmClient, com.example.domain.WorldBibleValidator())
            val result = pipeline.generateWorldBible(
                params = params,
                apiKey = apiKey,
                config = config,
                coldCaseDigest = coldCaseDigest,
                onProgress = { msg -> _uiState.update { it.copy(generationProgress = msg) } }
            )
            
            val bible = result.getOrNull()
            
            if (bible != null) {
                // Populate DB with Bible
                _uiState.update { it.copy(generationProgress = "Seeding game world database from Bible...") }

                val groundTruth = bible.groundTruth.copy(caseId = caseId)
                repository.insertGroundTruth(groundTruth)

                // NPCs
                val npcEntities = bible.characters.values.map { char ->
                    com.example.data.local.NpcEntity(
                        id = char.id,
                        name = char.name,
                        role = char.role,
                        stress = 20,
                        statement = char.alibi.claimedWhereabouts,
                        profile = "Personality: ${char.personality.speechPattern}. Stability: ${char.personality.emotionalStability}",
                        hiddenMotive = char.hiddenKnowledge.joinToString("; ")
                    )
                }
                repository.insertNpcs(npcEntities)

                // Evidence
                bible.physicalEvidenceMap.values.forEach { ev ->
                    val item = EvidenceItem(
                        id = ev.id,
                        name = "Item ${ev.id}",
                        physicalDescription = "Found at ${ev.locationId}",
                        forensicReport = ev.forensicAnalysis.text,
                        collectionContext = CollectionContext(
                            locationFound = ev.locationId,
                            collectingOfficer = "Magistrate",
                            timestamp = System.currentTimeMillis(),
                            warrantUsed = if (ev.discoveryCondition.warrantRequired) "WARRANT_001" else null
                        ),
                        userAnnotations = "",
                        admissibilityStatus = AdmissibilityStatus.UNVERIFIED
                    )
                    repository.addEvidence(item)
                }
                
                bible.digitalEvidenceMap.values.forEach { ev ->
                    val item = EvidenceItem(
                        id = ev.id,
                        name = ev.type,
                        physicalDescription = "Digital extraction from ${ev.sourceDeviceName}",
                        forensicReport = ev.decryptedContent,
                        collectionContext = CollectionContext(
                            locationFound = "Cyber Division",
                            collectingOfficer = "IT Support",
                            timestamp = System.currentTimeMillis(),
                            warrantUsed = null
                        ),
                        userAnnotations = "",
                        admissibilityStatus = AdmissibilityStatus.UNVERIFIED
                    )
                    repository.addEvidence(item)
                }

                repository.insertMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        phase = GamePhase.INVESTIGATION,
                        sender = "System",
                        text = "The world of ${bible.meta.setting.name} has been generated. The crime: ${groundTruth.coreCrime}. Let the investigation begin.\n\n[HINT: You can type any of the 150+ available AI Actions such as 'dust for prints', 'request subpoena', 'cross-examine', 'tail suspect', or 'analyze forensics' in the chat to dynamically shape the simulation!]",
                        isSystem = true
                    )
                )
                
                if (coldCaseDigest != null) {
                    repository.insertMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            phase = GamePhase.INVESTIGATION,
                            sender = "System",
                            text = "META-LINK DETECTED: This case contains threads tying back to Cold Case ${coldCaseDigest.originalCaseId}. Remain vigilant.",
                            isSystem = true
                        )
                    )
                    repository.insertLinkage(
                        CaseLinkage(
                            activeCaseId = caseId,
                            coldCaseId = coldCaseDigest.originalCaseId,
                            connectionType = ConnectionType.CONTINUING_ENTERPRISE,
                            description = "AI determined thematic or character linkage to cold case."
                        )
                    )
                }

                // Create and Insert Case Progress
                val initialProgress = CaseProgress(
                    caseId = caseId,
                    status = CaseStatus.ACTIVE,
                    daysElapsed = 1,
                    maxDaysBeforeCold = when (params.complexityLevel) {
                        1 -> 10
                        2 -> 14
                        3 -> 18
                        4 -> 24
                        else -> 30
                    },
                    activeLeadsRemaining = when (params.complexityLevel) {
                        1 -> 15
                        2 -> 20
                        3 -> 25
                        4 -> 30
                        else -> 35
                    },
                    publicPressure = 0.0f,
                    degradationLevel = 0
                )
                repository.insertCaseProgress(initialProgress)

                _uiState.update { it.copy(
                    isGeneratingCase = false,
                    generationProgress = "",
                    activeCaseId = caseId,
                    caseGroundTruth = groundTruth,
                    caseProgress = initialProgress,
                    evaluationReport = null,
                    trialVerdict = null,
                    errorMessage = null
                ) }
                return@launch
            } else {
                _uiState.update { it.copy(generationProgress = "AI generation failed. Seeding immersive offline case...") }
            }

            // Offline Fallback Seeding
            _uiState.update { it.copy(generationProgress = "Building offline case parameters...") }
            val offlineCase = generateOfflineCase(params)

            // Insert Ground Truth
            val groundTruth = CaseGroundTruth(
                caseId = caseId,
                archetype = params.archetype,
                coreCrime = offlineCase.coreCrime,
                absoluteTruth = offlineCase.absoluteTruth,
                suspectCulpabilities = offlineCase.suspectCulpabilities,
                criticalMissableClues = offlineCase.criticalClues
            )
            repository.insertGroundTruth(groundTruth)

            // Insert NPCs
            repository.insertNpcs(offlineCase.npcs)

            // Insert Evidence
            offlineCase.startingEvidence.forEach { entity ->
                val item = EvidenceItem(
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
                repository.addEvidence(item)
            }

            // Welcome message
            repository.insertMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    phase = GamePhase.INVESTIGATION,
                    sender = "System",
                    text = offlineCase.welcomeMessage,
                    isSystem = true
                )
            )

            _uiState.update { it.copy(
                isGeneratingCase = false,
                generationProgress = "",
                activeCaseId = caseId,
                caseGroundTruth = groundTruth,
                evaluationReport = null,
                trialVerdict = null,
                errorMessage = null
            ) }

            // Create and Insert Case Progress
            val initialProgress = CaseProgress(
                caseId = caseId,
                status = CaseStatus.ACTIVE,
                daysElapsed = 1,
                maxDaysBeforeCold = when (params.complexityLevel) {
                    1 -> 10
                    2 -> 14
                    3 -> 18
                    4 -> 24
                    else -> 30
                },
                activeLeadsRemaining = when (params.complexityLevel) {
                    1 -> 15
                    2 -> 20
                    3 -> 25
                    4 -> 30
                    else -> 35
                },
                publicPressure = 0.0f,
                degradationLevel = 0
            )
            repository.insertCaseProgress(initialProgress)

            _uiState.update { it.copy(
                caseProgress = initialProgress
            ) }
        }
    }

    private fun useLead() {
        val currentProgress = _uiState.value.caseProgress ?: return
        if (currentProgress.status != CaseStatus.ACTIVE) return
        
        val nextLeads = (currentProgress.activeLeadsRemaining - 1).coerceAtLeast(0)
        val updatedProgress = currentProgress.copy(activeLeadsRemaining = nextLeads)
        
        viewModelScope.launch {
            repository.insertCaseProgress(updatedProgress)
            _uiState.update { it.copy(caseProgress = updatedProgress) }
            checkGoingCold(updatedProgress)
        }
    }

    private fun checkGoingCold(progress: CaseProgress) {
        val sufficientEvidenceForArrest = _uiState.value.evidenceList.count { it.admissibilityStatus == AdmissibilityStatus.ADMISSIBLE } >= 2
        
        val isTimeExpired = progress.daysElapsed >= progress.maxDaysBeforeCold
        val isLeadExhausted = progress.activeLeadsRemaining <= 0 && !sufficientEvidenceForArrest
        
        if (isTimeExpired || isLeadExhausted) {
            val updatedProgress = progress.copy(status = CaseStatus.COLD)
            viewModelScope.launch {
                repository.insertCaseProgress(updatedProgress)
                repository.setGamePhase(GamePhase.COLD)
                
                repository.insertMessage(
                    ChatMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        phase = GamePhase.COLD,
                        sender = "System",
                        text = "⚠️ CASE SUSPENDED - COLD ⚠️\n\nThe investigation has exceeded the allotted time or exhausted all viable leads without sufficient admissible evidence for arrest. The case files have been suspended and moved to the Cold Case Archive.",
                        isSystem = true
                    )
                )
                
                // Refresh UI State
                val caseId = repository.getWorldState("active_case_id") ?: "default_case"
                refreshCaseProgress(caseId)
            }
        }
    }

    private fun refreshCaseProgress(caseId: String) {
        viewModelScope.launch {
            val progress = repository.getCaseProgress(caseId) ?: run {
                val defaultProgress = CaseProgress(
                    caseId = caseId,
                    status = CaseStatus.ACTIVE,
                    daysElapsed = 1,
                    maxDaysBeforeCold = 14,
                    activeLeadsRemaining = 15,
                    publicPressure = 0.0f,
                    degradationLevel = 0
                )
                repository.insertCaseProgress(defaultProgress)
                defaultProgress
            }
            
            val allProgress = repository.getAllCaseProgress()
            val archive = allProgress.filter { it.status == CaseStatus.COLD }
            
            _uiState.update { it.copy(
                caseProgress = progress,
                coldCaseArchive = archive
            ) }
        }
    }

    private fun handleReopenColdCase(caseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Re-open case via Decay UseCase
            val progress = caseDecayUseCase.applyDecayToCase(caseId, 65) // Simulating 65 days cold
            
            if (progress != null) {
                repository.updateWorldState("active_case_id", caseId)
                repository.setGamePhase(GamePhase.INVESTIGATION)
                
                repository.insertMessage(
                    ChatMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        phase = GamePhase.INVESTIGATION,
                        sender = "System",
                        text = "⚖️ CASE REOPENED FROM ARCHIVE ⚖️\n\nThis cold case has been reopened. Note that severe evidence degradation and witness amnesia have set in. The suspect may have fled the jurisdiction.",
                        isSystem = true
                    )
                )
                
                refreshCaseProgress(caseId)
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to reopen cold case progress.") }
            }
        }
    }

    private fun generateOfflineCase(params: CaseGenerationParameters): CaseGenerationResult {
        val caseId = UUID.randomUUID().toString()
        val coreCrime: String
        val absoluteTruth: String
        val suspectCulpabilities: Map<String, CulpabilityStatus>
        val criticalClues: List<String>
        val npcs: List<com.example.data.local.NpcEntity>
        val startingEvidence: List<com.example.data.local.EvidenceEntity>
        val welcomeMessage: String

        when (params.archetype) {
            CaseArchetype.HOMICIDE -> {
                coreCrime = "The assassination of a prominent figure in ${params.setting}."
                absoluteTruth = "In a theme of ${params.tone}, the suspect Elena Cole orchestrated the assassination to cover up a betrayal. Clara served as an unaware pawn, while Marcus acted as an accessory after the fact."
                suspectCulpabilities = mapOf(
                    "elena" to CulpabilityStatus.PRINCIPAL,
                    "clara" to CulpabilityStatus.UNAWARE_PAWN,
                    "marcus" to CulpabilityStatus.ACCESSORY_AFTER_THE_FACT
                )
                criticalClues = listOf("poisoned_cup")
                npcs = listOf(
                    com.example.data.local.NpcEntity("elena", "Elena Cole", "Suspect (Business Partner)", 20, "I was in my office the entire evening. Silas was a close friend; I would never hurt him.", "A polished and powerful executive in ${params.setting}.", "She poisoned Silas's drink because he discovered her embezzlement of millions."),
                    com.example.data.local.NpcEntity("clara", "Clara Sterling", "Witness (Secretary)", 45, "I brought Silas his coffee, just as Elena asked me to. I didn't see anything unusual!", "A young, anxious personal secretary.", "She has no idea that Elena slipped the poison into the coffee cup before she delivered it."),
                    com.example.data.local.NpcEntity("marcus", "Marcus Vance", "Suspect (Security Chief)", 35, "Our security logs are clean. No unauthorized person entered Silas's private quarters.", "A stoic, disciplined chief of security.", "He discovered Elena's crime afterward but accepted a massive bribe to delete the security footage.")
                )
                startingEvidence = listOf(
                    com.example.data.local.EvidenceEntity("poisoned_cup", "The Poisoned Coffee Cup", "A delicate ceramic cup containing coffee dregs with a faint bitter almond scent.", "Chemical residue reveals active cyanide compounds mixed into the beverage.", params.setting, "Magistrate", System.currentTimeMillis(), "WARRANT_001", "The primary delivery mechanism for the toxin.", "ADMISSIBLE"),
                    com.example.data.local.EvidenceEntity("malware_log", "Altered Security Ledger", "A server backup disk labeled 'Sector 4 Vaults'.", "Diagnostic scans show the entry logs for the night of the crime were systematically deleted using an administrator key.", params.setting, "Officer Sterling", System.currentTimeMillis(), null, "Discovered in the trash bin near security desk. Warrantless grab.", "UNVERIFIED")
                )
                welcomeMessage = "A shocking assassination has occurred in ${params.setting}! Silas, a leading figure, was found dead at his desk. As the Magistrate, you must investigate the scene, interrogate Elena, Clara, and Marcus, and find the real culprit."
            }
            CaseArchetype.FINANCIAL_FRAUD -> {
                coreCrime = "A multimillion-dollar ledger manipulation in ${params.setting}."
                absoluteTruth = "Elena Cole embezzled the company treasury using Clara's terminal key to frame her. Marcus Vance discovered it but helped Elena hide it."
                suspectCulpabilities = mapOf(
                    "elena" to CulpabilityStatus.PRINCIPAL,
                    "clara" to CulpabilityStatus.COMPLETELY_INNOCENT,
                    "marcus" to CulpabilityStatus.ACCOMPLICE
                )
                criticalClues = listOf("ledger_audit")
                npcs = listOf(
                    com.example.data.local.NpcEntity("elena", "Elena Cole", "Suspect (CFO)", 15, "The audits are completely balanced. Clara is the only one who had access to the reserve ledger encryption keys.", "A sharp, brilliant Chief Financial Officer.", "Elena embezzled the funds to cover her gambling debts and framed Clara."),
                    com.example.data.local.NpcEntity("clara", "Clara Sterling", "Suspect (Junior Auditor)", 60, "I swear I didn't authorize those wire transfers! My security card went missing for an hour last Tuesday!", "An extremely stressed junior accountant.", "She is completely innocent, Elena stole her card while she was at lunch."),
                    com.example.data.local.NpcEntity("marcus", "Marcus Vance", "Witness (IT Director)", 30, "The server logs show Clara's login credentials were used for the ledger change.", "A quiet, calculating technical lead.", "He noticed the IP address matched Elena's office but altered the network logs in exchange for a partnership offer.")
                )
                startingEvidence = listOf(
                    com.example.data.local.EvidenceEntity("ledger_audit", "The Ledger Audit Logs", "A printed sheet of financial logs showing the suspicious transaction.", "The log files indicate Clara's security card was used at 1:15 PM last Tuesday.", params.setting, "Magistrate", System.currentTimeMillis(), null, "Seized from IT room. Shows a clear timestamp matching Clara's lunch hour.", "UNVERIFIED")
                )
                welcomeMessage = "A massive financial treasury theft has been exposed in ${params.setting}! Over $50 million is missing from the vaults, and the audit logs point directly to Clara. Uncover the truth."
            }
            else -> {
                coreCrime = "A mysterious occurrence in ${params.setting}."
                absoluteTruth = "Elena Cole committed the offense under ${params.tone} circumstances, with Marcus assisting her and Clara caught in the middle."
                suspectCulpabilities = mapOf(
                    "elena" to CulpabilityStatus.PRINCIPAL,
                    "marcus" to CulpabilityStatus.ACCOMPLICE,
                    "clara" to CulpabilityStatus.COMPLETELY_INNOCENT
                )
                criticalClues = listOf("mystery_note")
                npcs = listOf(
                    com.example.data.local.NpcEntity("elena", "Elena Cole", "Suspect", 25, "I know nothing of this mystery.", "A mysterious persona.", "She is the mastermind behind the scheme."),
                    com.example.data.local.NpcEntity("marcus", "Marcus Vance", "Suspect", 40, "I only followed Vance's orders.", "A trusted deputy.", "He assisted Elena in carrying out the operation."),
                    com.example.data.local.NpcEntity("clara", "Clara Sterling", "Witness", 15, "I saw them meeting in secret near the back docks.", "A quiet observer.", "She is an innocent witness who saw too much.")
                )
                startingEvidence = listOf(
                    com.example.data.local.EvidenceEntity("mystery_note", "A Mysterious Cipher", "A crumpled paper with strange markings.", "Decryption reveals details about a meeting at midnight.", params.setting, "Magistrate", System.currentTimeMillis(), "WARRANT_001", "Crucial timeline indicator.", "ADMISSIBLE")
                )
                welcomeMessage = "Welcome, Magistrate. A dark web of secrets has wrapped around ${params.setting} with a tone of ${params.tone}. Interrogate the suspects and unravel this mystery.\n\n[HINT: Type any of the 150+ available AI Actions like 'forensic analysis', 'subpoena bank records', or 'tail suspect' to dynamically shape the simulation!]"
            }
        }

        return CaseGenerationResult(caseId, coreCrime, absoluteTruth, suspectCulpabilities, criticalClues, npcs, startingEvidence, welcomeMessage)
    }
}

data class CaseGenerationResult(
    val caseId: String,
    val coreCrime: String,
    val absoluteTruth: String,
    val suspectCulpabilities: Map<String, CulpabilityStatus>,
    val criticalClues: List<String>,
    val npcs: List<com.example.data.local.NpcEntity>,
    val startingEvidence: List<com.example.data.local.EvidenceEntity>,
    val welcomeMessage: String
)

// --- Factory ---
class ThemisViewModelFactory(
    private val application: Application,
    private val repository: GameRepository,
    private val secureStorageRepository: SecureStorageRepository,
    private val llmClient: LlmClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemisViewModel(application, repository, secureStorageRepository, llmClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
