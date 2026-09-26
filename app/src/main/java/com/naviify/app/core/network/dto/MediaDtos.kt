package com.naviify.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Indexes(
    @SerialName("lastModified") val lastModified: Long? = null,
    @SerialName("ignoredArticles") val ignoredArticles: String? = null,
    val index: List<ArtistIndex> = emptyList(),
    val shortcut: List<ArtistIndex> = emptyList(),
)

@Serializable
data class ArtistIndex(
    val name: String = "",
    val artist: List<ArtistID3> = emptyList(),
)

@Serializable
data class ArtistID3(
    val id: String,
    val name: String,
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("albumCount") val albumCount: Int = 0,
    @SerialName("artistImageUrl") val artistImageUrl: String? = null,
    val starred: Boolean? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
    @SerialName("playCount") val playCount: Long? = null,
)

@Serializable
data class ArtistWithAlbumsID3(
    val id: String,
    val name: String,
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("albumCount") val albumCount: Int = 0,
    @SerialName("artistImageUrl") val artistImageUrl: String? = null,
    val starred: Boolean? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
    @SerialName("playCount") val playCount: Long? = null,
    val album: List<AlbumID3> = emptyList(),
)

@Serializable
data class AlbumID3(
    val id: String,
    val name: String,
    val artist: String? = null,
    @SerialName("artistId") val artistId: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val playCount: Long? = null,
    val created: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val starred: Boolean? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
)

@Serializable
data class AlbumWithSongsID3(
    val id: String,
    val name: String,
    val artist: String? = null,
    @SerialName("artistId") val artistId: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val playCount: Long? = null,
    val created: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val starred: Boolean? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
    val song: List<Child> = emptyList(),
)

/** A track (or directory) in Subsonic's generic `child` node. */
@Serializable
data class Child(
    val id: String,
    val parent: String? = null,
    @SerialName("isDir") val isDir: Boolean = false,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    @SerialName("artistId") val artistId: String? = null,
    @SerialName("albumArtist") val albumArtist: String? = null,
    val track: Int? = null,
    @SerialName("discNumber") val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    val size: Long? = null,
    @SerialName("contentType") val contentType: String? = null,
    val suffix: String? = null,
    @SerialName("transcodedContentType") val transcodedContentType: String? = null,
    @SerialName("transcodedSuffix") val transcodedSuffix: String? = null,
    val duration: Int? = null,
    @SerialName("bitRate") val bitRate: Int? = null,
    val path: String? = null,
    @SerialName("playCount") val playCount: Long? = null,
    val created: String? = null,
    val starred: Boolean? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
    @SerialName("bookmarkPosition") val bookmarkPosition: Long? = null,
    @SerialName("isVideo") val isVideo: Boolean = false,
    @SerialName("structuredLyrics") val structuredLyrics: List<StructuredLyrics> = emptyList(),
)

@Serializable
data class ArtistInfoID3(
    val biography: String? = null,
    val musicBrainzId: String? = null,
    val lastFmUrl: String? = null,
    val smallImageUrl: String? = null,
    val mediumImageUrl: String? = null,
    val largeImageUrl: String? = null,
    val similarArtist: List<ArtistID3> = emptyList(),
)

@Serializable
data class StructuredLyrics(
    val lang: String? = null,
    val synced: Boolean = false,
    val offset: Int? = null,
    @SerialName("displayArtist") val displayArtist: String? = null,
    @SerialName("displayTitle") val displayTitle: String? = null,
    val line: List<LyricLine> = emptyList(),
)

@Serializable
data class LyricLine(
    val start: Long? = null,
    val end: Long? = null,
    val value: String = "",
)
