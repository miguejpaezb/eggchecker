package com.adso.eggchecker.ui.clientes

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.adso.eggchecker.ui.clientes.components.ClienteAccionesMenu
import com.adso.eggchecker.ui.clientes.components.ClienteCard
import com.adso.eggchecker.ui.clientes.components.ClienteDetalleModal
import com.adso.eggchecker.ui.clientes.components.EditarClienteModal
import com.adso.eggchecker.ui.clientes.components.EliminarClienteModal
import com.adso.eggchecker.ui.clientes.components.NuevoClienteModal
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.BotonLimpiar
import com.adso.eggchecker.ui.components.CampoBusqueda
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

private val OPCIONES_ESTADO = listOf(
    "activos" to "Activos",
    "suspendidos" to "Suspendidos",
    "todos" to "Todos"
)

/** Pantalla de Clientes (modo online, fiel al web móvil). */
@Composable
fun ClientesScreen(
    viewModel: ClientesViewModel,
    onRegistrarVenta: (Cliente) -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    var nuevoAbierto by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Cliente?>(null) }
    var enDetalle by remember { mutableStateOf<Cliente?>(null) }
    var aSuspender by remember { mutableStateOf<Cliente?>(null) }
    var suspendiendo by remember { mutableStateOf(false) }
    var aEliminar by remember { mutableStateOf<Cliente?>(null) }

    val filtrados = remember(
        estado.clientes,
        estado.busqueda,
        estado.filtroEstado
    ) {
        val texto = estado.busqueda.trim().lowercase()
        estado.clientes
            .filter { cliente ->
                texto.isEmpty() ||
                    cliente.nombreCliente.lowercase().contains(texto) ||
                    (cliente.direccion ?: "").lowercase().contains(texto)
            }
            .filter { cliente ->
                when (estado.filtroEstado) {
                    "activos" -> cliente.activo
                    "suspendidos" -> !cliente.activo
                    else -> true
                }
            }
            .sortedBy { it.nombreCliente.lowercase() }
    }

    val etiquetaEstado = OPCIONES_ESTADO
        .find { it.first == estado.filtroEstado }?.second ?: "Activos"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Clientes",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Compradores de tus productos avícolas.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SuperficieTarjeta,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        SelectorPill(
                            etiqueta = "MOSTRAR",
                            valorTexto = etiquetaEstado,
                            opciones = OPCIONES_ESTADO,
                            onSeleccionar = viewModel::cambiarFiltroEstado,
                            modifier = Modifier.weight(1f)
                        )
                        BotonLimpiar(onClick = viewModel::limpiarFiltros)
                    }
                    Row {
                        BotonApp(
                            texto = "Añadir Cliente",
                            onClick = { nuevoAbierto = true },
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
                CartelEstado(texto = "Cargando clientes…")
            }
            estado.error != null -> item {
                CartelEstado(texto = estado.error.orEmpty(), esError = true)
            }
            filtrados.isEmpty() -> item {
                CartelEstado(texto = "No hay clientes para este filtro.")
            }
            else -> items(filtrados, key = { it.idCliente }) { cliente ->
                ClienteCard(
                    cliente = cliente,
                    onVerDetalles = { enDetalle = it }
                ) {
                    ClienteAccionesMenu(
                        cliente = cliente,
                        onRegistrarVenta = onRegistrarVenta,
                        onEditar = { enEdicion = it },
                        onSuspender = { aSuspender = it },
                        onActivar = { viewModel.activar(it.idCliente) },
                        onEliminar = { aEliminar = it }
                    )
                }
            }
        }
    }

    NuevoClienteModal(
        abierto = nuevoAbierto,
        onCerrar = { nuevoAbierto = false },
        onCrear = { nombre, telefono, direccion, onResultado ->
            viewModel.crearCliente(
                nombre = nombre,
                telefono = telefono,
                direccion = direccion,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    EditarClienteModal(
        abierto = enEdicion != null,
        cliente = enEdicion,
        onCerrar = { enEdicion = null },
        onGuardar = { id, nombre, telefono, direccion, onResultado ->
            viewModel.editarCliente(
                idCliente = id,
                nombre = nombre,
                telefono = telefono,
                direccion = direccion,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    ClienteDetalleModal(
        abierto = enDetalle != null,
        cliente = enDetalle,
        onCerrar = { enDetalle = null }
    )

    EliminarClienteModal(
        abierto = aEliminar != null,
        cliente = aEliminar,
        onCerrar = { aEliminar = null },
        onEliminar = { contrasena, onResultado ->
            val objetivo = aEliminar
            if (objetivo == null) {
                onResultado(false)
            } else {
                viewModel.eliminar(
                    idCliente = objetivo.idCliente,
                    contrasena = contrasena,
                    onExito = { onResultado(true) },
                    onError = { onResultado(false) }
                )
            }
        }
    )

    ConfirmDialog(
        abierto = aSuspender != null,
        titulo = "Suspender cliente",
        mensaje = "¿Suspender \"${aSuspender?.nombreCliente.orEmpty()}\"? " +
            "Podrás reactivarlo cuando quieras.",
        textoConfirmar = "Suspender",
        cargando = suspendiendo,
        onConfirmar = {
            val objetivo = aSuspender ?: return@ConfirmDialog
            suspendiendo = true
            viewModel.suspender(
                idCliente = objetivo.idCliente,
                onExito = {
                    suspendiendo = false
                    aSuspender = null
                },
                onError = { suspendiendo = false }
            )
        },
        onCerrar = { aSuspender = null }
    )
}
