package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Aplicación pendiente que debe abrir el modal desde una notificación. */
data class SolicitudAplicarDosis(
    val idRecordatorio: Int,
    val fecha: String
)

/**
 * Aviso para abrir el modal de aplicación de una dosis (botón "Ver" de
 * los recordatorios sanitarios). Lo escucha el shell, así funciona desde
 * cualquier pantalla.
 */
class RecordatorioAplicarBus {

    private val _solicitud = MutableStateFlow<SolicitudAplicarDosis?>(null)

    /** Dosis pendiente de abrir en el modal. */
    val solicitud: StateFlow<SolicitudAplicarDosis?> = _solicitud.asStateFlow()

    /** Marca el recordatorio y la fecha que se deben aplicar. */
    fun solicitar(idRecordatorio: Int, fecha: String) {
        _solicitud.value = SolicitudAplicarDosis(idRecordatorio, fecha)
    }

    /** Consume el aviso tras abrir el modal. */
    fun consumir() {
        _solicitud.value = null
    }
}
