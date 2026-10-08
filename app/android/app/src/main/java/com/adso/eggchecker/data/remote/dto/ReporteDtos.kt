package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Rango de fechas y agrupación del reporte. */
data class PeriodoDto(
    @SerializedName("desde") val desde: String,
    @SerializedName("hasta") val hasta: String
)

/** Cubetas completas producidas de un tipo de huevo. */
data class CubetaTipoDto(
    @SerializedName("nombre_tipo") val nombreTipo: String,
    @SerializedName("cubetas") val cubetas: Int
)

/** Resumen de producción del período. */
data class ProduccionReporteDto(
    @SerializedName("total_huevos") val totalHuevos: Int,
    @SerializedName("cubetas_completas") val cubetasCompletas: Int,
    @SerializedName("huevos_no_aptos") val huevosNoAptos: Int,
    @SerializedName("promedio_diario") val promedioDiario: Double,
    @SerializedName("cubetas_por_tipo") val cubetasPorTipo: List<CubetaTipoDto> =
        emptyList()
)

/** Ingreso total del período. */
data class VentasReporteDto(
    @SerializedName("ingreso_total") val ingresoTotal: String
)

/** Gasto total del período. */
data class GastosReporteDto(
    @SerializedName("total") val total: String
)

/** Ganancia estimada del período. */
data class GananciaReporteDto(
    @SerializedName("valor") val valor: String,
    @SerializedName("porcentaje") val porcentaje: String
)

/** Salud y bajas del período. */
data class SaludReporteDto(
    @SerializedName("gallinas_perdidas") val gallinasPerdidas: Int,
    @SerializedName("causa_principal") val causaPrincipal: String? = null,
    @SerializedName("vacunacion_al_dia") val vacunacionAlDia: Boolean
)

/** Consumo de alimento del período. */
data class AlimentoReporteDto(
    @SerializedName("kg_usados") val kgUsados: String,
    @SerializedName("promedio_diario") val promedioDiario: String
)

/** Ficha de una camada incluida en el reporte. */
data class CamadaReporteDto(
    @SerializedName("nombre_camada") val nombreCamada: String
)

/** Reporte de rentabilidad consolidado del período. */
data class ReporteConsolidadoDto(
    @SerializedName("periodo") val periodo: PeriodoDto,
    @SerializedName("produccion") val produccion: ProduccionReporteDto,
    @SerializedName("ventas") val ventas: VentasReporteDto,
    @SerializedName("gastos") val gastos: GastosReporteDto,
    @SerializedName("ganancia") val ganancia: GananciaReporteDto,
    @SerializedName("salud") val salud: SaludReporteDto,
    @SerializedName("alimento") val alimento: AlimentoReporteDto,
    @SerializedName("camada") val camada: CamadaReporteDto? = null
)
