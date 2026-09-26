package com.naviify.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.download.TrackDownloadState

data class TrackActionHandler(
    val openOptions: (
        track: Track,
        downloadState: TrackDownloadState?,
        onDownloadToggle: (() -> Unit)?,
    ) -> Unit,
)

val LocalTrackActionHandler = staticCompositionLocalOf<TrackActionHandler?> { null }
