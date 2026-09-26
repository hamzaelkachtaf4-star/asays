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
    // Reglages MANUELS (in/out points), l'equivalent du "deplace les morceaux"
    // de Spotify : ou le sortant quitte et ou l'entrant demarre.
    // outroOffsetMs <= 0 : le sortant commence a quitter plus tot (0 = fin naturelle).
    // introSkipMs   >= 0 : l'entrant demarre apres son intro (0 = depuis le debut).
    val outroOffsetMs: Long = 0L,
    val introSkipMs: Long = 0L,
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

    /**
     * Debut reel du blend dans le morceau sortant : fin naturelle (duree - recouvrement)
     * decalee par le reglage manuel, bornee pour rester jouable. Utilise par
     * PlaybackController ET par l'apercu de l'UI pour ne jamais diverger.
     */
    fun transitionStartMs(durationMs: Long, overlapMs: Long): Long {
        val natural = durationMs - overlapMs
        val shift = outroOffsetMs.coerceIn(-MAX_OUTRO_SHIFT_MS, 0L)
        return (natural + shift).coerceAtLeast(1_000L)
    }

    companion object {
        const val MAX_OUTRO_SHIFT_MS = 30_000L
        const val MAX_INTRO_SKIP_MS = 30_000L
    }
}

/**
 * Gains (sortant, entrant) de la transition a la position [progress] (0..1).
 *
 * Source unique de verite : consomme a la fois par PlaybackController (le gain
 * reellement applique a l'audio) et par l'apercu de courbe affiche dans l'UI,
 * pour que le dessin ne puisse jamais mentir sur ce qui est joue.
 */
fun mixGains(
    progress: Float,
    mode: PlaylistMixMode,
    equalPower: Boolean,
): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    return when (mode) {
        PlaylistMixMode.AUTO, PlaylistMixMode.FADE ->
            if (equalPower) {
                Pair(
                    kotlin.math.cos(p * Math.PI / 2.0).toFloat(),
                    kotlin.math.sin(p * Math.PI / 2.0).toFloat(),
                )
            } else {
                Pair(1f - p, p)
            }

        PlaylistMixMode.RISE -> Pair(
            if (p < 0.75f) 1f else ((1f - p) / 0.25f),
            (p * p).coerceIn(0f, 1f),
        )

        PlaylistMixMode.MELT -> Pair(
            (1f - p) * (1f - p),
            kotlin.math.sqrt(p.toDouble()).toFloat(),
        )

        PlaylistMixMode.SLAM -> Pair(0f, 1f)
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

// Les anciens getDjBpm() / getCamelotKey() (un hash transforme en nombre plausible :
// 84 + hash % 45, et une cle tiree du hash de l'artiste) sont SUPPRIMES.
// Les valeurs reelles viennent de l'analyseur serveur (DjTrackMeta, cf.
// DjMetadata.kt) et du tag TBPM remonte par l'API Subsonic : voir realBpmOf()
// et realCamelotOf(). Un morceau non analyse n'affiche simplement rien.

/**
 * Tri harmonique base sur les valeurs MESUREES (analyseur serveur) : on part du
 * morceau le plus lent puis on enchaine les cles Camelot les plus proches en
 * tenant compte de l'ecart de BPM.
 *
 * Les morceaux sans metadonnees ne sont plus tries sur un hash : ils sont places
 * a la fin, dans leur ordre d'origine. Si aucun morceau n'est analyse, la liste
 * est renvoyee telle quelle.
 */
fun sortTracksHarmonically(
    tracks: List<Track>,
    meta: Map<String, DjTrackMeta> = emptyMap(),
): List<Track> {
    if (tracks.size <= 2) return tracks
    val analysed = tracks.filter { realBpmOf(it, meta) != null && realCamelotOf(it, meta) != null }
    val untouched = tracks.filterNot { it in analysed }
    if (analysed.isEmpty()) return tracks

    val pool = analysed.sortedBy { realBpmOf(it, meta) ?: 0 }.toMutableList()
    val result = mutableListOf<Track>()
    var current = pool.removeAt(0)
    result.add(current)

    while (pool.isNotEmpty()) {
        val currentKey = realCamelotOf(current, meta) ?: break
        val currentBpm = realBpmOf(current, meta) ?: break

        var bestIndex = 0
        var minScore = Double.MAX_VALUE

        for (i in pool.indices) {
            val candidate = pool[i]
            val candKey = realCamelotOf(candidate, meta) ?: continue
            val candBpm = realBpmOf(candidate, meta) ?: continue

            // Distance sur la roue Camelot
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

    result.addAll(untouched)
    return result
}


