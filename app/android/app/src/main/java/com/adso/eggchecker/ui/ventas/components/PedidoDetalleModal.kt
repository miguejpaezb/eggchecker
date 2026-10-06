package com.adso.eggchecker.ui.ventas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.ventas.etiquetaEstadoPedido
import com.adso.eggchecker.ui.ventas.formatearFechaPedido
import com.adso.eggchecker.ui.ventas.formatoMoneda

/** Modal con el detalle completo de un pedido. */
@Composable
fun PedidoDetalleModal(
    abierto: Boolean,
    pedido: Pedido?,
    onCerrar: () -> Unit
) {
    ModalApp(
        abierto = abierto,
        titulo = "Detalle del Pedido",
        onCerrar = onCerrar
    ) {
        if (pedido == null) return@ModalApp

        val (fondo, colorTexto) = coloresBadge(pedido.estadoPedido)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pedido.clienteNombre,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Brown
            )
            Surface(shape = RoundedCornerShape(50), color = fondo) {
                Text(
                    text = etiquetaEstadoPedido(pedido.estadoPedido),
                    color = colorTexto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    )
                )
            }
        }
        Text(
            text = pedido.clienteDireccion?.ifBlank { null } ?: "Sin dirección",
            fontSize = 13.6.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Fecha: ${formatearFechaPedido(pedido.fechaPedido)}",
            fontSize = 13.6.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        HorizontalDivider(
            color = Color(0xFFE5E7EB),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pedido.detalles.forEach { detalle ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detalle.nombreTipo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Dark
                        )
                        Text(
                            text = "${detalle.cantidad} und x " +
                                formatoMoneda(detalle.precioUnitario),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = formatoMoneda(detalle.subtotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Dark
                    )
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFFE5E7EB),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                color = Brown
            )
            Text(
                text = formatoMoneda(pedido.valorTotal),
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Brown
            )
        }
    }
}
