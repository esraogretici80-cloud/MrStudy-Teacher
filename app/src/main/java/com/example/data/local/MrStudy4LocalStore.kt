package com.example.data.local

import android.content.Context
import com.example.data.model.AppNotificationItem
import com.example.data.model.AudioLessonItem
import com.example.data.model.CodeProjectFile
import com.example.data.model.CodingLanguage
import com.example.data.model.Custom3dGameConfig
import com.example.data.model.DiagramEdge
import com.example.data.model.DiagramNode
import com.example.data.model.DiagramProject
import com.example.data.model.DiagramType
import com.example.data.model.FlashcardReviewRating
import com.example.data.model.Game3dQuestion
import com.example.data.model.GameGenre
import com.example.data.model.PresentationDeck
import com.example.data.model.PresentationSlide
import com.example.data.model.SavedMathBoard
import com.example.data.model.VoiceNoteRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

data class MrStudy4LocalState(
    val lowPower3dMode: Boolean = false,
    val customGames: List<Custom3dGameConfig> = emptyList(),
    val diagrams: List<DiagramProject> = emptyList(),
    val codeFiles: List<CodeProjectFile> = emptyList(),
    val mathBoards: List<SavedMathBoard> = emptyList(),
    val presentations: List<PresentationDeck> = emptyList(),
    val voiceNotes: List<VoiceNoteRecord> = emptyList(),
    val audioLessons: List<AudioLessonItem> = emptyList(),
    val notifications: List<AppNotificationItem> = emptyList(),
    val flashcardRatings: Map<String, FlashcardReviewRating> = emptyMap(),
    val examCountdownLabel: String = "YKS / LGS Hazırlık",
    val examTargetEpochMs: Long = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L
)

