package com.adso.eggchecker.ui.reportes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.reportes.PRESETS_REPORTE
import com.adso.eggchecker.ui.reportes.fechaDesdeUtcMillis
import com.adso.eggchecker.ui.reportes.formatearFechaCorta
import com.adso.eggchecker.ui.reportes.utcMillisDeFecha
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.SuperficieTarjeta

/** Tarjeta de filtros del reporte: camada, período y descarga de PDF. */
@Composable
fun FiltroPeriodoCard(
    camadas: List<Camada>,
    camadaId: Int?,
    preset: String,
    desde: String,
    hasta: String,
    descargando: Boolean,
    onCamada: (Int?) -> Unit,
    onPreset: (String) -> Unit,
    onDesde: (String) -> Unit,
    onHasta: (String) -> Unit,
    onDescargar: () -> Unit
) {
    val opcionesCamada = buildList {
        add("-1" to "Toda la granja")
        camadas.forEach { add(it.idCamada.toString() to it.nombreCamada) }
    }
    val opcionesPreset = buildList {
        addAll(PRESETS_REPORTE)
        if (preset == "personalizado") {
            add("personalizado" to "Personalizado")
        }
    }

    val camadaTexto = if (camadaId == null) {
        "Toda la granja"
    } else {
        camadas.find { it.idCamada == camadaId }?.nombreCamada
            ?: "Toda la granja"
    }
    val presetTexto = opcionesPreset
        .find { it.first == preset }?.second ?: "Este mes"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectorCampo(
                etiqueta = "Reporte",
                valorTexto = camadaTexto,
                opciones = opcionesCamada,
                onSeleccionar = { valor ->
                    onCamada(valor.toIntOrNull()?.takeIf { it > 0 })
                }
            )
            SelectorCampo(
                etiqueta = "Período",
                valorTexto = presetTexto,
                opciones = opcionesPreset,
                onSeleccionar = onPreset
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoFechaReporte(
                    etiqueta = "Desde",
                    valor = desde,
                    onCambiar = onDesde,
                    esSeleccionable = { it <= utcMillisDeFecha(hasta) },
                    modifier = Modifier.weight(1f)
                )
                CampoFechaReporte(
                    etiqueta = "Hasta",
                    valor = hasta,
                    onCambiar = onHasta,
                    esSeleccionable = { it >= utcMillisDeFecha(desde) },
                    modifier = Modifier.weight(1f)
                )
            }
            BotonApp(
                texto = if (descargando) "Generando…" else "Descargar PDF",
                onClick = onDescargar,
                tipo = TipoBoton.PRIMARIO,
                cargando = descargando,
                icono = painterResource(R.drawable.ic_reportes),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Selector con etiqueta y valor desplegable (camada y período). */
@Composable
private fun SelectorCampo(
    etiqueta: String,
    valorTexto: String,
    opciones: List<Pair<String, String>>,
    onSeleccionar: (String) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Placeholder,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
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
                    Text(text = valorTexto, fontSize = 14.sp, color = Dark)
                    Text(text = "▾", color = Placeholder)
                }
                DropdownMenu(
                    expanded = abierto,
                    onDismissRequest = { abierto = false }
                ) {
                    opciones.forEach { (valor, texto) ->
                        DropdownMenuItem(
                            text = { Text(texto) },
                            onClick = {
                                abierto = false
                                onSeleccionar(valor)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Campo de fecha clickeable que abre el selector de fecha. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFechaReporte(
    etiqueta: String,
    valor: String,
    onCambiar: (String) -> Unit,
    esSeleccionable: (Long) -> Boolean,
    modifier: Modifier = Modifier
) {
    var mostrar by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = etiqueta.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Placeholder,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Border),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { mostrar = true }
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(
                    text = formatearFechaCorta(valor),
                    fontSize = 14.sp,
                    color = Dark
                )
            }
        }
    }

    if (mostrar) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisDeFecha(valor),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    esSeleccionable(utcTimeMillis)
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let {
                            onCambiar(fechaDesdeUtcMillis(it))
                        }
                        mostrar = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrar = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}
