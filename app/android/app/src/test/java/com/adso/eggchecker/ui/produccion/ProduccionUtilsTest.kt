package com.adso.eggchecker.ui.produccion

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.domain.model.ProduccionDetalle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pruebas unitarias de las utilidades del módulo Producción. */
class ProduccionUtilsTest {

    private fun camada(estado: String, edad: Int) = Camada(
        idCamada = 1,
        idUsuario = 1,
        nombreCamada = "Camada",
        fechaIngreso = "2026-01-01",
        cantidadInicial = 100,
        cantidadActual = 90,
        estado = estado,
        edadSemanas = edad,
        fechaProximoAviso = null,
        fechaCreacion = "2026-01-01T00:00:00",
        requiereDecision = false,
        puedeEditarInicial = false,
        edadDias = null,
        fechaRetiroEstimada = null
    )

    @Test
    fun hay_cuatro_tipos_de_huevo() {
        assertEquals(4, TIPOS_HUEVO.size)
        assertEquals(listOf("aa", "a", "b", "no_apto"), TIPOS_HUEVO.map { it.clave })
    }

    @Test
    fun cantidades_cero_tiene_los_cuatro_tipos_en_cero() {
        val cero = cantidadesCero()
        assertEquals(4, cero.size)
        assertTrue(cero.values.all { it == 0 })
    }

    @Test
    fun total_huevos_suma_los_tipos_presentes() {
        val cantidades = mapOf("aa" to 20, "a" to 15, "b" to 5, "no_apto" to 2)
        assertEquals(42, totalHuevos(cantidades))
        assertEquals(35, totalHuevos(mapOf("aa" to 20, "a" to 15)))
    }

    @Test
    fun camada_en_produccion_requiere_activa_y_28_semanas() {
        assertTrue(camadaEnProduccion(camada("activa", EDAD_PRODUCCION_SEMANAS)))
        assertTrue(camadaEnProduccion(camada("activa", 40)))
        assertFalse(camadaEnProduccion(camada("activa", 27)))
        assertFalse(camadaEnProduccion(camada("retirada", 40)))
    }

    @Test
    fun cantidades_desde_detalle_mapea_por_nombre_de_tipo() {
        val detalle = listOf(
            ProduccionDetalle(1, "AA", 12),
            ProduccionDetalle(3, "B", 3),
            ProduccionDetalle(4, "No_apto", 1)
        )
        val cantidades = cantidadesDesdeDetalle(detalle)
        assertEquals(12, cantidades["aa"])
        assertEquals(3, cantidades["b"])
        assertEquals(1, cantidades["no_apto"])
        assertEquals(0, cantidades["a"])
    }

    @Test
    fun hoy_iso_tiene_formato_iso() {
        assertTrue(hoyIso().matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun fecha_hace_dias_retrocede_los_dias_indicados() {
        val calendario = Calendar.getInstance()
        calendario.add(Calendar.DAY_OF_YEAR, -3)
        val esperado = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendario.time)
        assertEquals(esperado, fechaHaceDiasIso(3))
    }

    @Test
    fun etiqueta_fecha_corta_formatea_y_controla_nulos() {
        assertEquals("—", etiquetaFechaCorta(null))
        assertEquals("—", etiquetaFechaCorta(""))
        assertEquals("—", etiquetaFechaCorta("no-fecha"))
        assertTrue(etiquetaFechaCorta("2026-03-01").contains("01 Mar"))
    }

    @Test
    fun formatear_fecha_larga_controla_nulos() {
        assertEquals("—", formatearFechaLarga(null))
        assertEquals("01 de marzo de 2026", formatearFechaLarga("2026-03-01"))
    }

    @Test
    fun conversion_utc_es_reversible() {
        val millis = utcMillisDeFecha("2026-03-01")
        assertTrue(millis > 0L)
        assertEquals("2026-03-01", fechaDesdeUtcMillis(millis))
    }
}
