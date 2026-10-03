package com.naviify.app.domain.playback

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.naviify.app.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaybackRepeatMode {
    OFF,
    ALL,
    ONE,
}

@Immutable
data class PlayerUiState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val error: String? = null,
    val activePlaylistId: String? = null,
    val isMixBlending: Boolean = false,
    val mixProgress: Float = 0f,
    val mixOutgoingTrack: Track? = null,
) {
    val currentTrack: Track?
        get() = queue.getOrNull(currentIndex)
}

/**
 * Source of truth for what should be playing. Phase 3 attaches ExoPlayer
 * effects to these actions; until then this drives the mini player and the
 * now-playing sheet so no control is a dead button.
 */
@Singleton
class PlayerQueueStore private constructor(
    private val prefs: SharedPreferences?,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        context.getSharedPreferences("naviify_playback_prefs", Context.MODE_PRIVATE),
    )

    constructor() : this(null as SharedPreferences?)

    private val initialRepeatMode: PlaybackRepeatMode = prefs?.getString(KEY_REPEAT_MODE, null)?.let {
        runCatching { PlaybackRepeatMode.valueOf(it) }.getOrNull()
    } ?: PlaybackRepeatMode.OFF

    private val initialShuffleEnabled: Boolean = prefs?.getBoolean(KEY_SHUFFLE, false) ?: false

    private val initialActivePlaylistId: String? = prefs?.getString(KEY_ACTIVE_PLAYLIST_ID, null)

    private val _state = MutableStateFlow(
        PlayerUiState(
            repeatMode = initialRepeatMode,
            isShuffleEnabled = initialShuffleEnabled,
            activePlaylistId = initialActivePlaylistId,
        ),
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    @Volatile
    private var originalQueue: List<Track> = emptyList()

    fun play(
        queue: List<Track>,
        startIndex: Int = 0,
        startShuffled: Boolean = false,
        playlistId: String? = null,
    ) {
        if (queue.isEmpty()) return
        originalQueue = queue
        val shuffleOn = startShuffled || _state.value.isShuffleEnabled
        prefs?.edit()?.apply {
            if (startShuffled) putBoolean(KEY_SHUFFLE, true)
            putString(KEY_ACTIVE_PLAYLIST_ID, playlistId)
            apply()
        }
        val targetStart = startIndex.coerceIn(queue.indices)
        val activeQueue: List<Track>
        val activeIndex: Int
        if (shuffleOn && queue.size > 1) {
            val seedTrack = queue[targetStart]
            val rest = queue.filterIndexed { i, _ -> i != targetStart }
            activeQueue = listOf(seedTrack) + smartShuffle(rest)
            activeIndex = 0
        } else {
            activeQueue = queue
            activeIndex = targetStart
        }

        _state.update { current ->
            persistCurrentTrack(activeQueue.getOrNull(activeIndex))
            current.copy(
                queue = activeQueue,
                currentIndex = activeIndex,
                isPlaying = true,
                positionMs = 0L,
                isShuffleEnabled = shuffleOn,
                error = null,
                activePlaylistId = playlistId,
            )
        }
    }

    fun playShuffled(queue: List<Track>, playlistId: String? = null) {
        if (queue.isEmpty()) return
        val randomStart = queue.indices.random()
        play(queue, startIndex = randomStart, startShuffled = true, playlistId = playlistId)
    }

    /** Engine write-back: authoritative playback/transport state. */
    fun setIsPlaying(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
    }

    /** Engine write-back for media transitions (e.g. ended -> next). */
    fun setCurrentIndex(index: Int) {
        _state.update { current ->
            if (index in current.queue.indices) {
                persistCurrentTrack(current.queue[index])
                current.copy(currentIndex = index)
            } else current
        }
    }

    fun setPosition(positionMs: Long, durationMs: Long) {
        val newPosition = positionMs.coerceAtLeast(0)
        val newDuration = durationMs.coerceAtLeast(0)
        _state.update { current ->
            // Treat sub-second duration jitter as unchanged so VBR streams do not
            // defeat the no-op guard and double the tick rate.
            val durationStable = current.durationMs == newDuration ||
                (current.durationMs > 0 && newDuration > 0 && kotlin.math.abs(current.durationMs - newDuration) < 1_000L)
            if (current.positionMs == newPosition && durationStable) {
                current
            } else {
                current.copy(positionMs = newPosition, durationMs = newDuration)
            }
        }
    }

    fun setError(message: String?) {
        _state.update { it.copy(error = message) }
    }

    fun setMixBlending(isBlending: Boolean, progress: Float = 0f, outgoingTrack: Track? = null) {
        _state.update {
            it.copy(
                isMixBlending = isBlending,
                mixProgress = if (isBlending) progress else 0f,
                mixOutgoingTrack = if (isBlending) outgoingTrack else null,
            )
        }
    }

    fun togglePlayPause() {
        _state.update { if (it.currentTrack == null) it else it.copy(isPlaying = !it.isPlaying) }
    }

    fun next() {
        _state.update { current ->
            if (current.currentIndex < current.queue.lastIndex) {
                current.copy(currentIndex = current.currentIndex + 1, isPlaying = true)
            } else {
                current
            }
        }
    }

    fun previous() {
        _state.update { current ->
            if (current.currentIndex > 0) {
                current.copy(currentIndex = current.currentIndex - 1, isPlaying = true)
            } else {
                current
            }
        }
    }

    fun select(trackIndex: Int) {
        _state.update { current ->
            if (trackIndex in current.queue.indices) {
                persistCurrentTrack(current.queue[trackIndex])
                current.copy(currentIndex = trackIndex, isPlaying = true)
            } else {
                current
            }
        }
    }

    fun clear() {
        originalQueue = emptyList()
        _state.update { current ->
            PlayerUiState(
                repeatMode = current.repeatMode,
                isShuffleEnabled = current.isShuffleEnabled,
            )
        }
    }

    /**
     * Inserts [track] into the user's priority queue immediately after the currently
     * playing track, or after previously user-queued tracks (Spotify FIFO behavior).
     * Returns the 0-based index where the track was inserted into [PlayerUiState.queue].
     */
    fun addToUserQueue(track: Track): Int {
        val taggedTrack = track.copy(isUserQueued = true)
        var insertedIndex = -1
        _state.update { current ->
            if (current.queue.isEmpty()) {
                originalQueue = listOf(taggedTrack)
                insertedIndex = 0
                current.copy(
                    queue = listOf(taggedTrack),
                    currentIndex = 0,
                    isPlaying = true,
                )
            } else {
                val currIdx = current.currentIndex
                val insertAt = if (currIdx in current.queue.indices) {
                    var endOfUserQueue = currIdx + 1
                    while (endOfUserQueue < current.queue.size && current.queue[endOfUserQueue].isUserQueued) {
                        endOfUserQueue++
                    }
                    endOfUserQueue
                } else {
                    current.queue.size
                }

                val newQueue = current.queue.toMutableList()
                newQueue.add(insertAt, taggedTrack)

                // Sync originalQueue so un-shuffling retains user-queued songs
                val origList = originalQueue.toMutableList()
                if (currIdx in current.queue.indices) {
                    val currentTrackId = current.queue[currIdx].id
                    val origIdx = origList.indexOfFirst { it.id == currentTrackId }
                    if (origIdx >= 0) {
                        var origInsertAt = origIdx + 1
                        while (origInsertAt < origList.size && origList[origInsertAt].isUserQueued) {
                            origInsertAt++
                        }
                        origList.add(origInsertAt, taggedTrack)
                    } else {
                        origList.add(taggedTrack)
                    }
                } else {
                    origList.add(taggedTrack)
                }
                originalQueue = origList

                insertedIndex = insertAt
                current.copy(queue = newQueue)
            }
        }
        return insertedIndex
    }

    fun append(track: Track) {
        originalQueue = originalQueue + track
        _state.update { it.copy(queue = it.queue + track) }
    }

    fun seekTo(positionMs: Long) {
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0)) }
    }

    fun setShuffle(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SHUFFLE, enabled)?.apply()
        _state.update { current ->
            if (current.isShuffleEnabled == enabled) {
                return@update current
            }
            val k = current.currentIndex
            if (current.queue.isEmpty() || k !in current.queue.indices) {
                return@update current.copy(isShuffleEnabled = enabled)
            }
            val historyAndCurrent = current.queue.take(k + 1)
            val upcoming = current.queue.drop(k + 1)

            if (upcoming.isEmpty()) {
                return@update current.copy(isShuffleEnabled = enabled)
            }

            if (enabled) {
                if (originalQueue.isEmpty()) {
                    originalQueue = current.queue
                }
                val userQueued = upcoming.filter { it.isUserQueued }
                val context = upcoming.filter { !it.isUserQueued }
                val newUpcoming = userQueued + smartShuffle(context)
                current.copy(
                    queue = historyAndCurrent + newUpcoming,
                    currentIndex = k,
                    isShuffleEnabled = true,
                )
            } else {
                val userQueued = upcoming.filter { it.isUserQueued }
                val context = upcoming.filter { !it.isUserQueued }
                val restoredContext = context.sortedBy { track ->
                    val origIdx = originalQueue.indexOfFirst { it.id == track.id }
                    if (origIdx >= 0) origIdx else Int.MAX_VALUE
                }
                current.copy(
                    queue = historyAndCurrent + userQueued + restoredContext,
                    currentIndex = k,
                    isShuffleEnabled = false,
                )
            }
        }
    }

    fun reshuffle() {
        _state.update { current ->
            val k = current.currentIndex
            if (current.queue.isEmpty() || k !in current.queue.indices) return@update current
            val historyAndCurrent = current.queue.take(k + 1)
            val upcoming = current.queue.drop(k + 1)
            if (upcoming.size <= 1) return@update current

            val userQueued = upcoming.filter { it.isUserQueued }
            val context = upcoming.filter { !it.isUserQueued }
            val newUpcoming = userQueued + smartShuffle(context)
            current.copy(
                queue = historyAndCurrent + newUpcoming,
                currentIndex = k,
                isShuffleEnabled = true,
            )
        }
    }

    /**
     * Smart shuffle with artist de-clustering:
     * 1. Fisher-Yates randomization
     * 2. Anti-identity shuffle prevention (guaranteed permutation for lists with >1 item)
     * 3. Artist de-clumping (prevents tracks from same artist playing consecutively when possible)
     */
    private fun smartShuffle(tracks: List<Track>): List<Track> {
        if (tracks.size <= 1) return tracks
        var shuffled = tracks.shuffled()
        if (shuffled == tracks && tracks.size > 1) {
            shuffled = shuffled.drop(1) + shuffled.first()
        }
        if (shuffled.size > 2) {
            val balanced = mutableListOf<Track>()
            val pool = shuffled.toMutableList()
            balanced.add(pool.removeAt(0))
            while (pool.isNotEmpty()) {
                val lastArtist = balanced.last().artist
                val nextIdx = pool.indexOfFirst { it.artist != lastArtist }
                if (nextIdx >= 0) {
                    balanced.add(pool.removeAt(nextIdx))
                } else {
                    balanced.add(pool.removeAt(0))
                }
            }
            shuffled = balanced
        }
        return shuffled
    }

    fun move(fromIndex: Int, toIndex: Int) {
        _state.update { current ->
            if (fromIndex !in current.queue.indices || toIndex !in current.queue.indices || fromIndex == toIndex) {
                return@update current
            }
            val newQueue = current.queue.toMutableList()
            val item = newQueue.removeAt(fromIndex)
            newQueue.add(toIndex, item)
            val newCurrentIndex = when {
                current.currentIndex == fromIndex -> toIndex
                fromIndex < current.currentIndex && toIndex >= current.currentIndex -> current.currentIndex - 1
                fromIndex > current.currentIndex && toIndex <= current.currentIndex -> current.currentIndex + 1
                else -> current.currentIndex
            }
            current.copy(queue = newQueue, currentIndex = newCurrentIndex)
        }
    }

    fun setRepeatMode(mode: PlaybackRepeatMode) {
        prefs?.edit()?.putString(KEY_REPEAT_MODE, mode.name)?.apply()
        _state.update { it.copy(repeatMode = mode) }
    }

    /** Removes [index] locally, shifting the current index when needed. */
    fun removeAt(index: Int) {
        _state.update { current ->
            if (index !in current.queue.indices) return@update current
            val removedTrack = current.queue[index]
            val newQueue = current.queue.toMutableList().apply { removeAt(index) }
            val origList = originalQueue.toMutableList()
            val origIdx = origList.indexOfFirst { it.id == removedTrack.id }
            if (origIdx >= 0) origList.removeAt(origIdx)
            originalQueue = origList

            if (newQueue.isEmpty()) return@update PlayerUiState(
                repeatMode = current.repeatMode,
                isShuffleEnabled = current.isShuffleEnabled,
            )
            val newIndex = when {
                index < current.currentIndex -> (current.currentIndex - 1).coerceIn(newQueue.indices)
                index == current.currentIndex -> current.currentIndex.coerceAtMost(newQueue.lastIndex)
                else -> current.currentIndex
            }
            current.copy(queue = newQueue, currentIndex = newIndex)
        }
    }

    private fun persistCurrentTrack(track: Track?) {
        if (track == null || prefs == null) return
        prefs.edit().apply {
            putString(KEY_LAST_TRACK_ID, track.id)
            putString(KEY_LAST_TRACK_TITLE, track.title)
            putString(KEY_LAST_TRACK_ARTIST, track.artist.orEmpty())
            putString(KEY_LAST_TRACK_COVER_ID, track.coverArtId.orEmpty())
            apply()
        }
    }

    fun setActivePlaylistId(playlistId: String?) {
        prefs?.edit()?.putString(KEY_ACTIVE_PLAYLIST_ID, playlistId)?.apply()
        _state.update { it.copy(activePlaylistId = playlistId) }
    }

    companion object {
        private const val KEY_SHUFFLE = "is_shuffle_enabled"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_LAST_TRACK_ID = "last_track_id"
        const val KEY_LAST_TRACK_TITLE = "last_track_title"
        const val KEY_LAST_TRACK_ARTIST = "last_track_artist"
        const val KEY_LAST_TRACK_COVER_ID = "last_track_cover_id"
        private const val KEY_ACTIVE_PLAYLIST_ID = "active_playlist_id"
    }
}
