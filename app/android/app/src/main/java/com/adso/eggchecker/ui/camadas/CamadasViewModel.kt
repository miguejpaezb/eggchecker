package com.adso.eggchecker.ui.camadas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.CamadaRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del listado de camadas. */
data class CamadasUiState(
    val camadas: List<Camada> = emptyList(),
    val filtro: String = "activa",
    val cargando: Boolean = true,
    val error: String? = null
)

/** Carga y gestiona las camadas del usuario (modo online). */
class CamadasViewModel(
    private val camadaRepository: CamadaRepository,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val _estado = MutableStateFlow(CamadasUiState())

    /** Estado observable de la pantalla de camadas. */
    val estado: StateFlow<CamadasUiState> = _estado.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
    }

    /** Cambia el filtro de estado y recarga la lista. */
    fun cambiarFiltro(filtro: String) {
        _estado.update { it.copy(filtro = filtro) }
        cargar()
    }

    /** Recarga la lista según el filtro actual. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val filtro = _estado.value.filtro
            camadaRepository.listar(filtro.ifBlank { null })
                .onSuccess { lista ->
                    _estado.update {
                        it.copy(cargando = false, camadas = lista)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            camadas = emptyList(),
                            error = error.message
                                ?: "No se pudieron cargar las camadas"
                        )
                    }
                }
        }
    }

    /** Carga el detalle de una camada (edad en días y retiro estimado). */
    fun cargarDetalle(
        idCamada: Int,
        onResultado: (Camada?, String?) -> Unit
    ) {
        viewModelScope.launch {
            camadaRepository.detalle(idCamada)
                .onSuccess { onResultado(it, null) }
                .onFailure {
                    onResultado(null, it.message ?: "No se pudo cargar el detalle")
                }
        }
    }

    /** Crea una camada nueva. */
    fun crear(
        nombre: String,
        fechaIngreso: String,
        cantidadInicial: Int,
        estado: String,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            camadaRepository.crear(nombre, fechaIngreso, cantidadInicial, estado)
                .onSuccess {
                    mensajeManager.exito("Camada creada correctamente")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo registrar")
                    onError()
                }
        }
    }

    /** Edita el nombre y, si aplica, la cantidad inicial. */
    fun editar(
        idCamada: Int,
        nombre: String,
        cantidadInicial: Int?,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            camadaRepository.editar(idCamada, nombre, cantidadInicial)
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

    /** Registra mortalidad en una camada. */
    fun mortalidad(
        idCamada: Int,
        cantidad: Int,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            camadaRepository.mortalidad(idCamada, cantidad)
                .onSuccess {
                    mensajeManager.exito("Mortalidad registrada")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo registrar")
                    onError()
                }
        }
    }

    /** Suma una semana de vida a la camada. */
    fun avanzarSemana(idCamada: Int) {
        viewModelScope.launch {
            camadaRepository.avanzarSemana(idCamada)
                .onSuccess { cargar() }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo avanzar")
                }
        }
    }

    /** Posponer una semana la decisión de la camada. */
    fun seguirActiva(idCamada: Int) {
        viewModelScope.launch {
            camadaRepository.seguirActiva(idCamada)
                .onSuccess { cargar() }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo continuar")
                }
        }
    }

    /** Descarta la camada (irreversible). */
    fun descartar(
        idCamada: Int,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            camadaRepository.descartar(idCamada)
                .onSuccess {
                    mensajeManager.exito("Camada descartada")
                    onExito()
                    cargar()
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo descartar")
                    onError()
                }
        }
    }
}
