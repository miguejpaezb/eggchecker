package com.adso.eggchecker.ui.analisis.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.EstadoIA
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Tarjeta para tomar o elegir la foto y lanzar el análisis (RF-33).
 *
 * @param camadas Camadas activas para asociar el análisis (RF-36).
 * @param camadaId Camada elegida (null = sin camada).
 * @param vistaPrevia Foto lista para enviar, si ya se tomó.
 */
@Composable
fun CapturaCard(
    estadoIA: EstadoIA,
    camadas: List<Camada>,
    camadaId: Int?,
    vistaPrevia: ImageBitmap?,
    preparandoFoto: Boolean,
    analizando: Boolean,
    onCamada: (Int?) -> Unit,
    onTomarFoto: () -> Unit,
    onGaleria: () -> Unit,
    onAnalizar: () -> Unit,
    onCambiarFoto: () -> Unit
) {
    val opcionesCamada = buildList {
        add("-1" to "Sin camada")
        camadas.forEach { add(it.idCamada.toString() to it.nombreCamada) }
    }
    val camadaTexto = camadas.find { it.idCamada == camadaId }?.nombreCamada
        ?: "Sin camada"
    val sinCupo = estadoIA.restantesHoy == 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nuevo análisis",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Brown
            )
            SelectorPill(
                etiqueta = "CAMADA",
                valorTexto = camadaTexto,
                opciones = opcionesCamada,
                onSeleccionar = { valor ->
                    onCamada(valor.toIntOrNull()?.takeIf { it > 0 })
                },
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Cream,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        preparandoFoto -> CircularProgressIndicator(color = Brown)
                        vistaPrevia != null -> Image(
                            bitmap = vistaPrevia,
                            contentDescription = "Foto de los huevos a analizar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                        else -> ConsejosFoto()
                    }
                }
            }

            if (vistaPrevia == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonApp(
                        texto = "Tomar foto",
                        onClick = onTomarFoto,
                        tipo = TipoBoton.PRIMARIO,
                        habilitado = !sinCupo && !preparandoFoto,
                        icono = rememberVectorPainter(Icons.Outlined.PhotoCamera),
                        modifier = Modifier.weight(1f)
                    )
                    BotonApp(
                        texto = "Galería",
                        onClick = onGaleria,
                        tipo = TipoBoton.AMARILLO,
                        habilitado = !sinCupo && !preparandoFoto,
                        icono = rememberVectorPainter(Icons.Outlined.PhotoLibrary),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                BotonApp(
                    texto = if (analizando) "Analizando…" else "Analizar con IA",
                    onClick = onAnalizar,
                    tipo = TipoBoton.PRIMARIO,
                    cargando = analizando,
                    habilitado = !sinCupo,
                    icono = rememberVectorPainter(Icons.Outlined.AutoAwesome),
                    modifier = Modifier.fillMaxWidth()
                )
                BotonApp(
                    texto = "Cambiar foto",
                    onClick = onCambiarFoto,
                    tipo = TipoBoton.GHOST,
                    habilitado = !analizando,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = if (sinCupo) {
                    "Alcanzaste el límite de ${estadoIA.limiteDiario} análisis de hoy."
                } else {
                    "Te quedan ${estadoIA.restantesHoy} de ${estadoIA.limiteDiario} " +
                        "análisis hoy."
                },
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Indicaciones para que la foto sirva al análisis. */
@Composable
private fun ConsejosFoto() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(20.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = Brown,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Cómo tomar una buena foto",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Brown
        )
        Text(
            text = "Huevos en la bandeja, de cerca y con luz natural.\n" +
                "Evita sombras y reflejos. Hasta 30 huevos por foto.",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
