package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class SimulatedDialogueTurn(
    val speakerLabel: String,
    val text: String,
    val pitch: Float
)

class ComplianceTtsAnnouncer(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var simulationJob: Job? = null

    private val _isSpeakingNotice = MutableStateFlow(false)
    val isSpeakingNotice: StateFlow<Boolean> = _isSpeakingNotice.asStateFlow()

    private val _isSimulatingCall = MutableStateFlow(false)
    val isSimulatingCall: StateFlow<Boolean> = _isSimulatingCall.asStateFlow()

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
                @Suppress("DEPRECATION")
                audioManager?.isSpeakerphoneOn = true
            }
        }

        _isSpeakingNotice.value = true

        val engine = tts
        if (!isInitialized || engine == null) {
            // If TTS engine is initializing or unavailable in container, simulate spoken timing cleanly
            scope.launch(Dispatchers.Main) {
                delay(2200L)
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
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
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

    /**
     * Plays a multi-speaker business call conversation aloud via TextToSpeech while recording is active,
     * allowing users to test live call recording, waveform response, and Gemini AI transcription/summarization
     * even when not on an active cellular phone call.
     */
    fun startSimulatedCallDialogue(
        contactName: String,
        scenarioTopic: String,
        onLineSpoken: (formattedLine: String, syntheticAmplitude: Float) -> Unit,
        onCompleted: () -> Unit
    ) {
        simulationJob?.cancel()
        _isSimulatingCall.value = true
        val partner = contactName.ifBlank { "Elena Vance" }

        val turns = when (scenarioTopic) {
            "Sales & Contract Renewal" -> listOf(
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "Hi $partner, thanks for joining. As announced by our compliance alert, this call is being recorded for our contract summary.",
                    1.0f
                ),
                SimulatedDialogueTurn(
                    "Speaker 2 ($partner)",
                    "Thanks for the heads-up, I consent to the recording. Let's finalize the Q4 Enterprise renewal and seat expansion.",
                    1.18f
                ),
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "We can lock in a 15 percent volume discount if we expand from 120 to 200 seats before October 15th, bringing the annual total to 84,000 dollars.",
                    1.0f
                ),
                SimulatedDialogueTurn(
                    "Speaker 2 ($partner)",
                    "That pricing works for our Q4 budget. Please send the updated Master Services Agreement and security compliance addendum by Thursday so Legal can sign off.",
                    1.18f
                ),
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "Agreed. I will send the revised MSA by Thursday noon and schedule our technical onboarding kickoff for next Tuesday.",
                    1.0f
                )
            )

            "Product Roadmap & Engineering" -> listOf(
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "Hey $partner, recording is active with consent verified. Let's review the v2.4 mobile release blockers and cloud storage migration.",
                    1.0f
                ),
                SimulatedDialogueTurn(
                    "Speaker 2 ($partner)",
                    "Sounds good. Latency on the real-time keyword tagging pipeline dropped by 42 percent after switching to Gemini Flash.",
                    0.88f
                ),
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "Awesome. What about the Notion and Evernote export connectors for enterprise workspaces?",
                    1.0f
                ),
                SimulatedDialogueTurn(
                    "Speaker 2 ($partner)",
                    "Both Notion API page creation and Evernote ENEX export passed QA today. We just need to finalize the SHA-256 cloud vault checksum verification before Friday's freeze.",
                    0.88f
                )
            )

            else -> listOf(
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "Hello $partner, confirming you heard the automated privacy compliance notice at the start of this call.",
                    1.0f
                ),
                SimulatedDialogueTurn(
                    "Speaker 2 ($partner)",
                    "Yes, confirmed. Let's review the project milestones, budget allocation, and next week's deliverables.",
                    1.12f
                ),
                SimulatedDialogueTurn(
                    "Speaker 1 (You)",
                    "We are on track for the phase two launch. I will share the Notion summary and action items right after we hang up.",
                    1.0f
                )
            )
        }

        simulationJob = scope.launch(Dispatchers.Main) {
            var elapsedSec = 2
            for (turn in turns) {
                val timeTag = "[%02d:%02d]".format(elapsedSec / 60, elapsedSec % 60)
                val formatted = "$timeTag ${turn.speakerLabel}: ${turn.text}"
                onLineSpoken(formatted, 0.78f)

                if (isInitialized && tts != null) {
                    runCatching {
                        tts?.setPitch(turn.pitch)
                        tts?.speak(turn.text, TextToSpeech.QUEUE_FLUSH, null, "sim_turn_$elapsedSec")
                    }
                }

                // Pulse synthetic waveform while the line is spoken
                val steps = 14
                for (i in 0 until steps) {
                    val pulse = 0.35f + ((i * 17) % 55) / 100f
                    onLineSpoken("", pulse)
                    delay(220L)
                }
                elapsedSec += 7
            }
            _isSimulatingCall.value = false
            onCompleted()
        }
    }

    fun stopAll() {
        simulationJob?.cancel()
        simulationJob = null
        _isSpeakingNotice.value = false
        _isSimulatingCall.value = false
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stopAll()
        runCatching { tts?.shutdown() }
    }
}
