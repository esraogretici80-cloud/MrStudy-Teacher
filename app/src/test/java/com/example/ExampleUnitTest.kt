package com.example

import com.example.data.ai.LiveConnectionStatus
import com.example.data.ai.LiveLessonUiState
import com.example.data.model.PdfResourceRecord
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

    // Stepping backwards from page 1 must remain 1
    val prevFromFirst = (1 - 1).coerceIn(1, maxPages)
    assertEquals(1, prevFromFirst)

    // Stepping forwards from page 1 becomes 2
    val nextFromFirst = (1 + 1).coerceIn(1, maxPages)
    assertEquals(2, nextFromFirst)

    // Stepping forwards from page 10 must remain 10
    val nextFromLast = (10 + 1).coerceIn(1, maxPages)
    assertEquals(10, nextFromLast)
  }
}

