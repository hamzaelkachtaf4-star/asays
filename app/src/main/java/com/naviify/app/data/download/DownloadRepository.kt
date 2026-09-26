package com.naviify.app.data.download

import android.content.Context
import android.os.Environment
import com.naviify.app.core.image.ImageValidator
import com.naviify.app.core.network.SubsonicUrlProvider
import com.naviify.app.core.storage.room.DownloadDao
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import android.media.MediaMetadataRetriever
import com.naviify.app.domain.model.LyricsData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import com.naviify.app.data.repository.MediaRepository

/**
 * Downloads tracks into persistent external storage as lossless originals (no
 * maxBitRate) and mirrors their state in Room. Playback prefers the local file.
 */
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DownloadDao,
    private val okHttpClient: OkHttpClient,
    private val urlProvider: SubsonicUrlProvider,
    private val mediaRepositoryProvider: Provider<MediaRepository>,
    private val offlinePlaylistStore: OfflinePlaylistStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = ConcurrentHashMap.newKeySet<String>()
    private val trackJobs = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val downloadSemaphore = Semaphore(3)

    // Concurrent collections: up to three downloads plus the disk sync all
    // mutate these, so a @Volatile Map with read-modify-write would silently
    // drop entries.
    private val localFileIndex = ConcurrentHashMap<String, File>()
    private val coverIndex = ConcurrentHashMap.newKeySet<String>()
    private val coverFileIndex = ConcurrentHashMap<String, File>()

    @Volatile
    private var isDiskSynced: Boolean = false

    private val syncRequested = AtomicBoolean(false)

    private val baseDirectory: File by lazy {
        val external = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val dir = if (external != null) {
            val asaysDir = File(external, "ASAYS_Downloads")
            val tusicDir = File(external, "Tusic_Downloads")
            when {
                asaysDir.exists() -> asaysDir
                tusicDir.exists() -> tusicDir
                else -> asaysDir
            }
        } else {
            File(context.filesDir, "downloads")
        }
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val legacyDirectory: File by lazy {
        File(context.filesDir, "downloads")
    }

    private val coversDirectory: File by lazy {
        val dir = File(context.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val lyricsDirectory: File by lazy {
        val dir = File(context.filesDir, "lyrics")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    init {
        scope.launch {
            migrateLegacyDownloads()
            syncDiskDownloads()
        }
    }

    fun observeDownloads(): Flow<List<DownloadEntity>> = dao.observeAll()

    suspend fun downloadedTrack(trackId: String): DownloadEntity? =
        dao.get(trackId)?.takeIf { it.status == DownloadEntity.STATUS_DONE }

    /**
     * Synchronous in-memory lookup so a track's MediaItem can map to its local
     * file without touching the disk on the calling thread. The index is built
     * by [syncDiskDownloads] on [Dispatchers.IO]; before that first pass has
     * finished we ask for a sync and report "not downloaded yet" instead of
     * scanning the directory tree on the caller (which was happening on the
     * main thread during queue construction).
     */
    fun localFileFor(trackId: String): File? {
        localFileIndex[trackId]?.takeIf { it.isFile && it.length() > 0L }?.let { return it }
        requestDiskSyncIfNeeded()
        return null
    }

    private fun requestDiskSyncIfNeeded() {
        if (isDiskSynced) return
        if (syncRequested.compareAndSet(false, true)) {
            scope.launch { syncDiskDownloads() }
        }
    }

    /**
     * Filesystem-safe id. Server ids are opaque, so an id containing path
     * separators or `..` would otherwise escape the download directory. Safe
     * ids are returned untouched so existing files keep their names.
     */
    private fun safeFileId(id: String): String {
        val trimmed = id.trim()
        val safe = trimmed.isNotEmpty() &&
            !trimmed.contains('/') &&
            !trimmed.contains('\\') &&
            !trimmed.contains("..") &&
            trimmed.none { it.code < 0x20 }
        return if (safe) trimmed else sha1Hex(trimmed)
    }

    private fun sha1Hex(value: String): String =
        java.security.MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun lyricsFile(trackId: String): File =
        File(lyricsDir(), "${safeFileId(trackId)}.json")

    private fun coverFile(coverArtId: String): File =
        File(coversDir(), "${safeFileId(coverArtId)}.jpg")

    /**
     * Sniffs the container signature. A captive portal or proxy can answer 200
     * with HTML or a JSON error body; without this check that payload would be
     * stored as a `.flac` and fail later inside ExoPlayer as a source error.
     */
    private fun detectAudioExtension(file: File): String? {
        val header = ByteArray(16)
        val read = runCatching { file.inputStream().use { it.read(header) } }.getOrDefault(0)
        if (read < 4) return null
        fun ascii(offset: Int, length: Int): String =
            String(header, offset, length, Charsets.US_ASCII)
        val b0 = header[0].toInt() and 0xFF
        val b1 = header[1].toInt() and 0xFF
        return when {
            ascii(0, 4) == "fLaC" -> "flac"
            ascii(0, 4) == "OggS" -> "ogg"
            ascii(0, 3) == "ID3" -> "mp3"
            b0 == 0xFF && (b1 and 0xF0) == 0xF0 -> "aac"
            b0 == 0xFF && (b1 and 0xE0) == 0xE0 -> "mp3"
            read >= 12 && ascii(0, 4) == "RIFF" && ascii(8, 4) == "WAVE" -> "wav"
            read >= 12 && ascii(4, 4) == "ftyp" -> "m4a"
            else -> null
        }
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        val file = localFileFor(trackId)
        return file != null && file.isFile && file.length() > 0L
    }

    fun downloadTrack(track: Track) {
        if (isTrackDownloaded(track.id)) return
        if (!active.add(track.id)) return
        val job = scope.launch {
            try {
                downloadSemaphore.withPermit {
                    runDownload(track)
                }
            } finally {
                active.remove(track.id)
                trackJobs.remove(track.id)
            }
        }
        trackJobs[track.id] = job
    }

    fun downloadTracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            // Filter out tracks that are already in progress or already downloaded
            val toDownload = tracks.filter { track ->
                if (active.contains(track.id)) return@filter false
                if (isTrackDownloaded(track.id)) return@filter false
                val inDao = dao.get(track.id)
                if (inDao != null && inDao.status == DownloadEntity.STATUS_DONE) {
                    val file = inDao.localFilePath?.let { File(it) }
                    if (file != null && file.isFile && file.length() > 0L) {
                        localFileIndex[track.id] = file
                        return@filter false
                    }
                }
                true
            }

            if (toDownload.isEmpty()) return@launch

            // Download remaining tracks concurrently (up to 3 parallel downloads)
            coroutineScope {
                toDownload.forEach { track ->
                    if (active.add(track.id)) {
                        val childJob = launch {
                            try {
                                downloadSemaphore.withPermit {
                                    runDownload(track)
                                }
                            } finally {
                                active.remove(track.id)
                                trackJobs.remove(track.id)
                            }
                        }
                        trackJobs[track.id] = childJob
                    }
                }
            }
        }
    }

    fun cancelDownload(trackId: String) {
        val job = trackJobs.remove(trackId)
        active.remove(trackId)
        scope.launch {
            job?.cancelAndJoin()
            val safeId = safeFileId(trackId)
            File(baseDir(), "$safeId.part").delete()
            val current = dao.get(trackId)
            if (current != null && current.status != DownloadEntity.STATUS_DONE) {
                baseDir().listFiles()?.forEach { file ->
                    if (file.name == safeId || file.name.startsWith("$safeId.")) {
                        file.delete()
                    }
                }
                localFileIndex.remove(trackId)
                dao.delete(trackId)
            }
        }
    }

    fun cancelPlaylistDownload(playlist: Playlist) {
        playlist.tracks.forEach { track ->
            cancelDownload(track.id)
        }
    }

    fun downloadPlaylist(playlist: Playlist) {
        scope.launch {
            // 1. Save playlist and its tracks into offline store for offline availability
            offlinePlaylistStore.savePlaylist(playlist)
            val coverId = playlist.coverArtId
            if (!coverId.isNullOrBlank()) {
                downloadCover(coverId)
            }
            // 2. Download all missing tracks
            downloadTracks(playlist.tracks)
        }
    }

    fun downloadCover(coverArtId: String?) {
        if (coverArtId.isNullOrBlank()) return
        val coverFile = coverFile(coverArtId)
        if (coverFile.exists() && coverFile.length() > 0L) {
            if (ImageValidator.isValidImage(coverFile)) {
                coverIndex.add(coverArtId)
                coverFileIndex[coverArtId] = coverFile
                return
            } else {
                coverFile.delete()
            }
        }
        val coverUrl = urlProvider.coverArtUrl(coverArtId, 512)?.toString() ?: return
        scope.launch {
            runCatching {
                val req = Request.Builder().url(coverUrl).build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val temp = File.createTempFile("cov_${safeFileId(coverArtId)}_", ".part", coversDir())
                    try {
                        resp.body?.byteStream()?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        }
                        // Validate image structure & EOF markers before publishing
                        if (temp.exists() && ImageValidator.isValidImage(temp)) {
                            if (!temp.renameTo(coverFile)) {
                                temp.copyTo(coverFile, overwrite = true)
                                temp.delete()
                            }
                            if (coverFile.exists() && ImageValidator.isValidImage(coverFile)) {
                                coverIndex.add(coverArtId)
                                coverFileIndex[coverArtId] = coverFile
                            }
                        }
                    } finally {
                        if (temp.exists()) temp.delete()
                    }
                }
            }
        }
    }

    fun deleteCustomCover(coverArtId: String) {
        val fileJpg = coverFile(coverArtId)
        val filePng = File(coversDir(), "${safeFileId(coverArtId)}.png")
        if (fileJpg.exists()) fileJpg.delete()
        if (filePng.exists()) filePng.delete()
        coverIndex.remove(coverArtId)
        coverFileIndex.remove(coverArtId)
    }

    fun deleteDownload(trackId: String) {
        scope.launch {
            val safeId = safeFileId(trackId)
            localFileIndex.remove(trackId)?.delete()
            // Also sweep any files for this id that are not in the index (for
            // example when the row outlives a partially written download).
            listOf(baseDir(), legacyDir()).forEach { dir ->
                dir.listFiles()?.forEach { file ->
                    if (file.name == safeId || file.name.startsWith("$safeId.")) file.delete()
                }
            }
            lyricsFile(trackId).delete()
            dao.delete(trackId)
        }
    }

    /**
     * Wipes the cached artwork on disk and in memory. Called when the user
     * clears caches so a later re-download cannot be masked by stale files.
     */
    fun clearCoverCache() {
        scope.launch {
            coverIndex.clear()
            coverFileIndex.clear()
            coversDir().listFiles()?.forEach { it.delete() }
        }
    }

    fun deleteAllDownloads() {
        scope.launch {
            localFileIndex.clear()
            coverIndex.clear()
            coverFileIndex.clear()
            baseDir().listFiles()?.forEach { it.delete() }
            legacyDir().listFiles()?.forEach { it.delete() }
            coversDir().listFiles()?.forEach { it.delete() }
            lyricsDir().listFiles()?.forEach { it.delete() }
            dao.deleteAll()
        }
    }

    suspend fun totalSizeBytes(): Long = dao.totalSizeBytes()

    private suspend fun runDownload(track: Track) {
        val url = urlProvider.streamUrl(track.id)?.toString()
        if (url == null) {
            dao.upsert(toEntity(track, DownloadEntity.STATUS_FAILED))
            return
        }
        val safeId = safeFileId(track.id)
        // The real container is only known once the bytes have landed, so the
        // part file is extension-less and the final name comes from the
        // signature rather than from untrusted metadata.
        val part = File(baseDir(), "$safeId.part")
        dao.upsert(toEntity(track, DownloadEntity.STATUS_DOWNLOADING))
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                part.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            if (!part.exists() || part.length() == 0L) {
                part.delete()
                throw IOException("Downloaded file is empty")
            }
            val extension = detectAudioExtension(part)
                ?: throw IOException("Downloaded payload is not a recognised audio container")
            val target = File(baseDir(), "$safeId.$extension")
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            if (!target.exists() || target.length() == 0L) {
                target.delete()
                throw IOException("Target file verification failed")
            }
            localFileIndex[track.id] = target
            val entity = toEntity(track, DownloadEntity.STATUS_DONE).copy(
                localFilePath = target.absolutePath,
                fileSize = target.length(),
                suffix = extension,
                downloadedAt = System.currentTimeMillis(),
            )
            dao.upsert(entity)

            // Extract & cache embedded album art for instant offline display
            val coverId = track.coverArtId ?: "cover-${track.id}"
            extractAndSaveEmbeddedArtwork(target, coverId)

            // Pre-cache lyrics for instant offline lyrics viewing & generate companion .lrc
            val artist = track.artist
            val title = track.title
            if (!artist.isNullOrBlank() && !title.isNullOrBlank()) {
                val lyr = mediaRepositoryProvider.get().getLyrics(track.id, artist, title, track.duration)
                if (lyr.hasLyrics) {
                    saveCompanionLrc(track.id, lyr, title, artist)
                }
            }
        } catch (e: Exception) {
            part.delete()
            if (e is kotlinx.coroutines.CancellationException) {
                dao.delete(track.id)
                throw e
            }
            dao.upsert(toEntity(track, DownloadEntity.STATUS_FAILED))
        }
    }

    private val jsonSerializer = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    fun coversDir(): File = coversDirectory

    fun lyricsDir(): File = lyricsDirectory

    fun localCoverFor(coverArtId: String?): File? {
        if (coverArtId.isNullOrBlank()) return null
        // Pure in-memory O(1) lookup with zero disk I/O or BitmapFactory decoding on the UI thread.
        // Files are already validated in the background by syncDiskDownloads and download routines.
        return coverFileIndex[coverArtId] ?: coverFileIndex[safeFileId(coverArtId)]
    }

    fun extractAndSaveEmbeddedArtwork(audioFile: File, coverArtId: String): File? {
        val target = coverFile(coverArtId)
        if (target.exists() && target.length() > 0L) {
            if (ImageValidator.isValidImage(target)) {
                coverIndex.add(coverArtId)
                coverFileIndex[coverArtId] = target
                return target
            } else {
                target.delete()
            }
        }
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(audioFile.absolutePath)
            val pic = mmr.embeddedPicture
            if (pic == null || pic.isEmpty() || !ImageValidator.isValidImageBytes(pic)) {
                null
            } else {
                val temp = File.createTempFile("cov_${safeFileId(coverArtId)}_", ".part", coversDir())
                try {
                    temp.writeBytes(pic)
                    if (temp.length() <= 0L || !ImageValidator.isValidImage(temp)) {
                        null
                    } else {
                        if (!temp.renameTo(target)) {
                            temp.copyTo(target, overwrite = true)
                            temp.delete()
                        }
                        if (target.exists() && ImageValidator.isValidImage(target)) {
                            coverIndex.add(coverArtId)
                            coverFileIndex[coverArtId] = target
                            target
                        } else {
                            null
                        }
                    }
                } finally {
                    if (temp.exists()) temp.delete()
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    fun saveCustomCover(coverArtId: String, inputStream: java.io.InputStream): Boolean {
        return runCatching {
            val target = coverFile(coverArtId)
            val temp = File.createTempFile("cov_${safeFileId(coverArtId)}_", ".part", coversDir())
            try {
                temp.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                if (temp.length() <= 0L || !ImageValidator.isValidImage(temp)) {
                    return@runCatching false
                }
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
                if (target.exists() && ImageValidator.isValidImage(target)) {
                    coverIndex.add(coverArtId)
                    coverFileIndex[coverArtId] = target
                    true
                } else false
            } finally {
                if (temp.exists()) temp.delete()
            }
        }.getOrDefault(false)
    }

    suspend fun saveLyrics(trackId: String, lyricsData: LyricsData) = withContext(Dispatchers.IO) {
        if (lyricsData.isEmpty) return@withContext
        runCatching {
            val file = lyricsFile(trackId)
            val content = jsonSerializer.encodeToString(lyricsData)
            file.writeText(content)
        }
    }

    suspend fun getCachedLyrics(trackId: String): LyricsData? = withContext(Dispatchers.IO) {
        runCatching {
            val file = lyricsFile(trackId)
            if (file.exists() && file.length() > 0L) {
                val content = file.readText()
                jsonSerializer.decodeFromString<LyricsData>(content)
            } else null
        }.getOrNull()
    }

    suspend fun deleteLyrics(trackId: String) = withContext(Dispatchers.IO) {
        runCatching {
            val file = lyricsFile(trackId)
            if (file.exists()) file.delete()
        }
    }

    /**
     * Saves a companion `.lrc` sidecar file directly alongside the downloaded audio file
     * in `ASAYS_Downloads/`. This allows users to transfer music folders to Navidrome or any
     * media player with pre-calibrated lyrics.
     */
    suspend fun saveCompanionLrc(
        trackId: String,
        lyricsData: LyricsData,
        title: String? = null,
        artist: String? = null,
    ) = withContext(Dispatchers.IO) {
        if (lyricsData.isEmpty) return@withContext
        runCatching {
            val audioFile = localFileFor(trackId) ?: dao.get(trackId)?.localFilePath?.let { File(it) }
            if (audioFile != null && audioFile.exists()) {
                val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
                val lrcText = lyricsData.toLrcString(title = title, artist = artist)
                if (lrcText.isNotBlank()) {
                    lrcFile.writeText(lrcText)
                }
            }
        }
    }

    /**
     * Clears only temporary lyrics cache files. Permanent user custom lyrics in `CustomLyricsStore`
     * are isolated and completely unaffected.
     */
    suspend fun clearLyricsCache() = withContext(Dispatchers.IO) {
        runCatching {
            lyricsDir().listFiles()?.forEach { it.delete() }
        }
    }

    private fun baseDir(): File = baseDirectory

    private fun legacyDir(): File = legacyDirectory

    private fun migrateLegacyDownloads() {
        runCatching {
            val legacy = legacyDir()
            val target = baseDir()
            if (legacy.exists() && legacy.canonicalPath != target.canonicalPath) {
                legacy.listFiles()?.forEach { file ->
                    if (!file.name.endsWith(".part") && file.isFile && file.length() > 0L) {
                        val dest = File(target, file.name)
                        if (!dest.exists()) {
                            file.copyTo(dest, overwrite = true)
                        }
                        file.delete()
                    }
                }
            }
        }
    }

    private suspend fun syncDiskDownloads() {
        try {
            val files = baseDir().listFiles()
                ?.filter { !it.name.endsWith(".part") && it.isFile }
                .orEmpty()
            val existing = dao.getAll().associateBy { it.trackId }
            val newIndex = mutableMapOf<String, File>()
            val newCoverIndex = mutableSetOf<String>()
            val newCoverFileIndex = mutableMapOf<String, File>()

            coversDir().listFiles()?.forEach { coverFile ->
                // Skip in-flight `.part` writes so a half-written image is never
                // indexed as if it were a finished cover.
                if (coverFile.isFile && coverFile.length() > 0L && !coverFile.name.endsWith(".part")) {
                    if (ImageValidator.isValidImage(coverFile)) {
                        newCoverIndex.add(coverFile.nameWithoutExtension)
                        newCoverFileIndex[coverFile.nameWithoutExtension] = coverFile
                    } else {
                        coverFile.delete()
                    }
                }
            }

            for (file in files) {
                if (file.length() == 0L) {
                    file.delete()
                    continue
                }

                val trackId = file.name.substringBeforeLast(".")
                val ext = detectAudioExtension(file)
                if (ext == null) {
                    // Corrupted or not an audio file (e.g. truncated download or HTML error response)
                    file.delete()
                    dao.delete(trackId)
                    continue
                }

                val current = existing[trackId]
                newIndex[trackId] = file

                var artist = current?.artist
                var title = current?.title
                var album = current?.album
                val coverArtId = current?.coverArtId ?: "cover-$trackId"

                // Check if cover file already exists on disk
                val targetCover = coverFile(coverArtId)
                if (targetCover.exists() && ImageValidator.isValidImage(targetCover)) {
                    newCoverIndex.add(coverArtId)
                    newCoverFileIndex[coverArtId] = targetCover
                }

                val needsMetadata = current == null || current.artist.isNullOrBlank() || current.title.isNullOrBlank()
                if (needsMetadata) {
                    val mmr = MediaMetadataRetriever()
                    try {
                        mmr.setDataSource(file.absolutePath)
                        if (artist.isNullOrBlank()) {
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                                artist = it
                            }
                        }
                        if (title.isNullOrBlank()) {
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                                title = it
                            }
                        }
                        if (album.isNullOrBlank()) {
                            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                                album = it
                            }
                        }
                        val pic = mmr.embeddedPicture
                        if (pic != null && pic.isNotEmpty() && !targetCover.exists() && ImageValidator.isValidImageBytes(pic)) {
                            targetCover.writeBytes(pic)
                            if (ImageValidator.isValidImage(targetCover)) {
                                newCoverIndex.add(coverArtId)
                                newCoverFileIndex[coverArtId] = targetCover
                            } else {
                                targetCover.delete()
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        runCatching { mmr.release() }
                    }
                }

                val resolvedTitle = title?.takeIf { it.isNotBlank() } ?: "Downloaded Track"

                if (current == null || current.status != DownloadEntity.STATUS_DONE || current.artist.isNullOrBlank() || current.coverArtId.isNullOrBlank() || current.title.isNullOrBlank()) {
                    dao.upsert(
                        DownloadEntity(
                            trackId = trackId,
                            title = resolvedTitle,
                            artist = artist,
                            album = album,
                            albumId = current?.albumId,
                            coverArtId = coverArtId,
                            suffix = ext,
                            status = DownloadEntity.STATUS_DONE,
                            localFilePath = file.absolutePath,
                            fileSize = file.length(),
                            // Preserve the original timestamp so the Downloads
                            // list does not reshuffle on every cold start.
                            downloadedAt = current?.downloadedAt?.takeIf { it > 0L }
                                ?: file.lastModified(),
                        ),
                    )
                }
            }
            localFileIndex.clear()
            localFileIndex.putAll(newIndex)
            coverIndex.clear()
            coverIndex.addAll(newCoverIndex)
            coverFileIndex.clear()
            coverFileIndex.putAll(newCoverFileIndex)
            isDiskSynced = true
        } catch (e: Exception) {
            // Keep whatever was already indexed; the next request retries.
        } finally {
            syncRequested.set(false)
        }
    }

    private fun toEntity(track: Track, status: String): DownloadEntity = DownloadEntity(
        trackId = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        albumId = track.albumId,
        coverArtId = track.coverArtId,
        suffix = track.suffix,
        status = status,
    )
}
