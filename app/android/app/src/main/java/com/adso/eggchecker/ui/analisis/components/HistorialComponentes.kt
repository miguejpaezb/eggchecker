package com.adso.eggchecker.ui.analisis.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Analisis
import com.adso.eggchecker.ui.analisis.estiloCalidad
import com.adso.eggchecker.ui.analisis.formatearFechaHora
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/** Fila del historial de análisis (RF-36). */
@Composable
fun HistorialItem(analisis: Analisis, onClick: () -> Unit) {
    val (texto, fondo, color) = estiloCalidad(analisis.calidadGeneral)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatearFechaHora(analisis.fechaAnalisis),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Dark
                    )
                    Text(
                        text = analisis.nombreCamada ?: "Sin camada",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                analisis.puntajeCalidad?.let {
                    Text(
                        text = "$it/100",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = color
                    )
                }
                Etiqueta(texto = texto, fondo = fondo, color = color)
            }
            Text(
                text = analisis.resultadoDiagnostico,
                fontSize = 13.sp,
                color = Dark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Referencia simple (no observable) al último análisis abierto. */
private class UltimoAnalisis(var valor: Analisis? = null)

/** Detalle de un análisis del historial, con su foto y opción de borrar. */
@Composable
fun AnalisisDetalleModal(
    analisis: Analisis?,
    imagen: ImageBitmap?,
    cargandoImagen: Boolean,
    confirmarEliminar: Boolean,
    eliminando: Boolean,
    onCerrar: () -> Unit,
    onRetroalimentar: (Int, Boolean) -> Unit,
    onPedirEliminar: () -> Unit,
    onCancelarEliminar: () -> Unit,
    onEliminar: () -> Unit
) {
    // Conserva el último análisis para que el contenido no desaparezca
    // mientras el modal se desvanece al cerrarse.
    val ultimo = remember { UltimoAnalisis() }
    if (analisis != null) ultimo.valor = analisis
    val mostrado = ultimo.valor

    ModalApp(
        abierto = analisis != null && !confirmarEliminar,
        titulo = "Detalle del análisis",
        onCerrar = onCerrar
    ) {
        if (mostrado != null) {
            if (mostrado.tieneImagen) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Cream,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .padding(bottom = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            imagen != null -> Image(
                                bitmap = imagen,
                                contentDescription = "Foto analizada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            cargandoImagen -> CircularProgressIndicator(color = Brown)
                            else -> Text(
                                text = "No se pudo cargar la foto",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
            DiagnosticoContenido(mostrado) { correcto ->
                onRetroalimentar(mostrado.idAnalisis, correcto)
            }
            BotonApp(
                texto = "Eliminar análisis",
                onClick = onPedirEliminar,
                tipo = TipoBoton.GHOST,
                icono = rememberVectorPainter(Icons.Outlined.Delete),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }

    ConfirmDialog(
        abierto = analisis != null && confirmarEliminar,
        titulo = "Eliminar análisis",
        mensaje = "Se borrarán el diagnóstico y la foto. Esta acción no se puede deshacer.",
        onConfirmar = onEliminar,
        onCerrar = onCancelarEliminar,
        textoConfirmar = "Eliminar",
        peligro = true,
        cargando = eliminando
    )
}

/** Aviso para usuarios del plan gratuito (CU-06: solo Premium). */
@Composable
fun PremiumBloqueadoCard(mensaje: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Brown,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Función Premium",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Brown
            )
            Text(text = mensaje, fontSize = 14.sp, color = Dark)
            Text(
                text = "Con el plan Premium puedes fotografiar tus huevos y recibir " +
                    "un diagnóstico de calidad con anomalías detectadas, " +
                    "recomendaciones para tu granja e historial por camada.\n" +
                    "Consulta los planes en Perfil › Plan.",
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp
            )
        }
    }
}
