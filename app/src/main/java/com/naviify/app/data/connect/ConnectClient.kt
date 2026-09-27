package com.naviify.app.data.connect

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * Le transport vers le hub.
 *
 * Ce client possede son PROPRE [OkHttpClient] : celui de l'application porte les
 * intercepteurs d'authentification Subsonic (qui reecrivent l'hote et exigent des
 * parametres `u`/`t`/`s`) et un `callTimeout` de 20 s. Les deux tueraient le flux
 * d'evenements, qui doit rester ouvert des heures.
 */
@Singleton
class ConnectClient @Inject constructor(private val json: Json) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Le flux d'evenements du hub (Server-Sent Events). Le flux se termine des que
     * la connexion tombe : c'est [ConnectRepository] qui decide de se reconnecter.
     */
    fun events(baseUrl: String, deviceId: String): Flow<ConnectEvent> = callbackFlow {
        val request = Request.Builder()
            .url("$baseUrl/api/events?device=$deviceId")
            .header("Accept", "text/event-stream")
            .build()
        val call = http.newCall(request)

        val worker = thread(name = "asays-connect-sse", isDaemon = true) {
            try {
                call.execute().use { response ->
                    val source = response.body?.source() ?: return@use
                    var name = "message"
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        when {
                            line.startsWith("event:") -> name = line.removePrefix("event:").trim()
                            line.startsWith("data:") -> {
                                val event = parse(name, line.removePrefix("data:").trim())
                                if (event != null) trySend(event)
                            }
                            // Les lignes vides separent les evenements, les lignes
                            // commencant par ':' sont des commentaires de maintien
                            // de connexion : rien a faire dans les deux cas.
                            else -> Unit
                        }
                    }
                }
            } catch (_: Throwable) {
                // reseau coupe, hub redemarre : la boucle de reconnexion s'en charge
            } finally {
                close()
            }
        }

        awaitClose {
            call.cancel()
            worker.interrupt()
        }
    }

    /** Un ordre court (annonce, etat, commande, transfert). */
    fun post(baseUrl: String, path: String, body: String): Boolean = runCatching {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        http.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)

    /** Encode n'importe lequel de nos messages en JSON. */
    inline fun <reified T> encode(value: T): String = json.encodeToString(value)
    // (retire : l'encodage se fait avec des serializers explicites, plus surs a
    // relire et sans dependre d'une extension importee par effet de bord)

    private fun parse(name: String, data: String): ConnectEvent? = runCatching {
        when (name) {
            "cluster" -> ConnectEvent.Cluster(json.decodeFromString(ConnectCluster.serializer(), data))
            "hello" -> ConnectEvent.Hello(json.decodeFromString(ConnectHello.serializer(), data))
            "command" -> ConnectEvent.Command(json.decodeFromString(ConnectCommand.serializer(), data))
            "transfer" -> ConnectEvent.Transferred(json.decodeFromString(ConnectTransfer.serializer(), data))
            else -> null
        }
    }.getOrNull()

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}