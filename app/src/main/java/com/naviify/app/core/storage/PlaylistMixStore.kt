package com.naviify.app.core.storage

import android.content.Context
import com.naviify.app.domain.model.PlaylistMixConfig
import com.naviify.app.domain.model.PlaylistMixMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistMixStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("naviify_playlist_mix", Context.MODE_PRIVATE)
    private val _configs = MutableStateFlow<Map<String, PlaylistMixConfig>>(emptyMap())
    val configs: StateFlow<Map<String, PlaylistMixConfig>> = _configs.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        val map = mutableMapOf<String, PlaylistMixConfig>()
        val allKeys = prefs.all.keys
        val playlistIds = allKeys.mapNotNull { key ->
            if (key.endsWith("_enabled")) key.removeSuffix("_enabled") else null
        }.toSet()

        for (id in playlistIds) {
            val enabled = prefs.getBoolean("${id}_enabled", false)
            val modeName = prefs.getString("${id}_mode", PlaylistMixMode.AUTO.name) ?: PlaylistMixMode.AUTO.name
            val mode = runCatching { PlaylistMixMode.valueOf(modeName) }.getOrDefault(PlaylistMixMode.AUTO)
            val duration = prefs.getFloat("${id}_duration", 6f)
            val bassSwap = prefs.getBoolean("${id}_bass_swap", true)
            val eqVol = prefs.getBoolean("${id}_eq_vol", true)

            val overridesJson = prefs.getString("${id}_overrides", null)
            val overrides = mutableMapOf<String, PlaylistMixMode>()
            if (!overridesJson.isNullOrBlank()) {
                runCatching {
                    val json = JSONObject(overridesJson)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val mName = json.optString(key, "")
                        runCatching { PlaylistMixMode.valueOf(mName) }.getOrNull()?.let {
                            overrides[key] = it
                        }
                    }
                }
            }

            map[id] = PlaylistMixConfig(
                isEnabled = enabled,
                mode = mode,
                durationSeconds = duration,
                smartBassSwap = bassSwap,
                equalPowerVolume = eqVol,
                transitionOverrides = overrides,
            )
        }
        _configs.value = map
    }

    fun getConfig(playlistId: String): PlaylistMixConfig {
        return _configs.value[playlistId] ?: PlaylistMixConfig()
    }

    fun saveConfig(playlistId: String, config: PlaylistMixConfig) {
        val overridesJson = JSONObject().apply {
            config.transitionOverrides.forEach { (k, v) -> put(k, v.name) }
        }.toString()

        prefs.edit()
            .putBoolean("${playlistId}_enabled", config.isEnabled)
            .putString("${playlistId}_mode", config.mode.name)
            .putFloat("${playlistId}_duration", config.durationSeconds)
            .putBoolean("${playlistId}_bass_swap", config.smartBassSwap)
            .putBoolean("${playlistId}_eq_vol", config.equalPowerVolume)
            .putString("${playlistId}_overrides", overridesJson)
            .apply()

        val updated = _configs.value.toMutableMap()
        updated[playlistId] = config
        _configs.value = updated
    }

    fun setTransitionOverride(
        playlistId: String,
        fromTrackId: String,
        toTrackId: String,
        mode: PlaylistMixMode,
    ) {
        val current = getConfig(playlistId)
        val updated = current.withOverride(fromTrackId, toTrackId, mode)
        saveConfig(playlistId, updated)
    }

    fun removeTransitionOverride(
        playlistId: String,
        fromTrackId: String,
        toTrackId: String,
    ) {
        val current = getConfig(playlistId)
        val updated = current.withoutOverride(fromTrackId, toTrackId)
        saveConfig(playlistId, updated)
    }

    fun resetAllOverrides(playlistId: String) {
        val current = getConfig(playlistId)
        val updated = current.withoutAllOverrides()
        saveConfig(playlistId, updated)
    }

    fun setGlobalModeAndClearOverrides(playlistId: String, mode: PlaylistMixMode) {
        val current = getConfig(playlistId)
        val updated = current.copy(mode = mode, transitionOverrides = emptyMap())
        saveConfig(playlistId, updated)
    }
}
