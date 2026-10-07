package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.AlimentoReporteDto
import com.adso.eggchecker.data.remote.dto.CamadaReporteDto
import com.adso.eggchecker.data.remote.dto.CubetaTipoDto
import com.adso.eggchecker.data.remote.dto.GananciaReporteDto
import com.adso.eggchecker.data.remote.dto.GastosReporteDto
import com.adso.eggchecker.data.remote.dto.ProduccionReporteDto
import com.adso.eggchecker.data.remote.dto.ReporteConsolidadoDto
import com.adso.eggchecker.data.remote.dto.SaludReporteDto
import com.adso.eggchecker.data.remote.dto.VentasReporteDto
import com.adso.eggchecker.domain.model.AlimentoReporte
import com.adso.eggchecker.domain.model.CamadaReporte
import com.adso.eggchecker.domain.model.CubetaTipo
import com.adso.eggchecker.domain.model.GananciaReporte
import com.adso.eggchecker.domain.model.GastosReporte
import com.adso.eggchecker.domain.model.Periodo
import com.adso.eggchecker.domain.model.ProduccionReporte
import com.adso.eggchecker.domain.model.ReporteConsolidado
import com.adso.eggchecker.domain.model.SaludReporte
import com.adso.eggchecker.domain.model.VentasReporte

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Acceso a los reportes de rentabilidad del usuario (modo online). */
class ReporteRepository(
    private val api: ApiService
) {

    /** Reporte consolidado del período (y camada opcional). */
    suspend fun obtenerConsolidado(
        desde: String,
        hasta: String,
        camada: Int?
    ): Result<ReporteConsolidado> =
        apiCall { api.obtenerReporteConsolidado(desde, hasta, camada) }
            .map { it.toDomain() }

    /** Descarga el PDF del reporte como bytes. */
    suspend fun descargarPdf(
        desde: String,
        hasta: String,
        camada: Int?
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        apiCall { api.descargarReportePdf(desde, hasta, camada).bytes() }
    }
}

private fun ReporteConsolidadoDto.toDomain(): ReporteConsolidado =
    ReporteConsolidado(
        periodo = Periodo(desde = periodo.desde, hasta = periodo.hasta),
        produccion = produccion.toDomain(),
        ventas = ventas.toDomain(),
        gastos = gastos.toDomain(),
        ganancia = ganancia.toDomain(),
        salud = salud.toDomain(),
        alimento = alimento.toDomain(),
        camada = camada?.toDomain()
    )

private fun ProduccionReporteDto.toDomain(): ProduccionReporte =
    ProduccionReporte(
        totalHuevos = totalHuevos,
        cubetasCompletas = cubetasCompletas,
        huevosNoAptos = huevosNoAptos,
        promedioDiario = promedioDiario,
        cubetasPorTipo = cubetasPorTipo.map { it.toDomain() }
    )

private fun CubetaTipoDto.toDomain(): CubetaTipo =
    CubetaTipo(nombreTipo = nombreTipo, cubetas = cubetas)

private fun VentasReporteDto.toDomain(): VentasReporte =
    VentasReporte(ingresoTotal = ingresoTotal.toDoubleOrNull() ?: 0.0)

private fun GastosReporteDto.toDomain(): GastosReporte =
    GastosReporte(total = total.toDoubleOrNull() ?: 0.0)

private fun GananciaReporteDto.toDomain(): GananciaReporte = GananciaReporte(
    valor = valor.toDoubleOrNull() ?: 0.0,
    porcentaje = porcentaje.toDoubleOrNull() ?: 0.0
)

private fun SaludReporteDto.toDomain(): SaludReporte = SaludReporte(
    gallinasPerdidas = gallinasPerdidas,
    causaPrincipal = causaPrincipal,
    vacunacionAlDia = vacunacionAlDia
)

private fun AlimentoReporteDto.toDomain(): AlimentoReporte = AlimentoReporte(
    kgUsados = kgUsados.toDoubleOrNull() ?: 0.0,
    promedioDiario = promedioDiario.toDoubleOrNull() ?: 0.0
)

private fun CamadaReporteDto.toDomain(): CamadaReporte =
    CamadaReporte(nombreCamada = nombreCamada)
