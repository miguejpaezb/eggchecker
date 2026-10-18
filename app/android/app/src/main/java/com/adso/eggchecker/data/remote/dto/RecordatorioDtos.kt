package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Recordatorio sanitario (regla) devuelto por /recordatorios. */
data class RecordatorioDto(
    @SerializedName("id_recordatorio") val idRecordatorio: Int,
    @SerializedName("id_camada") val idCamada: Int? = null,
    @SerializedName("nombre_camada") val nombreCamada: String? = null,
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("unidad_medida") val unidadMedida: String,
    @SerializedName("stock_actual") val stockActual: String,
    @SerializedName("cantidad") val cantidad: String,
    @SerializedName("modo") val modo: String,
    @SerializedName("intervalo_dias") val intervaloDias: Int? = null,
    @SerializedName("dias_semana") val diasSemana: List<Int>? = null,
    @SerializedName("fechas") val fechas: List<String>? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("activo") val activo: Boolean = true,
    @SerializedName("proxima_dosis") val proximaDosis: String? = null,
    @SerializedName("dosis_pendientes") val dosisPendientes: Int = 0,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null
)

/** Datos para POST y PUT /recordatorios. */
data class RecordatorioCreateDto(
    @SerializedName("id_camada") val idCamada: Int? = null,
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("modo") val modo: String,
    @SerializedName("intervalo_dias") val intervaloDias: Int? = null,
    @SerializedName("dias_semana") val diasSemana: List<Int>? = null,
    @SerializedName("fechas") val fechas: List<String>? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("cantidad") val cantidad: Double,
    @SerializedName("activo") val activo: Boolean = true
)

/** Aplicación programada de un recordatorio en una camada. */
data class DosisDto(
    @SerializedName("id_dosis") val idDosis: Int,
    @SerializedName("id_recordatorio") val idRecordatorio: Int,
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("nombre_camada") val nombreCamada: String,
    @SerializedName("fecha_programada") val fechaProgramada: String,
    @SerializedName("numero_dosis") val numeroDosis: Int,
    @SerializedName("aplicada") val aplicada: Boolean,
    @SerializedName("fecha_aplicacion") val fechaAplicacion: String? = null,
    @SerializedName("id_movimiento") val idMovimiento: Int? = null,
    @SerializedName("id_evento") val idEvento: Int? = null
)

/** Dosis con el insumo y el stock, para el modal de aplicación. */
data class DosisDetalleDto(
    @SerializedName("id_dosis") val idDosis: Int,
    @SerializedName("id_recordatorio") val idRecordatorio: Int,
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("nombre_camada") val nombreCamada: String,
    @SerializedName("fecha_programada") val fechaProgramada: String,
    @SerializedName("numero_dosis") val numeroDosis: Int,
    @SerializedName("aplicada") val aplicada: Boolean,
    @SerializedName("fecha_aplicacion") val fechaAplicacion: String? = null,
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("unidad_medida") val unidadMedida: String,
    @SerializedName("cantidad") val cantidad: String,
    @SerializedName("stock_actual") val stockActual: String,
    @SerializedName("id_movimiento") val idMovimiento: Int? = null,
    @SerializedName("id_evento") val idEvento: Int? = null
)

/** Body de POST /recordatorios/dosis/{id}/aplicar. */
data class DosisAplicarDto(
    @SerializedName("cantidad") val cantidad: Double? = null
)

/** Body de POST /recordatorios/dosis/aplicar-lote. */
data class DosisLoteDto(
    @SerializedName("ids_dosis") val idsDosis: List<Int>,
    @SerializedName("cantidad") val cantidad: Double? = null
)

/** Resultado de registrar una aplicación. */
data class DosisAplicadaDto(
    @SerializedName("id_dosis") val idDosis: Int,
    @SerializedName("id_recordatorio") val idRecordatorio: Int,
    @SerializedName("id_camada") val idCamada: Int,
    @SerializedName("nombre_camada") val nombreCamada: String,
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("unidad_medida") val unidadMedida: String,
    @SerializedName("cantidad") val cantidad: String,
    @SerializedName("stock_resultante") val stockResultante: String,
    @SerializedName("fecha_programada") val fechaProgramada: String,
    @SerializedName("fecha_aplicacion") val fechaAplicacion: String,
    @SerializedName("id_movimiento") val idMovimiento: Int,
    @SerializedName("id_evento") val idEvento: Int
)
