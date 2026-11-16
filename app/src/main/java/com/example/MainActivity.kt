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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.model.PdfResourceRecord
import com.example.data.repository.MrStudyRepository
import com.example.ui.screens.AppCheckSetupDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExamCenterScreen
import com.example.ui.screens.LiveLessonScreen
import com.example.ui.screens.PdfStudioScreen
import com.example.ui.screens.StudentProfileDialog
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.TeacherChatScreen
import com.example.ui.theme.MrStudyTheme
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
    NavDestinationItem("dashboard", "Panel", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavDestinationItem("teacher", "Öğretmen", Icons.Filled.School, Icons.Outlined.School),
    NavDestinationItem("pdf", "PDF", Icons.Filled.Description, Icons.Outlined.Description),
    NavDestinationItem("exam", "Sınav", Icons.Filled.Quiz, Icons.Outlined.Quiz),
    NavDestinationItem("study", "Çalışma", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
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
                            initialEmail = user.email.orEmpty()
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

    if (currentRoute != "dashboard" && !isLiveRoute) {
        BackHandler { navigateToTab("dashboard") }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail && !isLiveRoute) {
                    NavigationBar {
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
                                label = { Text(item.label, maxLines = 1) }
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
                if (useRail && !isLiveRoute) {
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
                                    onOpenLiveLesson = { navController.navigate("live_lesson") },
                                    onOpenPdfStudio = { navigateToTab("pdf") },
                                    onOpenExamCenter = { navigateToTab("exam") },
                                    onOpenStudyHub = { navigateToTab("study") },
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
                                    onOpenLiveLesson = { navController.navigate("live_lesson") },
                                    onOpenAppCheckSetup = { showAppCheckSetupDialog = true }
                                )
                            }

                            composable("live_lesson") {
                                LiveLessonScreen(
                                    appState = state,
                                    liveState = liveState,
                                    onBack = { navController.popBackStack() },
                                    onSelectSubject = viewModel::selectSubject,
                                    onSelectVoice = viewModel::selectLiveVoice,
                                    onToggleEchoGuard = viewModel::setLiveEchoGuardEnabled,
                                    onInterruptTeacher = viewModel::interruptLiveTeacher,
                                    onStartLiveSession = { viewModel.startLiveLesson(context) },
                                    onStopLiveSession = viewModel::stopLiveLesson,
                                    onToggleMute = viewModel::toggleLiveMute,
                                    onToggleBluetoothAudio = viewModel::toggleBluetoothAudio,
                                    onChangeLivePdfPage = viewModel::changeLivePdfPage,
                                    onSelectPdfForLive = viewModel::selectPdfForLive,
                                    onSendLiveText = viewModel::sendLiveText,
                                    onSendLiveImageUri = { uri -> viewModel.sendLiveImageUri(context, uri) },
                                    onSendActivePdfPage = viewModel::sendActivePdfPageToLive,
                                    onSaveTranscriptAsNote = viewModel::saveLiveTranscriptAsNote,
                                    onDismissError = viewModel::dismissLiveError,
                                    onOpenAppCheckSetup = { showAppCheckSetupDialog = true }
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
                                        navController.navigate("live_lesson")
                                    },
                                    onCreateExamFromPdf = { pdf ->
                                        preselectedExamPdf = pdf
                                        navigateToTab("exam")
                                    },
                                    onCreateFlashcardsFromPdf = { pdf ->
                                        viewModel.generateAiFlashcards(pdf.subject, pdf.title, pdf)
                                        navigateToTab("study")
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
