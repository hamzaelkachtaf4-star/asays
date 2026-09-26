package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class LyricsLineData(
    val startMs: Long? = null,
    val text: String,
)

@Serializable
@Immutable
data class LyricsData(
    val syncedLines: List<LyricsLineData> = emptyList(),
    val plainText: String? = null,
    val offsetMs: Long = 0L,
    val isCustom: Boolean = false,
) {
    val isEmpty: Boolean
        get() = syncedLines.isEmpty() && plainText.isNullOrBlank()

    val hasLyrics: Boolean
        get() = !isEmpty

    val isSynced: Boolean
        get() = syncedLines.any { it.startMs != null }

    /**
     * Serializes this [LyricsData] into standard `.lrc` format suitable for Navidrome sidecar files.
     * Incorporates [offsetMs] into each synced line's timestamp so the resulting file is calibrated.
     */
    fun toLrcString(title: String? = null, artist: String? = null): String {
        if (syncedLines.isEmpty()) {
            return plainText.orEmpty()
        }
        val sb = StringBuilder()
        if (!title.isNullOrBlank()) {
            sb.append("[ti:").append(title.trim()).append("]\n")
        }
        if (!artist.isNullOrBlank()) {
            sb.append("[ar:").append(artist.trim()).append("]\n")
        }
        for (line in syncedLines) {
            val rawMs = line.startMs
            if (rawMs != null) {
                val adjustedMs = maxOf(0L, rawMs + offsetMs)
                val minutes = adjustedMs / 60000
                val seconds = (adjustedMs % 60000) / 1000
                val hundredths = (adjustedMs % 1000) / 10
                sb.append(String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s\n", minutes, seconds, hundredths, line.text))
            } else {
                sb.append(line.text).append("\n")
            }
        }
        return sb.toString().trimEnd()
    }
}

