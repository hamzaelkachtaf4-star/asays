package com.naviify.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.naviify.app.core.network.DataStoreCookieJar
import com.naviify.app.core.network.SessionStateHolder
import com.naviify.app.core.network.SubsonicUrlProvider
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.data.download.DownloadRepository
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.domain.playback.PlayerQueueStore
import com.naviify.app.ui.widget.PlayerWidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NaviifyApplication : Application(), coil.ImageLoaderFactory {

    @Inject lateinit var configStore: ServerConfigStore
    @Inject lateinit var sessionState: SessionStateHolder
    @Inject lateinit var cookieJar: DataStoreCookieJar
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var urlProvider: SubsonicUrlProvider
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var playerQueueStore: PlayerQueueStore
    @Inject lateinit var favoritesRepository: FavoritesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(imageLoader)
        CoverUrls.provider = urlProvider
        CoverUrls.downloadRepository = downloadRepository
        appScope.launch {
            // Warm the in-memory session before the first network request so
            // interceptors never need to touch disk.
            cookieJar.initialize()
            configStore.config.collectLatest { config ->
                CoverUrls.clearCache()
                sessionState.publish(config)
            }
        }
        appScope.launch {
            playerQueueStore.state
                .map { Triple(it.currentTrack?.id, it.isPlaying, it.currentTrack?.isFavorite) }
                .distinctUntilChanged()
                .collect {
                    PlayerWidgetUpdater.updateAllWidgets(this@NaviifyApplication)
                }
        }
        appScope.launch {
            favoritesRepository.observeFavorites()
                .collect {
                    PlayerWidgetUpdater.updateAllWidgets(this@NaviifyApplication)
                }
        }
    }
}
