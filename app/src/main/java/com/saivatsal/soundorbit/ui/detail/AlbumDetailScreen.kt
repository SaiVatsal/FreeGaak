package com.saivatsal.soundorbit.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.model.AlbumDetails
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.core.source.SourceResult
import com.saivatsal.soundorbit.ui.component.TrackItem
import com.saivatsal.soundorbit.ui.theme.DarkSurfaceVariant
import com.saivatsal.soundorbit.ui.theme.EmeraldGreenBright
import com.saivatsal.soundorbit.ui.theme.OledBlack
import com.saivatsal.soundorbit.ui.theme.TextMediumEmphasis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val details: AlbumDetails? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val favoritesRepository: FavoritesRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<Track>> = favoritesRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadAlbum(sourceIdStr: String, albumId: String) {
        viewModelScope.launch {
            _uiState.value = AlbumDetailUiState(isLoading = true)
            try {
                val sourceId = try {
                    SourceId.valueOf(sourceIdStr.uppercase())
                } catch (_: Exception) {
                    SourceId.DEEZER
                }

                val source = sourceRegistry.getSource(sourceId)
                    ?: sourceRegistry.getSource(SourceId.DEEZER)

                val result = source?.album(albumId)
                if (result is SourceResult.Success) {
                    _uiState.value = AlbumDetailUiState(
                        isLoading = false,
                        details = result.value
                    )
                } else {
                    _uiState.value = AlbumDetailUiState(
                        isLoading = false,
                        errorMessage = "Could not load album details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AlbumDetailUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load album"
                )
            }
        }
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            viewModelScope.launch {
                audioPlayer.playTrack(tracks[startIndex], tracks, startIndex)
            }
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(track)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    sourceId: String,
    albumId: String,
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(sourceId, albumId) {
        viewModel.loadAlbum(sourceId, albumId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.map { it.compositeKey }.toSet() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.details?.album?.name ?: "Album", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = OledBlack,
        modifier = modifier
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldGreenBright)
            }
        } else if (uiState.details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Album details unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMediumEmphasis
                )
            }
        } else {
            val details = uiState.details!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Album Banner Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        EmeraldGreenBright.copy(alpha = 0.3f),
                                        Color(0xFF0F1713),
                                        OledBlack
                                    )
                                )
                            )
                            .padding(top = padding.calculateTopPadding())
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!details.album.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = details.album.artworkUrl,
                                        contentDescription = details.album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = EmeraldGreenBright,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = details.album.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = details.album.artistName,
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldGreenBright,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${details.tracks.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumEmphasis
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (details.tracks.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.playTrackList(details.tracks, 0) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreenBright,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Album", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shuffled = details.tracks.shuffled()
                                            viewModel.playTrackList(shuffled, 0)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                itemsIndexed(details.tracks) { index, track ->
                    TrackItem(
                        track = track,
                        isPlaying = false,
                        isFavorite = favoriteIds.contains(track.compositeKey),
                        onClick = { viewModel.playTrackList(details.tracks, index) },
                        onFavoriteToggle = { viewModel.toggleFavorite(track) }
                    )
                }
            }
        }
    }
}
