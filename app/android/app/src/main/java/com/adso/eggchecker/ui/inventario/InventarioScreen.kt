package com.adso.eggchecker.ui.inventario

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoBusqueda
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.inventario.components.AnadirStockModal
import com.adso.eggchecker.ui.inventario.components.CategoriasModal
import com.adso.eggchecker.ui.inventario.components.EditarInsumoModal
import com.adso.eggchecker.ui.inventario.components.InsumoAccionesMenu
import com.adso.eggchecker.ui.inventario.components.InsumoCard
import com.adso.eggchecker.ui.inventario.components.NuevoInsumoModal
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

private val LIMPIAR_FONDO = Color(0xFFFCFCFC)

/** Pantalla de Inventario (modo online, fiel al web móvil). */
@Composable
fun InventarioScreen(viewModel: InventarioViewModel) {
    val estado by viewModel.estado.collectAsState()

    var nuevoAbierto by remember { mutableStateOf(false) }
    var categoriasAbiertas by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Insumo?>(null) }
    var enStock by remember { mutableStateOf<Insumo?>(null) }
    var aDescontinuar by remember { mutableStateOf<Insumo?>(null) }
    var descontinuando by remember { mutableStateOf(false) }

    val filtrados = remember(
        estado.insumos,
        estado.busqueda,
        estado.filtroCategoria,
        estado.filtroStock
    ) {
        val texto = estado.busqueda.trim().lowercase()
        estado.insumos
            .filter { texto.isEmpty() || it.nombreInsumo.lowercase().contains(texto) }
            .filter {
                estado.filtroCategoria == null ||
                    it.idCategoria == estado.filtroCategoria
            }
            .filter {
                when (estado.filtroStock) {
                    "critico" -> esCritico(it)
                    "optimo" -> esOptimo(it)
                    else -> true
                }
            }
            .sortedBy { it.stockActual }
    }

    val nombreCategoria: (Int) -> String = { id ->
        estado.categorias.find { it.idCategoria == id }?.nombreCateg
            ?: "Sin categoría"
    }
    val opcionesStock = listOf(
        "" to "Todos",
        "critico" to "Crítico / Bajo",
        "optimo" to "Óptimo"
    )
    val opcionesCategoria = listOf("" to "Todas") +
        estado.categorias.map { it.idCategoria.toString() to it.nombreCateg }
    val etiquetaStock =
        opcionesStock.find { it.first == estado.filtroStock }?.second ?: "Todos"
    val etiquetaCategoria =
        estado.filtroCategoria?.let { nombreCategoria(it) } ?: "Todas"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Inventario",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Existencias de insumos y materiales.",
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
                        marcador = "Buscar insumo..."
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SelectorPill(
                            etiqueta = "CATEGORÍA",
                            valorTexto = etiquetaCategoria,
                            opciones = opcionesCategoria,
                            onSeleccionar = { valor ->
                                viewModel.cambiarFiltroCategoria(
                                    valor.ifBlank { null }?.toIntOrNull()
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectorPill(
                                etiqueta = "STOCK",
                                valorTexto = etiquetaStock,
                                opciones = opcionesStock,
                                onSeleccionar = viewModel::cambiarFiltroStock,
                                modifier = Modifier.weight(1f)
                            )
                            BotonLimpiar(onClick = viewModel::limpiarFiltros)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonApp(
                            texto = "Categorías",
                            onClick = { categoriasAbiertas = true },
                            tipo = TipoBoton.AMARILLO,
                            icono = painterResource(R.drawable.ic_add),
                            modifier = Modifier.weight(1f)
                        )
                        BotonApp(
                            texto = "Insumo",
                            onClick = { nuevoAbierto = true },
                            tipo = TipoBoton.PRIMARIO,
                            icono = painterResource(R.drawable.ic_add),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando insumos…")
            }
            estado.error != null -> item {
                CartelEstado(texto = estado.error.orEmpty(), esError = true)
            }
            filtrados.isEmpty() -> item {
                CartelEstado(texto = "No hay insumos para este filtro.")
            }
            else -> items(filtrados, key = { it.idInsumo }) { insumo ->
                InsumoCard(
                    insumo = insumo,
                    categoria = nombreCategoria(insumo.idCategoria)
                ) {
                    InsumoAccionesMenu(
                        insumo = insumo,
                        onEditar = { enEdicion = it },
                        onAnadirStock = { enStock = it },
                        onSuspender = { viewModel.suspender(it.idInsumo) },
                        onActivar = { viewModel.activar(it.idInsumo) },
                        onDescontinuar = { aDescontinuar = it }
                    )
                }
            }
        }
    }

    NuevoInsumoModal(
        abierto = nuevoAbierto,
        categorias = estado.categorias,
        onCerrar = { nuevoAbierto = false },
        onCrear = { id, nombre, unidad, stock, umbral, onResultado ->
            viewModel.crearInsumo(
                idCategoria = id,
                nombre = nombre,
                unidad = unidad,
                stock = stock,
                umbral = umbral,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    EditarInsumoModal(
        abierto = enEdicion != null,
        insumo = enEdicion,
        onCerrar = { enEdicion = null },
        onGuardar = { id, nombre, unidad, umbral, onResultado ->
            viewModel.editarInsumo(
                idInsumo = id,
                nombre = nombre,
                unidad = unidad,
                umbral = umbral,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    AnadirStockModal(
        abierto = enStock != null,
        insumo = enStock,
        onCerrar = { enStock = null },
        onAnadir = { id, cantidad, onResultado ->
            viewModel.anadirStock(
                idInsumo = id,
                cantidad = cantidad,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    CategoriasModal(
        abierto = categoriasAbiertas,
        categorias = estado.categorias,
        onCerrar = { categoriasAbiertas = false },
        onCrear = { nombre, descripcion, onResultado ->
            viewModel.crearCategoria(
                nombre = nombre,
                descripcion = descripcion,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        },
        onEditar = { id, nombre, descripcion, onResultado ->
            viewModel.editarCategoria(
                idCategoria = id,
                nombre = nombre,
                descripcion = descripcion,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        },
        onEliminar = { id, onResultado ->
            viewModel.eliminarCategoria(
                idCategoria = id,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    ConfirmDialog(
        abierto = aDescontinuar != null,
        titulo = "Descontinuar insumo",
        mensaje = "¿Descontinuar \"${aDescontinuar?.nombreInsumo.orEmpty()}\"? " +
            "No se podrá volver a usar; sus registros se conservan.",
        textoConfirmar = "Descontinuar",
        peligro = true,
        cargando = descontinuando,
        onConfirmar = {
            val objetivo = aDescontinuar ?: return@ConfirmDialog
            descontinuando = true
            viewModel.descontinuar(
                idInsumo = objetivo.idInsumo,
                onExito = {
                    descontinuando = false
                    aDescontinuar = null
                },
                onError = { descontinuando = false }
            )
        },
        onCerrar = { aDescontinuar = null }
    )
}

@Composable
private fun BotonLimpiar(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LIMPIAR_FONDO,
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Limpiar",
                color = Brown,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
