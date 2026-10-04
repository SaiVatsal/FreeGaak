package com.saivatsal.soundorbit.core.source.spotify

import com.saivatsal.soundorbit.core.common.Dispatcher
import com.saivatsal.soundorbit.core.common.RateLimiter
import com.saivatsal.soundorbit.core.common.SoundOrbitDispatchers
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.source.*
import com.saivatsal.soundorbit.core.source.deezer.DeezerSource
import com.saivatsal.soundorbit.core.source.spotify.model.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifySource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val deezerSource: DeezerSource,
    @Dispatcher(SoundOrbitDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MusicSource {

    override val id: SourceId = SourceId.SPOTIFY
    override val displayName: String = "Spotify Discovery"

    override val capabilities: SourceCapabilities = SourceCapabilities(
        canSearch = true,
        hasTrending = true,
        hasGenres = true,
        hasTags = true,
        hasLanguageFilter = false,
        hasPlaylists = true,
        hasArtistBio = false,
        supportsSelectableQuality = false,
        allowsDownloads = false,
        ownsAudioPipeline = false
    )

    private val rateLimiter = RateLimiter(minIntervalMs = 500L)
    private val tokenMutex = Mutex()

    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiryEpochMs: Long = 0L

    private val trackCache = ConcurrentHashMap<String, Track>()
    private val streamCache = ConcurrentHashMap<String, ResolvedStream>()

    private val searchAdapter = moshi.adapter(SpotifySearchResponse::class.java)
    private val trackAdapter = moshi.adapter(SpotifyTrackDto::class.java)
    private val artistAdapter = moshi.adapter(SpotifyArtistDto::class.java)
    private val albumAdapter = moshi.adapter(SpotifyAlbumDto::class.java)
    private val playlistAdapter = moshi.adapter(SpotifyPlaylistDto::class.java)
    private val tokenAdapter = moshi.adapter(SpotifyTokenResponse::class.java)

    private suspend fun getValidAccessToken(): String? = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < (tokenExpiryEpochMs - 60_000L)) {
            return cachedToken
        }

        try {
            val tokenRequest = Request.Builder()
                .url(TOKEN_URL)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            val response = okHttpClient.newCall(tokenRequest).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val tokenDto = tokenAdapter.fromJson(body)
                    if (tokenDto != null) {
                        val token = tokenDto.accessToken ?: tokenDto.altAccessToken
                        if (!token.isNullOrBlank()) {
                            cachedToken = token
                            tokenExpiryEpochMs = if (tokenDto.expirationMs > 0) {
                                tokenDto.expirationMs
                            } else {
                                now + (tokenDto.expiresInSec * 1000L)
                            }
                            return cachedToken
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall back or proceed
        }

        return cachedToken
    }

    override suspend fun trending(
        genre: String?,
        window: TrendingWindow,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val cleanGenre = genre?.trim()?.lowercase()
        val playlistId = when (cleanGenre) {
            "india", "bollywood", "hindi" -> TOP_50_INDIA_PLAYLIST
            "viral", "viral 50" -> VIRAL_50_GLOBAL_PLAYLIST
            "pop" -> POP_RISING_PLAYLIST
            "hip-hop", "rap" -> RAP_CAVIAR_PLAYLIST
            "dance", "electronic", "edm" -> DANCE_HITS_PLAYLIST
            "rock" -> ROCK_CLASSICS_PLAYLIST
            null, "", "all", "global" -> TOP_50_GLOBAL_PLAYLIST
            else -> null
        }

        if (playlistId != null) {
            val playlistResult = playlist(playlistId)
            if (playlistResult is SourceResult.Success) {
                val allTracks = playlistResult.value.tracks
                val fromIndex = page.offset.coerceAtMost(allTracks.size)
                val toIndex = (page.offset + page.limit).coerceAtMost(allTracks.size)
                val sublist = allTracks.subList(fromIndex, toIndex)
                sublist.forEach { trackCache[it.sourceTrackId] = it }
                val nextOffset = if (toIndex < allTracks.size) toIndex else null
                return@withContext SourceResult.Success(Page(sublist, nextOffset))
            }
        }

        // Fallback to searching top hits for the genre
        val query = when (cleanGenre) {
            "bollywood", "hindi" -> "Bollywood Top 50"
            "punjabi" -> "Punjabi Hits"
            "latin" -> "Viva Latino"
            else -> "${genre ?: "Global"} Top 50"
        }
        val searchRes = search(query, SearchFilter(type = SearchFilterType.ALL), page)
        if (searchRes is SourceResult.Success) {
            SourceResult.Success(searchRes.value.tracks)
        } else {
            SourceResult.Failure((searchRes as SourceResult.Failure).error)
        }
    }

    override suspend fun search(
        query: String,
        filter: SearchFilter,
        page: PageRequest
    ): SourceResult<SearchResults> = withContext(ioDispatcher) {
        if (query.isBlank()) {
            return@withContext SourceResult.Success(SearchResults())
        }

        val types = when (filter.type) {
            SearchFilterType.ALL -> "track,artist,album,playlist"
            SearchFilterType.SONGS -> "track"
            SearchFilterType.ARTISTS -> "artist"
            SearchFilterType.ALBUMS -> "album"
            SearchFilterType.PLAYLISTS -> "playlist"
        }

        val urlBuilder = "$API_BASE_URL/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("type", types)
            ?.addQueryParameter("offset", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        executeGet(urlBuilder.build().toString()) { body ->
            val response = searchAdapter.fromJson(body)
            val trackDtos = response?.tracks?.items ?: emptyList()
            val artistDtos = response?.artists?.items ?: emptyList()
            val albumDtos = response?.albums?.items ?: emptyList()
            val playlistDtos = response?.playlists?.items ?: emptyList()

            val domainTracks = trackDtos.map { it.toDomainTrack() }
            domainTracks.forEach { trackCache[it.sourceTrackId] = it }

            val domainArtists = artistDtos.map { it.toDomainArtist() }
            val domainAlbums = albumDtos.map { it.toDomainAlbum() }
            val domainPlaylists = playlistDtos.map { it.toDomainPlaylist() }

            val trackNext = if (domainTracks.size >= page.limit) page.offset + domainTracks.size else null
            val artistNext = if (domainArtists.size >= page.limit) page.offset + domainArtists.size else null
            val albumNext = if (domainAlbums.size >= page.limit) page.offset + domainAlbums.size else null
            val playlistNext = if (domainPlaylists.size >= page.limit) page.offset + domainPlaylists.size else null

            SearchResults(
                tracks = Page(domainTracks, trackNext),
                artists = Page(domainArtists, artistNext),
                albums = Page(domainAlbums, albumNext),
                playlists = Page(domainPlaylists, playlistNext)
            )
        }
    }

    override suspend fun browse(
        kind: BrowseKind,
        value: String,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val searchRes = search(value, SearchFilter(type = SearchFilterType.SONGS), page)
        if (searchRes is SourceResult.Success) {
            SourceResult.Success(searchRes.value.tracks)
        } else {
            SourceResult.Failure((searchRes as SourceResult.Failure).error)
        }
    }

    override suspend fun track(id: String): SourceResult<Track> = withContext(ioDispatcher) {
        trackCache[id]?.let { return@withContext SourceResult.Success(it) }

        val url = "$API_BASE_URL/tracks/$id"
        executeGet(url) { body ->
            val dto = trackAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Spotify track")
            val domain = dto.toDomainTrack()
            trackCache[id] = domain
            domain
        }
    }

    override suspend fun artist(id: String): SourceResult<ArtistDetails> = withContext(ioDispatcher) {
        val artistUrl = "$API_BASE_URL/artists/$id"
        val topTracksUrl = "$API_BASE_URL/artists/$id/top-tracks?market=US"

        val artistRes = executeGet(artistUrl) { body ->
            artistAdapter.fromJson(body)?.toDomainArtist()
                ?: throw IllegalArgumentException("Failed to parse Spotify artist")
        }

        val artist = when (artistRes) {
            is SourceResult.Success -> artistRes.value
            is SourceResult.Failure -> return@withContext artistRes
        }

        val topTracksRes = executeGet(topTracksUrl) { _ ->
            emptyList<Track>()
        }

        SourceResult.Success(
            ArtistDetails(
                artist = artist,
                bio = null,
                topTracks = topTracksRes.getOrNull() ?: emptyList(),
                albums = emptyList()
            )
        )
    }

    override suspend fun album(id: String): SourceResult<AlbumDetails> = withContext(ioDispatcher) {
        val url = "$API_BASE_URL/albums/$id"
        executeGet(url) { body ->
            val dto = albumAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Spotify album")
            val tracks = dto.tracks?.items?.map { it.toDomainTrack() } ?: emptyList()
            tracks.forEach { trackCache[it.sourceTrackId] = it }
            AlbumDetails(
                album = dto.toDomainAlbum(),
                tracks = tracks
            )
        }
    }

    override suspend fun playlist(id: String): SourceResult<PlaylistDetails> = withContext(ioDispatcher) {
        val url = "$API_BASE_URL/playlists/$id"
        executeGet(url) { body ->
            val dto = playlistAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Spotify playlist")
            val tracks = dto.tracks?.items?.mapNotNull { it.track?.toDomainTrack() } ?: emptyList()
            tracks.forEach { trackCache[it.sourceTrackId] = it }
            PlaylistDetails(
                playlist = dto.toDomainPlaylist(),
                tracks = tracks
            )
        }
    }

    override suspend fun resolveStream(
        trackId: String,
        quality: AudioQuality
    ): SourceResult<ResolvedStream> = withContext(ioDispatcher) {
        streamCache[trackId]?.let { return@withContext SourceResult.Success(it) }

        var track = trackCache[trackId]
        if (track == null) {
            val trackRes = track(trackId)
            if (trackRes is SourceResult.Success) {
                track = trackRes.value
            }
        }

        if (track == null) {
            return@withContext SourceResult.Failure(SourceError.NotFound)
        }

        // Cross-source stream resolution via Deezer high-quality stream
        val resolved = deezerSource.findMatchingStream(track.title, track.artistName)
        if (resolved != null) {
            streamCache[trackId] = resolved
            return@withContext SourceResult.Success(resolved)
        }

        SourceResult.Failure(SourceError.NotFound)
    }

    private suspend fun <T> executeGet(
        url: String,
        parse: (String) -> T
    ): SourceResult<T> = withContext(ioDispatcher) {
        val token = getValidAccessToken()
        if (token.isNullOrBlank()) {
            return@withContext SourceResult.Failure(SourceError.MissingOrInvalidKey)
        }

        try {
            rateLimiter.execute {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    when (response.code) {
                        200 -> {
                            val bodyString = response.body?.string()
                                ?: return@use SourceResult.Failure(SourceError.Unknown(null))
                            try {
                                SourceResult.Success(parse(bodyString))
                            } catch (e: Exception) {
                                SourceResult.Failure(SourceError.Unknown(e))
                            }
                        }
                        401 -> {
                            cachedToken = null
                            SourceResult.Failure(SourceError.MissingOrInvalidKey)
                        }
                        403 -> SourceResult.Failure(SourceError.MissingOrInvalidKey)
                        404 -> SourceResult.Failure(SourceError.NotFound)
                        429 -> {
                            val retryAfterHeader = response.header("Retry-After")
                            val retryAfterMs = retryAfterHeader?.toLongOrNull()?.let { it * 1000L }
                            SourceResult.Failure(SourceError.RateLimited(retryAfterMs))
                        }
                        else -> SourceResult.Failure(SourceError.Unknown(IOException("HTTP ${response.code}")))
                    }
                }
            }
        } catch (e: IOException) {
            SourceResult.Failure(SourceError.Offline)
        } catch (e: Exception) {
            SourceResult.Failure(SourceError.Unknown(e))
        }
    }

    companion object {
        private const val API_BASE_URL = "https://api.spotify.com/v1"
        private const val TOKEN_URL = "https://open.spotify.com/get_access_token"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        // Public Global and Regional Chart Playlist IDs
        const val TOP_50_GLOBAL_PLAYLIST = "37i9dQZEVXbMDoHDwVN2tF"
        const val TOP_50_INDIA_PLAYLIST = "37i9dQZEVXbLZ52XmNYSJg"
        const val VIRAL_50_GLOBAL_PLAYLIST = "37i9dQZEVXbLiRSasKsNU9"
        const val POP_RISING_PLAYLIST = "37i9dQZF1DWUa8ZRTfalHk"
        const val RAP_CAVIAR_PLAYLIST = "37i9dQZF1DX0XUsuxWHRQd"
        const val DANCE_HITS_PLAYLIST = "37i9dQZF1DX4dyzvuaRJ0n"
        const val ROCK_CLASSICS_PLAYLIST = "37i9dQZF1DWXRqgorJj26U"
    }
}
