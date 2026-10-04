package com.saivatsal.soundorbit.core.repository

import com.saivatsal.soundorbit.core.database.dao.ListeningHistoryDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.ArtistPlayCount
import com.saivatsal.soundorbit.core.database.entity.ListeningHistoryEntity
import com.saivatsal.soundorbit.core.database.entity.toDomain
import com.saivatsal.soundorbit.core.database.entity.toEntity
import com.saivatsal.soundorbit.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ListeningHistoryRepository {
    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<Track>>
    fun getTopArtists(sinceTimestamp: Long = 0, limit: Int = 10): Flow<List<ArtistPlayCount>>
    fun getMostPlayedTracks(sinceTimestamp: Long = 0, limit: Int = 20): Flow<List<Track>>
    suspend fun recordPlayback(
        track: Track,
        completionPercent: Float,
        playDurationMs: Long
    )
    suspend fun clearHistoryOlderThan(days: Int)
    suspend fun clearAllHistory()
    fun getTotalPlayCount(): Flow<Int>
}

@Singleton
class ListeningHistoryRepositoryImpl @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val trackDao: TrackDao
) : ListeningHistoryRepository {

    override fun getRecentlyPlayedTracks(limit: Int): Flow<List<Track>> {
        return listeningHistoryDao.getRecentlyPlayedTracks(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTopArtists(sinceTimestamp: Long, limit: Int): Flow<List<ArtistPlayCount>> {
        return listeningHistoryDao.getTopArtists(sinceTimestamp, limit)
    }

    override fun getMostPlayedTracks(sinceTimestamp: Long, limit: Int): Flow<List<Track>> {
        return listeningHistoryDao.getMostPlayedTracks(sinceTimestamp, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordPlayback(
        track: Track,
        completionPercent: Float,
        playDurationMs: Long
    ) {
        val entity = track.toEntity()
        trackDao.insertOrUpdate(entity)
        val history = ListeningHistoryEntity(
            trackId = entity.id,
            playedAt = System.currentTimeMillis(),
            completionPercent = completionPercent.coerceIn(0f, 1f),
            playDurationMs = playDurationMs
        )
        listeningHistoryDao.insertHistory(history)
    }

    override suspend fun clearHistoryOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        listeningHistoryDao.clearHistoryOlderThan(cutoff)
    }

    override suspend fun clearAllHistory() {
        listeningHistoryDao.clearAllHistory()
    }

    override fun getTotalPlayCount(): Flow<Int> {
        return listeningHistoryDao.getTotalPlayCount()
    }
}
