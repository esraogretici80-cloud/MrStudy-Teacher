package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class TeachingMode(
    val title: String,
    val subtitle: String,
    val promptGuidance: String
) {
    NORMAL(
        title = "Normal Öğretmen",
        subtitle = "Basitten zora, anlaşılır ve dengeli anlatım",
        promptGuidance = "Öğrenciye gerçek bir öğretmen gibi sıcak, sabırlı ve yapılandırılmış şekilde basitten zora doğru konu anlat. Günlük hayattan örnekler ver, sonunda kısa bir öğrenme kontrolü sorusu sor."
    ),
    FAST(
        title = "Hızlı Öğretmen",
        subtitle = "Kısa, öz ve ana noktalar odaklı",
        promptGuidance = "Konuyu en kritik kavramlar, kurallar ve kısa örneklerle net, öz ve akıcı şekilde anlat. Gereksiz uzatmalardan kaçın."
    ),
    EXAM(
        title = "Sınav Öğretmeni",
        subtitle = "Yazılı, LGS/YKS soru tipleri ve tuzaklar",
        promptGuidance = "Konuyu sınavlarda en çok çıkan soru kalıpları, dikkat edilmesi gereken tuzaklar, puan getiren çözüm stratejileri ve örnek sınav sorusuyla anlat."
    ),
    SOLVER(
        title = "Soru Çözme",
        subtitle = "Adım adım mantıklı çözüm",
        promptGuidance = "Soruyu adım adım çöz. Önce verilenleri ve isteneni çıkar, kullanılacak kuralı/formülü belirt, her işlem basamağını gerekçesiyle açıkla ve sonucu kontrol et."
    ),
    REVIEW(
        title = "Tekrar Modu",
        subtitle = "Önceki bilgileri tazeleme ve pekiştirme",
        promptGuidance = "Daha önce öğrenilmiş konuyu hızlı hatırlatma kartları, özet formüller ve 2 adet pekiştirici mini soruyla tazele."
    ),
    DISCOVERY(
        title = "Keşif Modu",
        subtitle = "Günlük hayat ve merak uyandıran örnekler",
        promptGuidance = "Konuyu günlük hayattan, doğadan, teknolojiden veya spordan somut örnekler ve deneylerle keşfettirerek anlat. Ezber yerine mantığı kavrat."
    ),
    SOCRATIC(
        title = "Sokratik Mod",
        subtitle = "Doğrudan cevap vermeden düşündürerek öğretim",
        promptGuidance = "Öğrenciye doğrudan nihai cevabı verme. Onu küçük ipuçları ve yönlendirici sorularla kendi çözümüne ulaşmaya teşvik et. Yanlış yaparsa yargılamadan nedenini buldur."
    );

    companion object {
        fun fromTitle(title: String): TeachingMode =
            entries.firstOrNull { it.title == title } ?: NORMAL
    }
}

enum class ExplanationDepth(val key: String, val label: String) {
    SHORT("kisa", "Kısa"),
    NORMAL("normal", "Normal"),
    DETAILED("detayli", "Detaylı");

