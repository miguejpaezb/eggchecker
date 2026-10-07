package com.adso.eggchecker.domain.model

/** Rango de fechas del reporte. */
data class Periodo(
    val desde: String,
    val hasta: String
)

/** Cubetas completas producidas de un tipo de huevo. */
data class CubetaTipo(
    val nombreTipo: String,
    val cubetas: Int
)

/** Resumen de producción del período. */
data class ProduccionReporte(
    val totalHuevos: Int,
    val cubetasCompletas: Int,
    val huevosNoAptos: Int,
    val promedioDiario: Double,
    val cubetasPorTipo: List<CubetaTipo>
)

/** Resumen de ventas del período. */
data class VentasReporte(
    val ingresoTotal: Double
)

/** Gasto del período. */
data class GastosReporte(
    val total: Double
)

/** Ganancia estimada del período. */
data class GananciaReporte(
    val valor: Double,
    val porcentaje: Double
)

/** Salud y bajas del período. */
data class SaludReporte(
    val gallinasPerdidas: Int,
    val causaPrincipal: String?,
    val vacunacionAlDia: Boolean
)

/** Consumo de alimento del período. */
data class AlimentoReporte(
    val kgUsados: Double,
    val promedioDiario: Double
)

/** Ficha de una camada incluida en el reporte. */
data class CamadaReporte(
    val nombreCamada: String
)

/** Reporte de rentabilidad consolidado del período. */
data class ReporteConsolidado(
    val periodo: Periodo,
    val produccion: ProduccionReporte,
    val ventas: VentasReporte,
    val gastos: GastosReporte,
    val ganancia: GananciaReporte,
    val salud: SaludReporte,
    val alimento: AlimentoReporte,
    val camada: CamadaReporte?
)
