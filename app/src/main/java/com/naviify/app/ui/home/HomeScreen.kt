package com.naviify.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.R
import com.naviify.app.domain.model.Album
import com.naviify.app.ui.components.AmbientGlassBackdrop
import com.naviify.app.ui.components.AlbumCard
import com.naviify.app.ui.components.ArtistCard
import com.naviify.app.ui.components.EmptyState
import com.naviify.app.ui.components.ErrorBubble
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.components.PlaylistCard
import com.naviify.app.ui.components.SectionHeader
import com.naviify.app.ui.components.SpotifyQuickGridTile
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAllPlaylists: () -> Unit = {},
    onOpenNowPlaying: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Stations "radio" du jour (cartes facon Spotify), regenerees chaque jour cote serveur.
    val radioStations by viewModel.radioStations.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isOffline = state.isOfflineMode || (!state.serverReachable && state.quickPicks.isEmpty())
    val displayQuickPicks = state.quickPicks
    val displayRecentlyAdded = state.recentlyAdded

    val quickGridRows = remember(state.quickGridItems) { state.quickGridItems.chunked(2) }
    val quickPickRows = remember(displayQuickPicks) { displayQuickPicks.chunked(2) }

    var isQuickAccessGridView by rememberSaveable { mutableStateOf(true) }

    // (Le "hero" / FEATURED PLAYLIST a ete supprime : il repetait un titre deja
    // present dans la grille "Quick Access". A sa place : les stations radio.)

    val userPlaylistsRowState = rememberLazyListState()
    val recentlyAddedRowState = rememberLazyListState()
    val featuredArtistsRowState = rememberLazyListState()

    if (state.isLoading && state.quickPicks.isEmpty() && state.userPlaylists.isEmpty()) {
        LoadingBox()
        return
    }

    val accentColor = LocalNaviifyPalette.current.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Ambient Glassy Blurry Gradient Backdrop cached in hardware layer
        AmbientGlassBackdrop(
            accentColor = accentColor,
            backdropHeight = 460.dp,
            modifier = Modifier.graphicsLayer { },
        )

        LazyColumn(
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            // Header Bar & Greeting
            item(key = "home_header_and_greeting") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.app_logo),
                                contentDescription = "ASAYS",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "ASAYS",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = SurfaceCardHigh.copy(alpha = 0.70f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (state.serverReachable) MaterialTheme.colorScheme.primary else Color(0xFFFFB300),
                                        ),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = if (state.serverReachable) {
                                        state.serverLabel
                                    } else {
                                        "Offline Mode"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                )
                            }
                        }
                        IconButton(onClick = onOpenStats) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "Stats",
                                tint = TextPrimary,
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary,
                            )
                        }
                    }

                    Text(
                        text = stringResource(greetingResource(state.greeting)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.isOfflineMode,
                            onClick = viewModel::toggleOffline,
                            label = { Text(if (state.isOfflineMode) "Offline" else "Offline") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (state.isOfflineMode) Icons.Rounded.CloudOff else Icons.Rounded.DownloadDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                        FilterChip(
                            selected = false,
                            onClick = onOpenFavorites,
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }

            // Stations "radio" du jour : cartes facon Spotify, contenu renouvele
            // chaque jour par le serveur (~/scripts/radio_stations.py).
            if (radioStations.isNotEmpty()) {
                item(key = "header_radio") {
                    SectionHeader("Radio du jour")
                }
                item(key = "carousel_radio") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(radioStations, key = { it.name }, contentType = { "radio_card" }) { station ->
                            RadioStationCard(
                                name = station.name,
                                accent = station.accent,
                                trackCount = station.size,
                                onClick = {
                                    viewModel.playStation(station)
                                    onOpenNowPlaying()
                                },
                            )
                        }
                    }
                }
            }

            // Quick Access Section with BitChord Style View Switcher (Grid vs List)
            if (state.quickGridItems.isNotEmpty()) {
                item(key = "header_quick_access") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Quick Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        IconButton(
                            onClick = { isQuickAccessGridView = !isQuickAccessGridView },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (isQuickAccessGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                contentDescription = "Toggle view",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            if (isQuickAccessGridView) {
                // Spotify 6-Grid Quick Access. Rendered as three fixed rows of two:
                // a LazyColumn builds far less per frame than a 2-column grid whose
                // every other child spanned the full width.
                // Spotify 6-Grid Quick Access. Rendered as fixed rows of two with item recycling.
                items(
                    items = quickGridRows,
                    key = { row -> "qg_" + row.joinToString("_") { it.id } },
                    contentType = { "quick_grid_row" },
                ) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { item ->
                            val tileCover = remember(
                                item.id,
                                item.coverArtId,
                                item.playlistId,
                                item.isPlaylist,
                            ) {
                                if (item.isPlaylist && item.playlistId != null) {
                                    com.naviify.app.core.image.CoverUrls.playlistUrl(
                                        item.playlistId,
                                        item.coverArtId,
                                        256,
                                    )
                                } else {
                                    com.naviify.app.core.image.CoverUrls.url(item.coverArtId, 256)
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SpotifyQuickGridTile(
                                    title = item.title,
                                    coverUrl = tileCover,
                                    playlistId = item.playlistId,
                                    onClick = {
                                        if (item.isPlaylist && item.playlistId != null) {
                                            onOpenPlaylist(item.playlistId)
                                        } else if (item.albumId != null) {
                                            onOpenAlbum(item.albumId)
                                        }
                                    },
                                    onPlay = { viewModel.playQuickGridItem(item) },
                                )
                            }
                        }
                        // Keep the last row's single tile at half width instead of
                        // letting it stretch across the screen.
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                // Horizontal scrolling shelves of quick items (BitChord RecentShelf style)
                item(key = "quick_access_list_carousel") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(state.quickGridItems, key = { "qa_list_" + it.id }) { item ->
                            val tileCover = remember(item.id, item.coverArtId, item.playlistId, item.isPlaylist) {
                                if (item.isPlaylist && item.playlistId != null) {
                                    com.naviify.app.core.image.CoverUrls.playlistUrl(item.playlistId, item.coverArtId, 256)
                                } else {
                                    com.naviify.app.core.image.CoverUrls.url(item.coverArtId, 256)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardHigh,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        if (item.isPlaylist && item.playlistId != null) {
                                            onOpenPlaylist(item.playlistId)
                                        } else if (item.albumId != null) {
                                            onOpenAlbum(item.albumId)
                                        }
                                    },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp),
                                ) {
                                    // Requete memoisee et sans crossfade : un fondu par vignette
                                    // faisait travailler le GPU a chaque apparition au scroll.
                                    val thumbRequest = remember(tileCover) {
                                        ImageRequest.Builder(context)
                                            .data(tileCover)
                                            .crossfade(false)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = thumbRequest,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = if (item.isPlaylist) "Playlist" else "Album",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.playQuickGridItem(item) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Play",
                                            tint = SpotifyGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (
                !state.isLoading &&
                state.error == null &&
                state.quickGridItems.isEmpty() &&
                state.quickPicks.isEmpty() &&
                state.userPlaylists.isEmpty() &&
                state.recentlyAdded.isEmpty() &&
                state.featuredArtists.isEmpty()
            ) {
                item(key = "home_empty_state") {
                    EmptyState(
                        if (isOffline) "No downloaded music found. Connect to your server to download songs, albums or playlists."
                        else "Nothing to show yet. Check your server connection."
                    )
                }
            }

            // Your Playlists (Made by You) Carousel
            if (state.userPlaylists.isNotEmpty()) {
                item(key = "header_user_playlists") {
                    SectionHeader(
                        title = "Your Playlists",
                        actionText = "See all",
                        onActionClick = onOpenAllPlaylists,
                    )
                }
                item(key = "carousel_user_playlists") {
                    LazyRow(
                        state = userPlaylistsRowState,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.userPlaylists, key = { it.id }, contentType = { "playlist_card" }) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { onOpenPlaylist(playlist.id) },
                                modifier = Modifier.width(150.dp),
                            )
                        }
                    }
                }
            }

            // Recently Added Carousel
            if (displayRecentlyAdded.isNotEmpty()) {
                item(key = "header_recently_added") {
                    SectionHeader(stringResource(R.string.home_recently_added))
                }
                item(key = "carousel_recently_added") {
                    LazyRow(
                        state = recentlyAddedRowState,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(displayRecentlyAdded, key = { it.id }, contentType = { "album_card" }) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { onOpenAlbum(album.id) },
                                modifier = Modifier.width(150.dp),
                            )
                        }
                    }
                }
            }

            // Featured Artists Carousel
            if (state.featuredArtists.isNotEmpty()) {
                item(key = "header_featured_artists") {
                    SectionHeader(stringResource(R.string.home_featured_artists))
                }
                item(key = "carousel_featured_artists") {
                    LazyRow(
                        state = featuredArtistsRowState,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.featuredArtists, key = { it.id }, contentType = { "artist_card" }) { artist ->
                            ArtistCard(
                                artist = artist,
                                onClick = { onOpenArtist(artist.id) },
                                modifier = Modifier.width(140.dp),
                            )
                        }
                    }
                }
            }

            // Quick Picks : section retiree de l'accueil (demande de Tayeb, 26/09).
            // Le fetch reste dans HomeViewModel car `quickPicks` sert aussi a la
            // detection du mode hors-ligne, mais plus rien n'est dessine.
        }

        // Error notification overlay
        AnimatedVisibility(
            visible = state.error != null,
            enter = slideInVertically(
                initialOffsetY = { -it * 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it * 2 },
                animationSpec = tween(250),
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        ) {
            state.error?.let { message ->
                ErrorBubble(
                    message = message,
                    onRetry = viewModel::refresh,
                    onDismiss = viewModel::dismissError,
                )
            }
        }
    }
}

