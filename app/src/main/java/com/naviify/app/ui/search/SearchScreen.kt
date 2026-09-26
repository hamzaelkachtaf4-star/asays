package com.naviify.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.data.search.SearchHistoryEntry
import com.naviify.app.data.search.SearchHistoryType
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.components.AlbumCard
import com.naviify.app.ui.components.AlbumRow
import com.naviify.app.ui.components.ArtistCard
import com.naviify.app.ui.components.ArtistRow
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.EmptyState
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.components.PlaylistRow
import com.naviify.app.ui.components.SectionHeader
import com.naviify.app.ui.components.TrackRow
import com.naviify.app.ui.player.PlayerViewModel
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val accentColor = LocalNaviifyPalette.current.accent
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                viewModel.onQueryChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Search your music", color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                )
            },
            trailingIcon = {
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = {
                        query = ""
                        viewModel.onQueryChange("")
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(SearchCategory.entries, key = { it.name }) { category ->
                val isSelected = state.category == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) SpotifyGreen else Color.White.copy(alpha = 0.08f)
                        )
                        .clickable {
                            val newCategory = if (isSelected && category != SearchCategory.ALL) {
                                SearchCategory.ALL
                            } else {
                                category
                            }
                            viewModel.onCategoryChange(newCategory)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.label,
                        color = if (isSelected) Color.Black else TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        val hasCategoryResults = when (state.category) {
            SearchCategory.ALL -> !state.results.isEmpty
            SearchCategory.SONGS -> state.results.tracks.isNotEmpty()
            SearchCategory.ALBUMS -> state.results.albums.isNotEmpty()
            SearchCategory.ARTISTS -> state.results.artists.isNotEmpty()
            SearchCategory.PLAYLISTS -> state.results.playlists.isNotEmpty()
        }

        when {
            state.error != null -> {
                ErrorBanner(message = state.error.orEmpty(), onRetry = viewModel::retry)
                EmptyState("No results. Try a different search.", Modifier.weight(1f))
            }
            state.isLoading && query.isNotBlank() -> LoadingBox(Modifier.weight(1f))
            query.isBlank() -> {
                val displayedHistory = remember(state.history, state.category) {
                    when (state.category) {
                        SearchCategory.ALL -> state.history
                        SearchCategory.SONGS -> state.history.filter { it.type == SearchHistoryType.TRACK }
                        SearchCategory.ALBUMS -> state.history.filter { it.type == SearchHistoryType.ALBUM }
                        SearchCategory.ARTISTS -> state.history.filter { it.type == SearchHistoryType.ARTIST }
                        SearchCategory.PLAYLISTS -> state.history.filter { it.type == SearchHistoryType.PLAYLIST }
                    }
                }
                // Show search history when idle
                if (state.history.isEmpty()) {
                    EmptyState("Search tracks, artists, albums and playlists.", Modifier.weight(1f))
                } else if (displayedHistory.isEmpty()) {
                    EmptyState("No recent ${state.category.label.lowercase()} found.", Modifier.weight(1f))
                } else {
                    SearchHistoryList(
                        history = displayedHistory,
                        onEntryClick = { entry ->
                            when (entry.type) {
                                SearchHistoryType.TRACK -> {
                                    val track = Track(
                                        id = entry.id,
                                        title = entry.title,
                                        artist = entry.subtitle,
                                        album = entry.album,
                                        albumId = entry.albumId,
                                        coverArtId = entry.coverArtId,
                                        duration = entry.duration,
                                    )
                                    viewModel.onTrackClicked(track)
                                    playerViewModel.play(listOf(track), 0)
                                    onOpenNowPlaying()
                                }
                                SearchHistoryType.ALBUM -> onOpenAlbum(entry.id)
                                SearchHistoryType.ARTIST -> onOpenArtist(entry.id)
                                SearchHistoryType.PLAYLIST -> onOpenPlaylist(entry.id)
                                SearchHistoryType.QUERY -> {
                                    query = entry.title
                                    viewModel.onQueryChange(entry.title)
                                }
                            }
                        },
                        onRemoveEntry = viewModel::removeHistoryEntry,
                        onClearAll = viewModel::clearHistory,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            !hasCategoryResults -> {
                EmptyState(
                    if (state.category == SearchCategory.ALL) {
                        "No results for \"$query\""
                    } else {
                        "No ${state.category.label.lowercase()} found for \"$query\""
                    },
                    Modifier.weight(1f)
                )
            }
            else -> SearchResultsList(
                state = state,
                onOpenAlbum = { album ->
                    viewModel.onAlbumClicked(album)
                    onOpenAlbum(album.id)
                },
                onOpenArtist = { artist ->
                    viewModel.onArtistClicked(artist)
                    onOpenArtist(artist.id)
                },
                onOpenPlaylist = { playlist ->
                    viewModel.onPlaylistClicked(playlist)
                    onOpenPlaylist(playlist.id)
                },
                onPlayTrack = { index ->
                    val track = state.results.tracks[index]
                    viewModel.onTrackClicked(track)
                    // Precharge la pochette au format du lecteur (768 px) pendant la
                    // transition : sinon l'image se telecharge et se decode pendant le
                    // slide, ce qui fait saccader l'animation d'ouverture.
                    val coverUrl = com.naviify.app.core.image.CoverUrls.url(track.coverArtId, 768)
                    if (coverUrl != null) {
                        coil.Coil.imageLoader(context).enqueue(
                            coil.request.ImageRequest.Builder(context)
                                .data(coverUrl)
                                .memoryCacheKey("cover-${track.coverArtId}-768")
                                .diskCacheKey("cover-${track.coverArtId}-768")
                                .build(),
                        )
                    }
                    playerViewModel.play(state.results.tracks, index)
                    onOpenNowPlaying()
                },
                onToggleTrackFavorite = viewModel::toggleTrackFavorite,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchHistoryList(
    history: List<SearchHistoryEntry>,
    onEntryClick: (SearchHistoryEntry) -> Unit,
    onRemoveEntry: (SearchHistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent searches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearAll) {
                    Text(
                        text = "Clear all",
                        color = LocalNaviifyPalette.current.accent,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        items(history, key = { "${it.type}_${it.id}" }) { entry ->
            SearchHistoryRow(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onRemove = { onRemoveEntry(entry) },
            )
        }
    }
}

@Composable
private fun SearchHistoryRow(
    entry: SearchHistoryEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover art or type icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (entry.type == SearchHistoryType.ARTIST) CircleShape else RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.coverArtId != null) {
                CoverImage(
                    coverArtId = entry.coverArtId,
                    modifier = Modifier.fillMaxSize(),
                    size = 128,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            SurfaceCardHigh,
                            if (entry.type == SearchHistoryType.ARTIST) CircleShape else RoundedCornerShape(6.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (entry.type) {
                            SearchHistoryType.TRACK, SearchHistoryType.QUERY -> Icons.Rounded.MusicNote
                            SearchHistoryType.ALBUM -> Icons.Rounded.Album
                            SearchHistoryType.ARTIST -> Icons.Rounded.Person
                            SearchHistoryType.PLAYLIST -> Icons.AutoMirrored.Rounded.QueueMusic
                        },
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.subtitle != null) {
                Text(
                    text = buildString {
                        append(
                            when (entry.type) {
                                SearchHistoryType.TRACK -> "Song"
                                SearchHistoryType.ALBUM -> "Album"
                                SearchHistoryType.ARTIST -> "Artist"
                                SearchHistoryType.PLAYLIST -> "Playlist"
                                SearchHistoryType.QUERY -> "Search"
                            },
                        )
                        append(" · ")
                        append(entry.subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = when (entry.type) {
                        SearchHistoryType.TRACK -> "Song"
                        SearchHistoryType.ALBUM -> "Album"
                        SearchHistoryType.ARTIST -> "Artist"
                        SearchHistoryType.PLAYLIST -> "Playlist"
                        SearchHistoryType.QUERY -> "Search"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    state: SearchUiState,
    onOpenAlbum: (com.naviify.app.domain.model.Album) -> Unit,
    onOpenArtist: (com.naviify.app.domain.model.Artist) -> Unit,
    onOpenPlaylist: (com.naviify.app.domain.model.Playlist) -> Unit,
    onPlayTrack: (Int) -> Unit,
    onToggleTrackFavorite: (com.naviify.app.domain.model.Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        when (state.category) {
            SearchCategory.ALL -> {
                // 1. Music / Songs first!
                if (state.results.tracks.isNotEmpty()) {
                    item { SectionHeader("Songs") }
                    itemsIndexed(state.results.tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                        TrackRow(
                            index = index,
                            track = track,
                            isFavorite = track.id in state.favoriteTrackIds,
                            onClick = { onPlayTrack(index) },
                            onToggleFavorite = { onToggleTrackFavorite(track) },
                        )
                    }
                }
                // 2. Albums second!
                if (state.results.albums.isNotEmpty()) {
                    item {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.height(216.dp),
                        ) {
                            items(state.results.albums, key = { it.id }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onOpenAlbum(album) },
                                    modifier = Modifier.width(150.dp),
                                )
                            }
                        }
                    }
                }
                // 3. Artists third!
                if (state.results.artists.isNotEmpty()) {
                    item {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.height(216.dp),
                        ) {
                            items(state.results.artists, key = { it.id }) { artist ->
                                ArtistCard(
                                    artist = artist,
                                    onClick = { onOpenArtist(artist) },
                                    modifier = Modifier.width(150.dp),
                                )
                            }
                        }
                    }
                }
                // 4. Playlists fourth!
                if (state.results.playlists.isNotEmpty()) {
                    item { SectionHeader("Playlists") }
                    items(state.results.playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist) },
                        )
                    }
                }
            }
            SearchCategory.SONGS -> {
                itemsIndexed(state.results.tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    TrackRow(
                        index = index,
                        track = track,
                        isFavorite = track.id in state.favoriteTrackIds,
                        onClick = { onPlayTrack(index) },
                        onToggleFavorite = { onToggleTrackFavorite(track) },
                    )
                }
            }
            SearchCategory.ALBUMS -> {
                items(state.results.albums, key = { it.id }) { album ->
                    AlbumRow(
                        album = album,
                        onClick = { onOpenAlbum(album) },
                    )
                }
            }
            SearchCategory.ARTISTS -> {
                items(state.results.artists, key = { it.id }) { artist ->
                    ArtistRow(
                        artist = artist,
                        onClick = { onOpenArtist(artist) },
                    )
                }
            }
            SearchCategory.PLAYLISTS -> {
                items(state.results.playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist) },
                    )
                }
            }
        }
    }
}
