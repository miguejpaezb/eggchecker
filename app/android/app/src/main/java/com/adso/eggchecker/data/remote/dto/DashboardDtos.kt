package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Un día de la serie semanal de producción. */
data class DiaProduccionDto(
    @SerializedName("etiqueta") val etiqueta: String,
    @SerializedName("total_huevos") val totalHuevos: Int
)

/** Día de mayor recolección de la semana. */
data class MejorDiaDto(
    @SerializedName("etiqueta") val etiqueta: String,
    @SerializedName("total_huevos") val totalHuevos: Int
)

/** Resumen de producción de los últimos siete días. */
data class SemanaDashboardDto(
    @SerializedName("serie") val serie: List<DiaProduccionDto> = emptyList(),
    @SerializedName("total_huevos") val totalHuevos: Int,
    @SerializedName("mejor_dia") val mejorDia: MejorDiaDto? = null,
    @SerializedName("valor_producido") val valorProducido: String,
    @SerializedName("tasa_postura") val tasaPostura: String,
    @SerializedName("mortalidad") val mortalidad: Int
)

/** Insumo en alerta del dashboard. */
data class AlertaDashboardDto(
    @SerializedName("id_insumo") val idInsumo: Int,
    @SerializedName("nombre_insumo") val nombreInsumo: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("stock_actual") val stockActual: String,
    @SerializedName("umbral_minimo") val umbralMinimo: String
)

/** Pedido reciente resumido para el dashboard. */
data class PedidoRecienteDto(
    @SerializedName("id_pedido") val idPedido: Int,
    @SerializedName("cliente_nombre") val clienteNombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("fecha_pedido") val fechaPedido: String,
    @SerializedName("estado_pedido") val estadoPedido: String
)

/** Indicadores agregados del dashboard. */
data class DashboardDto(
    @SerializedName("produccion_hoy") val produccionHoy: Int,
    @SerializedName("variacion_produccion") val variacionProduccion: String? = null,
    @SerializedName("aves_activas") val avesActivas: Int,
    @SerializedName("pedidos_pendientes") val pedidosPendientes: Int,
    @SerializedName("alertas_count") val alertasCount: Int,
    @SerializedName("semana") val semana: SemanaDashboardDto,
    @SerializedName("alertas") val alertas: List<AlertaDashboardDto> = emptyList(),
    @SerializedName("pedidos_recientes")
    val pedidosRecientes: List<PedidoRecienteDto> = emptyList()
)
