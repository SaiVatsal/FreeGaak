package com.saivatsal.soundorbit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.saivatsal.soundorbit.core.database.entity.DownloadEntity
import com.saivatsal.soundorbit.core.database.entity.DownloadStatus
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE trackId = :trackId LIMIT 1")
    fun getDownloadFlow(trackId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE trackId = :trackId LIMIT 1")
    suspend fun getDownload(trackId: String): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Transaction
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN downloads d ON t.id = d.trackId
        WHERE d.status = 'COMPLETED'
        ORDER BY d.completedAt DESC
    """)
    fun getCompletedOfflineTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status")
    suspend fun getDownloadsByStatus(status: DownloadStatus): List<DownloadEntity>

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun deleteDownload(trackId: String)

    @Query("UPDATE downloads SET status = :status, progress = :progress, bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes WHERE trackId = :trackId")
    suspend fun updateProgress(
        trackId: String,
        status: DownloadStatus,
        progress: Int,
        bytesDownloaded: Long,
        totalBytes: Long
    )

    @Query("UPDATE downloads SET status = :status, completedAt = :completedAt, localFilePath = :localFilePath, progress = 100 WHERE trackId = :trackId")
    suspend fun markCompleted(
        trackId: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        completedAt: Long = System.currentTimeMillis(),
        localFilePath: String
    )

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE trackId = :trackId")
    suspend fun markFailed(
        trackId: String,
        status: DownloadStatus = DownloadStatus.FAILED,
        errorMessage: String
    )
}
