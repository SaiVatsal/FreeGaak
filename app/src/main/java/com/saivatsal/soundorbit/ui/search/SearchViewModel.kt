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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Collections
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

private data class CachedSearchResults(
    val tracks: List<Track>,
    val artists: List<Artist>,
    val albums: List<Album>,
    val playlists: List<Playlist>,
    val timestamp: Long = System.currentTimeMillis()
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
    private var searchJob: Job? = null

    // LRU In-Memory Search Cache (capacity 50 queries, 5-minute TTL)
    private val memoryCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedSearchResults>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSearchResults>?): Boolean {
                return size > 50
            }
        }
    )

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        searchJob?.cancel()
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

    private fun getCacheKey(query: String, sourceId: SourceId?, filter: SearchFilterType): String {
        return "${query.trim().lowercase()}#${sourceId?.name ?: "ALL"}#${filter.name}"
    }

    private fun performSearch(query: String, sourceId: SourceId?, filter: SearchFilterType) {
        searchJob?.cancel()

        val normalizedKey = getCacheKey(query, sourceId, filter)
        val cached = memoryCache[normalizedKey]
        val now = System.currentTimeMillis()

        // Check if fresh cache exists (valid for 5 minutes)
        if (cached != null && (now - cached.timestamp < 300_000L)) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    tracks = cached.tracks,
                    artists = cached.artists,
                    albums = cached.albums,
                    playlists = cached.playlists,
                    errorMessage = null
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }

            val sources = if (sourceId != null) {
                listOfNotNull(sourceRegistry.getSource(sourceId))
            } else {
                sourceRegistry.allSources()
            }

            val accumulatedTracks = mutableListOf<Track>()
            val accumulatedArtists = mutableListOf<Artist>()
            val accumulatedAlbums = mutableListOf<Album>()
            val accumulatedPlaylists = mutableListOf<Playlist>()

            // Progressive stream merging: launch each source independently and update UI incrementally
            val sourceJobs = sources.map { src ->
                launch {
                    try {
                        val result = src.search(query.trim(), SearchFilter(type = filter), PageRequest(0, 20))
                        if (result is SourceResult.Success) {
                            val newTracks = result.value.tracks.items
                            val newArtists = result.value.artists.items
                            val newAlbums = result.value.albums.items
                            val newPlaylists = result.value.playlists.items

                            synchronized(accumulatedTracks) {
                                accumulatedTracks.addAll(newTracks)
                                accumulatedArtists.addAll(newArtists)
                                accumulatedAlbums.addAll(newAlbums)
                                accumulatedPlaylists.addAll(newPlaylists)

                                val distinctTracks = accumulatedTracks.distinctBy { t -> t.compositeKey }
                                val distinctArtists = accumulatedArtists.distinctBy { a -> a.compositeKey }
                                val distinctAlbums = accumulatedAlbums.distinctBy { al -> al.compositeKey }
                                val distinctPlaylists = accumulatedPlaylists.distinctBy { p -> p.compositeKey }

                                _uiState.update { state ->
                                    state.copy(
                                        tracks = distinctTracks,
                                        artists = distinctArtists,
                                        albums = distinctAlbums,
                                        playlists = distinctPlaylists
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Source errors are gracefully isolated so other sources continue streaming
                    }
                }
            }

            sourceJobs.forEach { it.join() }

            val finalTracks = accumulatedTracks.distinctBy { t -> t.compositeKey }
            val finalArtists = accumulatedArtists.distinctBy { a -> a.compositeKey }
            val finalAlbums = accumulatedAlbums.distinctBy { al -> al.compositeKey }
            val finalPlaylists = accumulatedPlaylists.distinctBy { p -> p.compositeKey }

            // Store in in-memory LRU cache
            memoryCache[normalizedKey] = CachedSearchResults(
                tracks = finalTracks,
                artists = finalArtists,
                albums = finalAlbums,
                playlists = finalPlaylists,
                timestamp = System.currentTimeMillis()
            )

            _uiState.update {
                it.copy(
                    isSearching = false,
                    tracks = finalTracks,
                    artists = finalArtists,
                    albums = finalAlbums,
                    playlists = finalPlaylists,
                    errorMessage = if (finalTracks.isEmpty() && finalArtists.isEmpty() && finalAlbums.isEmpty()) {
                        "No results found for '$query'"
                    } else null
                )
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
