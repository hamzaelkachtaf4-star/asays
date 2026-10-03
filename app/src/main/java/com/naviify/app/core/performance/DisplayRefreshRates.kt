package com.naviify.app.core.performance

import android.view.Display
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_PERFORMANCE_REFRESH_RATE = 50
private const val MAX_REASONABLE_REFRESH_RATE = 240

/**
 * Refresh rates the current display can actually select, normalized for UI labels.
 * Dynamically probes supported display refresh rates.
 */
fun Display?.supportedPerformanceRefreshRates(): List<Int> {
    val fallback = this?.refreshRate?.roundToInt()?.coerceAtLeast(MIN_PERFORMANCE_REFRESH_RATE) ?: 60
    return this?.supportedModes
        ?.asSequence()
        ?.map { it.refreshRate.roundToInt() }
        ?.filter { it in MIN_PERFORMANCE_REFRESH_RATE..MAX_REASONABLE_REFRESH_RATE }
        ?.distinct()
        ?.sorted()
        ?.toList()
        ?.ifEmpty { listOf(fallback) }
        ?: listOf(fallback)
}

/**
 * Maps a saved preference to a real mode supported by this device's display,
 * including preferences restored across different hardware devices.
 */
fun Display?.resolvePerformanceRefreshRate(preferred: Int): Int =
    supportedPerformanceRefreshRates().minByOrNull { abs(it - preferred) } ?: 60
