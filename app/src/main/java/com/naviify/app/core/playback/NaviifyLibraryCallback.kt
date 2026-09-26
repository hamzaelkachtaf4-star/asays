package com.naviify.app.core.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.naviify.app.R
import com.naviify.app.data.repository.AlbumListType
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlaybackRepeatMode
import com.naviify.app.domain.playback.PlayerQueueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android Auto browsing tree. The dashboard navigates root -> Favorites /
 * Playlists / Albums / Artists, drills into tracks, and "Play" routes through
 * [onAddMediaItems] which returns fully-signed playable items.
 */
class NaviifyLibraryCallback(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val itemMapper: MediaItemMapper,
    private val queueStore: PlayerQueueStore,
) : MediaLibrarySession.Callback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var attachedSession: MediaLibrarySession? = null
    @Volatile
    private var currentFavoriteTrackIds: Set<String> = emptySet()

    init {
        scope.launch {
            favoritesRepository.observeFavorites().collect { favorites ->
                currentFavoriteTrackIds = favorites
                    .filter { it.type == FavoriteType.TRACK }
                    .map { it.id }
                    .toSet()
                updateCustomLayout()
            }
        }
        scope.launch {
            queueStore.state.collect {
                updateCustomLayout()
            }
        }
    }

    fun attachSession(session: MediaLibrarySession) {
        attachedSession = session
        updateCustomLayout()
    }

    /** Cancels in-flight browse requests; invoked when the service is destroyed. */
    fun release() {
        attachedSession = null
        scope.cancel()
    }

    fun buildCustomLayout(
        isFavorite: Boolean,
        isShuffle: Boolean,
        repeatMode: PlaybackRepeatMode,
    ): ImmutableList<CommandButton> {
        val shuffleButton = CommandButton.Builder(
            if (isShuffle) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF,
        )
            .setSessionCommand(SHUFFLE_COMMAND)
            .setDisplayName(if (isShuffle) "Shuffle On" else "Shuffle Off")
            .setIconResId(if (isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
            .setEnabled(true)
            .build()

        val favoriteButton = CommandButton.Builder(
            if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED,
        )
            .setSessionCommand(FAVORITE_COMMAND)
            .setDisplayName(if (isFavorite) "Favorited" else "Favorite")
            .setIconResId(if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart)
            .setEnabled(true)
            .build()

        val (repeatIcon, repeatResId, repeatName) = when (repeatMode) {
            PlaybackRepeatMode.OFF -> Triple(CommandButton.ICON_REPEAT_OFF, R.drawable.ic_repeat, "Repeat Off")
            PlaybackRepeatMode.ALL -> Triple(CommandButton.ICON_REPEAT_ALL, R.drawable.ic_repeat_all, "Repeat All")
            PlaybackRepeatMode.ONE -> Triple(CommandButton.ICON_REPEAT_ONE, R.drawable.ic_repeat_one, "Repeat One")
        }
        val repeatButton = CommandButton.Builder(repeatIcon)
            .setSessionCommand(REPEAT_COMMAND)
            .setDisplayName(repeatName)
            .setIconResId(repeatResId)
            .setEnabled(true)
            .build()

        return ImmutableList.of(shuffleButton, favoriteButton, repeatButton)
    }

    fun updateCustomLayout() {
        val session = attachedSession ?: return
        scope.launch(Dispatchers.Main) {
            val currentSession = attachedSession ?: return@launch
            runCatching {
                val currentItem = currentSession.player.currentMediaItem
                val trackId = currentItem?.mediaId?.removePrefix(LibraryIds.TRACK_PREFIX)
                    ?: queueStore.state.value.currentTrack?.id
                val isFav = if (trackId != null) currentFavoriteTrackIds.contains(trackId) else false
                val isShuffle = queueStore.state.value.isShuffleEnabled
                val repeatMode = queueStore.state.value.repeatMode
                currentSession.setCustomLayout(buildCustomLayout(isFav, isShuffle, repeatMode))
            }
        }
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            .buildUpon()
            .add(FAVORITE_COMMAND)
            .add(SHUFFLE_COMMAND)
            .add(REPEAT_COMMAND)
            .build()

        val currentItem = session.player.currentMediaItem
        val trackId = currentItem?.mediaId?.removePrefix(LibraryIds.TRACK_PREFIX)
            ?: queueStore.state.value.currentTrack?.id
        val isFav = if (trackId != null) currentFavoriteTrackIds.contains(trackId) else false
        val isShuffle = queueStore.state.value.isShuffleEnabled
        val repeatMode = queueStore.state.value.repeatMode

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableCommands)
            .setCustomLayout(buildCustomLayout(isFav, isShuffle, repeatMode))
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            ACTION_FAVORITE -> {
                val currentItem = session.player.currentMediaItem
                val trackId = currentItem?.mediaId?.removePrefix(LibraryIds.TRACK_PREFIX)
                    ?: queueStore.state.value.currentTrack?.id
                if (trackId != null) {
                    val title = currentItem?.mediaMetadata?.title?.toString()
                        ?: queueStore.state.value.currentTrack?.title
                        ?: trackId
                    val artist = currentItem?.mediaMetadata?.artist?.toString()
                        ?: queueStore.state.value.currentTrack?.artist
                    scope.launch {
                        runCatching {
                            favoritesRepository.toggleFavorite(
                                id = trackId,
                                type = FavoriteType.TRACK,
                                name = title,
                                secondaryText = artist,
                            )
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            ACTION_TOGGLE_SHUFFLE -> {
                val newShuffle = !queueStore.state.value.isShuffleEnabled
                queueStore.setShuffle(newShuffle)
                val updated = queueStore.state.value
                val player = session.player
                val nextIndex = updated.currentIndex + 1
                if (player.currentMediaItemIndex == updated.currentIndex && nextIndex <= player.mediaItemCount) {
                    val count = player.mediaItemCount
                    if (count > nextIndex) {
                        player.removeMediaItems(nextIndex, count)
                    }
                    val upcoming = updated.queue.drop(nextIndex)
                    if (upcoming.isNotEmpty()) {
                        player.addMediaItems(itemMapper.queueItems(upcoming))
                    }
                } else if (updated.queue.isNotEmpty()) {
                    runCatching {
                        val currentPos = player.currentPosition.coerceAtLeast(0)
                        val shouldPlay = player.playWhenReady
                        player.setMediaItems(
                            itemMapper.queueItems(updated.queue),
                            updated.currentIndex.coerceAtLeast(0),
                            currentPos,
                        )
                        if (shouldPlay) player.play()
                    }
                }
                updateCustomLayout()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            ACTION_TOGGLE_REPEAT -> {
                val nextMode = when (queueStore.state.value.repeatMode) {
                    PlaybackRepeatMode.OFF -> PlaybackRepeatMode.ALL
                    PlaybackRepeatMode.ALL -> PlaybackRepeatMode.ONE
                    PlaybackRepeatMode.ONE -> PlaybackRepeatMode.OFF
                }
                queueStore.setRepeatMode(nextMode)
                session.player.repeatMode = when (nextMode) {
                    PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
                }
                updateCustomLayout()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }
        return super.onCustomCommand(session, controller, customCommand, args)
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> = future {
        LibraryResult.ofItem(itemMapper.browsable(LibraryIds.ROOT, "ASAYS"), params)
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
        // Null-safe: empty or still-loading folders return RESULT_SUCCESS with [].
        val children = runCatching { resolveChildren(parentId).paged(page, pageSize) }
            .getOrDefault(emptyList())
        LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = future {
        LibraryResult.ofItem(resolveItem(mediaId), null)
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> = future {
        if (query.isNotBlank()) {
            runCatching {
                val results = mediaRepository.search(query, songCount = 30)
                session.notifySearchResultChanged(browser, query, results.tracks.size, params)
            }
        }
        LibraryResult.ofVoid(params)
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
        val tracks = runCatching {
            mediaRepository.search(query, songCount = 50).tracks
        }.getOrDefault(emptyList())
        val pagedTracks = tracks.map(itemMapper::trackItem).paged(page, pageSize)
        LibraryResult.ofItemList(ImmutableList.copyOf(pagedTracks), params)
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        val settable = SettableFuture.create<List<MediaItem>>()
        scope.launch {
            val playable = try {
                resolveIncomingMediaItems(mediaItems)
            } catch (e: Exception) {
                emptyList()
            }
            settable.set(playable)
        }
        return settable
    }

    /**
     * Items that already carry a stream/local URI (mapped by [MediaItemMapper]
     * from the in-app queue) must be returned untouched; only bare browse ids
     * are resolved against the server here.
     */
    private suspend fun resolveIncomingMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        if (mediaItems.isNotEmpty() && mediaItems.all { it.localConfiguration?.uri != null }) {
            return mediaItems
        }
        val resolved = mutableListOf<MediaItem>()
        for (item in mediaItems) {
            if (item.localConfiguration?.uri != null) {
                resolved += item
                continue
            }
            resolved += resolvePlayableItems(item.mediaId)
        }
        return resolved
    }

    private suspend fun resolveChildren(parentId: String): List<MediaItem> = when (parentId) {
        LibraryIds.ROOT -> listOf(
            itemMapper.browsable(LibraryIds.FAVORITES, "Favorites"),
            itemMapper.browsable(LibraryIds.PLAYLISTS, "Playlists"),
            itemMapper.browsable(LibraryIds.ALBUMS, "Albums"),
            itemMapper.browsable(LibraryIds.ARTISTS, "Artists"),
        )
        LibraryIds.FAVORITES -> favoritesChildren()
        LibraryIds.PLAYLISTS -> runCatching { mediaRepository.getPlaylists().map(itemMapper::playlistFolder) }.getOrDefault(emptyList())
        LibraryIds.ALBUMS -> runCatching { mediaRepository.getAlbums(AlbumListType.RANDOM, size = 100).map(itemMapper::albumFolder) }.getOrDefault(emptyList())
        LibraryIds.ARTISTS -> runCatching { mediaRepository.getArtists().map(itemMapper::artistFolder) }.getOrDefault(emptyList())
        else -> when {
            parentId.startsWith(LibraryIds.PLAYLIST_PREFIX) -> {
                runCatching {
                    mediaRepository.getPlaylist(idAfter(LibraryIds.PLAYLIST_PREFIX, parentId))
                        .tracks
                        .map(itemMapper::trackItem)
                }.getOrDefault(emptyList())
            }
            parentId.startsWith(LibraryIds.ALBUM_PREFIX) -> {
                runCatching {
                    mediaRepository.getAlbumDetail(idAfter(LibraryIds.ALBUM_PREFIX, parentId))
                        .tracks
                        .map(itemMapper::trackItem)
                }.getOrDefault(emptyList())
            }
            parentId.startsWith(LibraryIds.ARTIST_PREFIX) -> {
                runCatching {
                    mediaRepository.getArtistDetail(idAfter(LibraryIds.ARTIST_PREFIX, parentId))
                        .albums
                        .map(itemMapper::albumFolder)
                }.getOrDefault(emptyList())
            }
            else -> emptyList()
        }
    }

    private suspend fun resolveItem(mediaId: String): MediaItem = when (mediaId) {
        LibraryIds.ROOT -> itemMapper.browsable(LibraryIds.ROOT, "ASAYS")
        LibraryIds.FAVORITES -> itemMapper.browsable(mediaId, "Favorites")
        LibraryIds.PLAYLISTS -> itemMapper.browsable(mediaId, "Playlists")
        LibraryIds.ALBUMS -> itemMapper.browsable(mediaId, "Albums")
        LibraryIds.ARTISTS -> itemMapper.browsable(mediaId, "Artists")
        else -> when {
            mediaId.startsWith(LibraryIds.PLAYLIST_PREFIX) -> {
                val playlist = runCatching { mediaRepository.getPlaylist(idAfter(LibraryIds.PLAYLIST_PREFIX, mediaId)) }.getOrNull()
                if (playlist != null) itemMapper.playlistFolder(playlist)
                else itemMapper.browsable(mediaId, "Playlist")
            }
            mediaId.startsWith(LibraryIds.ALBUM_PREFIX) -> {
                val detail = runCatching { mediaRepository.getAlbumDetail(idAfter(LibraryIds.ALBUM_PREFIX, mediaId)) }.getOrNull()
                if (detail != null) itemMapper.albumFolder(detail.album)
                else itemMapper.browsable(mediaId, "Album")
            }
            mediaId.startsWith(LibraryIds.ARTIST_PREFIX) -> {
                val detail = runCatching { mediaRepository.getArtistDetail(idAfter(LibraryIds.ARTIST_PREFIX, mediaId)) }.getOrNull()
                if (detail != null) itemMapper.artistFolder(detail.artist)
                else itemMapper.browsable(mediaId, "Artist")
            }
            mediaId.startsWith(LibraryIds.TRACK_PREFIX) -> {
                val trackId = idAfter(LibraryIds.TRACK_PREFIX, mediaId)
                val track = favoriteTrack(trackId)
                    ?: queueStore.state.value.queue.find { it.id == trackId }
                    ?: runCatching { mediaRepository.getSong(trackId) }.getOrNull()
                track?.let(itemMapper::trackItem) ?: itemMapper.browsable(mediaId, "Track")
            }
            else -> itemMapper.browsable(mediaId, "Unknown")
        }
    }

    private suspend fun resolvePlayableItems(mediaId: String): List<MediaItem> = when {
        mediaId == LibraryIds.FAVORITES -> runCatching { favoriteTracks().map(itemMapper::trackItem) }.getOrDefault(emptyList())
        mediaId.startsWith(LibraryIds.PLAYLIST_PREFIX) -> {
            runCatching {
                mediaRepository.getPlaylist(idAfter(LibraryIds.PLAYLIST_PREFIX, mediaId)).tracks.map(itemMapper::trackItem)
            }.getOrDefault(emptyList())
        }
        mediaId.startsWith(LibraryIds.ALBUM_PREFIX) -> {
            runCatching {
                mediaRepository.getAlbumDetail(idAfter(LibraryIds.ALBUM_PREFIX, mediaId)).tracks.map(itemMapper::trackItem)
            }.getOrDefault(emptyList())
        }
        mediaId.startsWith(LibraryIds.ARTIST_PREFIX) -> {
            runCatching {
                val artist = mediaRepository.getArtistDetail(idAfter(LibraryIds.ARTIST_PREFIX, mediaId))
                artist.albums
                    .take(5)
                    .flatMap { album ->
                        runCatching { mediaRepository.getAlbumDetail(album.id).tracks }.getOrDefault(emptyList())
                    }
                    .map(itemMapper::trackItem)
            }.getOrDefault(emptyList())
        }
        mediaId.startsWith(LibraryIds.TRACK_PREFIX) -> {
            val trackId = idAfter(LibraryIds.TRACK_PREFIX, mediaId)
            val track = favoriteTrack(trackId)
                ?: queueStore.state.value.queue.find { it.id == trackId }
                ?: runCatching { mediaRepository.getSong(trackId) }.getOrNull()
            track?.let { listOf(itemMapper.trackItem(it)) }.orEmpty()
        }
        else -> emptyList()
    }

    private suspend fun favoritesChildren(): List<MediaItem> {
        val favorites = favoritesRepository.observeFavorites().first()
        val albums = favorites
            .filter { it.type == FavoriteType.ALBUM }
            .map {
                itemMapper.albumFolder(
                    Album(
                        id = it.id,
                        name = it.name,
                        artist = it.secondaryText,
                        artistId = null,
                        coverArtId = it.coverArtId,
                        songCount = 0,
                        duration = 0,
                        year = null,
                        isFavorite = true,
                    ),
                )
            }
        val artists = favorites
            .filter { it.type == FavoriteType.ARTIST }
            .map {
                itemMapper.artistFolder(
                    Artist(
                        id = it.id,
                        name = it.name,
                        coverArtId = it.coverArtId,
                        albumCount = 0,
                        isFavorite = true,
                    ),
                )
            }
        return albums + artists + favoriteTracks().map(itemMapper::trackItem)
    }

    private suspend fun favoriteTracks(): List<Track> =
        favoritesRepository.observeFavorites()
            .first()
            .filter { it.type == FavoriteType.TRACK }
            .map {
                Track(
                    id = it.id,
                    title = it.name,
                    artist = it.secondaryText,
                    coverArtId = it.coverArtId,
                )
            }

    private suspend fun favoriteTrack(trackId: String): Track? =
        favoriteTracks().firstOrNull { it.id == trackId }

    private fun <T> future(block: suspend () -> LibraryResult<T>): ListenableFuture<LibraryResult<T>> {
        val settable = SettableFuture.create<LibraryResult<T>>()
        scope.launch {
            val result: LibraryResult<T> = try {
                block()
            } catch (e: Exception) {
                @Suppress("UNCHECKED_CAST")
                LibraryResult.ofError<Unit>(SessionError.ERROR_IO) as LibraryResult<T>
            }
            settable.set(result)
        }
        return settable
    }

    private fun List<MediaItem>.paged(page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize <= 0) return this
        return drop(page * pageSize).take(pageSize)
    }

    private fun idAfter(prefix: String, mediaId: String): String = mediaId.removePrefix(prefix)

    companion object {
        const val ACTION_FAVORITE = "com.naviify.app.command.FAVORITE"
        const val ACTION_TOGGLE_SHUFFLE = "com.naviify.app.command.TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.naviify.app.command.TOGGLE_REPEAT"

        val FAVORITE_COMMAND = SessionCommand(ACTION_FAVORITE, Bundle())
        val SHUFFLE_COMMAND = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle())
        val REPEAT_COMMAND = SessionCommand(ACTION_TOGGLE_REPEAT, Bundle())
    }
}
