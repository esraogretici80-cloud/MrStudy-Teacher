package com.example.data.ai

import android.graphics.Bitmap
import com.example.data.model.AiToolType
import com.example.data.model.CodingLanguage
import com.example.data.model.Custom3dGameConfig
import com.example.data.model.DiagramEdge
import com.example.data.model.DiagramNode
import com.example.data.model.DiagramProject
import com.example.data.model.DiagramType
import com.example.data.model.Game3dQuestion
import com.example.data.model.GameGenre
import com.example.data.model.MathSolutionResult
import com.example.data.model.PresentationDeck
import com.example.data.model.PresentationSlide
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

suspend fun MrStudyAiService.generateDiagramProject(
    topic: String,
    diagramType: DiagramType,
    subject: String,
    gradeLevel: String
): DiagramProject {
    val nodeSchema = Schema.obj(
        properties = mapOf(
            "id" to Schema.string("Kısa benzersiz kimlik örn n1, n2"),
            "label" to Schema.string("Düğüm başlığı (1-5 kelime)"),
            "note" to Schema.string("1 kısa açıklama cümlesi"),
            "x" to Schema.float("Tuvalde yatay konum 0.12 ile 0.88 arası"),
            "y" to Schema.float("Tuvalde dikey konum 0.12 ile 0.88 arası")
        )
    )
    val edgeSchema = Schema.obj(
        properties = mapOf(
            "fromId" to Schema.string("Başlangıç düğüm id"),
            "toId" to Schema.string("Hedef düğüm id"),
            "label" to Schema.string("Bağlantı etiketi (1-3 kelime)")
        )
    )
    val rootSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string("Diyagram başlığı"),
            "nodes" to Schema.array(nodeSchema, "5 ile 8 arası düzenli konumlandırılmış düğüm"),
            "edges" to Schema.array(edgeSchema, "Düğümleri bağlayan oklar")
        )
    )
    val prompt = content {
        text(
            """
            $gradeLevel düzeyi '$subject' dersi '$topic' konusu için bir '${diagramType.label}' diyagramı oluştur.
            Düğümlerin x ve y koordinatları 0.14..0.86 aralığında çakışmayacak biçimde düzenli yerleştirilsin.
            Tüm içerik net Türkçe olsun.
            """.trimIndent()
        )
    }
    val raw = generateJsonWithSchema(rootSchema, prompt)
    val json = JSONObject(raw)
    val palette = listOf(0xFF8B5CF6L, 0xFF3B82F6L, 0xFF22D3EEL, 0xFF22C55EL, 0xFFF59E0BL, 0xFFEC4899L)
    val nArr = json.optJSONArray("nodes")
    val nodes = mutableListOf<DiagramNode>()
    if (nArr != null) {
        val count = nArr.length().coerceAtLeast(1)
        for (i in 0 until count) {
            val o = nArr.getJSONObject(i)
            val fallbackAngle = (2.0 * Math.PI * i / count).toFloat()
            val fx = (0.5f + 0.30f * cos(fallbackAngle)).coerceIn(0.14f, 0.86f)
            val fy = (0.5f + 0.30f * sin(fallbackAngle)).coerceIn(0.14f, 0.86f)
            nodes.add(
                DiagramNode(
                    id = o.optString("id", "n${i + 1}").ifBlank { "n${i + 1}" },
                    label = o.optString("label", "Kavram ${i + 1}"),
                    note = o.optString("note", ""),
                    x = o.optDouble("x", fx.toDouble()).toFloat().coerceIn(0.10f, 0.90f),
                    y = o.optDouble("y", fy.toDouble()).toFloat().coerceIn(0.10f, 0.90f),
                    colorHex = palette[i % palette.size]
                )
            )
        }
    }
    val eArr = json.optJSONArray("edges")
    val validIds = nodes.map { it.id }.toSet()
    val edges = if (eArr != null) {
        (0 until eArr.length()).mapNotNull { i ->
            val o = eArr.getJSONObject(i)
            val f = o.optString("fromId")
            val t = o.optString("toId")
            if (f in validIds && t in validIds && f != t) {
                DiagramEdge(fromId = f, toId = t, label = o.optString("label", ""))
            } else null
        }
    } else emptyList()

    return DiagramProject(
        title = json.optString("title", "$topic • ${diagramType.label}"),
        subject = subject,
        diagramType = diagramType,
        nodes = nodes,
        edges = edges
    )
}

