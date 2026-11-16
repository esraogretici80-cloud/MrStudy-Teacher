package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveLessonUiState
import com.example.data.ai.LiveTranscriptEntry
import com.example.data.model.SubjectCatalog
import com.example.ui.state.MrStudyUiState
import java.io.File

@Composable
fun LiveLessonScreen(
    appState: MrStudyUiState,
    liveState: LiveLessonUiState,
    onBack: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectVoice: (String) -> Unit,
    onStartLiveSession: () -> Unit,
    onStopLiveSession: () -> Unit,
    onToggleMute: () -> Unit,
    onSendLiveText: (String) -> Unit,
    onSendLiveImageUri: (Uri) -> Unit,
    onSendActivePdfPage: () -> Unit,
    onSaveTranscriptAsNote: () -> Unit,
    onDismissError: () -> Unit,
    onOpenAppCheckSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDeniedCard by remember { mutableStateOf(false) }
    var showSubjectMenu by remember { mutableStateOf(false) }
    var quickInputText by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var copiedToken by remember(liveState.debugAppCheckToken) { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        showPermissionDeniedCard = !granted
        if (granted) {
            onStartLiveSession()
        }
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
        if (success && uri != null) {
            onSendLiveImageUri(uri)
        }
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

    val deepStageGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(deepStageGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Live Classroom Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = {
                            if (liveState.isSessionActive) onStopLiveSession()
                            onBack()
                        },
                        modifier = Modifier.testTag("live_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri Dön"
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Gemini Live Ders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            LiveStatusPill(
                                status = liveState.status,
                                elapsedSeconds = liveState.elapsedSeconds
                            )
                        }
                        Text(
                            text = "${appState.selectedSubject} • ${appState.selectedMode.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AssistChip(
                            onClick = { if (!liveState.isSessionActive) showSubjectMenu = true },
                            label = { Text(appState.selectedSubject, maxLines = 1) },
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
                            modifier = Modifier.testTag("save_live_note_button")
                        ) {
                            Icon(
                                Icons.Default.BookmarkAdd,
                                contentDescription = "Dersi Notlara Kaydet",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Error or App Check Diagnostic Banner
        AnimatedVisibility(visible = !liveState.errorMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        TextButton(onClick = onDismissError) {
                            Text("Kapat")
                        }
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.MicOff, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mikrofon İzni Gerekli", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Canlı sesli ders anlatımı için mikrofon erişimine izin vermeniz gerekir.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("İzin Ver")
                    }
                }
            }
        }

        // Animated Live Voice Orb + Voice Config Stage
        LiveAcousticOrbStage(
            status = liveState.status,
            userLevel = liveState.userAudioLevel,
            teacherLevel = liveState.teacherAudioLevel,
            statusNotice = liveState.statusNotice,
            selectedVoice = liveState.selectedVoice,
            isSessionActive = liveState.isSessionActive,
            activePdfTitle = appState.activePdf?.title,
            activePdfPage = appState.activePdfPage,
            onSelectVoice = onSelectVoice
        )

        // Live Scrolling Transcripts
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (liveState.transcriptItems.isEmpty() &&
                liveState.liveUserTranscript.isBlank() &&
                liveState.liveTeacherTranscript.isBlank()
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Gerçek Zamanlı Sesli Ders Başlat",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Alttaki Başlat düğmesine dokunduğunda MrStudy öğretmeninle telefon görüşmesi gibi akıcı konuşabilir, istediğin an sözünü kesip soru sorabilir veya defter/PDF sayfanı göstererek anlık anlatım alabilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Multimodal / Quick Action Strip when Connected
        if (liveState.isSessionActive && liveState.status != LiveConnectionStatus.CONNECTING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
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

            // Optional silent text bar during live voice call
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Canlı Mesaj Gönder",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Bottom Call Controls Bar
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute / Unmute
                FilledTonalButton(
                    onClick = onToggleMute,
                    enabled = liveState.isSessionActive && liveState.status != LiveConnectionStatus.CONNECTING,
                    shape = CircleShape,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("live_mute_button")
                ) {
                    Icon(
                        imageVector = if (liveState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (liveState.isMuted) "Mikrofonu Aç" else "Mikrofonu Kapat"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (liveState.isMuted) "Sessizde" else "Mikrofon")
                }

                // Start or End Live Lesson Call
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
                        containerColor = if (liveState.isSessionActive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .height(58.dp)
                        .testTag("live_call_toggle_button")
                ) {
                    if (liveState.status == LiveConnectionStatus.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
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

@Composable
private fun LiveStatusPill(status: LiveConnectionStatus, elapsedSeconds: Int) {
    val bg = when (status) {
        LiveConnectionStatus.TEACHER_SPEAKING -> MaterialTheme.colorScheme.primaryContainer
        LiveConnectionStatus.LISTENING -> MaterialTheme.colorScheme.secondaryContainer
        LiveConnectionStatus.MUTED -> MaterialTheme.colorScheme.surfaceVariant
        LiveConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        LiveConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.tertiaryContainer
        LiveConnectionStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when (status) {
        LiveConnectionStatus.TEACHER_SPEAKING -> MaterialTheme.colorScheme.onPrimaryContainer
        LiveConnectionStatus.LISTENING -> MaterialTheme.colorScheme.onSecondaryContainer
        LiveConnectionStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LiveAcousticOrbStage(
    status: LiveConnectionStatus,
    userLevel: Float,
    teacherLevel: Float,
    statusNotice: String?,
    selectedVoice: String,
    isSessionActive: Boolean,
    activePdfTitle: String?,
    activePdfPage: Int,
    onSelectVoice: (String) -> Unit
) {
    val targetLevel = maxOf(userLevel, teacherLevel)
    val animatedLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = spring(stiffness = 260f, dampingRatio = 0.7f),
        label = "orb_level"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(164.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.28f
                val activeBoost = if (isSessionActive) animatedLevel * 28f + idlePulse * 6f else idlePulse * 4f
                val ringColor = when (status) {
                    LiveConnectionStatus.TEACHER_SPEAKING -> primaryColor
                    LiveConnectionStatus.LISTENING -> secondaryColor
                    else -> tertiaryColor
                }

                drawCircle(
                    color = ringColor.copy(alpha = 0.14f + animatedLevel * 0.18f),
                    radius = baseRadius + 30f + activeBoost * 1.4f,
                    center = center
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.24f + animatedLevel * 0.22f),
                    radius = baseRadius + 14f + activeBoost,
                    center = center
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.55f),
                    radius = baseRadius + 4f + activeBoost * 0.4f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            Image(
                painter = painterResource(id = R.drawable.img_teacher_avatar),
                contentDescription = "MrStudy Canlı Öğretmen",
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        if (!statusNotice.isNullOrBlank()) {
            Text(
                text = statusNotice,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2
            )
        }

        if (!activePdfTitle.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Bağlı Kaynak: $activePdfTitle (s.$activePdfPage)",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Voice selector (interactive before call starts)
        if (!isSessionActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                LiveLessonUiState.voiceOptions.forEach { voice ->
                    FilterChip(
                        selected = selectedVoice == voice.id,
                        onClick = { onSelectVoice(voice.id) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = { Text(voice.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
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
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (entry.isStudent) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            tonalElevation = if (entry.isStudent) 0.dp else 2.dp,
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
                        color = if (entry.isStudent) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                    if (entry.tag.isNotBlank()) {
                        Text(
                            text = "• ${entry.tag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.isStudent) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
