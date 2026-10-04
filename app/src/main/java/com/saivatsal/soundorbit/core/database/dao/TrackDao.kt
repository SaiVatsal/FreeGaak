package com.saivatsal.soundorbit.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import com.saivatsal.soundorbit.core.database.entity.TrackWithFavorite
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Upsert
    suspend fun insertOrUpdate(track: TrackEntity)

    @Upsert
    suspend fun insertOrUpdateAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE sourceId = :sourceId AND sourceTrackId = :sourceTrackId LIMIT 1")
    suspend fun getTrackBySource(sourceId: String, sourceTrackId: String): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY cachedAt DESC LIMIT :limit")
    fun getRecentlyCachedTracks(limit: Int = 50): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%' ORDER BY titleSortKey ASC")
    fun searchLocalTracks(query: String): Flow<List<TrackEntity>>

    @Transaction
    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    fun getTrackWithFavoriteFlow(id: String): Flow<TrackWithFavorite?>

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("DELETE FROM tracks WHERE id NOT IN (SELECT trackId FROM favorites) AND id NOT IN (SELECT trackId FROM playlist_tracks) AND id NOT IN (SELECT trackId FROM downloads) AND id NOT IN (SELECT trackId FROM listening_history ORDER BY playedAt DESC LIMIT 200)")
    suspend fun purgeUnreferencedTracks()
}
