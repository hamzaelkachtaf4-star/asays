package com.naviify.app.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.naviify.app.core.storage.KeystoreEncryptor
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverCookiesDataStore: DataStore<Preferences> by preferencesDataStore(name = "naviify_cookies")

/**
 * CookieJar backed by Preferences DataStore. Navidrome relies on session
 * cookies for some flows (e.g. playlists), so cookies survive process restarts.
 */
@Singleton
class DataStoreCookieJar @Inject constructor(
    @ApplicationContext private val context: Context,
) : CookieJar {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** host -> "name=value; name2=value2" (attributes intentionally dropped). */
    @Volatile
    private var cookiesByHost: Map<String, String> = emptyMap()

    /** Called once from [com.naviify.app.NaviifyApplication] before any request. */
    suspend fun initialize() {
        // Cookies are session credentials, so they are encrypted at rest like
        // the password and API token. Values written by earlier builds were
        // plain JSON; KeystoreEncryptor.decrypt passes those through unchanged.
        val raw = context.serverCookiesDataStore.data.first()[Keys.COOKIES]
        if (raw != null) {
            val json = KeystoreEncryptor.decrypt(raw)
            runCatching { Json.decodeFromString<Map<String, String>>(json) }
                .onSuccess { cookiesByHost = it }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        val alive = cookies
            .filter { it.expiresAt > now }
            .map { "${it.name}=${it.value}" }
        val snapshot = alive
        scope.launch {
            mutex.withLock {
                val updated = cookiesByHost.toMutableMap()
                if (snapshot.isEmpty()) {
                    updated.remove(url.host)
                } else {
                    updated[url.host] = snapshot.joinToString("; ")
                }
                cookiesByHost = updated
                persist(updated)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = cookiesByHost[url.host].orEmpty()
        return raw.split("; ").mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val name = part.substring(0, separator)
            val value = part.substring(separator + 1)
            if (name.isBlank() || value.isBlank()) return@mapNotNull null
            Cookie.Builder()
                .name(name)
                .value(value)
                .domain(url.host)
                .path("/")
                .build()
        }
    }

    suspend fun clear() {
        mutex.withLock {
            cookiesByHost = emptyMap()
            persist(emptyMap())
        }
    }

    private suspend fun persist(cookies: Map<String, String>) {
        val payload = KeystoreEncryptor.encrypt(Json.encodeToString(cookies))
        if (payload.isBlank()) return
        context.serverCookiesDataStore.edit {
            it[Keys.COOKIES] = payload
        }
    }

    private object Keys {
        val COOKIES = stringPreferencesKey("cookies")
    }
}
