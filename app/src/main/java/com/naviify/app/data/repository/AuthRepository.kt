package com.naviify.app.data.repository

import com.naviify.app.core.network.ServerNotConfiguredException
import com.naviify.app.core.network.SessionStateHolder
import com.naviify.app.core.network.SubsonicService
import com.naviify.app.core.network.normalizeServerUrl
import com.naviify.app.core.storage.ServerConfig
import com.naviify.app.core.storage.ServerConfigStore
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class ServerInfo(
    val type: String?,
    val version: String?,
    val serverVersion: String?,
    val openSubsonic: Boolean,
)

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Checking : ConnectionState
    data class Connected(val config: ServerConfig, val serverInfo: ServerInfo) : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: SubsonicService,
    private val configStore: ServerConfigStore,
    private val sessionState: SessionStateHolder,
) {

    val config: Flow<ServerConfig> = configStore.config

    suspend fun persist(config: ServerConfig) {
        val normalized = config.copy(serverUrl = normalizeServerUrl(config.serverUrl))
        configStore.save(normalized)
        sessionState.publish(normalized)
    }

    /**
     * Publishes the config to the session holder synchronously (so interceptors
     * see it), then probes the server with [com.naviify.app.core.network.SubsonicService.ping].
     */
    suspend fun testConnection(config: ServerConfig): ConnectionState {
        val normalized = config.copy(serverUrl = normalizeServerUrl(config.serverUrl))
        sessionState.publish(normalized)
        return try {
            val envelope = api.ping()
            val response = envelope.response
            if (response.isOk) {
                ConnectionState.Connected(
                    config = normalized,
                    serverInfo = ServerInfo(
                        type = response.type,
                        version = response.version,
                        serverVersion = response.serverVersion,
                        openSubsonic = response.openSubsonic,
                    ),
                )
            } else {
                ConnectionState.Failed(
                    response.error?.message ?: "Server responded with status '${response.status}'",
                )
            }
        } catch (e: ServerNotConfiguredException) {
            ConnectionState.Failed(e.message ?: "Server not configured")
        } catch (e: IOException) {
            ConnectionState.Failed("Cannot reach server: ${e.message ?: "network error"}")
        } catch (e: SerializationException) {
            ConnectionState.Failed("Unexpected response format (is this a Subsonic server?)")
        } catch (e: Exception) {
            ConnectionState.Failed(e.message ?: "Unexpected error")
        }
    }

    suspend fun disconnect() {
        sessionState.publish(null)
        configStore.clear()
    }
}
