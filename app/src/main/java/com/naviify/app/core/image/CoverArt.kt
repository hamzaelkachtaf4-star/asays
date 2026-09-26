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
        val key = "pl-$playlistId-$coverArtId-$size"
        synchronized(urlCache) {
            urlCache[key]?.let { return it }
        }

        val resolved = localUrl("playlist-$playlistId") ?: remoteUrl(coverArtId, size)
        if (resolved != null) {
            synchronized(urlCache) {
                urlCache.put(key, resolved)
            }
        }
        return resolved
    }
}
