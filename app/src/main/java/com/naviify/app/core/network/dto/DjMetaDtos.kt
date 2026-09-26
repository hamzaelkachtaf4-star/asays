package com.naviify.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Payload de l'endpoint `djmeta.json` servi par l'analyseur DJ du serveur maison
 * (voir `/home/tayeb/dj-analyzer/README.md`). Cle = pid Navidrome (le meme id que
 * `Child.id` dans l'API Subsonic), donc l'app retrouve ses morceaux directement.
 */
@Serializable
data class DjMetaPayload(
    val count: Int = 0,
    @SerialName("generated_at") val generatedAt: String? = null,
    val source: String? = null,
    val tracks: Map<String, DjTrackMetaDto> = emptyMap(),
)

@Serializable
data class DjTrackMetaDto(
    val bpm: Double? = null,
    val key: String? = null,
    val camelot: String? = null,
    val bpmConfidence: Double? = null,
    val keyConfidence: Double? = null,
    val path: String? = null,
)
