package com.naviify.app.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.data.connect.ConnectDevice
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

/**
 * « Ecouter sur » : la meme chose que le choix d'appareil d'une enceinte
 * connectee. On y voit les appareils du foyer ; toucher l'un d'eux lui confie la
 * lecture, et si c'est un autre appareil qui joue, les commandes d'ici agissent
 * sur lui a distance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectDevicesSheet(
    viewModel: ConnectDevicesViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cluster by viewModel.cluster.collectAsStateWithLifecycle()

    val devices = cluster?.devices.orEmpty().sortedBy { it.name.lowercase() }
    val activeId = cluster?.activeId
    val active = devices.firstOrNull { it.id == activeId }
    val selfActive = activeId == viewModel.selfId
    val playing = cluster?.player?.playing == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
        ) {
            Text(
                text = "Ecouter sur",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = when {
                    active == null -> "Aucun appareil connecte pour l'instant."
                    selfActive -> "Cet appareil joue."
                    else -> "Lecture sur ${active.name}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.size(16.dp))

            // Les commandes a distance n'ont de sens que si un autre appareil joue.
            if (!selfActive && active != null) {
                RemoteTransport(
                    playing = playing,
                    onCommand = { type -> viewModel.send(active.id, type) },
                )
                Spacer(Modifier.size(16.dp))
            }

            if (devices.isEmpty()) {
                Text(
                    text = "Ouvre le site ASAYS ou l'application sur un autre appareil : " +
                        "il apparaitra ici tout seul.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                devices.forEach { device ->
                    DeviceRow(
                        device = device,
                        subtitle = when {
                            device.id == activeId -> "Lecture en cours"
                            device.id == viewModel.selfId -> "Cet appareil"
                            else -> "Disponible"
                        },
                        isActive = device.id == activeId,
                        onClick = {
                            when {
                                device.id == activeId -> Unit
                                device.id == viewModel.selfId -> viewModel.activateSelf()
                                else -> viewModel.transferTo(device.id)
                            }
                        },
                    )
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: ConnectDevice,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (device.kind == "web") Icons.Rounded.Computer else Icons.Rounded.Smartphone

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) SpotifyGreen else TextSecondary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.ifBlank { "Appareil" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isActive) SpotifyGreen else TextPrimary,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Appareil actif",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Trois boutons qui agissent sur l'appareil qui joue, depuis ce telephone. */
@Composable
private fun RemoteTransport(
    playing: Boolean,
    onCommand: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCardHigh, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onCommand("prev") }) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Precedent",
                tint = TextPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable { onCommand(if (playing) "pause" else "play") },
            shape = CircleShape,
            color = SpotifyGreen,
        ) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pause" else "Lecture",
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(14.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { onCommand("next") }) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Suivant",
                tint = TextPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
