package com.adso.eggchecker.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Línea de un pedido con el nombre del tipo y su subtotal. */
data class PedidoDetalleDto(
    @SerializedName("id_detalle_pedido") val idDetallePedido: Int,
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("nombre_tipo") val nombreTipo: String,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio_unitario") val precioUnitario: String,
    @SerializedName("subtotal") val subtotal: String
)

/** Pedido del usuario con su cliente y detalle. */
data class PedidoDto(
    @SerializedName("id_pedido") val idPedido: Int,
    @SerializedName("id_cliente") val idCliente: Int,
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("cliente_nombre") val clienteNombre: String,
    @SerializedName("cliente_direccion") val clienteDireccion: String? = null,
    @SerializedName("fecha_pedido") val fechaPedido: String,
    @SerializedName("estado_pedido") val estadoPedido: String,
    @SerializedName("valor_total") val valorTotal: String,
    @SerializedName("unidades_totales") val unidadesTotales: Int,
    @SerializedName("detalles") val detalles: List<PedidoDetalleDto> = emptyList()
)

/** Línea enviada al crear o editar un pedido. */
data class PedidoDetalleCreateDto(
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio_unitario") val precioUnitario: Double
)

/** Datos para POST /ventas/pedidos. */
data class PedidoCreateDto(
    @SerializedName("id_cliente") val idCliente: Int,
    @SerializedName("detalles") val detalles: List<PedidoDetalleCreateDto>
)

/** Datos para PATCH /ventas/pedidos/{id}. Los nulos se omiten. */
data class PedidoUpdateDto(
    @SerializedName("id_cliente") val idCliente: Int? = null,
    @SerializedName("detalles") val detalles: List<PedidoDetalleCreateDto>? = null
)

/** Datos para PATCH /ventas/pedidos/{id}/estado. */
data class CambiarEstadoDto(
    @SerializedName("estado") val estado: String
)

/** Datos para POST /ventas/pedidos/{id}/eliminar. */
data class PedidoEliminarDto(
    @SerializedName("contrasena") val contrasena: String
)

/** Existencia disponible de un tipo de huevo. */
data class StockTipoDto(
    @SerializedName("id_tipo") val idTipo: Int,
    @SerializedName("nombre_tipo") val nombreTipo: String,
    @SerializedName("cantidad_actual") val cantidadActual: Int,
    @SerializedName("valor_unidad") val valorUnidad: String
)

/** Stock total disponible y detalle por tipo. */
data class StockDto(
    @SerializedName("total_disponible") val totalDisponible: Int,
    @SerializedName("por_tipo") val porTipo: List<StockTipoDto> = emptyList()
)
