package com.adso.eggchecker.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.ClienteRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de clientes. */
data class ClientesUiState(
    val clientes: List<Cliente> = emptyList(),
    val busqueda: String = "",
    val filtroEstado: String = "activos",
    val cargando: Boolean = true,
    val error: String? = null
)

/** Gestiona los clientes del usuario (modo online). */
class ClientesViewModel(
    private val clienteRepository: ClienteRepository,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val _estado = MutableStateFlow(ClientesUiState())

    /** Estado observable de la pantalla de clientes. */
    val estado: StateFlow<ClientesUiState> = _estado.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
    }

    /** Carga los clientes incluyendo los suspendidos. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            clienteRepository.listar()
                .onSuccess { lista ->
                    _estado.update {
                        it.copy(cargando = false, clientes = lista)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            clientes = emptyList(),
                            error = error.message
                                ?: "No se pudieron cargar los clientes"
                        )
                    }
                }
        }
    }

    /** Actualiza el texto de búsqueda. */
    fun cambiarBusqueda(valor: String) {
        _estado.update { it.copy(busqueda = valor) }
    }

    /** Cambia el filtro de estado. */
    fun cambiarFiltroEstado(valor: String) {
        _estado.update { it.copy(filtroEstado = valor) }
    }

    /** Limpia los filtros a su estado por defecto. */
    fun limpiarFiltros() {
        _estado.update { it.copy(busqueda = "", filtroEstado = "activos") }
    }

    /** Registra un cliente nuevo. */
    fun crearCliente(
        nombre: String,
        telefono: String?,
        direccion: String?,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            clienteRepository.crear(nombre, telefono, direccion)
                .onSuccess {
                    mensajeManager.exito("Cliente registrado")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo registrar")
                    onError()
                }
        }
    }

    /** Edita un cliente. */
    fun editarCliente(
        idCliente: Int,
        nombre: String,
        telefono: String?,
        direccion: String?,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            clienteRepository.editar(idCliente, nombre, telefono, direccion)
                .onSuccess {
                    mensajeManager.exito("Cambios guardados")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo guardar")
                    onError()
                }
        }
    }

    /** Suspende un cliente (reversible). */
    fun suspender(idCliente: Int, onExito: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            clienteRepository.suspender(idCliente)
                .onSuccess {
                    mensajeManager.exito("Cliente suspendido")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo suspender")
                    onError()
                }
        }
    }

    /** Reactiva un cliente suspendido. */
    fun activar(idCliente: Int) {
        viewModelScope.launch {
            clienteRepository.activar(idCliente)
                .onSuccess {
                    mensajeManager.exito("Cliente activado")
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo activar")
                }
        }
    }

    /** Elimina un cliente confirmando la contraseña. */
    fun eliminar(
        idCliente: Int,
        contrasena: String,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            clienteRepository.eliminar(idCliente, contrasena)
                .onSuccess {
                    mensajeManager.exito("Cliente eliminado")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo eliminar")
                    onError()
                }
        }
    }
}