    companion object {
        fun fromKey(key: String): ExplanationDepth =
            entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

enum class ErrorCategory(val title: String, val strategy: String) {
    KNOWLEDGE_GAP("Bilgi Eksikliği", "Temel kavramları sıfırdan, sade örneklerle yeniden anlat."),
    CALCULATION_ERROR("İşlem Hatası", "Adım adım işlem kontrolü ve ara basamak yazma alışkanlığı kazandır."),
    MISREADING("Soruyu Yanlış Anlama", "Soru kökündeki anahtar kelimeleri ve olumsuzluk eklerini altını çizerek analiz ettir."),
    ATTENTION_SLIP("Dikkat Hatası", "Birim dönüşümleri, işaretler ve şık eleme kontrol listesi uygula."),
    MISCONCEPTION("Kavram Yanılgısı", "Yanlış sezgisel düşünceyi karşıt örnek ve günlük hayat benzetmesiyle düzelt.")
}

enum class ExamDifficulty(val label: String) {
    EASY("Kolay"),
    MEDIUM("Orta"),
    HARD("Zor"),
    EXPERT("Uzman")
}

enum class QuestionType(val label: String) {
    MULTIPLE_CHOICE("Çoktan Seçmeli"),
    TRUE_FALSE("Doğru-Yanlış"),
    FILL_BLANK("Boşluk Doldurma"),
    OPEN_ENDED("Açık Uçlu"),
    CLASSIC_WRITTEN("Klasik Yazılı")
}

object SubjectCatalog {
    val subjects = listOf(
        "Genel",
        "Matematik",
        "Türkçe / Edebiyat",
        "Fizik",
        "Kimya",
        "Biyoloji",
        "Tarih",
        "Coğrafya",
        "İngilizce",
        "Fen Bilimleri",
        "Sosyal Bilgiler"
    )

    val gradeLevels = listOf(
        "5. Sınıf",
        "6. Sınıf",
        "7. Sınıf",
        "8. Sınıf (LGS)",
        "9. Sınıf",
        "10. Sınıf",
        "11. Sınıf",
        "12. Sınıf (YKS)",
        "Üniversite / Akademik"
    )
}

@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Öğrenci",
    val email: String = "",
    val gradeLevel: String = "9. Sınıf",
    val targetGoal: String = "Konuları derinlemesine öğrenmek ve sınav başarımı artırmak",
    val dailyGoalMinutes: Long = 45L,
    val xp: Long = 0L,
    val level: Long = 1L,
    val streakDays: Long = 1L,
    val totalStudyMinutes: Long = 0L,
    val questionsSolved: Long = 0L,
    val darkThemeMode: String = "system",
    val preferredMode: String = TeachingMode.NORMAL.title,
    val badges: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Yeni Ders Oturumu",
    val subject: String = "Genel",
    val teachingMode: String = TeachingMode.NORMAL.title,
    val explanationDepth: String = ExplanationDepth.NORMAL.key,
    val sourcePdfId: String = "",
    val sourcePdfTitle: String = "",
    val sourcePage: Long = 0L,
    val messageCount: Long = 0L,
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val teachingMode: String = TeachingMode.NORMAL.title,
    val hasAttachment: Boolean = false,
    val attachmentLabel: String = "",
    val sourceCitation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PdfResourceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "Genel",
    val localUriString: String = "",
    val pageCount: Long = 1L,
    val lastStudiedPage: Long = 1L,
    val summary: String = "",
    val topics: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExamRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "Genel",
    val topic: String = "",
    val difficulty: String = "Orta",
    val questionCount: Long = 5L,
    val correctCount: Long = 0L,
    val scorePercent: Long = 0L,
    val durationSeconds: Long = 0L,
    val sourceType: String = "Konu",
    val weakTopics: List<String> = emptyList(),
    val errorCategories: List<String> = emptyList(),
    val aiReport: String = "",
    val wrongQuestionsSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyPlanItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "Matematik",
    val topic: String = "",
    val dayLabel: String = "Bugün",
    val durationMinutes: Long = 40L,
    val priority: String = "Orta",
    val planType: String = "Ders",
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val postponedCount: Long = 0L,
    val examDateText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "Genel",
    val content: String = "",
    val formulas: List<String> = emptyList(),
    val definitions: List<String> = emptyList(),
    val keyConcepts: List<String> = emptyList(),
    val sourcePdfTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FlashcardItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "Genel",
    val cardType: String = "Kavram",
    val front: String = "",
    val back: String = "",
    val masteryStatus: String = "new", // new, review, learned
    val reviewCount: Long = 0L,
    val nextReviewAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyLearningTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Long = 30L,
    val dateKey: String = "",
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// In-memory Exam session models
data class ExamQuestion(
    val number: Int,
    val type: QuestionType,
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val subTopic: String
)

data class GeneratedExam(
    val title: String,
    val subject: String,
    val topic: String,
    val difficulty: ExamDifficulty,
    val sourceType: String,
    val timedMinutes: Int,
    val questions: List<ExamQuestion>
)

data class EvaluatedQuestionResult(
    val question: ExamQuestion,
    val userAnswer: String,
    val isCorrect: Boolean,
    val aiFeedback: String,
    val errorCategory: String? // Null if correct
)

data class ExamResultSummary(
    val title: String,
    val subject: String,
    val topic: String,
    val difficulty: String,
    val sourceType: String,
    val durationSeconds: Int,
    val correctCount: Int,
    val totalCount: Int,
    val scorePercent: Int,
    val results: List<EvaluatedQuestionResult>,
    val overallReport: String,
    val weakTopics: List<String>,
    val errorCategories: List<String>,
    val wrongQuestionsSummary: String
)

data class MiniQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

enum class SmartBoardElementType {
    TRIANGLE,
    GRAPH,
    FORMULA,
    DIAGRAM;

    companion object {
        fun fromString(raw: String): SmartBoardElementType =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: FORMULA
    }
}

data class SmartBoardElement(
    val type: SmartBoardElementType = SmartBoardElementType.FORMULA,
    val title: String = "",
    val primaryText: String = "",
    val labels: List<String> = emptyList(),
    val values: List<Float> = emptyList()
)

data class SmartBoardPayload(
    val boardTitle: String = "Dijital Akıllı Tahta",
    val summaryNote: String = "",
    val formulas: List<String> = emptyList(),
    val elements: List<SmartBoardElement> = emptyList()
)

data class Game3dQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class WorksheetPracticeQuestion(
    val number: Int,
    val question: String,
    val options: List<String> = emptyList(),
    val answerKey: String
)

data class WorksheetData(
    val title: String,
    val subject: String,
    val gradeLevel: String,
    val summary: String,
    val formulas: List<String>,
    val questions: List<WorksheetPracticeQuestion>
)

