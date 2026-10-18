package com.adso.eggchecker.ui.recordatorios.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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

import com.adso.eggchecker.data.sync.SolicitudAplicarDosis
import com.adso.eggchecker.domain.model.Dosis
import com.adso.eggchecker.domain.model.Recordatorio
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.recordatorios.RecordatoriosViewModel
import com.adso.eggchecker.ui.recordatorios.fechaCorta
import com.adso.eggchecker.ui.recordatorios.formatoCantidad
import com.adso.eggchecker.ui.recordatorios.textoCantidad
import com.adso.eggchecker.ui.recordatorios.textoDosis
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Modal para registrar la aplicación de una dosis o de todas las camadas
 * de un recordatorio global. Se abre desde el botón "Ver" del aviso.
 *
 * @param solicitud Recordatorio y fecha que se deben aplicar.
 * @param onAplicado Se llama tras registrar al menos una dosis.
 */
@Composable
fun AplicarDosisModal(
    solicitud: SolicitudAplicarDosis?,
    viewModel: RecordatoriosViewModel,
    onCerrar: () -> Unit,
    onAplicado: () -> Unit
) {
    var regla by remember { mutableStateOf<Recordatorio?>(null) }
    var dosis by remember { mutableStateOf<List<Dosis>>(emptyList()) }
    var cantidad by remember { mutableStateOf("") }
    var seleccion by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmando by remember { mutableStateOf(false) }

    LaunchedEffect(solicitud) {
        if (solicitud == null) {
            regla = null
            dosis = emptyList()
            seleccion = emptySet()
            cantidad = ""
            error = null
            cargando = true
            return@LaunchedEffect
        }
        cargando = true
        viewModel.cargarParaAplicar(solicitud.idRecordatorio, solicitud.fecha) {
            cargada, pendientes ->
            regla = cargada
            dosis = pendientes
            cantidad = cargada?.let { formatoCantidad(it.cantidad) } ?: ""
            seleccion = pendientes.map { it.idDosis }.toSet()
            cargando = false
        }
    }

    val cantidadNumerica = cantidad.replace(',', '.').toDoubleOrNull()
    val total = (cantidadNumerica ?: 0.0) * seleccion.size

    ModalApp(
        abierto = solicitud != null,
        titulo = "Registrar aplicación",
        onCerrar = onCerrar
    ) {
        when {
            cargando -> Text(
                text = "Cargando…",
                color = TextMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            regla == null || dosis.isEmpty() -> {
                Text(
                    text = "No hay dosis pendientes para esta fecha.",
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    BotonApp(
                        texto = "Cerrar",
                        onClick = onCerrar,
                        tipo = TipoBoton.GHOST
                    )
                }
            }
            else -> {
                val actual = regla ?: return@ModalApp
                Text(
                    text = actual.nombreInsumo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Dark
                )
                Text(
                    text = "Programado para ${fechaCorta(solicitud?.fecha)} · " +
                        textoCantidad(actual.cantidad, actual.unidadMedida) +
                        " por camada",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text(
                    text = "Stock actual: " + textoCantidad(
                        actual.stockActual,
                        actual.unidadMedida
                    ),
                    fontSize = 13.sp,
                    color = Brown,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                CampoTexto(
                    valor = cantidad,
                    onValorChange = {
                        cantidad = it
                        error = null
                    },
                    etiqueta = "Cantidad por camada (${actual.unidadMedida})",
                    keyboardType = KeyboardType.Decimal,
                    soloDecimal = true
                )

                Text(
                    text = "Camadas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Dark,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                dosis.forEach { pendiente ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = pendiente.idDosis in seleccion,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Brown,
                                uncheckedColor = TextMuted
                            ),
                            onCheckedChange = { marcado ->
                                seleccion = if (marcado) {
                                    seleccion + pendiente.idDosis
                                } else {
                                    seleccion - pendiente.idDosis
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pendiente.nombreCamada,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Dark
                            )
                            Text(
                                text = textoDosis(
                                    pendiente.numeroDosis,
                                    dosis.size
                                ),
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Text(
                    text = "Total a descontar: " + textoCantidad(
                        total,
                        actual.unidadMedida
                    ) + " (${seleccion.size} camada(s))",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brown,
                    modifier = Modifier.padding(top = 12.dp)
                )

                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = ErrorRed,
                        fontSize = 12.sp,
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
                        texto = "Registrar aplicación",
                        onClick = {
                            when {
                                seleccion.isEmpty() -> error =
                                    "Marca al menos una camada"
                                cantidadNumerica == null ||
                                    cantidadNumerica <= 0 -> error =
                                    "Escribe una cantidad mayor que cero"
                                else -> confirmando = true
                            }
                        },
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }

    val actual = regla
    if (actual != null) {
        ConfirmDialog(
            abierto = confirmando,
            titulo = "Confirmar aplicación",
            mensaje = "Se registrará la aplicación en ${seleccion.size} " +
                "camada(s) y se descontarán " +
                textoCantidad(total, actual.unidadMedida) +
                " de ${actual.nombreInsumo}. Esta acción no se puede deshacer.",
            textoConfirmar = "Aplicar",
            onConfirmar = {
                val valor = cantidadNumerica ?: actual.cantidad
                viewModel.aplicar(seleccion.toList(), valor) {
                    confirmando = false
                    onAplicado()
                }
            },
            onCerrar = { confirmando = false }
        )
    }
}
