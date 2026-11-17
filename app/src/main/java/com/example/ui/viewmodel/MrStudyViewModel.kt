package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.friendlyFirestoreErrorMessage
import com.example.core.getLocalAppCheckDebugToken
import com.example.core.isAppCheckErrorMessage
import com.example.data.ai.GeminiLiveTeacherManager
import com.example.data.ai.LiveLessonUiState
import com.example.data.ai.MrStudyAiService
import com.example.data.local.LocalStudyMetricsStore
import com.example.data.model.ChatSession
import com.example.data.model.DailyLearningTask
import com.example.data.model.ExamDifficulty
import com.example.data.model.ExamRecord
import com.example.data.model.ExplanationDepth
import com.example.data.model.FlashcardItem
import com.example.data.model.PdfResourceRecord
import com.example.data.model.QuestionType
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyPlanItem
import com.example.data.model.TeachingMode
import com.example.data.repository.MrStudyRepository
import com.example.ui.state.AttachedImage
import com.example.ui.state.ExamUiState
import com.example.ui.state.MrStudyUiState
import com.example.util.PdfHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class MrStudyViewModel(
    private val repository: MrStudyRepository,
    private val aiService: MrStudyAiService,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    private val metricsStore: LocalStudyMetricsStore? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MrStudyUiState())
    val uiState: StateFlow<MrStudyUiState> = _uiState.asStateFlow()

    private val liveManager = GeminiLiveTeacherManager()
    val liveState: StateFlow<LiveLessonUiState> = liveManager.state

    private var messagesJob: Job? = null
    private var examTimerJob: Job? = null
    private var teacherJob: Job? = null

    init {
        initializeUserData()
        observeLocalParentMetrics()
    }

    private fun observeLocalParentMetrics() {
        val store = metricsStore ?: return
        viewModelScope.launch {
            store.metrics.collectLatest { local ->
                _uiState.update { it.copy(parentMetrics = local) }
            }
        }
    }

    private fun initializeUserData() {
        viewModelScope.launch {
            try {
                val existed = repository.ensureProfileExists(
                    userId = currentUserId,
                    displayName = initialDisplayName,
                    email = initialEmail
                )
                repository.ensureTodayTasksExist(currentUserId)
                _uiState.update {
                    it.copy(
                        profile = existed,
                        isProfileLoaded = true,
                        showOnboardingDialog = existed.totalStudyMinutes == 0L && existed.questionsSolved == 0L && existed.xp <= 50L
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile bootstrap failed", e)
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }

        observeAllCollections()
    }

    private fun observeAllCollections() {
        viewModelScope.launch {
            repository.observeProfile(currentUserId)
                .catch { handleStreamError("profile", it) }
                .collectLatest { profile ->
                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                profile = profile,
                                isProfileLoaded = true,
                                selectedMode = TeachingMode.fromTitle(profile.preferredMode)
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.observeChatSessions(currentUserId)
                .catch { handleStreamError("chat_sessions", it) }
                .collectLatest { sessions ->
                    _uiState.update { state ->
                        val activeId = state.activeSessionId?.takeIf { id -> sessions.any { it.id == id } }
                            ?: sessions.firstOrNull()?.id
                        if (activeId != state.activeSessionId && activeId != null) {
                            observeMessagesForSession(activeId)
                        }
                        state.copy(sessions = sessions, activeSessionId = activeId)
                    }
                }
        }

        viewModelScope.launch {
            repository.observePdfSources(currentUserId)
                .catch { handleStreamError("pdf_sources", it) }
                .collectLatest { pdfs ->
                    _uiState.update { state ->
                        val activePdf = state.activePdfId?.takeIf { id -> pdfs.any { it.id == id } }
                            ?: pdfs.firstOrNull()?.id
                        state.copy(pdfSources = pdfs, activePdfId = activePdf)
                    }
                }
        }

        viewModelScope.launch {
            repository.observeExams(currentUserId)
                .catch { handleStreamError("exams", it) }
                .collectLatest { exams ->
                    _uiState.update { it.copy(examHistory = exams) }
                }
        }

        viewModelScope.launch {
            repository.observeStudyPlans(currentUserId)
                .catch { handleStreamError("study_plans", it) }
                .collectLatest { plans ->
                    _uiState.update { it.copy(studyPlans = plans) }
                }
        }

        viewModelScope.launch {
            repository.observeNotes(currentUserId)
                .catch { handleStreamError("notes", it) }
                .collectLatest { notes ->
                    _uiState.update { it.copy(notes = notes) }
                }
        }

        viewModelScope.launch {
            repository.observeFlashcards(currentUserId)
                .catch { handleStreamError("flashcards", it) }
                .collectLatest { cards ->
                    _uiState.update { it.copy(flashcards = cards) }
                }
        }

        viewModelScope.launch {
            repository.observeDailyTasks(currentUserId)
                .catch { handleStreamError("daily_tasks", it) }
                .collectLatest { tasks ->
                    _uiState.update { it.copy(dailyTasks = tasks) }
                }
        }
    }

    private fun handleStreamError(source: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Stream error in $source", firestoreError ?: error)
        _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(firestoreError ?: error)) }
    }

    private fun observeMessagesForSession(sessionId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeChatMessages(currentUserId, sessionId)
                .catch { handleStreamError("messages", it) }
                .collectLatest { msgs ->
                    _uiState.update { it.copy(messages = msgs) }
                }
        }
    }

    fun dismissBanner() {
        _uiState.update {
            it.copy(
                statusBanner = null,
                teacherError = null,
                teacherAppCheckToken = null,
                pdfError = null,
                examError = null
            )
        }
    }

    fun dismissTeacherError() {
        _uiState.update { it.copy(teacherError = null, teacherAppCheckToken = null) }
    }

    fun setOnboardingVisible(visible: Boolean) {
        _uiState.update { it.copy(showOnboardingDialog = visible) }
    }

    fun setProfileSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showProfileSheet = visible) }
    }

    fun saveStudentProfile(
        displayName: String,
        gradeLevel: String,
        targetGoal: String,
        dailyGoalMinutes: Long,
        darkThemeMode: String
    ) {
        viewModelScope.launch {
            try {
                val updated = _uiState.value.profile.copy(
                    displayName = displayName.trim().ifBlank { "Öğrenci" },
                    gradeLevel = gradeLevel,
                    targetGoal = targetGoal.trim(),
                    dailyGoalMinutes = dailyGoalMinutes.coerceIn(10L, 480L),
                    darkThemeMode = darkThemeMode
                )
                repository.updateProfile(currentUserId, updated)
                _uiState.update {
                    it.copy(
                        showOnboardingDialog = false,
                        showProfileSheet = false,
                        statusBanner = "Öğrenci profilin güncellendi."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun toggleThemeMode(mode: String) {
        viewModelScope.launch {
            try {
                repository.updateProfile(
                    currentUserId,
                    _uiState.value.profile.copy(darkThemeMode = mode)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun switchGradeMode(targetGrade: String) {
        val current = _uiState.value.profile
        val updatedGoal = if (targetGrade.contains("6")) {
            "Ortaokul 6. Sınıf Ders Başarısı"
        } else {
            "YKS (TYT + AYT) Yüksek Başarı"
        }
        val updated = current.copy(gradeLevel = targetGrade, targetGoal = updatedGoal)
        _uiState.update { it.copy(profile = updated, statusBanner = "$targetGrade modu aktif edildi.") }
        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                    repository.updateProfile(currentUserId, updated)
                }
            }
        }
    }

    fun updateParentPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            metricsStore?.updateParentPin(newPin)
            _uiState.update { it.copy(statusBanner = "Ebeveyn PIN şifresi güncellendi.") }
        }
    }

    // --- AI Teacher Controls ---
    fun selectTeachingMode(mode: TeachingMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        viewModelScope.launch {
            try {
                repository.updateProfile(
                    currentUserId,
                    _uiState.value.profile.copy(preferredMode = mode.title)
                )
            } catch (_: Exception) {
            }
        }
    }

    fun selectExplanationDepth(depth: ExplanationDepth) {
        _uiState.update { it.copy(selectedDepth = depth) }
    }

    fun selectSubject(subject: String) {
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    fun selectChatSession(session: ChatSession) {
        _uiState.update {
            it.copy(
                activeSessionId = session.id,
                selectedSubject = session.subject,
                selectedMode = TeachingMode.fromTitle(session.teachingMode),
                selectedDepth = ExplanationDepth.fromKey(session.explanationDepth)
            )
        }
        observeMessagesForSession(session.id)
    }

    fun createNewChatSession(
        title: String = "Yeni Ders Oturumu",
        subject: String = _uiState.value.selectedSubject,
        pdf: PdfResourceRecord? = null,
        page: Int = 1
    ) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val newId = repository.createChatSession(
                    userId = currentUserId,
                    title = title,
                    subject = subject,
                    teachingMode = state.selectedMode.title,
                    explanationDepth = state.selectedDepth.key,
                    sourcePdfId = pdf?.id.orEmpty(),
                    sourcePdfTitle = pdf?.title.orEmpty(),
                    sourcePage = if (pdf != null) page.toLong() else 0L
                )
                _uiState.update { it.copy(activeSessionId = newId, messages = emptyList()) }
                observeMessagesForSession(newId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(teacherError = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteChatSession(currentUserId, sessionId)
                if (_uiState.value.activeSessionId == sessionId) {
                    _uiState.update { it.copy(activeSessionId = null, messages = emptyList()) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(teacherError = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun attachImages(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            try {
                val decoded = uris.take(4).mapIndexed { idx, uri ->
                    val bmp = PdfHelper.decodeScaledBitmap(context, uri)
                    AttachedImage(uri = uri, bitmap = bmp, label = "Görsel ${idx + 1}")
                }
                _uiState.update { it.copy(attachedImages = (it.attachedImages + decoded).take(4)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(teacherError = "Görsel yüklenemedi: ${e.localizedMessage}") }
            }
        }
    }

    fun removeAttachedImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(attachedImages = state.attachedImages.filterNot { it.uri == uri })
        }
    }

    fun cancelTeacherStream() {
        teacherJob?.cancel()
        teacherJob = null
        _uiState.update {
            it.copy(
                isTeacherStreaming = false,
                streamingReply = ""
            )
        }
    }

    fun sendTeacherMessage(context: Context, promptText: String, includeActivePdf: Boolean = true) {
        val cleanPrompt = promptText.trim()
        if (cleanPrompt.isBlank() && _uiState.value.attachedImages.isEmpty()) return

        val appContext = context.applicationContext
        teacherJob?.cancel()
        teacherJob = viewModelScope.launch {
            val state = _uiState.value
            val effectivePrompt = cleanPrompt.ifBlank {
                "Yüklediğim görseli analiz et. Matematik sorusuysa adım adım çöz; şema, grafik, tablo veya harita ise açıkla. Okunamayan alan varsa belirt."
            }
            val imagesToSend = state.attachedImages
            val activePdf = if (includeActivePdf) state.activePdf else null
            val activePage = state.activePdfPage

            _uiState.update {
                it.copy(
                    isTeacherStreaming = true,
                    streamingReply = "",
                    teacherError = null,
                    teacherAppCheckToken = null,
                    attachedImages = emptyList()
                )
            }

            try {
                val sessionId = state.activeSessionId ?: withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                    repository.createChatSession(
                        userId = currentUserId,
                        title = effectivePrompt.take(40),
                        subject = state.selectedSubject,
                        teachingMode = state.selectedMode.title,
                        explanationDepth = state.selectedDepth.key,
                        sourcePdfId = activePdf?.id.orEmpty(),
                        sourcePdfTitle = activePdf?.title.orEmpty(),
                        sourcePage = if (activePdf != null) activePage.toLong() else 0L
                    )
                }.also { createdId ->
                    _uiState.update { it.copy(activeSessionId = createdId) }
                    observeMessagesForSession(createdId)
                }

                val attachmentSummary = buildString {
                    if (imagesToSend.isNotEmpty()) append("${imagesToSend.size} Görsel")
                    if (activePdf != null) {
                        if (isNotEmpty()) append(" • ")
                        append("${activePdf.title} s.$activePage")
                    }
                }

                withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                    repository.addChatMessage(
                        userId = currentUserId,
                        sessionId = sessionId,
                        role = "user",
                        content = effectivePrompt,
                        teachingMode = state.selectedMode.title,
                        hasAttachment = imagesToSend.isNotEmpty() || activePdf != null,
                        attachmentLabel = attachmentSummary
                    )
                }

                val pdfSamples = if (activePdf != null && PdfHelper.isLocalPdfAvailable(activePdf.localUriString)) {
                    PdfHelper.renderPdfSamplePages(activePdf.localUriString, activePage, maxPages = 2)
                } else {
                    emptyList()
                }

                val finalReply = StringBuilder()
                aiService.streamTeacherReply(
                    studentProfile = state.profile,
                    subject = state.selectedSubject,
                    mode = state.selectedMode,
                    depth = state.selectedDepth,
                    userPrompt = effectivePrompt,
                    recentHistory = state.messages,
                    attachedImages = imagesToSend.map { it.bitmap },
                    pdfPageSamples = pdfSamples,
                    pdfTitle = if (pdfSamples.isNotEmpty()) activePdf?.title else null,
                    activePdfPage = if (pdfSamples.isNotEmpty()) activePage else null
                ).collect { partial ->
                    finalReply.clear()
                    finalReply.append(partial)
                    _uiState.update { it.copy(streamingReply = partial) }
                }

                val citation = if (pdfSamples.isNotEmpty() && activePdf != null) {
                    "Kaynak: ${activePdf.title} (s.$activePage)"
                } else {
                    "Genel Ders Anlatımı"
                }

                withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                    repository.addChatMessage(
                        userId = currentUserId,
                        sessionId = sessionId,
                        role = "assistant",
                        content = finalReply.toString(),
                        teachingMode = state.selectedMode.title,
                        sourceCitation = citation
                    )
                }

                metricsStore?.recordQuestionSolved(state.selectedSubject, 1)
                runCatching {
                    withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                        repository.awardProgress(
                            userId = currentUserId,
                            currentProfile = _uiState.value.profile,
                            xpDelta = 15L,
                            minutesDelta = 3L,
                            questionsDelta = 1L,
                            newBadge = if (_uiState.value.profile.questionsSolved >= 4) "Meraklı Öğrenci" else null
                        )
                    }
                }.onFailure { err ->
                    if (err is CancellationException && err !is TimeoutCancellationException) throw err
                    Log.w(TAG, "Progress update skipped after reply", err)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Firestore operation timed out during chat", e)
                _uiState.update {
                    it.copy(
                        teacherError = "Sunucu bağlantısı 15 saniye içinde yanıt vermedi. İnternetinizi kontrol edip tekrar deneyin."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending message", e)
                val msg = e.localizedMessage ?: "Öğretmen yanıtı alınamadı."
                val debugToken = if (isAppCheckErrorMessage(msg)) {
                    getLocalAppCheckDebugToken(appContext)
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        teacherError = msg,
                        teacherAppCheckToken = debugToken
                    )
                }
            } finally {
                _uiState.update { it.copy(isTeacherStreaming = false, streamingReply = "") }
            }
        }
    }

    // --- PDF Studio ---
    fun importPdf(context: Context, uri: Uri, subject: String) {
        importMultiplePdfs(context, listOf(uri), subject)
    }

    fun importMultiplePdfs(context: Context, uris: List<Uri>, subject: String) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPdfBusy = true,
                    pdfError = null,
                    pdfImportProgressText = if (uris.size > 1) {
                        "1 / ${uris.size} PDF işleniyor ve Firestore'a kaydediliyor..."
                    } else {
                        "PDF işleniyor ve Firestore'a kaydediliyor..."
                    }
                )
            }
            val importedNames = mutableListOf<String>()
            val failedMessages = mutableListOf<String>()
            var lastPdfId: String? = null
            var lastPageBitmap: android.graphics.Bitmap? = null

            try {
                uris.forEachIndexed { index, uri ->
                    if (uris.size > 1) {
                        _uiState.update {
                            it.copy(
                                pdfImportProgressText = "${index + 1} / ${uris.size} PDF işleniyor ve Firestore'a kaydediliyor..."
                            )
                        }
                    }
                    try {
                        val displayName = resolveFileName(context, uri)
                        val info = PdfHelper.importPdfToAppStorage(context, uri, displayName)
                        val pdfId = repository.addPdfSource(
                            userId = currentUserId,
                            title = info.fileName,
                            subject = subject,
                            localUriString = info.localPath,
                            pageCount = info.pageCount.toLong()
                        )
                        lastPdfId = pdfId
                        lastPageBitmap = PdfHelper.renderPdfPage(info.localPath, 0)
                        importedNames.add(info.fileName)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val name = runCatching { resolveFileName(context, uri) }.getOrDefault("Dosya")
                        failedMessages.add("$name: ${e.localizedMessage ?: "Yüklenemedi"}")
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        activePdfId = lastPdfId ?: state.activePdfId,
                        activePdfPage = if (lastPdfId != null) 1 else state.activePdfPage,
                        renderedPdfPage = lastPageBitmap ?: state.renderedPdfPage,
                        pdfError = if (failedMessages.isNotEmpty()) failedMessages.joinToString("\n") else null,
                        statusBanner = when {
                            importedNames.size == 1 -> "'${importedNames.first()}' Firestore'a kaydedildi."
                            importedNames.size > 1 -> "${importedNames.size} PDF kaynağı Firestore'a kaydedildi."
                            else -> state.statusBanner
                        }
                    )
                }
            } finally {
                _uiState.update { it.copy(isPdfBusy = false, pdfImportProgressText = null) }
            }
        }
    }

    fun relinkPdfFile(context: Context, pdf: PdfResourceRecord, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPdfBusy = true,
                    pdfError = null,
                    pdfImportProgressText = "'${pdf.title}' dosya referansı güncelleniyor..."
                )
            }
            try {
                val displayName = resolveFileName(context, uri)
                val info = PdfHelper.importPdfToAppStorage(context, uri, displayName)
                PdfHelper.deleteLocalPdfIfOwned(context, pdf.localUriString)
                val safeLastPage = pdf.lastStudiedPage.coerceIn(1L, info.pageCount.toLong())
                repository.updatePdfSource(
                    currentUserId,
                    pdf.copy(
                        localUriString = info.localPath,
                        pageCount = info.pageCount.toLong(),
                        lastStudiedPage = safeLastPage
                    )
                )
                val bitmap = PdfHelper.renderPdfPage(info.localPath, (safeLastPage - 1).toInt())
                _uiState.update {
                    it.copy(
                        activePdfId = pdf.id,
                        activePdfPage = safeLastPage.toInt(),
                        renderedPdfPage = bitmap,
                        statusBanner = "'${pdf.title}' dosya referansı Firestore'da güncellendi."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(pdfError = e.localizedMessage ?: "Dosya referansı güncellenemedi.")
                }
            } finally {
                _uiState.update { it.copy(isPdfBusy = false, pdfImportProgressText = null) }
            }
        }
    }

    fun selectPdfAndLoadPage(pdf: PdfResourceRecord, pageNumber: Int = pdf.lastStudiedPage.toInt().coerceAtLeast(1)) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPdfBusy = true, pdfError = null, activePdfId = pdf.id) }
            try {
                val safePage = pageNumber.coerceIn(1, pdf.pageCount.toInt().coerceAtLeast(1))
                val bitmap = PdfHelper.renderPdfPage(pdf.localUriString, safePage - 1)
                _uiState.update { it.copy(activePdfPage = safePage, renderedPdfPage = bitmap) }
                if (safePage.toLong() != pdf.lastStudiedPage) {
                    repository.updatePdfSource(currentUserId, pdf.copy(lastStudiedPage = safePage.toLong()))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfError = e.localizedMessage ?: "PDF sayfası açılamadı.") }
            } finally {
                _uiState.update { it.copy(isPdfBusy = false) }
            }
        }
    }

    fun renamePdf(pdf: PdfResourceRecord, newTitle: String, newSubject: String) {
        viewModelScope.launch {
            try {
                repository.updatePdfSource(
                    currentUserId,
                    pdf.copy(
                        title = newTitle.trim().ifBlank { pdf.title },
                        subject = newSubject
                    )
                )
                _uiState.update { it.copy(statusBanner = "PDF referansı Firestore'da güncellendi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfError = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun deletePdf(context: Context, pdf: PdfResourceRecord) {
        viewModelScope.launch {
            try {
                repository.deletePdfSource(currentUserId, pdf.id)
                PdfHelper.deleteLocalPdfIfOwned(context, pdf.localUriString)
                if (_uiState.value.activePdfId == pdf.id) {
                    _uiState.update { it.copy(activePdfId = null, renderedPdfPage = null) }
                }
                _uiState.update { it.copy(statusBanner = "'${pdf.title}' referansı silindi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfError = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun analyzeActivePdfAndSaveNote(saveAsNote: Boolean) {
        val pdf = _uiState.value.activePdf ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPdfBusy = true, pdfError = null) }
            try {
                val samples = PdfHelper.renderPdfSamplePages(
                    pdf.localUriString,
                    _uiState.value.activePdfPage,
                    maxPages = 4
                )
                val analysis = aiService.analyzePdfPages(
                    pdfTitle = pdf.title,
                    subject = pdf.subject,
                    gradeLevel = _uiState.value.profile.gradeLevel,
                    pageSamples = samples
                )
                repository.updatePdfSource(
                    currentUserId,
                    pdf.copy(summary = analysis.summary, topics = analysis.topics)
                )
                if (saveAsNote) {
                    repository.addNote(
                        currentUserId,
                        StudyNote(
                            title = "${pdf.title} - Ders Notu",
                            subject = pdf.subject,
                            content = analysis.summary,
                            formulas = analysis.formulas,
                            definitions = analysis.definitions,
                            keyConcepts = analysis.keyConcepts,
                            sourcePdfTitle = pdf.title
                        )
                    )
                }
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = _uiState.value.profile,
                    xpDelta = 25L,
                    minutesDelta = 5L,
                    newBadge = "PDF Araştırmacısı"
                )
                _uiState.update {
                    it.copy(
                        statusBanner = if (saveAsNote) {
                            "PDF özetlendi ve Çalışma Notlarına kaydedildi."
                        } else {
                            "PDF analizi ve konu haritası güncellendi."
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfError = e.localizedMessage ?: "PDF analiz edilemedi.") }
            } finally {
                _uiState.update { it.copy(isPdfBusy = false) }
            }
        }
    }

    // --- Smart Exam System ---
    fun startSmartExam(
        subject: String,
        topic: String,
        difficulty: ExamDifficulty,
        questionCount: Int,
        sourceType: String,
        selectedTypes: List<QuestionType>,
        timedMinutes: Int,
        pdfSource: PdfResourceRecord? = null,
        focusWrongSummary: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(examUiState = ExamUiState.Generating, examError = null) }
            try {
                val pdfSamples = if (pdfSource != null) {
                    PdfHelper.renderPdfSamplePages(pdfSource.localUriString, _uiState.value.activePdfPage, maxPages = 3)
                } else {
                    emptyList()
                }
                val exam = aiService.generateExam(
                    gradeLevel = _uiState.value.profile.gradeLevel,
                    subject = subject,
                    topic = topic.ifBlank { pdfSource?.title ?: subject },
                    difficulty = difficulty,
                    questionCount = questionCount,
                    sourceType = sourceType,
                    selectedTypes = selectedTypes.ifEmpty { listOf(QuestionType.MULTIPLE_CHOICE) },
                    timedMinutes = timedMinutes,
                    pdfSamples = pdfSamples,
                    focusWrongQuestions = focusWrongSummary
                )
                _uiState.update {
                    it.copy(
                        examUiState = ExamUiState.InProgress(
                            exam = exam,
                            remainingSeconds = if (timedMinutes > 0) timedMinutes * 60 else 0
                        )
                    )
                }
                startExamTimer(timedMinutes > 0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        examUiState = ExamUiState.Config,
                        examError = e.localizedMessage ?: "Sınav oluşturulamadı."
                    )
                }
            }
        }
    }

    private fun startExamTimer(hasCountdown: Boolean) {
        examTimerJob?.cancel()
        examTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _uiState.value.examUiState
                if (current !is ExamUiState.InProgress) break
                val nextElapsed = current.elapsedSeconds + 1
                val nextRemaining = if (hasCountdown) (current.remainingSeconds - 1).coerceAtLeast(0) else 0
                _uiState.update {
                    it.copy(
                        examUiState = current.copy(
                            elapsedSeconds = nextElapsed,
                            remainingSeconds = nextRemaining
                        )
                    )
                }
                if (hasCountdown && nextRemaining == 0) {
                    submitExam()
                    break
                }
            }
        }
    }

    fun updateExamAnswer(questionNumber: Int, answer: String) {
        val current = _uiState.value.examUiState as? ExamUiState.InProgress ?: return
        _uiState.update {
            it.copy(
                examUiState = current.copy(
                    answers = current.answers + (questionNumber to answer)
                )
            )
        }
    }

    fun submitExam() {
        val current = _uiState.value.examUiState as? ExamUiState.InProgress ?: return
        examTimerJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(examUiState = ExamUiState.Grading, examError = null) }
            try {
                val summary = aiService.evaluateExamAndAnalyzeErrors(
                    exam = current.exam,
                    answers = current.answers,
                    durationSeconds = current.elapsedSeconds,
                    gradeLevel = _uiState.value.profile.gradeLevel
                )
                repository.saveExamRecord(
                    currentUserId,
                    ExamRecord(
                        title = summary.title,
                        subject = summary.subject,
                        topic = summary.topic,
                        difficulty = summary.difficulty,
                        questionCount = summary.totalCount.toLong(),
                        correctCount = summary.correctCount.toLong(),
                        scorePercent = summary.scorePercent.toLong(),
                        durationSeconds = summary.durationSeconds.toLong(),
                        sourceType = summary.sourceType,
                        weakTopics = summary.weakTopics,
                        errorCategories = summary.errorCategories,
                        aiReport = summary.overallReport,
                        wrongQuestionsSummary = summary.wrongQuestionsSummary
                    )
                )
                metricsStore?.recordQuestionSolved(summary.subject, summary.totalCount)
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = _uiState.value.profile,
                    xpDelta = (summary.scorePercent / 2 + 20).toLong(),
                    minutesDelta = (summary.durationSeconds / 60).coerceAtLeast(2).toLong(),
                    questionsDelta = summary.totalCount.toLong(),
                    newBadge = if (summary.scorePercent >= 80) "Sınav Yıldızı" else null
                )
                _uiState.update { it.copy(examUiState = ExamUiState.Result(summary)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        examUiState = current,
                        examError = e.localizedMessage ?: "Sınav değerlendirilemedi."
                    )
                }
            }
        }
    }

    fun resetExamToConfig() {
        examTimerJob?.cancel()
        _uiState.update { it.copy(examUiState = ExamUiState.Config, examError = null) }
    }

    // --- Study Plans ---
    fun addManualStudyPlan(
        subject: String,
        topic: String,
        dayLabel: String,
        durationMinutes: Long,
        priority: String,
        planType: String,
        examDateText: String
    ) {
        viewModelScope.launch {
            try {
                repository.addStudyPlanItem(
                    currentUserId,
                    StudyPlanItem(
                        subject = subject,
                        topic = topic,
                        dayLabel = dayLabel,
                        durationMinutes = durationMinutes,
                        priority = priority,
                        planType = planType,
                        examDateText = examDateText
                    )
                )
                _uiState.update { it.copy(statusBanner = "Çalışma planı eklendi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun generateAiWeeklyPlan(focusSubject: String, examDateText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPlan = true) }
            try {
                val weakTopics = _uiState.value.examHistory.flatMap { it.weakTopics }.distinct().take(6)
                val items = aiService.generateStudyPlans(
                    profile = _uiState.value.profile,
                    weakTopics = weakTopics,
                    examDateText = examDateText,
                    focusSubject = focusSubject
                )
                items.forEach { repository.addStudyPlanItem(currentUserId, it) }
                _uiState.update { it.copy(statusBanner = "Yapay zekâ destekli haftalık çalışma planın oluşturuldu.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = e.localizedMessage ?: "Plan oluşturulamadı.") }
            } finally {
                _uiState.update { it.copy(isGeneratingPlan = false) }
            }
        }
    }

    fun toggleStudyPlanComplete(item: StudyPlanItem) {
        viewModelScope.launch {
            try {
                val nowComplete = !item.isCompleted
                repository.updateStudyPlanItem(currentUserId, item.copy(isCompleted = nowComplete))
                if (nowComplete) {
                    repository.awardProgress(
                        userId = currentUserId,
                        currentProfile = _uiState.value.profile,
                        xpDelta = 30L,
                        minutesDelta = item.durationMinutes,
                        newBadge = "Planlı Çalışkan"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun postponeStudyPlan(item: StudyPlanItem) {
        val nextDays = listOf("Bugün", "Yarın", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
        val idx = nextDays.indexOf(item.dayLabel)
        val nextDay = if (idx >= 0) nextDays[(idx + 1) % nextDays.size] else "Yarın"
        viewModelScope.launch {
            try {
                repository.updateStudyPlanItem(
                    currentUserId,
                    item.copy(
                        dayLabel = nextDay,
                        postponedCount = item.postponedCount + 1L,
                        priority = "Yüksek"
                    )
                )
                _uiState.update { it.copy(statusBanner = "'${item.topic}' $nextDay gününe yeniden planlandı.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun deleteStudyPlan(planId: String) {
        viewModelScope.launch {
            try {
                repository.deleteStudyPlanItem(currentUserId, planId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    // --- Smart Notes ---
    fun generateAiNote(subject: String, topic: String, pdf: PdfResourceRecord? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingNote = true) }
            try {
                val samples = if (pdf != null) {
                    PdfHelper.renderPdfSamplePages(pdf.localUriString, _uiState.value.activePdfPage, maxPages = 3)
                } else emptyList()
                val note = aiService.generateStructuredNote(
                    subject = subject,
                    topicOrPrompt = topic,
                    gradeLevel = _uiState.value.profile.gradeLevel,
                    pdfTitle = pdf?.title,
                    pdfSamples = samples
                )
                repository.addNote(currentUserId, note)
                _uiState.update { it.copy(statusBanner = "'${note.title}' notu oluşturuldu.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = e.localizedMessage ?: "Not oluşturulamadı.") }
            } finally {
                _uiState.update { it.copy(isGeneratingNote = false) }
            }
        }
    }

    fun saveManualOrEditedNote(note: StudyNote) {
        viewModelScope.launch {
            try {
                if (note.id.isBlank()) {
                    repository.addNote(currentUserId, note)
                } else {
                    repository.updateNote(currentUserId, note)
                }
                _uiState.update { it.copy(statusBanner = "Not kaydedildi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                repository.deleteNote(currentUserId, noteId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun exportNotePdf(context: Context, note: StudyNote) {
        viewModelScope.launch {
            try {
                val file = PdfHelper.exportNoteAsPdf(context, note)
                PdfHelper.sharePdfFile(context, file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "PDF dışa aktarılamadı: ${e.localizedMessage}") }
            }
        }
    }

    // --- Flashcards ---
    fun generateAiFlashcards(subject: String, topic: String, pdf: PdfResourceRecord? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingCards = true) }
            try {
                val samples = if (pdf != null) {
                    PdfHelper.renderPdfSamplePages(pdf.localUriString, _uiState.value.activePdfPage, maxPages = 3)
                } else emptyList()
                val cards = aiService.generateFlashcards(
                    subject = subject,
                    topic = topic,
                    gradeLevel = _uiState.value.profile.gradeLevel,
                    count = 5,
                    pdfSamples = samples
                )
                cards.forEach { repository.addFlashcard(currentUserId, it) }
                _uiState.update { it.copy(statusBanner = "${cards.size} adet bilgi kartı eklendi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = e.localizedMessage ?: "Kartlar üretilemedi.") }
            } finally {
                _uiState.update { it.copy(isGeneratingCards = false) }
            }
        }
    }

    fun reviewFlashcard(card: FlashcardItem, learned: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateFlashcardReview(currentUserId, card, learned)
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = _uiState.value.profile,
                    xpDelta = if (learned) 15L else 5L
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun deleteFlashcard(cardId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFlashcard(currentUserId, cardId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    // --- Interactive Daily Tasks & Mini Quiz ---
    fun toggleDailyTask(task: DailyLearningTask) {
        viewModelScope.launch {
            try {
                val becomingDone = !task.isCompleted
                repository.toggleDailyTask(currentUserId, task)
                if (becomingDone) {
                    repository.awardProgress(
                        userId = currentUserId,
                        currentProfile = _uiState.value.profile,
                        xpDelta = task.xpReward,
                        minutesDelta = 10L
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    fun loadMiniQuiz(subject: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMiniQuiz = true) }
            try {
                val qs = aiService.generateMiniQuiz(_uiState.value.profile.gradeLevel, subject)
                _uiState.update { it.copy(miniQuizQuestions = qs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = e.localizedMessage ?: "Mini yarışma yüklenemedi.") }
            } finally {
                _uiState.update { it.copy(isLoadingMiniQuiz = false) }
            }
        }
    }

    fun awardMiniQuizXp(correctCount: Int) {
        metricsStore?.recordQuestionSolved("Genel", 3)
        viewModelScope.launch {
            try {
                repository.awardProgress(
                    userId = currentUserId,
                    currentProfile = _uiState.value.profile,
                    xpDelta = (correctCount * 15L + 10L),
                    questionsDelta = 3L,
                    newBadge = if (correctCount == 3) "Bilgi Yarışması Şampiyonu" else null
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "PDF Kaynak"
            }
        }
        return uri.lastPathSegment ?: "PDF Kaynak"
    }

    // --- Gemini Live Canlı Ders ---
    fun selectLiveVoice(voiceId: String) {
        liveManager.selectVoice(voiceId)
    }

    fun setLiveEchoGuardEnabled(enabled: Boolean) {
        liveManager.setEchoGuardEnabled(enabled)
    }

    fun interruptLiveTeacher() {
        liveManager.interruptAndSpeakNow()
    }

    fun dismissLiveError() {
        liveManager.clearError()
    }

    fun toggleLiveMute() {
        liveManager.toggleMute()
    }

    fun startLiveLesson(context: Context) {
        val state = _uiState.value
        val activePdf = state.activePdf
        _uiState.update {
            it.copy(
                liveSessionEndedShowGameCta = false,
                midLessonQuestion = null
            )
        }
        liveManager.startSession(
            scope = viewModelScope,
            context = context,
            profile = state.profile,
            subject = state.selectedSubject,
            teachingMode = state.selectedMode,
            pdfTitle = activePdf?.title,
            activePdfPage = if (activePdf != null) state.activePdfPage else null,
            initialPdfBitmap = if (activePdf != null) state.renderedPdfPage else null,
            onUserPageCommand = { targetPage ->
                jumpLiveToPdfPage(targetPage)
            }
        )
    }

    fun stopLiveLesson() {
        val snapshot = liveManager.state.value
        val elapsed = snapshot.elapsedSeconds
        val hadActivity = elapsed >= 5 || snapshot.transcriptItems.isNotEmpty()
        val lastTopic = snapshot.transcriptItems.lastOrNull { !it.isStudent }?.text?.take(80)
            ?: _uiState.value.activePdf?.title
            ?: _uiState.value.selectedSubject
        liveManager.stopSession()

        if (elapsed >= 5) {
            metricsStore?.recordLiveSession(_uiState.value.selectedSubject, elapsed, lastTopic)
        }
        _uiState.update {
            it.copy(
                liveSessionEndedShowGameCta = hadActivity,
                game3dTopic = lastTopic
            )
        }
        if (elapsed >= 20) {
            val mins = (elapsed / 60).coerceAtLeast(1).toLong()
            viewModelScope.launch {
                runCatching {
                    withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                        repository.awardProgress(
                            userId = currentUserId,
                            currentProfile = _uiState.value.profile,
                            xpDelta = 25L,
                            minutesDelta = mins,
                            newBadge = "Canlı Ders Öğrencisi"
                        )
                    }
                }
            }
        }
    }

    fun sendLiveText(text: String) {
        liveManager.sendTextDuringLive(text)
    }

    fun sendLiveImageUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bmp = PdfHelper.decodeScaledBitmap(context, uri, maxDim = 960)
                liveManager.sendVisualContextDuringLive(
                    bitmap = bmp,
                    label = "Soru Görseli",
                    promptText = "Ekrana gönderdiğim bu soruyu/görseli incele ve sesli olarak adım adım anlat."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "Görsel canlı derse gönderilemedi: ${e.localizedMessage}") }
            }
        }
    }

    fun sendActivePdfPageToLive() {
        val state = _uiState.value
        val activePdf = state.activePdf ?: return
        val page = state.activePdfPage
        viewModelScope.launch {
            try {
                val bmp = state.renderedPdfPage
                    ?: PdfHelper.renderPdfPage(activePdf.localUriString, (page - 1).coerceAtLeast(0), maxWidthPx = 960)
                liveManager.sendVisualContextDuringLive(
                    bitmap = bmp,
                    label = "${activePdf.title} s.$page",
                    promptText = "Öğrencinin PDF kaynağının $page. sayfasını görüyorsun. Bu sayfadaki ana konuyu ve varsa örnekleri canlı olarak anlat."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "PDF sayfası gönderilemedi: ${e.localizedMessage}") }
            }
        }
    }

    fun changeLivePdfPage(delta: Int) {
        val state = _uiState.value
        val activePdf = state.activePdf ?: return
        val maxPages = activePdf.pageCount.toInt().coerceAtLeast(1)
        val currentPage = state.activePdfPage
        val newPage = (currentPage + delta).coerceIn(1, maxPages)
        if (newPage == currentPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPdfBusy = true) }
            try {
                val bitmap = PdfHelper.renderPdfPage(activePdf.localUriString, newPage - 1, maxWidthPx = 960)
                _uiState.update {
                    it.copy(
                        activePdfPage = newPage,
                        renderedPdfPage = bitmap,
                        statusBanner = "${activePdf.title}: $newPage. sayfaya geçildi"
                    )
                }
                repository.updatePdfSource(currentUserId, activePdf.copy(lastStudiedPage = newPage.toLong()))

                if (liveManager.state.value.isSessionActive) {
                    if (bitmap != null) {
                        liveManager.sendVisualContextDuringLive(
                            bitmap = bitmap,
                            label = "${activePdf.title} s.$newPage",
                            promptText = "Öğrenci PDF kaynağında $newPage. sayfaya geçti (önceki sayfa: $currentPage). Bu yeni sayfadaki konuyu/soruları kısaca inceleyip öğrenciye rehberlik et."
                        )
                    } else {
                        liveManager.sendTextDuringLive("Öğrenci ${activePdf.title} dökümanında $newPage. sayfaya geçti.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfError = "Sayfa değiştirilemedi: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isPdfBusy = false) }
            }
        }
    }

    fun jumpLiveToPdfPage(targetPage: Int) {
        val state = _uiState.value
        val activePdf = state.activePdf ?: return
        val maxPages = activePdf.pageCount.toInt().coerceAtLeast(1)
        val safePage = targetPage.coerceIn(1, maxPages)
        if (safePage == state.activePdfPage) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPdfBusy = true) }
            try {
                val bitmap = PdfHelper.renderPdfPage(activePdf.localUriString, safePage - 1, maxWidthPx = 960)
                _uiState.update {
                    it.copy(
                        activePdfPage = safePage,
                        renderedPdfPage = bitmap,
                        statusBanner = "${activePdf.title}: $safePage. sayfaya sesli komutla geçildi"
                    )
                }
                repository.updatePdfSource(currentUserId, activePdf.copy(lastStudiedPage = safePage.toLong()))

                if (liveManager.state.value.isSessionActive) {
                    if (bitmap != null) {
                        liveManager.sendVisualContextDuringLive(
                            bitmap = bitmap,
                            label = "${activePdf.title} s.$safePage",
                            promptText = "Öğrenci sesli komutla $safePage. sayfaya geçti. Bu yeni sayfadaki konuyu ve soruları inceleyip anlat."
                        )
                    } else {
                        liveManager.sendTextDuringLive("Öğrenci ${activePdf.title} dökümanında $safePage. sayfaya geçti.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfError = "Sayfaya geçilemedi: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isPdfBusy = false) }
            }
        }
    }

    fun selectPdfForLive(pdf: PdfResourceRecord) {
        selectPdfAndLoadPage(pdf, pdf.lastStudiedPage.toInt().coerceAtLeast(1))
        if (liveManager.state.value.isSessionActive) {
            viewModelScope.launch {
                delay(350L)
                sendActivePdfPageToLive()
            }
        }
    }

    fun toggleBluetoothAudio() {
        liveManager.toggleAudioOutput()
    }

    fun saveLiveTranscriptAsNote() {
        val entries = liveManager.state.value.transcriptItems
        if (entries.isEmpty()) return
        val subject = _uiState.value.selectedSubject
        val pdfTitle = _uiState.value.activePdf?.title.orEmpty()
        val body = entries.joinToString("\n\n") { entry ->
            val who = if (entry.isStudent) "Öğrenci" else "MrStudy Canlı"
            "$who: ${entry.text}"
        }
        viewModelScope.launch {
            try {
                withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                    repository.addNote(
                        currentUserId,
                        StudyNote(
                            title = "Canlı Ders Notu - $subject",
                            subject = subject,
                            content = body,
                            sourcePdfTitle = pdfTitle
                        )
                    )
                }
                _uiState.update { it.copy(statusBanner = "Canlı ders transkripti Çalışma Notlarına kaydedildi.") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = friendlyFirestoreErrorMessage(e)) }
            }
        }
    }

    // --- MrStudy 3.0 Smart Board, Mid-Lesson Quiz, 3D Game & A4 Worksheet PDF ---
    fun refreshLiveSmartBoard(topicHint: String = "") {
        val state = _uiState.value
        val live = liveManager.state.value
        val contextHint = topicHint.ifBlank {
            live.transcriptItems.takeLast(4).joinToString(" ") { it.text }
                .ifBlank { state.activePdf?.title ?: state.selectedSubject }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingSmartBoard = true) }
            try {
                val payload = aiService.generateSmartBoardDrawing(
                    topicOrTranscript = contextHint,
                    subject = state.selectedSubject,
                    gradeLevel = state.profile.gradeLevel
                )
                _uiState.update { it.copy(smartBoardState = payload) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "Akıllı tahta güncellenemedi: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isGeneratingSmartBoard = false) }
            }
        }
    }

    fun triggerMidLessonQuestion() {
        val state = _uiState.value
        val live = liveManager.state.value
        val transcript = live.transcriptItems.takeLast(4).joinToString(" ") { it.text }
            .ifBlank { "${state.selectedSubject} dersi (${state.activePdf?.title ?: "Genel tekrar"})" }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMidQuestion = true) }
            try {
                val q = aiService.generateMidLessonQuestion(
                    transcript = transcript,
                    subject = state.selectedSubject,
                    gradeLevel = state.profile.gradeLevel
                )
                _uiState.update { it.copy(midLessonQuestion = q) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "Anlık soru üretilemedi: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoadingMidQuestion = false) }
            }
        }
    }

    fun answerMidLessonQuestion(isCorrect: Boolean) {
        val subject = _uiState.value.selectedSubject
        metricsStore?.recordQuestionSolved(subject, 1)
        if (isCorrect) {
            viewModelScope.launch {
                runCatching {
                    withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                        repository.awardProgress(
                            userId = currentUserId,
                            currentProfile = _uiState.value.profile,
                            xpDelta = 15L,
                            questionsDelta = 1L
                        )
                    }
                }
            }
        }
    }

    fun dismissMidLessonQuestion() {
        _uiState.update { it.copy(midLessonQuestion = null) }
    }

    fun load3dGameQuestions(customTopic: String = "") {
        val state = _uiState.value
        val live = liveManager.state.value
        val topic = customTopic.ifBlank {
            state.game3dTopic.ifBlank {
                live.transcriptItems.lastOrNull { !it.isStudent }?.text?.take(60)
                    ?: state.activePdf?.title
                    ?: state.selectedSubject
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGame3d = true, game3dTopic = topic, game3dQuestions = emptyList()) }
            try {
                val qs = aiService.generate3dGameQuestions(
                    topic = topic,
                    subject = state.selectedSubject,
                    gradeLevel = state.profile.gradeLevel
                )
                _uiState.update { it.copy(game3dQuestions = qs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "3B oyun soruları üretilemedi: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoadingGame3d = false) }
            }
        }
    }

    fun record3dGameCompletion(correctCount: Int, totalCount: Int, score: Int) {
        val subject = _uiState.value.selectedSubject
        metricsStore?.record3dGameResult(subject, correctCount, totalCount, score)
        viewModelScope.launch {
            runCatching {
                withTimeout(FIRESTORE_WRITE_TIMEOUT_MS) {
                    repository.awardProgress(
                        userId = currentUserId,
                        currentProfile = _uiState.value.profile,
                        xpDelta = (correctCount * 20L + 15L),
                        minutesDelta = 2L,
                        questionsDelta = totalCount.toLong(),
                        newBadge = if (correctCount >= 4) "3B Uzay Kaşifi" else null
                    )
                }
            }
        }
    }

    fun exportChatWorksheetPdf(context: Context) {
        val state = _uiState.value
        val chatContext = state.messages.takeLast(8).joinToString("\n") {
            val who = if (it.role == "user") "Öğrenci" else "Öğretmen"
            "$who: ${it.content}"
        }.ifBlank { "${state.selectedSubject} dersi genel konu özeti ve çalışma soruları." }
        exportWorksheetFromContextInternal(
            context = context,
            title = "${state.selectedSubject} • Sohbet Çalışma Kağıdı",
            subject = state.selectedSubject,
            contextText = chatContext,
            boardFormulas = state.smartBoardState?.formulas.orEmpty()
        )
    }

    fun exportLiveWorksheetPdf(context: Context) {
        val state = _uiState.value
        val live = liveManager.state.value
        val transcript = live.transcriptItems.takeLast(12).joinToString("\n") {
            val who = if (it.isStudent) "Öğrenci" else "MrStudy Canlı"
            "$who: ${it.text}"
        }.ifBlank { state.smartBoardState?.summaryNote.orEmpty().ifBlank { "${state.selectedSubject} Canlı Ders Özeti" } }
        exportWorksheetFromContextInternal(
            context = context,
            title = "${state.selectedSubject} • Canlı Ders Çalışma Kağıdı",
            subject = state.selectedSubject,
            contextText = transcript,
            boardFormulas = state.smartBoardState?.formulas.orEmpty()
        )
    }

    fun exportNoteWorksheetPdf(context: Context, note: StudyNote) {
        val boardFormulas = (note.formulas + _uiState.value.smartBoardState?.formulas.orEmpty()).distinct()
        exportWorksheetFromContextInternal(
            context = context,
            title = note.title,
            subject = note.subject,
            contextText = "${note.content}\nTanımlar: ${note.definitions.joinToString("; ")}",
            boardFormulas = boardFormulas
        )
    }

    private fun exportWorksheetFromContextInternal(
        context: Context,
        title: String,
        subject: String,
        contextText: String,
        boardFormulas: List<String>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingWorksheet = true) }
            try {
                val worksheet = aiService.generateWorksheetFromContext(
                    title = title,
                    subject = subject,
                    gradeLevel = _uiState.value.profile.gradeLevel,
                    contextText = contextText,
                    boardFormulas = boardFormulas
                )
                val file = PdfHelper.exportWorksheetPdf(context, worksheet)
                _uiState.update { it.copy(statusBanner = "A4 Çalışma Kağıdı hazırlandı ve İndirilenler'e kaydedildi.") }
                PdfHelper.sharePdfFile(context, file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "Çalışma kağıdı PDF oluşturulamadı: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isExportingWorksheet = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveManager.stopSession()
        teacherJob?.cancel()
        examTimerJob?.cancel()
        messagesJob?.cancel()
    }

    private companion object {
        const val TAG = "MrStudyViewModel"
        const val FIRESTORE_WRITE_TIMEOUT_MS = 15_000L
    }
}
