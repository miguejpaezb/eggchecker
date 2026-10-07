package com.adso.eggchecker.ui.reportes.components

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

import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Yellow
import com.adso.eggchecker.ui.ventas.formatoMoneda

/** Tarjeta "Cuentas del período": ventas, gastos y ganancia. */
@Composable
fun CuentasCard(ventas: Double, gastos: Double, ganancia: Double) {
    TarjetaReporte(titulo = "Cuentas del período") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BloqueCuenta(
                titulo = "Ventas",
                valor = formatoMoneda(ventas),
                fondo = VERDE_BG,
                colorTexto = VERDE_TEXTO,
                modifier = Modifier.weight(1f)
            )
            BloqueCuenta(
                titulo = "Gastos",
                valor = formatoMoneda(gastos),
                fondo = ROJO_BG,
                colorTexto = ROJO_TEXTO,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(Brown, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ganancias",
                color = Yellow,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatoMoneda(ganancia),
                color = Yellow,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun BloqueCuenta(
    titulo: String,
    valor: String,
    fondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = fondo,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                color = colorTexto,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = valor,
                color = colorTexto,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
