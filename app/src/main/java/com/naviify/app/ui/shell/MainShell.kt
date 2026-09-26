package com.naviify.app.ui.shell

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.components.AddToPlaylistSheet
import com.naviify.app.ui.components.LocalTrackActionHandler
import com.naviify.app.ui.components.TrackActionHandler
import com.naviify.app.ui.components.TrackOptionsBottomSheet
import com.naviify.app.ui.download.DownloadViewModel
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.player.QueueSheet
import com.naviify.app.ui.playlist.PlaylistsViewModel
import com.naviify.app.ui.album.AlbumDetailScreen
import com.naviify.app.ui.artist.ArtistDetailScreen
import com.naviify.app.ui.home.HomeScreen
import com.naviify.app.ui.library.LibraryScreen
import com.naviify.app.ui.navigation.Routes
import com.naviify.app.ui.player.MiniPlayerBar
import com.naviify.app.ui.player.NowPlayingScreen
import com.naviify.app.ui.player.PlayerViewModel
import com.naviify.app.ui.playlist.PlaylistsScreen
import com.naviify.app.ui.stats.StatsScreen
import com.naviify.app.ui.playlist.PlaylistDetailScreen
import com.naviify.app.ui.search.SearchScreen
import com.naviify.app.ui.settings.SettingsScreen
import com.naviify.app.ui.theme.NaviifyBlack
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

