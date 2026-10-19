package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Aviso persistente devuelto por GET /notificaciones. */
data class NotificacionDto(
    @SerializedName("id_notificacion") val idNotificacion: Int,
    @SerializedName("id_camada") val idCamada: Int? = null,
    @SerializedName("id_insumo") val idInsumo: Int? = null,
    @SerializedName("id_recordatorio") val idRecordatorio: Int? = null,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("leida") val leida: Boolean,
    @SerializedName("fecha_referencia") val fechaReferencia: String? = null,
    @SerializedName("momento") val momento: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String
)
