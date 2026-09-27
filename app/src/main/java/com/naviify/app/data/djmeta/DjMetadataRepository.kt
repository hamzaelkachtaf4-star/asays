package com.naviify.app.data.djmeta

import com.naviify.app.core.network.ServerUrlRouter
import com.naviify.app.core.network.SessionStateHolder
import com.naviify.app.core.network.dto.DjMetaPayload
import com.naviify.app.core.network.dto.DjTrackMetaDto
import com.naviify.app.core.network.dto.MixWaveformDto
import com.naviify.app.core.network.normalizeServerUrl
import com.naviify.app.domain.model.DjTrackMeta
import com.naviify.app.domain.model.MixWaveform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catalogue des metadonnees DJ mesurees (BPM + tonalite Camelot) servi par
 * l'analyseur du serveur maison : `http://<hote du serveur>:8788/djmeta.json`
 * (unite systemd `djmeta-http`, cf. `/home/tayeb/dj-analyzer/README.md`).
 *
 * Le payload est indexe par pid Navidrome, donc l'app retrouve chaque morceau
 * par son `Track.id` sans table de correspondance. Il est charge une fois par
 * session (~200 Ko pour 1487 titres) et garde en memoire.
 *
 * Volontairement separe de l'API Subsonic : l'endpoint n'est pas authentifie et
 * ne doit pas passer par l'intercepteur qui reecrit les URLs Subsonic.
 */
@Singleton
class DjMetadataRepository @Inject constructor(
    private val sessionState: SessionStateHolder,
    private val router: ServerUrlRouter,
    private val json: Json,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _meta = MutableStateFlow<Map<String, DjTrackMeta>>(emptyMap())
    val meta: StateFlow<Map<String, DjTrackMeta>> = _meta.asStateFlow()

    val isLoaded: Boolean get() = _meta.value.isNotEmpty()

    /** Derniere erreur (affichable en debug), null si le dernier chargement a reussi. */
    @Volatile
    var lastError: String? = null
        private set

    /** Lance le chargement si le catalogue n'est pas deja en memoire. */
    fun ensureLoaded(scope: CoroutineScope) {
        if (isLoaded) return
        scope.launch { refresh() }
    }

    /** Recharge le catalogue. Retourne le nombre de titres charges (0 en cas d'echec). */
    suspend fun refresh(): Int = withContext(Dispatchers.IO) {
        val url = endpointUrl()
        if (url == null) {
            lastError = "aucun serveur configure"
            return@withContext 0
        }
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    lastError = "HTTP ${response.code}"
                    return@withContext 0
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    lastError = "reponse vide"
                    return@withContext 0
                }
                val payload = json.decodeFromString(DjMetaPayload.serializer(), body)
                val mapped = payload.tracks.mapValues { (_, dto) -> dto.toDomain() }
                _meta.value = mapped
                lastError = null
                mapped.size
            }
        }.getOrElse { throwable ->
            lastError = throwable.message ?: throwable.javaClass.simpleName
            0
        }
    }

    /** Base du serveur DJ (port dedie 8788), derivee du serveur de musique actif. */
    private fun baseUrl(): HttpUrl? {
        val config = sessionState.config.value ?: return null
        val raw = router.effectiveSync().ifBlank {
            config.homeServerUrl.ifBlank { config.serverUrl }
        }
        val normalized = normalizeServerUrl(raw)
        if (normalized.isBlank()) return null
        val base = runCatching { normalized.toHttpUrl() }.getOrNull() ?: return null
        return base.newBuilder().port(DJMETA_PORT).query(null).build()
    }

    /** URL de l'endpoint : meme hote que le serveur actif, port dedie 8788. */
    private fun endpointUrl(): String? = baseUrl()
        ?.newBuilder()
        ?.encodedPath("/$DJMETA_FILE")
        ?.query(null)
        ?.build()
        ?.toString()

    /**
     * Forme d'onde REELLE d'un morceau, calculee par le serveur en decodant l'audio
     * (`/waveform/<pid>.json`). Renvoie null si le serveur ne repond pas : l'appelant
     * n'affiche alors rien de dessine, plutot que d'inventer une courbe.
     */
    suspend fun fetchWaveform(songId: String): MixWaveform? = withContext(Dispatchers.IO) {
        if (songId.isBlank()) return@withContext null
        val url = baseUrl()
            ?.newBuilder()
            ?.encodedPath("/waveform/$songId.json")
            ?.query(null)
            ?.build()
            ?.toString() ?: return@withContext null
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val dto = waveJson.decodeFromString(MixWaveformDto.serializer(), body)
                MixWaveform(
                    songId = dto.songId.ifBlank { songId },
                    durationMs = dto.durationMs,
                    peaks = dto.peaks,
                    low = dto.low,
                )
            }
        }.getOrNull()
    }

    companion object {
        const val DJMETA_PORT = 8788
        const val DJMETA_FILE = "djmeta.json"

        /** Le serveur renvoie aussi `path` et `points`, inutiles ici. */
        private val waveJson = Json { ignoreUnknownKeys = true }
    }
}

/** DTO du payload -> modele de domaine (les ids sont deja les pids Navidrome). */
private fun DjTrackMetaDto.toDomain(): DjTrackMeta = DjTrackMeta(
    bpm = bpm,
    key = key,
    camelot = camelot,
    bpmConfidence = bpmConfidence,
    keyConfidence = keyConfidence,
)
