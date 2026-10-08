package com.adso.eggchecker.ui.reportes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de las utilidades del módulo Reportes. */
class ReportesUtilsTest {

    @Test
    fun regla_de_cubeta_es_30_huevos() {
        assertEquals(30, HUEVOS_POR_CUBETA)
    }

    @Test
    fun dias_del_rango_es_inclusivo() {
        assertEquals(3, diasDelRango("2026-03-01", "2026-03-03"))
        assertEquals(1, diasDelRango("2026-03-01", "2026-03-01"))
    }

    @Test
    fun dias_del_rango_controla_fechas_invalidas() {
        assertEquals(0, diasDelRango("", "2026-03-03"))
        assertEquals(0, diasDelRango("no-fecha", "2026-03-03"))
    }

    @Test
    fun rango_preset_hoy_es_el_mismo_dia() {
        val (desde, hasta) = rangoPreset("hoy")
        assertEquals(hoyIso(), desde)
        assertEquals(hoyIso(), hasta)
    }

    @Test
    fun rango_preset_semana_cubre_siete_dias() {
        val (desde, hasta) = rangoPreset("semana")
        assertEquals(hoyIso(), hasta)
        assertEquals(7, diasDelRango(desde, hasta))
    }

    @Test
    fun rango_preset_mes_inicia_el_dia_uno() {
        val (desde, _) = rangoPreset("mes")
        assertTrue(desde.endsWith("-01"))
    }

    @Test
    fun formato_numero_usa_separadores_locales() {
        assertEquals("1.234", formatoNumero(1234.0))
        assertEquals("1.234,5", formatoNumero(1234.5, decimales = 1))
    }

    @Test
    fun formatear_fecha_corta_controla_nulos_y_formato() {
        assertEquals("01/03/2026", formatearFechaCorta("2026-03-01"))
        assertEquals("—", formatearFechaCorta(null))
        assertEquals("—", formatearFechaCorta("2026-03"))
    }

    @Test
    fun conversion_utc_es_reversible() {
        val millis = utcMillisDeFecha("2026-03-01")
        assertTrue(millis > 0L)
        assertEquals("2026-03-01", fechaDesdeUtcMillis(millis))
    }

    @Test
    fun hay_tres_presets_de_reporte() {
        assertEquals(3, PRESETS_REPORTE.size)
    }
}
