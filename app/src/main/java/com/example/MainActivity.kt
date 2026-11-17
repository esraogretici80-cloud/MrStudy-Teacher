package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.ai.MrStudyAiService
import com.example.data.local.LocalStudyMetricsStore
import com.example.data.model.PdfResourceRecord
import com.example.data.repository.MrStudyRepository
import com.example.ui.screens.AppCheckSetupDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExamCenterScreen
import com.example.ui.screens.Game3dScreen
import com.example.ui.screens.LiveLessonScreen
import com.example.ui.screens.ParentPanelScreen
import com.example.ui.screens.PdfStudioScreen
import com.example.ui.screens.StudentProfileDialog
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.TeacherChatScreen
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.MrStudyTheme
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.viewmodel.MrStudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val mrStudyNavItems = listOf(
    NavDestinationItem("dashboard", "Ana Sayfa", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem("live_lesson", "Canlı Ders", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    NavDestinationItem("teacher", "Sohbet", Icons.Filled.School, Icons.Outlined.School),
    NavDestinationItem("parent", "Ebeveyn", Icons.Filled.Shield, Icons.Outlined.Shield)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Check MUST be configured with launch intent before setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        MrStudyTheme {
            AuthScreen(
                onAuthSuccess = { /* AuthStateListener drives transition */ }
            )
        }
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: MrStudyViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MrStudyViewModel(
                            repository = MrStudyRepository(db),
                            aiService = MrStudyAiService(),
                            currentUserId = uid,
                            initialDisplayName = user.displayName ?: "Öğrenci",
                            initialEmail = user.email.orEmpty(),
                            metricsStore = LocalStudyMetricsStore(app, uid)
                        )
                    }
                }
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            MrStudyTheme(themeMode = state.profile.darkThemeMode) {
                MrStudyAppShell(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun MrStudyAppShell(viewModel: MrStudyViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val snackbarHostState = remember { SnackbarHostState() }
    var preselectedExamPdf by remember { mutableStateOf<PdfResourceRecord?>(null) }
    var showAppCheckSetupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusBanner) {
        val msg = state.statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isLiveRoute = currentRoute == "live_lesson"
    val isGame3dRoute = currentRoute == "game_3d"
    val hideBottomChrome = isGame3dRoute || (isLiveRoute && liveState.isSessionActive)

    if (currentRoute != "dashboard" && !isLiveRoute && !isGame3dRoute) {
        BackHandler { navigateToTab("dashboard") }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && !hideBottomChrome) {
                    NavigationBar(
                        containerColor = Color(0xFF0E1628).copy(alpha = 0.96f),
                        contentColor = Color.White
                    ) {
                        mrStudyNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyanTertiary,
                                    selectedTextColor = CyberCyanTertiary,
                                    indicatorColor = NeonPurplePrimary.copy(alpha = 0.28f),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !hideBottomChrome) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        mrStudyNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 920.dp)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard"
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    state = state,
                                    onOpenTeacher = { navigateToTab("teacher") },
                                    onOpenLiveLesson = { navigateToTab("live_lesson") },
                                    onOpenPdfStudio = { navController.navigate("pdf") },
                                    onOpenExamCenter = { navController.navigate("exam") },
                                    onOpenStudyHub = { navController.navigate("study") },
                                    onOpen3dGame = {
                                        viewModel.load3dGameQuestions()
                                        navController.navigate("game_3d")
                                    },
                                    onSwitchGradeMode = viewModel::switchGradeMode,
                                    onToggleDailyTask = viewModel::toggleDailyTask,
                                    onLoadMiniQuiz = viewModel::loadMiniQuiz,
                                    onMiniQuizCompleted = viewModel::awardMiniQuizXp,
                                    onOpenProfile = { viewModel.setProfileSheetVisible(true) },
                                    onToggleTheme = viewModel::toggleThemeMode
                                )
                            }

                            composable("teacher") {
                                TeacherChatScreen(
                                    state = state,
                                    onSelectMode = viewModel::selectTeachingMode,
                                    onSelectDepth = viewModel::selectExplanationDepth,
                                    onSelectSubject = viewModel::selectSubject,
                                    onSelectSession = viewModel::selectChatSession,
                                    onNewSession = { viewModel.createNewChatSession() },
                                    onDeleteSession = viewModel::deleteChatSession,
                                    onAttachImages = { uris -> viewModel.attachImages(context, uris) },
                                    onRemoveImage = viewModel::removeAttachedImage,
                                    onSendMessage = { text, bindPdf ->
                                        viewModel.sendTeacherMessage(context, text, bindPdf)
                                    },
                                    onCancelStreaming = viewModel::cancelTeacherStream,
                                    onDismissError = viewModel::dismissTeacherError,
                                    onOpenLiveLesson = { navigateToTab("live_lesson") },
                                    onExportWorksheet = { viewModel.exportChatWorksheetPdf(context) },
                                    onOpenAppCheckSetup = { showAppCheckSetupDialog = true }
                                )
                            }

                            composable("live_lesson") {
                                LiveLessonScreen(
                                    appState = state,
                                    liveState = liveState,
                                    onBack = { navigateToTab("dashboard") },
                                    onSelectSubject = viewModel::selectSubject,
                                    onSwitchGradeMode = viewModel::switchGradeMode,
                                    onSelectVoice = viewModel::selectLiveVoice,
                                    onToggleEchoGuard = viewModel::setLiveEchoGuardEnabled,
                                    onInterruptTeacher = viewModel::interruptLiveTeacher,
                                    onStartLiveSession = { viewModel.startLiveLesson(context) },
                                    onStopLiveSession = viewModel::stopLiveLesson,
                                    onToggleMute = viewModel::toggleLiveMute,
                                    onToggleBluetoothAudio = viewModel::toggleBluetoothAudio,
                                    onChangeLivePdfPage = viewModel::changeLivePdfPage,
                                    onJumpLivePdfPage = viewModel::jumpLiveToPdfPage,
                                    onSelectPdfForLive = viewModel::selectPdfForLive,
                                    onSendLiveText = viewModel::sendLiveText,
                                    onSendLiveImageUri = { uri -> viewModel.sendLiveImageUri(context, uri) },
                                    onSendActivePdfPage = viewModel::sendActivePdfPageToLive,
                                    onSaveTranscriptAsNote = viewModel::saveLiveTranscriptAsNote,
                                    onRefreshSmartBoard = { viewModel.refreshLiveSmartBoard() },
                                    onTriggerMidQuestion = viewModel::triggerMidLessonQuestion,
                                    onAnswerMidQuestion = viewModel::answerMidLessonQuestion,
                                    onDismissMidQuestion = viewModel::dismissMidLessonQuestion,
                                    onOpen3dGame = {
                                        viewModel.load3dGameQuestions()
                                        navController.navigate("game_3d")
                                    },
                                    onExportLiveWorksheet = { viewModel.exportLiveWorksheetPdf(context) },
                                    onDismissError = viewModel::dismissLiveError,
                                    onOpenAppCheckSetup = { showAppCheckSetupDialog = true }
                                )
                            }

                            composable("parent") {
                                ParentPanelScreen(
                                    state = state,
                                    onUpdatePin = viewModel::updateParentPin
                                )
                            }

                            composable("game_3d") {
                                Game3dScreen(
                                    subject = state.selectedSubject,
                                    topic = state.game3dTopic,
                                    questions = state.game3dQuestions,
                                    isLoading = state.isLoadingGame3d,
                                    onReloadQuestions = { viewModel.load3dGameQuestions() },
                                    onGameCompleted = viewModel::record3dGameCompletion,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("pdf") {
                                PdfStudioScreen(
                                    state = state,
                                    onImportPdfs = { uris, subj -> viewModel.importMultiplePdfs(context, uris, subj) },
                                    onRelinkPdf = { pdf, uri -> viewModel.relinkPdfFile(context, pdf, uri) },
                                    onSelectPdfAndPage = viewModel::selectPdfAndLoadPage,
                                    onRenamePdf = viewModel::renamePdf,
                                    onDeletePdf = { pdf -> viewModel.deletePdf(context, pdf) },
                                    onAnalyzePdf = viewModel::analyzeActivePdfAndSaveNote,
                                    onTeachPageWithAi = { pdf, page, mode, prompt ->
                                        viewModel.selectTeachingMode(mode)
                                        viewModel.selectSubject(pdf.subject)
                                        viewModel.createNewChatSession(
                                            title = "${pdf.title} (s.$page)",
                                            subject = pdf.subject,
                                            pdf = pdf,
                                            page = page
                                        )
                                        navigateToTab("teacher")
                                        viewModel.sendTeacherMessage(context, prompt, includeActivePdf = true)
                                    },
                                    onStartLiveFromPdf = { pdf, page ->
                                        viewModel.selectSubject(pdf.subject)
                                        viewModel.selectPdfAndLoadPage(pdf, page)
                                        navigateToTab("live_lesson")
                                    },
                                    onCreateExamFromPdf = { pdf ->
                                        preselectedExamPdf = pdf
                                        navController.navigate("exam")
                                    },
                                    onCreateFlashcardsFromPdf = { pdf ->
                                        viewModel.generateAiFlashcards(pdf.subject, pdf.title, pdf)
                                        navController.navigate("study")
                                    }
                                )
                            }

                            composable("exam") {
                                ExamCenterScreen(
                                    state = state,
                                    preselectedPdf = preselectedExamPdf,
                                    onStartExam = viewModel::startSmartExam,
                                    onAnswerChange = viewModel::updateExamAnswer,
                                    onSubmitExam = viewModel::submitExam,
                                    onResetToConfig = viewModel::resetExamToConfig
                                )
                            }

                            composable("study") {
                                StudyHubScreen(
                                    state = state,
                                    onAddPlan = viewModel::addManualStudyPlan,
                                    onGenerateAiPlan = viewModel::generateAiWeeklyPlan,
                                    onTogglePlan = viewModel::toggleStudyPlanComplete,
                                    onPostponePlan = viewModel::postponeStudyPlan,
                                    onDeletePlan = viewModel::deleteStudyPlan,
                                    onGenerateAiNote = viewModel::generateAiNote,
                                    onSaveNote = viewModel::saveManualOrEditedNote,
                                    onDeleteNote = viewModel::deleteNote,
                                    onExportNotePdf = { note -> viewModel.exportNotePdf(context, note) },
                                    onExportNoteWorksheet = { note -> viewModel.exportNoteWorksheetPdf(context, note) },
                                    onGenerateAiFlashcards = viewModel::generateAiFlashcards,
                                    onReviewFlashcard = viewModel::reviewFlashcard,
                                    onDeleteFlashcard = viewModel::deleteFlashcard
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showOnboardingDialog || state.showProfileSheet) {
        StudentProfileDialog(
            profile = state.profile,
            isOnboarding = state.showOnboardingDialog,
            onDismiss = {
                viewModel.setOnboardingVisible(false)
                viewModel.setProfileSheetVisible(false)
            },
            onSave = viewModel::saveStudentProfile,
            onSignOut = {
                viewModel.setProfileSheetVisible(false)
                signOutUser(credentialManager, scope)
            },
            onOpenAppCheckSetup = {
                viewModel.setProfileSheetVisible(false)
                showAppCheckSetupDialog = true
            }
        )
    }

    if (showAppCheckSetupDialog) {
        AppCheckSetupDialog(
            onDismiss = { showAppCheckSetupDialog = false },
            onVerifiedSuccess = {
                viewModel.dismissTeacherError()
                viewModel.dismissLiveError()
            }
        )
    }
}
