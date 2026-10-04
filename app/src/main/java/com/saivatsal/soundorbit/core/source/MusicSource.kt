package com.saivatsal.soundorbit.core.source

import com.saivatsal.soundorbit.core.model.*

data class SourceCapabilities(
    val canSearch: Boolean = true,
    val hasTrending: Boolean = true,
    val hasGenres: Boolean = true,
    val hasTags: Boolean = true,
    val hasLanguageFilter: Boolean = false,
    val hasPlaylists: Boolean = true,
    val hasArtistBio: Boolean = false,
    val supportsSelectableQuality: Boolean = false,
    val allowsDownloads: Boolean = false,
    val ownsAudioPipeline: Boolean = true
)

data class PageRequest(val offset: Int = 0, val limit: Int = 20)
data class Page<T>(val items: List<T>, val nextOffset: Int?)

sealed interface SourceError {
    data object Offline : SourceError
    data class RateLimited(val retryAfterMs: Long?) : SourceError
    data object Gated : SourceError
    data object NotFound : SourceError
    data object MissingOrInvalidKey : SourceError
    data class Unknown(val cause: Throwable?) : SourceError
}

sealed interface SourceResult<out T> {
    data class Success<T>(val value: T) : SourceResult<T>
    data class Failure(val error: SourceError) : SourceResult<Nothing>

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
}

data class SearchResults(
    val tracks: Page<Track> = Page(emptyList(), null),
    val artists: Page<Artist> = Page(emptyList(), null),
    val albums: Page<Album> = Page(emptyList(), null),
    val playlists: Page<Playlist> = Page(emptyList(), null)
)

interface MusicSource {
    val id: SourceId
    val displayName: String
    val capabilities: SourceCapabilities

    suspend fun search(query: String, filter: SearchFilter, page: PageRequest): SourceResult<SearchResults>
    suspend fun trending(genre: String?, window: TrendingWindow, page: PageRequest): SourceResult<Page<Track>>
    suspend fun browse(kind: BrowseKind, value: String, page: PageRequest): SourceResult<Page<Track>>
    suspend fun track(id: String): SourceResult<Track>
    suspend fun artist(id: String): SourceResult<ArtistDetails>
    suspend fun album(id: String): SourceResult<AlbumDetails>
    suspend fun playlist(id: String): SourceResult<PlaylistDetails>
    suspend fun resolveStream(trackId: String, quality: AudioQuality): SourceResult<ResolvedStream>
}
