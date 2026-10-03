package com.naviify.app.data.connect

import com.naviify.app.core.playback.PlaybackController
import com.naviify.app.domain.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
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

    private val started = AtomicBoolean(false)

    /**
     * Vrai quand ce telephone s'est tu parce qu'un autre appareil a pris la
     * lecture. Garantit une seule coupure par prise de main, et n'est leve que
     * lorsque l'utilisateur relance explicitement la musique ici (ou quand le hub
     * rend la main a ce telephone).
     */
    private val isMutedForRemote = AtomicBoolean(false)

    // Main : MediaController refuse tout appel hors du thread de son looper. Les
    // ordres du hub arrivent sur un thread reseau, on les ramene donc ici.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        repository.start(
            stateProvider = ::snapshot,
            onCommand = ::apply,
            onTransfer = ::adopt,
        )
        // Un SEUL observateur pour les deux sens. Avant, deux coroutines
        // independantes (l'une sur le hub, l'autre sur le lecteur) pouvaient se
        // contredire : l'une coupait le son pendant que l'autre reprenait la main,
        // d'ou des saccades et des ordres croises. Ici chaque changement (appareil
        // actif OU etat de lecture) est traite dans l'ordre, sur un seul fil.
        scope.launch {
            var lastActiveId: String? = null
            var wasPlaying = false
            combine(
                repository.cluster.map { it?.activeId }.distinctUntilChanged(),
                controller.state.map { it.isPlaying }.distinctUntilChanged(),
            ) { activeId, playing -> activeId to playing }
                .collect { (activeId, playing) ->
                    guarded { reconcile(lastActiveId, activeId, wasPlaying, playing) }
                    lastActiveId = activeId
                    wasPlaying = playing
                }
        }
    }

    /**
     * Decide, pour un couple (appareil actif, lecture en cours), s'il faut se taire
     * ou reprendre la main. Appele uniquement depuis l'observateur unique.
     */
    private fun reconcile(
        previousActiveId: String?,
        activeId: String?,
        wasPlaying: Boolean,
        playing: Boolean,
    ) {
        val selfId = repository.deviceId
        val remoteActive = activeId != null && activeId != selfId
        when {
            // Un autre appareil prend la lecture (le Mac, par exemple) : ce
            // telephone devient une telecommande, donc il coupe son propre son.
            // Sans ca les deux appareils jouent en meme temps : c'est le "manque
            // de synchronisation". La pause est envoyee une seule fois par prise
            // de main ; elle est aussi envoyee si le son n'est pas audible a cet
            // instant (appel en cours, mise en tampon), sinon le lecteur
            // repartirait tout seul et volerait la main a l'autre appareil.
            remoteActive && activeId != previousActiveId -> {
                if (isMutedForRemote.compareAndSet(false, true) || playing) {
                    controller.pausePlayback()
                }
            }
            // L'utilisateur lance la musique ICI : ce telephone prend la main, et
            // l'autre appareil s'arrete. C'est ce qui manquait : sans ca le site
            // continuait de jouer et les deux appareils chantaient ensemble.
            // Puisque la coupure ci-dessus a annule la lecture differee, un
            // redemarrage ici ne peut venir que d'un geste explicite.
            playing && !wasPlaying -> {
                isMutedForRemote.set(false)
                if (activeId != selfId) repository.activateSelf()
            }
            // Le hub nous rend la main (ou plus personne ne joue) : plus de coupure.
            !remoteActive -> isMutedForRemote.set(false)
        }
    }

    /** Une erreur ponctuelle ne doit jamais tuer l'observateur ni l'application. */
    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Le hub est un confort : on ignore et on attend le prochain evenement.
        }
    }

    /** Ramene un ordre venu du reseau sur le thread principal du lecteur. */
    private fun onMain(block: () -> Unit) {
        scope.launch { guarded(block) }
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
    private fun apply(command: ConnectCommand) = onMain {
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
    private fun adopt(player: ConnectPlayerState) = onMain {
        val items = player.queue
        if (items.isEmpty()) return@onMain
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