package com.naviify.app.core.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.domain.playback.StreamQuality
import com.naviify.app.domain.playback.resolveQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the active stream quality from the Wi-Fi / mobile preference pair
 * and the current network type. Exposed both as a [StateFlow] (settings UI +
 * re-apply on network change) and a synchronous snapshot for URL building.
 */
@Singleton
class StreamQualityProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configStore: ServerConfigStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isWifi = MutableStateFlow(currentNetworkIsWifi())
    val isWifi: StateFlow<Boolean> = _isWifi.asStateFlow()

    @Volatile
    private var wifiQuality: StreamQuality = StreamQuality.LOSSLESS

    @Volatile
    private var mobileQuality: StreamQuality = StreamQuality.LOSSLESS

    val effectiveQuality: StateFlow<StreamQuality> =
        combine(configStore.config, isWifi) { config, wifi ->
            wifiQuality = config.wifiQuality
            mobileQuality = config.mobileQuality
            resolveQuality(config.wifiQuality, config.mobileQuality, wifi)
        }.stateIn(scope, SharingStarted.Eagerly, StreamQuality.LOSSLESS)

    init {
        connectivityManager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = refresh()
                override fun onLost(network: Network) = refresh()
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = refresh()
            },
            Handler(Looper.getMainLooper())
        )
    }

    /** Synchronous snapshot used while building MediaItems. */
    fun current(): StreamQuality =
        resolveQuality(wifiQuality, mobileQuality, _isWifi.value)

    private fun refresh() {
        _isWifi.value = currentNetworkIsWifi()
    }

    private fun currentNetworkIsWifi(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return true
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
