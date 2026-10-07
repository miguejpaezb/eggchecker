package com.adso.eggchecker.ui.camadas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.data.sync.CamadaPendienteBus
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.camadas.components.CamadaCard
import com.adso.eggchecker.ui.camadas.components.CamadaDetalleModal
import com.adso.eggchecker.ui.camadas.components.EditarCamadaModal
import com.adso.eggchecker.ui.camadas.components.MortalidadModal
import com.adso.eggchecker.ui.camadas.components.NuevaCamadaModal
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/** Pantalla de gestión de camadas (modo online, fiel al web móvil). */
@Composable
fun CamadasScreen(
    viewModel: CamadasViewModel,
    camadaPendienteBus: CamadaPendienteBus
) {
    val estado by viewModel.estado.collectAsState()

    var nuevaAbierta by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Camada?>(null) }
    var enMortalidad by remember { mutableStateOf<Camada?>(null) }
    var enDetalle by remember { mutableStateOf<Camada?>(null) }
    var aDescartar by remember { mutableStateOf<Camada?>(null) }
    var descartando by remember { mutableStateOf(false) }

    // Abre el detalle cuando se llega desde una notificación de camada.
    val camadaPendiente by camadaPendienteBus.camadaId.collectAsState()
    LaunchedEffect(camadaPendiente, estado.camadas, estado.cargando) {
        val id = camadaPendiente ?: return@LaunchedEffect
        if (estado.cargando) return@LaunchedEffect
        val encontrada = estado.camadas.find { it.idCamada == id }
        if (encontrada != null) {
            enDetalle = encontrada
            camadaPendienteBus.consumir()
        } else {
            viewModel.cargarDetalle(id) { camada, _ ->
                if (camada != null) enDetalle = camada
                camadaPendienteBus.consumir()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Gestión de Camadas",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Control de lotes y galpones activos.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FiltroEstado(
                    valor = estado.filtro,
                    onValorChange = viewModel::cambiarFiltro,
                    modifier = Modifier.weight(1f)
                )
                BotonApp(
                    texto = "Nueva Camada",
                    onClick = { nuevaAbierta = true },
                    icono = painterResource(R.drawable.ic_add),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando camadas…")
            }
            estado.error != null -> item {
                CartelEstado(texto = estado.error.orEmpty(), esError = true)
            }
            estado.camadas.isEmpty() -> item {
                CartelEstado(texto = "No hay camadas para este filtro.")
            }
            else -> items(estado.camadas, key = { it.idCamada }) { camada ->
                CamadaCard(
                    camada = camada,
                    onVerDetalles = { enDetalle = it },
                    onEditar = { enEdicion = it },
                    onMortalidad = { enMortalidad = it },
                    onAvanzarSemana = { viewModel.avanzarSemana(it.idCamada) },
                    onSeguirActiva = { viewModel.seguirActiva(it.idCamada) },
                    onDescartar = { aDescartar = it }
                )
            }
        }
    }

    NuevaCamadaModal(
        abierto = nuevaAbierta,
        onCerrar = { nuevaAbierta = false },
        onCrear = { nombre, fecha, cantidad, estadoCamada, onResultado ->
            viewModel.crear(
                nombre = nombre,
                fechaIngreso = fecha,
                cantidadInicial = cantidad,
                estado = estadoCamada,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    EditarCamadaModal(
        abierto = enEdicion != null,
        camada = enEdicion,
        onCerrar = { enEdicion = null },
        onGuardar = { id, nombre, cantidad, onResultado ->
            viewModel.editar(
                idCamada = id,
                nombre = nombre,
                cantidadInicial = cantidad,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    MortalidadModal(
        abierto = enMortalidad != null,
        camada = enMortalidad,
        onCerrar = { enMortalidad = null },
        onRegistrar = { id, cantidad, onResultado ->
            viewModel.mortalidad(
                idCamada = id,
                cantidad = cantidad,
                onExito = { onResultado(true) },
                onError = { onResultado(false) }
            )
        }
    )

    CamadaDetalleModal(
        abierto = enDetalle != null,
        camada = enDetalle,
        onCerrar = { enDetalle = null },
        onCargar = { id, onResultado ->
            viewModel.cargarDetalle(id, onResultado)
        }
    )

    ConfirmDialog(
        abierto = aDescartar != null,
        titulo = "Descartar camada",
        mensaje = "¿Descartar \"${aDescartar?.nombreCamada.orEmpty()}\"? " +
            "Esta acción no se puede revertir.",
        textoConfirmar = "Descartar",
        peligro = true,
        cargando = descartando,
        onConfirmar = {
            val objetivo = aDescartar ?: return@ConfirmDialog
            descartando = true
            viewModel.descartar(
                idCamada = objetivo.idCamada,
                onExito = {
                    descartando = false
                    aDescartar = null
                },
                onError = { descartando = false }
            )
        },
        onCerrar = { aDescartar = null }
    )
}

@Composable
private fun FiltroEstado(
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val etiqueta = when (valor) {
        "activa" -> "Activas"
        "retirada" -> "Retiradas"
        else -> "Todas"
    }
    var abierto by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SuperficieTarjeta,
        border = BorderStroke(1.dp, Border),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { abierto = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ESTADO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "$etiqueta ▾",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brown
                )
            }
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false }
            ) {
                listOf(
                    "activa" to "Activas",
                    "retirada" to "Retiradas",
                    "" to "Todas"
                ).forEach { (v, l) ->
                    DropdownMenuItem(
                        text = { Text(l) },
                        onClick = {
                            abierto = false
                            onValorChange(v)
                        }
                    )
                }
            }
        }
    }
}
