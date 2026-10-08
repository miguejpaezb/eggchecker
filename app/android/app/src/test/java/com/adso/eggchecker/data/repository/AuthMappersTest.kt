package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.local.UsuarioEntity
import com.adso.eggchecker.data.remote.dto.PerfilDto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de los mapeos de autenticación y perfil. */
class AuthMappersTest {

    private fun perfilDto() = PerfilDto(
        idUsuario = 7,
        nombreCompleto = "Ana Avicultora",
        correoElectronico = "ana@example.com",
        telefono = "3001234567",
        nombreGranja = "Granja Demo",
        planSuscripcion = "premium",
        fechaRegistro = "2026-01-01",
        activo = true,
        plan = "premium",
        avesMax = null,
        clientesMax = null,
        iaIncluida = true,
        avesActuales = 120,
        clientesActuales = 8,
        totalHuevosProducidos = 5000,
        totalAvesGestionadas = 300,
        notifProduccionBaja = true,
        notifStockBajo = false,
        notifVacunacion = true,
        notifResumenSemanal = false
    )

    @Test
    fun perfil_dto_a_entidad_conserva_los_datos_base() {
        val entidad = perfilDto().toEntity()
        assertEquals(7, entidad.idUsuario)
        assertEquals("Ana Avicultora", entidad.nombreCompleto)
        assertEquals("ana@example.com", entidad.correoElectronico)
        assertEquals("3001234567", entidad.telefono)
        assertEquals("Granja Demo", entidad.nombreGranja)
        assertEquals("premium", entidad.planSuscripcion)
        assertEquals("2026-01-01", entidad.fechaRegistro)
        assertTrue(entidad.activo)
    }

    @Test
    fun entidad_a_dominio_conserva_los_datos_base() {
        val entidad = UsuarioEntity(
            idUsuario = 3,
            nombreCompleto = "Luis",
            correoElectronico = "luis@example.com",
            telefono = null,
            nombreGranja = null,
            planSuscripcion = "gratuito",
            fechaRegistro = "2025-12-01",
            activo = false
        )
        val usuario = entidad.toDomain()
        assertEquals(3, usuario.idUsuario)
        assertEquals("Luis", usuario.nombreCompleto)
        assertEquals("gratuito", usuario.planSuscripcion)
        assertFalse(usuario.activo)
    }

    @Test
    fun perfil_dto_a_perfil_incluye_limites_y_uso() {
        val perfil = perfilDto().toPerfil()
        assertEquals(7, perfil.idUsuario)
        assertEquals("premium", perfil.planSuscripcion)
        assertEquals(true, perfil.iaIncluida)
        assertEquals(120, perfil.avesActuales)
        assertEquals(8, perfil.clientesActuales)
        assertEquals(5000, perfil.totalHuevosProducidos)
        assertEquals(300, perfil.totalAvesGestionadas)
        assertTrue(perfil.notifProduccionBaja)
        assertFalse(perfil.notifStockBajo)
        assertTrue(perfil.notifVacunacion)
        assertFalse(perfil.notifResumenSemanal)
    }
}
