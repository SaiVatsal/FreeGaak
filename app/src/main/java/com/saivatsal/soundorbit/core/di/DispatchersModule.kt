package com.saivatsal.soundorbit.core.di

import com.saivatsal.soundorbit.core.common.Dispatcher
import com.saivatsal.soundorbit.core.common.SoundOrbitDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(SoundOrbitDispatchers.IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(SoundOrbitDispatchers.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Dispatcher(SoundOrbitDispatchers.Main)
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
