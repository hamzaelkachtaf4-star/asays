package com.naviify.app.core.network

import com.naviify.app.core.network.dto.SubsonicEnvelope
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicDtoParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `parses ping response with server metadata`() {
        val envelope = parse("ping.json")
        assertTrue(envelope.response.isOk)
        assertEquals("navidrome", envelope.response.type)
        assertEquals("0.52.5", envelope.response.serverVersion)
        assertTrue(envelope.response.openSubsonic)
    }

    @Test
    fun `parses getArtists index`() {
        val envelope = parse("getartists.json")
        val artist = envelope.response.artists!!.index.first().artist.first()
        assertEquals("Daft Punk", artist.name)
        assertEquals(9, artist.albumCount)
        assertEquals("al-6f8f9c0a", artist.coverArt)
        assertTrue(artist.starred == true)
    }

    @Test
    fun `parses search3 result`() {
        val envelope = parse("search3.json")
        val result = envelope.response.searchResult3!!

        assertEquals(1, result.artist.size)
        assertEquals("Radiohead", result.artist.first().name)

        val album = result.album.first()
        assertEquals("OK Computer", album.name)
        assertEquals(12, album.songCount)
        assertEquals(6425, album.duration)

        val song = result.song.first()
        assertEquals("tr1", song.id)
        assertEquals("Paranoid Android", song.title)
        assertEquals(2, song.track)
        assertEquals(1, song.discNumber)
        assertEquals(383, song.duration)
        assertEquals("audio/mpeg", song.contentType)
        assertEquals("mp3", song.suffix)
        assertEquals(4L, song.playCount)
    }

    @Test
    fun `parses playlist summaries including public flag`() {
        val envelope = parse("playlist.json")
        val playlist = envelope.response.playlists!!.playlist.first()
        assertEquals("pl1", playlist.id)
        assertEquals("Morning Run", playlist.name)
        assertTrue(playlist.`public` == true)
        assertEquals(24, playlist.songCount)
    }

    @Test
    fun `parses playlist detail with song entries`() {
        val envelope = parse("playlistdetail.json")
        val playlist = envelope.response.playlist!!
        assertEquals(2, playlist.entry.size)
        assertEquals("Song One", playlist.entry.first().title)
    }

    @Test
    fun `parses randomSongs with song entries`() {
        val envelope = parse("randomsongs.json")
        val songs = envelope.response.randomSongs!!
        assertEquals(1, songs.song.size)
        assertEquals("Paranoid Android", songs.song.first().title)
    }

    @Test
    fun `parses failed response with error payload`() {
        val envelope = parse("error.json")
        assertFalse(envelope.response.isOk)
        assertNotNull(envelope.response.error)
        assertEquals(40, envelope.response.error!!.code)
        assertEquals("Wrong username or password.", envelope.response.error!!.message)
    }

    @Test
    fun `parses album detail`() {
        val envelope = parse("album.json")
        val album = envelope.response.album!!
        assertEquals("al-okcomputer", album.id)
        assertEquals(12, album.songCount)
        assertEquals("Radiohead", album.artist)
        assertEquals(2, album.song.size)
    }

    @Test
    fun `parses albumList2`() {
        val envelope = parse("albumlist2.json")
        val albums = envelope.response.albumList2!!.album
        assertEquals(2, albums.size)
        assertEquals("Random Access Memories", albums.first().name)
        assertEquals(2013, albums.first().year)
    }

    @Test
    fun `parses OpenSubsonic structured lyrics`() {
        val envelope = parse("lyrics_structured.json")
        val lines = envelope.response.structuredLyrics.first().line
        assertEquals(2, lines.size)
        assertEquals(12_000L, lines.first().start)
        assertEquals("First line", lines.first().value)
    }

    @Test
    fun `parses legacy lyrics payload`() {
        val envelope = parse("lyrics_legacy.json")
        val lyric = envelope.response.lyrics!!.lyric
        assertTrue(lyric!!.contains("[00:12.00]In the next world war"))
    }

    private fun parse(fileName: String): SubsonicEnvelope {
        val content = requireNotNull(javaClass.classLoader!!.getResource(fileName)) {
            "Missing test resource $fileName"
        }.readText()
        return json.decodeFromString(content)
    }
}
