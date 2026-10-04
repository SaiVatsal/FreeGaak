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
    val selectedLanguage: String = "all",
    val regionalTrending: List<Track> = emptyList(),
    val latestReleases: List<Track> = emptyList(),
    val teluguTrending: List<Track> = emptyList(),
    val hindiTrending: List<Track> = emptyList(),
    val koreanTrending: List<Track> = emptyList(),
    val spotifyGlobalTrending: List<Track> = emptyList(),
    val spotifyIndiaTrending: List<Track> = emptyList(),
    val deezerTrending: List<Track> = emptyList(),
    val deezerGenreHits: List<Track> = emptyList(),
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

    fun selectLanguageFilter(language: String) {
        val currentLang = _feedState.value.selectedLanguage
        if (currentLang == language) return

        _feedState.update { it.copy(selectedLanguage = language) }
        loadRegionalLanguageFeed(language)
    }

    private fun loadRegionalLanguageFeed(language: String) {
        if (language == "all") {
            _feedState.update { it.copy(regionalTrending = emptyList()) }
            return
        }

        viewModelScope.launch {
            try {
                val deezer = sourceRegistry.getSource(SourceId.DEEZER)
                val spotify = sourceRegistry.getSource(SourceId.SPOTIFY)

                val deezerDeferred = async {
                    deezer?.trending(language, TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }
                val spotifyDeferred = async {
                    spotify?.trending(language, TrendingWindow.WEEK, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val tracks = (deezerDeferred.await() + spotifyDeferred.await()).distinctBy { it.compositeKey }
                _feedState.update { it.copy(regionalTrending = tracks) }
            } catch (e: Exception) {
                // Ignore regional filter error and keep existing feed
            }
        }
    }

    private fun loadFeed(isRefresh: Boolean) {
        viewModelScope.launch {
            _feedState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, errorMessage = null) }

            try {
                val deezer = sourceRegistry.getSource(SourceId.DEEZER)
                val spotify = sourceRegistry.getSource(SourceId.SPOTIFY)
                val audius = sourceRegistry.getSource(SourceId.AUDIUS)
                val jamendo = sourceRegistry.getSource(SourceId.JAMENDO)
                val local = sourceRegistry.getSource(SourceId.LOCAL)

                // 1. Priority #1: Telugu Top Hits
                val teluguDeferred = async {
                    deezer?.trending("telugu", TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                // 2. Priority #2: Hindi & Bollywood Top Hits
                val hindiDeferred = async {
                    deezer?.trending("hindi", TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                // 3. Priority #3: Korean / K-Pop Hits
                val koreanDeferred = async {
                    deezer?.trending("korean", TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                // 4. Latest Releases
                val latestDeferred = async {
                    val deezerNew = deezer?.trending("pop", TrendingWindow.WEEK, PageRequest(0, 10))?.getOrNull()?.items ?: emptyList()
                    val audiusNew = audius?.trending(null, TrendingWindow.WEEK, PageRequest(0, 10))?.getOrNull()?.items ?: emptyList()
                    (deezerNew + audiusNew).distinctBy { it.compositeKey }.take(15)
                }

                val spotifyGlobalDeferred = async {
                    spotify?.trending(null, TrendingWindow.WEEK, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val spotifyIndiaDeferred = async {
                    spotify?.trending("india", TrendingWindow.WEEK, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val deezerDeferred = async {
                    deezer?.trending(null, TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val deezerGenreDeferred = async {
                    deezer?.trending("pop", TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val audiusDeferred = async {
                    audius?.trending(null, TrendingWindow.WEEK, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val jamendoDeferred = async {
                    jamendo?.trending(null, TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val localDeferred = async {
                    local?.trending(null, TrendingWindow.ALL_TIME, PageRequest(0, 15))?.getOrNull()?.items ?: emptyList()
                }

                val teluguTracks = teluguDeferred.await()
                val hindiTracks = hindiDeferred.await()
                val koreanTracks = koreanDeferred.await()
                val latestTracks = latestDeferred.await()
                val spotifyGlobalTracks = spotifyGlobalDeferred.await()
                val spotifyIndiaTracks = spotifyIndiaDeferred.await()
                val deezerTracks = deezerDeferred.await()
                val deezerGenreTracks = deezerGenreDeferred.await()
                val audiusTracks = audiusDeferred.await()
                val jamendoTracks = jamendoDeferred.await()
                val localTracks = localDeferred.await()

                _feedState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        teluguTrending = teluguTracks,
                        hindiTrending = hindiTracks,
                        koreanTrending = koreanTracks,
                        latestReleases = latestTracks,
                        spotifyGlobalTrending = spotifyGlobalTracks,
                        spotifyIndiaTrending = spotifyIndiaTracks,
                        deezerTrending = deezerTracks,
                        deezerGenreHits = deezerGenreTracks,
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
