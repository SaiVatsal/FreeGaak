package com.saivatsal.soundorbit.core.source.audius

import com.saivatsal.soundorbit.core.common.Dispatcher
import com.saivatsal.soundorbit.core.common.RateLimiter
import com.saivatsal.soundorbit.core.common.SoundOrbitDispatchers
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.source.*
import com.saivatsal.soundorbit.core.source.audius.model.AudiusPlaylistDto
import com.saivatsal.soundorbit.core.source.audius.model.AudiusResponse
import com.saivatsal.soundorbit.core.source.audius.model.AudiusTrackDto
import com.saivatsal.soundorbit.core.source.audius.model.AudiusUserDto
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
class AudiusSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val hostSelector: AudiusHostSelector,
    @Dispatcher(SoundOrbitDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : MusicSource {

    override val id: SourceId = SourceId.AUDIUS
    override val displayName: String = "Audius"

    override val capabilities: SourceCapabilities = SourceCapabilities(
        canSearch = true,
        hasTrending = true,
        hasGenres = true,
        hasTags = true,
        hasLanguageFilter = false,
        hasPlaylists = true,
        hasArtistBio = true,
        supportsSelectableQuality = false,
        allowsDownloads = true,
        ownsAudioPipeline = false
    )

    // Audius allows ~100 req/min; SoundOrbit conservatively limits to 40 req/min (1500ms between requests)
    private val rateLimiter = RateLimiter(minIntervalMs = 1500L)

    private val trackListType = Types.newParameterizedType(
        AudiusResponse::class.java,
        Types.newParameterizedType(List::class.java, AudiusTrackDto::class.java)
    )
    private val singleTrackType = Types.newParameterizedType(
        AudiusResponse::class.java,
        AudiusTrackDto::class.java
    )
    private val userListType = Types.newParameterizedType(
        AudiusResponse::class.java,
        Types.newParameterizedType(List::class.java, AudiusUserDto::class.java)
    )
    private val singleUserType = Types.newParameterizedType(
        AudiusResponse::class.java,
        AudiusUserDto::class.java
    )
    private val playlistListType = Types.newParameterizedType(
        AudiusResponse::class.java,
        Types.newParameterizedType(List::class.java, AudiusPlaylistDto::class.java)
    )
    private val singlePlaylistType = Types.newParameterizedType(
        AudiusResponse::class.java,
        AudiusPlaylistDto::class.java
    )

    private val trackListAdapter = moshi.adapter<AudiusResponse<List<AudiusTrackDto>>>(trackListType)
    private val singleTrackAdapter = moshi.adapter<AudiusResponse<AudiusTrackDto>>(singleTrackType)
    private val userListAdapter = moshi.adapter<AudiusResponse<List<AudiusUserDto>>>(userListType)
    private val singleUserAdapter = moshi.adapter<AudiusResponse<AudiusUserDto>>(singleUserType)
    private val playlistListAdapter = moshi.adapter<AudiusResponse<List<AudiusPlaylistDto>>>(playlistListType)
    private val singlePlaylistAdapter = moshi.adapter<AudiusResponse<AudiusPlaylistDto>>(singlePlaylistType)

    override suspend fun trending(
        genre: String?,
        window: TrendingWindow,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        val host = hostSelector.getHealthyHost()
        val urlBuilder = "$host/v1/tracks/trending".toHttpUrlOrNull()?.newBuilder()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid host $host")))

        urlBuilder.addQueryParameter("app_name", APP_NAME)
        urlBuilder.addQueryParameter("limit", page.limit.toString())
        urlBuilder.addQueryParameter("offset", page.offset.toString())
        urlBuilder.addQueryParameter("time", mapTrendingWindow(window))
        if (!genre.isNullOrBlank()) {
            urlBuilder.addQueryParameter("genre", genre)
        }

        executeGet(urlBuilder.build().toString()) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            val tracks = dtos.mapNotNull { it.toDomainTrack() }
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
        val host = hostSelector.getHealthyHost()

        // 1. Search tracks
        val tracksUrl = "$host/v1/tracks/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("app_name", APP_NAME)
            ?.addQueryParameter("query", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
            ?.build()
            ?.toString()
            ?: return@withContext SourceResult.Failure(SourceError.Unknown(IllegalArgumentException("Invalid URL")))

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            val tracks = dtos.mapNotNull { it.toDomainTrack() }
            val nextOffset = if (tracks.size >= page.limit) page.offset + tracks.size else null
            Page(tracks, nextOffset)
        }

        // 2. Search users (artists)
        val usersUrl = "$host/v1/users/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("app_name", APP_NAME)
            ?.addQueryParameter("query", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
            ?.build()
            ?.toString()

        val artistsResult = if (usersUrl != null) {
            executeGet(usersUrl) { body ->
                val response = userListAdapter.fromJson(body)
                val dtos = response?.data ?: emptyList()
                val artists = dtos.map { it.toDomainArtist() }
                val nextOffset = if (artists.size >= page.limit) page.offset + artists.size else null
                Page(artists, nextOffset)
            }
        } else {
            SourceResult.Success(Page(emptyList(), null))
        }

        // 3. Search playlists
        val playlistsUrl = "$host/v1/playlists/search".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("app_name", APP_NAME)
            ?.addQueryParameter("query", query)
            ?.addQueryParameter("limit", page.limit.toString())
            ?.addQueryParameter("offset", page.offset.toString())
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

        val tracksPage = (tracksResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)
        val artistsPage = (artistsResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)
        val playlistsPage = (playlistsResult as? SourceResult.Success)?.value ?: Page(emptyList(), null)

        SourceResult.Success(
            SearchResults(
                tracks = tracksPage,
                artists = artistsPage,
                albums = Page(emptyList(), null), // Audius models albums as playlists
                playlists = playlistsPage
            )
        )
    }

    override suspend fun browse(
        kind: BrowseKind,
        value: String,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(ioDispatcher) {
        when (kind) {
            BrowseKind.GENRE -> trending(genre = value, window = TrendingWindow.WEEK, page = page)
            BrowseKind.TAG -> search(query = value, filter = SearchFilter(), page = page).let { result ->
                when (result) {
                    is SourceResult.Success -> SourceResult.Success(result.value.tracks)
                    is SourceResult.Failure -> SourceResult.Failure(result.error)
                }
            }
            BrowseKind.MOOD -> search(query = value, filter = SearchFilter(), page = page).let { result ->
                when (result) {
                    is SourceResult.Success -> SourceResult.Success(result.value.tracks)
                    is SourceResult.Failure -> SourceResult.Failure(result.error)
                }
            }
            else -> SourceResult.Success(Page(emptyList(), null))
        }
    }

    override suspend fun track(id: String): SourceResult<Track> = withContext(ioDispatcher) {
        val host = hostSelector.getHealthyHost()
        val url = "$host/v1/tracks/$id?app_name=$APP_NAME"
        executeGet(url) { body ->
            val response = singleTrackAdapter.fromJson(body)
            val trackDto = response?.data ?: throw IOException("Track $id not found")
            trackDto.toDomainTrack() ?: throw IOException("Failed to parse track $id")
        }
    }

    override suspend fun artist(id: String): SourceResult<ArtistDetails> = withContext(ioDispatcher) {
        val host = hostSelector.getHealthyHost()
        val userUrl = "$host/v1/users/$id?app_name=$APP_NAME"
        val tracksUrl = "$host/v1/users/$id/tracks?app_name=$APP_NAME&limit=20"

        val userResult = executeGet(userUrl) { body ->
            val response = singleUserAdapter.fromJson(body)
            val userDto = response?.data ?: throw IOException("User $id not found")
            userDto.toDomainArtist()
        }

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            dtos.mapNotNull { it.toDomainTrack() }
        }

        when (userResult) {
            is SourceResult.Success -> {
                val topTracks = (tracksResult as? SourceResult.Success)?.value ?: emptyList()
                SourceResult.Success(
                    ArtistDetails(
                        artist = userResult.value,
                        topTracks = topTracks,
                        albums = emptyList(),
                        bio = null
                    )
                )
            }
            is SourceResult.Failure -> SourceResult.Failure(userResult.error)
        }
    }

    override suspend fun album(id: String): SourceResult<AlbumDetails> = withContext(ioDispatcher) {
        // Audius treats albums under playlist endpoints
        val host = hostSelector.getHealthyHost()
        val playlistUrl = "$host/v1/playlists/$id?app_name=$APP_NAME"
        val tracksUrl = "$host/v1/playlists/$id/tracks?app_name=$APP_NAME"

        val playlistResult = executeGet(playlistUrl) { body ->
            val response = singlePlaylistAdapter.fromJson(body)
            val dto = response?.data ?: throw IOException("Playlist/Album $id not found")
            Album(
                sourceId = SourceId.AUDIUS,
                sourceAlbumId = dto.id,
                name = dto.playlistName,
                artistName = dto.user?.name ?: "Unknown Artist",
                artworkUrl = dto.artwork?.medium ?: dto.artwork?.large,
                trackCount = dto.trackCount
            )
        }

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            dtos.mapNotNull { it.toDomainTrack() }
        }

        when (playlistResult) {
            is SourceResult.Success -> {
                val tracks = (tracksResult as? SourceResult.Success)?.value ?: emptyList()
                SourceResult.Success(
                    AlbumDetails(
                        album = playlistResult.value,
                        tracks = tracks
                    )
                )
            }
            is SourceResult.Failure -> SourceResult.Failure(playlistResult.error)
        }
    }

    override suspend fun playlist(id: String): SourceResult<PlaylistDetails> = withContext(ioDispatcher) {
        val host = hostSelector.getHealthyHost()
        val playlistUrl = "$host/v1/playlists/$id?app_name=$APP_NAME"
        val tracksUrl = "$host/v1/playlists/$id/tracks?app_name=$APP_NAME"

        val playlistResult = executeGet(playlistUrl) { body ->
            val response = singlePlaylistAdapter.fromJson(body)
            val dto = response?.data ?: throw IOException("Playlist $id not found")
            dto.toDomainPlaylist()
        }

        val tracksResult = executeGet(tracksUrl) { body ->
            val response = trackListAdapter.fromJson(body)
            val dtos = response?.data ?: emptyList()
            dtos.mapNotNull { it.toDomainTrack() }
        }

        when (playlistResult) {
            is SourceResult.Success -> {
                val tracks = (tracksResult as? SourceResult.Success)?.value ?: emptyList()
                SourceResult.Success(
                    PlaylistDetails(
                        playlist = playlistResult.value,
                        tracks = tracks
                    )
                )
            }
            is SourceResult.Failure -> SourceResult.Failure(playlistResult.error)
        }
    }

    override suspend fun resolveStream(trackId: String, quality: AudioQuality): SourceResult<ResolvedStream> =
        withContext(ioDispatcher) {
            val host = hostSelector.getHealthyHost()
            val streamUrl = "$host/v1/tracks/$trackId/stream?app_name=$APP_NAME"
            SourceResult.Success(
                ResolvedStream(
                    uri = streamUrl,
                    mimeType = "audio/mpeg"
                )
            )
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

    private fun AudiusTrackDto.toDomainTrack(): Track? {
        if (isStreamGated) return null // Skip gated tracks according to Rule 2 & 4
        val artist = user?.name ?: "Unknown Artist"
        val artUrl = artwork?.medium ?: artwork?.large ?: artwork?.small
        return Track(
            sourceId = SourceId.AUDIUS,
            sourceTrackId = id,
            title = title,
            titleSortKey = title.lowercase().trim(),
            artistName = artist,
            albumName = null,
            durationMs = duration * 1000L,
            artworkUrl = artUrl,
            licenseName = "All Rights Reserved",
            licenseUrl = null,
            downloadAllowed = downloadable && !isDownloadGated
        )
    }

    private fun AudiusUserDto.toDomainArtist(): Artist {
        val artUrl = profilePicture?.medium ?: profilePicture?.large ?: profilePicture?.small
        return Artist(
            sourceId = SourceId.AUDIUS,
            sourceArtistId = id,
            name = name,
            imageUrl = artUrl
        )
    }

    private fun AudiusPlaylistDto.toDomainPlaylist(): Playlist {
        val artUrl = artwork?.medium ?: artwork?.large ?: artwork?.small
        return Playlist(
            sourceId = SourceId.AUDIUS,
            sourcePlaylistId = id,
            name = playlistName,
            description = description,
            artworkUrl = artUrl,
            trackCount = trackCount
        )
    }

    private fun mapTrendingWindow(window: TrendingWindow): String = when (window) {
        TrendingWindow.WEEK -> "week"
        TrendingWindow.MONTH -> "month"
        TrendingWindow.ALL_TIME -> "allTime"
    }

    companion object {
        private const val APP_NAME = "SoundOrbit"
    }
}
