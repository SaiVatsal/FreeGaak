package com.saivatsal.soundorbit.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.database.dao.DownloadDao
import com.saivatsal.soundorbit.core.database.dao.FavoriteDao
import com.saivatsal.soundorbit.core.database.dao.ListeningHistoryDao
import com.saivatsal.soundorbit.core.database.dao.PlaylistDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.DownloadEntity
import com.saivatsal.soundorbit.core.database.entity.DownloadStatus
import com.saivatsal.soundorbit.core.database.entity.FavoriteEntity
import com.saivatsal.soundorbit.core.database.entity.ListeningHistoryEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistTrackCrossRef
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
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
class RoomDatabaseTest {

    private lateinit var db: SoundOrbitDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var playlistDao: PlaylistDao
    private lateinit var listeningHistoryDao: ListeningHistoryDao
    private lateinit var downloadDao: DownloadDao

    private val sampleTrack = TrackEntity(
        id = "AUDIUS_101",
        sourceId = "AUDIUS",
        sourceTrackId = "101",
        title = "Starlight Odyssey",
        titleSortKey = "starlight odyssey",
        artistName = "Cosmic Dreamer",
        albumName = "Nebula",
        durationMs = 210000L,
        artworkUrl = "https://example.com/art.jpg",
        licenseName = "CC BY-SA",
        licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
        downloadAllowed = true
    )

    private val sampleTrack2 = TrackEntity(
        id = "JAMENDO_202",
        sourceId = "JAMENDO",
        sourceTrackId = "202",
        title = "Solar Flare",
        titleSortKey = "solar flare",
        artistName = "Cosmic Dreamer",
        albumName = "Solaris",
        durationMs = 180000L,
        artworkUrl = null,
        licenseName = "CC BY",
        licenseUrl = null,
        downloadAllowed = false
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SoundOrbitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trackDao = db.trackDao()
        favoriteDao = db.favoriteDao()
        playlistDao = db.playlistDao()
        listeningHistoryDao = db.listeningHistoryDao()
        downloadDao = db.downloadDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetTrack() = runTest {
        trackDao.insertOrUpdate(sampleTrack)
        val loaded = trackDao.getTrackById("AUDIUS_101")
        assertThat(loaded).isNotNull()
        assertThat(loaded?.title).isEqualTo("Starlight Odyssey")
        assertThat(loaded?.artistName).isEqualTo("Cosmic Dreamer")
    }

    @Test
    fun favoriteToggleAndQuery() = runTest {
        trackDao.insertOrUpdate(sampleTrack)
        assertThat(favoriteDao.isFavorite("AUDIUS_101")).isFalse()

        favoriteDao.insertFavorite(FavoriteEntity(trackId = "AUDIUS_101"))
        assertThat(favoriteDao.isFavorite("AUDIUS_101")).isTrue()

        favoriteDao.getFavoriteTracks().test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo("AUDIUS_101")
        }

        favoriteDao.deleteFavorite("AUDIUS_101")
        assertThat(favoriteDao.isFavorite("AUDIUS_101")).isFalse()
    }

    @Test
    fun playlistCreationAndTrackReordering() = runTest {
        trackDao.insertOrUpdateAll(listOf(sampleTrack, sampleTrack2))

        val playlist = PlaylistEntity(
            id = "pl_1",
            name = "Chill Vibes",
            description = "Evening ambient",
            coverArtworkUri = null
        )
        playlistDao.insertPlaylist(playlist)

        playlistDao.insertTrackToPlaylist(
            PlaylistTrackCrossRef(playlistId = "pl_1", trackId = "AUDIUS_101", position = 0)
        )
        playlistDao.insertTrackToPlaylist(
            PlaylistTrackCrossRef(playlistId = "pl_1", trackId = "JAMENDO_202", position = 1)
        )

        playlistDao.getTracksForPlaylist("pl_1").test {
            val tracks = awaitItem()
            assertThat(tracks).hasSize(2)
            assertThat(tracks[0].id).isEqualTo("AUDIUS_101")
            assertThat(tracks[1].id).isEqualTo("JAMENDO_202")
        }

        // Reorder
        playlistDao.reorderPlaylistTracks("pl_1", listOf("JAMENDO_202", "AUDIUS_101"))

        playlistDao.getTracksForPlaylist("pl_1").test {
            val reordered = awaitItem()
            assertThat(reordered).hasSize(2)
            assertThat(reordered[0].id).isEqualTo("JAMENDO_202")
            assertThat(reordered[1].id).isEqualTo("AUDIUS_101")
        }
    }

