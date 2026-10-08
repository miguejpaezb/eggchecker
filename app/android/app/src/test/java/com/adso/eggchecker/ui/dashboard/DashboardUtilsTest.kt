package com.adso.eggchecker.ui.dashboard

import com.adso.eggchecker.domain.model.MejorDia

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas unitarias de las utilidades del módulo Dashboard. */
class DashboardUtilsTest {

    @Test
    fun formato_porcentaje_usa_decimales_fijos() {
        assertEquals("76,43%", formatoPorcentaje(76.432))
        assertEquals("10%", formatoPorcentaje(10.0, decimales = 0))
    }

    @Test
    fun texto_variacion_sin_registro() {
        assertEquals("Sin registro de ayer", textoVariacion(null))
    }

    @Test
    fun texto_variacion_positiva_lleva_flecha_ascendente_y_signo() {
        assertEquals("↑ +5,0% vs ayer", textoVariacion(5.0))
    }

    @Test
    fun texto_variacion_negativa_lleva_flecha_descendente() {
        assertEquals("↓ -3,0% vs ayer", textoVariacion(-3.0))
    }

    @Test
    fun texto_mejor_dia_sin_registros() {
        assertEquals("Sin registros", textoMejorDia(null))
    }

    @Test
    fun texto_mejor_dia_formatea_cantidad_y_etiqueta() {
        assertEquals("42 - Sáb 22", textoMejorDia(MejorDia("Sáb 22", 42)))
    }
}
