package com.saivatsal.soundorbit.core.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TrackWithFavorite(
    @Embedded val track: TrackEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val favorite: FavoriteEntity?
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
)

data class TrackWithHistory(
    @Embedded val history: ListeningHistoryEntity,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "id"
    )
    val track: TrackEntity
)

data class ArtistPlayCount(
    val artistName: String,
    val playCount: Int,
    val totalDurationMs: Long
)
