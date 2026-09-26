package com.naviify.app.domain.playback

import com.naviify.app.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueStoreTest {

    private val store = PlayerQueueStore()

    private val tracks = listOf(
        Track(id = "t1", title = "One"),
        Track(id = "t2", title = "Two"),
        Track(id = "t3", title = "Three"),
    )

    @Test
    fun `play starts at requested index and isPlaying`() {
        store.play(tracks, startIndex = 1)

        val state = store.state.value
        assertEquals(1, state.currentIndex)
        assertTrue(state.isPlaying)
        assertEquals("Two", state.currentTrack?.title)
    }

    @Test
    fun `next advances and previous goes back`() {
        store.play(tracks)
        store.next()
        assertEquals(1, store.state.value.currentIndex)
        store.previous()
        assertEquals(0, store.state.value.currentIndex)
    }

    @Test
    fun `next clamps at queue end`() {
        store.play(tracks, startIndex = 2)
        store.next()
        assertEquals(2, store.state.value.currentIndex)
    }

    @Test
    fun `toggle play pause flips state`() {
        store.play(tracks)
        store.togglePlayPause()
        assertFalse(store.state.value.isPlaying)
        store.togglePlayPause()
        assertTrue(store.state.value.isPlaying)
    }

    @Test
    fun `select out of range is ignored`() {
        store.play(tracks)
        store.select(99)
        assertEquals(0, store.state.value.currentIndex)
    }

    @Test
    fun `clear resets to empty`() {
        store.play(tracks)
        store.clear()
        assertNull(store.state.value.currentTrack)
        assertEquals(0, store.state.value.queue.size)
    }

    @Test
    fun `seekTo updates local position`() {
        store.play(tracks)
        store.seekTo(1_500)
        assertEquals(1_500, store.state.value.positionMs)
    }

    @Test
    fun `engine position and duration write back`() {
        store.play(tracks)
        store.setPosition(2_000, 3_000)
        assertEquals(2_000, store.state.value.positionMs)
        assertEquals(3_000, store.state.value.durationMs)
    }

    @Test
    fun `shuffle and repeat toggles`() {
        store.play(tracks)
        store.setShuffle(true)
        assertTrue(store.state.value.isShuffleEnabled)
        store.setRepeatMode(PlaybackRepeatMode.ALL)
        assertEquals(PlaybackRepeatMode.ALL, store.state.value.repeatMode)
    }

    @Test
    fun `play preserves shuffle and repeat mode`() {
        store.setShuffle(true)
        store.setRepeatMode(PlaybackRepeatMode.ALL)
        store.play(tracks)
        assertTrue(store.state.value.isShuffleEnabled)
        assertEquals(PlaybackRepeatMode.ALL, store.state.value.repeatMode)
    }

    @Test
    fun `engine media transition updates current index`() {
        store.play(tracks)
        store.setCurrentIndex(2)
        assertEquals(2, store.state.value.currentIndex)
    }

    @Test
    fun `error is set then cleared on new queue`() {
        store.setError("boom")
        assertEquals("boom", store.state.value.error)
        store.play(tracks)
        assertNull(store.state.value.error)
    }

    @Test
    fun `removeAt before current shifts index back`() {
        store.play(tracks, startIndex = 2)
        store.removeAt(0)
        assertEquals(1, store.state.value.currentIndex)
        assertEquals(2, store.state.value.queue.size)
    }

    @Test
    fun `removeAt current keeps index pointing at next track`() {
        store.play(tracks, startIndex = 0)
        store.removeAt(0)
        assertEquals(0, store.state.value.currentIndex)
        assertEquals("Two", store.state.value.currentTrack?.title)
    }

    @Test
    fun `removeAt last item clears the queue`() {
        store.play(listOf(tracks.first()))
        store.removeAt(0)
        assertNull(store.state.value.currentTrack)
        assertTrue(store.state.value.queue.isEmpty())
    }

    @Test
    fun `removeAt out of range is ignored`() {
        store.play(tracks)
        store.removeAt(99)
        assertEquals(3, store.state.value.queue.size)
    }

    @Test
    fun `setShuffle true shuffles upcoming tracks while preserving current track`() {
        val testTracks = (1..10).map { Track(id = "t$it", title = "Track $it") }
        store.play(testTracks, startIndex = 0)
        store.setShuffle(true)

        val state = store.state.value
        assertTrue(state.isShuffleEnabled)
        assertEquals(0, state.currentIndex)
        assertEquals("t1", state.currentTrack?.id)
        assertEquals(10, state.queue.size)
        assertEquals(testTracks.map { it.id }.toSet(), state.queue.map { it.id }.toSet())
    }

    @Test
    fun `setShuffle false restores original order at current track position`() {
        val testTracks = (1..10).map { Track(id = "t$it", title = "Track $it") }
        store.play(testTracks, startIndex = 0)
        store.setShuffle(true)
        store.setShuffle(false)

        val state = store.state.value
        assertFalse(state.isShuffleEnabled)
        assertEquals(testTracks.map { it.id }, state.queue.map { it.id })
        assertEquals(0, state.currentIndex)
        assertEquals("t1", state.currentTrack?.id)
    }

    @Test
    fun `move reorders track in queue and maintains currentIndex`() {
        val testTracks = listOf(
            Track(id = "t1", title = "One"),
            Track(id = "t2", title = "Two"),
            Track(id = "t3", title = "Three"),
            Track(id = "t4", title = "Four"),
        )
        store.play(testTracks, startIndex = 0)
        // Move t4 (index 3) to index 1 (directly after playing track)
        store.move(3, 1)

        val state = store.state.value
        assertEquals(listOf("t1", "t4", "t2", "t3"), state.queue.map { it.id })
        assertEquals(0, state.currentIndex)
        assertEquals("t1", state.currentTrack?.id)
    }

    @Test
    fun `move updates currentIndex when current playing track is moved`() {
        val testTracks = listOf(
            Track(id = "t1", title = "One"),
            Track(id = "t2", title = "Two"),
            Track(id = "t3", title = "Three"),
        )
        store.play(testTracks, startIndex = 0)
        // Move current track (index 0) to end (index 2)
        store.move(0, 2)

        val state = store.state.value
        assertEquals(listOf("t2", "t3", "t1"), state.queue.map { it.id })
        assertEquals(2, state.currentIndex)
        assertEquals("t1", state.currentTrack?.id)
    }

    @Test
    fun `reshuffle reorders tracks and maintains currentTrack`() {
        val testTracks = (1..10).map { Track(id = "t$it", title = "Track $it") }
        store.play(testTracks, startIndex = 0, startShuffled = true)
        store.reshuffle()

        val state = store.state.value
        assertTrue(state.isShuffleEnabled)
        assertEquals(0, state.currentIndex)
        assertEquals("t1", state.currentTrack?.id)
        assertEquals(10, state.queue.size)
        assertEquals(testTracks.map { it.id }.toSet(), state.queue.map { it.id }.toSet())
    }

    @Test
    fun `addToUserQueue inserts single track directly after currently playing track`() {
        store.play(tracks, startIndex = 0)
        val insertedIdx = store.addToUserQueue(Track(id = "tx", title = "Queued X"))

        assertEquals(1, insertedIdx)
        val state = store.state.value
        assertEquals(listOf("t1", "tx", "t2", "t3"), state.queue.map { it.id })
        assertEquals(0, state.currentIndex)
        assertTrue(state.queue[1].isUserQueued)
        assertFalse(state.queue[0].isUserQueued)
        assertFalse(state.queue[2].isUserQueued)
    }

    @Test
    fun `addToUserQueue appends consecutive tracks in Spotify FIFO order`() {
        store.play(tracks, startIndex = 0)
        val idx1 = store.addToUserQueue(Track(id = "tx", title = "Queued X"))
        val idx2 = store.addToUserQueue(Track(id = "ty", title = "Queued Y"))
        val idx3 = store.addToUserQueue(Track(id = "tz", title = "Queued Z"))

        assertEquals(1, idx1)
        assertEquals(2, idx2)
        assertEquals(3, idx3)

        val state = store.state.value
        assertEquals(listOf("t1", "tx", "ty", "tz", "t2", "t3"), state.queue.map { it.id })
        assertEquals(0, state.currentIndex)
        assertTrue(state.queue[1].isUserQueued)
        assertTrue(state.queue[2].isUserQueued)
        assertTrue(state.queue[3].isUserQueued)
        assertFalse(state.queue[4].isUserQueued)
    }

    @Test
    fun `addToUserQueue while playing user queued track inserts at end of user queue`() {
        store.play(tracks, startIndex = 0)
        store.addToUserQueue(Track(id = "tx", title = "Queued X"))
        store.addToUserQueue(Track(id = "ty", title = "Queued Y"))
        store.addToUserQueue(Track(id = "tz", title = "Queued Z"))

        // Advance to tx (index 1)
        store.setCurrentIndex(1)
        assertEquals("tx", store.state.value.currentTrack?.id)

        // Add tw to queue: should go after tz (index 4), before t2
        val idxW = store.addToUserQueue(Track(id = "tw", title = "Queued W"))
        assertEquals(4, idxW)

        val state = store.state.value
        assertEquals(listOf("t1", "tx", "ty", "tz", "tw", "t2", "t3"), state.queue.map { it.id })
        assertEquals(1, state.currentIndex)
        assertTrue(state.queue[4].isUserQueued)
    }

    @Test
    fun `addToUserQueue on empty queue initializes and plays track`() {
        val emptyStore = PlayerQueueStore()
        val idx = emptyStore.addToUserQueue(Track(id = "t_new", title = "Fresh"))

        assertEquals(0, idx)
        val state = emptyStore.state.value
        assertEquals(1, state.queue.size)
        assertEquals("t_new", state.currentTrack?.id)
        assertTrue(state.isPlaying)
        assertTrue(state.queue[0].isUserQueued)
    }
}
