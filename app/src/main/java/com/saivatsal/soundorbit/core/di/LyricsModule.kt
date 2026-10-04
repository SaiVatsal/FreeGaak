package com.saivatsal.soundorbit.core.di

import com.saivatsal.soundorbit.core.lyrics.repository.LyricsRepository
import com.saivatsal.soundorbit.core.lyrics.repository.LyricsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LyricsModule {

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(
        impl: LyricsRepositoryImpl
    ): LyricsRepository
}
