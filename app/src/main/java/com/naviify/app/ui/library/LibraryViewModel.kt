package com.naviify.app.ui.library

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.data.repository.AlbumListType
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.Favorite
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.ui.common.toUserMessage
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.data.download.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LibraryCategory {
    ARTISTS,
    ALBUMS,
    FAVORITES,
    DOWNLOADS,
}

@Immutable
data class LibraryUiState(
    val category: LibraryCategory = LibraryCategory.ARTISTS,
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreateDialogVisible: Boolean = false,
    val newPlaylistName: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            favoritesRepository.observeFavorites().collect { favorites ->
                _uiState.value = _uiState.value.copy(favorites = favorites)
            }
        }
        selectCategory(LibraryCategory.ARTISTS)
    }

    fun selectCategory(category: LibraryCategory) {
        _uiState.value = _uiState.value.copy(category = category, isLoading = true, error = null)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                when (category) {
                    LibraryCategory.ARTISTS -> {
                        val online = runCatching { mediaRepository.getArtists() }.getOrNull()
                        if (online != null && online.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(artists = online, isLoading = false, error = null)
                        } else {
                            val offlineArtists = withContext(Dispatchers.Default) {
                                val downloads = downloadRepository.observeDownloads().first()
                                    .filter { it.status == DownloadEntity.STATUS_DONE }
                                downloads
                                    .filter { !it.artist.isNullOrBlank() }
                                    .groupBy { it.artist.orEmpty() }
                                    .map { (artistName, tracks) ->
                                        val safeId = "offline-artist-" + java.util.Base64.getUrlEncoder().withoutPadding()
                                            .encodeToString(artistName.toByteArray(Charsets.UTF_8))
                                        Artist(
                                            id = safeId,
                                            name = artistName,
                                            coverArtId = tracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId,
                                            albumCount = tracks.mapNotNull { it.album }.distinct().size.coerceAtLeast(1),
                                            isFavorite = false,
                                        )
                                    }
                            }
                            _uiState.value = _uiState.value.copy(artists = offlineArtists, isLoading = false, error = null)
                        }
                    }
                    LibraryCategory.ALBUMS -> {
                        val online = runCatching { mediaRepository.getAlbums(AlbumListType.RANDOM, size = 100) }.getOrNull()
                        if (online != null && online.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(albums = online, isLoading = false, error = null)
                        } else {
                            val offlineAlbums = withContext(Dispatchers.Default) {
                                val downloads = downloadRepository.observeDownloads().first()
                                    .filter { it.status == DownloadEntity.STATUS_DONE }
                                downloads
                                    .groupBy { it.album ?: it.title }
                                    .map { (albumName, tracks) ->
                                        val first = tracks.first()
                                        Album(
                                            id = first.albumId ?: "offline-${first.trackId}",
                                            name = albumName,
                                            artist = first.artist,
                                            artistId = null,
                                            coverArtId = tracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId,
                                            songCount = tracks.size,
                                            duration = 0,
                                            year = null,
                                            isFavorite = false,
                                        )
                                    }
                            }
                            _uiState.value = _uiState.value.copy(albums = offlineAlbums, isLoading = false, error = null)
                        }
                    }
                    LibraryCategory.FAVORITES -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    }
                    LibraryCategory.DOWNLOADS -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.toUserMessage())
            }
        }
    }

    fun showCreatePlaylistDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogVisible = true, createError = null)
    }

    fun hideCreatePlaylistDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogVisible = false, createError = null)
    }

    fun onNewPlaylistNameChange(name: String) {
        _uiState.value = _uiState.value.copy(newPlaylistName = name, createError = null)
    }

    fun createPlaylist() {
        val name = _uiState.value.newPlaylistName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            runCatching { mediaRepository.createPlaylist(name) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        isCreateDialogVisible = false,
                        newPlaylistName = "",
                    )
                    selectCategory(LibraryCategory.ARTISTS)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createError = error.toUserMessage(),
                    )
                }
        }
    }

    fun toggleFavorite(favorite: Favorite) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(
                id = favorite.id,
                type = favorite.type,
                name = favorite.name,
                secondaryText = favorite.secondaryText,
                coverArtId = favorite.coverArtId,
            )
        }
    }

    fun playTrack(track: Track) {
        playbackController.play(listOf(track))
    }

    fun scanLibrary(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = runCatching { mediaRepository.startScan(fullScan = true) }
            val ok = res.isSuccess
            if (ok) {
                selectCategory(_uiState.value.category)
            }
            onResult(ok)
        }
    }
}
