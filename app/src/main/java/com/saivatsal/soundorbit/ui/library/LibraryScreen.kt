package com.saivatsal.soundorbit.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.ui.component.TrackItem
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurface
import com.saivatsal.soundorbit.ui.theme.DarkSurfaceVariant
import com.saivatsal.soundorbit.ui.theme.OledBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val localTracks by viewModel.localTracks.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Cosmos Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            IconButton(onClick = { showCreatePlaylistDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Playlist",
                    tint = CosmicTeal,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = DarkSurface,
            contentColor = CosmicTeal,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = CosmicTeal
                )
            }
        ) {
            LibraryTab.values().forEach { tab ->
                val title = when (tab) {
                    LibraryTab.PLAYLISTS -> "Playlists (${playlists.size})"
                    LibraryTab.FAVORITES -> "Favorites (${favorites.size})"
                    LibraryTab.LOCAL_DEVICE -> "On Device"
                    LibraryTab.OFFLINE -> "Offline"
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == tab) CosmicTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Tab Contents
        when (selectedTab) {
            LibraryTab.PLAYLISTS -> {
                if (playlists.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.QueueMusic,
                        title = "No Playlists Yet",
                        message = "Create custom playlists to organize your favorite tracks",
                        actionLabel = "Create Playlist",
                        onAction = { showCreatePlaylistDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playlists) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = { onNavigateToPlaylist(playlist.sourcePlaylistId) },
                                onDelete = { viewModel.deletePlaylist(playlist.sourcePlaylistId) }
                            )
                        }
                    }
                }
            }

            LibraryTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.FavoriteBorder,
                        title = "No Favorites Yet",
                        message = "Tap the heart icon on any song to add it to your favorites cosmos"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            // Play All & Shuffle Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.playTrackList(favorites) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play All", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.playTrackList(favorites.shuffled()) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shuffle", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        itemsIndexed(favorites) { index, track ->
                            TrackItem(
                                track = track,
                                isPlaying = false,
                                isFavorite = true,
                                onClick = { viewModel.playTrackList(favorites, index) },
                                onFavoriteToggle = { viewModel.toggleFavorite(track) }
                            )
                        }
                    }
                }
            }

            LibraryTab.LOCAL_DEVICE -> {
                if (localTracks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Folder,
                        title = "No Local Audio Found",
                        message = "Make sure audio files (MP3, FLAC, AAC, WAV) are stored on your device storage",
                        actionLabel = "Rescan Storage",
                        onAction = { viewModel.loadLocalTracks() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(localTracks) { index, track ->
                            TrackItem(
                                track = track,
                                isPlaying = false,
                                isFavorite = false,
                                onClick = { viewModel.playTrackList(localTracks, index) },
                                onFavoriteToggle = { viewModel.toggleFavorite(track) }
                            )
                        }
                    }
                }
            }

            LibraryTab.OFFLINE -> {
                EmptyState(
                    icon = Icons.Default.CloudDone,
                    title = "Offline Storage Mode",
                    message = "Tracks downloaded or cached for offline playback will appear here"
                )
            }
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            var playlistName by remember { mutableStateOf("") }
            var playlistDesc by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                containerColor = DarkSurface,
                title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = playlistDesc,
                            onValueChange = { playlistDesc = it },
                            label = { Text("Description (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                viewModel.createPlaylist(playlistName.trim(), playlistDesc.trim().ifEmpty { null })
                                showCreatePlaylistDialog = false
                            }
                        },
                        enabled = playlistName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = Color.Black)
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.artworkUrl != null) {
                AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = CosmicTeal,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CosmicTeal,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
