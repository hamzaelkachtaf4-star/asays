package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Metadonnees DJ reellement mesurees pour un morceau, produites par l'analyse
 * offline du serveur maison (`/home/tayeb/dj-analyzer`, essentia
 * RhythmExtractor2013 + KeyExtractor EDMA) et servies sur
 * `http://<serveur>:8788/djmeta.json`.
 *
 * Aucune de ces valeurs n'est devinee : un morceau absent de la carte n'a
 * simplement pas encore ete analyse, et l'UI doit alors ne rien afficher au lieu
 * d'inventer un chiffre (l'ancien `getDjBpm()` tirait 84 + hash % 45).
 */
@Immutable
data class DjTrackMeta(
    val bpm: Double? = null,
    val key: String? = null,
    val camelot: String? = null,
    val bpmConfidence: Double? = null,
    val keyConfidence: Double? = null,
) {
    /** BPM arrondi pour l'affichage (essentia renvoie des decimales : 102.4). */
    val bpmRounded: Int? get() = bpm?.let { kotlin.math.round(it).toInt() }

    /** Cle Camelot prete pour le badge, ou null si le code est absent/invalide. */
    val camelotKey: CamelotKey? get() = camelot?.let { camelotFromCode(it) }
}

/** "8A" -> CamelotKey(num = 8, letter = A). null si le code est invalide. */
fun camelotFromCode(code: String): CamelotKey? {
    val match = Regex("^([0-9]{1,2})([AB])$").find(code.trim().uppercase()) ?: return null
    val num = match.groupValues[1].toIntOrNull() ?: return null
    if (num !in 1..12) return null
    val letter = match.groupValues[2]
    return CamelotKey(code = "$num$letter", colorHex = camelotColorHex(num), num = num, letter = letter)
}

/** Palette des badges Camelot (indexee sur un vrai code, plus sur un hash). */
fun camelotColorHex(num: Int): Long = when (num) {
    1 -> 0xFF4DD0E1
    2 -> 0xFF26A69A
    3 -> 0xFFFFD54F
    4 -> 0xFFFFB74D
    5 -> 0xFFFF8A65
    6 -> 0xFFE57373
    7 -> 0xFFF06292
    8 -> 0xFFBA68C8
    9 -> 0xFF9575CD
    10 -> 0xFF7986CB
    11 -> 0xFF64B5F6
    12 -> 0xFF81C784
    else -> 0xFFFFB74D
}

/**
 * BPM reel d'un morceau : analyse serveur (djmeta) sinon tag TBPM remonte par
 * l'API Subsonic. null = aucune valeur mesuree.
 */
fun realBpmOf(track: Track, meta: Map<String, DjTrackMeta>): Int? =
    meta[track.id]?.bpmRounded ?: track.bpm

/** Cle Camelot reelle d'un morceau. null = morceau non analyse. */
fun realCamelotOf(track: Track, meta: Map<String, DjTrackMeta>): CamelotKey? =
    meta[track.id]?.camelotKey
