package com.saivatsal.soundorbit.core.lyrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.lyrics.api.LrclibApi
import com.saivatsal.soundorbit.core.lyrics.repository.LyricsRepositoryImpl
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyricsRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var lyricsRepo: LyricsRepositoryImpl

    private val sampleTrack = Track(
        sourceId = SourceId.AUDIUS,
        sourceTrackId = "123",
        title = "Starlight Odyssey (feat. Cosmic)",
        titleSortKey = "starlight odyssey",
        artistName = "Cosmic Dreamer feat. Star",
        albumName = "Space Journey",
        durationMs = 210000L,
        artworkUrl = null,
        licenseName = "CC BY",
        licenseUrl = null,
        downloadAllowed = true
    )

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(LrclibApi::class.java)
        val context = ApplicationProvider.getApplicationContext<Context>()
        lyricsRepo = LyricsRepositoryImpl(context, api)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun fetchLyricsSuccessAndParseSynced() = runTest {
        val mockJson = """
            {
                "id": 42,
                "trackName": "Starlight Odyssey",
                "artistName": "Cosmic Dreamer",
                "albumName": "Space Journey",
                "duration": 210,
                "instrumental": false,
                "plainLyrics": "Flying through the stars\nInto the cosmic night",
                "syncedLyrics": "[00:10.50]Flying through the stars\n[00:20.00]Into the cosmic night"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockJson))

        val result = lyricsRepo.getLyrics(sampleTrack)
        assertThat(result.isSuccess).isTrue()

        val lyrics = result.getOrNull()
        assertThat(lyrics).isNotNull()
        assertThat(lyrics?.isSynced).isTrue()
        assertThat(lyrics?.syncedLyrics).hasSize(2)
        assertThat(lyrics?.syncedLyrics?.get(0)?.text).isEqualTo("Flying through the stars")
        assertThat(lyrics?.syncedLyrics?.get(0)?.timestampMs).isEqualTo(10500L)

        // Verify query cleaning
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).contains("track_name=Starlight%20Odyssey")
        assertThat(recordedRequest.path).contains("artist_name=Cosmic%20Dreamer")

        // Second call should hit memory cache (no new request to mockWebServer)
        val cachedResult = lyricsRepo.getLyrics(sampleTrack)
        assertThat(cachedResult.isSuccess).isTrue()
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun fallbackToSearchWhenDirectGetFails() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val searchJson = """
            [
                {
                    "id": 99,
                    "trackName": "Starlight Odyssey",
                    "artistName": "Cosmic Dreamer",
                    "plainLyrics": "Found via search",
                    "syncedLyrics": "[00:05.00]Found via search"
                }
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(searchJson))

        val result = lyricsRepo.getLyrics(sampleTrack)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.plainLyrics).isEqualTo("Found via search")
    }
}
