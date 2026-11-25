package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppCheckFailureReason
import com.example.core.classifyAppCheckError
import com.example.core.isAppCheckErrorMessage
import com.example.core.isValidDebugTokenUuid
import com.example.data.local.MrStudy4LocalStore
import com.example.data.model.AiToolType
import com.example.data.model.CodingLanguage
import com.example.data.model.Custom3dGameConfig
import com.example.data.model.DiagramNode
import com.example.data.model.DiagramProject
import com.example.data.model.DiagramType
import com.example.data.model.FlashcardReviewRating
import com.example.data.model.GameGenre
import com.example.data.model.ScienceLabModule
import com.example.data.model.TeachingMode
import com.example.ui.components.Engine3d
import com.example.ui.components.THREE_JS_LOCAL_SCRIPT_URL
import com.example.ui.components.ThreeJsSceneParams
import com.example.ui.components.Vec3
import com.example.util.PythonMiniInterpreter
import com.example.widget.AiTeacherWidgetProvider
import com.example.widget.DailyGoalWidgetProvider
import com.example.widget.DailyQuestionWidgetProvider
import com.example.widget.ExamCountdownWidgetProvider
import com.example.widget.FlashcardWidgetProvider
import com.example.widget.MotivationWidgetProvider
import com.example.widget.MrStudyWidgetContract
import com.example.widget.MrStudyWidgetStore
import com.example.widget.MrStudyWidgetUpdater
import com.example.widget.QuickNoteWidgetProvider
import com.example.widget.ScheduleWidgetProvider
import com.example.widget.StudyStopwatchWidgetProvider
import com.example.widget.XpStreakWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class MrStudy4UnitTest {

    @Test
    fun mrStudy4Catalogs_haveExpectedCounts() {
        assertEquals(8, TeachingMode.entries.size)
        assertEquals(8, GameGenre.entries.size)
        assertEquals(8, DiagramType.entries.size)
        assertEquals(8, ScienceLabModule.entries.size)
        assertEquals(12, AiToolType.entries.size)
        assertEquals(8, CodingLanguage.entries.size)
        assertEquals(4, FlashcardReviewRating.entries.size)
    }

    @Test
    fun mrStudy4Routes_all17RoutesDistinct() {
        val routes = listOf(
            MrStudyRoutes.DASHBOARD,
            MrStudyRoutes.LIVE_LESSON,
            MrStudyRoutes.TEACHER,
            MrStudyRoutes.PARENT,
            MrStudyRoutes.PDF,
            MrStudyRoutes.EXAM,
            MrStudyRoutes.STUDY,
            MrStudyRoutes.GAME_3D,
            MrStudyRoutes.FLASHCARDS,
            MrStudyRoutes.GAME_STUDIO,
            MrStudyRoutes.DIAGRAM_BOARD,
            MrStudyRoutes.SCIENCE_LAB,
            MrStudyRoutes.AI_TOOLS,
            MrStudyRoutes.CODING_STUDIO,
            MrStudyRoutes.MATH_BOARD,
            MrStudyRoutes.AUDIO_STUDIO,
            MrStudyRoutes.DIGITAL_LIBRARY
        )
        assertEquals(17, routes.size)
        assertEquals(17, routes.toSet().size)
    }

    @Test
    fun pythonMiniInterpreter_executesLoopsAndConditionals() {
        val code = """
            total = 0
            for i in range(1, 5):
                total += i
            if total == 10:
                print("Toplam", total)
            else:
                print("Hata")
        """.trimIndent()
        val res = PythonMiniInterpreter.execute(code)
        assertNull(res.errorMessage)
        assertEquals(listOf("Toplam 10"), res.outputLines)
    }

    @Test
    fun pythonMiniInterpreter_guardsInfiniteWhileLoop() {
        val code = """
            x = 1
            while x > 0:
                x += 1
        """.trimIndent()
        val res = PythonMiniInterpreter.execute(code)
        assertNotNull(res.errorMessage)
    }

    @Test
    fun engine3d_projectionAndShapes_valid() {
        val p = Engine3d.project(
            v = Vec3(0f, 0f, 0f),
            yawRad = 0f,
            pitchRad = 0f,
            centerX = 200f,
            centerY = 150f,
            fovScale = 100f
        )
        assertEquals(200f, p.x, 0.01f)
        assertEquals(150f, p.y, 0.01f)
        val palette = listOf(Color.Cyan, Color.Magenta, Color.Yellow)
        assertEquals(6, Engine3d.cubeFaces(palette = palette).size)
        assertEquals(5, Engine3d.pyramidFaces(palette = palette).size)
        assertEquals(8, Engine3d.prismFaces(palette = palette).size)
        assertTrue(Engine3d.coneFaces(palette = palette).size >= 12)
    }

    @Test
    fun appCheck_rejectsLegacySharedTokenAndValidatesUuid() {
        assertFalse(isValidDebugTokenUuid(null))
        assertFalse(isValidDebugTokenUuid(""))
        assertFalse(isValidDebugTokenUuid("YOUR_FIREBASE_APPCHECK_DEBUG_TOKEN"))
        assertFalse(isValidDebugTokenUuid("MrStudy-Shared-Debug-Token-2026-AppCheck"))
        assertTrue(isValidDebugTokenUuid("550e8400-e29b-41d4-a716-446655440000"))

        assertTrue(isAppCheckErrorMessage("App attestation failed (403)"))
        val (reason, msg) = classifyAppCheckError(IllegalStateException("App attestation failed 403"), null)
        assertTrue(
            reason == AppCheckFailureReason.PLAY_INTEGRITY_ATTESTATION ||
                reason == AppCheckFailureReason.DEBUG_ATTESTATION_UNREGISTERED
        )
        assertTrue(msg.contains("403"))
    }

    @Test
    fun threeJsSceneParams_serializesValidConfig() {
        assertTrue(THREE_JS_LOCAL_SCRIPT_URL.endsWith("/assets/three/three.min.js"))
        val json = ThreeJsSceneParams(
            module = ScienceLabModule.SOLAR_SYSTEM,
            zoom = 1.25f,
            orbitSpeed = 1.5f,
            eclipseAngle = 0f,
            forceNewton = 20f,
            massKg = 4f,
            circuitClosed = true,
            voltageV = 12f,
            resistanceOhm = 4f,
            temperatureC = 25f,
            geometryShapeIdx = 0,
            resetCounter = 0
        ).toJsonString()
        assertTrue(json.contains("SOLAR_SYSTEM"))
        assertTrue(json.contains("1.25"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MrStudy4WidgetAndStoreTest {

    @Test
    fun localStore_persistsDiagramsAndGamesPerUid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MrStudy4LocalStore(context, "test_uid_40")

        val game = Custom3dGameConfig(title = "Gezegen Koşusu", subject = "Fen Bilimleri")
        store.saveCustomGame(game)

        val diagram = DiagramProject(
            title = "Fotosentez",
            diagramType = DiagramType.CONCEPT_MAP,
            nodes = listOf(DiagramNode(label = "Kloroplast"))
        )
        store.saveDiagram(diagram)
        store.setLowPower3dMode(true)

        val reloaded = MrStudy4LocalStore(context, "test_uid_40").state.value
        assertTrue(reloaded.lowPower3dMode)
        assertTrue(reloaded.customGames.any { it.title == "Gezegen Koşusu" })
        assertTrue(reloaded.diagrams.any { it.title == "Fotosentez" })
    }

    @Test
    fun widgetStore_andAll10Providers_workCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widgetStore = MrStudyWidgetStore(context)

        widgetStore.flipFlashcard()
        assertTrue(widgetStore.getSnapshot().flashcardFlipped)
        widgetStore.nextFlashcard()
        assertFalse(widgetStore.getSnapshot().flashcardFlipped)

        widgetStore.startStopwatch()
        assertTrue(widgetStore.getSnapshot().stopwatchRunning)
        widgetStore.pauseStopwatch()
        assertFalse(widgetStore.getSnapshot().stopwatchRunning)

        widgetStore.answerDailyQuestion(2)
        assertEquals(2, widgetStore.getSnapshot().dailyQuestionSelected)
        widgetStore.nextDailyQuestion()
        assertEquals(-1, widgetStore.getSnapshot().dailyQuestionSelected)

        // Exercise all 10 RemoteViews builders cleanly
        val snap = widgetStore.getSnapshot()
        assertNotNull(MrStudyWidgetUpdater.buildAiTeacherViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildDailyGoalViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildFlashcardViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildScheduleViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildXpStreakViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildExamCountdownViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildStopwatchViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildQuickNoteViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildDailyQuestionViews(context, snap))
        assertNotNull(MrStudyWidgetUpdater.buildMotivationViews(context, snap))

        // Verify broadcasts to widget providers do not throw
        val mgr = AppWidgetManager.getInstance(context)
        val providers = listOf(
            AiTeacherWidgetProvider(),
            DailyGoalWidgetProvider(),
            FlashcardWidgetProvider(),
            ScheduleWidgetProvider(),
            XpStreakWidgetProvider(),
            ExamCountdownWidgetProvider(),
            StudyStopwatchWidgetProvider(),
            QuickNoteWidgetProvider(),
            DailyQuestionWidgetProvider(),
            MotivationWidgetProvider()
        )
        providers.forEach { p ->
            p.onUpdate(context, mgr, intArrayOf(1))
        }
        FlashcardWidgetProvider().onReceive(context, Intent(MrStudyWidgetContract.ACTION_FLASHCARD_FLIP))
        MotivationWidgetProvider().onReceive(context, Intent(MrStudyWidgetContract.ACTION_MOTIVATION_NEXT))
        assertTrue(widgetStore.getSnapshot().motivationIndex >= 1)
    }

    @Test
    fun richMarkdownParser_extractsBlocksAndBadges() {
        val markdown = """
            # Matematik Dersi
            Bu bir **önemli** kuraldır.
            `x + y = 10`
            📐 Formül / Kural: E = m * c^2
            💡 Püf Noktası: Soru kökünü dikkatlice oku.
            ✅ Doğru Cevap: A şıkkı doğrudur.
        """.trimIndent()

        val annotated = com.example.ui.components.parseRichMarkdown(markdown)
        assertTrue(annotated.text.isNotBlank())
        assertTrue(annotated.text.contains("Matematik Dersi"))
        assertTrue(annotated.text.contains("önemli"))
        assertTrue(annotated.text.contains("E = m * c^2"))
        assertTrue(annotated.text.contains("Püf Noktası"))
        assertTrue(annotated.text.contains("Doğru Cevap"))
        assertTrue(annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun pdfMarkupModels_andTools_operateCorrectly() {
        assertEquals(3, com.example.ui.components.PdfMarkupTool.values().size)
        assertTrue(com.example.ui.components.DefaultMarkupColors.size >= 5)

        val stroke = com.example.ui.components.PdfMarkupStroke(
            points = listOf(
                com.example.ui.components.PdfMarkupPoint(0.1f, 0.1f),
                com.example.ui.components.PdfMarkupPoint(0.2f, 0.2f),
                com.example.ui.components.PdfMarkupPoint(0.3f, 0.3f)
            ),
            color = Color.Yellow,
            strokeWidthDp = 24f,
            isHighlighter = true
        )

        assertEquals(3, stroke.points.size)
        assertTrue(stroke.isHighlighter)
        assertEquals(24f, stroke.strokeWidthDp, 0.01f)
    }
}
