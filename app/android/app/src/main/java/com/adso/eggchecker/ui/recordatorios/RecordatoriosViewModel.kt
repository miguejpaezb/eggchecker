package com.adso.eggchecker.ui.recordatorios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.CategoriaRepository
import com.adso.eggchecker.data.repository.InsumoRepository
import com.adso.eggchecker.data.repository.RecordatorioRepository
import com.adso.eggchecker.domain.model.Categoria
import com.adso.eggchecker.domain.model.Dosis
import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.domain.model.Recordatorio
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Datos del formulario de un recordatorio. */
data class DatosRecordatorio(
    val idCamada: Int?,
    val idInsumo: Int,
    val modo: String,
    val intervaloDias: Int?,
    val diasSemana: List<Int>?,
    val fechas: List<String>?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val cantidad: Double,
    val activo: Boolean
)

/** Estado de la gestión de recordatorios de una camada. */
data class RecordatoriosUiState(
    val recordatorios: List<Recordatorio> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

/** Gestiona los recordatorios sanitarios, sus dosis y las aplicaciones. */
class RecordatoriosViewModel(
    private val recordatorioRepository: RecordatorioRepository,
    private val insumoRepository: InsumoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val _estado = MutableStateFlow(RecordatoriosUiState())

    /** Estado observable de la lista de recordatorios. */
    val estado: StateFlow<RecordatoriosUiState> = _estado.asStateFlow()

    /** Carga las reglas de una camada (incluye las globales). */
    fun cargar(idCamada: Int?) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            recordatorioRepository.listar(idCamada, incluirGlobales = true)
                .onSuccess { lista ->
                    _estado.update {
                        it.copy(cargando = false, recordatorios = lista)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.message ?: "No se pudieron cargar"
                        )
                    }
                }
        }
    }

    /** Entrega insumos y categorías para el formulario. */
    fun cargarCatalogos(onListo: (List<Insumo>, List<Categoria>) -> Unit) {
        viewModelScope.launch {
            val insumos = insumoRepository.listar().getOrDefault(emptyList())
            val categorias = categoriaRepository.listar().getOrDefault(emptyList())
            onListo(insumos, categorias)
        }
    }

    /** Entrega la regla y las dosis pendientes de una fecha. */
    fun cargarParaAplicar(
        idRecordatorio: Int,
        fecha: String,
        onListo: (Recordatorio?, List<Dosis>) -> Unit
    ) {
        viewModelScope.launch {
            val regla = recordatorioRepository.obtener(idRecordatorio).getOrNull()
            if (regla == null) {
                mensajeManager.error("No se pudo cargar el recordatorio")
            }
            val dosis = recordatorioRepository
                .dosis(idRecordatorio, fecha, fecha, soloPendientes = true)
                .getOrDefault(emptyList())
            onListo(regla, dosis)
        }
    }

    /** Historial completo de dosis de una regla. */
    fun cargarHistorial(
        idRecordatorio: Int,
        onListo: (List<Dosis>) -> Unit
    ) {
        viewModelScope.launch {
            val dosis = recordatorioRepository
                .dosis(idRecordatorio)
                .getOrDefault(emptyList())
            onListo(dosis)
        }
    }

    /** Crea un recordatorio y recarga la lista de la camada. */
    fun crear(datos: DatosRecordatorio, onListo: () -> Unit) {
        viewModelScope.launch {
            recordatorioRepository.crear(
                idCamada = datos.idCamada,
                idInsumo = datos.idInsumo,
                modo = datos.modo,
                intervaloDias = datos.intervaloDias,
                diasSemana = datos.diasSemana,
                fechas = datos.fechas,
                fechaInicio = datos.fechaInicio,
                fechaFin = datos.fechaFin,
                cantidad = datos.cantidad,
                activo = datos.activo
            )
                .onSuccess {
                    mensajeManager.exito("Recordatorio creado")
                    onListo()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo crear el recordatorio"
                    )
                }
        }
    }

    /** Reemplaza un recordatorio y recalcula sus dosis futuras. */
    fun actualizar(
        idRecordatorio: Int,
        datos: DatosRecordatorio,
        onListo: () -> Unit
    ) {
        viewModelScope.launch {
            recordatorioRepository.actualizar(
                idRecordatorio = idRecordatorio,
                idCamada = datos.idCamada,
                idInsumo = datos.idInsumo,
                modo = datos.modo,
                intervaloDias = datos.intervaloDias,
                diasSemana = datos.diasSemana,
                fechas = datos.fechas,
                fechaInicio = datos.fechaInicio,
                fechaFin = datos.fechaFin,
                cantidad = datos.cantidad,
                activo = datos.activo
            )
                .onSuccess {
                    mensajeManager.exito("Recordatorio actualizado")
                    onListo()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo actualizar el recordatorio"
                    )
                }
        }
    }

    /** Desactiva un recordatorio y descarta sus dosis pendientes. */
    fun eliminar(idRecordatorio: Int, onListo: () -> Unit) {
        viewModelScope.launch {
            recordatorioRepository.eliminar(idRecordatorio)
                .onSuccess {
                    mensajeManager.exito("Recordatorio eliminado")
                    onListo()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo eliminar el recordatorio"
                    )
                }
        }
    }

    /** Aplica una o varias dosis en una sola transacción. */
    fun aplicar(
        idsDosis: List<Int>,
        cantidad: Double,
        onExito: () -> Unit
    ) {
        if (idsDosis.isEmpty()) return
        viewModelScope.launch {
            val resultado = if (idsDosis.size == 1) {
                recordatorioRepository.aplicar(idsDosis.first(), cantidad)
                    .map { listOf(it) }
            } else {
                recordatorioRepository.aplicarLote(idsDosis, cantidad)
            }
            resultado
                .onSuccess { aplicadas ->
                    val ultima = aplicadas.last()
                    mensajeManager.exito(
                        "Aplicado. Stock de ${ultima.nombreInsumo}: " +
                            textoCantidad(
                                ultima.stockResultante,
                                ultima.unidadMedida
                            )
                    )
                    onExito()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo registrar la aplicación"
                    )
                }
        }
    }
}
