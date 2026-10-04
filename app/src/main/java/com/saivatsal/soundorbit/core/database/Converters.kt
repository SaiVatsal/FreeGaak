package com.saivatsal.soundorbit.core.database

import androidx.room.TypeConverter
import com.saivatsal.soundorbit.core.database.entity.DownloadStatus
import com.saivatsal.soundorbit.core.model.SourceId

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (e: Exception) {
        DownloadStatus.FAILED
    }

    @TypeConverter
    fun fromSourceId(sourceId: SourceId): String = sourceId.name

    @TypeConverter
    fun toSourceId(value: String): SourceId = try {
        SourceId.valueOf(value)
    } catch (e: Exception) {
        SourceId.LOCAL
    }
}
