package com.adso.eggchecker.ui.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.DiaProduccion
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.TextMuted

// Colores del gráfico, iguales al web.
private val AMARILLO = Color(0xFFFDC33B)
private val VERDE = Color(0xFF7C8A3E)
private val REJILLA = Color(0xFFE5E7EB)

// En móvil solo caben los últimos tres días.
private const val DIAS_MOVIL = 3
private val ALTO_BARRA = 120.dp
private val ALTO_GRAFICO = 168.dp

/** Gráfica de barras con la producción de los últimos días. */
@Composable
fun ProduccionSemanalChart(
    serie: List<DiaProduccion>,
    modifier: Modifier = Modifier
) {
    val visibles = serie.takeLast(DIAS_MOVIL)
    if (visibles.isEmpty()) {
        Text(text = "Sin datos de producción.", color = TextMuted, modifier = modifier)
        return
    }

    val maximo = visibles.maxOf { it.totalHuevos }.coerceAtLeast(1)

    Box(modifier = modifier.fillMaxWidth().height(ALTO_GRAFICO)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pasos = 4
            repeat(pasos + 1) { indice ->
                val y = size.height * indice / pasos
                drawLine(
                    color = REJILLA,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            visibles.forEachIndexed { indice, dia ->
                val esUltimo = indice == visibles.lastIndex
                val fraccion = dia.totalHuevos.toFloat() / maximo
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = dia.totalHuevos.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Dark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(
                                (ALTO_BARRA * fraccion).coerceAtLeast(2.dp)
                            )
                            .clip(
                                RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp
                                )
                            )
                            .background(if (esUltimo) VERDE else AMARILLO)
                    )
                    Text(
                        text = dia.etiqueta,
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
