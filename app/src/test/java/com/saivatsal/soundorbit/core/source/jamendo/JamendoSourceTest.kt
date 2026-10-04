package com.saivatsal.soundorbit.core.source.jamendo

import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SearchFilter
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.TrendingWindow
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceError
import com.saivatsal.soundorbit.core.source.SourceResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JamendoSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var moshi: Moshi
    private lateinit var jamendoSource: JamendoSource

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder().build()
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        jamendoSource = JamendoSource(okHttpClient, moshi, testDispatcher)
        jamendoSource.baseUrl = mockWebServer.url("").toString().removeSuffix("/")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun trendingTracks_parsesSuccessfully() = runTest(testDispatcher) {
        val json = """
            {
              "headers": {
                "status": "success",
                "code": 0,
                "results_count": 1
              },
              "results": [
                {
                  "id": "189234",
                  "name": "Starlight Journey",
                  "duration": 240,
                  "artist_id": "456",
                  "artist_name": "Cosmic Voyager",
                  "album_name": "Deep Space",
                  "album_id": "789",
                  "license_ccurl": "http://creativecommons.org/licenses/by-sa/4.0/",
                  "image": "https://img.jamendo.com/track1.jpg",
                  "audio": "https://prod.jamendo.com/audio1.mp3",
                  "audiodl": "https://prod.jamendo.com/audiodl1.mp3"
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = jamendoSource.trending(null, TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val page = (result as SourceResult.Success).value
        assertThat(page.items).hasSize(1)

        val track = page.items[0]
        assertThat(track.sourceId).isEqualTo(SourceId.JAMENDO)
        assertThat(track.sourceTrackId).isEqualTo("189234")
        assertThat(track.title).isEqualTo("Starlight Journey")
        assertThat(track.artistName).isEqualTo("Cosmic Voyager")
        assertThat(track.albumName).isEqualTo("Deep Space")
        assertThat(track.durationMs).isEqualTo(240000L)
        assertThat(track.licenseName).isEqualTo("CC BY-SA")
        assertThat(track.licenseUrl).isEqualTo("http://creativecommons.org/licenses/by-sa/4.0/")
        assertThat(track.downloadAllowed).isTrue()
    }

    @Test
    fun search_returnsCombinedResults() = runTest(testDispatcher) {
        val tracksJson = """
            {
              "headers": { "status": "success", "code": 0, "results_count": 1 },
              "results": [
                {
                  "id": "trk_1",
                  "name": "Solar Flare",
                  "duration": 180,
                  "artist_id": "art_1",
                  "artist_name": "Solaris",
                  "license_ccurl": "http://creativecommons.org/licenses/by/4.0/"
                }
              ]
            }
        """.trimIndent()

        val artistsJson = """
            {
              "headers": { "status": "success", "code": 0, "results_count": 1 },
              "results": [
                {
                  "id": "art_1",
                  "name": "Solaris",
                  "image": "https://img.jamendo.com/art1.jpg"
                }
              ]
            }
        """.trimIndent()

        val albumsJson = """
            {
              "headers": { "status": "success", "code": 0, "results_count": 1 },
              "results": [
                {
                  "id": "alb_1",
                  "name": "Solar Flares EP",
                  "artist_name": "Solaris",
                  "image": "https://img.jamendo.com/alb1.jpg"
                }
              ]
            }
        """.trimIndent()

        val playlistsJson = """
            {
              "headers": { "status": "success", "code": 0, "results_count": 1 },
              "results": [
                {
                  "id": "pl_1",
                  "name": "Cosmic Chill",
                  "user_name": "StarGazer"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(tracksJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(artistsJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(albumsJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playlistsJson))

        val result = jamendoSource.search("Solar", SearchFilter(), PageRequest(0, 10))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val searchResults = (result as SourceResult.Success).value
        assertThat(searchResults.tracks.items).hasSize(1)
        assertThat(searchResults.tracks.items[0].title).isEqualTo("Solar Flare")
        assertThat(searchResults.artists.items).hasSize(1)
        assertThat(searchResults.artists.items[0].name).isEqualTo("Solaris")
        assertThat(searchResults.albums.items).hasSize(1)
        assertThat(searchResults.albums.items[0].name).isEqualTo("Solar Flares EP")
        assertThat(searchResults.playlists.items).hasSize(1)
        assertThat(searchResults.playlists.items[0].name).isEqualTo("Cosmic Chill")
    }

    @Test
    fun resolveStream_highQuality_returnsAudiodl() = runTest(testDispatcher) {
        val trackDetailJson = """
            {
              "headers": { "status": "success", "code": 0, "results_count": 1 },
              "results": [
                {
                  "id": "189234",
                  "name": "Starlight Journey",
                  "duration": 240,
                  "artist_name": "Cosmic Voyager",
                  "audio": "https://prod.jamendo.com/audio1.mp3",
                  "audiodl": "https://prod.jamendo.com/audiodl1.mp3"
                }
              ]
            }
        """.trimIndent()
        // Twice: once for track(), once inside resolveStream()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(trackDetailJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(trackDetailJson))

        val streamResult = jamendoSource.resolveStream("189234", AudioQuality.HIGH)
        assertThat(streamResult).isInstanceOf(SourceResult.Success::class.java)
        val stream = (streamResult as SourceResult.Success).value
        assertThat(stream.uri).isEqualTo("https://prod.jamendo.com/audiodl1.mp3")
        assertThat(stream.mimeType).isEqualTo("audio/mpeg")
    }

    @Test
    fun rateLimited_returnsRateLimitedError() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "10")
        )

        val result = jamendoSource.trending(null, TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Failure::class.java)
        val error = (result as SourceResult.Failure).error
        assertThat(error).isInstanceOf(SourceError.RateLimited::class.java)
    }
}
