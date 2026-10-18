package com.adso.eggchecker.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

import com.adso.eggchecker.MainActivity
import com.adso.eggchecker.R
import com.adso.eggchecker.data.local.AjustesNotificacion
import com.adso.eggchecker.data.local.NotificacionEstadoStore
import com.adso.eggchecker.data.repository.NotificacionRepository
import com.adso.eggchecker.data.repository.PerfilRepository
import com.adso.eggchecker.domain.model.Notificacion
import com.adso.eggchecker.domain.model.Perfil
import com.adso.eggchecker.ui.shell.debeMostrarNotificacion

import java.util.concurrent.TimeUnit

/**
 * Publica las notificaciones del backend en la barra del dispositivo y
 * gestiona su reaparición tras una hora si siguen sin leerse.
 */
class NotificacionGestor(
    private val context: Context,
    private val notificacionRepository: NotificacionRepository,
    private val perfilRepository: PerfilRepository,
    private val estadoStore: NotificacionEstadoStore
) {

    /**
     * Aplica los ajustes locales: canal (sonido/vibración) y trabajo periódico.
     *
     * En Android 8+ el sonido de un canal no se puede editar: si cambia el
     * sonido o la vibración se crea un canal nuevo (con otra generación) y
     * se borran los anteriores, para que el sistema aplique el sonido de
     * EggChecker en lugar del predeterminado del teléfono.
     */
    suspend fun sincronizarAjustes() {
        val ajustes = estadoStore.leerAjustes()
        asegurarCanal(ajustes)
        aplicarEstadoTrabajo(ajustes.activas)
    }

    /** Reproduce el sonido de notificación actual (vista previa). */
    suspend fun reproducirSonido() {
        val uri = uriSonido(estadoStore.leerAjustes().sonidoUri)
        runCatching {
            RingtoneManager.getRingtone(context, uri)?.play()
        }
    }

    /** Publica las no leídas nuevas y limpia las que ya no aplican. */
    suspend fun sincronizar(): Boolean {
        val ajustes = estadoStore.leerAjustes()
        if (!ajustes.activas) {
            cancelarTodas()
            return true
        }
        val canal = asegurarCanal(ajustes)
        val lista = notificacionRepository.listar().getOrNull() ?: return false
        val perfil = perfilRepository.obtenerPerfil().getOrNull()
        val descartadas = estadoStore.descartadas()
        val activas = idsActivas()
        val noLeidas = lista.filter { !it.leida }
        val idsNoLeidas = noLeidas.map { it.idNotificacion }.toSet()

        (activas - idsNoLeidas).forEach { cancelar(it) }

        noLeidas.forEach { aviso ->
            val id = aviso.idNotificacion
            if (id in activas || id in descartadas) return@forEach
            if (mostrable(aviso.tipo, perfil)) publicar(aviso, ajustes, canal)
        }
        return true
    }

    /**
     * Reintenta publicar una notificación descartada tras la hora de espera.
     *
     * @return true si el trabajo terminó; false si hay que reintentar.
     */
    suspend fun repostar(id: Int): Boolean {
        val ajustes = estadoStore.leerAjustes()
        if (!ajustes.activas) {
            cancelar(id)
            estadoStore.quitar(id)
            return true
        }
        val canal = asegurarCanal(ajustes)
        val lista = notificacionRepository.listar().getOrNull() ?: return false
        val perfil = perfilRepository.obtenerPerfil().getOrNull()
        val aviso = lista.find { it.idNotificacion == id && !it.leida }
        estadoStore.quitar(id)
        if (aviso != null && mostrable(aviso.tipo, perfil)) {
            publicar(aviso, ajustes, canal)
        }
        return true
    }

    /** Marca una notificación como descartada y agenda su reaparición. */
    suspend fun marcarDescartada(id: Int) {
        estadoStore.agregar(id)
        programarRepost(id)
    }

    /** Cancela la notificación del sistema y su reaparición pendiente. */
    suspend fun quitarDescartada(id: Int) {
        estadoStore.quitar(id)
        cancelarRepost(id)
        cancelar(id)
    }

    /** Cancela todas las notificaciones visibles del sistema. */
    fun cancelarTodas() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    /** Detiene todo al cerrar sesión. */
    suspend fun detener() {
        cancelarTodas()
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_PERIODICO)
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_SINCRONIZAR)
        estadoStore.limpiar()
    }

    private fun aplicarEstadoTrabajo(activas: Boolean) {
        if (activas) {
            asegurarPeriodico()
            sincronizarAhora()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_PERIODICO)
            WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_SINCRONIZAR)
            cancelarTodas()
        }
    }

    /** Programa la sincronización periódica de notificaciones. */
    private fun asegurarPeriodico() {
        val solicitud = PeriodicWorkRequestBuilder<NotificacionSyncWorker>(
            MINUTOS_SINCRONIZACION,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TRABAJO_PERIODICO,
            ExistingPeriodicWorkPolicy.UPDATE,
            solicitud
        )
    }

    /** Lanza una sincronización inmediata (al abrir la app). */
    private fun sincronizarAhora() {
        val solicitud = OneTimeWorkRequestBuilder<NotificacionSyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABAJO_SINCRONIZAR,
            ExistingWorkPolicy.REPLACE,
            solicitud
        )
    }

    private fun publicar(
        aviso: Notificacion,
        ajustes: AjustesNotificacion,
        canal: String?
    ) {
        if (!puedeNotificar()) return

        val id = aviso.idNotificacion
        val abrir = PendingIntent.getActivity(
            context,
            id,
            intentAbrirNotificaciones(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val ver = PendingIntent.getActivity(
            context,
            id + CODIGO_VER,
            intentAbrirNotificaciones(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val marcarLeido = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, NotificacionBorradaReceiver::class.java).apply {
                action = NotificacionBorradaReceiver.ACTION_MARCAR_LEIDO
                putExtra(NotificacionBorradaReceiver.EXTRA_ID, id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val borrar = PendingIntent.getBroadcast(
            context,
            id + CODIGO_BORRAR,
            Intent(context, NotificacionBorradaReceiver::class.java).apply {
                action = NotificacionBorradaReceiver.ACTION_DESCARTAR
                putExtra(NotificacionBorradaReceiver.EXTRA_ID, id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, canal ?: CANAL_BASE)
            .setSmallIcon(R.drawable.ic_stat_notificacion)
            .setColor(Color.parseColor("#905E27"))
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            // Una alerta ya visible se actualiza sin volver a sonar.
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .setDeleteIntent(borrar)
            .addAction(0, "Ver", ver)
            .addAction(0, "Marcar como leído", marcarLeido)

        // En Android 8+ el sonido y la vibración los controla el canal.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(uriSonido(ajustes.sonidoUri))
            if (ajustes.vibracion) {
                builder.setVibrate(PATRON_VIBRACION)
            }
        }

        NotificationManagerCompat.from(context)
            .notify(aviso.idNotificacion, builder.build())
    }

    /**
     * Garantiza que exista el canal con el sonido y la importancia correctos.
     *
     * Android no permite editar el sonido de un canal ya creado, así que la
     * única forma de forzar el sonido de EggChecker (o el que el usuario elija
     * en la app) es crear un canal nuevo. El número de generación va en el id
     * y los canales anteriores se borran.
     *
     * @return El id del canal activo, o null antes de Android 8.
     */
    private suspend fun asegurarCanal(ajustes: AjustesNotificacion): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return null

        val sonido = uriSonido(ajustes.sonidoUri)
        val firma = "$sonido|${ajustes.vibracion}"
        var generacion = estadoStore.generacionCanal()
        var id = idCanal(generacion)
        val canal = manager.getNotificationChannel(id)
        val firmaGuardada = estadoStore.firmaCanal()

        if (canal == null || firmaGuardada != firma) {
            generacion += 1
            id = idCanal(generacion)
            manager.createNotificationChannel(
                construirCanal(id, sonido, ajustes.vibracion)
            )
            estadoStore.guardarCanal(generacion, firma)
            limpiarCanalesViejos(manager, id)
        }
        return id
    }

    /** Borra los canales de versiones anteriores que ya no se usan. */
    private fun limpiarCanalesViejos(manager: NotificationManager, activo: String) {
        manager.notificationChannels
            .map { it.id }
            .filter { it.startsWith(PREFIJO_CANAL) && it != activo }
            .forEach { manager.deleteNotificationChannel(it) }
    }

    private fun idCanal(generacion: Int): String = "${CANAL_BASE}_g$generacion"

    private fun construirCanal(
        id: String,
        sonido: Uri,
        vibrar: Boolean
    ): NotificationChannel =
        NotificationChannel(
            id,
            "Alertas de la granja",
            // HIGH habilita la ventana emergente (heads-up) al llegar.
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de producción, inventario, camadas y " +
                "recordatorios sanitarios."
            setSound(sonido, atributosSonido())
            enableVibration(vibrar)
            if (vibrar) {
                vibrationPattern = PATRON_VIBRACION
            }
            enableLights(true)
            lightColor = Color.parseColor("#FDC33B")
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }

    /**
     * URI del sonido de las notificaciones.
     *
     * Si el usuario eligió uno en la app se usa ese; si no, el sonido
     * incluido en el APK (`res/raw/notif_eggchecker_sound.wav`).
     */
    private fun uriSonido(sonidoGuardado: String?): Uri =
        sonidoGuardado?.let { Uri.parse(it) }
            ?: Uri.parse(
                "android.resource://${context.packageName}/" +
                    "${R.raw.notif_eggchecker_sound}"
            )

    private fun atributosSonido(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun intentAbrirNotificaciones(): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ABRIR_NOTIFICACIONES, true)
        }

    private fun cancelar(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    private fun programarRepost(id: Int) {
        val solicitud = OneTimeWorkRequestBuilder<NotificacionRepostWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(
                workDataOf(NotificacionRepostWorker.CLAVE_ID to id)
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            trabajoRepost(id),
            ExistingWorkPolicy.REPLACE,
            solicitud
        )
    }

    private fun cancelarRepost(id: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(trabajoRepost(id))
    }

    private fun idsActivas(): Set<Int> =
        NotificationManagerCompat.from(context)
            .activeNotifications
            .map { it.id }
            .toSet()

    private fun puedeNotificar(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun mostrable(tipo: String, perfil: Perfil?): Boolean =
        debeMostrarNotificacion(tipo, perfil)

    private fun trabajoRepost(id: Int): String = "notif_repost_$id"

    companion object {
        /** Base del id del canal; la generación se agrega al final. */
        const val CANAL_BASE = "alertas_granja_v4"

        /** Prefijo de todos los canales de la app (para limpiar los viejos). */
        private const val PREFIJO_CANAL = "alertas_granja"

        /** Patrón de vibración de las alertas. */
        private val PATRON_VIBRACION = longArrayOf(0, 250, 200, 250)

        /** Cada cuántos minutos se revisan notificaciones nuevas. */
        const val MINUTOS_SINCRONIZACION = 15L

        private const val TRABAJO_PERIODICO = "notif_sync_periodico"
        private const val TRABAJO_SINCRONIZAR = "notif_sync_ahora"

        // Códigos extra para no chocar los PendingIntent de una misma notificación.
        private const val CODIGO_VER = 100_000
        private const val CODIGO_BORRAR = 200_000
    }
}
