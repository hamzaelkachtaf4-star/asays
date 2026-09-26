package com.naviify.app.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.naviify.app.core.theme.AppFont
import com.naviify.app.core.theme.AppTheme
import com.naviify.app.domain.playback.StreamQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "naviify_server")

/**
 * Persists the Navidrome connection details. The password or Subsonic API
 * token is stored under app-private storage only.
 */
@Singleton
class ServerConfigStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val config: Flow<ServerConfig> = context.serverConfigDataStore.data.map { prefs ->
        val legacyUrl = prefs[Keys.SERVER_URL].orEmpty()
        val homeUrl = (prefs[Keys.HOME_SERVER_URL] ?: "").ifEmpty { legacyUrl }
            .ifBlank { ServerConfig.DEFAULT_HOME_URL }
        ServerConfig(
            serverUrl = legacyUrl,
            homeServerUrl = homeUrl,
            remoteServerUrl = prefs[Keys.REMOTE_SERVER_URL].orEmpty().ifBlank { ServerConfig.DEFAULT_REMOTE_URL },
            activeServerMode = ServerMode.fromStorage(prefs[Keys.SERVER_MODE]),
            username = prefs[Keys.USERNAME].orEmpty(),
            password = KeystoreEncryptor.decrypt(prefs[Keys.PASSWORD].orEmpty()),
            token = KeystoreEncryptor.decrypt(prefs[Keys.TOKEN].orEmpty()),
            wifiQuality = StreamQuality.fromStorage(prefs[Keys.WIFI_QUALITY]),
            mobileQuality = StreamQuality.fromStorage(prefs[Keys.MOBILE_QUALITY]),
            theme = AppTheme.fromStorage(prefs[Keys.THEME]),
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX].orEmpty(),
            font = AppFont.fromStorage(prefs[Keys.FONT]),
            highPerformanceMode = prefs[Keys.HIGH_PERFORMANCE_MODE] ?: true,
            performanceRefreshRate = prefs[Keys.PERFORMANCE_REFRESH_RATE] ?: 120,
            iosOverscrollEnabled = prefs[Keys.IOS_OVERSCROLL_ENABLED] ?: true,
        )
    }

    suspend fun save(config: ServerConfig) {
        context.serverConfigDataStore.edit { prefs ->
            val home = config.homeServerUrl.ifBlank { config.serverUrl }
            prefs[Keys.SERVER_URL] = home.trim()
            prefs[Keys.HOME_SERVER_URL] = home.trim()
            prefs[Keys.REMOTE_SERVER_URL] = config.remoteServerUrl.trim()
            prefs[Keys.SERVER_MODE] = config.activeServerMode.name
            prefs[Keys.USERNAME] = config.username.trim()
            prefs[Keys.PASSWORD] = KeystoreEncryptor.encrypt(config.password.trim())
            prefs[Keys.TOKEN] = KeystoreEncryptor.encrypt(config.token.trim())
            prefs[Keys.WIFI_QUALITY] = config.wifiQuality.name
            prefs[Keys.MOBILE_QUALITY] = config.mobileQuality.name
            prefs[Keys.THEME] = config.theme.name
            prefs[Keys.CUSTOM_ACCENT_HEX] = config.customAccentHex.trim()
            prefs[Keys.FONT] = config.font.name
            prefs[Keys.HIGH_PERFORMANCE_MODE] = config.highPerformanceMode
            prefs[Keys.PERFORMANCE_REFRESH_RATE] = config.performanceRefreshRate
            prefs[Keys.IOS_OVERSCROLL_ENABLED] = config.iosOverscrollEnabled
        }
    }

    suspend fun setHighPerformanceMode(enabled: Boolean) {
        context.serverConfigDataStore.edit { prefs ->
            prefs[Keys.HIGH_PERFORMANCE_MODE] = enabled
        }
    }

    suspend fun setPerformanceRefreshRate(rate: Int) {
        context.serverConfigDataStore.edit { prefs ->
            prefs[Keys.PERFORMANCE_REFRESH_RATE] = rate
        }
    }

    suspend fun setIosOverscrollEnabled(enabled: Boolean) {
        context.serverConfigDataStore.edit { prefs ->
            prefs[Keys.IOS_OVERSCROLL_ENABLED] = enabled
        }
    }

    suspend fun clear() {
        context.serverConfigDataStore.edit { it.clear() }
    }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val TOKEN = stringPreferencesKey("token")
        val WIFI_QUALITY = stringPreferencesKey("wifi_quality")
        val MOBILE_QUALITY = stringPreferencesKey("mobile_quality")
        val HOME_SERVER_URL = stringPreferencesKey("home_server_url")
        val REMOTE_SERVER_URL = stringPreferencesKey("remote_server_url")
        val SERVER_MODE = stringPreferencesKey("server_mode")
        val THEME = stringPreferencesKey("theme")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val FONT = stringPreferencesKey("font")
        val HIGH_PERFORMANCE_MODE = booleanPreferencesKey("high_performance_mode")
        val PERFORMANCE_REFRESH_RATE = intPreferencesKey("performance_refresh_rate")
        val IOS_OVERSCROLL_ENABLED = booleanPreferencesKey("ios_overscroll_enabled")
    }
}
