package com.adso.eggchecker.ui.analisis

import androidx.compose.ui.graphics.Color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de las utilidades del módulo Análisis IA. */
class AnalisisUtilsTest {

    @Test
    fun estilo_calidad_traduce_los_niveles_conocidos() {
        assertEquals("Buena", estiloCalidad("buena").first)
        assertEquals("Regular", estiloCalidad("regular").first)
        assertEquals("Mala", estiloCalidad("mala").first)
    }

    @Test
    fun estilo_calidad_desconocida_usa_texto_neutro() {
        assertEquals("Sin detalle", estiloCalidad("otra").first)
        assertEquals("Sin detalle", estiloCalidad(null).first)
    }

    @Test
    fun estilo_calidad_devuelve_los_colores_de_cada_nivel() {
        assertEquals(Color(0xFFE7F6EC), estiloCalidad("buena").second)
        assertEquals(Color(0xFFFEF3C7), estiloCalidad("regular").second)
        assertEquals(Color(0xFFFDECEA), estiloCalidad("mala").second)
        assertEquals(Color(0xFFF0F0F0), estiloCalidad(null).second)
    }

    @Test
    fun colores_gravedad_mapea_cada_nivel() {
        assertEquals(Color(0xFFE7F6EC), coloresGravedad("leve").first)
        assertEquals(Color(0xFFFEF3C7), coloresGravedad("moderada").first)
        assertEquals(Color(0xFFFDECEA), coloresGravedad("grave").first)
    }

    @Test
    fun colores_gravedad_desconocida_usa_color_neutro() {
        assertEquals(Color(0xFFF0F0F0), coloresGravedad("desconocida").first)
    }

    @Test
    fun etiqueta_anomalia_traduce_los_tipos_conocidos() {
        assertEquals("Grieta o fisura", etiquetaAnomalia("grieta"))
        assertEquals("Cáscara rota", etiquetaAnomalia("cascara_rota"))
        assertEquals("Suciedad en la cáscara", etiquetaAnomalia("suciedad"))
        assertEquals("Mancha de sangre", etiquetaAnomalia("mancha_sangre"))
        assertEquals("Forma irregular", etiquetaAnomalia("deformidad"))
        assertEquals("Cáscara rugosa o delgada", etiquetaAnomalia("cascara_rugosa_delgada"))
        assertEquals("Tamaño anormal", etiquetaAnomalia("tamano_anormal"))
        assertEquals("Color irregular", etiquetaAnomalia("color_irregular"))
    }

    @Test
    fun etiqueta_anomalia_desconocida_usa_texto_generico() {
        assertEquals("Otra anomalía", etiquetaAnomalia("tipo_raro"))
    }

    @Test
    fun capitalizar_pone_la_primera_letra_en_mayuscula() {
        assertEquals("Leve", capitalizar("leve"))
        assertEquals("Grave", capitalizar("grave"))
        assertEquals("", capitalizar(""))
    }

    @Test
    fun porcentaje_convierte_fraccion_a_entero() {
        assertEquals("85 %", porcentaje(0.85))
        assertEquals("100 %", porcentaje(1.0))
        assertEquals("0 %", porcentaje(0.0))
    }

    @Test
    fun formatear_fecha_hora_acepta_el_iso_del_backend() {
        val formateada = formatearFechaHora("2026-10-08T14:30:12")
        assertTrue(formateada.contains("2026"))
        assertNotEquals("2026-10-08T14:30:12", formateada)
    }

    @Test
    fun formatear_fecha_hora_acepta_espacio_como_separador() {
        assertTrue(formatearFechaHora("2026-10-08 09:00:00").contains("2026"))
    }

    @Test
    fun formatear_fecha_hora_invalida_devuelve_el_texto_original() {
        assertEquals("texto", formatearFechaHora("texto"))
    }
}
