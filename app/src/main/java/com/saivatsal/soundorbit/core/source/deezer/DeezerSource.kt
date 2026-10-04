package com.saivatsal.soundorbit.core.source.deezer

import com.saivatsal.soundorbit.BuildConfig
import com.saivatsal.soundorbit.core.common.Dispatcher
import com.saivatsal.soundorbit.core.common.RateLimiter
import com.saivatsal.soundorbit.core.common.SoundOrbitDispatchers
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.source.*
import com.saivatsal.soundorbit.core.source.deezer.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeezerSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    @Dispatcher(SoundOrbitDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MusicSource {

    override val id: SourceId = SourceId.DEEZER
    override val displayName: String = "Deezer"

    override val capabilities: SourceCapabilities = SourceCapabilities(
        canSearch = true,
        hasTrending = true,
        hasGenres = true,
        hasTags = false,
        hasLanguageFilter = false,
        hasPlaylists = true,
        hasArtistBio = false,
        supportsSelectableQuality = false,
        allowsDownloads = false,
        ownsAudioPipeline = false
    )

    private val rateLimiter = RateLimiter(minIntervalMs = 300L)

    private val trackListAdapter = moshi.adapter<DeezerDataList<DeezerTrackDto>>(
        Types.newParameterizedType(DeezerDataList::class.java, DeezerTrackDto::class.java)
    )
    private val artistListAdapter = moshi.adapter<DeezerDataList<DeezerArtistDto>>(
        Types.newParameterizedType(DeezerDataList::class.java, DeezerArtistDto::class.java)
    )
    private val albumListAdapter = moshi.adapter<DeezerDataList<DeezerAlbumDto>>(
        Types.newParameterizedType(DeezerDataList::class.java, DeezerAlbumDto::class.java)
    )
    private val playlistListAdapter = moshi.adapter<DeezerDataList<DeezerPlaylistDto>>(
        Types.newParameterizedType(DeezerDataList::class.java, DeezerPlaylistDto::class.java)
    )
    private val trackAdapter = moshi.adapter(DeezerTrackDto::class.java)
    private val artistAdapter = moshi.adapter(DeezerArtistDto::class.java)
    private val albumAdapter = moshi.adapter(DeezerAlbumDto::class.java)
    private val playlistAdapter = moshi.adapter(DeezerPlaylistDto::class.java)
    private val chartAdapter = moshi.adapter(DeezerChartDto::class.java)

    var baseUrl: String = BASE_URL

    var apiKeys: List<String> = parseConfiguredKeys()

    private val currentKeyIndex = AtomicInteger(0)

    fun getCurrentApiKey(): String {
        val keys = apiKeys
        if (keys.isEmpty()) return ""
        val index = Math.floorMod(currentKeyIndex.get(), keys.size)
        return keys[index]
    }

    fun rotateApiKey(): Boolean {
        val keys = apiKeys
        if (keys.size <= 1) return false
        currentKeyIndex.incrementAndGet()
        return true
    }

    private fun parseConfiguredKeys(): List<String> {
        val keysString = try {
            BuildConfig.RAPIDAPI_DEEZER_KEYS
        } catch (_: Exception) {
            ""
        }
        val parsed = keysString.split(',', ';', ' ', '\n', '\t')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (parsed.isNotEmpty()) {
            parsed
        } else {
            val fallbackKey = try {
                BuildConfig.RAPIDAPI_DEEZER_KEY
            } catch (_: Exception) {
                ""
            }
            if (fallbackKey.isNotBlank()) listOf(fallbackKey.trim()) else emptyList()
        }
    }

    override suspend fun trending(
        genre: String?,
        window: TrendingWindow,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val cleanGenre = genre?.trim()?.lowercase()
        val genreId = when (cleanGenre) {
            "pop" -> 132
            "hip-hop", "hiphop", "rap" -> 116
            "dance", "electronic", "edm" -> 113
            "rock" -> 152
            "r&b", "rnb", "soul" -> 165
            "jazz" -> 129
            "alternative" -> 85
            "reggae" -> 144
            null, "", "all" -> 0
            else -> null
        }

        if (genreId != null) {
            val urlBuilder = "$baseUrl/chart/$genreId/tracks".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

            urlBuilder.addQueryParameter("index", page.offset.toString())
            urlBuilder.addQueryParameter("limit", page.limit.toString())

            val result = executeGet(urlBuilder.build().toString()) { body ->
                val response = trackListAdapter.fromJson(body)
                val dtos = response?.data ?: emptyList()
                val tracks = dtos.map { it.toDomainTrack() }
                val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
                Page(tracks, nextOffset)
            }

            if (result is SourceResult.Success && result.value.items.isNotEmpty()) {
                return@withContext result
            }
            if (result is SourceResult.Failure) {
                return@withContext result
            }

            if (genreId == 0) {
                // Fallback to /chart root if chart/0/tracks is empty
                return@withContext executeGet("$baseUrl/chart") { body ->
                    val chart = chartAdapter.fromJson(body)
                    val dtos = chart?.tracks?.data ?: emptyList()
                    val tracks = dtos.map { it.toDomainTrack() }
                    Page(tracks, null)
                }
            }
        }

        // For named regional queries and multi-language charts
        val searchQuery = when (cleanGenre) {
            "bollywood", "hindi" -> "Bollywood Top Hits"
            "india", "punjabi" -> "Punjabi Top Hits"
            "telugu" -> "Telugu Top Hits"
            "tamil" -> "Tamil Top Hits"
            "korean", "kpop", "k-pop" -> "K-Pop Top Hits"
            "kannada" -> "Kannada Top Hits"
            "malayalam" -> "Malayalam Top Hits"
            "bengali" -> "Bengali Top Hits"
            "marathi" -> "Marathi Top Hits"
            "english", "global" -> "Global Top Hits"
            "latin" -> "Latin Hits"
            else -> genre ?: "Top Hits"
        }

        val searchUrl = "$baseUrl/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", searchQuery)
            ?.addQueryParameter("index", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?.build()
            ?.toString()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        executeGet(searchUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            val tracks = dtos.map { it.toDomainTrack() }
            val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
            Page(tracks, nextOffset)
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

        val tracksUrl = "$baseUrl/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("index", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?.build()
            ?.toString()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            val tracks = dtos.map { it.toDomainTrack() }
            val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
            Page(tracks, nextOffset)
        }

        val artistsUrl = "$baseUrl/search/artist".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("index", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?.build()
            ?.toString()

        val artistsResult = if (artistsUrl != null) {
            executeGet(artistsUrl) { body ->
                val response = artistListAdapter.fromJson(body)
                val dtos = response?.data ?: emptyList()
                val artists = dtos.map { it.toDomainArtist() }
                val nextOffset = if (artists.size >= page.limit) page.offset + artists.size else null
                Page(artists, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        val albumsUrl = "$baseUrl/search/album".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("index", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?.build()
            ?.toString()

        val albumsResult = if (albumsUrl != null) {
            executeGet(albumsUrl) { body ->
                val response = albumListAdapter.fromJson(body)
                val dtos = response?.data ?: emptyList()
                val albums = dtos.map { it.toDomainAlbum() }
                val nextOffset = if (albums.size >= page.limit) page.offset + albums.size else null
                Page(albums, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        val playlistsUrl = "$baseUrl/search/playlist".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("index", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?.build()
            ?.toString()

        val playlistsResult = if (playlistsUrl != null) {
            executeGet(playlistsUrl) { body ->
                val response = playlistListAdapter.fromJson(body)
                val dtos = response?.data ?: emptyList()
                val playlists = dtos.map { it.toDomainPlaylist() }
                val nextOffset = if (playlists.size >= page.limit) page.offset + playlists.size else null
                Page(playlists, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        SourceResult.Success(
            SearchResults(
                tracks = tracksResult.getOrNull() ?: Page(emptyList(), null),
                artists = artistsResult.getOrNull() ?: Page(emptyList(), null),
                albums = albumsResult.getOrNull() ?: Page(emptyList(), null),
                playlists = playlistsResult.getOrNull() ?: Page(emptyList(), null)
            )
        )
    }

    override suspend fun browse(
        kind: BrowseKind,
        value: String,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val url = "$baseUrl/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", value)
            ?.addQueryParameter("index", page.offset.toString())
            ?.addQueryParameter("limit", page.limit.toString())
            ?.build()
            ?.toString()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        executeGet(url) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            val tracks = dtos.map { it.toDomainTrack() }
            val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
            Page(tracks, nextOffset)
        }
    }

    override suspend fun track(id: String): SourceResult<Track> = withContext(ioDispatcher) {
        val url = "$baseUrl/track/$id"
        executeGet(url) { body ->
            val dto = trackAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Deezer track")
            dto.toDomainTrack()
        }
    }

    override suspend fun artist(id: String): SourceResult<ArtistDetails> = withContext(ioDispatcher) {
        val artistUrl = "$baseUrl/artist/$id"
        val topTracksUrl = "$baseUrl/artist/$id/top?limit=20"
        val albumsUrl = "$baseUrl/artist/$id/albums?limit=20"

        val artistResult = executeGet(artistUrl) { body ->
            artistAdapter.fromJson(body)?.toDomainArtist()
                ?: throw IllegalArgumentException("Failed to parse Deezer artist")
        }

        val artist = when (artistResult) {
            is SourceResult.Success -> artistResult.value
            is SourceResult.Failure -> return@withContext artistResult
        }

        val topTracksResult = executeGet(topTracksUrl) { body ->
            trackListAdapter.fromJson(body)?.data?.map { it.toDomainTrack() } ?: emptyList()
        }

        val albumsResult = executeGet(albumsUrl) { body ->
            albumListAdapter.fromJson(body)?.data?.map { it.toDomainAlbum() } ?: emptyList()
        }

        SourceResult.Success(
            ArtistDetails(
                artist = artist,
                bio = null,
                topTracks = topTracksResult.getOrNull() ?: emptyList(),
                albums = albumsResult.getOrNull() ?: emptyList()
            )
        )
    }

    override suspend fun album(id: String): SourceResult<AlbumDetails> = withContext(ioDispatcher) {
        val url = "$baseUrl/album/$id"
        executeGet(url) { body ->
            val dto = albumAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Deezer album")
            val tracks = dto.tracks?.data?.map { it.toDomainTrack() } ?: emptyList()
            AlbumDetails(
                album = dto.toDomainAlbum(),
                tracks = tracks
            )
        }
    }

    override suspend fun playlist(id: String): SourceResult<PlaylistDetails> = withContext(ioDispatcher) {
        val url = "$baseUrl/playlist/$id"
        executeGet(url) { body ->
            val dto = playlistAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Deezer playlist")
            val tracks = dto.tracks?.data?.map { it.toDomainTrack() } ?: emptyList()
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
        val url = "$baseUrl/track/$trackId"
        executeGet(url) { body ->
            val dto = trackAdapter.fromJson(body)
                ?: throw IllegalArgumentException("Failed to parse Deezer track")
            val previewUrl = dto.preview
            if (previewUrl.isNullOrBlank()) {
                throw IllegalStateException("Deezer track does not have a preview URL")
            }
            ResolvedStream(
                uri = previewUrl,
                mimeType = "audio/mpeg"
            )
        }
    }

    suspend fun findMatchingStream(
        title: String,
        artistName: String
    ): ResolvedStream? = withContext(ioDispatcher) {
        val cleanTitle = title.replace(Regex("(?i)\\s*\\(feat\\..*?\\)|\\s*\\[feat\\..*?\\]|\\s*\\(with.*?\\)"), "").trim()
        val query = "$cleanTitle $artistName".trim()
        if (query.isBlank()) return@withContext null

        val searchUrl = "$baseUrl/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("limit", "5")
            ?.build()
            ?.toString() ?: return@withContext null

        val result = executeGet(searchUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            response?.data ?: emptyList()
        }

        if (result is SourceResult.Success) {
            val candidate = result.value.firstOrNull { !it.preview.isNullOrBlank() }
            if (candidate?.preview != null) {
                return@withContext ResolvedStream(
                    uri = candidate.preview,
                    mimeType = "audio/mpeg"
                )
            }
        }
        null
    }

    private suspend fun <T> executeGet(
        url: String,
        parse: (String) -> T
    ): SourceResult<T> = withContext(ioDispatcher) {
        val keys = apiKeys
        val maxAttempts = if (keys.isNotEmpty()) keys.size else 1
        var lastRateLimitMs: Long? = null

        for (attempt in 0 until maxAttempts) {
            val key = getCurrentApiKey()
            val result = executeSingleGet(url, key, parse)
            when (result) {
                is SourceResult.Success -> return@withContext result
                is SourceResult.Failure -> {
                    when (val err = result.error) {
                        is SourceError.RateLimited -> {
                            lastRateLimitMs = err.retryAfterMs
                            if (attempt < maxAttempts - 1 && rotateApiKey()) {
                                continue
                            }
                            return@withContext result
                        }
                        is SourceError.MissingOrInvalidKey -> {
                            if (attempt < maxAttempts - 1 && rotateApiKey()) {
                                continue
                            }
                            return@withContext result
                        }
                        else -> return@withContext result
                    }
                }
            }
        }
        SourceResult.Failure(SourceError.RateLimited(lastRateLimitMs))
    }

    private suspend fun <T> executeSingleGet(
        url: String,
        key: String,
        parse: (String) -> T
    ): SourceResult<T> {
        return try {
            rateLimiter.execute {
                val request = Request.Builder()
                    .url(url)
                    .header("x-rapidapi-host", "deezerdevs-deezer.p.rapidapi.com")
                    .apply {
                        if (key.isNotBlank()) {
                            header("x-rapidapi-key", key)
                        }
                    }
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
                        401, 403 -> SourceResult.Failure(SourceError.MissingOrInvalidKey)
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
        const val BASE_URL = "https://deezerdevs-deezer.p.rapidapi.com"
    }
}
