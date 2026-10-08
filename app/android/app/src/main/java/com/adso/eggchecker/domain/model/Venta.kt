package com.adso.eggchecker.domain.model

/** Línea de un pedido (tipo de huevo, cantidad y precio). */
data class PedidoDetalle(
    val idTipo: Int,
    val nombreTipo: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

/** Pedido del avicultor con su cliente y detalle. */
data class Pedido(
    val idPedido: Int,
    val idCliente: Int,
    val clienteNombre: String,
    val clienteDireccion: String?,
    val fechaPedido: String,
    val estadoPedido: String,
    val valorTotal: Double,
    val unidadesTotales: Int,
    val detalles: List<PedidoDetalle>
)

/** Línea que la UI envía al crear o editar un pedido. */
data class LineaPedido(
    val idTipo: Int,
    val cantidad: Int,
    val precioUnitario: Double
)

/** Existencia disponible de un tipo de huevo. */
data class StockTipo(
    val idTipo: Int,
    val nombreTipo: String,
    val cantidadActual: Int,
    val valorUnidad: Double
)

/** Stock total disponible del usuario. */
data class Stock(
    val totalDisponible: Int,
    val porTipo: List<StockTipo>
)
