package com.naviify.app.ui.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.data.stats.ArtistStat
import com.naviify.app.data.stats.ListeningStatsStore
import com.naviify.app.data.stats.PlayStat
import com.naviify.app.data.stats.StatsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class StatsPeriod(val label: String, val days: Int) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
    ALL("All", Int.MAX_VALUE),
}

@Immutable
data class DailyActivityPoint(
    val dateStr: String,
    val displayLabel: String,
    val plays: Int,
    val isToday: Boolean,
)

@Immutable
data class HourlyPoint(
    val hour: Int,
    val plays: Int,
    val isPeak: Boolean,
)

@Immutable
data class GenreShare(
    val name: String,
    val percentage: Int,
    val colorHex: Long,
)

@Immutable
data class HeatmapDay(
    val dateStr: String,
    val formattedDate: String,
    val dayOfWeek: Int, // 0 = Mon .. 6 = Sun
    val plays: Int,
    val level: Int, // -1 = future, 0 = 0 plays, 1 = 1-2, 2 = 3-5, 3 = 6-9, 4 = 10+
    val isToday: Boolean = false,
    val isFuture: Boolean = false,
)

@Immutable
data class HeatmapWeek(
    val weekIndex: Int,
    val monthLabel: String?,
    val days: List<HeatmapDay>,
)

@Immutable
data class RecentPlayItem(
    val trackId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val coverArtId: String?,
    val relativeTime: String,
    val plays: Long,
    val lastPlayedAt: Long,
)

