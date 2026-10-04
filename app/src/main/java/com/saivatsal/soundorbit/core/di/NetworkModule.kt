package com.saivatsal.soundorbit.core.di

import android.content.Context
import com.saivatsal.soundorbit.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, 25L * 1024 * 1024) // 25 MB
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cache: Cache): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @Named("LRCLIB_CLIENT")
    fun provideLrclibOkHttpClient(baseClient: OkHttpClient): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SoundOrbit/${BuildConfig.VERSION_NAME} (https://github.com/saivatsal/soundorbit)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("MUSICBRAINZ_CLIENT")
    fun provideMusicBrainzOkHttpClient(baseClient: OkHttpClient): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor { chain ->
                val contact = BuildConfig.CONTACT_EMAIL.ifBlank { "anonymous@soundorbit.local" }
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SoundOrbit/${BuildConfig.VERSION_NAME} ( $contact )")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideLrclibApi(
        @Named("LRCLIB_CLIENT") client: OkHttpClient,
        moshi: Moshi
    ): com.saivatsal.soundorbit.core.lyrics.api.LrclibApi {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
            .create(com.saivatsal.soundorbit.core.lyrics.api.LrclibApi::class.java)
    }
}
