package com.saivatsal.soundorbit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.saivatsal.soundorbit.core.database.dao.DownloadDao
import com.saivatsal.soundorbit.core.database.dao.FavoriteDao
import com.saivatsal.soundorbit.core.database.dao.ListeningHistoryDao
import com.saivatsal.soundorbit.core.database.dao.PlaylistDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.DownloadEntity
import com.saivatsal.soundorbit.core.database.entity.FavoriteEntity
import com.saivatsal.soundorbit.core.database.entity.ListeningHistoryEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistTrackCrossRef
import com.saivatsal.soundorbit.core.database.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        ListeningHistoryEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SoundOrbitDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        const val DATABASE_NAME = "soundorbit.db"
    }
}
