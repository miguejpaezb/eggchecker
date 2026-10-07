package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aviso para abrir el panel de notificaciones, disparado desde una
 * notificación del sistema (botón Ver o toque sobre el aviso).
 */
class AbrirNotificacionesBus {

    private val _solicitado = MutableStateFlow(false)

    /** Indica que la app debe abrir el panel de notificaciones. */
    val solicitado: StateFlow<Boolean> = _solicitado.asStateFlow()

    /** Marca que hay que abrir el panel. */
    fun solicitar() {
        _solicitado.value = true
    }

    /** Consume el aviso tras abrir el panel. */
    fun consumir() {
        _solicitado.value = false
    }
}
