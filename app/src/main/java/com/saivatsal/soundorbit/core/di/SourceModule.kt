package com.saivatsal.soundorbit.core.di

import com.saivatsal.soundorbit.core.source.MusicSource
import com.saivatsal.soundorbit.core.source.audius.AudiusSource
import com.saivatsal.soundorbit.core.source.deezer.DeezerSource
import com.saivatsal.soundorbit.core.source.jamendo.JamendoSource
import com.saivatsal.soundorbit.core.source.local.LocalFilesSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
abstract class SourceModule {

    @Multibinds
    abstract fun bindMusicSources(): Set<MusicSource>

    @Binds
    @IntoSet
    abstract fun bindLocalFilesSource(source: LocalFilesSource): MusicSource

    @Binds
    @IntoSet
    abstract fun bindAudiusSource(source: AudiusSource): MusicSource

    @Binds
    @IntoSet
    abstract fun bindJamendoSource(source: JamendoSource): MusicSource

    @Binds
    @IntoSet
    abstract fun bindDeezerSource(source: DeezerSource): MusicSource
}
