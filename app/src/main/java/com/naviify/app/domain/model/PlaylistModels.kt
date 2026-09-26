package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val coverArtId: String? = null,
    val isPublic: Boolean = false,
    /** ISO-8601 timestamp from Navidrome, used for the Recently Added sort. */
    val created: String? = null,
    /** ISO-8601 timestamp of the last server-side edit. */
    val changed: String? = null,
    val tracks: List<Track> = emptyList(),
)

@Immutable
data class AlbumDetail(
    val album: Album,
    val tracks: List<Track>,
)

@Immutable
data class ArtistDetail(
    val artist: Artist,
    val albums: List<Album>,
    val topSongs: List<Track> = emptyList(),
    val biography: String? = null,
    val imageUrl: String? = null,
    val similarArtists: List<Artist> = emptyList(),
)

@Immutable
data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val tracks: List<Track> = emptyList(),
) {
    val isEmpty: Boolean
        get() = artists.isEmpty() && albums.isEmpty() && playlists.isEmpty() && tracks.isEmpty()
}

enum class FavoriteType(val storageValue: String) {
    ARTIST("ARTIST"),
    ALBUM("ALBUM"),
    TRACK("TRACK"),
    ;

    companion object {
        fun fromStorage(value: String): FavoriteType = entries.firstOrNull { it.storageValue == value } ?: TRACK
    }
}

@Immutable
data class Favorite(
    val type: FavoriteType,
    val id: String,
    val name: String,
    val secondaryText: String? = null,
    val coverArtId: String? = null,
)
