package com.adso.eggchecker.ui.perfil

// Nombres cortos de los meses para el formato "Ene 2026".
private val MESES = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

/** Formatea una fecha ISO (YYYY-MM-DD) como mes corto y año. */
fun formatearMesAnio(fechaISO: String?): String {
    if (fechaISO.isNullOrBlank()) return "—"
    val partes = fechaISO.split("-")
    if (partes.size < 2) return fechaISO
    val indice = (partes[1].toIntOrNull() ?: 0) - 1
    if (indice !in MESES.indices) return fechaISO
    return "${MESES[indice]} ${partes[0]}"
}

/** Nombre visible del plan con la primera letra en mayúscula. */
fun textoPlan(plan: String?): String {
    if (plan.isNullOrBlank()) return "Plan Gratuito"
    return "Plan ${plan.replaceFirstChar { it.uppercase() }}"
}

/** Describe las ventajas del plan del usuario. */
fun descripcionPlan(plan: String?): String = if (plan == "premium") {
    "Gestión ilimitada de camadas e Inteligencia Artificial activada."
} else {
    "Gestión de hasta 300 aves y 10 clientes. " +
        "La Inteligencia Artificial no está incluida."
}

/** Días de la semana en el orden que usa el backend (0 = lunes). */
val DIAS_SEMANA_CORTOS = listOf(
    "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
)

/** Frecuencias del recordatorio de reportes. */
val FRECUENCIAS_RESUMEN = listOf(
    "semanal" to "Semanal",
    "quincenal" to "Quincenal",
    "mensual" to "Mensual"
)

/** Texto corto de la frecuencia elegida. */
fun textoFrecuencia(frecuencia: String?): String =
    FRECUENCIAS_RESUMEN.firstOrNull { it.first == frecuencia }?.second
        ?: "Semanal"

/** Convierte la hora del backend ("05:00:00") a "HH:mm" para la UI. */
fun horaCorta(hora: String?): String {
    if (hora.isNullOrBlank()) return "05:00"
    return hora.take(5)
}

/** Nombre del día de la semana (0 = lunes). */
fun nombreDia(dia: Int): String =
    DIAS_SEMANA_CORTOS.getOrElse(dia) { DIAS_SEMANA_CORTOS[0] }

