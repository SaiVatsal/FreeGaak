package com.saivatsal.soundorbit.core.di

import android.content.Context
import androidx.room.Room
import com.saivatsal.soundorbit.core.database.SoundOrbitDatabase
import com.saivatsal.soundorbit.core.database.dao.DownloadDao
import com.saivatsal.soundorbit.core.database.dao.FavoriteDao
import com.saivatsal.soundorbit.core.database.dao.ListeningHistoryDao
import com.saivatsal.soundorbit.core.database.dao.PlaylistDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.repository.DownloadRepository
import com.saivatsal.soundorbit.core.repository.DownloadRepositoryImpl
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.repository.FavoritesRepositoryImpl
import com.saivatsal.soundorbit.core.repository.ListeningHistoryRepository
import com.saivatsal.soundorbit.core.repository.ListeningHistoryRepositoryImpl
import com.saivatsal.soundorbit.core.repository.PlaylistRepository
import com.saivatsal.soundorbit.core.repository.PlaylistRepositoryImpl
import com.saivatsal.soundorbit.core.repository.YourOrbitRepository
import com.saivatsal.soundorbit.core.repository.YourOrbitRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingModule {

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryImpl
    ): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindListeningHistoryRepository(
        impl: ListeningHistoryRepositoryImpl
    ): ListeningHistoryRepository

    @Binds
    @Singleton
    abstract fun bindYourOrbitRepository(
        impl: YourOrbitRepositoryImpl
    ): YourOrbitRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SoundOrbitDatabase {
        return Room.databaseBuilder(
            context,
            SoundOrbitDatabase::class.java,
            SoundOrbitDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTrackDao(database: SoundOrbitDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideFavoriteDao(database: SoundOrbitDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun providePlaylistDao(database: SoundOrbitDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideListeningHistoryDao(database: SoundOrbitDatabase): ListeningHistoryDao = database.listeningHistoryDao()

    @Provides
    fun provideDownloadDao(database: SoundOrbitDatabase): DownloadDao = database.downloadDao()
}
