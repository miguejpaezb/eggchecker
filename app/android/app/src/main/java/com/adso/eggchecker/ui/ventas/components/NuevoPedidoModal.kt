package com.adso.eggchecker.ui.ventas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.domain.model.LineaPedido
import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.domain.model.StockTipo
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.ventas.formatoMoneda

/** Cantidad ya reservada por un pedido en edición para un tipo de huevo. */
private fun cantidadReservada(pedido: Pedido?, idTipo: Int): Int =
    pedido?.detalles?.find { it.idTipo == idTipo }?.cantidad ?: 0

/** Modal para registrar o editar un pedido. */
@Composable
fun NuevoPedidoModal(
    abierto: Boolean,
    clientes: List<Cliente>,
    stockPorTipo: List<StockTipo>,
    pedido: Pedido?,
    clienteInicial: Cliente?,
    onCerrar: () -> Unit,
    onGuardar: (
        idCliente: Int,
        lineas: List<LineaPedido>,
        idPedido: Int?,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var clienteSeleccionado by remember { mutableStateOf<Cliente?>(null) }
    var cantidades by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var precios by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val esEdicion = pedido != null

    LaunchedEffect(abierto, pedido, clienteInicial, stockPorTipo, clientes) {
        if (!abierto) return@LaunchedEffect
        val cantidadesIniciales = mutableMapOf<Int, String>()
        val preciosIniciales = mutableMapOf<Int, String>()
        stockPorTipo.forEach { tipo ->
            val detalle = pedido?.detalles?.find { it.idTipo == tipo.idTipo }
            cantidadesIniciales[tipo.idTipo] =
                detalle?.cantidad?.toString() ?: ""
            preciosIniciales[tipo.idTipo] =
                detalle?.precioUnitario?.toString() ?: tipo.valorUnidad.toString()
        }
        cantidades = cantidadesIniciales
        precios = preciosIniciales
        clienteSeleccionado = if (pedido != null) {
            clientes.find { it.idCliente == pedido.idCliente }
                ?: Cliente(
                    idCliente = pedido.idCliente,
                    nombreCliente = pedido.clienteNombre,
                    telefono = null,
                    direccion = null,
                    fechaUltimaCompra = null,
                    activo = true
                )
        } else {
            clienteInicial
        }
        enviando = false
        error = null
    }

    val disponible: Map<Int, Int> = stockPorTipo.associate {
        it.idTipo to (it.cantidadActual + cantidadReservada(pedido, it.idTipo))
    }

    val total = stockPorTipo.sumOf { tipo ->
        val cantidad = cantidades[tipo.idTipo]?.toDoubleOrNull() ?: 0.0
        val precio = precios[tipo.idTipo]?.toDoubleOrNull() ?: 0.0
        cantidad * precio
    }

    ModalApp(
        abierto = abierto,
        titulo = if (esEdicion) "Editar Pedido" else "Nuevo Pedido",
        onCerrar = onCerrar
    ) {
        SelectorCliente(
            clientes = clientes,
            seleccionado = clienteSeleccionado,
            onSeleccionar = { clienteSeleccionado = it }
        )

        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            stockPorTipo.forEach { tipo ->
                val cantidad = cantidades[tipo.idTipo]?.toDoubleOrNull() ?: 0.0
                val precio = precios[tipo.idTipo]?.toDoubleOrNull() ?: 0.0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Cream,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tipo.nombreTipo,
                                fontFamily = MaterialTheme.typography.titleLarge
                                    .fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Brown
                            )
                            Text(
                                text = "Disponible: ${disponible[tipo.idTipo] ?: 0}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CampoTexto(
                                valor = cantidades[tipo.idTipo].orEmpty(),
                                onValorChange = { valor ->
                                    cantidades = cantidades +
                                        (tipo.idTipo to valor.filter(Char::isDigit))
                                },
                                etiqueta = "Cantidad",
                                marcador = "0",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            CampoTexto(
                                valor = precios[tipo.idTipo].orEmpty(),
                                onValorChange = { valor ->
                                    precios = precios + (tipo.idTipo to valor)
                                },
                                etiqueta = "Valor unidad",
                                marcador = "0",
                                keyboardType = KeyboardType.Decimal,
                                soloDecimal = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Subtotal: ${formatoMoneda(cantidad * precio)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.6.sp,
                            color = Dark,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total del pedido",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                color = Brown
            )
            Text(
                text = formatoMoneda(total),
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Brown
            )
        }

        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Cancelar",
                onClick = onCerrar,
                tipo = TipoBoton.GHOST
            )
            BotonApp(
                texto = if (esEdicion) "Guardar cambios" else "Generar venta",
                onClick = {
                    error = null
                    val cliente = clienteSeleccionado
                    if (cliente == null) {
                        error = "Selecciona un cliente"
                        return@BotonApp
                    }
                    val lineas = mutableListOf<LineaPedido>()
                    var mensajeError: String? = null
                    stockPorTipo.forEach { tipo ->
                        if (mensajeError != null) return@forEach
                        val cantidad = cantidades[tipo.idTipo]?.toIntOrNull() ?: 0
                        if (cantidad == 0) return@forEach
                        if (cantidad < 0) {
                            mensajeError =
                                "Las cantidades deben ser números enteros positivos"
                            return@forEach
                        }
                        val precio = precios[tipo.idTipo]?.toDoubleOrNull()
                        if (precio == null || precio <= 0.0) {
                            mensajeError =
                                "El precio de ${tipo.nombreTipo} debe ser mayor que cero"
                            return@forEach
                        }
                        if (cantidad > (disponible[tipo.idTipo] ?: 0)) {
                            mensajeError = "La cantidad de ${tipo.nombreTipo} " +
                                "supera el stock disponible " +
                                "(${disponible[tipo.idTipo] ?: 0})"
                            return@forEach
                        }
                        lineas.add(
                            LineaPedido(
                                idTipo = tipo.idTipo,
                                cantidad = cantidad,
                                precioUnitario = precio
                            )
                        )
                    }
                    if (mensajeError != null) {
                        error = mensajeError
                    } else if (lineas.isEmpty()) {
                        error = "Ingresa al menos una cantidad de huevos"
                    } else {
                        enviando = true
                        onGuardar(
                            cliente.idCliente,
                            lineas,
                            pedido?.idPedido
                        ) { exito ->
                            enviando = false
                            if (exito) onCerrar()
                        }
                    }
                },
                tipo = TipoBoton.PRIMARIO,
                cargando = enviando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
