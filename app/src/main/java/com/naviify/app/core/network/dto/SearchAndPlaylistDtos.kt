package com.naviify.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult3(
    val artist: List<ArtistID3> = emptyList(),
    val album: List<AlbumID3> = emptyList(),
    val song: List<Child> = emptyList(),
)

@Serializable
data class Playlists(
    val playlist: List<PlaylistSummary> = emptyList(),
)

@Serializable
data class PlaylistSummary(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val `public`: Boolean? = null,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
)

@Serializable
data class PlaylistDetail(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val `public`: Boolean? = null,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    val entry: List<Child> = emptyList(),
)
