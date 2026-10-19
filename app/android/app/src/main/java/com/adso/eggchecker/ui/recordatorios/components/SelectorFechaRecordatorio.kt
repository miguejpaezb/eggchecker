package com.adso.eggchecker.ui.recordatorios.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.recordatorios.fechaCorta
import com.adso.eggchecker.ui.recordatorios.fechaDesdeUtcMillisRecordatorio
import com.adso.eggchecker.ui.recordatorios.hoyIsoRecordatorio
import com.adso.eggchecker.ui.recordatorios.utcMillisDeFechaRecordatorio
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted

import java.util.Calendar

/**
 * Selector de fecha de un recordatorio (no permite fechas pasadas).
 *
 * @param minima Fecha ISO mínima seleccionable (por defecto, hoy).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaRecordatorio(
    etiqueta: String,
    fecha: String?,
    minima: String? = null,
    modifier: Modifier = Modifier,
    onCambiar: (String) -> Unit
) {
    var mostrar by remember { mutableStateOf(false) }
    val valorInicial = fecha ?: hoyIsoRecordatorio()

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clickable { mostrar = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = etiqueta, fontSize = 13.sp, color = TextMuted)
            Text(
                text = if (fecha == null) "Elegir ▾" else "${fechaCorta(fecha)} ▾",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Brown
            )
        }
    }

    if (mostrar) {
        val minimoMillis = utcMillisDeFechaRecordatorio(
            minima ?: hoyIsoRecordatorio()
        )
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisDeFechaRecordatorio(valorInicial),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= minimoMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year in anioActual..(anioActual + 3)
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let {
                            onCambiar(fechaDesdeUtcMillisRecordatorio(it))
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
