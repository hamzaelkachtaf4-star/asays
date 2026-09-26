package com.naviify.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlayerUiState
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import java.util.Collections

private data class QueueEntry(
    val key: String,
    val track: Track,
)

@Composable
fun QueueSheet(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
) {
    // Collect inside the sheet so the 5 Hz position tick invalidates only this
    // composable and not the shell, bottom bar and navigation host behind it.
    val state by viewModel.state.collectAsStateWithLifecycle()
    QueueSheet(
        state = state,
        onDismiss = onDismiss,
        onSelect = onSelect,
        onRemove = viewModel::removeFromQueue,
        onMove = viewModel::moveInQueue,
        onToggleShuffle = onToggleShuffle,
        onReshuffle = viewModel::reshuffleQueue,
        onClear = viewModel::clear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onToggleShuffle: () -> Unit = {},
    onReshuffle: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHistory by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 56.dp.toPx() } }

    val currentTrack = state.currentTrack
    val hasPrevious = state.currentIndex > 0

    // Build stable entries for upcoming tracks so each has a unique, persistent key across reordering
    val rawUpcomingEntries = remember(state.queue, state.currentIndex) {
        if (state.currentIndex in state.queue.indices) {
            val upcoming = state.queue.subList(state.currentIndex + 1, state.queue.size)
            val counts = mutableMapOf<String, Int>()
            upcoming.map { track ->
                val c = counts.getOrDefault(track.id, 0)
                counts[track.id] = c + 1
                QueueEntry(key = "q_${track.id}_$c", track = track)
            }
        } else emptyList()
    }

    // Local mutable list for smooth visual reordering without tearing or resetting gestures
    val localUpcoming = remember { mutableStateListOf<QueueEntry>() }

    var isDragging by remember { mutableStateOf(false) }
    var draggedItemKey by remember { mutableStateOf<String?>(null) }
    var initialDragLocalIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(rawUpcomingEntries) {
        if (!isDragging) {
            localUpcoming.clear()
            localUpcoming.addAll(rawUpcomingEntries)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCardHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${state.queue.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Shuffle toggle chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.isShuffleEnabled) SpotifyGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            if (state.isShuffleEnabled) SpotifyGreen else Color.White.copy(alpha = 0.15f),
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onToggleShuffle() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Toggle Shuffle",
                                tint = if (state.isShuffleEnabled) SpotifyGreen else TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (state.isShuffleEnabled) "Shuffled" else "Shuffle",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.isShuffleEnabled) SpotifyGreen else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Reshuffle button if shuffle is on
                    if (state.isShuffleEnabled && state.queue.size > 2) {
                        IconButton(
                            onClick = onReshuffle,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reshuffle Queue",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Clear queue button
                    if (state.queue.size > 1) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear Queue",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Now Playing Section
                if (currentTrack != null) {
                    item(key = "header_now_playing") {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                    item(key = "now_playing_${currentTrack.id}") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SpotifyGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CoverImage(
                                    coverArtId = currentTrack.coverArtId,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SpotifyGreen,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    currentTrack.artist?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }

                // Up Next Section Header
                item(key = "header_up_next") {
                    val firstIsUserQueued = localUpcoming.firstOrNull()?.track?.isUserQueued == true
                    val userQueuedCount = if (firstIsUserQueued) {
                        localUpcoming.takeWhile { it.track.isUserQueued }.size
                    } else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (firstIsUserQueued) "NEXT IN QUEUE ($userQueuedCount)" else "UP NEXT (${localUpcoming.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (firstIsUserQueued) SpotifyGreen else TextSecondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (localUpcoming.isEmpty()) {
                    item(key = "empty_up_next") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No upcoming tracks in queue",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary.copy(alpha = 0.6f),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = localUpcoming,
                        key = { _, entry -> entry.key },
                    ) { index, entry ->
                        val track = entry.track
                        val isBeingDragged = entry.key == draggedItemKey
                        val isFirstFromPlaylist = index > 0 && !track.isUserQueued && localUpcoming.getOrNull(index - 1)?.track?.isUserQueued == true

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (isFirstFromPlaylist) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (!currentTrack?.album.isNullOrBlank()) "NEXT FROM: ${currentTrack?.album?.uppercase()}" else "NEXT UP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                )
                            }

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
                                    .clickable {
                                        val queueIdx = state.currentIndex + 1 + index
                                        onSelect(queueIdx)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                            CoverImage(
                                coverArtId = track.coverArtId,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isBeingDragged) SpotifyGreen else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                track.artist?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            // Clean remove button
                            IconButton(
                                onClick = {
                                    val queueIdx = state.currentIndex + 1 + index
                                    onRemove(queueIdx)
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove from Queue",
                                    tint = TextSecondary.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // Spotify-style Drag Handle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .pointerInput(entry.key) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDragging = true
                                                draggedItemKey = entry.key
                                                initialDragLocalIndex = localUpcoming.indexOfFirst { it.key == entry.key }
                                                dragOffsetY = 0f
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val current = localUpcoming.indexOfFirst { it.key == entry.key }
                                                if (current < 0) return@detectDragGestures
                                                val threshold = itemHeightPx * 0.5f

                                                if (dragOffsetY > threshold && current < localUpcoming.lastIndex) {
                                                    Collections.swap(localUpcoming, current, current + 1)
                                                    dragOffsetY -= itemHeightPx
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else if (dragOffsetY < -threshold && current > 0) {
                                                    Collections.swap(localUpcoming, current, current - 1)
                                                    dragOffsetY += itemHeightPx
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            },
                                            onDragEnd = {
                                                val start = initialDragLocalIndex
                                                val finalIdx = localUpcoming.indexOfFirst { it.key == entry.key }
                                                if (start != null && finalIdx >= 0 && start != finalIdx) {
                                                    val fromQueueIdx = state.currentIndex + 1 + start
                                                    val toQueueIdx = state.currentIndex + 1 + finalIdx
                                                    onMove(fromQueueIdx, toQueueIdx)
                                                }
                                                isDragging = false
                                                draggedItemKey = null
                                                initialDragLocalIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                localUpcoming.clear()
                                                localUpcoming.addAll(rawUpcomingEntries)
                                                isDragging = false
                                                draggedItemKey = null
                                                initialDragLocalIndex = null
                                                dragOffsetY = 0f
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = if (isBeingDragged) SpotifyGreen else TextSecondary.copy(alpha = 0.75f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }

                // Previously Played Section
                if (hasPrevious) {
                    item(key = "header_history") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHistory = !showHistory }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "PREVIOUSLY PLAYED (${state.currentIndex})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (showHistory) "Hide" else "Show",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotifyGreen,
                            )
                        }
                    }

                    if (showHistory) {
                        val historyIndices = (0 until state.currentIndex).toList()
                        itemsIndexed(
                            items = historyIndices,
                            key = { _, index -> "history_${state.queue[index].id}_$index" },
                        ) { _, index ->
                            val track = state.queue[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(index) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CoverImage(
                                    coverArtId = track.coverArtId,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    track.artist?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onSelect(index) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay,
                                        contentDescription = "Replay",
                                        tint = TextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp),
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
