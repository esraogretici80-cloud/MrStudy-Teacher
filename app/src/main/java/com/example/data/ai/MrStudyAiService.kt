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
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
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

    private val teacherGenerationConfig = generationConfig {
        temperature = 0.7f
        topP = 0.9f
        thinkingConfig = thinkingConfig {
            thinkingLevel = ThinkingLevel.LOW
        }
    }

    private fun buildSystemInstruction(): Content = content {
        text(
            """
            Sen MrStudy: Türkiye'deki öğrenciler için en üst düzey, uzman bir öğretmen, rehber ve yapay zekâ eğitim asistanısın.
            AMACIN: Öğrenciye yalnızca yüzeysel bir cevap vermek değil; konunun mantığını kavratmak, motive etmek ve adım adım kristal netliğinde çok güzel çözümler sunmak.

            PEDAGOJİK VE ESTETİK ÇÖZÜM KURALLARI:
            1. NET VE GÖZ ALICI ÇÖZÜM DÜZENİ:
               - Soru veya problem sorulduğunda lafı uzatmadan başla.
               - Sorunun cevabını veya ana sonucunu en başta ya da belirgin bir kutucuk gibi açıkça belirt (örn: '**✅ Doğru Cevap:** C şıkkı (24)' veya '**✅ Çözüm Sonucu:** x = 5').
               - Matematik, Fen ve diğer derslerde işlemleri adım adım numaralandırarak gerekçeleriyle açıkla ('**1. Adım: Verilenleri Belirleme**', '**2. Adım: Kuralı / Formülü Uygulama**', '**3. Adım: İşlem ve Sonuç**').
               - Önemli formül ve kuralları özel vurgula: '📐 **Formül / Kural:** ...'
               - Sınavlarda (LGS, YKS, okul yazılıları) puan getiren püf noktalarını ve tuzakları belirt: '💡 **Öğretmen Püf Noktası:** ...'
            2. ANLAŞILIR, SICAK VE MOTİVE EDİCİ TÜRKÇE:
               - Samimi, enerjik, saygılı ve motive edici bir öğretmen üslubu kullan. Asla aşağılama veya sıkıcı olma.
               - Öğrencinin sınıf seviyesine (LGS, YKS, ortaokul, lise) tam uygun terimler ve örnekler seç.
            3. GÖRSEL VE SORU FOTOĞRAFLARI:
               - Öğrenci el yazısı veya kitap fotoğrafı yüklediğinde:
                 Önce soruyu kısaca teyit et: '📝 **Sorunun Metni:** ...'
                 Ardından şekil, grafik veya denklemleri analiz ederek adım adım çözüme geç. Okunmayan bir yer varsa nazikçe belirt.
            4. ETKİLEŞİM VE PEKİŞTİRME:
               - Anlatımın sonunda öğrenciyi cevabı anlamaya ve pekiştirmeye davet eden kısa bir soru veya benzer mini bir kontrol sorusu sor ('Şimdi şu benzer örneği sen çöz bakalım: ...').
            5. KAYNAK GÜVENİLİRLİĞİ (PDF KURALI):
               - Öğrenci bir PDF kaynağı seçtiyse öncelikle PDF sayfasındaki bilgilere sadık kal ([Kaynak PDF s.X]). Ek genel bilgileri [Genel Bilgi] olarak ayır. Asla bilgi uydurma.
            """.trimIndent()
        )
    }

    private val primaryChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_TEXT_MODEL,
            generationConfig = teacherGenerationConfig,
            systemInstruction = buildSystemInstruction()
        )
    }

    private val fallbackChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_TEXT_MODEL,
            generationConfig = teacherGenerationConfig,
            systemInstruction = buildSystemInstruction()
        )
    }

    private fun structuredModel(modelName: String, schema: Schema) = Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.LOW
            }
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

    private fun isQuotaOrTransientError(error: Throwable): Boolean {
        val chain = generateSequence(error) { it.cause }
            .mapNotNull { it.message ?: it.localizedMessage }
            .joinToString(" | ")
        return chain.contains("429", ignoreCase = true) ||
            chain.contains("quota", ignoreCase = true) ||
            chain.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            chain.contains("ResourceExhausted", ignoreCase = true) ||
            chain.contains("rate limit", ignoreCase = true) ||
            chain.contains("temporarily unavailable", ignoreCase = true) ||
            chain.contains("503", ignoreCase = true) ||
            chain.contains("500", ignoreCase = true) ||
            chain.contains("FirstChunkTimeout", ignoreCase = true) ||
            error is FirstChunkTimeoutException
    }

    private fun mapAiError(error: Throwable): Exception {
        if (error is CancellationException && error !is TimeoutCancellationException) {
            throw error
        }
        if (error is FirstChunkTimeoutException) {
            return IllegalStateException(
                "Yapay zekâ yanıt hazırlarken zaman aşımına uğradı. Lütfen tekrar deneyin.",
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
                    "Firebase App Check doğrulaması reddedildi (403): Bu cihazdaki Debug Token henüz Firebase Console'a kayıtlı değil. Aşağıdan token'ı kopyalayıp Firebase Console > App Check > Manage debug tokens bölümüne ekleyerek doğrulayabilirsiniz.",
                    error
                )
            }

            chainMessages.contains("429", ignoreCase = true) ||
                chainMessages.contains("quota", ignoreCase = true) ||
                chainMessages.contains("RESOURCE_EXHAUSTED", ignoreCase = true) -> {
                IllegalStateException(
                    "Yapay zekâ istek kotasına geçici olarak ulaşıldı. Otomatik yedek model devreye alınıyor, lütfen 5-10 saniye sonra tekrar deneyin.",
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

    private suspend fun executeStructuredWithFallback(
        schema: Schema,
        promptContent: Content
    ): String {
        ensureAppCheckReady()
        return withTimeout(AI_TOTAL_TIMEOUT_MS) {
            try {
                val primary = structuredModel(PRIMARY_TEXT_MODEL, schema)
                val res = primary.generateContent(promptContent)
                res.text ?: throw IllegalStateException("Model boş yanıt döndürdü.")
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) throw mapAiError(e)
                throw e
            } catch (e: Exception) {
                if (isQuotaOrTransientError(e)) {
                    Log.w(TAG, "Primary structured model hit quota/transient error, falling back to $FALLBACK_TEXT_MODEL", e)
                    delay(800L)
                    val fallback = structuredModel(FALLBACK_TEXT_MODEL, schema)
                    val res = fallback.generateContent(promptContent)
                    res.text ?: throw IllegalStateException("Yedek model boş yanıt döndürdü.")
                } else {
                    throw mapAiError(e)
                }
            }
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
            ExplanationDepth.SHORT -> "KISA ANLATIM: En fazla 3-4 net paragraf veya madde ile öz yanıt ve direkt sonuç ver."
            ExplanationDepth.NORMAL -> "NORMAL ANLATIM: Dengeli, adım adım açıklamalı, püf noktalı ve öğrenme kontrollü anlatım yap."
            ExplanationDepth.DETAILED -> "DETAYLI ANLATIM: Derinlemesine açıklamalar, adım adım çözüm basamakları, formül/kavram analizi ve sonda kontrol sorusu ver."
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
                    var fallbackRequired = false
                    val firstChunkWatchdog = launch {
                        delay(FIRST_CHUNK_TIMEOUT_MS)
                        if (!receivedFirstChunk.get()) {
                            Log.w(TAG, "Primary model first chunk timed out ($FIRST_CHUNK_TIMEOUT_MS ms), triggering fallback")
                            fallbackRequired = true
                            throw FirstChunkTimeoutException()
                        }
                    }

                    try {
                        primaryChatModel.generateContentStream(inputContent).collect { chunk ->
                            val piece = chunk.text.orEmpty()
                            if (piece.isNotEmpty()) {
                                if (receivedFirstChunk.compareAndSet(false, true)) {
                                    firstChunkWatchdog.cancel()
                                }
                                accumulated.append(piece)
                                emit(accumulated.toString())
                            }
                        }
                    } catch (e: Exception) {
                        firstChunkWatchdog.cancel()
                        if (e is CancellationException && e !is TimeoutCancellationException && !fallbackRequired) {
                            throw e
                        }

                        // If no chunks were received yet and it's a quota, timeout, or transient error,
                        // seamlessly stream from fallback model (gemini-3.5-flash-lite)
                        if (!receivedFirstChunk.get() && (fallbackRequired || isQuotaOrTransientError(e))) {
                            Log.w(TAG, "Seamlessly switching to fallback model: $FALLBACK_TEXT_MODEL", e)
                            if (e.message?.contains("429", ignoreCase = true) == true) {
                                delay(600L)
                            }
                            fallbackChatModel.generateContentStream(inputContent).collect { chunk ->
                                val piece = chunk.text.orEmpty()
                                if (piece.isNotEmpty()) {
                                    receivedFirstChunk.set(true)
                                    accumulated.append(piece)
                                    emit(accumulated.toString())
                                }
                            }
                        } else {
                            throw e
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
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string("Türkçe kapsamlı özet (sadece PDF sayfalarından)"),
                "topics" to Schema.array(Schema.string(), "PDF içinde tespit edilen ana konu başlıkları"),
                "keyConcepts" to Schema.array(Schema.string(), "Önemli kavramlar"),
                "formulas" to Schema.array(Schema.string(), "PDF'te geçen formüller veya kurallar"),
                "definitions" to Schema.array(Schema.string(), "PDF'teki önemli tanımlar")
            )
        )
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
        val raw = executeStructuredWithFallback(schema, promptContent)
        val json = JSONObject(raw)
        return PdfAiAnalysis(
            summary = json.optString("summary", "Özet çıkarılamadı."),
            topics = json.optJSONArray("topics")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
            keyConcepts = json.optJSONArray("keyConcepts")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
            formulas = json.optJSONArray("formulas")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
            definitions = json.optJSONArray("definitions")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
        )
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

        val raw = executeStructuredWithFallback(examSchema, promptContent)
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

        return GeneratedExam(
            title = title,
            subject = subject,
            topic = topic,
            difficulty = difficulty,
            sourceType = sourceType,
            timedMinutes = timedMinutes,
            questions = questions
        )
    }

    suspend fun evaluateExamAndAnalyzeErrors(
        exam: GeneratedExam,
        answers: Map<Int, String>,
        durationSeconds: Int,
        gradeLevel: String
    ): ExamResultSummary {
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

        val promptContent = content {
            text(
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
        }

        val raw = executeStructuredWithFallback(reportSchema, promptContent)
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

        return ExamResultSummary(
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

    suspend fun generateStructuredNote(
        subject: String,
        topicOrPrompt: String,
        gradeLevel: String,
        pdfTitle: String? = null,
        pdfSamples: List<Pair<Int, Bitmap>> = emptyList()
    ): StudyNote {
        val schema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string("Not başlığı"),
                "content" to Schema.string("Detaylı ama düzenli ders notu özeti"),
                "formulas" to Schema.array(Schema.string(), "Formüller veya kurallar"),
                "definitions" to Schema.array(Schema.string(), "Kavram tanımları"),
                "keyConcepts" to Schema.array(Schema.string(), "Anahtar kelimeler/kavramlar")
            )
        )
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
        val raw = executeStructuredWithFallback(schema, contentInput)
        val json = JSONObject(raw)
        return StudyNote(
            title = json.optString("title", topicOrPrompt.take(60)),
            subject = subject,
            content = json.optString("content", ""),
            formulas = json.optJSONArray("formulas")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            definitions = json.optJSONArray("definitions")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            keyConcepts = json.optJSONArray("keyConcepts")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            sourcePdfTitle = pdfTitle.orEmpty()
        )
    }

    suspend fun generateFlashcards(
        subject: String,
        topic: String,
        gradeLevel: String,
        count: Int = 6,
        pdfSamples: List<Pair<Int, Bitmap>> = emptyList()
    ): List<FlashcardItem> {
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
        val raw = executeStructuredWithFallback(rootSchema, promptContent)
        val arr = JSONObject(raw).getJSONArray("cards")
        return (0 until arr.length()).map { i ->
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

    suspend fun generateStudyPlans(
        profile: StudentProfile,
        weakTopics: List<String>,
        examDateText: String,
        focusSubject: String
    ): List<StudyPlanItem> {
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
        val promptContent = content {
            text(
                """
                Öğrenci: ${profile.gradeLevel}, Hedef: ${profile.targetGoal}, Günlük hedef süre: ${profile.dailyGoalMinutes} dk.
                Odak ders: $focusSubject
                Yaklaşan sınav tarihi: ${examDateText.ifBlank { "Belirtilmedi" }}
                Eksik/zayıf konular: ${weakTopics.joinToString(", ").ifBlank { "Genel müfredat pekiştirme" }}
                
                Öğrenciye dengeli, eksik konulara öncelik veren, tekrar blokları da içeren 6 adet haftalık çalışma planı maddesi oluştur.
                """.trimIndent()
            )
        }
        val raw = executeStructuredWithFallback(rootSchema, promptContent)
        val arr = JSONObject(raw).getJSONArray("items")
        return (0 until arr.length()).map { i ->
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

    suspend fun generateMiniQuiz(gradeLevel: String, subject: String): List<MiniQuizQuestion> {
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
        val promptContent = content {
            text("$gradeLevel öğrencisi için $subject alanında 3 soruluk hızlı ve öğretici mini bilgi yarışması üret.")
        }
        val raw = executeStructuredWithFallback(rootSchema, promptContent)
        val arr = JSONObject(raw).getJSONArray("questions")
        return (0 until arr.length()).map { i ->
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

    data class PdfAiAnalysis(
        val summary: String,
        val topics: List<String>,
        val keyConcepts: List<String>,
        val formulas: List<String>,
        val definitions: List<String>
    )

    private companion object {
        const val TAG = "MrStudyAiService"
        const val PRIMARY_TEXT_MODEL = "gemini-3.8-flash"
        const val FALLBACK_TEXT_MODEL = "gemini-3.5-flash-lite"
        const val APP_CHECK_TIMEOUT_MS = 10_000L
        const val FIRST_CHUNK_TIMEOUT_MS = 10_000L
        const val AI_TOTAL_TIMEOUT_MS = 60_000L
    }
}
