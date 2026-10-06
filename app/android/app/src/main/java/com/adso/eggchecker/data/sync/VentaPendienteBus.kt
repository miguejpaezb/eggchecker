package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aviso compartido para abrir el módulo de Ventas con un cliente
 * precargado (acción "Registrar venta" desde Clientes).
 */
class VentaPendienteBus {

    private val _clienteId = MutableStateFlow<Int?>(null)

    /** Cliente que se debe precargar en el modal de Nuevo Pedido. */
    val clienteId: StateFlow<Int?> = _clienteId.asStateFlow()

    /** Marca el cliente a precargar. */
    fun solicitar(idCliente: Int) {
        _clienteId.value = idCliente
    }

    /** Consume el aviso tras abrir el modal. */
    fun consumir() {
        _clienteId.value = null
    }
}
