package com.adso.eggchecker.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.SemanaDashboard
import com.adso.eggchecker.ui.dashboard.formatoPorcentaje
import com.adso.eggchecker.ui.dashboard.textoMejorDia
import com.adso.eggchecker.ui.reportes.formatoNumero
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.ventas.formatoMoneda

private val ROJO = Color(0xFFE11D48)
private val ETIQUETA = Color(0xFF2C2418)

/** Tarjeta "Resumen semanal": totales, valor, postura y mortalidad. */
@Composable
fun ResumenSemanalCard(semana: SemanaDashboard) {
    TarjetaDashboard(titulo = "Resumen semanal") {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            FilaResumen(
                etiqueta = "Total huevos (7d):",
                valor = formatoNumero(semana.totalHuevos.toDouble())
            )
            FilaResumen(
                etiqueta = "Mejor día:",
                valor = textoMejorDia(semana.mejorDia),
                colorValor = Green
            )
            FilaResumen(
                etiqueta = "Valor producido:",
                valor = formatoMoneda(semana.valorProducido)
            )
            FilaResumen(
                etiqueta = "Tasa postura:",
                valor = formatoPorcentaje(semana.tasaPostura),
                colorValor = Green
            )
            FilaResumen(
                etiqueta = "Mortalidad:",
                valor = "${semana.mortalidad} " +
                    if (semana.mortalidad == 1) "ave" else "aves",
                colorValor = ROJO
            )
        }
    }
}

@Composable
private fun FilaResumen(
    etiqueta: String,
    valor: String,
    colorValor: Color = Dark
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ETIQUETA.copy(alpha = 0.8f)
        )
        Text(
            text = valor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = colorValor
        )
    }
}
