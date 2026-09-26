package com.naviify.app.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds signed URLs for binary endpoints. Coil (cover art) and the Media3
 * data source (audio streams) cannot go through Retrofit, so they construct a
 * URL with the same auth parameters the interceptor would add.
 */
@Singleton
class SubsonicUrlProvider @Inject constructor(
    private val sessionState: SessionStateHolder,
    private val router: ServerUrlRouter,
) {

    fun streamUrl(trackId: String, maxBitRate: Int? = null): HttpUrl? =
        buildUrl("stream.view") {
            addQueryParameter("id", trackId)
            maxBitRate?.let { addQueryParameter("maxBitRate", it.toString()) }
        }

    fun coverArtUrl(coverArtId: String, size: Int = 512): HttpUrl? =
        buildUrl("getCoverArt.view", staticSalt = true) {
            addQueryParameter("id", coverArtId)
            addQueryParameter("size", size.toString())
        }

    private fun buildUrl(endpoint: String, staticSalt: Boolean = false, extra: HttpUrl.Builder.() -> Unit): HttpUrl? {
        val config = sessionState.config.value ?: return null
        val normalized = normalizeServerUrl(router.effectiveSync().ifBlank { config.homeServerUrl.ifBlank { config.serverUrl } })
        if (normalized.isBlank()) return null
        val baseUrl = runCatching { normalized.toHttpUrl() }.getOrNull() ?: return null

        val salt = if (staticSalt) "naviify_cover" else SubsonicAuth.salt()
        val secret = config.token.ifBlank { config.password }
        val basePath = baseUrl.encodedPath.trimEnd('/')

        return baseUrl.newBuilder()
            .encodedPath("$basePath/rest/$endpoint")
            .addQueryParameter("u", config.username)
            .addQueryParameter("t", SubsonicAuth.token(secret, salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", SubsonicAuth.API_VERSION)
            .addQueryParameter("c", SubsonicAuth.CLIENT_NAME)
            .apply(extra)
            .build()
    }
}
