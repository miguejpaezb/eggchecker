package com.adso.eggchecker.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.ClienteRepository
import com.adso.eggchecker.data.repository.VentaRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.data.sync.VentaPendienteBus
import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.domain.model.LineaPedido
import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.domain.model.Stock
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de ventas. */
data class VentasUiState(
    val pedidos: List<Pedido> = emptyList(),
    val stock: Stock = Stock(totalDisponible = 0, porTipo = emptyList()),
    val clientes: List<Cliente> = emptyList(),
    val busqueda: String = "",
    val fechaFiltro: String = "",
    val filtroEstado: String = "pendiente",
    val cargando: Boolean = true,
    val error: String? = null
)

/** Gestiona los pedidos y el stock del usuario (modo online). */
class VentasViewModel(
    private val ventaRepository: VentaRepository,
    private val clienteRepository: ClienteRepository,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager,
    private val ventaPendienteBus: VentaPendienteBus
) : ViewModel() {

    private val _estado = MutableStateFlow(VentasUiState())

    /** Estado observable de la pantalla de ventas. */
    val estado: StateFlow<VentasUiState> = _estado.asStateFlow()

    private val _clientePendiente = MutableStateFlow<Cliente?>(null)

    /** Cliente que la navegación pidió precargar (o null). */
    val clientePendiente: StateFlow<Cliente?> = _clientePendiente.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
        viewModelScope.launch {
            ventaPendienteBus.clienteId.collect { id ->
                if (id != null) resolverPendiente()
            }
        }
    }

    /** Carga pedidos, stock y clientes activos. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val resultadoPedidos = ventaRepository.listarPedidos()
            val resultadoStock = ventaRepository.obtenerStock()
            val resultadoClientes = clienteRepository.listar()

            val clientes = resultadoClientes.getOrNull()
                ?.filter { it.activo }
                ?: _estado.value.clientes

            _estado.update {
                it.copy(
                    cargando = false,
                    pedidos = resultadoPedidos.getOrNull() ?: emptyList(),
                    stock = resultadoStock.getOrNull()
                        ?: Stock(totalDisponible = 0, porTipo = emptyList()),
                    clientes = clientes,
                    error = if (resultadoPedidos.isFailure) {
                        resultadoPedidos.exceptionOrNull()?.message
                            ?: "No se pudieron cargar los pedidos"
                    } else {
                        null
                    }
                )
            }
            resolverPendiente()
        }
    }

    /** Actualiza el texto de búsqueda. */
    fun cambiarBusqueda(valor: String) {
        _estado.update { it.copy(busqueda = valor) }
    }

    /** Actualiza la fecha del filtro (formato ISO o vacío). */
    fun cambiarFecha(valor: String) {
        _estado.update { it.copy(fechaFiltro = valor) }
    }

    /** Cambia el filtro por estado. */
    fun cambiarFiltroEstado(valor: String) {
        _estado.update { it.copy(filtroEstado = valor) }
    }

    /** Limpia los filtros a su estado por defecto. */
    fun limpiarFiltros() {
        _estado.update {
            it.copy(busqueda = "", fechaFiltro = "", filtroEstado = "pendiente")
        }
    }

    /** Descarta el cliente pendiente tras abrir el modal. */
    fun consumirPendiente() {
        _clientePendiente.value = null
    }

    /** Registra un pedido nuevo. */
    fun crear(
        idCliente: Int,
        lineas: List<LineaPedido>,
        onResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            ventaRepository.crearPedido(idCliente, lineas)
                .onSuccess {
                    mensajeManager.exito("Pedido registrado")
                    onResultado(true)
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo registrar")
                    onResultado(false)
                }
        }
    }

    /** Edita un pedido pendiente. */
    fun editar(
        idPedido: Int,
        idCliente: Int,
        lineas: List<LineaPedido>,
        onResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            ventaRepository.editarPedido(idPedido, idCliente, lineas)
                .onSuccess {
                    mensajeManager.exito("Cambios guardados")
                    onResultado(true)
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo guardar")
                    onResultado(false)
                }
        }
    }

    /** Avanza el estado de un pedido a enviado o recibido. */
    fun cambiarEstado(idPedido: Int, estado: String) {
        viewModelScope.launch {
            ventaRepository.cambiarEstado(idPedido, estado)
                .onSuccess {
                    mensajeManager.exito("Estado actualizado")
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(
                        it.message ?: "No se pudo cambiar el estado"
                    )
                }
        }
    }

    /** Cancela un pedido y repone el stock. */
    fun cancelar(idPedido: Int, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            ventaRepository.cancelar(idPedido)
                .onSuccess {
                    mensajeManager.exito("Pedido cancelado")
                    onResultado(true)
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo cancelar")
                    onResultado(false)
                }
        }
    }

    /** Elimina un pedido confirmando la contraseña. */
    fun eliminar(
        idPedido: Int,
        contrasena: String,
        onResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            ventaRepository.eliminar(idPedido, contrasena)
                .onSuccess {
                    mensajeManager.exito("Pedido eliminado")
                    onResultado(true)
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo eliminar")
                    onResultado(false)
                }
        }
    }

    /** Resuelve el cliente pedido por la navegación si ya está cargado. */
    private fun resolverPendiente() {
        val id = ventaPendienteBus.clienteId.value ?: return
        val cliente = _estado.value.clientes.find { it.idCliente == id } ?: return
        _clientePendiente.value = cliente
        ventaPendienteBus.consumir()
    }
}
