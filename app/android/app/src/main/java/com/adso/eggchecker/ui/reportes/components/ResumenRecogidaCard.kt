package com.adso.eggchecker.ui.reportes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.ProduccionReporte
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.reportes.formatoNumero

/** Tarjeta "Resumen de recogida": cubetas totales, por tipo y no aptos. */
@Composable
fun ResumenRecogidaCard(
    produccion: ProduccionReporte,
    camadaNombre: String?
) {
    TarjetaReporte(titulo = "Resumen de Recogida") {
        if (!camadaNombre.isNullOrBlank()) {
            Text(
                text = camadaNombre,
                color = Brown,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = "Tus gallinas recogieron:",
            color = TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = produccion.cubetasCompletas.toString(),
                color = VERDE_TEXTO,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = "Cubetas", fontSize = 16.sp)
                Text(text = "completas", fontSize = 16.sp)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            produccion.cubetasPorTipo.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.cubetas.toString(),
                            color = Brown,
                            fontFamily = MaterialTheme.typography.titleLarge
                                .fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "Cub. ${item.nombreTipo}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(ROJO_BG, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hubo ${formatoNumero(produccion.huevosNoAptos.toDouble())} " +
                    "huevos rotos o dañados",
                color = ROJO_TEXTO,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
