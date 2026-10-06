package com.adso.eggchecker.di

import android.content.Context

import com.adso.eggchecker.data.local.EggCheckerDatabase
import com.adso.eggchecker.data.local.SessionDataStore
import com.adso.eggchecker.data.remote.ApiClient
import com.adso.eggchecker.data.repository.AuthRepository
import com.adso.eggchecker.data.repository.CamadaRepository
import com.adso.eggchecker.data.repository.CategoriaRepository
import com.adso.eggchecker.data.repository.ClienteRepository
import com.adso.eggchecker.data.repository.InsumoRepository
import com.adso.eggchecker.data.repository.NotificacionRepository
import com.adso.eggchecker.data.repository.ProduccionRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.ui.mensajes.MensajeManager

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

    /** Repositorio de camadas del usuario. */
    val camadaRepository = CamadaRepository(api)

    /** Repositorio de producción diaria del usuario. */
    val produccionRepository = ProduccionRepository(api)

    /** Repositorios de inventario del usuario. */
    val insumoRepository = InsumoRepository(api)
    val categoriaRepository = CategoriaRepository(api)

    /** Repositorio de clientes del usuario. */
    val clienteRepository = ClienteRepository(api)

    /** Bus de recarga para el pull-to-refresh de las pantallas. */
    val refreshBus = RefreshBus()

    /** Canal global de mensajes flotantes (toasts). */
    val mensajeManager = MensajeManager()
}
