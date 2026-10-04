package com.saivatsal.soundorbit.core.repository

import com.saivatsal.soundorbit.core.database.dao.PlaylistDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.PlaylistEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistTrackCrossRef
import com.saivatsal.soundorbit.core.database.entity.toDomain
import com.saivatsal.soundorbit.core.database.entity.toEntity
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylist(playlistId: String): Flow<Playlist?>
    fun getTracksForPlaylist(playlistId: String): Flow<List<Track>>
    suspend fun createPlaylist(name: String, description: String? = null, artworkUri: String? = null): String
    suspend fun updatePlaylist(playlistId: String, name: String, description: String?, artworkUri: String?)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    suspend fun reorderPlaylistTracks(playlistId: String, trackIdsInOrder: List<String>)
}

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { playlists ->
            playlists.map { it.toDomain() }
        }
    }

    override fun getPlaylist(playlistId: String): Flow<Playlist?> {
        val playlistFlow = playlistDao.getPlaylistById(playlistId)
        val countFlow = playlistDao.getTrackCountForPlaylist(playlistId)
        return combine(playlistFlow, countFlow) { playlist, count ->
            playlist?.toDomain(trackCount = count)
        }
    }

    override fun getTracksForPlaylist(playlistId: String): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createPlaylist(
        name: String,
        description: String?,
        artworkUri: String?
    ): String {
        val id = UUID.randomUUID().toString()
        val playlist = PlaylistEntity(
            id = id,
            name = name.trim(),
            description = description?.trim(),
            coverArtworkUri = artworkUri,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(playlist)
        return id
    }

    override suspend fun updatePlaylist(
        playlistId: String,
        name: String,
        description: String?,
        artworkUri: String?
    ) {
        val playlist = PlaylistEntity(
            id = playlistId,
            name = name.trim(),
            description = description?.trim(),
            coverArtworkUri = artworkUri,
            updatedAt = System.currentTimeMillis()
        )
        playlistDao.updatePlaylist(playlist)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        val entity = track.toEntity()
        trackDao.insertOrUpdate(entity)
        val nextPosition = playlistDao.getNextPositionForPlaylist(playlistId)
        val crossRef = PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = entity.id,
            position = nextPosition
        )
        playlistDao.insertTrackToPlaylist(crossRef)
    }

    override suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val entities = tracks.map { it.toEntity() }
        trackDao.insertOrUpdateAll(entities)
        var startPosition = playlistDao.getNextPositionForPlaylist(playlistId)
        val crossRefs = entities.map { entity ->
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = entity.id,
                position = startPosition++
            )
        }
        playlistDao.insertTracksToPlaylist(crossRefs)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    override suspend fun reorderPlaylistTracks(
        playlistId: String,
        trackIdsInOrder: List<String>
    ) {
        playlistDao.reorderPlaylistTracks(playlistId, trackIdsInOrder)
    }
}
