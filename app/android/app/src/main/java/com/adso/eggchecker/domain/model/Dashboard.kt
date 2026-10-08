package com.adso.eggchecker.domain.model

/** Un día de la serie semanal de producción. */
data class DiaProduccion(
    val etiqueta: String,
    val totalHuevos: Int
)

/** Día de mayor recolección de la semana. */
data class MejorDia(
    val etiqueta: String,
    val totalHuevos: Int
)

/** Resumen de producción de los últimos siete días. */
data class SemanaDashboard(
    val serie: List<DiaProduccion>,
    val totalHuevos: Int,
    val mejorDia: MejorDia?,
    val valorProducido: Double,
    val tasaPostura: Double,
    val mortalidad: Int
)

/** Insumo en alerta del dashboard. */
data class AlertaDashboard(
    val idInsumo: Int,
    val nombreInsumo: String,
    val categoria: String,
    val stockActual: Double,
    val umbralMinimo: Double
)

/** Pedido reciente resumido para el dashboard. */
data class PedidoReciente(
    val idPedido: Int,
    val clienteNombre: String,
    val descripcion: String,
    val fechaPedido: String,
    val estadoPedido: String
)

/** Indicadores agregados del dashboard. */
data class Dashboard(
    val produccionHoy: Int,
    val variacionProduccion: Double?,
    val avesActivas: Int,
    val pedidosPendientes: Int,
    val alertasCount: Int,
    val semana: SemanaDashboard,
    val alertas: List<AlertaDashboard>,
    val pedidosRecientes: List<PedidoReciente>
)
