package com.naviify.app.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.ui.components.AlbumCard
import com.naviify.app.ui.components.ArtistRow
import com.naviify.app.ui.components.EmptyState
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.components.PlaylistCard
import com.naviify.app.ui.components.PlaylistRow
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (String) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenAlbum: (String) -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The auto-generated "My own" playlist is the whole server library rather
    // than a user-made playlist, so it stays pinned above whatever sort is
    // active instead of competing with the user's own ordering.
    val visiblePlaylists = remember(
        state.playlists,
        state.filter,
        state.sortOrder,
        state.lastPlayedAt,
    ) {
        val virtualLibraryId = com.naviify.app.data.repository.MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID
        val isMasterLibrary = { pl: com.naviify.app.domain.model.Playlist ->
            pl.id == virtualLibraryId || pl.name.trim().equals(com.naviify.app.data.repository.MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)
        }
        val (pinned, rest) = state.playlists.partition(isMasterLibrary)
        val sorted = when (state.sortOrder) {
            PlaylistSort.RECENTLY_ADDED -> rest.sortedWith(
                compareByDescending<com.naviify.app.domain.model.Playlist> { it.created.orEmpty() }
                    .thenByDescending { it.changed.orEmpty() }
                    .thenBy { it.name.lowercase() },
            )
            PlaylistSort.RECENTLY_PLAYED -> rest.sortedWith(
                compareByDescending<com.naviify.app.domain.model.Playlist> {
                    state.lastPlayedAt[it.id] ?: 0L
                }.thenBy { it.name.lowercase() },
            )
            PlaylistSort.ALPHABETICAL -> rest.sortedBy { it.name.lowercase() }
            PlaylistSort.MOST_SONGS -> rest.sortedWith(
                compareByDescending<com.naviify.app.domain.model.Playlist> { it.songCount }
                    .thenBy { it.name.lowercase() },
            )
        }
        val ordered = pinned + sorted
        when (state.filter) {
            LibraryFilter.ALL -> ordered
            LibraryFilter.PLAYLISTS -> ordered
            LibraryFilter.ARTISTS, LibraryFilter.ALBUMS -> emptyList()
            LibraryFilter.DOWNLOADS -> emptyList()
            LibraryFilter.FAVORITES -> emptyList()
        }
    }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSilently()
        onPauseOrDispose { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                val countLabel = when (state.filter) {
                    LibraryFilter.ARTISTS -> if (state.artists.isNotEmpty()) {
                        "${state.artists.size} ${if (state.artists.size == 1) "artist" else "artists"}"
                    } else {
                        null
                    }
                    LibraryFilter.ALBUMS -> if (state.albums.isNotEmpty()) {
                        "${state.albums.size} ${if (state.albums.size == 1) "album" else "albums"}"
                    } else {
                        null
                    }
                    else -> if (visiblePlaylists.isNotEmpty()) {
                        "${visiblePlaylists.size} ${if (visiblePlaylists.size == 1) "playlist" else "playlists"} · ${state.sortOrder.label}"
                    } else {
                        null
                    }
                }
                if (countLabel != null) {
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            // Sort selector. The active order is echoed under the title so the
            // user can see what is applied without opening the menu. Artists and
            // albums are server-ordered, so the control is hidden for those.
            if (state.filter != LibraryFilter.ARTISTS && state.filter != LibraryFilter.ALBUMS) {
            Box {
                IconButton(onClick = viewModel::toggleSortMenu) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = "Sort playlists",
                        tint = if (state.isSortMenuVisible) SpotifyGreen else TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DropdownMenu(
                    expanded = state.isSortMenuVisible,
                    onDismissRequest = viewModel::dismissSortMenu,
                    containerColor = SurfaceCardHigh,
                ) {
                    PlaylistSort.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    color = if (state.sortOrder == order) SpotifyGreen else TextPrimary,
                                    fontWeight = if (state.sortOrder == order) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            onClick = { viewModel.setSortOrder(order) },
                        )
                    }
                }
            }
            }
            // Spotify-style layout switch, persisted between launches.
            IconButton(onClick = {
                viewModel.setLayout(
                    if (state.layout == PlaylistLayout.GRID) PlaylistLayout.LIST else PlaylistLayout.GRID
                )
            }) {
                Icon(
                    imageVector = if (state.layout == PlaylistLayout.GRID) {
                        Icons.AutoMirrored.Rounded.ViewList
                    } else {
                        Icons.Rounded.GridView
                    },
                    contentDescription = if (state.layout == PlaylistLayout.GRID) {
                        "Switch to list view"
                    } else {
                        "Switch to grid view"
                    },
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = viewModel::pullRefresh,
                modifier = Modifier.padding(end = 4.dp),
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = SpotifyGreen,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Sync playlists",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = SpotifyGreen.copy(alpha = 0.15f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = viewModel::showCreateDialog),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New playlist",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreen,
                    )
                }
            }
        }

        // Spotify-style filter bubbles.
        if (state.playlists.isNotEmpty() || state.artists.isNotEmpty() || state.albums.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listItems(
                    items = LibraryFilter.entries.toList(),
                    key = { it.name },
                ) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.selectFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = Color.Black,
                            labelColor = TextSecondary,
                        ),
                    )
                }
            }
        }

        when {
            state.error != null && state.filter == LibraryFilter.ALL ->
                ErrorBanner(state.error.orEmpty(), onRetry = viewModel::refresh)
            state.filter == LibraryFilter.ARTISTS -> {
                when {
                    state.isLoadingArtists && state.artists.isEmpty() -> LoadingBox()
                    state.artists.isEmpty() -> EmptyState("No artists found.")
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    ) {
                        listItems(
                            items = state.artists,
                            key = { it.id },
                        ) { artist ->
                            ArtistRow(
                                artist = artist,
                                onClick = { onOpenArtist(artist.id) },
                            )
                        }
                    }
                }
            }
            state.filter == LibraryFilter.ALBUMS -> {
                when {
                    state.isLoadingAlbums && state.albums.isEmpty() -> LoadingBox()
                    state.albums.isEmpty() -> EmptyState("No albums found.")
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        gridItems(
                            items = state.albums,
                            key = { it.id },
                        ) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { onOpenAlbum(album.id) },
                            )
                        }
                    }
                }
            }
            state.isLoading && state.playlists.isEmpty() -> LoadingBox()
            state.playlists.isEmpty() -> EmptyState("No playlists yet. Tap 'New' to create one.")
            visiblePlaylists.isEmpty() -> EmptyState("Nothing in ${state.filter.label.lowercase()} yet.")
            state.layout == PlaylistLayout.GRID -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                gridItems(
                    items = visiblePlaylists,
                    key = { it.id },
                ) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist.id) },
                    )
                }
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listItems(
                    items = visiblePlaylists,
                    key = { it.id },
                ) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist.id) },
                    )
                }
            }
        }
    }

    if (state.isCreateDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("New playlist", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.newPlaylistName,
                        onValueChange = viewModel::onNameChange,
                        singleLine = true,
                        placeholder = { Text("Playlist name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = SpotifyGreen,
                        ),
                    )
                    state.createError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::createPlaylist,
                    enabled = state.newPlaylistName.isNotBlank() && !state.isCreating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(if (state.isCreating) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCreateDialog, enabled = !state.isCreating) {
                    Text("Cancel")
                }
            },
        )
    }
}
