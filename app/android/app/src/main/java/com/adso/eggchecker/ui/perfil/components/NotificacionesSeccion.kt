package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Perfil
import com.adso.eggchecker.domain.model.PreferenciasNotificaciones
import com.adso.eggchecker.ui.components.SelectorPill
import com.adso.eggchecker.ui.perfil.DIAS_SEMANA_CORTOS
import com.adso.eggchecker.ui.perfil.FRECUENCIAS_RESUMEN
import com.adso.eggchecker.ui.perfil.horaCorta
import com.adso.eggchecker.ui.perfil.nombreDia
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Sección de notificaciones: preferencias de alertas y ajustes de la app.
 *
 * @param perfil Perfil con las preferencias guardadas.
 * @param onGuardar Guarda las preferencias y avisa del resultado.
 */
@Composable
fun NotificacionesSeccion(
    perfil: Perfil,
    notificacionesActivas: Boolean,
    vibracion: Boolean,
    sonidoPersonalizado: String?,
    onGuardar: (
        PreferenciasNotificaciones,
        () -> Unit,
        () -> Unit
    ) -> Unit,
    onCambiarNotificaciones: (Boolean) -> Unit,
    onCambiarVibracion: (Boolean) -> Unit,
    onCambiarSonido: (String?) -> Unit,
    onReproducirSonido: () -> Unit
) {
    var preferencias by remember { mutableStateOf(preferenciasDe(perfil)) }
    var enviando by remember { mutableStateOf(false) }
    var horaAbierta by remember { mutableStateOf(false) }

    LaunchedEffect(perfil) {
        preferencias = preferenciasDe(perfil)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TarjetaPerfil {
            SubtituloSeccion("Preferencias de Alertas")
            Text(
                text = "Activa o desactiva los avisos que quieres recibir.",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FilaSwitch(
                titulo = "Producción baja",
                descripcion = "Avisa si la recolección de ayer quedó por debajo " +
                    "del 85% de las aves activas.",
                activo = preferencias.produccionBaja,
                habilitado = !enviando,
                onCambiar = {
                    preferencias = preferencias.copy(produccionBaja = it)
                }
            )
            FilaDetalle(
                etiqueta = "Hora del análisis",
                valor = preferencias.produccionHora,
                habilitado = !enviando && preferencias.produccionBaja,
                onClick = { horaAbierta = true }
            )

            FilaSwitch(
                titulo = "Camada al límite",
                descripcion = "Avisa cuando una camada supera las 72 semanas " +
                    "de vida productiva.",
                activo = preferencias.camadaLimite,
                habilitado = !enviando,
                onCambiar = {
                    preferencias = preferencias.copy(camadaLimite = it)
                }
            )

            FilaSwitch(
                titulo = "Nivel bajo de alimento",
                descripcion = "Aviso preventivo para reabastecer el inventario.",
                activo = preferencias.stockBajo,
                habilitado = !enviando,
                onCambiar = {
                    preferencias = preferencias.copy(stockBajo = it)
                }
            )

            FilaSwitch(
                titulo = "Recordatorios sanitarios",
                descripcion = "Vacunas y vitaminas: te avisa el día antes y el " +
                    "mismo día de cada aplicación.",
                activo = preferencias.vacunacion,
                habilitado = !enviando,
                onCambiar = {
                    preferencias = preferencias.copy(vacunacion = it)
                }
            )

            FilaSwitch(
                titulo = "Resumen de reportes",
                descripcion = "Un recordatorio para revisar el reporte del período.",
                activo = preferencias.resumenSemanal,
                habilitado = !enviando,
                onCambiar = {
                    preferencias = preferencias.copy(resumenSemanal = it)
                }
            )

            if (preferencias.resumenSemanal) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectorPill(
                        etiqueta = "FRECUENCIA",
                        valorTexto = (
                            FRECUENCIAS_RESUMEN.firstOrNull {
                                it.first == preferencias.resumenFrecuencia
                            } ?: FRECUENCIAS_RESUMEN.first()
                            ).second,
                        opciones = FRECUENCIAS_RESUMEN,
                        onSeleccionar = {
                            preferencias = preferencias.copy(resumenFrecuencia = it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SelectorPill(
                        etiqueta = "DÍA",
                        valorTexto = nombreDia(preferencias.resumenDia),
                        opciones = DIAS_SEMANA_CORTOS.mapIndexed { indice, nombre ->
                            indice.toString() to nombre
                        },
                        onSeleccionar = {
                            preferencias = preferencias.copy(
                                resumenDia = it.toIntOrNull() ?: 0
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "El reporte cubre 7, 14 o 30 días según la frecuencia.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            AccionesFormulario(
                textoGuardar = "Guardar Cambios",
                enviando = enviando,
                onCancelar = { preferencias = preferenciasDe(perfil) },
                onGuardar = {
                    enviando = true
                    onGuardar(
                        preferencias,
                        { enviando = false },
                        { enviando = false }
                    )
                }
            )
        }

        AjustesNotificacionApp(
            notificacionesActivas = notificacionesActivas,
            vibracion = vibracion,
            sonidoPersonalizado = sonidoPersonalizado,
            onCambiarNotificaciones = onCambiarNotificaciones,
            onCambiarVibracion = onCambiarVibracion,
            onCambiarSonido = onCambiarSonido,
            onReproducirSonido = onReproducirSonido
        )
    }

    HoraPickerModal(
        abierto = horaAbierta,
        hora = preferencias.produccionHora,
        onCerrar = { horaAbierta = false },
        onGuardar = { nuevaHora ->
            preferencias = preferencias.copy(produccionHora = nuevaHora)
            horaAbierta = false
        }
    )
}

/** Construye las preferencias editables a partir del perfil guardado. */
private fun preferenciasDe(perfil: Perfil): PreferenciasNotificaciones =
    PreferenciasNotificaciones(
        produccionBaja = perfil.notifProduccionBaja,
        camadaLimite = perfil.notifCamadaLimite,
        stockBajo = perfil.notifStockBajo,
        vacunacion = perfil.notifVacunacion,
        resumenSemanal = perfil.notifResumenSemanal,
        produccionHora = horaCorta(perfil.notifProduccionHora),
        resumenFrecuencia = perfil.notifResumenFrecuencia,
        resumenDia = perfil.notifResumenDia
    )

/** Fila con título, descripción y switch. */
@Composable
private fun FilaSwitch(
    titulo: String,
    descripcion: String,
    activo: Boolean,
    habilitado: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Dark
            )
            Text(
                text = descripcion,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        SwitchApp(
            activo = activo,
            onCambiar = onCambiar,
            habilitado = habilitado,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/** Fila con una etiqueta y un valor tocable (ej. la hora). */
@Composable
private fun FilaDetalle(
    etiqueta: String,
    valor: String,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(enabled = habilitado, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = etiqueta,
                fontSize = 13.sp,
                color = if (habilitado) TextMuted else TextMuted.copy(alpha = 0.5f)
            )
            Text(
                text = "$valor ▾",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (habilitado) Brown else Brown.copy(alpha = 0.4f)
            )
        }
    }
}
