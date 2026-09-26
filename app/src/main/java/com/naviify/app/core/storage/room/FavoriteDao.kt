package com.naviify.app.core.storage.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE id = :id AND type = :type")
    suspend fun get(id: String, type: String): FavoriteEntity?

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id AND type = :type")
    suspend fun delete(id: String, type: String)

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int
}
