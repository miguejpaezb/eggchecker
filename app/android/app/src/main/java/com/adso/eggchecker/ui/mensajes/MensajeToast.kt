package com.adso.eggchecker.ui.mensajes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.Green

/** Tarjeta del mensaje flotante (éxito verde / error rojo). */
@Composable
fun MensajeToast(
    mensaje: Mensaje,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val esError = mensaje.tipo == TipoMensaje.ERROR
    val fondo = if (esError) ErrorRed else Green

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = fondo,
        shadowElevation = 10.dp,
        modifier = modifier
            .widthIn(max = 320.dp)
            .clickable(onClick = onCerrar)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (esError) {
                    Icons.Filled.Error
                } else {
                    Icons.Filled.CheckCircle
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = mensaje.texto,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
