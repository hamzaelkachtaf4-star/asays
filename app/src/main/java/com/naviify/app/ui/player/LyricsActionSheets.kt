package com.naviify.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naviify.app.data.lyrics.LyricsCandidate
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.theme.ThemeOutline
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsOptionsSheet(
    track: Track,
    hasLyrics: Boolean,
    currentOffsetMs: Long,
    isCustomLyrics: Boolean = false,
    onAdjustTiming: () -> Unit,
    onQuickOffset: (Long) -> Unit,
    onSearchManual: () -> Unit,
    onRefreshLyrics: () -> Unit,
    onResetCustomLyrics: () -> Unit = {},
    onCopyLrc: () -> Unit = {},
    onShareLrc: () -> Unit = {},
    onBlockLyrics: () -> Unit,
    onReportLyrics: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lyrics Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "${track.title} • ${track.artist.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            if (isCustomLyrics) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = SpotifyGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Custom Synced • Saved permanently (Safe from cache clear)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SpotifyGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quick timing offset adjustment strip
            if (hasLyrics) {
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Quick Timing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            val seconds = currentOffsetMs / 1000.0
                            val offsetText = if (currentOffsetMs == 0L) "In Sync (0.0s)"
                            else "%.1fs (%s)".format(Locale.US, seconds, if (currentOffsetMs > 0) "Rushed" else "Delayed")
                            Text(
                                text = offsetText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (currentOffsetMs == 0L) TextSecondary else SpotifyGreen,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onQuickOffset(-500L) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("-0.5s", fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { onQuickOffset(500L) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("+0.5s", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Menu actions
            LyricsMenuItem(
                icon = Icons.Rounded.Search,
                title = "Search & Change Lyrics",
                subtitle = "Pick from LRCLIB database or search custom terms",
                tint = SpotifyGreen,
                onClick = {
                    onDismiss()
                    onSearchManual()
                },
            )

            if (hasLyrics) {
                LyricsMenuItem(
                    icon = Icons.Rounded.Tune,
                    title = "Fine-tune Timing (Slider)",
                    subtitle = "Adjust sync millisecond-by-millisecond",
                    tint = TextPrimary,
                    onClick = {
                        onDismiss()
                        onAdjustTiming()
                    },
                )

                LyricsMenuItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "Copy .lrc (for Navidrome)",
                    subtitle = "Copy calibrated .lrc to clipboard for your Navidrome server folder",
                    tint = SpotifyGreen,
                    onClick = {
                        onDismiss()
                        onCopyLrc()
                    },
                )

                LyricsMenuItem(
                    icon = Icons.Rounded.Share,
                    title = "Export / Share .lrc File",
                    subtitle = "Share calibrated .lrc file to device storage or apps",
                    tint = TextPrimary,
                    onClick = {
                        onDismiss()
                        onShareLrc()
                    },
                )
            }

            if (isCustomLyrics) {
                LyricsMenuItem(
                    icon = Icons.Rounded.Refresh,
                    title = "Reset to Server Lyrics",
                    subtitle = "Remove custom adjustments and restore Navidrome default",
                    tint = Color(0xFFFFB300),
                    onClick = {
                        onDismiss()
                        onResetCustomLyrics()
                    },
                )
            } else {
                LyricsMenuItem(
                    icon = Icons.Rounded.Refresh,
                    title = "Reload Lyrics",
                    subtitle = "Clear cache and re-fetch from server & LRCLIB",
                    tint = TextPrimary,
                    onClick = {
                        onDismiss()
                        onRefreshLyrics()
                    },
                )
            }

            if (hasLyrics) {
                LyricsMenuItem(
                    icon = Icons.Rounded.Block,
                    title = "Block & Remove These Lyrics",
                    subtitle = "Wipe wrong lyrics from cache and suppress them",
                    tint = Color(0xFFFF5252),
                    onClick = {
                        onDismiss()
                        onBlockLyrics()
                    },
                )
            }

            LyricsMenuItem(
                icon = Icons.Rounded.Flag,
                title = "Report Lyrics Problem",
                subtitle = "Save to your server to-do list in Settings",
                tint = Color(0xFFFFB300),
                onClick = {
                    onDismiss()
                    onReportLyrics()
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LyricsMenuItem(
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
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
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
fun ManualLyricsSearchDialog(
    initialQuery: String,
    trackDurationSeconds: Int,
    onSearch: (String, (List<LyricsCandidate>) -> Unit) -> Unit,
    onSelectCandidate: (LyricsCandidate) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<LyricsCandidate>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (query.isNotBlank()) {
            isSearching = true
            onSearch(query) { list ->
                results = list
                isSearching = false
                hasSearched = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardHigh,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Search Lyrics (LRCLIB)",
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
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Song title and artist...", color = TextSecondary) },
                singleLine = true,
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = SpotifyGreen,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = {
                            if (query.isNotBlank()) {
                                isSearching = true
                                onSearch(query) { list ->
                                    results = list
                                    isSearching = false
                                    hasSearched = true
                                }
                            }
                        }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = SpotifyGreen)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) {
                        isSearching = true
                        onSearch(query) { list ->
                            results = list
                            isSearching = false
                            hasSearched = true
                        }
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = ThemeOutline,
                    cursorColor = SpotifyGreen,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(12.dp))

            if (results.isNotEmpty()) {
                Text(
                    text = "${results.size} matches found • tap to apply",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { candidate ->
                        CandidateItem(
                            candidate = candidate,
                            targetDuration = trackDurationSeconds,
                            onSelect = {
                                onSelectCandidate(candidate)
                                onDismiss()
                            },
                        )
                    }
                }
            } else if (hasSearched && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No lyrics found. Try refining artist or song name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CandidateItem(
    candidate: LyricsCandidate,
    targetDuration: Int,
    onSelect: () -> Unit,
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = candidate.trackName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (candidate.isSynced) SpotifyGreen.copy(alpha = 0.2f) else SurfaceCardHigh,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = if (candidate.isSynced) "SYNCED" else "PLAIN",
                        color = if (candidate.isSynced) SpotifyGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${candidate.artistName}${candidate.albumName?.let { " • $it" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (candidate.durationSeconds > 0) {
                    val m = candidate.durationSeconds / 60
                    val s = candidate.durationSeconds % 60
                    val timeStr = "%d:%02d".format(m, s)
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (targetDuration > 0 && kotlin.math.abs(candidate.durationSeconds - targetDuration) <= 3) SpotifyGreen else TextSecondary,
                    )
                }
            }
            val preview = (candidate.syncedLyrics ?: candidate.plainLyrics).orEmpty()
                .lines()
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString(" • ") { it.replace(Regex("""^\[\d+:\d+(\.\d+)?\]"""), "").trim() }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\"$preview\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLyricsDialog(
    track: Track,
    onReport: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val reasons = listOf(
        "Wrong song lyrics (Completely different music)",
        "Out of sync with audio (Lyrics too fast or slow)",
        "Incomplete / Cut-off lyrics",
        "Instrumental track (Should not have lyrics)",
        "Spelling or formatting mistakes",
    )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Report Lyrics Issue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Flags '${track.title}' to review in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            reasons.forEach { reason ->
                Surface(
                    color = SurfaceCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onReport(reason)
                            onDismiss()
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
