package com.example.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WidgetFlashcard(
    val subject: String,
    val front: String,
    val back: String
)

data class WidgetDailyQuestion(
    val subject: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

data class WidgetSnapshot(
    val displayName: String,
    val gradeLevel: String,
    val xp: Long,
    val level: Long,
    val streakDays: Long,
    val dailyGoalMinutes: Long,
    val todayMinutes: Long,
    val scheduleItems: List<String>,
    val flashcards: List<WidgetFlashcard>,
    val flashcardIndex: Int,
    val flashcardFlipped: Boolean,
    val examCountdownLabel: String,
    val examTargetEpochMs: Long,
    val stopwatchRunning: Boolean,
    val stopwatchStartMs: Long,
    val stopwatchAccumulatedMs: Long,
    val pendingStopwatchMinutes: Int,
    val quickNoteTitle: String,
    val quickNoteBody: String,
    val dailyQuestions: List<WidgetDailyQuestion>,
    val dailyQuestionIndex: Int,
    val dailyQuestionSelected: Int,
    val motivationIndex: Int,
    val customHeaderTitle: String
)

class MrStudyWidgetStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "mrstudy4_widgets_shared",
        Context.MODE_PRIVATE
    )

    fun saveAppSync(
        displayName: String,
        gradeLevel: String,
        xp: Long,
        level: Long,
        streakDays: Long,
        dailyGoalMinutes: Long,
        totalStudyMinutes: Long,
        scheduleItems: List<String>,
        flashcards: List<WidgetFlashcard>,
        examCountdownLabel: String,
        examTargetEpochMs: Long,
        quickNoteTitle: String,
        quickNoteBody: String
    ) {
        val fcJson = JSONArray().apply {
            flashcards.take(30).forEach { fc ->
                put(JSONObject().apply {
                    put("subject", fc.subject)
                    put("front", fc.front)
                    put("back", fc.back)
                })
            }
        }.toString()

        val schedJson = JSONArray(scheduleItems.take(8)).toString()

        prefs.edit()
            .putString(K_NAME, displayName.ifBlank { "Öğrenci" })
            .putString(K_GRADE, gradeLevel.ifBlank { "9. Sınıf" })
            .putLong(K_XP, xp.coerceAtLeast(0L))
            .putLong(K_LEVEL, level.coerceAtLeast(1L))
            .putLong(K_STREAK, streakDays.coerceAtLeast(0L))
            .putLong(K_GOAL_MINS, dailyGoalMinutes.coerceIn(10L, 480L))
            .putLong(K_TODAY_MINS, totalStudyMinutes.coerceAtLeast(0L))
            .putString(K_SCHEDULE, schedJson)
            .putString(K_FLASHCARDS, fcJson)
            .putString(K_COUNTDOWN_LABEL, examCountdownLabel.ifBlank { "Hedef Sınav" })
            .putLong(K_COUNTDOWN_EPOCH, examTargetEpochMs)
            .putString(K_NOTE_TITLE, quickNoteTitle)
            .putString(K_NOTE_BODY, quickNoteBody)
            .apply()
    }

    fun getSnapshot(): WidgetSnapshot {
        val rawSchedule = prefs.getString(K_SCHEDULE, null)
        val schedList = if (!rawSchedule.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(rawSchedule)
                (0 until arr.length()).map { arr.getString(it) }
            }.getOrDefault(emptyList())
        } else {
            listOf("Matematik • Konu Tekrarı (30 dk)", "Fen Bilimleri • Test Çözümü (25 dk)")
        }

        val rawFc = prefs.getString(K_FLASHCARDS, null)
        val fcList = if (!rawFc.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(rawFc)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    WidgetFlashcard(o.optString("subject", "Genel"), o.optString("front"), o.optString("back"))
                }
            }.getOrDefault(emptyList())
        } else emptyList()

        val resolvedFc = fcList.ifEmpty {
            listOf(
                WidgetFlashcard("Fizik", "Newton'un 2. Yasası nedir?", "F = m · a (Net kuvvet = Kütle × İvme)"),
                WidgetFlashcard("Matematik", "Pisagor Bağıntısı formülü?", "Dik üçgende a² + b² = c²"),
                WidgetFlashcard("Biyoloji", "Hücrede ATP nerede üretilir?", "Mitokondri organelinde üretilir.")
            )
        }

        val questions = DEFAULT_DAILY_QUESTIONS

        return WidgetSnapshot(
            displayName = prefs.getString(K_NAME, "Öğrenci") ?: "Öğrenci",
            gradeLevel = prefs.getString(K_GRADE, "9. Sınıf") ?: "9. Sınıf",
            xp = prefs.getLong(K_XP, 50L),
            level = prefs.getLong(K_LEVEL, 1L),
            streakDays = prefs.getLong(K_STREAK, 1L),
            dailyGoalMinutes = prefs.getLong(K_GOAL_MINS, 45L),
            todayMinutes = prefs.getLong(K_TODAY_MINS, 0L),
            scheduleItems = schedList,
            flashcards = resolvedFc,
            flashcardIndex = prefs.getInt(K_FC_INDEX, 0).coerceAtLeast(0),
            flashcardFlipped = prefs.getBoolean(K_FC_FLIPPED, false),
            examCountdownLabel = prefs.getString(K_COUNTDOWN_LABEL, "Hedef Sınav") ?: "Hedef Sınav",
            examTargetEpochMs = prefs.getLong(K_COUNTDOWN_EPOCH, System.currentTimeMillis() + 30L * 86_400_000L),
            stopwatchRunning = prefs.getBoolean(K_SW_RUNNING, false),
            stopwatchStartMs = prefs.getLong(K_SW_START_MS, 0L),
            stopwatchAccumulatedMs = prefs.getLong(K_SW_ACCUM_MS, 0L),
            pendingStopwatchMinutes = prefs.getInt(K_SW_PENDING_MINS, 0),
            quickNoteTitle = prefs.getString(K_NOTE_TITLE, "Hızlı Ders Notu") ?: "Hızlı Ders Notu",
            quickNoteBody = prefs.getString(K_NOTE_BODY, "Yeni not eklemek için dokun.") ?: "Yeni not eklemek için dokun.",
            dailyQuestions = questions,
            dailyQuestionIndex = prefs.getInt(K_DQ_INDEX, 0).coerceAtLeast(0),
            dailyQuestionSelected = prefs.getInt(K_DQ_SELECTED, -1),
            motivationIndex = prefs.getInt(K_MOTIV_INDEX, 0).coerceAtLeast(0),
            customHeaderTitle = prefs.getString(K_CONFIG_LABEL, "") ?: ""
        )
    }

    fun flipFlashcard() {
        val cur = prefs.getBoolean(K_FC_FLIPPED, false)
        prefs.edit().putBoolean(K_FC_FLIPPED, !cur).apply()
    }

    fun nextFlashcard() {
        val snap = getSnapshot()
        val next = (snap.flashcardIndex + 1) % snap.flashcards.size.coerceAtLeast(1)
        prefs.edit().putInt(K_FC_INDEX, next).putBoolean(K_FC_FLIPPED, false).apply()
    }

    fun startStopwatch() {
        val running = prefs.getBoolean(K_SW_RUNNING, false)
        if (!running) {
            prefs.edit()
                .putBoolean(K_SW_RUNNING, true)
                .putLong(K_SW_START_MS, System.currentTimeMillis())
                .apply()
        }
    }

    fun pauseStopwatch() {
        val running = prefs.getBoolean(K_SW_RUNNING, false)
        if (running) {
            val start = prefs.getLong(K_SW_START_MS, System.currentTimeMillis())
            val accum = prefs.getLong(K_SW_ACCUM_MS, 0L) + (System.currentTimeMillis() - start).coerceAtLeast(0L)
            prefs.edit()
                .putBoolean(K_SW_RUNNING, false)
                .putLong(K_SW_ACCUM_MS, accum)
                .putLong(K_SW_START_MS, 0L)
                .apply()
        }
    }

    fun finishStopwatchAndLog(): Int {
        val running = prefs.getBoolean(K_SW_RUNNING, false)
        val extra = if (running) {
            (System.currentTimeMillis() - prefs.getLong(K_SW_START_MS, System.currentTimeMillis())).coerceAtLeast(0L)
        } else 0L
        val totalMs = prefs.getLong(K_SW_ACCUM_MS, 0L) + extra
        val mins = if (totalMs >= 10_000L) (totalMs / 60_000L).toInt().coerceAtLeast(1) else 0
        val currentPending = prefs.getInt(K_SW_PENDING_MINS, 0)
        val currentToday = prefs.getLong(K_TODAY_MINS, 0L)
        prefs.edit()
            .putBoolean(K_SW_RUNNING, false)
            .putLong(K_SW_START_MS, 0L)
            .putLong(K_SW_ACCUM_MS, 0L)
            .putInt(K_SW_PENDING_MINS, currentPending + mins)
            .putLong(K_TODAY_MINS, currentToday + mins)
            .apply()
        return mins
    }

    fun consumePendingStopwatchMinutes(): Int {
        val mins = prefs.getInt(K_SW_PENDING_MINS, 0)
        if (mins > 0) {
            prefs.edit().putInt(K_SW_PENDING_MINS, 0).apply()
        }
        return mins
    }

    fun answerDailyQuestion(optionIndex: Int) {
        prefs.edit().putInt(K_DQ_SELECTED, optionIndex).apply()
    }

    fun nextDailyQuestion() {
        val idx = (prefs.getInt(K_DQ_INDEX, 0) + 1) % DEFAULT_DAILY_QUESTIONS.size
        prefs.edit().putInt(K_DQ_INDEX, idx).putInt(K_DQ_SELECTED, -1).apply()
    }

    fun nextMotivationQuote() {
        val idx = (prefs.getInt(K_MOTIV_INDEX, 0) + 1) % MOTIVATION_QUOTES.size
        prefs.edit().putInt(K_MOTIV_INDEX, idx).apply()
    }

    fun saveWidgetConfigLabel(widgetId: Int, label: String) {
        prefs.edit().putString("${K_WIDGET_PREFIX}_$widgetId", label).apply()
        if (label.isNotBlank()) {
            prefs.edit().putString(K_CONFIG_LABEL, label).apply()
        }
    }

    companion object {
        private const val K_NAME = "name"
        private const val K_GRADE = "grade"
        private const val K_XP = "xp"
        private const val K_LEVEL = "level"
        private const val K_STREAK = "streak"
        private const val K_GOAL_MINS = "goal_mins"
        private const val K_TODAY_MINS = "today_mins"
        private const val K_SCHEDULE = "schedule"
        private const val K_FLASHCARDS = "flashcards"
        private const val K_FC_INDEX = "fc_idx"
        private const val K_FC_FLIPPED = "fc_flipped"
        private const val K_COUNTDOWN_LABEL = "cd_label"
        private const val K_COUNTDOWN_EPOCH = "cd_epoch"
        private const val K_SW_RUNNING = "sw_running"
        private const val K_SW_START_MS = "sw_start"
        private const val K_SW_ACCUM_MS = "sw_accum"
        private const val K_SW_PENDING_MINS = "sw_pending_mins"
        private const val K_NOTE_TITLE = "note_title"
        private const val K_NOTE_BODY = "note_body"
        private const val K_DQ_INDEX = "dq_idx"
        private const val K_DQ_SELECTED = "dq_selected"
        private const val K_MOTIV_INDEX = "motiv_idx"
        private const val K_CONFIG_LABEL = "config_label"
        private const val K_WIDGET_PREFIX = "widget_label"

        val MOTIVATION_QUOTES = listOf(
            "Her gün atılan küçük bir adım, büyük hedeflerin mimarıdır.",
            "Zorlandığın konu, seni bir üst seviyeye taşıyacak basamaktır.",
            "Düzenli tekrar, bilgiyi kalıcı güce dönüştürür.",
            "Bugün çözdüğün her soru, yarınki özgüvenini inşa eder.",
            "Odaklan, planla, keşfet: MrStudy 4.0 hep yanında!"
        )

        val DEFAULT_DAILY_QUESTIONS = listOf(
            WidgetDailyQuestion(
                "Matematik",
                "3x - 7 = 14 denkleminde x değeri kaçtır?",
                listOf("5", "6", "7", "8"),
                2
            ),
            WidgetDailyQuestion(
                "Fen Bilimleri",
                "Bitki hücresinde fotosentez hangi organelde gerçekleşir?",
                listOf("Mitokondri", "Kloroplast", "Ribozom", "Lizozom"),
                1
            ),
            WidgetDailyQuestion(
                "Fizik",
                "Kütlesi 4 kg olan cisme 20 N net kuvvet etki ederse ivmesi kaç m/s² olur?",
                listOf("4", "5", "6", "8"),
                1
            ),
            WidgetDailyQuestion(
                "Türkçe",
                "'Gözden düşmek' deyiminin anlamı aşağıdakilerden hangisidir?",
                listOf("Unutulmak", "Sevgi ve değeri azalmak", "Hastalanmak", "Şaşırmak"),
                1
            )
        )
    }
}
