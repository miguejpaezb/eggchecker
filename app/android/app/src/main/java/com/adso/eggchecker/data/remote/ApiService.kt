package com.adso.eggchecker.data.remote

import com.adso.eggchecker.data.remote.dto.LoginRequestDto
import com.adso.eggchecker.data.remote.dto.MensajeResponseDto
import com.adso.eggchecker.data.remote.dto.NotificacionDto
import com.adso.eggchecker.data.remote.dto.RecuperarRequestDto
import com.adso.eggchecker.data.remote.dto.RegistroRequestDto
import com.adso.eggchecker.data.remote.dto.TokenResponseDto
import com.adso.eggchecker.data.remote.dto.UsuarioDto

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints del backend EggChecker consumidos por la app.
 * Las rutas son relativas a la base URL (termina en /api/).
 */
interface ApiService {

    /** Inicia sesión y devuelve el token de acceso. */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    /** Registra un avicultor nuevo. */
    @POST("auth/registro")
    suspend fun registrar(@Body body: RegistroRequestDto): UsuarioDto

    /** Consulta el perfil del usuario autenticado. */
    @GET("usuarios/me")
    suspend fun obtenerPerfil(): UsuarioDto

    /** Solicita la recuperación de contraseña. */
    @POST("auth/recuperar")
    suspend fun recuperar(@Body body: RecuperarRequestDto): MensajeResponseDto

    /** Lista las notificaciones del usuario (no leídas primero). */
    @GET("notificaciones")
    suspend fun listarNotificaciones(): List<NotificacionDto>

    /** Marca una notificación como leída. */
    @POST("notificaciones/{id}/leer")
    suspend fun marcarNotificacionLeida(
        @Path("id") idNotificacion: Int
    ): NotificacionDto

    /** Marca todas las notificaciones como leídas. */
    @POST("notificaciones/leer-todas")
    suspend fun marcarTodasLeidas()

    /** Elimina una notificación del usuario. */
    @DELETE("notificaciones/{id}")
    suspend fun eliminarNotificacion(@Path("id") idNotificacion: Int)
}