private fun greetingResource(key: String): Int = when (key) {
    "morning" -> R.string.home_greeting_morning
    "afternoon" -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
}

@Composable
private fun AppleMusicHeroCard(
    title: String,
    subtitle: String,
    coverUrl: String?,
    tag: String = "FEATURED",
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Degrades statiques memoises : les recreer a chaque recomposition allouait
    // deux objets de plus par image affichee.
    val placeholderBrush = remember { Brush.linearGradient(listOf(Color(0xFF2E2E2E), Color(0xFF121212))) }
    val scrimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.50f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.88f),
            ),
        )
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        color = Color(0xFF1E1E1E),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.85f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!coverUrl.isNullOrBlank()) {
                val heroRequest = remember(coverUrl) {
                    ImageRequest.Builder(context)
                        .data(coverUrl)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = heroRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderBrush),
                )
            }

            // Top-down and bottom-up gradients for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimBrush),
            )

            // Tag badge at top left
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.60f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart),
            ) {
                Text(
                    text = tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // Bottom title, subtitle, and play button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = SpotifyGreen,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onPlay),
                    shadowElevation = 6.dp,
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Carte d'une station "radio" du jour (facon Spotify) : tuile degradee batie sur
 * la couleur d'accent envoyee par le serveur, nom de la station et nombre de
 * titres. Aucune image distante, donc rien a telecharger pour afficher l'accueil.
 */
@Composable
private fun RadioStationCard(
    name: String,
    accent: String,
    trackCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Zero-SDK rule : aucune valeur @Composable (SpotifyGreen, TextPrimary...) ici,
    // tout est calculé avant la composition (remember + couleurs brutes).
    val accentColor = remember(accent) { parseAccentColor(accent) }
    val brush = remember(accentColor) {
        Brush.linearGradient(
            listOf(
                accentColor,
                accentColor.copy(alpha = 0.45f),
                Color(0xFF161616),
            ),
        )
    }
    Surface(
        color = Color(0xFF1C1C1C),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .width(152.dp)
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(brush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(34.dp),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$trackCount titres",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** "#RRGGBB" -> Color. Retourne l'accent du theme si la valeur est illisible. */
private fun parseAccentColor(hex: String): Color {
    val clean = hex.trim().removePrefix("#")
    return runCatching {
        if (clean.length == 6) Color(0xFF000000L or clean.toLong(16)) else Color(0xFFFA2D48)
    }.getOrDefault(Color(0xFFFA2D48))
}
