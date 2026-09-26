package com.naviify.app.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-bases requests onto the configured server. Retrofit is deliberately built
 * against a throwaway base URL; this interceptor rewrites host, scheme and path
 * prefix so the server URL can change at runtime without rebuilding the client.
 */
@Singleton
class DynamicServerInterceptor @Inject constructor(
    private val sessionState: SessionStateHolder,
    private val router: ServerUrlRouter,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request().url
        val host = original.host
        if (host.contains("lrclib.net")) {
            // External lyrics service - never rebase onto the Subsonic server.
            return chain.proceed(chain.request())
        }

        val config = sessionState.config.value ?: throw ServerNotConfiguredException()
        if (config.activeServerMode == com.naviify.app.core.storage.ServerMode.OFFLINE) {
            throw java.io.IOException("ASAYS is in Offline Mode")
        }
        val normalized = normalizeServerUrl(router.effectiveSync().ifBlank { config.serverUrl })
        if (normalized.isBlank()) throw ServerNotConfiguredException()
        val baseUrl = normalized.toHttpUrl()

        if (host != "localhost" && host != baseUrl.host && !host.startsWith("127.")) {
            return chain.proceed(chain.request())
        }
        if (original.host == baseUrl.host && original.port == baseUrl.port && original.scheme == baseUrl.scheme) {
            return chain.proceed(chain.request())
        }
        val newUrl = rebase(baseUrl, original)
        val request = chain.request().newBuilder().url(newUrl).build()

        return try {
            chain.proceed(request)
        } catch (e: java.io.IOException) {
            // In AUTO mode, if primary URL fails (LAN down / out of home), failover to alternate URL instantly
            if (config.activeServerMode == com.naviify.app.core.storage.ServerMode.AUTO) {
                val home = normalizeServerUrl(config.homeServerUrl.ifBlank { config.serverUrl })
                val remote = normalizeServerUrl(config.remoteServerUrl)
                val fallbackUrlStr = if (normalized == home && remote.isNotBlank()) {
                    remote
                } else if (normalized == remote && home.isNotBlank()) {
                    home
                } else {
                    null
                }

                if (fallbackUrlStr != null) {
                    val fallbackBaseUrl = fallbackUrlStr.toHttpUrl()
                    val fallbackUrl = rebase(fallbackBaseUrl, original)
                    val fallbackRequest = chain.request().newBuilder().url(fallbackUrl).build()
                    try {
                        val fallbackResponse = chain.proceed(fallbackRequest)
                        if (fallbackResponse.isSuccessful) {
                            router.reportWorkingUrl(fallbackUrlStr)
                        }
                        return fallbackResponse
                    } catch (_: java.io.IOException) {
                        // Both failed; rethrow original error
                    }
                }
            }
            throw e
        }
    }

    private fun rebase(base: HttpUrl, original: HttpUrl): HttpUrl {
        val basePath = base.encodedPath.trimEnd('/')
        return base.newBuilder()
            .encodedPath(basePath + original.encodedPath)
            .encodedQuery(original.encodedQuery)
            .build()
    }
}
