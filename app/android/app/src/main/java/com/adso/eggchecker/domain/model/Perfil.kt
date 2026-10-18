package com.adso.eggchecker.domain.model

/** Preferencias de alertas del avicultor tal como las edita el Perfil. */
data class PreferenciasNotificaciones(
    val produccionBaja: Boolean = true,
    val camadaLimite: Boolean = true,
    val stockBajo: Boolean = true,
    val vacunacion: Boolean = false,
    val resumenSemanal: Boolean = true,
    /** Hora del análisis diario en formato "HH:mm". */
    val produccionHora: String = "05:00",
    val resumenFrecuencia: String = "semanal",
    val resumenDia: Int = 0
)

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
    val notifCamadaLimite: Boolean,
    val notifProduccionHora: String,
    val notifStockBajo: Boolean,
    val notifVacunacion: Boolean,
    val notifResumenSemanal: Boolean,
    val notifResumenFrecuencia: String,
    val notifResumenDia: Int
)
