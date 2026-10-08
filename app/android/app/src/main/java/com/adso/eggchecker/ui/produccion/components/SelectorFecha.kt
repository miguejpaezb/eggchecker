package com.adso.eggchecker.ui.produccion.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.produccion.fechaDesdeUtcMillis
import com.adso.eggchecker.ui.produccion.formatearFechaLarga
import com.adso.eggchecker.ui.produccion.hoyIso
import com.adso.eggchecker.ui.produccion.utcMillisDeFecha
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Placeholder

import java.util.Calendar

/** Selector de fecha de recolección (no permite fechas futuras). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    fecha: String,
    onCambiar: (String) -> Unit,
    habilitado: Boolean = true
) {
    var mostrar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado) { mostrar = true }
    ) {
        Text(
            text = "FECHA DE RECOLECCIÓN",
            fontSize = 10.4.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Placeholder
        )
        Text(
            text = formatearFechaLarga(fecha),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Brown,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    if (mostrar) {
        val hoyMillis = utcMillisDeFecha(hoyIso())
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisDeFecha(fecha),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= hoyMillis

                override fun isSelectableYear(year: Int): Boolean =
                    year <= anioActual
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
