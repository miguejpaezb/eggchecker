package com.adso.eggchecker.data.repository

import com.adso.eggchecker.data.local.SessionDataStore
import com.adso.eggchecker.data.local.Sesion
import com.adso.eggchecker.data.local.UsuarioDao
import com.adso.eggchecker.data.remote.ApiException
import com.adso.eggchecker.data.remote.ApiService
import com.adso.eggchecker.data.remote.apiCall
import com.adso.eggchecker.data.remote.dto.LoginRequestDto
import com.adso.eggchecker.data.remote.dto.RecuperarRequestDto
import com.adso.eggchecker.data.remote.dto.RegistroRequestDto
import com.adso.eggchecker.domain.model.Usuario

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Punto único de acceso a autenticación: coordina Retrofit, DataStore y Room.
 */
class AuthRepository(
    private val api: ApiService,
    private val usuarioDao: UsuarioDao,
    private val sessionDataStore: SessionDataStore
) {

    /** Perfil local observado por la UI. */
    val usuarioFlow: Flow<Usuario?> = usuarioDao.observar().map { it?.toDomain() }

    /** Flujo de la sesión persistida, observado por el arranque de la app. */
    val sesionFlow: Flow<Sesion?> = sessionDataStore.sesionFlow

    /**
     * Indica si hay una sesión guardada aún vigente (menos de 90 días).
     * Si venció, borra la sesión para forzar el login.
     */
    suspend fun haySesionVigente(): Boolean {
        val sesion = sessionDataStore.sesionFlow.first() ?: return false
        if (!sesion.esVigente()) {
            sessionDataStore.limpiar()
            usuarioDao.limpiar()
            return false
        }
        return true
    }

    /**
     * Inicia sesión y sincroniza el perfil.
     *
     * @param correo Correo electrónico del usuario.
     * @param contrasena Contraseña en texto plano.
     * @return Result con error legible si falla.
     */
    suspend fun iniciarSesion(correo: String, contrasena: String): Result<Unit> {
        val login = apiCall {
            api.login(LoginRequestDto(correo, contrasena))
        }
        if (login.isFailure) {
            return Result.failure(login.exceptionOrNull()!!)
        }
        val token = login.getOrThrow()
        sessionDataStore.guardarSesion(
            token.accessToken,
            System.currentTimeMillis()
        )

        val perfil = apiCall { api.obtenerPerfil() }
        perfil.onSuccess { usuarioDao.guardar(it.toEntity()) }
        if (perfil.isFailure) {
            val error = perfil.exceptionOrNull()!!
            // Token rechazado: no tiene sentido conservar la sesión.
            if ((error as? ApiException)?.codigo == 401) {
                cerrarSesion()
                return Result.failure(error)
            }
        }
        return Result.success(Unit)
    }

    /**
     * Registra un avicultor nuevo.
     *
     * @return Result con error legible si falla.
     */
    suspend fun registrar(
        nombreCompleto: String,
        correo: String,
        contrasena: String
    ): Result<Unit> = apiCall {
        api.registrar(
            RegistroRequestDto(nombreCompleto, correo, contrasena)
        )
    }.map { }

    /**
     * Solicita la recuperación de contraseña.
     *
     * @return Mensaje de confirmación del backend.
     */
    suspend fun recuperar(correo: String): Result<String> = apiCall {
        api.recuperar(RecuperarRequestDto(correo))
    }.map { it.mensaje }

    /** Refresca el perfil desde GET /usuarios/me hacia Room. */
    suspend fun sincronizarPerfil(): Result<Unit> = apiCall {
        api.obtenerPerfil()
    }.map { usuarioDao.guardar(it.toEntity()) }

    /** Cierra la sesión: borra token y datos locales. */
    suspend fun cerrarSesion() {
        sessionDataStore.limpiar()
        usuarioDao.limpiar()
    }
}
