package com.naviify.app.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Payload de `http://<hote>:8788/radio.json` : les stations "radio" du jour,
 * generees cote serveur (~/scripts/radio_stations.py) et renouvelees chaque jour.
 *
 * Le serveur envoie deja tout ce qu'il faut pour afficher ET jouer les titres
 * (id, titre, artiste, album, album_id, duree, bpm, tonalite) : aucun appel
 * Subsonic supplementaire n'est necessaire.
 */
@Serializable
data class RadioPayload(
    val generated: String = "",
    val seed: String = "",
    val stations: List<RadioStationDto> = emptyList(),
)

@Serializable
data class RadioStationDto(
    val name: String = "",
    val accent: String = "",
    val count: Int = 0,
    val tracks: List<RadioTrackDto> = emptyList(),
)

@Serializable
data class RadioTrackDto(
    val id: String = "",
    val t: String = "",
    val a: String = "",
    val al: String = "",
    val aid: String = "",
    val d: Int = 0,
    // le serveur ecrit un entier, mais on tolere un flottant (JSON d'une autre version)
    val bpm: Double = 0.0,
    val k: String = "",
)
