package com.adso.eggchecker.ui.ventas.components

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
import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted

/** Menú desplegable de acciones de un pedido (botón more_vert). */
@Composable
fun PedidoAccionesMenu(
    pedido: Pedido,
    onMarcarEnviado: (Pedido) -> Unit,
    onMarcarRecibido: (Pedido) -> Unit,
    onEditar: (Pedido) -> Unit,
    onCancelar: (Pedido) -> Unit,
    onEliminar: (Pedido) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val esPendiente = pedido.estadoPedido == "pendiente"
    val esEnviado = pedido.estadoPedido == "enviado"
    val operable = esPendiente || esEnviado

    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = "Acciones del pedido",
                tint = TextMuted
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            if (!operable) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (pedido.estadoPedido == "recibido") {
                                "Pedido recibido"
                            } else {
                                "Pedido cancelado"
                            },
                            color = Placeholder
                        )
                    },
                    enabled = false,
                    onClick = {}
                )
            }
            if (esPendiente) {
                ItemMenu("Marcar en camino", Dark) {
                    abierto = false
                    onMarcarEnviado(pedido)
                }
            }
            if (operable) {
                ItemMenu("Marcar recibido", Dark) {
                    abierto = false
                    onMarcarRecibido(pedido)
                }
            }
            if (esPendiente) {
                ItemMenu("Editar", Dark) {
                    abierto = false
                    onEditar(pedido)
                }
            }
            if (operable) {
                ItemMenu("Cancelar", Dark) {
                    abierto = false
                    onCancelar(pedido)
                }
            }
            if (operable) {
                ItemMenu("Eliminar", ErrorRed) {
                    abierto = false
                    onEliminar(pedido)
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
