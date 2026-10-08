package com.adso.eggchecker.ui.analisis

import android.content.ActivityNotFoundException
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.analisis.components.AnalisisDetalleModal
import com.adso.eggchecker.ui.analisis.components.Aviso
import com.adso.eggchecker.ui.analisis.components.CapturaCard
import com.adso.eggchecker.ui.analisis.components.DiagnosticoCard
import com.adso.eggchecker.ui.analisis.components.HistorialItem
import com.adso.eggchecker.ui.analisis.components.PremiumBloqueadoCard
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Pantalla del módulo Análisis IA (RF-33 a RF-36, CU-06).
 *
 * Flujo: elegir camada → tomar o elegir foto → analizar → ver diagnóstico
 * con anomalías y recomendaciones → queda en el historial de la camada.
 */
@Composable
fun AnalisisScreen(viewModel: AnalisisViewModel) {
    val estado by viewModel.estado.collectAsState()

    // Uri de la foto que la cámara está tomando (sobrevive a rotaciones).
    var uriCamara by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { guardada ->
        val uri = uriCamara
        if (guardada && uri != null) viewModel.fotoSeleccionada(uri)
        uriCamara = null
    }
    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.fotoSeleccionada(uri)
    }

    val tomarFoto = {
        val uri = viewModel.nuevaUriCamara()
        uriCamara = uri
        try {
            camaraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            // Equipo sin app de cámara: queda la opción de la galería.
            uriCamara = null
            viewModel.sinCamara()
        }
    }
    val abrirGaleria = {
        galeriaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Análisis IA",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Fotografía tus huevos y recibe un diagnóstico de calidad.",
                fontSize = 15.sp,
                color = TextMuted
            )
        }

        val estadoIA = estado.estadoIA
        when {
            estado.cargandoEstado && estadoIA == null -> item {
                CartelEstado(texto = "Cargando análisis IA…")
            }
            estado.errorEstado != null && estadoIA == null -> item {
                CartelEstado(texto = estado.errorEstado.orEmpty(), esError = true)
                Spacer(modifier = Modifier.height(12.dp))
                BotonApp(
                    texto = "Reintentar",
                    onClick = viewModel::cargar,
                    tipo = TipoBoton.AMARILLO,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            estadoIA != null && !estadoIA.disponible -> item {
                PremiumBloqueadoCard(mensaje = estadoIA.mensaje)
            }
            estadoIA != null -> {
                if (estadoIA.modoDemo) {
                    item {
                        Aviso(
                            texto = estadoIA.mensaje,
                            fondo = CALIDAD_REGULAR_BG,
                            color = CALIDAD_REGULAR_TEXTO
                        )
                    }
                }

                val resultado = estado.resultado
                item {
                    if (resultado != null) {
                        DiagnosticoCard(
                            analisis = resultado,
                            onRetroalimentar = { correcto ->
                                viewModel.retroalimentar(resultado.idAnalisis, correcto)
                            },
                            onNuevoAnalisis = viewModel::nuevoAnalisis
                        )
                    } else {
                        CapturaCard(
                            estadoIA = estadoIA,
                            camadas = estado.camadas.filter { it.esActiva },
                            camadaId = estado.camadaAnalisis,
                            vistaPrevia = estado.vistaPrevia,
                            preparandoFoto = estado.preparandoFoto,
                            analizando = estado.analizando,
                            onCamada = viewModel::cambiarCamadaAnalisis,
                            onTomarFoto = tomarFoto,
                            onGaleria = abrirGaleria,
                            onAnalizar = viewModel::analizar,
                            onCambiarFoto = viewModel::descartarFoto
                        )
                    }
                }

                item {
                    HistorialEncabezado(
                        camadas = estado.camadas.map { it.idCamada to it.nombreCamada },
                        filtro = estado.filtroHistorial,
                        onFiltro = viewModel::cambiarFiltroHistorial
                    )
                }

                when {
                    estado.cargandoHistorial && estado.historial.isEmpty() -> item {
                        CartelEstado(texto = "Cargando historial…")
                    }
                    estado.errorHistorial != null -> item {
                        CartelEstado(
                            texto = estado.errorHistorial.orEmpty(),
                            esError = true
                        )
                    }
                    estado.historial.isEmpty() -> item {
                        CartelEstado(
                            texto = if (estado.filtroHistorial == null) {
                                "Aún no tienes análisis. Toma tu primera foto."
                            } else {
                                "Esta camada aún no tiene análisis."
                            }
                        )
                    }
                    else -> items(
                        items = estado.historial,
                        key = { it.idAnalisis }
                    ) { analisis ->
                        HistorialItem(
                            analisis = analisis,
                            onClick = { viewModel.abrirDetalle(analisis) }
                        )
                    }
                }
            }
        }
    }

    AnalisisDetalleModal(
        analisis = estado.detalle,
        imagen = estado.imagenDetalle,
        cargandoImagen = estado.cargandoImagenDetalle,
        confirmarEliminar = estado.confirmarEliminar,
        eliminando = estado.eliminando,
        onCerrar = viewModel::cerrarDetalle,
        onRetroalimentar = viewModel::retroalimentar,
        onPedirEliminar = viewModel::pedirEliminar,
        onCancelarEliminar = viewModel::cancelarEliminar,
        onEliminar = viewModel::eliminarDetalle
    )
}

/** Título del historial con el filtro por camada (RF-36). */
@Composable
private fun HistorialEncabezado(
    camadas: List<Pair<Int, String>>,
    filtro: Int?,
    onFiltro: (Int?) -> Unit
) {
    val opciones = buildList {
        add("-1" to "Toda la granja")
        camadas.forEach { (id, nombre) -> add(id.toString() to nombre) }
    }
    val texto = camadas.find { it.first == filtro }?.second ?: "Toda la granja"

    Text(
        text = "Historial",
        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = Brown
    )
    Spacer(modifier = Modifier.height(8.dp))
    SelectorPill(
        etiqueta = "VER",
        valorTexto = texto,
        opciones = opciones,
        onSeleccionar = { valor -> onFiltro(valor.toIntOrNull()?.takeIf { it > 0 }) },
        modifier = Modifier.fillMaxWidth()
    )
}
