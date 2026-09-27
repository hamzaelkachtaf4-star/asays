package com.naviify.app.ui.connect

import androidx.lifecycle.ViewModel
import com.naviify.app.data.connect.ConnectCluster
import com.naviify.app.data.connect.ConnectCommand
import com.naviify.app.data.connect.ConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * L'ecran « Ecouter sur » : les appareils du foyer et les ordres qu'on peut leur
 * envoyer.
 *
 * Toute la logique vit dans [ConnectRepository] ; ce modele ne fait que l'exposer
 * a l'interface, sans garder d'etat a lui (le flux du hub reste la seule verite).
 */
@HiltViewModel
class ConnectDevicesViewModel @Inject constructor(
    private val repository: ConnectRepository,
) : ViewModel() {

    /** Appareils du foyer + lecteur de l'appareil actif. */
    val cluster: StateFlow<ConnectCluster?> = repository.cluster

    /** Identifiant et nom de ce telephone. */
    val selfId: String get() = repository.deviceId

    val selfName: String get() = repository.deviceName

    /** Confie la lecture a un autre appareil : celui-ci s'arrete. */
    fun transferTo(target: String) = repository.transferTo(target)

    /** Reprend la main ici (l'autre appareil s'arrete). */
    fun activateSelf() = repository.activateSelf()

    /** Envoie un ordre a l'appareil qui joue : play, pause, next, prev, seek. */
    fun send(target: String, type: String, positionMs: Long? = null) {
        repository.commandTo(
            target,
            ConnectCommand(type = type, from = selfId, positionMs = positionMs),
        )
    }
}
