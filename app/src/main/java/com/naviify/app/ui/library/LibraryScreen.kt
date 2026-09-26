package com.naviify.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.download.DownloadViewModel
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.components.AlbumRow
import com.naviify.app.ui.components.ArtistRow
import com.naviify.app.ui.components.EmptyState
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.components.PlaylistRow
import com.naviify.app.ui.components.SectionHeader
import com.naviify.app.ui.components.TrackRow
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    initialCategory: LibraryCategory = LibraryCategory.ARTISTS,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.selectCategory(initialCategory)
    }
    val downloads by downloadViewModel.downloads.collectAsStateWithLifecycle()
    val downloadStatus by downloadViewModel.status.collectAsStateWithLifecycle()
    val downloadedTracks = remember(downloads) {
        downloads.map { entity ->
            Track(
                id = entity.trackId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                albumId = entity.albumId,
                coverArtId = entity.coverArtId,
                suffix = entity.suffix,
                path = entity.localFilePath,
            )
        }
    }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(LibraryCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.name.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            IconButton(
                onClick = {
                    viewModel.scanLibrary { ok ->
                        android.widget.Toast.makeText(
                            context,
                            if (ok) "Server library scan triggered" else "Failed to trigger scan",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Scan library",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        when {
            state.error != null -> ErrorBanner(message = state.error.orEmpty(), onRetry = { viewModel.selectCategory(state.category) })
            state.isLoading -> LoadingBox()
            state.category == LibraryCategory.DOWNLOADS -> DownloadsList(
                downloadedTracks = downloadedTracks,
                downloadCount = downloads.size,
                onPlayQueue = { index -> downloadViewModel.playQueue(downloadedTracks, index) },
                onDelete = downloadViewModel::deleteDownload,
                onDeleteAll = downloadViewModel::deleteAllDownloads,
            )
            else -> Content(
                state = state,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onOpenPlaylist = onOpenPlaylist,
                onPlayTrack = viewModel::playTrack,
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
    }

}

@Composable
private fun Content(
    state: LibraryUiState,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlayTrack: (com.naviify.app.domain.model.Track) -> Unit,
    onToggleFavorite: (com.naviify.app.domain.model.Favorite) -> Unit,
) {
    val favoriteIdsByType = remember(state.favorites) {
        state.favorites.groupBy { it.type }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        when (state.category) {
            LibraryCategory.ARTISTS -> {
                if (state.artists.isEmpty()) {
                    item { EmptyState("No artists found") }
                } else {
                    items(state.artists, key = { it.id }) { artist ->
                        ArtistRow(artist = artist, onClick = { onOpenArtist(artist.id) })
                    }
                }
            }
            LibraryCategory.ALBUMS -> {
                if (state.albums.isEmpty()) {
                    item { EmptyState("No albums found") }
                } else {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumRow(album = album, onClick = { onOpenAlbum(album.id) })
                    }
                }
            }
            LibraryCategory.DOWNLOADS -> Unit
            LibraryCategory.FAVORITES -> {
                if (state.favorites.isEmpty()) {
                    item { EmptyState("Nothing favorited yet. Tap the star on any album, artist or song.") }
                } else {
                    val artists = favoriteIdsByType[FavoriteType.ARTIST].orEmpty()
                    if (artists.isNotEmpty()) {
                        item { SectionHeader("Artists") }
                        items(artists, key = { it.id }) { favorite ->
                            ArtistRow(
                                artist = com.naviify.app.domain.model.Artist(
                                    id = favorite.id,
                                    name = favorite.name,
                                    coverArtId = favorite.coverArtId,
                                    albumCount = 0,
                                    isFavorite = true,
                                ),
                                onClick = { onOpenArtist(favorite.id) },
                                isFavorite = true,
                                onToggleFavorite = { onToggleFavorite(favorite) },
                            )
                        }
                    }
                    val albums = favoriteIdsByType[FavoriteType.ALBUM].orEmpty()
                    if (albums.isNotEmpty()) {
                        item { SectionHeader("Albums") }
                        items(albums, key = { it.id }) { favorite ->
                            AlbumRow(
                                album = com.naviify.app.domain.model.Album(
                                    id = favorite.id,
                                    name = favorite.name,
                                    artist = favorite.secondaryText,
                                    artistId = null,
                                    coverArtId = favorite.coverArtId,
                                    songCount = 0,
                                    duration = 0,
                                    year = null,
                                    isFavorite = true,
                                ),
                                onClick = { onOpenAlbum(favorite.id) },
                                isFavorite = true,
                                onToggleFavorite = { onToggleFavorite(favorite) },
                            )
                        }
                    }
                    val tracks = favoriteIdsByType[FavoriteType.TRACK].orEmpty()
                    if (tracks.isNotEmpty()) {
                        item { SectionHeader("Songs") }
                        itemsIndexed(tracks, key = { _, favorite -> favorite.id }) { index, favorite ->
                            TrackRow(
                                index = index,
                                track = com.naviify.app.domain.model.Track(
                                    id = favorite.id,
                                    title = favorite.name,
                                    artist = favorite.secondaryText,
                                    coverArtId = favorite.coverArtId,
                                ),
                                isFavorite = true,
                                onClick = {
                                    onPlayTrack(
                                        com.naviify.app.domain.model.Track(
                                            id = favorite.id,
                                            title = favorite.name,
                                            artist = favorite.secondaryText,
                                            coverArtId = favorite.coverArtId,
                                        ),
                                    )
                                },
                                onToggleFavorite = { onToggleFavorite(favorite) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsList(
    downloadedTracks: List<Track>,
    downloadCount: Int,
    onPlayQueue: (Int) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    text = "Downloads",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDeleteAll, enabled = downloadCount > 0) {
                    Text("Delete all")
                }
            }
        }
        if (downloadCount == 0) {
            item {
                EmptyState("No downloads yet. Tap the download icon on any track, album or playlist.")
            }
        } else {
            itemsIndexed(downloadedTracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                TrackRow(
                    index = index,
                    track = track,
                    downloadState = TrackDownloadState.DONE,
                    onDownloadToggle = { onDelete(track.id) },
                    onClick = { onPlayQueue(index) },
                )
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    name: String,
    isCreating: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New playlist", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
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
                error?.let {
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
                onClick = onCreate,
                enabled = name.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = androidx.compose.ui.graphics.Color.Black,
                ),
            ) {
                Text(if (isCreating) "Creating..." else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancel")
            }
        },
    )
}
