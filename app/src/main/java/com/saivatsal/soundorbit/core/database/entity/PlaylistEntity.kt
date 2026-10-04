package com.saivatsal.soundorbit.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.core.model.SourceId

@Entity(
    tableName = "playlists",
    indices = [
        Index("createdAt"),
        Index("name")
    ]
)
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val coverArtworkUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun PlaylistEntity.toDomain(trackCount: Int = 0): Playlist {
    return Playlist(
        sourceId = SourceId.LOCAL,
        sourcePlaylistId = id,
        name = name,
        description = description,
        artworkUrl = coverArtworkUri,
        trackCount = trackCount
    )
}
