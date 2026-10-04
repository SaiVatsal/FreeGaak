package com.saivatsal.soundorbit.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.saivatsal.soundorbit.R
import com.saivatsal.soundorbit.core.database.dao.DownloadDao
import com.saivatsal.soundorbit.core.database.entity.DownloadStatus
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.core.source.SourceResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class TrackDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadWorkerEntryPoint {
        fun downloadDao(): DownloadDao
        fun sourceRegistry(): SourceRegistry
        fun okHttpClient(): OkHttpClient
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
        val sourceIdStr = inputData.getString(KEY_SOURCE_ID) ?: return@withContext Result.failure()
        val sourceTrackId = inputData.getString(KEY_SOURCE_TRACK_ID) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Track"
        val artist = inputData.getString(KEY_ARTIST) ?: "Unknown Artist"

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                DownloadWorkerEntryPoint::class.java
            )
        } catch (_: Exception) {
            return@withContext Result.failure()
        }
        val downloadDao = entryPoint.downloadDao()
        val sourceRegistry = entryPoint.sourceRegistry()
        val okHttpClient = entryPoint.okHttpClient()

        createNotificationChannel()

        val notificationId = trackId.hashCode()
        try {
            setForeground(createForegroundInfo(notificationId, title, artist, 0))
        } catch (_: Exception) {
            // Foreground notification might fail on background restrictions, continue work
        }

        downloadDao.updateProgress(
            trackId = trackId,
            status = DownloadStatus.DOWNLOADING,
            progress = 0,
            bytesDownloaded = 0L,
            totalBytes = 0L
        )

        val downloadsDir = File(context.filesDir, "downloads").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedKey = trackId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val tempFile = File(downloadsDir, "$sanitizedKey.tmp")
        val finalFile = File(downloadsDir, "$sanitizedKey.mp3")

        try {
            val sourceId = try {
                SourceId.valueOf(sourceIdStr)
            } catch (_: Exception) {
                SourceId.LOCAL
            }

            val source = sourceRegistry.getSource(sourceId)
            val streamResult = source?.resolveStream(sourceTrackId, AudioQuality.HIGH)
            val streamUri = (streamResult as? SourceResult.Success)?.value?.uri

            if (streamUri.isNullOrBlank()) {
                val errorMsg = "Failed to resolve stream URL for track $trackId"
                downloadDao.markFailed(trackId, errorMessage = errorMsg)
                return@withContext Result.failure()
            }

            val request = Request.Builder()
                .url(streamUri)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP error ${response.code} downloading track"
                downloadDao.markFailed(trackId, errorMessage = errorMsg)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                downloadDao.markFailed(trackId, errorMessage = "Empty response body")
                return@withContext Result.failure()
            }

            val contentLength = body.contentLength()
            var downloadedBytes = 0L
            var lastReportedProgress = 0

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            tempFile.delete()
                            downloadDao.updateProgress(
                                trackId = trackId,
                                status = DownloadStatus.CANCELLED,
                                progress = 0,
                                bytesDownloaded = downloadedBytes,
                                totalBytes = contentLength
                            )
                            return@withContext Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (contentLength > 0) {
                            ((downloadedBytes * 100) / contentLength).toInt().coerceIn(0, 100)
                        } else 0

                        if (progress >= lastReportedProgress + 5 || downloadedBytes == contentLength) {
                            lastReportedProgress = progress
                            downloadDao.updateProgress(
                                trackId = trackId,
                                status = DownloadStatus.DOWNLOADING,
                                progress = progress,
                                bytesDownloaded = downloadedBytes,
                                totalBytes = contentLength
                            )
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (finalFile.exists()) {
                    finalFile.delete()
                }
                val renamed = tempFile.renameTo(finalFile)
                if (renamed) {
                    downloadDao.markCompleted(
                        trackId = trackId,
                        status = DownloadStatus.COMPLETED,
                        completedAt = System.currentTimeMillis(),
                        localFilePath = finalFile.absolutePath
                    )
                    Result.success(workDataOf(KEY_FILE_PATH to finalFile.absolutePath))
                } else {
                    downloadDao.markFailed(trackId, errorMessage = "Failed to rename temporary file")
                    Result.failure()
                }
            } else {
                downloadDao.markFailed(trackId, errorMessage = "Downloaded file is empty")
                Result.failure()
            }
        } catch (e: Exception) {
            tempFile.delete()
            downloadDao.markFailed(trackId, errorMessage = e.message ?: "Unknown download error")
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Track Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of offline track downloads"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(
        notificationId: Int,
        title: String,
        artist: String,
        progress: Int
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading $title")
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "soundorbit_downloads"
        const val KEY_TRACK_ID = "track_id"
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_SOURCE_TRACK_ID = "source_track_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
    }
}
