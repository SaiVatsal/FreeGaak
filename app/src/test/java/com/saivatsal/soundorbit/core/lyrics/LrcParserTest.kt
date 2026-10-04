package com.saivatsal.soundorbit.core.lyrics

import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.lyrics.model.Lyrics
import com.saivatsal.soundorbit.core.lyrics.parser.LrcParser
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseStandardLrc() {
        val lrc = """
            [ti:Test Song]
            [ar:Test Artist]
            [00:05.50]First line of lyrics
            [00:10.25]Second line with centiseconds
            [01:05.100]Third line with milliseconds
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertThat(lines).hasSize(3)

        assertThat(lines[0].timestampMs).isEqualTo(5500L)
        assertThat(lines[0].text).isEqualTo("First line of lyrics")

        assertThat(lines[1].timestampMs).isEqualTo(10250L)
        assertThat(lines[1].text).isEqualTo("Second line with centiseconds")

        assertThat(lines[2].timestampMs).isEqualTo(65100L)
        assertThat(lines[2].text).isEqualTo("Third line with milliseconds")
    }

    @Test
    fun parseMultiTimestampLine() {
        val lrc = """
            [00:12.00][00:30.00]Chorus repeating line
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertThat(lines).hasSize(2)
        assertThat(lines[0].timestampMs).isEqualTo(12000L)
        assertThat(lines[0].text).isEqualTo("Chorus repeating line")
        assertThat(lines[1].timestampMs).isEqualTo(30000L)
        assertThat(lines[1].text).isEqualTo("Chorus repeating line")
    }

    @Test
    fun parseWithGlobalOffset() {
        val lrc = """
            [offset:+500]
            [00:02.00]Offsetted by half a second
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertThat(lines).hasSize(1)
        assertThat(lines[0].timestampMs).isEqualTo(2500L)
        assertThat(lines[0].text).isEqualTo("Offsetted by half a second")
    }

    @Test
    fun activeLineIndexBinarySearch() {
        val lrc = """
            [00:05.00]Line 1
            [00:10.00]Line 2
            [00:20.00]Line 3
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        val lyrics = Lyrics(
            trackId = "test_1",
            plainLyrics = null,
            syncedLyrics = lines
        )

        assertThat(lyrics.activeLineIndex(2000L)).isEqualTo(-1)
        assertThat(lyrics.activeLineIndex(5000L)).isEqualTo(0)
        assertThat(lyrics.activeLineIndex(7500L)).isEqualTo(0)
        assertThat(lyrics.activeLineIndex(10000L)).isEqualTo(1)
        assertThat(lyrics.activeLineIndex(15000L)).isEqualTo(1)
        assertThat(lyrics.activeLineIndex(20000L)).isEqualTo(2)
        assertThat(lyrics.activeLineIndex(99999L)).isEqualTo(2)
    }

    @Test
    fun handleEmptyOrCorruptLrc() {
        assertThat(LrcParser.parse("")).isEmpty()
        assertThat(LrcParser.parse(null)).isEmpty()
        assertThat(LrcParser.parse("Just plain text with no tags")).isEmpty()
    }
}
