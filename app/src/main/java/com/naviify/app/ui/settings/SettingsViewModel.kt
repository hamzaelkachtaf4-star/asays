package com.naviify.app.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import androidx.media3.common.util.UnstableApi
import coil.annotation.ExperimentalCoilApi
import androidx.media3.datasource.cache.SimpleCache
import com.naviify.app.core.playback.StreamQualityProvider
import com.naviify.app.core.network.ServerUrlRouter
import com.naviify.app.core.storage.ServerConfig
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.core.storage.ServerMode
import com.naviify.app.core.theme.AppFont
import com.naviify.app.core.theme.AppTheme
import com.naviify.app.data.repository.AuthRepository
import com.naviify.app.data.repository.ConnectionState
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.domain.playback.StreamQuality
import com.naviify.app.data.download.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.naviify.app.data.lyrics.LyricsPreferencesStore
import com.naviify.app.data.lyrics.ReportedTrack
import javax.inject.Inject

@Immutable
data class SettingsUiState(
    val serverUrl: String = "",
    val homeServerUrl: String = "",
    val remoteServerUrl: String = "",
    val activeServerMode: ServerMode = ServerMode.AUTO,
    val effectiveUrl: String = "",
    val homeReachable: Boolean = true,
    val editHomeUrl: String = "",
    val editRemoteUrl: String = "",
    val theme: AppTheme = AppTheme.SPOTIFY,
    val customAccentHex: String = "",
    val font: AppFont = AppFont.SYSTEM,
    val username: String = "",
    val usesToken: Boolean = false,
    val serverType: String? = null,
    val serverVersion: String? = null,
    val isConnected: Boolean? = null,
    val statusMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isScanning: Boolean = false,
    val scanCount: Long? = null,
    val scanFinished: Boolean = false,
    val scanMessage: String? = null,
    val diskCacheBytes: Long = 0,
    val memoryCacheBytes: Long = 0,
    val audioCacheBytes: Long = 0,
    val downloadsBytes: Long = 0,
    val wifiQuality: StreamQuality = StreamQuality.LOSSLESS,
    val mobileQuality: StreamQuality = StreamQuality.LOSSLESS,
    val effectiveQuality: StreamQuality = StreamQuality.LOSSLESS,
    val isWifi: Boolean = true,
    val reportedTracks: List<ReportedTrack> = emptyList(),
    val blockedLyricsCount: Int = 0,
    val isHardwareKeystore: Boolean = true,
    val isKeystoreHealthy: Boolean = true,
    val isIncognitoMode: Boolean = false,
    val highPerformanceMode: Boolean = true,
    val performanceRefreshRate: Int = 120,
    val iosOverscrollEnabled: Boolean = true,
)

