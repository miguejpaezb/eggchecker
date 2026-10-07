package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.NotificacionDto
import com.adso.eggchecker.domain.model.Notificacion

/** Acceso a las notificaciones del usuario autenticado. */
class NotificacionRepository(
    private val api: ApiService
) {

    /** Lista las notificaciones (no leídas primero). */
    suspend fun listar(): Result<List<Notificacion>> =
        apiCall { api.listarNotificaciones() }
            .map { lista -> lista.map { it.toDomain() } }

    /** Marca una notificación como leída. */
    suspend fun marcarLeida(idNotificacion: Int): Result<Notificacion> =
        apiCall { api.marcarNotificacionLeida(idNotificacion) }
            .map { it.toDomain() }

    /** Marca todas las notificaciones como leídas. */
    suspend fun marcarTodas(): Result<Unit> =
        apiCall { api.marcarTodasLeidas() }.map { }

    /** Elimina una notificación. */
    suspend fun eliminar(idNotificacion: Int): Result<Unit> =
        apiCall { api.eliminarNotificacion(idNotificacion) }.map { }
}

private fun NotificacionDto.toDomain(): Notificacion = Notificacion(
    idNotificacion = idNotificacion,
    idCamada = idCamada,
    idInsumo = idInsumo,
    tipo = tipo,
    titulo = titulo,
    mensaje = mensaje,
    leida = leida
)
