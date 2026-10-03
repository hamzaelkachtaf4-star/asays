package com.naviify.app.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlin.OptIn
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.naviify.app.MainActivity
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.playback.PlayerQueueStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Media3 MediaLibraryService: feeds Android Auto, the system media notification
 * and lock-screen controls, and streams through the LRU playback cache.
 */
@AndroidEntryPoint
class NaviifyPlaybackService : MediaLibraryService() {

    @Inject lateinit var cacheDataSourceFactory: CacheDataSource.Factory
    @Inject lateinit var mediaItemMapper: MediaItemMapper
    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var favoritesRepository: FavoritesRepository
    @Inject lateinit var playerQueueStore: PlayerQueueStore

    private lateinit var mediaLibrarySession: MediaLibrarySession
    private lateinit var player: ExoPlayer
    private lateinit var libraryCallback: NaviifyLibraryCallback

    /**
     * Keeps the custom layout and home-screen widgets in sync with the player.
     * Held as a field so it can be detached before [player] is released.
     */
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (::libraryCallback.isInitialized) libraryCallback.updateCustomLayout()
            com.naviify.app.ui.widget.PlayerWidgetUpdater.updateAllWidgets(this@NaviifyPlaybackService)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            com.naviify.app.ui.widget.PlayerWidgetUpdater.updateAllWidgets(this@NaviifyPlaybackService)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(MUSIC_AUDIO_ATTRIBUTES, true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // Fast start: 500ms to first frame, generous up-buffer for FLAC.
                    .setBufferDurationsMs(30_000, 120_000, 500, 2_000)
                    .build(),
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        libraryCallback = NaviifyLibraryCallback(
            mediaRepository = mediaRepository,
            favoritesRepository = favoritesRepository,
            itemMapper = mediaItemMapper,
            queueStore = playerQueueStore,
        )

        val sessionExtras = Bundle().apply {
            putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS", true)
            putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT", true)
            putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_QUEUE", true)
        }

        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            libraryCallback,
        )
            .setPeriodicPositionUpdateEnabled(true)
            .setSessionActivity(sessionActivityPendingIntent())
            .setSessionExtras(sessionExtras)
            .build()

        libraryCallback.attachSession(mediaLibrarySession)

        player.addListener(playerListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val shouldStop = !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        if (shouldStop) {
            player.stop()
            player.clearMediaItems()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Detach first: release() can still dispatch final events, and the
        // listener would otherwise touch a released callback / dead service.
        if (::player.isInitialized) runCatching { player.removeListener(playerListener) }
        if (::libraryCallback.isInitialized) libraryCallback.release()
        if (::mediaLibrarySession.isInitialized) runCatching { mediaLibrarySession.release() }
        if (::player.isInitialized) runCatching { player.release() }
        super.onDestroy()
    }

    private fun sessionActivityPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private val MUSIC_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    }
}
