package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.perfil.horaCorta

import java.util.Locale

/**
 * Selector de la hora del análisis diario de producción.
 *
 * @param hora Hora actual en formato "HH:mm".
 * @param onGuardar Devuelve la hora elegida en formato "HH:mm".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoraPickerModal(
    abierto: Boolean,
    hora: String,
    onCerrar: () -> Unit,
    onGuardar: (String) -> Unit
) {
    val estado = rememberTimePickerState(
        initialHour = 5,
        initialMinute = 0,
        is24Hour = true
    )

    LaunchedEffect(abierto, hora) {
        if (abierto) {
            val partes = horaCorta(hora).split(":")
            estado.hour = (partes.getOrNull(0)?.toIntOrNull() ?: 5).coerceIn(0, 23)
            estado.minute = (partes.getOrNull(1)?.toIntOrNull() ?: 0)
                .coerceIn(0, 59)
        }
    }

    ModalApp(
        abierto = abierto,
        titulo = "Hora del análisis",
        onCerrar = onCerrar
    ) {
        TimePicker(state = estado)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Cancelar",
                onClick = onCerrar,
                tipo = TipoBoton.GHOST
            )
            BotonApp(
                texto = "Guardar",
                onClick = {
                    val elegida = String.format(
                        Locale.US,
                        "%02d:%02d",
                        estado.hour,
                        estado.minute
                    )
                    onGuardar(elegida)
                },
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
