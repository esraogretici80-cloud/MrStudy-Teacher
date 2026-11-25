package com.example.data.model

import java.util.UUID

enum class FlashcardReviewRating(
    val label: String,
    val firestoreStatus: String,
    val intervalDays: Long
) {
    HARD("Zorlanıyorum", "new", 0L),
    REVIEW("Tekrar Edilmeli", "review", 1L),
    GOOD("Biliyorum", "learned", 3L),
    MASTERED("Öğrenildi", "learned", 7L)
}

enum class GameGenre(
    val label: String,
    val subtitle: String,
    val defaultHero: String,
    val defaultEnvironment: String
) {
    SPACE_PLATFORM("3D Uzay Platform", "Yörünge kapılarından geç, bilgi topla", "Astro Robot M4", "Derin Uzay İstasyonu"),
    QUIZ_RACE("Bilgi Yarışması", "Hızlı yanıtlarla kombo serisi yap", "Bilim Kaptanı", "Neon Arena"),
    WORD_MATCH("Kelime Oyunu", "Kavram ve terim eşleştirme düellosu", "Dil Kaşifi", "Kütüphane Evreni"),
    MATH_SPEED("Matematik Hız", "Zamana karşı işlem ve denklem yarışı", "Kuantum Hesaplayıcı", "Siber Matris"),
    MEMORY_PAIR("Hafıza Eşleştirme", "Kart çiftlerini en az hamlede eşleştir", "Zihin Muhafızı", "Hologram Laboratuvarı"),
    PUZZLE_SORT("Bulmaca & Sıralama", "Doğru mantık sırasını keşfet", "Analiz Uzmanı", "Mantık Kulesi"),
    SCIENCE_EXPLORE("Bilim Keşfi", "Deney ve doğa olaylarını çöz", "Uzay Biyoloğu", "Yörünge Gözlemevi"),
    QUESTION_ADVENTURE("Soru Macerası", "Bölümleri geçerek rozet kazan", "Akademi Şampiyonu", "Bilgi Adası")
}

