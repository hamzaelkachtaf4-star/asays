package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Une station "radio" du jour (carte de l'accueil, facon Spotify).
 *
 * Les titres sont deja des [Track] complets : l'UI n'a rien a convertir, elle
 * joue [tracks] tel quel. Le contenu est regenere chaque jour cote serveur.
 */
@Immutable
data class RadioStation(
    val name: String,
    /** Couleur d'accent (hex "#RRGGBB") fournie par le serveur. */
    val accent: String,
    val tracks: List<Track>,
) {
    val size: Int get() = tracks.size
}
