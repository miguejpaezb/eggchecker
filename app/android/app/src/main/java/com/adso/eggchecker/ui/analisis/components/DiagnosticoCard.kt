package com.adso.eggchecker.ui.analisis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Analisis
import com.adso.eggchecker.domain.model.Anomalia
import com.adso.eggchecker.domain.model.DistribucionTipos
import com.adso.eggchecker.ui.analisis.CALIDAD_BUENA_TEXTO
import com.adso.eggchecker.ui.analisis.CALIDAD_MALA_TEXTO
import com.adso.eggchecker.ui.analisis.CALIDAD_REGULAR_BG
import com.adso.eggchecker.ui.analisis.CALIDAD_REGULAR_TEXTO
import com.adso.eggchecker.ui.analisis.capitalizar
import com.adso.eggchecker.ui.analisis.coloresGravedad
import com.adso.eggchecker.ui.analisis.estiloCalidad
import com.adso.eggchecker.ui.analisis.etiquetaAnomalia
import com.adso.eggchecker.ui.analisis.formatearFechaHora
import com.adso.eggchecker.ui.analisis.porcentaje
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

/** Tarjeta con el resultado recién obtenido (RF-34 y RF-35). */
@Composable
fun DiagnosticoCard(
    analisis: Analisis,
    onRetroalimentar: (Boolean) -> Unit,
    onNuevoAnalisis: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Resultado del análisis",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Brown,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            DiagnosticoContenido(analisis, onRetroalimentar)
            BotonApp(
                texto = "Hacer otro análisis",
                onClick = onNuevoAnalisis,
                tipo = TipoBoton.AMARILLO,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

/**
 * Contenido del diagnóstico, reutilizado en la tarjeta de resultado y en
 * el detalle del historial.
 */
@Composable
fun ColumnScope.DiagnosticoContenido(
    analisis: Analisis,
    onRetroalimentar: (Boolean) -> Unit
) {
    if (analisis.modoDemo) {
        Aviso(
            texto = "Modo demostración: resultado de ejemplo, no analiza la foto.",
            fondo = CALIDAD_REGULAR_BG,
            color = CALIDAD_REGULAR_TEXTO
        )
    }

    EncabezadoCalidad(analisis)

    Text(
        text = analisis.resultadoDiagnostico,
        fontSize = 14.sp,
        color = Dark,
        lineHeight = 20.sp,
        modifier = Modifier.padding(vertical = 12.dp)
    )

    analisis.distribucion?.takeIf { it.total > 0 }?.let { distribucion ->
        Seccion("Clasificación estimada")
        BarraDistribucion(distribucion)
    }

    if (analisis.anomalias.isNotEmpty()) {
        Seccion("Anomalías detectadas")
        analisis.anomalias.forEach { FilaAnomalia(it) }
    }

    if (analisis.recomendaciones.isNotEmpty()) {
        Seccion("Recomendaciones para tu granja")
        analisis.recomendaciones.forEachIndexed { indice, texto ->
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "${indice + 1}.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Brown,
                    modifier = Modifier.width(22.dp)
                )
                Text(
                    text = texto,
                    fontSize = 14.sp,
                    color = Dark,
                    lineHeight = 20.sp
                )
            }
        }
    }

    HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 12.dp))
    Retroalimentacion(analisis.diagnosticoCorrecto, onRetroalimentar)

    Text(
        text = buildString {
            append(formatearFechaHora(analisis.fechaAnalisis))
            analisis.nombreCamada?.let { append(" · $it") }
            append("\nDiagnóstico orientativo generado con IA")
            analisis.confianzaGeneral?.let { append(" (confianza ${porcentaje(it)})") }
            append(".")
        },
        fontSize = 12.sp,
        color = TextMuted,
        lineHeight = 16.sp,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun EncabezadoCalidad(analisis: Analisis) {
    val (texto, fondo, color) = estiloCalidad(analisis.calidadGeneral)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Etiqueta(texto = "Calidad $texto", fondo = fondo, color = color)
            analisis.aptoVenta?.let { apto ->
                Text(
                    text = if (apto) "Apto para la venta" else "No apto para la venta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (apto) CALIDAD_BUENA_TEXTO else CALIDAD_MALA_TEXTO,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            analisis.huevosDetectados?.let {
                Text(
                    text = if (it == 1) "1 huevo detectado" else "$it huevos detectados",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }
        analisis.puntajeCalidad?.let { puntaje ->
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$puntaje",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(text = "de 100", fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}

@Composable
private fun BarraDistribucion(distribucion: DistribucionTipos) {
    val segmentos = listOf(
        Triple("AA", distribucion.aa, Green),
        Triple("A", distribucion.a, Yellow),
        Triple("B", distribucion.b, Color(0xFFF59E0B)),
        Triple("No apto", distribucion.noApto, CALIDAD_MALA_TEXTO)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
    ) {
        segmentos.filter { it.second > 0 }.forEach { (_, cantidad, color) ->
            Box(
                modifier = Modifier
                    .weight(cantidad.toFloat())
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        segmentos.forEach { (nombre, cantidad, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Text(
                    text = " $nombre: $cantidad",
                    fontSize = 12.sp,
                    color = Dark
                )
            }
        }
    }
}

@Composable
private fun FilaAnomalia(anomalia: Anomalia) {
    val (fondo, color) = coloresGravedad(anomalia.gravedad)
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = etiquetaAnomalia(anomalia.tipo),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Dark,
                modifier = Modifier.weight(1f)
            )
            Etiqueta(texto = capitalizar(anomalia.gravedad), fondo = fondo, color = color)
        }
        Text(
            text = "${anomalia.descripcion} " +
                "(${anomalia.huevosAfectados} huevo" +
                (if (anomalia.huevosAfectados == 1) "" else "s") +
                " · confianza ${porcentaje(anomalia.confianza)})",
            fontSize = 13.sp,
            color = TextMuted,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun Retroalimentacion(valor: Boolean?, onRetroalimentar: (Boolean) -> Unit) {
    Text(
        text = when (valor) {
            true -> "Marcaste este diagnóstico como correcto."
            false -> "Marcaste este diagnóstico como incorrecto."
            null -> "¿El diagnóstico coincide con lo que ves?"
        },
        fontSize = 13.sp,
        color = TextMuted,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotonApp(
            texto = "Sí",
            onClick = { onRetroalimentar(true) },
            tipo = if (valor == true) TipoBoton.PRIMARIO else TipoBoton.GHOST,
            icono = rememberVectorPainter(Icons.Outlined.ThumbUp),
            modifier = Modifier.weight(1f)
        )
        BotonApp(
            texto = "No",
            onClick = { onRetroalimentar(false) },
            tipo = if (valor == false) TipoBoton.PRIMARIO else TipoBoton.GHOST,
            icono = rememberVectorPainter(Icons.Outlined.ThumbDown),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
internal fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
internal fun Aviso(texto: String, fondo: Color, color: Color) {
    Text(
        text = texto,
        fontSize = 13.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .padding(12.dp)
    )
}
