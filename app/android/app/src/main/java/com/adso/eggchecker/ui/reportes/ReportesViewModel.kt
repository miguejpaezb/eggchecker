package com.adso.eggchecker.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.CamadaRepository
import com.adso.eggchecker.data.repository.ReporteRepository
import com.adso.eggchecker.data.storage.PdfStorage
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.ReporteConsolidado
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de reportes. */
data class ReportesUiState(
    val camadas: List<Camada> = emptyList(),
    val preset: String = "mes",
    val desde: String = "",
    val hasta: String = "",
    val camadaId: Int? = null,
    val reporte: ReporteConsolidado? = null,
    val cargando: Boolean = true,
    val descargando: Boolean = false,
    val error: String? = null
)

/** Gestiona el reporte de rentabilidad y su descarga en PDF. */
class ReportesViewModel(
    private val reporteRepository: ReporteRepository,
    private val camadaRepository: CamadaRepository,
    private val pdfStorage: PdfStorage,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val rangoInicial = rangoPreset("mes")

    private val _estado = MutableStateFlow(
        ReportesUiState(
            desde = rangoInicial.first,
            hasta = rangoInicial.second
        )
    )

    /** Estado observable de la pantalla de reportes. */
    val estado: StateFlow<ReportesUiState> = _estado.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
    }

    /** Carga las camadas y el reporte del período actual. */
    fun cargar() {
        cargarCamadas()
        cargarReporte()
    }

    /** Cambia el preset de período y recarga el reporte. */
    fun cambiarPreset(preset: String) {
        if (preset == "personalizado") return
        val (desde, hasta) = rangoPreset(preset)
        _estado.update {
            it.copy(preset = preset, desde = desde, hasta = hasta)
        }
        cargarReporte()
    }

    /** Cambia la fecha inicial y pasa el período a personalizado. */
    fun cambiarDesde(valor: String) {
        if (valor.isBlank()) return
        _estado.update { it.copy(preset = "personalizado", desde = valor) }
        cargarReporte()
    }

    /** Cambia la fecha final y pasa el período a personalizado. */
    fun cambiarHasta(valor: String) {
        if (valor.isBlank()) return
        _estado.update { it.copy(preset = "personalizado", hasta = valor) }
        cargarReporte()
    }

    /** Acota el reporte a una camada (o a toda la granja si es null). */
    fun cambiarCamada(id: Int?) {
        _estado.update { it.copy(camadaId = id) }
        cargarReporte()
    }

    /** Genera y guarda el PDF del reporte en Descargas. */
    fun descargarPdf() {
        val actual = _estado.value
        viewModelScope.launch {
            _estado.update { it.copy(descargando = true) }
            reporteRepository.descargarPdf(
                actual.desde,
                actual.hasta,
                actual.camadaId
            )
                .onSuccess { bytes ->
                    pdfStorage.guardar(nombreArchivo(actual), bytes)
                        .onSuccess {
                            mensajeManager.exito("Reporte guardado en Descargas")
                        }
                        .onFailure {
                            mensajeManager.error("No se pudo guardar el reporte")
                        }
                }
                .onFailure {
                    mensajeManager.error(
                        it.message ?: "No se pudo generar el PDF"
                    )
                }
            _estado.update { it.copy(descargando = false) }
        }
    }

    /** Avisa que hace falta el permiso de almacenamiento para descargar. */
    fun avisarPermisoDenegado() {
        mensajeManager.error("Se necesita permiso para guardar el reporte")
    }

    private fun cargarCamadas() {
        viewModelScope.launch {
            camadaRepository.listar(null)
                .onSuccess { lista ->
                    _estado.update { it.copy(camadas = lista) }
                }
                .onFailure {
                    _estado.update { it.copy(camadas = emptyList()) }
                }
        }
    }

    private fun cargarReporte() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val actual = _estado.value
            reporteRepository.obtenerConsolidado(
                actual.desde,
                actual.hasta,
                actual.camadaId
            )
                .onSuccess { reporte ->
                    _estado.update {
                        it.copy(cargando = false, reporte = reporte)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            reporte = null,
                            error = error.message
                                ?: "No se pudo cargar el reporte"
                        )
                    }
                }
        }
    }

    private fun nombreArchivo(estado: ReportesUiState): String =
        "reporte_rentabilidad_${estado.desde}_${estado.hasta}.pdf"
}
