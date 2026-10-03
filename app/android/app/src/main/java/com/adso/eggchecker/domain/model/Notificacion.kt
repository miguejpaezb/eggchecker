package com.adso.eggchecker.domain.model

/** Notificación del usuario usada por la UI. */
data class Notificacion(
    val idNotificacion: Int,
    val idCamada: Int?,
    val idInsumo: Int?,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean
)
