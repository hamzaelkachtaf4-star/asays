package com.naviify.app.data.lyrics

import com.naviify.app.domain.model.LyricsLineData

private val TIMED_LINE = Regex("""\[(\d{1,3}):(\d{1,2}(?:[.:]\d{1,3})?)\]""")

/**
 * Parses legacy Subsonic lyrics: lines may be prefixed with `[mm:ss]` or
 * `[mm:ss.xx]` timestamps; untimed lines are kept as plain text lines.
 */
fun parseLegacyLyrics(text: String): List<LyricsLineData> {
    return text.lineSequence()
        .flatMap { line ->
            val trimmed = line.trim()
            // A released .lrc file may carry several timestamps for one line
            // ("[00:12.00][00:45.30] chorus"), so every leading timestamp is
            // consumed and each yields its own cue.
            val timestamps = mutableListOf<Long>()
            var rest = trimmed
            while (true) {
                val match = TIMED_LINE.find(rest) ?: break
                if (match.range.first != 0) break
                val minutes = match.groupValues[1].toLongOrNull()
                val seconds = match.groupValues[2].replace(':', '.').toDoubleOrNull()
                if (minutes == null || seconds == null) break
                timestamps += minutes * 60_000 + (seconds * 1000).toLong()
                rest = rest.substring(match.range.last + 1)
            }
            val content = rest.trim()
            if (timestamps.isEmpty()) {
                sequenceOf(LyricsLineData(text = trimmed))
            } else {
                timestamps.asSequence().map { LyricsLineData(startMs = it, text = content) }
            }
        }
        .filter { it.text.isNotBlank() }
        .toList()
}
