package com.naviify.app.data.lyrics

import com.naviify.app.domain.model.LyricsData
import com.naviify.app.domain.model.LyricsLineData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LyricsCandidate(
    val id: Long = 0L,
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val durationSeconds: Int = 0,
    val isSynced: Boolean = false,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
)

/**
 * Free LRCLIB fallback for synced LRC lyrics when Navidrome has none.
 * Endpoint: GET https://lrclib.net/api/get?artist_name=..&track_name=..&duration=..
 * Falls back to a strict search with artist and duration validation.
 */
@Singleton
class LrclibLyricsClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(
        artist: String?,
        title: String?,
        durationSeconds: Int = 0,
        baseUrl: String = LRCLIB_BASE_URL,
    ): LyricsData? = withContext(Dispatchers.IO) {
        val primaryArtist = sanitizeLyricsArtist(artist)
        val cleanTitle = sanitizeLyricsTitle(title)
        val rawArtist = artist?.trim().orEmpty()
        val rawTitle = title?.trim().orEmpty()
        if (cleanTitle.isBlank() && rawTitle.isBlank()) return@withContext null

        val titleToUse = if (cleanTitle.isNotBlank()) cleanTitle else rawTitle

        // 1. Try exact lookup with duration
        if (primaryArtist.isNotBlank() && durationSeconds > 0) {
            val res = runCatching { get(primaryArtist, titleToUse, durationSeconds, baseUrl) }.getOrNull()
            if (res != null && res.isSynced) return@withContext res
        }

        // 2. Try exact lookup without duration
        if (primaryArtist.isNotBlank()) {
            val res = runCatching { get(primaryArtist, titleToUse, null, baseUrl) }.getOrNull()
            if (res != null && res.isSynced) return@withContext res
        }

        // 3. If raw artist is different from primary artist, try exact lookup with raw artist
        if (rawArtist.isNotBlank() && rawArtist != primaryArtist) {
            val res = runCatching { get(rawArtist, titleToUse, null, baseUrl) }.getOrNull()
            if (res != null && res.isSynced) return@withContext res
        }

        // 4. Multi-query search:
        // Query A: "$titleToUse $primaryArtist"
        // Query B: "$rawArtist $rawTitle" (The exact query used by manual search!)
        // Query C: "$titleToUse"
        val artistForFilter = primaryArtist.ifBlank { rawArtist }
        val searchQueries = listOfNotNull(
            "$titleToUse $primaryArtist".takeIf { titleToUse.isNotBlank() && primaryArtist.isNotBlank() },
            "$rawArtist $rawTitle".trim().takeIf { it.isNotBlank() && it != "$titleToUse $primaryArtist" },
            titleToUse.takeIf { it.isNotBlank() && it != "$titleToUse $primaryArtist" },
        ).distinct()

        var bestPlainLyrics: LyricsData? = null

        for (q in searchQueries) {
            val res = runCatching { search(q, artistForFilter, durationSeconds, baseUrl) }.getOrNull()
            if (res != null) {
                if (res.isSynced) {
                    return@withContext res
                } else if (bestPlainLyrics == null) {
                    bestPlainLyrics = res
                }
            }
        }

        return@withContext bestPlainLyrics
    }

    suspend fun searchCandidates(
        query: String,
        baseUrl: String = LRCLIB_BASE_URL,
    ): List<LyricsCandidate> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()
        val searchBase = baseUrl.substringBeforeLast("/") + "/search"
        val url = searchBase.toHttpUrl().newBuilder()
            .addQueryParameter("q", clean)
            .build()
        val text = execute(url) ?: return@withContext emptyList()
        val results = runCatching { json.decodeFromString<List<LrclibPayload>>(text) }.getOrDefault(emptyList())
        results.mapNotNull { p ->
            val hasSynced = !p.syncedLyrics.isNullOrBlank()
            val hasPlain = !p.plainLyrics.isNullOrBlank()
            if (!hasSynced && !hasPlain) null
            else LyricsCandidate(
                id = p.id ?: 0L,
                trackName = p.trackName.orEmpty().ifBlank { clean },
                artistName = p.artistName.orEmpty(),
                albumName = p.albumName,
                durationSeconds = (p.duration ?: 0.0).toInt(),
                isSynced = hasSynced,
                syncedLyrics = p.syncedLyrics,
                plainLyrics = p.plainLyrics,
            )
        }
    }

    fun candidateToLyricsData(candidate: LyricsCandidate): LyricsData {
        val raw = candidate.syncedLyrics ?: candidate.plainLyrics.orEmpty()
        val lines = parseLegacyLyrics(raw)
        return LyricsData(
            syncedLines = lines,
            plainText = raw,
            offsetMs = 0L,
        )
    }

    private fun get(artist: String, title: String, duration: Int?, baseUrl: String): LyricsData? {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("track_name", title)
            .apply { if (duration != null && duration > 0) addQueryParameter("duration", duration.toString()) }
            .build()
        val text = execute(url)
        if (text.isNullOrBlank()) return null
        val payload = runCatching { json.decodeFromString<LrclibPayload>(text) }.getOrNull() ?: return null
        return toLyricsData(payload)
    }

    private fun search(
        query: String,
        requiredArtist: String?,
        targetDuration: Int?,
        baseUrl: String,
    ): LyricsData? {
        val searchBase = baseUrl.substringBeforeLast("/") + "/search"
        val url = searchBase.toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()
        val text = execute(url)
        if (text.isNullOrBlank()) return null
        val results = runCatching { json.decodeFromString<List<LrclibPayload>>(text) }.getOrDefault(emptyList())
        if (results.isEmpty()) return null

        // Filter: artist must match to avoid returning lyrics of completely different songs
        val matched = if (!requiredArtist.isNullOrBlank()) {
            results.filter { item ->
                artistMatches(item.artistName, requiredArtist)
            }
        } else {
            results
        }
        if (matched.isEmpty()) return null

        val syncedCandidates = matched.filter { !it.syncedLyrics.isNullOrBlank() }
        val candidate = if (syncedCandidates.isNotEmpty()) {
            if (targetDuration != null && targetDuration > 0) {
                // Tier 1: within ±6s
                val tier1 = syncedCandidates.filter {
                    val d = (it.duration ?: 0.0).toInt()
                    d <= 0 || kotlin.math.abs(d - targetDuration) <= 6
                }
                if (tier1.isNotEmpty()) {
                    tier1.minByOrNull { kotlin.math.abs((it.duration ?: 0.0).toInt() - targetDuration) }
                } else {
                    // Tier 2: within ±15s (radio edits, intros, silence padding)
                    val tier2 = syncedCandidates.filter {
                        val d = (it.duration ?: 0.0).toInt()
                        d <= 0 || kotlin.math.abs(d - targetDuration) <= 15
                    }
                    if (tier2.isNotEmpty()) {
                        tier2.minByOrNull { kotlin.math.abs((it.duration ?: 0.0).toInt() - targetDuration) }
                    } else null
                }
            } else {
                syncedCandidates.first()
            }
        } else {
            // Plain text fallback if duration is close
            if (targetDuration != null && targetDuration > 0) {
                matched.firstOrNull {
                    val d = (it.duration ?: 0.0).toInt()
                    d <= 0 || kotlin.math.abs(d - targetDuration) <= 15
                }
            } else {
                matched.firstOrNull()
            }
        }
        return candidate?.let(::toLyricsData)
    }

    private fun execute(url: HttpUrl): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ASAYS/1.0 (Android; Navidrome/Subsonic client)")
            .header("Accept", "application/json")
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }

    private fun toLyricsData(payload: LrclibPayload): LyricsData? {
        val synced = payload.syncedLyrics
        val plain = payload.plainLyrics
        if (synced.isNullOrBlank() && plain.isNullOrBlank()) return null
        val lines: List<LyricsLineData> = parseLegacyLyrics(synced ?: plain.orEmpty())
        return if (lines.isEmpty()) null else LyricsData(syncedLines = lines, plainText = synced ?: plain)
    }

    private companion object {
        const val LRCLIB_BASE_URL = "https://lrclib.net/api/get"
    }
}

