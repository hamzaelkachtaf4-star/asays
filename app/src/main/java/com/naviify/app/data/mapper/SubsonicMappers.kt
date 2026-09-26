package com.naviify.app.data.mapper

import com.naviify.app.core.network.dto.AlbumID3
import com.naviify.app.core.network.dto.AlbumWithSongsID3
import com.naviify.app.core.network.dto.ArtistID3
import com.naviify.app.core.network.dto.ArtistWithAlbumsID3
import com.naviify.app.core.network.dto.Child
import com.naviify.app.core.network.dto.PlaylistDetail
import com.naviify.app.core.network.dto.PlaylistSummary
import com.naviify.app.core.network.dto.LyricLine
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.AlbumDetail
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.ArtistDetail
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.model.LyricsLineData

fun ArtistID3.toDomain(): Artist = Artist(
    id = id,
    name = name,
    coverArtId = coverArt,
    albumCount = albumCount,
    isFavorite = starred ?: false,
)

fun AlbumID3.toDomain(): Album = Album(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    coverArtId = coverArt,
    songCount = songCount,
    duration = duration,
    year = year,
    isFavorite = starred ?: false,
)

fun Child.toTrack(): Track = Track(
    id = id,
    title = title.orEmpty(),
    artist = artist,
    album = album,
    albumId = parent,
    artistId = artistId,
    albumArtist = albumArtist,
    coverArtId = coverArt ?: parent,
    duration = duration ?: 0,
    trackNumber = track,
    discNumber = discNumber,
    year = year,
    contentType = contentType,
    suffix = suffix,
    bitRate = bitRate,
    path = path,
    isFavorite = starred ?: false,
    created = created,
    genre = genre,
)

fun LyricLine.toDomain(): LyricsLineData = LyricsLineData(
    startMs = start,
    text = value,
)

fun sanitizePlaylistName(rawName: String): String {
    if (!rawName.contains('%')) return rawName
    var decoded = rawName
    try {
        if (decoded.contains('%')) decoded = java.net.URLDecoder.decode(decoded, "UTF-8")
        if (decoded.contains('%')) decoded = java.net.URLDecoder.decode(decoded, "UTF-8")
    } catch (_: Exception) {
        decoded = decoded.replace("%20", " ")
    }
    return decoded
}

fun PlaylistSummary.toDomain(): Playlist = Playlist(
    id = id,
    name = sanitizePlaylistName(name),
    comment = comment,
    owner = owner,
    songCount = songCount,
    duration = duration,
    coverArtId = coverArt,
    isPublic = `public` ?: false,
    created = created,
    changed = changed,
)

fun PlaylistDetail.toDomain(): Playlist = Playlist(
    id = id,
    name = sanitizePlaylistName(name),
    comment = comment,
    owner = owner,
    songCount = songCount,
    duration = duration,
    coverArtId = coverArt,
    isPublic = `public` ?: false,
    created = created,
    changed = changed,
    tracks = entry.map {
        val t = it.toTrack()
        if (t.coverArtId.isNullOrBlank()) t.copy(coverArtId = coverArt) else t
    },
)

fun AlbumWithSongsID3.toDomain(): AlbumDetail = AlbumDetail(
    album = Album(
        id = id,
        name = name,
        artist = artist,
        artistId = artistId,
        coverArtId = coverArt,
        songCount = songCount,
        duration = duration,
        year = year,
        isFavorite = starred ?: false,
    ),
    tracks = song.map {
        val t = it.toTrack()
        if (t.coverArtId.isNullOrBlank()) t.copy(coverArtId = coverArt) else t
    },
)

fun ArtistWithAlbumsID3.toDomain(): ArtistDetail = ArtistDetail(
    artist = Artist(
        id = id,
        name = name,
        coverArtId = coverArt,
        albumCount = albumCount,
        isFavorite = starred ?: false,
    ),
    albums = album.map { it.toDomain() },
)
