package com.naviify.app.core.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    private const val CACHE_MAX_BYTES = 1024L * 1024 * 1024
    private const val CACHE_DIRECTORY = "playback_cache"

    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider,
    ): SimpleCache = SimpleCache(
        File(context.cacheDir, CACHE_DIRECTORY),
        LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
        databaseProvider,
    ).also { cache ->
        Runtime.getRuntime().addShutdownHook(Thread { runCatching { cache.release() } })
    }

    @Provides
    @Singleton
    fun provideCacheDatabaseProvider(@ApplicationContext context: Context): StandaloneDatabaseProvider =
        StandaloneDatabaseProvider(context)

    /**
     * Stream URLs are pre-signed by [SubsonicUrlProvider] (http/https), while
     * downloaded tracks use local `file://` URIs. [DefaultDataSource] adds
     * fallback data sources (file/content) on top of the HTTP factory so both
     * streaming and offline playback work with the same pipeline.
     */
    @Provides
    @Singleton
    fun provideUpstreamDataSourceFactory(@ApplicationContext context: Context): DataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(context, "ASAYS"))
            .setAllowCrossProtocolRedirects(true)
        return DefaultDataSource.Factory(context, httpFactory)
    }

    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        simpleCache: SimpleCache,
        upstream: DataSource.Factory,
    ): CacheDataSource.Factory = CacheDataSource.Factory()
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(upstream)
}
