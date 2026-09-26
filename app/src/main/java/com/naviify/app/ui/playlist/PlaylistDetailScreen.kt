package com.naviify.app.ui.playlist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import java.util.Collections
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.WaterDrop
import com.naviify.app.domain.model.PlaylistMixMode
import com.naviify.app.domain.model.getDjBpm
import com.naviify.app.domain.model.getCamelotKey
import com.naviify.app.ui.components.formatDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.components.AddToPlaylistSheet
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.LoadingBox
import com.naviify.app.ui.components.PlaylistCoverArt
import com.naviify.app.ui.components.TrackRow
import com.naviify.app.ui.components.getPlaylistTheme
import com.naviify.app.ui.download.DownloadViewModel
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.player.PlayerViewModel
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.NaviifyBlack
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

private data class PlaylistEditEntry(
    val key: String,
    val track: Track,
)

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadStatus by downloadViewModel.status.collectAsStateWithLifecycle()
    val playlist = state.playlist
    val isVirtualLibrary = playlist?.id == "virtual-library"
    val isMasterLibrary = playlist?.name?.trim().equals(com.naviify.app.data.repository.MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)
    val isSpecialCollection = isVirtualLibrary || isMasterLibrary
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 56.dp.toPx() } }

    var showSortSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }
    var showCoverOptionsSheet by remember { mutableStateOf(false) }
    var showAddSongsSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Pair<Int, Track>?>(null) }
    var trackForAddToPlaylist by remember { mutableStateOf<Track?>(null) }

    // Stable keys for playlist tracks during Spotify-style reordering and removal
    val rawEditEntries = remember(state.sortedTracks) {
        val counts = mutableMapOf<String, Int>()
        state.sortedTracks.map { track ->
            val c = counts.getOrDefault(track.id, 0)
            counts[track.id] = c + 1
            PlaylistEditEntry(key = "pe_${track.id}_$c", track = track)
        }
    }

    val localEditTracks = remember { mutableStateListOf<PlaylistEditEntry>() }

    var isDragging by remember { mutableStateOf(false) }
    var draggedItemKey by remember { mutableStateOf<String?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(rawEditEntries, state.isReorderMode) {
        if (!isDragging) {
            localEditTracks.clear()
            localEditTracks.addAll(rawEditEntries)
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.loadSilently()
        onPauseOrDispose { }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setCustomCover(uri, context.contentResolver)
            }
        },
    )

    when {
        state.isLoading && playlist == null -> LoadingBox()
        state.error != null && playlist == null -> ErrorBanner(state.error.orEmpty(), onRetry = viewModel::load)
        playlist != null -> {
            val theme = remember(playlist.id, playlist.name) { getPlaylistTheme(playlist.id, playlist.name) }
            val totalTracks = playlist.tracks.size
            val doneTracks = playlist.tracks.count {
                downloadStatus.byTrackId[it.id] == TrackDownloadState.DONE
            }
            val allDownloaded = totalTracks > 0 && doneTracks == totalTracks
            val isDownloading = playlist.tracks.any {
                downloadStatus.byTrackId[it.id] == TrackDownloadState.DOWNLOADING
            }
            val downloadProgress = if (totalTracks > 0) (doneTracks.toFloat() / totalTracks.toFloat()).coerceIn(0f, 1f) else 0f

            // High-resolution 1024px cover resolution
            val coverResolvedId = state.resolvedCoverArtId ?: playlist.coverArtId
            val coverUrl = remember(playlist.id, coverResolvedId, state.coverUpdateTrigger, state.hasCustomCover) {
                if (state.hasCustomCover) {
                    CoverUrls.localUrl("playlist-${playlist.id}")
                        ?: CoverUrls.remoteUrl(coverResolvedId, 1024)
                } else {
                    CoverUrls.remoteUrl(coverResolvedId, 1024)
                        ?: if (!isVirtualLibrary) CoverUrls.remoteUrl("pl-${playlist.id}", 1024) else null
                        ?: playlist.tracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId?.let { CoverUrls.remoteUrl(it, 1024) }
                }
            }

            var dominantColor by remember(coverUrl) { mutableStateOf<Color?>(null) }
            val fallbackAccent = theme.gradientColors.firstOrNull() ?: LocalNaviifyPalette.current.accent
            val effectiveAccent = dominantColor ?: fallbackAccent

            val totalDurationSeconds = remember(playlist.duration, playlist.tracks) {
                if (playlist.duration > 0) playlist.duration else playlist.tracks.sumOf { it.duration }
            }
            val formattedDuration = remember(totalDurationSeconds) {
                formatTotalDuration(totalDurationSeconds)
            }
            val songCount = remember(playlist.songCount, playlist.tracks.size) {
                playlist.tracks.size.takeIf { it > 0 } ?: playlist.songCount
            }

            val playerUiState by playerViewModel.state.collectAsStateWithLifecycle()
            val listState = rememberLazyListState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Rich atmospheric vertical gradient matching the album cover's vibe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to effectiveAccent.copy(alpha = 0.65f),
                                    0.30f to effectiveAccent.copy(alpha = 0.40f),
                                    0.58f to effectiveAccent.copy(alpha = 0.16f),
                                    0.84f to Color.Transparent,
                                    1.00f to Color.Transparent,
                                ),
                            ),
                        ),
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // Top App Bar
                    item {
                        if (state.isReorderMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = viewModel::toggleReorderMode) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Close edit mode",
                                        tint = TextPrimary,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Edit playlist",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = "Drag ≡ to reorder, tap - to remove",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                Button(
                                    onClick = viewModel::toggleReorderMode,
                                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                ) {
                                    Text("Done", color = NaviifyBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = TextPrimary,
                                    )
                                }
                            }
                        }
                    }

                    // Main Playlist Cover Art (Crisp, rounded corners, subtle shadow)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(240.dp)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        spotColor = Color.Black.copy(alpha = 0.5f),
                                        ambientColor = Color.Black.copy(alpha = 0.35f),
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceCardHigh,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(enabled = !isSpecialCollection) {
                                            showCoverOptionsSheet = true
                                        },
                                ) {
                                    if (!coverUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(coverUrl)
                                                .crossfade(true)
                                                .allowHardware(false)
                                                .listener(
                                                    onSuccess = { _, result ->
                                                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                                                        if (bitmap != null) {
                                                            dominantColor = extractDominantColor(bitmap)
                                                        }
                                                    },
                                                )
                                                .build(),
                                            contentDescription = playlist.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        PlaylistCoverArt(
                                            playlistId = playlist.id,
                                            playlistName = playlist.name,
                                            coverUrl = null,
                                            iconSize = 72.dp,
                                            showLetter = true,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }

                                    if (!isSpecialCollection) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(10.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PhotoCamera,
                                                    contentDescription = "Change cover",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Text(
                                                    text = "Edit",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Playlist Title, Description, and Single Clean Metadata Row
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            if (state.isReorderMode) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.showEditSheet() }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "Tap to edit name & description",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SpotifyGreen,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.EditNote,
                                        contentDescription = "Edit details",
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            } else {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                val isAutoImportComment = playlist.comment?.let { comment ->
                                    comment.contains("imported", ignoreCase = true) ||
                                        comment.contains(".m3u", ignoreCase = true) ||
                                        comment.contains("subsonic", ignoreCase = true) ||
                                        comment.contains("navidrome", ignoreCase = true)
                                } ?: false

                                if (!playlist.comment.isNullOrBlank() && !isAutoImportComment) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = playlist.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Spotify Creator & Metadata Row: Avatar, Username, Duration, Public/Private
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // User avatar
                                    Surface(
                                        modifier = Modifier.size(24.dp),
                                        shape = CircleShape,
                                        color = SpotifyGreen.copy(alpha = 0.25f),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = state.username.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SpotifyGreen,
                                            )
                                        }
                                    }

                                    // Username • Duration
                                    Text(
                                        text = buildString {
                                            append(state.username)
                                            if (formattedDuration.isNotBlank()) {
                                                append(" • ")
                                                append(formattedDuration)
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = TextSecondary,
                                    )

                                    if (!isSpecialCollection) {
                                        Icon(
                                            imageVector = if (playlist.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                                            contentDescription = if (playlist.isPublic) "Public" else "Private",
                                            tint = TextSecondary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Spotify Action Row: Stack Deck, Download (with % & Cancel), Share, More on Left; Shuffle & Play on Right
                    if (!state.isReorderMode) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                // Left Icons: Mini Artwork Deck, Download, Share, More
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    // Download Button (with percentage spinner & cancel dialog)
                                    if (isDownloading) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .clickable { showCancelDownloadDialog = true },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { downloadProgress },
                                                modifier = Modifier.size(26.dp),
                                                strokeWidth = 2.5.dp,
                                                color = SpotifyGreen,
                                                trackColor = Color.White.copy(alpha = 0.2f),
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = "Downloading ${(downloadProgress * 100).toInt()}% - tap to cancel",
                                                tint = SpotifyGreen,
                                                modifier = Modifier.size(13.dp),
                                            )
                                        }
                                    } else if (allDownloaded) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(SpotifyGreen)
                                                .clickable {
                                                    Toast.makeText(context, "All songs downloaded", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = "Playlist downloaded",
                                                tint = NaviifyBlack,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                val missingCount = playlist.tracks.count {
                                                    downloadStatus.byTrackId[it.id] != TrackDownloadState.DONE
                                                }
                                                if (missingCount > 0) {
                                                    Toast.makeText(context, "Downloading $missingCount songs...", Toast.LENGTH_SHORT).show()
                                                }
                                                downloadViewModel.downloadPlaylist(playlist)
                                            },
                                            modifier = Modifier.size(38.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = "Download playlist",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }

                                    // Share button
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Listen to \"${playlist.name}\" on Naviify")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share playlist"))
                                        },
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Share playlist",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }

                                    // More options 3-dots
                                    Box {
                                        IconButton(
                                            onClick = { showMoreMenu = true },
                                            modifier = Modifier.size(38.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "More options",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showMoreMenu,
                                            onDismissRequest = { showMoreMenu = false },
                                            containerColor = SurfaceCardHigh,
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Find in playlist", color = TextPrimary) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Search,
                                                        contentDescription = null,
                                                        tint = SpotifyGreen,
                                                    )
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    viewModel.setSearchVisible(true)
                                                },
                                            )
                                            if (!isSpecialCollection) {
                                                DropdownMenuItem(
                                                    text = { Text("Edit details", color = TextPrimary) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.EditNote,
                                                            contentDescription = null,
                                                            tint = SpotifyGreen,
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        viewModel.showEditSheet()
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            if (playlist.isPublic) "Make private" else "Make public",
                                                            color = TextPrimary,
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = if (playlist.isPublic) Icons.Rounded.Lock else Icons.Rounded.Public,
                                                            contentDescription = null,
                                                            tint = SpotifyGreen,
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        viewModel.togglePublicPrivate()
                                                        Toast.makeText(
                                                            context,
                                                            if (playlist.isPublic) "Playlist is now private" else "Playlist is now public",
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                    },
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Change cover", color = TextPrimary) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.PhotoCamera,
                                                            contentDescription = null,
                                                            tint = SpotifyGreen,
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        showCoverOptionsSheet = true
                                                    },
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Sync with Navidrome", color = TextPrimary) },
                                                leadingIcon = {
                                                    if (state.isRefreshing) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            strokeWidth = 2.dp,
                                                            color = SpotifyGreen,
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Sync,
                                                            contentDescription = null,
                                                            tint = SpotifyGreen,
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    viewModel.pullRefresh()
                                                },
                                            )
                                            if (!isSpecialCollection) {
                                                DropdownMenuItem(
                                                    text = { Text("Delete playlist", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.DeleteOutline,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        showDeleteConfirmDialog = true
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right Action Buttons: Shuffle & Play
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    val isCurrentPlaylistPlaying = playerUiState.isPlaying && playerUiState.activePlaylistId == playlist.id
                                    val isCurrentPlaylistActive = playerUiState.activePlaylistId == playlist.id
                                    val isCurrentPlaylistShuffled = isCurrentPlaylistActive && playerUiState.isShuffleEnabled

                                    IconButton(
                                        onClick = {
                                            if (isCurrentPlaylistActive) {
                                                playerViewModel.setShuffle(!playerUiState.isShuffleEnabled)
                                            } else {
                                                viewModel.playShuffled()
                                            }
                                        },
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Shuffle,
                                                contentDescription = if (isCurrentPlaylistShuffled) "Shuffle On" else "Shuffle Off",
                                                tint = if (isCurrentPlaylistShuffled) SpotifyGreen else TextSecondary,
                                                modifier = Modifier.size(26.dp),
                                            )
                                            if (isCurrentPlaylistShuffled) {
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
                                    Surface(
                                        onClick = {
                                            if (isCurrentPlaylistPlaying) {
                                                playerViewModel.togglePlayPause()
                                            } else {
                                                viewModel.playFrom(0)
                                            }
                                        },
                                        shape = CircleShape,
                                        color = SpotifyGreen,
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(56.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isCurrentPlaylistPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isCurrentPlaylistPlaying) "Pause" else "Play",
                                                tint = NaviifyBlack,
                                                modifier = Modifier.size(34.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Spotify Pill Bar: [+ Add]  [🎚 Mix]  [≡ Edit]  [⇅ Sort]  [✏ Name]
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // [+ Add]
                                if (!isSpecialCollection) {
                                    Surface(
                                        onClick = { showAddSongsSheet = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                tint = TextPrimary,
                                                modifier = Modifier.size(17.dp),
                                            )
                                            Text(
                                                text = "Add",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = TextPrimary,
                                            )
                                        }
                                    }
                                }

                                // [🎚 Mix]
                                if (!isSpecialCollection) {
                                    val isMixOn = state.mixConfig.isEnabled
                                    Surface(
                                        onClick = {
                                            if (!isMixOn) {
                                                viewModel.toggleMixEnabled()
                                            }
                                            viewModel.showMixSheet()
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isMixOn) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(
                                            if (isMixOn) 1.5.dp else 1.dp,
                                            if (isMixOn) Color.White else Color.White.copy(alpha = 0.12f),
                                        ),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = null,
                                                tint = if (isMixOn) Color.White else TextPrimary,
                                                modifier = Modifier.size(17.dp),
                                            )
                                            Text(
                                                text = "Mix",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = if (isMixOn) Color.White else TextPrimary,
                                            )
                                        }
                                    }
                                }

                                // [≡ Edit]
                                if (!isSpecialCollection) {
                                    Surface(
                                        onClick = viewModel::toggleReorderMode,
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (state.isReorderMode) SpotifyGreen.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, if (state.isReorderMode) SpotifyGreen else Color.White.copy(alpha = 0.12f)),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DragHandle,
                                                contentDescription = null,
                                                tint = if (state.isReorderMode) SpotifyGreen else TextPrimary,
                                                modifier = Modifier.size(17.dp),
                                            )
                                            Text(
                                                text = "Edit",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = if (state.isReorderMode) SpotifyGreen else TextPrimary,
                                            )
                                        }
                                    }
                                }

                                // [⇅ Sort]
                                val isCustomSort = state.sortOrder == PlaylistSortOrder.CUSTOM_ORDER
                                Surface(
                                    onClick = { showSortSheet = true },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (!isCustomSort) SpotifyGreen.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, if (!isCustomSort) SpotifyGreen else Color.White.copy(alpha = 0.12f)),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapVert,
                                            contentDescription = "Sort order",
                                            tint = if (!isCustomSort) SpotifyGreen else TextPrimary,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Text(
                                            text = if (!isCustomSort) state.sortOrder.label else "Sort",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                            ),
                                            color = if (!isCustomSort) SpotifyGreen else TextPrimary,
                                        )
                                    }
                                }

                                // [✏ Name]
                                if (!isSpecialCollection) {
                                    Surface(
                                        onClick = viewModel::showEditSheet,
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = null,
                                                tint = TextPrimary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = "Name",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = TextPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.sortedTracks.isEmpty() && state.searchQuery.isNotBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No songs matching \"${state.searchQuery}\"",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else if (state.isReorderMode) {
                        // Spotify-style Edit Track Rows with Minus button on left and Drag Handle on right
                        itemsIndexed(
                            items = localEditTracks,
                            key = { _, entry -> entry.key },
                        ) { index, entry ->
                            val isBeingDragged = entry.key == draggedItemKey
                            SpotifyEditTrackRow(
                                entry = entry,
                                isBeingDragged = isBeingDragged,
                                dragOffsetY = dragOffsetY,
                                onRemove = {
                                    val idx = localEditTracks.indexOfFirst { it.key == entry.key }
                                    if (idx >= 0) {
                                        localEditTracks.removeAt(idx)
                                        viewModel.removeTrack(idx)
                                        Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                dragHandleModifier = Modifier.pointerInput(entry.key) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                            draggedItemKey = entry.key
                                            initialDragIndex = localEditTracks.indexOfFirst { it.key == entry.key }
                                            dragOffsetY = 0f
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val current = localEditTracks.indexOfFirst { it.key == entry.key }
                                            if (current < 0) return@detectDragGestures
                                            val threshold = itemHeightPx * 0.5f

                                            if (dragOffsetY > threshold && current < localEditTracks.lastIndex) {
                                                Collections.swap(localEditTracks, current, current + 1)
                                                dragOffsetY -= itemHeightPx
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } else if (dragOffsetY < -threshold && current > 0) {
                                                Collections.swap(localEditTracks, current, current - 1)
                                                dragOffsetY += itemHeightPx
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        onDragEnd = {
                                            val start = initialDragIndex
                                            val finalIdx = localEditTracks.indexOfFirst { it.key == entry.key }
                                            if (start != null && finalIdx >= 0 && start != finalIdx) {
                                                viewModel.moveTrack(start, finalIdx)
                                            }
                                            isDragging = false
                                            draggedItemKey = null
                                            initialDragIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            localEditTracks.clear()
                                            localEditTracks.addAll(rawEditEntries)
                                            isDragging = false
                                            draggedItemKey = null
                                            initialDragIndex = null
                                            dragOffsetY = 0f
                                        },
                                    )
                                },
                            )
                        }
                    } else if (state.mixConfig.isEnabled) {
                        // Spotify DJ Mix View matching real Spotify UI (Image 5)
                        itemsIndexed(
                            items = state.sortedTracks,
                            key = { index, track -> "${track.id}_$index" },
                        ) { index, track ->
                            val stateForTrack = downloadStatus.byTrackId[track.id]
                            val isPlayingThis = playerUiState.isPlaying && playerUiState.currentTrack?.id == track.id

                            SpotifyMixTrackRow(
                                track = track,
                                isPlaying = isPlayingThis,
                                downloadState = stateForTrack,
                                onOptionsClick = { selectedTrackForOptions = Pair(index, track) },
                                onClick = { viewModel.playFrom(index) },
                            )

                            if (index < state.sortedTracks.size - 1) {
                                val nextTrack = state.sortedTracks[index + 1]
                                val bridgeMode = state.mixConfig.transitionModeFor(track.id, nextTrack.id)
                                val isCustom = state.mixConfig.hasCustomOverride(track.id, nextTrack.id)
                                SpotifyMixTransitionBridge(
                                    mode = bridgeMode,
                                    isCustomized = isCustom,
                                    onClick = {
                                        viewModel.openTransitionBridgeSheet(fromTrack = track, toTrack = nextTrack)
                                    },
                                )
                            }
                        }
                    } else {
                        // Track Rows
                        itemsIndexed(state.sortedTracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                            val stateForTrack = downloadStatus.byTrackId[track.id]
                            TrackRow(
                                index = index,
                                track = track,
                                downloadState = stateForTrack,
                                onDownloadToggle = if (stateForTrack == TrackDownloadState.DOWNLOADING) {
                                    null
                                } else {
                                    {
                                        if (stateForTrack == TrackDownloadState.DONE) {
                                            downloadViewModel.deleteDownload(track.id)
                                        } else {
                                            downloadViewModel.downloadTrack(track)
                                        }
                                    }
                                },
                                onOptionsClick = { selectedTrackForOptions = Pair(index, track) },
                                onClick = { viewModel.playFrom(index) },
                            )
                        }
                    }
                }


            }
        }
    }

    if (state.isRenameDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRenameDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Rename Playlist", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.renameText,
                        onValueChange = viewModel::onRenameTextChange,
                        singleLine = true,
                        placeholder = { Text("Playlist name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = SpotifyGreen,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = TextSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmRename,
                    enabled = state.renameText.isNotBlank() && !state.isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                ) {
                    Text(if (state.isUpdating) "Saving..." else "Save", color = NaviifyBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRenameDialog) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    if (showDeleteConfirmDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = SurfaceCardHigh,
            title = { Text("Delete playlist?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${playlist.name}\"? This action cannot be undone.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePlaylist { ok ->
                            if (ok) {
                                Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Could not delete playlist", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    if (showSortSheet) {
        SortOptionsBottomSheet(
            selectedOrder = state.sortOrder,
            onSelectOrder = viewModel::setSortOrder,
            onDismiss = { showSortSheet = false },
        )
    }

    if (showCoverOptionsSheet && playlist != null) {
        PlaylistCoverOptionsSheet(
            playlistName = playlist.name,
            hasCustomCover = state.hasCustomCover,
            onPickFromGallery = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onResetToServerCover = viewModel::resetToServerCover,
            onDismiss = { showCoverOptionsSheet = false },
        )
    }

    selectedTrackForOptions?.let { (index, track) ->
        val trackDownloadState = downloadStatus.byTrackId[track.id]
        PlaylistTrackOptionsSheet(
            track = track,
            downloadState = trackDownloadState,
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist.orEmpty()}")
                }
                context.startActivity(Intent.createChooser(intent, "Share song"))
            },
            onAddToPlaylist = {
                trackForAddToPlaylist = track
            },
            onAddToQueue = {
                playerViewModel.appendToQueue(track)
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            },
            onGoToAlbum = track.albumId?.takeIf { it.isNotBlank() }?.let { albumId ->
                { onOpenAlbum(albumId) }
            },
            onGoToArtist = track.artistId?.takeIf { it.isNotBlank() }?.let { artistId ->
                { onOpenArtist(artistId) }
            },
            onDownloadToggle = {
                if (trackDownloadState == TrackDownloadState.DONE) {
                    downloadViewModel.deleteDownload(track.id)
                } else {
                    downloadViewModel.downloadTrack(track)
                }
            },
            onMoveToTop = if (!isSpecialCollection && index > 0) {
                {
                    viewModel.moveTrackToTop(index)
                    selectedTrackForOptions = null
                }
            } else null,
            onMoveUp = if (!isSpecialCollection && index > 0) {
                {
                    viewModel.moveTrackUp(index)
                    selectedTrackForOptions = null
                }
            } else null,
            onMoveDown = if (!isSpecialCollection && index < state.sortedTracks.size - 1) {
                {
                    viewModel.moveTrackDown(index)
                    selectedTrackForOptions = null
                }
            } else null,
            onRemoveFromPlaylist = if (!isSpecialCollection) {
                {
                    viewModel.removeTrack(index)
                    selectedTrackForOptions = null
                }
            } else null,
            onDismiss = { selectedTrackForOptions = null },
        )
    }

    if (state.isEditSheetVisible && playlist != null) {
        val coverUrl = remember(playlist.id, playlist.coverArtId, state.coverUpdateTrigger) {
            CoverUrls.playlistUrl(playlist.id, playlist.coverArtId, 512)
        }
        SpotifyEditPlaylistSheet(
            playlistName = state.editNameText,
            playlistComment = state.editDescriptionText,
            isPublic = state.editIsPublic,
            coverUrl = coverUrl,
            isUpdating = state.isUpdating,
            onChangeCover = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onSave = { name, description, isPub ->
                viewModel.onEditNameChange(name)
                viewModel.onEditDescriptionChange(description)
                viewModel.onEditIsPublicChange(isPub)
                viewModel.savePlaylistDetails()
            },
            onDismiss = viewModel::dismissEditSheet,
        )
    }

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

    if (showAddSongsSheet && playlist != null) {
        AddSongsToPlaylistSheet(
            existingTrackIds = playlist.tracks.map { it.id }.toSet(),
            onFetchLibraryTracks = viewModel::getAvailableTracks,
            onAddTrack = viewModel::addTrack,
            onDismiss = { showAddSongsSheet = false },
        )
    }

    if (showCancelDownloadDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showCancelDownloadDialog = false },
            containerColor = SurfaceCardHigh,
            title = { Text("Cancel download?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Do you want to stop downloading songs for \"${playlist.name}\"?",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDownloadDialog = false
                        downloadViewModel.cancelPlaylistDownload(playlist)
                        Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Cancel download", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDownloadDialog = false }) {
                    Text("Keep downloading", color = TextSecondary)
                }
            },
        )
    }

    if (state.isMixSheetVisible && playlist != null) {
        PlaylistMixStudioSheet(
            playlistName = playlist.name,
            config = state.mixConfig,
            onToggleEnabled = viewModel::toggleMixEnabled,
            onSetMode = viewModel::setMixMode,
            onSetDuration = viewModel::setMixDuration,
            onSetSmartBassSwap = viewModel::setSmartBassSwap,
            onSetEqualPowerVolume = viewModel::setEqualPowerVolume,
            onReorderHarmonicFlow = {
                viewModel.reorderPlaylistByHarmonicFlow {
                    Toast.makeText(context, "Playlist reordered by harmonic flow", Toast.LENGTH_SHORT).show()
                }
            },
            onResetAllOverrides = {
                viewModel.resetAllBridgeTransitions()
                Toast.makeText(context, "All transitions reset to default", Toast.LENGTH_SHORT).show()
            },
            onDismiss = viewModel::dismissMixSheet,
        )
    }

    val selectedBridge = state.selectedTransitionBridge
    if (selectedBridge != null) {
        val fromTrack = selectedBridge.first
        val toTrack = selectedBridge.second
        val bridgeMode = state.mixConfig.transitionModeFor(fromTrack.id, toTrack.id)
        val isCustom = state.mixConfig.hasCustomOverride(fromTrack.id, toTrack.id)

        PlaylistTransitionBridgeSheet(
            fromTrack = fromTrack,
            toTrack = toTrack,
            currentMode = bridgeMode,
            defaultMode = state.mixConfig.mode,
            isCustomOverride = isCustom,
            onSelectMode = { mode ->
                viewModel.setBridgeTransitionMode(fromTrack.id, toTrack.id, mode)
            },
            onApplyToAll = { mode ->
                viewModel.applyBridgeModeToAll(mode)
                Toast.makeText(context, "Applied ${mode.title} to all transitions", Toast.LENGTH_SHORT).show()
            },
            onResetToDefault = {
                viewModel.resetBridgeTransitionMode(fromTrack.id, toTrack.id)
                Toast.makeText(context, "Reset to default (${state.mixConfig.mode.title})", Toast.LENGTH_SHORT).show()
            },
            equalPowerVolume = state.mixConfig.equalPowerVolume,
            durationSeconds = state.mixConfig.durationSeconds,
            onDismiss = viewModel::dismissTransitionBridgeSheet,
        )
    }
}

