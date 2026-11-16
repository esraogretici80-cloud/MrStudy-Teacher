package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.StudyNote
import com.example.data.model.StudyPlanItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MrStudyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileExists_authenticatedUser_createsAndObservesProfile() = runBlocking {
        val uid = signInTestUser("student_a_${UUID.randomUUID()}@test.com")
        val repository = MrStudyRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists(uid, "Deniz Kaya", "deniz@test.com")
        }
        assertEquals(uid, profile.userId)
        assertEquals("Deniz Kaya", profile.displayName)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals("Deniz Kaya", observed?.displayName)
    }

    @Test
    fun createChatSessionAndMessage_authenticatedOwner_persistsAndEmits() = runBlocking {
        val uid = signInTestUser("student_b_${UUID.randomUUID()}@test.com")
        val repository = MrStudyRepository(firestore)

        val sessionId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createChatSession(
                userId = uid,
                title = "Fonksiyonlar",
                subject = "Matematik",
                teachingMode = "Normal Öğretmen",
                explanationDepth = "normal"
            )
        }
        assertTrue(sessionId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addChatMessage(
                userId = uid,
                sessionId = sessionId,
                role = "user",
                content = "Birebir fonksiyon nedir?",
                teachingMode = "Normal Öğretmen"
            )
        }

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeChatMessages(uid, sessionId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Birebir fonksiyon nedir?", messages.first().content)
    }

    @Test
    fun studyPlanAndNoteCrud_authenticatedOwner_createsUpdatesAndDeletes() = runBlocking {
        val uid = signInTestUser("student_c_${UUID.randomUUID()}@test.com")
        val repository = MrStudyRepository(firestore)

        val planId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addStudyPlanItem(
                uid,
                StudyPlanItem(
                    subject = "Fizik",
                    topic = "Dinamik Tekrarı",
                    dayLabel = "Salı",
                    durationMinutes = 40L,
                    priority = "Yüksek",
                    planType = "Tekrar"
                )
            )
        }

        val plans = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeStudyPlans(uid).first { list -> list.any { it.id == planId } }
        }
        val createdPlan = plans.first { it.id == planId }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateStudyPlanItem(uid, createdPlan.copy(isCompleted = true))
        }

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addNote(
                uid,
                StudyNote(
                    title = "Kuvvet ve Hareket",
                    subject = "Fizik",
                    content = "Net kuvvet sıfırsa cisim durumunu korur.",
                    formulas = listOf("F = m * a"),
                    definitions = listOf("İvme: Birim zamandaki hız değişimi"),
                    keyConcepts = listOf("Eylemsizlik")
                )
            )
        }
        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeNotes(uid).first { list -> list.any { it.id == noteId } }
        }
        assertTrue(notes.any { it.id == noteId })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteNote(uid, noteId)
        }
    }

    @Test
    fun crossUserAccess_otherStudentReadsNotes_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val aliceRepo = MrStudyRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addNote(
                aliceUid,
                StudyNote(title = "Gizli Not", subject = "Kimya", content = "Mol kavramı")
            )
        }

        signInTestUser("bob_${UUID.randomUUID()}@test.com")
        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("notes").get().await()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertNotNull("Expected PERMISSION_DENIED for cross-user read", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun observeNotes_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = MrStudyRepository(firestore)
        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeNotes("some_uid_123").first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertNotNull("Expected nested FirebaseFirestoreException on unauthenticated access", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
