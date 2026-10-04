package com.saivatsal.soundorbit.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.repository.PlaylistRepository
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.core.source.SourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab {
    PLAYLISTS,
    FAVORITES,
    LOCAL_DEVICE,
    OFFLINE
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository,
    private val sourceRegistry: SourceRegistry,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.PLAYLISTS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Track>> = favoritesRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    init {
        loadLocalTracks()
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun loadLocalTracks() {
        viewModelScope.launch {
            val localSource = sourceRegistry.getSource(SourceId.LOCAL)
            if (localSource != null) {
                val result = localSource.browse(BrowseKind.GENRE, "all", PageRequest(0, 100))
                if (result is SourceResult.Success) {
                    _localTracks.value = result.value.items
                }
            }
        }
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
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

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track)
        }
    }
}
