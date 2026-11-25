package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

abstract class BaseMrStudyWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        MrStudyWidgetUpdater.refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val store = MrStudyWidgetStore(context)
        when (intent.action) {
            MrStudyWidgetContract.ACTION_FLASHCARD_FLIP -> {
                store.flipFlashcard()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_FLASHCARD_NEXT -> {
                store.nextFlashcard()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_STOPWATCH_START -> {
                store.startStopwatch()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_STOPWATCH_PAUSE -> {
                store.pauseStopwatch()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_STOPWATCH_FINISH -> {
                store.finishStopwatchAndLog()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_DAILY_QUESTION_ANSWER -> {
                val idx = intent.getIntExtra(MrStudyWidgetContract.EXTRA_OPTION_INDEX, -1)
                if (idx >= 0) {
                    store.answerDailyQuestion(idx)
                    MrStudyWidgetUpdater.refreshAll(context)
                }
            }
            MrStudyWidgetContract.ACTION_DAILY_QUESTION_NEXT -> {
                store.nextDailyQuestion()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_MOTIVATION_NEXT -> {
                store.nextMotivationQuote()
                MrStudyWidgetUpdater.refreshAll(context)
            }
            MrStudyWidgetContract.ACTION_REFRESH_ALL -> {
                MrStudyWidgetUpdater.refreshAll(context)
            }
        }
    }
}

class AiTeacherWidgetProvider : BaseMrStudyWidgetProvider()
class DailyGoalWidgetProvider : BaseMrStudyWidgetProvider()
class FlashcardWidgetProvider : BaseMrStudyWidgetProvider()
class ScheduleWidgetProvider : BaseMrStudyWidgetProvider()
class XpStreakWidgetProvider : BaseMrStudyWidgetProvider()
class ExamCountdownWidgetProvider : BaseMrStudyWidgetProvider()
class StudyStopwatchWidgetProvider : BaseMrStudyWidgetProvider()
class QuickNoteWidgetProvider : BaseMrStudyWidgetProvider()
class DailyQuestionWidgetProvider : BaseMrStudyWidgetProvider()
class MotivationWidgetProvider : BaseMrStudyWidgetProvider()
