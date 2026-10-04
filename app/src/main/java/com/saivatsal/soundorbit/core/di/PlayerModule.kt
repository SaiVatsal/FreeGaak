package com.saivatsal.soundorbit.core.di

import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.player.CrossfadePlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(player: CrossfadePlayer): AudioPlayer
}
