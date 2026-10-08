package com.adso.eggchecker.ui.perfil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de las utilidades del módulo Perfil. */
class PerfilUtilsTest {

    @Test
    fun formatear_mes_anio_controla_nulos_y_formato() {
        assertEquals("Mar 2026", formatearMesAnio("2026-03-01"))
        assertEquals("—", formatearMesAnio(null))
        assertEquals("—", formatearMesAnio(""))
        assertEquals("2026", formatearMesAnio("2026"))
        assertEquals("2026-13-01", formatearMesAnio("2026-13-01"))
    }

    @Test
    fun texto_plan_capitaliza_y_usa_gratuito_por_defecto() {
        assertEquals("Plan Premium", textoPlan("premium"))
        assertEquals("Plan Gratuito", textoPlan("gratuito"))
        assertEquals("Plan Gratuito", textoPlan(null))
        assertEquals("Plan Gratuito", textoPlan(""))
    }

    @Test
    fun descripcion_plan_premium_menciona_la_ia() {
        assertTrue(descripcionPlan("premium").contains("Inteligencia Artificial activada"))
    }

    @Test
    fun descripcion_plan_gratuito_menciona_los_limites() {
        val texto = descripcionPlan("gratuito")
        assertTrue(texto.contains("300 aves"))
        assertTrue(texto.contains("10 clientes"))
    }
}
