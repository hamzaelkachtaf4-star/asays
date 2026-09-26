package com.naviify.app.data.stats

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records when each playlist was last opened for playback.
 *
 * Navidrome exposes `created`/`changed` on playlists but no "last played"
 * timestamp, so the Recently Played sort has to be driven by the client. A
 * SharedPreferences map is enough here: it is a handful of ids, read on demand
 * when the library list is built, and written once per playback start.
 */
@Singleton
class PlaylistHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Epoch millis of the last playback start, keyed by playlist id. */
    fun lastPlayedAt(playlistId: String): Long = prefs.getLong(key(playlistId), 0L)

    fun lastPlayedMap(): Map<String, Long> =
        prefs.all.mapNotNull { (key, value) ->
            val id = key.removePrefix(KEY_PREFIX).takeIf { it != key } ?: return@mapNotNull null
            val millis = (value as? Long) ?: return@mapNotNull null
            id to millis
        }.toMap()

    fun recordPlayed(playlistId: String) {
        if (playlistId.isBlank()) return
        prefs.edit().putLong(key(playlistId), System.currentTimeMillis()).apply()
    }

    fun clear(playlistId: String) {
        prefs.edit().remove(key(playlistId)).apply()
    }

    private fun key(playlistId: String) = KEY_PREFIX + playlistId

    private companion object {
        const val PREFS = "naviify_playlist_history"
        const val KEY_PREFIX = "played_"
    }
}