private fun extractDominantColor(bitmap: Bitmap): Color {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return Color(0xFFE50914)

    // Downscale for fast, zero-jank sampling without allocating large pixel buffers
    val scaled = if (width > 24 || height > 24) {
        Bitmap.createScaledBitmap(bitmap, 24, 24, false)
    } else {
        bitmap
    }

    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    var count = 0L

    val sWidth = scaled.width
    val sHeight = scaled.height

    for (x in 0 until sWidth) {
        for (y in 0 until sHeight) {
            val pixel = scaled.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3
            if (brightness in 35..220) {
                totalR += r
                totalG += g
                totalB += b
                count++
            }
        }
    }

    if (scaled != bitmap && !scaled.isRecycled) {
        scaled.recycle()
    }

    return if (count > 0) {
        Color(
            red = (totalR / count).toInt(),
            green = (totalG / count).toInt(),
            blue = (totalB / count).toInt(),
        )
    } else {
        val center = bitmap.getPixel(width / 2, height / 2)
        Color((center shr 16) and 0xFF, (center shr 8) and 0xFF, center and 0xFF)
    }
}

private fun formatTotalDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "0m"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${maxOf(1, minutes)}m"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOptionsBottomSheet(
    selectedOrder: PlaylistSortOrder,
    onSelectOrder: (PlaylistSortOrder) -> Unit,
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
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            PlaylistSortOrder.entries.forEach { order ->
                val isSelected = order == selectedOrder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectOrder(order)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = order.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) SpotifyGreen else TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpotifyEditTrackRow(
    entry: PlaylistEditEntry,
    isBeingDragged: Boolean,
    dragOffsetY: Float,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
) {
    val track = entry.track
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isBeingDragged) 10f else 1f)
            .graphicsLayer {
                if (isBeingDragged) {
                    translationY = dragOffsetY
                    scaleX = 1.02f
                    scaleY = 1.02f
                    shadowElevation = 8.dp.toPx()
                }
            }
            .background(
                color = if (isBeingDragged) SurfaceCardHigh.copy(alpha = 0.95f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Minus Button (-) on Left (Spotify style delete circle)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFE22134))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Remove from playlist",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }

        // Track Cover Art
        CoverImage(
            coverArtId = track.coverArtId,
            size = 128,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        // Track Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isBeingDragged) SpotifyGreen else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Drag Handle (≡) on Right (Spotify style drag handle)
        Box(
            modifier = dragHandleModifier
                .size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (isBeingDragged) SpotifyGreen else TextSecondary.copy(alpha = 0.75f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun SpotifyMixTrackRow(
    track: Track,
    isPlaying: Boolean,
    downloadState: TrackDownloadState?,
    onOptionsClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bpm = getDjBpm(track.id, track.title)
    val camelot = getCamelotKey(track.id, track.artist)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 48dp Album artwork
        CoverImage(
            coverArtId = track.coverArtId,
            size = 128,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(Modifier.width(12.dp))

        // Track title & metadata
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Playing",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isPlaying) SpotifyGreen else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (downloadState == TrackDownloadState.DONE) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = "Downloaded",
                            tint = NaviifyBlack,
                            modifier = Modifier.size(9.dp),
                        )
                    }
                }

                if (track.discNumber != null && track.discNumber > 1 || track.title.contains("explicit", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = Color.White.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = "E",
                            color = TextPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        )
                    }
                }

                Text(
                    text = track.artist.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right side: BPM above, Camelot key badge + Duration below
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "$bpm bpm",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = TextSecondary,
            )

            Spacer(Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Camelot Key Badge (e.g. 4A, 10A, 1A, 8A)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(camelot.colorHex),
                ) {
                    Text(
                        text = camelot.code,
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }

                Text(
                    text = formatDuration(track.duration ?: 0),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = TextSecondary,
                )
            }
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SpotifyMixTransitionBridge(
    mode: PlaylistMixMode,
    isCustomized: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
    ) {
        // Vertical connector line passing through the album art column (center = 16dp + 24dp = 40dp)
        Box(
            modifier = Modifier
                .padding(start = 39.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.15f)),
        )

        // Pill button sitting on the line
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = if (isCustomized) Color(0xFF1E2620) else Color(0xFF222222),
            border = BorderStroke(
                width = if (isCustomized) 1.5.dp else 1.dp,
                color = if (isCustomized) SpotifyGreen.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.10f),
            ),
            modifier = Modifier
                .padding(start = 28.dp)
                .align(Alignment.CenterStart),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isCustomized) SpotifyGreen else Color.White.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (mode) {
                            PlaylistMixMode.AUTO -> Icons.Rounded.AutoAwesome
                            PlaylistMixMode.FADE -> Icons.Rounded.GraphicEq
                            PlaylistMixMode.RISE -> Icons.AutoMirrored.Rounded.TrendingUp
                            PlaylistMixMode.MELT -> Icons.Rounded.WaterDrop
                            PlaylistMixMode.SLAM -> Icons.Rounded.Bolt
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(11.dp),
                    )
                }

                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                    color = Color.White,
                )

                if (isCustomized) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Edit mix transition",
                    tint = TextSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

