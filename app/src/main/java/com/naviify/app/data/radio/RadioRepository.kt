package com.naviify.app.data.radio

import com.naviify.app.core.network.ServerUrlRouter
import com.naviify.app.core.network.SessionStateHolder
import com.naviify.app.core.network.dto.RadioPayload
import com.naviify.app.core.network.dto.RadioStationDto
import com.naviify.app.core.network.dto.RadioTrackDto
import com.naviify.app.core.network.normalizeServerUrl
import com.naviify.app.domain.model.RadioStation
import com.naviify.app.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stations "radio" du jour, servies par le serveur maison :
 * `http://<hote du serveur>:8788/radio.json` (unite systemd `djmeta-http`,
 * genere par `~/scripts/radio_stations.py` sur le serveur).
 *
 * Chaque station arrive avec ses titres complets (id, titre, artiste, album,
 * album_id, duree, bpm) : l'accueil peut donc afficher les cartes et lancer la
 * lecture sans aucun appel Subsonic supplementaire. Le contenu change chaque
 * jour cote serveur ; ici on recharge des que la date change.
 *
 * Comme pour le catalogue DJ, on n'utilise PAS le client Retrofit de l'app :
 * l'endpoint n'est pas authentifie et ne doit pas passer par l'intercepteur qui
 * reecrit les URLs Subsonic.
 */
@Singleton
class RadioRepository @Inject constructor(
    private val sessionState: SessionStateHolder,
    private val router: ServerUrlRouter,
    private val json: Json,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    val isLoaded: Boolean get() = _stations.value.isNotEmpty()

    /** Jour (AAAA-MM-JJ) pour lequel les stations en memoire ont ete chargees. */
    @Volatile
    private var loadedFor: String? = null

    /** Derniere erreur (debug), null si le dernier chargement a reussi. */
    @Volatile
    var lastError: String? = null
        private set

    /** Charge les stations si elles manquent ou si la journee a change. */
    fun ensureLoaded(scope: CoroutineScope) {
        val today = todayKey()
        if (isLoaded && loadedFor == today) return
        scope.launch { refresh() }
    }

    /** Recharge les stations. Retourne le nombre de stations chargees (0 si echec). */
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
                val payload = json.decodeFromString(RadioPayload.serializer(), body)
                val mapped = payload.stations
                    .map { dto -> dto.toDomain() }
                    .filter { it.tracks.isNotEmpty() }
                _stations.value = mapped
                loadedFor = payload.generated.ifBlank { todayKey() }
                lastError = null
                mapped.size
            }
        }.getOrElse { throwable ->
            lastError = throwable.message ?: throwable.javaClass.simpleName
            0
        }
    }

    /** URL de l'endpoint : meme hote que le serveur actif, port dedie 8788. */
    private fun endpointUrl(): String? {
        val config = sessionState.config.value ?: return null
        val raw = router.effectiveSync().ifBlank {
            config.homeServerUrl.ifBlank { config.serverUrl }
        }
        val normalized = normalizeServerUrl(raw)
        if (normalized.isBlank()) return null
        val base = runCatching { normalized.toHttpUrl() }.getOrNull() ?: return null
        return base.newBuilder()
            .port(RADIO_PORT)
            .encodedPath("/$RADIO_FILE")
            .query(null)
            .build()
            .toString()
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        const val RADIO_PORT = 8788
        const val RADIO_FILE = "radio.json"
    }
}

private fun RadioStationDto.toDomain(): RadioStation = RadioStation(
    name = name,
    accent = accent,
    tracks = tracks.map { it.toTrack() },
)

private fun RadioTrackDto.toTrack(): Track = Track(
    id = id,
    title = t,
    artist = a.ifBlank { null },
    album = al.ifBlank { null },
    albumId = aid.ifBlank { null },
    // Le cover art Subsonic s'obtient par id d'album : on reutilise album_id.
    coverArtId = aid.ifBlank { null },
    duration = d,
    bpm = bpm.toInt().takeIf { it > 0 },
)
