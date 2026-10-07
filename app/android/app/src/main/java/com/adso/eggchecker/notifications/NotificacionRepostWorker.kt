package com.adso.eggchecker.notifications

import android.content.Context

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.adso.eggchecker.EggCheckerApp

/** Reintenta mostrar una notificación descartada una hora después. */
class NotificacionRepostWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getInt(CLAVE_ID, -1)
        if (id <= 0) return Result.success()
        val gestor = (applicationContext as EggCheckerApp)
            .container
            .notificacionGestor
        return if (gestor.repostar(id)) Result.success() else Result.retry()
    }

    companion object {
        /** Clave con el id de la notificación a reintentar. */
        const val CLAVE_ID = "id_notificacion"
    }
}
