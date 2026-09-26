package com.naviify.app.data.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "asays_search_history")

@Serializable
enum class SearchHistoryType {
    QUERY, TRACK, ALBUM, ARTIST, PLAYLIST
}

@Serializable
data class SearchHistoryEntry(
    val type: SearchHistoryType,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val coverArtId: String? = null,
    val timestamp: Long = 0L,
    val album: String? = null,
    val albumId: String? = null,
    val duration: Int = 0,
)

@Serializable
private data class SearchHistorySnapshot(
    val entries: List<SearchHistoryEntry> = emptyList(),
)

private const val MAX_HISTORY = 30

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _entries = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    val entries: StateFlow<List<SearchHistoryEntry>> = _entries.asStateFlow()

    init {
        scope.launch {
            val raw = context.searchHistoryDataStore.data.first()[Keys.JSON]
            if (raw != null) {
                runCatching { json.decodeFromString<SearchHistorySnapshot>(raw) }
                    .onSuccess { _entries.value = it.entries }
            }
        }
    }

    fun addEntry(entry: SearchHistoryEntry) {
        scope.launch {
            val current = _entries.value.toMutableList()
            // Remove duplicate (same type + id)
            current.removeAll { it.type == entry.type && it.id == entry.id }
            // Add at front with current timestamp
            current.add(0, entry.copy(timestamp = System.currentTimeMillis()))
            // Trim to max
            val trimmed = current.take(MAX_HISTORY)
            _entries.value = trimmed
            persist(trimmed)
        }
    }

    fun removeEntry(entry: SearchHistoryEntry) {
        scope.launch {
            val updated = _entries.value.filterNot { it.type == entry.type && it.id == entry.id }
            _entries.value = updated
            persist(updated)
        }
    }

    fun clearAll() {
        scope.launch {
            _entries.value = emptyList()
            persist(emptyList())
        }
    }

    private suspend fun persist(entries: List<SearchHistoryEntry>) {
        context.searchHistoryDataStore.edit {
            it[Keys.JSON] = json.encodeToString(SearchHistorySnapshot.serializer(), SearchHistorySnapshot(entries))
        }
    }

    private object Keys {
        val JSON = stringPreferencesKey("search_history")
    }
}
