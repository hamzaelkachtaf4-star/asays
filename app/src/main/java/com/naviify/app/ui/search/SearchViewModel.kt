package com.naviify.app.ui.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.data.search.SearchHistoryEntry
import com.naviify.app.data.search.SearchHistoryStore
import com.naviify.app.data.search.SearchHistoryType
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.model.SearchResults
import com.naviify.app.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchCategory(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
}

@Immutable
data class SearchUiState(
    val category: SearchCategory = SearchCategory.ALL,
    val results: SearchResults = SearchResults(),
    val favoriteTrackIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: List<SearchHistoryEntry> = emptyList(),
)

@HiltViewModel
@OptIn(kotlinx.coroutines.FlowPreview::class)
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val searchHistoryStore: SearchHistoryStore,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow(SearchCategory.ALL)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var lastQuery: String = ""

    init {
        viewModelScope.launch {
            _query
                .debounce(300L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query, _category.value)
                }
        }
        viewModelScope.launch {
            favoritesRepository.observeFavorites()
                .map { favorites -> favorites.filter { it.type == FavoriteType.TRACK }.map { it.id }.toSet() }
                .collect { ids ->
                    _uiState.value = _uiState.value.copy(favoriteTrackIds = ids)
                }
        }
        viewModelScope.launch {
            searchHistoryStore.entries.collect { entries ->
                _uiState.value = _uiState.value.copy(history = entries)
            }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onCategoryChange(category: SearchCategory) {
        _category.value = category
        _uiState.update { it.copy(category = category) }
        viewModelScope.launch {
            performSearch(_query.value, category)
        }
    }

    fun retry() {
        viewModelScope.launch {
            performSearch(lastQuery, _category.value)
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

    // --- Search History ---

    fun onTrackClicked(track: Track) {
        searchHistoryStore.addEntry(
            SearchHistoryEntry(
                type = SearchHistoryType.TRACK,
                id = track.id,
                title = track.title,
                subtitle = track.artist,
                coverArtId = track.coverArtId,
                album = track.album,
                albumId = track.albumId,
                duration = track.duration,
            ),
        )
    }

    fun onAlbumClicked(album: Album) {
        searchHistoryStore.addEntry(
            SearchHistoryEntry(
                type = SearchHistoryType.ALBUM,
                id = album.id,
                title = album.name,
                subtitle = album.artist,
                coverArtId = album.coverArtId,
            ),
        )
    }

    fun onArtistClicked(artist: Artist) {
        searchHistoryStore.addEntry(
            SearchHistoryEntry(
                type = SearchHistoryType.ARTIST,
                id = artist.id,
                title = artist.name,
                coverArtId = artist.coverArtId,
            ),
        )
    }

    fun onPlaylistClicked(playlist: Playlist) {
        searchHistoryStore.addEntry(
            SearchHistoryEntry(
                type = SearchHistoryType.PLAYLIST,
                id = playlist.id,
                title = playlist.name,
                coverArtId = playlist.coverArtId,
            ),
        )
    }

    fun removeHistoryEntry(entry: SearchHistoryEntry) {
        searchHistoryStore.removeEntry(entry)
    }

    fun clearHistory() {
        searchHistoryStore.clearAll()
    }

    private suspend fun performSearch(query: String, category: SearchCategory) {
        lastQuery = query
        _uiState.update { it.copy(category = category) }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(results = SearchResults(), isLoading = false, error = null, category = category) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, category = category) }
        runCatching {
            val playlists = runCatching {
                mediaRepository.getPlaylists()
                    .filter { it.name.contains(trimmed, ignoreCase = true) }
            }.getOrDefault(emptyList())

            val baseResults = mediaRepository.search(trimmed, artistCount = 30, albumCount = 30, songCount = 100)
            baseResults.copy(playlists = playlists)
        }.onSuccess { results ->
            if (query == _query.value && category == _category.value) {
                _uiState.update { it.copy(results = results, isLoading = false, error = null, category = category) }
            }
        }.onFailure { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            if (query == _query.value && category == _category.value) {
                _uiState.update { it.copy(isLoading = false, error = error.toUserMessage(), category = category) }
            }
        }
    }
}
