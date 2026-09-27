package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Forme d'onde reelle d'un morceau, telle que servie par le serveur maison
 * (`http://<serveur>:8788/waveform/<id>.json`).
 *
 * [peaks] est l'amplitude crete par tranche (0..1000) et [low] la meme chose pour
 * les basses seules : deux series, donc deux couleurs, exactement comme la maquette.
 * Ces valeurs sont mesurees en decodant l'audio : l'app n'en invente aucune, et
 * quand le serveur ne repond pas on ne dessine rien plutot que de dessiner faux.
 */
@Immutable
data class MixWaveform(
    val songId: String,
    val durationMs: Int,
    val peaks: List<Int>,
    val low: List<Int>,
) {
    val isEmpty: Boolean get() = peaks.isEmpty()
}
