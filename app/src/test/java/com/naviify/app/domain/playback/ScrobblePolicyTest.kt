package com.naviify.app.domain.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrobblePolicyTest {

    @Test
    fun `scrobbles after half the duration`() {
        assertTrue(ScrobblePolicy.shouldScrobble(120_000, 240_000))
        assertFalse(ScrobblePolicy.shouldScrobble(119_999, 240_000))
    }

    @Test
    fun `caps scrobble at four minutes regardless of duration`() {
        assertTrue(ScrobblePolicy.shouldScrobble(240_000, 3_600_000))
        assertFalse(ScrobblePolicy.shouldScrobble(239_999, 3_600_000))
    }

    @Test
    fun `short tracks scrobble at half, not four minutes`() {
        assertTrue(ScrobblePolicy.shouldScrobble(90_000, 180_000))
        assertFalse(ScrobblePolicy.shouldScrobble(89_999, 180_000))
    }

    @Test
    fun `ignores zero duration or untouched position`() {
        assertFalse(ScrobblePolicy.shouldScrobble(0, 240_000))
        assertFalse(ScrobblePolicy.shouldScrobble(60_000, 0))
    }
}
