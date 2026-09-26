package com.naviify.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naviify.app.core.storage.ServerConfigStore
import com.naviify.app.core.theme.AppFont
import com.naviify.app.core.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ThemeState(
    val theme: AppTheme = AppTheme.SPOTIFY,
    val customAccentHex: String = "",
    val font: AppFont = AppFont.SYSTEM,
    val highPerformanceMode: Boolean = true,
    val performanceRefreshRate: Int = 120,
    val iosOverscrollEnabled: Boolean = true,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    serverConfigStore: ServerConfigStore,
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = serverConfigStore.config
        .map {
            ThemeState(
                theme = it.theme,
                customAccentHex = it.customAccentHex,
                font = it.font,
                highPerformanceMode = it.highPerformanceMode,
                performanceRefreshRate = it.performanceRefreshRate,
                iosOverscrollEnabled = it.iosOverscrollEnabled,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())

    val theme: StateFlow<AppTheme> = serverConfigStore.config
        .map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SPOTIFY)
}
