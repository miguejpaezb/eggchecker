package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.AlertaDashboardDto
import com.adso.eggchecker.data.remote.dto.DashboardDto
import com.adso.eggchecker.data.remote.dto.DiaProduccionDto
import com.adso.eggchecker.data.remote.dto.MejorDiaDto
import com.adso.eggchecker.data.remote.dto.PedidoRecienteDto
import com.adso.eggchecker.data.remote.dto.SemanaDashboardDto
import com.adso.eggchecker.domain.model.AlertaDashboard
import com.adso.eggchecker.domain.model.Dashboard
import com.adso.eggchecker.domain.model.DiaProduccion
import com.adso.eggchecker.domain.model.MejorDia
import com.adso.eggchecker.domain.model.PedidoReciente
import com.adso.eggchecker.domain.model.SemanaDashboard

/** Acceso a los indicadores del dashboard del usuario (modo online). */
class DashboardRepository(
    private val api: ApiService
) {

    /** Indicadores agregados del dashboard. */
    suspend fun obtenerDashboard(): Result<Dashboard> =
        apiCall { api.obtenerDashboard() }.map { it.toDomain() }
}

private fun DashboardDto.toDomain(): Dashboard = Dashboard(
    produccionHoy = produccionHoy,
    variacionProduccion = variacionProduccion?.toDoubleOrNull(),
    avesActivas = avesActivas,
    pedidosPendientes = pedidosPendientes,
    alertasCount = alertasCount,
    semana = semana.toDomain(),
    alertas = alertas.map { it.toDomain() },
    pedidosRecientes = pedidosRecientes.map { it.toDomain() }
)

private fun SemanaDashboardDto.toDomain(): SemanaDashboard = SemanaDashboard(
    serie = serie.map { it.toDomain() },
    totalHuevos = totalHuevos,
    mejorDia = mejorDia?.toDomain(),
    valorProducido = valorProducido.toDoubleOrNull() ?: 0.0,
    tasaPostura = tasaPostura.toDoubleOrNull() ?: 0.0,
    mortalidad = mortalidad
)

private fun DiaProduccionDto.toDomain(): DiaProduccion =
    DiaProduccion(etiqueta = etiqueta, totalHuevos = totalHuevos)

private fun MejorDiaDto.toDomain(): MejorDia =
    MejorDia(etiqueta = etiqueta, totalHuevos = totalHuevos)

private fun AlertaDashboardDto.toDomain(): AlertaDashboard = AlertaDashboard(
    idInsumo = idInsumo,
    nombreInsumo = nombreInsumo,
    categoria = categoria,
    stockActual = stockActual.toDoubleOrNull() ?: 0.0,
    umbralMinimo = umbralMinimo.toDoubleOrNull() ?: 0.0
)

private fun PedidoRecienteDto.toDomain(): PedidoReciente = PedidoReciente(
    idPedido = idPedido,
    clienteNombre = clienteNombre,
    descripcion = descripcion,
    fechaPedido = fechaPedido,
    estadoPedido = estadoPedido
)
