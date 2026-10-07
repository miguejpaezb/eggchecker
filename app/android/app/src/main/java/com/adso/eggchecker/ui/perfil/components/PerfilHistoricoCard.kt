package com.adso.eggchecker.ui.perfil.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.perfil.formatearMesAnio
import com.adso.eggchecker.ui.reportes.formatoNumero
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/** Tarjeta con el histórico de la cuenta: huevos, aves y antigüedad. */
@Composable
fun PerfilHistoricoCard(
    totalHuevos: Int,
    totalAves: Int,
    fechaRegistro: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Histórico",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Brown,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Estadistica(
                etiqueta = "Total huevos producidos",
                valor = formatoNumero(totalHuevos.toDouble())
            )
            Estadistica(
                etiqueta = "Total aves gestionadas",
                valor = formatoNumero(totalAves.toDouble())
            )
            Estadistica(
                etiqueta = "Miembro desde",
                valor = formatearMesAnio(fechaRegistro)
            )
        }
    }
}

@Composable
private fun Estadistica(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = etiqueta, fontSize = 14.sp, color = TextMuted)
        Text(text = valor, fontWeight = FontWeight.Bold, color = Dark)
    }
}
