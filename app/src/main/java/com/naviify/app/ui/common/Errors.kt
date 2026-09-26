package com.naviify.app.ui.common

import com.naviify.app.core.network.SubsonicApiException
import java.io.IOException

fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "Can't reach your server. Check the connection."
    is SubsonicApiException -> message ?: "Server error"
    else -> message ?: "Something went wrong"
}
