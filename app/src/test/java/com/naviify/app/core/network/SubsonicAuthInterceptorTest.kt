package com.naviify.app.core.network

import com.naviify.app.core.storage.ServerConfig
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.TimeUnit

class SubsonicAuthInterceptorTest {

    @Test
    fun `adds auth params with md5 token derived from salt`() {
        val session = SessionStateHolder().apply {
            publish(
                ServerConfig(
                    serverUrl = "https://music.example.com",
                    username = "user",
                    password = "pw",
                ),
            )
        }
        val interceptor = SubsonicAuthInterceptor(session)
        val chain = RecordingChain(
            request = Request.Builder()
                .url("https://music.example.com/rest/ping.view")
                .build(),
        )

        interceptor.intercept(chain)

        val url = chain.capturedRequest!!.url
        assertEquals("user", url.queryParameter("u"))
        assertEquals("1.16.1", url.queryParameter("v"))
        assertEquals("ASAYS", url.queryParameter("c"))
        assertEquals("json", url.queryParameter("f"))
        val salt = url.queryParameter("s")
        assertNotNull(salt)
        assertEquals(SubsonicAuth.token("pw", salt!!), url.queryParameter("t"))
    }

    @Test
    fun `api token takes precedence over password`() {
        val session = SessionStateHolder().apply {
            publish(
                ServerConfig(
                    serverUrl = "https://music.example.com",
                    username = "user",
                    password = "pw",
                    token = "mytoken",
                ),
            )
        }
        val interceptor = SubsonicAuthInterceptor(session)
        val chain = RecordingChain(
            request = Request.Builder()
                .url("https://music.example.com/rest/ping.view")
                .build(),
        )

        interceptor.intercept(chain)

        val salt = chain.capturedRequest!!.url.queryParameter("s")!!
        assertEquals(SubsonicAuth.token("mytoken", salt), chain.capturedRequest!!.url.queryParameter("t"))
    }

    @Test
    fun `never attaches credentials to an external host`() {
        val session = SessionStateHolder().apply {
            publish(
                ServerConfig(
                    serverUrl = "https://music.example.com",
                    username = "user",
                    password = "pw",
                ),
            )
        }
        val interceptor = SubsonicAuthInterceptor(session)
        val chain = RecordingChain(
            request = Request.Builder()
                .url("https://lrclib.net/api/get?artist_name=a&track_name=b")
                .build(),
        )

        interceptor.intercept(chain)

        val url = chain.capturedRequest!!.url
        assertNull(url.queryParameter("u"))
        assertNull(url.queryParameter("t"))
        assertNull(url.queryParameter("s"))
        assertEquals("a", url.queryParameter("artist_name"))
    }

    @Test
    fun `throws when no server is configured`() {
        val interceptor = SubsonicAuthInterceptor(SessionStateHolder())
        val chain = RecordingChain(
            request = Request.Builder()
                .url("https://example.test/rest/ping.view")
                .build(),
        )

        assertThrows(ServerNotConfiguredException::class.java) {
            interceptor.intercept(chain)
        }
    }

    private class RecordingChain(
        private val request: Request,
    ) : Interceptor.Chain {

        var capturedRequest: Request? = null

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            capturedRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException("Not used in test")
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