suspend fun MrStudyAiService.explainDiagramNode(
    nodeLabel: String,
    diagramTopic: String,
    subject: String,
    gradeLevel: String,
    actionType: String
): String {
    val prompt = content {
        text(
            "$gradeLevel '$subject' dersindeki '$diagramTopic' diyagramının '$nodeLabel' kutusu için '$actionType' üret. " +
                "Kısa, öğrenci dostu, net ve Türkçe yanıt ver."
        )
    }
    return generatePlainTextWithFallback(prompt)
}

suspend fun MrStudyAiService.generateCustom3dGameContent(
    genre: GameGenre,
    heroName: String,
    environmentName: String,
    topic: String,
    ruleSummary: String,
    subject: String,
    gradeLevel: String
): Custom3dGameConfig {
    val qSchema = Schema.obj(
        properties = mapOf(
            "prompt" to Schema.string("Kısa oyun sorusu (en fazla 14 kelime)"),
            "options" to Schema.array(Schema.string(), "Tam 4 kısa şık"),
            "correctIndex" to Schema.integer("Doğru şık 0..3"),
            "explanation" to Schema.string("1 kısa cümle açıklama")
        )
    )
    val pairSchema = Schema.obj(
        properties = mapOf(
            "left" to Schema.string("Kavram veya ifade (en fazla 4 kelime)"),
            "right" to Schema.string("Karşılığı veya tanımı (en fazla 5 kelime)")
        )
    )
    val rootSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string("Yaratıcı Türkçe oyun başlığı"),
            "questions" to Schema.array(qSchema, "Tam 6 adet konuya özel oyun sorusu"),
            "wordPairs" to Schema.array(pairSchema, "Tam 6 adet eşleştirme/hafıza çifti")
        )
    )
    val prompt = content {
        text(
            """
            MrStudy 4.0 Oyun Stüdyosu için $gradeLevel '$subject - $topic' konusunda '${genre.label}' oyunu üret.
            Karakter: $heroName | Ortam: $environmentName | Kural: $ruleSummary
            Sorular hızlı oynanabilir ve kısa olsun. Ayrıca kelime/hafıza modu için 6 eşleştirme çifti ver.
            """.trimIndent()
        )
    }
    val raw = generateJsonWithSchema(rootSchema, prompt, tiers = MrStudyAiService.GAME_3D_TIERS)
    val json = JSONObject(raw)
    val qArr = json.optJSONArray("questions")
    val questions = if (qArr != null) {
        (0 until qArr.length()).map { i ->
            val o = qArr.getJSONObject(i)
            val opts = o.optJSONArray("options")?.let { oa -> (0 until oa.length()).map { oa.getString(it) } }?.take(4).orEmpty()
            Game3dQuestion(
                prompt = o.optString("prompt", "Soru ${i + 1}"),
                options = if (opts.size == 4) opts else listOf("A", "B", "C", "D"),
                correctIndex = o.optInt("correctIndex", 0).coerceIn(0, 3),
                explanation = o.optString("explanation", "")
            )
        }
    } else emptyList()

    val pArr = json.optJSONArray("wordPairs")
    val pairs = if (pArr != null) {
        (0 until pArr.length()).map { i ->
            val o = pArr.getJSONObject(i)
            o.optString("left", "Terim ${i + 1}") to o.optString("right", "Tanım ${i + 1}")
        }
    } else emptyList()

    return Custom3dGameConfig(
        title = json.optString("title", "$topic Macerası"),
        subject = subject,
        topic = topic,
        genre = genre,
        heroName = heroName,
        environmentName = environmentName,
        ruleSummary = ruleSummary,
        questions = questions,
        wordPairs = pairs
    )
}

