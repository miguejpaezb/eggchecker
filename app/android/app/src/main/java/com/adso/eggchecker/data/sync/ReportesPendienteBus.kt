package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Rango de fechas con el que se debe abrir la pantalla de reportes. */
data class RangoReporte(
    val desde: String,
    val hasta: String
)

/**
 * Aviso para abrir Reportes con un período precargado (botón "Ver" del
 * recordatorio de reportes).
 */
class ReportesPendienteBus {

    private val _rango = MutableStateFlow<RangoReporte?>(null)

    /** Rango solicitado por el aviso. */
    val rango: StateFlow<RangoReporte?> = _rango.asStateFlow()

    /** Marca el rango que debe abrir la pantalla de reportes. */
    fun solicitar(desde: String, hasta: String) {
        _rango.value = RangoReporte(desde, hasta)
    }

    /** Consume el aviso tras aplicarlo. */
    fun consumir() {
        _rango.value = null
    }
}
