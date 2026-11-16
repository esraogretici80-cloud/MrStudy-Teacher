package com.example.data.ai

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.core.getLocalAppCheckDebugToken
import com.example.core.isAppCheckErrorMessage
import com.example.data.model.StudentProfile
import com.example.data.model.TeachingMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ActivityDetectionConfig
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.LiveSessionResumptionUpdate
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RealtimeInputConfig
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SessionResumptionConfig
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.activityDetectionConfig
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.realtimeInputConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

enum class LiveConnectionStatus(val label: String) {
    IDLE("Hazır"),
    CONNECTING("Bağlanıyor..."),
    LISTENING("Dinliyor"),
    TEACHER_SPEAKING("Öğretmen Anlatıyor"),
    MUTED("Mikrofon Kapalı"),
    ERROR("Bağlantı Hatası")
}

data class LiveTranscriptEntry(
    val id: String,
    val isStudent: Boolean,
    val text: String,
    val tag: String = ""
)

data class LiveVoiceOption(
    val id: String,
    val label: String,
    val description: String
)

data class LiveLessonUiState(
    val status: LiveConnectionStatus = LiveConnectionStatus.IDLE,
    val isMuted: Boolean = false,
    val echoGuardEnabled: Boolean = true,
    val userAudioLevel: Float = 0f,
    val teacherAudioLevel: Float = 0f,
    val liveUserTranscript: String = "",
    val liveTeacherTranscript: String = "",
    val transcriptItems: List<LiveTranscriptEntry> = emptyList(),
    val elapsedSeconds: Int = 0,
    val selectedVoice: String = "AOEDE",
    val activeModelName: String = PRIMARY_LIVE_MODEL,
    val errorMessage: String? = null,
    val debugAppCheckToken: String? = null,
    val statusNotice: String? = null
) {
    val isSessionActive: Boolean
        get() = status == LiveConnectionStatus.CONNECTING ||
            status == LiveConnectionStatus.LISTENING ||
            status == LiveConnectionStatus.TEACHER_SPEAKING ||
            status == LiveConnectionStatus.MUTED

    companion object {
        const val PRIMARY_LIVE_MODEL = "gemini-3.1-flash-live-preview"
        const val FALLBACK_LIVE_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"

        val voiceOptions = listOf(
            LiveVoiceOption("AOEDE", "Aoede", "Sıcak ve akıcı öğretmen sesi"),
            LiveVoiceOption("FENRIR", "Fenrir", "Net ve güçlü erkek öğretmen"),
            LiveVoiceOption("CHARON", "Charon", "Sakin ve rehber ton"),
            LiveVoiceOption("KORE", "Kore", "Enerjik konu anlatımı")
        )
    }
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveTeacherManager {

    private val _state = MutableStateFlow(LiveLessonUiState())
    val state: StateFlow<LiveLessonUiState> = _state.asStateFlow()

    private var liveSession: LiveSession? = null
    private var sessionScopeJob: Job? = null
    private var timerJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var playbackChannel: Channel<ByteArray>? = null
    private var resumptionHandle: String? = null
    private var turnCounter = 0

    // Audio routing & focus management
    private var audioManager: AudioManager? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerphoneOn: Boolean = false
    private var audioFocusRequest: AudioFocusRequest? = null

    // Echo & turn-state coordination
    private val isReceivingTeacherAudio = AtomicBoolean(false)
    private val playbackDrainDeadlineMs = AtomicLong(0L)
    private val forceBargeInWindowUntilMs = AtomicLong(0L)

    fun selectVoice(voiceId: String) {
        if (!_state.value.isSessionActive) {
            _state.update { it.copy(selectedVoice = voiceId) }
        }
    }

    fun setEchoGuardEnabled(enabled: Boolean) {
        _state.update {
            it.copy(
                echoGuardEnabled = enabled,
                statusNotice = if (enabled) {
                    "Akıcı Anlatım (Yankı Koruması) açık: Öğretmen konuşurken hoparlör sesi sözünü kesmez."
                } else {
                    "Serbest Diyalog açık (Kulaklık önerilir): İstediğin an konuşarak sözünü kesebilirsin."
                }
            )
        }
    }

    fun clearError() {
        _state.update {
            it.copy(
                errorMessage = null,
                debugAppCheckToken = null,
                statusNotice = null,
                status = if (it.status == LiveConnectionStatus.ERROR) LiveConnectionStatus.IDLE else it.status
            )
        }
    }

    fun toggleMute() {
        val nextMuted = !_state.value.isMuted
        _state.update { state ->
            val nextStatus = when {
                !state.isSessionActive -> state.status
                nextMuted -> LiveConnectionStatus.MUTED
                isTeacherSpeakingNow() -> LiveConnectionStatus.TEACHER_SPEAKING
                else -> LiveConnectionStatus.LISTENING
            }
            state.copy(
                isMuted = nextMuted,
                userAudioLevel = if (nextMuted) 0f else state.userAudioLevel,
                status = nextStatus
            )
        }
    }

    fun interruptAndSpeakNow() {
        if (!_state.value.isSessionActive) return
        val queue = playbackChannel
        if (queue != null) {
            drainChannel(queue)
        }
        runCatching {
            audioTrack?.let { track ->
                track.pause()
                track.flush()
                track.play()
            }
        }
        isReceivingTeacherAudio.set(false)
        playbackDrainDeadlineMs.set(0L)
        // Open microphone immediately for the next 8 seconds even if server finishes a trailing packet
        forceBargeInWindowUntilMs.set(SystemClock.elapsedRealtime() + 8_000L)
        _state.update {
            it.copy(
                isMuted = false,
                teacherAudioLevel = 0f,
                liveTeacherTranscript = "",
                status = LiveConnectionStatus.LISTENING,
                statusNotice = "Söz sende! Sorunu doğrudan söyleyebilirsin."
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startSession(
        scope: CoroutineScope,
        context: Context,
        profile: StudentProfile,
        subject: String,
        teachingMode: TeachingMode,
        pdfTitle: String?,
        activePdfPage: Int?,
        initialPdfBitmap: Bitmap?
    ) {
        val appContext = context.applicationContext
        stopSessionInternal(resetTranscripts = true)

        _state.update {
            it.copy(
                status = LiveConnectionStatus.CONNECTING,
                isMuted = false,
                userAudioLevel = 0f,
                teacherAudioLevel = 0f,
                liveUserTranscript = "",
                liveTeacherTranscript = "",
                transcriptItems = emptyList(),
                elapsedSeconds = 0,
                errorMessage = null,
                debugAppCheckToken = null,
                statusNotice = null
            )
        }

        sessionScopeJob = scope.launch(Dispatchers.IO) {
            try {
                ensureAppCheckReady()

                val systemInstructionContent = buildLiveSystemInstruction(
                    profile = profile,
                    subject = subject,
                    mode = teachingMode,
                    pdfTitle = pdfTitle,
                    activePdfPage = activePdfPage
                )

                val selectedVoice = _state.value.selectedVoice
                val echoGuard = _state.value.echoGuardEnabled
                val (session, usedModel) = connectWithFallback(
                    voiceName = selectedVoice,
                    echoGuardEnabled = echoGuard,
                    systemInstruction = systemInstructionContent,
                    resumeHandle = null
                )
                liveSession = session

                configureCommunicationAudioRouting(appContext)

                _state.update {
                    it.copy(
                        status = LiveConnectionStatus.LISTENING,
                        activeModelName = usedModel,
                        statusNotice = "Canlı ders bağlantısı kuruldu. Doğrudan konuşabilirsiniz."
                    )
                }

                startElapsedTimer(this)
                val queue = Channel<ByteArray>(capacity = Channel.UNLIMITED)
                playbackChannel = queue

                val track = createAndStartAudioTrack()
                audioTrack = track

                // Optional: push initial PDF page visually if selected
                if (initialPdfBitmap != null && !pdfTitle.isNullOrBlank()) {
                    runCatching {
                        val jpegBytes = compressBitmapToJpeg(initialPdfBitmap)
                        session.sendVideoRealtime(InlineData(jpegBytes, "image/jpeg"))
                    }
                }

                // Trigger warm opening greeting from the live teacher
                runCatching {
                    val pdfHint = if (!pdfTitle.isNullOrBlank()) {
                        " Öğrencinin elinde '$pdfTitle' PDF'inin ${activePdfPage ?: 1}. sayfası açık."
                    } else ""
                    session.send(
                        "Merhaba hocam! Canlı ders anlatımına başlayalım. Konu: $subject.$pdfHint " +
                            "Kısaca sıcak bir merhaba de ve bugün hangi konuyu veya soruyu çalışmak istediğimi sor."
                    )
                }

                val playbackJob = launch(Dispatchers.IO) {
                    runPlaybackLoop(queue, track)
                }

                val statusSyncJob = launch(Dispatchers.IO) {
                    runSpeakingStatusMonitorLoop()
                }

                val micJob = launch(Dispatchers.IO) {
                    runMicrophoneLoop(session)
                }

                val receiveJob = launch(Dispatchers.IO) {
                    runReceiveLoop(session, queue, track)
                }

                receiveJob.join()
                micJob.cancel()
                playbackJob.cancel()
                statusSyncJob.cancel()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Live session failed", e)
                val mappedMsg = mapLiveErrorMessage(e)
                val debugToken = if (isAppCheckErrorMessage(mappedMsg)) {
                    getLocalAppCheckDebugToken(appContext)
                } else {
                    null
                }
                _state.update {
                    it.copy(
                        status = LiveConnectionStatus.ERROR,
                        errorMessage = mappedMsg,
                        debugAppCheckToken = debugToken,
                        userAudioLevel = 0f,
                        teacherAudioLevel = 0f
                    )
                }
            } finally {
                withContext(NonCancellable) {
                    releaseAudioAndSession()
                }
            }
        }
    }

    fun sendTextDuringLive(text: String) {
        val clean = text.trim()
        val session = liveSession ?: return
        if (clean.isBlank()) return

        appendTranscriptEntry(isStudent = true, text = clean, tag = "Yazılı Soru")
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                session.send(clean)
            }.onFailure { err ->
                Log.w(TAG, "Failed to send live text", err)
                _state.update { it.copy(statusNotice = "Mesaj gönderilemedi: ${err.localizedMessage}") }
            }
        }
    }

    fun sendVisualContextDuringLive(bitmap: Bitmap, label: String, promptText: String) {
        val session = liveSession ?: return
        appendTranscriptEntry(isStudent = true, text = "📎 $label: $promptText", tag = "Görsel")
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val jpegBytes = compressBitmapToJpeg(bitmap)
                session.sendVideoRealtime(InlineData(jpegBytes, "image/jpeg"))
                session.send(promptText)
                _state.update { it.copy(statusNotice = "$label canlı öğretmene gösterildi.") }
            }.onFailure { err ->
                Log.w(TAG, "Failed to send live image", err)
                _state.update { it.copy(statusNotice = "Görsel gönderilemedi: ${err.localizedMessage}") }
            }
        }
    }

    fun stopSession() {
        stopSessionInternal(resetTranscripts = false)
    }

    private fun stopSessionInternal(resetTranscripts: Boolean) {
        timerJob?.cancel()
        timerJob = null
        sessionScopeJob?.cancel()
        sessionScopeJob = null

        isReceivingTeacherAudio.set(false)
        playbackDrainDeadlineMs.set(0L)
        forceBargeInWindowUntilMs.set(0L)

        val sessionToClose = liveSession
        liveSession = null
        if (sessionToClose != null) {
            CoroutineScope(Dispatchers.IO + NonCancellable).launch {
                runCatching { sessionToClose.stopReceiving() }
                runCatching { sessionToClose.close() }
            }
        }
        releaseAudioOnly()

        _state.update {
            it.copy(
                status = LiveConnectionStatus.IDLE,
                userAudioLevel = 0f,
                teacherAudioLevel = 0f,
                liveUserTranscript = "",
                liveTeacherTranscript = "",
                statusNotice = null,
                transcriptItems = if (resetTranscripts) emptyList() else it.transcriptItems
            )
        }
    }

    private suspend fun ensureAppCheckReady() {
        try {
            val result = withTimeout(APP_CHECK_TIMEOUT_MS) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
            if (result.token.isBlank()) {
                throw IllegalStateException("Firebase App Check token boş döndü.")
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException(
                "Güvenlik doğrulaması (App Check) 10 saniyede yanıt vermedi. İnternet bağlantınızı kontrol edin.",
                e
            )
        }
    }

    private suspend fun connectWithFallback(
        voiceName: String,
        echoGuardEnabled: Boolean,
        systemInstruction: com.google.firebase.ai.type.Content,
        resumeHandle: String?
    ): Pair<LiveSession, String> {
        val config = liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
            speechConfig = SpeechConfig(voice = Voice(voiceName))
            inputAudioTranscription = AudioTranscriptionConfig()
            outputAudioTranscription = AudioTranscriptionConfig()
            realtimeInputConfig = realtimeInputConfig {
                automaticActivityDetection = activityDetectionConfig {
                    startSensitivity = ActivityDetectionConfig.Sensitivity.LOW
                    endSensitivity = ActivityDetectionConfig.Sensitivity.LOW
                    prefixPaddingMs = 180
                    silenceDurationMs = 900
                }
                activityHandling = if (echoGuardEnabled) {
                    RealtimeInputConfig.ActivityHandling.NO_INTERRUPT
                } else {
                    RealtimeInputConfig.ActivityHandling.INTERRUPT
                }
            }
        }

        val resumption = if (resumeHandle != null) {
            SessionResumptionConfig(handle = resumeHandle)
        } else {
            SessionResumptionConfig()
        }

        return try {
            val primaryModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName = LiveLessonUiState.PRIMARY_LIVE_MODEL,
                generationConfig = config,
                systemInstruction = systemInstruction
            )
            val session = withTimeout(CONNECT_TIMEOUT_MS) {
                primaryModel.connect(sessionResumption = resumption)
            }
            session to LiveLessonUiState.PRIMARY_LIVE_MODEL
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) throw e
            throw e
        } catch (primaryErr: Exception) {
            val msg = primaryErr.message.orEmpty()
            val isAuthOrAppCheck = isAppCheckErrorMessage(msg) ||
                msg.contains("PERMISSION_DENIED", ignoreCase = true)
            val shouldFallback = !isAuthOrAppCheck && (
                msg.contains("not found", ignoreCase = true) ||
                    msg.contains("unsupported", ignoreCase = true) ||
                    msg.contains("model", ignoreCase = true) ||
                    msg.contains("404", ignoreCase = true) ||
                    msg.contains("invalid", ignoreCase = true)
                )
            if (!shouldFallback) throw primaryErr

            Log.w(TAG, "Primary Live model unavailable, trying fallback model", primaryErr)
            val fallbackModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName = LiveLessonUiState.FALLBACK_LIVE_MODEL,
                generationConfig = config,
                systemInstruction = systemInstruction
            )
            val session = withTimeout(CONNECT_TIMEOUT_MS) {
                fallbackModel.connect(sessionResumption = resumption)
            }
            session to LiveLessonUiState.FALLBACK_LIVE_MODEL
        }
    }

    private fun startElapsedTimer(scope: CoroutineScope) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun configureCommunicationAudioRouting(context: Context) {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager = am
            previousAudioMode = am.mode
            @Suppress("DEPRECATION")
            previousSpeakerphoneOn = am.isSpeakerphoneOn

            am.mode = AudioManager.MODE_IN_COMMUNICATION

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { }
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = am.availableCommunicationDevices
                val hasExternalDevice = devices.any {
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
                if (!hasExternalDevice) {
                    val speaker = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speaker != null) {
                        am.setCommunicationDevice(speaker)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                if (!am.isWiredHeadsetOn && !am.isBluetoothScoOn && !am.isBluetoothA2dpOn) {
                    am.isSpeakerphoneOn = true
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "AudioManager communication routing setup failed", err)
        }
    }

    private fun isTeacherSpeakingNow(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now < forceBargeInWindowUntilMs.get()) return false
        return isReceivingTeacherAudio.get() || now < playbackDrainDeadlineMs.get()
    }

    private suspend fun runSpeakingStatusMonitorLoop() {
        while (true) {
            delay(120L)
            val current = _state.value
            if (!current.isSessionActive || current.status == LiveConnectionStatus.CONNECTING) continue
            val speaking = isTeacherSpeakingNow()
            val desiredStatus = when {
                current.isMuted -> LiveConnectionStatus.MUTED
                speaking -> LiveConnectionStatus.TEACHER_SPEAKING
                else -> LiveConnectionStatus.LISTENING
            }
            if (current.status != desiredStatus || (!speaking && current.teacherAudioLevel > 0f)) {
                _state.update {
                    it.copy(
                        status = desiredStatus,
                        teacherAudioLevel = if (speaking) it.teacherAudioLevel else 0f
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runMicrophoneLoop(session: LiveSession) = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(3200)
        val bufferSize = (minBuf * 2).coerceAtLeast(6400)

        var record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                INPUT_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("Mikrofon başlatılamadı. Başka bir uygulama mikrofonu kullanıyor olabilir.")
        }

        audioRecord = record
        if (AcousticEchoCanceler.isAvailable()) {
            runCatching {
                echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)?.apply { enabled = true }
            }
        }
        if (NoiseSuppressor.isAvailable()) {
            runCatching {
                noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
            }
        }
        if (AutomaticGainControl.isAvailable()) {
            runCatching {
                automaticGainControl = AutomaticGainControl.create(record.audioSessionId)?.apply { enabled = true }
            }
        }

        record.startRecording()
        val chunk = ByteArray(3200) // 100ms @ 16kHz 16-bit mono
        val preRollQueue = ArrayDeque<ByteArray>(3)
        var lastSpeechDetectedMs = 0L
        var lastUiMeterUpdateMs = 0L

        try {
            while (isActive && _state.value.isSessionActive) {
                val read = record.read(chunk, 0, chunk.size)
                if (read <= 0) continue

                val stateSnapshot = _state.value
                val now = SystemClock.elapsedRealtime()

                if (stateSnapshot.isMuted) {
                    preRollQueue.clear()
                    if (stateSnapshot.userAudioLevel != 0f) {
                        _state.update { it.copy(userAudioLevel = 0f) }
                    }
                    continue
                }

                // Echo Guard: while the teacher's voice is coming out of speaker (+ tail),
                // pause mic streaming so speaker output does NOT cause 2-word self-interruptions or phantom words.
                val teacherTalking = isTeacherSpeakingNow()
                if (stateSnapshot.echoGuardEnabled && teacherTalking) {
                    preRollQueue.clear()
                    if (stateSnapshot.userAudioLevel != 0f) {
                        _state.update { it.copy(userAudioLevel = 0f) }
                    }
                    continue
                }

                val payload = chunk.copyOf(read)
                val level = computePcm16Rms(payload, read)

                // Adaptive noise gate + pre-roll so ambient static does NOT hallucinate phantom transcripts
                val speechThreshold = if (teacherTalking) BARGE_IN_SPEECH_RMS else NOISE_GATE_SPEECH_RMS
                if (level >= speechThreshold) {
                    lastSpeechDetectedMs = now
                }
                val isGateOpen = (now - lastSpeechDetectedMs) <= SPEECH_HANGOVER_MS

                if (now - lastUiMeterUpdateMs >= 90L) {
                    lastUiMeterUpdateMs = now
                    val visibleLevel = if (isGateOpen) level else (level * 0.25f)
                    _state.update { it.copy(userAudioLevel = visibleLevel) }
                }

                if (isGateOpen) {
                    // Flush ~200ms pre-roll first so opening syllables aren't clipped
                    while (preRollQueue.isNotEmpty()) {
                        val pre = preRollQueue.removeFirst()
                        runCatching {
                            session.sendAudioRealtime(InlineData(pre, "audio/pcm"))
                        }.onFailure { err ->
                            if (err is CancellationException) throw err
                            Log.w(TAG, "sendAudioRealtime pre-roll error", err)
                        }
                    }
                    runCatching {
                        session.sendAudioRealtime(InlineData(payload, "audio/pcm"))
                    }.onFailure { err ->
                        if (err is CancellationException) throw err
                        Log.w(TAG, "sendAudioRealtime error", err)
                    }
                } else {
                    if (preRollQueue.size >= 2) {
                        preRollQueue.removeFirst()
                    }
                    preRollQueue.addLast(payload)
                }
            }
        } finally {
            runCatching { record.stop() }
        }
    }

    private fun createAndStartAudioTrack(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4800)

        // ~500ms jitter-resistant playback buffer at 24kHz 16-bit mono (48,000 B/s)
        val smoothBufferSize = maxOf(minBuf * 4, 24_000)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(smoothBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        return track
    }

    private suspend fun runPlaybackLoop(queue: Channel<ByteArray>, track: AudioTrack) = withContext(Dispatchers.IO) {
        var lastMeterUpdateMs = 0L
        for (firstChunk in queue) {
            if (!isActive) break

            // Coalesce burst of immediately-ready PCM chunks on a fresh turn for stutter-free playback
            val batch = ByteArrayOutputStream(firstChunk.size * 2)
            batch.write(firstChunk)
            if (playbackDrainDeadlineMs.get() < SystemClock.elapsedRealtime()) {
                delay(45L) // Small jitter pre-buffer only at start of utterance
            }
            while (batch.size() < 9_600) {
                val next = queue.tryReceive().getOrNull() ?: break
                batch.write(next)
            }

            val pcm = batch.toByteArray()
            val now = SystemClock.elapsedRealtime()
            val durationMs = (pcm.size.toLong() * 1000L) / (OUTPUT_SAMPLE_RATE * 2L)
            playbackDrainDeadlineMs.updateAndGet { current ->
                (maxOf(current, now) + durationMs + SPEAKER_TAIL_HOLD_MS)
                    .coerceAtMost(now + 20_000L)
            }

            if (now - lastMeterUpdateMs >= 80L) {
                lastMeterUpdateMs = now
                val level = computePcm16Rms(pcm, pcm.size)
                _state.update { state ->
                    state.copy(
                        teacherAudioLevel = level,
                        status = if (state.isSessionActive && !state.isMuted) {
                            LiveConnectionStatus.TEACHER_SPEAKING
                        } else {
                            state.status
                        }
                    )
                }
            }

            var offset = 0
            while (offset < pcm.size && isActive) {
                val written = track.write(pcm, offset, pcm.size - offset)
                if (written <= 0) break
                offset += written
            }
        }
    }

    private suspend fun runReceiveLoop(
        session: LiveSession,
        queue: Channel<ByteArray>,
        track: AudioTrack
    ) {
        val userTurnBuffer = StringBuilder()
        val teacherTurnBuffer = StringBuilder()

        session.receive().collect { message ->
            when (message) {
                is LiveServerContent -> {
                    if (message.interrupted) {
                        drainChannel(queue)
                        runCatching {
                            track.pause()
                            track.flush()
                            track.play()
                        }
                        isReceivingTeacherAudio.set(false)
                        playbackDrainDeadlineMs.set(0L)
                        if (teacherTurnBuffer.isNotBlank()) {
                            appendTranscriptEntry(
                                isStudent = false,
                                text = teacherTurnBuffer.toString().trim(),
                                tag = "Kesildi"
                            )
                            teacherTurnBuffer.clear()
                        }
                        _state.update {
                            it.copy(
                                teacherAudioLevel = 0f,
                                liveTeacherTranscript = "",
                                status = if (it.isMuted) LiveConnectionStatus.MUTED else LiveConnectionStatus.LISTENING
                            )
                        }
                    }

                    val inputText = message.inputTranscription?.text
                    if (!inputText.isNullOrBlank()) {
                        userTurnBuffer.append(inputText)
                        _state.update { it.copy(liveUserTranscript = userTurnBuffer.toString().trim()) }
                    }

                    val outputText = message.outputTranscription?.text
                    if (!outputText.isNullOrBlank()) {
                        teacherTurnBuffer.append(outputText)
                        _state.update { it.copy(liveTeacherTranscript = teacherTurnBuffer.toString().trim()) }
                    }

                    val parts = message.content?.parts.orEmpty()
                    for (part in parts) {
                        when (part) {
                            is InlineDataPart -> {
                                if (part.mimeType.startsWith("audio/pcm", ignoreCase = true) &&
                                    part.inlineData.isNotEmpty()
                                ) {
                                    forceBargeInWindowUntilMs.set(0L)
                                    isReceivingTeacherAudio.set(true)
                                    queue.trySend(part.inlineData)
                                }
                            }
                            is TextPart -> {
                                if (outputText.isNullOrBlank() && part.text.isNotBlank()) {
                                    teacherTurnBuffer.append(part.text)
                                    _state.update { it.copy(liveTeacherTranscript = teacherTurnBuffer.toString().trim()) }
                                }
                            }
                            else -> Unit
                        }
                    }

                    if (message.turnComplete) {
                        isReceivingTeacherAudio.set(false)
                        val uText = userTurnBuffer.toString().trim()
                        val tText = teacherTurnBuffer.toString().trim()
                        if (uText.isNotBlank()) {
                            appendTranscriptEntry(isStudent = true, text = uText)
                        }
                        if (tText.isNotBlank()) {
                            appendTranscriptEntry(isStudent = false, text = tText)
                        }
                        userTurnBuffer.clear()
                        teacherTurnBuffer.clear()
                        val stillPlaying = isTeacherSpeakingNow()
                        _state.update {
                            it.copy(
                                liveUserTranscript = "",
                                liveTeacherTranscript = "",
                                teacherAudioLevel = if (stillPlaying) it.teacherAudioLevel else 0f,
                                status = when {
                                    it.isMuted -> LiveConnectionStatus.MUTED
                                    stillPlaying -> LiveConnectionStatus.TEACHER_SPEAKING
                                    else -> LiveConnectionStatus.LISTENING
                                }
                            )
                        }
                    }
                }

                is LiveSessionResumptionUpdate -> {
                    if (message.resumable == true && !message.newHandle.isNullOrBlank()) {
                        resumptionHandle = message.newHandle
                    }
                }

                is LiveServerGoAway -> {
                    _state.update {
                        it.copy(statusNotice = "Canlı oturum yenilenmek üzere, kısa süre içinde yeniden bağlanabilirsiniz.")
                    }
                }

                else -> Unit
            }
        }
    }

    private fun appendTranscriptEntry(isStudent: Boolean, text: String, tag: String = "") {
        turnCounter++
        val entry = LiveTranscriptEntry(
            id = "live_turn_${System.currentTimeMillis()}_$turnCounter",
            isStudent = isStudent,
            text = text,
            tag = tag
        )
        _state.update { state ->
            state.copy(transcriptItems = (state.transcriptItems + entry).takeLast(80))
        }
    }

    private fun drainChannel(channel: Channel<ByteArray>) {
        while (channel.tryReceive().isSuccess) {
            // discard buffered audio on interruption
        }
    }

    private fun releaseAudioAndSession() {
        val s = liveSession
        liveSession = null
        if (s != null) {
            runCatching { s.stopReceiving() }
            kotlinx.coroutines.runBlocking {
                runCatching { s.close() }
            }
        }
        releaseAudioOnly()
    }

    private fun releaseAudioOnly() {
        playbackChannel?.close()
        playbackChannel = null

        runCatching { echoCanceler?.release() }
        echoCanceler = null
        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
        runCatching { automaticGainControl?.release() }
        automaticGainControl = null

        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null

        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null

        audioManager?.let { am ->
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { req -> am.abandonAudioFocusRequest(req) }
                } else {
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus(null)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.clearCommunicationDevice()
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = previousSpeakerphoneOn
                }
                am.mode = previousAudioMode
            }
        }
        audioFocusRequest = null
        audioManager = null
    }

    private fun buildLiveSystemInstruction(
        profile: StudentProfile,
        subject: String,
        mode: TeachingMode,
        pdfTitle: String?,
        activePdfPage: Int?
    ): com.google.firebase.ai.type.Content {
        val pdfLine = if (!pdfTitle.isNullOrBlank()) {
            "Öğrencinin şu an açık olan PDF kaynağı: '$pdfTitle' (Sayfa ${activePdfPage ?: 1}). " +
                "PDF görüntüsü gönderildiğinde yalnızca gördüğün bilgileri referans al, uydurma."
        } else {
            "Aktif PDF kaynağı yok."
        }

        return content {
            text(
                """
                Sen MrStudy Canlı Ders Öğretmenisin. Öğrenciyle gerçek zamanlı sesli ders yapıyorsun.
                ÖĞRENCİ: ${profile.displayName}, Sınıf: ${profile.gradeLevel}, Hedef: ${profile.targetGoal}
                DERS: $subject
                ÖĞRETMEN MODU: ${mode.title} (${mode.promptGuidance})
                $pdfLine

                CANLI DERS KURALLARI:
                1. Her zaman son derece akıcı, doğal vurgulu, sıcak, motive edici ve anlaşılır TÜRKÇE konuş.
                2. Cümleleri yarıda kesme; konuyu mantıklı bir akışla, 3-5 akıcı cümlelik anlamlı bir anlatım bloğu halinde tamamla, sonunda öğrenciye "Buraya kadar anlaşıldı mı?" veya "Bir örnekle pekiştirelim mi?" diye sor.
                3. Matematik ifadelerini ve sembolleri kulakla rahat anlaşılacak şekilde tane tane oku (örn. 'x kare artı iki x eşittir beş').
                4. Yabancı sesler veya anlamsız arka plan gürültüsü duyarsan uydurma yanıt verme, sadece öğrencinin net Türkçe sorusuna odaklan.
                5. Emin olmadığın bilgiyi asla uydurma.
                """.trimIndent()
            )
        }
    }

    private fun compressBitmapToJpeg(bitmap: Bitmap): ByteArray {
        val maxSide = 768
        val scale = minOf(
            maxSide.toFloat() / bitmap.width.coerceAtLeast(1),
            maxSide.toFloat() / bitmap.height.coerceAtLeast(1),
            1f
        )
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
            out.toByteArray()
        }
    }

    private fun computePcm16Rms(bytes: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var sumSq = 0.0
        var count = 0
        var i = 0
        while (i + 1 < length) {
            val low = bytes[i].toInt() and 0xFF
            val high = bytes[i + 1].toInt()
            val sample = ((high shl 8) or low).toShort().toInt()
            val norm = sample / 32768.0
            sumSq += norm * norm
            count++
            i += 2
        }
        if (count == 0) return 0f
        val rms = sqrt(sumSq / count).toFloat()
        return (rms * 4.5f).coerceIn(0f, 1f)
    }

    private fun mapLiveErrorMessage(error: Throwable): String {
        if (error is TimeoutCancellationException) {
            return "Canlı ders bağlantısı zaman aşımına uğradı. İnternetinizi kontrol edip tekrar deneyin."
        }
        val chain = generateSequence(error) { it.cause }
            .mapNotNull { it.message ?: it.localizedMessage }
            .joinToString(" | ")
        return when {
            isAppCheckErrorMessage(chain) ||
                chain.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                "Firebase App Check doğrulaması reddedildi (403). Bu cihazdaki Debug Token henüz Firebase Console'a kayıtlı değil. Aşağıdan token'ı kopyalayıp Firebase Console > App Check > Manage debug tokens bölümüne ekleyerek Canlı Ders ve Normal Modu hemen açabilirsiniz."
            }
            error is IOException ||
                chain.contains("network", ignoreCase = true) ||
                chain.contains("WebSocket", ignoreCase = true) ||
                chain.contains("UnknownHost", ignoreCase = true) -> {
                "Canlı ses bağlantısı kurulamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
            }
            chain.contains("quota", ignoreCase = true) ||
                chain.contains("429", ignoreCase = true) -> {
                "Canlı ders istek kotasına ulaşıldı. Lütfen birkaç dakika sonra tekrar deneyin."
            }
            else -> error.localizedMessage ?: "Canlı ders oturumu başlatılamadı."
        }
    }

    private companion object {
        const val TAG = "GeminiLiveTeacher"
        const val INPUT_SAMPLE_RATE = 16_000
        const val OUTPUT_SAMPLE_RATE = 24_000
        const val APP_CHECK_TIMEOUT_MS = 10_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val SPEAKER_TAIL_HOLD_MS = 450L
        const val NOISE_GATE_SPEECH_RMS = 0.018f
        const val BARGE_IN_SPEECH_RMS = 0.065f
        const val SPEECH_HANGOVER_MS = 750L
    }
}