@HiltViewModel
@OptIn(ExperimentalCoilApi::class)
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    private val configStore: ServerConfigStore,
    private val imageLoader: ImageLoader,
    private val simpleCache: SimpleCache,
    private val streamQualityProvider: StreamQualityProvider,
    private val downloadRepository: DownloadRepository,
    private val serverUrlRouter: ServerUrlRouter,
    private val lyricsPreferencesStore: LyricsPreferencesStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            configStore.config.collect { config ->
                _uiState.value = _uiState.value.copy(
                    serverUrl = config.serverUrl,
                    homeServerUrl = config.homeServerUrl,
                    remoteServerUrl = config.remoteServerUrl,
                    activeServerMode = config.activeServerMode,
                    effectiveUrl = serverUrlRouter.effectiveSync(),
                    editHomeUrl = if (_uiState.value.editHomeUrl.isBlank()) config.homeServerUrl else _uiState.value.editHomeUrl,
                    editRemoteUrl = if (_uiState.value.editRemoteUrl.isBlank()) config.remoteServerUrl else _uiState.value.editRemoteUrl,
                    theme = config.theme,
                    customAccentHex = config.customAccentHex,
                    font = config.font,
                    username = config.username,
                    usesToken = config.token.isNotBlank(),
                    wifiQuality = config.wifiQuality,
                    mobileQuality = config.mobileQuality,
                    highPerformanceMode = config.highPerformanceMode,
                    performanceRefreshRate = config.performanceRefreshRate,
                    iosOverscrollEnabled = config.iosOverscrollEnabled,
                )
            }
        }
        viewModelScope.launch {
            streamQualityProvider.effectiveQuality.collect { quality ->
                _uiState.value = _uiState.value.copy(effectiveQuality = quality)
            }
        }
        viewModelScope.launch {
            streamQualityProvider.isWifi.collect { wifi ->
                _uiState.value = _uiState.value.copy(isWifi = wifi)
            }
        }
        viewModelScope.launch {
            serverUrlRouter.isHomeReachable.collect { reachable ->
                _uiState.value = _uiState.value.copy(homeReachable = reachable)
            }
        }
        lyricsPreferencesStore?.let { store ->
            viewModelScope.launch {
                store.reportedTracks.collect { reports ->
                    _uiState.value = _uiState.value.copy(reportedTracks = reports)
                }
            }
            viewModelScope.launch {
                store.blockedTrackIds.collect { ids ->
                    _uiState.value = _uiState.value.copy(blockedLyricsCount = ids.size)
                }
            }
        }
        val hardware = com.naviify.app.core.storage.KeystoreEncryptor.isHardwareBacked()
        val healthy = com.naviify.app.core.storage.KeystoreEncryptor.isHealthy()
        _uiState.value = _uiState.value.copy(
            isHardwareKeystore = hardware,
            isKeystoreHealthy = healthy,
        )
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            runCatching {
                val config: ServerConfig = configStore.config.first()
                val connection = authRepository.testConnection(config)
                val disk = imageLoader.diskCache?.size?.toLong() ?: 0L
                val memory = imageLoader.memoryCache?.size?.toLong() ?: 0L
                val audio = simpleCache.cacheSpace
                val downloadsSize = downloadRepository.totalSizeBytes()
                when (connection) {
                    is ConnectionState.Connected -> _uiState.value.copy(
                        isConnected = true,
                        serverType = connection.serverInfo.type,
                        serverVersion = connection.serverInfo.serverVersion ?: connection.serverInfo.version,
                        statusMessage = null,
                        diskCacheBytes = disk,
                        memoryCacheBytes = memory,
                        audioCacheBytes = audio,
                        downloadsBytes = downloadsSize,
                    )
                    is ConnectionState.Failed -> _uiState.value.copy(
                        isConnected = false,
                        statusMessage = connection.message,
                        diskCacheBytes = disk,
                        memoryCacheBytes = memory,
                        audioCacheBytes = audio,
                        downloadsBytes = downloadsSize,
                    )
                    else -> _uiState.value
                }
            }.onSuccess { updated ->
                _uiState.value = updated.copy(isRefreshing = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    isConnected = false,
                    statusMessage = error.message ?: "Refresh failed",
                )
            }
        }
    }

    fun onEditHomeUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(editHomeUrl = value)
    }

    fun onEditRemoteUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(editRemoteUrl = value)
    }

    fun saveServerUrls() {
        viewModelScope.launch {
            val current = configStore.config.first()
            val updated = current.copy(
                homeServerUrl = _uiState.value.editHomeUrl.trim().ifBlank { current.homeServerUrl },
                remoteServerUrl = _uiState.value.editRemoteUrl.trim().ifBlank { current.remoteServerUrl },
            )
            configStore.save(updated)
            serverUrlRouter.refreshFor(updated)
        }
    }

    fun setServerMode(mode: ServerMode) {
        viewModelScope.launch {
            serverUrlRouter.switchTo(mode)
        }
    }

    fun cycleServerMode() {
        val next = if (_uiState.value.activeServerMode == ServerMode.HOME) {
            ServerMode.REMOTE
        } else {
            ServerMode.HOME
        }
        setServerMode(next)
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            val current = configStore.config.first()
            configStore.save(current.copy(theme = theme))
        }
    }

    fun setCustomAccentHex(hex: String) {
        viewModelScope.launch {
            val current = configStore.config.first()
            configStore.save(current.copy(customAccentHex = hex.trim()))
        }
    }

    fun setFont(font: AppFont) {
        viewModelScope.launch {
            val current = configStore.config.first()
            configStore.save(current.copy(font = font))
        }
    }

    fun setHighPerformanceMode(enabled: Boolean) {
        viewModelScope.launch {
            configStore.setHighPerformanceMode(enabled)
        }
    }

    fun setPerformanceRefreshRate(rate: Int) {
        viewModelScope.launch {
            configStore.setPerformanceRefreshRate(rate)
        }
    }

    fun setIosOverscrollEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configStore.setIosOverscrollEnabled(enabled)
        }
    }

    fun setWifiQuality(quality: StreamQuality) = setQuality { copy(wifiQuality = quality) }

    fun setMobileQuality(quality: StreamQuality) = setQuality { copy(mobileQuality = quality) }

    private fun setQuality(transform: ServerConfig.() -> ServerConfig) {
        viewModelScope.launch {
            val current = configStore.config.first()
            configStore.save(current.transform())
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            downloadRepository.deleteAllDownloads()
            refresh()
        }
    }

    fun clearCaches() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
                runCatching { simpleCache.keys.forEach { simpleCache.removeResource(it) } }
                downloadRepository.clearLyricsCache()
            }
            refresh()
        }
    }

    fun clearImageCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            }
            refresh()
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { simpleCache.keys.forEach { simpleCache.removeResource(it) } }
            }
            refresh()
        }
    }

    fun toggleIncognito() {
        _uiState.value = _uiState.value.copy(isIncognitoMode = !_uiState.value.isIncognitoMode)
    }

    fun disconnect() {
        viewModelScope.launch {
            authRepository.disconnect()
        }
    }

    private var scanJob: Job? = null

    fun scanLibrary(fullScan: Boolean = true) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanFinished = false,
                scanCount = null,
                scanMessage = "Starting library scan on server...",
            )
            val result = runCatching { mediaRepository.startScan(fullScan) }
            result.onSuccess { initialStatus ->
                var currentStatus = initialStatus
                var count = currentStatus?.count
                val isStillScanning = currentStatus?.scanning == true
                _uiState.value = _uiState.value.copy(
                    isScanning = isStillScanning,
                    scanFinished = !isStillScanning,
                    scanCount = count,
                    scanMessage = if (isStillScanning) {
                        if (count != null && count > 0) "Scanning server library ($count files indexed)..."
                        else "Scanning server library..."
                    } else {
                        "Scan complete! ${count?.let { "($it files indexed)" } ?: ""}"
                    },
                )

                while (currentStatus?.scanning == true && isActive) {
                    delay(1200)
                    val status = runCatching { mediaRepository.getScanStatus() }.getOrNull()
                    if (status != null) {
                        currentStatus = status
                        count = status.count
                        val stillScanning = status.scanning == true
                        _uiState.value = _uiState.value.copy(
                            isScanning = stillScanning,
                            scanFinished = !stillScanning,
                            scanCount = count,
                            scanMessage = if (stillScanning) {
                                if (count != null && count > 0) "Scanning server library ($count files indexed)..."
                                else "Scanning server library..."
                            } else {
                                "Scan complete! ${count?.let { "($it files indexed)" } ?: ""}"
                            },
                        )
                    } else {
                        break
                    }
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanFinished = false,
                    scanMessage = "Scan failed: ${error.message ?: "Server error"}",
                )
            }
        }
    }

    fun removeReportedTrack(trackId: String) {
        lyricsPreferencesStore?.removeReport(trackId)
    }

    fun clearAllReportedTracks() {
        lyricsPreferencesStore?.clearReports()
    }

    fun unblockAllLyrics() {
        lyricsPreferencesStore?.clearBlocked()
    }
}
