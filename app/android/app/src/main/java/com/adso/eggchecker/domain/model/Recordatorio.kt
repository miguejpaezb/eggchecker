package com.adso.eggchecker.domain.model

/** Modos de programación de un recordatorio (los mismos del backend). */
const val MODO_CADA_DIAS = "cada_dias"
const val MODO_DIAS_SEMANA = "dias_semana"
const val MODO_FECHAS = "fechas"

/** Regla de vacunación o suministro de vitaminas. */
data class Recordatorio(
    val idRecordatorio: Int,
    val idCamada: Int?,
    val nombreCamada: String?,
    val idInsumo: Int,
    val nombreInsumo: String,
    val unidadMedida: String,
    val stockActual: Double,
    val cantidad: Double,
    val modo: String,
    val intervaloDias: Int?,
    val diasSemana: List<Int>,
    val fechas: List<String>,
    val fechaInicio: String,
    val fechaFin: String?,
    val activo: Boolean,
    val proximaDosis: String?,
    val dosisPendientes: Int
) {
    /** Un recordatorio global aplica a todas las camadas activas. */
    val esGlobal: Boolean get() = idCamada == null
}

/** Dosis programada de un recordatorio. */
data class Dosis(
    val idDosis: Int,
    val idRecordatorio: Int,
    val idCamada: Int,
    val nombreCamada: String,
    val fechaProgramada: String,
    val numeroDosis: Int,
    val aplicada: Boolean,
    val fechaAplicacion: String?
)

/** Dosis con el insumo y el stock para el modal de aplicación. */
data class DosisDetalle(
    val idDosis: Int,
    val idRecordatorio: Int,
    val idCamada: Int,
    val nombreCamada: String,
    val fechaProgramada: String,
    val numeroDosis: Int,
    val aplicada: Boolean,
    val idInsumo: Int,
    val nombreInsumo: String,
    val unidadMedida: String,
    val cantidad: Double,
    val stockActual: Double
)

/** Resultado de registrar una aplicación de dosis. */
data class DosisAplicada(
    val idDosis: Int,
    val nombreCamada: String,
    val nombreInsumo: String,
    val unidadMedida: String,
    val cantidad: Double,
    val stockResultante: Double
)
