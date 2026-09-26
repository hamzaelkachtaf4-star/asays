package com.naviify.app.ui.player

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naviify.app.core.image.CoverUrls
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.domain.model.LyricsLineData
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlaybackRepeatMode
import com.naviify.app.domain.playback.PlayerUiState
import com.naviify.app.ui.components.AddToPlaylistSheet
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.MarqueeText
import com.naviify.app.ui.components.EmptyState
import com.naviify.app.ui.components.ErrorBanner
import com.naviify.app.ui.components.formatDuration
import com.naviify.app.ui.download.DownloadViewModel
import com.naviify.app.ui.download.TrackDownloadState
import com.naviify.app.ui.playlist.PlaylistsViewModel
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.theme.ThemeOutline
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val trackDelegate by remember(viewModel) {
        viewModel.state.map { it.currentTrack }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.currentTrack)
    val track = trackDelegate

    val isPlaying by remember(viewModel) {
        viewModel.state.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.isPlaying)

    val isShuffleEnabled by remember(viewModel) {
        viewModel.state.map { it.isShuffleEnabled }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.isShuffleEnabled)

    val repeatMode by remember(viewModel) {
        viewModel.state.map { it.repeatMode }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.repeatMode)

    val error by remember(viewModel) {
        viewModel.state.map { it.error }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.error)

    val isMixBlending by remember(viewModel) {
        viewModel.state.map { it.isMixBlending }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.isMixBlending)

    val mixProgress by remember(viewModel) {
        viewModel.state.map { it.mixProgress }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.mixProgress)

    val mixOutgoingTrack by remember(viewModel) {
        viewModel.state.map { it.mixOutgoingTrack }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.mixOutgoingTrack)

    val positionFlow = remember(viewModel) {
        viewModel.state.map { it.positionMs }.distinctUntilChanged()
    }
    val durationFlow = remember(viewModel) {
        viewModel.state.map { it.durationMs }.distinctUntilChanged()
    }
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val artistState by viewModel.artistState.collectAsStateWithLifecycle()
    val downloadStatus by downloadViewModel.status.collectAsStateWithLifecycle()
    val playlistsState by playlistsViewModel.uiState.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    val isSleepUntilTrackEnd by viewModel.isSleepUntilTrackEnd.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var showAddToPlaylist by rememberSaveable { mutableStateOf(false) }
    var showSleepPicker by rememberSaveable { mutableStateOf(false) }
    var showFullscreenLyrics by rememberSaveable { mutableStateOf(false) }
    var showAdjustLyrics by rememberSaveable { mutableStateOf(false) }
    var showLyricsOptions by rememberSaveable { mutableStateOf(false) }
    var showManualLyricsSearch by rememberSaveable { mutableStateOf(false) }
    var showReportLyrics by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Ambient blurred artwork backdrop (isolated composable so it skips 200ms tick recompositions)
        BlurredBackdrop(coverArtId = track?.coverArtId)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    MarqueeText(
                        text = track?.album.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                track?.let {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = TextPrimary,
                        )
                    }
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = TextPrimary,
                    )
                }
            }

            if (track == null) {
                EmptyState("Nothing playing yet. Pick something from your library.", Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    val targetArtistRef = artistState.artist?.id?.takeIf { it.isNotBlank() }
                        ?: track.artistId?.takeIf { it.isNotBlank() }
                        ?: track.artist?.takeIf { it.isNotBlank() }

                    val currentTrackState = downloadStatus.byTrackId[track.id]
                    ArtPanel(
                        track = track,
                        downloadState = currentTrackState,
                        isMixBlending = isMixBlending,
                        mixProgress = mixProgress,
                        mixOutgoingTrack = mixOutgoingTrack,
                        onOpenArtist = {
                            targetArtistRef?.let(onOpenArtist)
                        },
                        onDownloadClick = {
                            when (currentTrackState) {
                                TrackDownloadState.DONE -> downloadViewModel.deleteDownload(track.id)
                                null -> downloadViewModel.downloadTrack(track)
                                else -> Unit
                            }
                        },
                        onAddToPlaylistClick = {
                            showAddToPlaylist = true
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    SeekSection(
                        positionFlow = positionFlow,
                        durationFlow = durationFlow,
                        onSeekTo = viewModel::seekTo,
                    )
                    ControlsRow(
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onPrevious = viewModel::previous,
                        onNext = viewModel::next,
                        onSetShuffle = viewModel::setShuffle,
                        onSetRepeat = viewModel::setRepeat,
                    )
                    error?.let { message ->
                        ErrorBanner(
                            message = message,
                            onRetry = viewModel::retryCurrent,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    LyricsCard(
                        positionFlow = positionFlow,
                        isLoading = lyricsState.isLoading,
                        lyrics = lyricsState.lyrics?.syncedLines.orEmpty(),
                        offsetMs = lyricsState.lyrics?.offsetMs ?: 0L,
                        onSeekTo = { offsetMs -> viewModel.seekTo(offsetMs) },
                        onExpand = { showFullscreenLyrics = true },
                        onShare = {
                            shareLyrics(
                                context = context,
                                track = track,
                                lyrics = lyricsState.lyrics?.syncedLines.orEmpty(),
                                plainLyrics = lyricsState.lyrics?.plainText,
                            )
                        },
                        onAdjustTiming = { showAdjustLyrics = true },
                        onOpenOptions = { showLyricsOptions = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    AboutArtistCard(
                        artistState = artistState,
                        track = track,
                        onOpenArtist = onOpenArtist,
                        onToggleFavorite = viewModel::toggleFavoriteArtist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showFullscreenLyrics) {
            LyricsFullScreenView(
                isPlaying = isPlaying,
                positionFlow = positionFlow,
                durationFlow = durationFlow,
                track = track,
                lyrics = lyricsState.lyrics?.syncedLines.orEmpty(),
                offsetMs = lyricsState.lyrics?.offsetMs ?: 0L,
                onSeekTo = viewModel::seekTo,
                onTogglePlayPause = viewModel::togglePlayPause,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                onShare = {
                    shareLyrics(
                        context = context,
                        track = track,
                        lyrics = lyricsState.lyrics?.syncedLines.orEmpty(),
                        plainLyrics = lyricsState.lyrics?.plainText,
                    )
                },
                onAdjustTiming = { showAdjustLyrics = true },
                onOpenOptions = { showLyricsOptions = true },
                onDismiss = { showFullscreenLyrics = false },
            )
        }
    }

    if (showQueue) {
        val queueState by viewModel.state.collectAsStateWithLifecycle()
        QueueSheet(
            state = queueState,
            onDismiss = { showQueue = false },
            onSelect = viewModel::select,
            onRemove = viewModel::removeFromQueue,
            onMove = viewModel::moveInQueue,
            onToggleShuffle = { viewModel.setShuffle(!isShuffleEnabled) },
            onReshuffle = viewModel::reshuffleQueue,
            onClear = viewModel::clear,
        )
    }
    if (showOptions && track != null) {
        OptionsSheet(
            track = track,
            onDismiss = { showOptions = false },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist.orEmpty()}")
                }
                context.startActivity(Intent.createChooser(intent, "Share song"))
            },
            onAddToPlaylist = {
                showOptions = false
                showAddToPlaylist = true
            },
            onAddToQueue = {
                viewModel.appendToQueue(track)
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            },
            onViewQueue = {
                showOptions = false
                showQueue = true
            },
            onGoToAlbum = {
                track.albumId?.let(onOpenAlbum)
            },
            onGoToArtist = {
                showOptions = false
                val targetRef = artistState.artist?.id?.takeIf { it.isNotBlank() }
                    ?: track.artistId?.takeIf { it.isNotBlank() }
                    ?: track.artist?.takeIf { it.isNotBlank() }
                if (!targetRef.isNullOrBlank()) {
                    onOpenArtist(targetRef)
                }
            },
            onSleepTimer = {
                showOptions = false
                showSleepPicker = true
            },
            onStartSimilarRadio = {
                showOptions = false
                viewModel.startSimilarRadio(track)
                Toast.makeText(context, "Playing similar music (style, artist, album)", Toast.LENGTH_SHORT).show()
            },
            onAdjustLyrics = {
                showOptions = false
                showLyricsOptions = true
            },
            hasSyncedLyrics = true,
            albumEnabled = !track.albumId.isNullOrBlank(),
            artistEnabled = !track.artistId.isNullOrBlank() || artistState.artist != null || !track.artist.isNullOrBlank(),
        )
    }
    if (showAddToPlaylist && track != null) {
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
            onDismiss = { showAddToPlaylist = false },
            onPick = { playlistId, playlistName, onDone ->
                scope.launch {
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
                }
            },
            onCreateNew = {
                playlistsViewModel.createPlaylist(initialTrack = track) { ok ->
                    if (ok) {
                        Toast.makeText(context, "Playlist created and track added", Toast.LENGTH_SHORT).show()
                        showAddToPlaylist = false
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
    if (showSleepPicker) {
        SleepTimerSheet(
            remainingSeconds = sleepRemaining,
            isEndOfTrack = isSleepUntilTrackEnd,
            currentTrackDurationSeconds = track?.duration ?: 0,
            currentPositionMs = viewModel.state.value.positionMs,
            onStartMinutes = { minutes ->
                viewModel.startSleepTimer(minutes)
                Toast.makeText(context, "Sleep timer: $minutes min", Toast.LENGTH_SHORT).show()
            },
            onStartEndOfTrack = {
                viewModel.startSleepUntilEndOfTrack()
                Toast.makeText(context, "Sleep timer: end of track", Toast.LENGTH_SHORT).show()
            },
            onCancel = {
                viewModel.cancelSleepTimer()
                Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSleepPicker = false },
        )
    }
    if (showAdjustLyrics && track != null) {
        AdjustLyricsSheet(
            track = track,
            currentOffsetMs = lyricsState.lyrics?.offsetMs ?: 0L,
            positionFlow = positionFlow,
            lyrics = lyricsState.lyrics?.syncedLines.orEmpty(),
            onOffsetChange = { offsetMs -> viewModel.setLyricsOffset(offsetMs) },
            onDismiss = { showAdjustLyrics = false },
        )
    }
    if (showLyricsOptions && track != null) {
        LyricsOptionsSheet(
            track = track,
            hasLyrics = lyricsState.lyrics?.hasLyrics == true,
            currentOffsetMs = lyricsState.lyrics?.offsetMs ?: 0L,
            isCustomLyrics = viewModel.isCurrentTrackLyricsCustom(),
            onAdjustTiming = {
                showLyricsOptions = false
                showAdjustLyrics = true
            },
            onQuickOffset = { deltaMs ->
                viewModel.adjustLyricsOffset(deltaMs)
            },
            onSearchManual = {
                showLyricsOptions = false
                showManualLyricsSearch = true
            },
            onRefreshLyrics = {
                viewModel.refreshCurrentLyrics()
                Toast.makeText(context, "Reloading lyrics...", Toast.LENGTH_SHORT).show()
            },
            onResetCustomLyrics = {
                viewModel.resetToOriginalLyrics()
                Toast.makeText(context, "Reverting to server original lyrics...", Toast.LENGTH_SHORT).show()
            },
            onCopyLrc = {
                val lrcText = viewModel.getCurrentLrcText()
                if (lrcText.isNotBlank()) {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Lyrics", lrcText))
                    Toast.makeText(context, "Copied .lrc to clipboard for Navidrome!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No lyrics text to copy", Toast.LENGTH_SHORT).show()
                }
            },
            onShareLrc = {
                val lrcText = viewModel.getCurrentLrcText()
                if (lrcText.isNotBlank()) {
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, lrcText)
                        putExtra(android.content.Intent.EXTRA_TITLE, "${track.title}.lrc")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Export or Share .lrc"))
                }
            },
            onBlockLyrics = {
                viewModel.blockLyricsForCurrentTrack()
                Toast.makeText(context, "Lyrics removed & blocked for this track", Toast.LENGTH_SHORT).show()
            },
            onReportLyrics = {
                showLyricsOptions = false
                showReportLyrics = true
            },
            onDismiss = { showLyricsOptions = false },
        )
    }
    if (showManualLyricsSearch && track != null) {
        ManualLyricsSearchDialog(
            initialQuery = "${track.artist.orEmpty()} ${track.title}".trim(),
            trackDurationSeconds = track.duration ?: 0,
            onSearch = { query, onResult ->
                viewModel.searchLrclibCandidates(query, onResult)
            },
            onSelectCandidate = { candidate ->
                viewModel.applyCustomLyrics(candidate)
                showManualLyricsSearch = false
                Toast.makeText(context, "Applied lyrics for \"${candidate.trackName}\"", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showManualLyricsSearch = false },
        )
    }
    if (showReportLyrics && track != null) {
        ReportLyricsDialog(
            track = track,
            onReport = { reason ->
                viewModel.reportLyricsIssue(reason)
                showReportLyrics = false
                Toast.makeText(context, "Reported: $reason (saved in Settings)", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showReportLyrics = false },
        )
    }
}

@Composable
private fun BlurredBackdrop(coverArtId: String?, modifier: Modifier = Modifier) {
    if (coverArtId.isNullOrBlank()) return
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = coverArtId,
            animationSpec = tween(700, easing = FastOutSlowInEasing),
            label = "BackdropCrossfade",
        ) { currentCoverId ->
            CoverImage(
                coverArtId = currentCoverId,
                size = 256,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.30f),
            )
        }
    }
}

@Composable
private fun ArtPanel(
    track: Track,
    downloadState: TrackDownloadState?,
    isMixBlending: Boolean = false,
    mixProgress: Float = 0f,
    mixOutgoingTrack: Track? = null,
    onOpenArtist: (() -> Unit)? = null,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Spotify-style smooth scale & crossfade when track changes
        AnimatedContent(
            targetState = track.coverArtId to track.id,
            transitionSpec = {
                (fadeIn(animationSpec = tween(550, easing = FastOutSlowInEasing)) +
                 scaleIn(initialScale = 0.94f, animationSpec = tween(550, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 1.04f, animationSpec = tween(450, easing = FastOutSlowInEasing))
                    )
            },
            label = "CoverArtTransition",
        ) { (targetCoverId, _) ->
            CoverImage(
                coverArtId = targetCoverId,
                size = 1024,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        AnimatedVisibility(
            visible = isMixBlending,
            enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) + expandVertically(tween(400)),
            exit = fadeOut(tween(350, easing = FastOutSlowInEasing)) + shrinkVertically(tween(350)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(14.dp))
                AutomixBlendPill(
                    progress = mixProgress,
                    outgoingTitle = mixOutgoingTrack?.title,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = track.title,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                         slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> height / 3 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { height -> -height / 3 }
                            )
                    },
                    label = "TitleTransition",
                ) { titleText ->
                    MarqueeText(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        textAlign = TextAlign.Start,
                    )
                }
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = (track.artist ?: track.album.orEmpty()) to track.id,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                         slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> height / 3 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { height -> -height / 3 }
                            )
                    },
                    label = "ArtistTransition",
                ) { (artistText, _) ->
                    MarqueeText(
                        text = artistText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = TextSecondary,
                        textAlign = TextAlign.Start,
                        modifier = if (onOpenArtist != null) {
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onOpenArtist)
                        } else Modifier,
                    )
                }
            }
            IconButton(
                onClick = onDownloadClick,
                enabled = downloadState != TrackDownloadState.DOWNLOADING,
                modifier = Modifier.size(42.dp),
            ) {
                when (downloadState) {
                    TrackDownloadState.DONE -> Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(24.dp),
                    )
                    TrackDownloadState.DOWNLOADING -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SpotifyGreen,
                        strokeWidth = 2.dp,
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Download",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircleOutline,
                    contentDescription = "Add to playlist",
                    tint = TextPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun AutomixBlendPill(
    progress: Float,
    outgoingTitle: String?,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AutomixWave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "w1",
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "w2",
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(490, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "w3",
    )

    Surface(
        color = Color(0xFF1DB954).copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(12.dp),
            ) {
                Box(Modifier.width(2.5.dp).height((12 * wave1).dp).background(SpotifyGreen, CircleShape))
                Box(Modifier.width(2.5.dp).height((12 * wave2).dp).background(SpotifyGreen, CircleShape))
                Box(Modifier.width(2.5.dp).height((12 * wave3).dp).background(SpotifyGreen, CircleShape))
            }
            Text(
                text = "AUTOMIX BLENDING ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = 10.sp,
                ),
                color = SpotifyGreen,
            )
            if (!outgoingTitle.isNullOrBlank()) {
                Text(
                    text = "• from ${outgoingTitle.take(16)}...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LyricsCard(
    positionFlow: Flow<Long>,
    isLoading: Boolean,
    lyrics: List<LyricsLineData>,
    offsetMs: Long = 0L,
    onSeekTo: (Long) -> Unit,
    onExpand: () -> Unit,
    onShare: () -> Unit,
    onAdjustTiming: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF242424),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (lyrics.isNotEmpty()) {
                    if (lyrics.any { it.startMs != null }) {
                        IconButton(
                            onClick = onAdjustTiming,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Adjust lyrics timing",
                                tint = if (offsetMs != 0L) SpotifyGreen else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share lyrics",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Fullscreen lyrics",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(
                    onClick = onOpenOptions,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Lyrics options",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
                lyrics.isEmpty() -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Instrumental or no lyrics available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.TextButton(onClick = onOpenOptions) {
                            Text("Search...", color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> SyncedLyricsList(
                    positionFlow = positionFlow,
                    lyrics = lyrics,
                    offsetMs = offsetMs,
                    onSeekTo = onSeekTo,
                    isFullScreen = false,
                )
            }
        }
    }
}

@Composable
private fun InstrumentalGapDots(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "instrumental_gap")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        for (i in 0 until 3) {
            val animAlpha by transition.animateFloat(
                initialValue = 0.30f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 650, delayMillis = i * 220, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_alpha_$i",
            )
            val animScale by transition.animateFloat(
                initialValue = 0.75f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 650, delayMillis = i * 220, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_scale_$i",
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .scale(if (isActive) animScale else 0.8f)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color.White.copy(alpha = animAlpha)
                        else Color.White.copy(alpha = 0.25f),
                    ),
            )
        }
    }
}

@Composable
private fun AppleMusicLyricLineItem(
    line: LyricsLineData,
    index: Int,
    activeIndex: Int,
    isSynced: Boolean,
    isBrowsing: Boolean,
    isFullScreen: Boolean,
    offsetMs: Long,
    onSeekTo: (Long) -> Unit,
) {
    val isActive = isSynced && index == activeIndex
    val distance = if (activeIndex >= 0) kotlin.math.abs(index - activeIndex) else 0

    val targetAlpha = when {
        !isSynced -> 0.88f
        isBrowsing -> 0.82f
        isActive -> 1.0f
        distance == 1 -> 0.72f
        distance == 2 -> 0.48f
        else -> 0.28f
    }

    val targetBlur = when {
        !isSynced || isBrowsing -> 0.dp
        isActive -> 0.dp
        distance == 1 -> 0.8.dp
        distance == 2 -> 1.7.dp
        else -> 2.6.dp
    }

    val targetScale = when {
        !isSynced -> 1.0f
        isBrowsing -> 0.98f
        isActive -> if (isFullScreen) 1.03f else 1.02f
        distance == 1 -> 0.98f
        else -> 0.96f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)),
        label = "lyric_alpha_$index",
    )
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)),
        label = "lyric_scale_$index",
    )
    val animatedBlur by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 350),
        label = "lyric_blur_$index",
    )

    val activeFontSize = if (isFullScreen) 28.sp else 23.sp
    val activeLineHeight = if (isFullScreen) 36.sp else 30.sp
    val inactiveFontSize = if (isFullScreen) 22.sp else 19.sp
    val inactiveLineHeight = if (isFullScreen) 30.sp else 25.sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(enabled = line.startMs != null) {
                line.startMs?.let { onSeekTo((it - offsetMs).coerceAtLeast(0L)) }
            }
            .padding(vertical = if (isFullScreen) 10.dp else 8.dp),
    ) {
        // Singing Bloom / Halo behind the active line (Apple Music & BitChord signature effect)
        if (isActive) {
            Text(
                text = line.text,
                fontSize = activeFontSize,
                lineHeight = activeLineHeight,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.52f }
                    .blur(8.dp, BlurredEdgeTreatment.Unbounded),
            )
        }

        // Crisp foreground text with distance-based blur & alpha
        Text(
            text = line.text,
            fontSize = if (isActive) activeFontSize else inactiveFontSize,
            lineHeight = if (isActive) activeLineHeight else inactiveLineHeight,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            color = Color.White.copy(alpha = animatedAlpha),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .blur(animatedBlur),
        )
    }
}

