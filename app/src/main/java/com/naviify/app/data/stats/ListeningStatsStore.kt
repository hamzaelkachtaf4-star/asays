package com.naviify.app.data.stats

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naviify.app.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.statsDataStore: DataStore<Preferences> by preferencesDataStore(name = "naviify_stats")

@Serializable
data class PlayStat(
    val trackId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val coverArtId: String? = null,
    val plays: Long = 0L,
    val lastPlayedAt: Long = 0L,
)

@Serializable
data class ArtistStat(
    val artistId: String? = null,
    val name: String,
    val plays: Long = 0L,
    val lastPlayedAt: Long = 0L,
)

@Serializable
data class StatsSnapshot(
    val plays: Long = 0L,
    val listeningMs: Long = 0L,
    val lastDay: Long = 0L,
    val streak: Long = 0L,
    val bestStreak: Long = 0L,
    val tracks: List<PlayStat> = emptyList(),
    val artists: List<ArtistStat> = emptyList(),
    val history: List<PlayStat> = emptyList(),
    val dailyPlays: Map<String, Int> = emptyMap(),
    val hourlyPlays: Map<String, Int> = emptyMap(),
    val genrePlays: Map<String, Int> = emptyMap(),
)

@Singleton
class ListeningStatsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _stats = MutableStateFlow(StatsSnapshot())
    val stats: StateFlow<StatsSnapshot> = _stats.asStateFlow()

    init {
        scope.launch {
            val raw = context.statsDataStore.data.first()[Keys.JSON]
            if (raw != null) {
                runCatching { json.decodeFromString<StatsSnapshot>(raw) }
                    .onSuccess { loaded ->
                        var snapshot = loaded
                        if (snapshot.dailyPlays.isEmpty() && snapshot.history.isNotEmpty()) {
                            val backfilledDaily = mutableMapOf<String, Int>()
                            val backfilledHourly = mutableMapOf<String, Int>()
                            for (item in snapshot.history) {
                                if (item.lastPlayedAt > 0) {
                                    val date = java.time.Instant.ofEpochMilli(item.lastPlayedAt)
                                        .atZone(java.time.ZoneId.systemDefault())
                                    val dStr = date.toLocalDate().toString()
                                    val hStr = date.toLocalTime().hour.toString()
                                    backfilledDaily[dStr] = (backfilledDaily[dStr] ?: 0) + 1
                                    backfilledHourly[hStr] = (backfilledHourly[hStr] ?: 0) + 1
                                }
                            }
                            snapshot = snapshot.copy(
                                dailyPlays = backfilledDaily,
                                hourlyPlays = backfilledHourly,
                            )
                        }
                        _stats.value = snapshot
                    }
            }
        }
    }

    /** Called once per completed playback (scrobble threshold). */
    fun recordPlayback(listenedSeconds: Int, track: Track?) {
        scope.launch {
            val todayDate = LocalDate.now()
            val today = todayDate.toEpochDay()
            val todayStr = todayDate.toString()
            val hourStr = java.time.LocalTime.now().hour.toString()
            val now = System.currentTimeMillis()
            val current = _stats.value
            val newStreak = when {
                current.lastDay == today -> if (current.streak > 0) current.streak else 1L
                current.lastDay == today - 1 -> current.streak + 1
                else -> 1L
            }

            val hasTrackInfo = track != null && !track.id.isNullOrBlank()
            val updatedTracks = if (hasTrackInfo) {
                upsertPlay(current.tracks, callPlayStat(track, now))
            } else {
                current.tracks
            }
            val updatedArtists = track?.artist?.takeIf { it.isNotBlank() }
                ?.let { name -> upsertArtist(current.artists, ArtistStat(artistId = track.artistId, name = name, plays = 0L, lastPlayedAt = now)) }
                ?: current.artists
            val updatedHistory = if (hasTrackInfo) {
                (listOf(callPlayStat(track, now)) + current.history)
                    .take(100)
            } else {
                current.history
            }

            val updatedDaily = current.dailyPlays.toMutableMap()
            updatedDaily[todayStr] = (updatedDaily[todayStr] ?: 0) + 1

            val updatedHourly = current.hourlyPlays.toMutableMap()
            updatedHourly[hourStr] = (updatedHourly[hourStr] ?: 0) + 1

            val updated = StatsSnapshot(
                plays = current.plays + 1,
                listeningMs = current.listeningMs + listenedSeconds.coerceAtLeast(0) * 1000L,
                lastDay = today,
                streak = newStreak,
                bestStreak = maxOf(newStreak, current.bestStreak),
                tracks = updatedTracks,
                artists = updatedArtists,
                history = updatedHistory,
                dailyPlays = updatedDaily,
                hourlyPlays = updatedHourly,
                genrePlays = current.genrePlays,
            )
            _stats.value = updated
            context.statsDataStore.edit { it[Keys.JSON] = json.encodeToString(StatsSnapshot.serializer(), updated) }
        }
    }

    private fun callPlayStat(track: Track?, now: Long): PlayStat = PlayStat(
        trackId = track?.id.orEmpty(),
        title = track?.title.orEmpty(),
        artist = track?.artist,
        album = track?.album,
        albumId = track?.albumId,
        coverArtId = track?.coverArtId,
        plays = 0L,
        lastPlayedAt = now,
    )

    private fun upsertPlay(list: List<PlayStat>, stat: PlayStat): List<PlayStat> {
        if (stat.trackId.isBlank()) return list
        val existing = list.firstOrNull { it.trackId == stat.trackId }
        return if (existing != null) {
            list.map {
                if (it.trackId == stat.trackId) it.copy(plays = it.plays + 1, lastPlayedAt = stat.lastPlayedAt) else it
            }
        } else {
            list + stat.copy(plays = 1)
        }
    }

    private fun upsertArtist(list: List<ArtistStat>, stat: ArtistStat): List<ArtistStat> {
        val existing = list.firstOrNull { it.artistId == stat.artistId && it.name == stat.name } ?: list.firstOrNull { it.name == stat.name }
        return if (existing != null) {
            list.map {
                if (it.name == stat.name) it.copy(plays = it.plays + 1, lastPlayedAt = stat.lastPlayedAt) else it
            }
        } else {
            list + stat.copy(plays = 1)
        }
    }

    private object Keys {
        val JSON = stringPreferencesKey("stats")
    }
}
