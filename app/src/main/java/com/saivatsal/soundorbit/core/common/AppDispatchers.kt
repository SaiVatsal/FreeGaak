package com.saivatsal.soundorbit.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val dispatcher: SoundOrbitDispatchers)

enum class SoundOrbitDispatchers {
    Default,
    IO,
    Main
}
