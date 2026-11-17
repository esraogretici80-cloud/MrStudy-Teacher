package com.example.ui.state

import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.LocalParentMetrics
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.DailyLearningTask
import com.example.data.model.ExamRecord
import com.example.data.model.ExamResultSummary
import com.example.data.model.ExplanationDepth
import com.example.data.model.FlashcardItem
import com.example.data.model.Game3dQuestion
import com.example.data.model.GeneratedExam
import com.example.data.model.MiniQuizQuestion
import com.example.data.model.PdfResourceRecord
import com.example.data.model.SmartBoardPayload
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyPlanItem
import com.example.data.model.TeachingMode

data class AttachedImage(
    val uri: Uri,
    val bitmap: Bitmap,
    val label: String
)

sealed interface ExamUiState {
    data object Config : ExamUiState
    data object Generating : ExamUiState
    data class InProgress(
        val exam: GeneratedExam,
        val answers: Map<Int, String> = emptyMap(),
        val remainingSeconds: Int = 0,
        val elapsedSeconds: Int = 0
    ) : ExamUiState
    data object Grading : ExamUiState
    data class Result(val summary: ExamResultSummary) : ExamUiState
}

data class MrStudyUiState(
    val profile: StudentProfile = StudentProfile(),
    val isProfileLoaded: Boolean = false,
    val showOnboardingDialog: Boolean = false,
    val showProfileSheet: Boolean = false,

    // Teacher Chat
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val selectedSubject: String = "Genel",
    val selectedMode: TeachingMode = TeachingMode.NORMAL,
    val selectedDepth: ExplanationDepth = ExplanationDepth.NORMAL,
    val attachedImages: List<AttachedImage> = emptyList(),
    val isTeacherStreaming: Boolean = false,
    val streamingReply: String = "",
    val teacherError: String? = null,
    val teacherAppCheckToken: String? = null,

    // PDF Studio
    val pdfSources: List<PdfResourceRecord> = emptyList(),
    val activePdfId: String? = null,
    val activePdfPage: Int = 1,
    val renderedPdfPage: Bitmap? = null,
    val isPdfBusy: Boolean = false,
    val pdfImportProgressText: String? = null,
    val pdfError: String? = null,

    // Smart Exam Center
    val examHistory: List<ExamRecord> = emptyList(),
    val examUiState: ExamUiState = ExamUiState.Config,
    val examError: String? = null,

    // Study Plans
    val studyPlans: List<StudyPlanItem> = emptyList(),
    val isGeneratingPlan: Boolean = false,

    // Smart Notes
    val notes: List<StudyNote> = emptyList(),
    val isGeneratingNote: Boolean = false,

    // Flashcards
    val flashcards: List<FlashcardItem> = emptyList(),
    val isGeneratingCards: Boolean = false,

    // Daily Tasks & Mini Quiz
    val dailyTasks: List<DailyLearningTask> = emptyList(),
    val miniQuizQuestions: List<MiniQuizQuestion> = emptyList(),
    val isLoadingMiniQuiz: Boolean = false,

    // MrStudy 3.0: Live Smart Board, Mid-Lesson Quiz, 3D Game, Worksheet & Parent Panel
    val smartBoardState: SmartBoardPayload? = null,
    val isGeneratingSmartBoard: Boolean = false,
    val midLessonQuestion: MiniQuizQuestion? = null,
    val isLoadingMidQuestion: Boolean = false,
    val liveSessionEndedShowGameCta: Boolean = false,
    val game3dQuestions: List<Game3dQuestion> = emptyList(),
    val game3dTopic: String = "",
    val isLoadingGame3d: Boolean = false,
    val isExportingWorksheet: Boolean = false,
    val parentMetrics: LocalParentMetrics = LocalParentMetrics(),

    // Global banner/feedback
    val statusBanner: String? = null
) {
    val activePdf: PdfResourceRecord?
        get() = pdfSources.firstOrNull { it.id == activePdfId }
}
