package com.adso.eggchecker.ui.shell

import com.adso.eggchecker.domain.model.Perfil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del filtrado de avisos según las preferencias del perfil. */
class NotificacionUtilsTest {

    private fun perfil(
        produccion: Boolean = true,
        camadaLimite: Boolean = true,
        stock: Boolean = true,
        vacunacion: Boolean = false,
        resumen: Boolean = true
    ): Perfil = Perfil(
        idUsuario = 1,
        nombreCompleto = "Ana Test",
        correoElectronico = "ana@test.com",
        telefono = null,
        nombreGranja = null,
        planSuscripcion = "gratuito",
        fechaRegistro = "2026-01-01",
        activo = true,
        avesMax = 300,
        clientesMax = 10,
        iaIncluida = false,
        avesActuales = 0,
        clientesActuales = 0,
        totalHuevosProducidos = 0,
        totalAvesGestionadas = 0,
        notifProduccionBaja = produccion,
        notifCamadaLimite = camadaLimite,
        notifProduccionHora = "05:00:00",
        notifStockBajo = stock,
        notifVacunacion = vacunacion,
        notifResumenSemanal = resumen,
        notifResumenFrecuencia = "semanal",
        notifResumenDia = 0
    )

    @Test
    fun sin_perfil_se_muestran_todos_los_avisos() {
        assertTrue(debeMostrarNotificacion("stock_bajo", null))
    }

    @Test
    fun stock_respeta_la_preferencia() {
        assertTrue(debeMostrarNotificacion("sin_stock", perfil(stock = true)))
        assertFalse(debeMostrarNotificacion("stock_minimo", perfil(stock = false)))
    }

    @Test
    fun camada_limite_tiene_su_propia_preferencia() {
        assertTrue(
            debeMostrarNotificacion("camada_limite", perfil(camadaLimite = true))
        )
        assertFalse(
            debeMostrarNotificacion("camada_limite", perfil(camadaLimite = false))
        )
    }

    @Test
    fun produccion_usa_la_preferencia_de_produccion() {
        assertTrue(
            debeMostrarNotificacion("produccion_baja", perfil(produccion = true))
        )
        assertFalse(
            debeMostrarNotificacion(
                "produccion_sin_registro",
                perfil(produccion = false)
            )
        )
    }

    @Test
    fun recordatorios_respetan_vacunacion() {
        assertTrue(
            debeMostrarNotificacion(
                "recordatorio_sanitario",
                perfil(vacunacion = true)
            )
        )
        assertFalse(
            debeMostrarNotificacion(
                "recordatorio_sanitario",
                perfil(vacunacion = false)
            )
        )
    }

    @Test
    fun resumen_respeta_su_preferencia() {
        assertTrue(debeMostrarNotificacion("resumen_periodico", perfil(resumen = true)))
        assertFalse(debeMostrarNotificacion("resumen_periodico", perfil(resumen = false)))
    }

    @Test
    fun tipos_desconocidos_se_muestran() {
        assertTrue(debeMostrarNotificacion("tipo_nuevo", perfil()))
    }
}
