package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.ui.state.MrStudyUiState

object MrStudyWidgetUpdater {

    fun syncFromUiState(context: Context, state: MrStudyUiState) {
        val store = MrStudyWidgetStore(context)
        val profile = state.profile
        val scheduleItems = state.studyPlans
            .filter { !it.isCompleted }
            .take(3)
            .map { "${it.subject} • ${it.topic.ifBlank { "Ders Planı" }} (${it.durationMinutes} dk)" }
        val flashcards = state.flashcards.map {
            WidgetFlashcard(subject = it.subject, front = it.front, back = it.back)
        }
        val latestNote = state.notes.firstOrNull()
        store.saveAppSync(
            displayName = profile.displayName,
            gradeLevel = profile.gradeLevel,
            xp = profile.xp,
            level = profile.level,
            streakDays = profile.streakDays,
            dailyGoalMinutes = profile.dailyGoalMinutes,
            totalStudyMinutes = profile.totalStudyMinutes,
            scheduleItems = scheduleItems,
            flashcards = flashcards,
            examCountdownLabel = state.studioState.examCountdownLabel,
            examTargetEpochMs = state.studioState.examTargetEpochMs,
            quickNoteTitle = latestNote?.title ?: "Hızlı Ders Notu",
            quickNoteBody = latestNote?.content?.take(140) ?: "Yeni not eklemek için dokun."
        )
        refreshAll(context)
    }

    fun refreshAll(context: Context) {
        val mgr = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
        val snap = MrStudyWidgetStore(context).getSnapshot()

        updateProvider(context, mgr, AiTeacherWidgetProvider::class.java) { buildAiTeacherViews(context, snap) }
        updateProvider(context, mgr, DailyGoalWidgetProvider::class.java) { buildDailyGoalViews(context, snap) }
        updateProvider(context, mgr, FlashcardWidgetProvider::class.java) { buildFlashcardViews(context, snap) }
        updateProvider(context, mgr, ScheduleWidgetProvider::class.java) { buildScheduleViews(context, snap) }
        updateProvider(context, mgr, XpStreakWidgetProvider::class.java) { buildXpStreakViews(context, snap) }
        updateProvider(context, mgr, ExamCountdownWidgetProvider::class.java) { buildExamCountdownViews(context, snap) }
        updateProvider(context, mgr, StudyStopwatchWidgetProvider::class.java) { buildStopwatchViews(context, snap) }
        updateProvider(context, mgr, QuickNoteWidgetProvider::class.java) { buildQuickNoteViews(context, snap) }
        updateProvider(context, mgr, DailyQuestionWidgetProvider::class.java) { buildDailyQuestionViews(context, snap) }
        updateProvider(context, mgr, MotivationWidgetProvider::class.java) { buildMotivationViews(context, snap) }
    }

    private fun updateProvider(
        context: Context,
        mgr: AppWidgetManager,
        cls: Class<*>,
        builder: () -> RemoteViews
    ) {
        val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
        if (ids.isNotEmpty()) {
            val views = builder()
            ids.forEach { mgr.updateAppWidget(it, views) }
        }
    }

    private fun launchAppIntent(context: Context, targetRoute: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MrStudyWidgetContract.EXTRA_TARGET_ROUTE, targetRoute)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionBroadcastIntent(
        context: Context,
        cls: Class<*>,
        action: String,
        requestCode: Int,
        optionIndex: Int = -1
    ): PendingIntent {
        val intent = Intent(context, cls).apply {
            this.action = action
            if (optionIndex >= 0) putExtra(MrStudyWidgetContract.EXTRA_OPTION_INDEX, optionIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildAiTeacherViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_ai_teacher).apply {
            val title = snap.customHeaderTitle.ifBlank { "MrStudy 4.0 • AI Öğretmen" }
            setTextViewText(R.id.widget_title, title)
            setTextViewText(R.id.widget_subtitle, "${snap.displayName} (${snap.gradeLevel}) için anında ders & soru çözümü.")
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "teacher", 101))
            setOnClickPendingIntent(R.id.btn_ask_ai, launchAppIntent(context, "teacher", 102))
            setOnClickPendingIntent(R.id.btn_open_live, launchAppIntent(context, "live_lesson", 103))
        }

