package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Producción diaria sin el detalle por tipo. */
data class ProduccionDto(
    @SerializedName("id_produccion") val idProduccion: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("fecha_recoleccion") val fechaRecoleccion: String,
    @SerializedName("total_huevos") val totalHuevos: Int,
    @SerializedName("observaciones") val observaciones: String? = null
)

/** Línea de detalle: cantidad de un tipo de huevo. */
data class ProduccionDetalleDto(
    @SerializedName("id_detalle") val idDetalle: Int,
    @SerializedName("id_produccion") val idProduccion: Int,
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("nombre_tipo") val nombreTipo: String,
    @SerializedName("cantidad") val cantidad: Int
)

/** Producción con su desglose por tipo de huevo. */
data class ProduccionConDetalleDto(
    @SerializedName("id_produccion") val idProduccion: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("fecha_recoleccion") val fechaRecoleccion: String,
    @SerializedName("total_huevos") val totalHuevos: Int,
    @SerializedName("observaciones") val observaciones: String? = null,
    @SerializedName("detalle") val detalle: List<ProduccionDetalleDto> = emptyList()
)

/** Datos para POST /produccion. */
data class ProduccionCreateDto(
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("fecha_recoleccion") val fechaRecoleccion: String,
    @SerializedName("unidad") val unidad: String = "unidad",
    @SerializedName("aa") val aa: Int,
    @SerializedName("a") val a: Int,
    @SerializedName("b") val b: Int,
    @SerializedName("no_apto") val noApto: Int
)
