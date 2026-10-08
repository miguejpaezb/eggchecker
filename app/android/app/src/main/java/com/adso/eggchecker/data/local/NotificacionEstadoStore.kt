package com.adso.eggchecker.data.local

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificacionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "eggchecker_notificaciones"
)

/** Ajustes locales de las notificaciones del dispositivo (no van al backend). */
data class AjustesNotificacion(
    val activas: Boolean,
    val vibracion: Boolean,
    val sonidoUri: String?
)

/**
 * Guarda los ajustes locales de notificaciones del dispositivo y los ids
 * de las notificaciones descartadas de la barra del sistema.
 *
 * Son ajustes exclusivos de la app: no se vinculan al usuario ni viajan al
 * backend, así que al desinstalar o iniciar sesión en otro teléfono vuelven
 * a sus valores por defecto.
 */
class NotificacionEstadoStore(private val context: Context) {

    private val descartadasKey = stringSetPreferencesKey("descartadas")
    private val activasKey = booleanPreferencesKey("notificaciones_activas")
    private val vibracionKey = booleanPreferencesKey("vibracion")
    private val sonidoKey = stringPreferencesKey("sonido_uri")

    /** Si las notificaciones del dispositivo están activas (por defecto sí). */
    val notificacionesFlow: Flow<Boolean> =
        context.notificacionDataStore.data.map { it[activasKey] ?: true }

    /** Si la vibración está activa (por defecto sí). */
    val vibracionFlow: Flow<Boolean> =
        context.notificacionDataStore.data.map { it[vibracionKey] ?: true }

    /** URI del sonido personalizado, o null si usa el de la app. */
    val sonidoFlow: Flow<String?> =
        context.notificacionDataStore.data.map { it[sonidoKey] }

    /** Lee los ajustes actuales de una sola vez. */
    suspend fun leerAjustes(): AjustesNotificacion {
        val prefs = context.notificacionDataStore.data.first()
        return AjustesNotificacion(
            activas = prefs[activasKey] ?: true,
            vibracion = prefs[vibracionKey] ?: true,
            sonidoUri = prefs[sonidoKey]
        )
    }

    /** Activa o desactiva las notificaciones del dispositivo. */
    suspend fun guardarNotificaciones(activas: Boolean) {
        context.notificacionDataStore.edit { prefs ->
            prefs[activasKey] = activas
        }
    }

    /** Activa o desactiva la vibración. */
    suspend fun guardarVibracion(activa: Boolean) {
        context.notificacionDataStore.edit { prefs ->
            prefs[vibracionKey] = activa
        }
    }

    /** Guarda el sonido elegido (null = sonido predeterminado de la app). */
    suspend fun guardarSonido(uri: String?) {
        context.notificacionDataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(sonidoKey)
            } else {
                prefs[sonidoKey] = uri
            }
        }
    }

    /** Ids de notificaciones descartadas y aún pendientes de reaparecer. */
    suspend fun descartadas(): Set<Int> =
        context.notificacionDataStore.data.first()[descartadasKey]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    /** Marca una notificación como descartada. */
    suspend fun agregar(id: Int) {
        context.notificacionDataStore.edit { prefs ->
            val actual = prefs[descartadasKey] ?: emptySet()
            prefs[descartadasKey] = actual + id.toString()
        }
    }

    /** Quita una notificación de la lista de descartadas. */
    suspend fun quitar(id: Int) {
        context.notificacionDataStore.edit { prefs ->
            val actual = prefs[descartadasKey] ?: emptySet()
            prefs[descartadasKey] = actual - id.toString()
        }
    }

    /** Limpia las marcas de descartadas (al cerrar sesión). */
    suspend fun limpiar() {
        context.notificacionDataStore.edit { it.remove(descartadasKey) }
    }
}
