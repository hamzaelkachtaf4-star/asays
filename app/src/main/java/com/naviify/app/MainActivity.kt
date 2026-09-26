package com.naviify.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.core.performance.resolvePerformanceRefreshRate
import com.naviify.app.ui.components.rememberIosOverscrollFactory
import com.naviify.app.ui.root.RootScreen
import com.naviify.app.ui.theme.NaviifyTheme
import com.naviify.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val notificationPermission =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeState by themeViewModel.themeState.collectAsStateWithLifecycle()
            val composeView = LocalView.current

            LaunchedEffect(themeState.highPerformanceMode, themeState.performanceRefreshRate) {
                applyPerformanceMode(
                    enabled = themeState.highPerformanceMode,
                    refreshRate = themeState.performanceRefreshRate,
                    composeView = composeView,
                )
            }

            val iosOverscrollFactory = rememberIosOverscrollFactory()

            NaviifyTheme(
                theme = themeState.theme,
                customAccentHex = themeState.customAccentHex,
                font = themeState.font,
            ) {
                if (themeState.iosOverscrollEnabled) {
                    CompositionLocalProvider(LocalOverscrollFactory provides iosOverscrollFactory) {
                        RootScreen()
                    }
                } else {
                    RootScreen()
                }
            }
        }
    }

    /**
     * Unlocks high refresh rate (90Hz / 120Hz ProMotion / 144Hz) rendering on supported displays.
     * Ported and optimized from BitChord performance architecture.
     */
    private fun applyPerformanceMode(enabled: Boolean, refreshRate: Int, composeView: View) {
        val supportedRefreshRate = composeView.display.resolvePerformanceRefreshRate(refreshRate)
        window.attributes = window.attributes.apply {
            preferredRefreshRate = if (enabled) supportedRefreshRate.toFloat() else 0f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setFrameRatePowerSavingsBalanced(!enabled)
            composeView.requestedFrameRate = if (enabled) {
                supportedRefreshRate.toFloat()
            } else {
                View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
            }
        }
    }
}
