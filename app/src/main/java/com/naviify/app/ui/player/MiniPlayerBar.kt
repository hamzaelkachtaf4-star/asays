package com.naviify.app.ui.player

import androidx.compose.foundation.background
import com.naviify.app.ui.components.MarqueeText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlayerUiState
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun MiniPlayerBar(
    viewModel: PlayerViewModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track by remember(viewModel) {
        viewModel.state.map { it.currentTrack }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.currentTrack)

    val isPlaying by remember(viewModel) {
        viewModel.state.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.state.value.isPlaying)

    val currentTrack = track ?: return

    val progressFractionFlow = remember(viewModel) {
        viewModel.state.map { state ->
            if (state.durationMs > 0) {
                (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
        }.distinctUntilChanged()
    }

    MiniPlayerContent(
        track = currentTrack,
        isPlaying = isPlaying,
        progressFractionFlow = progressFractionFlow,
        onTogglePlayPause = viewModel::togglePlayPause,
        onOpen = onOpen,
        modifier = modifier,
    )
}

/**
 * Overload for previews or static states.
 */
@Composable
fun MiniPlayerBar(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.currentTrack ?: return
    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    MiniPlayerContent(
        track = track,
        isPlaying = state.isPlaying,
        progressFractionFlow = remember(progress) { flowOf(progress) },
        onTogglePlayPause = onTogglePlayPause,
        onOpen = onOpen,
        modifier = modifier,
    )
}

@Composable
fun MiniPlayerContent(
    track: Track,
    isPlaying: Boolean,
    progressFractionFlow: Flow<Float>,
    onTogglePlayPause: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .clickable(onClick = onOpen),
    ) {
        MiniPlayerProgressBar(progressFractionFlow = progressFractionFlow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                MarqueeText(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                MarqueeText(
                    text = track.artist ?: track.album.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressBar(
    progressFractionFlow: Flow<Float>,
) {
    val progressFraction by progressFractionFlow.collectAsStateWithLifecycle(initialValue = 0f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(SurfaceCardHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressFraction.coerceIn(0.0001f, 1f))
                .height(2.dp)
                .background(SpotifyGreen),
        )
    }
}
