package com.saivatsal.soundorbit.ui.home

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
import com.saivatsal.soundorbit.core.database.entity.ArtistPlayCount
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.ui.component.SourceBadge
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurface
import com.saivatsal.soundorbit.ui.theme.DarkSurfaceVariant
import com.saivatsal.soundorbit.ui.theme.OledBlack

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedState by viewModel.feedState.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val heavyRotation by viewModel.heavyRotation.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(bottom = 80.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SoundOrbit",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = CosmicTeal
                    )
                    Text(
                        text = "Your Private Universe of Sound",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // Daily Mix Hero Card
        item {
            DailyMixCard(
                favoritesCount = favorites.size,
                historyCount = recentlyPlayed.size,
                onPlayDailyMix = {
                    val mixTracks = (favorites + recentlyPlayed + feedState.audiusTrending + feedState.jamendoTrending)
                        .distinctBy { it.compositeKey }
                        .shuffled()
                        .take(25)
                    if (mixTracks.isNotEmpty()) {
                        viewModel.playTrackList(mixTracks)
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Recently Played Carousel
        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Played", icon = Icons.Default.History)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(recentlyPlayed) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { viewModel.playTrackList(recentlyPlayed, index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Heavy Rotation Carousel
        if (heavyRotation.isNotEmpty()) {
            item {
                SectionHeader(title = "Heavy Rotation", icon = Icons.Default.Repeat)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(heavyRotation) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { viewModel.playTrackList(heavyRotation, index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Top Artists Chips
        if (topArtists.isNotEmpty()) {
            item {
                SectionHeader(title = "Top Artists", icon = Icons.Default.Person)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topArtists) { artist ->
                        ArtistAvatarItem(artist = artist)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Loading indicator or Trending content
        if (feedState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CosmicTeal)
                }
            }
        } else {
            // Audius Trending Carousel
            if (feedState.audiusTrending.isNotEmpty()) {
                item {
                    SectionHeader(title = "Trending on Audius", icon = Icons.Default.TrendingUp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.audiusTrending) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.audiusTrending, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Jamendo Trending Carousel
            if (feedState.jamendoTrending.isNotEmpty()) {
                item {
                    SectionHeader(title = "Top Hits on Jamendo", icon = Icons.Default.Star)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.jamendoTrending) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.jamendoTrending, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Local Media Carousel (if any)
            if (feedState.localRecent.isNotEmpty()) {
                item {
                    SectionHeader(title = "On Device Audio", icon = Icons.Default.Folder)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.localRecent) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.localRecent, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmicTeal,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DailyMixCard(
    favoritesCount: Int,
    historyCount: Int,
    onPlayDailyMix: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            CosmicTeal.copy(alpha = 0.85f),
            Color(0xFF006699)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlayDailyMix() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Orbit Mix",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tailored mix from your listening cosmos ($favoritesCount favorites & history)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Daily Mix",
                        tint = CosmicTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                SourceBadge(sourceId = track.sourceId)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = track.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistAvatarItem(artist: ArtistPlayCount) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = artist.artistName,
                tint = CosmicTeal,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = artist.artistName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${artist.playCount} plays",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
