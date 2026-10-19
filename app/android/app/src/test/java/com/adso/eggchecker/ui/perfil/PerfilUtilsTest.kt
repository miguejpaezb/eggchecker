package com.adso.eggchecker.ui.perfil

import com.adso.eggchecker.domain.model.PreferenciasNotificaciones

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

    @Test
    fun hora_corta_recorta_los_segundos_del_backend() {
        assertEquals("05:00", horaCorta("05:00:00"))
        assertEquals("23:45", horaCorta("23:45:00"))
        assertEquals("04:30", horaCorta("04:30"))
    }

    @Test
    fun hora_corta_usa_el_valor_por_defecto_si_falta() {
        assertEquals("05:00", horaCorta(null))
        assertEquals("05:00", horaCorta(""))
    }

    @Test
    fun nombre_dia_usa_el_orden_del_backend() {
        assertEquals("Lun", nombreDia(0))
        assertEquals("Dom", nombreDia(6))
        assertEquals("Lun", nombreDia(9))
    }

    @Test
    fun texto_frecuencia_traduce_las_opciones() {
        assertEquals("Semanal", textoFrecuencia("semanal"))
        assertEquals("Quincenal", textoFrecuencia("quincenal"))
        assertEquals("Mensual", textoFrecuencia("mensual"))
        assertEquals("Semanal", textoFrecuencia(null))
    }

    @Test
    fun preferencias_por_defecto_siguen_al_backend() {
        val prefs = PreferenciasNotificaciones()
        assertEquals(true, prefs.produccionBaja)
        assertEquals(true, prefs.camadaLimite)
        assertEquals(false, prefs.vacunacion)
        assertEquals("05:00", prefs.produccionHora)
        assertEquals("semanal", prefs.resumenFrecuencia)
        assertEquals(0, prefs.resumenDia)
    }
}
