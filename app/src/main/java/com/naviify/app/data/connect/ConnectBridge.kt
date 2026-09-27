package com.naviify.app.data.connect

import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Le pont entre le lecteur de l'application et le hub.
 *
 * Deux sens :
 *  - sortant : un instantane du lecteur part vers le hub toutes les deux secondes
 *    quand ce telephone est l'appareil actif, ce qui alimente le site web et
 *    l'ecran de l'autre appareil ;
 *  - entrant : les ordres recus (lecture, pause, suivant, position) sont appliques
 *    au lecteur, comme si l'utilisateur avait touche l'ecran.
 *
 * Tout se deduit de `PlaybackController.state` : le pont ne maintient aucun etat
 * de son cote, donc rien ne peut se desynchroniser.
 */
@Singleton
class ConnectBridge @Inject constructor(
    private val repository: ConnectRepository,
    private val controller: PlaybackController,
) {

    private var started = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (started) return
        started = true
        repository.start(
            stateProvider = ::snapshot,
            onCommand = ::apply,
            onTransfer = ::adopt,
        )
        // Un autre appareil prend la lecture (le Mac, par exemple) : ce telephone
        // devient une telecommande, donc il coupe son propre son. Sans ca les deux
        // appareils jouent en meme temps : c'est le "manque de synchronisation".
        scope.launch {
            runCatching {
                var mine = true
                repository.cluster.collect { cluster ->
                    val activeId = cluster?.activeId
                    val isMine = activeId == null || activeId == repository.deviceId
                    if (!isMine && mine && controller.state.value.isPlaying) {
                        controller.pausePlayback()
                    }
                    mine = isMine
                }
            }
        }
        // L'utilisateur lance la musique ICI : ce telephone prend la main, et
        // l'autre appareil s'arrete. C'est ce qui manquait : sans ca le site
        // continuait de jouer et les deux appareils chantaient ensemble.
        scope.launch {
            runCatching {
                var wasPlaying = false
                controller.state.collect { state ->
                    val playing = state.isPlaying
                    if (playing && !wasPlaying && !repository.isActive) repository.activateSelf()
                    wasPlaying = playing
                }
            }
        }
    }

    /** Ce que le hub (et donc le site) voit de ce telephone. */
    private fun snapshot(): ConnectPlayerState {
        val state = controller.state.value
        val track = state.currentTrack
        return ConnectPlayerState(
            trackId = track?.id,
            title = track?.title,
            artist = track?.artist,
            album = track?.album,
            coverArt = track?.coverArtId,
            durationMs = state.durationMs,
            positionMs = state.positionMs,
            playing = state.isPlaying,
            volume = 1.0,
            queue = state.queue.map {
                ConnectQueueItem(
                    id = it.id,
                    title = it.title ?: "",
                    artist = it.artist ?: "",
                    album = it.album ?: "",
                    coverArt = it.coverArtId,
                    duration = it.duration,
                )
            },
            queueIndex = state.currentIndex,
            deviceId = repository.deviceId,
            deviceName = repository.deviceName,
        )
    }

    /** Ordre venu d'un autre appareil. */
    private fun apply(command: ConnectCommand) {
        val state = controller.state.value
        when (command.type) {
            "play" -> if (!state.isPlaying) controller.togglePlayPause()
            "pause" -> if (state.isPlaying) controller.pausePlayback()
            "toggle" -> controller.togglePlayPause()
            "next" -> controller.next()
            "prev" -> controller.previous()
            "seek" -> command.positionMs?.let { controller.seekTo(it) }
            // "volume" et "set-queue" ne sont pas appliques pour l'instant : le
            // premier n'a pas d'equivalent dans le lecteur, le second demande de
            // resoudre des identifiants Subsonic en morceaux (etape suivante).
            else -> Unit
        }
    }

    /**
     * La lecture nous est confiee depuis un autre appareil : on reprend la meme
     * file, au meme morceau et a la meme seconde.
     *
     * La file partagee ne transporte que des identifiants Subsonic et de quoi
     * afficher la pochette, donc on reconstruit des morceaux jouables a partir de
     * ces champs : la lecture reprend sans aucune requete au serveur.
     */
    private fun adopt(player: ConnectPlayerState) {
        val items = player.queue
        if (items.isEmpty()) return
        val queue = items.map { it.toTrack() }
        val index = player.queueIndex.coerceIn(0, queue.lastIndex)
        controller.play(queue, startIndex = index)
        if (player.positionMs > 0) controller.seekTo(player.positionMs)
        // Un transfert demande pendant une pause doit nous laisser en pause.
        if (!player.playing) controller.pausePlayback()
    }

    private fun ConnectQueueItem.toTrack(): Track = Track(
        id = id,
        title = title.ifBlank { "Sans titre" },
        artist = artist.ifBlank { null },
        album = album.ifBlank { null },
        coverArtId = coverArt,
        duration = duration,
    )
}