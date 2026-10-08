package com.adso.eggchecker.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.CategoriaRepository
import com.adso.eggchecker.data.repository.InsumoRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Categoria
import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de inventario. */
data class InventarioUiState(
    val insumos: List<Insumo> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val busqueda: String = "",
    val filtroStock: String = "",
    val filtroCategoria: Int? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

/** Gestiona insumos y categorías del inventario (modo online). */
class InventarioViewModel(
    private val insumoRepository: InsumoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val _estado = MutableStateFlow(InventarioUiState())

    /** Estado observable de la pantalla de inventario. */
    val estado: StateFlow<InventarioUiState> = _estado.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
    }

    /** Carga insumos (incluye suspendidos) y categorías. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val resultadoInsumos = insumoRepository.listar()
            val resultadoCategorias = categoriaRepository.listar()
            val error = resultadoInsumos.exceptionOrNull()
                ?: resultadoCategorias.exceptionOrNull()
            if (error != null) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        error = error.message
                            ?: "No se pudo cargar el inventario"
                    )
                }
            } else {
                _estado.update {
                    it.copy(
                        cargando = false,
                        insumos = resultadoInsumos.getOrDefault(emptyList()),
                        categorias = resultadoCategorias.getOrDefault(emptyList())
                    )
                }
            }
        }
    }

    /** Actualiza el texto de búsqueda. */
    fun cambiarBusqueda(valor: String) {
        _estado.update { it.copy(busqueda = valor) }
    }

    /** Cambia el filtro de nivel de stock. */
    fun cambiarFiltroStock(valor: String) {
        _estado.update { it.copy(filtroStock = valor) }
    }

    /** Cambia el filtro de categoría (null = todas). */
    fun cambiarFiltroCategoria(idCategoria: Int?) {
        _estado.update { it.copy(filtroCategoria = idCategoria) }
    }

    /** Limpia todos los filtros. */
    fun limpiarFiltros() {
        _estado.update {
            it.copy(busqueda = "", filtroStock = "", filtroCategoria = null)
        }
    }

    /** Registra un insumo nuevo. */
    fun crearInsumo(
        idCategoria: Int,
        nombre: String,
        unidad: String,
        stock: Double,
        umbral: Double,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            insumoRepository.crear(idCategoria, nombre, unidad, stock, umbral)
                .onSuccess {
                    mensajeManager.exito("Insumo registrado")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo registrar")
                    onError()
                }
        }
    }

    /** Edita un insumo. */
    fun editarInsumo(
        idInsumo: Int,
        nombre: String,
        unidad: String,
        umbral: Double,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            insumoRepository.editar(idInsumo, nombre, unidad, umbral)
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

    /** Suma stock a un insumo. */
    fun anadirStock(
        idInsumo: Int,
        cantidad: Double,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            insumoRepository.anadirStock(idInsumo, cantidad)
                .onSuccess {
                    mensajeManager.exito("Stock actualizado")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo añadir stock")
                    onError()
                }
        }
    }

    /** Suspende un insumo (reversible). */
    fun suspender(idInsumo: Int) {
        viewModelScope.launch {
            insumoRepository.suspender(idInsumo)
                .onSuccess {
                    mensajeManager.exito("Insumo suspendido")
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo suspender")
                }
        }
    }

    /** Reactiva un insumo suspendido. */
    fun activar(idInsumo: Int) {
        viewModelScope.launch {
            insumoRepository.activar(idInsumo)
                .onSuccess {
                    mensajeManager.exito("Insumo activado")
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo activar")
                }
        }
    }

    /** Descontinúa un insumo (permanente). */
    fun descontinuar(
        idInsumo: Int,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            insumoRepository.descontinuar(idInsumo)
                .onSuccess {
                    mensajeManager.exito("Insumo descontinuado")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo descontinuar")
                    onError()
                }
        }
    }

    /** Crea una categoría nueva. */
    fun crearCategoria(
        nombre: String,
        descripcion: String?,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            categoriaRepository.crear(nombre, descripcion)
                .onSuccess {
                    mensajeManager.exito("Categoría creada")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo crear")
                    onError()
                }
        }
    }

    /** Edita una categoría. */
    fun editarCategoria(
        idCategoria: Int,
        nombre: String,
        descripcion: String?,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            categoriaRepository.actualizar(idCategoria, nombre, descripcion)
                .onSuccess {
                    mensajeManager.exito("Categoría actualizada")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo actualizar")
                    onError()
                }
        }
    }

    /** Elimina una categoría sin insumos. */
    fun eliminarCategoria(
        idCategoria: Int,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            categoriaRepository.eliminar(idCategoria)
                .onSuccess {
                    mensajeManager.exito("Categoría eliminada")
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
