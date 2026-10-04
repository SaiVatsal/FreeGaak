package com.saivatsal.soundorbit.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saivatsal.soundorbit.core.database.entity.ArtistPlayCount
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.model.TrendingWindow
import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.repository.ListeningHistoryRepository
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.core.source.SourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeFeedState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val audiusTrending: List<Track> = emptyList(),
    val jamendoTrending: List<Track> = emptyList(),
    val localRecent: List<Track> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val listeningHistoryRepository: ListeningHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _feedState = MutableStateFlow(HomeFeedState())
    val feedState: StateFlow<HomeFeedState> = _feedState.asStateFlow()

    val recentlyPlayed: StateFlow<List<Track>> = listeningHistoryRepository
        .getRecentlyPlayedTracks(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heavyRotation: StateFlow<List<Track>> = listeningHistoryRepository
        .getMostPlayedTracks(limit = 15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topArtists: StateFlow<List<ArtistPlayCount>> = listeningHistoryRepository
        .getTopArtists(limit = 10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = favoritesRepository
        .getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFeed(isRefresh = false)
    }

    fun refreshFeed() {
        loadFeed(isRefresh = true)
    }

    private fun loadFeed(isRefresh: Boolean) {
        viewModelScope.launch {
            _feedState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, errorMessage = null) }

            try {
                val audiusDeferred = async {
                    val audius = sourceRegistry.getSource(SourceId.AUDIUS)
                    audius?.trending(null, TrendingWindow.WEEK, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val jamendoDeferred = async {
                    val jamendo = sourceRegistry.getSource(SourceId.JAMENDO)
                    jamendo?.trending(null, TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val localDeferred = async {
                    val local = sourceRegistry.getSource(SourceId.LOCAL)
                    local?.trending(null, TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val audiusTracks = audiusDeferred.await()
                val jamendoTracks = jamendoDeferred.await()
                val localTracks = localDeferred.await()

                _feedState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        audiusTrending = audiusTracks,
                        jamendoTrending = jamendoTracks,
                        localRecent = localTracks
                    )
                }
            } catch (e: Exception) {
                _feedState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to load home feed"
                    )
                }
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            audioPlayer.playTrack(track)
        }
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            viewModelScope.launch {
                audioPlayer.playTrack(tracks[startIndex], tracks, startIndex)
            }
        }
    }
}
