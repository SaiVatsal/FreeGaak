package com.saivatsal.soundorbit.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.player.PlaybackStatus
import com.saivatsal.soundorbit.core.player.RepeatState
import com.saivatsal.soundorbit.ui.component.*
import com.saivatsal.soundorbit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsState()
    val equalizerState by viewModel.equalizerState.collectAsState()
    val sleepTimerState by viewModel.sleepTimerState.collectAsState()
    val lyricsUiState by viewModel.lyricsUiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var isLyricsViewActive by remember { mutableStateOf(false) }

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekSliderPosition by remember { mutableFloatStateOf(0f) }

    val track = playerState.currentTrack

    if (track == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(OledBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("No track currently loaded", color = Color.White)
        }
        return
    }

    val currentPosition = if (isUserSeeking) {
        (seekSliderPosition * playerState.durationMs).toLong()
    } else {
        playerState.positionMs
    }

    val sliderValue = if (playerState.durationMs > 0) {
        if (isUserSeeking) seekSliderPosition else (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            DarkSurfaceVariant,
            ObsidianBlack,
            OledBlack
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumEmphasis,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.sourceId.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenBright
                    )
                }

                Row {
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerState.isActive) EmeraldGreenBright else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle View: Toggle between Album Art & Synced Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = isLyricsViewActive, label = "ViewMode") { showLyrics ->
                    if (showLyrics) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, DarkBorderSubtle, RoundedCornerShape(20.dp))
                                .padding(14.dp)
                        ) {
                            when (val state = lyricsUiState) {
                                is LyricsUiState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = EmeraldGreenBright)
                                    }
                                }
                                is LyricsUiState.Success -> {
                                    LyricsView(
                                        lyrics = state.lyrics,
                                        isLoading = false,
                                        currentPositionMs = currentPosition,
                                        onSeekTo = { viewModel.seekTo(it) }
                                    )
                                }
                                is LyricsUiState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.MusicOff,
                                                contentDescription = null,
                                                tint = TextMediumEmphasis,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Lyrics not available for this track",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextMediumEmphasis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                LyricsUiState.Idle -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Tap to load lyrics",
                                            color = TextMediumEmphasis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Album Art Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp), ambientColor = EmeraldGreenBright)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(24.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Information & Favorite Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMediumEmphasis,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SourceBadge(sourceId = track.sourceId)
                    }
                    if (playerState.status == PlaybackStatus.ERROR && !playerState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playerState.errorMessage ?: "Playback failed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) EmeraldGreenBright else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Seekbar Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isUserSeeking = true
                        seekSliderPosition = it
                    },
                    onValueChangeFinished = {
                        val seekTargetMs = (seekSliderPosition * playerState.durationMs).toLong()
                        viewModel.seekTo(seekTargetMs)
                        isUserSeeking = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldGreenBright,
                        activeTrackColor = EmeraldGreenBright,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumEmphasis
                    )
                    Text(
                        text = formatTime(playerState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumEmphasis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.isShuffle) EmeraldGreenBright else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Track Button
                IconButton(
                    onClick = { viewModel.previous() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Floating Circle
                val isBuffering = playerState.status == PlaybackStatus.BUFFERING
                val isPlaying = playerState.status == PlaybackStatus.PLAYING
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreenBright)
                        .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = EmeraldGreenBright),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(68.dp)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Next Track Button
                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = {
                        val nextMode = when (playerState.repeatState) {
                            RepeatState.OFF -> RepeatState.ALL
                            RepeatState.ALL -> RepeatState.ONE
                            RepeatState.ONE -> RepeatState.OFF
                        }
                        viewModel.setRepeatMode(nextMode)
                    }
                ) {
                    val icon = when (playerState.repeatState) {
                        RepeatState.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val tint = when (playerState.repeatState) {
                        RepeatState.OFF -> Color.LightGray
                        else -> EmeraldGreenBright
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Auxiliary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Synced Lyrics Toggle Button
                IconButton(onClick = { isLyricsViewActive = !isLyricsViewActive }) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (isLyricsViewActive) EmeraldGreenBright else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Equalizer Button
                IconButton(onClick = { showEqualizerSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = if (equalizerState.isEnabled) EmeraldGreenBright else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Add to Playlist Button
                IconButton(onClick = { showAddToPlaylistDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Equalizer Modal Bottom Sheet
        if (showEqualizerSheet) {
            EqualizerSheet(
                state = equalizerState,
                onDismiss = { showEqualizerSheet = false },
                onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                onSelectPreset = { viewModel.selectEqualizerPreset(it) },
                onBandChange = { band, level -> viewModel.setEqualizerBandLevel(band, level) },
                onBassBoostChange = { viewModel.setBassBoost(it) },
                onVirtualizerChange = { viewModel.setVirtualizer(it) }
            )
        }

        // Queue Modal Bottom Sheet
        if (showQueueSheet) {
            QueueSheet(
                queue = playerState.queue,
                currentIndex = playerState.queueIndex,
                onDismiss = { showQueueSheet = false },
                onTrackSelected = { viewModel.playTrackFromQueue(it) },
                onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                onClearQueue = { viewModel.clearQueue() }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                state = sleepTimerState,
                onDismiss = { showSleepTimerDialog = false },
                onStartTimer = { mins, fade -> viewModel.startSleepTimer(mins, fade) },
                onStartEndOfTrack = { viewModel.startSleepTimerEndOfTrack() },
                onCancelTimer = { viewModel.cancelSleepTimer() }
            )
        }

        // Add to Playlist Dialog
        if (showAddToPlaylistDialog) {
            AddToPlaylistDialog(
                track = track,
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialog = false },
                onAddToPlaylist = { playlistId -> viewModel.addTrackToPlaylist(playlistId, track) },
                onCreateAndAdd = { name -> viewModel.createPlaylistAndAddTrack(name, track) }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
