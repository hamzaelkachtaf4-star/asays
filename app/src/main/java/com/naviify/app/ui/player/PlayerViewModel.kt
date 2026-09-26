package com.naviify.app.ui.player

import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.data.lyrics.LyricsCandidate
import com.naviify.app.data.lyrics.LyricsPreferencesStore
import com.naviify.app.data.lyrics.LrclibLyricsClient
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.LyricsData
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlayerQueueStore
import com.naviify.app.domain.playback.PlayerUiState
import com.naviify.app.domain.playback.PlaybackRepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class LyricsUiState(
    val isLoading: Boolean = false,
    val lyrics: LyricsData? = null,
)

@Immutable
data class ArtistOverview(
    val id: String,
    val name: String,
    val coverArtId: String? = null,
    val imageUrl: String? = null,
    val biography: String? = null,
    val albumCount: Int = 0,
    val isFavorite: Boolean = false,
)

@Immutable
data class PlayerArtistState(
    val isLoading: Boolean = false,
    val artist: ArtistOverview? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val store: PlayerQueueStore,
    private val playbackController: PlaybackController,
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val downloadRepository: DownloadRepository,
    private val lyricsPreferencesStore: LyricsPreferencesStore,
    private val lrclibLyricsClient: LrclibLyricsClient,
    private val customLyricsStore: com.naviify.app.data.lyrics.CustomLyricsStore,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = store.state

    private val _lyricsState = MutableStateFlow(LyricsUiState())
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _artistState = MutableStateFlow(PlayerArtistState())
    val artistState: StateFlow<PlayerArtistState> = _artistState.asStateFlow()

    private var artistJob: Job? = null

    init {
        viewModelScope.launch {
            playbackController.connect()
        }
        viewModelScope.launch {
            store.state
                .map { it.currentTrack?.id }
                .distinctUntilChanged()
                .collectLatest { trackId ->
                    val track = store.state.value.currentTrack?.takeIf { it.id == trackId }
                    loadLyrics(track)
                    loadArtistInfo(track)
                }
        }
    }

    fun play(queue: List<Track>, startIndex: Int = 0) = playbackController.play(queue, startIndex)
    fun playShuffled(queue: List<Track>) = playbackController.playShuffled(queue)
    fun togglePlayPause() = playbackController.togglePlayPause()
    fun next() = playbackController.next()
    fun previous() = playbackController.previous()
    fun select(trackIndex: Int) = playbackController.select(trackIndex)
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun setShuffle(enabled: Boolean) = playbackController.setShuffle(enabled)
    fun reshuffleQueue() = playbackController.reshuffleQueue()
    fun moveInQueue(fromIndex: Int, toIndex: Int) = playbackController.moveInQueue(fromIndex, toIndex)
    fun startSimilarRadio(track: Track) = playbackController.playSimilarRadio(track)
    fun setRepeat(mode: PlaybackRepeatMode) = playbackController.setRepeat(mode)
    fun removeFromQueue(index: Int) = playbackController.removeFromQueue(index)
    fun appendToQueue(track: Track) = playbackController.appendToQueue(track)
    fun clear() = playbackController.clear()

    val sleepTimerRemainingSeconds: StateFlow<Long?> = playbackController.sleepTimerRemainingSeconds
    val isSleepUntilTrackEnd: StateFlow<Boolean> = playbackController.isSleepUntilTrackEnd

    fun startSleepTimer(minutes: Int) = playbackController.startSleepTimer(minutes.coerceIn(1, 120))

    fun startSleepUntilEndOfTrack() = playbackController.startSleepTimerEndOfTrack()

    fun cancelSleepTimer() = playbackController.cancelSleepTimer()

    fun setLyricsOffset(offsetMs: Long) {
        val currentLyrics = _lyricsState.value.lyrics ?: return
        val currentTrack = state.value.currentTrack ?: return
        val updated = currentLyrics.copy(offsetMs = offsetMs, isCustom = true)
        _lyricsState.value = _lyricsState.value.copy(lyrics = updated)
        viewModelScope.launch(Dispatchers.IO) {
            customLyricsStore.saveCustomLyrics(currentTrack.id, currentTrack.artist, currentTrack.title, updated)
            downloadRepository.saveLyrics(currentTrack.id, updated)
            downloadRepository.saveCompanionLrc(currentTrack.id, updated, currentTrack.title, currentTrack.artist)
        }
    }

    private suspend fun loadLyrics(track: Track?) {
        if (track == null) {
            _lyricsState.value = LyricsUiState()
            return
        }
        _lyricsState.value = LyricsUiState(isLoading = true)
        _lyricsState.value = runCatching { mediaRepository.getLyrics(track.id, track.artist, track.title, track.duration) }
            .map { LyricsUiState(lyrics = it) }
            .getOrDefault(LyricsUiState())
    }

    fun blockLyricsForCurrentTrack() {
        val track = state.value.currentTrack ?: return
        lyricsPreferencesStore.blockLyrics(track.id)
        viewModelScope.launch(Dispatchers.IO) {
            customLyricsStore.deleteCustomLyrics(track.id, track.artist, track.title)
            downloadRepository.deleteLyrics(track.id)
        }
        _lyricsState.value = LyricsUiState(lyrics = LyricsData())
    }

    fun unblockAndReloadLyrics() {
        val track = state.value.currentTrack ?: return
        lyricsPreferencesStore.unblockLyrics(track.id)
        viewModelScope.launch {
            _lyricsState.value = LyricsUiState(isLoading = true)
            val fresh = mediaRepository.refreshLyrics(track.id, track.artist, track.title, track.duration)
            _lyricsState.value = LyricsUiState(lyrics = fresh)
        }
    }

    fun searchLrclibCandidates(query: String, onResult: (List<LyricsCandidate>) -> Unit) {
        viewModelScope.launch {
            val candidates = lrclibLyricsClient.searchCandidates(query)
            onResult(candidates)
        }
    }

    fun applyCustomLyrics(candidate: LyricsCandidate) {
        val track = state.value.currentTrack ?: return
        val lyricsData = lrclibLyricsClient.candidateToLyricsData(candidate).copy(isCustom = true)
        lyricsPreferencesStore.unblockLyrics(track.id)
        _lyricsState.value = LyricsUiState(lyrics = lyricsData)
        viewModelScope.launch(Dispatchers.IO) {
            customLyricsStore.saveCustomLyrics(track.id, track.artist, track.title, lyricsData)
            downloadRepository.saveLyrics(track.id, lyricsData)
            downloadRepository.saveCompanionLrc(track.id, lyricsData, track.title, track.artist)
        }
    }

    fun reportLyricsIssue(reason: String) {
        val track = state.value.currentTrack ?: return
        lyricsPreferencesStore.reportTrack(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            reason = reason,
        )
    }

    fun isCurrentTrackLyricsBlocked(): Boolean {
        val track = state.value.currentTrack ?: return false
        return lyricsPreferencesStore.isBlocked(track.id)
    }

    fun isCurrentTrackLyricsCustom(): Boolean {
        val track = state.value.currentTrack ?: return false
        return _lyricsState.value.lyrics?.isCustom == true ||
            customLyricsStore.hasCustomLyrics(track.id, track.artist, track.title)
    }

    fun getCurrentLrcText(): String {
        val track = state.value.currentTrack ?: return ""
        val lyrics = _lyricsState.value.lyrics ?: return ""
        return lyrics.toLrcString(title = track.title, artist = track.artist)
    }

    fun resetToOriginalLyrics() {
        val track = state.value.currentTrack ?: return
        viewModelScope.launch {
            _lyricsState.value = LyricsUiState(isLoading = true)
            val fresh = mediaRepository.refreshLyrics(
                trackId = track.id,
                artist = track.artist,
                title = track.title,
                duration = track.duration,
                resetCustom = true,
            )
            _lyricsState.value = LyricsUiState(lyrics = fresh)
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val currentLyrics = _lyricsState.value.lyrics ?: return
        val currentTrack = state.value.currentTrack ?: return
        val newOffset = (currentLyrics.offsetMs + deltaMs).coerceIn(-15000L, 15000L)
        val updated = currentLyrics.copy(offsetMs = newOffset, isCustom = true)
        _lyricsState.value = _lyricsState.value.copy(lyrics = updated)
        viewModelScope.launch(Dispatchers.IO) {
            customLyricsStore.saveCustomLyrics(currentTrack.id, currentTrack.artist, currentTrack.title, updated)
            downloadRepository.saveLyrics(currentTrack.id, updated)
            downloadRepository.saveCompanionLrc(currentTrack.id, updated, currentTrack.title, currentTrack.artist)
        }
    }

    fun refreshCurrentLyrics() {
        val track = state.value.currentTrack ?: return
        viewModelScope.launch {
            _lyricsState.value = LyricsUiState(isLoading = true)
            val fresh = mediaRepository.refreshLyrics(track.id, track.artist, track.title, track.duration)
            _lyricsState.value = LyricsUiState(lyrics = fresh)
        }
    }

    fun retryCurrent() = playbackController.retryCurrent()

    private fun loadArtistInfo(track: Track?) {
        artistJob?.cancel()
        if (track == null || track.artist.isNullOrBlank()) {
            _artistState.value = PlayerArtistState()
            return
        }

        val artistName = track.artist.orEmpty().trim()
        val artistId = track.artistId?.takeIf { it.isNotBlank() }

        // 1. If currently displaying the same artist and an image is present, keep it without flashing
        val current = _artistState.value.artist
        val isSameArtist = current != null && (
            (artistId != null && current.id == artistId) ||
            current.name.equals(artistName, ignoreCase = true)
        )
        if (isSameArtist && (!current?.imageUrl.isNullOrBlank() || !current?.coverArtId.isNullOrBlank())) {
            return
        }

        // 2. Check in-memory cache
        val cached = (if (artistId != null) artistOverviewCache[artistId] else null)
            ?: artistOverviewCache[artistName.lowercase()]
        if (cached != null) {
            _artistState.value = PlayerArtistState(isLoading = false, artist = cached)
            return
        }

        // 3. Fallback immediate overview (NEVER use track.coverArtId!)
        val immediateOverview = ArtistOverview(
            id = artistId ?: artistName,
            name = artistName,
            coverArtId = null,
            imageUrl = null,
            biography = null,
            albumCount = 0,
            isFavorite = false,
        )
        _artistState.value = PlayerArtistState(isLoading = true, artist = immediateOverview)

        artistJob = viewModelScope.launch(Dispatchers.IO) {
            var overview: ArtistOverview? = null

            if (!artistId.isNullOrBlank()) {
                runCatching {
                    val detail = mediaRepository.getArtistDetail(artistId)
                    val isFav = favoritesRepository.isFavorite(artistId, FavoriteType.ARTIST)
                    overview = ArtistOverview(
                        id = detail.artist.id,
                        name = detail.artist.name,
                        coverArtId = detail.artist.coverArtId,
                        imageUrl = detail.imageUrl ?: detail.artist.coverArtId?.let { CoverUrls.url(it, 720) },
                        biography = detail.biography,
                        albumCount = detail.albums.size,
                        isFavorite = isFav,
                    )
                }
            }

            if (overview == null) {
                runCatching {
                    val rawName = track.artist.orEmpty().trim()
                    var search = mediaRepository.search(rawName, artistCount = 3, albumCount = 0, songCount = 0)
                    var found = search.artists.firstOrNull { it.name.equals(rawName, ignoreCase = true) }
                        ?: search.artists.firstOrNull()

                    if (found == null) {
                        val primary = rawName.split(",", ";", " feat.", " feat ", " ft.", " ft ", " / ", " & ")
                            .firstOrNull()?.trim().orEmpty()
                        if (primary.isNotBlank() && !primary.equals(rawName, ignoreCase = true)) {
                            search = mediaRepository.search(primary, artistCount = 3, albumCount = 0, songCount = 0)
                            found = search.artists.firstOrNull { it.name.equals(primary, ignoreCase = true) }
                                ?: search.artists.firstOrNull()
                        }
                    }

                    if (found != null) {
                        val detail = mediaRepository.getArtistDetail(found.id)
                        val isFav = favoritesRepository.isFavorite(found.id, FavoriteType.ARTIST)
                        overview = ArtistOverview(
                            id = detail.artist.id,
                            name = detail.artist.name,
                            coverArtId = detail.artist.coverArtId,
                            imageUrl = detail.imageUrl ?: detail.artist.coverArtId?.let { CoverUrls.url(it, 720) },
                            biography = detail.biography,
                            albumCount = detail.albums.size,
                            isFavorite = isFav,
                        )
                    }
                }
            }

            if (overview != null) {
                if (artistId != null) {
                    artistOverviewCache.put(artistId, overview!!)
                }
                artistOverviewCache.put(overview!!.id, overview!!)
                artistOverviewCache.put(overview!!.name.lowercase(), overview!!)
                _artistState.value = PlayerArtistState(isLoading = false, artist = overview)
            } else {
                _artistState.value = PlayerArtistState(isLoading = false, artist = immediateOverview)
            }
        }
    }

    fun toggleFavoriteArtist() {
        val currentArtist = _artistState.value.artist ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(
                id = currentArtist.id,
                type = FavoriteType.ARTIST,
                name = currentArtist.name,
                coverArtId = currentArtist.coverArtId,
            )
            val isFav = favoritesRepository.isFavorite(currentArtist.id, FavoriteType.ARTIST)
            _artistState.update { it.copy(artist = it.artist?.copy(isFavorite = isFav)) }
        }
    }

    companion object {
        private val artistOverviewCache = LruCache<String, ArtistOverview>(100)
    }
}
