package com.naviify.app.ui.playlist

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.data.repository.AlbumListType
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.data.stats.PlaylistHistoryStore
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which slice of the library the chips are showing. */
enum class LibraryFilter(val label: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    DOWNLOADS("Downloads"),
    FAVORITES("Favorites"),
}

/** Spotify-style layout switch for the library list. */
enum class PlaylistLayout {
    LIST,
    GRID,
    ;

    companion object {
        fun fromStorage(value: String?): PlaylistLayout =
            entries.firstOrNull { it.name == value } ?: LIST
    }
}

/** How the playlist list is ordered. All options are client-side sorts. */
enum class PlaylistSort(val label: String) {
    RECENTLY_ADDED("Recently added"),
    RECENTLY_PLAYED("Recently played"),
    ALPHABETICAL("A to Z"),
    MOST_SONGS("Most songs"),
    ;

    companion object {
        fun fromStorage(value: String?): PlaylistSort =
            entries.firstOrNull { it.name == value } ?: RECENTLY_ADDED
    }
}

@Immutable
data class PlaylistsUiState(
    val filter: LibraryFilter = LibraryFilter.ALL,
    val layout: PlaylistLayout = PlaylistLayout.LIST,
    val sortOrder: PlaylistSort = PlaylistSort.RECENTLY_ADDED,
    val isSortMenuVisible: Boolean = false,
    /** Epoch millis of the last local playback start, keyed by playlist id. */
    val lastPlayedAt: Map<String, Long> = emptyMap(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isLoadingArtists: Boolean = false,
    val isLoadingAlbums: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isCreateDialogVisible: Boolean = false,
    val newPlaylistName: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
    val playlistTrackIds: Map<String, Set<String>> = emptyMap(),
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playlistHistoryStore: PlaylistHistoryStore,
    @ApplicationContext context: Context,
) : ViewModel() {

    // The layout choice is a small UI preference, so it lives in SharedPreferences
    // rather than DataStore to keep the read synchronous and flicker-free.
    private val prefs = context.getSharedPreferences(LIBRARY_PREFS, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        PlaylistsUiState(
            layout = PlaylistLayout.fromStorage(prefs.getString(KEY_LAYOUT, null)),
            sortOrder = PlaylistSort.fromStorage(prefs.getString(KEY_SORT, null)),
            lastPlayedAt = playlistHistoryStore.lastPlayedMap(),
        ),
    )
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    fun selectFilter(filter: LibraryFilter) {
        if (_uiState.value.filter == filter) return
        _uiState.value = _uiState.value.copy(filter = filter)
        // Artists and albums are only fetched the first time their chip is
        // opened, so the default Library view stays a single playlists call.
        when (filter) {
            LibraryFilter.ARTISTS -> ensureArtistsLoaded()
            LibraryFilter.ALBUMS -> ensureAlbumsLoaded()
            else -> Unit
        }
    }

    private fun ensureArtistsLoaded() {
        val state = _uiState.value
        if (state.artists.isNotEmpty() || state.isLoadingArtists) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingArtists = true)
            runCatching { mediaRepository.getArtists() }
                .onSuccess { artists ->
                    _uiState.value = _uiState.value.copy(
                        artists = artists.sortedBy { it.name.lowercase() },
                        isLoadingArtists = false,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingArtists = false,
                        error = error.toUserMessage(),
                    )
                }
        }
    }

    private fun ensureAlbumsLoaded() {
        val state = _uiState.value
        if (state.albums.isNotEmpty() || state.isLoadingAlbums) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAlbums = true)
            runCatching { mediaRepository.getAlbums(AlbumListType.NEWEST, size = 200) }
                .onSuccess { albums ->
                    _uiState.value = _uiState.value.copy(
                        albums = albums,
                        isLoadingAlbums = false,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingAlbums = false,
                        error = error.toUserMessage(),
                    )
                }
        }
    }

    /** Re-fetches whatever the active chip is showing, for pull-to-refresh. */
    private fun refreshActiveCategory() {
        when (_uiState.value.filter) {
            LibraryFilter.ARTISTS -> {
                _uiState.value = _uiState.value.copy(artists = emptyList(), isLoadingArtists = false)
                ensureArtistsLoaded()
            }
            LibraryFilter.ALBUMS -> {
                _uiState.value = _uiState.value.copy(albums = emptyList(), isLoadingAlbums = false)
                ensureAlbumsLoaded()
            }
            else -> Unit
        }
    }

    fun setLayout(layout: PlaylistLayout) {
        if (_uiState.value.layout == layout) return
        prefs.edit().putString(KEY_LAYOUT, layout.name).apply()
        _uiState.value = _uiState.value.copy(layout = layout)
    }

    fun setSortOrder(order: PlaylistSort) {
        if (_uiState.value.sortOrder == order) {
            _uiState.value = _uiState.value.copy(isSortMenuVisible = false)
            return
        }
        prefs.edit().putString(KEY_SORT, order.name).apply()
        _uiState.value = _uiState.value.copy(sortOrder = order, isSortMenuVisible = false)
    }

    fun toggleSortMenu() {
        _uiState.value = _uiState.value.copy(isSortMenuVisible = !_uiState.value.isSortMenuVisible)
    }

    fun dismissSortMenu() {
        if (!_uiState.value.isSortMenuVisible) return
        _uiState.value = _uiState.value.copy(isSortMenuVisible = false)
    }

    init {
        refresh()
        viewModelScope.launch {
            mediaRepository.playlistSyncEvents.collect {
                refreshSilently()
            }
        }
    }

    fun refreshSilently() {
        viewModelScope.launch {
            runCatching { mediaRepository.getPlaylists() }
                .onSuccess { playlists ->
                    val freshMap = mediaRepository.getCachedPlaylistTrackIds()
                    _uiState.value = _uiState.value.copy(
                        playlists = playlists,
                        playlistTrackIds = if (freshMap.isNotEmpty()) freshMap else _uiState.value.playlistTrackIds,
                    )
                }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            runCatching { mediaRepository.getPlaylists() }
                .onSuccess { playlists ->
                    val freshMap = mediaRepository.getCachedPlaylistTrackIds()
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        playlists = playlists,
                        playlistTrackIds = if (freshMap.isNotEmpty()) freshMap else _uiState.value.playlistTrackIds,
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isRefreshing = false, error = error.toUserMessage())
                }
            refreshActiveCategory()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val cachedMap = mediaRepository.getCachedPlaylistTrackIds()
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                playlistTrackIds = if (cachedMap.isNotEmpty()) cachedMap else _uiState.value.playlistTrackIds,
            )
            runCatching { mediaRepository.getPlaylists() }
                .onSuccess { playlists ->
                    val freshMap = mediaRepository.getCachedPlaylistTrackIds()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        playlists = playlists,
                        playlistTrackIds = if (freshMap.isNotEmpty()) freshMap else _uiState.value.playlistTrackIds,
                        lastPlayedAt = playlistHistoryStore.lastPlayedMap(),
                    )
                    if (playlists.any { it.id == MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID }) {
                        launch {
                            val tracks = runCatching { mediaRepository.getLibraryTracks() }.getOrNull()
                            if (tracks != null) {
                                _uiState.value = _uiState.value.copy(
                                    playlists = _uiState.value.playlists.map { pl ->
                                        if (pl.id == MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID) {
                                            pl.copy(songCount = tracks.size)
                                        } else pl
                                    }
                                )
                            }
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.toUserMessage())
                }
        }
    }

    fun checkTrackInPlaylists(trackId: String) {
        viewModelScope.launch {
            val currentMap = mediaRepository.getCachedPlaylistTrackIds().toMutableMap()
            _uiState.value = _uiState.value.copy(playlistTrackIds = currentMap)

            val customPlaylists = _uiState.value.playlists.filter {
                it.id != MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID &&
                !it.name.trim().equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)
            }
            customPlaylists.forEach { pl ->
                if (!currentMap.containsKey(pl.id)) {
                    runCatching {
                        val detail = mediaRepository.getPlaylist(pl.id)
                        currentMap[pl.id] = detail.tracks.map { it.id }.toSet()
                        _uiState.value = _uiState.value.copy(playlistTrackIds = currentMap.toMap())
                    }
                }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogVisible = true, createError = null)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(isCreateDialogVisible = false, createError = null)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(newPlaylistName = name)
    }

    fun createPlaylist(initialTrack: Track? = null, onDone: ((Boolean) -> Unit)? = null) {
        val name = _uiState.value.newPlaylistName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            val songIds = if (initialTrack != null) listOf(initialTrack.id) else emptyList()
            runCatching { mediaRepository.createPlaylist(name = name, songIds = songIds) }
                .onSuccess { createdPlaylist ->
                    val currentMap = _uiState.value.playlistTrackIds.toMutableMap()
                    if (initialTrack != null) {
                        currentMap[createdPlaylist.id] = setOf(initialTrack.id)
                    }
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        isCreateDialogVisible = false,
                        newPlaylistName = "",
                        playlistTrackIds = currentMap,
                    )
                    refresh()
                    onDone?.invoke(true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isCreating = false, createError = error.toUserMessage())
                    onDone?.invoke(false)
                }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { mediaRepository.addToPlaylist(playlistId, track.id) }
            if (result.isSuccess) {
                val current = _uiState.value.playlistTrackIds.toMutableMap()
                val updated = (current[playlistId].orEmpty() + track.id)
                current[playlistId] = updated
                _uiState.value = _uiState.value.copy(playlistTrackIds = current)
                refresh()
            }
            onDone(result.isSuccess)
        }
    }

    private companion object {
        const val LIBRARY_PREFS = "naviify_library_prefs"
        const val KEY_LAYOUT = "playlist_layout"
        const val KEY_SORT = "playlist_sort"
    }

    /** Called when playback starts from a playlist so Recently Played stays accurate. */
    fun recordPlaylistPlayed(playlistId: String) {
        if (playlistId.isBlank()) return
        playlistHistoryStore.recordPlayed(playlistId)
        _uiState.value = _uiState.value.copy(lastPlayedAt = playlistHistoryStore.lastPlayedMap())
    }

    fun deletePlaylist(playlistId: String, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = runCatching { mediaRepository.deletePlaylist(playlistId) }
            if (result.isSuccess) {
                refresh()
            }
            onDone?.invoke(result.isSuccess)
        }
    }
}
