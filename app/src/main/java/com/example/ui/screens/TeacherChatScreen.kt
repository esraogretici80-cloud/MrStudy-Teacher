package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.LiveConnectionStatus
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.ExplanationDepth
import com.example.data.model.SubjectCatalog
import com.example.data.model.TeachingMode
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.MrStudyRobotAvatar
import com.example.ui.components.RichMarkdownText
import com.example.ui.components.pressableScale
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepNavyBar
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.util.TurkishVoiceSpeaker
import com.example.util.buildTurkishSpeechRecognizerIntent
import java.io.File

@Composable
fun TeacherChatScreen(
    state: MrStudyUiState,
    onSelectMode: (TeachingMode) -> Unit,
    onSelectDepth: (ExplanationDepth) -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onAttachImages: (List<Uri>) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onCancelStreaming: () -> Unit,
    onDismissError: () -> Unit,
    onOpenLiveLesson: () -> Unit,
    onExportWorksheet: () -> Unit = {},
    onOpenAppCheckSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var bindActivePdf by remember(state.activePdfId) { mutableStateOf(state.activePdf != null) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var showSubjectMenu by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val voiceSpeaker = remember(context) { TurkishVoiceSpeaker(context) }
    val speakingMsgId by voiceSpeaker.speakingMessageId.collectAsStateWithLifecycle()
    DisposableEffect(voiceSpeaker) {
        onDispose { voiceSpeaker.release() }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) onAttachImages(uris)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onAttachImages(listOf(uri))
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.streamingReply) {
        val total = state.messages.size + if (state.isTeacherStreaming) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls Glass Header (no horizontal collisions!)
            Surface(
                color = DeepNavyBar.copy(alpha = 0.94f),
                border = BorderStroke(0.9.dp, NeonPurplePrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MrStudyRobotAvatar(
                                status = if (state.isTeacherStreaming) LiveConnectionStatus.TEACHER_SPEAKING else LiveConnectionStatus.IDLE,
                                teacherAudioLevel = if (state.isTeacherStreaming) 0.55f else 0f,
                                userAudioLevel = 0f,
                                avatarSize = 44.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MrStudy AI Öğretmen",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = state.selectedMode.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyanTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            AssistChip(
                                onClick = onOpenLiveLesson,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = CyberCyanTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = { Text("Canlı", color = Color.White, maxLines = 1) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = NeonPurplePrimary.copy(alpha = 0.28f)
                                ),
                                modifier = Modifier.testTag("open_live_from_chat_button")
                            )
                            Box {
                                AssistChip(
                                    onClick = { showSubjectMenu = true },
                                    label = { Text(state.selectedSubject, maxLines = 1) }
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
                            IconButton(
                                onClick = onExportWorksheet,
                                enabled = !state.isExportingWorksheet,
                                modifier = Modifier.testTag("chat_export_worksheet_button")
                            ) {
                                if (state.isExportingWorksheet) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CyberCyanTertiary)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Çalışma Kağıdı İndir", tint = CyberCyanTertiary)
                                }
                            }
                            IconButton(
                                onClick = { showSessionDialog = true },
                                modifier = Modifier.testTag("chat_sessions_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Sohbet Geçmişi", tint = Color.White)
                            }
                            IconButton(
                                onClick = onNewSession,
                                modifier = Modifier.testTag("new_chat_session_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Yeni Oturum", tint = Color.White)
                            }
                        }
                    }

                    // 7 Teaching Modes Strip
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TeachingMode.entries) { mode ->
                            val selected = state.selectedMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMode(mode) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurplePrimary.copy(alpha = 0.32f),
                                    selectedLabelColor = CyberCyanTertiary
                                ),
                                label = { Text(mode.title, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    // Depth + Active PDF Source toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExplanationDepth.entries.forEach { depth ->
                                FilterChip(
                                    selected = state.selectedDepth == depth,
                                    onClick = { onSelectDepth(depth) },
                                    label = { Text(depth.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        if (state.activePdf != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = CyberCyanTertiary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PDF s.${state.activePdfPage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(
                                    checked = bindActivePdf,
                                    onCheckedChange = { bindActivePdf = it }
                                )
                            }
                        }
                    }
                }
            }

            // Error Banner
            AnimatedVisibility(
                visible = !state.teacherError.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = state.teacherError.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissError) {
                                Text("Kapat")
                            }
                        }

                        if (state.isTeacherAppCheckError) {
                            AppCheckStatusQuickCard(
                                onOpenStatusDialog = onOpenAppCheckSetup
                            )
                        }
                    }
                }
            }

            // Messages Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.messages.isEmpty() && !state.isTeacherStreaming) {
                    item {
                        GlassCard(
                            accentBorder = NeonPurplePrimary,
                            secondaryBorder = CyberCyanTertiary,
                            cornerRadius = 20.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Hoş geldin ${state.profile.displayName}!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyanTertiary
                                )
                                Text(
                                    text = "Şu an '${state.selectedMode.title}' modundayım. Bana anlamadığın konuyu sorabilir, defterindeki sorunun fotoğrafını yükleyebilir veya PDF stüdyosundan seçtiğin sayfa üzerinden ders anlatmamı isteyebilirsin.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                items(state.messages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                    ChatBubble(
                        message = msg,
                        isSpeaking = speakingMsgId == msg.id,
                        onToggleSpeak = { voiceSpeaker.toggleSpeak(msg.id, msg.content) }
                    )
                }

                if (state.isTeacherStreaming) {
                    item {
                        ChatBubble(
                            message = ChatMessage(
                                id = "streaming",
                                role = "assistant",
                                content = state.streamingReply,
                                teachingMode = state.selectedMode.title
                            ),
                            isSpeaking = false,
                            isStreaming = true,
                            onToggleSpeak = {}
                        )
                    }
                }
            }

            // Quick Pedagogical Prompts & Weak-Topic Recommendations
            val weakTopics = remember(state.examHistory) {
                state.examHistory
                    .flatMap { it.weakTopics }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Pratik Soru" to "Bu konuyla ilgili seviyeme uygun 3 pratik soru sor.",
                    "Özet Çıkar" to "Şu ana kadar anlattığımız konunun kısa madde madde özetini çıkar.",
                    "Farklı Yolla Anlat" to "Bu konuyu günlük hayattan farklı bir örnek ve benzetmeyle tekrar anlat.",
                    "Görsel & Sesli Anlatım" to "Bu konuyu görsel canlandırmalar ve adım adım sesli anlatım kurgusuyla açıkla."
                ).forEach { (label, prompt) ->
                    AssistChip(
                        onClick = { onSendMessage(prompt, bindActivePdf) },
                        enabled = !state.isTeacherStreaming,
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                weakTopics.forEach { weak ->
                    AssistChip(
                        onClick = { onSendMessage("Sınavlarda zorlandığım '$weak' konusunu temelden anlat ve 2 alıştırma ver.", bindActivePdf) },
                        enabled = !state.isTeacherStreaming,
                        label = { Text("Zayıf Konu: $weak", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary) }
                    )
                }
            }

            // Attached Images Strip
            if (state.attachedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.attachedImages) { img ->
                        Box(modifier = Modifier.size(72.dp)) {
                            Image(
                                bitmap = img.bitmap.asImageBitmap(),
                                contentDescription = img.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Surface(
                                color = DeepNavyBar.copy(alpha = 0.88f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(22.dp)
                                    .clickable { onRemoveImage(img.uri) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Görseli Kaldır",
                                    tint = Color.White,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Input Composer (IME-safe padding)
            Surface(
                color = DeepNavyBar.copy(alpha = 0.96f),
                border = BorderStroke(0.9.dp, NeonPurplePrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("pick_image_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeriden Görsel Seç", tint = CyberCyanTertiary)
                    }

                    IconButton(
                        onClick = {
                            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
                            val photoFile = File(cameraDir, "soru_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.testTag("camera_capture_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Kamera ile Soru Çek", tint = CyberCyanTertiary)
                    }

                    IconButton(
                        onClick = {
                            try {
                                speechLauncher.launch(buildTurkishSpeechRecognizerIntent())
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.testTag("voice_input_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Sesli Soru Sor", tint = CyberCyanTertiary)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Sorunu veya konuyu yaz...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teacher_chat_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (state.isTeacherStreaming) {
                        IconButton(
                            onClick = onCancelStreaming,
                            modifier = Modifier
                                .size(48.dp)
                                .pressableScale()
                                .clip(CircleShape)
                                .background(DangerRed)
                                .testTag("stop_teacher_message_button")
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Yanıtı Durdur",
                                tint = Color.White
                            )
                        }
                    } else {
                        val canSend = inputText.isNotBlank() || state.attachedImages.isNotEmpty()
                        IconButton(
                            onClick = {
                                val text = inputText
                                inputText = ""
                                onSendMessage(text, bindActivePdf)
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(48.dp)
                                .pressableScale()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            NeonPurplePrimary.copy(alpha = if (canSend) 1f else 0.45f),
                                            ElectricBlueSecondary.copy(alpha = if (canSend) 1f else 0.45f)
                                        )
                                    )
                                )
                                .testTag("send_teacher_message_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gönder",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = { Text("Kayıtlı Ders Oturumları") },
            text = {
                if (state.sessions.isEmpty()) {
                    Text("Henüz kayıtlı bir sohbet oturumu yok.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sessions, key = { it.id }) { sess ->
                            Surface(
                                color = if (sess.id == state.activeSessionId) {
                                    NeonPurplePrimary.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectSession(sess)
                                        showSessionDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sess.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = "${sess.subject} • ${sess.teachingMode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteSession(sess.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Oturumu Sil")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessionDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    isStreaming: Boolean = false,
    onToggleSpeak: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) NeonPurplePrimary.copy(alpha = 0.88f) else Color(0xFF131B2E).copy(alpha = 0.92f),
            contentColor = Color.White,
            border = BorderStroke(
                1.dp,
                if (isUser) CyberCyanTertiary.copy(alpha = 0.35f) else ElectricBlueSecondary.copy(alpha = 0.42f)
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${message.teachingMode.ifBlank { "MrStudy" }} • ${message.sourceCitation.ifBlank { "Ders Anlatımı" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyanTertiary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isStreaming) {
                            IconButton(
                                onClick = onToggleSpeak,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking) "Seslendirmeyi Durdur" else "Sesli Dinle",
                                    tint = CyberCyanTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else if (message.hasAttachment && message.attachmentLabel.isNotBlank()) {
                    Text(
                        text = "📎 ${message.attachmentLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isStreaming && message.content.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyberCyanTertiary
                        )
                        Text(
                            text = "MrStudy sorunu inceliyor ve en iyi çözümü hazırlıyor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                } else {
                    RichMarkdownText(
                        text = message.content,
                        color = Color.White
                    )
                }
            }
        }
    }
}
