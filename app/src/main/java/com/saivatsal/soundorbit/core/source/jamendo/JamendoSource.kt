package com.saivatsal.soundorbit.core.source.jamendo

import com.saivatsal.soundorbit.BuildConfig
import com.saivatsal.soundorbit.core.common.Dispatcher
import com.saivatsal.soundorbit.core.common.RateLimiter
import com.saivatsal.soundorbit.core.common.SoundOrbitDispatchers
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.source.*
import com.saivatsal.soundorbit.core.source.jamendo.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JamendoSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    @Dispatcher(SoundOrbitDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MusicSource {

    override val id: SourceId = SourceId.JAMENDO
    override val displayName: String = "Jamendo (Creative Commons)"

    override val capabilities: SourceCapabilities = SourceCapabilities(
        canSearch = true,
        hasTrending = true,
        hasGenres = true,
        hasTags = true,
        hasLanguageFilter = true,
        hasPlaylists = true,
        hasArtistBio = false,
        supportsSelectableQuality = true,
        allowsDownloads = true,
        ownsAudioPipeline = false
    )

    // Jamendo allows 120 req/min; SoundOrbit uses 500ms min interval (120 req/min ceiling)
    private val rateLimiter = RateLimiter(minIntervalMs = 500L)

    private val trackListType = Types.newParameterizedType(
        JamendoResponse::class.java,
        Types.newParameterizedType(List::class.java, JamendoTrackDto::class.java)
    )
    private val artistListType = Types.newParameterizedType(
        JamendoResponse::class.java,
        Types.newParameterizedType(List::class.java, JamendoArtistDto::class.java)
    )
    private val albumListType = Types.newParameterizedType(
        JamendoResponse::class.java,
        Types.newParameterizedType(List::class.java, JamendoAlbumDto::class.java)
    )
    private val playlistListType = Types.newParameterizedType(
        JamendoResponse::class.java,
        Types.newParameterizedType(List::class.java, JamendoPlaylistDto::class.java)
    )

    private val trackListAdapter = moshi.adapter<JamendoResponse<List<JamendoTrackDto>>>(trackListType)
    private val artistListAdapter = moshi.adapter<JamendoResponse<List<JamendoArtistDto>>>(artistListType)
    private val albumListAdapter = moshi.adapter<JamendoResponse<List<JamendoAlbumDto>>>(albumListType)
    private val playlistListAdapter = moshi.adapter<JamendoResponse<List<JamendoPlaylistDto>>>(playlistListType)

    var baseUrl: String = BASE_URL

    private val clientId: String
        get() = BuildConfig.JAMENDO_CLIENT_ID.ifBlank { DEFAULT_CLIENT_ID }

    override suspend fun trending(
        genre: String?,
        window: TrendingWindow,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val urlBuilder = "$baseUrl/tracks/".toHttpUrlOrNull()?.newBuilder()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        urlBuilder.addQueryParameter("client_id", clientId)
        urlBuilder.addQueryParameter("format", "json")
        urlBuilder.addQueryParameter("limit", page.limit.toString())
        urlBuilder.addQueryParameter("offset", page.offset.toString())
        urlBuilder.addQueryParameter("order", mapTrendingOrder(window))
        urlBuilder.addQueryParameter("include", "musicinfo")

        if (!genre.isNullOrBlank()) {
            urlBuilder.addQueryParameter("fuzzytags", genre)
        }

        executeGet(urlBuilder.build().toString()) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.results ?: emptyList()
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

        val tracksUrl = "$baseUrl/tracks/".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("client_id", clientId)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("namesearch", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
            ?.apply {
                if (!filter.language.isNullOrBlank()) {
                    addQueryParameter("lang", filter.language)
                }
            }
            ?.build()
            ?.toString()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.results ?: emptyList()
            val tracks = dtos.map { it.toDomainTrack() }
            val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
            Page(tracks, nextOffset)
        }

        val artistsUrl = "$baseUrl/artists/".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("client_id", clientId)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("namesearch", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
            ?.build()
            ?.toString()

        val artistsResult = if (artistsUrl != null) {
            executeGet(artistsUrl) { body ->
                val response = artistListAdapter.fromJson(body)
                val dtos = response?.results ?: emptyList()
                val artists = dtos.map { it.toDomainArtist() }
                val nextOffset = if (artists.size >= page.limit) page.offset + artists.size else null
                Page(artists, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        val albumsUrl = "$baseUrl/albums/".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("client_id", clientId)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("namesearch", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
            ?.build()
            ?.toString()

        val albumsResult = if (albumsUrl != null) {
            executeGet(albumsUrl) { body ->
                val response = albumListAdapter.fromJson(body)
                val dtos = response?.results ?: emptyList()
                val albums = dtos.map { it.toDomainAlbum() }
                val nextOffset = if (albums.size >= page.limit) page.offset + albums.size else null
                Page(albums, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        val playlistsUrl = "$baseUrl/playlists/".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("client_id", clientId)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("namesearch", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
            ?.build()
            ?.toString()

        val playlistsResult = if (playlistsUrl != null) {
            executeGet(playlistsUrl) { body ->
                val response = playlistListAdapter.fromJson(body)
                val dtos = response?.results ?: emptyList()
                val playlists = dtos.map { it.toDomainPlaylist() }
                val nextOffset = if (playlists.size >= page.limit) page.offset + playlists.size else null
                Page(playlists, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        val tracksPage = (tracksResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)
        val artistsPage = (artistsResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)
        val albumsPage = (albumsResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)
        val playlistsPage = (playlistsResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)

        SourceResult.Success(
            SearchResults(
                tracks = tracksPage,
                artists = artistsPage,
                albums = albumsPage,
                playlists = playlistsPage
            )
        )
    }

    override suspend fun browse(
        kind: BrowseKind,
        value: String,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val urlBuilder = "$baseUrl/tracks/".toHttpUrlOrNull()?.newBuilder()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        urlBuilder.addQueryParameter("client_id", clientId)
        urlBuilder.addQueryParameter("format", "json")
        urlBuilder.addQueryParameter("limit", page.limit.toString())
        urlBuilder.addQueryParameter("offset", page.offset.toString())
        urlBuilder.addQueryParameter("order", "popularity_total")

        when (kind) {
            BrowseKind.GENRE -> urlBuilder.addQueryParameter("fuzzytags", value)
            BrowseKind.TAG -> urlBuilder.addQueryParameter("tags", value)
            BrowseKind.MOOD -> urlBuilder.addQueryParameter("fuzzytags", value)
            BrowseKind.LANGUAGE -> urlBuilder.addQueryParameter("lang", value)
        }

        executeGet(urlBuilder.build().toString()) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.results ?: emptyList()
            val tracks = dtos.map { it.toDomainTrack() }
            val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
            Page(tracks, nextOffset)
        }
    }

    override suspend fun track(id: String): SourceResult<Track> = withContext(ioDispatcher) {
        val url = "$baseUrl/tracks/?client_id=$clientId&format=json&id=$id"
        executeGet(url) { body ->
            val response = trackListAdapter.fromJson(body)
            val trackDto = response?.results?.firstOrNull() ?: throw IOException("Track $id not found")
            trackDto.toDomainTrack()
        }
    }

    override suspend fun artist(id: String): SourceResult<ArtistDetails> = withContext(ioDispatcher) {
        val artistUrl = "$baseUrl/artists/?client_id=$clientId&format=json&id=$id"
        val tracksUrl = "$baseUrl/tracks/?client_id=$clientId&format=json&artist_id=$id&limit=20&order=popularity_total"

        val artistResult = executeGet(artistUrl) { body ->
            val response = artistListAdapter.fromJson(body)
            val artistDto = response?.results?.firstOrNull() ?: throw IOException("Artist $id not found")
            artistDto.toDomainArtist()
        }

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.results ?: emptyList()
            dtos.map { it.toDomainTrack() }
        }

        when (artistResult) {
            is SourceResult.Success -> {
                val tracks = (tracksResult as? SourceResult.Success)?.value ?: emptyList()
                SourceResult.Success(
                    ArtistDetails(
                        artist = artistResult.value,
                        topTracks = tracks,
                        albums = emptyList(),
                        bio = null
                    )
                )
            }
            is SourceResult.Failure -> SourceResult.Failure(artistResult.error)
        }
    }

    override suspend fun album(id: String): SourceResult<AlbumDetails> = withContext(ioDispatcher) {
        val albumUrl = "$baseUrl/albums/?client_id=$clientId&format=json&id=$id"
        val tracksUrl = "$baseUrl/tracks/?client_id=$clientId&format=json&album_id=$id"

        val albumResult = executeGet(albumUrl) { body ->
            val response = albumListAdapter.fromJson(body)
            val albumDto = response?.results?.firstOrNull() ?: throw IOException("Album $id not found")
            albumDto.toDomainAlbum()
        }

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.results ?: emptyList()
            dtos.map { it.toDomainTrack() }
        }

        when (albumResult) {
            is SourceResult.Success -> {
                val tracks = (tracksResult as? SourceResult.Success)?.value ?: emptyList()
                SourceResult.Success(
                    AlbumDetails(
                        album = albumResult.value,
                        tracks = tracks
                    )
                )
            }
            is SourceResult.Failure -> SourceResult.Failure(albumResult.error)
        }
    }

    override suspend fun playlist(id: String): SourceResult<PlaylistDetails> = withContext(ioDispatcher) {
        val playlistUrl = "$baseUrl/playlists/tracks/?client_id=$clientId&format=json&id=$id"
        executeGet(playlistUrl) { body ->
            val response = playlistListAdapter.fromJson(body)
            val dto = response?.results?.firstOrNull() ?: throw IOException("Playlist $id not found")
            val tracks = dto.tracks?.map { it.toDomainTrack() } ?: emptyList()
            PlaylistDetails(
                playlist = dto.toDomainPlaylist(),
                tracks = tracks
            )
        }
    }

    override suspend fun resolveStream(trackId: String, quality: AudioQuality): SourceResult<ResolvedStream> =
        withContext(ioDispatcher) {
            val trackResult = track(trackId)
            when (trackResult) {
                is SourceResult.Success -> {
                    val url = "$baseUrl/tracks/?client_id=$clientId&format=json&id=$trackId"
                    executeGet(url) { body ->
                        val response = trackListAdapter.fromJson(body)
                        val trackDto = response?.results?.firstOrNull() ?: throw IOException("Track $trackId not found")
                        val streamUri = when (quality) {
                            AudioQuality.HIGH -> trackDto.audioDl ?: trackDto.audio ?: throw IOException("No audio URI")
                            else -> trackDto.audio ?: trackDto.audioDl ?: throw IOException("No audio URI")
                        }
                        ResolvedStream(
                            uri = streamUri,
                            mimeType = "audio/mpeg"
                        )
                    }
                }
                is SourceResult.Failure -> SourceResult.Failure(trackResult.error)
            }
        }

    private suspend fun <T> executeGet(url: String, parser: (String) -> T): SourceResult<T> {
        return try {
            rateLimiter.execute {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> {
                            val body = response.body?.string() ?: throw IOException("Empty response body")
                            SourceResult.Success(parser(body))
                        }
                        response.code == 429 -> {
                            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000)
                            SourceResult.Failure(SourceError.RateLimited(retryAfter))
                        }
                        response.code == 404 -> SourceResult.Failure(SourceError.NotFound)
                        response.code == 403 || response.code == 401 -> SourceResult.Failure(SourceError.Gated)
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

    private fun JamendoTrackDto.toDomainTrack(): Track {
        val license = parseLicense(licenseCcUrl)
        return Track(
            sourceId = SourceId.JAMENDO,
            sourceTrackId = id,
            title = name,
            titleSortKey = name.lowercase().trim(),
            artistName = artistName.ifBlank { "Unknown Artist" },
            albumName = albumName,
            durationMs = duration * 1000L,
            artworkUrl = image ?: albumImage,
            licenseName = license.first,
            licenseUrl = license.second,
            downloadAllowed = true
        )
    }

    private fun JamendoArtistDto.toDomainArtist(): Artist {
        return Artist(
            sourceId = SourceId.JAMENDO,
            sourceArtistId = id,
            name = name,
            imageUrl = image
        )
    }

    private fun JamendoAlbumDto.toDomainAlbum(): Album {
        return Album(
            sourceId = SourceId.JAMENDO,
            sourceAlbumId = id,
            name = name,
            artistName = artistName.ifBlank { "Unknown Artist" },
            artworkUrl = image,
            trackCount = tracks?.size ?: 0
        )
    }

    private fun JamendoPlaylistDto.toDomainPlaylist(): Playlist {
        return Playlist(
            sourceId = SourceId.JAMENDO,
            sourcePlaylistId = id,
            name = name,
            description = "Created by ${userName ?: "Unknown"}",
            artworkUrl = null,
            trackCount = tracks?.size ?: 0
        )
    }

    private fun parseLicense(licenseCcUrl: String?): Pair<String, String?> {
        if (licenseCcUrl.isNullOrBlank()) return Pair("Creative Commons", null)
        val clean = licenseCcUrl.trim().lowercase()
        val name = when {
            clean.contains("by-nc-nd") -> "CC BY-NC-ND"
            clean.contains("by-nc-sa") -> "CC BY-NC-SA"
            clean.contains("by-nc") -> "CC BY-NC"
            clean.contains("by-nd") -> "CC BY-ND"
            clean.contains("by-sa") -> "CC BY-SA"
            clean.contains("/by/") || clean.contains("/by") -> "CC BY"
            clean.contains("zero") || clean.contains("publicdomain") || clean.contains("cc0") -> "CC0 Public Domain"
            else -> "Creative Commons"
        }
        return Pair(name, licenseCcUrl)
    }

    private fun mapTrendingOrder(window: TrendingWindow): String = when (window) {
        TrendingWindow.WEEK -> "popularity_week"
        TrendingWindow.MONTH -> "popularity_month"
        TrendingWindow.ALL_TIME -> "popularity_total"
    }

    companion object {
        private const val BASE_URL = "https://api.jamendo.com/v3.0"
        private const val DEFAULT_CLIENT_ID = "soundorbit_client"
    }
}
