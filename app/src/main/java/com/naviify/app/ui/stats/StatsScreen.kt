package com.naviify.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.data.stats.PlayStat
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.components.CoverImage
import com.naviify.app.ui.components.SectionHeader
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.NaviifyBlack
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.theme.ThemeOutline

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    onPlayTrack: ((Track) -> Unit)? = null,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaviifyBlack),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Listening Recap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        text = "Your music habits & activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // Period Selector Tabs: [ 7D ] [ 30D ] [ 90D ] [ All ]
        item {
            PeriodSelector(
                selected = state.selectedPeriod,
                onSelect = viewModel::selectPeriod,
            )
        }

        // 2x2 Metric Cards (Total Plays, Listening Time, Unique Artists, Streak)
        item {
            MetricCardsGrid(state = state)
        }

        // Daily Activity Bar Chart
        if (state.dailyActivity.isNotEmpty()) {
            item {
                DailyActivityCard(
                    points = state.dailyActivity,
                    maxPlays = state.maxDailyPlays,
                )
            }
        }

        // Listening Hours Peak & Distribution
        if (state.hourlyActivity.isNotEmpty()) {
            item {
                ListeningHoursCard(
                    hours = state.hourlyActivity,
                    peakText = state.peakHourText,
                )
            }
        }

        // Genres / Artists Share Breakdown
        if (state.topGenres.isNotEmpty()) {
            item {
                ArtistsBreakdownCard(genres = state.topGenres)
            }
        }

        // Listening History Heatmap (GitHub Contribution Style with pixel-perfect alignment)
        if (state.heatmapWeeks.isNotEmpty()) {
            item {
                ListeningHistoryHeatmapCard(weeks = state.heatmapWeeks)
            }
        }

        // Recent Plays Section
        if (state.recentPlays.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("RECENT PLAYS")
            }
            itemsIndexed(
                state.recentPlays,
                key = { index, item -> "${item.trackId}_${item.lastPlayedAt}_$index" },
            ) { _, item ->
                RecentPlayRow(
                    item = item,
                    onClick = onPlayTrack?.let {
                        {
                            it(
                                Track(
                                    id = item.trackId,
                                    title = item.title,
                                    artist = item.artist,
                                    album = item.album,
                                    coverArtId = item.coverArtId,
                                ),
                            )
                        }
                    },
                )
            }
        }

        // Top Played Tracks Section
        if (state.topTracks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("TOP PLAYED TRACKS")
            }
            itemsIndexed(
                state.topTracks,
                key = { index, stat -> "${stat.trackId}_$index" },
            ) { index, stat ->
                TopTrackRow(
                    rank = index + 1,
                    stat = stat,
                    onClick = onPlayTrack?.let {
                        {
                            it(
                                Track(
                                    id = stat.trackId,
                                    title = stat.title,
                                    artist = stat.artist,
                                    album = stat.album,
                                    albumId = stat.albumId,
                                    coverArtId = stat.coverArtId,
                                ),
                            )
                        }
                    },
                )
            }
        }

        // Empty state when nothing has been listened to yet
        if (state.totalPlays == 0L && state.recentPlays.isEmpty()) {
            item {
                Surface(
                    color = SurfaceCardHigh,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No listening activity yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Play songs to track your stats, daily activity, listening hours, and streaks here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    Surface(
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatsPeriod.entries.forEach { period ->
                val isSelected = period == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SpotifyGreen else Color.Transparent)
                        .clickable { onSelect(period) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCardsGrid(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatsMetricCard(
                icon = Icons.Rounded.MusicNote,
                value = "${state.totalPlays}",
                title = "Total Plays",
                modifier = Modifier.weight(1f),
            )
            StatsMetricCard(
                icon = Icons.Rounded.Schedule,
                value = state.listeningTimeFormatted,
                title = "Listening Time",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatsMetricCard(
                icon = Icons.Rounded.Group,
                value = "${state.uniqueArtists}",
                title = "Unique Artists",
                modifier = Modifier.weight(1f),
            )
            StatsMetricCard(
                icon = Icons.Rounded.LocalFireDepartment,
                value = "${state.streakDays}d",
                title = if (state.bestStreakDays > state.streakDays) "Streak (Best: ${state.bestStreakDays}d)" else "Listening Streak",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsMetricCard(
    icon: ImageVector,
    value: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DailyActivityCard(
    points: List<DailyActivityPoint>,
    maxPlays: Int,
) {
    var selectedPoint by remember { mutableStateOf<DailyActivityPoint?>(null) }

    Surface(
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DAILY ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                if (selectedPoint != null) {
                    Text(
                        text = "${selectedPoint?.dateStr} · ${selectedPoint?.plays} plays",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreen,
                    )
                } else {
                    val totalPlays = points.sumOf { it.plays }
                    Text(
                        text = "$totalPlays plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Bars Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // Baseline rule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ThemeOutline.copy(alpha = 0.3f)),
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 1.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val barWidth = when {
                        points.size <= 7 -> 16.dp
                        points.size <= 30 -> 6.dp
                        else -> 3.dp
                    }

                    points.forEach { pt ->
                        val isSelected = selectedPoint == pt
                        val fraction = if (maxPlays > 0) (pt.plays.toFloat() / maxPlays).coerceIn(0f, 1f) else 0f
                        val barHeight = (fraction * 100).coerceAtLeast(if (pt.plays > 0) 6f else 2f).dp
                        val barColor = when {
                            isSelected -> TextPrimary
                            pt.plays > 0 -> LocalNaviifyPalette.current.accent
                            else -> SurfaceCard.copy(alpha = 0.6f)
                        }

                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(barColor)
                                .clickable { selectedPoint = pt },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Dates along X-axis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val labeledPoints = points.filter { it.displayLabel.isNotEmpty() }
                if (labeledPoints.isNotEmpty()) {
                    labeledPoints.forEach { pt ->
                        Text(
                            text = pt.displayLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                        )
                    }
                } else {
                    Text(
                        text = points.firstOrNull()?.dateStr.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                    )
                    Text(
                        text = points.lastOrNull()?.dateStr.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningHoursCard(
    hours: List<HourlyPoint>,
    peakText: String,
) {
    var selectedHour by remember { mutableStateOf<HourlyPoint?>(null) }
    val maxHourPlays = hours.maxOfOrNull { it.plays } ?: 1

    Surface(
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PEAK LISTENING HOURS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                if (selectedHour != null) {
                    Text(
                        text = "${formatHourLabel(selectedHour!!.hour)}: ${selectedHour!!.plays} plays",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LocalNaviifyPalette.current.accent,
                    )
                } else {
                    Text(
                        text = "Peak: $peakText",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 24-hour distribution bar graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ThemeOutline.copy(alpha = 0.3f)),
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    hours.forEach { hp ->
                        val isSelected = selectedHour == hp
                        val fraction = if (maxHourPlays > 0) (hp.plays.toFloat() / maxHourPlays).coerceIn(0f, 1f) else 0f
                        val barHeight = (fraction * 56).coerceAtLeast(if (hp.plays > 0) 6f else 2f).dp
                        val accent = LocalNaviifyPalette.current.accent
                        val color = when {
                            isSelected -> TextPrimary
                            hp.isPeak -> accent
                            hp.plays > 0 -> accent.copy(alpha = 0.50f)
                            else -> SurfaceCard.copy(alpha = 0.6f)
                        }

                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(color)
                                .clickable { selectedHour = hp },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Hours labels along X-axis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

private fun formatHourLabel(h: Int): String = when {
    h == 0 -> "12 AM"
    h < 12 -> "$h AM"
    h == 12 -> "12 PM"
    else -> "${h - 12} PM"
}

@Composable
private fun ArtistsBreakdownCard(genres: List<GenreShare>) {
    Surface(
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TOP ARTISTS BREAKDOWN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Multi-segment horizontal progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .weight(genre.percentage.toFloat().coerceAtLeast(1f))
                            .fillMaxSize()
                            .background(Color(genre.colorHex)),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Artist breakdown rows with percentages
            genres.forEach { genre ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(genre.colorHex)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "${genre.percentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningHistoryHeatmapCard(weeks: List<HeatmapWeek>) {
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    val accent = LocalNaviifyPalette.current.accent
    val surfaceCard = SurfaceCard
    val textPrimary = TextPrimary

    Surface(
        color = SurfaceCardHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LISTENING ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                if (selectedDay != null) {
                    Surface(
                        color = accent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "${selectedDay?.formattedDate} · ${selectedDay?.plays} plays",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else {
                    val totalPlays = weeks.sumOf { w -> w.days.filter { !it.isFuture }.sumOf { it.plays } }
                    val periodSubtitle = when (weeks.size) {
                        2 -> "2 wks"
                        5 -> "this month"
                        13 -> "3 mos"
                        else -> "${weeks.size} wks"
                    }
                    Text(
                        text = "$totalPlays plays ($periodSubtitle)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main Grid: Left day labels (Mon, Wed, Fri) aligned mathematically with the 7 day rows
            val tileSize = when {
                weeks.size <= 2 -> 24.dp
                weeks.size <= 5 -> 18.dp
                weeks.size <= 13 -> 13.dp
                else -> 11.5.dp
            }
            val tileGap = when {
                weeks.size <= 5 -> 4.dp
                else -> 3.dp
            }
            val monthHeaderHeight = 18.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Left Day of Week Labels
                Column(
                    modifier = Modifier.width(28.dp),
                    verticalArrangement = Arrangement.spacedBy(tileGap),
                ) {
                    // Spacer matching month header row height + separation
                    Spacer(Modifier.height(monthHeaderHeight + 4.dp))

                    // Row 0: Mon
                    Box(modifier = Modifier.height(tileSize), contentAlignment = Alignment.CenterStart) {
                        Text("Mon", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                    }
                    // Row 1: Tue (blank spacer)
                    Spacer(Modifier.height(tileSize))
                    // Row 2: Wed
                    Box(modifier = Modifier.height(tileSize), contentAlignment = Alignment.CenterStart) {
                        Text("Wed", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                    }
                    // Row 3: Thu (blank spacer)
                    Spacer(Modifier.height(tileSize))
                    // Row 4: Fri
                    Box(modifier = Modifier.height(tileSize), contentAlignment = Alignment.CenterStart) {
                        Text("Fri", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                    }
                    // Row 5: Sat (blank spacer)
                    Spacer(Modifier.height(tileSize))
                    // Row 6: Sun (blank spacer)
                    Spacer(Modifier.height(tileSize))
                }

                Spacer(Modifier.width(4.dp))

                // Columns of weeks: Month label strictly above each column + 7 squares below
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = if (weeks.size <= 5) Arrangement.spacedBy(tileGap * 2) else Arrangement.SpaceBetween,
                ) {
                    weeks.forEach { week ->
                        Column(
                            modifier = Modifier.width(tileSize),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Month Header for this week column - unbounded so month label is never clipped
                            Box(
                                modifier = Modifier
                                    .height(monthHeaderHeight)
                                    .wrapContentWidth(align = Alignment.Start, unbounded = true),
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                if (week.monthLabel != null) {
                                    Text(
                                        text = week.monthLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // 7 day tiles for this week
                            Column(verticalArrangement = Arrangement.spacedBy(tileGap)) {
                                week.days.forEach { day ->
                                    val isSelected = selectedDay == day
                                    val tileColor = when {
                                        day.isFuture -> Color.Transparent
                                        day.level == 1 -> accent.copy(alpha = 0.22f)
                                        day.level == 2 -> accent.copy(alpha = 0.45f)
                                        day.level == 3 -> accent.copy(alpha = 0.72f)
                                        day.level >= 4 -> accent
                                        else -> surfaceCard.copy(alpha = 0.75f)
                                    }

                                    val borderModifier = when {
                                        isSelected -> Modifier.border(BorderStroke(1.5.dp, textPrimary), RoundedCornerShape(2.5.dp))
                                        day.isToday -> Modifier.border(BorderStroke(1.dp, accent.copy(alpha = 0.8f)), RoundedCornerShape(2.5.dp))
                                        day.level == 0 -> Modifier.border(BorderStroke(0.5.dp, ThemeOutline.copy(alpha = 0.25f)), RoundedCornerShape(2.5.dp))
                                        else -> Modifier
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(tileSize)
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(tileColor)
                                            .then(borderModifier)
                                            .clickable(enabled = !day.isFuture) {
                                                selectedDay = if (selectedDay == day) null else day
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Heatmap Legend: Less [0] [1] [2] [3] [4] More
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                )
                Spacer(Modifier.width(6.dp))
                listOf(
                    surfaceCard.copy(alpha = 0.75f),
                    accent.copy(alpha = 0.22f),
                    accent.copy(alpha = 0.45f),
                    accent.copy(alpha = 0.72f),
                    accent,
                ).forEachIndexed { index, color ->
                    val border = if (index == 0) Modifier.border(BorderStroke(0.5.dp, ThemeOutline.copy(alpha = 0.25f)), RoundedCornerShape(2.dp)) else Modifier
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                            .then(border),
                    )
                    Spacer(Modifier.width(3.dp))
                }
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun RecentPlayRow(
    item: RecentPlayItem,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverArtId = item.coverArtId,
            size = 256,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val subtitle = if (!item.artist.isNullOrBlank() && !item.album.isNullOrBlank()) {
                "${item.artist} — ${item.album}"
            } else {
                item.artist ?: item.album.orEmpty()
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.relativeTime,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun TopTrackRow(
    rank: Int,
    stat: PlayStat,
    onClick: (() -> Unit)? = null,
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFE0E0E0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = rankColor,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(10.dp))
        CoverImage(
            coverArtId = stat.coverArtId,
            size = 256,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = stat.artist ?: stat.album.orEmpty()
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            color = SurfaceCardHigh,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
        ) {
            Text(
                text = "${stat.plays} plays",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
