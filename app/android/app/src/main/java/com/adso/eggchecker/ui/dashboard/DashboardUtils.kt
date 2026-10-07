package com.adso.eggchecker.ui.dashboard

import com.adso.eggchecker.domain.model.MejorDia
import com.adso.eggchecker.ui.reportes.formatoNumero

import java.text.NumberFormat
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

/** Formatea un porcentaje con decimales fijos (ej. "76,43%"). */
fun formatoPorcentaje(valor: Double, decimales: Int = 2): String {
    val formato = NumberFormat.getNumberInstance(LOCALE_CO)
    formato.minimumFractionDigits = decimales
    formato.maximumFractionDigits = decimales
    return "${formato.format(valor)}%"
}

/** Describe la variación de producción frente a ayer. */
fun textoVariacion(valor: Double?): String {
    if (valor == null) return "Sin registro de ayer"
    val flecha = if (valor < 0) "↓" else "↑"
    val signo = if (valor > 0) "+" else ""
    return "$flecha $signo${formatoNumero(valor, 1)}% vs ayer"
}

/** Describe el día de mayor recolección de la semana. */
fun textoMejorDia(mejorDia: MejorDia?): String {
    if (mejorDia == null) return "Sin registros"
    return "${formatoNumero(mejorDia.totalHuevos.toDouble())} - ${mejorDia.etiqueta}"
}
