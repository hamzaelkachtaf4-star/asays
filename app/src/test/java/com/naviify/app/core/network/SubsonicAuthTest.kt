package com.naviify.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicAuthTest {

    @Test
    fun `token is md5 of secret plus salt`() {
        assertEquals(
            "a5ea6e2303158e93de59aa7ff7c98782",
            SubsonicAuth.token(secret = "pw", salt = "abc123"),
        )
    }

    @Test
    fun `token auth uses the api token when provided`() {
        assertEquals(
            "dfc434c19d876fb6b33157fb69f61c49",
            SubsonicAuth.token(secret = "mytoken", salt = "xyz"),
        )
    }

    @Test
    fun `salt is alphanumeric and random`() {
        val first = SubsonicAuth.salt()
        val second = SubsonicAuth.salt()
        assertEquals(12, first.length)
        assertTrue(first.all { it.isLetterOrDigit() })
        assertTrue(first != second)
    }

    @Test
    fun `client advertises subsonic api version`() {
        assertEquals("1.16.1", SubsonicAuth.API_VERSION)
        assertEquals("ASAYS", SubsonicAuth.CLIENT_NAME)
    }
}
