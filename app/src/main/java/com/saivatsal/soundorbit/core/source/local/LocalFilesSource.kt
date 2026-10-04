package com.saivatsal.soundorbit.core.source.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.saivatsal.soundorbit.core.model.*
import com.saivatsal.soundorbit.core.source.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFilesSource @Inject constructor(
    @ApplicationContext private val context: Context
) : MusicSource {

    override val id: SourceId = SourceId.LOCAL
    override val displayName: String = "Local Files"
    override val capabilities: SourceCapabilities = SourceCapabilities(
        canSearch = true,
        hasTrending = false,
        hasGenres = true,
        hasTags = false,
        hasLanguageFilter = false,
        hasPlaylists = false,
        hasArtistBio = false,
        supportsSelectableQuality = false,
        allowsDownloads = false,
        ownsAudioPipeline = true
    )

    override suspend fun search(
        query: String,
        filter: SearchFilter,
        page: PageRequest
    ): SourceResult<SearchResults> = withContext(Dispatchers.IO) {
        try {
            val allTracks = queryMediaStore(
                selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ?",
                selectionArgs = arrayOf("%$query%", "%$query%", "%$query%"),
                limit = 200
            )

            val pagedTracks = allTracks.drop(page.offset).take(page.limit)
            val nextOffset = if (page.offset + page.limit < allTracks.size) page.offset + page.limit else null

            val artists = allTracks.map { it.artistName }
                .distinct()
                .map { name ->
                    Artist(
                        sourceId = SourceId.LOCAL,
                        sourceArtistId = name.hashCode().toString(),
                        name = name
                    )
                }

            val albums = allTracks.groupBy { it.albumName ?: "Unknown Album" }
                .map { (albumTitle, albumTracks) ->
                    val first = albumTracks.first()
                    Album(
                        sourceId = SourceId.LOCAL,
                        sourceAlbumId = albumTitle.hashCode().toString(),
                        name = albumTitle,
                        artistName = first.artistName,
                        artworkUrl = first.artworkUrl,
                        trackCount = albumTracks.size
                    )
                }

            val results = SearchResults(
                tracks = Page(pagedTracks, nextOffset),
                artists = Page(artists.take(page.limit), null),
                albums = Page(albums.take(page.limit), null),
                playlists = Page(emptyList(), null)
            )

            SourceResult.Success(results)
        } catch (e: Exception) {
            SourceResult.Failure(SourceError.Unknown(e))
        }
    }

    override suspend fun trending(
        genre: String?,
        window: TrendingWindow,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(Dispatchers.IO) {
        val tracks = queryMediaStore(limit = 100)
        val paged = tracks.drop(page.offset).take(page.limit)
        val nextOffset = if (page.offset + page.limit < tracks.size) page.offset + page.limit else null
        SourceResult.Success(Page(paged, nextOffset))
    }

    override suspend fun browse(
        kind: BrowseKind,
        value: String,
        page: PageRequest
    ): SourceResult<Page<Track>> = withContext(Dispatchers.IO) {
        val tracks = queryMediaStore(limit = 200)
        val paged = tracks.drop(page.offset).take(page.limit)
        val nextOffset = if (page.offset + page.limit < tracks.size) page.offset + page.limit else null
        SourceResult.Success(Page(paged, nextOffset))
    }

    override suspend fun track(id: String): SourceResult<Track> = withContext(Dispatchers.IO) {
        val rawId = id.removePrefix("local_")
        val tracks = queryMediaStore(
            selection = "${MediaStore.Audio.Media._ID} = ?",
            selectionArgs = arrayOf(rawId),
            limit = 1
        )
        val track = tracks.firstOrNull()
        if (track != null) {
            SourceResult.Success(track)
        } else {
            SourceResult.Failure(SourceError.NotFound)
        }
    }

    override suspend fun artist(id: String): SourceResult<ArtistDetails> = withContext(Dispatchers.IO) {
        val allTracks = queryMediaStore(limit = 500)
        val matchingTracks = allTracks.filter { it.artistName.hashCode().toString() == id }
        if (matchingTracks.isEmpty()) {
            return@withContext SourceResult.Failure(SourceError.NotFound)
        }

        val name = matchingTracks.first().artistName
        val artist = Artist(
            sourceId = SourceId.LOCAL,
            sourceArtistId = id,
            name = name
        )

        SourceResult.Success(
            ArtistDetails(
                artist = artist,
                bio = "Local library artist",
                topTracks = matchingTracks
            )
        )
    }

    override suspend fun album(id: String): SourceResult<AlbumDetails> = withContext(Dispatchers.IO) {
        val allTracks = queryMediaStore(limit = 500)
        val matchingTracks = allTracks.filter { (it.albumName ?: "Unknown Album").hashCode().toString() == id }
        if (matchingTracks.isEmpty()) {
            return@withContext SourceResult.Failure(SourceError.NotFound)
        }

        val first = matchingTracks.first()
        val album = Album(
            sourceId = SourceId.LOCAL,
            sourceAlbumId = id,
            name = first.albumName ?: "Unknown Album",
            artistName = first.artistName,
            artworkUrl = first.artworkUrl,
            trackCount = matchingTracks.size
        )

        SourceResult.Success(
            AlbumDetails(
                album = album,
                tracks = matchingTracks
            )
        )
    }

    override suspend fun playlist(id: String): SourceResult<PlaylistDetails> {
        return SourceResult.Failure(SourceError.NotFound)
    }

    override suspend fun resolveStream(
        trackId: String,
        quality: AudioQuality
    ): SourceResult<ResolvedStream> = withContext(Dispatchers.IO) {
        val rawId = trackId.removePrefix("local_")
        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, rawId.toLongOrNull() ?: return@withContext SourceResult.Failure(SourceError.NotFound))
        SourceResult.Success(
            ResolvedStream(
                uri = contentUri.toString(),
                mimeType = "audio/*"
            )
        )
    }

    suspend fun getAllTracks(limit: Int = 1000): List<Track> = withContext(Dispatchers.IO) {
        queryMediaStore(limit = limit)
    }

    private fun queryMediaStore(
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        limit: Int = 500
    ): List<Track> {
        val tracks = mutableListOf<Track>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC LIMIT $limit"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection ?: "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Title"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val albumId = c.getLong(albumIdCol)
                    val duration = c.getLong(durationCol)

                    val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")

                    tracks.add(
                        Track(
                            sourceId = SourceId.LOCAL,
                            sourceTrackId = id.toString(),
                            title = title,
                            titleSortKey = title.lowercase(),
                            artistName = artist,
                            albumName = album,
                            durationMs = duration,
                            artworkUrl = albumArtUri.toString(),
                            licenseName = "Local Media",
                            downloadAllowed = false
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permissions handled at UI runtime
        } catch (_: Exception) {
            // Ignore corrupted rows gracefully
        }

        return tracks
    }
}