    @Test
    fun cascadeDeleteTrackRemovesFavoriteAndPlaylistEntry() = runTest {
        trackDao.insertOrUpdate(sampleTrack)
        favoriteDao.insertFavorite(FavoriteEntity(trackId = "AUDIUS_101"))

        val playlist = PlaylistEntity(id = "pl_2", name = "Test")
        playlistDao.insertPlaylist(playlist)
        playlistDao.insertTrackToPlaylist(
            PlaylistTrackCrossRef(playlistId = "pl_2", trackId = "AUDIUS_101", position = 0)
        )

        // Delete track
        trackDao.deleteTrackById("AUDIUS_101")

        assertThat(favoriteDao.isFavorite("AUDIUS_101")).isFalse()
        playlistDao.getTracksForPlaylist("pl_2").test {
            val list = awaitItem()
            assertThat(list).isEmpty()
        }
    }

    @Test
    fun listeningHistoryAndTopArtists() = runTest {
        trackDao.insertOrUpdateAll(listOf(sampleTrack, sampleTrack2))

        listeningHistoryDao.insertHistory(
            ListeningHistoryEntity(trackId = "AUDIUS_101", playedAt = 1000L, completionPercent = 1.0f, playDurationMs = 210000L)
        )
        listeningHistoryDao.insertHistory(
            ListeningHistoryEntity(trackId = "JAMENDO_202", playedAt = 2000L, completionPercent = 0.8f, playDurationMs = 150000L)
        )

        listeningHistoryDao.getTopArtists(sinceTimestamp = 0).test {
            val top = awaitItem()
            assertThat(top).hasSize(1) // Both tracks are from "Cosmic Dreamer"
            assertThat(top[0].artistName).isEqualTo("Cosmic Dreamer")
            assertThat(top[0].playCount).isEqualTo(2)
            assertThat(top[0].totalDurationMs).isEqualTo(360000L)
        }

        listeningHistoryDao.getRecentlyPlayedTracks(limit = 10).test {
            val recents = awaitItem()
            assertThat(recents).hasSize(2)
            assertThat(recents[0].id).isEqualTo("JAMENDO_202") // More recent playedAt (2000 > 1000)
        }
    }

    @Test
    fun downloadStatusLifecycle() = runTest {
        trackDao.insertOrUpdate(sampleTrack)

        val download = DownloadEntity(
            trackId = "AUDIUS_101",
            status = DownloadStatus.QUEUED,
            progress = 0
        )
        downloadDao.insertOrUpdateDownload(download)

        downloadDao.getDownloadFlow("AUDIUS_101").test {
            val item = awaitItem()
            assertThat(item?.status).isEqualTo(DownloadStatus.QUEUED)
        }

        downloadDao.updateProgress("AUDIUS_101", DownloadStatus.DOWNLOADING, 50, 500000L, 1000000L)

        downloadDao.getDownloadFlow("AUDIUS_101").test {
            val item = awaitItem()
            assertThat(item?.status).isEqualTo(DownloadStatus.DOWNLOADING)
            assertThat(item?.progress).isEqualTo(50)
            assertThat(item?.bytesDownloaded).isEqualTo(500000L)
        }

        downloadDao.markCompleted("AUDIUS_101", localFilePath = "/storage/emulated/0/Music/101.mp3")

        downloadDao.getCompletedOfflineTracks().test {
            val offline = awaitItem()
            assertThat(offline).hasSize(1)
            assertThat(offline[0].id).isEqualTo("AUDIUS_101")
        }
    }
}
