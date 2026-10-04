package com.saivatsal.soundorbit.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(
    tableName = "downloads",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("trackId", unique = true),
        Index("status"),
        Index("createdAt")
    ]
)
data class DownloadEntity(
    @PrimaryKey
    val trackId: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val localFilePath: String? = null,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)
