package com.adso.eggchecker.ui.clientes.components

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
import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/** Menú desplegable de acciones de un cliente (botón more_vert). */
@Composable
fun ClienteAccionesMenu(
    cliente: Cliente,
    onRegistrarVenta: (Cliente) -> Unit,
    onEditar: (Cliente) -> Unit,
    onSuspender: (Cliente) -> Unit,
    onActivar: (Cliente) -> Unit,
    onEliminar: (Cliente) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = "Acciones del cliente",
                tint = TextMuted
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            if (cliente.activo) {
                ItemMenu("Registrar venta", Dark) {
                    abierto = false
                    onRegistrarVenta(cliente)
                }
            }
            ItemMenu("Editar", Dark) {
                abierto = false
                onEditar(cliente)
            }
            if (cliente.activo) {
                ItemMenu("Suspender", Dark) {
                    abierto = false
                    onSuspender(cliente)
                }
            } else {
                ItemMenu("Activar", Dark) {
                    abierto = false
                    onActivar(cliente)
                }
            }
            ItemMenu("Eliminar", ErrorRed) {
                abierto = false
                onEliminar(cliente)
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
