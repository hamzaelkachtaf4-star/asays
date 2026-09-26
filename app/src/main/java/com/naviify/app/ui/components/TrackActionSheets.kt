package com.naviify.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.theme.ThemeOutline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    downloadState: TrackDownloadState?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onViewQueue: () -> Unit,
    onGoToAlbum: (() -> Unit)?,
    onGoToArtist: (() -> Unit)?,
    onDownloadToggle: (() -> Unit)?,
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
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
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

        Spacer(Modifier.height(8.dp))

        TrackOptionRow(
            icon = Icons.Rounded.Share,
            label = "Share song",
            onClick = onShare,
        )
        TrackOptionRow(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            label = "Add to playlist",
            onClick = onAddToPlaylist,
        )
        TrackOptionRow(
            icon = Icons.Rounded.Queue,
            label = "Add to queue",
            onClick = onAddToQueue,
        )
        TrackOptionRow(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = "View queue",
            onClick = onViewQueue,
        )
        TrackOptionRow(
            icon = Icons.Rounded.Album,
            label = "Go to album",
            onClick = onGoToAlbum ?: {},
            enabled = onGoToAlbum != null,
        )
        TrackOptionRow(
            icon = Icons.Rounded.Person,
            label = "Go to artist",
            onClick = onGoToArtist ?: {},
            enabled = onGoToArtist != null,
        )

        // Download / Remove download option
        when (downloadState) {
            TrackDownloadState.DONE -> {
                TrackOptionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Remove download",
                    onClick = onDownloadToggle ?: {},
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
                    onClick = onDownloadToggle ?: {},
                    enabled = onDownloadToggle != null,
                )
            }
            null -> {
                TrackOptionRow(
                    icon = Icons.Rounded.Download,
                    label = "Download song",
                    onClick = onDownloadToggle ?: {},
                    enabled = onDownloadToggle != null,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun TrackOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = TextPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else TextSecondary.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) tint else TextSecondary.copy(alpha = 0.35f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    playlists: List<Playlist>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onPick: (playlistId: String, playlistName: String, onDone: (Boolean) -> Unit) -> Unit,
    onCreateNew: () -> Unit,
    newName: String,
    onNewNameChange: (String) -> Unit,
    creating: Boolean,
    existingPlaylistIds: Set<String> = emptySet(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var addingPlaylistId by remember { mutableStateOf<String?>(null) }
    var addedPlaylistId by remember { mutableStateOf<String?>(null) }
    var confirmationMessage by remember { mutableStateOf<String?>(null) }
    var showCreateInline by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Filter out virtual library and master library so user only sees real customizable playlists
    val availablePlaylists = remember(playlists) {
        playlists.filter {
            it.id != "virtual-library" &&
            !it.name.trim().equals("My own", ignoreCase = true)
        }
    }

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
                .padding(bottom = 16.dp),
        ) {
            // Track Header info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverImage(
                    coverArtId = track.coverArtId,
                    size = 128,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add to playlist",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreen,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist.orEmpty().ifBlank { track.album.orEmpty().ifBlank { "Unknown Artist" } },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (loading && availablePlaylists.isEmpty()) {
                LoadingBox()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                ) {
                    // Item 0: New Playlist Button / Inline Creator
                    item {
                        if (!showCreateInline) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCreateInline = true }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceCard)
                                        .border(BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "New playlist",
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "New playlist",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = "Create a new playlist",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = SurfaceCard,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Give your playlist a name",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = onNewNameChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        placeholder = { Text("My Playlist", color = TextSecondary) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = SpotifyGreen,
                                            unfocusedBorderColor = ThemeOutline.copy(alpha = 0.4f),
                                            cursorColor = SpotifyGreen,
                                        ),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextButton(onClick = { showCreateInline = false }) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = onCreateNew,
                                            enabled = newName.isNotBlank() && !creating,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SpotifyGreen,
                                                contentColor = Color.Black,
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                        ) {
                                            if (creating) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                            } else {
                                                Text("Create & Add", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (availablePlaylists.isEmpty() && !showCreateInline) {
                        item {
                            Text(
                                text = "No custom playlists yet. Tap \"New playlist\" above to create one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    } else {
                        itemsIndexed(availablePlaylists, key = { _, p -> p.id }) { _, playlist ->
                            val isAdding = addingPlaylistId == playlist.id
                            val isAdded = addedPlaylistId == playlist.id
                            val isAlreadyIn = existingPlaylistIds.contains(playlist.id)

                            val coverUrl = remember(playlist.id, playlist.coverArtId) {
                                com.naviify.app.core.image.CoverUrls.playlistUrl(
                                    playlist.id,
                                    playlist.coverArtId,
                                    256,
                                )
                                    ?: com.naviify.app.core.image.CoverUrls.url(playlist.coverArtId, 256)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = addingPlaylistId == null && addedPlaylistId == null) {
                                        if (isAlreadyIn) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "\"${track.title}\" is already in \"${playlist.name}\"",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        } else {
                                            addingPlaylistId = playlist.id
                                            onPick(playlist.id, playlist.name) { success ->
                                                addingPlaylistId = null
                                                if (success) {
                                                    addedPlaylistId = playlist.id
                                                    confirmationMessage = "Added to \"${playlist.name}\""
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    coroutineScope.launch {
                                                        delay(650)
                                                        onDismiss()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlaylistCoverArt(
                                    playlistId = playlist.id,
                                    playlistName = playlist.name,
                                    coverUrl = coverUrl,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    iconSize = 22.dp,
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isAlreadyIn) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(SpotifyGreen)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Already in playlist · ${playlist.songCount} songs",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SpotifyGreen,
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${playlist.songCount} songs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        isAdded -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(SpotifyGreen),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Added",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        isAdding -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = SpotifyGreen,
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                        isAlreadyIn -> {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Already in playlist",
                                                tint = SpotifyGreen,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Rounded.AddCircleOutline,
                                                contentDescription = "Add",
                                                tint = TextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // In-sheet confirmation banner with checkmark
            AnimatedVisibility(
                visible = confirmationMessage != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.5f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = confirmationMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}
