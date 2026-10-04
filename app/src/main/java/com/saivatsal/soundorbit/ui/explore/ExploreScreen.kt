package com.saivatsal.soundorbit.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saivatsal.soundorbit.ui.theme.EmeraldGreenBright
import com.saivatsal.soundorbit.ui.theme.OledBlack

data class CategoryItem(
    val id: String,
    val name: String,
    val gradient: List<Color>,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onBack: () -> Unit,
    onSelectGenre: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = listOf(
        CategoryItem("chill", "Chill & Relax", listOf(Color(0xFF2E86DE), Color(0xFF54A0FF)), Icons.Default.Spa),
        CategoryItem("energy", "High Energy", listOf(Color(0xFFFF9F43), Color(0xFFEE5253)), Icons.Default.Bolt),
        CategoryItem("workout", "Workout Gym", listOf(Color(0xFF10AC84), Color(0xFF1DD1A1)), Icons.Default.FitnessCenter),
        CategoryItem("focus", "Deep Focus", listOf(Color(0xFF5F27CD), Color(0xFF341F97)), Icons.Default.SelfImprovement),
        CategoryItem("party", "Party Dance", listOf(Color(0xFFFF5252), Color(0xFFFF793F)), Icons.Default.Celebration),
        CategoryItem("romantic", "Romantic Love", listOf(Color(0xFFE84393), Color(0xFFFD79A8)), Icons.Default.Favorite),
        CategoryItem("acoustic", "Acoustic Unplugged", listOf(Color(0xFF16A085), Color(0xFF2ECC71)), Icons.Default.MusicNote),
        CategoryItem("night-drive", "Night Drive", listOf(Color(0xFF2C3E50), Color(0xFF34495E)), Icons.Default.DirectionsCar),
        CategoryItem("gaming", "Gaming Beats", listOf(Color(0xFF8E44AD), Color(0xFF9B59B6)), Icons.Default.SportsEsports)
    )

    val genres = listOf(
        CategoryItem("pop", "Pop Hits", listOf(Color(0xFFFF007A), Color(0xFF7928CA)), Icons.Default.Album),
        CategoryItem("hip-hop", "Hip-Hop & Rap", listOf(Color(0xFFFF6B6B), Color(0xFFC0392B)), Icons.Default.Mic),
        CategoryItem("dance", "Dance & EDM", listOf(Color(0xFF00C6FF), Color(0xFF0072FF)), Icons.Default.GraphicEq),
        CategoryItem("rock", "Rock & Metal", listOf(Color(0xFFE74C3C), Color(0xFF8E44AD)), Icons.Default.ElectricBolt),
        CategoryItem("r&b", "R&B & Soul", listOf(Color(0xFFF39C12), Color(0xFFD35400)), Icons.Default.QueueMusic),
        CategoryItem("k-pop", "K-Pop", listOf(Color(0xFFFF758C), Color(0xFFFF7EB3)), Icons.Default.Star),
        CategoryItem("jazz", "Jazz & Blues", listOf(Color(0xFF3B4371), Color(0xFFF3904F)), Icons.Default.Piano),
        CategoryItem("indie", "Indie Alternative", listOf(Color(0xFF11998E), Color(0xFF38EF7D)), Icons.Default.Headphones),
        CategoryItem("classical", "Classical", listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)), Icons.Default.LibraryMusic)
    )

    val languages = listOf(
        CategoryItem("hindi", "Hindi / Bollywood", listOf(Color(0xFFFF6B6B), Color(0xFFFF9FF3)), Icons.Default.MusicNote),
        CategoryItem("punjabi", "Punjabi Pop", listOf(Color(0xFFFF9F43), Color(0xFFFF5252)), Icons.Default.Whatshot),
        CategoryItem("telugu", "Telugu Hits", listOf(Color(0xFF00BFA5), Color(0xFF004D40)), Icons.Default.GraphicEq),
        CategoryItem("tamil", "Tamil Melodies", listOf(Color(0xFF8B5CF6), Color(0xFF3B1E78)), Icons.Default.LibraryMusic),
        CategoryItem("korean", "Korean / K-Pop", listOf(Color(0xFFFF69B4), Color(0xFFBA55D3)), Icons.Default.AutoAwesome),
        CategoryItem("kannada", "Kannada Beats", listOf(Color(0xFF3498DB), Color(0xFF2980B9)), Icons.Default.Album),
        CategoryItem("malayalam", "Malayalam Grooves", listOf(Color(0xFF1ABC9C), Color(0xFF16A085)), Icons.Default.Mic),
        CategoryItem("bengali", "Bengali Classics", listOf(Color(0xFFE67E22), Color(0xFFD35400)), Icons.Default.Audiotrack),
        CategoryItem("marathi", "Marathi Songs", listOf(Color(0xFF9B59B6), Color(0xFF8E44AD)), Icons.Default.MusicVideo),
        CategoryItem("english", "International English", listOf(Color(0xFF00DF81), Color(0xFF004B23)), Icons.Default.Public)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Orbit", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBlack,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = OledBlack,
        modifier = modifier
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section: Languages & Regional Music
            item(span = { GridItemSpan(2) }) {
                SectionTitle("Languages & Regional Hits")
            }
            items(languages) { lang ->
                ExploreCategoryCard(
                    category = lang,
                    onClick = { onSelectLanguage(lang.id) }
                )
            }

            // Section: Moods & Activities
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Moods & Activities")
            }
            items(moods) { mood ->
                ExploreCategoryCard(
                    category = mood,
                    onClick = { onSelectGenre(mood.id) }
                )
            }

            // Section: Music Genres
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Genres & Styles")
            }
            items(genres) { genre ->
                ExploreCategoryCard(
                    category = genre,
                    onClick = { onSelectGenre(genre.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ExploreCategoryCard(
    category: CategoryItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(category.gradient))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
