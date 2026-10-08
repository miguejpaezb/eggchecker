package com.adso.eggchecker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/** Tarjeta de estado para "cargando", vacío o error. */
@Composable
fun CartelEstado(
    texto: String,
    modifier: Modifier = Modifier,
    esError: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = texto,
            color = if (esError) ErrorRed else TextMuted,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp)
        )
    }
}
