package com.naviify.app.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.WaterDrop
import com.naviify.app.ui.theme.NaviifyBlack
import com.naviify.app.domain.model.PlaylistMixConfig
import com.naviify.app.domain.model.PlaylistMixMode
import com.naviify.app.domain.model.MixPresets
import com.naviify.app.domain.model.CamelotKey
import com.naviify.app.domain.model.HarmonicRelationship
import com.naviify.app.domain.model.analyzeHarmonicRelationship
import com.naviify.app.domain.model.DjTrackMeta
import com.naviify.app.domain.model.realBpmOf
import com.naviify.app.domain.model.realCamelotOf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.TrackOptionRow
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.theme.ThemeOutline
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.naviify.app.domain.model.mixGains

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCoverOptionsSheet(
    playlistName: String,
    hasCustomCover: Boolean,
    onPickFromGallery: () -> Unit,
    onResetToServerCover: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Playlist Artwork",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = playlistName,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(Modifier.height(16.dp))

            CoverOptionItem(
                icon = Icons.Rounded.PhotoCamera,
                title = "Choose from Gallery",
                subtitle = "Pick a custom photo for this playlist on this device",
                tint = SpotifyGreen,
                onClick = {
                    onDismiss()
                    onPickFromGallery()
                },
            )

            if (hasCustomCover) {
                CoverOptionItem(
                    icon = Icons.Rounded.CloudSync,
                    title = "Use Navidrome Server Cover",
                    subtitle = "Remove local custom photo and sync with server's generated cover",
                    tint = Color(0xFFFFB300),
                    onClick = {
                        onDismiss()
                        onResetToServerCover()
                    },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connected to Navidrome Cover",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Currently displaying live cover/collage generated by Navidrome",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CoverOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceCard),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistTrackOptionsSheet(
    track: Track,
    downloadState: TrackDownloadState? = null,
    onShare: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onViewQueue: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onDownloadToggle: (() -> Unit)? = null,
    onMoveToTop: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    coverArtId = track.coverArtId,
                    size = 256,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist ?: track.album.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = ThemeOutline.copy(alpha = 0.2f))
            Spacer(Modifier.height(8.dp))

            TrackOptionRow(
                icon = Icons.Rounded.Share,
                label = "Share song",
                onClick = {
                    onDismiss()
                    onShare()
                },
            )
            TrackOptionRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = "Add to playlist",
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                },
            )
            TrackOptionRow(
                icon = Icons.Rounded.Queue,
                label = "Add to queue",
                onClick = {
                    onDismiss()
                    onAddToQueue()
                },
            )
            if (onViewQueue != null) {
                TrackOptionRow(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = "View queue",
                    onClick = {
                        onDismiss()
                        onViewQueue()
                    },
                )
            }
            TrackOptionRow(
                icon = Icons.Rounded.Album,
                label = "Go to album",
                onClick = {
                    onDismiss()
                    onGoToAlbum?.invoke()
                },
                enabled = onGoToAlbum != null,
            )
            TrackOptionRow(
                icon = Icons.Rounded.Person,
                label = "Go to artist",
                onClick = {
                    onDismiss()
                    onGoToArtist?.invoke()
                },
                enabled = onGoToArtist != null,
            )

            // Download / Remove download option
            when (downloadState) {
                TrackDownloadState.DONE -> {
                    TrackOptionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = "Remove download",
                        onClick = {
                            onDismiss()
                            onDownloadToggle?.invoke()
                        },
                        enabled = onDownloadToggle != null,
                        tint = Color(0xFFEF5350),
                    )
                }
                TrackDownloadState.DOWNLOADING -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = SpotifyGreen,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Downloading...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SpotifyGreen,
                        )
                    }
                }
                TrackDownloadState.FAILED -> {
                    TrackOptionRow(
                        icon = Icons.Rounded.Refresh,
                        label = "Retry download",
                        onClick = {
                            onDismiss()
                            onDownloadToggle?.invoke()
                        },
                        enabled = onDownloadToggle != null,
                    )
                }
                null -> {
                    TrackOptionRow(
                        icon = Icons.Rounded.Download,
                        label = "Download song",
                        onClick = {
                            onDismiss()
                            onDownloadToggle?.invoke()
                        },
                        enabled = onDownloadToggle != null,
                    )
                }
            }

            // Reorder options within playlist
            if (onMoveToTop != null || onMoveUp != null || onMoveDown != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = ThemeOutline.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))

                if (onMoveToTop != null) {
                    TrackOptionRow(
                        icon = Icons.Rounded.VerticalAlignTop,
                        label = "Move to top",
                        onClick = {
                            onDismiss()
                            onMoveToTop()
                        },
                    )
                }
                if (onMoveUp != null) {
                    TrackOptionRow(
                        icon = Icons.Rounded.ArrowUpward,
                        label = "Move up",
                        onClick = {
                            onDismiss()
                            onMoveUp()
                        },
                    )
                }
                if (onMoveDown != null) {
                    TrackOptionRow(
                        icon = Icons.Rounded.ArrowDownward,
                        label = "Move down",
                        onClick = {
                            onDismiss()
                            onMoveDown()
                        },
                    )
                }
            }

            // Remove from Playlist option
            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Remove from Playlist",
                    onClick = {
                        onDismiss()
                        onRemoveFromPlaylist()
                    },
                    tint = Color(0xFFFF5252),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyEditPlaylistSheet(
    playlistName: String,
    playlistComment: String,
    isPublic: Boolean,
    coverUrl: String?,
    isUpdating: Boolean = false,
    onChangeCover: () -> Unit,
    onSave: (name: String, description: String, isPublic: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(playlistName) { mutableStateOf(playlistName) }
    var description by remember(playlistComment) { mutableStateOf(playlistComment) }
    var publicState by remember(isPublic) { mutableStateOf(isPublic) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header bar: Cancel - Edit details - Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = "Edit details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Button(
                    onClick = { onSave(name, description, publicState) },
                    enabled = name.isNotBlank() && !isUpdating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        disabledContainerColor = SpotifyGreen.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black,
                        )
                    } else {
                        Text(
                            text = "Save",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Cover Image with Camera Edit Overlay
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onChangeCover),
                contentAlignment = Alignment.Center,
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Playlist cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF282828)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }

                // Dark translucent overlay with camera icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                        Text(
                            text = "Change photo",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Playlist Name Input Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = SpotifyGreen,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = SpotifyGreen,
                    unfocusedLabelColor = TextSecondary,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Playlist Description Input Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = SpotifyGreen,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = SpotifyGreen,
                    unfocusedLabelColor = TextSecondary,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Public / Private Privacy Switch
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (publicState) Icons.Rounded.Public else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (publicState) SpotifyGreen else TextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (publicState) "Public playlist" else "Private playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (publicState) {
                                "Anyone on your Navidrome server can see this playlist"
                            } else {
                                "Only you can see this playlist"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = publicState,
                        onCheckedChange = { publicState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpotifyGreen,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistSheet(
    existingTrackIds: Set<String>,
    onFetchLibraryTracks: suspend () -> List<Track>,
    onAddTrack: (Track) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var allTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val addedIds = remember { mutableStateOf(existingTrackIds.toMutableSet()) }

    LaunchedEffect(Unit) {
        isLoading = true
        allTracks = onFetchLibraryTracks()
        isLoading = false
    }

    val filteredTracks = remember(query, allTracks) {
        if (query.isBlank()) allTracks
        else {
            val q = query.trim().lowercase()
            allTracks.filter {
                it.title.lowercase().contains(q) ||
                    (it.artist?.lowercase()?.contains(q) == true) ||
                    (it.album?.lowercase()?.contains(q) == true)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
        modifier = Modifier.fillMaxHeight(0.85f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Add Songs to Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
                placeholder = { Text("Search songs or artists...", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = ThemeOutline.copy(alpha = 0.5f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
                filteredTracks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (query.isBlank()) "No songs available in library" else "No songs matching \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isAdded = addedIds.value.contains(track.id)
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAdded) {
                                        addedIds.value = (addedIds.value + track.id).toMutableSet()
                                        onAddTrack(track)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CoverImage(
                                        coverArtId = track.coverArtId,
                                        size = 128,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = track.artist ?: "Unknown artist",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (isAdded) {
                                        Surface(
                                            color = SpotifyGreen.copy(alpha = 0.15f),
                                            shape = CircleShape,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = SpotifyGreen,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                                Text(
                                                    text = "Added",
                                                    color = SpotifyGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                addedIds.value = (addedIds.value + track.id).toMutableSet()
                                                onAddTrack(track)
                                            },
                                            modifier = Modifier.size(34.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = "Add to playlist",
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
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMixStudioSheet(
    playlistName: String,
    config: PlaylistMixConfig,
    onToggleEnabled: () -> Unit,
    onSetMode: (PlaylistMixMode) -> Unit,
    onSetDuration: (Float) -> Unit,
    onSetSmartBassSwap: (Boolean) -> Unit,
    onSetEqualPowerVolume: (Boolean) -> Unit,
    onReorderHarmonicFlow: () -> Unit,
    onResetAllOverrides: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (config.isEnabled) SpotifyGreen.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = if (config.isEnabled) SpotifyGreen else TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mix",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "DJ transitions for \"$playlistName\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Activation Card (Switch)
            Surface(
                color = if (config.isEnabled) SpotifyGreen.copy(alpha = 0.12f) else SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (config.isEnabled) SpotifyGreen.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Mix for this playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Seamlessly blends tracks with zero dead air",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpotifyGreen,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        ),
                    )
                }
            }

            if (config.isEnabled) {
                Spacer(Modifier.height(20.dp))

                // Custom Transitions Overview badge if any exist
                if (config.transitionOverrides.isNotEmpty()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${config.transitionOverrides.size} Custom Transition(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Custom blends applied to specific track bridges",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            TextButton(onClick = onResetAllOverrides) {
                                Text(
                                    text = "Reset All",
                                    color = SpotifyGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Section: DEFAULT TRANSITION STYLE
                Text(
                    text = "PLAYLIST DEFAULT TRANSITION STYLE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PlaylistMixMode.values().forEach { mode ->
                        val isSelected = config.mode == mode
                        Surface(
                            onClick = { onSetMode(mode) },
                            color = if (isSelected) SpotifyGreen.copy(alpha = 0.12f) else SurfaceCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SpotifyGreen else Color.White.copy(alpha = 0.06f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SpotifyGreen.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = mixModeIcon(mode),
                                        contentDescription = null,
                                        tint = if (isSelected) SpotifyGreen else TextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = mode.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                        )
                                        if (mode == PlaylistMixMode.AUTO) {
                                            Surface(
                                                color = SpotifyGreen,
                                                shape = RoundedCornerShape(4.dp),
                                            ) {
                                                Text(
                                                    text = "RECOMMENDED",
                                                    color = NaviifyBlack,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = TextSecondary,
                                    )
                                }
                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                MixCurvePreview(
                    mode = config.mode,
                    equalPower = config.equalPowerVolume,
                    durationSeconds = config.durationSeconds,
                )

                Spacer(Modifier.height(20.dp))

                // Section: TIMING & OVERLAP
                Text(
                    text = "TRANSITION TIMING",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Overlap Duration",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                            Surface(
                                color = SpotifyGreen.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1fs", config.durationSeconds),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SpotifyGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = config.durationSeconds,
                            onValueChange = onSetDuration,
                            valueRange = 2f..12f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = SpotifyGreen,
                                activeTrackColor = SpotifyGreen,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("2.0s (Fast cut)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("12.0s (Long blend)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Section: AUDIO & EQ SHAPING
                Text(
                    text = "AUDIO & EQ SHAPING",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Smart Bass Swap
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Smart Bass Swap",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Cuts low-end frequencies on outgoing track to prevent clashing basslines",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = config.smartBassSwap,
                                onCheckedChange = onSetSmartBassSwap,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SpotifyGreen,
                                ),
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                        // Equal-Power Curve
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Equal-Power Volume Curve",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Maintains constant perceived loudness through the transition",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = config.equalPowerVolume,
                                onCheckedChange = onSetEqualPowerVolume,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SpotifyGreen,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Section: SMART REORDER (HARMONIC FLOW)
                Surface(
                    onClick = onReorderHarmonicFlow,
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Order by Harmonic Flow",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Arranges playlist for smooth, natural tempo and Camelot key progression",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "Done",
                    color = NaviifyBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Icon of a transition mode, shared by both mix sheets so they stay identical. */
private fun mixModeIcon(mode: PlaylistMixMode): ImageVector = when (mode) {
    PlaylistMixMode.AUTO -> Icons.Rounded.AutoAwesome
    PlaylistMixMode.FADE -> Icons.Rounded.GraphicEq
    PlaylistMixMode.RISE -> Icons.AutoMirrored.Rounded.TrendingUp
    PlaylistMixMode.MELT -> Icons.Rounded.WaterDrop
    PlaylistMixMode.SLAM -> Icons.Rounded.Bolt
}

/** Horizontal chip row: one tap to switch transition, Spotify-filter style. */
@Composable
private fun MixPresetChipsRow(
    selected: PlaylistMixMode,
    onSelect: (PlaylistMixMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaylistMixMode.values().forEach { mode ->
            val isSelected = mode == selected
            Surface(
                onClick = { onSelect(mode) },
                color = if (isSelected) SpotifyGreen else SurfaceCard,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) SpotifyGreen else Color.White.copy(alpha = 0.10f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = mixModeIcon(mode),
                        contentDescription = null,
                        tint = if (isSelected) NaviifyBlack else TextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = mode.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) NaviifyBlack else TextPrimary,
                    )
                }
            }
        }
    }
}

/**
 * Apercu de l'enveloppe reelle du mode choisi : gain sortant (pointille) et gain
 * entrant (plein, couleur d'accent) sur la fenetre de recouvrement. Les valeurs
 * viennent de [mixGains], la meme fonction que celle utilisee pour l'audio.
 */
@Composable
private fun MixCurvePreview(
    mode: PlaylistMixMode,
    equalPower: Boolean,
    durationSeconds: Float,
    modifier: Modifier = Modifier,
) {
    val outgoingColor = TextSecondary.copy(alpha = 0.70f)
    val incomingColor = SpotifyGreen
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TRANSITION CURVE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = SpotifyGreen.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fs", durationSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.28f)),
            ) {
                val w = size.width
                val h = size.height

                for (i in 1 until 8) {
                    val x = w * i / 8f
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f,
                    )
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(0f, h / 2f),
                    end = Offset(w, h / 2f),
                    strokeWidth = 1f,
                )

                val steps = 48
                val outgoing = Path()
                val incoming = Path()
                for (i in 0..steps) {
                    val p = i.toFloat() / steps
                    val gains = mixGains(p, mode, equalPower)
                    val x = w * p
                    val yOut = h * (1f - gains.first)
                    val yIn = h * (1f - gains.second)
                    if (i == 0) {
                        outgoing.moveTo(x, yOut)
                        incoming.moveTo(x, yIn)
                    } else {
                        outgoing.lineTo(x, yOut)
                        incoming.lineTo(x, yIn)
                    }
                }

                drawPath(
                    path = outgoing,
                    color = outgoingColor,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    ),
                )
                drawPath(
                    path = incoming,
                    color = incomingColor,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(incomingColor),
                )
                Spacer(Modifier.width(6.dp))
                Text("Incoming", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.width(14.dp))
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(outgoingColor),
                )
                Spacer(Modifier.width(6.dp))
                Text("Outgoing", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

/**
 * Reglages MANUELS de la transition (l'equivalent du "deplace les morceaux" de
 * Spotify) : on choisit ou le morceau sortant quitte et ou l'entrant demarre.
 * Les valeurs sont appliquees par PlaybackController (transitionStartMs) et
 * persistent dans PlaylistMixStore.
 */
@Composable
private fun MixManualTimingCard(
    outroOffsetMs: Long,
    introSkipMs: Long,
    durationSeconds: Float,
    onTimingChange: (Long, Long) -> Unit,
) {
    // Couleurs capturees AVANT les lambdas : getters @Composable du theme.
    val accent = SpotifyGreen
    val titleColor = TextPrimary
    val hintColor = TextSecondary

    val outroSeconds = (outroOffsetMs / 1000f).coerceIn(-20f, 0f)
    val introSeconds = (introSkipMs / 1000f).coerceIn(0f, 20f)
    val us = java.util.Locale.US

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manual timing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                    Text(
                        text = "Move where each track enters and exits",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = hintColor,
                    )
                }
                TextButton(onClick = { onTimingChange(0L, 0L) }) {
                    Text(
                        text = "Reset",
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            MixTimingBar(
                outroOffsetSeconds = outroSeconds,
                introSkipSeconds = introSeconds,
                blendSeconds = durationSeconds,
            )

            Spacer(Modifier.height(14.dp))

            MixTimingSlider(
                title = "Outgoing exit",
                valueText = if (outroOffsetMs == 0L) {
                    "At the natural end"
                } else {
                    "Leaves " + String.format(us, "%.1f", -outroSeconds) + "s early"
                },
                value = outroSeconds,
                range = -20f..0f,
                onChange = { onTimingChange((it * 1000f).toLong(), introSkipMs) },
            )

            Spacer(Modifier.height(10.dp))

            MixTimingSlider(
                title = "Incoming entry",
                valueText = if (introSkipMs == 0L) {
                    "From the first second"
                } else {
                    "Skips " + String.format(us, "%.1f", introSeconds) + "s of intro"
                },
                value = introSeconds,
                range = 0f..20f,
                onChange = { onTimingChange(outroOffsetMs, (it * 1000f).toLong()) },
            )
        }
    }
}

@Composable
private fun MixTimingSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    val accent = SpotifyGreen
    val titleColor = TextPrimary
    val hintColor = TextSecondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = hintColor,
            )
        }
        Slider(
            value = value,
            onValueChange = { raw -> onChange(raw.coerceIn(range.start, range.endInclusive)) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Schema du placement de la transition (poids relatifs, aucune waveform inventee) :
 * le sortant raccourcit quand il quitte plus tot, la zone de blend suit la duree
 * choisie, l'entrant est decale de l'intro sautee.
 */
@Composable
private fun MixTimingBar(
    outroOffsetSeconds: Float,
    introSkipSeconds: Float,
    blendSeconds: Float,
) {
    val outgoingColor = TextSecondary.copy(alpha = 0.55f)
    val blendColor = SpotifyGreen.copy(alpha = 0.30f)
    val incomingColor = SpotifyGreen
    val skipColor = Color.White.copy(alpha = 0.10f)
    val hintColor = TextSecondary
    val us = java.util.Locale.US

    val wOut = (40f - (-outroOffsetSeconds / 20f) * 12f).coerceAtLeast(6f)
    val wBlend = (4f + blendSeconds * 0.9f).coerceAtLeast(4f)
    val wSkip = introSkipSeconds * 0.5f
    val wIn = 30f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(wOut)
                    .fillMaxHeight()
                    .background(outgoingColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "OUT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .weight(wBlend)
                    .fillMaxHeight()
                    .background(blendColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = String.format(us, "%.0fs", blendSeconds),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            }
            if (wSkip > 0.5f) {
                Box(
                    modifier = Modifier
                        .weight(wSkip)
                        .fillMaxHeight()
                        .background(skipColor),
                )
            }
            Box(
                modifier = Modifier
                    .weight(wIn)
                    .fillMaxHeight()
                    .background(incomingColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "IN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = Color.Black,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Outgoing leaves here",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = hintColor,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Incoming starts here",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = hintColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistTransitionBridgeSheet(
    fromTrack: Track,
    toTrack: Track,
    currentMode: PlaylistMixMode,
    defaultMode: PlaylistMixMode,
    isCustomOverride: Boolean,
    onSelectMode: (PlaylistMixMode) -> Unit,
    onApplyToAll: (PlaylistMixMode) -> Unit,
    onResetToDefault: () -> Unit,
    equalPowerVolume: Boolean = true,
    durationSeconds: Float = 6f,
    outroOffsetMs: Long = 0L,
    introSkipMs: Long = 0L,
    onTimingChange: (Long, Long) -> Unit = { _, _ -> },
    /** BPM + Camelot mesures (analyseur serveur), cle = Track.id. */
    djMeta: Map<String, DjTrackMeta> = emptyMap(),
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Valeurs MESUREES uniquement (analyseur serveur) : plus rien n'est fabrique.
    val camelotA = realCamelotOf(fromTrack, djMeta)
    val camelotB = realCamelotOf(toTrack, djMeta)
    val bpmA = realBpmOf(fromTrack, djMeta)
    val bpmB = realBpmOf(toTrack, djMeta)
    val bpmDiff = if (bpmA != null && bpmB != null) bpmB - bpmA else null
    val harmony = if (camelotA != null && camelotB != null) {
        analyzeHarmonicRelationship(camelotA, camelotB)
    } else {
        null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .fillMaxHeight(0.88f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transition Style",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Customize blend for this song pair",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Two-track connector preview card
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Track A (Outgoing)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            CoverImage(
                                coverArtId = fromTrack.coverArtId,
                                size = 120,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = fromTrack.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = fromTrack.artist.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                camelotA?.let { key ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(key.colorHex),
                                    ) {
                                        Text(
                                            text = key.code,
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                                if (bpmA != null) {
                                    Text(
                                        text = "$bpmA bpm",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                    )
                                }
                                if (camelotA == null && bpmA == null) {
                                    Text(
                                        text = "Not analysed",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }

                        // Center Arrow / Delta
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = when {
                                    bpmDiff == null -> "—"
                                    bpmDiff > 0 -> "+$bpmDiff bpm"
                                    bpmDiff < 0 -> "$bpmDiff bpm"
                                    else -> "Sync bpm"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (bpmDiff != null && kotlin.math.abs(bpmDiff) <= 4) SpotifyGreen else TextSecondary,
                            )
                        }

                        // Track B (Incoming)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                        ) {
                            CoverImage(
                                coverArtId = toTrack.coverArtId,
                                size = 120,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = toTrack.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = toTrack.artist.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                camelotB?.let { key ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(key.colorHex),
                                    ) {
                                        Text(
                                            text = key.code,
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                                if (bpmB != null) {
                                    Text(
                                        text = "$bpmB bpm",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                    )
                                }
                                if (camelotB == null && bpmB == null) {
                                    Text(
                                        text = "Not analysed",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(Modifier.height(10.dp))

                    // Relation harmonique : affichee seulement si les DEUX cles sont mesurees.
                    harmony?.let { rel ->
                        Surface(
                            color = Color(rel.colorHex).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(rel.colorHex).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(rel.colorHex)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = rel.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(rel.colorHex),
                                    )
                                    Text(
                                        text = rel.description,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Courbe reellement appliquee a l'audio pour ce mode (mixGains)
            MixCurvePreview(
                mode = currentMode,
                equalPower = equalPowerVolume,
                durationSeconds = durationSeconds,
            )

            Spacer(Modifier.height(16.dp))

            // Reglages manuels (in/out points) : ou le sortant quitte, ou l'entrant
            // demarre, combien de temps dure le blend.
            MixManualTimingCard(
                outroOffsetMs = outroOffsetMs,
                introSkipMs = introSkipMs,
                durationSeconds = durationSeconds,
                onTimingChange = onTimingChange,
            )

            Spacer(Modifier.height(20.dp))

            // Section: Choose Mode for this bridge
            Text(
                text = "TRANSITION STYLE FOR THIS TRACK PAIR",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            // Le menu Mix : les cinq familles de presets, en pages balayables.
            MixPresetPager(
                selectedPresetId = MixPresets.closest(currentMode)?.id,
                onPresetSelected = { preset -> onSelectMode(preset.mode) },
            )

            Spacer(Modifier.height(12.dp))

            // Detail du style selectionne : les 5 grandes cartes empilees sont
            // remplacees par les chips ci-dessus + une seule fiche.
            Surface(
                color = SpotifyGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, SpotifyGreen),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = mixModeIcon(currentMode),
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = currentMode.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            if (currentMode == defaultMode) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = "DEFAULT",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            if (isCustomOverride) {
                                Surface(
                                    color = SpotifyGreen,
                                    shape = RoundedCornerShape(4.dp),
                                ) {
                                    Text(
                                        text = "CUSTOMIZED",
                                        color = NaviifyBlack,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentMode.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Primary: Save for this transition
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "Save for this transition",
                    color = NaviifyBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            if (isCustomOverride) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onResetToDefault,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Reset to playlist default (${defaultMode.title})",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Secondary subtle action: Apply to all
            TextButton(
                onClick = {
                    onApplyToAll(currentMode)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Apply \"${currentMode.title}\" to ALL songs in playlist",
                    color = TextSecondary.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

