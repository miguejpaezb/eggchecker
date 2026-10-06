package com.adso.eggchecker.ui.produccion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.CamadaRepository
import com.adso.eggchecker.data.repository.ProduccionRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.Produccion
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de producción diaria. */
data class ProduccionUiState(
    val camadas: List<Camada> = emptyList(),
    val camadaSeleccionada: Camada? = null,
    val fecha: String = hoyIso(),
    val cantidades: Map<String, Int> = cantidadesCero(),
    val producciones: List<Produccion> = emptyList(),
    val cargando: Boolean = true,
    val cargandoDia: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null
)

/** Gestiona la recolección diaria por camada (modo online). */
class ProduccionViewModel(
    private val camadaRepository: CamadaRepository,
    private val produccionRepository: ProduccionRepository,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val _estado = MutableStateFlow(ProduccionUiState())

    /** Estado observable de la pantalla de producción. */
    val estado: StateFlow<ProduccionUiState> = _estado.asStateFlow()

    init {
        cargarCamadas()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargarCamadas() }
        }
    }

    /** Carga las camadas activas en etapa de producción y selecciona una. */
    private fun cargarCamadas() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            camadaRepository.listar("activa")
                .onSuccess { lista ->
                    val enProduccion = lista.filter { camadaEnProduccion(it) }
                    val actual = _estado.value.camadaSeleccionada
                    val seleccionada = actual?.takeIf { camada ->
                        enProduccion.any { it.idCamada == camada.idCamada }
                    } ?: enProduccion.firstOrNull()
                    _estado.update {
                        it.copy(
                            cargando = false,
                            camadas = enProduccion,
                            camadaSeleccionada = seleccionada
                        )
                    }
                    if (seleccionada != null) {
                        cargarDia()
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

    /** Selecciona una camada y vuelve la fecha a hoy. */
    fun seleccionarCamada(camada: Camada) {
        _estado.update {
            it.copy(camadaSeleccionada = camada, fecha = hoyIso(), error = null)
        }
        cargarDia()
    }

    /** Cambia la fecha seleccionada (no permite futuro). */
    fun cambiarFecha(fecha: String) {
        if (fecha > hoyIso()) return
        _estado.update { it.copy(fecha = fecha, error = null) }
        cargarDia()
    }

    /** Carga las producciones de la camada y las cantidades del día. */
    private fun cargarDia() {
        val camada = _estado.value.camadaSeleccionada ?: return
        viewModelScope.launch {
            _estado.update { it.copy(cargandoDia = true, error = null) }
            produccionRepository.listar(camada.idCamada)
                .onSuccess { lista ->
                    val fecha = _estado.value.fecha
                    val delDia = lista.find { it.fechaRecoleccion == fecha }
                    if (delDia == null) {
                        _estado.update {
                            it.copy(
                                cargandoDia = false,
                                producciones = lista,
                                cantidades = cantidadesCero()
                            )
                        }
                    } else {
                        produccionRepository.detalle(delDia.idProduccion)
                            .onSuccess { detalle ->
                                _estado.update {
                                    it.copy(
                                        cargandoDia = false,
                                        producciones = lista,
                                        cantidades = cantidadesDesdeDetalle(
                                            detalle.detalle
                                        )
                                    )
                                }
                            }
                            .onFailure { error ->
                                _estado.update {
                                    it.copy(
                                        cargandoDia = false,
                                        producciones = lista,
                                        cantidades = cantidadesCero(),
                                        error = error.message
                                    )
                                }
                            }
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargandoDia = false,
                            producciones = emptyList(),
                            cantidades = cantidadesCero(),
                            error = error.message
                                ?: "No se pudo cargar la producción"
                        )
                    }
                }
        }
    }

    /** Aumenta en uno la cantidad de un tipo, respetando el límite. */
    fun incrementar(clave: String) {
        val estado = _estado.value
        if (!edicionHabilitada(estado)) return
        val siguiente = estado.cantidades.toMutableMap()
        siguiente[clave] = (siguiente[clave] ?: 0) + 1
        val limite = estado.camadaSeleccionada?.cantidadActual ?: 0
        if (totalHuevos(siguiente) > limite) return
        _estado.update { it.copy(cantidades = siguiente) }
    }

    /** Disminuye en uno la cantidad de un tipo (mínimo 0). */
    fun decrementar(clave: String) {
        val estado = _estado.value
        if (!edicionHabilitada(estado)) return
        val siguiente = estado.cantidades.toMutableMap()
        siguiente[clave] = maxOf(0, (siguiente[clave] ?: 0) - 1)
        _estado.update { it.copy(cantidades = siguiente) }
    }

    /** Guarda (crea o actualiza) la recolección del día. */
    fun guardar() {
        val estado = _estado.value
        val camada = estado.camadaSeleccionada ?: return
        if (!edicionHabilitada(estado) || totalHuevos(estado.cantidades) <= 0) return

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            produccionRepository.registrar(
                idCamada = camada.idCamada,
                fechaRecoleccion = estado.fecha,
                aa = estado.cantidades["aa"] ?: 0,
                a = estado.cantidades["a"] ?: 0,
                b = estado.cantidades["b"] ?: 0,
                noApto = estado.cantidades["no_apto"] ?: 0
            ).onSuccess {
                _estado.update { it.copy(guardando = false) }
                mensajeManager.exito("Producción guardada correctamente")
                cargarDia()
            }.onFailure { error ->
                _estado.update { it.copy(guardando = false) }
                mensajeManager.error(
                    error.message ?: "No se pudo guardar la producción"
                )
            }
        }
    }
}

/** Indica si la fecha seleccionada es hoy y se puede editar. */
fun edicionHabilitada(estado: ProduccionUiState): Boolean =
    estado.camadaSeleccionada != null &&
        estado.fecha == hoyIso() &&
        !estado.cargandoDia
