package com.saivatsal.soundorbit.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
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
        Index("favoritedAt")
    ]
)
data class FavoriteEntity(
    @PrimaryKey
    val trackId: String,
    val favoritedAt: Long = System.currentTimeMillis()
)
