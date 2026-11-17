package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

data class LocalParentMetrics(
    val parentPin: String = "1234",
    val totalQuestionsSolved: Int = 0,
    val totalLiveSeconds: Int = 0,
    val liveSessionsCount: Int = 0,
    val game3dPlayedCount: Int = 0,
    val game3dTotalQuestions: Int = 0,
    val game3dCorrectAnswers: Int = 0,
    val game3dBestScore: Int = 0,
    val subjectProgress: Map<String, Int> = mapOf(
        "Matematik" to 45,
        "Fen Bilimleri" to 38,
        "Türkçe / Edebiyat" to 52,
        "Fizik" to 30,
        "Tarih" to 40
    ),
    val lastLessonTopic: String = "Genel Tekrar"
) {
    val game3dSuccessRatePercent: Int
        get() = if (game3dTotalQuestions > 0) {
            ((game3dCorrectAnswers * 100f) / game3dTotalQuestions).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val formattedLiveDuration: String
        get() {
            val mins = totalLiveSeconds / 60
            val secs = totalLiveSeconds % 60
            return if (mins >= 60) {
                "${mins / 60} sa ${mins % 60} dk"
            } else if (mins > 0) {
                "$mins dk $secs sn"
            } else {
                "$secs sn"
            }
        }
}

class LocalStudyMetricsStore(context: Context, userId: String) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "mrstudy_parent_metrics_$userId",
        Context.MODE_PRIVATE
    )

    private val _metrics = MutableStateFlow(loadMetrics())
    val metrics: StateFlow<LocalParentMetrics> = _metrics.asStateFlow()

    private fun loadMetrics(): LocalParentMetrics {
        val subjectJson = prefs.getString(KEY_SUBJECT_PROGRESS, null)
        val subjectMap = mutableMapOf(
            "Matematik" to 45,
            "Fen Bilimleri" to 38,
            "Türkçe / Edebiyat" to 52,
            "Fizik" to 30,
            "Tarih" to 40
        )
        if (!subjectJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(subjectJson)
                obj.keys().forEach { k ->
                    subjectMap[k] = obj.optInt(k, 0).coerceIn(0, 100)
                }
            }
        }
        return LocalParentMetrics(
            parentPin = prefs.getString(KEY_PARENT_PIN, "1234")?.takeIf { it.length == 4 } ?: "1234",
            totalQuestionsSolved = prefs.getInt(KEY_QUESTIONS_SOLVED, 0),
            totalLiveSeconds = prefs.getInt(KEY_LIVE_SECONDS, 0),
            liveSessionsCount = prefs.getInt(KEY_LIVE_SESSIONS, 0),
            game3dPlayedCount = prefs.getInt(KEY_GAME3D_PLAYED, 0),
            game3dTotalQuestions = prefs.getInt(KEY_GAME3D_TOTAL_Q, 0),
            game3dCorrectAnswers = prefs.getInt(KEY_GAME3D_CORRECT, 0),
            game3dBestScore = prefs.getInt(KEY_GAME3D_BEST_SCORE, 0),
            subjectProgress = subjectMap,
            lastLessonTopic = prefs.getString(KEY_LAST_TOPIC, "Genel Tekrar") ?: "Genel Tekrar"
        )
    }

    private fun persist(updated: LocalParentMetrics) {
        val subjectJson = JSONObject().apply {
            updated.subjectProgress.forEach { (k, v) -> put(k, v) }
        }.toString()
        prefs.edit()
            .putString(KEY_PARENT_PIN, updated.parentPin)
            .putInt(KEY_QUESTIONS_SOLVED, updated.totalQuestionsSolved)
            .putInt(KEY_LIVE_SECONDS, updated.totalLiveSeconds)
            .putInt(KEY_LIVE_SESSIONS, updated.liveSessionsCount)
            .putInt(KEY_GAME3D_PLAYED, updated.game3dPlayedCount)
            .putInt(KEY_GAME3D_TOTAL_Q, updated.game3dTotalQuestions)
            .putInt(KEY_GAME3D_CORRECT, updated.game3dCorrectAnswers)
            .putInt(KEY_GAME3D_BEST_SCORE, updated.game3dBestScore)
            .putString(KEY_SUBJECT_PROGRESS, subjectJson)
            .putString(KEY_LAST_TOPIC, updated.lastLessonTopic)
            .apply()
    }

    fun recordQuestionSolved(subject: String, count: Int = 1, boostProgress: Int = 3) {
        _metrics.update { current ->
            val nextSubj = current.subjectProgress.toMutableMap()
            val prevVal = nextSubj[subject] ?: 25
            nextSubj[subject] = (prevVal + boostProgress).coerceIn(0, 100)
            current.copy(
                totalQuestionsSolved = current.totalQuestionsSolved + count,
                subjectProgress = nextSubj
            ).also { persist(it) }
        }
    }

    fun recordLiveSession(subject: String, elapsedSeconds: Int, topic: String) {
        if (elapsedSeconds <= 0) return
        _metrics.update { current ->
            val nextSubj = current.subjectProgress.toMutableMap()
            val prevVal = nextSubj[subject] ?: 25
            val boost = (elapsedSeconds / 30).coerceIn(2, 10)
            nextSubj[subject] = (prevVal + boost).coerceIn(0, 100)
            current.copy(
                totalLiveSeconds = current.totalLiveSeconds + elapsedSeconds,
                liveSessionsCount = current.liveSessionsCount + 1,
                subjectProgress = nextSubj,
                lastLessonTopic = topic.ifBlank { subject }
            ).also { persist(it) }
        }
    }

    fun record3dGameResult(subject: String, correctCount: Int, totalCount: Int, score: Int) {
        _metrics.update { current ->
            val nextSubj = current.subjectProgress.toMutableMap()
            val prevVal = nextSubj[subject] ?: 25
            nextSubj[subject] = (prevVal + correctCount * 4).coerceIn(0, 100)
            current.copy(
                game3dPlayedCount = current.game3dPlayedCount + 1,
                game3dTotalQuestions = current.game3dTotalQuestions + totalCount.coerceAtLeast(1),
                game3dCorrectAnswers = current.game3dCorrectAnswers + correctCount.coerceAtLeast(0),
                game3dBestScore = maxOf(current.game3dBestScore, score),
                totalQuestionsSolved = current.totalQuestionsSolved + totalCount.coerceAtLeast(0),
                subjectProgress = nextSubj
            ).also { persist(it) }
        }
    }

    fun updateParentPin(newPin: String): Boolean {
        val clean = newPin.filter { it.isDigit() }
        if (clean.length != 4) return false
        _metrics.update { current ->
            current.copy(parentPin = clean).also { persist(it) }
        }
        return true
    }

    private companion object {
        const val KEY_PARENT_PIN = "parent_pin"
        const val KEY_QUESTIONS_SOLVED = "questions_solved"
        const val KEY_LIVE_SECONDS = "live_seconds"
        const val KEY_LIVE_SESSIONS = "live_sessions"
        const val KEY_GAME3D_PLAYED = "game3d_played"
        const val KEY_GAME3D_TOTAL_Q = "game3d_total_q"
        const val KEY_GAME3D_CORRECT = "game3d_correct"
        const val KEY_GAME3D_BEST_SCORE = "game3d_best_score"
        const val KEY_SUBJECT_PROGRESS = "subject_progress"
        const val KEY_LAST_TOPIC = "last_topic"
    }
}