suspend fun MrStudyAiService.solveMathProblemStructured(
    problemText: String,
    imageBitmap: Bitmap?,
    gradeLevel: String
): MathSolutionResult {
    val rootSchema = Schema.obj(
        properties = mapOf(
            "detectedProblem" to Schema.string("Çözülen sorunun temiz yazılışı"),
            "finalAnswer" to Schema.string("Net final sonuç (örn: x = 4)"),
            "steps" to Schema.array(Schema.string(), "Adım adım çözüm basamakları"),
            "formulasUsed" to Schema.array(Schema.string(), "Kullanılan kural veya formüller"),
            "similarPracticeQuestion" to Schema.string("Öğrencinin çözmesi için benzer 1 alıştırma sorusu"),
            "similarPracticeAnswer" to Schema.string("Benzer alıştırma sorusunun cevabı")
        )
    )
    val prompt = content {
        text(
            "$gradeLevel düzeyindeki bu matematik sorusunu adım adım çöz, kuralı belirt ve benzer bir pratik soru üret.\nSoru metni: ${problemText.ifBlank { "Görseldeki soruyu analiz et" }}"
        )
        if (imageBitmap != null) image(imageBitmap)
    }
    val raw = generateJsonWithSchema(rootSchema, prompt)
    val json = JSONObject(raw)
    return MathSolutionResult(
        detectedProblem = json.optString("detectedProblem", problemText),
        finalAnswer = json.optString("finalAnswer", "Çözüldü"),
        steps = json.optJSONArray("steps")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        formulasUsed = json.optJSONArray("formulasUsed")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
        similarPracticeQuestion = json.optString("similarPracticeQuestion", ""),
        similarPracticeAnswer = json.optString("similarPracticeAnswer", "")
    )
}

suspend fun MrStudyAiService.generatePresentationDeck(
    topic: String,
    subject: String,
    slideCount: Int,
    gradeLevel: String
): PresentationDeck {
    val slideSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string("Slayt başlığı"),
            "bullets" to Schema.array(Schema.string(), "3-5 adet net sunum maddesi"),
            "speakerNotes" to Schema.string("Öğrencinin sunumda söyleyebileceği konuşma notu")
        )
    )
    val rootSchema = Schema.obj(
        properties = mapOf(
            "deckTitle" to Schema.string("Sunum ana başlığı"),
            "slides" to Schema.array(slideSchema, "Tam $slideCount adet slayt")
        )
    )
    val prompt = content {
        text("$gradeLevel '$subject' dersi için '$topic' konusunda tam $slideCount slaytlık profesyonel, anlaşılır Türkçe okul sunumu oluştur.")
    }
    val raw = generateJsonWithSchema(rootSchema, prompt)
    val json = JSONObject(raw)
    val sArr = json.optJSONArray("slides")
    val slides = if (sArr != null) {
        (0 until sArr.length()).map { i ->
            val o = sArr.getJSONObject(i)
            val bullets = o.optJSONArray("bullets")?.let { b -> (0 until b.length()).map { b.getString(it) } } ?: emptyList()
            PresentationSlide(
                title = o.optString("title", "Slayt ${i + 1}"),
                bullets = bullets,
                speakerNotes = o.optString("speakerNotes", "")
            )
        }
    } else emptyList()
    return PresentationDeck(
        title = json.optString("deckTitle", "$topic Sunumu"),
        subject = subject,
        slides = slides
    )
}

