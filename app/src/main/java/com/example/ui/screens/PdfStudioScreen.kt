package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PdfResourceRecord
import com.example.data.model.SubjectCatalog
import com.example.data.model.TeachingMode
import com.example.ui.state.MrStudyUiState
import com.example.util.PdfHelper

private enum class PdfSortOption(val label: String) {
    RECENT("En Yeni"),
    TITLE("Ada Göre"),
    PAGES("Sayfa Sayısı")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfStudioScreen(
    state: MrStudyUiState,
    onImportPdfs: (List<Uri>, String) -> Unit,
    onRelinkPdf: (PdfResourceRecord, Uri) -> Unit,
    onSelectPdfAndPage: (PdfResourceRecord, Int) -> Unit,
    onRenamePdf: (PdfResourceRecord, String, String) -> Unit,
    onDeletePdf: (PdfResourceRecord) -> Unit,
    onAnalyzePdf: (Boolean) -> Unit,
    onTeachPageWithAi: (PdfResourceRecord, Int, TeachingMode, String) -> Unit,
    onStartLiveFromPdf: (PdfResourceRecord, Int) -> Unit,
    onCreateExamFromPdf: (PdfResourceRecord) -> Unit,
    onCreateFlashcardsFromPdf: (PdfResourceRecord) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Dosya Yöneticisi, 1 = Sayfa & AI Stüdyosu
    var selectedUploadSubject by remember { mutableStateOf("Matematik") }
    var showSubjectPicker by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("Tümü") }
    var sortOption by remember { mutableStateOf(PdfSortOption.RECENT) }

    var editingPdf by remember { mutableStateOf<PdfResourceRecord?>(null) }
    var deletingPdf by remember { mutableStateOf<PdfResourceRecord?>(null) }
    var pendingRelinkPdf by remember { mutableStateOf<PdfResourceRecord?>(null) }
    var showJumpPageDialog by remember { mutableStateOf(false) }

    val activePdf = state.activePdf

