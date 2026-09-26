package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines. delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Announces the pre-recording privacy compliance disclosure aloud over the phone call / speakerphone
 * using Android's `TextToSpeech` engine so all call participants are notified prior to recording.
 */
class ComplianceTtsAnnouncer(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeakingNotice = MutableStateFlow(false)
    val isSpeakingNotice: StateFlow<Boolean> = _isSpeakingNotice.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isInitialized = true
        }
    }

    fun speakComplianceNotice(
        script: String,
        routeToSpeakerphone: Boolean,
        onFinished: () -> Unit = {}
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (routeToSpeakerphone) {
            runCatching {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = true
            }
        }

        _isSpeakingNotice.value = true

        val engine = tts
        if (!isInitialized || engine == null) {
            scope.launch(Dispatchers.Main) {
                delay(1800L)
                _isSpeakingNotice.value = false
                onFinished()
            }
            return
        }

        engine.setPitch(1.0f)
        engine.setSpeechRate(1.02f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeakingNotice.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeakingNotice.value = false
                scope.launch(Dispatchers.Main) { onFinished() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeakingNotice.value = false
                scope.launch(Dispatchers.Main) { onFinished() }
            }
        })

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
        }
        val result = engine.speak(script, TextToSpeech.QUEUE_FLUSH, params, "compliance_notice_utt")
        if (result == TextToSpeech.ERROR) {
            scope.launch(Dispatchers.Main) {
                delay(1800L)
                _isSpeakingNotice.value = false
                onFinished()
            }
        }
    }

    fun stopAll() {
        _isSpeakingNotice.value = false
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stopAll()
        runCatching { tts?.shutdown() }
    }
}