@Immutable
data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.THIRTY_DAYS,
    val totalPlays: Long = 0,
    val listeningTimeFormatted: String = "0m",
    val uniqueArtists: Int = 0,
    val streakDays: Long = 0,
    val bestStreakDays: Long = 0,
    val dailyActivity: List<DailyActivityPoint> = emptyList(),
    val maxDailyPlays: Int = 1,
    val hourlyActivity: List<HourlyPoint> = emptyList(),
    val peakHourText: String = "12:00 PM",
    val topGenres: List<GenreShare> = emptyList(),
    val heatmapWeeks: List<HeatmapWeek> = emptyList(),
    val recentPlays: List<RecentPlayItem> = emptyList(),
    val topTracks: List<PlayStat> = emptyList(),
    val topArtists: List<ArtistStat> = emptyList(),
    // Backward compatibility helpers
    val totalHours: Long = 0,
    val totalMinutes: Long = 0,
    val totalSeconds: Long = 0,
    val plays: Long = 0,
    val streak: Long = 0,
    val recent: List<PlayStat> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    listeningStatsStore: ListeningStatsStore,
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.THIRTY_DAYS)

    val uiState: StateFlow<StatsUiState> = combine(
        listeningStatsStore.stats,
        _selectedPeriod,
    ) { snapshot, period ->
        computeStats(snapshot, period)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())

    fun selectPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    private fun computeStats(snapshot: StatsSnapshot, period: StatsPeriod): StatsUiState {
        val today = LocalDate.now()
        val numDays = if (period.days == Int.MAX_VALUE) 30 else period.days

        // Build daily activity points
        val dailyPoints = mutableListOf<DailyActivityPoint>()
        val startDay = today.minusDays((numDays - 1).toLong())
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

        for (i in 0 until numDays) {
            val date = startDay.plusDays(i.toLong())
            val dateKey = date.toString()
            val plays = snapshot.dailyPlays[dateKey] ?: 0
            val showLabel = when (period) {
                StatsPeriod.SEVEN_DAYS -> true
                StatsPeriod.THIRTY_DAYS -> (i % 4 == 0) || (i == numDays - 1)
                StatsPeriod.NINETY_DAYS -> (i % 15 == 0) || (i == numDays - 1)
                StatsPeriod.ALL -> (i % 5 == 0) || (i == numDays - 1)
            }
            dailyPoints += DailyActivityPoint(
                dateStr = dateKey,
                displayLabel = if (showLabel) date.format(dateFormatter) else "",
                plays = plays,
                isToday = (date == today),
            )
        }

        val maxDaily = maxOf(dailyPoints.maxOfOrNull { it.plays } ?: 1, 1)

        // Metrics calculations
        val periodPlays = if (period == StatsPeriod.ALL) {
            snapshot.plays
        } else {
            val sum = dailyPoints.sumOf { it.plays.toLong() }
            if (sum > 0) sum else snapshot.plays
        }

        val periodListeningMs = if (snapshot.plays > 0 && period != StatsPeriod.ALL) {
            (snapshot.listeningMs * periodPlays) / snapshot.plays
        } else {
            snapshot.listeningMs
        }

        val listeningTimeFormatted = formatListeningDuration(periodListeningMs)

        // Unique artists
        val uniqueArtistsCount = if (snapshot.artists.isNotEmpty()) {
            snapshot.artists.size
        } else {
            snapshot.tracks.mapNotNull { it.artist }.distinct().size
        }

        // Hourly distribution & peak hour
        val hourlyList = mutableListOf<HourlyPoint>()
        var peakHour = 12
        var maxHourPlays = -1
        for (h in 0..23) {
            val plays = snapshot.hourlyPlays[h.toString()] ?: 0
            if (plays > maxHourPlays) {
                maxHourPlays = plays
                peakHour = h
            }
        }
        for (h in 0..23) {
            val plays = snapshot.hourlyPlays[h.toString()] ?: 0
            hourlyList += HourlyPoint(
                hour = h,
                plays = plays,
                isPeak = (h == peakHour && maxHourPlays > 0),
            )
        }
        val peakHourText = formatHourToAmPm(peakHour)

        // Genres / Artists breakdown
        val topGenres = computeTopGenres(snapshot)

        // Heatmap: adapts to selected period (2 wks for 7D, 5 wks for 30D, 13 wks for 90D, 16 wks for ALL)
        val heatmapWeeks = computeHeatmap(snapshot, today, period)

        // Recent plays
        val now = System.currentTimeMillis()
        val recentPlayItems = snapshot.history.take(30).map { stat ->
            RecentPlayItem(
                trackId = stat.trackId,
                title = stat.title,
                artist = stat.artist,
                album = stat.album,
                coverArtId = stat.coverArtId,
                relativeTime = formatRelativeTime(stat.lastPlayedAt, now),
                plays = stat.plays,
                lastPlayedAt = stat.lastPlayedAt,
            )
        }

        return StatsUiState(
            selectedPeriod = period,
            totalPlays = periodPlays,
            listeningTimeFormatted = listeningTimeFormatted,
            uniqueArtists = uniqueArtistsCount,
            streakDays = snapshot.streak,
            bestStreakDays = snapshot.bestStreak,
            dailyActivity = dailyPoints,
            maxDailyPlays = maxDaily,
            hourlyActivity = hourlyList,
            peakHourText = peakHourText,
            topGenres = topGenres,
            heatmapWeeks = heatmapWeeks,
            recentPlays = recentPlayItems,
            topTracks = snapshot.tracks.sortedByDescending { it.plays }.take(20),
            topArtists = snapshot.artists.sortedByDescending { it.plays }.take(20),
            totalHours = snapshot.listeningMs / 3_600_000,
            totalMinutes = (snapshot.listeningMs / 60_000) % 60,
            totalSeconds = (snapshot.listeningMs / 1000) % 60,
            plays = snapshot.plays,
            streak = snapshot.streak,
            recent = snapshot.history.take(30),
        )
    }

    private fun computeTopGenres(snapshot: StatsSnapshot): List<GenreShare> {
        val totalPlays = snapshot.artists.sumOf { it.plays }
        if (totalPlays <= 0) {
            return emptyList()
        }
        val colors = listOf(0xFF1DB954, 0xFF4D79FF, 0xFFFFA500, 0xFFE040FB, 0xFFFF5252)
        val top = snapshot.artists.sortedByDescending { it.plays }.take(5)
        return top.mapIndexed { index, artist ->
            val pct = ((artist.plays.toFloat() / totalPlays) * 100).toInt().coerceAtLeast(1)
            GenreShare(
                name = artist.name,
                percentage = pct,
                colorHex = colors[index % colors.size],
            )
        }
    }

    private fun computeHeatmap(
        snapshot: StatsSnapshot,
        today: LocalDate,
        period: StatsPeriod,
    ): List<HeatmapWeek> {
        val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val numWeeks = when (period) {
            StatsPeriod.SEVEN_DAYS -> 2
            StatsPeriod.THIRTY_DAYS -> 5
            StatsPeriod.NINETY_DAYS -> 13
            StatsPeriod.ALL -> 16
        }
        val startMonday = currentMonday.minusWeeks((numWeeks - 1).toLong())
        val monthFormatter = DateTimeFormatter.ofPattern("MMM", java.util.Locale.US)
        val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.US)
        val weeks = mutableListOf<HeatmapWeek>()

        var prevMonth: String? = null
        for (w in 0 until numWeeks) {
            val weekMonday = startMonday.plusWeeks(w.toLong())
            val currentMonth = weekMonday.format(monthFormatter)
            val monthLabel = if (currentMonth != prevMonth) {
                prevMonth = currentMonth
                currentMonth
            } else {
                null
            }

            val days = mutableListOf<HeatmapDay>()
            for (d in 0..6) {
                val date = weekMonday.plusDays(d.toLong())
                val key = date.toString()
                val plays = snapshot.dailyPlays[key] ?: 0
                val isFuture = date.isAfter(today)
                val isToday = (date == today)
                val level = when {
                    isFuture -> -1
                    plays <= 0 -> 0
                    plays in 1..2 -> 1
                    plays in 3..5 -> 2
                    plays in 6..9 -> 3
                    else -> 4
                }
                days += HeatmapDay(
                    dateStr = key,
                    formattedDate = date.format(dayFormatter),
                    dayOfWeek = d,
                    plays = plays,
                    level = level,
                    isToday = isToday,
                    isFuture = isFuture,
                )
            }
            weeks += HeatmapWeek(weekIndex = w, monthLabel = monthLabel, days = days)
        }
        return weeks
    }

    private fun formatHourPeak(hour: Int): String = when {
        hour == 0 -> "12:00 AM"
        hour < 12 -> "$hour:00 AM"
        hour == 12 -> "12:00 PM"
        else -> "${hour - 12}:00 PM"
    }

    private fun formatHourToAmPm(hour: Int): String = when {
        hour == 0 -> "12:00 AM"
        hour < 12 -> "$hour:00 AM"
        hour == 12 -> "12:00 PM"
        else -> "${hour - 12}:00 PM"
    }

    private fun formatListeningDuration(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "1m"
        }
    }

    private fun formatRelativeTime(timestamp: Long, now: Long): String {
        if (timestamp <= 0) return "Recently"
        val diff = (now - timestamp).coerceAtLeast(0)
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        val weeks = diff / (7 * 86_400_000)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            weeks < 4 -> "${weeks}w ago"
            else -> "${diff / (30 * 86_400_000)}mo ago"
        }
    }
}
