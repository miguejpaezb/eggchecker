package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.ui.theme.Brown

private val SWITCH_APAGADO = Color(0xFFD1D5DB)

/** Interruptor visual para las preferencias (estilo del web). */
@Composable
fun SwitchApp(
    activo: Boolean,
    onCambiar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (activo) Brown else SWITCH_APAGADO,
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = habilitado) { onCambiar(!activo) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = if (activo) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            }
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
