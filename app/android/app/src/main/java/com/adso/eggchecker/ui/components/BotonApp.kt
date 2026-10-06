package com.adso.eggchecker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

/** Variantes visuales del botón de la app. */
enum class TipoBoton { PRIMARIO, AMARILLO, GHOST, PELIGRO }

/**
 * Botón estándar de EggChecker.
 *
 * @param texto Texto del botón.
 * @param onClick Acción al pulsar.
 * @param tipo Variante de color.
 * @param cargando Muestra un indicador y deshabilita el botón.
 * @param icono Ícono opcional a la izquierda.
 */
@Composable
fun BotonApp(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tipo: TipoBoton = TipoBoton.PRIMARIO,
    cargando: Boolean = false,
    habilitado: Boolean = true,
    icono: Painter? = null
) {
    val colores = when (tipo) {
        TipoBoton.PRIMARIO -> ButtonDefaults.buttonColors(
            containerColor = Brown,
            contentColor = Yellow
        )
        TipoBoton.AMARILLO -> ButtonDefaults.buttonColors(
            containerColor = Yellow,
            contentColor = Brown
        )
        TipoBoton.GHOST -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = TextMuted
        )
        TipoBoton.PELIGRO -> ButtonDefaults.buttonColors(
            containerColor = ErrorRed,
            contentColor = Color.White
        )
    }

    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        shape = RoundedCornerShape(12.dp),
        colors = colores,
        border = if (tipo == TipoBoton.GHOST) {
            BorderStroke(1.dp, Border)
        } else {
            null
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        when {
            cargando -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            icono != null -> {
                Icon(
                    painter = icono,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
        Text(text = texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
