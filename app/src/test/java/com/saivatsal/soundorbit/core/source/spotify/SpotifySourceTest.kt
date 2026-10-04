package com.saivatsal.soundorbit.core.source.spotify

import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.model.SearchFilter
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.TrendingWindow
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceResult
import com.saivatsal.soundorbit.core.source.deezer.DeezerSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpotifySourceTest {

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var moshi: Moshi
    private lateinit var deezerSource: DeezerSource
    private lateinit var spotifySource: SpotifySource

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        okHttpClient = OkHttpClient.Builder().build()
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        deezerSource = DeezerSource(okHttpClient, moshi, testDispatcher)
        spotifySource = SpotifySource(okHttpClient, moshi, deezerSource, testDispatcher)
    }

    @Test
    fun sourceIdentity_isCorrect() {
        assertThat(spotifySource.id).isEqualTo(SourceId.SPOTIFY)
        assertThat(spotifySource.displayName).isEqualTo("Spotify Discovery")
        assertThat(spotifySource.capabilities.canSearch).isTrue()
        assertThat(spotifySource.capabilities.hasTrending).isTrue()
        assertThat(spotifySource.capabilities.hasPlaylists).isTrue()
    }
}
