package com.adso.eggchecker.ui.produccion

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.ProduccionDetalle

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Edad mínima, en semanas, para que una camada produzca. */
const val EDAD_PRODUCCION_SEMANAS = 28

private val LOCALE_CO = Locale("es", "CO")
private val LOCALE_US = Locale.US

/** Tipo de huevo con su información de presentación. */
data class TipoHuevo(
    val clave: String,
    val nombre: String,
    val titulo: String,
    val descripcion: String
)

/** Los cuatro tipos de huevo, en el orden del web. */
val TIPOS_HUEVO = listOf(
    TipoHuevo("aa", "AA", "Tipo AA - Primera calidad", "Cáscara dura, sin defectos, tamaño grande"),
    TipoHuevo("a", "A", "Tipo A - Buena calidad", "Pequeñas imperfecciones en cáscara o tamaño"),
    TipoHuevo("b", "B", "Tipo B", "Defectos visibles: cáscara rugosa o manchas"),
    TipoHuevo("no_apto", "No_apto", "No Aptos", "Rotos, cáscara blanda o contaminados")
)

/** Mapa de cantidades en cero por tipo. */
fun cantidadesCero(): Map<String, Int> = TIPOS_HUEVO.associate { it.clave to 0 }

/** Suma las cantidades de los cuatro tipos de huevo. */
fun totalHuevos(cantidades: Map<String, Int>): Int =
    TIPOS_HUEVO.sumOf { cantidades[it.clave] ?: 0 }

/** Indica si la camada está activa y en etapa de producción (≥28 semanas). */
fun camadaEnProduccion(camada: Camada): Boolean =
    camada.estado == "activa" && camada.edadSemanas >= EDAD_PRODUCCION_SEMANAS

/** Convierte el detalle por tipo a cantidades editables. */
fun cantidadesDesdeDetalle(detalle: List<ProduccionDetalle>): Map<String, Int> {
    val cantidades = cantidadesCero().toMutableMap()
    detalle.forEach { fila ->
        val tipo = TIPOS_HUEVO.find { it.nombre == fila.nombreTipo }
        if (tipo != null) {
            cantidades[tipo.clave] = fila.cantidad
        }
    }
    return cantidades
}

/** Fecha de hoy en formato ISO local (YYYY-MM-DD). */
fun hoyIso(): String = SimpleDateFormat("yyyy-MM-dd", LOCALE_US).format(Date())

/** Fecha ISO de hace N días. */
fun fechaHaceDiasIso(dias: Int): String {
    val calendario = Calendar.getInstance()
    calendario.add(Calendar.DAY_OF_YEAR, -dias)
    return SimpleDateFormat("yyyy-MM-dd", LOCALE_US).format(calendario.time)
}

/** Fecha ISO a etiqueta corta en español (ej. "Sáb 22 Mar"). */
fun etiquetaFechaCorta(fechaISO: String?): String {
    if (fechaISO.isNullOrBlank()) return "—"
    return try {
        val fecha = SimpleDateFormat("yyyy-MM-dd", LOCALE_US).parse(fechaISO)
        if (fecha == null) {
            "—"
        } else {
            SimpleDateFormat("EEE dd MMM", LOCALE_CO)
                .format(fecha)
                .replace(".", "")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { palabra ->
                    palabra.replaceFirstChar { it.uppercase() }
                }
        }
    } catch (e: Exception) {
        "—"
    }
}

/** Fecha ISO a texto largo en español. */
fun formatearFechaLarga(fechaISO: String?): String {
    if (fechaISO.isNullOrBlank()) return "—"
    return try {
        val fecha = SimpleDateFormat("yyyy-MM-dd", LOCALE_US).parse(fechaISO)
        if (fecha == null) {
            "—"
        } else {
            SimpleDateFormat("dd 'de' MMMM 'de' yyyy", LOCALE_CO).format(fecha)
        }
    } catch (e: Exception) {
        "—"
    }
}

/** Milisegundos UTC de una fecha ISO (medianoche UTC). */
fun utcMillisDeFecha(fechaISO: String): Long {
    return try {
        val fecha = SimpleDateFormat("yyyy-MM-dd", LOCALE_US).parse(fechaISO)
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
