package com.example.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TurkishVoiceSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val trLocale = Locale("tr", "TR")
                val res = tts?.setLanguage(trLocale)
                isReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                if (!isReady) {
                    tts?.setLanguage(Locale.getDefault())
                    isReady = true
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _speakingMessageId.value = null
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _speakingMessageId.value = null
                    }
                })
            }
        }
    }

    fun toggleSpeak(messageId: String, rawText: String) {
        if (_speakingMessageId.value == messageId) {
            stop()
            return
        }
        if (!isReady) return
        val cleanText = rawText
            .replace(Regex("[*#`_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(3500)
        _speakingMessageId.value = messageId
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stop() {
        tts?.stop()
        _speakingMessageId.value = null
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

fun buildTurkishSpeechRecognizerIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "MrStudy Öğretmenine sorunu söyle...")
    }
