package com.adso.eggchecker.domain.model

/** Camada del avicultor usada por la UI. */
data class Camada(
    val idCamada: Int,
    val idUsuario: Int,
    val nombreCamada: String,
    val fechaIngreso: String,
    val cantidadInicial: Int,
    val cantidadActual: Int,
    val estado: String,
    val edadSemanas: Int,
    val fechaProximoAviso: String?,
    val fechaCreacion: String,
    val requiereDecision: Boolean,
    val puedeEditarInicial: Boolean,
    val edadDias: Int?,
    val fechaRetiroEstimada: String?
) {
    /** La camada está en etapa activa. */
    val esActiva: Boolean get() = estado == "activa"
}

/** Registro del alimento consumido por una camada. */
data class Alimentacion(
    val idAlimentacion: Int,
    val idCamada: Int,
    val nombreCamada: String,
    val idInsumo: Int,
    val nombreInsumo: String,
    val unidadMedida: String,
    val cantidad: Double,
    val fechaHora: String,
    val observaciones: String?
)
