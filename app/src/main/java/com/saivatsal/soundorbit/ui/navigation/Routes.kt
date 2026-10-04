package com.saivatsal.soundorbit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Parameterized routes
    data object PlaylistDetails : Screen("playlist/{playlistId}", "Playlist") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }

    data object ArtistDetails : Screen("artist/{sourceId}/{artistId}", "Artist") {
        fun createRoute(sourceId: String, artistId: String) = "artist/$sourceId/$artistId"
    }

    data object AlbumDetails : Screen("album/{sourceId}/{albumId}", "Album") {
        fun createRoute(sourceId: String, albumId: String) = "album/$sourceId/$albumId"
    }
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Library,
    Screen.Settings
)
