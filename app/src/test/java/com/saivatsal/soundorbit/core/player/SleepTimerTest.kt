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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePlayer: FakeAudioPlayerForTimer
    private lateinit var sleepTimer: SleepTimer

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePlayer = FakeAudioPlayerForTimer()
        sleepTimer = SleepTimer(fakePlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startTimer sets state and counts down`() = runTest(testDispatcher) {
        sleepTimer.state.test {
            val initial = awaitItem()
            assertThat(initial.isActive).isFalse()

            sleepTimer.startTimer(minutes = 1, fadeOut = false)

            val started = awaitItem()
            assertThat(started.isActive).isTrue()
            assertThat(started.totalSeconds).isEqualTo(60L)
            assertThat(started.remainingSeconds).isEqualTo(60L)

            // Advance by 10 seconds
            advanceTimeBy(10000L)
            testScheduler.runCurrent()

            val ticked = expectMostRecentItem()
            assertThat(ticked.remainingSeconds).isEqualTo(50L)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startEndOfTrack activates end of track mode and pauses on track ended`() = runTest(testDispatcher) {
        sleepTimer.startEndOfTrack()
        assertThat(sleepTimer.state.value.isActive).isTrue()
        assertThat(sleepTimer.state.value.isEndOfTrack).isTrue()

        sleepTimer.onTrackEnded()

        assertThat(fakePlayer.pauseCalled).isTrue()
        assertThat(sleepTimer.state.value.isActive).isFalse()
    }

    @Test
    fun `cancelTimer resets state`() = runTest(testDispatcher) {
        sleepTimer.startTimer(10)
        assertThat(sleepTimer.state.value.isActive).isTrue()

        sleepTimer.cancelTimer()
        assertThat(sleepTimer.state.value.isActive).isFalse()
        assertThat(sleepTimer.state.value.remainingSeconds).isEqualTo(0L)
    }
}

private class FakeAudioPlayerForTimer : AudioPlayer {
    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    override val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    var pauseCalled = false
    var currentVolume = 1.0f

    override fun playTrack(track: Track, queue: List<Track>, startIndex: Int) {}
    override fun play() {}
    override fun pause() {
        pauseCalled = true
        _snapshot.value = _snapshot.value.copy(status = PlaybackStatus.PAUSED)
    }
    override fun togglePlayPause() {}
    override fun seekTo(positionMs: Long) {}
    override fun skipToNext() {}
    override fun skipToPrevious() {}
    override fun setShuffle(enabled: Boolean) {}
    override fun setRepeatState(state: RepeatState) {}
    override fun addToQueue(track: Track) {}
    override fun removeFromQueue(index: Int) {}
    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {}
    override fun clearQueue() {}
    override fun setCrossfadeDuration(seconds: Int) {}
    override fun setVolume(volume: Float) {
        currentVolume = volume
        _snapshot.value = _snapshot.value.copy(volume = volume)
    }
    override fun getAudioSessionId(): Int = 0
    override fun release() {}
}
