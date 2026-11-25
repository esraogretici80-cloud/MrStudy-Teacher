package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.data.model.ErrorCategory
import com.example.data.model.ExamDifficulty
import com.example.data.model.PdfResourceRecord
import com.example.data.model.QuestionType
import com.example.data.model.SubjectCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.ExamUiState
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamCenterScreen(
    state: MrStudyUiState,
    preselectedPdf: PdfResourceRecord?,
    onBack: () -> Unit = {},
    onStartExam: (
        subject: String,
        topic: String,
        difficulty: ExamDifficulty,
        questionCount: Int,
        sourceType: String,
        selectedTypes: List<QuestionType>,
        timedMinutes: Int,
        pdfSource: PdfResourceRecord?,
        focusWrongSummary: String?
    ) -> Unit,
    onAnswerChange: (Int, String) -> Unit,
    onSubmitExam: () -> Unit,
    onResetToConfig: () -> Unit
) {
    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Akıllı Sınav Merkezi",
                subtitle = when (state.examUiState) {
                    is ExamUiState.InProgress -> "Sınav Devam Ediyor"
                    is ExamUiState.Result -> "AI Değerlendirme Raporu"
                    is ExamUiState.Generating -> "Sorular Hazırlanıyor..."
                    is ExamUiState.Grading -> "AI Puanlama Yapılıyor..."
                    else -> "Karma Sınav & Hata Analizi"
                },
                onBack = onBack,
                accentColor = CyberCyanTertiary,
                badgeText = "${state.examHistory.size} Sınav"
            )

            when (val examState = state.examUiState) {
                is ExamUiState.Generating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassCard(accentBorder = CyberCyanTertiary) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = CyberCyanTertiary)
                                Text("Akıllı sınavın hazırlanıyor...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Seviyene ve seçtiğin soru tiplerine uygun sorular oluşturuluyor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is ExamUiState.Grading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GlassCard(accentBorder = NeonPurplePrimary) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = NeonPurplePrimary)
                                Text("AI Öğretmen sınavını değerlendiriyor...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Açık uçlu cevapların inceleniyor ve 5 kategoriye göre hata analizi çıkarılıyor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is ExamUiState.InProgress -> {
                    val exam = examState.exam
                    val examListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                    val flaggedNumbers = remember(exam.title) { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>() }

                    LazyColumn(
                        state = examListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            GlassCard(
                                cornerRadius = 20.dp,
                                accentBorder = NeonPurplePrimary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(exam.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (exam.timedMinutes > 0) {
                                            val mins = examState.remainingSeconds / 60
                                            val secs = examState.remainingSeconds % 60
                                            Surface(
                                                color = CyberCyanTertiary.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, CyberCyanTertiary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Timer, contentDescription = null, tint = CyberCyanTertiary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(String.format("%02d:%02d", mins, secs), fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${exam.subject} • ${exam.difficulty.label} • ${examState.answers.size}/${exam.questions.size} cevaplandı • ${flaggedNumbers.count { it.value }} işaretli",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LinearProgressIndicator(
                                        progress = {
                                            examState.answers.size.toFloat() / exam.questions.size.coerceAtLeast(1)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = CyberCyanTertiary
                                    )

                                    // Question Palette
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        exam.questions.forEachIndexed { idx, qItem ->
                                            val isAnswered = !examState.answers[qItem.number].isNullOrBlank()
                                            val isFlagged = flaggedNumbers[qItem.number] == true
                                            val bg = when {
                                                isFlagged -> WarningAmber.copy(alpha = 0.28f)
                                                isAnswered -> SuccessGreen.copy(alpha = 0.24f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            }
                                            val borderColor = when {
                                                isFlagged -> WarningAmber
                                                isAnswered -> SuccessGreen
                                                else -> ElectricBlueSecondary.copy(alpha = 0.35f)
                                            }
                                            Surface(
                                                color = bg,
                                                border = BorderStroke(1.dp, borderColor),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            examListState.animateScrollToItem(idx + 1)
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = if (isFlagged) "${qItem.number}★" else "${qItem.number}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(exam.questions, key = { it.number }) { q ->
                            val currentAns = examState.answers[q.number].orEmpty()
                            val isFlagged = flaggedNumbers[q.number] == true
                            GlassCard(
                                cornerRadius = 20.dp,
                                accentBorder = if (isFlagged) WarningAmber else ElectricBlueSecondary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Soru ${q.number}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = CyberCyanTertiary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(q.type.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC4B5FD))
                                            FilterChip(
                                                selected = isFlagged,
                                                onClick = { flaggedNumbers[q.number] = !isFlagged },
                                                label = { Text(if (isFlagged) "İşaretli" else "İşaretle", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                    Text(q.questionText, style = MaterialTheme.typography.bodyLarge)

                                    if (q.options.isNotEmpty()) {
                                        q.options.forEach { opt ->
                                            val selected = currentAns == opt
                                            Surface(
                                                color = if (selected) NeonPurplePrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) CyberCyanTertiary else ElectricBlueSecondary.copy(alpha = 0.25f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { onAnswerChange(q.number, opt) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(selected = selected, onClick = { onAnswerChange(q.number, opt) })
                                                    Text(opt, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = currentAns,
                                            onValueChange = { onAnswerChange(q.number, it) },
                                            label = {
                                                Text(
                                                    if (q.type == QuestionType.FILL_BLANK) "Boşluğa gelecek ifade"
                                                    else "Cevabını adım adım yaz"
                                                )
                                            },
                                            minLines = if (q.type == QuestionType.FILL_BLANK) 1 else 3,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onResetToConfig,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                ) {
                                    Text("Vazgeç")
                                }
                                NeonGradientButton(
                                    text = "Sınavı Bitir ve Değerlendir",
                                    onClick = onSubmitExam,
                                    modifier = Modifier
                                        .weight(2f)
                                        .heightIn(min = 50.dp)
                                        .testTag("submit_exam_button")
                                )
                            }
                        }
                    }
                }

                is ExamUiState.Result -> {
                    val summary = examState.summary
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            GlassCard(
                                cornerRadius = 22.dp,
                                accentBorder = if (summary.scorePercent >= 70) SuccessGreen else WarningAmber,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Sınav Başarı Raporu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "%${summary.scorePercent} Başarı (${summary.correctCount}/${summary.totalCount} Doğru)",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (summary.scorePercent >= 70) SuccessGreen else WarningAmber
                                    )
                                    Text(summary.overallReport, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (summary.errorCategories.isNotEmpty()) {
                            item {
                                GlassCard(
                                    cornerRadius = 20.dp,
                                    accentBorder = WarningAmber,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Analytics, contentDescription = null, tint = WarningAmber)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("AI Hata Analizi ve Öğretim Yaklaşımı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        }
                                        summary.errorCategories.forEach { catName ->
                                            val cat = ErrorCategory.entries.firstOrNull { it.title == catName }
                                            Surface(
                                                color = WarningAmber.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("• $catName", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                    if (cat != null) {
                                                        Text(cat.strategy, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (summary.wrongQuestionsSummary.isNotBlank()) {
                            item {
                                NeonGradientButton(
                                    text = "Yanlışlardan Yeni Tekrar Sınavı Oluştur",
                                    icon = Icons.Default.Refresh,
                                    onClick = {
                                        onStartExam(
                                            summary.subject,
                                            "${summary.topic} (Yanlışların Tekrarı)",
                                            ExamDifficulty.MEDIUM,
                                            (summary.totalCount - summary.correctCount).coerceIn(3, 6),
                                            "Tekrar",
                                            listOf(QuestionType.MULTIPLE_CHOICE, QuestionType.OPEN_ENDED),
                                            0,
                                            null,
                                            summary.wrongQuestionsSummary
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("retry_wrong_exam_button")
                                )
                            }
                        }

                        items(summary.results, key = { it.question.number }) { res ->
                            GlassCard(
                                cornerRadius = 18.dp,
                                accentBorder = if (res.isCorrect) SuccessGreen else DangerRed,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (res.isCorrect) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (res.isCorrect) SuccessGreen else DangerRed
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Soru ${res.question.number} • ${if (res.isCorrect) "Doğru" else res.errorCategory ?: "Yanlış"}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(res.question.questionText, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Senin Cevabın: ${res.userAnswer.ifBlank { "(Boş)" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Doğru Cevap: ${res.question.correctAnswer}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen
                                    )
                                    Text(text = res.aiFeedback, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = onResetToConfig,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Yeni Sınav Hazırla")
                            }
                        }
                    }
                }

                is ExamUiState.Config -> {
                    var sourceType by remember(preselectedPdf) {
                        mutableStateOf(if (preselectedPdf != null) "PDF" else "Konu")
                    }
                    var selectedSubject by remember(preselectedPdf) {
                        mutableStateOf(preselectedPdf?.subject ?: "Matematik")
                    }
                    var topicInput by remember(preselectedPdf) {
                        mutableStateOf(preselectedPdf?.title ?: "Temel Kavramlar")
                    }
                    var selectedPdf by remember(preselectedPdf, state.pdfSources) {
                        mutableStateOf(preselectedPdf ?: state.activePdf)
                    }
                    var difficulty by remember { mutableStateOf(ExamDifficulty.MEDIUM) }
                    var questionCount by remember { mutableIntStateOf(5) }
                    var timedMinutes by remember { mutableIntStateOf(0) }
                    val selectedTypes = remember {
                        mutableStateListOf(
                            QuestionType.MULTIPLE_CHOICE,
                            QuestionType.TRUE_FALSE,
                            QuestionType.FILL_BLANK,
                            QuestionType.OPEN_ENDED
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            GlassCard(
                                cornerRadius = 22.dp,
                                accentBorder = CyberCyanTertiary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Sınavını Özelleştir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Konu, ünite veya yüklediğin PDF kaynağından karma tipte sınav oluştur ve AI hata analizi al.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (!state.examError.isNullOrBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = state.examError,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Text("Sınav Kaynağı", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Konu", "Ünite", "PDF").forEach { src ->
                                            FilterChip(
                                                selected = sourceType == src,
                                                onClick = { sourceType = src },
                                                label = { Text(src) }
                                            )
                                        }
                                    }

                                    if (sourceType == "PDF" && state.pdfSources.isNotEmpty()) {
                                        Text("PDF Kaynağı Seç", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            state.pdfSources.forEach { pdf ->
                                                FilterChip(
                                                    selected = selectedPdf?.id == pdf.id,
                                                    onClick = {
                                                        selectedPdf = pdf
                                                        selectedSubject = pdf.subject
                                                        topicInput = pdf.title
                                                    },
                                                    label = { Text(pdf.title) }
                                                )
                                            }
                                        }
                                    }

                                    Text("Ders Seçimi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SubjectCatalog.subjects.drop(1).forEach { subj ->
                                            FilterChip(
                                                selected = selectedSubject == subj,
                                                onClick = { selectedSubject = subj },
                                                label = { Text(subj, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = topicInput,
                                        onValueChange = { topicInput = it },
                                        label = { Text("Konu veya Ünite Adı") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("exam_topic_input")
                                    )

                                    Text("Zorluk Seviyesi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ExamDifficulty.entries.forEach { diff ->
                                            FilterChip(
                                                selected = difficulty == diff,
                                                onClick = { difficulty = diff },
                                                label = { Text(diff.label) }
                                            )
                                        }
                                    }

                                    Text("Soru Tipleri", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        QuestionType.entries.forEach { qType ->
                                            val checked = selectedTypes.contains(qType)
                                            FilterChip(
                                                selected = checked,
                                                onClick = {
                                                    if (checked && selectedTypes.size > 1) selectedTypes.remove(qType)
                                                    else if (!checked) selectedTypes.add(qType)
                                                },
                                                label = { Text(qType.label, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }

                                    Text("Soru Sayısı & Süre", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(3, 5, 8, 10).forEach { cnt ->
                                            FilterChip(
                                                selected = questionCount == cnt,
                                                onClick = { questionCount = cnt },
                                                label = { Text("$cnt Soru") }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0 to "Süresiz", 5 to "5 dk", 10 to "10 dk", 15 to "15 dk").forEach { (mins, lbl) ->
                                            FilterChip(
                                                selected = timedMinutes == mins,
                                                onClick = { timedMinutes = mins },
                                                label = { Text(lbl) }
                                            )
                                        }
                                    }

                                    NeonGradientButton(
                                        text = "Sınavı Oluştur ve Başla",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = {
                                            onStartExam(
                                                selectedSubject,
                                                topicInput,
                                                difficulty,
                                                questionCount,
                                                sourceType,
                                                selectedTypes.toList(),
                                                timedMinutes,
                                                if (sourceType == "PDF") selectedPdf else null,
                                                null
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 52.dp)
                                            .testTag("start_exam_button")
                                    )
                                }
                            }
                        }

                        if (state.examHistory.isNotEmpty()) {
                            item {
                                Text("Geçmiş Sınav Sonuçları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(state.examHistory, key = { it.id }) { rec ->
                                GlassCard(
                                    cornerRadius = 18.dp,
                                    accentBorder = if (rec.scorePercent >= 70) SuccessGreen else WarningAmber,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(rec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Text(
                                                text = "%${rec.scorePercent}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rec.scorePercent >= 70) SuccessGreen else WarningAmber
                                            )
                                        }
                                        Text(
                                            text = "${rec.subject} • ${rec.difficulty} • ${rec.correctCount}/${rec.questionCount} doğru",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (rec.errorCategories.isNotEmpty()) {
                                            Text(
                                                text = "Hata Analizi: ${rec.errorCategories.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarningAmber
                                            )
                                        }
                                        if (rec.wrongQuestionsSummary.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    onStartExam(
                                                        rec.subject,
                                                        "${rec.topic} Tekrar",
                                                        ExamDifficulty.MEDIUM,
                                                        4,
                                                        "Tekrar",
                                                        listOf(QuestionType.MULTIPLE_CHOICE, QuestionType.OPEN_ENDED),
                                                        0,
                                                        null,
                                                        rec.wrongQuestionsSummary
                                                    )
                                                }
                                            ) {
                                                Text("Yanlışlardan Tekrar Sınavı Oluştur")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
