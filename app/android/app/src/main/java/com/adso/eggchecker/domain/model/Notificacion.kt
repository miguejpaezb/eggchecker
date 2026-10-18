package com.adso.eggchecker.domain.model

/** Notificación del usuario usada por la UI. */
data class Notificacion(
    val idNotificacion: Int,
    val idCamada: Int?,
    val idInsumo: Int?,
    val idRecordatorio: Int?,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val fechaReferencia: String?,
    val momento: String?
)