@Serializable
internal data class LrclibPayload(
    @SerialName("id") val id: Long? = null,
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("albumName") val albumName: String? = null,
    @SerialName("duration") val duration: Double? = null,
    @SerialName("instrumental") val instrumental: Boolean? = false,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null,
    @SerialName("plainLyrics") val plainLyrics: String? = null,
)

internal val LYRICS_PART_SEPARATORS = listOf("\u2022", "\u00B7", ",", "/", "&", "feat.", "ft.", "featuring", "with")

/** Extracts the primary artist from featuring/multi-artist strings. */
fun sanitizeLyricsArtist(raw: String?): String {
    val t = raw?.trim().orEmpty()
    if (t.isBlank()) return ""
    var end = t.length
    for (separator in LYRICS_PART_SEPARATORS) {
        val index = t.indexOf(separator, ignoreCase = true)
        if (index in 1 until end) end = index
    }
    return t.take(end).trim().trimEnd('-', ' ').trim()
}

/** Strips parenthesized and bracketed tags, file extensions, and extra labels. */
fun sanitizeLyricsTitle(raw: String?): String {
    val t = raw?.trim().orEmpty()
    if (t.isBlank()) return ""
    return t
        .replace(Regex("""\s*[\(\[][^()\[\]]*[\)\]]\s*"""), " ")
        .replace(Regex("""\s*-\s*(single|remastered|bonus track|live|remix).*$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s+(feat\.|ft\.).*$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\.(mp3|flac|m4a|ogg|opus)$""", RegexOption.IGNORE_CASE), "")
        .trim()
}

/** Flexible artist matching that tolerates punctuation differences ($ vs s, accents, spaces). */
fun artistMatches(artistA: String?, artistB: String?): Boolean {
    if (artistA.isNullOrBlank() || artistB.isNullOrBlank()) return true
    val a = artistA.lowercase().trim()
    val b = artistB.lowercase().trim()
    if (a.contains(b) || b.contains(a)) return true
    val normA = a.filter { it.isLetterOrDigit() }
    val normB = b.filter { it.isLetterOrDigit() }
    if (normA.isNotEmpty() && normB.isNotEmpty()) {
        if (normA.contains(normB) || normB.contains(normA)) return true
    }
    return false
}
