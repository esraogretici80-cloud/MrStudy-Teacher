package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiToolType
import com.example.data.model.PresentationDeck
import com.example.data.model.PresentationSlide
import com.example.data.model.SubjectCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.util.PdfHelper
import com.example.util.buildTurkishSpeechRecognizerIntent
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiToolsScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onSelectTool: (AiToolType) -> Unit,
    onRunTool: (AiToolType, String, String, String, Bitmap?) -> Unit,
    onSolveMath: (String, Bitmap?) -> Unit,
    onGeneratePresentation: (topic: String, subject: String, slideCount: Int) -> Unit,
    onSavePresentation: (PresentationDeck) -> Unit,
    onExportPresentationPdf: (PresentationDeck) -> Unit,
    onSaveResultAsNote: (subject: String, title: String, content: String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("Tümü") }
    var selectedSubject by remember { mutableStateOf(state.selectedSubject) }
    var inputText by remember(state.activeToolType) { mutableStateOf("") }
    var extraOption by remember(state.activeToolType) { mutableStateOf("Orta") }
    var slideCount by remember { mutableIntStateOf(5) }
    var attachedBitmap by remember(state.activeToolType) { mutableStateOf<Bitmap?>(null) }
    var copiedNotice by remember { mutableStateOf(false) }

    val activeTool = state.activeToolType
    val categories = remember { listOf("Tümü") + AiToolType.entries.map { it.category }.distinct() }
    val visibleTools = remember(selectedCategory) {
        if (selectedCategory == "Tümü") AiToolType.entries
        else AiToolType.entries.filter { it.category == selectedCategory }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { PdfHelper.decodeScaledBitmap(context, uri, 1280) }
                    .onSuccess { attachedBitmap = it }
            }
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

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "12 Yapay Zekâ Eğitim Aracı",
                subtitle = "${activeTool.title} • ${activeTool.category}",
                onBack = onBack,
                accentColor = NeonPurplePrimary,
                badgeText = "Gemini AI"
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            visibleTools.forEach { tool ->
                                val selected = activeTool == tool
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectTool(tool) },
                                    label = { Text(tool.title, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("ai_tool_chip_${tool.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                item {
                    GlassCard(
                        cornerRadius = 22.dp,
                        accentBorder = CyberCyanTertiary,
                        secondaryBorder = NeonPurplePrimary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(activeTool.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(activeTool.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SubjectCatalog.subjects.forEach { subj ->
                                    FilterChip(
                                        selected = selectedSubject == subj,
                                        onClick = { selectedSubject = subj },
                                        label = { Text(subj, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            if (activeTool == AiToolType.PRESENTATION_STUDIO) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Slayt Sayısı:", style = MaterialTheme.typography.labelMedium)
                                    listOf(4, 6, 8).forEach { count ->
                                        FilterChip(
                                            selected = slideCount == count,
                                            onClick = { slideCount = count },
                                            label = { Text("$count Slayt") }
                                        )
                                    }
                                }
                            } else if (activeTool == AiToolType.TRANSLATOR) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("İngilizce -> Türkçe", "Türkçe -> İngilizce", "Akademik Sadeleştirme").forEach { dir ->
                                        FilterChip(
                                            selected = extraOption == dir,
                                            onClick = { extraOption = dir },
                                            label = { Text(dir, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Temel", "Orta", "İleri / YKS").forEach { lvl ->
                                        FilterChip(
                                            selected = extraOption == lvl,
                                            onClick = { extraOption = lvl },
                                            label = { Text(lvl, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = { Text(activeTool.inputHint) },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_tool_input")
                            )

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeTool == AiToolType.MATH_SOLVER || activeTool == AiToolType.HOMEWORK_HELPER) {
                                    OutlinedButton(
                                        onClick = {
                                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Fotoğraf Ekle")
                                    }
                                }

                                if (activeTool == AiToolType.VOICE_NOTE || activeTool == AiToolType.HOMEWORK_HELPER) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching { speechLauncher.launch(buildTurkishSpeechRecognizerIntent()) }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Mikrofonla Söyle")
                                    }
                                }
                            }

                            attachedBitmap?.let { bmp ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Eklenen soru görseli",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Text("Görsel eklendi", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                                    IconButton(onClick = { attachedBitmap = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Görseli Kaldır")
                                    }
                                }
                            }

                            val busy = state.isRunningAiTool || state.isSolvingMath || state.isGeneratingPresentation
                            NeonGradientButton(
                                text = when {
                                    busy -> "Hazırlanıyor..."
                                    activeTool == AiToolType.MATH_SOLVER -> "Adım Adım Çöz"
                                    activeTool == AiToolType.PRESENTATION_STUDIO -> "Sunum Oluştur"
                                    else -> "${activeTool.title} Çalıştır"
                                },
                                icon = Icons.Default.AutoAwesome,
                                enabled = !busy && (inputText.isNotBlank() || attachedBitmap != null || activeTool == AiToolType.WEAKNESS_ANALYZER),
                                onClick = {
                                    copiedNotice = false
                                    when (activeTool) {
                                        AiToolType.MATH_SOLVER -> onSolveMath(inputText, attachedBitmap)
                                        AiToolType.PRESENTATION_STUDIO -> onGeneratePresentation(inputText, selectedSubject, slideCount)
                                        else -> onRunTool(activeTool, inputText, selectedSubject, extraOption, attachedBitmap)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_ai_tool_button")
                            )
                        }
                    }
                }

                // Specialized Math Solver Structured Result
                val mathRes = if (activeTool == AiToolType.MATH_SOLVER) state.mathSolutionResult else null
                if (mathRes != null) {
                    item {
                        GlassCard(accentBorder = SuccessGreen, cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Adım Adım Matematik Çözümü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                if (mathRes.detectedProblem.isNotBlank()) {
                                    Text("Soru: ${mathRes.detectedProblem}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Sonuç: ${mathRes.finalAnswer}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SuccessGreen,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Text("Çözüm Adımları:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                mathRes.steps.forEachIndexed { idx, step ->
                                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (mathRes.formulasUsed.isNotEmpty()) {
                                    Text("Kullanılan Kurallar / Formüller:", style = MaterialTheme.typography.labelLarge, color = Color(0xFFC4B5FD))
                                    mathRes.formulasUsed.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                }
                                if (mathRes.similarPracticeQuestion.isNotBlank()) {
                                    Surface(
                                        color = ElectricBlueSecondary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Benzer Pratik Soru", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                            Text(mathRes.similarPracticeQuestion, style = MaterialTheme.typography.bodySmall)
                                            Text("Cevap: ${mathRes.similarPracticeAnswer}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        }
                                    }
                                }
                                val combined = buildString {
                                    appendLine("Soru: ${mathRes.detectedProblem}")
                                    appendLine("Sonuç: ${mathRes.finalAnswer}")
                                    mathRes.steps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
                                }
                                ToolResultActionRow(
                                    copied = copiedNotice,
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(combined))
                                        copiedNotice = true
                                    },
                                    onSaveAsNote = {
                                        onSaveResultAsNote(selectedSubject, "Matematik Çözümü", combined)
                                    }
                                )
                            }
                        }
                    }
                }

                // Specialized Presentation Studio Deck Editor + PDF Export
                val deck = if (activeTool == AiToolType.PRESENTATION_STUDIO) state.activePresentation else null
                if (deck != null) {
                    item {
                        PresentationDeckEditorCard(
                            deck = deck,
                            onSaveDeck = onSavePresentation,
                            onExportPdf = { onExportPresentationPdf(it) }
                        )
                    }
                }

                // Generic Rich AI Result
                val toolOutput = state.activeToolResult
                if (!toolOutput.isNullOrBlank()) {
                    item {
                        GlassCard(accentBorder = ElectricBlueSecondary, cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("${activeTool.title} Çıktısı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                Text(toolOutput, style = MaterialTheme.typography.bodyMedium)
                                ToolResultActionRow(
                                    copied = copiedNotice,
                                    onCopy = {
                                        clipboard.setText(AnnotatedString(toolOutput))
                                        copiedNotice = true
                                    },
                                    onSaveAsNote = {
                                        onSaveResultAsNote(
                                            selectedSubject,
                                            "${activeTool.title}: ${inputText.take(28).ifBlank { selectedSubject }}",
                                            toolOutput
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Voice notes archive quick view when Voice Note tool is open
                if (activeTool == AiToolType.VOICE_NOTE && state.studioState.voiceNotes.isNotEmpty()) {
                    item {
                        Text("Kayıtlı Sesli Notlar (${state.studioState.voiceNotes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(state.studioState.voiceNotes, key = { it.id }) { vn ->
                        GlassCard(accentBorder = CyberCyanTertiary, cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(vn.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(vn.cleanedSummary, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolResultActionRow(
    copied: Boolean,
    onCopy: () -> Unit,
    onSaveAsNote: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCopy, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(
                if (copied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copied) "Kopyalandı" else "Kopyala")
        }
        Button(onClick = onSaveAsNote, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Notlara Kaydet")
        }
    }
}

@Composable
private fun PresentationDeckEditorCard(
    deck: PresentationDeck,
    onSaveDeck: (PresentationDeck) -> Unit,
    onExportPdf: (PresentationDeck) -> Unit
) {
    var editableDeck by remember(deck) { mutableStateOf(deck) }
    var activeSlideIdx by remember(deck.id) { mutableIntStateOf(0) }
    val safeIdx = activeSlideIdx.coerceIn(0, (editableDeck.slides.size - 1).coerceAtLeast(0))
    val slide = editableDeck.slides.getOrNull(safeIdx)

    GlassCard(accentBorder = NeonPurplePrimary, cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(editableDeck.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${editableDeck.slides.size} Slayt • ${editableDeck.subject}", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                }
                OutlinedButton(
                    onClick = {
                        val added = PresentationSlide(title = "Yeni Slayt ${editableDeck.slides.size + 1}", bullets = listOf("Ana fikir 1", "Örnek açıklama"))
                        editableDeck = editableDeck.copy(slides = editableDeck.slides + added)
                        activeSlideIdx = editableDeck.slides.lastIndex
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Slayt Ekle")
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                editableDeck.slides.forEachIndexed { i, _ ->
                    FilterChip(
                        selected = i == safeIdx,
                        onClick = { activeSlideIdx = i },
                        label = { Text("Slayt ${i + 1}") }
                    )
                }
            }

            if (slide != null) {
                OutlinedTextField(
                    value = slide.title,
                    onValueChange = { newTitle ->
                        val updated = editableDeck.slides.toMutableList()
                        updated[safeIdx] = slide.copy(title = newTitle)
                        editableDeck = editableDeck.copy(slides = updated)
                    },
                    label = { Text("Slayt Başlığı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = slide.bullets.joinToString("\n"),
                    onValueChange = { raw ->
                        val bullets = raw.lines().filter { it.isNotBlank() }
                        val updated = editableDeck.slides.toMutableList()
                        updated[safeIdx] = slide.copy(bullets = bullets)
                        editableDeck = editableDeck.copy(slides = updated)
                    },
                    label = { Text("Maddeler (her satır bir madde)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSaveDeck(editableDeck) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kaydet")
                }
                NeonGradientButton(
                    text = "PDF Dışa Aktar",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = { onExportPdf(editableDeck) },
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}
