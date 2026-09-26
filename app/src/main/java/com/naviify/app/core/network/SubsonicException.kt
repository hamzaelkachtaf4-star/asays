package com.naviify.app.core.network

import java.io.IOException

/**
 * Raised by the OkHttp interceptors when no server is configured. It extends
 * [IOException] on purpose: Retrofit only converts IO failures into an ordinary
 * call-site error, so a plain [Exception] would escape the coroutine and crash
 * instead of surfacing as a retryable UI error.
 */
class ServerNotConfiguredException(
    message: String = "Navidrome server is not configured",
) : IOException(message)

class SubsonicApiException(
    val code: Int?,
    message: String,
) : Exception(message)
