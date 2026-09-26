package com.naviify.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerUrlTest {

    @Test
    fun `strips trailing rest segment`() {
        assertEquals(
            "https://music.example.com",
            normalizeServerUrl("https://music.example.com/rest"),
        )
        assertEquals(
            "https://music.example.com",
            normalizeServerUrl("https://music.example.com/rest/"),
        )
    }

    @Test
    fun `keeps custom base path but drops its rest suffix`() {
        assertEquals(
            "https://music.example.com/navidrome",
            normalizeServerUrl("https://music.example.com/navidrome/rest"),
        )
    }

    @Test
    fun `adds a default scheme`() {
        assertEquals(
            "http://music.example.com",
            normalizeServerUrl("music.example.com"),
        )
    }

    @Test
    fun `trims trailing slashes`() {
        assertEquals(
            "https://music.example.com",
            normalizeServerUrl("https://music.example.com/"),
        )
    }

    @Test
    fun `blank input stays blank`() {
        assertEquals("", normalizeServerUrl("   "))
    }

    @Test
    fun `rejects unsupported schemes`() {
        assertEquals("", normalizeServerUrl("ftp://10.0.0.1:4533"))
        assertEquals("", normalizeServerUrl("file:///music"))
    }

    @Test
    fun `keeps http and https schemes`() {
        assertEquals("http://192.168.11.243:4533", normalizeServerUrl("http://192.168.11.243:4533/"))
        assertEquals("https://tail.example.com", normalizeServerUrl("https://tail.example.com"))
    }

    @Test
    fun `strips rest suffix from custom path`() {
        assertEquals("http://10.0.0.5/navidrome", normalizeServerUrl("http://10.0.0.5/navidrome/rest"))
    }
}
