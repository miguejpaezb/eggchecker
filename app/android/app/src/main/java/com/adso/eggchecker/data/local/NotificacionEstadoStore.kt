package com.adso.eggchecker.data.local

import android.content.Context

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.first

private val Context.notificacionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "eggchecker_notificaciones"
)

/**
 * Guarda los ids de las notificaciones que el usuario descartó de la barra
 * del sistema, para respetar la hora de espera antes de volver a mostrarlas.
 */
class NotificacionEstadoStore(private val context: Context) {

    private val descartadasKey = stringSetPreferencesKey("descartadas")

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

    /** Limpia todas las marcas (al cerrar sesión). */
    suspend fun limpiar() {
        context.notificacionDataStore.edit { it.remove(descartadasKey) }
    }
}
