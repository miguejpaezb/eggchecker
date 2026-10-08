package com.adso.eggchecker.ui.ventas

import com.adso.eggchecker.domain.model.Pedido

import java.text.NumberFormat
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

/** Estado de pedido con su etiqueta visible. */
data class EstadoPedido(val valor: String, val etiqueta: String)

/** Estados posibles de un pedido, en el orden del web. */
val ESTADOS_PEDIDO = listOf(
    EstadoPedido("pendiente", "Pendiente"),
    EstadoPedido("enviado", "En camino"),
    EstadoPedido("recibido", "Recibido"),
    EstadoPedido("cancelado", "Cancelado")
)

/** Traduce el estado técnico a su etiqueta visible. */
fun etiquetaEstadoPedido(estado: String): String =
    ESTADOS_PEDIDO.find { it.valor == estado }?.etiqueta ?: estado

/** Formatea un valor monetario en pesos colombianos (sin decimales). */
fun formatoMoneda(valor: Double): String {
    val formato = NumberFormat.getCurrencyInstance(LOCALE_CO)
    formato.maximumFractionDigits = 0
    return formato.format(valor)
}

/** Fecha ISO (YYYY-MM-DD) a formato corto DD/MM/AAAA. */
fun formatearFechaPedido(fechaISO: String?): String {
    if (fechaISO.isNullOrBlank()) return "—"
    val partes = fechaISO.split("-")
    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        "—"
    }
}

/** Describe las unidades de un pedido para la tarjeta. */
fun resumenUnidades(pedido: Pedido): String {
    val total = pedido.unidadesTotales
    val detalles = pedido.detalles
    return if (detalles.size == 1) {
        "$total und ${detalles[0].nombreTipo}"
    } else {
        "$total und"
    }
}
