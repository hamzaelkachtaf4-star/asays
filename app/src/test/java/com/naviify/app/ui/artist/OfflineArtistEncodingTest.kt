package com.naviify.app.ui.artist

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class OfflineArtistEncodingTest {

    @Test
    fun `artist names with special characters encode to url-safe identifier`() {
        val problematicNames = listOf(
            "AC/DC",
            "Wham! / George Michael",
            "Artist? With=Query&Parameters",
            "Rock & Roll #1",
            "100% Cotton",
            "Artist with spaces",
            "Saad Lamjarred / سعد لمجرد",
            "Fayrouz (فيروز) #Classic",
        )

        for (name in problematicNames) {
            val encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.toByteArray(Charsets.UTF_8))
            val safeId = "offline-artist-$encodedPayload"

            // Ensure no invalid URI characters survive in the route ID
            assert(!safeId.contains("/"))
            assert(!safeId.contains("?"))
            assert(!safeId.contains("#"))
            assert(!safeId.contains("&"))
            assert(!safeId.contains(" "))
            assert(!safeId.contains("%"))

            // Ensure symmetric decoding recovers exact raw artist name
            val decodedPayload = safeId.removePrefix("offline-artist-")
            val recoveredName = String(Base64.getUrlDecoder().decode(decodedPayload), Charsets.UTF_8)
            assertEquals(name, recoveredName)
        }
    }

    @Test
    fun `fallback decoding supports legacy plain or url-encoded strings`() {
        val legacyId = "offline-artist-Queen"
        val payload = legacyId.removePrefix("offline-artist-")
        val recovered = runCatching {
            String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        }.getOrElse {
            runCatching { java.net.URLDecoder.decode(payload, "UTF-8") }.getOrDefault(payload)
        }
        assertEquals("Queen", recovered)
    }
}
