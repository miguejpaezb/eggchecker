package com.adso.eggchecker.ui.reportes

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Regla de negocio RF-17: una cubeta equivale a 30 huevos. */
const val HUEVOS_POR_CUBETA = 30

/** Mínimo de días para mostrar la nota de promedio diario de comida. */
const val DIAS_MINIMOS_PROMEDIO = 4

/** Presets rápidos del rango del reporte. */
val PRESETS_REPORTE = listOf(
    "hoy" to "Hoy",
    "semana" to "Última semana",
    "mes" to "Este mes"
)

private val LOCALE_CO = Locale("es", "CO")
private val LOCALE_US = Locale.US
private val FORMATO_ISO = SimpleDateFormat("yyyy-MM-dd", LOCALE_US)

/** Fecha de hoy en formato ISO local (YYYY-MM-DD). */
fun hoyIso(): String = FORMATO_ISO.format(Date())

/** Rango desde/hasta de un preset de período. */
fun rangoPreset(preset: String): Pair<String, String> {
    val hasta = hoyIso()
    val desde = when (preset) {
        "semana" -> {
            val calendario = Calendar.getInstance()
            calendario.add(Calendar.DAY_OF_YEAR, -6)
            FORMATO_ISO.format(calendario.time)
        }
        "mes" -> {
            val calendario = Calendar.getInstance()
            calendario.set(Calendar.DAY_OF_MONTH, 1)
            FORMATO_ISO.format(calendario.time)
        }
        else -> hasta
    }
    return desde to hasta
}

/** Cuenta los días (inclusive) de un rango ISO. */
fun diasDelRango(desde: String, hasta: String): Int {
    if (desde.isBlank() || hasta.isBlank()) return 0
    return try {
        val inicio = FORMATO_ISO.parse(desde)
        val fin = FORMATO_ISO.parse(hasta)
        if (inicio == null || fin == null) {
            0
        } else {
            (((fin.time - inicio.time) / 86_400_000L) + 1).toInt()
        }
    } catch (e: Exception) {
        0
    }
}

/** Formatea un número con separadores de miles y decimales configurables. */
fun formatoNumero(valor: Double, decimales: Int = 0): String {
    val formato = NumberFormat.getNumberInstance(LOCALE_CO)
    formato.minimumFractionDigits = decimales
    formato.maximumFractionDigits = decimales
    return formato.format(valor)
}

/** Fecha ISO (YYYY-MM-DD) a formato corto DD/MM/AAAA. */
fun formatearFechaCorta(fechaISO: String?): String {
    if (fechaISO.isNullOrBlank()) return "—"
    val partes = fechaISO.split("-")
    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        "—"
    }
}

/** Milisegundos UTC de una fecha ISO (medianoche UTC). */
fun utcMillisDeFecha(fechaISO: String): Long {
    return try {
        val fecha = FORMATO_ISO.parse(fechaISO)
        if (fecha == null) {
            0L
        } else {
            val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendario.time = fecha
            calendario.set(Calendar.HOUR_OF_DAY, 0)
            calendario.set(Calendar.MINUTE, 0)
            calendario.set(Calendar.SECOND, 0)
            calendario.set(Calendar.MILLISECOND, 0)
            calendario.timeInMillis
        }
    } catch (e: Exception) {
        0L
    }
}

/** Fecha ISO (UTC) desde milisegundos del DatePicker. */
fun fechaDesdeUtcMillis(millis: Long): String {
    val formato = SimpleDateFormat("yyyy-MM-dd", LOCALE_US)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date(millis))
}
