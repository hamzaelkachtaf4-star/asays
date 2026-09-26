package com.naviify.app.domain.playback

/**
 * Subsonic/Navidrome scrobble rules: report after 50% of the track duration,
 * or after 4 minutes of uninterrupted playback, whichever comes first.
 */
object ScrobblePolicy {

    private const val FOUR_MINUTES_MS = 4L * 60 * 1000

    fun shouldScrobble(positionMs: Long, durationMs: Long, playedElapsedMs: Long = positionMs): Boolean {
        if (positionMs <= 0 || durationMs <= 0) return false
        val elapsed = playedElapsedMs.coerceAtLeast(positionMs)
        val threshold = minOf(durationMs / 2, FOUR_MINUTES_MS)
        return elapsed >= threshold
    }
}
