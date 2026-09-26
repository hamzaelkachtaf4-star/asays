package com.naviify.app.core.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.naviify.app.core.network.SubsonicUrlProvider
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts domain media into Media3 items. Stream URLs are signed per track so
 * ExoPlayer (via the service) can fetch them through the cache layer; cover art
 * URIs power the notification, lock screen and Android Auto artwork.
 */
@Singleton
class MediaItemMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val urlProvider: SubsonicUrlProvider,
    private val streamQualityProvider: StreamQualityProvider,
    private val downloadRepository: DownloadRepository,
) {

    fun trackItem(track: Track): MediaItem = MediaItem.Builder()
        .setMediaId(LibraryIds.track(track.id))
        .setUri(
            downloadRepository.localFileFor(track.id)
                ?.takeIf { it.isFile && it.length() > 0L }
                ?.let { Uri.fromFile(it).toString() }
                ?: urlProvider.streamUrl(track.id, streamQualityProvider.current().maxBitRate)?.toString().orEmpty(),
        )
        .setCustomCacheKey("${track.id}:${streamQualityProvider.current().name}")
        .setMediaMetadata(trackMetadata(track))
        .build()

    /** Playable queue items only; tracks without a signed/local URI are skipped. */
    fun queueItems(tracks: List<Track>): List<MediaItem> =
        tracks.mapNotNull { track ->
            val item = trackItem(track)
            if (item.localConfiguration?.uri?.toString().isNullOrBlank()) null else item
        }

    @Suppress("DEPRECATION")
    fun playlistFolder(playlist: Playlist): MediaItem = browsable(
        mediaId = LibraryIds.playlist(playlist.id),
        title = playlist.name,
        subtitle = "Playlist · ${playlist.songCount} songs",
        coverArtId = playlist.coverArtId,
        folderType = MediaMetadata.FOLDER_TYPE_PLAYLISTS,
    )

    @Suppress("DEPRECATION")
    fun albumFolder(album: Album): MediaItem = browsable(
        mediaId = LibraryIds.album(album.id),
        title = album.name,
        subtitle = album.artist,
        coverArtId = album.coverArtId,
        folderType = MediaMetadata.FOLDER_TYPE_ALBUMS,
    )

    @Suppress("DEPRECATION")
    fun artistFolder(artist: Artist): MediaItem = browsable(
        mediaId = LibraryIds.artist(artist.id),
        title = artist.name,
        subtitle = "Artist · ${artist.albumCount} albums",
        coverArtId = artist.coverArtId,
        folderType = MediaMetadata.FOLDER_TYPE_ARTISTS,
    )

    @Suppress("DEPRECATION")
    fun browsable(
        mediaId: String,
        title: String,
        subtitle: String? = null,
        coverArtId: String? = null,
        folderType: Int = MediaMetadata.FOLDER_TYPE_MIXED,
    ): MediaItem {
        val metaBuilder = MediaMetadata.Builder()
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setFolderType(folderType)
            .setMediaType(
                when (folderType) {
                    MediaMetadata.FOLDER_TYPE_PLAYLISTS -> MediaMetadata.MEDIA_TYPE_PLAYLIST
                    MediaMetadata.FOLDER_TYPE_ALBUMS -> MediaMetadata.MEDIA_TYPE_ALBUM
                    MediaMetadata.FOLDER_TYPE_ARTISTS -> MediaMetadata.MEDIA_TYPE_ARTIST
                    else -> MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
                }
            )
            .setTitle(title)
            .setSubtitle(subtitle)

        if (!coverArtId.isNullOrBlank()) {
            // Only the URI is set. Embedding artwork bytes here would read a
            // cover file per item on the calling thread and inflate every
            // MediaItem that Media3 publishes over Binder; the artwork content
            // provider already prefers the local file and falls back to server.
            metaBuilder.setArtworkUri(coverUri(coverArtId))
        }

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    @Suppress("DEPRECATION")
    private fun trackMetadata(track: Track): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)

        val coverId = track.coverArtId
        if (!coverId.isNullOrBlank()) {
            builder.setArtworkUri(coverUri(coverId))
        }

        return builder.build()
    }

    private fun coverUri(coverArtId: String?): Uri? {
        if (coverArtId.isNullOrBlank()) return null
        val local = downloadRepository.localCoverFor(coverArtId)
        val suffix = local?.extension?.takeIf { it.isNotBlank() } ?: "jpg"
        return Uri.parse("content://${context.packageName}.artwork/cover/$coverArtId.$suffix")
    }
}
