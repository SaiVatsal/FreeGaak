package com.saivatsal.soundorbit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.saivatsal.soundorbit.core.database.entity.ArtistPlayCount
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.ui.component.SourceBadge
import com.saivatsal.soundorbit.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToRecentlyPlayed: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    onNavigateToGenre: (String) -> Unit = {},
    onNavigateToLanguage: (String) -> Unit = {},
    onNavigateToArtist: (String, String) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val feedState by viewModel.feedState.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val heavyRotation by viewModel.heavyRotation.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val languageFilters = listOf(
        "all" to "All",
        "hindi" to "Hindi",
        "punjabi" to "Punjabi",
        "telugu" to "Telugu",
        "tamil" to "Tamil",
        "korean" to "K-Pop",
        "english" to "English",
        "kannada" to "Kannada",
        "malayalam" to "Malayalam",
        "bengali" to "Bengali",
        "marathi" to "Marathi"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(bottom = 80.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Top Header with Personalized Greeting & User Avatar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(1.5.dp, EmeraldGreenBright, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = EmeraldGreenBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Sound Orbit",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGreenBright,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToExplore) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Explore",
                            tint = EmeraldGreenBright
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
        }

        // Language Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(languageFilters) { (code, label) ->
                    val isSelected = feedState.selectedLanguage == code
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectLanguageFilter(code) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextMediumEmphasis,
                            selectedContainerColor = EmeraldGreenBright,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) EmeraldGreenBright else DarkBorderSubtle,
                            selectedBorderColor = EmeraldGreenBright,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Quick Access 2x2 Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAccessCard(
                        title = "Daily Orbit Mix",
                        icon = Icons.Default.AutoAwesome,
                        gradient = listOf(Color(0xFF00B050), Color(0xFF003830)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val mixTracks = (favorites + recentlyPlayed + feedState.spotifyGlobalTrending + feedState.spotifyIndiaTrending + feedState.deezerTrending)
                                .distinctBy { it.compositeKey }
                                .shuffled()
                                .take(30)
                            if (mixTracks.isNotEmpty()) viewModel.playTrackList(mixTracks)
                        }
                    )
                    QuickAccessCard(
                        title = "Liked Songs",
                        icon = Icons.Default.Favorite,
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFF3B1E78)),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFavorites
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAccessCard(
                        title = "Explore Hub",
                        icon = Icons.Default.Explore,
                        gradient = listOf(Color(0xFFFF6B6B), Color(0xFF6B1D2F)),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExplore
                    )
                    QuickAccessCard(
                        title = "Recently Played",
                        icon = Icons.Default.History,
                        gradient = listOf(Color(0xFF00BFA5), Color(0xFF004D40)),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRecentlyPlayed
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Selected Regional Language Trending Carousel (if active)
        if (feedState.selectedLanguage != "all" && feedState.regionalTrending.isNotEmpty()) {
            item {
                val langLabel = languageFilters.find { it.first == feedState.selectedLanguage }?.second ?: "Regional"
                SectionHeader(
                    title = "Trending in $langLabel",
                    icon = Icons.Default.Whatshot,
                    onSeeAll = { onNavigateToLanguage(feedState.selectedLanguage) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(feedState.regionalTrending) { index, track ->
                        TrackCard(
                            track = track,
                            onClick = { viewModel.playTrackList(feedState.regionalTrending, index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Hero Featured Daily Mix Card
        item {
            DailyMixHeroCard(
                favoritesCount = favorites.size,
                historyCount = recentlyPlayed.size,
                onPlayDailyMix = {
                    val mixTracks = (favorites + recentlyPlayed + feedState.spotifyGlobalTrending + feedState.spotifyIndiaTrending + feedState.deezerTrending + feedState.audiusTrending + feedState.jamendoTrending)
                        .distinctBy { it.compositeKey }
                        .shuffled()
                        .take(30)
                    if (mixTracks.isNotEmpty()) {
                        viewModel.playTrackList(mixTracks)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Mood & Genre Explore Quick Cards
        item {
            SectionHeader(
                title = "Explore Moods & Genres",
                icon = Icons.Default.Category,
                onSeeAll = onNavigateToExplore
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val moods = listOf(
                    MoodItem("Chill", listOf(Color(0xFF2E86DE), Color(0xFF54A0FF)), Icons.Default.Spa),
                    MoodItem("Energy", listOf(Color(0xFFFF9F43), Color(0xFFEE5253)), Icons.Default.Bolt),
                    MoodItem("Workout", listOf(Color(0xFF10AC84), Color(0xFF1DD1A1)), Icons.Default.FitnessCenter),
                    MoodItem("Bollywood", listOf(Color(0xFFFF6B6B), Color(0xFFFF9FF3)), Icons.Default.MusicNote),
                    MoodItem("Focus", listOf(Color(0xFF5F27CD), Color(0xFF341F97)), Icons.Default.SelfImprovement),
                    MoodItem("Party", listOf(Color(0xFFFF5252), Color(0xFFFF793F)), Icons.Default.Celebration),
                    MoodItem("Romantic", listOf(Color(0xFFE84393), Color(0xFFFD79A8)), Icons.Default.Favorite)
                )
                items(moods) { mood ->
                    MoodCard(
                        mood = mood,
                        onClick = { onNavigateToGenre(mood.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recently Played Carousel
        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently Played",
                    icon = Icons.Default.History,
                    onSeeAll = onNavigateToRecentlyPlayed
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(10.dp))
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

        // Top Artists
        if (topArtists.isNotEmpty()) {
            item {
                SectionHeader(title = "Top Artists", icon = Icons.Default.Person)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(topArtists) { artist ->
                        ArtistAvatarItem(
                            artist = artist,
                            onClick = {
                                onNavigateToArtist("DEEZER", artist.artistName)
                            }
                        )
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
                    CircularProgressIndicator(color = EmeraldGreenBright)
                }
            }
        } else {
            // Spotify Global Top 50 Carousel
            if (feedState.spotifyGlobalTrending.isNotEmpty()) {
                item {
                    SectionHeader(title = "Spotify Top 50 Global", icon = Icons.Default.TrendingUp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.spotifyGlobalTrending) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.spotifyGlobalTrending, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Spotify India Top 50 & Bollywood Carousel
            if (feedState.spotifyIndiaTrending.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Spotify Top 50 India",
                        icon = Icons.Default.Whatshot,
                        onSeeAll = { onNavigateToLanguage("hindi") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.spotifyIndiaTrending) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.spotifyIndiaTrending, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Deezer Trending Carousel
            if (feedState.deezerTrending.isNotEmpty()) {
                item {
                    SectionHeader(title = "Top Charts on Deezer", icon = Icons.Default.Album)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.deezerTrending) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.deezerTrending, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Deezer Genre Hits (Pop & New Releases) Carousel
            if (feedState.deezerGenreHits.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Pop & New Releases",
                        icon = Icons.Default.MusicNote,
                        onSeeAll = { onNavigateToGenre("Pop") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(feedState.deezerGenreHits) { index, track ->
                            TrackCard(
                                track = track,
                                onClick = { viewModel.playTrackList(feedState.deezerGenreHits, index) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Audius Trending Carousel
            if (feedState.audiusTrending.isNotEmpty()) {
                item {
                    SectionHeader(title = "Trending on Audius", icon = Icons.Default.Radio)
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldGreenBright,
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

        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldGreenBright,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun DailyMixHeroCard(
    favoritesCount: Int,
    historyCount: Int,
    onPlayDailyMix: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            EmeraldGreenDark,
            Color(0xFF004D40),
            ObsidianBlack
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onPlayDailyMix() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .border(1.dp, EmeraldGreenBright.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = EmeraldGreenBright.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "MADE FOR YOU",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Daily Orbit Mix",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Personalized mix based on your listening cosmos ($favoritesCount favorites & history)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreenBright),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Daily Mix",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

data class MoodItem(
    val name: String,
    val colors: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun MoodCard(
    mood: MoodItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(115.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(mood.colors))
                .padding(10.dp)
        ) {
            Icon(
                imageVector = mood.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
            )
            Text(
                text = mood.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopStart)
            )
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
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .border(0.8.dp, DarkBorderSubtle, RoundedCornerShape(16.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

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
            color = TextMediumEmphasis,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistAvatarItem(
    artist: ArtistPlayCount,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(1.5.dp, EmeraldGreenBright.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = artist.artistName,
                tint = EmeraldGreenBright,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = artist.artistName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${artist.playCount} plays",
            style = MaterialTheme.typography.labelSmall,
            color = TextMediumEmphasis
        )
    }
}
