package com.naviify.app.data.download

import android.content.Context
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.data.mapper.sanitizePlaylistName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StoredPlaylist(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val coverArtId: String? = null,
    val tracks: List<StoredTrack> = emptyList(),
)

@Serializable
data class StoredTrack(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val coverArtId: String? = null,
    val duration: Int = 0,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val suffix: String? = null,
    val bitRate: Int? = null,
    val genre: String? = null,
)

/**
 * Persists playlists and their tracks to local disk storage so they can be
 * discovered, displayed and played when Naviify is in Offline mode.
 */
@Singleton
class OfflinePlaylistStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val storeFile: File by lazy {
        File(context.filesDir, "offline_playlists.json")
    }

    @Volatile
    private var inMemoryCache: Map<String, StoredPlaylist> = emptyMap()

    init {
        loadFromDisk()
    }

    @Synchronized
    private fun loadFromDisk() {
        if (storeFile.exists()) {
            runCatching {
                val content = storeFile.readText()
                val list = json.decodeFromString<List<StoredPlaylist>>(content)
                inMemoryCache = list.associateBy { it.id }
            }
        }
    }

    @Synchronized
    private fun persistToDisk() {
        runCatching {
            val content = json.encodeToString(inMemoryCache.values.toList())
            storeFile.writeText(content)
        }
    }

    @Synchronized
    fun savePlaylistSummaries(playlists: List<Playlist>) {
        val updated = inMemoryCache.toMutableMap()
        playlists.forEach { pl ->
            val existing = updated[pl.id]
            updated[pl.id] = StoredPlaylist(
                id = pl.id,
                name = pl.name,
                comment = pl.comment,
                owner = pl.owner,
                coverArtId = pl.coverArtId ?: existing?.coverArtId,
                tracks = existing?.tracks ?: emptyList(),
            )
        }
        inMemoryCache = updated
        persistToDisk()
    }

    @Synchronized
    fun savePlaylist(playlist: Playlist) {
        val updated = inMemoryCache.toMutableMap()
        updated[playlist.id] = StoredPlaylist(
            id = playlist.id,
            name = playlist.name,
            comment = playlist.comment,
            owner = playlist.owner,
            coverArtId = playlist.coverArtId,
            tracks = playlist.tracks.map {
                StoredTrack(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    albumId = it.albumId,
                    coverArtId = it.coverArtId,
                    duration = it.duration,
                    year = it.year,
                    trackNumber = it.trackNumber,
                    discNumber = it.discNumber,
                    suffix = it.suffix,
                    bitRate = it.bitRate,
                    genre = it.genre,
                )
            },
        )
        inMemoryCache = updated
        persistToDisk()
    }

    fun getDownloadedPlaylists(downloadedTrackIds: Set<String>): List<Playlist> {
        return inMemoryCache.values.mapNotNull { stored ->
            val matchingCount = stored.tracks.count { downloadedTrackIds.contains(it.id) }
            if (matchingCount > 0) {
                Playlist(
                    id = stored.id,
                    name = sanitizePlaylistName(stored.name),
                    comment = stored.comment,
                    owner = stored.owner,
                    coverArtId = stored.coverArtId,
                    songCount = matchingCount,
                )
            } else null
        }
    }

    /** Cover id recorded from the last playlist summary, if any. */
    fun cachedCoverArtId(playlistId: String): String? =
        inMemoryCache[playlistId]?.coverArtId

    fun getCachedPlaylist(playlistId: String, downloadedTrackMap: Map<String, Track>): Playlist? {
        val stored = inMemoryCache[playlistId] ?: return null
        val availableTracks = stored.tracks.mapNotNull { st ->
            downloadedTrackMap[st.id]
        }
        if (availableTracks.isEmpty()) return null

        return Playlist(
            id = stored.id,
            name = sanitizePlaylistName(stored.name),
            comment = stored.comment,
            owner = stored.owner,
            coverArtId = stored.coverArtId,
            songCount = availableTracks.size,
            duration = availableTracks.sumOf { it.duration },
            tracks = availableTracks,
        )
    }

    @Synchronized
    fun deletePlaylist(playlistId: String) {
        val updated = inMemoryCache.toMutableMap()
        if (updated.remove(playlistId) != null) {
            inMemoryCache = updated
            persistToDisk()
        }
    }

    @Synchronized
    fun renamePlaylist(playlistId: String, newName: String) {
        val existing = inMemoryCache[playlistId] ?: return
        val updated = inMemoryCache.toMutableMap()
        updated[playlistId] = existing.copy(name = newName)
        inMemoryCache = updated
        persistToDisk()
    }

    @Synchronized
    fun removeTrackByIndex(playlistId: String, index: Int) {
        val existing = inMemoryCache[playlistId] ?: return
        if (index in existing.tracks.indices) {
            val updatedTracks = existing.tracks.toMutableList().apply { removeAt(index) }
            val updated = inMemoryCache.toMutableMap()
            updated[playlistId] = existing.copy(tracks = updatedTracks)
            inMemoryCache = updated
            persistToDisk()
        }
    }

    fun getTrackIdsForPlaylists(): Map<String, Set<String>> {
        return inMemoryCache.mapValues { entry ->
            entry.value.tracks.map { it.id }.toSet()
        }
    }
}
