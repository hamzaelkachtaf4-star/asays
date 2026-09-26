package com.naviify.app.core.storage.di

import android.content.Context
import androidx.room.Room
import com.naviify.app.core.storage.room.DownloadDao
import com.naviify.app.core.storage.room.FavoriteDao
import com.naviify.app.core.storage.room.NaviifyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NaviifyDatabase =
        Room.databaseBuilder(context, NaviifyDatabase::class.java, NaviifyDatabase.NAME)
            .addMigrations(NaviifyDatabase.MIGRATION_1_2, NaviifyDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideFavoriteDao(database: NaviifyDatabase): FavoriteDao =
        database.favoriteDao()

    @Provides
    fun provideDownloadDao(database: NaviifyDatabase): DownloadDao =
        database.downloadDao()
}
