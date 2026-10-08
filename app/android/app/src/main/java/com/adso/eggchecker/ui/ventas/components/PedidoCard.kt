package com.adso.eggchecker.ui.ventas.components

import androidx.compose.foundation.clickable
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
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow
import com.adso.eggchecker.ui.ventas.etiquetaEstadoPedido
import com.adso.eggchecker.ui.ventas.formatearFechaPedido
import com.adso.eggchecker.ui.ventas.formatoMoneda
import com.adso.eggchecker.ui.ventas.resumenUnidades

private val ENVIADO_FONDO = Color(0xFFDBEAFE)
private val ENVIADO_TEXTO = Color(0xFF1D4ED8)
private val CANCELADO_FONDO = Color(0xFFE5E7EB)
private val PENDIENTE_TEXTO = Color(0xFF92610A)

/** Colores de fondo y texto del badge según el estado del pedido. */
internal fun coloresBadge(estado: String): Pair<Color, Color> = when (estado) {
    "pendiente" -> Yellow.copy(alpha = 0.35f) to PENDIENTE_TEXTO
    "enviado" -> ENVIADO_FONDO to ENVIADO_TEXTO
    "recibido" -> Green.copy(alpha = 0.2f) to Green
    else -> CANCELADO_FONDO to TextMuted
}

/** Tarjeta resumen de un pedido del listado. */
@Composable
fun PedidoCard(
    pedido: Pedido,
    onVerDetalles: (Pedido) -> Unit,
    acciones: @Composable () -> Unit
) {
    val (fondo, colorTexto) = coloresBadge(pedido.estadoPedido)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pedido.clienteNombre,
                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Brown
                    )
                    Text(
                        text = pedido.clienteDireccion?.ifBlank { null }
                            ?: "Sin dirección",
                        fontSize = 13.6.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = fondo
                ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = resumenUnidades(pedido),
                    fontSize = 15.sp,
                    color = Color(0xFF4B5563)
                )
                Text(
                    text = formatoMoneda(pedido.valorTotal),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Dark
                )
            }

            HorizontalDivider(color = Color(0xFFE5E7EB))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatearFechaPedido(pedido.fechaPedido),
                    fontSize = 13.6.sp,
                    color = TextMuted
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ver Detalles",
                        color = Brown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onVerDetalles(pedido) }
                    )
                    acciones()
                }
            }
        }
    }
}
