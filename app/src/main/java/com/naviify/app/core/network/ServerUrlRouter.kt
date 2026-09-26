package com.naviify.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.naviify.app.core.storage.ServerConfig
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.core.storage.ServerMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes API/stream requests to the Home LAN or Tailscale server. In AUTO mode
 * it probes the home URL (short timeout) and falls back to the remote URL.
 * Automatically reacts to network transitions (WiFi/Cellular/Tailscale) with debounced probes.
 */
@Singleton
class ServerUrlRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: ServerConfigStore,
    private val sessionState: SessionStateHolder,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _effectiveUrl = MutableStateFlow("")
    val effectiveUrl: StateFlow<String> = _effectiveUrl.asStateFlow()

    private val _isHomeReachable = MutableStateFlow(true)
    val isHomeReachable: StateFlow<Boolean> = _isHomeReachable.asStateFlow()

    @Volatile
    private var lastConfig: ServerConfig = ServerConfig()

    private val probeMutex = Mutex()
    private var lastProbeTimestamp: Long = 0L
    private var networkDebounceJob: Job? = null

    private val probeClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(1_200, TimeUnit.MILLISECONDS)
        .readTimeout(1_200, TimeUnit.MILLISECONDS)
        .build()

    init {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = onNetworkAvailable()
                    override fun onLost(network: Network) = onNetworkLost()
                    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = onNetworkChanged()
                }
            )
        }

        scope.launch {
            configStore.config.collect { config ->
                lastConfig = config
                if (config.activeServerMode == ServerMode.AUTO) {
                    probeHome(config, force = true)
                } else {
                    resolve(config)
                }
            }
        }
    }

    private fun onNetworkAvailable() {
        if (lastConfig.activeServerMode != ServerMode.AUTO) return
        networkDebounceJob?.cancel()
        networkDebounceJob = scope.launch {
            // Immediate probe on fresh network connection (no delay)
            probeHome(lastConfig, force = true)
        }
    }

    private fun onNetworkChanged() {
        if (lastConfig.activeServerMode != ServerMode.AUTO) return
        networkDebounceJob?.cancel()
        networkDebounceJob = scope.launch {
            delay(300L) // Debounce rapid network changes (e.g. WiFi associating)
            probeHome(lastConfig, force = true)
        }
    }

    private fun onNetworkLost() {
        networkDebounceJob?.cancel()
    }

    /** Synchronous snapshot for interceptors/URL builders. */
    fun effectiveSync(): String {
        val cached = _effectiveUrl.value
        if (cached.isNotBlank()) return cached
        return resolveSync(lastConfig)
    }

    suspend fun probeHome(config: ServerConfig = lastConfig, force: Boolean = false) {
        if (config.activeServerMode == ServerMode.OFFLINE) {
            _effectiveUrl.value = ""
            return
        }
        val now = System.currentTimeMillis()
        if (!force && (now - lastProbeTimestamp) < 2_000L) {
            return
        }

        probeMutex.withLock {
            if (lastConfig.activeServerMode != ServerMode.AUTO) return@withLock
            if (!force && (System.currentTimeMillis() - lastProbeTimestamp) < 2_000L) {
                return
            }
            lastProbeTimestamp = System.currentTimeMillis()

            val home = config.homeServerUrl.ifBlank { config.serverUrl.ifBlank { ServerConfig.DEFAULT_HOME_URL } }
            val remote = config.remoteServerUrl.ifBlank { ServerConfig.DEFAULT_REMOTE_URL }

            if (home.isNotBlank() && remote.isNotBlank() && home != remote) {
                // Probe both concurrently for maximum speed
                coroutineScope {
                    val homeDeferred = async(Dispatchers.IO) { pingServer(config, home) }
                    val remoteDeferred = async(Dispatchers.IO) { pingServer(config, remote) }

                    // Local LAN ping is ultra-fast (<30ms). If home responds within 350ms, select home immediately!
                    val homeQuick = withTimeoutOrNull(350L) { homeDeferred.await() }
                    if (homeQuick == true) {
                        remoteDeferred.cancel()
                        if (lastConfig.activeServerMode == ServerMode.AUTO) {
                            _isHomeReachable.value = true
                            _effectiveUrl.value = normalizeServerUrl(home)
                        }
                        return@coroutineScope
                    }

                    // If home didn't finish within 350ms or failed: check remote
                    val remoteOk = remoteDeferred.await()
                    if (remoteOk) {
                        if (lastConfig.activeServerMode == ServerMode.AUTO) {
                            _isHomeReachable.value = false
                            _effectiveUrl.value = normalizeServerUrl(remote)
                        }
                        return@coroutineScope
                    }

                    // If remote failed, check if home eventually finished with success:
                    val homeOk = homeDeferred.await()
                    if (lastConfig.activeServerMode == ServerMode.AUTO) {
                        if (homeOk) {
                            _isHomeReachable.value = true
                            _effectiveUrl.value = normalizeServerUrl(home)
                        } else {
                            // Neither reachable - default to remote as best effort
                            _isHomeReachable.value = false
                            _effectiveUrl.value = normalizeServerUrl(remote)
                        }
                    }
                }
            } else if (home.isNotBlank()) {
                val homeOk = pingServer(config, home)
                if (lastConfig.activeServerMode == ServerMode.AUTO) {
                    _isHomeReachable.value = homeOk
                    _effectiveUrl.value = normalizeServerUrl(home)
                }
            } else if (remote.isNotBlank()) {
                val remoteOk = pingServer(config, remote)
                if (lastConfig.activeServerMode == ServerMode.AUTO) {
                    _isHomeReachable.value = false
                    _effectiveUrl.value = normalizeServerUrl(remote)
                }
            }
        }
    }

    suspend fun refreshFor(config: ServerConfig) {
        lastConfig = config
        sessionState.publish(config)
        _effectiveUrl.value = resolveSync(config)
        if (config.activeServerMode == ServerMode.AUTO) probeHome(config, force = true)
    }

    fun switchTo(mode: ServerMode) {
        val updated = lastConfig.copy(activeServerMode = mode)
        lastConfig = updated
        sessionState.publish(updated)
        // Apply synchronously so the very next network call uses the new route.
        _effectiveUrl.value = resolveSync(updated)
        scope.launch {
            configStore.save(updated)
            if (mode == ServerMode.AUTO) probeHome(updated, force = true)
        }
    }

    suspend fun switchToSuspend(mode: ServerMode): String {
        val updated = lastConfig.copy(activeServerMode = mode)
        lastConfig = updated
        sessionState.publish(updated)
        configStore.save(updated)
        if (mode == ServerMode.AUTO) {
            probeHome(updated, force = true)
        } else {
            _effectiveUrl.value = resolveSync(updated)
        }
        return _effectiveUrl.value
    }

    fun reportWorkingUrl(url: String) {
        val normalized = normalizeServerUrl(url)
        if (normalized.isBlank()) return
        val home = normalizeServerUrl(lastConfig.homeServerUrl.ifBlank { lastConfig.serverUrl })
        val isHome = normalized == home
        _isHomeReachable.value = isHome
        _effectiveUrl.value = normalized
    }

    private suspend fun resolve(config: ServerConfig) {
        _effectiveUrl.value = resolveSync(config)
    }

    private fun resolveSync(config: ServerConfig): String = when (config.activeServerMode) {
        ServerMode.HOME -> normalizeServerUrl(config.homeServerUrl.ifBlank { ServerConfig.DEFAULT_HOME_URL })
        ServerMode.REMOTE -> normalizeServerUrl(config.remoteServerUrl.ifBlank { ServerConfig.DEFAULT_REMOTE_URL })
        ServerMode.AUTO -> if (_isHomeReachable.value) {
            normalizeServerUrl(config.homeServerUrl.ifBlank { ServerConfig.DEFAULT_HOME_URL })
        } else {
            normalizeServerUrl(config.remoteServerUrl.ifBlank { ServerConfig.DEFAULT_REMOTE_URL })
        }
        ServerMode.OFFLINE -> ""
    }

    private suspend fun pingServer(config: ServerConfig, url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = normalizeServerUrl(url)
            if (normalized.isBlank()) return@withContext false
            val salt = SubsonicAuth.salt()
            val secret = config.token.ifBlank { config.password }
            val base = normalized.toHttpUrl()
            val pingUrl = base.newBuilder()
                .encodedPath(base.encodedPath.trimEnd('/') + "/rest/ping.view")
                .addQueryParameter("u", config.username)
                .addQueryParameter("t", SubsonicAuth.token(secret, salt))
                .addQueryParameter("s", salt)
                .addQueryParameter("v", SubsonicAuth.API_VERSION)
                .addQueryParameter("c", SubsonicAuth.CLIENT_NAME)
                .addQueryParameter("f", "json")
                .build()
            probeClient.newCall(Request.Builder().url(pingUrl).get().build())
                .execute()
                .use { response ->
                    response.isSuccessful &&
                        (response.body?.string().orEmpty().contains("\"status\":\"ok\""))
                }
        }.getOrDefault(false)
    }
}
