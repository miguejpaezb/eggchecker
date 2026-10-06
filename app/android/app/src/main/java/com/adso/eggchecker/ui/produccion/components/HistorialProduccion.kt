package com.adso.eggchecker.ui.produccion.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Produccion
import com.adso.eggchecker.ui.produccion.etiquetaFechaCorta
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/** Alto mínimo común para que las tarjetas del historial midan igual. */
private val ALTO_TARJETA = 190.dp

/** Columna con los registros recientes y el promedio de 7 días. */
@Composable
fun HistorialProduccion(
    registros: List<Produccion>,
    promedio: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SuperficieTarjeta,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ALTO_TARJETA)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Registros recientes",
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Brown,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (registros.isEmpty()) {
                    Text(
                        text = "Aún no hay registros para esta camada.",
                        fontSize = 16.sp,
                        color = TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        registros.forEach { registro ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = etiquetaFechaCorta(
                                        registro.fechaRecoleccion
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Dark.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = registro.totalHuevos.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Dark
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SuperficieTarjeta,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ALTO_TARJETA)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Promedio 7 días",
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Brown
                )
                Text(
                    text = promedio.toString(),
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    color = Brown,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Text(
                    text = "Huevos / día",
                    fontSize = 14.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
