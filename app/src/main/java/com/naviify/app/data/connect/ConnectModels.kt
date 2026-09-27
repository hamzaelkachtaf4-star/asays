package com.naviify.app.data.connect

import kotlinx.serialization.Serializable

/**
 * Les messages echanges avec le hub ASAYS Connect.
 *
 * Les formes suivent exactement ce que le hub emet sur son flux d'evenements :
 *  - `cluster`  : etat complet (appareils + lecteur de l'appareil actif)
 *  - `hello`    : accuse d'enregistrement de ce flux
 *  - `command`  : ordre a plat (`type`, `from`, `at`) — pas d'enveloppe imbriquee
 *  - `transfer` : `player` a reprendre tel quel
 *
 * Tous les champs ont une valeur par defaut : une version plus recente du hub ne
 * doit jamais faire echouer la lecture cote application.
 */

@Serializable
data class ConnectDevice(
    val id: String,
    val name: String = "",
    val kind: String = "phone",
    val platform: String = "",
    val lastSeen: Long = 0L,
    val isActive: Boolean = false,
)

/** Un morceau de la file partagee (assez pour reprendre la lecture ailleurs). */
@Serializable
data class ConnectQueueItem(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val coverArt: String? = null,
    val duration: Int = 0,
)

@Serializable
data class ConnectPlayerState(
    val trackId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val coverArt: String? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playing: Boolean = false,
    val volume: Double = 1.0,
    /** La file, en clair : chaque appareil peut la reprendre sans requete. */
    val queue: List<ConnectQueueItem> = emptyList(),
    val queueIndex: Int = 0,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val updatedAt: Long = 0L,
)

@Serializable
data class ConnectCluster(
    val revision: Int = 0,
    val activeId: String? = null,
    val devices: List<ConnectDevice> = emptyList(),
    val player: ConnectPlayerState? = null,
)

@Serializable
data class ConnectHello(
    val deviceId: String? = null,
    val revision: Int = 0,
)

/** Un ordre venu d'un autre appareil. */
@Serializable
data class ConnectCommand(
    val type: String,
    val from: String? = null,
    val at: Long = 0L,
    val positionMs: Long? = null,
    val volume: Double? = null,
    val queue: List<String>? = null,
)

/** La lecture nous est confiee : on reprend `player` tel quel. */
@Serializable
data class ConnectTransfer(
    val from: String? = null,
    val player: ConnectPlayerState? = null,
    val at: Long = 0L,
)

sealed interface ConnectEvent {
    data class Cluster(val cluster: ConnectCluster) : ConnectEvent
    data class Hello(val hello: ConnectHello) : ConnectEvent
    data class Command(val command: ConnectCommand) : ConnectEvent
    data class Transferred(val transfer: ConnectTransfer) : ConnectEvent
}