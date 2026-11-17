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
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.components.GradeModeBadge
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.MrStudyRobotAvatar
import com.example.ui.components.SmartBoardCanvas
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import java.io.File

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

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Live Classroom Bar (Glassmorphism, zero overlap)
            Surface(
                color = Color(0xFF0E1628).copy(alpha = 0.88f),
                border = BorderStroke(0.8.dp, NeonPurplePrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (liveState.isSessionActive) onStopLiveSession()
                                onBack()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("live_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri Dön",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "MrStudy Live",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                LiveStatusPill(
                                    status = liveState.status,
                                    elapsedSeconds = liveState.elapsedSeconds
                                )
                            }
                            Text(
                                text = "${appState.selectedSubject} • ${currentVoiceOption.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyanTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        GradeModeBadge(
                            currentGrade = appState.profile.gradeLevel,
                            onToggleGradeMode = onSwitchGradeMode
                        )

                        // BT / Speaker toggle
                        AssistChip(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
                                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    onToggleBluetoothAudio()
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (liveState.isBluetoothHeadsetConnected) Icons.Default.Headphones else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (liveState.isBluetoothHeadsetConnected) CyberCyanTertiary else Color.White
                                )
                            },
                            label = {
                                Text(
                                    text = if (liveState.isBluetoothHeadsetConnected) "BT" else "Hoparlör",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            },
                            modifier = Modifier.testTag("live_audio_route_chip")
                        )

                        Box {
                            AssistChip(
                                onClick = { if (!liveState.isSessionActive) showSubjectMenu = true },
                                label = {
                                    Text(
                                        appState.selectedSubject,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                },
                                enabled = !liveState.isSessionActive
                            )
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
                            IconButton(
                                onClick = onSaveTranscriptAsNote,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("save_live_note_button")
                            ) {
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

            // Error Banner
            AnimatedVisibility(visible = !liveState.errorMessage.isNullOrBlank()) {
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = liveState.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissError) { Text("Kapat") }
                        }
                        val token = liveState.debugAppCheckToken
                        if (!token.isNullOrBlank()) {
                            AppCheckTokenQuickCard(
                                debugToken = token,
                                onOpenSetupDialog = onOpenAppCheckSetup
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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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

            // Main Scrollable Classroom Stage
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. TOP: Digital Interactive Smart Board + Quick Board Actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmartBoardCanvas(
                            payload = appState.smartBoardState,
                            isGenerating = appState.isGeneratingSmartBoard,
                            subject = appState.selectedSubject
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = onRefreshSmartBoard,
                                enabled = !appState.isGeneratingSmartBoard,
                                leadingIcon = {
                                    if (appState.isGeneratingSmartBoard) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = CyberCyanTertiary)
                                    }
                                },
                                label = { Text("Tahtaya Çiz (JSON)", style = MaterialTheme.typography.labelSmall, color = Color.White) },
                                modifier = Modifier.testTag("live_smart_board_draw_button")
                            )

                            AssistChip(
                                onClick = onTriggerMidQuestion,
                                enabled = !appState.isLoadingMidQuestion,
                                leadingIcon = {
                                    if (appState.isLoadingMidQuestion) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(15.dp), tint = NeonPurplePrimary)
                                    }
                                },
                                label = { Text("Anlık Soru Sor", style = MaterialTheme.typography.labelSmall, color = Color.White) },
                                modifier = Modifier.testTag("live_mid_question_button")
                            )

                            AssistChip(
                                onClick = onExportLiveWorksheet,
                                enabled = !appState.isExportingWorksheet,
                                leadingIcon = {
                                    if (appState.isExportingWorksheet) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = ElectricBlueSecondary)
                                    }
                                },
                                label = { Text("PDF Çalışma Kağıdı İndir", style = MaterialTheme.typography.labelSmall, color = Color.White) },
                                modifier = Modifier.testTag("live_export_worksheet_button")
                            )
                        }
                    }
                }

                // 2. MID-LESSON POP-UP QUESTION CARD (when triggered)
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

                // 3. MIDDLE: Talking MrStudy Robot Mascot + Audio Visualizer + Voice Selector
                item {
                    GlassCard(accentBorder = NeonPurplePrimary) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MrStudyRobotAvatar(
                                status = liveState.status,
                                teacherAudioLevel = liveState.teacherAudioLevel,
                                userAudioLevel = liveState.userAudioLevel,
                                avatarSize = 148.dp
                            )

                            LiveAudioVisualizerBars(
                                level = maxOf(liveState.teacherAudioLevel, liveState.userAudioLevel),
                                isTeacherSpeaking = liveState.status == LiveConnectionStatus.TEACHER_SPEAKING,
                                isSessionActive = liveState.isSessionActive
                            )

                            if (!liveState.statusNotice.isNullOrBlank()) {
                                Text(
                                    text = liveState.statusNotice,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyanTertiary,
                                    maxLines = 2
                                )
                            }

                            // Voice Selector ("Öğretmen Sesi Seç") + Echo Guard
                            if (!liveState.isSessionActive) {
                                Box {
                                    OutlinedButton(
                                        onClick = { showVoiceMenu = true },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, NeonPurplePrimary.copy(alpha = 0.7f)),
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
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Color.White)
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

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                                ) {
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

                // 4. POST-LESSON 3D GAME CTA (appears after ending a Live session)
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
                                Button(
                                    onClick = onOpen3dGame,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("open_3d_game_cta_button")
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Konuyu 3B Dünyada Pekiştir", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onExportLiveWorksheet,
                                    enabled = !appState.isExportingWorksheet,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("PDF Çalışma Kağıdı İndir (A4)")
                                }
                            }
                        }
                    }
                }

                // 5. PDF Pagination & Document Controller Bar (preserved with Keyboard Jump + Crossfade)
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
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            modifier = Modifier.size(32.dp)
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

                                    Box {
                                        TextButton(
                                            onClick = { showPdfMenu = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Değiştir", style = MaterialTheme.typography.labelSmall)
                                        }
                                        DropdownMenu(
                                            expanded = showPdfMenu,
                                            onDismissRequest = { showPdfMenu = false }
                                        ) {
                                            appState.pdfSources.forEach { pdf ->
                                                DropdownMenuItem(
                                                    text = { Text(pdf.title) },
                                                    onClick = {
                                                        onSelectPdfForLive(pdf)
                                                        showPdfMenu = false
                                                    }
                                                )
                                            }
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
                                        enabled = appState.activePdfPage > 1 && !appState.isPdfBusy,
                                        modifier = Modifier
                                            .size(40.dp)
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
                                                jumpInputText = appState.activePdfPage.toString()
                                                showJumpToPageDialog = true
                                            }
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
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
                                            text = "Klavyeyle gitmek için dokun",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onChangeLivePdfPage(1) },
                                        enabled = appState.activePdfPage < currentPdf.pageCount && !appState.isPdfBusy,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("live_pdf_next_page")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Sonraki Sayfa (>)",
                                            tint = if (appState.activePdfPage < currentPdf.pageCount) CyberCyanTertiary else Color(0xFF475569)
                                        )
                                    }
                                }

                                if (appState.renderedPdfPage != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isPdfPreviewExpanded = !isPdfPreviewExpanded }
                                            .padding(vertical = 2.dp),
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
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }

                                if (showJumpToPageDialog) {
                                    val focusRequester = remember { FocusRequester() }
                                    val keyboardController = LocalSoftwareKeyboardController.current

                                    LaunchedEffect(Unit) {
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
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Gitmek istediğiniz sayfa numarasını yazın (1 - ${currentPdf.pageCount}):")
                                                OutlinedTextField(
                                                    value = jumpInputText,
                                                    onValueChange = { jumpInputText = it.filter(Char::isDigit) },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Number,
                                                        imeAction = ImeAction.Go
                                                    ),
                                                    keyboardActions = KeyboardActions(
                                                        onGo = {
                                                            jumpInputText.toIntOrNull()?.let(onJumpLivePdfPage)
                                                            keyboardController?.hide()
                                                            showJumpToPageDialog = false
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(focusRequester)
                                                        .testTag("jump_page_input")
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    jumpInputText.toIntOrNull()?.let(onJumpLivePdfPage)
                                                    keyboardController?.hide()
                                                    showJumpToPageDialog = false
                                                },
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
                    }
                } else if (appState.pdfSources.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box {
                                AssistChip(
                                    onClick = { showPdfMenu = true },
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("📄 Canlı Derse PDF Bağla (${appState.pdfSources.size} kaynak)") },
                                    modifier = Modifier.testTag("live_bind_pdf_chip")
                                )
                                DropdownMenu(
                                    expanded = showPdfMenu,
                                    onDismissRequest = { showPdfMenu = false }
                                ) {
                                    appState.pdfSources.forEach { pdf ->
                                        DropdownMenuItem(
                                            text = { Text("${pdf.title} (${pdf.pageCount} sayfa)") },
                                            onClick = {
                                                onSelectPdfForLive(pdf)
                                                showPdfMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Intro or Transcript Bubbles
                if (liveState.transcriptItems.isEmpty() &&
                    liveState.liveUserTranscript.isBlank() &&
                    liveState.liveTeacherTranscript.isBlank()
                ) {
                    item {
                        GlassCard(accentBorder = ElectricBlueSecondary) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Gerçek Zamanlı Sesli Ders Başlat",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Alttaki Başlat düğmesine dokunduğunda MrStudy robot öğretmeninle akıcı konuşabilir, sözünü kesip soru sorabilir veya Akıllı Tahta & PDF sayfan üzerinden anlatım alabilirsin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

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

            // Active Session Quick Strip
            if (liveState.isSessionActive && liveState.status != LiveConnectionStatus.CONNECTING) {
                Surface(
                    color = Color(0xFF0E1628).copy(alpha = 0.94f),
                    modifier = Modifier.fillMaxWidth()
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

            // Bottom Call Controls Bar
            Surface(
                color = Color(0xFF0B0F19).copy(alpha = 0.96f),
                border = BorderStroke(0.8.dp, NeonPurplePrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (liveState.status == LiveConnectionStatus.TEACHER_SPEAKING && liveState.echoGuardEnabled) {
                        FilledTonalButton(
                            onClick = onInterruptTeacher,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("live_interrupt_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Sözü Al ve Soru Sor")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sözü Al")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onToggleMute,
                            enabled = liveState.isSessionActive && liveState.status != LiveConnectionStatus.CONNECTING,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("live_mute_button")
                        ) {
                            Icon(
                                imageVector = if (liveState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (liveState.isMuted) "Mikrofonu Aç" else "Mikrofonu Kapat"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (liveState.isMuted) "Sessizde" else "Mikrofon")
                        }
                    }

                    Button(
                        onClick = {
                            if (liveState.isSessionActive) {
                                onStopLiveSession()
                            } else {
                                if (hasMicPermission) {
                                    onStartLiveSession()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liveState.isSessionActive) DangerRed else NeonPurplePrimary
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("live_call_toggle_button")
                    ) {
                        if (liveState.status == LiveConnectionStatus.CONNECTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("İptal")
                        } else if (liveState.isSessionActive) {
                            Icon(Icons.Default.CallEnd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dersi Bitir", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Canlı Dersi Başlat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
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

    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = "${status.label}$timeText",
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}