@Composable
fun MainShell(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var lastTab by remember { mutableStateOf(Routes.HOME) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = when (currentRoute) {
        Routes.HOME, Routes.SEARCH, Routes.PLAYLISTS, Routes.SETTINGS -> currentRoute
        Routes.LIBRARY, Routes.FAVORITES -> Routes.PLAYLISTS
        else -> lastTab
    }

    var trackForOptions by remember { mutableStateOf<Track?>(null) }
    var trackOptionsDownloadState by remember { mutableStateOf<TrackDownloadState?>(null) }
    var trackOptionsDownloadToggle by remember { mutableStateOf<(() -> Unit)?>(null) }

    var trackForAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }

    val trackActionHandler = remember {
        TrackActionHandler(
            openOptions = { track, downloadState, onDownloadToggle ->
                trackForOptions = track
                trackOptionsDownloadState = downloadState
                trackOptionsDownloadToggle = onDownloadToggle
            },
        )
    }

    CompositionLocalProvider(LocalTrackActionHandler provides trackActionHandler) {
        Scaffold(
            containerColor = NaviifyBlack,
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute != Routes.NOW_PLAYING,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = CubicBezierEasing(0.1f, 1f, 0.1f, 1f)),
                    ) + fadeIn(tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(300, easing = CubicBezierEasing(0.1f, 1f, 0.1f, 1f)),
                    ) + fadeOut(tween(150)),
                ) {
                    Column {
                        MiniPlayerBar(
                            viewModel = playerViewModel,
                            onOpen = { navController.navigate(Routes.NOW_PLAYING) },
                        )
                        SpotifyBottomBar(
                            selected = selectedTab,
                            onSelect = { tab ->
                                lastTab = tab
                                navigateToTab(navController, tab)
                            },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = {
                    if (targetState.destination.route == Routes.NOW_PLAYING) {
                        ExitTransition.None
                    } else {
                        fadeOut(tween(250))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == Routes.NOW_PLAYING) {
                        EnterTransition.None
                    } else {
                        fadeIn(tween(250))
                    }
                },
                popExitTransition = { fadeOut(tween(250)) },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenAlbum = { navController.navigate(Routes.album(it)) },
                        onOpenArtist = { navController.navigate(Routes.artist(it)) },
                        onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                        onOpenSettings = {
                            lastTab = Routes.SETTINGS
                            navigateToTab(navController, Routes.SETTINGS)
                        },
                        onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                        onOpenStats = { navController.navigate(Routes.STATS) },
                        onOpenAllPlaylists = {
                            lastTab = Routes.PLAYLISTS
                            navigateToTab(navController, Routes.PLAYLISTS)
                        },
                        onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onOpenAlbum = { navController.navigate(Routes.album(it)) },
                        onOpenArtist = { navController.navigate(Routes.artist(it)) },
                        onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                        onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        onOpenAlbum = { navController.navigate(Routes.album(it)) },
                        onOpenArtist = { navController.navigate(Routes.artist(it)) },
                        onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
                composable(Routes.PLAYLISTS) {
                    PlaylistsScreen(
                        onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                        onOpenArtist = { navController.navigate(Routes.artist(it)) },
                        onOpenAlbum = { navController.navigate(Routes.album(it)) },
                    )
                }
                composable(Routes.STATS) {
                    StatsScreen(
                        onBack = { navController.popBackStack() },
                        onPlayTrack = { track -> playerViewModel.play(listOf(track), 0) },
                    )
                }
                composable(Routes.FAVORITES) {
                    LibraryScreen(
                        onOpenAlbum = { navController.navigate(Routes.album(it)) },
                        onOpenArtist = { navController.navigate(Routes.artist(it)) },
                        onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                        initialCategory = com.naviify.app.ui.library.LibraryCategory.FAVORITES,
                    )
                }
                composable(
                    route = Routes.NOW_PLAYING,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = CubicBezierEasing(0.1f, 1f, 0.1f, 1f),
                            ),
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = CubicBezierEasing(0.1f, 1f, 0.1f, 1f),
                            ),
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = CubicBezierEasing(0.1f, 1f, 0.1f, 1f),
                            ),
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = CubicBezierEasing(0.1f, 1f, 0.1f, 1f),
                            ),
                        )
                    },
                ) {
                    NowPlayingScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { albumId -> navController.navigate(Routes.album(albumId)) },
                        onOpenArtist = { artistId -> navController.navigate(Routes.artist(artistId)) },
                    )
                }
                composable(
                    route = Routes.PLAYLIST,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) {
                    PlaylistDetailScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { albumId -> navController.navigate(Routes.album(albumId)) },
                        onOpenArtist = { artistId -> navController.navigate(Routes.artist(artistId)) },
                    )
                }
                composable(
                    route = Routes.ALBUM,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) {
                    AlbumDetailScreen(
                        onOpenArtist = { artistId ->
                            navController.navigate(Routes.artist(artistId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.ARTIST,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) {
                    ArtistDetailScreen(
                        onOpenAlbum = { albumId ->
                            navController.navigate(Routes.album(albumId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        // Global Track Options Bottom Sheet
        trackForOptions?.let { track ->
            TrackOptionsBottomSheet(
                track = track,
                downloadState = trackOptionsDownloadState,
                onDismiss = { trackForOptions = null },
                onShare = {
                    trackForOptions = null
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist.orEmpty()}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share song"))
                },
                onAddToPlaylist = {
                    val t = track
                    trackForOptions = null
                    trackForAddToPlaylist = t
                },
                onAddToQueue = {
                    playerViewModel.appendToQueue(track)
                    trackForOptions = null
                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                },
                onViewQueue = {
                    trackForOptions = null
                    showQueueSheet = true
                },
                onGoToAlbum = track.albumId?.takeIf { it.isNotBlank() }?.let { albumId ->
                    {
                        trackForOptions = null
                        navController.navigate(Routes.album(albumId))
                    }
                },
                onGoToArtist = (track.artistId?.takeIf { it.isNotBlank() } ?: track.artist?.takeIf { it.isNotBlank() })?.let { artistRef ->
                    {
                        trackForOptions = null
                        navController.navigate(Routes.artist(artistRef))
                    }
                },
                onDownloadToggle = {
                    val toggle = trackOptionsDownloadToggle
                    trackForOptions = null
                    if (toggle != null) {
                        toggle()
                    } else {
                        if (trackOptionsDownloadState == TrackDownloadState.DONE) {
                            downloadViewModel.deleteDownload(track.id)
                        } else {
                            downloadViewModel.downloadTrack(track)
                        }
                    }
                },
            )
        }

        // Add to Playlist Sheet
        trackForAddToPlaylist?.let { track ->
            val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(track.id) {
                playlistsViewModel.checkTrackInPlaylists(track.id)
            }
            val existingPlaylistIds = androidx.compose.runtime.remember(playlistsState.playlistTrackIds, track.id) {
                playlistsState.playlistTrackIds.filterValues { it.contains(track.id) }.keys
            }
            AddToPlaylistSheet(
                track = track,
                playlists = playlistsState.playlists,
                loading = playlistsState.isLoading,
                existingPlaylistIds = existingPlaylistIds,
                onDismiss = { trackForAddToPlaylist = null },
                onPick = { playlistId, playlistName, onDone ->
                    playlistsViewModel.addTrackToPlaylist(playlistId, track) { ok ->
                        onDone(ok)
                        if (ok) {
                            Toast.makeText(
                                context,
                                "Added to \"$playlistName\"",
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Could not add to playlist",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
                onCreateNew = {
                    playlistsViewModel.createPlaylist(initialTrack = track) { ok ->
                        if (ok) {
                            trackForAddToPlaylist = null
                            Toast.makeText(context, "Playlist created and track added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not create playlist", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                newName = playlistsState.newPlaylistName,
                onNewNameChange = playlistsViewModel::onNameChange,
                creating = playlistsState.isCreating,
            )
        }

        // Queue Sheet. Only the transport flags are needed here: collecting the
        // whole player state made the shell recompose five times a second from
        // the position ticker while the sheet was open.
        if (showQueueSheet) {
            val isShuffleEnabled by remember(playerViewModel) {
                playerViewModel.state.map { it.isShuffleEnabled }.distinctUntilChanged()
            }.collectAsStateWithLifecycle(initialValue = false)
            QueueSheet(
                viewModel = playerViewModel,
                onDismiss = { showQueueSheet = false },
                onSelect = { index ->
                    playerViewModel.select(index)
                    showQueueSheet = false
                },
                onToggleShuffle = { playerViewModel.setShuffle(!isShuffleEnabled) },
            )
        }
    }
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun SpotifyBottomBar(selected: String, onSelect: (String) -> Unit) {
    val tabs = remember {
        listOf(
            TabItem(Routes.HOME, "Home", Icons.Rounded.Home),
            TabItem(Routes.SEARCH, "Search", Icons.Rounded.Search),
            // The Library tab IS the playlists screen (playlists, layout toggle,
            // filters); there is no separate Library/Playlists pair any more.
            TabItem(Routes.PLAYLISTS, "Library", Icons.Rounded.LibraryMusic),
            TabItem(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
        )
    }
    NavigationBar(containerColor = NaviifyBlack) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = TextPrimary,
                    indicatorColor = SurfaceCard,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
            )
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    val current = navController.currentDestination?.route
    if (current == route) return

    val popped = navController.popBackStack(route, inclusive = false)
    if (!popped) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }
}
