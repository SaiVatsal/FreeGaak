package com.saivatsal.soundorbit.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.saivatsal.soundorbit.ui.home.HomeScreen
import com.saivatsal.soundorbit.ui.home.HomeViewModel
import com.saivatsal.soundorbit.ui.library.LibraryScreen
import com.saivatsal.soundorbit.ui.library.LibraryViewModel
import com.saivatsal.soundorbit.ui.library.PlaylistDetailScreen
import com.saivatsal.soundorbit.ui.player.MiniPlayer
import com.saivatsal.soundorbit.ui.player.NowPlayingScreen
import com.saivatsal.soundorbit.ui.player.PlayerViewModel
import com.saivatsal.soundorbit.ui.search.SearchScreen
import com.saivatsal.soundorbit.ui.search.SearchViewModel
import com.saivatsal.soundorbit.ui.settings.SettingsScreen
import com.saivatsal.soundorbit.ui.settings.SettingsViewModel
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurface
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
            if (showBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // MiniPlayer docked above Bottom Navigation
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
                                            tint = if (selected) CosmicTeal else Color.Gray
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        color = if (selected) CosmicTeal else Color.Gray
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = CosmicTeal.copy(alpha = 0.15f),
                                    selectedIconColor = CosmicTeal,
                                    selectedTextColor = CosmicTeal,
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
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        viewModel = searchViewModel
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

            // If on details screens without bottom bar, show floating MiniPlayer at the bottom
            if (!showBottomBar && playerState.currentTrack != null) {
                MiniPlayer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    playerState = playerState,
                    isFavorite = isFavorite,
                    onExpandNowPlaying = { isNowPlayingExpanded = true },
                    onPlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.next() },
                    onToggleFavorite = { playerViewModel.toggleFavorite() }
                )
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
