package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.CambiarContrasenaDto
import com.adso.eggchecker.data.remote.dto.NotificacionesUpdateDto
import com.adso.eggchecker.data.remote.dto.PerfilUpdateDto
import com.adso.eggchecker.domain.model.Perfil

/** Acceso al perfil del usuario autenticado (modo online). */
class PerfilRepository(
    private val api: ApiService
) {

    /** Consulta el perfil completo del usuario. */
    suspend fun obtenerPerfil(): Result<Perfil> =
        apiCall { api.obtenerPerfil() }.map { it.toPerfil() }

    /** Actualiza los datos personales y de la granja. */
    suspend fun actualizarPerfil(
        nombreCompleto: String,
        correoElectronico: String,
        telefono: String?,
        nombreGranja: String?,
        contrasenaActual: String?
    ): Result<Perfil> = apiCall {
        api.actualizarPerfil(
            PerfilUpdateDto(
                nombreCompleto = nombreCompleto,
                correoElectronico = correoElectronico,
                telefono = telefono,
                nombreGranja = nombreGranja,
                contrasenaActual = contrasenaActual
            )
        )
    }.map { it.toPerfil() }

    /** Guarda las preferencias de alertas del usuario. */
    suspend fun actualizarNotificaciones(
        produccionBaja: Boolean,
        stockBajo: Boolean,
        vacunacion: Boolean,
        resumenSemanal: Boolean
    ): Result<Perfil> = apiCall {
        api.actualizarNotificaciones(
            NotificacionesUpdateDto(
                notifProduccionBaja = produccionBaja,
                notifStockBajo = stockBajo,
                notifVacunacion = vacunacion,
                notifResumenSemanal = resumenSemanal
            )
        )
    }.map { it.toPerfil() }

    /** Cambia la contraseña del usuario. */
    suspend fun cambiarContrasena(
        contrasenaActual: String,
        contrasenaNueva: String
    ): Result<String> = apiCall {
        api.cambiarContrasena(
            CambiarContrasenaDto(
                contrasenaActual = contrasenaActual,
                contrasenaNueva = contrasenaNueva
            )
        )
    }.map { it.mensaje }
}
