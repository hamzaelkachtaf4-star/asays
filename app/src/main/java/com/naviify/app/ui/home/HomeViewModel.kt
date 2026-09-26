package com.naviify.app.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.network.ServerUrlRouter
import com.naviify.app.core.network.normalizeServerUrl
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.core.storage.ServerMode
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.data.repository.AlbumListType
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.FavoriteType
import com.naviify.app.domain.model.Playlist
import com.naviify.app.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import javax.inject.Inject

@Immutable
data class QuickGridItem(
    val id: String,
    val title: String,
    val coverArtId: String? = null,
    val playlistId: String? = null,
    val albumId: String? = null,
    val isPlaylist: Boolean = false,
)

@Immutable
data class HomeUiState(
    val greeting: String = "",
    val quickGridItems: List<QuickGridItem> = emptyList(),
    val userPlaylists: List<Playlist> = emptyList(),
    val quickPicks: List<Album> = emptyList(),
    val recentlyAdded: List<Album> = emptyList(),
    val featuredArtists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val serverLabel: String = "Offline",
    val serverReachable: Boolean = false,
    val isOfflineMode: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val serverConfigStore: ServerConfigStore,
    private val serverUrlRouter: ServerUrlRouter,
    private val playbackController: PlaybackController,
    private val downloadRepository: DownloadRepository,
    private val radioRepository: RadioRepository,
) : ViewModel() {

    companion object {
        @Volatile
        private var cachedHomeUiState: HomeUiState? = null

        /** Date du dernier chargement reussi (survit a la recreation du ViewModel). */
        @Volatile
        private var cachedAtMs: Long = 0L

        /**
         * Duree pendant laquelle le contenu deja a l'ecran est considere comme frais :
         * en dessous, revenir sur l'accueil n'entraine aucune requete reseau et aucune
         * reconstruction complete de l'ecran.
         */
        private const val FRESHNESS_MS = 60_000L
    }

    private val _uiState = MutableStateFlow(cachedHomeUiState ?: HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Stations "radio" du jour (cartes de l'accueil), regenerees chaque jour cote serveur. */
    val radioStations: StateFlow<List<RadioStation>> = radioRepository.stations

    private var loadJob: Job? = null

    init {
        // Stations du jour : rechargees si elles manquent ou si la date a change.
        radioRepository.ensureLoaded(viewModelScope)
        viewModelScope.launch {
            combine(
                serverConfigStore.config,
                serverUrlRouter.effectiveUrl,
            ) { config, effective -> config to effective }
                .collect { (config, effective) ->
                    val home = normalizeServerUrl(config.homeServerUrl.ifBlank { com.naviify.app.core.storage.ServerConfig.DEFAULT_HOME_URL })
                    val isHome = home.isNotBlank() && effective.startsWith(home)
                    val isOffline = config.activeServerMode == ServerMode.OFFLINE
                    _uiState.value = _uiState.value.copy(
                        serverLabel = if (isOffline || effective.isBlank()) "Offline" else if (isHome) "Home LAN" else "Tailscale",
                        serverReachable = !isOffline && effective.isNotBlank(),
                        isOfflineMode = isOffline,
                    )
                }
        }
        viewModelScope.launch {
            downloadRepository.observeDownloads()
                .map { entities ->
                    // Only completed downloads can change the offline library, so
                    // in-flight progress ticks no longer trigger a reload.
                    entities.filter { it.status == DownloadEntity.STATUS_DONE }
                        .map { it.trackId }
                        .sorted()
                }
                .distinctUntilChanged()
                // Le filtre/tri/comparaison reste hors du thread principal : chaque
                // progression de telechargement declenchait sinon du travail UI.
                .flowOn(Dispatchers.Default)
                .collect {
                    if (_uiState.value.isOfflineMode) {
                        loadOfflineState()
                    }
                }
        }
        load()
    }

    fun toggleOffline() {
        viewModelScope.launch {
            val current = serverConfigStore.config.first()
            if (current.activeServerMode == ServerMode.OFFLINE) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, isOfflineMode = false)
                serverUrlRouter.switchToSuspend(ServerMode.AUTO)
                load()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, isOfflineMode = true, serverReachable = false, serverLabel = "Offline")
                serverUrlRouter.switchTo(ServerMode.OFFLINE)
                loadOfflineState()
            }
        }
    }

    /**
     * Lance une station "radio" du jour : lecture melangee, comme Spotify.
     * Les titres viennent directement du payload du serveur, aucune requete Subsonic.
     */
    fun playStation(station: RadioStation) {
        val tracks = station.tracks
        if (tracks.isEmpty()) return
        playbackController.playShuffled(tracks, playlistId = "radio:${station.name}")
    }

    fun load() = loadInternal(force = false)

    /** Rechargement complet demande par l'utilisateur (bouton "Reessayer"). */
    fun refresh() = loadInternal(force = true)

    private fun loadInternal(force: Boolean) {
        val cached = cachedHomeUiState
        // Sortie rapide : le contenu est deja affiche et encore frais. Sans ce garde-fou,
        // chaque retour sur l'accueil relancait 5 requetes reseau + la reconstruction
        // complete de l'ecran (et de toutes ses images).
        if (!force && cached != null && System.currentTimeMillis() - cachedAtMs < FRESHNESS_MS) {
            if (_uiState.value.isLoading) _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val config = serverConfigStore.config.first()
            val isOffline = config.activeServerMode == ServerMode.OFFLINE || serverUrlRouter.effectiveSync().isBlank()
            if (isOffline) {
                loadOfflineState()
                return@launch
            }

            // If we don't have content yet, show loading indicator; otherwise refresh silently in background.
            val hasContent = _uiState.value.quickGridItems.isNotEmpty() || _uiState.value.userPlaylists.isNotEmpty()
            if (!hasContent) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(error = null)
            }

            runCatching {
                coroutineScope {
                    val favoriteAlbumsDeferred = async(Dispatchers.IO) {
                        favoritesRepository.observeFavorites()
                            .first()
                            .filter { it.type == FavoriteType.ALBUM }
                            .map { favorite ->
                                Album(
                                    id = favorite.id,
                                    name = favorite.name,
                                    artist = favorite.secondaryText,
                                    artistId = null,
                                    coverArtId = favorite.coverArtId,
                                    songCount = 0,
                                    duration = 0,
                                    year = null,
                                    isFavorite = true,
                                )
                            }
                    }
                    val randomDeferred = async(Dispatchers.IO) {
                        mediaRepository.getAlbums(AlbumListType.RANDOM, size = 12)
                    }
                    val newestDeferred = async(Dispatchers.IO) {
                        mediaRepository.getAlbums(AlbumListType.NEWEST, size = 20)
                    }
                    val artistsDeferred = async(Dispatchers.IO) {
                        mediaRepository.getArtists()
                    }
                    val playlistsDeferred = async(Dispatchers.IO) {
                        runCatching {
                            mediaRepository.getPlaylists()
                                .filter { it.id != MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID }
                        }.getOrDefault(emptyList())
                    }

                    val favoriteAlbums = favoriteAlbumsDeferred.await()
                    val random = randomDeferred.await()
                    val newest = newestDeferred.await()
                    val rawArtists = artistsDeferred.await()
                    val userPlaylists = playlistsDeferred.await()

                    // Heavy artist sorting executed on Dispatchers.Default, never blocking the main UI thread
                    val artists = withContext(Dispatchers.Default) {
                        rawArtists.sortedByDescending { it.albumCount }.take(12)
                    }

                    // Build Spotify 6-Grid Quick Access prioritizing user playlists
                    val quickGrid = mutableListOf<QuickGridItem>()
                    userPlaylists.take(6).forEach { pl ->
                        quickGrid.add(
                            QuickGridItem(
                                id = pl.id,
                                title = pl.name,
                                coverArtId = pl.coverArtId,
                                playlistId = pl.id,
                                isPlaylist = true,
                            )
                        )
                    }
                    val albumPicks = (favoriteAlbums + random).distinctBy { it.id }
                    albumPicks.take(6 - quickGrid.size).forEach { alb ->
                        quickGrid.add(
                            QuickGridItem(
                                id = alb.id,
                                title = alb.name,
                                coverArtId = alb.coverArtId,
                                albumId = alb.id,
                                isPlaylist = false,
                            )
                        )
                    }

                    HomeUiState(
                        greeting = greetingKey(LocalTime.now().hour),
                        quickGridItems = quickGrid.take(6),
                        userPlaylists = userPlaylists,
                        quickPicks = albumPicks.take(12),
                        recentlyAdded = newest,
                        featuredArtists = artists,
                        isLoading = false,
                    )
                }
            }.onSuccess { loaded ->
                _uiState.update { current ->
                    val finalState = loaded.copy(
                        serverLabel = current.serverLabel,
                        serverReachable = current.serverReachable,
                        isOfflineMode = current.isOfflineMode,
                    )
                    cachedHomeUiState = finalState
                    cachedAtMs = System.currentTimeMillis()
                    finalState
                }
            }.onFailure { error ->
                if (_uiState.value.isOfflineMode) {
                    loadOfflineState()
                } else {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            error = error.toUserMessage(),
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadOfflineState() {
        val downloads = downloadRepository.observeDownloads().first()
            .filter { it.status == DownloadEntity.STATUS_DONE }

        val offlineAlbums = downloads
            .groupBy { it.album ?: it.title }
            .map { (name, tracks) ->
                val first = tracks.first()
                Album(
                    id = first.albumId ?: "offline-${first.trackId}",
                    name = name,
                    artist = first.artist,
                    artistId = null,
                    coverArtId = tracks.firstOrNull { !it.coverArtId.isNullOrBlank() }?.coverArtId,
                    songCount = tracks.size,
                    duration = 0,
                    year = null,
                    isFavorite = false,
                )
            }

        val offlineArtists = downloads
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

        // Only downloaded playlists
        val offlinePlaylists = runCatching {
            mediaRepository.getPlaylists()
                .filter { it.id != MediaRepository.VIRTUAL_LIBRARY_PLAYLIST_ID }
        }.getOrDefault(emptyList())

        val quickGrid = mutableListOf<QuickGridItem>()
        offlinePlaylists.take(6).forEach { pl ->
            quickGrid.add(
                QuickGridItem(
                    id = pl.id,
                    title = pl.name,
                    coverArtId = pl.coverArtId,
                    playlistId = pl.id,
                    isPlaylist = true,
                )
            )
        }
        offlineAlbums.take(6 - quickGrid.size).forEach { alb ->
            quickGrid.add(
                QuickGridItem(
                    id = alb.id,
                    title = alb.name,
                    coverArtId = alb.coverArtId,
                    albumId = alb.id,
                    isPlaylist = false,
                )
            )
        }

        _uiState.update { current ->
            val offlineState = current.copy(
                greeting = greetingKey(LocalTime.now().hour),
                quickGridItems = quickGrid.take(6),
                userPlaylists = offlinePlaylists,
                quickPicks = offlineAlbums,
                recentlyAdded = offlineAlbums,
                featuredArtists = offlineArtists,
                isLoading = false,
                error = null,
                isOfflineMode = current.isOfflineMode,
                serverReachable = current.serverReachable,
                serverLabel = current.serverLabel,
            )
            cachedHomeUiState = offlineState
            cachedAtMs = System.currentTimeMillis()
            offlineState
        }
    }

    fun playPlaylist(playlist: Playlist) {
        if (playlist.tracks.isNotEmpty()) {
            playbackController.play(playlist.tracks, 0)
        } else {
            viewModelScope.launch {
                runCatching {
                    val full = mediaRepository.getPlaylist(playlist.id)
                    if (full.tracks.isNotEmpty()) {
                        playbackController.play(full.tracks, 0)
                    }
                }
            }
        }
    }

    fun playAlbum(albumId: String) {
        viewModelScope.launch {
            runCatching {
                val detail = mediaRepository.getAlbumDetail(albumId)
                if (detail.tracks.isNotEmpty()) {
                    playbackController.play(detail.tracks, 0)
                }
            }
        }
    }

    fun playQuickGridItem(item: QuickGridItem) {
        if (item.isPlaylist && item.playlistId != null) {
            val cached = _uiState.value.userPlaylists.firstOrNull { it.id == item.playlistId }
            if (cached != null) {
                playPlaylist(cached)
            } else {
                viewModelScope.launch {
                    runCatching {
                        val pl = mediaRepository.getPlaylist(item.playlistId)
                        playPlaylist(pl)
                    }
                }
            }
        } else if (item.albumId != null) {
            playAlbum(item.albumId)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun greetingKey(hour: Int): String = when (hour) {
        in 5..11 -> "morning"
        in 12..17 -> "afternoon"
        else -> "evening"
    }
}
