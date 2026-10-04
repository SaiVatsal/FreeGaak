package com.saivatsal.soundorbit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import com.saivatsal.soundorbit.core.database.entity.ArtistPlayCount
import com.saivatsal.soundorbit.core.database.entity.ListeningHistoryEntity
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningHistoryDao {

    @Insert
    suspend fun insertHistory(history: ListeningHistoryEntity): Long

    @Transaction
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN listening_history lh ON t.id = lh.trackId
        ORDER BY lh.playedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedTracks(limit: Int = 50): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.artistName, COUNT(lh.id) as playCount, SUM(lh.playDurationMs) as totalDurationMs
        FROM listening_history lh
        INNER JOIN tracks t ON lh.trackId = t.id
        WHERE lh.playedAt >= :sinceTimestamp
        GROUP BY t.artistName
        ORDER BY playCount DESC, totalDurationMs DESC
        LIMIT :limit
    """)
    fun getTopArtists(sinceTimestamp: Long = 0, limit: Int = 10): Flow<List<ArtistPlayCount>>

    @RewriteQueriesToDropUnusedColumns
    @Transaction
    @Query("""
        SELECT t.*, COUNT(lh.id) as playCount
        FROM tracks t
        INNER JOIN listening_history lh ON t.id = lh.trackId
        WHERE lh.playedAt >= :sinceTimestamp
        GROUP BY t.id
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun getMostPlayedTracks(sinceTimestamp: Long = 0, limit: Int = 20): Flow<List<TrackEntity>>

    @Query("DELETE FROM listening_history WHERE playedAt < :beforeTimestamp")
    suspend fun clearHistoryOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM listening_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM listening_history")
    fun getTotalPlayCount(): Flow<Int>
}
