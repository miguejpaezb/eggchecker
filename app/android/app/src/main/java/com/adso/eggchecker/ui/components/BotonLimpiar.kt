package com.adso.eggchecker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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

import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown

private val LIMPIAR_FONDO = Color(0xFFFCFCFC)

/** Botón de "Limpiar" filtros (ancho según su contenido). */
@Composable
fun BotonLimpiar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LIMPIAR_FONDO,
        border = BorderStroke(1.dp, Border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Limpiar",
                color = Brown,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
