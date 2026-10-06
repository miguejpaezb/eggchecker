package com.adso.eggchecker.ui.camadas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.camadas.calcularViabilidad
import com.adso.eggchecker.ui.camadas.etiquetaEstado
import com.adso.eggchecker.ui.camadas.formatearCantidad
import com.adso.eggchecker.ui.camadas.formatearFecha
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Modal con el detalle completo de una camada.
 * Consulta el detalle al servidor para obtener edad en días y retiro estimado.
 */
@Composable
fun CamadaDetalleModal(
    abierto: Boolean,
    camada: Camada?,
    onCerrar: () -> Unit,
    onCargar: (idCamada: Int, onResultado: (Camada?, String?) -> Unit) -> Unit
) {
    var detalle by remember { mutableStateOf<Camada?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto, camada) {
        if (abierto && camada != null) {
            cargando = true
            error = null
            detalle = null
            onCargar(camada.idCamada) { resultado, mensaje ->
                cargando = false
                if (resultado != null) detalle = resultado else error = mensaje
            }
        }
    }

    ModalApp(
        abierto = abierto,
        titulo = camada?.nombreCamada ?: "Detalle de la camada",
        onCerrar = onCerrar
    ) {
        when {
            cargando -> Text(text = "Cargando…", color = TextMuted)
            error != null -> Text(text = error.orEmpty(), color = ErrorRed, fontSize = 13.sp)
            detalle != null -> {
                val d = detalle!!
                FilaDetalle("Fecha de ingreso:", formatearFecha(d.fechaIngreso))
                FilaDetalle("Aves iniciales:", formatearCantidad(d.cantidadInicial))
                FilaDetalle("Aves actuales:", formatearCantidad(d.cantidadActual))
                FilaDetalle(
                    "Edad:",
                    "${d.edadSemanas} semanas (${d.edadDias ?: 0} días)"
                )
                FilaDetalle(
                    "Viabilidad:",
                    String.format(
                        "%.1f%%",
                        calcularViabilidad(d.cantidadActual, d.cantidadInicial)
                    ),
                    colorValor = Green
                )
                FilaDetalle(
                    "Retiro estimado:",
                    formatearFecha(d.fechaRetiroEstimada)
                )
                FilaDetalle("Estado:", etiquetaEstado(d.estado))
                if (d.requiereDecision) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Esta camada superó las 72 semanas de vida " +
                                "productiva. Decide si descartarla o seguir activa.",
                            color = Dark,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDetalle(
    etiqueta: String,
    valor: String,
    colorValor: androidx.compose.ui.graphics.Color = Dark
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
