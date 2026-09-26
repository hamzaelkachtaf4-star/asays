package com.naviify.app.core.storage

import com.naviify.app.core.theme.AppFont
import com.naviify.app.core.theme.AppTheme
import com.naviify.app.domain.playback.StreamQuality

enum class ServerMode {
    HOME,
    REMOTE,
    AUTO,
    OFFLINE,
    ;

    companion object {
        fun fromStorage(value: String?): ServerMode =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

data class ServerConfig(
    val serverUrl: String = "",
    val homeServerUrl: String = "",
    val remoteServerUrl: String = "",
    val activeServerMode: ServerMode = ServerMode.AUTO,
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val wifiQuality: StreamQuality = StreamQuality.LOSSLESS,
    val mobileQuality: StreamQuality = StreamQuality.LOSSLESS,
    val theme: AppTheme = AppTheme.SPOTIFY,
    val customAccentHex: String = "",
    val font: AppFont = AppFont.SYSTEM,
    val highPerformanceMode: Boolean = true,
    val performanceRefreshRate: Int = 120,
    val iosOverscrollEnabled: Boolean = true,
) {
    val isComplete: Boolean
        get() = (homeServerUrl.isNotBlank() || serverUrl.isNotBlank()) &&
            username.isNotBlank() &&
            (password.isNotBlank() || token.isNotBlank())

    companion object {
        const val DEFAULT_HOME_URL = "http://192.168.11.243:4533"
        const val DEFAULT_REMOTE_URL = "http://100.109.58.54:4533"
    }
}
