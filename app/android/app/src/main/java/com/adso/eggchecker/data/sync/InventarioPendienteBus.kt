package com.adso.eggchecker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aviso para abrir el inventario con el modal de stock de un insumo
 * (acción "Detalles" de las alertas del dashboard).
 */
class InventarioPendienteBus {

    private val _insumoId = MutableStateFlow<Int?>(null)

    /** Insumo cuyo modal de stock se debe abrir. */
    val insumoId: StateFlow<Int?> = _insumoId.asStateFlow()

    /** Marca el insumo a abrir. */
    fun solicitar(idInsumo: Int) {
        _insumoId.value = idInsumo
    }

    /** Consume el aviso tras abrir el modal. */
    fun consumir() {
        _insumoId.value = null
    }
}
