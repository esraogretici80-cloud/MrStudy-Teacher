package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.FlashcardItem
import com.example.data.model.PdfResourceRecord
import com.example.data.model.StudyNote
import com.example.data.model.StudyPlanItem
import com.example.data.model.SubjectCatalog
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun StudyHubScreen(
    state: MrStudyUiState,
    onAddPlan: (String, String, String, Long, String, String, String) -> Unit,
    onGenerateAiPlan: (String, String) -> Unit,
    onTogglePlan: (StudyPlanItem) -> Unit,
    onPostponePlan: (StudyPlanItem) -> Unit,
    onDeletePlan: (String) -> Unit,
    onGenerateAiNote: (String, String, PdfResourceRecord?) -> Unit,
    onSaveNote: (StudyNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onExportNotePdf: (StudyNote) -> Unit,
    onGenerateAiFlashcards: (String, String, PdfResourceRecord?) -> Unit,
    onReviewFlashcard: (FlashcardItem, Boolean) -> Unit,
    onDeleteFlashcard: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Çalışma Planı", "Akıllı Notlar", "Bilgi Kartları")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        when (selectedTab) {
            0 -> StudyPlansTab(
                state = state,
                onAddPlan = onAddPlan,
                onGenerateAiPlan = onGenerateAiPlan,
                onTogglePlan = onTogglePlan,
                onPostponePlan = onPostponePlan,
                onDeletePlan = onDeletePlan
            )
            1 -> SmartNotesTab(
                state = state,
                onGenerateAiNote = onGenerateAiNote,
                onSaveNote = onSaveNote,
                onDeleteNote = onDeleteNote,
                onExportNotePdf = onExportNotePdf
            )
            2 -> FlashcardsTab(
                state = state,
                onGenerateAiFlashcards = onGenerateAiFlashcards,
                onReviewFlashcard = onReviewFlashcard,
                onDeleteFlashcard = onDeleteFlashcard
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyPlansTab(
    state: MrStudyUiState,
    onAddPlan: (String, String, String, Long, String, String, String) -> Unit,
    onGenerateAiPlan: (String, String) -> Unit,
    onTogglePlan: (StudyPlanItem) -> Unit,
    onPostponePlan: (StudyPlanItem) -> Unit,
    onDeletePlan: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAiDialog = true },
                    enabled = !state.isGeneratingPlan,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("generate_ai_plan_button")
                ) {
                    if (state.isGeneratingPlan) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Haftalık Plan")
                }
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("add_manual_plan_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manuel Ekle")
                }
            }
        }

        if (state.studyPlans.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Henüz çalışma planın yok", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Eksik konularına ve sınav tarihine göre AI Haftalık Plan oluştur veya kendi ders programını ekle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.studyPlans, key = { it.id }) { plan ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (plan.isCompleted) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onTogglePlan(plan) }) {
                                Icon(
                                    imageVector = if (plan.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Tamamlandı İşaretle",
                                    tint = if (plan.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.topic, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${plan.subject} • ${plan.dayLabel} • ${plan.durationMinutes} dk • ${plan.planType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (plan.examDateText.isNotBlank()) {
                                    Text(
                                        text = "Sınav Tarihi: ${plan.examDateText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            IconButton(onClick = { onDeletePlan(plan.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Planı Sil")
                            }
                        }

                        if (!plan.isCompleted) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Öncelik: ${plan.priority}" + if (plan.postponedCount > 0) " (${plan.postponedCount}x ertelendi)" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (plan.priority == "Yüksek") WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { onPostponePlan(plan) }) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ertele / Yeniden Planla")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var subject by remember { mutableStateOf("Matematik") }
        var topic by remember { mutableStateOf("") }
        var dayLabel by remember { mutableStateOf("Bugün") }
        var durationText by remember { mutableStateOf("45") }
        var priority by remember { mutableStateOf("Orta") }
        var planType by remember { mutableStateOf("Ders") }
        var examDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Çalışma Bloğu") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubjectCatalog.subjects.drop(1).take(6).forEach { s ->
                            FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Konu (örn. İkinci Dereceden Denklemler)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dayLabel,
                        onValueChange = { dayLabel = it },
                        label = { Text("Gün (örn. Bugün, Pazartesi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit) },
                        label = { Text("Süre (dakika)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        label = { Text("Yaklaşan Sınav Tarihi (İsteğe bağlı)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Yüksek", "Orta", "Düşük").forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Ders", "Tekrar", "Ödev").forEach { t ->
                            FilterChip(selected = planType == t, onClick = { planType = t }, label = { Text(t) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (topic.isNotBlank()) {
                        onAddPlan(
                            subject,
                            topic,
                            dayLabel.ifBlank { "Bugün" },
                            (durationText.toLongOrNull() ?: 40L).coerceIn(10L, 240L),
                            priority,
                            planType,
                            examDate
                        )
                        showAddDialog = false
                    }
                }) { Text("Ekle") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showAiDialog) {
        var focusSubject by remember { mutableStateOf("Matematik") }
        var examDate by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI Haftalık Çalışma Planı") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Sınav sonuçlarındaki eksik konuların, sınıf seviyen ve hedef sürene göre haftalık plan oluşturulacak.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubjectCatalog.subjects.drop(1).take(6).forEach { s ->
                            FilterChip(selected = focusSubject == s, onClick = { focusSubject = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        label = { Text("Yaklaşan Sınav Tarihi (örn. 25 Kasım)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onGenerateAiPlan(focusSubject, examDate)
                    showAiDialog = false
                }) { Text("Planı Üret") }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartNotesTab(
    state: MrStudyUiState,
    onGenerateAiNote: (String, String, PdfResourceRecord?) -> Unit,
    onSaveNote: (StudyNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onExportNotePdf: (StudyNote) -> Unit
) {
    var filterSubject by remember { mutableStateOf("Tümü") }
    var editingNote by remember { mutableStateOf<StudyNote?>(null) }
    var showAiNoteDialog by remember { mutableStateOf(false) }

    val filteredNotes = remember(state.notes, filterSubject) {
        if (filterSubject == "Tümü") state.notes else state.notes.filter { it.subject == filterSubject }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAiNoteDialog = true },
                    enabled = !state.isGeneratingNote,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("ai_note_button")
                ) {
                    if (state.isGeneratingNote) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Özet & Formül Çıkar")
                }
                OutlinedButton(
                    onClick = { editingNote = StudyNote(subject = "Matematik") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("new_note_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yeni Not")
                }
            }
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("Tümü") + state.notes.map { it.subject }.distinct()).forEach { subj ->
                    FilterChip(
                        selected = filterSubject == subj,
                        onClick = { filterSubject = subj },
                        label = { Text(subj, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Henüz ders notu yok", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "AI ile otomatik konu özeti, formüller ve tanımlar çıkarabilir veya kendi notunu yazıp PDF olarak dışa aktarabilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredNotes, key = { it.id }) { note ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = note.subject + if (note.sourcePdfTitle.isNotBlank()) " • ${note.sourcePdfTitle}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row {
                                IconButton(onClick = { onExportNotePdf(note) }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Olarak Dışa Aktar")
                                }
                                IconButton(onClick = { editingNote = note }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Notu Düzenle")
                                }
                                IconButton(onClick = { onDeleteNote(note.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Notu Sil")
                                }
                            }
                        }

                        Text(note.content, style = MaterialTheme.typography.bodySmall)

                        if (note.formulas.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Formüller", style = MaterialTheme.typography.labelLarge)
                                    note.formulas.forEach { f ->
                                        Text("• $f", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        if (note.definitions.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Tanımlar", style = MaterialTheme.typography.labelLarge)
                                    note.definitions.forEach { d ->
                                        Text("• $d", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        if (note.keyConcepts.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                note.keyConcepts.forEach { k ->
                                    AssistChip(onClick = {}, label = { Text(k, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingNote?.let { target ->
        var title by remember(target) { mutableStateOf(target.title) }
        var subject by remember(target) { mutableStateOf(target.subject) }
        var content by remember(target) { mutableStateOf(target.content) }
        var formulasText by remember(target) { mutableStateOf(target.formulas.joinToString("\n")) }
        var defsText by remember(target) { mutableStateOf(target.definitions.joinToString("\n")) }

        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = { Text(if (target.id.isBlank()) "Yeni Çalışma Notu" else "Notu Düzenle") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Başlık") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubjectCatalog.subjects.drop(1).take(6).forEach { s ->
                            FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Ders Notu / Özet") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = formulasText,
                        onValueChange = { formulasText = it },
                        label = { Text("Formüller (Her satıra bir formül)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = defsText,
                        onValueChange = { defsText = it },
                        label = { Text("Tanımlar (Her satıra bir tanım)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSaveNote(
                            target.copy(
                                title = title,
                                subject = subject,
                                content = content,
                                formulas = formulasText.lines().map { it.trim() }.filter { it.isNotBlank() },
                                definitions = defsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                            )
                        )
                        editingNote = null
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) { Text("İptal") }
            }
        )
    }

    if (showAiNoteDialog) {
        val activePdf = state.activePdf
        var subject by remember { mutableStateOf("Fizik") }
        var topic by remember { mutableStateOf("") }
        var useActivePdf by remember { mutableStateOf(activePdf != null) }

        AlertDialog(
            onDismissRequest = { showAiNoteDialog = false },
            title = { Text("AI ile Not & Formül Çıkar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubjectCatalog.subjects.drop(1).take(6).forEach { s ->
                            FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Konu (örn. Elektromanyetizma)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (activePdf != null) {
                        FilterChip(
                            selected = useActivePdf,
                            onClick = { useActivePdf = !useActivePdf },
                            label = { Text("Aktif PDF'ten çıkar: ${activePdf.title}") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (topic.isNotBlank() || (useActivePdf && activePdf != null)) {
                        onGenerateAiNote(
                            subject,
                            topic.ifBlank { activePdf?.title ?: subject },
                            if (useActivePdf) activePdf else null
                        )
                        showAiNoteDialog = false
                    }
                }) { Text("Oluştur") }
            },
            dismissButton = {
                TextButton(onClick = { showAiNoteDialog = false }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlashcardsTab(
    state: MrStudyUiState,
    onGenerateAiFlashcards: (String, String, PdfResourceRecord?) -> Unit,
    onReviewFlashcard: (FlashcardItem, Boolean) -> Unit,
    onDeleteFlashcard: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDueOnly by remember { mutableStateOf(true) }
    var flipped by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val deck = remember(state.flashcards, showDueOnly) {
        if (showDueOnly) {
            state.flashcards.filter { it.masteryStatus != "learned" || it.nextReviewAtMs <= now }
        } else {
            state.flashcards
        }
    }
    val activeCard = deck.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showDueOnly,
                        onClick = { showDueOnly = true },
                        label = { Text("Tekrar Zamanı (${state.flashcards.count { it.masteryStatus != "learned" || it.nextReviewAtMs <= now }})") }
                    )
                    FilterChip(
                        selected = !showDueOnly,
                        onClick = { showDueOnly = false },
                        label = { Text("Tümü (${state.flashcards.size})") }
                    )
                }
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    enabled = !state.isGeneratingCards,
                    modifier = Modifier.testTag("generate_flashcards_button")
                ) {
                    if (state.isGeneratingCards) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kart Üret")
                }
            }
        }

        if (activeCard != null) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (flipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 210.dp)
                        .clickable { flipped = !flipped }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = flipped, label = "card_flip") { isBack ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${activeCard.subject} • ${activeCard.cardType} • ${if (isBack) "CEVAP" else "SORU / KAVRAM"}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = if (isBack) activeCard.back else activeCard.front,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "(Çevirmek için karta dokun)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                        onClick = {
                            flipped = false
                            onReviewFlashcard(activeCard, false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tekrar Edilmeli")
                    }
                    Button(
                        onClick = {
                            flipped = false
                            onReviewFlashcard(activeCard, true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Öğrenildi ✓")
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tekrar edilecek kart kalmadı!", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Harika gidiyorsun. Yeni konulardan otomatik Soru-Cevap, Kavram ve Formül kartları üretebilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (state.flashcards.isNotEmpty()) {
            item {
                Text("Destendeki Tüm Kartlar", style = MaterialTheme.typography.titleMedium)
            }
            items(state.flashcards, key = { it.id }) { card ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${card.cardType} • ${card.subject}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(card.front, style = MaterialTheme.typography.titleSmall)
                            Text(card.back, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteFlashcard(card.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Kartı Sil")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        val activePdf = state.activePdf
        var subject by remember { mutableStateOf("Biyoloji") }
        var topic by remember { mutableStateOf("") }
        var useActivePdf by remember { mutableStateOf(activePdf != null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Bilgi Kartları Oluştur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubjectCatalog.subjects.drop(1).take(6).forEach { s ->
                            FilterChip(selected = subject == s, onClick = { subject = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Konu (örn. Hücre Organelleri)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (activePdf != null) {
                        FilterChip(
                            selected = useActivePdf,
                            onClick = { useActivePdf = !useActivePdf },
                            label = { Text("Aktif PDF'ten üret: ${activePdf.title}") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (topic.isNotBlank() || (useActivePdf && activePdf != null)) {
                        onGenerateAiFlashcards(
                            subject,
                            topic.ifBlank { activePdf?.title ?: subject },
                            if (useActivePdf) activePdf else null
                        )
                        showCreateDialog = false
                    }
                }) { Text("Üret") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("İptal") }
            }
        )
    }
}
