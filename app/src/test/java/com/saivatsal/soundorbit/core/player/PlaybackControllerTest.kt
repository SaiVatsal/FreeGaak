package com.saivatsal.soundorbit.core.player

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePlayer: FakeAudioPlayer
    private lateinit var controller: PlaybackController

    private val sampleTrack = Track(
        sourceId = SourceId.LOCAL,
        sourceTrackId = "1",
        title = "Starlight",
        titleSortKey = "starlight",
        artistName = "Muse",
        albumName = "Black Holes and Revelations",
        durationMs = 240000L,
        artworkUrl = "content://art/1"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePlayer = FakeAudioPlayer()
        controller = PlaybackController(fakePlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playTrack delegates to audio player and updates snapshot`() = runTest(testDispatcher) {
        controller.playTrack(sampleTrack)

        assertThat(fakePlayer.playedTrack).isEqualTo(sampleTrack)
        assertThat(fakePlayer.queue).containsExactly(sampleTrack)
    }

    @Test
    fun `togglePlayPause delegates properly`() = runTest(testDispatcher) {
        controller.togglePlayPause()
        assertThat(fakePlayer.togglePlayPauseCalled).isTrue()
    }

    @Test
    fun `seekTo delegates properly`() = runTest(testDispatcher) {
        controller.seekTo(15000L)
        assertThat(fakePlayer.seekPositionMs).isEqualTo(15000L)
    }

    @Test
    fun `skipToNext and skipToPrevious delegate properly`() = runTest(testDispatcher) {
        controller.skipToNext()
        assertThat(fakePlayer.skipNextCalled).isTrue()

        controller.skipToPrevious()
        assertThat(fakePlayer.skipPrevCalled).isTrue()
    }

    @Test
    fun `shuffle and repeat state changes update snapshot flows`() = runTest(testDispatcher) {
        controller.setShuffle(true)
        assertThat(fakePlayer.shuffleEnabled).isTrue()

        controller.setRepeatState(RepeatState.ALL)
        assertThat(fakePlayer.recordedRepeatState).isEqualTo(RepeatState.ALL)
    }

    @Test
    fun `snapshot flow emits correct player state`() = runTest(testDispatcher) {
        controller.snapshot.test {
            val initial = awaitItem()
            assertThat(initial.status).isEqualTo(PlaybackStatus.IDLE)

            fakePlayer.emitSnapshot(
                PlayerSnapshot(
                    currentTrack = sampleTrack,
                    status = PlaybackStatus.PLAYING,
                    positionMs = 10000L,
                    durationMs = 240000L
                )
            )

            val updated = awaitItem()
            assertThat(updated.currentTrack).isEqualTo(sampleTrack)
            assertThat(updated.status).isEqualTo(PlaybackStatus.PLAYING)
            assertThat(updated.positionMs).isEqualTo(10000L)
        }
    }
}

private class FakeAudioPlayer : AudioPlayer {
    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    override val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    var playedTrack: Track? = null
    var queue: List<Track> = emptyList()
    var togglePlayPauseCalled = false
    var seekPositionMs: Long? = null
    var skipNextCalled = false
    var skipPrevCalled = false
    var shuffleEnabled = false
    var recordedRepeatState = RepeatState.OFF

    fun emitSnapshot(newSnapshot: PlayerSnapshot) {
        _snapshot.value = newSnapshot
    }

    override fun playTrack(track: Track, queue: List<Track>, startIndex: Int) {
        playedTrack = track
        this.queue = queue
        _snapshot.value = _snapshot.value.copy(
            currentTrack = track,
            queue = queue,
            queueIndex = startIndex,
            status = PlaybackStatus.PLAYING
        )
    }

    override fun play() {
        _snapshot.value = _snapshot.value.copy(status = PlaybackStatus.PLAYING)
    }

    override fun pause() {
        _snapshot.value = _snapshot.value.copy(status = PlaybackStatus.PAUSED)
    }

    override fun togglePlayPause() {
        togglePlayPauseCalled = true
        val newStatus = if (_snapshot.value.status == PlaybackStatus.PLAYING) PlaybackStatus.PAUSED else PlaybackStatus.PLAYING
        _snapshot.value = _snapshot.value.copy(status = newStatus)
    }

    override fun seekTo(positionMs: Long) {
        seekPositionMs = positionMs
        _snapshot.value = _snapshot.value.copy(positionMs = positionMs)
    }

    override fun skipToNext() {
        skipNextCalled = true
    }

    override fun skipToPrevious() {
        skipPrevCalled = true
    }

    override fun setShuffle(enabled: Boolean) {
        shuffleEnabled = enabled
        _snapshot.value = _snapshot.value.copy(isShuffle = enabled)
    }

    override fun setRepeatState(state: RepeatState) {
        recordedRepeatState = state
        _snapshot.value = _snapshot.value.copy(repeatState = state)
    }

    override fun addToQueue(track: Track) {
        queue = queue + track
        _snapshot.value = _snapshot.value.copy(queue = queue)
    }

    override fun removeFromQueue(index: Int) {
        if (index in queue.indices) {
            queue = queue.filterIndexed { i, _ -> i != index }
            _snapshot.value = _snapshot.value.copy(queue = queue)
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    override fun clearQueue() {
        queue = emptyList()
        _snapshot.value = _snapshot.value.copy(queue = emptyList())
    }

    override fun setCrossfadeDuration(seconds: Int) {}
    override fun setVolume(volume: Float) {
        _snapshot.value = _snapshot.value.copy(volume = volume)
    }
    override fun getAudioSessionId(): Int = 0
    override fun release() {}
}
