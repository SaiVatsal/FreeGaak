package com.saivatsal.soundorbit.core.player

import com.saivatsal.soundorbit.core.model.Track
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class RepeatState {
    OFF,
    ONE,
    ALL
}

data class PlayerSnapshot(
    val currentTrack: Track? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatState: RepeatState = RepeatState.OFF,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val volume: Float = 1.0f,
    val errorMessage: String? = null
)

interface AudioPlayer {
    val snapshot: StateFlow<PlayerSnapshot>

    fun playTrack(track: Track, queue: List<Track> = listOf(track), startIndex: Int = 0)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun setShuffle(enabled: Boolean)
    fun setRepeatState(state: RepeatState)
    fun addToQueue(track: Track)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun clearQueue()
    fun setCrossfadeDuration(seconds: Int)
    fun setVolume(volume: Float)
    fun getAudioSessionId(): Int
    fun release()
}