@Composable
private fun SyncedLyricsList(
    positionFlow: Flow<Long>,
    lyrics: List<LyricsLineData>,
    offsetMs: Long = 0L,
    onSeekTo: (Long) -> Unit,
    isFullScreen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val positionMs by positionFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val isSynced = remember(lyrics) { lyrics.any { it.startMs != null } }
    val effectivePos = positionMs + offsetMs
    val activeIndex = remember(effectivePos, isSynced) {
        if (!isSynced) -1
        else lyrics.indexOfLast { it.startMs != null && it.startMs <= effectivePos }
    }
    val listState = rememberLazyListState()

    var userScrolledRecently by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledRecently = true
        } else if (userScrolledRecently) {
            delay(1800L)
            userScrolledRecently = false
        }
    }

    LaunchedEffect(activeIndex, userScrolledRecently) {
        if (isSynced && !userScrolledRecently && activeIndex >= 0 && activeIndex in lyrics.indices) {
            val target = (activeIndex - 1).coerceAtLeast(0)
            if (listState.firstVisibleItemIndex != target) {
                listState.animateScrollToItem(target)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = if (isFullScreen) modifier else modifier
            .fillMaxWidth()
            .height(300.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        itemsIndexed(lyrics, key = { index, line -> "$index-${line.text}" }) { index, line ->
            AppleMusicLyricLineItem(
                line = line,
                index = index,
                activeIndex = activeIndex,
                isSynced = isSynced,
                isBrowsing = listState.isScrollInProgress || userScrolledRecently,
                isFullScreen = isFullScreen,
                offsetMs = offsetMs,
                onSeekTo = onSeekTo,
            )

            // Instrumental verse break / gap indicator
            val nextLine = lyrics.getOrNull(index + 1)
            val currentMs = line.startMs
            val nextMs = nextLine?.startMs
            if (currentMs != null && nextMs != null && (nextMs - currentMs >= 5000L)) {
                val isGapActive = isSynced && effectivePos > currentMs + 2000L && effectivePos < nextMs
                InstrumentalGapDots(isActive = isGapActive)
            }
        }
    }
}

@Composable
private fun LyricsFullScreenView(
    isPlaying: Boolean,
    positionFlow: Flow<Long>,
    durationFlow: Flow<Long>,
    track: Track?,
    lyrics: List<LyricsLineData>,
    offsetMs: Long = 0L,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShare: () -> Unit,
    onAdjustTiming: () -> Unit,
    onOpenOptions: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        BlurredBackdrop(coverArtId = track?.coverArtId)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Exit fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                ) {
                    MarqueeText(
                        text = track?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    MarqueeText(
                        text = track?.artist ?: track?.album.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                if (lyrics.any { it.startMs != null }) {
                    IconButton(
                        onClick = onAdjustTiming,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Adjust lyrics timing",
                            tint = if (offsetMs != 0L) SpotifyGreen else Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share lyrics",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = onOpenOptions,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Lyrics options",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            SyncedLyricsList(
                positionFlow = positionFlow,
                lyrics = lyrics,
                offsetMs = offsetMs,
                onSeekTo = onSeekTo,
                isFullScreen = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 8.dp),
            ) {
                SeekSection(
                    positionFlow = positionFlow,
                    durationFlow = durationFlow,
                    onSeekTo = onSeekTo,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun shareLyrics(
    context: android.content.Context,
    track: Track?,
    lyrics: List<LyricsLineData>,
    plainLyrics: String?,
) {
    if (track == null) return

    val lyricsBody = when {
        lyrics.isNotEmpty() -> {
            lyrics.joinToString("\n") { it.text.trim() }.trim()
        }
        !plainLyrics.isNullOrBlank() -> {
            plainLyrics.trim()
        }
        else -> ""
    }

    if (lyricsBody.isBlank()) return

    val shareText = buildString {
        append(lyricsBody)
        append("\n\n")
        append("🎵 ${track.title} - ${track.artist ?: track.album.orEmpty()}\n")
        append("Shared via ASAYS")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.title} - Lyrics")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share lyrics"))
}

@Composable
private fun SeekSection(
    positionFlow: Flow<Long>,
    durationFlow: Flow<Long>,
    onSeekTo: (Long) -> Unit,
) {
    val positionMs by positionFlow.collectAsStateWithLifecycle(initialValue = 0L)
    val durationMs by durationFlow.collectAsStateWithLifecycle(initialValue = 0L)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val fraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SpotifySeekBar(
            fraction = if (dragFraction >= 0f) dragFraction else fraction,
            onScrub = { dragFraction = it },
            onScrubEnd = {
                if (dragFraction >= 0f) {
                    onSeekTo((dragFraction * durationMs).toLong())
                    dragFraction = -1f
                }
            },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration((positionMs / 1000).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatDuration((durationMs / 1000).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun SpotifySeekBar(
    fraction: Float,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var trackWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .onSizeChanged { trackWidth = it.width }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onScrub((offset.x / trackWidth.coerceAtLeast(1)).coerceIn(0f, 1f))
                    onScrubEnd()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onScrub((offset.x / trackWidth.coerceAtLeast(1)).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onScrub((change.position.x / trackWidth.coerceAtLeast(1)).coerceIn(0f, 1f))
                    },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.20f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0.0001f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SpotifyGreen),
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((trackWidth * fraction).toInt() - 6.dp.roundToPx()).coerceIn(
                            0,
                            (trackWidth - 12.dp.roundToPx()).coerceAtLeast(0),
                        ),
                        y = 0,
                    )
                }
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: PlaybackRepeatMode,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSetShuffle: (Boolean) -> Unit,
    onSetRepeat: (PlaybackRepeatMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onSetShuffle(!isShuffleEnabled) },
            modifier = Modifier.size(44.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = if (isShuffleEnabled) "Shuffle On" else "Shuffle Off",
                    tint = if (isShuffleEnabled) SpotifyGreen else TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                if (isShuffleEnabled) {
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
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(
            onClick = {
                onSetRepeat(
                    when (repeatMode) {
                        PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
                        PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
                        PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
                    },
                )
            },
            modifier = Modifier.size(44.dp),
        ) {
            val isRepeatActive = repeatMode != PlaybackRepeatMode.OFF
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (repeatMode == PlaybackRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeatActive) SpotifyGreen else TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
                if (isRepeatActive) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsSheet(
    track: Track,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onViewQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onSleepTimer: () -> Unit,
    onStartSimilarRadio: () -> Unit,
    onAdjustLyrics: () -> Unit,
    hasSyncedLyrics: Boolean,
    albumEnabled: Boolean,
    artistEnabled: Boolean,
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(12.dp))
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
            OptionRow(Icons.Rounded.Shuffle, "Play similar songs (Radio)", onStartSimilarRadio)
            OptionRow(Icons.Rounded.Share, "Share song", onShare)
            OptionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to playlist", onAddToPlaylist)
            OptionRow(Icons.Rounded.Queue, "Add to queue", onAddToQueue)
            OptionRow(Icons.AutoMirrored.Rounded.QueueMusic, "View queue", onViewQueue)
            OptionRow(Icons.Rounded.Album, "Go to album", onGoToAlbum, enabled = albumEnabled)
            OptionRow(Icons.Rounded.Person, "Go to artist", onGoToArtist, enabled = artistEnabled)
            OptionRow(Icons.Rounded.Tune, "Lyrics & timing options", onAdjustLyrics, enabled = true)
            OptionRow(Icons.Rounded.Timer, "Sleep timer", onSleepTimer)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
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
            tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.4f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    remainingSeconds: Long?,
    isEndOfTrack: Boolean,
    currentTrackDurationSeconds: Int,
    currentPositionMs: Long,
    onStartMinutes: (Int) -> Unit,
    onStartEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var customMinutes by remember { mutableIntStateOf(15) }
    var showCustomSlider by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            val isTimerActive = remainingSeconds != null || isEndOfTrack
            if (isTimerActive) {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, SpotifyGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isEndOfTrack) Icons.Rounded.MusicNote else Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEndOfTrack) "Sleep at end of track" else "Sleep timer active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            val countdownText = if (remainingSeconds != null && remainingSeconds > 0) {
                                "${formatCountdown(remainingSeconds)} remaining"
                            } else if (isEndOfTrack) {
                                val leftSec = (currentTrackDurationSeconds - (currentPositionMs / 1000L)).coerceAtLeast(0L)
                                "${formatCountdown(leftSec)} left in track"
                            } else "Stopping soon..."
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreen,
                            )
                        }
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B1518),
                                contentColor = Color(0xFFFF5252),
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Turn off", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val leftInTrack = (currentTrackDurationSeconds - (currentPositionMs / 1000L)).coerceAtLeast(0L)
                SleepTimerOptionRow(
                    icon = Icons.Rounded.MusicNote,
                    title = "End of track",
                    subtitle = if (leftInTrack > 0) "Approximately ${formatCountdown(leftInTrack)} left" else "At end of current song",
                    isSelected = isEndOfTrack,
                    onClick = {
                        onStartEndOfTrack()
                        onDismiss()
                    },
                )

                val presets = listOf(5, 10, 15, 30, 45, 60)
                presets.forEach { mins ->
                    val isSelected = !isEndOfTrack && remainingSeconds != null &&
                        (remainingSeconds in ((mins * 60L - 10)..(mins * 60L + 10)))
                    SleepTimerOptionRow(
                        icon = Icons.Rounded.Timer,
                        title = if (mins >= 60) "1 hour" else "$mins minutes",
                        subtitle = null,
                        isSelected = isSelected,
                        onClick = {
                            onStartMinutes(mins)
                            onDismiss()
                        },
                    )
                }

                SleepTimerOptionRow(
                    icon = Icons.Rounded.Tune,
                    title = "Custom time ($customMinutes min)",
                    subtitle = "Set any duration up to 2 hours",
                    isSelected = showCustomSlider,
                    onClick = { showCustomSlider = !showCustomSlider },
                )

                if (showCustomSlider) {
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { customMinutes = (customMinutes - 5).coerceAtLeast(1) }) {
                                        Icon(Icons.Rounded.Remove, "Less", tint = TextPrimary)
                                    }
                                    Text(
                                        text = "$customMinutes min",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SpotifyGreen,
                                        modifier = Modifier.widthIn(min = 70.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                    IconButton(onClick = { customMinutes = (customMinutes + 5).coerceAtMost(120) }) {
                                        Icon(Icons.Rounded.Add, "More", tint = TextPrimary)
                                    }
                                }
                                Button(
                                    onClick = {
                                        onStartMinutes(customMinutes)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SpotifyGreen,
                                        contentColor = Color.Black,
                                    ),
                                ) {
                                    Text("Start", fontWeight = FontWeight.Bold)
                                }
                            }
                            Slider(
                                value = customMinutes.toFloat(),
                                onValueChange = { customMinutes = it.roundToInt() },
                                valueRange = 1f..120f,
                                steps = 118,
                                colors = SliderDefaults.colors(
                                    thumbColor = SpotifyGreen,
                                    activeTrackColor = SpotifyGreen,
                                    inactiveTrackColor = SurfaceCardHigh,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SleepTimerOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (isSelected) SurfaceCard else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(1.dp, SpotifyGreen) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) SpotifyGreen else TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimary,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustLyricsSheet(
    track: Track,
    currentOffsetMs: Long,
    positionFlow: Flow<Long>,
    lyrics: List<LyricsLineData>,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val positionMs by positionFlow.collectAsStateWithLifecycle(initialValue = 0L)
    var offsetMs by remember(currentOffsetMs) { mutableLongStateOf(currentOffsetMs) }
    val effectivePos = positionMs + offsetMs
    val activeLine = remember(effectivePos, lyrics) {
        lyrics.lastOrNull { it.startMs != null && it.startMs <= effectivePos }?.text
            ?: lyrics.firstOrNull()?.text
            ?: ""
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Adjust Lyrics Timing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = "${track.title} • Fine-tune lyrics sync",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "CURRENT LINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SpotifyGreen,
                            )
                        }
                        Text(
                            text = formatDuration((positionMs / 1000).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (activeLine.isNotBlank()) activeLine else "...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val seconds = offsetMs / 1000.0
            val formattedSeconds = if (offsetMs == 0L) "0.0s (In Sync)"
            else if (offsetMs > 0) "+%.1fs (Rushed)".format(Locale.US, seconds)
            else "%.1fs (Delayed)".format(Locale.US, seconds)

            Text(
                text = formattedSeconds,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (offsetMs == 0L) TextPrimary else SpotifyGreen,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Negative (-) delays lyrics • Positive (+) speeds them up",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
            )

            Spacer(Modifier.height(8.dp))

            Slider(
                value = offsetMs.toFloat(),
                onValueChange = { newVal ->
                    val snapped = (newVal / 100f).roundToInt() * 100L
                    offsetMs = snapped
                    onOffsetChange(snapped)
                },
                valueRange = -10000f..10000f,
                steps = 199,
                colors = SliderDefaults.colors(
                    thumbColor = SpotifyGreen,
                    activeTrackColor = SpotifyGreen,
                    inactiveTrackColor = SurfaceCard,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(-1000L to "-1.0s", -500L to "-0.5s", -100L to "-0.1s").forEach { (step, label) ->
                    OutlinedButton(
                        onClick = {
                            val next = (offsetMs + step).coerceIn(-10000L, 10000L)
                            offsetMs = next
                            onOffsetChange(next)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = {
                        offsetMs = 0L
                        onOffsetChange(0L)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (offsetMs == 0L) SurfaceCard else SpotifyGreen,
                        contentColor = if (offsetMs == 0L) TextSecondary else Color.Black,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                listOf(100L to "+0.1s", 500L to "+0.5s", 1000L to "+1.0s").forEach { (step, label) ->
                    OutlinedButton(
                        onClick = {
                            val next = (offsetMs + step).coerceIn(-10000L, 10000L)
                            offsetMs = next
                            onOffsetChange(next)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = Color.Black,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun formatCountdown(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

@Composable
private fun AboutArtistCard(
    artistState: PlayerArtistState,
    track: Track? = null,
    onOpenArtist: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artist = artistState.artist ?: track?.artist?.takeIf { it.isNotBlank() }?.let { artistName ->
        ArtistOverview(
            id = track.artistId?.takeIf { it.isNotBlank() } ?: artistName,
            name = artistName,
            coverArtId = null,
            imageUrl = null,
            biography = null,
            albumCount = 0,
            isFavorite = false,
        )
    } ?: return

    val imageUrl = remember(artist.imageUrl, artist.coverArtId) {
        artist.imageUrl ?: artist.coverArtId?.takeIf { !it.startsWith("al-") }?.let { CoverUrls.url(it, 720) }
    }

    Column(modifier = modifier) {
        Text(
            text = "About the artist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF242424),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .clickable { onOpenArtist(artist.id) },
        ) {
            Column {
                // Header Image Box with dark bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                ) {
                    // Placeholder background with artist monogram (always rendered underneath)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF333333),
                                        Color(0xFF1E1E1E),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = artist.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }

                    if (!imageUrl.isNullOrBlank()) {
                        val context = LocalContext.current
                        val request = remember(imageUrl) {
                            ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Scrim fade into card body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color(0x80242424),
                                        Color(0xFF242424),
                                    ),
                                ),
                            ),
                    )
                }

                // Body content: Artist Info + Follow button + Bio + Profile link
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
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            val statsText = if (artist.albumCount > 0) {
                                "${artist.albumCount} releases on server"
                            } else {
                                "Artist"
                            }
                            Text(
                                text = statsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }

                        // Spotify-style Follow pill button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (artist.isFavorite) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (artist.isFavorite) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onToggleFavorite() },
                        ) {
                            Text(
                                text = if (artist.isFavorite) "Following" else "Follow",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            )
                        }
                    }

                    if (!artist.biography.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = artist.biography,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "View profile",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
