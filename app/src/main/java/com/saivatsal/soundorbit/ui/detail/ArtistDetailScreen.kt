package com.saivatsal.soundorbit.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.model.Album
import com.saivatsal.soundorbit.core.model.ArtistDetails
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

data class ArtistDetailUiState(
    val isLoading: Boolean = true,
    val details: ArtistDetails? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val favoritesRepository: FavoritesRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<Track>> = favoritesRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadArtist(sourceIdStr: String, artistId: String) {
        viewModelScope.launch {
            _uiState.value = ArtistDetailUiState(isLoading = true)
            try {
                val sourceId = try {
                    SourceId.valueOf(sourceIdStr.uppercase())
                } catch (_: Exception) {
                    SourceId.DEEZER
                }

                val source = sourceRegistry.getSource(sourceId)
                    ?: sourceRegistry.getSource(SourceId.DEEZER)

                val result = source?.artist(artistId)
                if (result is SourceResult.Success) {
                    _uiState.value = ArtistDetailUiState(
                        isLoading = false,
                        details = result.value
                    )
                } else {
                    _uiState.value = ArtistDetailUiState(
                        isLoading = false,
                        errorMessage = "Could not load artist details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ArtistDetailUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load artist"
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
fun ArtistDetailScreen(
    sourceId: String,
    artistId: String,
    onBack: () -> Unit,
    onAlbumClick: (String, String) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(sourceId, artistId) {
        viewModel.loadArtist(sourceId, artistId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.map { it.compositeKey }.toSet() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.details?.artist?.name ?: "Artist", fontWeight = FontWeight.Bold) },
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
                    text = uiState.errorMessage ?: "Artist details unavailable",
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
                // Artist Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        EmeraldGreenBright.copy(alpha = 0.35f),
                                        Color(0xFF0D1812),
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
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!details.artist.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = details.artist.imageUrl,
                                        contentDescription = details.artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = EmeraldGreenBright,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = details.artist.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            if (!details.bio.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = details.bio.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMediumEmphasis,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (details.topTracks.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.playTrackList(details.topTracks, 0) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldGreenBright,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play Top Tracks", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shuffled = details.topTracks.shuffled()
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

                // Discography Albums Section
                if (details.albums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums & Discography",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(details.albums) { album ->
                                AlbumCardItem(
                                    album = album,
                                    onClick = { onAlbumClick(album.sourceId.name, album.sourceAlbumId) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Top Tracks Section Header
                if (details.topTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Popular Tracks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    itemsIndexed(details.topTracks) { index, track ->
                        TrackItem(
                            track = track,
                            isPlaying = false,
                            isFavorite = favoriteIds.contains(track.compositeKey),
                            onClick = { viewModel.playTrackList(details.topTracks, index) },
                            onFavoriteToggle = { viewModel.toggleFavorite(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCardItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        AsyncImage(
            model = album.artworkUrl,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(122.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.trackCount} tracks",
            style = MaterialTheme.typography.labelSmall,
            color = TextMediumEmphasis
        )
    }
}
