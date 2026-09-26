package com.naviify.app.core.network

import com.naviify.app.core.storage.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory mirror of the persisted server config. OkHttp interceptors and the
 * URL provider read it synchronously, so requests never block on DataStore IO.
 */
@Singleton
class SessionStateHolder @Inject constructor() {

    private val _config = MutableStateFlow<ServerConfig?>(null)

    val config: StateFlow<ServerConfig?> = _config.asStateFlow()

    fun publish(config: ServerConfig?) {
        _config.value = config
    }
}
