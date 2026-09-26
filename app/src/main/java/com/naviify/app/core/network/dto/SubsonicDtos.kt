package com.naviify.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic Subsonic/OpenSubsonic envelope. Every endpoint returns a
 * `subsonic-response` object; Navidrome additionally reports `serverVersion`
 * and `openSubsonic` flags. Payload nodes (`artists`, `album`, ...) live
 * *directly* on the response, not under a wrapper key.
 */
@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response") val response: SubsonicResponse,
)

@Serializable
data class SubsonicResponse(
    val status: String = "ok",
    val version: String? = null,
    val type: String? = null,
    @SerialName("serverVersion") val serverVersion: String? = null,
    @SerialName("openSubsonic") val openSubsonic: Boolean = false,
    val error: SubsonicError? = null,
    val artists: Indexes? = null,
    val artist: ArtistWithAlbumsID3? = null,
    val album: AlbumWithSongsID3? = null,
    @SerialName("searchResult3") val searchResult3: SearchResult3? = null,
    val playlists: Playlists? = null,
    val playlist: PlaylistDetail? = null,
    @SerialName("albumList2") val albumList2: AlbumList2? = null,
    val song: Child? = null,
    val lyrics: Lyrics? = null,
    @SerialName("structuredLyrics") val structuredLyrics: List<StructuredLyrics> = emptyList(),
    @SerialName("randomSongs") val randomSongs: SongsList? = null,
    @SerialName("similarSongs2") val similarSongs2: SongsList? = null,
    @SerialName("similarSongs") val similarSongs: SongsList? = null,
    @SerialName("topSongs") val topSongs: SongsList? = null,
    val artistInfo: ArtistInfoID3? = null,
    val artistInfo2: ArtistInfoID3? = null,
    val scanStatus: ScanStatus? = null,
) {
    val isOk: Boolean get() = status == "ok" && error == null
}

@Serializable
data class ScanStatus(
    val scanning: Boolean = false,
    val count: Long? = null,
)

@Serializable
data class SongsList(
    val song: List<Child> = emptyList(),
)

@Serializable
data class SubsonicError(
    val code: Int = 0,
    val message: String = "",
)

@Serializable
data class AlbumList2(
    val album: List<AlbumID3> = emptyList(),
)

/** Legacy `getLyrics.view` payload (plain text, optionally timed). */
@Serializable
data class Lyrics(
    @SerialName("songId") val songId: String? = null,
    @SerialName("value") val lyric: String? = null,
)
