package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Avisa a las pantallas que el usuario pidió refrescar (pull-to-refresh).
 * Cada ViewModel de módulo se suscribe y recarga sus datos.
 */
class RefreshBus {

    private val _eventos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Eventos emitidos al soltar el gesto de recarga. */
    val eventos: SharedFlow<Unit> = _eventos.asSharedFlow()

    /** Solicita una recarga de la pantalla actual. */
    fun solicitar() {
        _eventos.tryEmit(Unit)
    }
}
