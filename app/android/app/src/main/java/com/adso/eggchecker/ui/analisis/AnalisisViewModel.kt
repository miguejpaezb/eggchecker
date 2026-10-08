package com.adso.eggchecker.ui.analisis

import android.graphics.BitmapFactory
import android.net.Uri

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.remote.ApiException
import com.adso.eggchecker.data.repository.AnalisisRepository
import com.adso.eggchecker.data.repository.CamadaRepository
import com.adso.eggchecker.data.storage.FotoAnalisisStorage
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Analisis
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.EstadoIA
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado de la pantalla de Análisis IA. */
data class AnalisisUiState(
    val estadoIA: EstadoIA? = null,
    val cargandoEstado: Boolean = true,
    val errorEstado: String? = null,
    val camadas: List<Camada> = emptyList(),
    /** Camada a la que se asociará el próximo análisis (RF-36). */
    val camadaAnalisis: Int? = null,
    /** Filtro del historial: null = toda la granja. */
    val filtroHistorial: Int? = null,
    val vistaPrevia: ImageBitmap? = null,
    val preparandoFoto: Boolean = false,
    val analizando: Boolean = false,
    val resultado: Analisis? = null,
    val historial: List<Analisis> = emptyList(),
    val cargandoHistorial: Boolean = false,
    val errorHistorial: String? = null,
    val detalle: Analisis? = null,
    val imagenDetalle: ImageBitmap? = null,
    val cargandoImagenDetalle: Boolean = false,
    val confirmarEliminar: Boolean = false,
    val eliminando: Boolean = false
)

/**
 * Gestiona el módulo Análisis IA (RF-33 a RF-36, CU-06): captura de la
 * foto, envío al servidor, diagnóstico con recomendaciones, historial por
 * camada y retroalimentación del avicultor.
 */
