package com.naviify.app.data.repository

import com.naviify.app.core.network.SubsonicService
import com.naviify.app.core.storage.room.FavoriteDao
import com.naviify.app.core.storage.room.FavoriteEntity
import com.naviify.app.domain.model.Favorite
import com.naviify.app.domain.model.FavoriteType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the Room favorites table in sync with the server: toggling writes
 * locally first (works offline) and mirrors the change to Navidrome best-effort.
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteDao,
    private val api: SubsonicService,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeFavorites(): Flow<List<Favorite>> =
        dao.observeAll().map { entities ->
            entities.map { entity ->
                Favorite(
                    type = FavoriteType.fromStorage(entity.type),
                    id = entity.id,
                    name = entity.name,
                    secondaryText = entity.secondaryText,
                    coverArtId = entity.coverArtId,
                )
            }
        }.distinctUntilChanged()

    suspend fun isFavorite(id: String, type: FavoriteType): Boolean =
        dao.get(id, type.storageValue) != null

    suspend fun toggleFavorite(
        id: String,
        type: FavoriteType,
        name: String,
        secondaryText: String? = null,
        coverArtId: String? = null,
    ) {
        val typeValue = type.storageValue
        val existing = dao.get(id, typeValue)
        val currentlyFavorite = existing != null
        if (currentlyFavorite) {
            dao.delete(id, typeValue)
        } else {
            dao.upsert(
                FavoriteEntity(
                    id = id,
                    type = typeValue,
                    name = name,
                    secondaryText = secondaryText,
                    coverArtId = coverArtId,
                    addedAt = System.currentTimeMillis(),
                ),
            )
        }
        scope.launch {
            val result = runCatching {
                if (currentlyFavorite) api.unstar(listOf(id)) else api.star(listOf(id))
            }
            if (result.isFailure) {
                // Roll back optimistic write so Room does not desync from server
                if (currentlyFavorite) {
                    existing?.let { dao.upsert(it) }
                } else {
                    dao.delete(id, typeValue)
                }
            }
        }
    }

    private fun fireAndForget(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
        }
    }
}
