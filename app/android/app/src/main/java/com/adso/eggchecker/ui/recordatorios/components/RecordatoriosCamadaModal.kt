package com.adso.eggchecker.ui.recordatorios.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.Dosis
import com.adso.eggchecker.domain.model.Recordatorio
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.ConfirmDialog
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.recordatorios.RecordatoriosViewModel
import com.adso.eggchecker.ui.recordatorios.fechaCorta
import com.adso.eggchecker.ui.recordatorios.resumenModo
import com.adso.eggchecker.ui.recordatorios.textoAlcance
import com.adso.eggchecker.ui.recordatorios.textoCantidad
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

/**
 * Modal para gestionar los recordatorios sanitarios de una camada:
 * listar, crear, editar, eliminar y ver el historial de dosis.
 *
 * @param camada Camada desde cuyo menú se abrió (nulo = cerrado).
 */
@Composable
fun RecordatoriosCamadaModal(
    camada: Camada?,
    viewModel: RecordatoriosViewModel,
    onCerrar: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    var formAbierto by remember { mutableStateOf(false) }
    var enEdicion by remember { mutableStateOf<Recordatorio?>(null) }
    var aEliminar by remember { mutableStateOf<Recordatorio?>(null) }
    var historialDe by remember { mutableStateOf<Recordatorio?>(null) }
    var historial by remember { mutableStateOf<List<Dosis>>(emptyList()) }

    LaunchedEffect(camada?.idCamada) {
        if (camada != null) {
            viewModel.cargar(camada.idCamada)
        }
    }

    LaunchedEffect(historialDe?.idRecordatorio) {
        val regla = historialDe ?: return@LaunchedEffect
        historial = emptyList()
        viewModel.cargarHistorial(regla.idRecordatorio) { historial = it }
    }

    ModalApp(
        abierto = camada != null,
        titulo = "Recordatorios sanitarios",
        onCerrar = onCerrar
    ) {
        if (camada == null) return@ModalApp
        Text(
            text = "Vacunas y vitaminas de ${camada.nombreCamada}. " +
                "Un recordatorio de toda la granja aplica a todas las camadas.",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            estado.cargando -> Text(
                text = "Cargando recordatorios…",
                color = TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            estado.error != null -> Text(
                text = estado.error.orEmpty(),
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            estado.recordatorios.isEmpty() -> Text(
                text = "No hay recordatorios programados.",
                color = TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            else -> estado.recordatorios.forEach { regla ->
                TarjetaRecordatorio(
                    regla = regla,
                    onEditar = {
                        enEdicion = regla
                        formAbierto = true
                    },
                    onEliminar = { aEliminar = regla },
                    onHistorial = { historialDe = regla }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Nuevo recordatorio",
                onClick = {
                    enEdicion = null
                    formAbierto = true
                }
            )
        }
    }

    RecordatorioFormModal(
        abierto = formAbierto,
        camada = camada,
        recordatorio = enEdicion,
        viewModel = viewModel,
        onCerrar = { formAbierto = false },
        onGuardar = { datos ->
            val actual = enEdicion
            val alTerminar: () -> Unit = {
                formAbierto = false
                enEdicion = null
                camada?.let { actualCamada ->
                    viewModel.cargar(actualCamada.idCamada)
                }
                Unit
            }
            if (actual == null) {
                viewModel.crear(datos, alTerminar)
            } else {
                viewModel.actualizar(actual.idRecordatorio, datos, alTerminar)
            }
        }
    )

    ConfirmDialog(
        abierto = aEliminar != null,
        titulo = "Eliminar recordatorio",
        mensaje = "Se desactivará el recordatorio y se descartarán las dosis " +
            "pendientes. El historial de aplicaciones se conserva.",
        textoConfirmar = "Eliminar",
        peligro = true,
        onConfirmar = {
            val regla = aEliminar ?: return@ConfirmDialog
            viewModel.eliminar(regla.idRecordatorio) {
                aEliminar = null
                camada?.let { viewModel.cargar(it.idCamada) }
            }
        },
        onCerrar = { aEliminar = null }
    )

    ModalApp(
        abierto = historialDe != null,
        titulo = "Historial de dosis",
        onCerrar = { historialDe = null }
    ) {
        val regla = historialDe ?: return@ModalApp
        Text(
            text = "${regla.nombreInsumo} · ${textoAlcance(regla)}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Dark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (historial.isEmpty()) {
            Text(
                text = "Todavía no hay dosis generadas.",
                fontSize = 13.sp,
                color = TextMuted
            )
        } else {
            historial.forEach { dosis ->
                Text(
                    text = "${fechaCorta(dosis.fechaProgramada)} · " +
                        "${dosis.nombreCamada} · " +
                        (if (dosis.aplicada) "Aplicada" else "Pendiente"),
                    fontSize = 13.sp,
                    color = if (dosis.aplicada) TextMuted else Dark,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

/** Tarjeta de una regla con sus acciones. */
@Composable
private fun TarjetaRecordatorio(
    regla: Recordatorio,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onHistorial: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SuperficieTarjeta,
        border = BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = regla.nombreInsumo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Dark
                    )
                    Text(
                        text = textoCantidad(
                            regla.cantidad,
                            regla.unidadMedida
                        ) + " · " + resumenModo(regla),
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (regla.esGlobal) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Yellow.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "Toda la granja",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Brown,
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
                }
            }
            Text(
                text = "Próxima: ${fechaCorta(regla.proximaDosis)} · " +
                    "${regla.dosisPendientes} pendiente(s)",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AccionTexto("Historial", Brown, onHistorial)
                AccionTexto("Editar", Brown, onEditar)
                AccionTexto("Eliminar", ErrorRed, onEliminar)
            }
        }
    }
}

/** Acción de texto de la tarjeta. */
@Composable
private fun AccionTexto(
    texto: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Text(
        text = texto,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}
