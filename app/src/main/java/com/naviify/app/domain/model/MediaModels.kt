package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val id: String,
    val name: String,
    val coverArtId: String?,
    val albumCount: Int,
    val isFavorite: Boolean,
)

@Immutable
data class Album(
    val id: String,
    val name: String,
    val artist: String?,
    val artistId: String?,
    val coverArtId: String?,
    val songCount: Int,
    val duration: Int,
    val year: Int?,
    val isFavorite: Boolean,
)

@Immutable
data class Track(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val albumArtist: String? = null,
    val coverArtId: String? = null,
    val duration: Int = 0,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val bitRate: Int? = null,
    /** BPM reel remonte par l'API Subsonic (tag TBPM ecrit par l'analyseur serveur). */
    val bpm: Int? = null,
    val path: String? = null,
    val isFavorite: Boolean = false,
    val created: String? = null,
    val genre: String? = null,
    val isUserQueued: Boolean = false,
)
