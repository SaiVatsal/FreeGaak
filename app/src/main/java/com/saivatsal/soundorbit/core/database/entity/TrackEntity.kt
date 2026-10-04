package com.saivatsal.soundorbit.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track

@Entity(
    tableName = "tracks",
    indices = [
        Index("sourceId", "sourceTrackId", unique = true),
        Index("artistName"),
        Index("titleSortKey")
    ]
)
data class TrackEntity(
    @PrimaryKey
    val id: String, // Format: "${sourceId.name}_${sourceTrackId}"
    val sourceId: String,
    val sourceTrackId: String,
    val title: String,
    val titleSortKey: String,
    val artistName: String,
    val albumName: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val licenseName: String?,
    val licenseUrl: String?,
    val downloadAllowed: Boolean,
    val localUri: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

fun Track.toEntity(localUri: String? = null): TrackEntity {
    return TrackEntity(
        id = "${sourceId.name}_$sourceTrackId",
        sourceId = sourceId.name,
        sourceTrackId = sourceTrackId,
        title = title,
        titleSortKey = titleSortKey,
        artistName = artistName,
        albumName = albumName,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        licenseName = licenseName,
        licenseUrl = licenseUrl,
        downloadAllowed = downloadAllowed,
        localUri = localUri
    )
}

fun TrackEntity.toDomain(): Track {
    val source = try {
        SourceId.valueOf(sourceId)
    } catch (e: Exception) {
        SourceId.LOCAL
    }
    return Track(
        sourceId = source,
        sourceTrackId = sourceTrackId,
        title = title,
        titleSortKey = titleSortKey,
        artistName = artistName,
        albumName = albumName,
        durationMs = durationMs,
        artworkUrl = artworkUrl,
        licenseName = licenseName,
        licenseUrl = licenseUrl,
        downloadAllowed = downloadAllowed
    )
}