class MrStudy4LocalStore(context: Context, userId: String) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "mrstudy4_studio_$userId",
        Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<MrStudy4LocalState> = _state.asStateFlow()

    private fun loadInitial(): MrStudy4LocalState {
        val defaultNotifications = listOf(
            AppNotificationItem(
                id = "welcome_4_0",
                title = "MrStudy 4.0 Hazır!",
                body = "3B Bilim Laboratuvarı, Kodlama Stüdyosu, Diyagram Tahtası ve 10 Ana Ekran Widget'ı eklendi.",
                tag = "Yeni"
            )
        )
        val defaultCodeFiles = listOf(
            CodeProjectFile(
                id = "starter_py",
                name = "merhaba.py",
                language = CodingLanguage.PYTHON,
                code = CodingLanguage.PYTHON.starterTemplate
            ),
            CodeProjectFile(
                id = "starter_html",
                name = "kart.html",
                language = CodingLanguage.HTML,
                code = CodingLanguage.HTML.starterTemplate
            )
        )
        return MrStudy4LocalState(
            lowPower3dMode = prefs.getBoolean(KEY_LOW_POWER_3D, false),
            customGames = parseGames(prefs.getString(KEY_CUSTOM_GAMES, null)),
            diagrams = parseDiagrams(prefs.getString(KEY_DIAGRAMS, null)),
            codeFiles = parseCodeFiles(prefs.getString(KEY_CODE_FILES, null)).ifEmpty { defaultCodeFiles },
            mathBoards = parseMathBoards(prefs.getString(KEY_MATH_BOARDS, null)),
            presentations = parsePresentations(prefs.getString(KEY_PRESENTATIONS, null)),
            voiceNotes = parseVoiceNotes(prefs.getString(KEY_VOICE_NOTES, null)),
            audioLessons = parseAudioLessons(prefs.getString(KEY_AUDIO_LESSONS, null)),
            notifications = parseNotifications(prefs.getString(KEY_NOTIFICATIONS, null)).ifEmpty { defaultNotifications },
            flashcardRatings = parseRatings(prefs.getString(KEY_CARD_RATINGS, null)),
            examCountdownLabel = prefs.getString(KEY_COUNTDOWN_LABEL, "Hedef Sınav") ?: "Hedef Sınav",
            examTargetEpochMs = prefs.getLong(
                KEY_COUNTDOWN_EPOCH,
                System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L
            )
        )
    }

    fun setLowPower3dMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOW_POWER_3D, enabled).apply()
        _state.update { it.copy(lowPower3dMode = enabled) }
    }

    fun setExamCountdown(label: String, targetEpochMs: Long) {
        prefs.edit()
            .putString(KEY_COUNTDOWN_LABEL, label)
            .putLong(KEY_COUNTDOWN_EPOCH, targetEpochMs)
            .apply()
        _state.update { it.copy(examCountdownLabel = label, examTargetEpochMs = targetEpochMs) }
    }

    fun saveCustomGame(game: Custom3dGameConfig) {
        _state.update { current ->
            val updated = (listOf(game) + current.customGames.filterNot { it.id == game.id }).take(25)
            prefs.edit().putString(KEY_CUSTOM_GAMES, serializeGames(updated)).apply()
            current.copy(customGames = updated)
        }
    }

    fun deleteCustomGame(id: String) {
        _state.update { current ->
            val updated = current.customGames.filterNot { it.id == id }
            prefs.edit().putString(KEY_CUSTOM_GAMES, serializeGames(updated)).apply()
            current.copy(customGames = updated)
        }
    }

    fun saveDiagram(diagram: DiagramProject) {
        _state.update { current ->
            val updated = (listOf(diagram) + current.diagrams.filterNot { it.id == diagram.id }).take(30)
            prefs.edit().putString(KEY_DIAGRAMS, serializeDiagrams(updated)).apply()
            current.copy(diagrams = updated)
        }
    }

    fun deleteDiagram(id: String) {
        _state.update { current ->
            val updated = current.diagrams.filterNot { it.id == id }
            prefs.edit().putString(KEY_DIAGRAMS, serializeDiagrams(updated)).apply()
            current.copy(diagrams = updated)
        }
    }

    fun saveCodeFile(file: CodeProjectFile) {
        _state.update { current ->
            val updated = (listOf(file) + current.codeFiles.filterNot { it.id == file.id }).take(40)
            prefs.edit().putString(KEY_CODE_FILES, serializeCodeFiles(updated)).apply()
            current.copy(codeFiles = updated)
        }
    }

    fun deleteCodeFile(id: String) {
        _state.update { current ->
            val updated = current.codeFiles.filterNot { it.id == id }
            prefs.edit().putString(KEY_CODE_FILES, serializeCodeFiles(updated)).apply()
            current.copy(codeFiles = updated)
        }
    }

    fun saveMathBoard(board: SavedMathBoard) {
        _state.update { current ->
            val updated = (listOf(board) + current.mathBoards.filterNot { it.id == board.id }).take(30)
            prefs.edit().putString(KEY_MATH_BOARDS, serializeMathBoards(updated)).apply()
            current.copy(mathBoards = updated)
        }
    }

    fun deleteMathBoard(id: String) {
        _state.update { current ->
            val updated = current.mathBoards.filterNot { it.id == id }
            prefs.edit().putString(KEY_MATH_BOARDS, serializeMathBoards(updated)).apply()
            current.copy(mathBoards = updated)
        }
    }

    fun savePresentation(deck: PresentationDeck) {
        _state.update { current ->
            val updated = (listOf(deck) + current.presentations.filterNot { it.id == deck.id }).take(25)
            prefs.edit().putString(KEY_PRESENTATIONS, serializePresentations(updated)).apply()
            current.copy(presentations = updated)
        }
    }

    fun deletePresentation(id: String) {
        _state.update { current ->
            val updated = current.presentations.filterNot { it.id == id }
            prefs.edit().putString(KEY_PRESENTATIONS, serializePresentations(updated)).apply()
            current.copy(presentations = updated)
        }
    }

    fun saveVoiceNote(record: VoiceNoteRecord) {
        _state.update { current ->
            val updated = (listOf(record) + current.voiceNotes.filterNot { it.id == record.id }).take(30)
            prefs.edit().putString(KEY_VOICE_NOTES, serializeVoiceNotes(updated)).apply()
            current.copy(voiceNotes = updated)
        }
    }

    fun saveAudioLesson(item: AudioLessonItem) {
        _state.update { current ->
            val updated = (listOf(item) + current.audioLessons.filterNot { it.id == item.id }).take(30)
            prefs.edit().putString(KEY_AUDIO_LESSONS, serializeAudioLessons(updated)).apply()
            current.copy(audioLessons = updated)
        }
    }

    fun addNotification(title: String, body: String, tag: String = "Akademi") {
        _state.update { current ->
            val item = AppNotificationItem(title = title, body = body, tag = tag)
            val updated = (listOf(item) + current.notifications).take(30)
            prefs.edit().putString(KEY_NOTIFICATIONS, serializeNotifications(updated)).apply()
            current.copy(notifications = updated)
        }
    }

    fun markAllNotificationsRead() {
        _state.update { current ->
            val updated = current.notifications.map { it.copy(isRead = true) }
            prefs.edit().putString(KEY_NOTIFICATIONS, serializeNotifications(updated)).apply()
            current.copy(notifications = updated)
        }
    }

    fun setFlashcardRating(cardId: String, rating: FlashcardReviewRating) {
        _state.update { current ->
            val updated = current.flashcardRatings.toMutableMap().apply { put(cardId, rating) }
            val json = JSONObject().apply { updated.forEach { (k, v) -> put(k, v.name) } }.toString()
            prefs.edit().putString(KEY_CARD_RATINGS, json).apply()
            current.copy(flashcardRatings = updated)
        }
    }

    private fun serializeGames(list: List<Custom3dGameConfig>): String = JSONArray().apply {
        list.forEach { g ->
            put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("subject", g.subject)
                put("topic", g.topic)
                put("genre", g.genre.name)
                put("heroName", g.heroName)
                put("environmentName", g.environmentName)
                put("ruleSummary", g.ruleSummary)
                put("highScore", g.highScore)
                put("createdAtMs", g.createdAtMs)
                put("questions", JSONArray().apply {
                    g.questions.forEach { q ->
                        put(JSONObject().apply {
                            put("prompt", q.prompt)
                            put("options", JSONArray(q.options))
                            put("correctIndex", q.correctIndex)
                            put("explanation", q.explanation)
                        })
                    }
                })
                put("wordPairs", JSONArray().apply {
                    g.wordPairs.forEach { (a, b) ->
                        put(JSONObject().apply { put("a", a); put("b", b) })
                    }
                })
            })
        }
    }.toString()

    private fun parseGames(raw: String?): List<Custom3dGameConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val qArr = o.optJSONArray("questions") ?: JSONArray()
                val qs = (0 until qArr.length()).map { qi ->
                    val qo = qArr.getJSONObject(qi)
                    val opts = qo.optJSONArray("options")?.let { oa -> (0 until oa.length()).map { oa.getString(it) } } ?: emptyList()
                    Game3dQuestion(
                        prompt = qo.optString("prompt"),
                        options = opts,
                        correctIndex = qo.optInt("correctIndex", 0),
                        explanation = qo.optString("explanation")
                    )
                }
                val pArr = o.optJSONArray("wordPairs") ?: JSONArray()
                val pairs = (0 until pArr.length()).map { pi ->
                    val po = pArr.getJSONObject(pi)
                    po.optString("a") to po.optString("b")
                }
                val genre = GameGenre.entries.firstOrNull { it.name == o.optString("genre") } ?: GameGenre.SPACE_PLATFORM
                Custom3dGameConfig(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    subject = o.optString("subject"),
                    topic = o.optString("topic"),
                    genre = genre,
                    heroName = o.optString("heroName", genre.defaultHero),
                    environmentName = o.optString("environmentName", genre.defaultEnvironment),
                    ruleSummary = o.optString("ruleSummary"),
                    questions = qs,
                    wordPairs = pairs,
                    highScore = o.optInt("highScore", 0),
                    createdAtMs = o.optLong("createdAtMs", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun serializeDiagramToJson(d: DiagramProject): String = JSONObject().apply {
        put("id", d.id)
        put("title", d.title)
        put("subject", d.subject)
        put("diagramType", d.diagramType.name)
        put("updatedAtMs", d.updatedAtMs)
        put("nodes", JSONArray().apply {
            d.nodes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id); put("label", n.label); put("note", n.note)
                    put("x", n.x.toDouble()); put("y", n.y.toDouble()); put("colorHex", n.colorHex)
                })
            }
        })
        put("edges", JSONArray().apply {
            d.edges.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("fromId", e.fromId); put("toId", e.toId); put("label", e.label)
                })
            }
        })
    }.toString(2)

    private fun serializeDiagrams(list: List<DiagramProject>): String = JSONArray().apply {
        list.forEach { put(JSONObject(serializeDiagramToJson(it))) }
    }.toString()

    private fun parseDiagrams(raw: String?): List<DiagramProject> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val nodesArr = o.optJSONArray("nodes") ?: JSONArray()
                val nodes = (0 until nodesArr.length()).map { ni ->
                    val no = nodesArr.getJSONObject(ni)
                    DiagramNode(
                        id = no.optString("id"),
                        label = no.optString("label"),
                        note = no.optString("note"),
                        x = no.optDouble("x", 0.5).toFloat(),
                        y = no.optDouble("y", 0.5).toFloat(),
                        colorHex = no.optLong("colorHex", 0xFF8B5CF6)
                    )
                }
                val edgesArr = o.optJSONArray("edges") ?: JSONArray()
                val edges = (0 until edgesArr.length()).map { ei ->
                    val eo = edgesArr.getJSONObject(ei)
                    DiagramEdge(
                        id = eo.optString("id"),
                        fromId = eo.optString("fromId"),
                        toId = eo.optString("toId"),
                        label = eo.optString("label")
                    )
                }
                val dtype = DiagramType.entries.firstOrNull { it.name == o.optString("diagramType") } ?: DiagramType.CONCEPT_MAP
                DiagramProject(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    subject = o.optString("subject"),
                    diagramType = dtype,
                    nodes = nodes,
                    edges = edges,
                    updatedAtMs = o.optLong("updatedAtMs", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeCodeFiles(list: List<CodeProjectFile>): String = JSONArray().apply {
        list.forEach { f ->
            put(JSONObject().apply {
                put("id", f.id); put("name", f.name); put("lang", f.language.name)
                put("code", f.code); put("updatedAtMs", f.updatedAtMs)
            })
        }
    }.toString()

    private fun parseCodeFiles(raw: String?): List<CodeProjectFile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val lang = CodingLanguage.entries.firstOrNull { it.name == o.optString("lang") } ?: CodingLanguage.PYTHON
                CodeProjectFile(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    language = lang,
                    code = o.optString("code"),
                    updatedAtMs = o.optLong("updatedAtMs", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeMathBoards(list: List<SavedMathBoard>): String = JSONArray().apply {
        list.forEach { b ->
            put(JSONObject().apply {
                put("id", b.id); put("title", b.title); put("expression", b.expression)
                put("solutionSummary", b.solutionSummary); put("updatedAtMs", b.updatedAtMs)
            })
        }
    }.toString()

    private fun parseMathBoards(raw: String?): List<SavedMathBoard> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SavedMathBoard(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    expression = o.optString("expression"),
                    solutionSummary = o.optString("solutionSummary"),
                    updatedAtMs = o.optLong("updatedAtMs", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializePresentations(list: List<PresentationDeck>): String = JSONArray().apply {
        list.forEach { d ->
            put(JSONObject().apply {
                put("id", d.id); put("title", d.title); put("subject", d.subject); put("createdAtMs", d.createdAtMs)
                put("slides", JSONArray().apply {
                    d.slides.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id); put("title", s.title)
                            put("bullets", JSONArray(s.bullets))
                            put("speakerNotes", s.speakerNotes)
                        })
                    }
                })
            })
        }
    }.toString()

    private fun parsePresentations(raw: String?): List<PresentationDeck> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val sArr = o.optJSONArray("slides") ?: JSONArray()
                val slides = (0 until sArr.length()).map { si ->
                    val so = sArr.getJSONObject(si)
                    val bullets = so.optJSONArray("bullets")?.let { b -> (0 until b.length()).map { b.getString(it) } } ?: emptyList()
                    PresentationSlide(so.optString("id"), so.optString("title"), bullets, so.optString("speakerNotes"))
                }
                PresentationDeck(o.optString("id"), o.optString("title"), o.optString("subject"), slides, o.optLong("createdAtMs", 0L))
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeVoiceNotes(list: List<VoiceNoteRecord>): String = JSONArray().apply {
        list.forEach { v ->
            put(JSONObject().apply {
                put("id", v.id); put("title", v.title); put("subject", v.subject)
                put("rawTranscript", v.rawTranscript); put("cleanedSummary", v.cleanedSummary)
                put("createdAtMs", v.createdAtMs)
            })
        }
    }.toString()

    private fun parseVoiceNotes(raw: String?): List<VoiceNoteRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                VoiceNoteRecord(
                    o.optString("id"), o.optString("title"), o.optString("subject"),
                    o.optString("rawTranscript"), o.optString("cleanedSummary"), o.optLong("createdAtMs", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeAudioLessons(list: List<AudioLessonItem>): String = JSONArray().apply {
        list.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id); put("title", a.title); put("subject", a.subject)
                put("scriptText", a.scriptText); put("createdAtMs", a.createdAtMs)
            })
        }
    }.toString()

    private fun parseAudioLessons(raw: String?): List<AudioLessonItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AudioLessonItem(o.optString("id"), o.optString("title"), o.optString("subject"), o.optString("scriptText"), o.optLong("createdAtMs", 0L))
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeNotifications(list: List<AppNotificationItem>): String = JSONArray().apply {
        list.forEach { n ->
            put(JSONObject().apply {
                put("id", n.id); put("title", n.title); put("body", n.body)
                put("tag", n.tag); put("timestampMs", n.timestampMs); put("isRead", n.isRead)
            })
        }
    }.toString()

    private fun parseNotifications(raw: String?): List<AppNotificationItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AppNotificationItem(
                    o.optString("id"), o.optString("title"), o.optString("body"),
                    o.optString("tag"), o.optLong("timestampMs", 0L), o.optBoolean("isRead", false)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun parseRatings(raw: String?): Map<String, FlashcardReviewRating> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { k ->
                    val r = FlashcardReviewRating.entries.firstOrNull { it.name == obj.optString(k) }
                    if (r != null) put(k, r)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private companion object {
        const val KEY_LOW_POWER_3D = "low_power_3d"
        const val KEY_CUSTOM_GAMES = "custom_games_v4"
        const val KEY_DIAGRAMS = "diagrams_v4"
        const val KEY_CODE_FILES = "code_files_v4"
        const val KEY_MATH_BOARDS = "math_boards_v4"
        const val KEY_PRESENTATIONS = "presentations_v4"
        const val KEY_VOICE_NOTES = "voice_notes_v4"
        const val KEY_AUDIO_LESSONS = "audio_lessons_v4"
        const val KEY_NOTIFICATIONS = "notifications_v4"
        const val KEY_CARD_RATINGS = "flashcard_ratings_v4"
        const val KEY_COUNTDOWN_LABEL = "exam_countdown_label"
        const val KEY_COUNTDOWN_EPOCH = "exam_countdown_epoch"
    }
}
