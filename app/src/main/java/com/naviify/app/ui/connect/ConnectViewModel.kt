package com.naviify.app.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.network.ServerUrlRouter
import com.naviify.app.core.storage.ServerConfig
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.core.storage.ServerMode
import com.naviify.app.data.repository.AuthRepository
import com.naviify.app.data.repository.ConnectionState
import com.naviify.app.data.repository.ServerInfo
import com.naviify.app.core.network.normalizeServerUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConnectUiState {
    data object Loading : ConnectUiState

    data class Form(
        val serverUrl: String = "",
        val remoteUrl: String = "",
        val username: String = "",
        val secret: String = "",
        val useToken: Boolean = false,
    ) : ConnectUiState

    data class Checking(val form: Form) : ConnectUiState

    data class Connected(
        val serverUrl: String,
        val serverInfo: ServerInfo?,
        val serverMode: ServerMode = ServerMode.AUTO,
        val effectiveUrl: String = "",
    ) : ConnectUiState

    data class Failed(val form: Form, val message: String) : ConnectUiState
}

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val configStore: ServerConfigStore,
    private val serverUrlRouter: ServerUrlRouter,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectUiState>(ConnectUiState.Loading)
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private var statusJob: Job? = null
    private var lastConfig: ServerConfig? = null

    init {
        viewModelScope.launch {
            configStore.config.collect { config ->
                if (_uiState.value is ConnectUiState.Checking) return@collect
                if (config.isComplete) {
                    _uiState.value = ConnectUiState.Connected(config.serverUrl, null)
                    refreshStatus(config)
                } else {
                    _uiState.value = ConnectUiState.Form(
                        serverUrl = config.homeServerUrl.ifBlank { config.serverUrl },
                        remoteUrl = config.remoteServerUrl,
                    )
                }
            }
        }
    }

    fun onServerUrlChange(value: String) = updateForm { copy(serverUrl = value) }
    fun onRemoteUrlChange(value: String) = updateForm { copy(remoteUrl = value) }
    fun onUsernameChange(value: String) = updateForm { copy(username = value) }
    fun onSecretChange(value: String) = updateForm { copy(secret = value) }
    fun onUseTokenChange(value: Boolean) = updateForm { copy(useToken = value) }

    fun connect() {
        val form = _uiState.value as? ConnectUiState.Form ?: return
        doConnect(form)
    }

    fun retry() {
        val form = (_uiState.value as? ConnectUiState.Failed)?.form ?: return
        doConnect(form)
    }

    fun switchServerMode() {
        viewModelScope.launch {
            val config = configStore.config.first()
            val next = if (config.activeServerMode == ServerMode.HOME) {
                ServerMode.REMOTE
            } else {
                ServerMode.HOME
            }
            configStore.save(config.copy(activeServerMode = next))
            serverUrlRouter.switchTo(next)
        }
    }

    fun editServer() {
        val current = _uiState.value
        val form = when (current) {
            is ConnectUiState.Connected -> lastConfig.toForm() ?: ConnectUiState.Form(serverUrl = current.serverUrl)
            is ConnectUiState.Failed -> current.form
            else -> return
        }
        _uiState.value = form
    }

    fun disconnect() {
        viewModelScope.launch {
            authRepository.disconnect()
            _uiState.value = ConnectUiState.Form()
        }
    }

    private fun doConnect(form: ConnectUiState.Form) {
        _uiState.value = ConnectUiState.Checking(form)
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            val home = normalizeServerUrl(form.serverUrl)
            val remote = normalizeServerUrl(form.remoteUrl)
            val config = ServerConfig(
                serverUrl = home,
                homeServerUrl = home,
                remoteServerUrl = remote,
                activeServerMode = ServerMode.AUTO,
                username = form.username,
                password = if (form.useToken) "" else form.secret,
                token = if (form.useToken) form.secret else "",
            )
            authRepository.persist(config)
            serverUrlRouter.probeHome(config)
            lastConfig = config
            when (val result = authRepository.testConnection(config)) {
                is ConnectionState.Connected -> {
                    _uiState.value = ConnectUiState.Connected(
                        serverUrl = result.config.serverUrl,
                        serverInfo = result.serverInfo,
                        serverMode = result.config.activeServerMode,
                        effectiveUrl = serverUrlRouter.effectiveSync(),
                    )
                }
                is ConnectionState.Failed -> {
                    _uiState.value = ConnectUiState.Failed(form, result.message)
                }
                else -> Unit
            }
        }
    }

    private fun refreshStatus(config: ServerConfig) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            lastConfig = config
            when (val result = authRepository.testConnection(config)) {
                is ConnectionState.Connected -> {
                    _uiState.value = ConnectUiState.Connected(
                        serverUrl = result.config.serverUrl,
                        serverInfo = result.serverInfo,
                        serverMode = result.config.activeServerMode,
                        effectiveUrl = serverUrlRouter.effectiveSync(),
                    )
                }
                is ConnectionState.Failed -> {
                    _uiState.value = ConnectUiState.Failed(
                        form = ConnectUiState.Form(serverUrl = config.serverUrl),
                        message = result.message,
                    )
                }
                else -> Unit
            }
        }
    }

    private fun updateForm(transform: ConnectUiState.Form.() -> ConnectUiState.Form) {
        val form = _uiState.value as? ConnectUiState.Form ?: return
        _uiState.value = form.transform()
    }

    private fun ServerConfig?.toForm(): ConnectUiState.Form? {
        val config = this ?: return null
        return ConnectUiState.Form(
            serverUrl = config.homeServerUrl.ifBlank { config.serverUrl },
            remoteUrl = config.remoteServerUrl,
            username = config.username,
            secret = config.token.ifBlank { config.password },
            useToken = config.token.isNotBlank(),
        )
    }
}
