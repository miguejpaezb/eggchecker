package com.adso.eggchecker.domain.model

/** Detalle de producción de un tipo de huevo. */
data class ProduccionDetalle(
    val idTipo: Int,
    val nombreTipo: String,
    val cantidad: Int
)

/** Producción diaria de una camada. */
data class Produccion(
    val idProduccion: Int,
    val idCamada: Int,
    val fechaRecoleccion: String,
    val totalHuevos: Int,
    val observaciones: String?,
    val detalle: List<ProduccionDetalle>
)
