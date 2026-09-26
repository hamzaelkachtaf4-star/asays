package com.naviify.app.core.storage.room

import androidx.room.Entity

@Entity(tableName = "downloads", primaryKeys = ["trackId"])
data class DownloadEntity(
    val trackId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val coverArtId: String? = null,
    val suffix: String? = null,
    val localFilePath: String? = null,
    val fileSize: Long = 0L,
    val downloadedAt: Long = 0L,
    val status: String = STATUS_DOWNLOADING,
) {
    companion object {
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_DONE = "DONE"
        const val STATUS_FAILED = "FAILED"
    }
}
