package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.TextMuted

/** Opción de alerta configurable. */
private data class OpcionAlerta(
    val titulo: String,
    val descripcion: String
)

private val OPCIONES = listOf(
    OpcionAlerta(
        titulo = "Alertas de producción baja",
        descripcion = "Te avisa si la recolección de huevos cae por debajo " +
            "del promedio."
    ),
    OpcionAlerta(
        titulo = "Nivel bajo de alimento",
        descripcion = "Aviso preventivo para reabastecer el inventario de cuido."
    ),
    OpcionAlerta(
        titulo = "Recordatorio de Vacunación",
        descripcion = "Recibe un recordatorio 3 días antes de cada plan sanitario."
    ),
    OpcionAlerta(
        titulo = "Resumen Semanal",
        descripcion = "Un reporte general de la granja enviado a tu correo " +
            "todos los domingos."
    )
)

/** Sección de notificaciones: preferencias de alertas y ajustes de la app. */
@Composable
fun NotificacionesSeccion(
    perfil: Perfil,
    notificacionesActivas: Boolean,
    vibracion: Boolean,
    sonidoPersonalizado: String?,
    onGuardar: (
        produccionBaja: Boolean,
        stockBajo: Boolean,
        vacunacion: Boolean,
        resumenSemanal: Boolean,
        onExito: () -> Unit,
        onError: () -> Unit
    ) -> Unit,
    onCambiarNotificaciones: (Boolean) -> Unit,
    onCambiarVibracion: (Boolean) -> Unit,
    onCambiarSonido: (String?) -> Unit,
    onReproducirSonido: () -> Unit
) {
    var produccionBaja by remember { mutableStateOf(perfil.notifProduccionBaja) }
    var stockBajo by remember { mutableStateOf(perfil.notifStockBajo) }
    var vacunacion by remember { mutableStateOf(perfil.notifVacunacion) }
    var resumenSemanal by remember { mutableStateOf(perfil.notifResumenSemanal) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(perfil) {
        produccionBaja = perfil.notifProduccionBaja
        stockBajo = perfil.notifStockBajo
        vacunacion = perfil.notifVacunacion
        resumenSemanal = perfil.notifResumenSemanal
    }

    val valores = listOf(
        produccionBaja to { v: Boolean -> produccionBaja = v },
        stockBajo to { v: Boolean -> stockBajo = v },
        vacunacion to { v: Boolean -> vacunacion = v },
        resumenSemanal to { v: Boolean -> resumenSemanal = v }
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TarjetaPerfil {
            SubtituloSeccion("Preferencias de Alertas")
            Text(
                text = "Activa o desactiva los avisos que quieres recibir.",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OPCIONES.forEachIndexed { indice, opcion ->
                val (activo, cambiar) = valores[indice]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opcion.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Dark
                        )
                        Text(
                            text = opcion.descripcion,
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    SwitchApp(
                        activo = activo,
                        onCambiar = cambiar,
                        habilitado = !enviando,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            AccionesFormulario(
                textoGuardar = "Guardar Cambios",
                enviando = enviando,
                onCancelar = {
                    produccionBaja = perfil.notifProduccionBaja
                    stockBajo = perfil.notifStockBajo
                    vacunacion = perfil.notifVacunacion
                    resumenSemanal = perfil.notifResumenSemanal
                },
                onGuardar = {
                    enviando = true
                    onGuardar(
                        produccionBaja,
                        stockBajo,
                        vacunacion,
                        resumenSemanal,
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
}
