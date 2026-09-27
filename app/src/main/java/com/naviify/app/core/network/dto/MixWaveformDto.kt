package com.naviify.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Reponse de `GET /waveform/<id>.json` sur le serveur DJ (port 8788).
 *
 * Le serveur renvoie aussi `path` et `points` : ils ne sont pas necessaires ici,
 * la lecture se fait avec `ignoreUnknownKeys` active.
 */
@Serializable
data class MixWaveformDto(
    val songId: String = "",
    val durationMs: Int = 0,
    val peaks: List<Int> = emptyList(),
    val low: List<Int> = emptyList(),
)
