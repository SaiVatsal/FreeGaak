package com.saivatsal.soundorbit.core.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val isEndOfTrack: Boolean = false,
    val fadeOutEnabled: Boolean = true
)

@Singleton
class SleepTimer @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun startTimer(minutes: Int, fadeOut: Boolean = true) {
        cancelTimer()
        val totalSec = minutes * 60L
        if (totalSec <= 0) return

        _state.update {
            SleepTimerState(
                isActive = true,
                remainingSeconds = totalSec,
                totalSeconds = totalSec,
                isEndOfTrack = false,
                fadeOutEnabled = fadeOut
            )
        }

        timerJob = scope.launch {
            var remaining = totalSec
            val fadeDurationSec = 30L.coerceAtMost(totalSec / 2)

            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _state.update { it.copy(remainingSeconds = remaining) }

                // Audio fade out over final seconds
                if (fadeOut && remaining <= fadeDurationSec && fadeDurationSec > 0) {
                    val volumeFactor = (remaining.toFloat() / fadeDurationSec.toFloat()).coerceIn(0.05f, 1.0f)
                    audioPlayer.setVolume(volumeFactor)
                }
            }

            if (isActive) {
                audioPlayer.pause()
                audioPlayer.setVolume(1.0f) // reset volume
                _state.update { SleepTimerState() }
            }
        }
    }

    fun startEndOfTrack() {
        cancelTimer()
        _state.update {
            SleepTimerState(
                isActive = true,
                remainingSeconds = 0,
                totalSeconds = 0,
                isEndOfTrack = true,
                fadeOutEnabled = false
            )
        }
    }

    fun onTrackEnded() {
        if (_state.value.isActive && _state.value.isEndOfTrack) {
            audioPlayer.pause()
            cancelTimer()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        audioPlayer.setVolume(1.0f)
        _state.update { SleepTimerState() }
    }
}
