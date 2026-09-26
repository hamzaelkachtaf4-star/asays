package com.naviify.app.ui.album

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.Track
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isAlbumFavorite: Boolean = false,
    val favoriteTrackIds: Set<String> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: String = requireNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            favoritesRepository.observeFavorites().collect { favorites ->
                val isFavorite = favorites.any { it.type == FavoriteType.ALBUM && it.id == albumId }
                val trackIds = favorites
                    .filter { it.type == FavoriteType.TRACK }
                    .map { it.id }
                    .toSet()
                _uiState.value = _uiState.value.copy(
                    isAlbumFavorite = isFavorite,
                    favoriteTrackIds = trackIds,
                )
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (albumId.startsWith("offline-")) {
                loadOfflineAlbum(albumId)
                return@launch
            }

            runCatching { mediaRepository.getAlbumDetail(albumId) }
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        album = detail.album,
                        tracks = detail.tracks,
                    )
                }
                .onFailure { error ->
                    val fallbackLoaded = loadOfflineFallback(albumId)
                    if (!fallbackLoaded) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = error.toUserMessage())
                    }
                }
        }
    }

    private suspend fun loadOfflineAlbum(offlineAlbumId: String) {
        val targetTrackId = offlineAlbumId.removePrefix("offline-")
        val allDownloads = downloadRepository.observeDownloads().first()
            .filter { it.status == DownloadEntity.STATUS_DONE }

        val target = allDownloads.firstOrNull { it.trackId == targetTrackId }
            ?: allDownloads.firstOrNull()

        if (target == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "No downloaded tracks found",
            )
            return
        }

        val targetAlbumName = target.album ?: target.title
        val albumTracks = allDownloads.filter { (it.album ?: it.title) == targetAlbumName }

        val album = Album(
            id = offlineAlbumId,
            name = targetAlbumName,
            artist = target.artist,
            artistId = null,
            coverArtId = target.coverArtId,
            songCount = albumTracks.size,
            duration = 0,
            year = null,
            isFavorite = false,
        )

        val tracks = albumTracks.map { entity ->
            Track(
                id = entity.trackId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                coverArtId = entity.coverArtId,
                suffix = entity.suffix,
                path = entity.localFilePath,
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            album = album,
            tracks = tracks,
            error = null,
        )
    }

    private suspend fun loadOfflineFallback(id: String): Boolean {
        val allDownloads = downloadRepository.observeDownloads().first()
            .filter { it.status == DownloadEntity.STATUS_DONE }
        if (allDownloads.isEmpty()) return false

        val albumTracks = allDownloads.filter { it.album == id || it.trackId == id }
        if (albumTracks.isEmpty()) return false

        val target = albumTracks.first()
        val album = Album(
            id = id,
            name = target.album ?: target.title,
            artist = target.artist,
            artistId = null,
            coverArtId = target.coverArtId,
            songCount = albumTracks.size,
            duration = 0,
            year = null,
            isFavorite = false,
        )

        val tracks = albumTracks.map { entity ->
            Track(
                id = entity.trackId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                coverArtId = entity.coverArtId,
                suffix = entity.suffix,
                path = entity.localFilePath,
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            album = album,
            tracks = tracks,
            error = null,
        )
        return true
    }

    fun toggleAlbumFavorite() {
        val album = _uiState.value.album ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(
                id = album.id,
                type = FavoriteType.ALBUM,
                name = album.name,
                secondaryText = album.artist,
                coverArtId = album.coverArtId,
            )
        }
    }

    fun toggleTrackFavorite(track: Track) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(
                id = track.id,
                type = FavoriteType.TRACK,
                name = track.title,
                secondaryText = track.artist,
                coverArtId = track.coverArtId,
            )
        }
    }

    fun playFrom(index: Int) {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty() && index in tracks.indices) {
            playbackController.play(tracks, index)
        }
    }

    fun playShuffled() {
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            playbackController.playShuffled(tracks)
        }
    }
}
