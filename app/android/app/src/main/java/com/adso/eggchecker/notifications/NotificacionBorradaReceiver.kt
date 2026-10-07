package com.adso.eggchecker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.adso.eggchecker.EggCheckerApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Atiende las acciones de las notificaciones del sistema: el descarte
 * (deslizar) y el botón "Marcar como leído".
 */
class NotificacionBorradaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id <= 0) return

        val pendiente = goAsync()
        val container = (context.applicationContext as EggCheckerApp).container
        val gestor = container.notificacionGestor
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == ACTION_MARCAR_LEIDO) {
                    container.notificacionRepository.marcarLeida(id)
                        .onSuccess { gestor.quitarDescartada(id) }
                } else {
                    gestor.marcarDescartada(id)
                }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        /** Clave con el id de la notificación. */
        const val EXTRA_ID = "id_notificacion"

        /** Acción del botón "Marcar como leído". */
        const val ACTION_MARCAR_LEIDO = "com.adso.eggchecker.MARCAR_LEIDO"

        /** Acción al deslizar/descartar la notificación. */
        const val ACTION_DESCARTAR = "com.adso.eggchecker.DESCARTAR"
    }
}
