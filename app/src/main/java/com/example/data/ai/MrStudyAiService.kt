package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.ErrorCategory
import com.example.data.model.EvaluatedQuestionResult
import com.example.data.model.ExamDifficulty
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamResultSummary
import com.example.data.model.ExplanationDepth
import com.example.data.model.FlashcardItem
import com.example.data.model.GeneratedExam
import com.example.data.model.MiniQuizQuestion
import com.example.data.model.QuestionType
import com.example.data.model.StudentProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudyPlanItem
import com.example.data.model.TeachingMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class MrStudyAiService {

    private class FirstChunkTimeoutException : Exception("İlk yanıt zaman aşımına uğradı.")

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    Sen MrStudy: Türkiye'deki öğrenciler için kişiselleştirilmiş, gerçek bir öğretmen gibi ders anlatan gelişmiş yapay zekâ eğitim asistanısın.
                    KURALLAR:
                    1. Her zaman doğal, anlaşılır, nazik ve motive edici TÜRKÇE konuş.
                    2. Öğrencinin yaşına, sınıf seviyesine ve hedefine uygun anlat.
                    3. Öğrenciyi asla aşağılama; hatalarını öğrenme fırsatına çevir.
                    4. Bilmediğin veya emin olmadığın bir bilgiyi uydurma. Hata yaptığını fark edersen açıkça düzelt.
                    5. Ezber yerine kavramayı ve günlük hayat bağlantısını teşvik et.
                    6. PDF KAYNAĞI KURALI: Öğrenci bir PDF kaynağı veya sayfa seçtiyse öncelikle o sayfadaki/kaynaktaki bilgileri temel al. Kaynakta bulunmayan bir bilgiyi PDF'te varmış gibi gösterme. Kaynak dışındaki ek bilgileri mutlaka '[Genel Bilgi]' etiketiyle, kaynaktaki bilgileri '[Kaynak PDF]' etiketiyle açıkça belirt.
                    7. GÖRSEL KURALI: Öğrenci fotoğraf/görsel yüklediğinde el yazısını, matematik denklemlerini, fen şemalarını, grafik/tabloları veya haritaları dikkatle incele. Okunmayan veya hatalı görünen alan varsa açıkça bildir.
                    8. Gereksiz uzunlukta boğucu cevaplar verme; seçilen anlatım derinliğine (Kısa / Normal / Detaylı) sadık kal.
                    """.trimIndent()
                )
            }
        )
    }

    private fun structuredModel(schema: Schema) = Firebase.ai.generativeModel(
        modelName = "gemini-3.8-flash",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
        }
    )

    private suspend fun ensureAppCheckReady() {
        try {
            val result = withTimeout(APP_CHECK_TIMEOUT_MS) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
            if (result.token.isBlank()) {
                throw IllegalStateException("Firebase App Check token boş döndü.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "App Check token exchange timed out", e)
            throw IllegalStateException(
                "Güvenlik doğrulaması (App Check) 10 saniye içinde yanıt vermedi. İnternet bağlantınızı ve Google Play Hizmetlerini kontrol edip tekrar deneyin.",
                e
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "App Check preflight failed", e)
            throw mapAiError(e)
        }
    }

    private fun mapAiError(error: Throwable): Exception {
        if (error is CancellationException && error !is TimeoutCancellationException) {
            throw error
        }
        if (error is FirstChunkTimeoutException) {
            return IllegalStateException(
                "Yapay zekâ 25 saniye içinde yanıt vermeye başlamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.",
                error
            )
        }
        if (error is TimeoutCancellationException) {
            return IllegalStateException(
                "Yanıt süresi 60 saniyeyi aştı. Lütfen daha kısa bir soruyla tekrar deneyin.",
                error
            )
        }

        val chainMessages = generateSequence(error) { it.cause }
            .mapNotNull { it.message ?: it.localizedMessage }
            .joinToString(" | ")

        return when {
            chainMessages.contains("App Check", ignoreCase = true) ||
                chainMessages.contains("appcheck", ignoreCase = true) ||
                chainMessages.contains("App attestation failed", ignoreCase = true) ||
                chainMessages.contains("Exchange failed", ignoreCase = true) ||
                chainMessages.contains("403", ignoreCase = true) -> {
                IllegalStateException(
                    "Firebase App Check doğrulaması reddedildi (403): Bu cihazdaki debug APK token'ı henüz Firebase Console'a kayıtlı olmayabilir. Aşağıdaki Debug Token'ı kopyalayıp Firebase Console > App Check bölümüne ekleyebilirsiniz. ($chainMessages)",
                    error
                )
            }

            chainMessages.contains("429", ignoreCase = true) ||
                chainMessages.contains("quota", ignoreCase = true) ||
                chainMessages.contains("RESOURCE_EXHAUSTED", ignoreCase = true) -> {
                IllegalStateException(
                    "Yapay zekâ istek kotasına geçici olarak ulaşıldı. Lütfen birkaç dakika sonra tekrar deneyin.",
                    error
                )
            }

            error is IOException ||
                chainMessages.contains("UnknownHost", ignoreCase = true) ||
                chainMessages.contains("Unable to resolve host", ignoreCase = true) ||
                chainMessages.contains("network", ignoreCase = true) ||
                chainMessages.contains("timeout", ignoreCase = true) -> {
                IllegalStateException(
                    "İnternet bağlantısı kurulamadı veya zaman aşımına uğradı. Bağlantınızı kontrol edip tekrar deneyin.",
                    error
                )
            }

            error is IllegalStateException && !error.message.isNullOrBlank() -> error
            else -> IllegalStateException(
                error.localizedMessage ?: "Yapay zekâ yanıtı alınamadı.",
                error
            )
        }
    }

    fun streamTeacherReply(
        studentProfile: StudentProfile,
        subject: String,
        mode: TeachingMode,
        depth: ExplanationDepth,
        userPrompt: String,
        recentHistory: List<ChatMessage>,
        attachedImages: List<Bitmap>,
        pdfPageSamples: List<Pair<Int, Bitmap>>,
        pdfTitle: String?,
        activePdfPage: Int?
    ): Flow<String> = flow {
        ensureAppCheckReady()

        val depthRule = when (depth) {
            ExplanationDepth.SHORT -> "KISA ANLATIM: En fazla 3-4 kısa paragraf veya madde ile öz yanıt ver."
            ExplanationDepth.NORMAL -> "NORMAL ANLATIM: Dengeli, açık örnekli ve öğrenme kontrollü anlatım yap."
            ExplanationDepth.DETAILED -> "DETAYLI ANLATIM: Derinlemesine açıklamalar, birden fazla örnek, formül/kavram analizi ve sonda mini kontrol sorusu ver."
        }

        val historyText = recentHistory.takeLast(6).joinToString("\n") { msg ->
            val who = if (msg.role == "user") "Öğrenci" else "MrStudy"
            "$who: ${msg.content.take(500)}"
        }

        val pdfContextNote = if (!pdfTitle.isNullOrBlank()) {
            "SEÇİLİ PDF KAYNAĞI: '$pdfTitle' (Aktif sayfa: ${activePdfPage ?: 1}). " +
                "Eklenen PDF sayfa görsellerini dikkatle oku. Yalnızca bu PDF sayfalarında gördüğün bilgileri [Kaynak PDF s.${activePdfPage ?: 1}] olarak işaretle; dışarıdan eklediğin her bilgiyi [Genel Bilgi] olarak ayır."
        } else {
            "Aktif PDF kaynağı seçili değil."
        }

        val fullPrompt = """
            ÖĞRENCİ PROFİLİ: Ad=${studentProfile.displayName}, Sınıf=${studentProfile.gradeLevel}, Hedef=${studentProfile.targetGoal}
            DERS: $subject
            ÖĞRETMEN MODU: ${mode.title} -> ${mode.promptGuidance}
            DERİNLİK: $depthRule
            $pdfContextNote
            
            SON KONUŞMA GEÇMİŞİ:
            ${historyText.ifBlank { "(İlk mesaj)" }}
            
            ÖĞRENCİNİN MESAJI:
            $userPrompt
        """.trimIndent()

        val inputContent = content {
            pdfPageSamples.forEach { (pageNo, bmp) ->
                text("PDF Sayfa $pageNo görseli:")
                image(bmp)
            }
            attachedImages.forEachIndexed { idx, bmp ->
                text("Öğrencinin yüklediği görsel #${idx + 1}:")
                image(bmp)
            }
            text(fullPrompt)
        }

        val accumulated = StringBuilder()
        val receivedFirstChunk = AtomicBoolean(false)
        try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                coroutineScope {
                    val firstChunkWatchdog = launch {
                        delay(FIRST_CHUNK_TIMEOUT_MS)
                        if (!receivedFirstChunk.get()) {
                            throw FirstChunkTimeoutException()
                        }
                    }
                    try {
                        chatModel.generateContentStream(inputContent).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                if (receivedFirstChunk.compareAndSet(false, true)) {
                                    firstChunkWatchdog.cancel()
                                }
                                accumulated.append(piece)
                                emit(accumulated.toString())
                            }
                        }
                    } finally {
                        firstChunkWatchdog.cancel()
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Yapay zekâ öğretmeni boş yanıt döndürdü.")
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "Teacher stream timeout", e)
                throw mapAiError(e)
            }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Teacher stream failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun analyzePdfPages(
        pdfTitle: String,
        subject: String,
        gradeLevel: String,
        pageSamples: List<Pair<Int, Bitmap>>
    ): PdfAiAnalysis {
        ensureAppCheckReady()
        return try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                val schema = Schema.obj(
                    properties = mapOf(
                        "summary" to Schema.string("Türkçe kapsamlı özet (sadece PDF sayfalarından)"),
                        "topics" to Schema.array(Schema.string(), "PDF içinde tespit edilen ana konu başlıkları"),
                        "keyConcepts" to Schema.array(Schema.string(), "Önemli kavramlar"),
                        "formulas" to Schema.array(Schema.string(), "PDF'te geçen formüller veya kurallar"),
                        "definitions" to Schema.array(Schema.string(), "PDF'teki önemli tanımlar")
                    )
                )
                val model = structuredModel(schema)
                val promptContent = content {
                    pageSamples.forEach { (p, bmp) ->
                        text("Sayfa $p:")
                        image(bmp)
                    }
                    text(
                        """
                        Sen MrStudy PDF analiz motorusun. '$pdfTitle' ($subject, $gradeLevel) adlı PDF'in verilen sayfalarını analiz et.
                        ÖNEMLİ: Yalnızca görsellerde gerçekten yer alan bilgileri çıkar; kaynakta olmayan bilgiyi varmış gibi yazma.
                        Türkçe özet, konu başlıkları, önemli kavramlar, formüller ve tanımlar üret.
                        """.trimIndent()
                    )
                }
                val response = model.generateContent(promptContent)
                val raw = response.text ?: throw IllegalStateException("PDF analizi boş döndü.")
                val json = JSONObject(raw)
                PdfAiAnalysis(
                    summary = json.optString("summary", "Özet çıkarılamadı."),
                    topics = json.optJSONArray("topics")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
                    keyConcepts = json.optJSONArray("keyConcepts")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
                    formulas = json.optJSONArray("formulas")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
                    definitions = json.optJSONArray("definitions")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
                )
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "analyzePdfPages failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun generateExam(
        gradeLevel: String,
        subject: String,
        topic: String,
        difficulty: ExamDifficulty,
        questionCount: Int,
        sourceType: String,
        selectedTypes: List<QuestionType>,
        timedMinutes: Int,
        pdfSamples: List<Pair<Int, Bitmap>> = emptyList(),
        focusWrongQuestions: String? = null
    ): GeneratedExam {
        ensureAppCheckReady()
        return try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                val questionSchema = Schema.obj(
                    properties = mapOf(
                        "type" to Schema.enumeration(
                            listOf("MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK", "OPEN_ENDED", "CLASSIC_WRITTEN"),
                            "Soru tipi"
                        ),
                        "questionText" to Schema.string("Türkçe soru metni"),
                        "options" to Schema.array(
                            Schema.string(),
                            "Çoktan seçmeli ise 4 şık (A, B, C, D metinleri), Doğru-Yanlış ise ['Doğru', 'Yanlış'], diğerlerinde boş liste"
                        ),
                        "correctAnswer" to Schema.string("Doğru cevap veya örnek tam puanlık çözüm anahtarı"),
                        "explanation" to Schema.string("Adım adım çözüm açıklaması"),
                        "subTopic" to Schema.string("Alt konu adı")
                    )
                )
                val examSchema = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string("Sınav başlığı"),
                        "questions" to Schema.array(questionSchema, "Sınav soruları")
                    )
                )

                val typesText = selectedTypes.joinToString(", ") { it.name }
                val model = structuredModel(examSchema)
                val promptContent = content {
                    pdfSamples.forEach { (p, bmp) ->
                        text("PDF Kaynak Sayfa $p:")
                        image(bmp)
                    }
                    text(
                        """
                        Sen MrStudy Akıllı Sınav Üreticisisin.
                        Öğrenci Seviyesi: $gradeLevel
                        Ders: $subject
                        Konu/Ünite: $topic
                        Zorluk: ${difficulty.label}
                        Soru Sayısı: $questionCount
                        Kaynak Türü: $sourceType
                        İzin Verilen Soru Tipleri: $typesText
                        ${if (!focusWrongQuestions.isNullOrBlank()) "TEKRAR SINAVI ODAĞI (Önceki yanlışlar): $focusWrongQuestions" else ""}
                        ${if (pdfSamples.isNotEmpty()) "ÖNEMLİ: Soruları ekli PDF sayfalarındaki konulara dayanarak üret." else ""}
                        
                        Tam olarak $questionCount adet kaliteli, Türkçe, müfredata uygun soru üret.
                        - MULTIPLE_CHOICE için options listesinde tam 4 seçenek ver, correctAnswer tam olarak doğru seçeneğin metniyle eşleşsin.
                        - TRUE_FALSE için options = ["Doğru", "Yanlış"], correctAnswer = "Doğru" veya "Yanlış".
                        - FILL_BLANK için soru metninde '_____' boşluğu bırak, options = [], correctAnswer = boşluğa gelecek net kelime/ifade.
                        - OPEN_ENDED ve CLASSIC_WRITTEN için options = [], correctAnswer = anahtar çözüm.
                        """.trimIndent()
                    )
                }

                val response = model.generateContent(promptContent)
                val raw = response.text ?: throw IllegalStateException("Sınav oluşturulamadı.")
                val json = JSONObject(raw)
                val title = json.optString("title", "$subject - $topic Sınavı")
                val qArr = json.getJSONArray("questions")
                val questions = (0 until qArr.length()).map { idx ->
                    val qObj = qArr.getJSONObject(idx)
                    val qType = try {
                        QuestionType.valueOf(qObj.optString("type", "MULTIPLE_CHOICE"))
                    } catch (_: Exception) {
                        QuestionType.MULTIPLE_CHOICE
                    }
                    val optsJson = qObj.optJSONArray("options")
                    val opts = if (optsJson != null) (0 until optsJson.length()).map { optsJson.getString(it) } else emptyList()
                    ExamQuestion(
                        number = idx + 1,
                        type = qType,
                        questionText = qObj.getString("questionText"),
                        options = opts,
                        correctAnswer = qObj.getString("correctAnswer"),
                        explanation = qObj.getString("explanation"),
                        subTopic = qObj.optString("subTopic", topic)
                    )
                }

                GeneratedExam(
                    title = title,
                    subject = subject,
                    topic = topic,
                    difficulty = difficulty,
                    sourceType = sourceType,
                    timedMinutes = timedMinutes,
                    questions = questions
                )
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateExam failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun evaluateExamAndAnalyzeErrors(
        exam: GeneratedExam,
        answers: Map<Int, String>,
        durationSeconds: Int,
        gradeLevel: String
    ): ExamResultSummary {
        ensureAppCheckReady()
        return try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                val evalItemSchema = Schema.obj(
                    properties = mapOf(
                        "questionNumber" to Schema.integer("Soru numarası"),
                        "isCorrect" to Schema.boolean("Doğru mu"),
                        "feedback" to Schema.string("Açıklamalı Türkçe değerlendirme ve doğru çözüm yönlendirmesi"),
                        "errorCategory" to Schema.enumeration(
                            listOf(
                                "Yok",
                                ErrorCategory.KNOWLEDGE_GAP.title,
                                ErrorCategory.CALCULATION_ERROR.title,
                                ErrorCategory.MISREADING.title,
                                ErrorCategory.ATTENTION_SLIP.title,
                                ErrorCategory.MISCONCEPTION.title
                            ),
                            "Yanlışsa hata sınıflandırması, doğruysa 'Yok'"
                        )
                    )
                )
                val reportSchema = Schema.obj(
                    properties = mapOf(
                        "evaluations" to Schema.array(evalItemSchema, "Her soru için değerlendirme"),
                        "overallReport" to Schema.string("Öğrenciye özel kapsamlı Türkçe başarı ve gelişim raporu"),
                        "weakTopics" to Schema.array(Schema.string(), "Geliştirilmesi gereken alt konular")
                    )
                )

                val qaSummary = exam.questions.joinToString("\n\n") { q ->
                    val userAns = answers[q.number].orEmpty().ifBlank { "(Boş bırakıldı)" }
                    """
                    Soru ${q.number} [${q.type.label}] (Alt konu: ${q.subTopic}):
                    Metin: ${q.questionText}
                    Doğru Anahtar: ${q.correctAnswer}
                    Öğrenci Cevabı: $userAns
                    """.trimIndent()
                }

                val model = structuredModel(reportSchema)
                val response = model.generateContent(
                    """
                    Sen MrStudy Sınav Değerlendirici ve AI Hata Analisti öğretmenisin ($gradeLevel, ${exam.subject}).
                    Aşağıdaki öğrenci cevaplarını değerlendir:
                    - Çoktan seçmeli, Doğru-Yanlış ve Boşluk Doldurmada doğruluğu kesin kontrol et (küçük/büyük harf ve ufak yazım farklarını boşluk doldurmada tolere et).
                    - Açık uçlu ve Klasik yazılı cevapları kavramsal doğruluk açısından öğretmen gibi değerlendir ve yapıcı açıklamalı geri bildirim yaz.
                    - Yanlış olan her soruyu 5 hata türünden birine sınıflandır:
                      1) Bilgi Eksikliği
                      2) İşlem Hatası
                      3) Soruyu Yanlış Anlama
                      4) Dikkat Hatası
                      5) Kavram Yanılgısı
                    - Her hata türüne uygun farklı öğretim yaklaşımı öner.
                    
                    $qaSummary
                    """.trimIndent()
                )
                val raw = response.text ?: throw IllegalStateException("Değerlendirme yanıtı boş döndü.")
                val json = JSONObject(raw)
                val evalArr = json.getJSONArray("evaluations")
                val evalMap = mutableMapOf<Int, Triple<Boolean, String, String?>>()
                for (i in 0 until evalArr.length()) {
                    val obj = evalArr.getJSONObject(i)
                    val qNum = obj.getInt("questionNumber")
                    val ok = obj.getBoolean("isCorrect")
                    val fb = obj.getString("feedback")
                    val cat = obj.optString("errorCategory", "Yok").takeIf { !ok && it != "Yok" }
                    evalMap[qNum] = Triple(ok, fb, cat)
                }

                val results = exam.questions.map { q ->
                    val userAns = answers[q.number].orEmpty()
                    val mapped = evalMap[q.number]
                    val isCorrect = mapped?.first ?: userAns.trim().equals(q.correctAnswer.trim(), ignoreCase = true)
                    val feedback = mapped?.second ?: q.explanation
                    val errCat = if (isCorrect) null else (mapped?.third ?: ErrorCategory.KNOWLEDGE_GAP.title)
                    EvaluatedQuestionResult(
                        question = q,
                        userAnswer = userAns,
                        isCorrect = isCorrect,
                        aiFeedback = feedback,
                        errorCategory = errCat
                    )
                }

                val correctCount = results.count { it.isCorrect }
                val total = results.size.coerceAtLeast(1)
                val scorePercent = (correctCount * 100) / total
                val weakTopics = json.optJSONArray("weakTopics")
                    ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                    ?.distinct()
                    ?.take(20)
                    ?: results.filter { !it.isCorrect }.map { it.question.subTopic }.distinct()
                val errorCategories = results.mapNotNull { it.errorCategory }.distinct()
                val wrongSummary = results.filter { !it.isCorrect }
                    .joinToString("\n") { r ->
                        "Soru: ${r.question.questionText.take(120)} | Öğrenci: ${r.userAnswer.ifBlank { "Boş" }} | Doğru: ${r.question.correctAnswer} | Hata: ${r.errorCategory}"
                    }

                ExamResultSummary(
                    title = exam.title,
                    subject = exam.subject,
                    topic = exam.topic,
                    difficulty = exam.difficulty.label,
                    sourceType = exam.sourceType,
                    durationSeconds = durationSeconds,
                    correctCount = correctCount,
                    totalCount = total,
                    scorePercent = scorePercent,
                    results = results,
                    overallReport = json.optString("overallReport", "Sınav değerlendirmesi tamamlandı."),
                    weakTopics = weakTopics,
                    errorCategories = errorCategories,
                    wrongQuestionsSummary = wrongSummary
                )
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "evaluateExamAndAnalyzeErrors failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun generateStructuredNote(
        subject: String,
        topicOrPrompt: String,
        gradeLevel: String,
        pdfTitle: String? = null,
        pdfSamples: List<Pair<Int, Bitmap>> = emptyList()
    ): StudyNote {
        ensureAppCheckReady()
        return try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                val schema = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string("Not başlığı"),
                        "content" to Schema.string("Detaylı ama düzenli ders notu özeti"),
                        "formulas" to Schema.array(Schema.string(), "Formüller veya kurallar"),
                        "definitions" to Schema.array(Schema.string(), "Kavram tanımları"),
                        "keyConcepts" to Schema.array(Schema.string(), "Anahtar kelimeler/kavramlar")
                    )
                )
                val model = structuredModel(schema)
                val contentInput = content {
                    pdfSamples.forEach { (p, bmp) ->
                        text("PDF Sayfa $p:")
                        image(bmp)
                    }
                    text(
                        """
                        Sen MrStudy Akıllı Not Çıkarma asistanısın ($gradeLevel, $subject).
                        Konu/İstek: $topicOrPrompt
                        ${if (!pdfTitle.isNullOrBlank()) "Kaynak PDF: $pdfTitle (Yalnızca görsellerdeki içerikten not çıkar, ek bilgi varsa [Genel Bilgi] yaz)." else ""}
                        Düzenli Türkçe çalışma notu, ayrı formüller listesi, ayrı tanımlar listesi ve önemli kavramlar çıkar.
                        """.trimIndent()
                    )
                }
                val raw = model.generateContent(contentInput).text ?: throw IllegalStateException("Not oluşturulamadı.")
                val json = JSONObject(raw)
                StudyNote(
                    title = json.optString("title", topicOrPrompt.take(60)),
                    subject = subject,
                    content = json.optString("content", ""),
                    formulas = json.optJSONArray("formulas")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                    definitions = json.optJSONArray("definitions")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                    keyConcepts = json.optJSONArray("keyConcepts")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                    sourcePdfTitle = pdfTitle.orEmpty()
                )
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateStructuredNote failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun generateFlashcards(
        subject: String,
        topic: String,
        gradeLevel: String,
        count: Int = 6,
        pdfSamples: List<Pair<Int, Bitmap>> = emptyList()
    ): List<FlashcardItem> {
        ensureAppCheckReady()
        return try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                val cardSchema = Schema.obj(
                    properties = mapOf(
                        "cardType" to Schema.enumeration(listOf("Soru-Cevap", "Kavram", "Formül"), "Kart türü"),
                        "front" to Schema.string("Kartın ön yüzü (soru, kavram adı veya formül sorusu)"),
                        "back" to Schema.string("Kartın arka yüzü (net cevap, tanım veya formül açıklaması)")
                    )
                )
                val rootSchema = Schema.obj(
                    properties = mapOf(
                        "cards" to Schema.array(cardSchema, "Bilgi kartları listesi")
                    )
                )
                val model = structuredModel(rootSchema)
                val promptContent = content {
                    pdfSamples.forEach { (p, bmp) ->
                        text("PDF Sayfa $p:")
                        image(bmp)
                    }
                    text(
                        """
                        $gradeLevel düzeyi için $subject dersinin '$topic' konusunda $count adet Türkçe bilgi kartı (Soru-Cevap, Kavram, Formül karışık) üret.
                        """.trimIndent()
                    )
                }
                val raw = model.generateContent(promptContent).text ?: throw IllegalStateException("Kartlar üretilemedi.")
                val arr = JSONObject(raw).getJSONArray("cards")
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FlashcardItem(
                        subject = subject,
                        cardType = o.optString("cardType", "Kavram"),
                        front = o.getString("front"),
                        back = o.getString("back"),
                        masteryStatus = "new",
                        reviewCount = 0L,
                        nextReviewAtMs = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateFlashcards failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun generateStudyPlans(
        profile: StudentProfile,
        weakTopics: List<String>,
        examDateText: String,
        focusSubject: String
    ): List<StudyPlanItem> {
        ensureAppCheckReady()
        return try {
            withTimeout(AI_TOTAL_TIMEOUT_MS) {
                val itemSchema = Schema.obj(
                    properties = mapOf(
                        "subject" to Schema.string("Ders adı"),
                        "topic" to Schema.string("Çalışılacak konu"),
                        "dayLabel" to Schema.string("Gün adı (örn. Pazartesi, Salı, Çarşamba, Perşembe, Cuma, Cumartesi, Pazar)"),
                        "durationMinutes" to Schema.integer("Süre (dakika, 20-90 arası)"),
                        "priority" to Schema.enumeration(listOf("Yüksek", "Orta", "Düşük"), "Öncelik"),
                        "planType" to Schema.enumeration(listOf("Ders", "Tekrar", "Sınav Hazırlığı", "Ödev"), "Plan türü")
                    )
                )
                val rootSchema = Schema.obj(
                    properties = mapOf("items" to Schema.array(itemSchema, "Haftalık çalışma planı blokları"))
                )
                val model = structuredModel(rootSchema)
                val raw = model.generateContent(
                    """
                    Öğrenci: ${profile.gradeLevel}, Hedef: ${profile.targetGoal}, Günlük hedef süre: ${profile.dailyGoalMinutes} dk.
                    Odak ders: $focusSubject
                    Yaklaşan sınav tarihi: ${examDateText.ifBlank { "Belirtilmedi" }}
                    Eksik/zayıf konular: ${weakTopics.joinToString(", ").ifBlank { "Genel müfredat pekiştirme" }}
                    
                    Öğrenciye dengeli, eksik konulara öncelik veren, tekrar blokları da içeren 6 adet haftalık çalışma planı maddesi oluştur.
                    """.trimIndent()
                ).text ?: throw IllegalStateException("Çalışma planı üretilemedi.")

                val arr = JSONObject(raw).getJSONArray("items")
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    StudyPlanItem(
                        subject = o.optString("subject", focusSubject),
                        topic = o.getString("topic"),
                        dayLabel = o.optString("dayLabel", "Bugün"),
                        durationMinutes = o.optInt("durationMinutes", 40).toLong().coerceIn(10L, 180L),
                        priority = o.optString("priority", "Orta"),
                        planType = o.optString("planType", "Ders"),
                        isCompleted = false,
                        examDateText = examDateText
                    )
                }
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateStudyPlans failed", e)
            throw mapAiError(e)
        }
    }

    suspend fun generateMiniQuiz(gradeLevel: String, subject: String): List<MiniQuizQuestion> {
        ensureAppCheckReady()
        return try {
            withTimeout(45_000L) {
                val qSchema = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string("Kısa, eğlenceli Türkçe bilgi yarışması sorusu"),
                        "options" to Schema.array(Schema.string(), "Tam 4 kısa şık"),
                        "correctIndex" to Schema.integer("Doğru şık indeksi (0-3)"),
                        "explanation" to Schema.string("1-2 cümlelik net açıklama")
                    )
                )
                val rootSchema = Schema.obj(
                    properties = mapOf("questions" to Schema.array(qSchema, "3 soruluk mini yarışma"))
                )
                val raw = structuredModel(rootSchema).generateContent(
                    "$gradeLevel öğrencisi için $subject alanında 3 soruluk hızlı ve öğretici mini bilgi yarışması üret."
                ).text ?: throw IllegalStateException("Mini yarışma üretilemedi.")
                val arr = JSONObject(raw).getJSONArray("questions")
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    val optsArr = o.getJSONArray("options")
                    val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
                    MiniQuizQuestion(
                        question = o.getString("question"),
                        options = opts,
                        correctIndex = o.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                        explanation = o.optString("explanation", "")
                    )
                }
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw mapAiError(e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateMiniQuiz failed", e)
            throw mapAiError(e)
        }
    }

    data class PdfAiAnalysis(
        val summary: String,
        val topics: List<String>,
        val keyConcepts: List<String>,
        val formulas: List<String>,
        val definitions: List<String>
    )

    private companion object {
        const val TAG = "MrStudyAiService"
        const val APP_CHECK_TIMEOUT_MS = 10_000L
        const val FIRST_CHUNK_TIMEOUT_MS = 25_000L
        const val AI_TOTAL_TIMEOUT_MS = 60_000L
    }
}