    val multiPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportPdfs(uris, selectedUploadSubject)
        }
    }

    val singlePdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportPdfs(listOf(uri), selectedUploadSubject)
        }
    }

    val relinkPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = pendingRelinkPdf
        pendingRelinkPdf = null
        if (uri != null && target != null) {
            onRelinkPdf(target, uri)
        }
    }

    LaunchedEffect(state.activePdfId) {
        val active = state.activePdf
        if (active != null && state.renderedPdfPage == null && !state.isPdfBusy && PdfHelper.isLocalPdfAvailable(active.localUriString)) {
            onSelectPdfAndPage(active, active.lastStudiedPage.toInt().coerceAtLeast(1))
        }
    }

    val availableFilterSubjects = remember(state.pdfSources) {
        listOf("Tümü") + state.pdfSources.map { it.subject }.distinct().sorted()
    }

    val filteredPdfs = remember(state.pdfSources, searchQuery, subjectFilter, sortOption) {
        val query = searchQuery.trim().lowercase()
        val matched = state.pdfSources.filter { pdf ->
            val matchesSubject = subjectFilter == "Tümü" || pdf.subject == subjectFilter
            val matchesQuery = query.isBlank() ||
                pdf.title.lowercase().contains(query) ||
                pdf.subject.lowercase().contains(query) ||
                pdf.topics.any { it.lowercase().contains(query) }
            matchesSubject && matchesQuery
        }
        when (sortOption) {
            PdfSortOption.RECENT -> matched
            PdfSortOption.TITLE -> matched.sortedBy { it.title.lowercase() }
            PdfSortOption.PAGES -> matched.sortedByDescending { it.pageCount }
        }
    }

    val totalLibraryPages = remember(state.pdfSources) { state.pdfSources.sumOf { it.pageCount } }
    val totalLibraryBytes = remember(state.pdfSources) {
        state.pdfSources.sumOf { PdfHelper.getLocalPdfSizeBytes(it.localUriString) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero File Selection & Firestore Reference Card
        item {
            PdfPickerHeroCard(
                selectedSubject = selectedUploadSubject,
                showSubjectPicker = showSubjectPicker,
                isBusy = state.isPdfBusy,
                progressText = state.pdfImportProgressText,
                errorText = state.pdfError,
                onToggleSubjectPicker = { showSubjectPicker = it },
                onSelectSubject = {
                    selectedUploadSubject = it
                    showSubjectPicker = false
                },
                onPickMultiple = { multiPdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onPickSingle = { singlePdfPickerLauncher.launch(arrayOf("application/pdf")) }
            )
        }

        // Mode Switcher: Dosya Yöneticisi vs Sayfa & AI Stüdyosu
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_studio_mode_switcher")
            ) {
                SegmentedButton(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("pdf_manager_tab")
                ) {
                    Text("Dosya Yöneticisi (${state.pdfSources.size})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SegmentedButton(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("pdf_reader_tab")
                ) {
                    Text("Sayfa & AI Stüdyosu", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (selectedSubTab == 0) {
            // --- TAB 0: FILE SELECTION & FIRESTORE REFERENCE MANAGER ---
            if (state.pdfSources.isNotEmpty()) {
                item {
                    LibrarySummaryStrip(
                        sourceCount = state.pdfSources.size,
                        totalPages = totalLibraryPages,
                        totalSizeText = PdfHelper.formatFileSize(totalLibraryBytes)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("PDF adı, ders veya konuda ara...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Aramayı Temizle")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_search_input")
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableFilterSubjects) { subj ->
                                FilterChip(
                                    selected = subjectFilter == subj,
                                    onClick = { subjectFilter = subj },
                                    label = { Text(subj) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sırala:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PdfSortOption.entries.forEach { option ->
                                FilterChip(
                                    selected = sortOption == option,
                                    onClick = { sortOption = option },
                                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                if (filteredPdfs.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Eşleşen PDF bulunamadı", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = {
                                    searchQuery = ""
                                    subjectFilter = "Tümü"
                                }) {
                                    Text("Filtreleri Temizle")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPdfs, key = { it.id }) { pdf ->
                        PdfFileReferenceCard(
                            pdf = pdf,
                            isActive = pdf.id == state.activePdfId,
                            onOpenAndStudy = {
                                if (PdfHelper.isLocalPdfAvailable(pdf.localUriString)) {
                                    onSelectPdfAndPage(pdf, pdf.lastStudiedPage.toInt().coerceAtLeast(1))
                                    selectedSubTab = 1
                                } else {
                                    pendingRelinkPdf = pdf
                                    relinkPdfLauncher.launch(arrayOf("application/pdf"))
                                }
                            },
                            onRelink = {
                                pendingRelinkPdf = pdf
                                relinkPdfLauncher.launch(arrayOf("application/pdf"))
                            },
                            onEditReference = { editingPdf = pdf },
                            onRequestDelete = { deletingPdf = pdf }
                        )
                    }
                }
            } else {
                item {
                    EmptyPdfLibraryCard(
                        onPickMultiple = { multiPdfPickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                }
            }
        } else {
            // --- TAB 1: ACTIVE PDF PAGE READER & AI STUDIO ---
            if (state.pdfSources.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.pdfSources, key = { it.id }) { pdf ->
                            FilterChip(
                                selected = pdf.id == state.activePdfId,
                                onClick = { onSelectPdfAndPage(pdf, pdf.lastStudiedPage.toInt().coerceAtLeast(1)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = pdf.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (activePdf != null) {
                val isFileReady = PdfHelper.isLocalPdfAvailable(activePdf.localUriString)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activePdf.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${activePdf.subject} • Firestore Ref: ${activePdf.id.take(8)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AssistChip(
                                    onClick = { showJumpPageDialog = true },
                                    label = { Text("Sayfa ${state.activePdfPage} / ${activePdf.pageCount}") }
                                )
                            }

                            if (!isFileReady) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Bu PDF'in yerel dosya kopyası cihazda bulunamadı.",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Firestore referansın ve özetlerin güvende; dosyayı yeniden seçerek bağlantıyı tazeleyebilirsin.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Button(
                                            onClick = {
                                                pendingRelinkPdf = activePdf
                                                relinkPdfLauncher.launch(arrayOf("application/pdf"))
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(Icons.Default.Link, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PDF Dosyasını Yeniden Bağla")
                                        }
                                    }
                                }
                            } else {
                                // Page Image Canvas
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bmp = state.renderedPdfPage
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "PDF Sayfa ${state.activePdfPage}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text("Sayfa yükleniyor...", color = Color.DarkGray)
                                    }
                                }

                                // Prev / Next Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onSelectPdfAndPage(activePdf, state.activePdfPage - 1) },
                                        enabled = state.activePdfPage > 1 && !state.isPdfBusy,
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                                        Text("Önceki")
                                    }
                                    TextButton(
                                        onClick = { showJumpPageDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Sayfaya Git")
                                    }
                                    OutlinedButton(
                                        onClick = { onSelectPdfAndPage(activePdf, state.activePdfPage + 1) },
                                        enabled = state.activePdfPage < activePdf.pageCount && !state.isPdfBusy,
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Sonraki")
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                    }
                                }

                                // AI Action Buttons Grid
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onTeachPageWithAi(
                                                activePdf,
                                                state.activePdfPage,
                                                TeachingMode.NORMAL,
                                                "${activePdf.title} kaynağının ${state.activePdfPage}. sayfasındaki konuyu yalnızca bu sayfadaki bilgilere öncelik vererek öğretmen gibi anlat."
                                            )
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("teach_pdf_page_button")
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bu Sayfayı Anlat")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            onStartLiveFromPdf(activePdf, state.activePdfPage)
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("live_teach_pdf_page_button")
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Canlı Sesli Anlat")
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            onTeachPageWithAi(
                                                activePdf,
                                                state.activePdfPage,
                                                TeachingMode.SOLVER,
                                                "${activePdf.title} kaynağının ${state.activePdfPage}. sayfasındaki soruları/örnekleri adım adım çöz."
                                            )
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("solve_pdf_page_button")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sayfadaki Soruyu Çöz")
                                    }

                                    FilledTonalButton(
                                        onClick = { onAnalyzePdf(false) },
                                        enabled = !state.isPdfBusy,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("summarize_pdf_button")
                                    ) {
                                        Text("PDF'i Özetle")
                                    }

                                    FilledTonalButton(
                                        onClick = { onAnalyzePdf(true) },
                                        enabled = !state.isPdfBusy,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("note_from_pdf_button")
                                    ) {
                                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Not Çıkar")
                                    }

                                    OutlinedButton(
                                        onClick = { onCreateExamFromPdf(activePdf) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("exam_from_pdf_button")
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PDF'ten Sınav")
                                    }

                                    OutlinedButton(
                                        onClick = { onCreateFlashcardsFromPdf(activePdf) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("flashcards_from_pdf_button")
                                    ) {
                                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bilgi Kartı Üret")
                                    }
                                }
                            }

                            if (activePdf.summary.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("PDF Özeti (Kaynak İçi)", style = MaterialTheme.typography.titleSmall)
                                        Text(activePdf.summary, style = MaterialTheme.typography.bodySmall)
                                        if (activePdf.topics.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                activePdf.topics.forEach { topic ->
                                                    AssistChip(
                                                        onClick = {
                                                            onTeachPageWithAi(
                                                                activePdf,
                                                                state.activePdfPage,
                                                                TeachingMode.NORMAL,
                                                                "'${activePdf.title}' kaynağındaki '$topic' konusunu anlat."
                                                            )
                                                        },
                                                        label = { Text(topic, style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyPdfLibraryCard(
                        onPickMultiple = { multiPdfPickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                }
            }
        }
    }

    // Edit & Firestore File Reference Details Dialog
    editingPdf?.let { pdf ->
        var newTitle by remember(pdf) { mutableStateOf(pdf.title) }
        var newSubject by remember(pdf) { mutableStateOf(pdf.subject) }
        val fileAvailable = remember(pdf.localUriString) { PdfHelper.isLocalPdfAvailable(pdf.localUriString) }
        val fileSizeText = remember(pdf.localUriString) {
            if (fileAvailable) PdfHelper.formatFileSize(PdfHelper.getLocalPdfSizeBytes(pdf.localUriString))
            else "Dosya cihazda bulunamadı"
        }

        AlertDialog(
            onDismissRequest = { editingPdf = null },
            title = { Text("PDF Referansını Yönet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Firestore Belge Yolu",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "users/${pdf.userId}/pdf_sources/${pdf.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Cihaz Referansı (${pdf.pageCount} sayfa • $fileSizeText)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = pdf.localUriString,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Kaynak Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Ders Kategorisi", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubjectCatalog.subjects.take(8).forEach { subj ->
                            FilterChip(
                                selected = newSubject == subj,
                                onClick = { newSubject = subj },
                                label = { Text(subj) }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pendingRelinkPdf = pdf
                            editingPdf = null
                            relinkPdfLauncher.launch(arrayOf("application/pdf"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dosyayı Değiştir / Yeniden Bağla")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRenamePdf(pdf, newTitle, newSubject)
                    editingPdf = null
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { editingPdf = null }) { Text("İptal") }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingPdf?.let { pdf ->
        AlertDialog(
            onDismissRequest = { deletingPdf = null },
            title = { Text("PDF Referansını Sil") },
            text = {
                Text("'${pdf.title}' kaynağının Firestore referansı ve cihazdaki kopyası kalıcı olarak silinecek. Devam edilsin mi?")
            },
            confirmButton = {
                Button(onClick = {
                    onDeletePdf(pdf)
                    deletingPdf = null
                }) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deletingPdf = null }) { Text("Vazgeç") }
            }
        )
    }

    // Jump to Page Dialog
    if (showJumpPageDialog && activePdf != null) {
        var pageInput by remember { mutableStateOf(state.activePdfPage.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpPageDialog = false },
            title = { Text("Sayfa Numarasına Git (1 - ${activePdf.pageCount})") },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Sayfa No") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val target = pageInput.toIntOrNull() ?: state.activePdfPage
                    onSelectPdfAndPage(activePdf, target)
                    showJumpPageDialog = false
                }) { Text("Git") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpPageDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun PdfPickerHeroCard(
    selectedSubject: String,
    showSubjectPicker: Boolean,
    isBusy: Boolean,
    progressText: String?,
    errorText: String?,
    onToggleSubjectPicker: (Boolean) -> Unit,
    onSelectSubject: (String) -> Unit,
    onPickMultiple: () -> Unit,
    onPickSingle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PDF Kaynak Seçici & Yöneticisi",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Cihazından PDF seç, ders etiketi ata ve dosya referanslarını Cloud Firestore'a güvenle kaydet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Firestore Senkron",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Firestore",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { onToggleSubjectPicker(true) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Ders: $selectedSubject")
                        }
                        DropdownMenu(
                            expanded = showSubjectPicker,
                            onDismissRequest = { onToggleSubjectPicker(false) }
                        ) {
                            SubjectCatalog.subjects.forEach { subj ->
                                DropdownMenuItem(
                                    text = { Text(subj) },
                                    onClick = { onSelectSubject(subj) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onPickMultiple,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("upload_multi_pdf_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Çoklu PDF Seç", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OutlinedButton(
                    onClick = onPickSingle,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("upload_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tek PDF Seç ve Firestore'a Kaydet")
                }

                if (isBusy) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        if (!progressText.isNullOrBlank()) {
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (!errorText.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySummaryStrip(
    sourceCount: Int,
    totalPages: Long,
    totalSizeText: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$sourceCount Firestore Kaynağı",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$totalPages Sayfa • $totalSizeText",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PdfFileReferenceCard(
    pdf: PdfResourceRecord,
    isActive: Boolean,
    onOpenAndStudy: () -> Unit,
    onRelink: () -> Unit,
    onEditReference: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val isAvailable = remember(pdf.localUriString) { PdfHelper.isLocalPdfAvailable(pdf.localUriString) }
    val sizeText = remember(pdf.localUriString) {
        if (isAvailable) PdfHelper.formatFileSize(PdfHelper.getLocalPdfSizeBytes(pdf.localUriString))
        else "Dosya eksik"
    }
    val progressFraction = (pdf.lastStudiedPage.toFloat() / pdf.pageCount.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isActive) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_ref_card_${pdf.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdf.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pdf.subject} • Sayfa ${pdf.lastStudiedPage} / ${pdf.pageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Aktif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = if (isAvailable) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isAvailable) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Text(
                            text = if (isAvailable) "Cihazda Hazır • $sizeText" else "Dosya Eksik • Yeniden Bağla",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAvailable) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Firestore Ref: ${pdf.id.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (pdf.summary.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Özet Hazır",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAvailable) {
                    FilledTonalButton(
                        onClick = onOpenAndStudy,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("open_pdf_${pdf.id}")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aç & Çalış")
                    }
                } else {
                    Button(
                        onClick = onRelink,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("relink_pdf_${pdf.id}")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yeniden Bağla")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEditReference,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_pdf_${pdf.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Referansı Düzenle")
                    }
                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_pdf_${pdf.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "PDF Referansını Sil",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPdfLibraryCard(onPickMultiple: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text("Henüz PDF Kaynağı Seçmedin", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Ders kitaplarını, yaprak testleri veya deneme PDF'lerini seçtiğinde dosya referansları otomatik olarak Firestore hesabına kaydedilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onPickMultiple,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("İlk PDF Kaynağını Seç")
            }
        }
    }
}
