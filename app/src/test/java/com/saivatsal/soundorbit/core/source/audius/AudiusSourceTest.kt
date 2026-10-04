package com.saivatsal.soundorbit.core.source.audius

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
class AudiusSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var moshi: Moshi
    private lateinit var hostSelector: AudiusHostSelector
    private lateinit var audiusSource: AudiusSource

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder().build()
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        hostSelector = AudiusHostSelector(okHttpClient, moshi, testDispatcher)
        hostSelector.overrideHost = mockWebServer.url("/").toString().removeSuffix("/")
        audiusSource = AudiusSource(okHttpClient, moshi, hostSelector, testDispatcher)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `metadata matches expected Audius source properties`() {
        assertThat(audiusSource.id).isEqualTo(SourceId.AUDIUS)
        assertThat(audiusSource.displayName).isEqualTo("Audius")
        assertThat(audiusSource.capabilities.canSearch).isTrue()
        assertThat(audiusSource.capabilities.hasTrending).isTrue()
        assertThat(audiusSource.capabilities.hasGenres).isTrue()
        assertThat(audiusSource.capabilities.allowsDownloads).isTrue()
        assertThat(audiusSource.capabilities.ownsAudioPipeline).isFalse()
    }

    @Test
    fun `resolveStream constructs valid stream URL with app_name parameter`() = runTest {
        val result = audiusSource.resolveStream("track123", AudioQuality.HIGH)
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val stream = (result as SourceResult.Success).value
        assertThat(stream.uri).contains("/v1/tracks/track123/stream")
        assertThat(stream.uri).contains("app_name=SoundOrbit")
        assertThat(stream.mimeType).isEqualTo("audio/mpeg")
    }

    @Test
    fun `trending tracks deserializes and filters gated content correctly`() = runTest {
        val json = """
            {
              "data": [
                {
                  "id": "trk1",
                  "title": "Cosmic Drift",
                  "duration": 180,
                  "genre": "Electronic",
                  "is_stream_gated": false,
                  "artwork": {
                    "480x480": "https://audius.co/art/trk1.jpg"
                  },
                  "user": {
                    "id": "usr1",
                    "name": "Starlight Producer"
                  }
                },
                {
                  "id": "trk2_gated",
                  "title": "NFT Only Song",
                  "duration": 200,
                  "genre": "Electronic",
                  "is_stream_gated": true,
                  "user": {
                    "id": "usr2",
                    "name": "Crypto Artist"
                  }
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = audiusSource.trending("Electronic", TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val page = (result as SourceResult.Success).value
        assertThat(page.items).hasSize(1)
        val track = page.items[0]
        assertThat(track.sourceTrackId).isEqualTo("trk1")
        assertThat(track.title).isEqualTo("Cosmic Drift")
        assertThat(track.artistName).isEqualTo("Starlight Producer")
        assertThat(track.durationMs).isEqualTo(180000L)
        assertThat(track.artworkUrl).isEqualTo("https://audius.co/art/trk1.jpg")
    }

    @Test
    fun `rate limited response maps to SourceError RateLimited`() = runTest(testDispatcher) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "30")
                .setBody("Too many requests")
        )

        val result = audiusSource.trending(null, TrendingWindow.WEEK, PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Failure::class.java)
        val failure = result as SourceResult.Failure
        assertThat(failure.error).isInstanceOf(SourceError.RateLimited::class.java)
        val rateLimited = failure.error as SourceError.RateLimited
        assertThat(rateLimited.retryAfterMs).isEqualTo(30000L)
    }

    @Test
    fun `search tracks returns unified search results`() = runTest(testDispatcher) {
        val tracksJson = """
            {
              "data": [
                {
                  "id": "search_trk_1",
                  "title": "Nebula Beats",
                  "duration": 210,
                  "is_stream_gated": false,
                  "user": {
                    "id": "artist_1",
                    "name": "Astro Beats"
                  }
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(tracksJson))

        val usersJson = """
            {
              "data": [
                {
                  "id": "artist_1",
                  "name": "Astro Beats",
                  "follower_count": 5000
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(usersJson))

        val playlistsJson = """
            {
              "data": [
                {
                  "id": "playlist_1",
                  "playlist_name": "Space Lounge",
                  "track_count": 15
                }
              ]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(playlistsJson))

        val result = audiusSource.search("Astro", SearchFilter(), PageRequest(0, 10))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val searchResults = (result as SourceResult.Success).value
        assertThat(searchResults.tracks.items).hasSize(1)
        assertThat(searchResults.tracks.items[0].title).isEqualTo("Nebula Beats")
        assertThat(searchResults.artists.items).hasSize(1)
        assertThat(searchResults.artists.items[0].name).isEqualTo("Astro Beats")
        assertThat(searchResults.playlists.items).hasSize(1)
        assertThat(searchResults.playlists.items[0].name).isEqualTo("Space Lounge")
    }
}