suspend fun MrStudyAiService.runEducationalAiTool(
    toolType: AiToolType,
    inputText: String,
    subject: String,
    gradeLevel: String,
    extraOption: String = "",
    attachedBitmap: Bitmap? = null
): String {
    val instruction = when (toolType) {
        AiToolType.MATH_SOLVER -> "Matematik sorusunu adım adım çöz ve sonucu kontrol et."
        AiToolType.EXAM_PREP -> "Yazılı sınavı için konuya özel 5 muhtemel yazılı sorusu + kısa cevap anahtarı + yüksek puan püf noktaları hazırla."
        AiToolType.HOMEWORK_HELPER -> "Öğrencinin ödevini doğrudan kopyalanacak gibi değil mantığını öğreterek açıkla; kendi çözümü varsa doğru/yanlış noktalarını belirt."
        AiToolType.TEXT_SUMMARIZER -> "Verilen metni ana fikir, madde madde özet ve 3 anahtar kavram olarak özetle."
        AiToolType.PRESENTATION_STUDIO -> "Konu için düzenli slayt başlıkları ve konuşma notları oluştur."
        AiToolType.LESSON_NOTE_MAKER -> "Başlık, kısa özet, kritik formüller/kurallar, tanımlar ve 2 kontrol sorusuyla eksiksiz ders notu yaz."
        AiToolType.VOICE_NOTE -> "Bu ham sesli not metnindeki devrik/konuşma dili ifadelerini temizle, düzenli başlıklar ve madde işaretli çalışılabilir ders notuna dönüştür."
        AiToolType.QUESTION_BANK -> "Belirtilen konu ve zorluk için (${extraOption.ifBlank { "Karma" }}) cevap anahtarlı ve çözümlü 6 soruluk özgün soru bankası oluştur."
        AiToolType.LEARNING_COACH -> "Öğrencinin hedefine ve haftalık durumuna göre uygulanabilir 7 günlük çalışma koçluğu planı ve odak stratejisi çıkar."
        AiToolType.TRANSLATOR -> "Metni doğal, akademik açıdan doğru biçimde hedef dile (${extraOption.ifBlank { "İngilizce / Türkçe" }}) çevir ve 3 önemli kelime/kalıbı açıkla."
        AiToolType.TOPIC_MAP -> "Ünite için ön koşul bilgiler, ana başlıklar, alt başlıklar ve sınav ağırlığı haritası çıkar."
        AiToolType.WEAKNESS_ANALYZER -> "Verilen sınav geçmişi ve zayıf konulara göre kök neden analizi ve 3 adımlı telafi çalışma reçetesi hazırla."
    }
    val prompt = content {
        text(
            """
            Araç: ${toolType.title} | Ders: $subject | Sınıf: $gradeLevel
            Görev: $instruction
            Ek Seçenek: $extraOption
            Öğrenci Girdisi:
            $inputText
            """.trimIndent()
        )
        if (attachedBitmap != null) image(attachedBitmap)
    }
    return generatePlainTextWithFallback(prompt)
}

suspend fun MrStudyAiService.analyzeOrDryRunCode(
    language: CodingLanguage,
    code: String,
    action: String
): String {
    val prompt = content {
        text(
            """
            Dil: ${language.displayName}
            İstenen İşlem: $action
            Kurallar:
            - Eğer 'AI Kuru Çalıştırma / Analiz' isteniyorsa kodun beklenen çıktısını adım adım trace ederek göster ve bunun derlenmemiş mantıksal simülasyon (kuru çalıştırma) olduğunu net belirt.
            - Eğer 'Açıkla' isteniyorsa kodu öğrencinin anlayacağı netlikte satır satır açıkla.
            - Eğer 'Hata Bul / İyileştir' isteniyorsa hataları tespit edip düzeltilmiş kodu ve açıklamasını ver.
            Kod:
            ```${language.extension}
            $code
            ```
            """.trimIndent()
        )
    }
    return generatePlainTextWithFallback(prompt)
}
