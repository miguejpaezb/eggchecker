package com.adso.eggchecker.ui.mensajes

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Lleva la cuenta de cuántos modales (Dialog) están abiertos. La raíz usa
 * esto para ocultar su toast mientras un modal lo muestra dentro de su
 * propia ventana, que queda por encima.
 */
object ModalEstado {

    /** Cantidad de modales abiertos. */
    var abiertos by mutableIntStateOf(0)
        private set

    /** Registra la apertura de un modal. */
    fun registrar() {
        abiertos += 1
    }

    /** Registra el cierre de un modal. */
    fun quitar() {
        if (abiertos > 0) abiertos -= 1
    }
}
