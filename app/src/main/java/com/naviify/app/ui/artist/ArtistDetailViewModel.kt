package com.naviify.app.ui.artist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.Track
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ArtistDetailUiState(
    val isLoading: Boolean = true,
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val topSongs: List<Track> = emptyList(),
    val biography: String? = null,
    val imageUrl: String? = null,
    val isFavorite: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPlayingTrackId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val rawArtistId: String = requireNotNull(savedStateHandle["id"])
    private var resolvedArtistId: String = runCatching { android.net.Uri.decode(rawArtistId) }.getOrDefault(rawArtistId)

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            favoritesRepository.observeFavorites().collect { favorites ->
                val currentId = _uiState.value.artist?.id ?: resolvedArtistId
                _uiState.update {
                    it.copy(isFavorite = favorites.any { fav -> fav.type == FavoriteType.ARTIST && (fav.id == currentId || fav.id == rawArtistId) })
                }
            }
        }
        viewModelScope.launch {
            playbackController.state.collect { playerState ->
                _uiState.update {
                    it.copy(
                        isPlaying = playerState.isPlaying,
                        currentPlayingTrackId = playerState.currentTrack?.id,
                    )
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. If explicit offline ID, try offline fallback first
            if (resolvedArtistId.startsWith("offline-artist-")) {
                val loaded = loadOfflineArtistFallback(resolvedArtistId)
                if (loaded) return@launch
            }

            // 2. Direct lookup by ID
            val directResult = runCatching { mediaRepository.getArtistDetail(resolvedArtistId) }
            if (directResult.isSuccess) {
                val detail = directResult.getOrThrow()
                resolvedArtistId = detail.artist.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        artist = detail.artist,
                        albums = detail.albums,
                        topSongs = detail.topSongs,
                        biography = detail.biography,
                        imageUrl = detail.imageUrl,
                    )
                }
                return@launch
            }

            // 3. Fallback: If direct lookup failed, resolvedArtistId might be an artist name
            val cleanName = resolvedArtistId
                .removePrefix("offline-artist-")
                .removePrefix("artist-name:")
                .trim()

            if (cleanName.isNotBlank()) {
                val searchResult = runCatching {
                    mediaRepository.search(cleanName, artistCount = 5, albumCount = 0, songCount = 0)
                }.getOrNull()

                val matched = searchResult?.artists?.firstOrNull {
                    it.name.equals(cleanName, ignoreCase = true)
                } ?: searchResult?.artists?.firstOrNull()

                if (matched != null) {
                    val detailResult = runCatching { mediaRepository.getArtistDetail(matched.id) }
                    if (detailResult.isSuccess) {
                        val detail = detailResult.getOrThrow()
                        resolvedArtistId = detail.artist.id
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                artist = detail.artist,
                                albums = detail.albums,
                                topSongs = detail.topSongs,
                                biography = detail.biography,
                                imageUrl = detail.imageUrl,
                            )
                        }
                        return@launch
                    }
                }
            }

            // 4. Fallback to offline downloaded tracks for this artist
            val fallbackLoaded = loadOfflineArtistFallback(resolvedArtistId)
            if (!fallbackLoaded) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = directResult.exceptionOrNull()?.toUserMessage() ?: "Artist not found",
                    )
                }
            }
        }
    }

    private suspend fun loadOfflineArtistFallback(id: String): Boolean {
        val payload = id.removePrefix("offline-artist-")
        val rawName = runCatching {
            String(java.util.Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        }.getOrElse {
            runCatching { java.net.URLDecoder.decode(payload, "UTF-8") }.getOrDefault(payload)
        }
        val downloads = downloadRepository.observeDownloads().first()
            .filter { it.status == DownloadEntity.STATUS_DONE }
        val artistTracks = downloads.filter { it.artist.equals(rawName, ignoreCase = true) || it.trackId == id }
        if (artistTracks.isEmpty()) return false

        val artistName = artistTracks.first().artist ?: rawName
        val albums = artistTracks
            .groupBy { it.album ?: it.title }
            .map { (albumName, tracks) ->
                val first = tracks.first()
                Album(
                    id = first.albumId ?: "offline-${first.trackId}",
                    name = albumName,
                    artist = artistName,
                    artistId = null,
                    coverArtId = tracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId,
                    songCount = tracks.size,
                    duration = 0,
                    year = null,
                    isFavorite = false,
                )
            }

        val artist = Artist(
            id = id,
            name = artistName,
            coverArtId = artistTracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId,
            albumCount = albums.size,
            isFavorite = false,
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            artist = artist,
            albums = albums,
            error = null,
        )
        return true
    }

    fun toggleFavorite() {
        val artist = _uiState.value.artist ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(
                id = artist.id,
                type = FavoriteType.ARTIST,
                name = artist.name,
                secondaryText = "${artist.albumCount} albums",
                coverArtId = artist.coverArtId,
            )
        }
    }

    fun playTopSong(track: Track) {
        val topSongs = _uiState.value.topSongs
        val index = topSongs.indexOfFirst { it.id == track.id }
        if (index >= 0) {
            playbackController.play(topSongs, startIndex = index)
        } else {
            playbackController.play(listOf(track), 0)
        }
    }

    fun playAll(startShuffled: Boolean = false) {
        val topSongs = _uiState.value.topSongs
        if (topSongs.isNotEmpty()) {
            if (startShuffled) {
                playbackController.playShuffled(topSongs)
            } else {
                playbackController.play(topSongs, startIndex = 0)
            }
        } else {
            playShuffled()
        }
    }

    fun playShuffled() {
        viewModelScope.launch {
            val albums = _uiState.value.albums
            val onlineTracks = albums.flatMap { album ->
                // Synthetic offline albums have no server counterpart, so asking
                // the API for them would only produce a failed request.
                if (album.id.startsWith(OFFLINE_ALBUM_PREFIX)) {
                    emptyList()
                } else {
                    runCatching { mediaRepository.getAlbumDetail(album.id).tracks }.getOrDefault(emptyList())
                }
            }
            // Falls back to the downloaded tracks so shuffle play keeps working
            // offline and is never a dead control.
            val tracks = onlineTracks.ifEmpty {
                val artistName = _uiState.value.artist?.name
                downloadRepository.observeDownloads().first()
                    .filter { it.status == DownloadEntity.STATUS_DONE }
                    .filter { artistName.isNullOrBlank() || it.artist.equals(artistName, ignoreCase = true) }
                    .map { entity ->
                        Track(
                            id = entity.trackId,
                            title = entity.title,
                            artist = entity.artist,
                            album = entity.album,
                            albumId = entity.albumId,
                            coverArtId = entity.coverArtId,
                            suffix = entity.suffix,
                            path = entity.localFilePath,
                        )
                    }
            }
            if (tracks.isNotEmpty()) {
                playbackController.playShuffled(tracks)
            }
        }
    }

    private companion object {
        const val OFFLINE_ALBUM_PREFIX = "offline-"
    }
}
