package com.naviify.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val PLAYLISTS = "playlists"
    const val FAVORITES = "favorites"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"
    const val ARTIST = "artist/{id}"
    const val ALBUM = "album/{id}"
    const val PLAYLIST = "playlist/{id}"

    fun artist(id: String) = "artist/${android.net.Uri.encode(id)}"
    fun album(id: String) = "album/${android.net.Uri.encode(id)}"
    fun playlist(id: String) = "playlist/${android.net.Uri.encode(id)}"
}
