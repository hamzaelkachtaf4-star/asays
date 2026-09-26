package com.naviify.app.core.storage.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads")
    suspend fun getAll(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE trackId = :trackId")
    suspend fun get(trackId: String): DownloadEntity?

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM downloads")
    suspend fun totalSizeBytes(): Long

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun delete(trackId: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}
