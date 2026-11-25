package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import com.example.core.friendlyFirestoreErrorMessage
import com.example.data.ai.MrStudyAiService
import com.example.data.ai.analyzeOrDryRunCode
import com.example.data.ai.explainDiagramNode
import com.example.data.ai.generateCustom3dGameContent
import com.example.data.ai.generateDiagramProject
import com.example.data.ai.generatePresentationDeck
import com.example.data.ai.runEducationalAiTool
import com.example.data.ai.solveMathProblemStructured
import com.example.data.local.LocalStudyMetricsStore
import com.example.data.local.MrStudy4LocalStore
import com.example.data.model.AiToolType
import com.example.data.model.AudioLessonItem
import com.example.data.model.CodeProjectFile
import com.example.data.model.CodingLanguage
import com.example.data.model.Custom3dGameConfig
import com.example.data.model.DiagramProject
import com.example.data.model.DiagramType
import com.example.data.model.FlashcardItem
import com.example.data.model.FlashcardReviewRating
import com.example.data.model.GameGenre
import com.example.data.model.PresentationDeck
import com.example.data.model.SavedMathBoard
import com.example.data.model.VoiceNoteRecord
import com.example.data.repository.MrStudyRepository
import com.example.ui.state.MrStudyUiState
import com.example.util.PdfHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MrStudy4ViewModelController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<MrStudyUiState>,
    private val repository: MrStudyRepository,
    private val aiService: MrStudyAiService,
    private val currentUserId: String,
    private val studioStore: MrStudy4LocalStore?,
    private val metricsStore: LocalStudyMetricsStore?,
    private val onSyncWidgets: () -> Unit
) {

    fun setLowPower3dMode(enabled: Boolean) {
        studioStore?.setLowPower3dMode(enabled)
        uiState.update {
            it.copy(
                statusBanner = if (enabled) {
                    "Düşük Güç / 2D Basitleştirilmiş Mod aktif edildi."
                } else {
                    "Tam 3B Etkileşimli Görsel Mod aktif edildi."
                }
            )
        }
    }

    fun markAllNotificationsRead() {
        studioStore?.markAllNotificationsRead()
    }

    fun setExamCountdown(label: String, targetEpochMs: Long) {
        studioStore?.setExamCountdown(label, targetEpochMs)
        onSyncWidgets()
        uiState.update { it.copy(statusBanner = "Sınav geri sayımı güncellendi.") }
    }

    // --- Flashcards 2.0 ---
    fun addManualFlashcard(subject: String, cardType: String, front: String, back: String) {
        if (front.isBlank() || back.isBlank()) return
        scope.launch {
            try {
                repository.addFlashcard(
                    currentUserId,
                    FlashcardItem(
                        subject = subject.ifBlank { "Genel" },
                        cardType = cardType.ifBlank { "Kavram" },
                        front = front.trim(),
                        back = back.trim(),
                        masteryStatus = "new"
                    )
                )
                uiState.update { it.copy(statusBanner = "Yeni bilgi kartı oluşturuldu.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun editFlashcard(card: FlashcardItem) {
        if (card.id.isBlank()) return
        scope.launch {
            try {
                repository.updateFlashcardFull(currentUserId, card)
                uiState.update { it.copy(statusBanner = "Bilgi kartı güncellendi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun duplicateFlashcard(card: FlashcardItem) {
        scope.launch {
            try {
                repository.addFlashcard(
                    currentUserId,
                    card.copy(id = "", front = "${card.front} (Kopya)", masteryStatus = "new", reviewCount = 0L)
                )
                uiState.update { it.copy(statusBanner = "Kart kopyalandı.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun reviewFlashcardTiered(card: FlashcardItem, rating: FlashcardReviewRating) {
        studioStore?.setFlashcardRating(card.id, rating)
        scope.launch {
            try {
                repository.updateFlashcardTieredReview(
                    userId = currentUserId,
                    card = card,
                    status = rating.firestoreStatus,
                    intervalDays = rating.intervalDays
                )
                val xp = when (rating) {
                    FlashcardReviewRating.MASTERED -> 20L
                    FlashcardReviewRating.GOOD -> 15L
                    FlashcardReviewRating.REVIEW -> 8L
                    FlashcardReviewRating.HARD -> 5L
                }
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = uiState.value.profile,
                    xpDelta = xp
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    // --- 3B Game Studio ---
    fun saveManualCustomGame(game: Custom3dGameConfig) {
        studioStore?.saveCustomGame(game)
        studioStore?.addNotification("Yeni Oyun Kaydedildi", "${game.title} (${game.genre.label})", "Oyun Stüdyosu")
        uiState.update { it.copy(statusBanner = "'${game.title}' oyun kütüphanene kaydedildi.") }
    }

    fun generateAiCustomGame(
        genre: GameGenre,
        heroName: String,
        environmentName: String,
        topic: String,
        ruleSummary: String,
        subject: String
    ) {
        scope.launch {
            uiState.update { it.copy(isGeneratingCustomGame = true) }
            try {
                val created = aiService.generateCustom3dGameContent(
                    genre = genre,
                    heroName = heroName.ifBlank { genre.defaultHero },
                    environmentName = environmentName.ifBlank { genre.defaultEnvironment },
                    topic = topic.ifBlank { subject },
                    ruleSummary = ruleSummary.ifBlank { genre.subtitle },
                    subject = subject,
                    gradeLevel = uiState.value.profile.gradeLevel
                )
                studioStore?.saveCustomGame(created)
                studioStore?.addNotification("AI Oyun Üretildi", "${created.title} oynanmaya hazır!", "3.6 Flash")
                uiState.update { it.copy(statusBanner = "'${created.title}' 3B oyunu AI ile üretildi!") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Oyun üretilemedi: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isGeneratingCustomGame = false) }
            }
        }
    }

    fun deleteCustomGame(id: String) {
        studioStore?.deleteCustomGame(id)
    }

    fun recordCustomGameCompletion(gameId: String, score: Int, correct: Int, total: Int) {
        val current = uiState.value.studioState.customGames.firstOrNull { it.id == gameId }
        if (current != null && score > current.highScore) {
            studioStore?.saveCustomGame(current.copy(highScore = score))
        }
        val subject = current?.subject ?: uiState.value.selectedSubject
        metricsStore?.record3dGameResult(subject, correct, total, score)
        scope.launch {
            runCatching {
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = uiState.value.profile,
                    xpDelta = (correct * 15L + 10L),
                    minutesDelta = 2L,
                    questionsDelta = total.toLong()
                )
            }
        }
    }

    // --- Smart Diagram Board ---
    fun generateAiDiagram(topic: String, diagramType: DiagramType, subject: String) {
        scope.launch {
            uiState.update { it.copy(isGeneratingDiagram = true, diagramNodeExplanation = null) }
            try {
                val diagram = aiService.generateDiagramProject(
                    topic = topic.ifBlank { subject },
                    diagramType = diagramType,
                    subject = subject,
                    gradeLevel = uiState.value.profile.gradeLevel
                )
                studioStore?.saveDiagram(diagram)
                uiState.update {
                    it.copy(
                        activeDiagram = diagram,
                        statusBanner = "'${diagram.title}' diyagramı oluşturuldu."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Diyagram oluşturulamadı: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isGeneratingDiagram = false) }
            }
        }
    }

    fun selectOrSaveDiagram(diagram: DiagramProject) {
        studioStore?.saveDiagram(diagram.copy(updatedAtMs = System.currentTimeMillis()))
        uiState.update { it.copy(activeDiagram = diagram) }
    }

    fun deleteDiagram(id: String) {
        studioStore?.deleteDiagram(id)
        if (uiState.value.activeDiagram?.id == id) {
            uiState.update { it.copy(activeDiagram = null) }
        }
    }

    fun explainDiagramNode(nodeLabel: String, diagramTopic: String, subject: String, actionType: String) {
        scope.launch {
            uiState.update { it.copy(isExplainingDiagramNode = true) }
            try {
                val text = aiService.explainDiagramNode(
                    nodeLabel = nodeLabel,
                    diagramTopic = diagramTopic,
                    subject = subject,
                    gradeLevel = uiState.value.profile.gradeLevel,
                    actionType = actionType
                )
                uiState.update { it.copy(diagramNodeExplanation = text) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(diagramNodeExplanation = "Açıklama alınamadı: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isExplainingDiagramNode = false) }
            }
        }
    }

    fun clearDiagramNodeExplanation() {
        uiState.update { it.copy(diagramNodeExplanation = null) }
    }

    fun exportDiagramFile(context: Context, diagram: DiagramProject, bitmap: Bitmap?, format: String) {
        scope.launch {
            try {
                when (format) {
                    "JSON" -> {
                        val json = studioStore?.serializeDiagramToJson(diagram) ?: "{}"
                        val file = PdfHelper.exportTextFile(context, "${diagram.title.take(24).replace(' ', '_')}.json", json)
                        PdfHelper.shareFile(context, file, "application/json", "Diyagram JSON Paylaş")
                    }
                    "PNG" -> {
                        val bmp = bitmap ?: return@launch
                        val file = PdfHelper.exportBitmapAsPng(context, bmp, diagram.title)
                        PdfHelper.shareFile(context, file, "image/png", "Diyagram Görseli Paylaş")
                    }
                    else -> {
                        val bmp = bitmap ?: return@launch
                        val file = PdfHelper.exportBitmapAsPdf(context, bmp, diagram.title)
                        PdfHelper.sharePdfFile(context, file)
                    }
                }
                uiState.update { it.copy(statusBanner = "Diyagram $format olarak dışa aktarıldı.") }
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Dışa aktarma hatası: ${e.localizedMessage}") }
            }
        }
    }

    // --- 3B Science Lab Quiz ---
    fun awardScienceLabQuiz(subject: String, correctCount: Int, totalCount: Int) {
        metricsStore?.recordQuestionSolved(subject, totalCount)
        scope.launch {
            runCatching {
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = uiState.value.profile,
                    xpDelta = (correctCount * 15L + 10L),
                    minutesDelta = 2L,
                    questionsDelta = totalCount.toLong(),
                    newBadge = if (correctCount == totalCount) "3B Bilim Kaşifi" else null
                )
            }
        }
    }

    // --- 12 AI Educational Tools ---
    fun selectAiTool(toolType: AiToolType) {
        uiState.update { it.copy(activeToolType = toolType, activeToolResult = null) }
    }

    fun solveMathProblem(problemText: String, imageBitmap: Bitmap?) {
        scope.launch {
            uiState.update { it.copy(isSolvingMath = true, mathSolutionResult = null) }
            try {
                val res = aiService.solveMathProblemStructured(
                    problemText = problemText,
                    imageBitmap = imageBitmap,
                    gradeLevel = uiState.value.profile.gradeLevel
                )
                metricsStore?.recordQuestionSolved("Matematik", 1)
                uiState.update { it.copy(mathSolutionResult = res) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Matematik çözücü hatası: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isSolvingMath = false) }
            }
        }
    }

    fun generatePresentation(topic: String, subject: String, slideCount: Int) {
        scope.launch {
            uiState.update { it.copy(isGeneratingPresentation = true) }
            try {
                val deck = aiService.generatePresentationDeck(
                    topic = topic.ifBlank { subject },
                    subject = subject,
                    slideCount = slideCount.coerceIn(3, 10),
                    gradeLevel = uiState.value.profile.gradeLevel
                )
                studioStore?.savePresentation(deck)
                uiState.update { it.copy(activePresentation = deck, statusBanner = "Sunum hazırlandı.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Sunum oluşturulamadı: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isGeneratingPresentation = false) }
            }
        }
    }

    fun savePresentation(deck: PresentationDeck) {
        studioStore?.savePresentation(deck)
        uiState.update { it.copy(activePresentation = deck, statusBanner = "Sunum kaydedildi.") }
    }

    fun openPresentation(deck: PresentationDeck) {
        uiState.update {
            it.copy(
                activeToolType = AiToolType.PRESENTATION_STUDIO,
                activePresentation = deck
            )
        }
    }

    fun exportPresentationPdf(context: Context, deck: PresentationDeck) {
        scope.launch {
            try {
                val file = PdfHelper.exportPresentationPdf(
                    context = context,
                    title = deck.title,
                    subject = deck.subject,
                    slides = deck.slides.map { it.title to it.bullets }
                )
                PdfHelper.sharePdfFile(context, file)
                uiState.update { it.copy(statusBanner = "Sunum PDF olarak dışa aktarıldı.") }
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Sunum PDF hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun runAiTool(toolType: AiToolType, input: String, subject: String, extraOption: String, bitmap: Bitmap?) {
        val enrichedInput = if (toolType == AiToolType.WEAKNESS_ANALYZER) {
            val examsSummary = uiState.value.examHistory.take(8).joinToString("\n") {
                "${it.subject} (${it.topic}): %${it.scorePercent}, zayıf=${it.weakTopics.joinToString()}"
            }.ifBlank { "Henüz kayıtlı sınav yok." }
            "Sınav Geçmişi:\n$examsSummary\nÖğrenci Notu: $input"
        } else input

        scope.launch {
            uiState.update { it.copy(isRunningAiTool = true, activeToolResult = null) }
            try {
                val output = aiService.runEducationalAiTool(
                    toolType = toolType,
                    inputText = enrichedInput,
                    subject = subject,
                    gradeLevel = uiState.value.profile.gradeLevel,
                    extraOption = extraOption,
                    attachedBitmap = bitmap
                )
                if (toolType == AiToolType.VOICE_NOTE && input.isNotBlank()) {
                    studioStore?.saveVoiceNote(
                        VoiceNoteRecord(
                            title = "Sesli Not • $subject",
                            subject = subject,
                            rawTranscript = input,
                            cleanedSummary = output
                        )
                    )
                }
                uiState.update { it.copy(activeToolResult = output) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "AI aracı hatası: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isRunningAiTool = false) }
            }
        }
    }

    // --- Coding Studio ---
    fun saveCodeFile(file: CodeProjectFile) {
        studioStore?.saveCodeFile(file.copy(updatedAtMs = System.currentTimeMillis()))
        uiState.update { it.copy(statusBanner = "${file.name} kaydedildi.") }
    }

    fun deleteCodeFile(id: String) {
        studioStore?.deleteCodeFile(id)
    }

    fun analyzeCodeWithAi(language: CodingLanguage, code: String, action: String) {
        scope.launch {
            uiState.update { it.copy(isAnalyzingCode = true, codeAiOutput = null) }
            try {
                val res = aiService.analyzeOrDryRunCode(language, code, action)
                uiState.update { it.copy(codeAiOutput = res) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(codeAiOutput = "Analiz hatası: ${e.localizedMessage}") }
            } finally {
                uiState.update { it.copy(isAnalyzingCode = false) }
            }
        }
    }

    fun shareCodeFile(context: Context, file: CodeProjectFile) {
        scope.launch {
            try {
                val exported = PdfHelper.exportTextFile(context, file.name, file.code)
                PdfHelper.shareFile(context, exported, "text/plain", "Kodu Paylaş / Kaydet")
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Paylaşım hatası: ${e.localizedMessage}") }
            }
        }
    }

    // --- Math Board & Audio Lessons ---
    fun saveMathBoard(board: SavedMathBoard) {
        studioStore?.saveMathBoard(board.copy(updatedAtMs = System.currentTimeMillis()))
        uiState.update { it.copy(statusBanner = "Matematik tahtası kaydedildi.") }
    }

    fun deleteMathBoard(id: String) {
        studioStore?.deleteMathBoard(id)
    }

    fun exportMathBoard(context: Context, title: String, bitmap: Bitmap, asPdf: Boolean) {
        scope.launch {
            try {
                val file = if (asPdf) {
                    PdfHelper.exportBitmapAsPdf(context, bitmap, title)
                } else {
                    PdfHelper.exportBitmapAsPng(context, bitmap, title)
                }
                PdfHelper.shareFile(context, file, if (asPdf) "application/pdf" else "image/png")
            } catch (e: Exception) {
                uiState.update { it.copy(statusBanner = "Tahta dışa aktarılamadı: ${e.localizedMessage}") }
            }
        }
    }

    fun saveAudioLesson(title: String, subject: String, script: String) {
        if (script.isBlank()) return
        studioStore?.saveAudioLesson(
            AudioLessonItem(title = title.ifBlank { "$subject Sesli Ders" }, subject = subject, scriptText = script)
        )
        uiState.update { it.copy(statusBanner = "Sesli ders kütüphaneye kaydedildi.") }
    }
}
