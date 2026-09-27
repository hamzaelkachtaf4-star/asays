package com.naviify.app.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlayerUiState
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.connect.ConnectDevicesSheet
import com.naviify.app.ui.connect.ConnectDevicesViewModel
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

    val connectViewModel: ConnectDevicesViewModel = hiltViewModel()
    val cluster by connectViewModel.cluster.collectAsStateWithLifecycle()
    var showDevices by rememberSaveable { mutableStateOf(false) }

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

    // Le nom de l'appareil qui joue, quand ce n'est pas ce telephone : on le montre
    // dans la barre, et un appui ouvre le choix d'appareil.
    val active = cluster?.devices?.firstOrNull { it.id == cluster?.activeId }
    val remoteDevice = active
        ?.takeIf { it.id != connectViewModel.selfId }
        ?.name
        ?.takeIf { it.isNotBlank() }

    MiniPlayerContent(
        track = currentTrack,
        isPlaying = isPlaying,
        progressFractionFlow = progressFractionFlow,
        onTogglePlayPause = viewModel::togglePlayPause,
        onOpen = onOpen,
        modifier = modifier,
        remoteDevice = remoteDevice,
        onRemoteDevice = { showDevices = true },
    )

    if (showDevices) {
        ConnectDevicesSheet(
            viewModel = connectViewModel,
            onDismiss = { showDevices = false },
        )
    }
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
    remoteDevice: String? = null,
    onRemoteDevice: () -> Unit = {},
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
            Crossfade(
                targetState = track.coverArtId to track.id,
                animationSpec = tween(400),
                label = "MiniCoverCrossfade",
            ) { (coverId, _) ->
                CoverImage(
                    coverArtId = coverId,
                    size = 256,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Pastille « lecture sur <appareil> » : la meme chose que la barre
                // d'une enceinte connectee, visible sans ouvrir l'ecran de lecture.
                if (remoteDevice != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SurfaceCardHigh)
                            .clickable(onClick = onRemoteDevice)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cast,
                            contentDescription = "Appareil de lecture",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = remoteDevice,
                            style = MaterialTheme.typography.labelSmall,
                            color = SpotifyGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Crossfade(
                    targetState = track.title,
                    animationSpec = tween(300),
                    label = "MiniTitleCrossfade",
                ) { titleText ->
                    MarqueeText(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                Crossfade(
                    targetState = (track.artist ?: track.album.orEmpty()) to track.id,
                    animationSpec = tween(300),
                    label = "MiniArtistCrossfade",
                ) { (artistText, _) ->
                    MarqueeText(
                        text = artistText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
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
