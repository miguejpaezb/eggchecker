package com.adso.eggchecker.di

import android.content.Context

import com.adso.eggchecker.data.local.EggCheckerDatabase
import com.adso.eggchecker.data.local.SessionDataStore
import com.adso.eggchecker.data.remote.ApiClient
import com.adso.eggchecker.data.repository.AuthRepository
import com.adso.eggchecker.data.repository.NotificacionRepository

/**
 * Contenedor de dependencias manual (sin Hilt) para mantener el proyecto
 * simple mientras crece.
 */
class AppContainer(context: Context) {

    private val database = EggCheckerDatabase.obtener(context)

    /** Sesión persistida (token + fecha de login). */
    val sessionDataStore = SessionDataStore(context)

    private val api = ApiClient.crear(sessionDataStore)

    /** Repositorio de autenticación compartido por la app. */
    val authRepository = AuthRepository(
        api = api,
        usuarioDao = database.usuarioDao(),
        sessionDataStore = sessionDataStore
    )

    /** Repositorio de notificaciones del usuario. */
    val notificacionRepository = NotificacionRepository(api)
}
