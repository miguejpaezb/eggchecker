package com.adso.eggchecker.ui.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.BotonLimpiar
import com.adso.eggchecker.ui.components.CampoBusqueda
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.InputBackground
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.ventas.components.EliminarPedidoModal
import com.adso.eggchecker.ui.ventas.components.NuevoPedidoModal
import com.adso.eggchecker.ui.ventas.components.PedidoAccionesMenu
import com.adso.eggchecker.ui.ventas.components.PedidoCard
import com.adso.eggchecker.ui.ventas.components.PedidoDetalleModal

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val OPCIONES_FILTRO = listOf(
    "pendiente" to "Pendientes",
    "enviado" to "En camino",
    "recibido" to "Recibidos",
    "cancelado" to "Cancelados",
    "todos" to "Todos"
)

/** Pantalla de Ventas (modo online, fiel al web móvil). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(viewModel: VentasViewModel) {
    val estado by viewModel.estado.collectAsState()
    val clientePendiente by viewModel.clientePendiente.collectAsState()

    var nuevoAbierto by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Pedido?>(null) }
    var clienteInicial by remember { mutableStateOf<Cliente?>(null) }
    var enDetalle by remember { mutableStateOf<Pedido?>(null) }
    var aCancelar by remember { mutableStateOf<Pedido?>(null) }
    var cancelando by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Pedido?>(null) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(clientePendiente) {
        val cliente = clientePendiente ?: return@LaunchedEffect
        enEdicion = null
        clienteInicial = cliente
        nuevoAbierto = true
        viewModel.consumirPendiente()
    }

    val filtrados = remember(
        estado.pedidos,
        estado.busqueda,
        estado.fechaFiltro,
        estado.filtroEstado
    ) {
        val texto = estado.busqueda.trim().lowercase()
        estado.pedidos
            .filter {
                estado.filtroEstado == "todos" ||
                    it.estadoPedido == estado.filtroEstado
            }
            .filter {
                texto.isEmpty() ||
                    it.clienteNombre.lowercase().contains(texto) ||
                    (it.clienteDireccion ?: "").lowercase().contains(texto)
            }
            .filter {
                estado.fechaFiltro.isEmpty() ||
                    it.fechaPedido == estado.fechaFiltro
            }
    }

    val etiquetaFiltro = OPCIONES_FILTRO
        .find { it.first == estado.filtroEstado }?.second ?: "Pendientes"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ventas",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Pedidos y disponibilidad de huevos.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SuperficieTarjeta,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectorPill(
                        etiqueta = "FILTRO",
                        valorTexto = etiquetaFiltro,
                        opciones = OPCIONES_FILTRO,
                        onSeleccionar = viewModel::cambiarFiltroEstado,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Cream,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        ) {
                            Text(
                                text = "STOCK TOTAL",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = estado.stock.totalDisponible.toString(),
                                fontFamily = MaterialTheme.typography
                                    .titleLarge.fontFamily,
                                fontSize = 16.sp,
                                color = Brown
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SuperficieTarjeta,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CampoBusqueda(
                        valor = estado.busqueda,
                        onValorChange = viewModel::cambiarBusqueda,
                        marcador = "Buscar cliente..."
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CampoFecha(
                            valor = estado.fechaFiltro,
                            onClick = { mostrarDatePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        BotonLimpiar(onClick = viewModel::limpiarFiltros)
                    }
                    Row {
                        BotonApp(
                            texto = "Nuevo Pedido",
                            onClick = {
                                enEdicion = null
                                clienteInicial = null
                                nuevoAbierto = true
                            },
                            tipo = TipoBoton.PRIMARIO,
                            icono = painterResource(R.drawable.ic_add),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando pedidos…")
            }
            estado.error != null -> item {
                CartelEstado(texto = estado.error.orEmpty(), esError = true)
            }
            filtrados.isEmpty() -> item {
                CartelEstado(texto = "No hay pedidos para este filtro.")
            }
            else -> items(filtrados, key = { it.idPedido }) { pedido ->
                PedidoCard(
                    pedido = pedido,
                    onVerDetalles = { enDetalle = it }
                ) {
                    PedidoAccionesMenu(
                        pedido = pedido,
                        onMarcarEnviado = {
                            viewModel.cambiarEstado(it.idPedido, "enviado")
                        },
                        onMarcarRecibido = {
                            viewModel.cambiarEstado(it.idPedido, "recibido")
                        },
                        onEditar = {
                            clienteInicial = null
                            enEdicion = it
                            nuevoAbierto = true
                        },
                        onCancelar = { aCancelar = it },
                        onEliminar = { aEliminar = it }
                    )
                }
            }
        }
    }

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.cambiarFecha(isoDesdeMillis(millis))
                        }
                        mostrarDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    NuevoPedidoModal(
        abierto = nuevoAbierto,
        clientes = estado.clientes,
        stockPorTipo = estado.stock.porTipo,
        pedido = enEdicion,
        clienteInicial = clienteInicial,
        onCerrar = { nuevoAbierto = false },
        onGuardar = { idCliente, lineas, idPedido, onResultado ->
            if (idPedido != null) {
                viewModel.editar(idPedido, idCliente, lineas, onResultado)
            } else {
                viewModel.crear(idCliente, lineas, onResultado)
            }
        }
    )

    PedidoDetalleModal(
        abierto = enDetalle != null,
        pedido = enDetalle,
        onCerrar = { enDetalle = null }
    )

    EliminarPedidoModal(
        abierto = aEliminar != null,
        pedido = aEliminar,
        onCerrar = { aEliminar = null },
        onEliminar = { contrasena, onResultado ->
            val objetivo = aEliminar
            if (objetivo == null) {
                onResultado(false)
            } else {
                viewModel.eliminar(objetivo.idPedido, contrasena, onResultado)
            }
        }
    )

    ConfirmDialog(
        abierto = aCancelar != null,
        titulo = "Cancelar pedido",
        mensaje = "¿Cancelar el pedido de " +
            "\"${aCancelar?.clienteNombre.orEmpty()}\"? Quedará registrado " +
            "como cancelado y se repondrá el stock.",
        textoConfirmar = "Cancelar pedido",
        peligro = true,
        cargando = cancelando,
        onConfirmar = {
            val objetivo = aCancelar ?: return@ConfirmDialog
            cancelando = true
            viewModel.cancelar(objetivo.idPedido) { exito ->
                cancelando = false
                if (exito) aCancelar = null
            }
        },
        onCerrar = { aCancelar = null }
    )
}

/** Fecha ISO (YYYY-MM-DD) a partir de los milisegundos UTC del DatePicker. */
private fun isoDesdeMillis(millis: Long): String {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date(millis))
}

/** Campo de fecha clickeable que abre el selector de fecha. */
@Composable
private fun CampoFecha(
    valor: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, Border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (valor.isBlank()) {
                    "Filtrar por fecha"
                } else {
                    formatearFechaPedido(valor)
                },
                fontSize = 14.sp,
                color = if (valor.isBlank()) Placeholder else Dark
            )
        }
    }
}
