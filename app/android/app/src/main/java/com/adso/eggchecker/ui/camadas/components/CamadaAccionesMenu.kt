package com.adso.eggchecker.ui.camadas.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Menú desplegable de acciones de una camada (botón more_vert).
 */
@Composable
fun CamadaAccionesMenu(
    camada: Camada,
    onEditar: (Camada) -> Unit,
    onMortalidad: (Camada) -> Unit,
    onAvanzarSemana: (Camada) -> Unit,
    onSeguirActiva: (Camada) -> Unit,
    onDescartar: (Camada) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = "Acciones de la camada",
                tint = TextMuted
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            if (camada.esActiva) {
                ItemMenu("Editar camada", Dark) {
                    abierto = false
                    onEditar(camada)
                }
                ItemMenu("Registrar mortalidad", Dark) {
                    abierto = false
                    onMortalidad(camada)
                }
                ItemMenu("Avanzar semana", Dark) {
                    abierto = false
                    onAvanzarSemana(camada)
                }
                if (camada.requiereDecision) {
                    ItemMenu("Seguir activa", Dark) {
                        abierto = false
                        onSeguirActiva(camada)
                    }
                }
                ItemMenu("Descartar camada", ErrorRed) {
                    abierto = false
                    onDescartar(camada)
                }
            } else {
                DropdownMenuItem(
                    text = { Text("Camada retirada", color = Placeholder) },
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun ItemMenu(texto: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(texto, color = color) },
        onClick = onClick
    )
}
