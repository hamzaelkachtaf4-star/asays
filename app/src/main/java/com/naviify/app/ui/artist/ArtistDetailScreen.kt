package com.naviify.app.ui.artist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.components.formatDuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.MoreVert
import com.naviify.app.domain.model.Album
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.LocalTrackActionHandler
import com.naviify.app.ui.components.AlbumCard
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.player.PlayerViewModel
import java.util.Locale

@Composable
fun ArtistDetailScreen(
    onOpenAlbum: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val artist = state.artist

    when {
        state.isLoading && artist == null -> LoadingBox()
        state.error != null && artist == null -> {
            ErrorBanner(message = state.error.orEmpty(), onRetry = viewModel::load)
        }
        artist != null -> {
            val accentColor = LocalNaviifyPalette.current.accent
            var showAllTopSongs by remember { mutableStateOf(false) }
            val displayedTopSongs = if (showAllTopSongs) state.topSongs else state.topSongs.take(5)
            var selectedFilter by remember { mutableStateOf(DiscographyFilter.POPULAR) }

            val (singlesAndEps, fullAlbums) = remember(state.albums) {
                state.albums.sortedByDescending { it.year ?: 0 }.partition { it.isSingleOrEp() }
            }

            val heroImageUrl = remember(state.imageUrl, artist.coverArtId) {
                state.imageUrl ?: CoverUrls.url(artist.coverArtId, 1024)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // 1. HERO BANNER HEADER (Photo + Overlay + Title)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(310.dp),
                        ) {
                            // High-res Artist Hero Photo
                            if (!heroImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = heroImageUrl,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    accentColor.copy(alpha = 0.5f),
                                                    Color(0xFF181818),
                                                ),
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = artist.name.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.displayLarge,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            // Dark Scrim: subtle top scrim for buttons, deep bottom gradient into background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f),
                                                Color(0xCC121212),
                                                MaterialTheme.colorScheme.background,
                                            ),
                                        ),
                                    ),
                            )

                            // Top Bar Navigation (Back & Star / Favorite)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White,
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.45f),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    IconButton(onClick = viewModel::toggleFavorite) {
                                        Icon(
                                            imageVector = if (state.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            contentDescription = "Favorite artist",
                                            tint = if (state.isFavorite) SpotifyGreen else Color.White,
                                        )
                                    }
                                }
                            }

                            // Bottom Hero Text Info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                // Verified Artist Badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.padding(bottom = 6.dp),
                                ) {
                                    Text(
                                        text = "ARTIST",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }

                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                val subtitle = buildString {
                                    append("${state.albums.size} albums")
                                    if (state.topSongs.isNotEmpty()) {
                                        append(" • ${state.topSongs.size} popular tracks")
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }

                    // 2. ACTION CONTROLS ROW (Big Play Button + Shuffle + Follow)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Spotify Green Big Play Button
                            Surface(
                                shape = CircleShape,
                                color = SpotifyGreen,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.playAll(startShuffled = false)
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (state.isPlaying && state.topSongs.any { it.id == state.currentPlayingTrackId }) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = "Play",
                                        tint = Color(0xFF121212),
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            // Shuffle Button
                            val isCurrentArtistActive = playerState.currentTrack?.artistId == artist.id || playerState.currentTrack?.artist == artist.name
                            val isArtistShuffled = isCurrentArtistActive && playerState.isShuffleEnabled
                            IconButton(
                                onClick = {
                                    if (isCurrentArtistActive) {
                                        playerViewModel.setShuffle(!playerState.isShuffleEnabled)
                                    } else {
                                        viewModel.playAll(startShuffled = true)
                                    }
                                },
                                modifier = Modifier.size(42.dp),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = if (isArtistShuffled) "Shuffle artist on" else "Shuffle",
                                        tint = if (isArtistShuffled) SpotifyGreen else TextSecondary,
                                        modifier = Modifier.size(26.dp),
                                    )
                                    if (isArtistShuffled) {
                                        Spacer(Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(SpotifyGreen),
                                        )
                                    } else {
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Follow / Favorite Chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (state.isFavorite) SpotifyGreen.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (state.isFavorite) SpotifyGreen else Color.White.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { viewModel.toggleFavorite() },
                            ) {
                                Text(
                                    text = if (state.isFavorite) "FOLLOWING" else "FOLLOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (state.isFavorite) SpotifyGreen else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }

                    // 3. POPULAR SONGS SECTION ("Populaire dyask")
                    if (state.topSongs.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Popular",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp),
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                            ) {
                                displayedTopSongs.forEachIndexed { index, track ->
                                    val isCurrentPlaying = track.id == state.currentPlayingTrackId
                                    PopularTrackRow(
                                        rank = index + 1,
                                        track = track,
                                        isPlaying = isCurrentPlaying,
                                        onClick = { viewModel.playTopSong(track) },
                                    )
                                }

                                if (state.topSongs.size > 5) {
                                    Text(
                                        text = if (showAllTopSongs) "Show less" else "See more",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier
                                            .padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
                                            .clickable { showAllTopSongs = !showAllTopSongs },
                                    )
                                }
                            }
                        }
                    }

                    // 4. ALBUMS & DISCOGRAPHY SECTION (Spotify style)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(top = 18.dp)) {
                            Text(
                                text = "Discography",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            )

                            // Spotify Filter Pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DiscographyFilter.values().forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) SpotifyGreen else Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { selectedFilter = filter },
                                    ) {
                                        Text(
                                            text = filter.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.Black else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when (selectedFilter) {
                        DiscographyFilter.POPULAR -> {
                            if (state.albums.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "No releases found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            } else {
                                if (fullAlbums.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(
                                            text = "Albums",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                                        )
                                    }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            items(fullAlbums, key = { it.id }) { album ->
                                                SpotifyArtistReleaseCard(
                                                    album = album,
                                                    modifier = Modifier.width(148.dp),
                                                    onClick = { onOpenAlbum(album.id) },
                                                )
                                            }
                                        }
                                    }
                                }

                                if (singlesAndEps.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(
                                            text = "Singles and EPs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
                                        )
                                    }
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            items(singlesAndEps, key = { it.id }) { album ->
                                                SpotifyArtistReleaseCard(
                                                    album = album,
                                                    modifier = Modifier.width(148.dp),
                                                    onClick = { onOpenAlbum(album.id) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DiscographyFilter.ALBUMS -> {
                            if (fullAlbums.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "No albums found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            } else {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "Albums (${fullAlbums.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                                    )
                                }
                                items(fullAlbums, key = { it.id }) { album ->
                                    SpotifyArtistReleaseCard(
                                        album = album,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        onClick = { onOpenAlbum(album.id) },
                                    )
                                }
                            }
                        }

                        DiscographyFilter.SINGLES -> {
                            if (singlesAndEps.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "No singles or EPs found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            } else {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "Singles & EPs (${singlesAndEps.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                                    )
                                }
                                items(singlesAndEps, key = { it.id }) { album ->
                                    SpotifyArtistReleaseCard(
                                        album = album,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        onClick = { onOpenAlbum(album.id) },
                                    )
                                }
                            }
                        }
                    }

                    // 5. ABOUT THE ARTIST SECTION (Biography & details)
                    if (!state.biography.isNullOrBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                            ) {
                                Text(
                                    text = "About",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = SurfaceCardHigh,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        if (!heroImageUrl.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(170.dp),
                                            ) {
                                                AsyncImage(
                                                    model = heroImageUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(
                                                                    Color.Transparent,
                                                                    SurfaceCardHigh.copy(alpha = 0.9f),
                                                                    SurfaceCardHigh,
                                                                ),
                                                            ),
                                                        ),
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.padding(16.dp)) {
                                            var expanded by remember { mutableStateOf(false) }

                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = state.biography.orEmpty(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary,
                                                lineHeight = 20.sp,
                                                maxLines = if (expanded) Int.MAX_VALUE else 5,
                                                overflow = TextOverflow.Ellipsis,
                                            )

                                            if (state.biography.orEmpty().length > 250) {
                                                Text(
                                                    text = if (expanded) "Show less" else "Read more",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    modifier = Modifier
                                                        .padding(top = 8.dp)
                                                        .clickable { expanded = !expanded },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularTrackRow(
    rank: Int,
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank number
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isPlaying) SpotifyGreen else TextSecondary,
            modifier = Modifier.width(28.dp),
        )

        // Cover Art thumbnail
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SurfaceCardHigh,
            modifier = Modifier.size(46.dp),
        ) {
            val coverUrl = remember(track.coverArtId) {
                CoverUrls.url(track.coverArtId, 128)
            }
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Title and Album
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) SpotifyGreen else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = track.album ?: track.artist.orEmpty()
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Duration
        if (track.duration > 0) {
            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        val trackActionHandler = LocalTrackActionHandler.current
        IconButton(
            onClick = { trackActionHandler?.openOptions(track, null, null) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SpotifyArtistReleaseCard(
    album: Album,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        CoverImage(
            coverArtId = album.coverArtId,
            size = 512,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = album.releaseSubtitle(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
            ),
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class DiscographyFilter(val label: String) {
    POPULAR("Popular releases"),
    ALBUMS("Albums"),
    SINGLES("Singles and EPs"),
}

private fun Album.isSingleOrEp(): Boolean {
    val lower = name.lowercase()
    if (lower.contains("single") || lower.contains(" - ep") || lower.contains("(ep)") || lower.endsWith(" ep")) return true
    return songCount in 1..4
}

private fun Album.releaseType(): String {
    val lower = name.lowercase()
    if (lower.contains("single") || songCount in 1..2) return "Single"
    if (lower.contains("ep") || songCount in 3..6) return "EP"
    return "Album"
}

private fun Album.releaseSubtitle(): String {
    val type = releaseType()
    return if (year != null && year > 0) "$year • $type" else type
}
