package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SubjectCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import java.util.Locale

@Composable
fun AudioLessonScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onSaveAudioLesson: (title: String, subject: String, script: String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedSubject by remember { mutableStateOf(state.selectedSubject) }
    var titleInput by remember { mutableStateOf("Podcast Ders Özeti") }
    var scriptInput by remember {
        mutableStateOf(
            state.notes.firstOrNull()?.content
                ?: "MrStudy 4.0 Sesli Ders Stüdyosuna hoş geldin. Buraya ders notunu, özetini veya PDF'ten çıkardığın metni yapıştırarak podcast gibi dinleyebilirsin."
        )
    }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var isReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentChunkIdx by remember { mutableIntStateOf(0) }

    val chunks = remember(scriptInput) {
        scriptInput.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(scriptInput.trim()).filter { it.isNotBlank() } }
    }

    val tts = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = instance?.setLanguage(Locale("tr", "TR"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    instance?.setLanguage(Locale.getDefault())
                }
                isReady = true
            }
        }
        instance
    }

    fun speakFromChunk(startIdx: Int) {
        if (!isReady || chunks.isEmpty()) return
        val safeIdx = startIdx.coerceIn(0, chunks.lastIndex)
        currentChunkIdx = safeIdx
        isPlaying = true
        isPaused = false
        tts.setSpeechRate(speechRate)
        tts.stop()
        for (i in safeIdx..chunks.lastIndex) {
            val mode = if (i == safeIdx) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunks[i], mode, null, "chunk_$i")
        }
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("chunk_")?.toIntOrNull()
                if (idx != null) currentChunkIdx = idx
            }

            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("chunk_")?.toIntOrNull() ?: return
                if (idx >= chunks.lastIndex) {
                    isPlaying = false
                    isPaused = false
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isPlaying = false
            }
        })
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Sesli Ders & Podcast Stüdyosu",
                subtitle = "Metinleri & Notları Akıcı Sesle Dinle",
                onBack = onBack,
                accentColor = NeonPurplePrimary,
                badgeText = "${speechRate}x Hız"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.notes.isNotEmpty() || state.pdfSources.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Hızlı Kaynak Aktar (Not / PDF Özeti)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.notes.take(5).forEach { note ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            titleInput = note.title
                                            selectedSubject = note.subject
                                            scriptInput = "${note.title}. ${note.content}"
                                        },
                                        label = { Text("Not: ${note.title.take(18)}") }
                                    )
                                }
                                state.pdfSources.filter { it.summary.isNotBlank() }.take(4).forEach { pdf ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            titleInput = "${pdf.title} Özeti"
                                            selectedSubject = pdf.subject
                                            scriptInput = pdf.summary
                                        },
                                        label = { Text("PDF: ${pdf.title.take(18)}") }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    GlassCard(accentBorder = ElectricBlueSecondary, cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SubjectCatalog.subjects.take(8).forEach { subj ->
                                    FilterChip(
                                        selected = selectedSubject == subj,
                                        onClick = { selectedSubject = subj },
                                        label = { Text(subj, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Sesli Ders Başlığı") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = scriptInput,
                                onValueChange = { scriptInput = it },
                                label = { Text("Dinlenecek Ders Metni") },
                                minLines = 5,
                                maxLines = 10,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("audio_lesson_script_input")
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hız:", style = MaterialTheme.typography.labelMedium)
                                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                                    FilterChip(
                                        selected = speechRate == spd,
                                        onClick = {
                                            speechRate = spd
                                            if (isPlaying) speakFromChunk(currentChunkIdx)
                                        },
                                        label = { Text("${spd}x") }
                                    )
                                }
                            }

                            val progress = if (chunks.isNotEmpty() && (isPlaying || isPaused)) {
                                ((currentChunkIdx + 1).toFloat() / chunks.size).coerceIn(0f, 1f)
                            } else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = CyberCyanTertiary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NeonGradientButton(
                                    text = when {
                                        isPlaying -> "Duraklat"
                                        isPaused -> "Devam Et"
                                        else -> "Dinle"
                                    },
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    enabled = isReady && chunks.isNotEmpty(),
                                    onClick = {
                                        if (isPlaying) {
                                            tts.stop()
                                            isPlaying = false
                                            isPaused = true
                                        } else {
                                            speakFromChunk(if (isPaused) currentChunkIdx else 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("audio_play_pause_button")
                                )
                                OutlinedButton(
                                    onClick = {
                                        tts.stop()
                                        isPlaying = false
                                        isPaused = false
                                        currentChunkIdx = 0
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Durdur")
                                }
                                OutlinedButton(
                                    onClick = { onSaveAudioLesson(titleInput, selectedSubject, scriptInput) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Sesli Dersi Kaydet")
                                }
                            }
                        }
                    }
                }

                if (state.studioState.audioLessons.isNotEmpty()) {
                    item {
                        Text("Kayıtlı Sesli Dersler (${state.studioState.audioLessons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.studioState.audioLessons, key = { it.id }) { item ->
                        GlassCard(
                            onClick = {
                                titleInput = item.title
                                selectedSubject = item.subject
                                scriptInput = item.scriptText
                                speakFromChunk(0)
                            },
                            accentBorder = CyberCyanTertiary,
                            cornerRadius = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Headphones, contentDescription = null, tint = CyberCyanTertiary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${item.subject} • ${item.scriptText.take(60)}...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
