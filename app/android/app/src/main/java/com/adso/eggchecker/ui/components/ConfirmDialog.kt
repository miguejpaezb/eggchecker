package com.adso.eggchecker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed

/**
 * Diálogo de confirmación para acciones destructivas.
 *
 * @param peligro Usa el botón rojo en vez del primario.
 */
@Composable
fun ConfirmDialog(
    abierto: Boolean,
    titulo: String,
    mensaje: String,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit,
    textoConfirmar: String = "Confirmar",
    peligro: Boolean = false,
    cargando: Boolean = false,
    error: String = ""
) {
    ModalApp(abierto = abierto, titulo = titulo, onCerrar = onCerrar) {
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Text(
            text = mensaje,
            color = Dark,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Cancelar",
                onClick = onCerrar,
                tipo = TipoBoton.GHOST
            )
            BotonApp(
                texto = if (cargando) "Procesando…" else textoConfirmar,
                onClick = onConfirmar,
                tipo = if (peligro) TipoBoton.PELIGRO else TipoBoton.PRIMARIO,
                cargando = cargando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
