package com.adso.eggchecker.ui.recordatorios

import com.adso.eggchecker.domain.model.MODO_CADA_DIAS
import com.adso.eggchecker.domain.model.MODO_DIAS_SEMANA
import com.adso.eggchecker.domain.model.MODO_FECHAS
import com.adso.eggchecker.domain.model.Recordatorio

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas unitarias de las utilidades de recordatorios sanitarios. */
class RecordatorioUtilsTest {

    private fun regla(
        modo: String = MODO_CADA_DIAS,
        intervalo: Int? = 15,
        dias: List<Int> = emptyList(),
        fechas: List<String> = emptyList(),
        idCamada: Int? = 1,
        nombreCamada: String? = "Camada Norte"
    ): Recordatorio = Recordatorio(
        idRecordatorio = 1,
        idCamada = idCamada,
        nombreCamada = nombreCamada,
        idInsumo = 2,
        nombreInsumo = "Newcastle",
        unidadMedida = "ml",
        stockActual = 20.0,
        cantidad = 5.0,
        modo = modo,
        intervaloDias = intervalo,
        diasSemana = dias,
        fechas = fechas,
        fechaInicio = "2026-10-18",
        fechaFin = null,
        activo = true,
        proximaDosis = "2026-10-18",
        dosisPendientes = 2
    )

    @Test
    fun formato_cantidad_quita_los_decimales_innecesarios() {
        assertEquals("5", formatoCantidad(5.0))
        assertEquals("2.5", formatoCantidad(2.5))
        assertEquals("0.25", formatoCantidad(0.25))
        assertEquals("15", formatoCantidad(15.0))
    }

    @Test
    fun texto_cantidad_incluye_la_unidad() {
        assertEquals("5 ml", textoCantidad(5.0, "ml"))
        assertEquals("2.5 g", textoCantidad(2.5, "g"))
    }

    @Test
    fun resumen_cada_dias_muestra_el_intervalo() {
        assertEquals("Cada 15 días", resumenModo(regla(intervalo = 15)))
    }

    @Test
    fun resumen_cada_dia_usa_singular() {
        assertEquals("Cada día", resumenModo(regla(intervalo = 1)))
    }

    @Test
    fun resumen_dias_semana_lista_los_nombres() {
        val resumen = resumenModo(
            regla(modo = MODO_DIAS_SEMANA, intervalo = null, dias = listOf(0, 3))
        )
        assertEquals("Lun, Jue", resumen)
    }

    @Test
    fun resumen_fechas_cuenta_las_fechas() {
        val resumen = resumenModo(
            regla(
                modo = MODO_FECHAS,
                intervalo = null,
                fechas = listOf("2026-11-01", "2026-11-15", "2026-11-29")
            )
        )
        assertEquals("3 fechas", resumen)
    }

    @Test
    fun alcance_distingue_global_de_camada() {
        assertEquals("Camada Norte", textoAlcance(regla()))
        assertEquals(
            "Toda la granja",
            textoAlcance(regla(idCamada = null, nombreCamada = null))
        )
    }

    @Test
    fun fecha_corta_formatea_iso_y_vacio() {
        assertEquals("18/10/2026", fechaCorta("2026-10-18"))
        assertEquals("—", fechaCorta(null))
        assertEquals("—", fechaCorta(""))
    }

    @Test
    fun restar_dias_devuelve_la_fecha_esperada() {
        assertEquals("2026-10-12", restarDias("2026-10-18", 6))
        assertEquals("2026-09-28", restarDias("2026-10-18", 20))
        assertEquals("2026-10-18", restarDias("2026-10-18", 0))
    }

    @Test
    fun hoy_iso_tiene_el_formato_del_backend() {
        val hoy = hoyIsoRecordatorio()
        assertEquals(10, hoy.length)
        assertEquals('-', hoy[4])
        assertEquals('-', hoy[7])
    }

    @Test
    fun texto_dosis_indica_numero_y_total() {
        assertEquals("dosis 2 de 7", textoDosis(2, 7))
    }
}
