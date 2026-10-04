package com.saivatsal.soundorbit.core.source.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SearchFilter
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceResult
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalFilesSourceTest {

    private lateinit var context: Context
    private lateinit var source: LocalFilesSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        source = LocalFilesSource(context)
    }

    @Test
    fun `metadata matches expected local source properties`() {
        assertThat(source.id).isEqualTo(SourceId.LOCAL)
        assertThat(source.displayName).isEqualTo("Local Files")
        assertThat(source.capabilities.canSearch).isTrue()
        assertThat(source.capabilities.allowsDownloads).isFalse()
        assertThat(source.capabilities.ownsAudioPipeline).isTrue()
    }

    @Test
    fun `resolveStream generates valid android content media URI`() = runTest {
        val result = source.resolveStream("12345", AudioQuality.HIGH)
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val stream = (result as SourceResult.Success).value
        assertThat(stream.uri).isEqualTo("content://media/external/audio/media/12345")
    }

    @Test
    fun `search with empty database returns empty results gracefully`() = runTest {
        val result = source.search("any query", SearchFilter(), PageRequest(0, 20))
        assertThat(result).isInstanceOf(SourceResult.Success::class.java)
        val searchResults = (result as SourceResult.Success).value
        assertThat(searchResults.tracks.items).isEmpty()
        assertThat(searchResults.artists.items).isEmpty()
        assertThat(searchResults.albums.items).isEmpty()
    }
}
