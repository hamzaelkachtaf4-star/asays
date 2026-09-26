package com.naviify.app.data.lyrics

import com.naviify.app.domain.model.LyricsData
import com.naviify.app.domain.model.LyricsLineData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CustomLyricsStoreTest {

    private lateinit var tempDir: File
    private lateinit var store: CustomLyricsStore

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("asays_custom_lyrics_test").toFile()
        store = CustomLyricsStore(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `save and get custom lyrics by trackId`() {
        val lyrics = LyricsData(
            syncedLines = listOf(
                LyricsLineData(startMs = 1000L, text = "First line"),
                LyricsLineData(startMs = 5000L, text = "Second line"),
            ),
            offsetMs = 500L,
        )

        store.saveCustomLyrics(
            trackId = "track-101",
            artist = "Coldplay",
            title = "Yellow",
            lyrics = lyrics,
        )

        val retrieved = store.getCustomLyrics("track-101", "Coldplay", "Yellow")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isCustom)
        assertEquals(500L, retrieved.offsetMs)
        assertEquals(2, retrieved.syncedLines.size)
        assertEquals("First line", retrieved.syncedLines[0].text)
        assertTrue(store.hasCustomLyrics("track-101"))
    }

    @Test
    fun `get custom lyrics by fingerprint when trackId is unknown or changed`() {
        val lyrics = LyricsData(
            syncedLines = listOf(LyricsLineData(startMs = 2000L, text = "Synced line")),
            offsetMs = -200L,
        )

        store.saveCustomLyrics(
            trackId = "old-server-id-888",
            artist = "Daft Punk",
            title = "Get Lucky",
            lyrics = lyrics,
        )

        // Query with new server id, but matching artist and title
        val retrieved = store.getCustomLyrics(
            trackId = "new-server-id-999",
            artist = "Daft Punk",
            title = "Get Lucky",
        )

        assertNotNull(retrieved)
        assertTrue(retrieved!!.isCustom)
        assertEquals(-200L, retrieved.offsetMs)
        assertEquals("Synced line", retrieved.syncedLines[0].text)
    }

    @Test
    fun `delete custom lyrics removes from cache and disk`() {
        val lyrics = LyricsData(
            syncedLines = listOf(LyricsLineData(startMs = 3000L, text = "Line")),
        )

        store.saveCustomLyrics(
            trackId = "track-remove",
            artist = "Adele",
            title = "Hello",
            lyrics = lyrics,
        )

        assertTrue(store.hasCustomLyrics("track-remove", "Adele", "Hello"))

        store.deleteCustomLyrics("track-remove", "Adele", "Hello")

        assertNull(store.getCustomLyrics("track-remove", "Adele", "Hello"))
        assertFalse(store.hasCustomLyrics("track-remove", "Adele", "Hello"))
    }

    @Test
    fun `toLrcString formats standard lrc with offset incorporated`() {
        val lyrics = LyricsData(
            syncedLines = listOf(
                LyricsLineData(startMs = 65000L, text = "Look at the stars"),
                LyricsLineData(startMs = 70500L, text = "Look how they shine for you"),
            ),
            offsetMs = 500L, // +0.5s -> 65.5s and 71.0s
        )

        val lrc = lyrics.toLrcString(title = "Yellow", artist = "Coldplay")

        assertTrue(lrc.contains("[ti:Yellow]"))
        assertTrue(lrc.contains("[ar:Coldplay]"))
        assertTrue(lrc.contains("[01:05.50]Look at the stars"))
        assertTrue(lrc.contains("[01:11.00]Look how they shine for you"))
    }

    @Test
    fun `toLrcString falls back to plainText when no synced lines`() {
        val lyrics = LyricsData(
            plainText = "Line one\nLine two",
        )

        val lrc = lyrics.toLrcString()
        assertEquals("Line one\nLine two", lrc)
    }
}
