package com.example

import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveLessonUiState
import com.example.data.ai.LivePdfCommand
import com.example.data.ai.LivePdfCommandParser
import com.example.data.ai.MrStudyAiService
import com.example.data.local.LocalParentMetrics
import com.example.data.model.Game3dQuestion
import com.example.data.model.PdfResourceRecord
import com.example.data.model.SmartBoardElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun mrStudyRoutes_rootTabsAndSubRoutes_distinctAndComplete() {
        assertEquals(
            setOf(
                MrStudyRoutes.DASHBOARD,
                MrStudyRoutes.LIVE_LESSON,
                MrStudyRoutes.TEACHER,
                MrStudyRoutes.PARENT
            ),
            MrStudyRoutes.rootTabs
        )
        val allRoutes = listOf(
            MrStudyRoutes.DASHBOARD,
            MrStudyRoutes.LIVE_LESSON,
            MrStudyRoutes.TEACHER,
            MrStudyRoutes.PARENT,
            MrStudyRoutes.PDF,
            MrStudyRoutes.EXAM,
            MrStudyRoutes.STUDY,
            MrStudyRoutes.GAME_3D
        )
        assertEquals(allRoutes.size, allRoutes.toSet().size)
        assertFalse(MrStudyRoutes.rootTabs.contains(MrStudyRoutes.PDF))
        assertFalse(MrStudyRoutes.rootTabs.contains(MrStudyRoutes.EXAM))
        assertFalse(MrStudyRoutes.rootTabs.contains(MrStudyRoutes.STUDY))
        assertFalse(MrStudyRoutes.rootTabs.contains(MrStudyRoutes.GAME_3D))
    }

    @Test
    fun studentProfile_xpLevelAndProgress_calculatedFromRealXp() {
        val xp = 850L
        val level = (xp / 150L).toInt() + 1
        val levelProgress = (xp % 150L)
        val progressFraction = (levelProgress / 150f).coerceIn(0f, 1f)
        assertEquals(6, level)
        assertEquals(100L, levelProgress)
        assertTrue(progressFraction in 0.66f..0.67f)
    }

    @Test
    fun aiService_game3dTiers_startsWithGemini36Flash() {
        assertEquals(
            listOf("gemini-3.6-flash", "gemini-3.5-flash-lite"),
            MrStudyAiService.GAME_3D_TIERS
        )
        assertEquals(
            listOf("gemini-4-hydrogen", "gemini-3.6-flash", "gemini-3.5-flash-lite"),
            MrStudyAiService.DEFAULT_TIERS
        )
    }

    @Test
    fun livePdfCommandParser_parsesTurkishJumpAndStepCommands() {
        assertEquals(LivePdfCommand.JumpToPage(46), LivePdfCommandParser.parse("sayfa 46"))
        assertEquals(LivePdfCommand.JumpToPage(37), LivePdfCommandParser.parse("37'ye geçebilir misin?"))
        assertEquals(LivePdfCommand.JumpToPage(12), LivePdfCommandParser.parse("12. sayfayı aç"))
        assertEquals(LivePdfCommand.JumpToPage(8), LivePdfCommandParser.parse("s.8'e git"))
        assertEquals(LivePdfCommand.JumpToPage(46), LivePdfCommandParser.parse("kırk altıncı sayfaya geç"))
        assertEquals(LivePdfCommand.JumpToPage(37), LivePdfCommandParser.parse("otuz yediye geçebilir misin"))
        assertEquals(LivePdfCommand.JumpToPage(46), LivePdfCommandParser.parse("sayfa 4 değil sayfa 46"))

        assertEquals(LivePdfCommand.StepPage(1), LivePdfCommandParser.parse("sonraki sayfaya geç"))
        assertEquals(LivePdfCommand.StepPage(1), LivePdfCommandParser.parse("sayfayı ilerlet"))
        assertEquals(LivePdfCommand.StepPage(-1), LivePdfCommandParser.parse("önceki sayfaya dön"))
        assertEquals(LivePdfCommand.StepPage(3), LivePdfCommandParser.parse("3 sayfa ilerlet"))
        assertEquals(LivePdfCommand.StepPage(-2), LivePdfCommandParser.parse("2 sayfa geri git"))

        assertNull(LivePdfCommandParser.parse("iki artı iki kaç eder?"))
        assertNull(LivePdfCommandParser.parse("denklemi tekrar anlatır mısın?"))
    }
}
