package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GamePhase
import com.example.data.model.LlmEndpointConfig
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AntiqueBrassGold
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.DarkTerminalBg
import com.example.ui.theme.ParchmentBg
import com.example.ui.theme.ThemisTheme
import com.example.ui.theme.WarmWoodBrown
import com.example.ui.viewmodel.SettingsUiState
import com.example.ui.viewmodel.ThemisIntent
import com.example.ui.viewmodel.ThemisUiState
import kotlinx.coroutines.launch

sealed class NavigationDestination(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : NavigationDestination("dashboard", "Dashboard", Icons.Default.Home)
    data object Investigation : NavigationDestination("investigation", "Investigation", Icons.Default.Search)
    data object CaseFiles : NavigationDestination("case_files", "Case Files", Icons.Default.Info)
    data object LawAndOrder : NavigationDestination("law_and_order", "Law & Gazette", Icons.Default.List)
    data object Courtroom : NavigationDestination("courtroom", "Courtroom", Icons.Default.Build)
    data object ColdCaseArchive : NavigationDestination("cold_case_archive", "Cold Case Archive", Icons.Default.Lock)
    data object Settings : NavigationDestination("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemisGameLayout(
    uiState: ThemisUiState,
    onIntent: (ThemisIntent) -> Unit,
    settingsUiState: SettingsUiState,
    onConfigChange: (LlmEndpointConfig) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveSettings: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onThemeChange: (String) -> Unit,
    onAdUnitIdChange: (String) -> Unit,
    onUseSimulatedAdsChange: (Boolean) -> Unit,
    onAdsEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf<NavigationDestination>(NavigationDestination.Dashboard) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isCaseGeneratorActive by remember { mutableStateOf(false) }
    var hasInitiatedGeneration by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isGeneratingCase) {
        if (!uiState.isGeneratingCase && isCaseGeneratorActive && hasInitiatedGeneration) {
            hasInitiatedGeneration = false
            isCaseGeneratorActive = false
            currentDestination = NavigationDestination.Investigation
        }
    }

    val isDark = uiState.currentPhase == GamePhase.INVESTIGATION
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 840 // Modern Material 3 Expanded threshold

    val destinations = listOf(
        NavigationDestination.Dashboard,
        NavigationDestination.Investigation,
        NavigationDestination.CaseFiles,
        NavigationDestination.LawAndOrder,
        NavigationDestination.Courtroom,
        NavigationDestination.ColdCaseArchive,
        NavigationDestination.Settings
    )

    // Helper composable for content layout to avoid repetition
    @Composable
    fun MainContentLayout(innerPadding: PaddingValues) {
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(if (isDark) DarkTerminalBg else ParchmentBg)
        ) {
            // Header Dashboard (Time, Warrant, Conflict of Interest)
            DashboardHeader(
                uiState = uiState,
                onIntent = onIntent,
                isDark = isDark
            )

            // Main screen switching
            Crossfade(
                targetState = currentDestination,
                animationSpec = tween(300),
                modifier = Modifier.weight(1f),
                label = "NavigationTransition"
            ) { destination ->
                when (destination) {
                    NavigationDestination.Dashboard -> {
                        GameDashboardScreen(
                            uiState = uiState,
                            onNavigate = { currentDestination = it },
                            isDark = isDark,
                            onStartCustomCase = { isCaseGeneratorActive = true }
                        )
                    }
                    NavigationDestination.Investigation -> {
                        MagistrateTerminal(
                            messages = uiState.chatMessages,
                            isLoading = uiState.isLoading,
                            currentPhase = uiState.currentPhase,
                            activeNpcName = uiState.npcList.find { it.id == uiState.selectedNpcId }?.name,
                            onSendMessage = { onIntent(ThemisIntent.SendMessage(it)) }
                        )
                    }
                    NavigationDestination.CaseFiles -> {
                        EvidenceDossier(
                            evidenceList = uiState.evidenceList,
                            currentPhase = uiState.currentPhase,
                            onIntent = onIntent
                        )
                    }
                    NavigationDestination.LawAndOrder -> {
                        LawAndOrderScreen(
                            uiState = uiState,
                            onIntent = onIntent,
                            isDark = isDark
                        )
                    }
                    NavigationDestination.Courtroom -> {
                        if (isTablet) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CourtDocket(
                                        npcs = uiState.npcList,
                                        currentPhase = uiState.currentPhase,
                                        selectedNpcId = uiState.selectedNpcId,
                                        legalStatutes = uiState.legalStatutes,
                                        onSelectNpc = { onIntent(ThemisIntent.SelectNpc(it)) },
                                        onIssueVerdict = { id, convict, citedClauses -> onIntent(ThemisIntent.IssueVerdict(id, convict, citedClauses)) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1.5f)) {
                                    MagistrateTerminal(
                                        messages = uiState.chatMessages,
                                        isLoading = uiState.isLoading,
                                        currentPhase = uiState.currentPhase,
                                        activeNpcName = uiState.npcList.find { it.id == uiState.selectedNpcId }?.name,
                                        onSendMessage = { onIntent(ThemisIntent.SendMessage(it)) }
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CourtDocket(
                                        npcs = uiState.npcList,
                                        currentPhase = uiState.currentPhase,
                                        selectedNpcId = uiState.selectedNpcId,
                                        legalStatutes = uiState.legalStatutes,
                                        onSelectNpc = { onIntent(ThemisIntent.SelectNpc(it)) },
                                        onIssueVerdict = { id, convict, citedClauses -> onIntent(ThemisIntent.IssueVerdict(id, convict, citedClauses)) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1.2f)) {
                                    MagistrateTerminal(
                                        messages = uiState.chatMessages,
                                        isLoading = uiState.isLoading,
                                        currentPhase = uiState.currentPhase,
                                        activeNpcName = uiState.npcList.find { it.id == uiState.selectedNpcId }?.name,
                                        onSendMessage = { onIntent(ThemisIntent.SendMessage(it)) }
                                    )
                                }
                            }
                        }
                    }
                    NavigationDestination.ColdCaseArchive -> {
                        ColdCaseArchiveScreen(
                            uiState = uiState,
                            onIntent = onIntent,
                            isDark = isDark
                        )
                    }
                    NavigationDestination.Settings -> {
                        SettingsScreen(
                            uiState = settingsUiState,
                            onConfigChange = onConfigChange,
                            onApiKeyChange = onApiKeyChange,
                            onTestConnection = onTestConnection,
                            onSaveSettings = onSaveSettings,
                            isDark = isDark,
                            onFontSizeChange = onFontSizeChange,
                            onThemeChange = onThemeChange,
                            onAdUnitIdChange = onAdUnitIdChange,
                            onUseSimulatedAdsChange = onUseSimulatedAdsChange,
                            onAdsEnabledChange = onAdsEnabledChange,
                            onBack = { currentDestination = NavigationDestination.Dashboard }
                        )
                    }
                }
            }

            // Show bottom Google ads if enabled
            if (settingsUiState.adsEnabled) {
                AdBanner(
                    isDark = isDark,
                    adUnitId = settingsUiState.adUnitId,
                    useSimulatedAds = settingsUiState.useSimulatedAds,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun DrawerContentSheet(onItemClick: (NavigationDestination) -> Unit) {
        val sheetColor = if (isDark) Color(0xFF16161B) else ParchmentBg
        val textColor = if (isDark) Color.White else Color.Black

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .background(sheetColor)
                .padding(16.dp)
        ) {
            // Drawer Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = "⚖️ PROJECT THEMIS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    color = if (isDark) AmberAccent else WarmWoodBrown
                )
            }

            HorizontalDivider(color = if (isDark) Color(0xFF2A2A30) else Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items
            destinations.forEach { dest ->
                val isSelected = currentDestination == dest
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = dest.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    },
                    selected = isSelected,
                    onClick = { onItemClick(dest) },
                    icon = {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.title,
                            tint = if (isSelected) {
                                if (isDark) Color.Black else Color.White
                            } else {
                                if (isDark) Color.Gray else Color.DarkGray
                            }
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = if (isDark) AmberAccent else WarmWoodBrown,
                        selectedTextColor = if (isDark) Color.Black else Color.White,
                        unselectedTextColor = if (isDark) Color.Gray else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .testTag("drawer_item_${dest.route}")
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { onIntent(ThemisIntent.ExportLogToPdf) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF2A2A30) else Color(0xFFE0E0E0),
                    contentColor = if (isDark) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export PDF", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("EXPORT CASE LOG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }

            Text(
                text = "SYSTEM ENGINE: BYOK LLM\nVER 2.0.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                ),
                color = if (isDark) Color.DarkGray else Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(uiState.exportedPdfFile) {
        uiState.exportedPdfFile?.let { file ->
            android.widget.Toast.makeText(context, "Saved to Downloads: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Case Log PDF"))
            onIntent(ThemisIntent.ClearExportedPdf)
        }
    }

    ThemisTheme(phase = uiState.currentPhase) {
        if (isTablet) {
            // Tablet / Expanded mode: Permanent Navigation Drawer
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        drawerContainerColor = if (isDark) Color(0xFF16161B) else ParchmentBg,
                        drawerTonalElevation = 0.dp
                    ) {
                        DrawerContentSheet(onItemClick = { currentDestination = it })
                    }
                }
            ) {
                Scaffold(
                    modifier = modifier
                ) { innerPadding ->
                    MainContentLayout(innerPadding)
                }
            }
        } else {
            // Mobile mode: Modal Navigation Drawer
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = currentDestination != NavigationDestination.Investigation, // Disable gesture inside active terminal questioning
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = if (isDark) Color(0xFF16161B) else ParchmentBg,
                        drawerTonalElevation = 0.dp
                    ) {
                        DrawerContentSheet(onItemClick = { dest ->
                            currentDestination = dest
                            scope.launch { drawerState.close() }
                        })
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = currentDestination.title.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isDark) AmberAccent else WarmWoodBrown
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("drawer_open_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open Drawer",
                                        tint = if (isDark) Color.White else Color.Black
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { onIntent(ThemisIntent.ResetGame) },
                                    modifier = Modifier.testTag("reset_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restart Investigation",
                                        tint = if (isDark) Color.Gray else WarmWoodBrown
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (isDark) DarkTerminalBg else ParchmentBg
                            )
                        )
                    },
                    modifier = modifier
                ) { innerPadding ->
                    MainContentLayout(innerPadding)
                }
            }
        }

        // --- Overlays ---

        // Active Objection Overlay
        uiState.activeObjection?.let { objection ->
            ObjectionOverlay(
                objection = objection,
                currentPhase = uiState.currentPhase,
                onResolve = { sustain -> onIntent(ThemisIntent.ResolveObjection(sustain)) }
            )
        }

        // Trial Verdict Scroll Overlay
        if (uiState.evaluationReport == null) {
            uiState.trialVerdict?.let { verdict ->
                VerdictResultOverlay(
                    verdictText = verdict,
                    currentPhase = uiState.currentPhase,
                    onRestart = { onIntent(ThemisIntent.ResetGame) }
                )
            }
        }

        // Appellate Performance Evaluation Report Overlay
        uiState.evaluationReport?.let { report ->
            AppellateReportOverlay(
                report = report,
                onRestart = { onIntent(ThemisIntent.ResetGame) },
                onNavigateToGenerator = {
                    onIntent(ThemisIntent.ResetGame)
                    isCaseGeneratorActive = true
                }
            )
        }

        // Case Generator Overlay
        if (isCaseGeneratorActive) {
            CaseGeneratorScreen(
                isDark = isDark,
                onBack = { isCaseGeneratorActive = false },
                onGenerate = { params ->
                    hasInitiatedGeneration = true
                    onIntent(ThemisIntent.GenerateNewCase(params))
                },
                isGenerating = uiState.isGeneratingCase,
                progressText = uiState.generationProgress,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dossier Builder Overlay
        if (uiState.isDossierBuilderActive) {
            DossierBuilderScreen(
                evidenceList = uiState.evidenceList,
                selectedIds = uiState.selectedEvidenceIds,
                evidenceLinks = uiState.evidenceLinks,
                onToggleSelection = { onIntent(ThemisIntent.ToggleEvidenceSelection(it)) },
                onCreateLink = { sourceId, targetId, relType, justification ->
                    onIntent(ThemisIntent.CreateEvidenceLink(sourceId, targetId, relType, justification))
                },
                onGenerateMotions = { onIntent(ThemisIntent.GeneratePreTrialMotions) },
                onClose = { onIntent(ThemisIntent.CloseDossierBuilder) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Pre-Trial Hearing Overlay
        if (uiState.isPreTrialHearingActive) {
            PreTrialHearingScreen(
                motions = uiState.preTrialMotions,
                onRuleOnMotion = { motionId, sustain ->
                    onIntent(ThemisIntent.RuleOnPreTrialMotion(motionId, sustain))
                },
                onProceedToCourtroom = { onIntent(ThemisIntent.ProceedToCourtroomFromHearing) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GameDashboardScreen(
    uiState: ThemisUiState,
    onNavigate: (NavigationDestination) -> Unit,
    isDark: Boolean,
    onStartCustomCase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val coreCrimeText = uiState.caseGroundTruth?.coreCrime ?: "Duke Sterling is dead. Poisoned by alchemical toxicity during the grand banquet."
    val displayAbsoluteTruth = uiState.caseGroundTruth?.absoluteTruth != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkTerminalBg else ParchmentBg)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Welcoming Hero Banner Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF16161B) else Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "THE COURT OF THEMIS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = if (isDark) AmberAccent else WarmWoodBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "$coreCrimeText\n\nAs High Magistrate, you are the sole judge and arbiter of truth. Investigate the suspects, secure valid warrants, search for admissible evidence, and hold court to convict the guilty party or release the innocent.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onNavigate(NavigationDestination.Investigation) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) AmberAccent else WarmWoodBrown,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("enter_palace_btn")
                    ) {
                        Text("Enter Palace", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onStartCustomCase,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) AntiqueBrassGold else Color.DarkGray,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("start_custom_case_btn")
                    ) {
                        Text("Build Mystery ⚖️", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Immutable Ground Truth Secret Card (Private from player during normal play, but highly useful to check)
        if (displayAbsoluteTruth) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1B1B22) else Color(0xFFFFFDF5)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AntiqueBrassGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 IMMUTABLE GROUND TRUTH (MAGISTRATE'S EYE ONLY)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        ),
                        color = AmberAccent,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = uiState.caseGroundTruth?.absoluteTruth ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif, lineHeight = 16.sp),
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Culpability Graph:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.Gray else Color.DarkGray
                    )
                    uiState.caseGroundTruth?.suspectCulpabilities?.forEach { (suspectId, status) ->
                        Text(
                            text = "• Suspect ID '$suspectId' -> ${status.name}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        // Active State Cards
        Text(
            text = "CASE OVERVIEW",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = if (isDark) Color.Gray else Color.DarkGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF222226) else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    Text(
                        text = "${uiState.evidenceList.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) AmberAccent else WarmWoodBrown
                    )
                    Text(
                        text = "Evidence Items",
                        fontSize = 11.sp,
                        color = if (isDark) Color.Gray else Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF222226) else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    Text(
                        text = "${uiState.npcList.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) AmberAccent else WarmWoodBrown
                    )
                    Text(
                        text = "Active Suspects",
                        fontSize = 11.sp,
                        color = if (isDark) Color.Gray else Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Quick Board
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = if (isDark) Color.Gray else Color.DarkGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF16161B) else Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2A2A30) else Color.LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { onNavigate(NavigationDestination.CaseFiles) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF222226) else Color.LightGray,
                        contentColor = if (isDark) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Dossier", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inspect Evidence Dossier")
                    }
                }

                Button(
                    onClick = { onNavigate(NavigationDestination.Courtroom) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF222226) else Color.LightGray,
                        contentColor = if (isDark) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = "Gavel", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hold Trial Courtroom")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    uiState: ThemisUiState,
    onIntent: (ThemisIntent) -> Unit,
    isDark: Boolean
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF16161B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isDark) Color(0xFF2A2A30) else MaterialTheme.colorScheme.outlineVariant
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Game Phase & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFFFF8F00) else WarmWoodBrown)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.currentPhase.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.currentTime,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "🪙 ${uiState.magistrateGold}g",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) AmberAccent else Color(0xFFB57C00)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🗣️ ${uiState.publicSentiment}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CAF50)
                    )
                }

                // Phase Switcher Gavel Button
                Button(
                    onClick = {
                        if (uiState.currentPhase == GamePhase.INVESTIGATION) {
                            onIntent(ThemisIntent.OpenDossierBuilder)
                        } else {
                            onIntent(ThemisIntent.ChangePhase(GamePhase.INVESTIGATION))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) AntiqueBrassGold else WarmWoodBrown,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("phase_toggle_btn")
                ) {
                    Text(
                        text = if (isDark) "BUILD DOSSIER ⚖️" else "INVESTIGATE PALACE 🔍",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Conflict of Interest Meter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = "BIAS/COI: ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = { uiState.conflictOfInterest / 100f },
                        color = if (uiState.conflictOfInterest > 50) CrimsonAccent else AmberAccent,
                        trackColor = if (isDark) Color(0xFF2A2A30) else Color(0xFFE0E0E0),
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${uiState.conflictOfInterest}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (uiState.conflictOfInterest > 50) CrimsonAccent else (if (isDark) AmberAccent else WarmWoodBrown)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Toggle Search Warrant Active
                if (uiState.currentPhase == GamePhase.INVESTIGATION) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onIntent(ThemisIntent.ToggleWarrant) }
                    ) {
                        Text(
                            text = "SEARCH WARRANT: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isDark) Color.Gray else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (uiState.hasSearchWarrantActive) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.hasSearchWarrantActive) "ACTIVE" else "INACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (uiState.hasSearchWarrantActive) Color.Green else Color.Red
                            )
                        )
                    }
                }
            }

            val progress = uiState.caseProgress
            if (progress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF2A2A30) else Color.LightGray)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Days elapsed indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Days Elapsed",
                            tint = if (isDark) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DAYS: ${progress.daysElapsed}/${progress.maxDaysBeforeCold}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (progress.daysElapsed >= progress.maxDaysBeforeCold - 2) CrimsonAccent else (if (isDark) Color.White else Color.Black)
                        )
                    }

                    // Active leads remaining indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Leads Remaining",
                            tint = if (isDark) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LEADS REMAINING: ${progress.activeLeadsRemaining}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (progress.activeLeadsRemaining <= 3) CrimsonAccent else (if (isDark) Color.White else Color.Black)
                        )
                    }

                    // Public pressure / degradation warning
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Public Pressure",
                            tint = if (progress.publicPressure > 0.6f) CrimsonAccent else AmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PUBLIC PRESSURE: ${(progress.publicPressure * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (progress.publicPressure > 0.6f) CrimsonAccent else (if (isDark) AmberAccent else WarmWoodBrown)
                        )
                    }
                }
            }
        }
    }
}
