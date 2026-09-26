package com.naviify.app.core.network

import com.naviify.app.core.storage.ServerConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches `u`, `t`, `s`, `v`, `c`, `f` query parameters to every Subsonic
 * request. Uses a fresh random salt per request; API token takes precedence
 * over password when both are stored.
 */
@Singleton
class SubsonicAuthInterceptor @Inject constructor(
    private val sessionState: SessionStateHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalUrl = chain.request().url
        // Stream URLs built by SubsonicUrlProvider are already fully signed.
        if (originalUrl.queryParameter("u") != null) {
            return chain.proceed(chain.request())
        }
        val config = sessionState.config.value ?: throw ServerNotConfiguredException()
        requireCredentials(config)
        // Fail closed: the token and salt are only ever attached to a host the
        // user configured. External services (LRCLIB and any future API) pass
        // through untouched instead of leaking credentials to a third party.
        val hosts = configuredHosts(config)
        if (hosts.isNotEmpty() && originalUrl.host !in hosts) {
            return chain.proceed(chain.request())
        }

        val salt = SubsonicAuth.salt()
        val secret = config.token.ifBlank { config.password }

        val url = chain.request().url.newBuilder()
            .addQueryParameter("u", config.username)
            .addQueryParameter("t", SubsonicAuth.token(secret, salt))
            .addQueryParameter("s", salt)
            .addQueryParameter("v", SubsonicAuth.API_VERSION)
            .addQueryParameter("c", SubsonicAuth.CLIENT_NAME)
            .addQueryParameter("f", "json")
            .build()

        return chain.proceed(chain.request().newBuilder().url(url).build())
    }

    /** Hosts allowed to receive Subsonic credentials. */
    private fun configuredHosts(config: ServerConfig): Set<String> = buildSet {
        listOf(config.serverUrl, config.homeServerUrl, config.remoteServerUrl).forEach { raw ->
            runCatching { normalizeServerUrl(raw).toHttpUrl().host }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)
        }
    }

    private fun requireCredentials(config: ServerConfig) {
        if (config.username.isBlank() || (config.password.isBlank() && config.token.isBlank())) {
            throw ServerNotConfiguredException("Navidrome credentials are incomplete")
        }
    }
}
