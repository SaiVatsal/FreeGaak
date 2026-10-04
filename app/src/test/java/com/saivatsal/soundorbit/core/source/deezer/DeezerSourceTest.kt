package com.saivatsal.soundorbit.core.source.deezer

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
class DeezerSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var moshi: Moshi
    private lateinit var deezerSource: DeezerSource

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder().build()
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        deezerSource = DeezerSource(okHttpClient, moshi, testDispatcher)
        deezerSource.baseUrl = mockWebServer.url("").toString().removeSuffix("/")
        deezerSource.apiKeys = listOf("key_1", "key_2")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun trendingTracks_parsesSuccessfully() = runTest(testDispatcher) {
        val json = """
            {
              "data": [
                {
                  "id": 3135556,
                  "title": "Harder, Better, Faster, Stronger",
                  "title_short": "Harder, Better, Faster, Stronger",
                  "duration": 224,
                  "preview": "https://cdns-preview-d.dzcdn.net/stream/c-d.mp3",
                  "artist": {
                    "id": 27,
                    "name": "Daft Punk",
                    "picture_medium": "https://api.deezer.com/artist/27/image"
                  },
                  "album": {
                    "id": 302127,
                    "title": "Discovery",
                    "cover_medium": "https://api.deezer.com/album/302127/image"
                  }
                }
              ],
              "total": 1
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = deezerSource.trending(null, TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val page = (result as SourceResult.Success).value
        assertThat(page.items).hasSize(1)

        val track = page.items[0]
        assertThat(track.sourceId).isEqualTo(SourceId.DEEZER)
        assertThat(track.sourceTrackId).isEqualTo("3135556")
        assertThat(track.title).isEqualTo("Harder, Better, Faster, Stronger")
        assertThat(track.artistName).isEqualTo("Daft Punk")
        assertThat(track.albumName).isEqualTo("Discovery")
        assertThat(track.durationMs).isEqualTo(224000L)
        assertThat(track.downloadAllowed).isFalse()
    }

    @Test
    fun search_returnsCombinedResults() = runTest(testDispatcher) {
        val tracksJson = """
            {
              "data": [
                {
                  "id": 1109731,
                  "title": "One More Time",
                  "duration": 320,
                  "preview": "https://cdns-preview.mp3",
                  "artist": { "id": 27, "name": "Daft Punk" },
                  "album": { "id": 302127, "title": "Discovery" }
                }
              ],
              "total": 1
            }
        """.trimIndent()

        val artistsJson = """
            {
              "data": [
                {
                  "id": 27,
                  "name": "Daft Punk",
                  "picture_medium": "https://api.deezer.com/artist/27/image"
                }
              ],
              "total": 1
            }
        """.trimIndent()

        val albumsJson = """
            {
              "data": [
                {
                  "id": 302127,
                  "title": "Discovery",
                  "cover_medium": "https://api.deezer.com/album/302127/image"
                }
              ],
              "total": 1
            }
        """.trimIndent()

        val playlistsJson = """
            {
              "data": [
                {
                  "id": 908622995,
                  "title": "Daft Punk Best Of",
                  "nb_tracks": 25,
                  "picture_medium": "https://api.deezer.com/playlist/908622995/image"
                }
              ],
              "total": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(tracksJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(artistsJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(albumsJson))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playlistsJson))

        val result = deezerSource.search("Daft Punk", SearchFilter(), PageRequest(0, 10))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val searchResults = (result as SourceResult.Success).value
        assertThat(searchResults.tracks.items).hasSize(1)
        assertThat(searchResults.tracks.items[0].title).isEqualTo("One More Time")
        assertThat(searchResults.artists.items).hasSize(1)
        assertThat(searchResults.artists.items[0].name).isEqualTo("Daft Punk")
        assertThat(searchResults.albums.items).hasSize(1)
        assertThat(searchResults.albums.items[0].name).isEqualTo("Discovery")
        assertThat(searchResults.playlists.items).hasSize(1)
        assertThat(searchResults.playlists.items[0].name).isEqualTo("Daft Punk Best Of")
    }

    @Test
    fun resolveStream_returnsPreviewUri() = runTest(testDispatcher) {
        val trackDetailJson = """
            {
              "id": 3135556,
              "title": "Harder, Better, Faster, Stronger",
              "duration": 224,
              "preview": "https://cdns-preview-d.dzcdn.net/stream/c-d.mp3",
              "artist": { "id": 27, "name": "Daft Punk" }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(trackDetailJson))

        val streamResult = deezerSource.resolveStream("3135556", AudioQuality.NORMAL)
        assertThat(streamResult).isInstanceOf(SourceResult.Success::class.java)
        val stream = (streamResult as SourceResult.Success).value
        assertThat(stream.uri).isEqualTo("https://cdns-preview-d.dzcdn.net/stream/c-d.mp3")
        assertThat(stream.mimeType).isEqualTo("audio/mpeg")
    }

    @Test
    fun rateLimited_rotatesToNextKeyAndSucceeds() = runTest(testDispatcher) {
        val json = """
            {
              "data": [
                {
                  "id": 3135556,
                  "title": "Harder, Better, Faster, Stronger",
                  "title_short": "Harder, Better, Faster, Stronger",
                  "duration": 224,
                  "preview": "https://cdns-preview-d.dzcdn.net/stream/c-d.mp3",
                  "artist": { "id": 27, "name": "Daft Punk" },
                  "album": { "id": 302127, "title": "Discovery" }
                }
              ],
              "total": 1
            }
        """.trimIndent()

        // First attempt with key_1 returns 429
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "5")
        )
        // Second attempt with rotated key_2 returns 200 OK
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
        )

        val result = deezerSource.trending(null, TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)

        val req1 = mockWebServer.takeRequest()
        assertThat(req1.getHeader("x-rapidapi-key")).isEqualTo("key_1")

        val req2 = mockWebServer.takeRequest()
        assertThat(req2.getHeader("x-rapidapi-key")).isEqualTo("key_2")
    }

    @Test
    fun allKeysRateLimited_returnsRateLimitedError() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "10")
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "15")
        )

        val result = deezerSource.trending(null, TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Failure::class.java)
        val error = (result as SourceResult.Failure).error
        assertThat(error).isInstanceOf(SourceError.RateLimited::class.java)
    }
}
