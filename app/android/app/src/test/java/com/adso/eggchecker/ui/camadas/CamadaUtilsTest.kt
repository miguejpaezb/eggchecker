package com.adso.eggchecker.ui.camadas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pruebas unitarias de las utilidades del módulo Camadas. */
class CamadaUtilsTest {

    @Test
    fun viabilidad_calcula_porcentaje_de_aves_vivas() {
        assertEquals(96.0, calcularViabilidad(48, 50), 0.0001)
        assertEquals(100.0, calcularViabilidad(50, 50), 0.0001)
    }

    @Test
    fun viabilidad_con_inicial_cero_devuelve_cero() {
        assertEquals(0.0, calcularViabilidad(10, 0), 0.0001)
    }

    @Test
    fun etiqueta_estado_traduce_los_estados_conocidos() {
        assertEquals("Activa", etiquetaEstado("activa"))
        assertEquals("Retirada", etiquetaEstado("retirada"))
        assertEquals("desconocido", etiquetaEstado("desconocido"))
    }

    @Test
    fun formatear_cantidad_usa_separador_de_miles() {
        assertEquals("1.234", formatearCantidad(1234))
        assertEquals("50", formatearCantidad(50))
    }

    @Test
    fun formatear_fecha_nula_o_vacia_devuelve_guion() {
        assertEquals("—", formatearFecha(null))
        assertEquals("—", formatearFecha(""))
    }

    @Test
    fun formatear_fecha_valida_la_pasa_a_texto_largo() {
        assertEquals("01 de marzo de 2026", formatearFecha("2026-03-01"))
    }

    @Test
    fun formatear_fecha_invalida_devuelve_guion() {
        assertEquals("—", formatearFecha("no-es-fecha"))
    }

    @Test
    fun fecha_hoy_iso_tiene_formato_iso() {
        assertTrue(fechaHoyIso().matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun fecha_ayer_es_el_dia_anterior() {
        val calendario = Calendar.getInstance()
        calendario.add(Calendar.DAY_OF_YEAR, -1)
        val esperado = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendario.time)
        assertEquals(esperado, fechaAyerIso())
    }
}
