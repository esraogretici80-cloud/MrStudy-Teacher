package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.DailyLearningTask
import com.example.data.model.ExamRecord
import com.example.data.model.FlashcardItem
import com.example.data.model.PdfResourceRecord
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyPlanItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MrStudyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    // --- Profile ---
    fun observeProfile(userId: String): Flow<StudentProfile?> =
        userDoc(userId).snapshots().map { snapshot ->
            if (snapshot.exists()) {
                snapshot.toObject(StudentProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        }

    suspend fun ensureProfileExists(
        userId: String,
        displayName: String,
        email: String
    ): StudentProfile {
        val docRef = userDoc(userId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                return snap.toObject(StudentProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: StudentProfile(id = userId, userId = userId, displayName = displayName, email = email)
            }
            val safeName = displayName.trim().ifBlank { "Öğrenci" }.take(60)
            val payload = mapOf(
                "userId" to userId,
                "displayName" to safeName,
                "email" to email.take(120),
                "gradeLevel" to "9. Sınıf",
                "targetGoal" to "Düzenli tekrar ve sınav başarısı",
                "dailyGoalMinutes" to 45L,
                "xp" to 50L,
                "level" to 1L,
                "streakDays" to 1L,
                "totalStudyMinutes" to 0L,
                "questionsSolved" to 0L,
                "darkThemeMode" to "system",
                "preferredMode" to "Normal Öğretmen",
                "badges" to listOf("İlk Adım"),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return StudentProfile(
                id = userId,
                userId = userId,
                displayName = safeName,
                email = email,
                xp = 50L,
                badges = listOf("İlk Adım")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfile(userId: String, profile: StudentProfile) {
        val docRef = userDoc(userId)
        val payload = mapOf(
            "displayName" to profile.displayName.trim().ifBlank { "Öğrenci" }.take(60),
            "email" to profile.email.take(120),
            "gradeLevel" to profile.gradeLevel.ifBlank { "9. Sınıf" }.take(40),
            "targetGoal" to profile.targetGoal.take(120),
            "dailyGoalMinutes" to profile.dailyGoalMinutes.coerceIn(5L, 480L),
            "xp" to profile.xp.coerceAtLeast(0L),
            "level" to ((profile.xp.coerceAtLeast(0L) / 150L) + 1L),
            "streakDays" to profile.streakDays.coerceAtLeast(0L),
            "totalStudyMinutes" to profile.totalStudyMinutes.coerceAtLeast(0L),
            "questionsSolved" to profile.questionsSolved.coerceAtLeast(0L),
            "darkThemeMode" to profile.darkThemeMode.ifBlank { "system" },
            "preferredMode" to profile.preferredMode.ifBlank { "Normal Öğretmen" }.take(40),
            "badges" to profile.badges.take(30),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun awardProgress(
        userId: String,
        currentProfile: StudentProfile,
        xpDelta: Long,
        minutesDelta: Long = 0L,
        questionsDelta: Long = 0L,
        newBadge: String? = null
    ) {
        val updatedXp = (currentProfile.xp + xpDelta).coerceAtLeast(0L)
        val updatedBadges = if (!newBadge.isNullOrBlank() && !currentProfile.badges.contains(newBadge)) {
            (currentProfile.badges + newBadge).take(30)
        } else {
            currentProfile.badges
        }
        updateProfile(
            userId,
            currentProfile.copy(
                xp = updatedXp,
                level = (updatedXp / 150L) + 1L,
                totalStudyMinutes = currentProfile.totalStudyMinutes + minutesDelta,
                questionsSolved = currentProfile.questionsSolved + questionsDelta,
                badges = updatedBadges
            )
        )
    }

    // --- Chat Sessions & Messages ---
    fun observeChatSessions(userId: String): Flow<List<ChatSession>> =
        userDoc(userId).collection("chat_sessions")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatSession::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    fun observeChatMessages(userId: String, sessionId: String): Flow<List<ChatMessage>> =
        userDoc(userId).collection("chat_sessions").document(sessionId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun createChatSession(
        userId: String,
        title: String,
        subject: String,
        teachingMode: String,
        explanationDepth: String,
        sourcePdfId: String = "",
        sourcePdfTitle: String = "",
        sourcePage: Long = 0L
    ): String {
        val docRef = userDoc(userId).collection("chat_sessions").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().ifBlank { "Yeni Ders Oturumu" }.take(120),
            "subject" to subject.ifBlank { "Genel" }.take(60),
            "teachingMode" to teachingMode.ifBlank { "Normal Öğretmen" }.take(40),
            "explanationDepth" to explanationDepth.ifBlank { "normal" },
            "sourcePdfId" to sourcePdfId.take(128),
            "sourcePdfTitle" to sourcePdfTitle.take(120),
            "sourcePage" to sourcePage.coerceIn(0L, 5000L),
            "messageCount" to 0L,
            "lastMessagePreview" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateChatSessionMeta(
        userId: String,
        sessionId: String,
        teachingMode: String,
        explanationDepth: String,
        sourcePdfId: String,
        sourcePdfTitle: String,
        sourcePage: Long,
        lastMessagePreview: String? = null,
        incrementMessageCount: Boolean = false
    ) {
        val docRef = userDoc(userId).collection("chat_sessions").document(sessionId)
        val payload = mutableMapOf<String, Any>(
            "teachingMode" to teachingMode.take(40),
            "explanationDepth" to explanationDepth,
            "sourcePdfId" to sourcePdfId.take(128),
            "sourcePdfTitle" to sourcePdfTitle.take(120),
            "sourcePage" to sourcePage.coerceIn(0L, 5000L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (lastMessagePreview != null) {
            payload["lastMessagePreview"] = lastMessagePreview.take(300)
        }
        if (incrementMessageCount) {
            payload["messageCount"] = FieldValue.increment(1L)
        }
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun addChatMessage(
        userId: String,
        sessionId: String,
        role: String,
        content: String,
        teachingMode: String,
        hasAttachment: Boolean = false,
        attachmentLabel: String = "",
        sourceCitation: String = ""
    ) {
        val sessionRef = userDoc(userId).collection("chat_sessions").document(sessionId)
        val msgRef = sessionRef.collection("messages").document()
        val msgPayload = mapOf(
            "userId" to userId,
            "role" to role,
            "content" to content.trim().take(20000),
            "teachingMode" to teachingMode.take(40),
            "hasAttachment" to hasAttachment,
            "attachmentLabel" to attachmentLabel.take(120),
            "sourceCitation" to sourceCitation.take(200),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            msgRef.set(msgPayload).await()
            sessionRef.update(
                mapOf(
                    "lastMessagePreview" to content.trim().take(300),
                    "messageCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun deleteChatSession(userId: String, sessionId: String) {
        val docRef = userDoc(userId).collection("chat_sessions").document(sessionId)
        try {
            val msgs = docRef.collection("messages").get().await()
            msgs.documents.forEach { it.reference.delete().await() }
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- PDF Sources ---
    fun observePdfSources(userId: String): Flow<List<PdfResourceRecord>> =
        userDoc(userId).collection("pdf_sources")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PdfResourceRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun addPdfSource(
        userId: String,
        title: String,
        subject: String,
        localUriString: String,
        pageCount: Long,
        summary: String = "",
        topics: List<String> = emptyList()
    ): String {
        val docRef = userDoc(userId).collection("pdf_sources").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to title.trim().ifBlank { "PDF Kaynak" }.take(120),
            "subject" to subject.ifBlank { "Genel" }.take(60),
            "localUriString" to localUriString.take(1000),
            "pageCount" to pageCount.coerceIn(1L, 5000L),
            "lastStudiedPage" to 1L,
            "summary" to summary.take(8000),
            "topics" to topics.take(40),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePdfSource(userId: String, pdf: PdfResourceRecord) {
        require(pdf.id.isNotBlank())
        val docRef = userDoc(userId).collection("pdf_sources").document(pdf.id)
        val payload = mapOf(
            "title" to pdf.title.trim().ifBlank { "PDF Kaynak" }.take(120),
            "subject" to pdf.subject.ifBlank { "Genel" }.take(60),
            "localUriString" to pdf.localUriString.take(1000),
            "pageCount" to pdf.pageCount.coerceIn(1L, 5000L),
            "lastStudiedPage" to pdf.lastStudiedPage.coerceIn(1L, 5000L),
            "summary" to pdf.summary.take(8000),
            "topics" to pdf.topics.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deletePdfSource(userId: String, pdfId: String) {
        val docRef = userDoc(userId).collection("pdf_sources").document(pdfId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Exams ---
    fun observeExams(userId: String): Flow<List<ExamRecord>> =
        userDoc(userId).collection("exams")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ExamRecord::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun saveExamRecord(userId: String, record: ExamRecord): String {
        val docRef = userDoc(userId).collection("exams").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to record.title.trim().ifBlank { "Akıllı Sınav" }.take(120),
            "subject" to record.subject.ifBlank { "Genel" }.take(60),
            "topic" to record.topic.ifBlank { "Karışık" }.take(120),
            "difficulty" to record.difficulty.ifBlank { "Orta" }.take(30),
            "questionCount" to record.questionCount.coerceIn(1L, 50L),
            "correctCount" to record.correctCount.coerceIn(0L, 50L),
            "scorePercent" to record.scorePercent.coerceIn(0L, 100L),
            "durationSeconds" to record.durationSeconds.coerceIn(0L, 14400L),
            "sourceType" to record.sourceType.take(40),
            "weakTopics" to record.weakTopics.take(20),
            "errorCategories" to record.errorCategories.take(20),
            "aiReport" to record.aiReport.take(8000),
            "wrongQuestionsSummary" to record.wrongQuestionsSummary.take(8000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Study Plans ---
    fun observeStudyPlans(userId: String): Flow<List<StudyPlanItem>> =
        userDoc(userId).collection("study_plans")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudyPlanItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<StudyPlanItem> { it.isCompleted }.thenByDescending { it.createdAt ?: Timestamp(0, 0) })
            }

    suspend fun addStudyPlanItem(userId: String, item: StudyPlanItem): String {
        val docRef = userDoc(userId).collection("study_plans").document()
        val payload = mapOf(
            "userId" to userId,
            "subject" to item.subject.ifBlank { "Genel" }.take(60),
            "topic" to item.topic.trim().ifBlank { "Konu Tekrarı" }.take(120),
            "dayLabel" to item.dayLabel.ifBlank { "Bugün" }.take(40),
            "durationMinutes" to item.durationMinutes.coerceIn(5L, 300L),
            "priority" to item.priority.ifBlank { "Orta" }.take(30),
            "planType" to item.planType.ifBlank { "Ders" }.take(40),
            "isCompleted" to item.isCompleted,
            "postponedCount" to item.postponedCount.coerceIn(0L, 50L),
            "examDateText" to item.examDateText.take(40),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateStudyPlanItem(userId: String, item: StudyPlanItem) {
        require(item.id.isNotBlank())
        val docRef = userDoc(userId).collection("study_plans").document(item.id)
        val payload = mapOf(
            "subject" to item.subject.ifBlank { "Genel" }.take(60),
            "topic" to item.topic.trim().ifBlank { "Konu Tekrarı" }.take(120),
            "dayLabel" to item.dayLabel.ifBlank { "Bugün" }.take(40),
            "durationMinutes" to item.durationMinutes.coerceIn(5L, 300L),
            "priority" to item.priority.ifBlank { "Orta" }.take(30),
            "planType" to item.planType.ifBlank { "Ders" }.take(40),
            "isCompleted" to item.isCompleted,
            "postponedCount" to item.postponedCount.coerceIn(0L, 50L),
            "examDateText" to item.examDateText.take(40),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStudyPlanItem(userId: String, planId: String) {
        val docRef = userDoc(userId).collection("study_plans").document(planId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Notes ---
    fun observeNotes(userId: String): Flow<List<StudyNote>> =
        userDoc(userId).collection("notes")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudyNote::class.java, ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun addNote(userId: String, note: StudyNote): String {
        val docRef = userDoc(userId).collection("notes").document()
        val payload = mapOf(
            "userId" to userId,
            "title" to note.title.trim().ifBlank { "Ders Notu" }.take(120),
            "subject" to note.subject.ifBlank { "Genel" }.take(60),
            "content" to note.content.trim().ifBlank { "Not içeriği" }.take(20000),
            "formulas" to note.formulas.take(30),
            "definitions" to note.definitions.take(30),
            "keyConcepts" to note.keyConcepts.take(30),
            "sourcePdfTitle" to note.sourcePdfTitle.take(120),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateNote(userId: String, note: StudyNote) {
        require(note.id.isNotBlank())
        val docRef = userDoc(userId).collection("notes").document(note.id)
        val payload = mapOf(
            "title" to note.title.trim().ifBlank { "Ders Notu" }.take(120),
            "subject" to note.subject.ifBlank { "Genel" }.take(60),
            "content" to note.content.trim().ifBlank { "Not içeriği" }.take(20000),
            "formulas" to note.formulas.take(30),
            "definitions" to note.definitions.take(30),
            "keyConcepts" to note.keyConcepts.take(30),
            "sourcePdfTitle" to note.sourcePdfTitle.take(120),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNote(userId: String, noteId: String) {
        val docRef = userDoc(userId).collection("notes").document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Flashcards ---
    fun observeFlashcards(userId: String): Flow<List<FlashcardItem>> =
        userDoc(userId).collection("flashcards")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FlashcardItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.nextReviewAtMs }
            }

    suspend fun addFlashcard(userId: String, card: FlashcardItem): String {
        val docRef = userDoc(userId).collection("flashcards").document()
        val payload = mapOf(
            "userId" to userId,
            "subject" to card.subject.ifBlank { "Genel" }.take(60),
            "cardType" to card.cardType.ifBlank { "Kavram" }.take(40),
            "front" to card.front.trim().take(1000),
            "back" to card.back.trim().take(2000),
            "masteryStatus" to card.masteryStatus.ifBlank { "new" },
            "reviewCount" to card.reviewCount.coerceIn(0L, 1000L),
            "nextReviewAtMs" to card.nextReviewAtMs.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateFlashcardReview(userId: String, card: FlashcardItem, learned: Boolean) {
        require(card.id.isNotBlank())
        val docRef = userDoc(userId).collection("flashcards").document(card.id)
        val newCount = (card.reviewCount + 1L).coerceAtMost(1000L)
        val intervalMs = if (learned) {
            // Spaced repetition interval: 1d, 3d, 7d, 14d...
            val days = when (newCount.toInt()) {
                1 -> 1L
                2 -> 3L
                3 -> 7L
                else -> 14L
            }
            System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
        } else {
            // Needs review soon (10 minutes)
            System.currentTimeMillis() + 10L * 60L * 1000L
        }
        val payload = mapOf(
            "subject" to card.subject.take(60),
            "cardType" to card.cardType.take(40),
            "front" to card.front.take(1000),
            "back" to card.back.take(2000),
            "masteryStatus" to if (learned) "learned" else "review",
            "reviewCount" to newCount,
            "nextReviewAtMs" to intervalMs,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFlashcard(userId: String, cardId: String) {
        val docRef = userDoc(userId).collection("flashcards").document(cardId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Daily Learning Tasks ---
    fun observeDailyTasks(userId: String): Flow<List<DailyLearningTask>> =
        userDoc(userId).collection("daily_tasks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DailyLearningTask::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.isCompleted }
            }

    suspend fun ensureTodayTasksExist(userId: String) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val col = userDoc(userId).collection("daily_tasks")
        try {
            val existing = col.whereEqualTo("dateKey", dateKey).get().await()
            if (existing.isEmpty) {
                val starterTasks = listOf(
                    Triple("AI Öğretmenle 1 Konu Çalış", "Normal, Sokratik veya Keşif modunda bir soru sor.", 35L),
                    Triple("Akıllı Sınav veya Mini Quiz Çöz", "Seviyene uygun en az 1 test tamamla.", 50L),
                    Triple("3 Bilgi Kartı Tekrar Et", "Aralıklı tekrar sisteminden kartlarını gözden geçir.", 25L)
                )
                starterTasks.forEach { (title, desc, xp) ->
                    col.document().set(
                        mapOf(
                            "userId" to userId,
                            "title" to title,
                            "description" to desc,
                            "xpReward" to xp,
                            "dateKey" to dateKey,
                            "isCompleted" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
        }
    }

    suspend fun toggleDailyTask(userId: String, task: DailyLearningTask) {
        require(task.id.isNotBlank())
        val docRef = userDoc(userId).collection("daily_tasks").document(task.id)
        val payload = mapOf(
            "title" to task.title.take(120),
            "description" to task.description.take(300),
            "xpReward" to task.xpReward.coerceIn(5L, 500L),
            "dateKey" to task.dateKey.take(20),
            "isCompleted" to !task.isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
