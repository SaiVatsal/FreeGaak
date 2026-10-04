package com.saivatsal.soundorbit.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saivatsal.soundorbit.core.audio.EqualizerController
import com.saivatsal.soundorbit.core.audio.EqualizerState
import com.saivatsal.soundorbit.core.lyrics.model.Lyrics
import com.saivatsal.soundorbit.core.lyrics.repository.LyricsRepository
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.player.PlaybackStatus
import com.saivatsal.soundorbit.core.player.PlayerSnapshot
import com.saivatsal.soundorbit.core.player.RepeatState
import com.saivatsal.soundorbit.core.player.SleepTimer
import com.saivatsal.soundorbit.core.player.SleepTimerState
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LyricsUiState {
    object Idle : LyricsUiState
    object Loading : LyricsUiState
    data class Success(val lyrics: Lyrics) : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioPlayer: AudioPlayer,
    private val lyricsRepository: LyricsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistRepository: PlaylistRepository,
    private val equalizerController: EqualizerController,
    private val sleepTimer: SleepTimer
) : ViewModel() {

    val playerState: StateFlow<PlayerSnapshot> = audioPlayer.snapshot
    val equalizerState: StateFlow<EqualizerState> = equalizerController.state
    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimer.state

    private val _lyricsUiState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val lyricsUiState: StateFlow<LyricsUiState> = _lyricsUiState.asStateFlow()

    val isFavorite: StateFlow<Boolean> = playerState
        .map { it.currentTrack }
        .distinctUntilChanged()
        .flatMapLatest { track ->
            if (track != null) {
                favoritesRepository.isFavorite(track.compositeKey)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe current track changes to auto-load lyrics
        viewModelScope.launch {
            playerState
                .map { it.currentTrack }
                .distinctUntilChanged()
                .collect { track ->
                    if (track != null) {
                        loadLyrics(track)
                    } else {
                        _lyricsUiState.value = LyricsUiState.Idle
                    }
                }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            audioPlayer.playTrack(track)
        }
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        viewModelScope.launch {
            audioPlayer.playTrack(tracks[startIndex], tracks, startIndex)
        }
    }

    fun togglePlayPause() {
        if (playerState.value.status == PlaybackStatus.PLAYING) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }

    fun next() {
        audioPlayer.skipToNext()
    }

    fun previous() {
        audioPlayer.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    fun setRepeatMode(repeatMode: RepeatState) {
        audioPlayer.setRepeatState(repeatMode)
    }

    fun toggleShuffle() {
        audioPlayer.setShuffle(!playerState.value.isShuffle)
    }

    fun toggleFavorite() {
        val currentTrack = playerState.value.currentTrack ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(currentTrack)
        }
    }

    fun loadLyrics(track: Track) {
        viewModelScope.launch {
            _lyricsUiState.value = LyricsUiState.Loading
            lyricsRepository.getLyrics(track)
                .onSuccess { lyrics ->
                    _lyricsUiState.value = LyricsUiState.Success(lyrics)
                }
                .onFailure { error ->
                    _lyricsUiState.value = LyricsUiState.Error(error.message ?: "Lyrics not found")
                }
        }
    }

    // Equalizer controls
    fun setEqualizerEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }

    fun selectEqualizerPreset(presetIndex: Short) {
        equalizerController.usePreset(presetIndex)
    }

    fun setEqualizerBandLevel(bandIndex: Short, levelMb: Short) {
        equalizerController.setBandLevel(bandIndex, levelMb)
    }

    fun setBassBoost(strength: Short) {
        equalizerController.setBassBoostStrength(strength)
    }

    fun setVirtualizer(strength: Short) {
        equalizerController.setVirtualizerStrength(strength)
    }

    // Sleep Timer controls
    fun startSleepTimer(minutes: Int, fadeOut: Boolean) {
        sleepTimer.startTimer(minutes, fadeOut)
    }

    fun startSleepTimerEndOfTrack() {
        sleepTimer.startEndOfTrack()
    }

    fun cancelSleepTimer() {
        sleepTimer.cancelTimer()
    }

    // Queue management
    fun playTrackFromQueue(index: Int) {
        val queue = playerState.value.queue
        if (index in queue.indices) {
            audioPlayer.playTrack(queue[index], queue, index)
        }
    }

    fun removeFromQueue(index: Int) {
        audioPlayer.removeFromQueue(index)
    }

    fun clearQueue() {
        audioPlayer.clearQueue()
    }

    // Playlist addition
    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun createPlaylistAndAddTrack(name: String, track: Track) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addTrackToPlaylist(playlistId, track)
        }
    }
}
