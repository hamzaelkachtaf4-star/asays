package com.naviify.app.core.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.core.image.ImageValidator
import com.naviify.app.core.network.SubsonicUrlProvider
import com.naviify.app.data.download.DownloadRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * ContentProvider that exposes album art to external processes such as
 * Android Auto (com.google.android.projection.gearhead), the system media
 * notification, lock screen, and Wear OS via content:// URIs.
 *
 * It transparently resolves artwork from the local download cache or fetches
 * and caches it on demand from the Subsonic/Navidrome server.
 *
 * Employs fine-grained thread synchronization, image format integrity validation
 * (checking JPEG EOI / PNG IEND / WebP headers), and atomic file moves to prevent
 * tearing or half-grey image decoding on Android Auto.
 */
class ArtworkContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArtworkEntryPoint {
        fun downloadRepository(): DownloadRepository
        fun urlProvider(): SubsonicUrlProvider
    }

    private fun getEntryPoint(): ArtworkEntryPoint? {
        val ctx = context?.applicationContext ?: return null
        return runCatching {
            EntryPointAccessors.fromApplication(ctx, ArtworkEntryPoint::class.java)
        }.getOrNull()
    }

    /**
     * Shared HTTP client with connection retry and generous timeouts to handle
     * mobile network handovers and server-side cover generation latency.
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Striped locks (64 buckets) prevent concurrent requests for the same
     * cover (e.g. Android Auto background blur vs player card vs notification)
     * from racing and corrupting each other's temporary files.
     */
    private val fileLocks = Array(64) { Any() }
    private fun getLock(coverArtId: String): Any =
        fileLocks[abs(coverArtId.hashCode()) % fileLocks.size]

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String {
        val path = uri.path.orEmpty()
        return if (path.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val rawId = uri.lastPathSegment
            ?.removeSuffix(".jpg")
            ?.removeSuffix(".jpeg")
            ?.removeSuffix(".png")
            ?: throw FileNotFoundException("Invalid artwork URI: $uri")

        // Treat rawId as untrusted input
        val coverArtId = rawId.trim()
        val idIsSafe = coverArtId.isNotEmpty() &&
            coverArtId.length <= MAX_ID_LENGTH &&
            !coverArtId.contains('/') &&
            !coverArtId.contains('\\') &&
            !coverArtId.contains("..") &&
            coverArtId.none { it.code < 0x20 }
        if (!idIsSafe) throw FileNotFoundException("Rejected artwork id: $rawId")

        val ctx = context ?: throw FileNotFoundException("Context unavailable for URI: $uri")

        val entryPoint = getEntryPoint()
        val downloadRepo = entryPoint?.downloadRepository() ?: CoverUrls.downloadRepository
        val urlProvider = entryPoint?.urlProvider() ?: CoverUrls.provider

        synchronized(getLock(coverArtId)) {
            // 1. Check local download repository
            val localCover = downloadRepo?.localCoverFor(coverArtId)
            if (localCover != null && localCover.exists()) {
                if (ImageValidator.isValidImage(localCover)) {
                    return ParcelFileDescriptor.open(localCover, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    Log.w(TAG, "Local cover for $coverArtId is corrupt or truncated. Purging.")
                    localCover.delete()
                }
            }

            // 2. Check disk cache in cacheDir/artwork_cache
            val cacheDir = File(ctx.cacheDir, "artwork_cache").apply { if (!exists()) mkdirs() }
            val cachedFile = File(cacheDir, "$coverArtId.jpg")
            if (cachedFile.exists()) {
                if (ImageValidator.isValidImage(cachedFile)) {
                    return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    Log.w(TAG, "Cached artwork file for $coverArtId is corrupt or truncated. Purging from cache.")
                    cachedFile.delete()
                }
            }

            // 3. Fetch from remote server on-demand
            val remoteUrl = urlProvider?.coverArtUrl(coverArtId, 512)?.toString()
            if (!remoteUrl.isNullOrBlank()) {
                var tempFile: File? = null
                try {
                    tempFile = File.createTempFile("art_${coverArtId}_", ".tmp", cacheDir)
                    val request = Request.Builder().url(remoteUrl).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.w(TAG, "Server returned HTTP ${response.code} for cover $coverArtId")
                            return@use
                        }
                        val body = response.body ?: return@use
                        val expectedLength = body.contentLength()

                        tempFile.outputStream().use { out ->
                            body.byteStream().use { input ->
                                input.copyTo(out)
                            }
                        }

                        // Validate content length if reported by server
                        if (expectedLength > 0L && tempFile.length() != expectedLength) {
                            Log.w(
                                TAG,
                                "Incomplete download for $coverArtId: expected $expectedLength bytes, got ${tempFile.length()}",
                            )
                            return@use
                        }

                        // Validate image container structure & EOF markers
                        if (tempFile.exists() && tempFile.length() in MIN_ARTWORK_BYTES..MAX_ARTWORK_BYTES &&
                            ImageValidator.isValidImage(tempFile)
                        ) {
                            trimCache(cacheDir, tempFile.length())

                            // Atomic replacement to prevent reading during write
                            val moved = runCatching {
                                Files.move(
                                    tempFile.toPath(),
                                    cachedFile.toPath(),
                                    StandardCopyOption.ATOMIC_MOVE,
                                    StandardCopyOption.REPLACE_EXISTING,
                                )
                                true
                            }.getOrDefault(false)

                            if (!moved) {
                                if (!tempFile.renameTo(cachedFile)) {
                                    tempFile.copyTo(cachedFile, overwrite = true)
                                    tempFile.delete()
                                }
                            }

                            if (cachedFile.exists() && ImageValidator.isValidImage(cachedFile)) {
                                return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                            }
                        } else {
                            Log.w(TAG, "Artwork verification failed for $coverArtId (${tempFile.length()} bytes)")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch cover art for $coverArtId: ${e.message}")
                } finally {
                    tempFile?.let { if (it.exists()) it.delete() }
                }
            }
        }

        throw FileNotFoundException("Artwork not found or invalid for ID: $coverArtId")
    }

    /** Keeps the on-demand artwork cache bounded; cleans stale temporary files. */
    private fun trimCache(cacheDir: File, incomingBytes: Long) {
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        val now = System.currentTimeMillis()

        // Clean abandoned temporary files older than 5 minutes
        files.filter { it.name.endsWith(".tmp") && (now - it.lastModified() > 300_000L) }
            .forEach { it.delete() }

        var total = files.filter { !it.name.endsWith(".tmp") }.sumOf { it.length() } + incomingBytes
        if (total <= MAX_CACHE_BYTES) return

        files.filter { !it.name.endsWith(".tmp") }
            .sortedBy { it.lastModified() }
            .forEach { file ->
                if (total <= MAX_CACHE_BYTES) return
                total -= file.length()
                file.delete()
            }
    }

    private companion object {
        const val TAG = "ArtworkContentProvider"
        const val MAX_ID_LENGTH = 128
        const val MIN_ARTWORK_BYTES = 128L
        const val MAX_ARTWORK_BYTES = 8L * 1024 * 1024
        const val MAX_CACHE_BYTES = 128L * 1024 * 1024
    }

    override fun query(uri: Uri, p: Array<String>?, s: String?, sa: Array<String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, s: String?, sa: Array<String>?): Int = 0
}
