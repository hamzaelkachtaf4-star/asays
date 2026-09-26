package com.naviify.app.core.playback

object LibraryIds {
    const val ROOT = "naviify"
    const val FAVORITES = "favorites"
    const val PLAYLISTS = "playlists"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"

    const val PLAYLIST_PREFIX = "playlist/"
    const val ALBUM_PREFIX = "album/"
    const val ARTIST_PREFIX = "artist/"
    const val TRACK_PREFIX = "track/"

    fun playlist(id: String) = PLAYLIST_PREFIX + id
    fun album(id: String) = ALBUM_PREFIX + id
    fun artist(id: String) = ARTIST_PREFIX + id
    fun track(id: String) = TRACK_PREFIX + id

    fun idAfter(prefix: String, mediaId: String): String =
        mediaId.removePrefix(prefix)
}
