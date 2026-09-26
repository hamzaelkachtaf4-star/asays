package com.naviify.app.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamQualityTest {

    @Test
    fun `lossless omits maxBitRate and presets map to kbps`() {
        assertNull(StreamQuality.LOSSLESS.maxBitRate)
        assertEquals(320, StreamQuality.HIGH.maxBitRate)
        assertEquals(192, StreamQuality.BALANCED.maxBitRate)
        assertEquals(128, StreamQuality.DATA_SAVER.maxBitRate)
    }

    @Test
    fun `resolveQuality picks per-network preference`() {
        assertEquals(
            StreamQuality.HIGH,
            resolveQuality(StreamQuality.HIGH, StreamQuality.DATA_SAVER, isWifi = true),
        )
        assertEquals(
            StreamQuality.DATA_SAVER,
            resolveQuality(StreamQuality.HIGH, StreamQuality.DATA_SAVER, isWifi = false),
        )
    }

    @Test
    fun `fromStorage falls back to lossless`() {
        assertEquals(StreamQuality.LOSSLESS, StreamQuality.fromStorage("unknown"))
        assertEquals(StreamQuality.BALANCED, StreamQuality.fromStorage("BALANCED"))
        assertEquals(StreamQuality.LOSSLESS, StreamQuality.fromStorage(null))
    }
}
