package com.naviify.app.core.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteEntity::class, DownloadEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class NaviifyDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    abstract fun downloadDao(): DownloadDao

    companion object {
        const val NAME = "naviify.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS downloads (" +
                        "trackId TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, artist TEXT, album TEXT, coverArtId TEXT, " +
                        "suffix TEXT, localFilePath TEXT, fileSize INTEGER NOT NULL DEFAULT 0, " +
                        "downloadedAt INTEGER NOT NULL DEFAULT 0, status TEXT NOT NULL)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN albumId TEXT")
            }
        }
    }
}
