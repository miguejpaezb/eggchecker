package com.adso.eggchecker.ui.clientes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.ui.clientes.etiquetaEstadoCliente
import com.adso.eggchecker.ui.clientes.formatearUltimaCompra
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.formatearTelefono
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.TextMuted

private val SUSPENDIDO_FONDO = Color(0xFFE5E7EB)

/** Modal con los detalles completos de un cliente. */
@Composable
fun ClienteDetalleModal(
    abierto: Boolean,
    cliente: Cliente?,
    onCerrar: () -> Unit
) {
    ModalApp(
        abierto = abierto,
        titulo = cliente?.nombreCliente ?: "Detalle del cliente",
        onCerrar = onCerrar
    ) {
        if (cliente != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (cliente.activo) {
                    Green.copy(alpha = 0.2f)
                } else {
                    SUSPENDIDO_FONDO
                }
            ) {
                Text(
                    text = etiquetaEstadoCliente(cliente.activo),
                    color = if (cliente.activo) Green else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                FilaDetalle(
                    "Teléfono:",
                    cliente.telefono?.ifBlank { null }?.let {
                        formatearTelefono(it)
                    } ?: "Sin teléfono"
                )
                FilaDetalle(
                    "Dirección:",
                    cliente.direccion?.ifBlank { null } ?: "Sin dirección"
                )
                FilaDetalle(
                    "Última compra:",
                    formatearUltimaCompra(cliente.fechaUltimaCompra)
                )
            }
        }
    }
}

@Composable
private fun FilaDetalle(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = etiqueta, color = TextMuted, fontSize = 14.sp)
        Text(
            text = valor,
            color = Dark,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
