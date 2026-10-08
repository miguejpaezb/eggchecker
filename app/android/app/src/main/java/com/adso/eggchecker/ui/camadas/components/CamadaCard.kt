package com.adso.eggchecker.ui.camadas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.camadas.calcularViabilidad
import com.adso.eggchecker.ui.camadas.etiquetaEstado
import com.adso.eggchecker.ui.camadas.formatearCantidad
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

private val RETIRADA_FONDO = Color(0xFFE5E7EB)
private val AVISO_TEXTO = Color(0xFF92610A)

/**
 * Tarjeta resumen de una camada, fiel al web móvil.
 */
@Composable
fun CamadaCard(
    camada: Camada,
    onVerDetalles: (Camada) -> Unit,
    onEditar: (Camada) -> Unit,
    onMortalidad: (Camada) -> Unit,
    onAvanzarSemana: (Camada) -> Unit,
    onSeguirActiva: (Camada) -> Unit,
    onDescartar: (Camada) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(if (camada.esActiva) Green else TextMuted)
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = camada.nombreCamada,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Dark,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        if (camada.requiereDecision) {
                            Badge(
                                texto = "Requiere decisión",
                                fondo = Yellow.copy(alpha = 0.35f),
                                color = AVISO_TEXTO
                            )
                        }
                        Badge(
                            texto = etiquetaEstado(camada.estado),
                            fondo = if (camada.esActiva) {
                                Green.copy(alpha = 0.2f)
                            } else {
                                RETIRADA_FONDO
                            },
                            color = if (camada.esActiva) Green else TextMuted
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilaStat(
                        etiqueta = "Aves actuales:",
                        valor = formatearCantidad(camada.cantidadActual)
                    )
                    FilaStat(
                        etiqueta = "Edad:",
                        valor = "${camada.edadSemanas} " +
                            (if (camada.edadSemanas == 1) "Semana" else "Semanas")
                    )
                    FilaStat(
                        etiqueta = "Viabilidad:",
                        valor = String.format("%.1f%%", calcularViabilidad(
                            camada.cantidadActual,
                            camada.cantidadInicial
                        )),
                        colorValor = Green
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ver Detalles",
                        color = Brown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onVerDetalles(camada) }
                    )
                    CamadaAccionesMenu(
                        camada = camada,
                        onEditar = onEditar,
                        onMortalidad = onMortalidad,
                        onAvanzarSemana = onAvanzarSemana,
                        onSeguirActiva = onSeguirActiva,
                        onDescartar = onDescartar
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaStat(
    etiqueta: String,
    valor: String,
    colorValor: Color = Dark
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = etiqueta, color = TextMuted, fontSize = 14.sp)
        Text(
            text = valor,
            color = colorValor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Badge(texto: String, fondo: Color, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = fondo,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = texto,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
