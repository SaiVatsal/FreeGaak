package com.saivatsal.soundorbit.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("trackId"),
        Index("playedAt")
    ]
)
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val completionPercent: Float = 0f,
    val playDurationMs: Long = 0L
)
