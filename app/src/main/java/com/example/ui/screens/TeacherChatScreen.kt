package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.ExplanationDepth
import com.example.data.model.SubjectCatalog
import com.example.data.model.TeachingMode
import com.example.ui.state.MrStudyUiState
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
    onSendMessage: (String, Boolean) -> Unit
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Controls Header
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_teacher_avatar),
                            contentDescription = "MrStudy Öğretmen",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MrStudy AI Öğretmen",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Text(
                                text = state.selectedMode.subtitle,
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
                            onClick = { showSessionDialog = true },
                            modifier = Modifier.testTag("chat_sessions_button")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Sohbet Geçmişi")
                        }
                        IconButton(
                            onClick = onNewSession,
                            modifier = Modifier.testTag("new_chat_session_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Yeni Oturum")
                        }
                    }
                }

                // 7 Teaching Modes Strip
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TeachingMode.entries) { mode ->
                        FilterChip(
                            selected = state.selectedMode == mode,
                            onClick = { onSelectMode(mode) },
                            label = { Text(mode.title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                // Depth + Active PDF Source toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PDF s.${state.activePdfPage}",
                                style = MaterialTheme.typography.labelSmall,
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
        if (!state.teacherError.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.teacherError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Hoş geldin ${state.profile.displayName}!",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Şu an '${state.selectedMode.title}' modundayım. Bana anlamadığın konuyu sorabilir, defterindeki sorunun fotoğrafını yükleyebilir veya PDF stüdyosundan seçtiğin sayfa üzerinden ders anlatmamı isteyebilirsin.",
                                style = MaterialTheme.typography.bodyMedium
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
                    val liveText = state.streamingReply.ifBlank { "Öğretmenin düşünüyor ve yanıtı hazırlıyor..." }
                    ChatBubble(
                        message = ChatMessage(
                            id = "streaming",
                            role = "assistant",
                            content = liveText,
                            teachingMode = state.selectedMode.title
                        ),
                        isSpeaking = false,
                        isStreaming = true,
                        onToggleSpeak = {}
                    )
                }
            }
        }

        // Quick Pedagogical Prompts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Günlük hayattan örnek ver",
                "Farklı bir örnekle tekrar açıkla",
                "Öğrenme kontrolü sorusu sor",
                "Bana seviyeme uygun ödev ver"
            ).forEach { prompt ->
                AssistChip(
                    onClick = { onSendMessage(prompt, bindActivePdf) },
                    enabled = !state.isTeacherStreaming,
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
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
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
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
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input Composer
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
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
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeriden Görsel Seç")
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
                    Icon(Icons.Default.CameraAlt, contentDescription = "Kamera ile Soru Çek")
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
                    Icon(Icons.Default.Mic, contentDescription = "Sesli Soru Sor")
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

                IconButton(
                    onClick = {
                        val text = inputText
                        inputText = ""
                        onSendMessage(text, bindActivePdf)
                    },
                    enabled = !state.isTeacherStreaming && (inputText.isNotBlank() || state.attachedImages.isNotEmpty()),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("send_teacher_message_button")
                ) {
                    if (state.isTeacherStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
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
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            tonalElevation = if (isUser) 0.dp else 2.dp,
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
                            color = MaterialTheme.colorScheme.secondary,
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
                                    tint = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
