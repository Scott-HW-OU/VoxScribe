package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min

class CallAudioRecorder(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var monitorJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _waveformAmplitudes = MutableStateFlow(List(40) { 0.08f })
    val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

    fun startRecording(): Result<File> {
        stopRecordingCleanup()
        return try {
            val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val file = File(recordingsDir, "call_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                // VOICE_RECOGNITION or MIC works reliably across Android 10-16 when Speakerphone is active
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _isPaused.value = false
            _elapsedSeconds.value = 0
            startAmplitudeAndTimerLoop()
            Result.success(file)
        } catch (e: Exception) {
            stopRecordingCleanup()
            Result.failure(e)
        }
    }

    fun pauseRecording() {
        if (!_isRecording.value || _isPaused.value) return
        runCatching {
            mediaRecorder?.pause()
            _isPaused.value = true
        }
    }

    fun resumeRecording() {
        if (!_isRecording.value || !_isPaused.value) return
        runCatching {
            mediaRecorder?.resume()
            _isPaused.value = false
        }
    }

    fun injectSyntheticAmplitudePulse(level: Float) {
        val clamped = level.coerceIn(0.12f, 0.95f)
        val current = _waveformAmplitudes.value.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(0)
        }
        current.add(clamped)
        _waveformAmplitudes.value = current
    }

    fun stopRecording(): File? {
        val recordedFile = outputFile
        monitorJob?.cancel()
        monitorJob = null
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // If MediaRecorder fails to stop cleanly (e.g., very short duration or virtual mic), keep file reference
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            _isPaused.value = false
        }
        return recordedFile
    }

    private fun stopRecordingCleanup() {
        monitorJob?.cancel()
        monitorJob = null
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        _isRecording.value = false
        _isPaused.value = false
    }

    private fun startAmplitudeAndTimerLoop() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Default) {
            var tickCounter = 0
            while (isActive && _isRecording.value) {
                delay(100L)
                if (!_isPaused.value) {
                    tickCounter++
                    if (tickCounter % 10 == 0) {
                        _elapsedSeconds.value += 1
                    }

                    val rawAmp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    val normalized = if (rawAmp > 50) {
                        min(1.0f, max(0.12f, rawAmp / 18000f))
                    } else {
                        0.08f
                    }

                    val updated = _waveformAmplitudes.value.drop(1) + normalized
                    _waveformAmplitudes.value = updated
                }
            }
        }
    }
}
