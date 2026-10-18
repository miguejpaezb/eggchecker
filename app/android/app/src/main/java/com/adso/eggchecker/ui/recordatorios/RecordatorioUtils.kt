package com.adso.eggchecker.ui.recordatorios

import com.adso.eggchecker.domain.model.MODO_CADA_DIAS
import com.adso.eggchecker.domain.model.MODO_DIAS_SEMANA
import com.adso.eggchecker.domain.model.MODO_FECHAS
import com.adso.eggchecker.domain.model.Recordatorio

import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Días de la semana en el orden del backend (0 = lunes). */
val DIAS_SEMANA_RECORDATORIO = listOf(
    "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
)

/** Modos de programación con su etiqueta. */
val MODOS_RECORDATORIO = listOf(
    MODO_CADA_DIAS to "Cada N días",
    MODO_DIAS_SEMANA to "Días de la semana",
    MODO_FECHAS to "Fechas exactas"
)

/** Intervalos sugeridos para "cada N días". */
val INTERVALOS_SUGERIDOS = listOf(7, 15, 30, 60)

/** Categorías de insumo que acepta un recordatorio. */
val CATEGORIAS_RECORDATORIO = listOf(
    "medicamento" to "Medicamento",
    "vitamina" to "Vitamina"
)

/** Formatea una cantidad sin decimales innecesarios (5.0 → "5"). */
fun formatoCantidad(valor: Double): String {
    return if (valor % 1.0 == 0.0) {
        valor.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", valor).trimEnd('0').trimEnd('.')
    }
}

/** Cantidad con su unidad de medida ("5 ml"). */
fun textoCantidad(cantidad: Double, unidad: String): String =
    "${formatoCantidad(cantidad)} $unidad".trim()

/** Resume cómo se repite un recordatorio para las listas. */
fun resumenModo(recordatorio: Recordatorio): String = when (recordatorio.modo) {
    MODO_CADA_DIAS -> if (recordatorio.intervaloDias == 1) {
        "Cada día"
    } else {
        "Cada ${recordatorio.intervaloDias ?: 0} días"
    }
    MODO_DIAS_SEMANA -> recordatorio.diasSemana.joinToString(", ") { dia ->
        DIAS_SEMANA_RECORDATORIO.getOrElse(dia) { "" }
    }
    MODO_FECHAS -> if (recordatorio.fechas.size == 1) {
        "1 fecha"
    } else {
        "${recordatorio.fechas.size} fechas"
    }
    else -> recordatorio.modo
}

/** Alcance del recordatorio para las listas. */
fun textoAlcance(recordatorio: Recordatorio): String =
    if (recordatorio.esGlobal) {
        "Toda la granja"
    } else {
        recordatorio.nombreCamada ?: "Camada"
    }

/** Fecha ISO (YYYY-MM-DD) a DD/MM/AAAA. */
fun fechaCorta(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "—"
    val partes = fecha.split("-")
    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        fecha
    }
}

/** Etiqueta de la dosis: "dosis 2 de 7". */
fun textoDosis(numero: Int, total: Int): String = "dosis $numero de $total"

private val FORMATO_ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)

/** Fecha de hoy en ISO local (YYYY-MM-DD). */
fun hoyIsoRecordatorio(): String = FORMATO_ISO.format(Date())

/** Milisegundos UTC de una fecha ISO (medianoche UTC). */
fun utcMillisDeFechaRecordatorio(fecha: String): Long {
    val valor = FORMATO_ISO.parse(fecha) ?: return 0L
    val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendario.time = valor
    calendario.set(Calendar.HOUR_OF_DAY, 0)
    calendario.set(Calendar.MINUTE, 0)
    calendario.set(Calendar.SECOND, 0)
    calendario.set(Calendar.MILLISECOND, 0)
    return calendario.timeInMillis
}

/** Fecha ISO (UTC) desde milisegundos del DatePicker. */
fun fechaDesdeUtcMillisRecordatorio(millis: Long): String {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date(millis))
}

/** Resta días a una fecha ISO (YYYY-MM-DD). */
fun restarDias(fecha: String, dias: Int): String {
    val valor = FORMATO_ISO.parse(fecha) ?: Date()
    val calendario = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendario.time = valor
    calendario.add(Calendar.DAY_OF_YEAR, -dias)
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(calendario.time)
}

