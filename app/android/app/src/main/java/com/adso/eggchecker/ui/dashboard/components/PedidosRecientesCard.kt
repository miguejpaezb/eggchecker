package com.adso.eggchecker.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.PedidoReciente
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.ventas.etiquetaEstadoPedido
import com.adso.eggchecker.ui.ventas.formatearFechaPedido

private val PEDIDO_FONDO = Color(0xFFF6F9F4)
private val PEDIDO_BORDE = Color(0xFFEDF3E8)

// Alto máximo de la lista: tres tarjetas visibles, el resto se desliza.
private val ALTO_MAX_PEDIDOS = 220.dp

/** Tarjeta "Pedidos recientes": últimos pedidos con su estado. */
@Composable
fun PedidosRecientesCard(pedidos: List<PedidoReciente>) {
    TarjetaDashboard(titulo = "Pedidos recientes") {
        if (pedidos.isEmpty()) {
            Text(text = "Aún no hay pedidos registrados.", color = TextMuted)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = ALTO_MAX_PEDIDOS)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pedidos.forEach { pedido -> TarjetaPedido(pedido) }
            }
        }
    }
}

@Composable
private fun TarjetaPedido(pedido: PedidoReciente) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PEDIDO_FONDO,
        border = BorderStroke(1.dp, PEDIDO_BORDE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pedido.clienteNombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Dark
                )
                Text(
                    text = "${pedido.descripcion} | " +
                        formatearFechaPedido(pedido.fechaPedido),
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = fondoEstado(pedido.estadoPedido)
            ) {
                Text(
                    text = etiquetaEstadoPedido(pedido.estadoPedido),
                    color = textoEstado(pedido.estadoPedido),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun fondoEstado(estado: String): Color = when (estado) {
    "pendiente" -> Color(0xFFFFF9E6)
    "enviado" -> Color(0xFFEFF6FF)
    "recibido" -> Color(0xFFEEFCF2)
    else -> Color(0xFFFEF2F2)
}

private fun textoEstado(estado: String): Color = when (estado) {
    "pendiente" -> Color(0xFFD9A300)
    "enviado" -> Color(0xFF2563EB)
    "recibido" -> Color(0xFF16A34A)
    else -> Color(0xFFB91C1C)
}
