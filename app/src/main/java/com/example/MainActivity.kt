package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.pressableScale
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
import com.example.ui.theme.DeepNavyBar
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.MrStudyTheme
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.viewmodel.MrStudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

internal object MrStudyRoutes {
    const val DASHBOARD = "dashboard"
    const val LIVE_LESSON = "live_lesson"
    const val TEACHER = "teacher"
    const val PARENT = "parent"
    const val PDF = "pdf"
    const val EXAM = "exam"
    const val STUDY = "study"
    const val GAME_3D = "game_3d"

    val rootTabs = setOf(DASHBOARD, LIVE_LESSON, TEACHER, PARENT)
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val mrStudyNavItems = listOf(
    NavDestinationItem(MrStudyRoutes.DASHBOARD, "Ana Sayfa", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(MrStudyRoutes.LIVE_LESSON, "Canlı Ders", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    NavDestinationItem(MrStudyRoutes.TEACHER, "Sohbet", Icons.Filled.School, Icons.Outlined.School),
    NavDestinationItem(MrStudyRoutes.PARENT, "Ebeveyn", Icons.Filled.Shield, Icons.Outlined.Shield)
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MrStudyRoutes.DASHBOARD

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
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToScreen(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateBackSafely() {
        val popped = navController.popBackStack()
        if (!popped && currentRoute != MrStudyRoutes.DASHBOARD) {
            navigateToTab(MrStudyRoutes.DASHBOARD)
        }
    }

    val isLiveRoute = currentRoute == MrStudyRoutes.LIVE_LESSON
    val isGame3dRoute = currentRoute == MrStudyRoutes.GAME_3D
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val hideBottomChrome = isGame3dRoute || (isLiveRoute && liveState.isSessionActive) || isImeVisible

    // On non-start root tabs (Sohbet, Ebeveyn), system Back pops cleanly (e.g. PDF -> Sohbet -> PDF) or returns to Dashboard instead of closing app.
    if (currentRoute == MrStudyRoutes.TEACHER || currentRoute == MrStudyRoutes.PARENT) {
        BackHandler { navigateBackSafely() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                AnimatedVisibility(
                    visible = !useRail && !hideBottomChrome,
                    enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut(tween(140)) + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    MrStudyBottomBar(
                        currentRoute = currentRoute,
                        onSelectRoute = { navigateToTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !hideBottomChrome) {
                    NavigationRail(
                        containerColor = DeepNavyBar,
                        contentColor = Color.White
                    ) {
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
                                label = { Text(item.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
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

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 940.dp)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = MrStudyRoutes.DASHBOARD,
                            enterTransition = {
                                fadeIn(animationSpec = tween(220)) +
                                    slideInHorizontally(animationSpec = tween(250)) { fullWidth -> fullWidth / 16 }
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(170)) +
                                    slideOutHorizontally(animationSpec = tween(210)) { fullWidth -> -fullWidth / 20 }
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(220)) +
                                    slideInHorizontally(animationSpec = tween(250)) { fullWidth -> -fullWidth / 16 }
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(170)) +
                                    slideOutHorizontally(animationSpec = tween(210)) { fullWidth -> fullWidth / 20 }
                            }
                        ) {
                            composable(MrStudyRoutes.DASHBOARD) {
                                DashboardScreen(
                                    state = state,
                                    onOpenTeacher = { navigateToTab(MrStudyRoutes.TEACHER) },
                                    onOpenLiveLesson = { navigateToTab(MrStudyRoutes.LIVE_LESSON) },
                                    onOpenPdfStudio = { navigateToScreen(MrStudyRoutes.PDF) },
                                    onOpenExamCenter = { navigateToScreen(MrStudyRoutes.EXAM) },
                                    onOpenStudyHub = { navigateToScreen(MrStudyRoutes.STUDY) },
                                    onOpen3dGame = {
                                        viewModel.load3dGameQuestions()
                                        navigateToScreen(MrStudyRoutes.GAME_3D)
                                    },
                                    onSwitchGradeMode = viewModel::switchGradeMode,
                                    onToggleDailyTask = viewModel::toggleDailyTask,
                                    onLoadMiniQuiz = viewModel::loadMiniQuiz,
                                    onMiniQuizCompleted = viewModel::awardMiniQuizXp,
                                    onOpenProfile = { viewModel.setProfileSheetVisible(true) },
                                    onToggleTheme = viewModel::toggleThemeMode
                                )
                            }

                            composable(MrStudyRoutes.TEACHER) {
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
                                    onOpenLiveLesson = { navigateToScreen(MrStudyRoutes.LIVE_LESSON) },
                                    onExportWorksheet = { viewModel.exportChatWorksheetPdf(context) },
                                    onOpenAppCheckSetup = { showAppCheckSetupDialog = true }
                                )
                            }

                            composable(MrStudyRoutes.LIVE_LESSON) {
                                LiveLessonScreen(
                                    appState = state,
                                    liveState = liveState,
                                    onBack = { navigateBackSafely() },
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
                                        navigateToScreen(MrStudyRoutes.GAME_3D)
                                    },
                                    onExportLiveWorksheet = { viewModel.exportLiveWorksheetPdf(context) },
                                    onOpenPdfStudio = { navigateToScreen(MrStudyRoutes.PDF) },
                                    onDismissError = viewModel::dismissLiveError,
                                    onOpenAppCheckSetup = { showAppCheckSetupDialog = true }
                                )
                            }

                            composable(MrStudyRoutes.PARENT) {
                                ParentPanelScreen(
                                    state = state,
                                    onUpdatePin = viewModel::updateParentPin
                                )
                            }

                            composable(MrStudyRoutes.GAME_3D) {
                                Game3dScreen(
                                    subject = state.selectedSubject,
                                    topic = state.game3dTopic,
                                    questions = state.game3dQuestions,
                                    isLoading = state.isLoadingGame3d,
                                    onReloadQuestions = { viewModel.load3dGameQuestions() },
                                    onGameCompleted = viewModel::record3dGameCompletion,
                                    onBack = { navigateBackSafely() }
                                )
                            }

                            composable(MrStudyRoutes.PDF) {
                                PdfStudioScreen(
                                    state = state,
                                    onBack = { navigateBackSafely() },
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
                                        navigateToScreen(MrStudyRoutes.TEACHER)
                                        viewModel.sendTeacherMessage(context, prompt, includeActivePdf = true)
                                    },
                                    onStartLiveFromPdf = { pdf, page ->
                                        viewModel.selectSubject(pdf.subject)
                                        viewModel.selectPdfAndLoadPage(pdf, page)
                                        navigateToScreen(MrStudyRoutes.LIVE_LESSON)
                                    },
                                    onCreateExamFromPdf = { pdf ->
                                        preselectedExamPdf = pdf
                                        navigateToScreen(MrStudyRoutes.EXAM)
                                    },
                                    onCreateFlashcardsFromPdf = { pdf ->
                                        viewModel.generateAiFlashcards(pdf.subject, pdf.title, pdf)
                                        navigateToScreen(MrStudyRoutes.STUDY)
                                    }
                                )
                            }

                            composable(MrStudyRoutes.EXAM) {
                                ExamCenterScreen(
                                    state = state,
                                    preselectedPdf = preselectedExamPdf,
                                    onBack = { navigateBackSafely() },
                                    onStartExam = viewModel::startSmartExam,
                                    onAnswerChange = viewModel::updateExamAnswer,
                                    onSubmitExam = viewModel::submitExam,
                                    onResetToConfig = viewModel::resetExamToConfig
                                )
                            }

                            composable(MrStudyRoutes.STUDY) {
                                StudyHubScreen(
                                    state = state,
                                    onBack = { navigateBackSafely() },
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

@Composable
private fun MrStudyBottomBar(
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    Surface(
        color = DeepNavyBar.copy(alpha = 0.97f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Glowing top hairline separating content from bottom bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonPurplePrimary.copy(alpha = 0.75f),
                                CyberCyanTertiary.copy(alpha = 0.85f),
                                ElectricBlueSecondary.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mrStudyNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    val interactionSource = remember { MutableInteractionSource() }
                    val tintColor by animateColorAsState(
                        targetValue = if (selected) CyberCyanTertiary else Color(0xFF94A3B8),
                        animationSpec = tween(200),
                        label = "nav_tint"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) NeonPurplePrimary.copy(alpha = 0.28f) else Color.Transparent,
                        animationSpec = tween(220),
                        label = "nav_pill"
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = pillColor,
                        border = if (selected) {
                            BorderStroke(
                                1.1.dp,
                                Brush.linearGradient(listOf(NeonPurplePrimary, CyberCyanTertiary))
                            )
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .pressableScale(interactionSource)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(color = CyberCyanTertiary),
                                onClick = { onSelectRoute(item.route) }
                            )
                            .testTag("nav_tab_${item.route}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = tintColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = tintColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
