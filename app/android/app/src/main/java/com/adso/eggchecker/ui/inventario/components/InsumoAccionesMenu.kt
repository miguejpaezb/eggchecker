package com.adso.eggchecker.ui.inventario.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/** Menú desplegable de acciones de un insumo (botón more_vert). */
@Composable
fun InsumoAccionesMenu(
    insumo: Insumo,
    onEditar: (Insumo) -> Unit,
    onAnadirStock: (Insumo) -> Unit,
    onSuspender: (Insumo) -> Unit,
    onActivar: (Insumo) -> Unit,
    onDescontinuar: (Insumo) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = "Acciones del insumo",
                tint = TextMuted
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            if (insumo.activo) {
                ItemMenu("Editar", Dark) {
                    abierto = false
                    onEditar(insumo)
                }
                ItemMenu("Añadir stock", Dark) {
                    abierto = false
                    onAnadirStock(insumo)
                }
                ItemMenu("Suspender", Dark) {
                    abierto = false
                    onSuspender(insumo)
                }
                ItemMenu("Descontinuar", ErrorRed) {
                    abierto = false
                    onDescontinuar(insumo)
                }
            } else {
                ItemMenu("Activar", Dark) {
                    abierto = false
                    onActivar(insumo)
                }
            }
        }
    }
}

@Composable
private fun ItemMenu(texto: String, color: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(texto, color = color) },
        onClick = onClick
    )
}
