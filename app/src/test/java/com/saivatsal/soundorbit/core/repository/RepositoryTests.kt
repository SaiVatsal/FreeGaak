package com.saivatsal.soundorbit.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.database.SoundOrbitDatabase
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.BrowseKind
import com.saivatsal.soundorbit.core.model.SearchFilter
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.model.TrendingWindow
import com.saivatsal.soundorbit.core.source.MusicSource
import com.saivatsal.soundorbit.core.source.Page
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SearchResults
import com.saivatsal.soundorbit.core.source.SourceCapabilities
import com.saivatsal.soundorbit.core.source.SourceResult
import com.saivatsal.soundorbit.core.source.SourceRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTests {

    private lateinit var db: SoundOrbitDatabase
    private lateinit var favoritesRepo: FavoritesRepositoryImpl
    private lateinit var playlistRepo: PlaylistRepositoryImpl
    private lateinit var historyRepo: ListeningHistoryRepositoryImpl
    private lateinit var yourOrbitRepo: YourOrbitRepositoryImpl
    private lateinit var downloadRepo: DownloadRepositoryImpl

    private val track1 = Track(
        sourceId = SourceId.AUDIUS,
        sourceTrackId = "track_1",
        title = "Track One",
        titleSortKey = "track one",
        artistName = "Artist Alpha",
        albumName = "Album Alpha",
        durationMs = 180000L,
        artworkUrl = "https://example.com/art1.jpg",
        licenseName = "CC BY",
        licenseUrl = null,
        downloadAllowed = true
    )

    private val track2 = Track(
        sourceId = SourceId.JAMENDO,
        sourceTrackId = "track_2",
        title = "Track Two",
        titleSortKey = "track two",
        artistName = "Artist Beta",
        albumName = "Album Beta",
        durationMs = 240000L,
        artworkUrl = "https://example.com/art2.jpg",
        licenseName = "CC BY-SA",
        licenseUrl = null,
        downloadAllowed = false
    )

    private val fakeSource = object : MusicSource {
        override val id: SourceId = SourceId.AUDIUS
        override val displayName: String = "Audius"
        override val capabilities: SourceCapabilities = SourceCapabilities()

        override suspend fun search(query: String, filter: SearchFilter, page: PageRequest): SourceResult<SearchResults> =
            SourceResult.Success(SearchResults())

        override suspend fun trending(genre: String?, window: TrendingWindow, page: PageRequest): SourceResult<Page<Track>> =
            SourceResult.Success(Page(listOf(track1, track2), null))

        override suspend fun browse(kind: BrowseKind, value: String, page: PageRequest): SourceResult<Page<Track>> =
            SourceResult.Success(Page(emptyList(), null))

        override suspend fun track(id: String) = SourceResult.Success(track1)
        override suspend fun artist(id: String) = throw UnsupportedOperationException()
        override suspend fun album(id: String) = throw UnsupportedOperationException()
        override suspend fun playlist(id: String) = throw UnsupportedOperationException()
        override suspend fun resolveStream(trackId: String, quality: AudioQuality) = throw UnsupportedOperationException()
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            androidx.work.WorkManager.initialize(
                context,
                androidx.work.Configuration.Builder().build()
            )
        } catch (_: Exception) {}

        db = Room.inMemoryDatabaseBuilder(context, SoundOrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        favoritesRepo = FavoritesRepositoryImpl(db.favoriteDao(), db.trackDao())
        playlistRepo = PlaylistRepositoryImpl(db.playlistDao(), db.trackDao())
        historyRepo = ListeningHistoryRepositoryImpl(db.listeningHistoryDao(), db.trackDao())
        downloadRepo = DownloadRepositoryImpl(context, db.downloadDao(), db.trackDao())
        val sourceRegistry = SourceRegistry(setOf(fakeSource))
        yourOrbitRepo = YourOrbitRepositoryImpl(
            db.listeningHistoryDao(),
            db.favoriteDao(),
            db.trackDao(),
            sourceRegistry
        )
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun favoritesRepositoryToggleAndFlow() = runTest {
        favoritesRepo.toggleFavorite(track1)
        assertThat(favoritesRepo.isFavoriteDirect("AUDIUS_track_1")).isTrue()

        favoritesRepo.getFavoriteTracks().test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].title).isEqualTo("Track One")
        }

        favoritesRepo.toggleFavorite(track1)
        assertThat(favoritesRepo.isFavoriteDirect("AUDIUS_track_1")).isFalse()
    }

    @Test
    fun playlistRepositoryFullFlow() = runTest {
        val playlistId = playlistRepo.createPlaylist(name = "Morning Workout", description = "High energy")
        assertThat(playlistId).isNotEmpty()

        playlistRepo.addTracksToPlaylist(playlistId, listOf(track1, track2))

        playlistRepo.getTracksForPlaylist(playlistId).test {
            val tracks = awaitItem()
            assertThat(tracks).hasSize(2)
            assertThat(tracks[0].title).isEqualTo("Track One")
            assertThat(tracks[1].title).isEqualTo("Track Two")
        }

        playlistRepo.removeTrackFromPlaylist(playlistId, "AUDIUS_track_1")

        playlistRepo.getTracksForPlaylist(playlistId).test {
            val tracks = awaitItem()
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].title).isEqualTo("Track Two")
        }
    }

    @Test
    fun historyAndOrbitSummaryGeneration() = runTest {
        // Record playbacks
        historyRepo.recordPlayback(track1, completionPercent = 1.0f, playDurationMs = 180000L)
        historyRepo.recordPlayback(track1, completionPercent = 0.9f, playDurationMs = 160000L)
        historyRepo.recordPlayback(track2, completionPercent = 0.5f, playDurationMs = 120000L)
        favoritesRepo.addFavorite(track1)

        yourOrbitRepo.getOrbitSummary().test {
            val summary = awaitItem()
            assertThat(summary.totalTracksPlayed).isEqualTo(3)
            assertThat(summary.favoriteTracksCount).isEqualTo(1)
            assertThat(summary.topArtists).contains("Artist Alpha")
            assertThat(summary.heavyRotationTracks).isNotEmpty()
            assertThat(summary.dailyMixTracks).isNotEmpty()
        }
    }

    @Test
    fun discoveryMixReturnsTrendingFromSources() = runTest {
        yourOrbitRepo.getDiscoveryMix().test {
            val mix = awaitItem()
            assertThat(mix).isNotEmpty()
            assertThat(mix.map { it.title }).contains("Track One")
            awaitComplete()
        }
    }

    @Test
    fun downloadRepositoryLifecycle() = runTest {
        downloadRepo.enqueueDownload(track1)
        assertThat(downloadRepo.isTrackDownloaded(track1.compositeKey)).isFalse()

        downloadRepo.updateProgress(
            trackId = track1.compositeKey,
            status = com.saivatsal.soundorbit.core.database.entity.DownloadStatus.DOWNLOADING,
            progress = 50,
            bytesDownloaded = 500000L,
            totalBytes = 1000000L
        )

        downloadRepo.getDownload(track1.compositeKey).test {
            val download = awaitItem()
            assertThat(download).isNotNull()
            assertThat(download?.progress).isEqualTo(50)
            assertThat(download?.status).isEqualTo(com.saivatsal.soundorbit.core.database.entity.DownloadStatus.DOWNLOADING)
        }

        val testFilePath = "/data/user/0/com.saivatsal.soundorbit/files/downloads/track_1.mp3"
        downloadRepo.markCompleted(track1.compositeKey, testFilePath)
        assertThat(downloadRepo.isTrackDownloaded(track1.compositeKey)).isTrue()

        downloadRepo.getCompletedOfflineTracks().test {
            val tracks = awaitItem()
            assertThat(tracks).hasSize(1)
            assertThat(tracks[0].title).isEqualTo("Track One")
        }

        downloadRepo.deleteDownload(track1.compositeKey)
        assertThat(downloadRepo.isTrackDownloaded(track1.compositeKey)).isFalse()
    }
}
