package com.adso.eggchecker.ui.ventas

import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.domain.model.PedidoDetalle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de las utilidades del módulo Ventas. */
class VentaUtilsTest {

    private fun detalle(nombre: String, cantidad: Int) = PedidoDetalle(
        idTipo = 1,
        nombreTipo = nombre,
        cantidad = cantidad,
        precioUnitario = 500.0,
        subtotal = cantidad * 500.0
    )

    private fun pedido(unidades: Int, detalles: List<PedidoDetalle>) = Pedido(
        idPedido = 1,
        idCliente = 1,
        clienteNombre = "Cliente",
        clienteDireccion = "Cra 1",
        fechaPedido = "2026-03-01",
        estadoPedido = "pendiente",
        valorTotal = unidades * 500.0,
        unidadesTotales = unidades,
        detalles = detalles
    )

    @Test
    fun etiqueta_estado_traduce_los_estados_conocidos() {
        assertEquals("Pendiente", etiquetaEstadoPedido("pendiente"))
        assertEquals("En camino", etiquetaEstadoPedido("enviado"))
        assertEquals("Recibido", etiquetaEstadoPedido("recibido"))
        assertEquals("Cancelado", etiquetaEstadoPedido("cancelado"))
        assertEquals("otro", etiquetaEstadoPedido("otro"))
    }

    @Test
    fun formato_moneda_usa_pesos_sin_decimales() {
        val formateado = formatoMoneda(1000.0)
        assertTrue(formateado.contains("1.000"))
        assertTrue(formateado.contains("$"))
    }

    @Test
    fun formatear_fecha_pedido_controla_nulos_y_formato() {
        assertEquals("01/03/2026", formatearFechaPedido("2026-03-01"))
        assertEquals("—", formatearFechaPedido(null))
        assertEquals("—", formatearFechaPedido(""))
        assertEquals("—", formatearFechaPedido("2026-03"))
    }

    @Test
    fun resumen_unidades_indica_el_tipo_cuando_hay_una_linea() {
        val unico = pedido(30, listOf(detalle("AA", 30)))
        assertEquals("30 und AA", resumenUnidades(unico))
    }

    @Test
    fun resumen_unidades_omite_el_tipo_con_varias_lineas() {
        val mixto = pedido(50, listOf(detalle("AA", 30), detalle("A", 20)))
        assertEquals("50 und", resumenUnidades(mixto))
    }
}
