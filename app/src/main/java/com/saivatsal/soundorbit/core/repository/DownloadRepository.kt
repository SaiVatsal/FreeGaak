package com.saivatsal.soundorbit.core.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.saivatsal.soundorbit.core.database.dao.DownloadDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.DownloadEntity
import com.saivatsal.soundorbit.core.database.entity.DownloadStatus
import com.saivatsal.soundorbit.core.database.entity.toDomain
import com.saivatsal.soundorbit.core.database.entity.toEntity
import com.saivatsal.soundorbit.core.download.TrackDownloadWorker
import com.saivatsal.soundorbit.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    fun getDownload(trackId: String): Flow<DownloadEntity?>
    fun getCompletedOfflineTracks(): Flow<List<Track>>
    suspend fun enqueueDownload(track: Track)
    suspend fun updateProgress(trackId: String, status: DownloadStatus, progress: Int, bytesDownloaded: Long, totalBytes: Long)
    suspend fun markCompleted(trackId: String, localFilePath: String)
    suspend fun markFailed(trackId: String, errorMessage: String)
    suspend fun deleteDownload(trackId: String)
    suspend fun isTrackDownloaded(trackId: String): Boolean
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val trackDao: TrackDao
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadEntity>> {
        return downloadDao.getAllDownloads()
    }

    override fun getDownload(trackId: String): Flow<DownloadEntity?> {
        return downloadDao.getDownloadFlow(trackId)
    }

    override fun getCompletedOfflineTracks(): Flow<List<Track>> {
        return downloadDao.getCompletedOfflineTracks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun enqueueDownload(track: Track) {
        val entity = track.toEntity()
        trackDao.insertOrUpdate(entity)
        val download = DownloadEntity(
            trackId = entity.id,
            status = DownloadStatus.QUEUED,
            progress = 0
        )
        downloadDao.insertOrUpdateDownload(download)

        try {
            val workRequest = OneTimeWorkRequestBuilder<TrackDownloadWorker>()
                .setInputData(
                    workDataOf(
                        TrackDownloadWorker.KEY_TRACK_ID to track.compositeKey,
                        TrackDownloadWorker.KEY_SOURCE_ID to track.sourceId.name,
                        TrackDownloadWorker.KEY_SOURCE_TRACK_ID to track.sourceTrackId,
                        TrackDownloadWorker.KEY_TITLE to track.title,
                        TrackDownloadWorker.KEY_ARTIST to track.artistName
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("download_${track.compositeKey}")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_${track.compositeKey}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (_: Exception) {
            // WorkManager might not be initialized in test environments or low memory conditions
        }
    }

    override suspend fun updateProgress(
        trackId: String,
        status: DownloadStatus,
        progress: Int,
        bytesDownloaded: Long,
        totalBytes: Long
    ) {
        downloadDao.updateProgress(trackId, status, progress, bytesDownloaded, totalBytes)
    }

    override suspend fun markCompleted(trackId: String, localFilePath: String) {
        downloadDao.markCompleted(
            trackId = trackId,
            status = DownloadStatus.COMPLETED,
            localFilePath = localFilePath
        )
    }

    override suspend fun markFailed(trackId: String, errorMessage: String) {
        downloadDao.markFailed(
            trackId = trackId,
            status = DownloadStatus.FAILED,
            errorMessage = errorMessage
        )
    }

    override suspend fun deleteDownload(trackId: String) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("download_$trackId")
        } catch (_: Exception) {}

        val existing = downloadDao.getDownload(trackId)
        if (existing?.localFilePath != null) {
            val file = File(existing.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        downloadDao.deleteDownload(trackId)
    }

    override suspend fun isTrackDownloaded(trackId: String): Boolean {
        val download = downloadDao.getDownload(trackId)
        return download?.status == DownloadStatus.COMPLETED && !download.localFilePath.isNullOrBlank()
    }
}
