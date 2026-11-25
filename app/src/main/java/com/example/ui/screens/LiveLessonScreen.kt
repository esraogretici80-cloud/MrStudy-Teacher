package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.RichMarkdownText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveLessonUiState
import com.example.data.ai.LiveTranscriptEntry
import com.example.data.model.MiniQuizQuestion
import com.example.data.model.PdfResourceRecord
import com.example.data.model.SubjectCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.MrStudyLogoHeader
import com.example.ui.components.MrStudyRobotAvatar
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SmartBoardCanvas
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyBar
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@Composable
fun LiveLessonScreen(
    appState: MrStudyUiState,
    liveState: LiveLessonUiState,
    onBack: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onSwitchGradeMode: (String) -> Unit = {},
    onSelectVoice: (String) -> Unit,
    onToggleEchoGuard: (Boolean) -> Unit = {},
    onInterruptTeacher: () -> Unit = {},
    onStartLiveSession: () -> Unit,
    onStopLiveSession: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleBluetoothAudio: () -> Unit = {},
    onChangeLivePdfPage: (delta: Int) -> Unit = {},
    onJumpLivePdfPage: (pageNumber: Int) -> Unit = {},
    onSelectPdfForLive: (PdfResourceRecord) -> Unit = {},
    onSendLiveText: (String) -> Unit,
    onSendLiveImageUri: (Uri) -> Unit,
    onSendActivePdfPage: () -> Unit,
    onSaveTranscriptAsNote: () -> Unit,
    onRefreshSmartBoard: () -> Unit = {},
    onTriggerMidQuestion: () -> Unit = {},
    onAnswerMidQuestion: (Boolean) -> Unit = {},
    onDismissMidQuestion: () -> Unit = {},
    onOpen3dGame: () -> Unit = {},
    onExportLiveWorksheet: () -> Unit = {},
    onOpenPdfStudio: () -> Unit = {},
    onDismissError: () -> Unit,
    onOpenAppCheckSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var showPermissionDeniedCard by remember { mutableStateOf(false) }
    var showSubjectMenu by remember { mutableStateOf(false) }
    var showPdfMenu by remember { mutableStateOf(false) }
    var showVoiceMenu by remember { mutableStateOf(false) }
    var isPdfPreviewExpanded by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var jumpInputText by remember { mutableStateOf("") }
    var quickInputText by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothPermission = granted
        if (granted) onToggleBluetoothAudio()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        showPermissionDeniedCard = !granted
        if (granted) onStartLiveSession()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSendLiveImageUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) onSendLiveImageUri(uri)
    }

    BackHandler {
        if (liveState.isSessionActive) {
            onStopLiveSession()
        }
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (liveState.isSessionActive) {
                onStopLiveSession()
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(
        liveState.transcriptItems.size,
        liveState.liveUserTranscript,
        liveState.liveTeacherTranscript
    ) {
        val count = liveState.transcriptItems.size +
            (if (liveState.liveUserTranscript.isNotBlank()) 1 else 0) +
            (if (liveState.liveTeacherTranscript.isNotBlank()) 1 else 0)
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    val currentVoiceOption = LiveLessonUiState.voiceOptions.firstOrNull { it.id == liveState.selectedVoice }
        ?: LiveLessonUiState.voiceOptions.first()

    fun handleStartOrResumeVoice() {
        if (liveState.isSessionActive) {
            if (liveState.status == LiveConnectionStatus.TEACHER_SPEAKING && liveState.echoGuardEnabled) {
                onInterruptTeacher()
            } else {
                onToggleMute()
            }
        } else {
            if (hasMicPermission) {
                onStartLiveSession()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Clean Two-Tier Futuristic Top Bar (zero overflow or collisions)
            Surface(
                color = DeepNavyBar.copy(alpha = 0.88f),
                border = BorderStroke(0.8.dp, NeonPurplePrimary.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MrStudyLogoHeader(
                        gradeLevel = appState.profile.gradeLevel,
                        onToggleGradeMode = onSwitchGradeMode
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Circular 48dp Back Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF172038),
                            border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (liveState.isSessionActive) onStopLiveSession()
                                    onBack()
                                }
                                .testTag("live_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Geri Dön",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        LiveStatusPill(
                            status = liveState.status,
                            elapsedSeconds = liveState.elapsedSeconds
                        )

                        // BT / Speaker Audio Route Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF141D35),
                            border = BorderStroke(
                                1.dp,
                                if (liveState.isBluetoothHeadsetConnected) CyberCyanTertiary else ElectricBlueSecondary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
                                        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    } else {
                                        onToggleBluetoothAudio()
                                    }
                                }
                                .testTag("live_audio_route_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (liveState.isBluetoothHeadsetConnected) Icons.Default.Headphones else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (liveState.isBluetoothHeadsetConnected) CyberCyanTertiary else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (liveState.isBluetoothHeadsetConnected) "BT" else "Hoparlör",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        // Subject Selector Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF141D35),
                                border = BorderStroke(1.dp, NeonPurplePrimary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = !liveState.isSessionActive) { showSubjectMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color(0xFFC4B5FD),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = appState.selectedSubject,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showSubjectMenu,
                                onDismissRequest = { showSubjectMenu = false }
                            ) {
                                SubjectCatalog.subjects.forEach { subj ->
                                    DropdownMenuItem(
                                        text = { Text(subj) },
                                        onClick = {
                                            onSelectSubject(subj)
                                            showSubjectMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        if (liveState.transcriptItems.isNotEmpty() && !liveState.isSessionActive) {
                            Surface(
                                shape = CircleShape,
                                color = CyberCyanTertiary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, CyberCyanTertiary.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onSaveTranscriptAsNote)
                                    .testTag("save_live_note_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.BookmarkAdd,
                                        contentDescription = "Dersi Notlara Kaydet",
                                        tint = CyberCyanTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error Banner
            AnimatedVisibility(
                visible = !liveState.errorMessage.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = liveState.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        if (hasMicPermission) {
                                            onStartLiveSession()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("live_reconnect_button")
                                ) {
                                    Text("Yeniden Bağlan", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = onDismissError) { Text("Kapat") }
                            }
                        }
                        if (liveState.isAppCheckError) {
                            AppCheckStatusQuickCard(
                                onOpenStatusDialog = onOpenAppCheckSetup
                            )
                        }
                    }
                }
            }

            if (showPermissionDeniedCard && !hasMicPermission) {
                GlassCard(
                    accentBorder = DangerRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.MicOff, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mikrofon İzni Gerekli", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(
                                "Canlı sesli ders anlatımı için mikrofon erişimine izin vermeniz gerekir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        TextButton(onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text("İzin Ver")
                        }
                    }
                }
            }

            // 2. Main Scrollable Live Classroom Content (never overlaps bottom bar)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .animateContentSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Digital Smart Board + 3 Horizontal Action Cards (matching Görsel 2!)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmartBoardCanvas(
                            payload = appState.smartBoardState,
                            isGenerating = appState.isGeneratingSmartBoard,
                            subject = appState.selectedSubject,
                            onCustomizeClick = onRefreshSmartBoard
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiveActionTile(
                                title = "Tahtaya Çiz",
                                subtitle = "(JSON)",
                                icon = Icons.Default.Edit,
                                accent = CyberCyanTertiary,
                                isLoading = appState.isGeneratingSmartBoard,
                                onClick = onRefreshSmartBoard,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("live_smart_board_draw_button")
                            )
                            LiveActionTile(
                                title = "Anlık Soru Sor",
                                subtitle = "(AI)",
                                icon = Icons.Default.Quiz,
                                accent = NeonPurplePrimary,
                                isLoading = appState.isLoadingMidQuestion,
                                onClick = onTriggerMidQuestion,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("live_mid_question_button")
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                LiveActionTile(
                                    title = "PDF Çalış",
                                    subtitle = if (appState.activePdf != null) "s.${appState.activePdfPage}" else "(Dosya)",
                                    icon = Icons.Default.PictureAsPdf,
                                    accent = ElectricBlueSecondary,
                                    isLoading = appState.isExportingWorksheet,
                                    onClick = { showPdfMenu = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("live_bind_pdf_chip")
                                )
                                DropdownMenu(
                                    expanded = showPdfMenu,
                                    onDismissRequest = { showPdfMenu = false }
                                ) {
                                    if (appState.pdfSources.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Kayıtlı PDF yok — PDF Stüdyo'da Ekle") },
                                            onClick = {
                                                showPdfMenu = false
                                                onOpenPdfStudio()
                                            }
                                        )
                                    } else {
                                        appState.pdfSources.forEach { pdf ->
                                            DropdownMenuItem(
                                                text = { Text("${pdf.title} (${pdf.pageCount} s.)") },
                                                onClick = {
                                                    onSelectPdfForLive(pdf)
                                                    showPdfMenu = false
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("PDF Stüdyo'yu Aç (Yükle / Yönet)") },
                                        onClick = {
                                            showPdfMenu = false
                                            onOpenPdfStudio()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("PDF Çalışma Kağıdı İndir (A4)") },
                                        onClick = {
                                            showPdfMenu = false
                                            onExportLiveWorksheet()
                                        }
                                    )
                                }
                            }
                        }

                        // Compact Worksheet Export strip cleanly aligned
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ElectricBlueSecondary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = !appState.isExportingWorksheet, onClick = onExportLiveWorksheet)
                                .testTag("live_export_worksheet_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (appState.isExportingWorksheet) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyberCyanTertiary)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CyberCyanTertiary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ders Özeti & 5 Soruluk PDF Çalışma Kağıdı İndir (A4)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Mid-Lesson Pop-up Question Card
                val midQ = appState.midLessonQuestion
                if (midQ != null) {
                    item {
                        MidLessonQuestionPopupCard(
                            question = midQ,
                            onAnswered = onAnswerMidQuestion,
                            onDismiss = onDismissMidQuestion
                        )
                    }
                }

                // 3. Hero "Gemini Live • Canlı Ders" Card with 3D Robot + CTA (matching Görsel 2!)
                item {
                    GlassCard(
                        accentBorder = NeonPurplePrimary,
                        secondaryBorder = CyberCyanTertiary,
                        cornerRadius = 24.dp,
                        containerColor = Color(0xFF121833).copy(alpha = 0.90f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MrStudyRobotAvatar(
                                    status = liveState.status,
                                    teacherAudioLevel = liveState.teacherAudioLevel,
                                    userAudioLevel = liveState.userAudioLevel,
                                    avatarSize = 126.dp,
                                    showBook = true
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        liveState.isSessionActive -> SuccessGreen
                                                        else -> CyberCyanTertiary
                                                    }
                                                )
                                        )
                                        Text(
                                            text = "Gemini Live • Canlı Ders",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = liveState.statusNotice?.takeIf { it.isNotBlank() }
                                            ?: "Sorularını sor, konuları birlikte çözelim. Yapay zekâ öğretmenin hep yanında!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (liveState.isSessionActive) {
                                        LiveAudioVisualizerBars(
                                            level = maxOf(liveState.teacherAudioLevel, liveState.userAudioLevel),
                                            isTeacherSpeaking = liveState.status == LiveConnectionStatus.TEACHER_SPEAKING,
                                            isSessionActive = liveState.isSessionActive
                                        )
                                    }

                                    NeonGradientButton(
                                        text = when {
                                            !liveState.isSessionActive -> "Mikrofonu Aç"
                                            liveState.status == LiveConnectionStatus.TEACHER_SPEAKING && liveState.echoGuardEnabled -> "Sözü Al"
                                            liveState.isMuted -> "Mikrofonu Aç"
                                            else -> "Mikrofon Açık"
                                        },
                                        icon = if (liveState.isMuted && liveState.isSessionActive) Icons.Default.MicOff else Icons.Default.Mic,
                                        onClick = { handleStartOrResumeVoice() },
                                        gradientColors = listOf(NeonPurplePrimary, ElectricBlueSecondary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Voice Selector & Echo Guard strip when idle (clean, no overlap)
                            if (!liveState.isSessionActive) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box {
                                        OutlinedButton(
                                            onClick = { showVoiceMenu = true },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, NeonPurplePrimary.copy(alpha = 0.65f)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("live_voice_selector_button")
                                        ) {
                                            Icon(
                                                Icons.Default.RecordVoiceOver,
                                                contentDescription = null,
                                                tint = CyberCyanTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Öğretmen Sesi Seç: ${currentVoiceOption.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }

                                        DropdownMenu(
                                            expanded = showVoiceMenu,
                                            onDismissRequest = { showVoiceMenu = false }
                                        ) {
                                            LiveLessonUiState.voiceOptions.forEach { voice ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = if (voice.id == liveState.selectedVoice) "✓ ${voice.label}" else voice.label,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = voice.description,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        onSelectVoice(voice.id)
                                                        showVoiceMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    FilterChip(
                                        selected = liveState.echoGuardEnabled,
                                        onClick = { onToggleEchoGuard(true) },
                                        label = { Text("Akıcı Anlatım (Hoparlör)", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("live_echo_guard_chip")
                                    )
                                    FilterChip(
                                        selected = !liveState.echoGuardEnabled,
                                        onClick = { onToggleEchoGuard(false) },
                                        label = { Text("Serbest Diyalog (Kulaklık)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Post-Lesson 3D Game CTA
                if (appState.liveSessionEndedShowGameCta && !liveState.isSessionActive) {
                    item {
                        GlassCard(
                            accentBorder = CyberCyanTertiary,
                            containerColor = Color(0xFF1A103D).copy(alpha = 0.90f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Canlı Ders Tamamlandı! Konuyu Pekiştir",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyanTertiary
                                )
                                NeonGradientButton(
                                    text = "Konuyu 3B Dünyada Pekiştir",
                                    icon = Icons.Default.RocketLaunch,
                                    onClick = onOpen3dGame,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_3d_game_cta_button")
                                )
                            }
                        }
                    }
                }

                // 5. PDF Pagination & Keyboard Page Jump Controller
                val currentPdf = appState.activePdf
                if (currentPdf != null) {
                    item {
                        GlassCard(
                            accentBorder = ElectricBlueSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_pdf_control_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = ElectricBlueSecondary.copy(alpha = 0.22f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Description,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = CyberCyanTertiary
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentPdf.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Canlı derse bağlı PDF kaynağı",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        TextButton(
                                            onClick = {
                                                jumpInputText = ""
                                                showJumpToPageDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("live_pdf_jump_button")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyanTertiary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sayfaya Git", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                                        }
                                        TextButton(
                                            onClick = { showPdfMenu = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Değiştir", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0A1020), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { onChangeLivePdfPage(-1) },
                                        enabled = appState.activePdfPage > 1,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("live_pdf_prev_page")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Önceki Sayfa (<)",
                                            tint = if (appState.activePdfPage > 1) CyberCyanTertiary else Color(0xFF475569)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                jumpInputText = ""
                                                showJumpToPageDialog = true
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("pdf_page_counter_clickable")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Sayfa ${appState.activePdfPage} / ${currentPdf.pageCount}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Klavyeden Sayfa Seç",
                                                modifier = Modifier.size(14.dp),
                                                tint = CyberCyanTertiary
                                            )
                                            if (appState.isPdfBusy) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            }
                                        }
                                        Text(
                                            text = "Sayfa seçmek için dokun veya kaydır",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onChangeLivePdfPage(1) },
                                        enabled = appState.activePdfPage < currentPdf.pageCount,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("live_pdf_next_page")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Sonraki Sayfa (>)",
                                            tint = if (appState.activePdfPage < currentPdf.pageCount) CyberCyanTertiary else Color(0xFF475569)
                                        )
                                    }
                                }

                                if (currentPdf.pageCount > 2) {
                                    var sliderPos by remember(currentPdf.id, appState.activePdfPage) {
                                        mutableFloatStateOf(appState.activePdfPage.toFloat())
                                    }
                                    var isDraggingSlider by remember(currentPdf.id) { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "1",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Slider(
                                            value = if (isDraggingSlider) sliderPos else appState.activePdfPage.toFloat(),
                                            onValueChange = {
                                                isDraggingSlider = true
                                                sliderPos = it
                                            },
                                            onValueChangeFinished = {
                                                val target = sliderPos.roundToInt().coerceIn(1, currentPdf.pageCount.toInt().coerceAtLeast(1))
                                                isDraggingSlider = false
                                                onJumpLivePdfPage(target)
                                            },
                                            valueRange = 1f..currentPdf.pageCount.toFloat().coerceAtLeast(2f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = CyberCyanTertiary,
                                                activeTrackColor = NeonPurplePrimary,
                                                inactiveTrackColor = Color(0xFF1E293B)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("live_pdf_page_slider")
                                        )
                                        Text(
                                            text = if (isDraggingSlider) "s.${sliderPos.roundToInt()}" else "${currentPdf.pageCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isDraggingSlider) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDraggingSlider) CyberCyanTertiary else Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                val pdfErr = appState.pdfError
                                if (!pdfErr.isNullOrBlank()) {
                                    Surface(
                                        color = DangerRed.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = pdfErr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFFCA5A5),
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = onOpenPdfStudio,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("PDF Stüdyo", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                                            }
                                        }
                                    }
                                }

                                if (appState.renderedPdfPage != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isPdfPreviewExpanded = !isPdfPreviewExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (isPdfPreviewExpanded) "Sayfa önizlemesini gizle" else "Sayfa önizlemesini göster",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberCyanTertiary
                                        )
                                        Icon(
                                            imageVector = if (isPdfPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = CyberCyanTertiary
                                        )
                                    }

                                    AnimatedVisibility(visible = isPdfPreviewExpanded) {
                                        Crossfade(
                                            targetState = appState.renderedPdfPage,
                                            animationSpec = tween(350),
                                            label = "PdfPageCrossfade"
                                        ) { bmp ->
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "PDF Sayfa Önizleme",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 200.dp)
                                                        .clip(RoundedCornerShape(10.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Transcript Bubbles
                items(liveState.transcriptItems, key = { it.id }) { entry ->
                    LiveTranscriptBubble(entry = entry)
                }

                if (liveState.liveUserTranscript.isNotBlank()) {
                    item {
                        LiveTranscriptBubble(
                            entry = LiveTranscriptEntry(
                                id = "partial_user",
                                isStudent = true,
                                text = liveState.liveUserTranscript,
                                tag = "Dinleniyor..."
                            )
                        )
                    }
                }

                if (liveState.liveTeacherTranscript.isNotBlank()) {
                    item {
                        LiveTranscriptBubble(
                            entry = LiveTranscriptEntry(
                                id = "partial_teacher",
                                isStudent = false,
                                text = liveState.liveTeacherTranscript,
                                tag = "Anlatıyor..."
                            )
                        )
                    }
                }
            }

            // 3. Active Session Quick Input Bar (IME-safe!)
            AnimatedVisibility(
                visible = liveState.isSessionActive && liveState.status != LiveConnectionStatus.CONNECTING,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = DeepNavyBar.copy(alpha = 0.96f),
                    border = BorderStroke(0.8.dp, ElectricBlueSecondary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (appState.activePdf != null) {
                                AssistChip(
                                    onClick = onSendActivePdfPage,
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("PDF s.${appState.activePdfPage} Göster") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = ElectricBlueSecondary.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.testTag("live_send_pdf_button")
                                )
                            }

                            AssistChip(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Soru Görseli Göster") }
                            )

                            AssistChip(
                                onClick = {
                                    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
                                    val photoFile = File(cameraDir, "live_soru_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Kameradan Çek") }
                            )

                            listOf(
                                "Biraz daha yavaş anlat",
                                "Örnek soru sor",
                                "Günlük hayattan örnek ver",
                                "Kısaca özetle"
                            ).forEach { prompt ->
                                AssistChip(
                                    onClick = { onSendLiveText(prompt) },
                                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickInputText,
                                onValueChange = { quickInputText = it },
                                placeholder = { Text("Canlı derse yazılı soru veya formül yaz...") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val t = quickInputText.trim()
                                    if (t.isNotBlank()) {
                                        quickInputText = ""
                                        onSendLiveText(t)
                                    }
                                },
                                enabled = quickInputText.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurplePrimary)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Canlı Mesaj Gönder",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 4. Bottom Call Controls Bar (sits cleanly inside Scaffold above BottomNavBar — ZERO overlap!)
            Surface(
                color = DeepNavyBar.copy(alpha = 0.96f),
                border = BorderStroke(0.8.dp, NeonPurplePrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (liveState.status == LiveConnectionStatus.TEACHER_SPEAKING && liveState.echoGuardEnabled) {
                        FilledTonalButton(
                            onClick = onInterruptTeacher,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("live_interrupt_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Sözü Al ve Soru Sor")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sözü Al", maxLines = 1)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onToggleMute,
                            enabled = liveState.isSessionActive && liveState.status != LiveConnectionStatus.CONNECTING,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                1.dp,
                                if (liveState.isSessionActive) CyberCyanTertiary.copy(alpha = 0.6f) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("live_mute_button")
                        ) {
                            Icon(
                                imageVector = if (liveState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (liveState.isMuted) "Mikrofonu Aç" else "Mikrofonu Kapat"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (liveState.isMuted) "Sessizde" else "Mikrofon", maxLines = 1)
                        }
                    }

                    if (liveState.isSessionActive) {
                        Button(
                            onClick = onStopLiveSession,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .testTag("live_call_toggle_button")
                        ) {
                            if (liveState.status == LiveConnectionStatus.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("İptal", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CallEnd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dersi Bitir", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    } else {
                        NeonGradientButton(
                            text = "Canlı Dersi Başlat",
                            icon = Icons.Default.Call,
                            onClick = {
                                if (hasMicPermission) {
                                    onStartLiveSession()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            gradientColors = listOf(NeonPurplePrimary, ElectricBlueSecondary),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .testTag("live_call_toggle_button")
                        )
                    }
                }
            }
        }

        val dialogPdf = appState.activePdf
        if (showJumpToPageDialog && dialogPdf != null) {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            val maxPageInt = dialogPdf.pageCount.toInt().coerceAtLeast(1)

            LaunchedEffect(showJumpToPageDialog) {
                delay(100L)
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            AlertDialog(
                onDismissRequest = {
                    keyboardController?.hide()
                    showJumpToPageDialog = false
                },
                title = { Text("Klavyeden Sayfa Seç") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Gitmek istediğiniz sayfa numarasını yazın (1 - $maxPageInt • Şu an: ${appState.activePdfPage}):",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = jumpInputText,
                            onValueChange = { jumpInputText = it.filter(Char::isDigit).take(5) },
                            placeholder = { Text("Örn: 46") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    jumpInputText.toIntOrNull()?.let { target ->
                                        onJumpLivePdfPage(target.coerceIn(1, maxPageInt))
                                        keyboardController?.hide()
                                        showJumpToPageDialog = false
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("jump_page_input")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "1" to 1,
                                "-10" to (appState.activePdfPage - 10).coerceAtLeast(1),
                                "+10" to (appState.activePdfPage + 10).coerceAtMost(maxPageInt),
                                "$maxPageInt (Son)" to maxPageInt
                            ).forEach { (label, target) ->
                                AssistChip(
                                    onClick = {
                                        onJumpLivePdfPage(target)
                                        keyboardController?.hide()
                                        showJumpToPageDialog = false
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            jumpInputText.toIntOrNull()?.let { target ->
                                onJumpLivePdfPage(target.coerceIn(1, maxPageInt))
                            }
                            keyboardController?.hide()
                            showJumpToPageDialog = false
                        },
                        enabled = jumpInputText.toIntOrNull() != null,
                        modifier = Modifier.testTag("jump_page_confirm")
                    ) { Text("Git") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        keyboardController?.hide()
                        showJumpToPageDialog = false
                    }) { Text("İptal") }
                }
            )
        }
    }
}

@Composable
private fun LiveActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        onClick = { if (!isLoading) onClick() },
        cornerRadius = 16.dp,
        accentBorder = accent,
        secondaryBorder = accent.copy(alpha = 0.4f),
        containerColor = Color(0xFF0E162B).copy(alpha = 0.90f),
        modifier = modifier.heightIn(min = 58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = accent)
                    } else {
                        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveAudioVisualizerBars(
    level: Float,
    isTeacherSpeaking: Boolean,
    isSessionActive: Boolean
) {
    val barColor = when {
        isTeacherSpeaking -> NeonPurplePrimary
        isSessionActive -> CyberCyanTertiary
        else -> Color(0xFF475569)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(22.dp)
    ) {
        val weights = listOf(0.35f, 0.7f, 1.0f, 0.85f, 1.0f, 0.7f, 0.35f)
        weights.forEach { mult ->
            val heightDp = if (isSessionActive) {
                (6f + level.coerceIn(0.08f, 1f) * 16f * mult).dp
            } else {
                (5f + mult * 3f).dp
            }
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun MidLessonQuestionPopupCard(
    question: MiniQuizQuestion,
    onAnswered: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember(question) { mutableStateOf<Int?>(null) }

    GlassCard(accentBorder = CyberCyanTertiary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Ders Ortası Kontrol Sorusu",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyanTertiary
                )
                TextButton(onClick = onDismiss) { Text("Kapat") }
            }
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            question.options.forEachIndexed { idx, opt ->
                val isCorrect = idx == question.correctIndex
                val isPicked = picked == idx
                val bg = when {
                    picked == null -> Color(0xFF1E293B).copy(alpha = 0.85f)
                    isCorrect -> SuccessGreen.copy(alpha = 0.28f)
                    isPicked -> DangerRed.copy(alpha = 0.28f)
                    else -> Color(0xFF1E293B).copy(alpha = 0.45f)
                }
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            picked != null && isCorrect -> SuccessGreen
                            isPicked -> DangerRed
                            else -> ElectricBlueSecondary.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = picked == null) {
                            picked = idx
                            onAnswered(idx == question.correctIndex)
                        }
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            if (picked != null && question.explanation.isNotBlank()) {
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyanTertiary
                )
            }
        }
    }
}

@Composable
private fun LiveStatusPill(status: LiveConnectionStatus, elapsedSeconds: Int) {
    val bg = when (status) {
        LiveConnectionStatus.TEACHER_SPEAKING -> NeonPurplePrimary.copy(alpha = 0.30f)
        LiveConnectionStatus.LISTENING -> ElectricBlueSecondary.copy(alpha = 0.30f)
        LiveConnectionStatus.ERROR -> DangerRed.copy(alpha = 0.30f)
        LiveConnectionStatus.CONNECTING -> CyberCyanTertiary.copy(alpha = 0.25f)
        else -> Color(0xFF1E293B)
    }
    val fg = when (status) {
        LiveConnectionStatus.TEACHER_SPEAKING -> Color(0xFFDDD6FE)
        LiveConnectionStatus.LISTENING -> Color(0xFFBFDBFE)
        LiveConnectionStatus.ERROR -> Color(0xFFFCA5A5)
        else -> CyberCyanTertiary
    }
    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val timeText = if (elapsedSeconds > 0) String.format(" • %02d:%02d", mins, secs) else ""

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = "${status.label}$timeText",
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LiveTranscriptBubble(entry: LiveTranscriptEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isStudent) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (entry.isStudent) {
                NeonPurplePrimary.copy(alpha = 0.88f)
            } else {
                Color(0xFF141B2D).copy(alpha = 0.90f)
            },
            contentColor = Color.White,
            border = if (entry.isStudent) null else BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (entry.isStudent) 16.dp else 4.dp,
                bottomEnd = if (entry.isStudent) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (entry.isStudent) "Sen" else "MrStudy Canlı",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isStudent) Color.White.copy(alpha = 0.85f) else CyberCyanTertiary
                    )
                    if (entry.tag.isNotBlank()) {
                        Text(
                            text = "• ${entry.tag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                if (entry.isStudent) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                } else {
                    RichMarkdownText(
                        markdown = entry.text,
                        color = Color.White
                    )
                }
            }
        }
    }
}
