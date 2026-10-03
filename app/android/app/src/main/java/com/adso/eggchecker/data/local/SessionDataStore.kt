package com.adso.eggchecker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Sesión persistida localmente: token Bearer y momento del login. */
data class Sesion(
    val token: String,
    val fechaLoginMillis: Long
) {
    /** Indica si la sesión sigue vigente (menos de 90 días). */
    fun esVigente(ahoraMillis: Long = System.currentTimeMillis()): Boolean =
        ahoraMillis - fechaLoginMillis < VIGENCIA_MILLIS

    companion object {
        /** 90 días en milisegundos. */
        const val VIGENCIA_MILLIS: Long = 90L * 24 * 60 * 60 * 1000
    }
}

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "eggchecker_session"
)

/**
 * Guarda y recupera la sesión del usuario con DataStore Preferences.
 * El token nunca se expone en texto plano al resto de la UI.
 */
class SessionDataStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("access_token")
    private val fechaLoginKey = longPreferencesKey("fecha_login")

    /** Flujo con la sesión actual o null si no hay login guardado. */
    val sesionFlow: Flow<Sesion?> = context.sessionDataStore.data.map { prefs ->
        val token = prefs[tokenKey]
        val fecha = prefs[fechaLoginKey]
        if (token.isNullOrBlank() || fecha == null) null else Sesion(token, fecha)
    }

    /**
     * Persiste el token y la fecha del login.
     *
     * @param token Token JWT devuelto por el backend.
     * @param fechaLoginMillis Marca de tiempo del inicio de sesión.
     */
    suspend fun guardarSesion(token: String, fechaLoginMillis: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[fechaLoginKey] = fechaLoginMillis
        }
    }

    /** Elimina la sesión almacenada (logout manual o expiración). */
    suspend fun limpiar() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(fechaLoginKey)
        }
    }

    /** Devuelve el token actual de forma sincrónica para el interceptor. */
    suspend fun tokenActual(): String? = sesionFlow.first()?.token
}
