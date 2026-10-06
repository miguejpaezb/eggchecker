package com.adso.eggchecker.domain.model

/** Cliente (comprador) del avicultor. */
data class Cliente(
    val idCliente: Int,
    val nombreCliente: String,
    val telefono: String?,
    val direccion: String?,
    val fechaUltimaCompra: String?,
    val activo: Boolean
)
