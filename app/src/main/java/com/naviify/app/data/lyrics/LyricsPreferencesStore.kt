package com.naviify.app.data.lyrics

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ReportedTrack(
    val trackId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Persists blocked track lyrics (tracks where lyrics are rejected/wiped)
 * and reported lyrics issues (flagged for review/correction on Navidrome server).
 */
@Singleton
class LyricsPreferencesStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("asays_lyrics_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _blockedTrackIds = MutableStateFlow<Set<String>>(loadBlockedIds())
    val blockedTrackIds: StateFlow<Set<String>> = _blockedTrackIds.asStateFlow()

    private val _reportedTracks = MutableStateFlow<List<ReportedTrack>>(loadReportedTracks())
    val reportedTracks: StateFlow<List<ReportedTrack>> = _reportedTracks.asStateFlow()

    fun isBlocked(trackId: String): Boolean =
        _blockedTrackIds.value.contains(trackId)

    fun blockLyrics(trackId: String) {
        val updated = _blockedTrackIds.value + trackId
        _blockedTrackIds.value = updated
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, updated).apply()
    }

    fun unblockLyrics(trackId: String) {
        val updated = _blockedTrackIds.value - trackId
        _blockedTrackIds.value = updated
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, updated).apply()
    }

    fun clearBlocked() {
        _blockedTrackIds.value = emptySet()
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, emptySet()).apply()
    }

    fun reportTrack(
        trackId: String,
        title: String,
        artist: String?,
        album: String?,
        reason: String,
    ) {
        val current = _reportedTracks.value.filterNot { it.trackId == trackId }
        val updated = listOf(
            ReportedTrack(
                trackId = trackId,
                title = title,
                artist = artist,
                album = album,
                reason = reason,
                timestamp = System.currentTimeMillis(),
            )
        ) + current
        _reportedTracks.value = updated
        saveReportedTracks(updated)
    }

    fun removeReport(trackId: String) {
        val updated = _reportedTracks.value.filterNot { it.trackId == trackId }
        _reportedTracks.value = updated
        saveReportedTracks(updated)
    }

    fun clearReports() {
        _reportedTracks.value = emptyList()
        saveReportedTracks(emptyList())
    }

    private fun loadBlockedIds(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_IDS, emptySet())?.toSet() ?: emptySet()
    }

    private fun loadReportedTracks(): List<ReportedTrack> {
        val raw = prefs.getString(KEY_REPORTED_TRACKS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ReportedTrack>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun saveReportedTracks(tracks: List<ReportedTrack>) {
        val raw = json.encodeToString(tracks)
        prefs.edit().putString(KEY_REPORTED_TRACKS, raw).apply()
    }

    companion object {
        private const val KEY_BLOCKED_IDS = "blocked_lyrics_track_ids"
        private const val KEY_REPORTED_TRACKS = "reported_lyrics_tracks"
    }
}
