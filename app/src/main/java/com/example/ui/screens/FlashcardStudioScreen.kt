package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.FlashcardItem
import com.example.data.model.FlashcardReviewRating
import com.example.data.model.PdfResourceRecord
import com.example.data.model.SubjectCatalog
import com.example.ui.components.GlassCard
import com.example.ui.components.MrStudy3Backdrop
import com.example.ui.components.NeonGradientButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.state.MrStudyUiState
import com.example.ui.theme.CyberCyanTertiary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricBlueSecondary
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlashcardStudioScreen(
    state: MrStudyUiState,
    onBack: () -> Unit,
    onAddManualCard: (subject: String, cardType: String, front: String, back: String) -> Unit,
    onEditCard: (FlashcardItem) -> Unit,
    onDuplicateCard: (FlashcardItem) -> Unit,
    onDeleteCard: (String) -> Unit,
    onReviewTiered: (FlashcardItem, FlashcardReviewRating) -> Unit,
    onGenerateAiCards: (subject: String, topic: String, pdf: PdfResourceRecord?) -> Unit
) {
    BackHandler { onBack() }

    var selectedSubjectFilter by remember { mutableStateOf("Tümü") }
    var isStudyMode by remember { mutableStateOf(false) }
    var studyIdx by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<FlashcardItem?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }

    val availableDecks = remember(state.flashcards) {
        listOf("Tümü") + state.flashcards.map { it.subject }.distinct().sorted()
    }
    val filteredCards = remember(state.flashcards, selectedSubjectFilter) {
        if (selectedSubjectFilter == "Tümü") state.flashcards
        else state.flashcards.filter { it.subject == selectedSubjectFilter }
    }

    MrStudy3Backdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "Flash Kart Stüdyosu",
                subtitle = "Desteler, Düzenleme & Aralıklı Tekrar",
                onBack = onBack,
                accentColor = CyberCyanTertiary,
                badgeText = "${state.flashcards.size} Kart"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(accentBorder = NeonPurplePrimary, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeonGradientButton(
                                    text = "Yeni Kart",
                                    icon = Icons.Default.Add,
                                    onClick = { showCreateDialog = true },
                                    modifier = Modifier.weight(1f).testTag("flashcard_add_btn")
                                )
                                OutlinedButton(
                                    onClick = { showAiDialog = true },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("flashcard_ai_btn")
                                ) {
                                    if (state.isGeneratingCards) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("AI / PDF Üret")
                                }
                            }
                            if (filteredCards.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        isStudyMode = !isStudyMode
                                        studyIdx = 0
                                        isFlipped = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Style, contentDescription = null, tint = CyberCyanTertiary)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isStudyMode) "Deste Listesine Dön" else "Tekrar Oturumu Başlat (${filteredCards.size} Kart)")
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableDecks.forEach { deck ->
                            FilterChip(
                                selected = selectedSubjectFilter == deck,
                                onClick = { selectedSubjectFilter = deck; studyIdx = 0; isFlipped = false },
                                label = { Text(deck) }
                            )
                        }
                    }
                }

                if (isStudyMode && filteredCards.isNotEmpty()) {
                    item {
                        val card = filteredCards[studyIdx.coerceIn(0, filteredCards.lastIndex)]
                        val rotY by animateFloatAsState(
                            targetValue = if (isFlipped) 180f else 0f,
                            animationSpec = tween(380),
                            label = "card_flip"
                        )
                        GlassCard(
                            accentBorder = if (isFlipped) CyberCyanTertiary else NeonPurplePrimary,
                            onClick = { isFlipped = !isFlipped },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 210.dp)
                                .graphicsLayer {
                                    rotationY = rotY
                                    cameraDistance = 12f * density
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp)
                                    .graphicsLayer { rotationY = if (rotY > 90f) 180f else 0f },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${card.subject} • ${card.cardType} • Kart ${studyIdx + 1}/${filteredCards.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyanTertiary
                                )
                                Text(
                                    text = if (!isFlipped) card.front else card.back,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Flip, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (!isFlipped) "Cevabı görmek için karta dokun" else "Aşağıdan öğrenme durumunu seç",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlashcardReviewRating.entries.forEach { rating ->
                                val tint = when (rating) {
                                    FlashcardReviewRating.HARD -> DangerRed
                                    FlashcardReviewRating.REVIEW -> WarningAmber
                                    FlashcardReviewRating.GOOD -> ElectricBlueSecondary
                                    FlashcardReviewRating.MASTERED -> SuccessGreen
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = tint.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, tint),
                                    modifier = Modifier.clickable {
                                        onReviewTiered(card, rating)
                                        isFlipped = false
                                        studyIdx = (studyIdx + 1) % filteredCards.size
                                    }
                                ) {
                                    Text(
                                        text = rating.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (filteredCards.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Henüz Bilgi Kartı Yok", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                    Text(
                                        "Manuel kart ekleyebilir veya konudan/PDF'ten otomatik deste oluşturabilirsin.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredCards, key = { it.id }) { card ->
                            val customRating = state.studioState.flashcardRatings[card.id]?.label
                                ?: when (card.masteryStatus) {
                                    "learned" -> "Öğrenildi"
                                    "review" -> "Tekrar Edilmeli"
                                    else -> "Yeni"
                                }
                            GlassCard(accentBorder = ElectricBlueSecondary, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${card.subject} • ${card.cardType} • $customRating",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberCyanTertiary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(onClick = { editingCard = card }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { onDuplicateCard(card) }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = CyberCyanTertiary, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { onDeleteCard(card.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = DangerRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    Text(card.front, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(card.back, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingCard != null) {
        val target = editingCard
        var subj by remember(target) { mutableStateOf(target?.subject ?: state.selectedSubject) }
        var cardType by remember(target) { mutableStateOf(target?.cardType ?: "Kavram") }
        var front by remember(target) { mutableStateOf(target?.front.orEmpty()) }
        var back by remember(target) { mutableStateOf(target?.back.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false; editingCard = null },
            title = { Text(if (target == null) "Yeni Bilgi Kartı" else "Kartı Düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subj, onValueChange = { subj = it }, label = { Text("Ders") }, singleLine = true)
                    OutlinedTextField(value = cardType, onValueChange = { cardType = it }, label = { Text("Kart Türü (Kavram/Formül/Tarih)") }, singleLine = true)
                    OutlinedTextField(value = front, onValueChange = { front = it }, label = { Text("Ön Yüz (Soru / Terim)") }, minLines = 2)
                    OutlinedTextField(value = back, onValueChange = { back = it }, label = { Text("Arka Yüz (Cevap / Açıklama)") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (target == null) {
                            onAddManualCard(subj, cardType, front, back)
                        } else {
                            onEditCard(target.copy(subject = subj, cardType = cardType, front = front, back = back))
                        }
                        showCreateDialog = false
                        editingCard = null
                    },
                    enabled = front.isNotBlank() && back.isNotBlank()
                ) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false; editingCard = null }) { Text("İptal") } }
        )
    }

    if (showAiDialog) {
        var aiSubject by remember { mutableStateOf(state.selectedSubject) }
        var aiTopic by remember { mutableStateOf("") }
        var selectedPdf by remember { mutableStateOf<PdfResourceRecord?>(state.activePdf) }

        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI ile Kart Destesi Üret") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubjectCatalog.subjects.take(6).forEach { s ->
                            FilterChip(selected = aiSubject == s, onClick = { aiSubject = s }, label = { Text(s) })
                        }
                    }
                    OutlinedTextField(value = aiTopic, onValueChange = { aiTopic = it }, label = { Text("Konu") })
                    if (state.pdfSources.isNotEmpty()) {
                        FilterChip(
                            selected = selectedPdf != null,
                            onClick = { selectedPdf = if (selectedPdf == null) state.pdfSources.first() else null },
                            label = { Text(selectedPdf?.title?.let { "PDF: $it" } ?: "PDF Kaynağını Bağla") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onGenerateAiCards(aiSubject, aiTopic.ifBlank { aiSubject }, selectedPdf)
                    showAiDialog = false
                }) { Text("Üret") }
            },
            dismissButton = { TextButton(onClick = { showAiDialog = false }) { Text("İptal") } }
        )
    }
}
