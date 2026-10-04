package com.saivatsal.soundorbit.core.player

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.saivatsal.soundorbit.core.database.dao.DownloadDao
import com.saivatsal.soundorbit.core.database.entity.DownloadStatus
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.core.source.SourceResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@OptIn(UnstableApi::class)
@Singleton
class CrossfadePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceRegistry: SourceRegistry,
    private val downloadDao: DownloadDao
) : AudioPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    override val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    private var crossfadeDurationSec = 0
    private var isCrossfading = false

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var playerA: ExoPlayer = createExoPlayer()
    private var playerB: ExoPlayer = createExoPlayer()

    private var activePlayer: ExoPlayer = playerA
    private var standbyPlayer: ExoPlayer = playerB

    private var tickerJob: Job? = null
    private var fadeJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private var isReceiverRegistered = false

    init {
        setupPlayerListener(playerA)
        setupPlayerListener(playerB)
        startPositionTicker()
    }

    private fun createExoPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private fun setupPlayerListener(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (player != activePlayer) return
                when (playbackState) {
                    Player.STATE_BUFFERING -> _snapshot.update { it.copy(status = PlaybackStatus.BUFFERING) }
                    Player.STATE_READY -> {
                        val duration = max(0L, player.duration)
                        _snapshot.update {
                            it.copy(
                                status = if (player.isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
                                durationMs = duration
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (!isCrossfading) {
                            handleTrackEnded()
                        }
                    }
                    Player.STATE_IDLE -> {
                        _snapshot.update { it.copy(status = PlaybackStatus.IDLE) }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player != activePlayer) return
                _snapshot.update {
                    it.copy(status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED)
                }
            }
        })
    }

    override fun playTrack(track: Track, queue: List<Track>, startIndex: Int) {
        if (!requestAudioFocus()) return
        registerNoisyReceiver()

        val validQueue = if (queue.isEmpty()) listOf(track) else queue
        val index = if (startIndex in validQueue.indices) startIndex else validQueue.indexOf(track).coerceAtLeast(0)
        val selectedTrack = validQueue[index]

        _snapshot.update {
            it.copy(
                currentTrack = selectedTrack,
                queue = validQueue,
                queueIndex = index,
                status = PlaybackStatus.BUFFERING,
                positionMs = 0L,
                durationMs = selectedTrack.durationMs
            )
        }

        stopStandbyPlayer()
        activePlayer.volume = 1.0f

        scope.launch {
            val streamUri = resolveTrackUri(selectedTrack)
            val mediaItem = buildMediaItem(selectedTrack, streamUri)
            activePlayer.setMediaItem(mediaItem)
            activePlayer.prepare()
            activePlayer.play()
        }
    }

    override fun play() {
        if (!requestAudioFocus()) return
        registerNoisyReceiver()
        fadeJob?.cancel()
        fadeJob = scope.launch {
            activePlayer.volume = 0f
            activePlayer.play()
            smoothFadeVolume(activePlayer, 0f, 1f, 150)
        }
    }

    override fun pause() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            smoothFadeVolume(activePlayer, activePlayer.volume, 0f, 150)
            activePlayer.pause()
            activePlayer.volume = 1.0f
            _snapshot.update { it.copy(status = PlaybackStatus.PAUSED) }
        }
    }

    override fun togglePlayPause() {
        if (activePlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        activePlayer.seekTo(positionMs)
        _snapshot.update { it.copy(positionMs = positionMs) }
    }

    override fun skipToNext() {
        val snap = _snapshot.value
        if (snap.queue.isEmpty()) return

        val nextIndex = getNextTrackIndex(snap)
        if (nextIndex != -1) {
            playTrack(snap.queue[nextIndex], snap.queue, nextIndex)
        } else {
            activePlayer.stop()
            _snapshot.update { it.copy(status = PlaybackStatus.ENDED, positionMs = 0L) }
        }
    }

    override fun skipToPrevious() {
        val snap = _snapshot.value
        if (snap.queue.isEmpty()) return

        if (activePlayer.currentPosition > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = getPreviousTrackIndex(snap)
        if (prevIndex != -1) {
            playTrack(snap.queue[prevIndex], snap.queue, prevIndex)
        } else {
            seekTo(0L)
        }
    }

    override fun setShuffle(enabled: Boolean) {
        _snapshot.update { it.copy(isShuffle = enabled) }
    }

    override fun setRepeatState(state: RepeatState) {
        _snapshot.update { it.copy(repeatState = state) }
    }

    override fun addToQueue(track: Track) {
        _snapshot.update {
            val newQueue = it.queue + track
            it.copy(queue = newQueue)
        }
    }

    override fun removeFromQueue(index: Int) {
        _snapshot.update {
            if (index !in it.queue.indices) return@update it
            val newQueue = it.queue.toMutableList().apply { removeAt(index) }
            val newIndex = when {
                newQueue.isEmpty() -> -1
                index < it.queueIndex -> it.queueIndex - 1
                index == it.queueIndex -> it.queueIndex.coerceAtMost(newQueue.size - 1)
                else -> it.queueIndex
            }
            it.copy(queue = newQueue, queueIndex = newIndex)
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _snapshot.update {
            if (fromIndex !in it.queue.indices || toIndex !in it.queue.indices) return@update it
            val newQueue = it.queue.toMutableList()
            val item = newQueue.removeAt(fromIndex)
            newQueue.add(toIndex, item)
            val newCurrentIndex = when {
                it.queueIndex == fromIndex -> toIndex
                fromIndex < it.queueIndex && toIndex >= it.queueIndex -> it.queueIndex - 1
                fromIndex > it.queueIndex && toIndex <= it.queueIndex -> it.queueIndex + 1
                else -> it.queueIndex
            }
            it.copy(queue = newQueue, queueIndex = newCurrentIndex)
        }
    }

    override fun clearQueue() {
        activePlayer.stop()
        stopStandbyPlayer()
        _snapshot.update {
            PlayerSnapshot(
                isShuffle = it.isShuffle,
                repeatState = it.repeatState
            )
        }
    }

    override fun setCrossfadeDuration(seconds: Int) {
        crossfadeDurationSec = seconds.coerceIn(0, 12)
    }

    private fun getNextTrackIndex(snap: PlayerSnapshot): Int {
        if (snap.queue.isEmpty()) return -1
        if (snap.repeatState == RepeatState.ONE) return snap.queueIndex

        if (snap.isShuffle) {
            val validIndices = snap.queue.indices.filter { it != snap.queueIndex }
            return if (validIndices.isNotEmpty()) validIndices.random() else snap.queueIndex
        }

        val next = snap.queueIndex + 1
        return if (next < snap.queue.size) {
            next
        } else if (snap.repeatState == RepeatState.ALL) {
            0
        } else {
            -1
        }
    }

    private fun getPreviousTrackIndex(snap: PlayerSnapshot): Int {
        if (snap.queue.isEmpty()) return -1
        val prev = snap.queueIndex - 1
        return if (prev >= 0) {
            prev
        } else if (snap.repeatState == RepeatState.ALL) {
            snap.queue.size - 1
        } else {
            -1
        }
    }

    private fun handleTrackEnded() {
        val snap = _snapshot.value
        if (snap.repeatState == RepeatState.ONE) {
            seekTo(0L)
            play()
            return
        }
        skipToNext()
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                if (activePlayer.isPlaying) {
                    val pos = activePlayer.currentPosition
                    val dur = max(0L, activePlayer.duration)
                    val buffered = activePlayer.bufferedPosition

                    _snapshot.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = if (dur > 0) dur else it.durationMs,
                            bufferedPositionMs = buffered
                        )
                    }

                    checkCrossfadeTrigger(pos, dur)
                }
                delay(250L)
            }
        }
    }

    private fun checkCrossfadeTrigger(currentPos: Long, duration: Long) {
        if (crossfadeDurationSec <= 0 || isCrossfading || duration <= 0) return

        val remainingMs = duration - currentPos
        val crossfadeMs = crossfadeDurationSec * 1000L

        if (remainingMs in 1..crossfadeMs) {
            val snap = _snapshot.value
            val nextIndex = getNextTrackIndex(snap)
            if (nextIndex != -1 && nextIndex != snap.queueIndex) {
                triggerCrossfade(snap.queue[nextIndex], nextIndex, remainingMs)
            }
        }
    }

    private fun triggerCrossfade(nextTrack: Track, nextIndex: Int, fadeDurationMs: Long) {
        isCrossfading = true
        standbyPlayer.volume = 0f

        scope.launch {
            val streamUri = resolveTrackUri(nextTrack)
            val mediaItem = buildMediaItem(nextTrack, streamUri)
            standbyPlayer.setMediaItem(mediaItem)
            standbyPlayer.prepare()
            standbyPlayer.play()

            val steps = 20
            val interval = max(10L, fadeDurationMs / steps)
            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                activePlayer.volume = 1f - progress
                standbyPlayer.volume = progress
                delay(interval)
            }

            activePlayer.stop()
            activePlayer.volume = 1.0f

            // Swap players
            val temp = activePlayer
            activePlayer = standbyPlayer
            standbyPlayer = temp

            _snapshot.update {
                it.copy(
                    currentTrack = nextTrack,
                    queueIndex = nextIndex,
                    positionMs = 0L,
                    durationMs = nextTrack.durationMs
                )
            }
            isCrossfading = false
        }
    }

    private suspend fun resolveTrackUri(track: Track): String {
        try {
            val download = downloadDao.getDownload(track.compositeKey)
            if (download?.status == DownloadStatus.COMPLETED && !download.localFilePath.isNullOrBlank()) {
                val file = File(download.localFilePath)
                if (file.exists() && file.length() > 0) {
                    return android.net.Uri.fromFile(file).toString()
                }
            }
        } catch (_: Exception) {}

        val streamResult = sourceRegistry.getSource(track.sourceId)?.resolveStream(track.sourceTrackId, AudioQuality.HIGH)
        return (streamResult as? SourceResult.Success)?.value?.uri
            ?: when (track.sourceId) {
                SourceId.LOCAL -> {
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        track.sourceTrackId.toLongOrNull() ?: 0L
                    ).toString()
                }
                else -> track.sourceTrackId
            }
    }

    private suspend fun smoothFadeVolume(player: ExoPlayer, from: Float, to: Float, durationMs: Long) {
        val steps = 15
        val interval = durationMs / steps
        for (i in 0..steps) {
            val progress = i.toFloat() / steps
            player.volume = from + (to - from) * progress
            delay(interval)
        }
        player.volume = to
    }

    private fun buildMediaItem(track: Track, streamUri: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(track.albumName)
            .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setUri(streamUri)
            .setMediaId(track.compositeKey)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun stopStandbyPlayer() {
        standbyPlayer.stop()
        standbyPlayer.clearMediaItems()
        standbyPlayer.volume = 1.0f
        isCrossfading = false
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AndroidAudioAttributes.Builder()
                .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
                .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> activePlayer.volume = 0.3f
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            activePlayer.volume = 1.0f
                            play()
                        }
                    }
                }
                .build()
            audioFocusRequest = request
            return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        pause()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun registerNoisyReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(becomingNoisyReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        activePlayer.volume = clamped
        _snapshot.update { it.copy(volume = clamped) }
    }

    override fun getAudioSessionId(): Int {
        return activePlayer.audioSessionId
    }

    fun getActivePlayerInstance(): ExoPlayer = activePlayer

    override fun release() {
        unregisterNoisyReceiver()
        tickerJob?.cancel()
        fadeJob?.cancel()
        scope.cancel()
        playerA.release()
        playerB.release()
    }
}
