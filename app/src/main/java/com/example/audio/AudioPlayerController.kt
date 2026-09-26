package com.example.audio

import android.media.MediaPlayer
import android.media.PlaybackParams
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

class AudioPlayerController(private val scope: CoroutineScope) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var activeFilePath: String? = null

    fun togglePlayPause(filePath: String?): Result<Unit> {
        if (filePath.isNullOrBlank() || !File(filePath).exists() || File(filePath).length() < 128L) {
            return Result.failure(IllegalStateException("Audio file is empty or unavailable on disk."))
        }

        if (activeFilePath == filePath && mediaPlayer != null) {
            val player = mediaPlayer!!
            return try {
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                } else {
                    player.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                stopAndRelease()
                Result.failure(e)
            }
        }

        stopAndRelease()
        return try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                    progressJob?.cancel()
                }
            }
            activeFilePath = filePath
            mediaPlayer = player
            _durationMs.value = player.duration.coerceAtLeast(1000)
            _currentPositionMs.value = 0
            applySpeed(_playbackSpeed.value)
            player.start()
            _isPlaying.value = true
            startProgressTracker()
            Result.success(Unit)
        } catch (e: Exception) {
            stopAndRelease()
            Result.failure(e)
        }
    }

    fun seekToFraction(fraction: Float) {
        val player = mediaPlayer ?: return
        val targetMs = (_durationMs.value * fraction.coerceIn(0f, 1f)).toInt()
        runCatching {
            player.seekTo(targetMs)
            _currentPositionMs.value = targetMs
        }
    }

    fun cyclePlaybackSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        if (_isPlaying.value) {
            applySpeed(nextSpeed)
        }
    }

    private fun applySpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val player = mediaPlayer ?: return
                val wasPlaying = player.isPlaying
                player.playbackParams = PlaybackParams().setSpeed(speed)
                if (!wasPlaying) {
                    player.pause()
                }
            }
        }
    }

    fun stopAndRelease() {
        progressJob?.cancel()
        progressJob = null
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
        activeFilePath = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && _isPlaying.value) {
                val pos = runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0)
                _currentPositionMs.value = pos
                delay(150L)
            }
        }
    }
}
