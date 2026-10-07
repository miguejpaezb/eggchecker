package com.adso.eggchecker

import android.app.Application

import com.adso.eggchecker.di.AppContainer

/** Punto de entrada de la app; expone el contenedor de dependencias. */
class EggCheckerApp : Application() {

    /** Contenedor único de dependencias de la aplicación. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificacionGestor.crearCanal()
    }
}
