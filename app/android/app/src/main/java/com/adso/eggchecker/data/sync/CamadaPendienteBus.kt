package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aviso para abrir el detalle de una camada (acción "Ver" de una
 * notificación de camada al límite).
 */
class CamadaPendienteBus {

    private val _camadaId = MutableStateFlow<Int?>(null)

    /** Camada cuyo detalle se debe abrir. */
    val camadaId: StateFlow<Int?> = _camadaId.asStateFlow()

    /** Marca la camada a abrir. */
    fun solicitar(idCamada: Int) {
        _camadaId.value = idCamada
    }

    /** Consume el aviso tras abrir el detalle. */
    fun consumir() {
        _camadaId.value = null
    }
}
