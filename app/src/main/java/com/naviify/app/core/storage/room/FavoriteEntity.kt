package com.naviify.app.core.storage.room

import androidx.room.Entity

@Entity(tableName = "favorites", primaryKeys = ["id", "type"])
data class FavoriteEntity(
    val id: String,
    val type: String,
    val name: String,
    val secondaryText: String? = null,
    val coverArtId: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TYPE_ARTIST = "ARTIST"
        const val TYPE_ALBUM = "ALBUM"
        const val TYPE_TRACK = "TRACK"
    }
}
