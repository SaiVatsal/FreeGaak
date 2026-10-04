package com.saivatsal.soundorbit.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.model.Album
import com.saivatsal.soundorbit.core.model.Artist
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.core.model.SearchFilterType
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.ui.component.TrackItem
import com.saivatsal.soundorbit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToArtist: (String, String) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String) -> Unit = { _, _ -> },
    onNavigateToPlaylist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
    ) {
        // Search Header Title
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        // Search TextField Header
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("Search songs, artists, albums, or genres...", color = TextLowEmphasis) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = EmeraldGreenBright
                )
            },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.White
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreenBright,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Source Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedSource == null,
                    onClick = { viewModel.onSourceFilterSelect(null) },
                    label = { Text("All Sources", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenBright,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextMediumEmphasis
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedSource == SourceId.SPOTIFY,
                    onClick = { viewModel.onSourceFilterSelect(SourceId.SPOTIFY) },
                    label = { Text("Spotify", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenBright,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextMediumEmphasis
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedSource == SourceId.DEEZER,
                    onClick = { viewModel.onSourceFilterSelect(SourceId.DEEZER) },
                    label = { Text("Deezer", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenBright,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextMediumEmphasis
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedSource == SourceId.AUDIUS,
                    onClick = { viewModel.onSourceFilterSelect(SourceId.AUDIUS) },
                    label = { Text("Audius", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenBright,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextMediumEmphasis
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedSource == SourceId.JAMENDO,
                    onClick = { viewModel.onSourceFilterSelect(SourceId.JAMENDO) },
                    label = { Text("Jamendo", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenBright,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextMediumEmphasis
                    ),
                    border = null
                )
            }
            item {
                FilterChip(
                    selected = uiState.selectedSource == SourceId.LOCAL,
                    onClick = { viewModel.onSourceFilterSelect(SourceId.LOCAL) },
                    label = { Text("Local Audio", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenBright,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextMediumEmphasis
                    ),
                    border = null
                )
            }
        }

        // Category Filter Chips Row (if query is active)
        if (uiState.query.isNotBlank()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchFilterType.values().forEach { filter ->
                    item {
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { viewModel.onCategoryFilterSelect(filter) },
                            label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreenBright.copy(alpha = 0.85f),
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextMediumEmphasis
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Content Area: Browse Genres or Search Results
        if (uiState.query.isBlank()) {
            // Browse Genres Title
            Text(
                text = "Explore Categories & Genres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(POPULAR_GENRES) { genre ->
                    GenreCard(
                        genre = genre,
                        onClick = { viewModel.searchGenre(genre) }
                    )
                }
            }
        } else {
            // Search Results
            if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldGreenBright)
                }
            } else if (uiState.tracks.isEmpty() && uiState.artists.isEmpty() && uiState.albums.isEmpty() && uiState.playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMediumEmphasis,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found for \"${uiState.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMediumEmphasis
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Artists Section (if any)
                    if (uiState.artists.isNotEmpty() && (uiState.selectedFilter == SearchFilterType.ALL || uiState.selectedFilter == SearchFilterType.ARTISTS)) {
                        item {
                            Text(
                                text = "Artists",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.artists) { artist ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(90.dp)
                                            .clickable { onNavigateToArtist(artist.sourceId.name, artist.sourceArtistId) }
                                    ) {
                                        AsyncImage(
                                            model = artist.imageUrl,
                                            contentDescription = artist.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(DarkSurfaceVariant)
                                                .border(1.5.dp, EmeraldGreenBright.copy(alpha = 0.5f), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = artist.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Albums Section (if any)
                    if (uiState.albums.isNotEmpty() && (uiState.selectedFilter == SearchFilterType.ALL || uiState.selectedFilter == SearchFilterType.ALBUMS)) {
                        item {
                            Text(
                                text = "Albums",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.albums) { album ->
                                    Column(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable { onNavigateToAlbum(album.sourceId.name, album.sourceAlbumId) }
                                    ) {
                                        AsyncImage(
                                            model = album.artworkUrl,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(DarkSurfaceVariant)
                                                .border(0.8.dp, DarkBorderSubtle, RoundedCornerShape(12.dp))
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
                                            text = album.artistName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMediumEmphasis,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Playlists Section (if any)
                    if (uiState.playlists.isNotEmpty() && (uiState.selectedFilter == SearchFilterType.ALL || uiState.selectedFilter == SearchFilterType.PLAYLISTS)) {
                        item {
                            Text(
                                text = "Playlists",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.playlists) { playlist ->
                                    Column(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable { onNavigateToPlaylist(playlist.sourcePlaylistId) }
                                    ) {
                                        AsyncImage(
                                            model = playlist.artworkUrl,
                                            contentDescription = playlist.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(DarkSurfaceVariant)
                                                .border(0.8.dp, DarkBorderSubtle, RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${playlist.trackCount} tracks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMediumEmphasis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Tracks Section
                    if (uiState.tracks.isNotEmpty() && (uiState.selectedFilter == SearchFilterType.ALL || uiState.selectedFilter == SearchFilterType.SONGS)) {
                        item {
                            Text(
                                text = "Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        itemsIndexed(uiState.tracks) { index, track ->
                            TrackItem(
                                track = track,
                                isPlaying = false,
                                isFavorite = false,
                                onClick = { viewModel.playTrackList(uiState.tracks, index) },
                                onFavoriteToggle = { viewModel.toggleFavorite(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreCard(
    genre: String,
    onClick: () -> Unit
) {
    val gradientColors = when (genre) {
        "Electronic" -> listOf(Color(0xFF00E5FF), Color(0xFF0055FF))
        "Hip-Hop" -> listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
        "Rock" -> listOf(Color(0xFFFF1744), Color(0xFFB71C1C))
        "Pop" -> listOf(Color(0xFFFF4081), Color(0xFFC51162))
        "Bollywood" -> listOf(Color(0xFFFF6B6B), Color(0xFFFF2A6D))
        "Punjabi" -> listOf(Color(0xFFFF7043), Color(0xFFD84315))
        "Jazz" -> listOf(Color(0xFFFFD600), Color(0xFFFF6D00))
        "Ambient" -> listOf(Color(0xFF00E676), Color(0xFF00B0FF))
        "Indie" -> listOf(Color(0xFF7C4DFF), Color(0xFF536DFE))
        "Classical" -> listOf(Color(0xFF8D6E63), Color(0xFF4E342E))
        "Lo-Fi" -> listOf(Color(0xFF9C27B0), Color(0xFFE040FB))
        "Synthwave" -> listOf(Color(0xFFFF007F), Color(0xFF7B1FA2))
        "K-Pop" -> listOf(Color(0xFFFF4081), Color(0xFF7C4DFF))
        "Latin" -> listOf(Color(0xFFFF5722), Color(0xFFFF9800))
        "Folk" -> listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
        else -> listOf(Color(0xFF00B4D8), Color(0xFF0077B6))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
