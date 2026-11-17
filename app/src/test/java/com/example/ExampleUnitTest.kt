package com.example

import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveLessonUiState
import com.example.data.local.LocalParentMetrics
import com.example.data.model.Game3dQuestion
import com.example.data.model.PdfResourceRecord
import com.example.data.model.SmartBoardElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun liveLessonUiState_defaults_correct() {
        val state = LiveLessonUiState()
        assertEquals(LiveConnectionStatus.IDLE, state.status)
        assertFalse(state.isMuted)
        assertTrue(state.echoGuardEnabled)
        assertFalse(state.isBluetoothHeadsetConnected)
        assertEquals("Hoparlör", state.activeAudioDeviceName)
        assertFalse(state.isSessionActive)
    }

    @Test
    fun liveLessonUiState_voicePersonas_present() {
        val labels = LiveLessonUiState.voiceOptions.map { it.label }
        assertTrue(labels.contains("Enerjik Öğretmen"))
        assertTrue(labels.contains("Sakin Rehber"))
        assertTrue(labels.contains("Genç Asistan"))
        assertTrue(labels.contains("Tok ve Ciddi Hoca"))
        assertEquals(4, labels.size)
    }

    @Test
    fun liveLessonUiState_withBluetoothHeadset_active() {
        val state = LiveLessonUiState(
            status = LiveConnectionStatus.LISTENING,
            isBluetoothHeadsetConnected = true,
            activeAudioDeviceName = "Galaxy Buds Pro"
        )
        assertTrue(state.isSessionActive)
        assertTrue(state.isBluetoothHeadsetConnected)
        assertEquals("Galaxy Buds Pro", state.activeAudioDeviceName)
    }

    @Test
    fun livePdf_pageClamping_boundsRespected() {
        val pdf = PdfResourceRecord(
            id = "test_pdf",
            title = "TYT Matematik",
            pageCount = 10L,
            lastStudiedPage = 1L
        )
        val maxPages = pdf.pageCount.toInt()
        assertEquals(1, (1 - 1).coerceIn(1, maxPages))
        assertEquals(2, (1 + 1).coerceIn(1, maxPages))
        assertEquals(10, (10 + 1).coerceIn(1, maxPages))
    }

    @Test
    fun parentMetrics_defaultPinAndSuccessRate() {
        val defaults = LocalParentMetrics()
        assertEquals("1234", defaults.parentPin)
        assertEquals(0, defaults.game3dSuccessRatePercent)

        val played = LocalParentMetrics(
            game3dTotalQuestions = 10,
            game3dCorrectAnswers = 8
        )
        assertEquals(80, played.game3dSuccessRatePercent)
    }

    @Test
    fun smartBoardElementType_parsesSafeEnumValues() {
        assertEquals(SmartBoardElementType.TRIANGLE, SmartBoardElementType.fromString("TRIANGLE"))
        assertEquals(SmartBoardElementType.GRAPH, SmartBoardElementType.fromString("graph"))
        assertEquals(SmartBoardElementType.DIAGRAM, SmartBoardElementType.fromString("DIAGRAM"))
        assertEquals(SmartBoardElementType.FORMULA, SmartBoardElementType.fromString("unknown_shape"))
    }

    @Test
    fun game3dQuestion_defaultsValid() {
        val q = Game3dQuestion(
            prompt = "2x + 4 = 10 ise x kaçtır?",
            options = listOf("2", "3", "4", "5"),
            correctIndex = 1,
            explanation = "2x = 6, x = 3"
        )
        assertEquals(4, q.options.size)
        assertEquals("3", q.options[q.correctIndex])
    }
}
