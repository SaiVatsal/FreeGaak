package com.saivatsal.soundorbit.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.core.source.SourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedSource: SourceId? = null, // null means all sources
    val selectedFilter: SearchFilterType = SearchFilterType.ALL,
    val isSearching: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val errorMessage: String? = null
)

val POPULAR_GENRES = listOf(
    "Bollywood", "Punjabi", "Pop", "Hip-Hop", "Dance", "EDM",
    "Rock", "R&B", "Lo-Fi", "Latin", "K-Pop", "Electronic",
    "Indie", "Acoustic", "Jazz", "Classical"
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val favoritesRepository: FavoritesRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                tracks = emptyList(),
                                artists = emptyList(),
                                albums = emptyList(),
                                playlists = emptyList(),
                                errorMessage = null
                            )
                        }
                    } else {
                        performSearch(query, _uiState.value.selectedSource, _uiState.value.selectedFilter)
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryFlow.value = newQuery
    }

    fun onSourceFilterSelect(sourceId: SourceId?) {
        _uiState.update { it.copy(selectedSource = sourceId) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query, sourceId, _uiState.value.selectedFilter)
        }
    }

    fun onCategoryFilterSelect(filter: SearchFilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query, _uiState.value.selectedSource, filter)
        }
    }

    fun searchGenre(genre: String) {
        onQueryChange(genre)
    }

    private fun performSearch(query: String, sourceId: SourceId?, filter: SearchFilterType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }

            val sources = if (sourceId != null) {
                listOfNotNull(sourceRegistry.getSource(sourceId))
            } else {
                sourceRegistry.allSources()
            }

            try {
                val results = sources.map { src ->
                    async {
                        src.search(query, SearchFilter(type = filter), PageRequest(0, 20))
                    }
                }.awaitAll()

                val allTracks = mutableListOf<Track>()
                val allArtists = mutableListOf<Artist>()
                val allAlbums = mutableListOf<Album>()
                val allPlaylists = mutableListOf<Playlist>()

                for (res in results) {
                    if (res is SourceResult.Success) {
                        allTracks.addAll(res.value.tracks.items)
                        allArtists.addAll(res.value.artists.items)
                        allAlbums.addAll(res.value.albums.items)
                        allPlaylists.addAll(res.value.playlists.items)
                    }
                }

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        tracks = allTracks.distinctBy { t -> t.compositeKey },
                        artists = allArtists.distinctBy { a -> a.compositeKey },
                        albums = allAlbums.distinctBy { al -> al.compositeKey },
                        playlists = allPlaylists.distinctBy { p -> p.compositeKey }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = e.message ?: "Search failed"
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

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track)
        }
    }
}