    fun buildDailyGoalViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_daily_goal).apply {
            val pct = ((snap.todayMinutes.toFloat() / snap.dailyGoalMinutes.coerceAtLeast(1L)) * 100f)
                .toInt()
                .coerceIn(0, 100)
            setTextViewText(R.id.goal_progress_text, "${snap.todayMinutes} / ${snap.dailyGoalMinutes} dk")
            setProgressBar(R.id.goal_progress_bar, 100, pct, false)
            setTextViewText(
                R.id.goal_status_caption,
                if (pct >= 100) "🎉 Günlük hedef tamamlandı!" else "Hedefe %$pct yaklaştın • Devam et"
            )
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "dashboard", 201))
        }

    fun buildFlashcardViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_flashcard).apply {
            val card = snap.flashcards[snap.flashcardIndex % snap.flashcards.size.coerceAtLeast(1)]
            val sideText = if (snap.flashcardFlipped) "Cevap: ${card.back}" else card.front
            setTextViewText(R.id.flashcard_subject, "Flash Kart • ${card.subject}")
            setTextViewText(R.id.flashcard_body, sideText)
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "flashcards", 301))
            setOnClickPendingIntent(
                R.id.btn_fc_flip,
                actionBroadcastIntent(context, FlashcardWidgetProvider::class.java, MrStudyWidgetContract.ACTION_FLASHCARD_FLIP, 302)
            )
            setOnClickPendingIntent(
                R.id.btn_fc_next,
                actionBroadcastIntent(context, FlashcardWidgetProvider::class.java, MrStudyWidgetContract.ACTION_FLASHCARD_NEXT, 303)
            )
        }

    fun buildScheduleViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_schedule).apply {
            val s0 = snap.scheduleItems.getOrElse(0) { "Plan eklemek için dokun" }
            val s1 = snap.scheduleItems.getOrElse(1) { "Düzenli tekrar XP kazandırır" }
            val s2 = snap.scheduleItems.getOrElse(2) { "Notlar & Plan Stüdyosu" }
            setTextViewText(R.id.schedule_line_1, "• $s0")
            setTextViewText(R.id.schedule_line_2, "• $s1")
            setTextViewText(R.id.schedule_line_3, "• $s2")
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "study", 401))
        }

    fun buildXpStreakViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_xp_streak).apply {
            setTextViewText(R.id.xp_level_text, "Seviye ${snap.level} • ${snap.xp} XP")
            setTextViewText(R.id.streak_days_text, "🔥 ${snap.streakDays} Gün Kesintisiz Seri")
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "dashboard", 501))
        }

    fun buildExamCountdownViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_exam_countdown).apply {
            val days = ((snap.examTargetEpochMs - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L)
            setTextViewText(R.id.countdown_label, "Geri Sayım • ${snap.examCountdownLabel}")
            setTextViewText(R.id.countdown_days, "$days Gün Kaldı")
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "exam", 601))
        }

    fun buildStopwatchViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_study_stopwatch).apply {
            val nowEpoch = System.currentTimeMillis()
            val runningElapsed = if (snap.stopwatchRunning && snap.stopwatchStartMs > 0L) {
                (nowEpoch - snap.stopwatchStartMs).coerceAtLeast(0L)
            } else 0L
            val totalElapsed = snap.stopwatchAccumulatedMs + runningElapsed
            val base = SystemClock.elapsedRealtime() - totalElapsed
            setChronometer(R.id.stopwatch_chrono, base, null, snap.stopwatchRunning)
            val secs = (totalElapsed / 1000L)
            val mins = secs / 60L
            val remSecs = secs % 60L
            setTextViewText(
                R.id.stopwatch_status,
                when {
                    snap.stopwatchRunning -> "Çalışıyor... Odaklan!"
                    totalElapsed > 0L -> String.format("Duraklatıldı (%02d:%02d)", mins, remSecs)
                    else -> "Hazır • Başlat'a bas"
                }
            )
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "dashboard", 701))
            setOnClickPendingIntent(
                R.id.btn_sw_start,
                actionBroadcastIntent(context, StudyStopwatchWidgetProvider::class.java, MrStudyWidgetContract.ACTION_STOPWATCH_START, 702)
            )
            setOnClickPendingIntent(
                R.id.btn_sw_pause,
                actionBroadcastIntent(context, StudyStopwatchWidgetProvider::class.java, MrStudyWidgetContract.ACTION_STOPWATCH_PAUSE, 703)
            )
            setOnClickPendingIntent(
                R.id.btn_sw_finish,
                actionBroadcastIntent(context, StudyStopwatchWidgetProvider::class.java, MrStudyWidgetContract.ACTION_STOPWATCH_FINISH, 704)
            )
        }

    fun buildQuickNoteViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_quick_note).apply {
            setTextViewText(R.id.quick_note_header, snap.quickNoteTitle)
            setTextViewText(R.id.quick_note_body, snap.quickNoteBody)
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "study", 801))
            setOnClickPendingIntent(R.id.btn_open_notes, launchAppIntent(context, "study", 802))
        }

    fun buildDailyQuestionViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_daily_question).apply {
            val q = snap.dailyQuestions[snap.dailyQuestionIndex % snap.dailyQuestions.size.coerceAtLeast(1)]
            setTextViewText(R.id.dq_subject, "Günün Sorusu • ${q.subject}")
            setTextViewText(R.id.dq_prompt, q.prompt)
            val feedback = when (snap.dailyQuestionSelected) {
                -1 -> "Doğru cevaba dokun"
                q.correctIndex -> "✅ Doğru! Harika iş."
                else -> "❌ Yanlış! Doğrusu: ${q.options.getOrElse(q.correctIndex) { "" }}"
            }
            setTextViewText(R.id.dq_feedback, feedback)
            val btnIds = listOf(R.id.btn_dq_opt_0, R.id.btn_dq_opt_1, R.id.btn_dq_opt_2, R.id.btn_dq_opt_3)
            btnIds.forEachIndexed { idx, viewId ->
                setTextViewText(viewId, q.options.getOrElse(idx) { "${'A' + idx}" })
                setOnClickPendingIntent(
                    viewId,
                    actionBroadcastIntent(
                        context,
                        DailyQuestionWidgetProvider::class.java,
                        MrStudyWidgetContract.ACTION_DAILY_QUESTION_ANSWER,
                        910 + idx,
                        idx
                    )
                )
            }
            setOnClickPendingIntent(
                R.id.btn_dq_next,
                actionBroadcastIntent(
                    context,
                    DailyQuestionWidgetProvider::class.java,
                    MrStudyWidgetContract.ACTION_DAILY_QUESTION_NEXT,
                    905
                )
            )
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "exam", 901))
        }

    fun buildMotivationViews(context: Context, snap: WidgetSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_motivation).apply {
            val quotes = MrStudyWidgetStore.MOTIVATION_QUOTES
            val quote = quotes[snap.motivationIndex % quotes.size]
            setTextViewText(R.id.motivation_quote, quote)
            setOnClickPendingIntent(R.id.widget_root, launchAppIntent(context, "dashboard", 1001))
            setOnClickPendingIntent(
                R.id.btn_motivation_next,
                actionBroadcastIntent(
                    context,
                    MotivationWidgetProvider::class.java,
                    MrStudyWidgetContract.ACTION_MOTIVATION_NEXT,
                    1002
                )
            )
        }
}
