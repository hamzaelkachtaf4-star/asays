package com.naviify.app.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyLyricsParserTest {

    @Test
    fun `parses mm ss centiseconds timestamps`() {
        val lines = parseLegacyLyrics("[01:05.50]Hello world\n[02:30.00]Second line")
        assertEquals(2, lines.size)
        assertEquals(65_500L, lines[0].startMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(150_000L, lines[1].startMs)
    }

    @Test
    fun `keeps untimed lines and drops blanks`() {
        val lines = parseLegacyLyrics("Intro\n\nJust plain text\n   ")
        assertEquals(2, lines.size)
        assertNull(lines[0].startMs)
        assertEquals("Just plain text", lines[1].text)
    }

    @Test
    fun `untimed lyric with no content yields nothing`() {
        assertEquals(0, parseLegacyLyrics("\n   \n").size)
    }

    @Test
    fun `malformed timestamps degrade to plain lines without crashing`() {
        val lines = parseLegacyLyrics("[99:99.99]bad time\n[abc]tag only\n[01:02]valid\n")
        assertEquals(3, lines.size)
        assertEquals("bad time", lines[0].text)
        assertEquals("[abc]tag only", lines[1].text)
        assertEquals(62_000L, lines[2].startMs)
    }

    @Test
    fun `embedded bracket text is treated as plain text`() {
        val lines = parseLegacyLyrics("chorus [01:02] inside\n")
        assertEquals(1, lines.size)
        assertNull(lines[0].startMs)
        assertEquals("chorus [01:02] inside", lines[0].text)
    }
}
