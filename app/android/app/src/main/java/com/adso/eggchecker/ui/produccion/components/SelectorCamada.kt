package com.adso.eggchecker.ui.produccion.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.Placeholder

/** Selector de camada en etapa de producción. */
@Composable
fun SelectorCamada(
    camadas: List<Camada>,
    seleccionada: Camada?,
    habilitado: Boolean,
    onSeleccionar: (Camada) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = habilitado) { abierto = true }
        ) {
            Text(
                text = "CAMADA",
                fontSize = 10.4.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Placeholder
            )
            Text(
                text = seleccionada?.nombreCamada ?: "Selecciona una camada",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (seleccionada != null) Green else Placeholder,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            camadas.forEach { camada ->
                DropdownMenuItem(
                    text = { Text(camada.nombreCamada) },
                    onClick = {
                        abierto = false
                        onSeleccionar(camada)
                    }
                )
            }
        }
    }
}
