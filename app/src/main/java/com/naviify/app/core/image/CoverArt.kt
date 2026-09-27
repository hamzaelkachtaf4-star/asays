package com.naviify.app.core.image

import androidx.collection.LruCache
import com.naviify.app.core.network.SubsonicUrlProvider
import com.naviify.app.data.download.DownloadRepository

/**
 * Bridge between signable cover URLs and composables. The provider is registered
 * once from the Application so screens never need DI plumbing for artwork.
 * Includes a fast in-memory LRU cache so repeated binds/scrolls never re-run
 * string manipulation, URL parsing or MD5 hashing.
 */
object CoverUrls {

    @Volatile
    var provider: SubsonicUrlProvider? = null

    @Volatile
    var downloadRepository: DownloadRepository? = null

    private val urlCache = LruCache<String, String>(512)

    /**
     * Version des pochettes, observable par Compose.
     *
     * Poser ou retirer une pochette personnelle change la bonne URL, mais pas
     * les cles de cache (LRU ci-dessus et cles Coil des vignettes) : les listes
     * continuaient donc d'afficher l'ancienne image. Toute modification de
     * pochette appelle [invalidateCoverCaches], ce qui vide le LRU et fait
     * recomposer les vignettes (la version entre dans leurs cles de cache).
     */
    private val _coverVersion = androidx.compose.runtime.mutableStateOf(0L)

    val coverVersion: Long
        get() = _coverVersion.value

    fun invalidateCoverCaches() {
        synchronized(urlCache) {
            urlCache.evictAll()
        }
        _coverVersion.value += 1
    }

    fun clearCache() {
        synchronized(urlCache) {
            urlCache.evictAll()
        }
    }

    /**
     * Best source for an id: the locally cached file when it exists, otherwise
     * a freshly signed server URL.
     */
    fun url(coverArtId: String?, size: Int = 512): String? {
        if (coverArtId.isNullOrBlank()) return null
        val key = "$coverArtId-$size"
        synchronized(urlCache) {
            urlCache[key]?.let { return it }
        }

        val resolved = localUrl(coverArtId) ?: remoteUrl(coverArtId, size)
        if (resolved != null) {
            synchronized(urlCache) {
                urlCache.put(key, resolved)
            }
        }
        return resolved
    }

    /** Local cached artwork only, for callers that prefer it over the network. */
    fun localUrl(coverArtId: String?): String? {
        if (coverArtId.isNullOrBlank()) return null
        val local = downloadRepository?.localCoverFor(coverArtId) ?: return null
        return android.net.Uri.fromFile(local).toString()
    }

    /** Signed server artwork URL only; never touches the local cache. */
    fun remoteUrl(coverArtId: String?, size: Int = 512): String? {
        if (coverArtId.isNullOrBlank()) return null
        return provider?.coverArtUrl(coverArtId, size)?.toString()
    }

    /**
     * Prefers the user's custom cover, then the server's artwork for the
     * playlist. Never returns a local-only value: a miss has to fall back to
     * the server URL, otherwise a playlist that has not been downloaded shows
     * the placeholder even though Navidrome serves cover art for it.
     */
    fun playlistUrl(playlistId: String, coverArtId: String?, size: Int = 512): String? {
        // La presence d'une pochette personnelle fait partie de la cle : sans
        // elle, l'URL du serveur restait en cache apres avoir pose une pochette,
        // et les listes continuaient d'afficher l'ancienne image.
        val custom = localUrl("playlist-$playlistId")
        val key = "pl-$playlistId-$coverArtId-$size-${custom != null}"
        synchronized(urlCache) {
            urlCache[key]?.let { return it }
        }

        val resolved = custom ?: remoteUrl(coverArtId, size)
        if (resolved != null) {
            synchronized(urlCache) {
                urlCache.put(key, resolved)
            }
        }
        return resolved
    }
}
