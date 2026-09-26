package com.naviify.app.ui.widget

import coil.ImageLoader
import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.data.repository.FavoritesRepository
import com.naviify.app.domain.playback.PlayerQueueStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun playbackController(): PlaybackController
    fun playerQueueStore(): PlayerQueueStore
    fun favoritesRepository(): FavoritesRepository
    fun imageLoader(): ImageLoader
}
