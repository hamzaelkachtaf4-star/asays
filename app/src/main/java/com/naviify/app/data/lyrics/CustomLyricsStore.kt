package com.naviify.app.data.lyrics

import android.content.Context
import com.naviify.app.domain.model.LyricsData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CustomLyricsRecord(
    val trackId: String,
    val artist: String? = null,
    val title: String? = null,
    val lyrics: LyricsData,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Permanent storage for user-adjusted and user-synchronized lyrics.
 *
 * Unlike temporary HTTP cache files (which reside in `context.filesDir/lyrics`), records in
 * [CustomLyricsStore] reside in `context.filesDir/custom_lyrics` and are NEVER erased during
 * routine cache clears or download deletions.
 *
 * Dual-indexed by:
 * 1. [trackId] (Navidrome server ID)
 * 2. Fingerprint (`artist:title`) so that if a server library is rescanned and song IDs
 *    change, the calibrated lyrics remain matched and available.
 */
@Singleton
class CustomLyricsStore internal constructor(
    private val storageDir: File,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        File(context.filesDir, "custom_lyrics").apply { if (!exists()) mkdirs() }
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val idCache = ConcurrentHashMap<String, LyricsData>()
    private val fingerprintCache = ConcurrentHashMap<String, LyricsData>()

    init {
        scope.launch {
            loadAllFromDisk()
        }
    }

    /**
     * Retrieves permanent user-customized lyrics.
     * Checks exact [trackId] first, then falls back to metadata fingerprint matching.
     */
    fun getCustomLyrics(trackId: String, artist: String? = null, title: String? = null): LyricsData? {
        idCache[trackId]?.let { return it }

        val fp = createFingerprint(artist, title)
        if (fp != null) {
            fingerprintCache[fp]?.let { return it }
        }

        // Synchronous disk fallback if background init hasn't completed yet
        return loadRecordFromDisk(trackId)
            ?: (if (fp != null) loadRecordByFingerprintFromDisk(fp) else null)
    }

    fun hasCustomLyrics(trackId: String, artist: String? = null, title: String? = null): Boolean {
        return getCustomLyrics(trackId, artist, title) != null
    }

    /**
     * Persists customized lyrics (with timing offset or selected synced candidate) permanently.
     */
    fun saveCustomLyrics(
        trackId: String,
        artist: String?,
        title: String?,
        lyrics: LyricsData,
    ) {
        val customLyrics = lyrics.copy(isCustom = true)
        idCache[trackId] = customLyrics

        val fp = createFingerprint(artist, title)
        if (fp != null) {
            fingerprintCache[fp] = customLyrics
        }

        runCatching {
            val record = CustomLyricsRecord(
                trackId = trackId,
                artist = artist,
                title = title,
                lyrics = customLyrics,
                updatedAt = System.currentTimeMillis(),
            )
            val file = fileForId(trackId)
            file.writeText(json.encodeToString(record))
        }
    }

    /**
     * Removes custom override, allowing playback to revert to original server / default lyrics.
     */
    fun deleteCustomLyrics(trackId: String, artist: String? = null, title: String? = null) {
        idCache.remove(trackId)
        val fp = createFingerprint(artist, title)
        if (fp != null) {
            fingerprintCache.remove(fp)
        }

        runCatching {
            val file = fileForId(trackId)
            if (file.exists()) file.delete()
        }
    }

    private fun loadAllFromDisk() {
        val files = storageDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }.orEmpty()
        for (file in files) {
            runCatching {
                val record = json.decodeFromString<CustomLyricsRecord>(file.readText())
                idCache[record.trackId] = record.lyrics
                val fp = createFingerprint(record.artist, record.title)
                if (fp != null) {
                    fingerprintCache[fp] = record.lyrics
                }
            }
        }
    }

    private fun loadRecordFromDisk(trackId: String): LyricsData? {
        val file = fileForId(trackId)
        if (!file.exists() || file.length() == 0L) return null
        return runCatching {
            val record = json.decodeFromString<CustomLyricsRecord>(file.readText())
            idCache[trackId] = record.lyrics
            val fp = createFingerprint(record.artist, record.title)
            if (fp != null) fingerprintCache[fp] = record.lyrics
            record.lyrics
        }.getOrNull()
    }

    private fun loadRecordByFingerprintFromDisk(fingerprint: String): LyricsData? {
        val files = storageDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }.orEmpty()
        for (file in files) {
            val record = runCatching {
                json.decodeFromString<CustomLyricsRecord>(file.readText())
            }.getOrNull() ?: continue

            val fp = createFingerprint(record.artist, record.title)
            if (fp == fingerprint) {
                idCache[record.trackId] = record.lyrics
                fingerprintCache[fingerprint] = record.lyrics
                return record.lyrics
            }
        }
        return null
    }

    private fun createFingerprint(artist: String?, title: String?): String? {
        val cleanTitle = title?.trim()?.lowercase()
        if (cleanTitle.isNullOrBlank()) return null
        val cleanArtist = artist?.trim()?.lowercase().orEmpty()
        return "$cleanArtist:$cleanTitle"
    }

    private fun fileForId(trackId: String): File {
        val safeName = sha1Hex(trackId)
        return File(storageDir, "$safeName.json")
    }

    private fun sha1Hex(value: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
