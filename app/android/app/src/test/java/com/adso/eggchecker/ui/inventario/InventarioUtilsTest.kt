package com.adso.eggchecker.ui.inventario

import com.adso.eggchecker.domain.model.Insumo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de las utilidades del módulo Inventario. */
class InventarioUtilsTest {

    private fun insumo(
        stock: Double,
        umbral: Double,
        activo: Boolean = true,
        descontinuado: Boolean = false
    ) = Insumo(
        idInsumo = 1,
        idCategoria = 1,
        nombreInsumo = "Concentrado",
        unidadMedida = "kg",
        stockActual = stock,
        umbralMinimo = umbral,
        costoUnitario = 1000.0,
        activo = activo,
        descontinuado = descontinuado
    )

    @Test
    fun nivel_stock_sin_stock_cuando_es_cero() {
        assertEquals("sin_stock", nivelStock(0.0, 10.0))
    }

    @Test
    fun nivel_stock_critico_en_o_bajo_el_umbral() {
        assertEquals("critico", nivelStock(5.0, 10.0))
        assertEquals("critico", nivelStock(10.0, 10.0))
        assertEquals("critico", nivelStock(5.0, 0.0))
    }

    @Test
    fun nivel_stock_bajo_entre_uno_y_tres_veces_el_umbral() {
        assertEquals("bajo", nivelStock(20.0, 10.0))
    }

    @Test
    fun nivel_stock_optimo_desde_tres_veces_el_umbral() {
        assertEquals("optimo", nivelStock(30.0, 10.0))
        assertEquals("optimo", nivelStock(100.0, 10.0))
    }

    @Test
    fun es_critico_cuando_el_stock_no_supera_el_umbral() {
        assertTrue(esCritico(insumo(8.0, 10.0)))
        assertFalse(esCritico(insumo(20.0, 10.0)))
    }

    @Test
    fun es_optimo_cuando_supera_tres_veces_el_umbral() {
        assertFalse(esOptimo(insumo(20.0, 10.0)))
        assertTrue(esOptimo(insumo(30.0, 10.0)))
    }

    @Test
    fun porcentaje_barra_calcula_y_limita_entre_0_y_100() {
        assertEquals(50f, porcentajeBarra(15.0, 10.0), 0.01f)
        assertEquals(0f, porcentajeBarra(0.0, 10.0), 0.01f)
        assertEquals(0f, porcentajeBarra(15.0, 0.0), 0.01f)
        assertEquals(100f, porcentajeBarra(1000.0, 10.0), 0.01f)
    }

    @Test
    fun formatear_cantidad_usa_decimales_locales() {
        assertEquals("1.234,5", formatearCantidad(1234.5))
        assertEquals("40", formatearCantidad(40.0))
    }
}
