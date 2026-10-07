package com.adso.eggchecker.domain.model

/** Perfil completo del avicultor usado por la pantalla de Perfil. */
data class Perfil(
    val idUsuario: Int,
    val nombreCompleto: String,
    val correoElectronico: String,
    val telefono: String?,
    val nombreGranja: String?,
    val planSuscripcion: String,
    val fechaRegistro: String,
    val activo: Boolean,
    val avesMax: Int?,
    val clientesMax: Int?,
    val iaIncluida: Boolean,
    val avesActuales: Int,
    val clientesActuales: Int,
    val totalHuevosProducidos: Int,
    val totalAvesGestionadas: Int,
    val notifProduccionBaja: Boolean,
    val notifStockBajo: Boolean,
    val notifVacunacion: Boolean,
    val notifResumenSemanal: Boolean
)
