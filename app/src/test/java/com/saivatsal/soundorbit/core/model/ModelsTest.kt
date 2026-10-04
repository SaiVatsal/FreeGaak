package com.saivatsal.soundorbit.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun `track composite key formats correctly`() {
        val track = Track(
            sourceId = SourceId.AUDIUS,
            sourceTrackId = "D72kA",
            title = "Test Track",
            titleSortKey = "test track",
            artistName = "Artist",
            durationMs = 180000L
        )

        assertEquals("AUDIUS_D72kA", track.compositeKey)
    }

    @Test
    fun `album composite key formats correctly`() {
        val album = Album(
            sourceId = SourceId.JAMENDO,
            sourceAlbumId = "alb_991",
            name = "Chill Beats",
            artistName = "Lo-Fi Producer"
        )

        assertEquals("JAMENDO_alb_991", album.compositeKey)
    }

    @Test
    fun `artist composite key formats correctly`() {
        val artist = Artist(
            sourceId = SourceId.AUDIUS,
            sourceArtistId = "art_55",
            name = "Skrillex"
        )

        assertEquals("AUDIUS_art_55", artist.compositeKey)
    }

    @Test
    fun `playlist composite key formats correctly`() {
        val playlist = Playlist(
            sourceId = SourceId.JAMENDO,
            sourcePlaylistId = "pl_102",
            name = "Best of Ambient"
        )

        assertEquals("JAMENDO_pl_102", playlist.compositeKey)
    }
}
