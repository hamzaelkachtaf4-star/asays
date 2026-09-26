package com.naviify.app.ui.playlist

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.data.stats.PlaylistHistoryStore
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.common.toUserMessage
import com.naviify.app.core.storage.PlaylistMixStore
import com.naviify.app.domain.model.PlaylistMixConfig
import com.naviify.app.domain.model.PlaylistMixMode
import com.naviify.app.domain.model.DjTrackMeta
import com.naviify.app.data.djmeta.DjMetadataRepository
import com.naviify.app.domain.model.sortTracksHarmonically
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class PlaylistSortOrder(val label: String) {
    CUSTOM_ORDER("Custom order"),
    RECENTLY_ADDED("Recently added"),
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
}

@Immutable
data class PlaylistDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val playlist: Playlist? = null,
    val sortOrder: PlaylistSortOrder = PlaylistSortOrder.CUSTOM_ORDER,
    val sortedTracks: List<Track> = emptyList(),
    val error: String? = null,
    val isRenameDialogVisible: Boolean = false,
    val renameText: String = "",
    val isEditSheetVisible: Boolean = false,
    val editNameText: String = "",
    val editDescriptionText: String = "",
    val editIsPublic: Boolean = false,
    val isReorderMode: Boolean = false,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val isUpdating: Boolean = false,
    val coverUpdateTrigger: Long = 0L,
    val hasCustomCover: Boolean = false,
    val resolvedCoverArtId: String? = null,
    val username: String = "tayeb",
    val isMixSheetVisible: Boolean = false,
    val selectedTransitionBridge: Pair<Track, Track>? = null,
    val mixConfig: PlaylistMixConfig = PlaylistMixConfig(),
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackController: PlaybackController,
    private val downloadRepository: DownloadRepository,
    private val playlistHistoryStore: PlaylistHistoryStore,
    private val serverConfigStore: ServerConfigStore,
    private val playlistMixStore: PlaylistMixStore,
    private val djMetadataRepository: DjMetadataRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId: String = requireNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    /**
     * Metadonnees DJ mesurees (BPM + Camelot) chargees depuis l'analyseur du
     * serveur : cle = Track.id. Vide tant que le catalogue n'est pas charge.
     */
    val djMeta: StateFlow<Map<String, DjTrackMeta>> = djMetadataRepository.meta

    private var reorderJob: Job? = null

    init {
        load()
        djMetadataRepository.ensureLoaded(viewModelScope)
        viewModelScope.launch {
            serverConfigStore.config.collect { cfg ->
                val name = cfg.username.trim().ifEmpty { "tayeb" }
                _uiState.update { it.copy(username = name) }
            }
        }
        viewModelScope.launch {
            mediaRepository.playlistSyncEvents.collect { changedId ->
                if (changedId == null || changedId == playlistId) {
                    loadSilently()
                }
            }
        }
        viewModelScope.launch {
            playlistMixStore.configs.collect { configs ->
                val cfg = configs[playlistId] ?: PlaylistMixConfig()
                _uiState.update { it.copy(mixConfig = cfg) }
            }
        }
    }

    fun load(showFullLoading: Boolean = true) {
        viewModelScope.launch {
            if (showFullLoading && _uiState.value.playlist == null) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            val hasCustom = downloadRepository.localCoverFor("playlist-$playlistId") != null
            runCatching { mediaRepository.getPlaylist(playlistId, forceRefresh = true) }
                .onSuccess { playlist ->
                    val resolvedCover = resolveCoverArtId(playlist)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            playlist = playlist,
                            sortedTracks = computeSortedTracks(playlist, it.sortOrder, it.searchQuery),
                            hasCustomCover = hasCustom,
                            resolvedCoverArtId = resolvedCover,
                        )
                    }
                }
                .onFailure { error ->
                    val resolvedCover = _uiState.value.playlist?.let { resolveCoverArtId(it) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = if (it.playlist == null) error.toUserMessage() else null,
                            hasCustomCover = hasCustom,
                            resolvedCoverArtId = resolvedCover,
                        )
                    }
                }
        }
    }

    /**
     * Navidrome's playlist detail response often omits `coverArt` even though the
     * summary carried it, and for small playlists the header could then resolve
     * no artwork at all. Fall back to the first track's cover art, then to a
     * locally cached custom cover, so the detail header always has an image.
     */
    private fun resolveCoverArtId(playlist: Playlist): String? {
        playlist.coverArtId?.takeIf { it.isNotBlank() }?.let { return it }
        if (playlist.id.isNotBlank() && playlist.id != "virtual-library") {
            return "pl-${playlist.id}"
        }
        playlist.tracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId?.let { return it }
        return downloadRepository.localCoverFor("playlist-${playlist.id}")?.let { "playlist-${playlist.id}" }
    }

    fun pullRefresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        load(showFullLoading = false)
    }

    fun loadSilently() {
        load(showFullLoading = false)
    }

    fun setSortOrder(order: PlaylistSortOrder) {
        _uiState.update {
            it.copy(
                sortOrder = order,
                sortedTracks = computeSortedTracks(it.playlist, order, it.searchQuery),
            )
        }
    }

    private fun computeSortedTracks(
        playlist: Playlist?,
        sortOrder: PlaylistSortOrder,
        query: String = "",
    ): List<Track> {
        val tracks = playlist?.tracks.orEmpty()
        val sorted = when (sortOrder) {
            PlaylistSortOrder.CUSTOM_ORDER -> tracks
            PlaylistSortOrder.RECENTLY_ADDED -> {
                if (tracks.any { !it.created.isNullOrBlank() }) {
                    tracks.sortedWith(
                        compareByDescending<Track> { it.created.orEmpty() }
                            .thenByDescending { it.year ?: 0 }
                    )
                } else {
                    tracks.reversed()
                }
            }
            PlaylistSortOrder.TITLE -> tracks.sortedBy { it.title.lowercase() }
            PlaylistSortOrder.ARTIST -> tracks.sortedBy { (it.artist ?: "").lowercase() }
            PlaylistSortOrder.ALBUM -> tracks.sortedBy { (it.album ?: "").lowercase() }
            PlaylistSortOrder.DURATION -> tracks.sortedByDescending { it.duration }
        }
        return if (query.isBlank()) {
            sorted
        } else {
            sorted.filter {
                it.title.contains(query, ignoreCase = true) ||
                    (it.artist?.contains(query, ignoreCase = true) == true) ||
                    (it.album?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    // --- Search inside playlist ---

    fun toggleSearch() {
        _uiState.update {
            val next = !it.isSearchVisible
            val newQuery = if (!next) "" else it.searchQuery
            it.copy(
                isSearchVisible = next,
                searchQuery = newQuery,
                sortedTracks = computeSortedTracks(it.playlist, it.sortOrder, newQuery),
            )
        }
    }

    fun setSearchVisible(visible: Boolean) {
        _uiState.update {
            if (it.isSearchVisible == visible) return@update it
            val newQuery = if (!visible) "" else it.searchQuery
            it.copy(
                isSearchVisible = visible,
                searchQuery = newQuery,
                sortedTracks = computeSortedTracks(it.playlist, it.sortOrder, newQuery),
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                sortedTracks = computeSortedTracks(it.playlist, it.sortOrder, query),
            )
        }
    }

    // --- Reordering & Manual Sorting ---

    fun toggleReorderMode() {
        _uiState.update {
            val newMode = !it.isReorderMode
            if (newMode) {
                // When entering reorder mode, lock into CUSTOM_ORDER and clear search filter
                it.copy(
                    isReorderMode = true,
                    sortOrder = PlaylistSortOrder.CUSTOM_ORDER,
                    sortedTracks = computeSortedTracks(it.playlist, PlaylistSortOrder.CUSTOM_ORDER, ""),
                    isSearchVisible = false,
                    searchQuery = "",
                )
            } else {
                it.copy(isReorderMode = false)
            }
        }
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val playlist = _uiState.value.playlist ?: return
        if (playlistId == "virtual-library" || playlist.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) return
        val currentTracks = playlist.tracks.toMutableList()
        if (fromIndex !in currentTracks.indices || toIndex !in currentTracks.indices || fromIndex == toIndex) return

        val item = currentTracks.removeAt(fromIndex)
        currentTracks.add(toIndex.coerceIn(0, currentTracks.size), item)

        val updatedPlaylist = playlist.copy(tracks = currentTracks)
        _uiState.update {
            it.copy(
                playlist = updatedPlaylist,
                sortOrder = PlaylistSortOrder.CUSTOM_ORDER,
                sortedTracks = computeSortedTracks(updatedPlaylist, PlaylistSortOrder.CUSTOM_ORDER, it.searchQuery),
            )
        }

        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(200L)
            val finalTrackIds = _uiState.value.playlist?.tracks?.map { it.id } ?: currentTracks.map { it.id }
            runCatching {
                mediaRepository.reorderPlaylist(playlistId, playlist.name, finalTrackIds)
            }.onFailure {
                loadSilently()
            }
        }
    }

    fun moveTrackUp(index: Int) {
        if (index > 0) moveTrack(index, index - 1)
    }

    fun moveTrackDown(index: Int) {
        val tracks = _uiState.value.playlist?.tracks.orEmpty()
        if (index in 0 until tracks.size - 1) moveTrack(index, index + 1)
    }

    fun moveTrackToTop(index: Int) {
        if (index > 0) moveTrack(index, 0)
    }

    // --- Spotify-style Edit Details Sheet ---

    fun showEditSheet() {
        val current = _uiState.value.playlist ?: return
        _uiState.update {
            it.copy(
                isEditSheetVisible = true,
                editNameText = current.name,
                editDescriptionText = current.comment.orEmpty(),
                editIsPublic = current.isPublic,
            )
        }
    }

    fun dismissEditSheet() {
        _uiState.update { it.copy(isEditSheetVisible = false) }
    }

    fun onEditNameChange(name: String) {
        _uiState.update { it.copy(editNameText = name) }
    }

    fun onEditDescriptionChange(description: String) {
        _uiState.update { it.copy(editDescriptionText = description) }
    }

    fun onEditIsPublicChange(isPublic: Boolean) {
        _uiState.update { it.copy(editIsPublic = isPublic) }
    }

    fun savePlaylistDetails() {
        val newName = _uiState.value.editNameText.trim()
        val newDesc = _uiState.value.editDescriptionText.trim()
        val newPublic = _uiState.value.editIsPublic
        val current = _uiState.value.playlist ?: return
        if (newName.isBlank() || playlistId == "virtual-library" || current.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            runCatching {
                mediaRepository.updatePlaylist(
                    playlistId = playlistId,
                    name = newName,
                    comment = newDesc,
                    isPublic = newPublic,
                )
            }.onSuccess {
                val updated = current.copy(
                    name = newName,
                    comment = newDesc,
                    isPublic = newPublic,
                )
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        isEditSheetVisible = false,
                        playlist = updated,
                        sortedTracks = computeSortedTracks(updated, it.sortOrder, it.searchQuery),
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isUpdating = false, error = e.toUserMessage()) }
            }
        }
    }

    fun togglePublicPrivate() {
        val current = _uiState.value.playlist ?: return
        if (playlistId == "virtual-library" || current.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) return
        val newPublic = !current.isPublic
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            runCatching {
                mediaRepository.updatePlaylist(
                    playlistId = playlistId,
                    name = current.name,
                    comment = current.comment,
                    isPublic = newPublic,
                )
            }.onSuccess {
                val updated = current.copy(isPublic = newPublic)
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        playlist = updated,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isUpdating = false, error = e.toUserMessage()) }
            }
        }
    }

    // Legacy rename dialog handlers kept for backwards compatibility
    fun showRenameDialog() {
        val currentName = _uiState.value.playlist?.name.orEmpty()
        _uiState.update { it.copy(isRenameDialogVisible = true, renameText = currentName) }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(isRenameDialogVisible = false) }
    }

    fun onRenameTextChange(newText: String) {
        _uiState.update { it.copy(renameText = newText) }
    }

    fun confirmRename() {
        val newName = _uiState.value.renameText.trim()
        val currentPlaylist = _uiState.value.playlist
        if (newName.isBlank() || playlistId == "virtual-library" || currentPlaylist?.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            runCatching {
                mediaRepository.updatePlaylist(playlistId, newName)
            }.onSuccess {
                val updatedPlaylist = _uiState.value.playlist?.copy(name = newName)
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        isRenameDialogVisible = false,
                        playlist = updatedPlaylist,
                        sortedTracks = computeSortedTracks(updatedPlaylist, it.sortOrder, it.searchQuery),
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isUpdating = false, error = e.toUserMessage()) }
            }
        }
    }

    fun setCustomCover(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        downloadRepository.saveCustomCover("playlist-$playlistId", stream)
                    }
                }.getOrNull() ?: false
            }
            if (ok) {
                _uiState.value = _uiState.value.copy(
                    hasCustomCover = true,
                    coverUpdateTrigger = System.currentTimeMillis(),
                )
            }
        }
    }

    fun resetToServerCover() {
        viewModelScope.launch {
            mediaRepository.resetPlaylistCover(playlistId)
            _uiState.value = _uiState.value.copy(
                hasCustomCover = false,
                coverUpdateTrigger = System.currentTimeMillis(),
            )
            loadSilently()
        }
    }

    fun removeTrack(songIndex: Int) {
        val currentTracks = _uiState.value.sortedTracks
        val currentPlaylist = _uiState.value.playlist
        if (songIndex !in currentTracks.indices || playlistId == "virtual-library" || currentPlaylist?.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) return
        val targetTrack = currentTracks[songIndex]
        val originalIndex = currentPlaylist?.tracks?.indexOfFirst { it.id == targetTrack.id }?.takeIf { it >= 0 } ?: songIndex

        val updated = currentTracks.toMutableList().apply { removeAt(songIndex) }
        val updatedPlaylist = currentPlaylist?.copy(
            tracks = updated,
            songCount = updated.size,
        )
        _uiState.value = _uiState.value.copy(
            playlist = updatedPlaylist,
            sortedTracks = updated,
        )

        viewModelScope.launch {
            runCatching {
                mediaRepository.removeFromPlaylist(playlistId, originalIndex)
            }.onFailure {
                loadSilently()
            }
        }
    }

    fun addTrack(track: Track) {
        val currentPlaylist = _uiState.value.playlist
        if (playlistId == "virtual-library" || currentPlaylist?.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) return
        viewModelScope.launch {
            runCatching {
                mediaRepository.addToPlaylist(playlistId, track.id)
            }.onSuccess {
                loadSilently()
            }
        }
    }

    suspend fun getAvailableTracks(): List<Track> {
        return runCatching { mediaRepository.getLibraryTracks() }.getOrDefault(emptyList())
    }

    fun playFrom(index: Int) {
        val tracks = _uiState.value.sortedTracks
        if (tracks.isNotEmpty() && index in tracks.indices) {
            playlistHistoryStore.recordPlayed(playlistId)
            playbackController.play(tracks, index, playlistId = playlistId)
        }
    }

    fun playShuffled() {
        val tracks = _uiState.value.sortedTracks
        if (tracks.isNotEmpty()) {
            playlistHistoryStore.recordPlayed(playlistId)
            playbackController.playShuffled(tracks, playlistId = playlistId)
        }
    }

    fun deletePlaylist(onDone: (Boolean) -> Unit) {
        val currentPlaylist = _uiState.value.playlist
        if (playlistId == "virtual-library" || currentPlaylist?.name.equals(MediaRepository.VIRTUAL_LIBRARY_NAME, ignoreCase = true)) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            val result = runCatching { mediaRepository.deletePlaylist(playlistId) }
            _uiState.value = _uiState.value.copy(isUpdating = false)
            onDone(result.isSuccess)
        }
    }

    // --- Spotify Playlist Mix (DJ Transitions) ---

    fun showMixSheet() {
        _uiState.update { it.copy(isMixSheetVisible = true) }
    }

    fun dismissMixSheet() {
        _uiState.update { it.copy(isMixSheetVisible = false) }
    }

    fun updateMixConfig(config: PlaylistMixConfig) {
        playlistMixStore.saveConfig(playlistId, config)
        _uiState.update { it.copy(mixConfig = config) }
    }

    fun toggleMixEnabled() {
        val current = _uiState.value.mixConfig
        updateMixConfig(current.copy(isEnabled = !current.isEnabled))
    }

    fun setMixMode(mode: PlaylistMixMode) {
        updateMixConfig(_uiState.value.mixConfig.copy(mode = mode))
    }

    fun setMixDuration(durationSeconds: Float) {
        updateMixConfig(_uiState.value.mixConfig.copy(durationSeconds = durationSeconds))
    }

    fun setSmartBassSwap(enabled: Boolean) {
        updateMixConfig(_uiState.value.mixConfig.copy(smartBassSwap = enabled))
    }

    fun setEqualPowerVolume(enabled: Boolean) {
        updateMixConfig(_uiState.value.mixConfig.copy(equalPowerVolume = enabled))
    }

    fun openTransitionBridgeSheet(fromTrack: Track, toTrack: Track) {
        _uiState.update { it.copy(selectedTransitionBridge = Pair(fromTrack, toTrack)) }
    }

    fun dismissTransitionBridgeSheet() {
        _uiState.update { it.copy(selectedTransitionBridge = null) }
    }

    fun setBridgeTransitionMode(fromTrackId: String, toTrackId: String, mode: PlaylistMixMode) {
        playlistMixStore.setTransitionOverride(playlistId, fromTrackId, toTrackId, mode)
    }

    fun resetBridgeTransitionMode(fromTrackId: String, toTrackId: String) {
        playlistMixStore.removeTransitionOverride(playlistId, fromTrackId, toTrackId)
    }

    fun applyBridgeModeToAll(mode: PlaylistMixMode) {
        playlistMixStore.setGlobalModeAndClearOverrides(playlistId, mode)
    }

    fun resetAllBridgeTransitions() {
        playlistMixStore.resetAllOverrides(playlistId)
    }

    /**
     * Reglages manuels de transition (in/out points) : ou le sortant quitte et ou
     * l'entrant demarre. Valeurs playlist-level, comme la duree et l'equal-power.
     */
    fun setBridgeTiming(outroOffsetMs: Long, introSkipMs: Long) {
        val current = playlistMixStore.getConfig(playlistId)
        playlistMixStore.saveConfig(
            playlistId,
            current.copy(
                outroOffsetMs = outroOffsetMs.coerceIn(-PlaylistMixConfig.MAX_OUTRO_SHIFT_MS, 0L),
                introSkipMs = introSkipMs.coerceIn(0L, PlaylistMixConfig.MAX_INTRO_SKIP_MS),
            ),
        )
    }

    fun reorderPlaylistByHarmonicFlow(onDone: () -> Unit = {}) {
        val playlist = _uiState.value.playlist ?: return
        val tracks = playlist.tracks
        if (tracks.size <= 2) return

        val sorted = sortTracksHarmonically(tracks, djMetadataRepository.meta.value)

        val updatedPlaylist = playlist.copy(tracks = sorted)
        _uiState.update {
            it.copy(
                playlist = updatedPlaylist,
                sortOrder = PlaylistSortOrder.CUSTOM_ORDER,
                sortedTracks = computeSortedTracks(updatedPlaylist, PlaylistSortOrder.CUSTOM_ORDER, it.searchQuery),
            )
        }

        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(200L)
            runCatching {
                mediaRepository.reorderPlaylist(playlistId, playlist.name, sorted.map { it.id })
            }.onFailure {
                loadSilently()
            }
            onDone()
        }
    }
}
