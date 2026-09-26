package com.naviify.app.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.ui.components.AmbientGlassBackdrop
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.components.SectionHeader
import com.naviify.app.ui.components.TrackRow
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.download.DownloadViewModel
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.player.PlayerViewModel

@Composable
fun AlbumDetailScreen(
    onOpenArtist: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadStatus by downloadViewModel.status.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val album = state.album
    val context = LocalContext.current
    val allDownloaded = state.tracks.isNotEmpty() && state.tracks.all {
        downloadStatus.byTrackId[it.id] == TrackDownloadState.DONE
    }
    val isDownloading = state.tracks.any {
        downloadStatus.byTrackId[it.id] == TrackDownloadState.DOWNLOADING
    }

    when {
        state.isLoading && album == null -> LoadingBox()
        state.error != null && album == null -> {
            ErrorBanner(message = state.error.orEmpty(), onRetry = viewModel::load)
        }
        album != null -> {
            val accentColor = LocalNaviifyPalette.current.accent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Ambient gradient backdrop in the album's colour
                AmbientGlassBackdrop(
                    accentColor = accentColor,
                    coverArtId = album.coverArtId,
                    backdropHeight = 520.dp,
                )

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 6.dp),
                            ) {
                                // Ambient glowing radial aura behind album cover
                                Box(
                                    modifier = Modifier
                                        .size(265.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    accentColor.copy(alpha = 0.45f),
                                                    accentColor.copy(alpha = 0.15f),
                                                    Color.Transparent,
                                                ),
                                            ),
                                            shape = CircleShape,
                                        ),
                                )
                                CoverImage(
                                    coverArtId = album.coverArtId,
                                    size = 512,
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(8.dp))
                            album.artistId?.let { artistId ->
                                val artistModifier = if (artistId.isNotBlank()) {
                                    Modifier.clickable { onOpenArtist(artistId) }
                                } else {
                                    Modifier
                                }
                                Text(
                                    text = album.artist ?: "Unknown Artist",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accentColor,
                                    modifier = artistModifier,
                                )
                            }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${album.songCount} songs · ${album.year ?: "unknown year"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    if (allDownloaded) {
                                        Toast.makeText(context, "Album already downloaded", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val missingCount = state.tracks.count {
                                            downloadStatus.byTrackId[it.id] != TrackDownloadState.DONE
                                        }
                                        if (missingCount > 0) {
                                            Toast.makeText(context, "Downloading $missingCount songs...", Toast.LENGTH_SHORT).show()
                                        }
                                        downloadViewModel.downloadTracks(state.tracks)
                                    }
                                }
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = SpotifyGreen,
                                    )
                                } else if (allDownloaded) {
                                    Icon(
                                        imageVector = Icons.Rounded.DownloadDone,
                                        contentDescription = "Album downloaded",
                                        tint = SpotifyGreen,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = "Download album",
                                        tint = SpotifyGreen,
                                    )
                                }
                            }
                            IconButton(onClick = viewModel::toggleAlbumFavorite) {
                                Icon(
                                    imageVector = if (state.isAlbumFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Favorite album",
                                    tint = if (state.isAlbumFavorite) SpotifyGreen else TextPrimary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            FilledIconButton(
                                onClick = { viewModel.playFrom(0) },
                                modifier = Modifier.size(56.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play album",
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            val isCurrentAlbumActive = playerState.currentTrack?.albumId == album.id
                            val isAlbumShuffled = isCurrentAlbumActive && playerState.isShuffleEnabled
                            IconButton(
                                onClick = {
                                    if (isCurrentAlbumActive) {
                                        playerViewModel.setShuffle(!playerState.isShuffleEnabled)
                                    } else {
                                        viewModel.playShuffled()
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = if (isAlbumShuffled) "Shuffle album on" else "Shuffle album",
                                        tint = if (isAlbumShuffled) SpotifyGreen else TextSecondary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    if (isAlbumShuffled) {
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
                        }
                    }
                }
                item { SectionHeader("Songs") }
                itemsIndexed(state.tracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    val trackState = downloadStatus.byTrackId[track.id]
                    TrackRow(
                        index = index,
                        track = track,
                        isFavorite = track.id in state.favoriteTrackIds,
                        onClick = { viewModel.playFrom(index) },
                        onToggleFavorite = { viewModel.toggleTrackFavorite(track) },
                        downloadState = trackState,
                        onDownloadToggle = if (trackState == TrackDownloadState.DOWNLOADING) {
                            null
                        } else {
                            {
                                if (trackState == TrackDownloadState.DONE) {
                                    downloadViewModel.deleteDownload(track.id)
                                } else {
                                    downloadViewModel.downloadTrack(track)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
}
