package com.adso.eggchecker.ui.camadas

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

/** Viabilidad como porcentaje de aves vivas (0-100). */
fun calcularViabilidad(cantidadActual: Int, cantidadInicial: Int): Double =
    if (cantidadInicial == 0) 0.0 else cantidadActual.toDouble() / cantidadInicial * 100.0

/** Etiqueta legible del estado de la camada. */
fun etiquetaEstado(estado: String): String = when (estado) {
    "activa" -> "Activa"
    "retirada" -> "Retirada"
    else -> estado
}

/** Cantidad con separador de miles local (es-CO). */
fun formatearCantidad(cantidad: Int): String =
    NumberFormat.getIntegerInstance(LOCALE_CO).format(cantidad)

/** Fecha ISO (YYYY-MM-DD) a texto largo en español; '—' si no es válida. */
fun formatearFecha(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "—"
    return try {
        val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fecha)
        if (entrada == null) {
            "—"
        } else {
            SimpleDateFormat("dd 'de' MMMM 'de' yyyy", LOCALE_CO).format(entrada)
        }
    } catch (e: Exception) {
        "—"
    }
}

/** Fecha de hoy en formato ISO local. */
fun fechaHoyIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** Fecha de ayer en formato ISO local. */
fun fechaAyerIso(): String {
    val calendario = Calendar.getInstance()
    calendario.add(Calendar.DAY_OF_YEAR, -1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendario.time)
}
