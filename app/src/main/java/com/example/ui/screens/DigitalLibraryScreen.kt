package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PdfResourceRecord
import com.example.data.model.PresentationDeck
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen

@Composable
fun DigitalLibraryScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onOpenPdfStudio: (PdfResourceRecord) -> Unit,
    onCreateExamFromPdf: (PdfResourceRecord) -> Unit,
    onCreateCardsFromPdf: (PdfResourceRecord) -> Unit,
    onOpenNoteInStudyHub: () -> Unit,
    onOpenAudioStudio: () -> Unit,
    onOpenPresentation: (PresentationDeck) -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("Tümü") }
    val q = searchQuery.trim().lowercase()

    val filteredPdfs = remember(state.pdfSources, q) {
        state.pdfSources.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) || it.summary.lowercase().contains(q)
        }
    }
    val filteredNotes = remember(state.notes, q) {
        state.notes.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q) || it.content.lowercase().contains(q)
        }
    }
    val filteredPresentations = remember(state.studioState.presentations, q) {
        state.studioState.presentations.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q)
        }
    }
    val filteredAudio = remember(state.studioState.audioLessons, q) {
        state.studioState.audioLessons.filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.subject.lowercase().contains(q)
        }
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Dijital Akademi Kütüphanesi",
                subtitle = "PDF'ler, Özetler, Notlar, Sunumlar & Sesli Dersler",
                onBack = onBack,
                accentColor = ElectricBlueSecondary,
                badgeText = "${state.pdfSources.size + state.notes.size + state.studioState.presentations.size} Kaynak"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Kitap, özet, not veya sunum ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tümü", "PDF & Özetler", "Notlar", "Sunumlar", "Sesli Dersler").forEach { f ->
                            FilterChip(
                                selected = activeFilter == f,
                                onClick = { activeFilter = f },
                                label = { Text(f) }
                            )
                        }
                    }
                }

                if ((activeFilter == "Tümü" || activeFilter == "PDF & Özetler") && filteredPdfs.isNotEmpty()) {
                    item {
                        Text("PDF Kitaplar & Çıkarılan Özetler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(filteredPdfs, key = { "pdf_${it.id}" }) { pdf ->
                        GlassCard(accentBorder = ElectricBlueSecondary, cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CyberCyanTertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(pdf.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("${pdf.pageCount} s.", style = MaterialTheme.typography.labelSmall, color = CyberCyanTertiary)
                                }
                                if (pdf.summary.isNotBlank()) {
                                    Text(pdf.summary, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = { onOpenPdfStudio(pdf) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Oku & Çalış")
                                    }
                                    OutlinedButton(onClick = { onCreateCardsFromPdf(pdf) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kart Üret")
                                    }
                                    OutlinedButton(onClick = { onCreateExamFromPdf(pdf) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sınav Yap")
                                    }
                                }
                            }
                        }
                    }
                }

                if ((activeFilter == "Tümü" || activeFilter == "Notlar") && filteredNotes.isNotEmpty()) {
                    item {
                        Text("Çalışma Notları", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(filteredNotes, key = { "note_${it.id}" }) { note ->
                        GlassCard(onClick = onOpenNoteInStudyHub, accentBorder = NeonPurplePrimary, cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = NeonPurplePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("${note.subject} • ${note.content.take(90)}...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if ((activeFilter == "Tümü" || activeFilter == "Sunumlar") && filteredPresentations.isNotEmpty()) {
                    item {
                        Text("Kayıtlı Sunumlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(filteredPresentations, key = { "pres_${it.id}" }) { deck ->
                        GlassCard(onClick = { onOpenPresentation(deck) }, accentBorder = CyberCyanTertiary, cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Slideshow, contentDescription = null, tint = CyberCyanTertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(deck.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${deck.subject} • ${deck.slides.size} Slayt", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if ((activeFilter == "Tümü" || activeFilter == "Sesli Dersler") && filteredAudio.isNotEmpty()) {
                    item {
                        Text("Sesli Ders Arşivi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(filteredAudio, key = { "aud_${it.id}" }) { item ->
                        GlassCard(onClick = onOpenAudioStudio, accentBorder = SuccessGreen, cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Headphones, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${item.subject} • ${item.scriptText.take(65)}...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
