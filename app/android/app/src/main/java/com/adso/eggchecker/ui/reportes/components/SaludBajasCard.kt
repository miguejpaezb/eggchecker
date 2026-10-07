package com.adso.eggchecker.ui.reportes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Dark

/** Tarjeta "Salud y bajas": mortalidad, causa principal y vacunación. */
@Composable
fun SaludBajasCard(
    gallinasPerdidas: Int,
    causaPrincipal: String?,
    vacunacionAlDia: Boolean
) {
    TarjetaReporte(titulo = "Salud y Bajas") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gallinas Perdidas",
                color = Dark,
                fontSize = 15.sp
            )
            Text(
                text = gallinasPerdidas.toString(),
                color = ROJO_TEXTO,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp
            )
        }
        Text(
            text = "Causa principal: ${causaPrincipal ?: "Sin registro"}",
            color = ROJO_TEXTO,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VERDE_BG, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (vacunacionAlDia) {
                    "Vacunas y vitaminas al día"
                } else {
                    "Vacunas sin registro en el período"
                },
                color = VERDE_TEXTO,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
