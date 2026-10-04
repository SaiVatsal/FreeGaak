package com.saivatsal.soundorbit.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.model.TrendingWindow
import com.saivatsal.soundorbit.core.player.AudioPlayer
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceRegistry
import com.saivatsal.soundorbit.ui.component.TrackItem
import com.saivatsal.soundorbit.ui.theme.EmeraldGreenBright
import com.saivatsal.soundorbit.ui.theme.OledBlack
import com.saivatsal.soundorbit.ui.theme.TextMediumEmphasis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class LanguageDetailUiState(
    val isLoading: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class LanguageDetailViewModel @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val favoritesRepository: FavoritesRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageDetailUiState())
    val uiState: StateFlow<LanguageDetailUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<Track>> = favoritesRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadLanguage(languageCode: String) {
        viewModelScope.launch {
            _uiState.value = LanguageDetailUiState(isLoading = true)
            try {
                val deezerDeferred = async {
                    val deezer = sourceRegistry.getSource(SourceId.DEEZER)
                    deezer?.trending(languageCode, TrendingWindow.ALL_TIME, PageRequest(0, 35))?.getOrNull()?.items ?: emptyList()
                }

                val spotifyDeferred = async {
                    val spotify = sourceRegistry.getSource(SourceId.SPOTIFY)
                    val query = when (languageCode.lowercase()) {
                        "hindi" -> "india"
                        "korean", "kpop" -> "k-pop"
                        else -> languageCode
                    }
                    spotify?.trending(query, TrendingWindow.WEEK, PageRequest(0, 35))?.getOrNull()?.items ?: emptyList()
                }

                val deezerTracks = deezerDeferred.await()
                val spotifyTracks = spotifyDeferred.await()

                val combined = (deezerTracks + spotifyTracks)
                    .distinctBy { it.compositeKey }
                    .ifEmpty {
                        val deezer = sourceRegistry.getSource(SourceId.DEEZER)
                        deezer?.browse(com.saivatsal.soundorbit.core.model.BrowseKind.GENRE, "$languageCode top hits", PageRequest(0, 35))?.getOrNull()?.items ?: emptyList()
                    }

                _uiState.value = LanguageDetailUiState(
                    isLoading = false,
                    tracks = combined
                )
            } catch (e: Exception) {
                _uiState.value = LanguageDetailUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load $languageCode music"
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
fun LanguageDetailScreen(
    languageCode: String,
    onBack: () -> Unit,
    viewModel: LanguageDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(languageCode) {
        viewModel.loadLanguage(languageCode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.map { it.compositeKey }.toSet() }

    val formattedTitle = remember(languageCode) {
        when (languageCode.lowercase()) {
            "hindi" -> "Hindi & Bollywood"
            "punjabi" -> "Punjabi Hits"
            "telugu" -> "Telugu Cinema"
            "tamil" -> "Tamil Melodies"
            "korean" -> "Korean K-Pop"
            "kannada" -> "Kannada Beats"
            "malayalam" -> "Malayalam Hits"
            "bengali" -> "Bengali Music"
            "marathi" -> "Marathi Songs"
            "english" -> "International English"
            else -> languageCode.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    val gradientColors = remember(languageCode) {
        when (languageCode.lowercase()) {
            "hindi" -> listOf(Color(0xFFFF6B6B), Color(0xFF380816))
            "punjabi" -> listOf(Color(0xFFFF9F43), Color(0xFF3B1503))
            "telugu" -> listOf(Color(0xFF00BFA5), Color(0xFF002E24))
            "tamil" -> listOf(Color(0xFF8B5CF6), Color(0xFF1E0A40))
            "korean", "kpop" -> listOf(Color(0xFFFF69B4), Color(0xFF3B0824))
            "kannada" -> listOf(Color(0xFF3498DB), Color(0xFF0A2338))
            "malayalam" -> listOf(Color(0xFF1ABC9C), Color(0xFF042921))
            "bengali" -> listOf(Color(0xFFE67E22), Color(0xFF361A02))
            "marathi" -> listOf(Color(0xFF9B59B6), Color(0xFF260833))
            else -> listOf(EmeraldGreenBright, Color(0xFF002917))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formattedTitle, fontWeight = FontWeight.Bold, color = Color.White) },
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
        } else if (uiState.tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "No tracks found for $formattedTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMediumEmphasis
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(gradientColors))
                            .padding(top = padding.calculateTopPadding())
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = formattedTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Discover trending & chart-topping ${uiState.tracks.size} tracks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.playTrackList(uiState.tracks, 0) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreenBright,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play All", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val shuffled = uiState.tracks.shuffled()
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

                itemsIndexed(uiState.tracks) { index, track ->
                    TrackItem(
                        track = track,
                        isPlaying = false,
                        isFavorite = favoriteIds.contains(track.compositeKey),
                        onClick = { viewModel.playTrackList(uiState.tracks, index) },
                        onFavoriteToggle = { viewModel.toggleFavorite(track) }
                    )
                }
            }
        }
    }
}
