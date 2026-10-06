package com.adso.eggchecker.ui.mensajes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tipo visual del mensaje flotante. */
enum class TipoMensaje { EXITO, ERROR }

/** Mensaje flotante con su texto y tipo. */
data class Mensaje(
    val id: Long,
    val texto: String,
    val tipo: TipoMensaje
)

/** Tiempo visible de cada mensaje antes de cerrarse solo. */
private const val MS_VISIBLE = 3500L

/**
 * Estado global de los mensajes tipo toast. Mantiene la cola y el mensaje
 * actual, para que cualquier host (raíz o dentro de un modal) muestre el
 * mismo mensaje sin duplicarlo.
 */
class MensajeManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cola = ArrayDeque<Mensaje>()

    private val _actual = MutableStateFlow<Mensaje?>(null)

    /** Mensaje visible en este momento (o null si no hay ninguno). */
    val actual: StateFlow<Mensaje?> = _actual.asStateFlow()

    private var contador = 0L
    private var cierre: Job? = null

    /** Encola un mensaje de éxito. */
    fun exito(texto: String) {
        emitir(texto, TipoMensaje.EXITO)
    }

    /** Encola un mensaje de error. */
    fun error(texto: String) {
        emitir(texto, TipoMensaje.ERROR)
    }

    /** Cierra el mensaje actual y muestra el siguiente de la cola. */
    fun descartar() {
        cierre?.cancel()
        cierre = null
        val siguiente = if (cola.isNotEmpty()) cola.removeFirst() else null
        _actual.value = siguiente
        if (siguiente != null) {
            programarCierre()
        }
    }

    private fun emitir(texto: String, tipo: TipoMensaje) {
        if (texto.isBlank()) return
        contador += 1
        val mensaje = Mensaje(id = contador, texto = texto, tipo = tipo)
        if (_actual.value == null) {
            _actual.value = mensaje
            programarCierre()
        } else {
            cola.addLast(mensaje)
        }
    }

    private fun programarCierre() {
        cierre?.cancel()
        cierre = scope.launch {
            delay(MS_VISIBLE)
            descartar()
        }
    }
}
