package com.adso.eggchecker.ui.reportes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta

internal val VERDE_BG = Color(0xFFF0FDF4)
internal val VERDE_TEXTO = Color(0xFF15803D)
internal val ROJO_BG = Color(0xFFFEF2F2)
internal val ROJO_TEXTO = Color(0xFFB91C1C)
internal val NOTA_BG = Color(0xFFDADADA)
internal val NOTA_TEXTO = Color(0xFF87827A)

/** Tarjeta base de los reportes con su título. */
@Composable
internal fun TarjetaReporte(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = titulo,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Brown,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            contenido()
        }
    }
}
