package com.adso.eggchecker.ui.clientes

import java.text.SimpleDateFormat
import java.util.Locale

private val LOCALE_CO = Locale("es", "CO")

/** Traduce el estado del cliente a su etiqueta visible. */
fun etiquetaEstadoCliente(activo: Boolean): String =
    if (activo) "Activo" else "Suspendido"

/** Formatea la fecha de última compra; 'Sin compras' si aún no ha comprado. */
fun formatearUltimaCompra(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "Sin compras"
    return try {
        val parseada = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fecha)
        if (parseada == null) {
            "Sin compras"
        } else {
            SimpleDateFormat("dd 'de' MMMM 'de' yyyy", LOCALE_CO)
                .format(parseada)
        }
    } catch (e: Exception) {
        "Sin compras"
    }
}