class AnalisisViewModel(
    private val analisisRepository: AnalisisRepository,
    private val camadaRepository: CamadaRepository,
    private val fotoStorage: FotoAnalisisStorage,
    private val refreshBus: RefreshBus,
    private val mensajeManager: MensajeManager
) : ViewModel() {

    private val _estado = MutableStateFlow(AnalisisUiState())

    /** Estado observable de la pantalla. */
    val estado: StateFlow<AnalisisUiState> = _estado.asStateFlow()

    /** Foto comprimida lista para enviar (fuera del estado para no copiarla). */
    private var fotoJpeg: ByteArray? = null

    /** Carga en curso del historial; se cancela si cambia el filtro. */
    private var historialJob: Job? = null

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
    }

    /** Carga el estado del módulo y, si está disponible, camadas e historial. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargandoEstado = true, errorEstado = null) }
            analisisRepository.estado()
                .onSuccess { estadoIA ->
                    _estado.update {
                        it.copy(estadoIA = estadoIA, cargandoEstado = false)
                    }
                    if (estadoIA.disponible) {
                        cargarCamadas()
                        cargarHistorial()
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargandoEstado = false,
                            errorEstado = error.message
                                ?: "No se pudo cargar el análisis IA"
                        )
                    }
                }
        }
    }

    /** Uri donde la cámara guardará la próxima foto. */
    fun nuevaUriCamara(): Uri = fotoStorage.crearUriCamara()

    /** Avisa que el equipo no tiene app de cámara. */
    fun sinCamara() {
        mensajeManager.error("No hay cámara disponible. Elige la foto de la galería.")
    }

    /** Procesa la foto tomada o elegida y muestra la vista previa. */
    fun fotoSeleccionada(uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(preparandoFoto = true, resultado = null) }
            fotoStorage.leerComprimida(uri)
                .onSuccess { bytes ->
                    fotoJpeg = bytes
                    val vista = decodificar(bytes)
                    _estado.update {
                        it.copy(vistaPrevia = vista, preparandoFoto = false)
                    }
                }
                .onFailure {
                    fotoJpeg = null
                    _estado.update { it.copy(preparandoFoto = false) }
                    mensajeManager.error("No se pudo leer la foto. Intenta con otra.")
                }
        }
    }

    /** Quita la foto actual para tomar otra. */
    fun descartarFoto() {
        fotoJpeg = null
        limpiarTemporales()
        _estado.update { it.copy(vistaPrevia = null, resultado = null) }
    }

    /** Elige la camada de los huevos que se van a analizar. */
    fun cambiarCamadaAnalisis(id: Int?) {
        _estado.update { it.copy(camadaAnalisis = id) }
    }

    /** Filtra el historial por camada (null = toda la granja). */
    fun cambiarFiltroHistorial(id: Int?) {
        _estado.update { it.copy(filtroHistorial = id) }
        cargarHistorial()
    }

    /** Envía la foto al servidor y muestra el diagnóstico (RF-34, RF-35). */
    fun analizar() {
        val bytes = fotoJpeg ?: run {
            mensajeManager.error("Primero toma o elige una foto de los huevos")
            return
        }
        val actual = _estado.value
        if (actual.analizando) return
        viewModelScope.launch {
            _estado.update { it.copy(analizando = true) }
            analisisRepository.analizar(bytes, actual.camadaAnalisis)
                .onSuccess { analisis ->
                    _estado.update {
                        it.copy(
                            analizando = false,
                            resultado = analisis,
                            estadoIA = it.estadoIA?.let { e ->
                                e.copy(usadosHoy = e.usadosHoy + 1)
                            }
                        )
                    }
                    fotoJpeg = null
                    limpiarTemporales()
                    mensajeManager.exito("Análisis guardado en el historial")
                    cargarHistorial()
                }
                .onFailure { error ->
                    _estado.update { it.copy(analizando = false) }
                    mensajeManager.error(
                        error.message ?: "No se pudo analizar la foto"
                    )
                    // Límite diario alcanzado: refresca el cupo mostrado.
                    if ((error as? ApiException)?.codigo == CODIGO_LIMITE_DIARIO) {
                        refrescarEstado()
                    }
                }
        }
    }

    /** Empieza un análisis nuevo después de ver un resultado. */
    fun nuevoAnalisis() {
        fotoJpeg = null
        limpiarTemporales()
        _estado.update { it.copy(vistaPrevia = null, resultado = null) }
    }

    /** Abre el detalle de un análisis del historial y carga su foto. */
    fun abrirDetalle(analisis: Analisis) {
        _estado.update {
            it.copy(
                detalle = analisis,
                imagenDetalle = null,
                cargandoImagenDetalle = analisis.tieneImagen
            )
        }
        if (!analisis.tieneImagen) return
        viewModelScope.launch {
            analisisRepository.imagen(analisis.idAnalisis)
                .onSuccess { bytes ->
                    val imagen = decodificar(bytes)
                    _estado.update {
                        if (it.detalle?.idAnalisis == analisis.idAnalisis) {
                            it.copy(imagenDetalle = imagen, cargandoImagenDetalle = false)
                        } else {
                            it
                        }
                    }
                }
                .onFailure {
                    _estado.update { it.copy(cargandoImagenDetalle = false) }
                }
        }
    }

    /** Cierra el detalle del análisis. */
    fun cerrarDetalle() {
        _estado.update {
            it.copy(detalle = null, imagenDetalle = null, confirmarEliminar = false)
        }
    }

    /** Registra si el diagnóstico fue correcto (dataset para un modelo propio). */
    fun retroalimentar(idAnalisis: Int, correcto: Boolean) {
        viewModelScope.launch {
            analisisRepository.retroalimentar(idAnalisis, correcto)
                .onSuccess { actualizado ->
                    _estado.update { estado ->
                        estado.copy(
                            resultado = estado.resultado.reemplazar(actualizado),
                            detalle = estado.detalle.reemplazar(actualizado),
                            historial = estado.historial.map {
                                if (it.idAnalisis == idAnalisis) actualizado else it
                            }
                        )
                    }
                    mensajeManager.exito("¡Gracias! Tu opinión mejora el análisis")
                }
                .onFailure {
                    mensajeManager.error(it.message ?: "No se pudo guardar tu opinión")
                }
        }
    }

    /** Pide confirmación antes de borrar el análisis abierto. */
    fun pedirEliminar() {
        _estado.update { it.copy(confirmarEliminar = true) }
    }

    /** Cancela el borrado. */
    fun cancelarEliminar() {
        _estado.update { it.copy(confirmarEliminar = false) }
    }

    /** Borra el análisis abierto en el detalle. */
    fun eliminarDetalle() {
        val id = _estado.value.detalle?.idAnalisis ?: return
        viewModelScope.launch {
            _estado.update { it.copy(eliminando = true) }
            analisisRepository.eliminar(id)
                .onSuccess {
                    _estado.update { estado ->
                        estado.copy(
                            eliminando = false,
                            confirmarEliminar = false,
                            detalle = null,
                            imagenDetalle = null,
                            resultado = estado.resultado?.takeIf { it.idAnalisis != id },
                            historial = estado.historial.filter { it.idAnalisis != id }
                        )
                    }
                    mensajeManager.exito("Análisis eliminado")
                }
                .onFailure {
                    _estado.update { it.copy(eliminando = false) }
                    mensajeManager.error(it.message ?: "No se pudo eliminar")
                }
        }
    }

    private fun cargarCamadas() {
        viewModelScope.launch {
            // Todas las camadas: las retiradas siguen teniendo historial.
            camadaRepository.listar(null)
                .onSuccess { lista -> _estado.update { it.copy(camadas = lista) } }
                .onFailure { _estado.update { it.copy(camadas = emptyList()) } }
        }
    }

    private fun cargarHistorial() {
        val filtro = _estado.value.filtroHistorial
        historialJob?.cancel()
        historialJob = viewModelScope.launch {
            _estado.update { it.copy(cargandoHistorial = true, errorHistorial = null) }
            analisisRepository.historial(filtro)
                .onSuccess { lista ->
                    _estado.update {
                        it.copy(historial = lista, cargandoHistorial = false)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargandoHistorial = false,
                            errorHistorial = error.message
                                ?: "No se pudo cargar el historial"
                        )
                    }
                }
        }
    }

    private fun refrescarEstado() {
        viewModelScope.launch {
            analisisRepository.estado().onSuccess { estadoIA ->
                _estado.update { it.copy(estadoIA = estadoIA) }
            }
        }
    }

    private fun limpiarTemporales() {
        viewModelScope.launch(Dispatchers.IO) { fotoStorage.limpiarTemporales() }
    }

    private suspend fun decodificar(bytes: ByteArray): ImageBitmap? =
        withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
}

/** Devuelve [nuevo] si es el mismo análisis; si no, deja el actual. */
private fun Analisis?.reemplazar(nuevo: Analisis): Analisis? =
    if (this?.idAnalisis == nuevo.idAnalisis) nuevo else this

/** Código HTTP con el que el servidor indica que se agotó el cupo del día. */
private const val CODIGO_LIMITE_DIARIO = 429
