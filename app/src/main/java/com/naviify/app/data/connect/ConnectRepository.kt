package com.naviify.app.data.connect

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.naviify.app.core.network.ServerUrlRouter
import com.naviify.app.core.network.SessionStateHolder
import com.naviify.app.core.network.normalizeServerUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Le lien entre l'application et le hub ASAYS Connect.
 *
 * Le hub tourne sur la meme machine que Navidrome, sur un port dedie (3030) :
 * l'adresse se deduit donc du serveur de musique actif, exactement comme pour le
 * serveur DJ. Rien a configurer cote telephone.
 *
 * L'appareil s'annonce au hub, y publie ce qu'il joue toutes les deux secondes
 * quand il est l'appareil actif, et recoit en retour les ordres du site web ou
 * d'un autre telephone.
 */
@Singleton
class ConnectRepository @Inject constructor(
    private val client: ConnectClient,
    private val router: ServerUrlRouter,
    private val sessionState: SessionStateHolder,
    private val json: Json,
    @ApplicationContext private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _cluster = MutableStateFlow<ConnectCluster?>(null)
    val cluster: StateFlow<ConnectCluster?> = _cluster.asStateFlow()

    /** Identifiant stable de ce telephone (aucun stockage necessaire). */
    val deviceId: String by lazy { "phone-${androidId().takeLast(8)}" }

    val deviceName: String by lazy {
        val raw = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
            .joinToString(" ")
        raw.ifBlank { "Telephone ASAYS" }.replaceFirstChar { it.uppercase() }
    }

    val isActive: Boolean get() = _cluster.value?.activeId == deviceId
    val devices: List<ConnectDevice> get() = _cluster.value?.devices.orEmpty()
    val activeDevice: ConnectDevice? get() = devices.firstOrNull { it.isActive }

    private var started = false

    /**
     * Branche ce telephone sur le hub. [stateProvider] est appele a chaque
     * battement de coeur (toutes les 2 s, et seulement si l'appareil est actif).
     */
    fun start(
        stateProvider: () -> ConnectPlayerState,
        onCommand: (ConnectCommand) -> Unit,
        onTransfer: (ConnectPlayerState) -> Unit,
    ) {
        if (started) return
        started = true
        // runCatching : une exception dans une coroutine non rattrapee tuerait
        // l'application entiere (le SupervisorJob protege les voisines, pas la
        // thread). Le hub est un confort, jamais une raison de planter.
        scope.launch { runCatching { eventLoop(onCommand, onTransfer) } }
        scope.launch { runCatching { heartbeatLoop(stateProvider) } }
    }

    /** Publie l'etat du lecteur (morceau, position, file d'attente). */
    fun publish(state: ConnectPlayerState) {
        val base = hubBase() ?: return
        val payload = json.encodeToString(
            ConnectPlayerState.serializer(),
            state.copy(deviceId = deviceId, deviceName = deviceName),
        )
        scope.launch { client.post(base, "/api/state", payload) }
    }

    /** Confie la lecture a l'appareil [target]. */
    fun transferTo(target: String) {
        val base = hubBase() ?: return
        val payload = json.encodeToString(
            TransferBody.serializer(),
            TransferBody(from = deviceId, to = target),
        )
        scope.launch { client.post(base, "/api/transfer", payload) }
    }

    /** Reprend la main sur cet appareil (l'autre s'arrete). */
    fun activateSelf() {
        val base = hubBase() ?: return
        val payload = json.encodeToString(
            ActiveBody.serializer(),
            ActiveBody(deviceId = deviceId, id = deviceId),
        )
        scope.launch { client.post(base, "/api/active", payload) }
    }

    /** Envoie un ordre a un appareil (play/pause/next/seek...). */
    fun commandTo(target: String, command: ConnectCommand) {
        val base = hubBase() ?: return
        val payload = json.encodeToString(
            CommandBody.serializer(),
            CommandBody(to = target, command = command),
        )
        scope.launch { client.post(base, "/api/command", payload) }
    }

    // ------------------------------------------------------------------ interne

    private suspend fun eventLoop(
        onCommand: (ConnectCommand) -> Unit,
        onTransfer: (ConnectPlayerState) -> Unit,
    ) {
        while (scope.isActive) {
            val base = hubBase()
            if (base == null) {
                delay(RETRY_MS)
                continue
            }
            register(base)
            client.events(base, deviceId).collect { event ->
                when (event) {
                    is ConnectEvent.Cluster -> _cluster.value = event.cluster
                    is ConnectEvent.Hello -> Unit
                    is ConnectEvent.Command -> onCommand(event.command)
                    is ConnectEvent.Transferred -> event.transfer.player?.let(onTransfer)
                }
            }
            // Le flux s'est termine (hub redemarre, reseau coupe) : on retente.
            delay(RETRY_MS)
        }
    }

    private suspend fun heartbeatLoop(stateProvider: () -> ConnectPlayerState) {
        while (scope.isActive) {
            delay(HEARTBEAT_MS)
            if (isActive) publish(stateProvider())
        }
    }

    private fun register(base: String) {
        val device = ConnectDevice(
            id = deviceId,
            name = deviceName,
            kind = "phone",
            platform = "Android ${Build.VERSION.RELEASE}",
        )
        client.post(base, "/api/register", json.encodeToString(ConnectDevice.serializer(), device))
    }

    /** Meme hote que le serveur actif, port dedie au hub. */
    private fun hubBase(): String? {
        val config = sessionState.config.value ?: return null
        val raw = router.effectiveSync().ifBlank {
            config.homeServerUrl.ifBlank { config.serverUrl }
        }
        val normalized = normalizeServerUrl(raw)
        if (normalized.isBlank()) return null
        val base = runCatching { normalized.toHttpUrl() }.getOrNull() ?: return null
        return base.newBuilder().port(HUB_PORT).query(null).build().toString().trimEnd('/')
    }

    @SuppressLint("HardwareIds")
    private fun androidId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "asays"

    /** Corps des messages : noms de champs attendus par le hub. */
    @Serializable
    private data class TransferBody(val from: String, val to: String)

    @Serializable
    private data class ActiveBody(val deviceId: String, val id: String)

    @Serializable
    private data class CommandBody(val to: String, val command: ConnectCommand)

    private companion object {
        const val HUB_PORT = 3030
        const val HEARTBEAT_MS = 2_000L
        const val RETRY_MS = 3_000L
    }
}