data class Custom3dGameConfig(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Uzay Bilgi Görevi",
    val subject: String = "Matematik",
    val topic: String = "Denklemler",
    val genre: GameGenre = GameGenre.SPACE_PLATFORM,
    val heroName: String = genre.defaultHero,
    val environmentName: String = genre.defaultEnvironment,
    val ruleSummary: String = "Her doğru cevap +100 puan ve kombo kazandırır.",
    val questions: List<Game3dQuestion> = emptyList(),
    val wordPairs: List<Pair<String, String>> = emptyList(),
    val highScore: Int = 0,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class DiagramType(val label: String, val subtitle: String) {
    CONCEPT_MAP("Kavram Haritası", "Kavramlar arası hiyerarşik ilişkiler"),
    MIND_MAP("Zihin Haritası", "Merkez fikirden yayılan dallar"),
    FLOWCHART("Akış Şeması", "Adım adım karar ve süreç akışı"),
    INFOGRAPHIC("İnfografik Özet", "Başlıklar ve anahtar veri blokları"),
    TIMELINE("Zaman Çizelgesi", "Tarihsel veya aşamalı sıralama"),
    VENN_COMPARE("Karşılaştırma / Venn", "Ortak ve farklı özellikler"),
    CAUSE_EFFECT("Neden - Sonuç Şeması", "Etkenler ve sonuç zinciri"),
    SCIENCE_SCHEMA("Bilim / Biyoloji / Fizik", "Deney, kuvvet veya sistem şeması")
}

data class DiagramNode(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val note: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val colorHex: Long = 0xFF8B5CF6
)

data class DiagramEdge(
    val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val toId: String,
    val label: String = ""
)

data class DiagramProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Yeni Diyagram",
    val subject: String = "Genel",
    val diagramType: DiagramType = DiagramType.CONCEPT_MAP,
    val nodes: List<DiagramNode> = emptyList(),
    val edges: List<DiagramEdge> = emptyList(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

enum class ScienceLabModule(
    val title: String,
    val subtitle: String,
    val subject: String
) {
    SOLAR_SYSTEM("Güneş Sistemi ve Gezegenler", "Yörünge hızı, güneş ve gezegen seçimi", "Fen Bilimleri"),
    ECLIPSE_SIM("Ay ve Güneş Tutulması", "Evre açısı, gölge konisi ve tutulma", "Fen Bilimleri"),
    FORCE_MOTION("Kuvvet ve Hareket", "F = m·a kütle, kuvvet ve sürtünme", "Fizik"),
    PLANT_CELL("Hücre Organelleri (3B)", "Çekirdek, mitokondri, kloroplast incele", "Biyoloji"),
    HUMAN_ORGANS("İnsan Vücudu Organları", "Kalp, akciğer, beyin ve sindirim sistemi", "Biyoloji"),
    ELECTRIC_CIRCUIT("Elektrik Devreleri", "Anahtar, gerilim, direnç ve akım şiddeti", "Fizik"),
    MATTER_STATES("Maddenin Hâlleri", "Katı, sıvı, gaz ve sıcaklık titreşimi", "Kimya"),
    GEOMETRY_3D("Geometrik Cisimler (3B)", "Küp, piramit, prizma, koni ve silindir", "Matematik")
}

enum class AiToolType(
    val title: String,
    val subtitle: String,
    val category: String,
    val inputHint: String
) {
    MATH_SOLVER("Matematik Çözücü", "Yazı veya fotoğraftan adım adım çözüm", "Soru & Çözüm", "Denklem veya soruyu yaz ya da fotoğraf ekle"),
    EXAM_PREP("Yazılı Hazırlayıcı", "Okul yazılısı provası & püf noktaları", "Sınav", "Yazılı konularını ve sınıfını yaz"),
    HOMEWORK_HELPER("Ödev Yardımcısı", "İpucu ver, öğrenci cevabını kontrol et", "Soru & Çözüm", "Ödev sorusu ve varsa kendi çözümün"),
    TEXT_SUMMARIZER("Metin Özetleyici", "Uzun metni madde madde özete çevir", "Üretkenlik", "Özetlenecek metni buraya yapıştır"),
    PRESENTATION_STUDIO("Sunum Stüdyosu", "Slayt taslağı, içerik & PDF dışa aktarım", "Üretkenlik", "Sunum konusunu ve hedef kitlesini yaz"),
    LESSON_NOTE_MAKER("Ders Notu Oluşturucu", "Kapsamlı konu notu ve tanımlar üret", "Not & Plan", "Hangi ders ve konudan not üretilsin?"),
    VOICE_NOTE("Sesli Not", "Sesini metne çevir ve AI ile düzenle", "Not & Plan", "Mikrofona konuş veya sesli not metnini yaz"),
    QUESTION_BANK("Soru Bankası", "Konuya özel seviyeli pratik soru seti", "Sınav", "Ders, konu ve soru zorluğunu yaz"),
    LEARNING_COACH("Öğrenme Koçu", "Günlük ve haftalık kişisel koçluk planı", "Rehberlik", "Hedefin, boş saatlerin ve zorlandığın ders"),
    TRANSLATOR("Çevirmen", "Akademik Türkçe ↔ İngilizce & dil çevirisi", "Dil", "Çevrilecek metin ve hedef dili belirt"),
    TOPIC_MAP("Konu Haritası", "Ünitenin ön koşulları ve alt başlık ağacı", "Not & Plan", "Haritasını çıkaracağın ünite adını yaz"),
    WEAKNESS_ANALYZER("Eksik Analizi", "Sınav sonuçlarından zayıf konu reçetesi", "Rehberlik", "Eklemek istediğin özel not veya hedefi yaz")
}

data class MathSolutionResult(
    val detectedProblem: String,
    val finalAnswer: String,
    val steps: List<String>,
    val formulasUsed: List<String>,
    val similarPracticeQuestion: String,
    val similarPracticeAnswer: String
)

data class PresentationSlide(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val bullets: List<String>,
    val speakerNotes: String = ""
)

data class PresentationDeck(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val slides: List<PresentationSlide>,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class VoiceNoteRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val rawTranscript: String,
    val cleanedSummary: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class CodingLanguage(
    val displayName: String,
    val extension: String,
    val isLiveRunnable: Boolean,
    val runtimeBadge: String,
    val starterTemplate: String
) {
    PYTHON(
        "Python",
        "py",
        true,
        "Yerel Python Mini Yorumlayıcı",
        "print(\"MrStudy 4.0 Kodlama Stüdyosu!\")\nfor i in range(1, 6):\n    print(\"Kare:\", i, \"->\", i * i)\n"
    ),
    JAVASCRIPT(
        "JavaScript",
        "js",
        true,
        "WebView JS Konsol",
        "console.log(\"Merhaba MrStudy 4.0!\");\nconst toplam = [2, 4, 6, 8].reduce((a, b) => a + b, 0);\nconsole.log(\"Toplam:\", toplam);"
    ),
    HTML(
        "HTML",
        "html",
        true,
        "WebView Canlı Önizleme",
        "<!DOCTYPE html>\n<html><body style=\"background:#0B1120;color:#F8FAFC;font-family:sans-serif;padding:20px\">\n<h2 style=\"color:#22D3EE\">MrStudy 4.0 Web Kartı</h2>\n<p>Etkileşimli HTML çıktın burada canlı görünür.</p>\n</body></html>"
    ),
    CSS(
        "CSS",
        "css",
        true,
        "WebView Stil Önizleme",
        ".card {\n  background: linear-gradient(135deg, #8B5CF6, #22D3EE);\n  color: white;\n  padding: 20px;\n  border-radius: 18px;\n  font-family: sans-serif;\n}"
    ),
    JAVA(
        "Java",
        "java",
        false,
        "AI Kuru Çalıştırma / Analiz",
        "public class Main {\n  public static void main(String[] args) {\n    int sonuc = 12 * 4;\n    System.out.println(\"Sonuç: \" + sonuc);\n  }\n}"
    ),
    KOTLIN(
        "Kotlin",
        "kt",
        false,
        "AI Kuru Çalıştırma / Analiz",
        "fun main() {\n    val notlar = listOf(85, 92, 98)\n    println(\"Ortalama: \${notlar.average()}\")\n}"
    ),
    C(
        "C",
        "c",
        false,
        "AI Kuru Çalıştırma / Analiz",
        "#include <stdio.h>\nint main() {\n    printf(\"MrStudy 4.0 C Dili\\n\");\n    return 0;\n}"
    ),
    CPP(
        "C++",
        "cpp",
        false,
        "AI Kuru Çalıştırma / Analiz",
        "#include <iostream>\nint main() {\n    std::cout << \"MrStudy 4.0 C++\" << std::endl;\n    return 0;\n}"
    )
}

data class CodeProjectFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val language: CodingLanguage,
    val code: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class SavedMathBoard(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val expression: String,
    val solutionSummary: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class AudioLessonItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subject: String,
    val scriptText: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class AppNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val tag: String = "Sistem",
    val timestampMs: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
