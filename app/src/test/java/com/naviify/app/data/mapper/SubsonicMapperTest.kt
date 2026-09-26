package com.naviify.app.data.mapper

import com.naviify.app.core.network.dto.SubsonicEnvelope
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `playlist detail maps to domain with tracks`() {
        val envelope = parse("playlistdetail.json")
        val playlist = envelope.response.playlist!!.toDomain()

        assertEquals("Morning Run", playlist.name)
        assertEquals(2, playlist.songCount)
        assertFalse(playlist.isPublic)
        assertEquals(2, playlist.tracks.size)
        assertEquals("Song One", playlist.tracks.first().title)
        assertEquals("Artist A", playlist.tracks.first().artist)
    }

    @Test
    fun `album detail maps to domain with track list`() {
        val envelope = parse("album.json")
        val detail = envelope.response.album!!.toDomain()

        assertEquals("OK Computer", detail.album.name)
        assertEquals("Radiohead", detail.album.artist)
        assertEquals(12, detail.album.songCount)
        assertEquals(2, detail.tracks.size)
        assertEquals("Airbag", detail.tracks.first().title)
    }

    @Test
    fun `artist with albums maps both levels`() {
        val envelope = parse("artist.json")
        val detail = envelope.response.artist!!.toDomain()

        assertEquals("Daft Punk", detail.artist.name)
        assertEquals(9, detail.artist.albumCount)
        assertEquals(2, detail.albums.size)
        assertEquals("Discovery", detail.albums.first().name)
        assertTrue(detail.albums.first().isFavorite)
    }

    @Test
    fun `sanitizePlaylistName correctly decodes percent encoded spaces and symbols`() {
        assertEquals("fuck off", sanitizePlaylistName("fuck%20off"))
        assertEquals("fuck off", sanitizePlaylistName("fuck%2520off"))
        assertEquals("Normal Name", sanitizePlaylistName("Normal Name"))
        assertEquals("Rock & Roll", sanitizePlaylistName("Rock%20%26%20Roll"))
    }

    private fun parse(fileName: String): SubsonicEnvelope {
        val content = requireNotNull(javaClass.classLoader!!.getResource(fileName)) {
            "Missing test resource $fileName"
        }.readText()
        return json.decodeFromString(content)
    }
}
