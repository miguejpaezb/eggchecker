package com.adso.eggchecker.ui.analisis

import androidx.compose.ui.graphics.Color

import java.text.SimpleDateFormat
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

internal val CALIDAD_BUENA_BG = Color(0xFFE7F6EC)
internal val CALIDAD_BUENA_TEXTO = Color(0xFF1E6B3A)
internal val CALIDAD_REGULAR_BG = Color(0xFFFEF3C7)
internal val CALIDAD_REGULAR_TEXTO = Color(0xFF92400E)
internal val CALIDAD_MALA_BG = Color(0xFFFDECEA)
internal val CALIDAD_MALA_TEXTO = Color(0xFFB3261E)
internal val NEUTRO_BG = Color(0xFFF0F0F0)
internal val NEUTRO_TEXTO = Color(0xFF6B7280)

/** Texto y colores (fondo, texto) de la calidad general. */
fun estiloCalidad(calidad: String?): Triple<String, Color, Color> = when (calidad) {
    "buena" -> Triple("Buena", CALIDAD_BUENA_BG, CALIDAD_BUENA_TEXTO)
    "regular" -> Triple("Regular", CALIDAD_REGULAR_BG, CALIDAD_REGULAR_TEXTO)
    "mala" -> Triple("Mala", CALIDAD_MALA_BG, CALIDAD_MALA_TEXTO)
    else -> Triple("Sin detalle", NEUTRO_BG, NEUTRO_TEXTO)
}

/** Colores (fondo, texto) según la gravedad de la anomalía. */
fun coloresGravedad(gravedad: String): Pair<Color, Color> = when (gravedad) {
    "leve" -> CALIDAD_BUENA_BG to CALIDAD_BUENA_TEXTO
    "moderada" -> CALIDAD_REGULAR_BG to CALIDAD_REGULAR_TEXTO
    "grave" -> CALIDAD_MALA_BG to CALIDAD_MALA_TEXTO
    else -> NEUTRO_BG to NEUTRO_TEXTO
}

/** Nombre legible del tipo de anomalía que devuelve la API. */
fun etiquetaAnomalia(tipo: String): String = when (tipo) {
    "grieta" -> "Grieta o fisura"
    "cascara_rota" -> "Cáscara rota"
    "suciedad" -> "Suciedad en la cáscara"
    "mancha_sangre" -> "Mancha de sangre"
    "deformidad" -> "Forma irregular"
    "cascara_rugosa_delgada" -> "Cáscara rugosa o delgada"
    "tamano_anormal" -> "Tamaño anormal"
    "color_irregular" -> "Color irregular"
    else -> "Otra anomalía"
}

/** Primera letra en mayúscula ("leve" → "Leve"). */
fun capitalizar(texto: String): String =
    texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(LOCALE_CO) else it.toString() }

/** Porcentaje entero a partir de una fracción (0,85 → "85 %"). */
fun porcentaje(fraccion: Double): String = "${(fraccion * 100).toInt()} %"

/**
 * Fecha y hora legibles a partir del ISO del backend.
 * Ej.: "2026-10-08T14:30:12" → "8 oct 2026 · 2:30 p. m.".
 */
fun formatearFechaHora(iso: String): String = try {
    val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val fecha = entrada.parse(iso.replace(' ', 'T').take(19))
    if (fecha == null) {
        iso
    } else {
        SimpleDateFormat("d MMM yyyy · h:mm a", LOCALE_CO).format(fecha)
    }
} catch (e: Exception) {
    iso
}
