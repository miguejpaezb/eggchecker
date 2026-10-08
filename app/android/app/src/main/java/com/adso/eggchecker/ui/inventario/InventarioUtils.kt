package com.adso.eggchecker.ui.inventario

import com.adso.eggchecker.domain.model.Insumo

import java.text.NumberFormat
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

/**
 * Nivel de stock de un insumo:
 * - `sin_stock`: 0 · `critico`: 0 < stock <= mínimo
 * - `bajo`: mínimo < stock < 3×mínimo · `optimo`: >= 3×mínimo
 */
fun nivelStock(stock: Double, umbral: Double): String {
    if (stock <= 0.0) return "sin_stock"
    if (umbral <= 0.0 || stock <= umbral) return "critico"
    if (stock < umbral * 3) return "bajo"
    return "optimo"
}

/** Indica si el insumo está en el mínimo o menos. */
fun esCritico(insumo: Insumo): Boolean =
    insumo.stockActual <= insumo.umbralMinimo

/** Indica si el insumo está en nivel óptimo (>= 3× el mínimo). */
fun esOptimo(insumo: Insumo): Boolean =
    nivelStock(insumo.stockActual, insumo.umbralMinimo) == "optimo"

/** Ancho de la barra de stock (0-100) respecto a 3× el mínimo. */
fun porcentajeBarra(stock: Double, umbral: Double): Float {
    if (umbral <= 0.0) return 0f
    val porcentaje = (stock / (umbral * 3)) * 100.0
    return porcentaje.coerceIn(0.0, 100.0).toFloat()
}

/** Cantidad con separadores locales y hasta 2 decimales. */
fun formatearCantidad(valor: Double): String {
    val formato = NumberFormat.getNumberInstance(LOCALE_CO)
    formato.maximumFractionDigits = 2
    return formato.format(valor)
}
