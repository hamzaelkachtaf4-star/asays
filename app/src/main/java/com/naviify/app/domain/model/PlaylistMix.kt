package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

enum class PlaylistMixMode(
    val title: String,
    val subtitle: String,
) {
    AUTO(
        title = "Auto",
        subtitle = "Smart Spotify blend with auto-detected outro, intro and bass swap",
    ),
    FADE(
        title = "Fade",
        subtitle = "Smooth equal-power crossfade between outgoing and incoming tracks",
    ),
    RISE(
        title = "Rise",
        subtitle = "High-pass build-up sweep that swells energy into the next track's drop",
    ),
    MELT(
        title = "Melt",
        subtitle = "Warm low-pass filter blend that dissolves smoothly into the next song",
    ),
    SLAM(
        title = "Slam",
        subtitle = "Instant energetic drop on the first beat with zero silence",
    ),
}

@Immutable
data class PlaylistMixConfig(
    val isEnabled: Boolean = false,
    val mode: PlaylistMixMode = PlaylistMixMode.AUTO,
    val durationSeconds: Float = 6f, // 2s to 12s
    val smartBassSwap: Boolean = true,
    val equalPowerVolume: Boolean = true,
    val transitionOverrides: Map<String, PlaylistMixMode> = emptyMap(),
) {
    fun transitionKey(fromTrackId: String, toTrackId: String): String = "${fromTrackId}_${toTrackId}"

    fun transitionModeFor(fromTrackId: String?, toTrackId: String?): PlaylistMixMode {
        if (fromTrackId != null && toTrackId != null) {
            val key = transitionKey(fromTrackId, toTrackId)
            transitionOverrides[key]?.let { return it }
        }
        return mode
    }

    fun hasCustomOverride(fromTrackId: String?, toTrackId: String?): Boolean {
        if (fromTrackId == null || toTrackId == null) return false
        return transitionOverrides.containsKey(transitionKey(fromTrackId, toTrackId))
    }

    fun withOverride(fromTrackId: String, toTrackId: String, overrideMode: PlaylistMixMode): PlaylistMixConfig {
        val updated = transitionOverrides.toMutableMap()
        updated[transitionKey(fromTrackId, toTrackId)] = overrideMode
        return copy(transitionOverrides = updated)
    }

    fun withoutOverride(fromTrackId: String, toTrackId: String): PlaylistMixConfig {
        val updated = transitionOverrides.toMutableMap()
        updated.remove(transitionKey(fromTrackId, toTrackId))
        return copy(transitionOverrides = updated)
    }

    fun withoutAllOverrides(): PlaylistMixConfig {
        return copy(transitionOverrides = emptyMap())
    }
}

data class CamelotKey(
    val code: String,
    val colorHex: Long,
    val num: Int = 1,
    val letter: String = "A",
)

enum class HarmonicRelationship(
    val label: String,
    val description: String,
    val colorHex: Long,
) {
    PERFECT_MATCH("Perfect Key Match", "Identical key and scale for a flawless, smooth blend", 0xFF1DB954),
    HARMONIC_LIFT("Harmonic Lift (+1)", "Step up the Camelot wheel for a natural energy lift", 0xFF64B5F6),
    HARMONIC_WARM("Harmonic Warmth (-1)", "Step down the Camelot wheel for a deeper, warmer vibe", 0xFFFFB74D),
    RELATIVE_SHIFT("Relative Shift", "Seamless switch between minor mood and major brightness", 0xFFBA68C8),
    ENERGY_BOOST("Energy Boost (+2)", "Two-step leap around the wheel for an intense vibe jump", 0xFFFF8A65),
    MODULATION("Key Contrast", "Distinct harmonic change that adds flavor and contrast", 0xFF90A4AE),
}

fun analyzeHarmonicRelationship(keyA: CamelotKey, keyB: CamelotKey): HarmonicRelationship {
    if (keyA.num == keyB.num && keyA.letter == keyB.letter) {
        return HarmonicRelationship.PERFECT_MATCH
    }
    if (keyA.num == keyB.num && keyA.letter != keyB.letter) {
        return HarmonicRelationship.RELATIVE_SHIFT
    }
    if (keyA.letter == keyB.letter) {
        val step = (keyB.num - keyA.num + 12) % 12
        return when (step) {
            1 -> HarmonicRelationship.HARMONIC_LIFT
            11 -> HarmonicRelationship.HARMONIC_WARM
            2 -> HarmonicRelationship.ENERGY_BOOST
            else -> HarmonicRelationship.MODULATION
        }
    }
    return HarmonicRelationship.MODULATION
}

fun getDjBpm(trackId: String, title: String): Int {
    val hash = kotlin.math.abs(trackId.hashCode() xor title.hashCode())
    return 84 + (hash % 45) // range 84..128 bpm
}

fun getCamelotKey(trackId: String, artist: String?): CamelotKey {
    val hash = kotlin.math.abs(trackId.hashCode() * 31 + (artist?.hashCode() ?: 0))
    val num = (hash % 12) + 1
    val isMinor = (hash % 3) != 0
    val letter = if (isMinor) "A" else "B"
    val code = "$num$letter"

    // Spotify pastel badge colors matching real DJ Mix UI
    val colorHex = when (num) {
        1 -> 0xFF4DD0E1 // Cyan / Teal (1A/1B)
        2 -> 0xFF26A69A // Teal
        3 -> 0xFFFFD54F // Yellow
        4 -> 0xFFFFB74D // Gold / Tan / Orange (4A)
        5 -> 0xFFFF8A65 // Light Orange
        6 -> 0xFFE57373 // Coral Red
        7 -> 0xFFF06292 // Rose
        8 -> 0xFFBA68C8 // Pink / Magenta (8A)
        9 -> 0xFF9575CD // Purple
        10 -> 0xFF7986CB // Lavender / Blue (10A)
        11 -> 0xFF64B5F6 // Sky Blue
        12 -> 0xFF81C784 // Emerald Green
        else -> 0xFFFFB74D
    }
    return CamelotKey(code = code, colorHex = colorHex, num = num, letter = letter)
}

fun sortTracksHarmonically(tracks: List<Track>): List<Track> {
    if (tracks.size <= 2) return tracks
    val pool = tracks.toMutableList()
    val result = mutableListOf<Track>()

    // Start with the lowest BPM track as anchor
    pool.sortBy { getDjBpm(it.id, it.title) }
    var current = pool.removeAt(0)
    result.add(current)

    while (pool.isNotEmpty()) {
        val currentKey = getCamelotKey(current.id, current.artist)
        val currentBpm = getDjBpm(current.id, current.title)

        var bestIndex = 0
        var minScore = Double.MAX_VALUE

        for (i in pool.indices) {
            val candidate = pool[i]
            val candKey = getCamelotKey(candidate.id, candidate.artist)
            val candBpm = getDjBpm(candidate.id, candidate.title)

            // Camelot step distance
            val semitoneDist = (candKey.num - currentKey.num + 12) % 12
            val camelotStep = kotlin.math.min(semitoneDist, (12 - semitoneDist) % 12)
            val modePenalty = if (candKey.letter != currentKey.letter) 1.5 else 0.0
            val bpmDiff = kotlin.math.abs(candBpm - currentBpm)

            val score = (camelotStep * 15.0) + (modePenalty * 10.0) + bpmDiff
            if (score < minScore) {
                minScore = score
                bestIndex = i
            }
        }

        current = pool.removeAt(bestIndex)
        result.add(current)
    }

    return result
}


