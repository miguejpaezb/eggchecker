package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.CambiarEstadoDto
import com.adso.eggchecker.data.remote.dto.PedidoCreateDto
import com.adso.eggchecker.data.remote.dto.PedidoDetalleCreateDto
import com.adso.eggchecker.data.remote.dto.PedidoDetalleDto
import com.adso.eggchecker.data.remote.dto.PedidoDto
import com.adso.eggchecker.data.remote.dto.PedidoEliminarDto
import com.adso.eggchecker.data.remote.dto.PedidoUpdateDto
import com.adso.eggchecker.data.remote.dto.StockDto
import com.adso.eggchecker.data.remote.dto.StockTipoDto
import com.adso.eggchecker.domain.model.LineaPedido
import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.domain.model.PedidoDetalle
import com.adso.eggchecker.domain.model.Stock
import com.adso.eggchecker.domain.model.StockTipo

/** Acceso a las ventas (pedidos y stock) del usuario (modo online). */
class VentaRepository(
    private val api: ApiService
) {

    /** Lista los pedidos del usuario. */
    suspend fun listarPedidos(): Result<List<Pedido>> =
        apiCall { api.listarPedidos() }
            .map { lista -> lista.map { it.toDomain() } }

    /** Stock de huevos disponible por tipo. */
    suspend fun obtenerStock(): Result<Stock> =
        apiCall { api.obtenerStock() }.map { it.toDomain() }

    /** Registra un pedido nuevo. */
    suspend fun crearPedido(
        idCliente: Int,
        lineas: List<LineaPedido>
    ): Result<Pedido> = apiCall {
        api.crearPedido(
            PedidoCreateDto(
                idCliente = idCliente,
                detalles = lineas.map { it.toDto() }
            )
        )
    }.map { it.toDomain() }

    /** Edita un pedido pendiente. */
    suspend fun editarPedido(
        idPedido: Int,
        idCliente: Int,
        lineas: List<LineaPedido>
    ): Result<Pedido> = apiCall {
        api.actualizarPedido(
            idPedido,
            PedidoUpdateDto(
                idCliente = idCliente,
                detalles = lineas.map { it.toDto() }
            )
        )
    }.map { it.toDomain() }

    /** Avanza el estado de un pedido. */
    suspend fun cambiarEstado(idPedido: Int, estado: String): Result<Pedido> =
        apiCall { api.cambiarEstadoPedido(idPedido, CambiarEstadoDto(estado)) }
            .map { it.toDomain() }

    /** Cancela un pedido y repone el stock. */
    suspend fun cancelar(idPedido: Int): Result<Pedido> =
        apiCall { api.cancelarPedido(idPedido) }.map { it.toDomain() }

    /** Elimina un pedido confirmando la contraseña. */
    suspend fun eliminar(idPedido: Int, contrasena: String): Result<Unit> =
        apiCall { api.eliminarPedido(idPedido, PedidoEliminarDto(contrasena)) }
            .map { }
}

private fun LineaPedido.toDto(): PedidoDetalleCreateDto = PedidoDetalleCreateDto(
    idTipo = idTipo,
    cantidad = cantidad,
    precioUnitario = precioUnitario
)

private fun PedidoDetalleDto.toDomain(): PedidoDetalle = PedidoDetalle(
    idTipo = idTipo,
    nombreTipo = nombreTipo,
    cantidad = cantidad,
    precioUnitario = precioUnitario.toDoubleOrNull() ?: 0.0,
    subtotal = subtotal.toDoubleOrNull() ?: 0.0
)

private fun PedidoDto.toDomain(): Pedido = Pedido(
    idPedido = idPedido,
    idCliente = idCliente,
    clienteNombre = clienteNombre,
    clienteDireccion = clienteDireccion,
    fechaPedido = fechaPedido,
    estadoPedido = estadoPedido,
    valorTotal = valorTotal.toDoubleOrNull() ?: 0.0,
    unidadesTotales = unidadesTotales,
    detalles = detalles.map { it.toDomain() }
)

private fun StockTipoDto.toDomain(): StockTipo = StockTipo(
    idTipo = idTipo,
    nombreTipo = nombreTipo,
    cantidadActual = cantidadActual,
    valorUnidad = valorUnidad.toDoubleOrNull() ?: 0.0
)

private fun StockDto.toDomain(): Stock = Stock(
    totalDisponible = totalDisponible,
    porTipo = porTipo.map { it.toDomain() }
)
