package com.saivatsal.soundorbit.core.player

import com.saivatsal.soundorbit.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor(
    private val player: AudioPlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val snapshot: StateFlow<PlayerSnapshot> = player.snapshot

    val currentTrack: StateFlow<Track?> = snapshot
        .map { it.currentTrack }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val isPlaying: StateFlow<Boolean> = snapshot
        .map { it.status == PlaybackStatus.PLAYING }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val positionMs: StateFlow<Long> = snapshot
        .map { it.positionMs }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    val durationMs: StateFlow<Long> = snapshot
        .map { it.durationMs }
        .stateIn(scope, SharingStarted.Eagerly, 0L)

    val queue: StateFlow<List<Track>> = snapshot
        .map { it.queue }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val repeatState: StateFlow<RepeatState> = snapshot
        .map { it.repeatState }
        .stateIn(scope, SharingStarted.Eagerly, RepeatState.OFF)

    val isShuffle: StateFlow<Boolean> = snapshot
        .map { it.isShuffle }
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun playTrack(track: Track, queue: List<Track> = listOf(track), startIndex: Int = 0) {
        player.playTrack(track, queue, startIndex)
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun togglePlayPause() = player.togglePlayPause()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun skipToNext() = player.skipToNext()
    fun skipToPrevious() = player.skipToPrevious()
    fun setShuffle(enabled: Boolean) = player.setShuffle(enabled)
    fun setRepeatState(state: RepeatState) = player.setRepeatState(state)
    fun addToQueue(track: Track) = player.addToQueue(track)
    fun removeFromQueue(index: Int) = player.removeFromQueue(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = player.moveQueueItem(fromIndex, toIndex)
    fun clearQueue() = player.clearQueue()
    fun setCrossfadeDuration(seconds: Int) = player.setCrossfadeDuration(seconds)
    fun release() = player.release()
}
