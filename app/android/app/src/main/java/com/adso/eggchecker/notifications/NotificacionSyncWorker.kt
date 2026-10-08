package com.adso.eggchecker.notifications

import android.content.Context

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.adso.eggchecker.EggCheckerApp

/** Sincroniza las notificaciones del backend con la barra del sistema. */
class NotificacionSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val gestor = (applicationContext as EggCheckerApp)
            .container
            .notificacionGestor
        return if (gestor.sincronizar()) Result.success() else Result.retry()
    }
}
