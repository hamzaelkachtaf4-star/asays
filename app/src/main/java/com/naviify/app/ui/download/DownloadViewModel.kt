package com.naviify.app.ui.download

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.core.storage.room.DownloadEntity
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class DownloadStatus(
    val byTrackId: Map<String, TrackDownloadState> = emptyMap(),
    val totalBytes: Long = 0L,
)

enum class TrackDownloadState {
    DOWNLOADING,
    DONE,
    FAILED,
}

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {

    val downloads: StateFlow<List<DownloadEntity>> = downloadRepository.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val status: StateFlow<DownloadStatus> = downloads
        .map { list ->
            DownloadStatus(
                byTrackId = list.associate { it.trackId to it.status.toTrackDownloadState() },
                totalBytes = list.sumOf { it.fileSize },
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DownloadStatus())

    fun downloadTrack(track: Track) = downloadRepository.downloadTrack(track)

    fun downloadTracks(tracks: List<Track>) = downloadRepository.downloadTracks(tracks)

    fun downloadPlaylist(playlist: Playlist) = downloadRepository.downloadPlaylist(playlist)

    fun cancelTrackDownload(trackId: String) = downloadRepository.cancelDownload(trackId)

    fun cancelPlaylistDownload(playlist: Playlist) = downloadRepository.cancelPlaylistDownload(playlist)

    fun deleteDownload(trackId: String) = downloadRepository.deleteDownload(trackId)

    fun deleteAllDownloads() = downloadRepository.deleteAllDownloads()

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isNotEmpty()) playbackController.play(tracks, startIndex)
    }
}

private fun String.toTrackDownloadState(): TrackDownloadState = when (this) {
    DownloadEntity.STATUS_DONE -> TrackDownloadState.DONE
    DownloadEntity.STATUS_FAILED -> TrackDownloadState.FAILED
    else -> TrackDownloadState.DOWNLOADING
}
