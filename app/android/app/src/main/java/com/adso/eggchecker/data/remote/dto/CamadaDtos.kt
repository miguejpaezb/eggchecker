package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Camada devuelta por el backend. Los campos de detalle llegan nulos en el listado. */
data class CamadaDto(
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre_camada") val nombreCamada: String,
    @SerializedName("fecha_ingreso") val fechaIngreso: String,
    @SerializedName("cantidad_inicial") val cantidadInicial: Int,
    @SerializedName("cantidad_actual") val cantidadActual: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("edad_semanas") val edadSemanas: Int,
    @SerializedName("fecha_proximo_aviso") val fechaProximoAviso: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String,
    @SerializedName("requiere_decision") val requiereDecision: Boolean = false,
    @SerializedName("puede_editar_inicial") val puedeEditarInicial: Boolean = false,
    @SerializedName("edad_dias") val edadDias: Int? = null,
    @SerializedName("fecha_retiro_estimada") val fechaRetiroEstimada: String? = null
)

/** Datos para POST /camadas. */
data class CamadaCreateDto(
    @SerializedName("nombre_camada") val nombreCamada: String,
    @SerializedName("fecha_ingreso") val fechaIngreso: String,
    @SerializedName("cantidad_inicial") val cantidadInicial: Int,
    @SerializedName("estado") val estado: String = "activa"
)

/** Datos para PATCH /camadas/{id}. Los nulos se omiten en el JSON. */
data class CamadaUpdateDto(
    @SerializedName("nombre_camada") val nombreCamada: String? = null,
    @SerializedName("cantidad_inicial") val cantidadInicial: Int? = null
)

/** Datos para POST /camadas/{id}/mortalidad. */
data class MortalidadRequestDto(
    @SerializedName("cantidad") val cantidad: Int
)

/** Datos para POST /camadas/{id}/alimentacion. */
data class AlimentacionCreateDto(
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("cantidad") val cantidad: Double,
    @SerializedName("observaciones") val observaciones: String? = null
)

/** Registro de alimentación devuelto por la API. */
data class AlimentacionDto(
    @SerializedName("id_alimentacion") val idAlimentacion: Int,
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("nombre_camada") val nombreCamada: String,
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("unidad_medida") val unidadMedida: String,
    @SerializedName("cantidad") val cantidad: String,
    @SerializedName("fecha_hora") val fechaHora: String,
    @SerializedName("observaciones") val observaciones: String? = null
)
