package com.naviify.app.core.image

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            // RGB_565 halves bitmap memory consumption for opaque album art, eliminating GC pauses
            .bitmapConfig(Bitmap.Config.RGB_565)
            // Cover art URLs are signed per request; local cache wins over headers.
            .respectCacheHeaders(false)
            .allowHardware(true)
            // No crossfade: keeps list scrolling hitch-free on 120Hz displays.
            .crossfade(false)
            .build()
}
