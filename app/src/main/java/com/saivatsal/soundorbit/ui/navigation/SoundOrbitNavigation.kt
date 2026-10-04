package com.saivatsal.soundorbit.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saivatsal.soundorbit.ui.component.EqualizerSheet
import com.saivatsal.soundorbit.ui.detail.AlbumDetailScreen
import com.saivatsal.soundorbit.ui.detail.ArtistDetailScreen
import com.saivatsal.soundorbit.ui.explore.ExploreScreen
import com.saivatsal.soundorbit.ui.explore.GenreDetailScreen
import com.saivatsal.soundorbit.ui.explore.LanguageDetailScreen
import com.saivatsal.soundorbit.ui.home.HomeScreen
import com.saivatsal.soundorbit.ui.home.HomeViewModel
import com.saivatsal.soundorbit.ui.library.FavoritesScreen
import com.saivatsal.soundorbit.ui.library.LibraryScreen
import com.saivatsal.soundorbit.ui.library.LibraryViewModel
import com.saivatsal.soundorbit.ui.library.PlaylistDetailScreen
import com.saivatsal.soundorbit.ui.library.RecentlyPlayedScreen
import com.saivatsal.soundorbit.ui.player.MiniPlayer
import com.saivatsal.soundorbit.ui.player.NowPlayingScreen
import com.saivatsal.soundorbit.ui.player.PlayerViewModel
import com.saivatsal.soundorbit.ui.search.SearchScreen
import com.saivatsal.soundorbit.ui.search.SearchViewModel
import com.saivatsal.soundorbit.ui.settings.SettingsScreen
import com.saivatsal.soundorbit.ui.settings.SettingsViewModel
import com.saivatsal.soundorbit.ui.theme.DarkSurface
import com.saivatsal.soundorbit.ui.theme.EmeraldGreenBright
import com.saivatsal.soundorbit.ui.theme.OledBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundOrbitApp(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerState by playerViewModel.playerState.collectAsState()
    val isFavorite by playerViewModel.isFavorite.collectAsState()
    val equalizerState by playerViewModel.equalizerState.collectAsState()

    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var showDirectEqualizerSheet by remember { mutableStateOf(false) }

    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    Scaffold(
        containerColor = OledBlack,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // MiniPlayer docked above Bottom Navigation on tabs or standalone on detail screens
                if (playerState.currentTrack != null) {
                    MiniPlayer(
                        playerState = playerState,
                        isFavorite = isFavorite,
                        onExpandNowPlaying = { isNowPlayingExpanded = true },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext = { playerViewModel.next() },
                        onToggleFavorite = { playerViewModel.toggleFavorite() }
                    )
                }

                if (showBottomBar) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 8.dp
                    ) {
                        BottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let {
                                        Icon(
                                            it,
                                            contentDescription = screen.title,
                                            tint = if (selected) EmeraldGreenBright else Color.Gray
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        color = if (selected) EmeraldGreenBright else Color.Gray
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = EmeraldGreenBright.copy(alpha = 0.15f),
                                    selectedIconColor = EmeraldGreenBright,
                                    selectedTextColor = EmeraldGreenBright,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else if (playerState.currentTrack != null) {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToFavorites = {
                            navController.navigate(Screen.Favorites.route)
                        },
                        onNavigateToRecentlyPlayed = {
                            navController.navigate(Screen.RecentlyPlayed.route)
                        },
                        onNavigateToExplore = {
                            navController.navigate(Screen.Explore.route)
                        },
                        onNavigateToGenre = { genre ->
                            navController.navigate(Screen.GenreDetails.createRoute(genre))
                        },
                        onNavigateToLanguage = { language ->
                            navController.navigate(Screen.LanguageDetails.createRoute(language))
                        },
                        onNavigateToArtist = { sourceId, artistId ->
                            navController.navigate(Screen.ArtistDetails.createRoute(sourceId, artistId))
                        },
                        onNavigateToAlbum = { sourceId, albumId ->
                            navController.navigate(Screen.AlbumDetails.createRoute(sourceId, albumId))
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateToArtist = { sourceId, artistId ->
                            navController.navigate(Screen.ArtistDetails.createRoute(sourceId, artistId))
                        },
                        onNavigateToAlbum = { sourceId, albumId ->
                            navController.navigate(Screen.AlbumDetails.createRoute(sourceId, albumId))
                        },
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate(Screen.PlaylistDetails.createRoute(playlistId))
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate(Screen.PlaylistDetails.createRoute(playlistId))
                        }
                    )
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.RecentlyPlayed.route) {
                    RecentlyPlayedScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Explore.route) {
                    ExploreScreen(
                        onBack = { navController.popBackStack() },
                        onSelectGenre = { genre ->
                            navController.navigate(Screen.GenreDetails.createRoute(genre))
                        },
                        onSelectLanguage = { language ->
                            navController.navigate(Screen.LanguageDetails.createRoute(language))
                        }
                    )
                }

                composable(
                    route = Screen.GenreDetails.route,
                    arguments = listOf(
                        navArgument("genreName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val genreName = backStackEntry.arguments?.getString("genreName") ?: ""
                    GenreDetailScreen(
                        genreName = genreName,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.LanguageDetails.route,
                    arguments = listOf(
                        navArgument("languageCode") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val languageCode = backStackEntry.arguments?.getString("languageCode") ?: ""
                    LanguageDetailScreen(
                        languageCode = languageCode,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ArtistDetails.route,
                    arguments = listOf(
                        navArgument("sourceId") { type = NavType.StringType },
                        navArgument("artistId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
                    val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                    ArtistDetailScreen(
                        sourceId = sourceId,
                        artistId = artistId,
                        onBack = { navController.popBackStack() },
                        onAlbumClick = { sId, aId ->
                            navController.navigate(Screen.AlbumDetails.createRoute(sId, aId))
                        }
                    )
                }

                composable(
                    route = Screen.AlbumDetails.route,
                    arguments = listOf(
                        navArgument("sourceId") { type = NavType.StringType },
                        navArgument("albumId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                    AlbumDetailScreen(
                        sourceId = sourceId,
                        albumId = albumId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.PlaylistDetails.route,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onOpenEqualizer = { showDirectEqualizerSheet = true }
                    )
                }
            }

            // Full-screen Now Playing Overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                NowPlayingScreen(
                    viewModel = playerViewModel,
                    onCollapse = { isNowPlayingExpanded = false }
                )
            }

            // Direct Equalizer Sheet Trigger from Settings
            if (showDirectEqualizerSheet) {
                EqualizerSheet(
                    state = equalizerState,
                    onDismiss = { showDirectEqualizerSheet = false },
                    onToggleEnabled = { playerViewModel.setEqualizerEnabled(it) },
                    onSelectPreset = { playerViewModel.selectEqualizerPreset(it) },
                    onBandChange = { band, level -> playerViewModel.setEqualizerBandLevel(band, level) },
                    onBassBoostChange = { playerViewModel.setBassBoost(it) },
                    onVirtualizerChange = { playerViewModel.setVirtualizer(it) }
                )
            }
        }
    }
}
