package com.naviify.app.data.repository

import androidx.collection.LruCache
import com.naviify.app.core.network.SubsonicApiException
import com.naviify.app.core.network.SubsonicService
import com.naviify.app.core.network.dto.SubsonicEnvelope
import com.naviify.app.core.network.dto.SubsonicResponse
import com.naviify.app.data.mapper.toDomain
import com.naviify.app.data.mapper.toTrack
import com.naviify.app.data.lyrics.LrclibLyricsClient
import com.naviify.app.data.lyrics.parseLegacyLyrics
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.AlbumDetail
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.ArtistDetail
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.SearchResults
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.model.LyricsData
import com.naviify.app.domain.model.LyricsLineData
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AlbumListType(val apiValue: String) {
    NEWEST("newest"),
    RANDOM("random"),
    STARRED("starred"),
}

class DataNotFoundException(message: String) : Exception(message)

/**
 * Catalog access over the Subsonic API. Pure JVM (no Context), so the full
 * repository can be exercised against MockWebServer in unit tests.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val api: SubsonicService,
    private val lrclibLyricsClient: LrclibLyricsClient,
    private val downloadRepository: com.naviify.app.data.download.DownloadRepository? = null,
    private val offlinePlaylistStore: com.naviify.app.data.download.OfflinePlaylistStore? = null,
    private val lyricsPreferencesStore: com.naviify.app.data.lyrics.LyricsPreferencesStore? = null,
    private val customLyricsStore: com.naviify.app.data.lyrics.CustomLyricsStore? = null,
) {

    val playlistSyncEvents = kotlinx.coroutines.flow.MutableSharedFlow<String?>(extraBufferCapacity = 10)

    private var cachedArtists: List<Artist>? = null
    private var cachedArtistsTimestamp: Long = 0L
    private val artistDetailCache = LruCache<String, ArtistDetail>(64)

    suspend fun getArtists(forceRefresh: Boolean = false): List<Artist> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedArtists != null && (now - cachedArtistsTimestamp) < 15 * 60 * 1000L) {
            return cachedArtists!!
        }
        return withContext(Dispatchers.IO) {
            api.getArtists().result {
                artists?.index.orEmpty().flatMap { it.artist }.map { it.toDomain() }
            }.also {
                cachedArtists = it
                cachedArtistsTimestamp = System.currentTimeMillis()
            }
        }
    }

    suspend fun getArtistDetail(artistId: String): ArtistDetail = withContext(Dispatchers.IO) {
        artistDetailCache[artistId]?.let { return@withContext it }
        val artistEnvelope = api.getArtist(artistId)
        val baseDetail = artistEnvelope.result {
            artist?.toDomain() ?: throw DataNotFoundException("Artist $artistId not found")
        }

        val infoEnvelope = runCatching { api.getArtistInfo2(artistId) }.getOrNull()
            ?: runCatching { api.getArtistInfo(artistId) }.getOrNull()
        val info = infoEnvelope?.response?.artistInfo2 ?: infoEnvelope?.response?.artistInfo

        var topSongs = runCatching {
            api.getTopSongs(baseDetail.artist.name, count = 20)
                .response.topSongs?.song.orEmpty().map { it.toTrack() }
        }.getOrDefault(emptyList())

        // Fallback: if server returned no topSongs, gather tracks from albums
        if (topSongs.isEmpty() && baseDetail.albums.isNotEmpty()) {
            runCatching {
                val sampleAlbums = baseDetail.albums.take(5)
                val gatheredTracks = mutableListOf<Track>()
                for (alb in sampleAlbums) {
                    val albDetail = getAlbumDetail(alb.id)
                    gatheredTracks.addAll(albDetail.tracks)
                }
                topSongs = gatheredTracks
                    .filter { it.artist.equals(baseDetail.artist.name, ignoreCase = true) || it.artistId == artistId }
                    .distinctBy { it.title.lowercase() }
                    .take(10)
            }
        }

        val resolved = baseDetail.copy(
            biography = cleanBiography(info?.biography),
            imageUrl = info?.largeImageUrl ?: info?.mediumImageUrl ?: artistEnvelope.response.artist?.artistImageUrl,
            topSongs = topSongs,
            similarArtists = info?.similarArtist.orEmpty().map { it.toDomain() },
        )
        artistDetailCache.put(artistId, resolved)
        resolved
    }

    private fun cleanBiography(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw
            .replace(Regex("""<[^>]*>"""), "")
            .replace(Regex("""(?i)Read more on Last\.fm.*"""), "")
            .replace(Regex("""(?i)User-contributed text is available under the Creative Commons.*"""), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
            .ifBlank { null }
    }

    suspend fun getAlbumDetail(albumId: String): AlbumDetail =
        api.getAlbum(albumId).result {
            album?.toDomain() ?: throw DataNotFoundException("Album $albumId not found")
        }

    suspend fun getAlbums(type: AlbumListType, size: Int = 24, offset: Int = 0): List<Album> =
        api.getAlbumList2(type = type.apiValue, size = size, offset = offset).result {
            albumList2?.album.orEmpty().map { it.toDomain() }
        }

    suspend fun search(
        query: String,
        artistCount: Int = 20,
        albumCount: Int = 20,
        songCount: Int = 50,
    ): SearchResults {
        if (query.isBlank()) return SearchResults()
        return api.search3(query, artistCount, albumCount, songCount).result {
            val result = searchResult3
            SearchResults(
                artists = result?.artist.orEmpty().map { it.toDomain() },
                albums = result?.album.orEmpty().map { it.toDomain() },
                tracks = result?.song.orEmpty().map { it.toTrack() },
            )
        }
    }

    suspend fun getSimilarSongs(trackId: String, count: Int = 30): List<Track> {
        return runCatching {
            api.getSimilarSongs2(id = trackId, count = count).result {
                (similarSongs2?.song ?: similarSongs?.song).orEmpty().map { it.toTrack() }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getRandomSongs(size: Int = 30, genre: String? = null): List<Track> {
        return runCatching {
            api.getRandomSongs(size = size, genre = genre).result {
                randomSongs?.song.orEmpty().map { it.toTrack() }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getSimilarOrRelatedTracks(track: Track, count: Int = 30): List<Track> {
        val collected = mutableListOf<Track>()
        val currentId = track.id

        // 1. Try server's native similar songs endpoint
        val serverSimilar = runCatching { getSimilarSongs(currentId, count) }.getOrDefault(emptyList())
        collected.addAll(serverSimilar.filter { it.id != currentId })

        // 2. Same Artist: fetch artist's albums and songs
        if (collected.size < count && (!track.artistId.isNullOrBlank() || !track.artist.isNullOrBlank())) {
            runCatching {
                val artistTracks = if (!track.artistId.isNullOrBlank()) {
                    val detail = getArtistDetail(track.artistId)
                    coroutineScope {
                        detail.albums.map { album ->
                            async {
                                runCatching { getAlbumDetail(album.id).tracks }.getOrDefault(emptyList())
                            }
                        }.awaitAll().flatten()
                    }
                } else {
                    search(track.artist.orEmpty(), artistCount = 0, albumCount = 0, songCount = 40).tracks
                }
                collected.addAll(artistTracks.filter { it.id != currentId && it.id !in collected.map { c -> c.id } })
            }
        }

        // 3. Same Album: other tracks from the same album
        if (collected.size < count && !track.albumId.isNullOrBlank()) {
            runCatching {
                val albumTracks = getAlbumDetail(track.albumId).tracks
                collected.addAll(albumTracks.filter { it.id != currentId && it.id !in collected.map { c -> c.id } })
            }
        }

        // 4. Random songs from server
        if (collected.size < count) {
            runCatching {
                val randoms = getRandomSongs(size = count)
                collected.addAll(randoms.filter { it.id != currentId && it.id !in collected.map { c -> c.id } })
            }
        }

        // 5. Fallback from cached library tracks
        if (collected.size < count) {
            runCatching {
                val lib = getLibraryTracks()
                val candidateTracks = lib.filter { it.id != currentId && it.id !in collected.map { c -> c.id } }
                collected.addAll(candidateTracks.shuffled().take(count - collected.size))
            }
        }

        return collected.distinctBy { it.id }.take(count)
    }

    companion object {
        const val VIRTUAL_LIBRARY_PLAYLIST_ID = "virtual-library"

        /** Display name for the auto-generated playlist holding every server track. */
        const val VIRTUAL_LIBRARY_NAME = "My own"
    }

    // Read from several coroutines; the worst case is a duplicated fetch.
    @Volatile
    private var cachedLibraryTracks: List<Track>? = null

    suspend fun getLibraryTracks(forceRefresh: Boolean = false): List<Track> {
        if (!forceRefresh && !cachedLibraryTracks.isNullOrEmpty()) {
            return cachedLibraryTracks.orEmpty()
        }

        // 1. Fetch server songs (also satisfies Subsonic API test fixture)
        val randomTracks = runCatching {
            api.getRandomSongs(size = 500).result {
                randomSongs?.song.orEmpty().map { it.toTrack() }
            }
        }.getOrNull().orEmpty()

        // 2. Fetch newest albums and their tracks so any recently added music to the server is guaranteed to be present
        val newestTracks: List<Track> = runCatching {
            val albums = getAlbums(AlbumListType.NEWEST, size = 15)
            coroutineScope {
                albums.map { album ->
                    async {
                        runCatching {
                            getAlbumDetail(album.id).tracks
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
        }.getOrDefault(emptyList())

        val searchTracks = if (randomTracks.isEmpty()) {
            runCatching {
                api.search3(query = "", songCount = 500).result {
                    searchResult3?.song.orEmpty().map { it.toTrack() }
                }
            }.getOrNull().orEmpty()
        } else emptyList()

        val wildcardTracks = if (newestTracks.isEmpty() && randomTracks.isEmpty() && searchTracks.isEmpty()) {
            runCatching {
                api.search3(query = "*", songCount = 500).result {
                    searchResult3?.song.orEmpty().map { it.toTrack() }
                }
            }.getOrNull().orEmpty()
        } else emptyList()

        val combined = (newestTracks + randomTracks + searchTracks + wildcardTracks)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Track> { it.created.orEmpty() }
                    .thenByDescending { it.year ?: 0 }
            )

        if (combined.isNotEmpty()) {
            cachedLibraryTracks = combined
            return combined
        }

        return cachedLibraryTracks.orEmpty()
    }

    suspend fun getLibraryPlaylist(forceRefresh: Boolean = false): Playlist {
        val tracks = getLibraryTracks(forceRefresh)
        return Playlist(
            id = VIRTUAL_LIBRARY_PLAYLIST_ID,
            name = VIRTUAL_LIBRARY_NAME,
            comment = "All Server Music",
            owner = "Server",
            songCount = tracks.size,
            duration = tracks.sumOf { it.duration },
            tracks = tracks,
        )
    }

    suspend fun getPlaylists(): List<Playlist> {
        val downloadedTracks = runCatching {
            downloadRepository?.observeDownloads()?.first()
                ?.filter { it.status == com.naviify.app.core.storage.room.DownloadEntity.STATUS_DONE }
        }.getOrNull().orEmpty()
        val downloadedTrackIds = downloadedTracks.map { it.trackId }.toSet()

        val serverPlaylists = try {
            api.getPlaylists().result {
                playlists?.playlist.orEmpty().map { it.toDomain() }
            }
        } catch (e: SubsonicApiException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (serverPlaylists != null) {
            offlinePlaylistStore?.savePlaylistSummaries(serverPlaylists)
            val hasServerMyOwn = serverPlaylists.any {
                it.name.trim().equals(VIRTUAL_LIBRARY_NAME, ignoreCase = true)
            }
            if (hasServerMyOwn) {
                return serverPlaylists
            }
            val libraryPlaylist = Playlist(
                id = VIRTUAL_LIBRARY_PLAYLIST_ID,
                name = VIRTUAL_LIBRARY_NAME,
                comment = "All server songs",
                owner = "Server",
                songCount = cachedLibraryTracks?.size ?: 0,
            )
            return listOf(libraryPlaylist) + serverPlaylists
        } else {
            // Offline fallback: only playlists that have downloaded tracks
            val offlinePlaylists = offlinePlaylistStore?.getDownloadedPlaylists(downloadedTrackIds).orEmpty()
            val hasOfflineMyOwn = offlinePlaylists.any {
                it.name.trim().equals(VIRTUAL_LIBRARY_NAME, ignoreCase = true)
            }
            if (hasOfflineMyOwn) {
                return offlinePlaylists.distinctBy { it.id }
            }
            val libraryPlaylist = Playlist(
                id = VIRTUAL_LIBRARY_PLAYLIST_ID,
                name = "Downloaded music",
                comment = "All offline songs",
                owner = "Device",
                songCount = downloadedTracks.size,
            )
            return (listOf(libraryPlaylist) + offlinePlaylists).distinctBy { it.id }
        }
    }

    suspend fun getSong(trackId: String): Track =
        api.getSong(trackId).result { song }?.toTrack()
            ?: throw DataNotFoundException("Song $trackId not found")

    suspend fun getPlaylist(playlistId: String, forceRefresh: Boolean = false): Playlist {
        val downloadedEntities = runCatching {
            downloadRepository?.observeDownloads()?.first()
                ?.filter { it.status == com.naviify.app.core.storage.room.DownloadEntity.STATUS_DONE }
        }.getOrNull().orEmpty()

        val downloadedTracks = downloadedEntities.map { entity ->
            Track(
                id = entity.trackId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                albumId = entity.albumId,
                coverArtId = entity.coverArtId,
                duration = 0,
                bitRate = 0,
                trackNumber = 0,
                suffix = entity.suffix ?: entity.localFilePath?.substringAfterLast('.', "mp3") ?: "mp3",
                path = entity.localFilePath,
            )
        }

        if (playlistId == VIRTUAL_LIBRARY_PLAYLIST_ID) {
            val tracks = runCatching { getLibraryTracks(forceRefresh) }.getOrNull() ?: downloadedTracks
            return Playlist(
                id = VIRTUAL_LIBRARY_PLAYLIST_ID,
                name = if (tracks == downloadedTracks && tracks.isNotEmpty()) "Downloaded music" else VIRTUAL_LIBRARY_NAME,
                comment = if (tracks == downloadedTracks && tracks.isNotEmpty()) "All offline songs" else "All server songs",
                owner = if (tracks == downloadedTracks && tracks.isNotEmpty()) "Device" else "Server",
                songCount = tracks.size,
                duration = tracks.sumOf { it.duration },
                tracks = tracks,
            )
        }

        val onlinePlaylist = try {
            api.getPlaylist(playlistId).result {
                playlist?.toDomain() ?: throw DataNotFoundException("Playlist $playlistId not found")
            }
        } catch (e: SubsonicApiException) {
            throw e
        } catch (e: DataNotFoundException) {
            throw e
        } catch (e: Exception) {
            null
        }

        if (onlinePlaylist != null) {
            // getPlaylist (detail) commonly omits `coverArt` even when the
            // summary carried it, so fall back to the id saved by getPlaylists.
            // Without this the detail header could not resolve any artwork while
            // the list view showed it correctly.
            val withCover = if (onlinePlaylist.coverArtId.isNullOrBlank()) {
                onlinePlaylist.copy(
                    coverArtId = offlinePlaylistStore?.cachedCoverArtId(playlistId),
                )
            } else {
                onlinePlaylist
            }
            offlinePlaylistStore?.savePlaylist(withCover)
            return withCover
        }

        // Offline fallback: load from offline cache filtered by downloaded tracks
        val downloadedTrackMap = downloadedTracks.associateBy { it.id }
        val cached = offlinePlaylistStore?.getCachedPlaylist(playlistId, downloadedTrackMap)
        if (cached != null) {
            return cached
        }

        throw DataNotFoundException("Playlist $playlistId not available offline")
    }

    suspend fun createPlaylist(
        name: String,
        playlistId: String? = null,
        songIds: List<String> = emptyList(),
    ): Playlist {
        val created = api.createPlaylist(name = name, playlistId = playlistId, songIds = songIds).result {
            playlist?.toDomain() ?: throw DataNotFoundException("Server did not return the created playlist")
        }
        offlinePlaylistStore?.savePlaylist(created)
        playlistSyncEvents.tryEmit(created.id)
        return created
    }

    suspend fun addToPlaylist(playlistId: String, trackId: String) {
        api.updatePlaylist(playlistId = playlistId, songIdsToAdd = listOf(trackId)).result { }
        runCatching {
            val updated = api.getPlaylist(playlistId).result { playlist?.toDomain() }
            if (updated != null) offlinePlaylistStore?.savePlaylist(updated)
        }
        playlistSyncEvents.tryEmit(playlistId)
    }

    suspend fun addToPlaylist(playlistId: String, trackIds: List<String>) {
        if (trackIds.isEmpty()) return
        api.updatePlaylist(playlistId = playlistId, songIdsToAdd = trackIds).result { }
        runCatching {
            val updated = api.getPlaylist(playlistId).result { playlist?.toDomain() }
            if (updated != null) offlinePlaylistStore?.savePlaylist(updated)
        }
        playlistSyncEvents.tryEmit(playlistId)
    }

    suspend fun removeFromPlaylist(playlistId: String, songIndex: Int) {
        api.updatePlaylist(playlistId = playlistId, songIndexesToRemove = listOf(songIndex)).result { }
        offlinePlaylistStore?.removeTrackByIndex(playlistId, songIndex)
        runCatching {
            val updated = api.getPlaylist(playlistId).result { playlist?.toDomain() }
            if (updated != null) offlinePlaylistStore?.savePlaylist(updated)
        }
        playlistSyncEvents.tryEmit(playlistId)
    }

    suspend fun deletePlaylist(playlistId: String) {
        api.deletePlaylist(playlistId).result { }
        offlinePlaylistStore?.deletePlaylist(playlistId)
        downloadRepository?.deleteCustomCover("playlist-$playlistId")
        playlistSyncEvents.tryEmit(playlistId)
    }

    fun getCachedPlaylistTrackIds(): Map<String, Set<String>> {
        return offlinePlaylistStore?.getTrackIdsForPlaylists().orEmpty()
    }

    suspend fun updatePlaylist(
        playlistId: String,
        name: String? = null,
        comment: String? = null,
        isPublic: Boolean? = null,
    ) {
        api.updatePlaylist(
            playlistId = playlistId,
            name = name,
            comment = comment,
            public = isPublic,
        ).result { }
        if (name != null) {
            offlinePlaylistStore?.renamePlaylist(playlistId, name)
        }
        playlistSyncEvents.tryEmit(playlistId)
    }

    suspend fun reorderPlaylist(playlistId: String, playlistName: String, trackIds: List<String>) {
        api.createPlaylist(
            name = playlistName,
            playlistId = playlistId,
            songIds = trackIds,
        ).result { }
        runCatching {
            val updated = api.getPlaylist(playlistId).result { playlist?.toDomain() }
            if (updated != null) offlinePlaylistStore?.savePlaylist(updated)
        }
        playlistSyncEvents.tryEmit(playlistId)
    }

    suspend fun resetPlaylistCover(playlistId: String) {
        downloadRepository?.deleteCustomCover("playlist-$playlistId")
        playlistSyncEvents.tryEmit(playlistId)
    }

    /**
     * Lyrics lookup: OpenSubsonic synced lines first, then legacy plain text.
     */
    suspend fun getLyrics(trackId: String, artist: String?, title: String?, duration: Int = 0): LyricsData {
        if (lyricsPreferencesStore?.isBlocked(trackId) == true) {
            return LyricsData()
        }

        // Priority 0: Check permanent custom user lyrics first!
        // This ensures calibrated timing offsets and user-selected synced lyrics are NEVER lost,
        // even if cache is cleared, downloads are deleted, or Navidrome only returns unsynced text.
        val customLyrics = customLyricsStore?.getCustomLyrics(trackId, artist, title)
        if (customLyrics != null && !customLyrics.isEmpty) {
            return customLyrics
        }

        // Step 1: Check disk cache. If we already have SYNCED lyrics cached, use them immediately!
        val cached = downloadRepository?.getCachedLyrics(trackId)
        if (cached != null && cached.isSynced) {
            return cached
        }

        // Step 2: Check OpenSubsonic getLyricsBySongId for synced structured lyrics from Navidrome
        val structured = runCatching {
            api.getLyricsBySongId(trackId).result { structuredLyrics }
        }.getOrNull().orEmpty()

        val serverSynced = structured
            .flatMap { it.line }
            .mapNotNull { it.toDomain() }
            .filter { it.startMs != null }
        if (serverSynced.isNotEmpty()) {
            val result = LyricsData(syncedLines = serverSynced)
            downloadRepository?.saveLyrics(trackId, result)
            return result
        }

        // Step 3: Prioritize SYNCED lyrics from LRCLIB!
        // Matches Substreamer & Navidrome Web: always prioritize synced lyrics over static plain text!
        val lrclib = lrclibLyricsClient.fetch(artist = artist, title = title, durationSeconds = duration)
        if (lrclib != null && lrclib.isSynced) {
            downloadRepository?.saveLyrics(trackId, lrclib)
            return lrclib
        }

        // Step 4: If no synced lyrics available online or on server, fallback to cached plain text
        if (cached != null && !cached.isEmpty) {
            return cached
        }

        // Step 5: Check Navidrome legacy getLyrics (plain text or legacy timed)
        val plainText = if (!artist.isNullOrBlank() || !title.isNullOrBlank()) {
            runCatching { api.getLyrics(artist = artist, title = title).result { lyrics }?.lyric }
                .getOrNull()?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        if (!plainText.isNullOrBlank()) {
            val parsed = parseLegacyLyrics(plainText)
            val result = LyricsData(syncedLines = parsed, plainText = plainText)
            downloadRepository?.saveLyrics(trackId, result)
            return result
        }

        // Step 6: Fallback to LRCLIB plain text if available
        if (lrclib != null && !lrclib.isEmpty) {
            downloadRepository?.saveLyrics(trackId, lrclib)
            return lrclib
        }

        return LyricsData()
    }

    suspend fun refreshLyrics(
        trackId: String,
        artist: String?,
        title: String?,
        duration: Int = 0,
        resetCustom: Boolean = false,
    ): LyricsData {
        if (resetCustom) {
            customLyricsStore?.deleteCustomLyrics(trackId, artist, title)
        }
        downloadRepository?.deleteLyrics(trackId)
        lyricsPreferencesStore?.unblockLyrics(trackId)
        return getLyrics(trackId, artist, title, duration)
    }

    /** Notifies Navidrome that playback of [trackId] has started. */
    suspend fun reportNowPlaying(trackId: String) {
        runCatching { api.scrobble(id = trackId, time = null, submission = false) }
    }

    /** Submits a full scrobble for [trackId] with the playback start time. */
    suspend fun submitScrobble(trackId: String, playedAtMs: Long) {
        runCatching {
            api.scrobble(
                id = trackId,
                time = playedAtMs / 1000,
                submission = true,
            )
        }
    }

    /** Triggers a music library scan on the Subsonic / Navidrome server. */
    suspend fun startScan(fullScan: Boolean = true): com.naviify.app.core.network.dto.ScanStatus? {
        return api.startScan(fullScan = fullScan).result { scanStatus }
    }

    /** Checks the current library scan status on the server. */
    suspend fun getScanStatus(): com.naviify.app.core.network.dto.ScanStatus? {
        return api.getScanStatus().result { scanStatus }
    }
}

private inline fun <T> SubsonicEnvelope.result(block: SubsonicResponse.() -> T): T {
    val response = this.response
    if (!response.isOk) {
        throw SubsonicApiException(
            code = response.error?.code,
            message = response.error?.message ?: "Subsonic error '${response.status}'",
        )
    }
    return response.block()
}
