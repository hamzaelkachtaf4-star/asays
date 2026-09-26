package com.naviify.app.data.lyrics

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LrclibLyricsClientTest {

    private lateinit var server: MockWebServer
    private val client = LrclibLyricsClient(OkHttpClient())

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses synced LRC into lyrics lines`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"syncedLyrics":"[00:01.50]Hello world\n[00:04.00]Second line"}"""),
        )

        val result = client.fetch("ElGrandeToto", "Track Name", 240, server.url("/").toString())

        assertEquals(2, result?.syncedLines?.size)
        assertEquals(1_500L, result!!.syncedLines[0].startMs)
        assertEquals("Hello world", result.syncedLines[0].text)
        assertEquals(4_000L, result.syncedLines[1].startMs)
    }

    @Test
    fun `returns null for non-200 response`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))

        val result = client.fetch("Artist", "Missing", 120, server.url("/").toString())

        assertNull(result)
    }

    @Test
    fun `falls back to search when get returns 404`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    "[{\"id\":1,\"artistName\":\"Rudy Mancuso\",\"trackName\":\"Mama\",\"duration\":240," +
                        "\"syncedLyrics\":\"[00:02.00]Found via search\"}," +
                        "{\"id\":2,\"artistName\":\"Other\",\"trackName\":\"Mama\",\"duration\":240}]",
                ),
        )

        val result = client.fetch("Rudy Mancuso", "Mama", 240, server.url("/").toString())

        assertEquals(1, result?.syncedLines?.size)
        assertEquals("Found via search", result!!.syncedLines.first().text)
    }

    @Test
    fun `sanitizes featuring artists`() {
        assertEquals("Rudy Mancuso", sanitizeLyricsArtist("Rudy Mancuso \u2022 Ori Rakib \u2022 Jamie Almos"))
        assertEquals("DJ Snake", sanitizeLyricsArtist("DJ Snake & Lena Meyer-Landrut"))
        assertEquals("Aya Nakamura", sanitizeLyricsArtist("Aya Nakamura feat. Damso"))
        assertEquals("", sanitizeLyricsArtist(null))
    }

    @Test
    fun `sanitizes titles with parenthesized tags`() {
        assertEquals("Mama", sanitizeLyricsTitle("Mama (Official Video)"))
        assertEquals("Sans nuance", sanitizeLyricsTitle("Sans nuance (feat. Soolking)"))
        assertEquals("", sanitizeLyricsTitle("   "))
    }

    @Test
    fun `rejects candidate if duration difference exceeds 6 seconds`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    "[{\"id\":1,\"artistName\":\"Coldplay\",\"trackName\":\"Yellow\",\"duration\":300," +
                        "\"syncedLyrics\":\"[00:02.00]Look at the stars\"}]",
                ),
        )

        // Song is 240s, candidate is 300s (delta = 60s > 6s) -> rejected to avoid wrong song
        val result = client.fetch("Coldplay", "Yellow", 240, server.url("/").toString())
        assertNull(result)
    }

    @Test
    fun `rejects candidate if artist name does not match`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    "[{\"id\":1,\"artistName\":\"Justin Bieber\",\"trackName\":\"Sorry\",\"duration\":200," +
                        "\"syncedLyrics\":\"[00:02.00]Is it too late now\"}]",
                ),
        )

        // Querying for The Motels - Sorry (200s), candidate is Justin Bieber - Sorry (200s) -> rejected!
        val result = client.fetch("The Motels", "Sorry", 200, server.url("/").toString())
        assertNull(result)
    }

    @Test
    fun `searchCandidates returns candidates list and candidateToLyricsData converts properly`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    "[{\"id\":123,\"artistName\":\"Adele\",\"trackName\":\"Hello\",\"duration\":295," +
                        "\"syncedLyrics\":\"[00:05.00]Hello from the other side\"," +
                        "\"plainLyrics\":\"Hello from the other side\"}]",
                ),
        )

        val candidates = client.searchCandidates("Adele Hello", server.url("/").toString())
        assertEquals(1, candidates.size)
        val first = candidates.first()
        assertEquals(123L, first.id)
        assertEquals("Adele", first.artistName)
        assertEquals("Hello", first.trackName)
        assertEquals(true, first.isSynced)

        val lyricsData = client.candidateToLyricsData(first)
        assertEquals(1, lyricsData.syncedLines.size)
        assertEquals("Hello from the other side", lyricsData.syncedLines.first().text)
        assertEquals(5000L, lyricsData.syncedLines.first().startMs)
    }
}
