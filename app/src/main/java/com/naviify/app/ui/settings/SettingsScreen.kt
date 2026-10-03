package com.naviify.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FormatColorReset
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.naviify.app.core.performance.resolvePerformanceRefreshRate
import com.naviify.app.core.performance.supportedPerformanceRefreshRates
import androidx.compose.ui.text.AnnotatedString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.core.storage.ServerMode
import com.naviify.app.core.theme.AppFont
import com.naviify.app.core.theme.AppTheme
import com.naviify.app.domain.playback.StreamQuality
import com.naviify.app.ui.components.SectionHeader
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.theme.ThemeOutline
import com.naviify.app.ui.theme.fontFamilyFor
import com.naviify.app.ui.theme.paletteFor
import com.naviify.app.ui.theme.parseHexColor

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        // 1. Server & Network
        item {
            SettingsGroupCard(
                title = "Server & Network",
                icon = Icons.Rounded.CloudQueue,
            ) {
                // Status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (state.isConnected) {
                                    true -> SpotifyGreen
                                    false -> MaterialTheme.colorScheme.error
                                    null -> MaterialTheme.colorScheme.outline
                                },
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = serverStatusLabel(state),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        state.statusMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isConnected == false) MaterialTheme.colorScheme.error else TextSecondary,
                            )
                        }
                    }
                    TextButton(
                        onClick = viewModel::refresh,
                        enabled = !state.isRefreshing,
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = SpotifyGreen,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Checking...", color = SpotifyGreen)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SpotifyGreen,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh", color = SpotifyGreen)
                        }
                    }
                }

                SettingsDivider()

                RouteStatusRow(
                    effectiveUrl = state.effectiveUrl,
                    mode = state.activeServerMode,
                    reachable = state.homeReachable,
                    onSwitch = viewModel::cycleServerMode,
                )

                SettingsDivider()

                Text(
                    text = "Connection Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                SettingsChips(
                    options = ServerMode.entries,
                    selected = state.activeServerMode,
                    label = { mode ->
                        when (mode) {
                            ServerMode.HOME -> "Home"
                            ServerMode.REMOTE -> "Tailscale"
                            ServerMode.AUTO -> "Auto"
                            ServerMode.OFFLINE -> "Offline"
                        }
                    },
                    onSelect = viewModel::setServerMode,
                )

                SettingsDivider()

                ServerSetupCard(
                    home = state.editHomeUrl,
                    remote = state.editRemoteUrl,
                    onHomeChange = viewModel::onEditHomeUrlChange,
                    onRemoteChange = viewModel::onEditRemoteUrlChange,
                    onSave = viewModel::saveServerUrls,
                )

                SettingsDivider()

                SettingsRow(label = "Username", value = state.username.ifBlank { "—" })
                SettingsRow(label = "Authentication", value = if (state.usesToken) "API Token (Salt & MD5)" else "Plain Password")
            }
        }

        // 2. Server Library Scan
        item {
            SettingsGroupCard(
                title = "Server Library",
                icon = Icons.Rounded.Sync,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full Library Scan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Re-scan all music files, tags, covers & lyrics on your server",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { viewModel.scanLibrary(fullScan = true) },
                        enabled = !state.isScanning,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = SpotifyGreen.copy(alpha = 0.5f),
                            disabledContentColor = Color.Black.copy(alpha = 0.6f),
                        ),
                    ) {
                        if (state.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Scanning...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Progress Line (requested by user: "dir liha b7al wa7d lighne dyal progress bach nchouf fin wslat")
                AnimatedVisibility(
                    visible = state.isScanning,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SpotifyGreen,
                            trackColor = Color.White.copy(alpha = 0.1f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.scanMessage ?: "Scanning server files...",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpotifyGreen,
                                fontWeight = FontWeight.Medium,
                            )
                            state.scanCount?.let { count ->
                                Surface(
                                    color = SpotifyGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = "$count files",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SpotifyGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Status message after scanning
                AnimatedVisibility(
                    visible = !state.isScanning && state.scanMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (state.scanFinished) Icons.Rounded.CheckCircle else Icons.Rounded.CloudQueue,
                            contentDescription = null,
                            tint = if (state.scanFinished) SpotifyGreen else TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.scanMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.scanFinished) SpotifyGreen else TextSecondary,
                        )
                    }
                }
            }
        }

        // 3. Storage & Cache
        item {
            SettingsGroupCard(
                title = "Storage & Cache",
                icon = Icons.Rounded.DownloadDone,
            ) {
                SettingsRow(label = "Audio playback cache", value = formatBytes(state.audioCacheBytes))
                SettingsRow(label = "Image cache (disk)", value = formatBytes(state.diskCacheBytes))
                SettingsRow(label = "Image cache (memory)", value = formatBytes(state.memoryCacheBytes))
                SettingsRow(label = "Downloaded tracks", value = formatBytes(state.downloadsBytes))

                SettingsDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::clearCaches,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Cache", color = TextPrimary, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = viewModel::deleteAllDownloads,
                        enabled = state.downloadsBytes > 0L,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (state.downloadsBytes > 0L) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (state.downloadsBytes > 0L) MaterialTheme.colorScheme.error else TextSecondary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Delete All",
                            color = if (state.downloadsBytes > 0L) MaterialTheme.colorScheme.error else TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // 4. Audio Quality
        item {
            SettingsGroupCard(
                title = "Streaming Quality",
                icon = Icons.Rounded.MusicNote,
            ) {
                Text(
                    text = "Wi-Fi Quality (${state.wifiQuality.label})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                QualityChips(
                    selected = state.wifiQuality,
                    onSelect = viewModel::setWifiQuality,
                )

                SettingsDivider()

                Text(
                    text = "Mobile Data Quality (${state.mobileQuality.label})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                QualityChips(
                    selected = state.mobileQuality,
                    onSelect = viewModel::setMobileQuality,
                )

                Spacer(Modifier.height(10.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Currently streaming ${state.effectiveQuality.label} " +
                                (if (state.isWifi) "on Wi-Fi" else "on Mobile Data"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // 5. Lyrics & Corrections
        item {
            SettingsGroupCard(
                title = "Lyrics & Corrections",
                icon = Icons.Rounded.Flag,
            ) {
                // Blocked lyrics count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocked Track Lyrics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.blockedLyricsCount == 0) "No tracks blocked"
                            else "${state.blockedLyricsCount} track(s) suppressed",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    if (state.blockedLyricsCount > 0) {
                        TextButton(onClick = viewModel::unblockAllLyrics) {
                            Text("Reset All", color = SpotifyGreen, fontSize = 12.sp)
                        }
                    }
                }

                SettingsDivider()

                // Reported tracks header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reported Lyrics Issues",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.reportedTracks.isEmpty()) "No flagged tracks"
                            else "${state.reportedTracks.size} track(s) pending review",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    if (state.reportedTracks.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearAllReportedTracks) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (state.reportedTracks.isEmpty()) {
                    Text(
                        text = "When you encounter incorrect, desynced, or broken lyrics, tap \"Report Lyrics Problem\" in the player. They will be listed here with track info so you can update them on your server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.8f),
                    )
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.reportedTracks.forEach { report ->
                            Surface(
                                color = SurfaceCardHigh,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = report.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = report.artist ?: "Unknown artist",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Surface(
                                                color = Color(0xFFFFB300).copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp),
                                            ) {
                                                Text(
                                                    text = report.reason,
                                                    color = Color(0xFFFFB300),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                            Text(
                                                text = dateFormat.format(Date(report.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary.copy(alpha = 0.6f),
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val copyText = "${report.artist.orEmpty()} ${report.title}".trim()
                                            clipboardManager.setText(AnnotatedString(copyText))
                                            Toast.makeText(context, "Copied: $copyText", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy track info",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeReportedTrack(report.trackId) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextSecondary,
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

        // 6. Personalization & Style
        item {
            SettingsGroupCard(
                title = "Personalization & Style",
                icon = Icons.Rounded.Tune,
            ) {
                ThemeLivePreviewCard(state = state)
                Spacer(Modifier.height(16.dp))
                ThemePresetsSection(
                    selectedTheme = state.theme,
                    onSelectTheme = viewModel::setTheme,
                )
                SettingsDivider()
                AccentColorSection(
                    customAccentHex = state.customAccentHex,
                    activeTheme = state.theme,
                    onSelectHex = viewModel::setCustomAccentHex,
                )
                SettingsDivider()
                FontTypographySection(
                    selectedFont = state.font,
                    onSelectFont = viewModel::setFont,
                )
            }
        }

        // 7. Fluidity & Motion (High-Refresh & Elastic Overscroll)
        item {
            val currentDisplay = LocalView.current.display
            val supportedRates = remember(currentDisplay) {
                currentDisplay.supportedPerformanceRefreshRates()
            }
            val selectedRate = remember(currentDisplay, state.performanceRefreshRate) {
                currentDisplay.resolvePerformanceRefreshRate(state.performanceRefreshRate)
            }

            SettingsGroupCard(
                title = "Fluidity & Motion",
                icon = Icons.Rounded.Speed,
            ) {
                // High Performance Refresh Rate Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "High Refresh Rate (ProMotion)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = SpotifyGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "${selectedRate}Hz",
                                    color = SpotifyGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = if (state.highPerformanceMode) {
                                "Unlocks ${selectedRate}Hz hardware refresh rate for ultra-smooth 120fps scrolling and transitions."
                            } else {
                                "Standard 60Hz balanced battery mode."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Switch(
                        checked = state.highPerformanceMode,
                        onCheckedChange = { viewModel.setHighPerformanceMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpotifyGreen,
                        ),
                    )
                }

                // Supported refresh rates selector (if multiple rates available and mode is enabled)
                if (state.highPerformanceMode && supportedRates.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Preferred Refresh Rate",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        supportedRates.forEach { rate ->
                            val isSelected = rate == selectedRate
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) SpotifyGreen.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SpotifyGreen else ThemeOutline.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setPerformanceRefreshRate(rate) },
                            ) {
                                Text(
                                    text = "${rate}Hz",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SpotifyGreen else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                SettingsDivider()

                // iOS Rubber-Band Overscroll Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "iOS Elastic Overscroll",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.iosOverscrollEnabled) {
                                "UIKit rubber-band elasticity with critically damped physics."
                            } else {
                                "Standard Android stretch effect."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Switch(
                        checked = state.iosOverscrollEnabled,
                        onCheckedChange = { viewModel.setIosOverscrollEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SpotifyGreen,
                        ),
                    )
                }
            }
        }

        // 8. Security, Privacy & Keystore Architecture
        item {
            SettingsGroupCard(
                title = "Security & Privacy",
                icon = Icons.Rounded.Security,
            ) {
                // Keystore status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hardware KeyStore Encryption",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.isHardwareKeystore) "AES-256-GCM Hardware-Backed & Active" else "AES-256-GCM Software Keystore",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotifyGreen,
                        )
                    }
                }

                Text(
                    text = "All server credentials, salts, and tokens are encrypted at rest with hardware keys via Android KeyStore. Zero credentials leave in plaintext or cloud backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )

                SettingsDivider()

                // Keyset Health
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Healing Keystore Health",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.isKeystoreHealthy) "Keyset integrity verified" else "Corrupted keyset healed automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = SpotifyGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.30f)),
                    ) {
                        Text(
                            text = "VERIFIED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                SettingsDivider()

                // Private Listening (Incognito Mode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private Listening (Incognito)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (state.isIncognitoMode) "Active: Scrobbling and history logging paused" else "Inactive: Normal playback logging",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isIncognitoMode) SpotifyGreen else TextSecondary,
                        )
                    }
                    Switch(
                        checked = state.isIncognitoMode,
                        onCheckedChange = { viewModel.toggleIncognito() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = SpotifyGreen,
                        ),
                    )
                }

                SettingsDivider()

                // Fast Privacy & Cache Purge Actions
                Text(
                    text = "Storage & Privacy Hygiene",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearImageCache()
                            Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Clear Images", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearAudioCache()
                            Toast.makeText(context, "Audio stream cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Clear Audio", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 8. Account & Session
        item {
            SettingsGroupCard(
                title = "Account & Session",
                icon = Icons.Rounded.Person,
            ) {
                SettingsRow(label = "Logged in as", value = state.username.ifBlank { "User" })
                SettingsRow(label = "Active Server", value = state.effectiveUrl.ifBlank { state.serverUrl })

                SettingsDivider()

                Button(
                    onClick = viewModel::disconnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Disconnect Server", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SpotifyGreen,
                letterSpacing = 0.8.sp,
            )
        }
        Surface(
            color = SurfaceCardHigh,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f)),
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun serverStatusLabel(state: SettingsUiState): String = when {
    state.isRefreshing -> "Checking server..."
    state.isConnected == true -> buildString {
        append(state.serverType ?: "Subsonic")
        state.serverVersion?.let { append(" · version $it") }
    }
    state.isConnected == false -> "Server unreachable"
    else -> "Unknown"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return "${"%.1f".format(value).trimEnd('0').trimEnd('.')} ${units[unitIndex]}"
}

@Composable
private fun ServerSetupCard(
    home: String,
    remote: String,
    onHomeChange: (String) -> Unit,
    onRemoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Server URLs",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        OutlinedTextField(
            value = home,
            onValueChange = onHomeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Home LAN URL") },
            placeholder = { Text("http://192.168.x.x:4533") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = SpotifyGreen,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        OutlinedTextField(
            value = remote,
            onValueChange = onRemoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tailscale / Remote URL") },
            placeholder = { Text("http://100.x.x.x:4533") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = SpotifyGreen,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Save URLs", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RouteStatusRow(
    effectiveUrl: String,
    mode: ServerMode,
    reachable: Boolean,
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (reachable) SpotifyGreen else MaterialTheme.colorScheme.error),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (mode == ServerMode.HOME) Icons.Rounded.Home else Icons.Rounded.Public,
            contentDescription = null,
            tint = SpotifyGreen,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Active route · ${if (mode == ServerMode.HOME) "Home LAN" else "Tailscale"}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                text = effectiveUrl.ifBlank { "Not connected" },
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
            )
        }
        TextButton(onClick = onSwitch) {
            Text("Switch", color = SpotifyGreen)
        }
    }
}

@Composable
private fun <T> SettingsChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun QualityChips(
    selected: StreamQuality,
    onSelect: (StreamQuality) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StreamQuality.entries.forEach { quality ->
            FilterChip(
                selected = selected == quality,
                onClick = { onSelect(quality) },
                label = { Text(quality.label) },
            )
        }
    }
}

@Composable
private fun ThemeLivePreviewCard(state: SettingsUiState) {
    val activePalette = paletteFor(state.theme).let {
        val customColor = parseHexColor(state.customAccentHex)
        if (customColor != null) it.copy(accent = customColor) else it
    }
    val currentFont = fontFamilyFor(state.font)

    Surface(
        color = activePalette.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (activePalette.isGlass) 1.5.dp else 1.dp,
            color = if (activePalette.isGlass) activePalette.accent.copy(alpha = 0.5f) else activePalette.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(activePalette.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = activePalette.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Live Theme Preview",
                            fontFamily = currentFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = activePalette.textPrimary,
                        )
                        Text(
                            text = "${state.theme.label} • ${state.font.label}",
                            fontFamily = currentFont,
                            fontSize = 12.sp,
                            color = activePalette.textSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(activePalette.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (activePalette.accent.luminance() > 0.6f) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(activePalette.surfaceHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(activePalette.accent),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "1:42",
                    fontFamily = currentFont,
                    fontSize = 11.sp,
                    color = activePalette.textSecondary,
                )
                Text(
                    text = if (state.customAccentHex.isNotBlank()) "Accent: ${state.customAccentHex.uppercase()}" else "Theme Default Accent",
                    fontFamily = currentFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activePalette.accent,
                )
                Text(
                    text = "3:24",
                    fontFamily = currentFont,
                    fontSize = 11.sp,
                    color = activePalette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ThemePresetsSection(
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PRESET THEMES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyGreen,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTheme.entries.forEach { theme ->
                val isSelected = theme == selectedTheme
                val pal = paletteFor(theme)
                Surface(
                    color = if (isSelected) SurfaceCardHigh else SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) SpotifyGreen else ThemeOutline.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier
                        .width(165.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTheme(theme) },
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(pal.background)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(pal.surface)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(pal.accent)
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(SpotifyGreen),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = theme.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.9f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = theme.subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

private val PRESET_ACCENTS = listOf(
    "#69B987" to "ASAYS Green (Logo)",
    "#1DB954" to "Spotify Green",
    "#38BDF8" to "Ice Cyan",
    "#00D2FF" to "Electric Marine",
    "#FF5E7E" to "Sunset Coral",
    "#A855F7" to "Neon Purple",
    "#E040FB" to "Cyber Magenta",
    "#10B981" to "Emerald Mint",
    "#F59E0B" to "Solar Amber",
    "#EF4444" to "Crimson Flame",
    "#FB7185" to "Rose Gold",
    "#E2E8F0" to "Platinum Titanium",
)

@Composable
private fun AccentColorSection(
    customAccentHex: String,
    activeTheme: AppTheme,
    onSelectHex: (String) -> Unit,
) {
    var customInput by remember(customAccentHex) { mutableStateOf(customAccentHex) }
    var inputError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = "ACCENT COLOR",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyGreen,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text = "Match your active theme colors or pick a custom accent",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isDefault = customAccentHex.isBlank()
            val defaultColor = parseHexColor(activeTheme.defaultAccentHex) ?: SpotifyGreen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectHex("") }
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardHigh)
                        .border(
                            width = if (isDefault) 2.5.dp else 1.dp,
                            color = if (isDefault) SpotifyGreen else ThemeOutline,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatColorReset,
                        contentDescription = "Theme default",
                        tint = defaultColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Match Theme",
                    fontSize = 10.sp,
                    color = if (isDefault) SpotifyGreen else TextSecondary,
                    fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Normal,
                )
            }

            PRESET_ACCENTS.forEach { (hex, name) ->
                val isSelected = customAccentHex.equals(hex, ignoreCase = true)
                val color = parseHexColor(hex) ?: Color.White
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectHex(hex) }
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.6f) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = name.split(" ").first(),
                        fontSize = 10.sp,
                        color = if (isSelected) SpotifyGreen else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = customInput,
                onValueChange = {
                    customInput = it
                    inputError = false
                },
                placeholder = { Text("Custom HEX (e.g. #FF0077)", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                isError = inputError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = ThemeOutline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val trimmed = customInput.trim()
                    if (parseHexColor(trimmed) != null) {
                        onSelectHex(if (trimmed.startsWith("#")) trimmed else "#$trimmed")
                        inputError = false
                    } else {
                        inputError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FontTypographySection(
    selectedFont: AppFont,
    onSelectFont: (AppFont) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = "TYPOGRAPHY & FONT",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SpotifyGreen,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text = "Select your preferred font family across player, titles & lyrics",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFont.entries.forEach { font ->
                val isSelected = font == selectedFont
                val sampleFamily = fontFamilyFor(font)
                Surface(
                    color = if (isSelected) SurfaceCardHigh else SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) SpotifyGreen else ThemeOutline.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectFont(font) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aa",
                                    fontFamily = sampleFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (isSelected) SpotifyGreen else TextPrimary,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = font.label,
                                    fontFamily = sampleFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = font.subtitle,
                                fontFamily = sampleFamily,
                                fontSize = 12.sp,
                                color = TextSecondary,
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
