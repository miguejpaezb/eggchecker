package com.adso.eggchecker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.AuthRepository

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado global de la app según la sesión guardada. */
sealed interface EstadoApp {
    /** Aún se está revisando la sesión almacenada. */
    data object Cargando : EstadoApp

    /** Hay una sesión vigente: mostrar inicio. */
    data object Autenticado : EstadoApp

    /** No hay sesión: mostrar autenticación. */
    data object NoAutenticado : EstadoApp
}

/**
 * Decide la pantalla inicial observando la sesión persistida.
 * Reacciona solo al login y logout; no requiere refrescos manuales.
 */
class MainViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Estado de sesión derivado del token y su fecha de login. */
    val estado: StateFlow<EstadoApp> = authRepository.sesionFlow
        .map { sesion ->
            if (sesion != null && sesion.esVigente()) {
                EstadoApp.Autenticado
            } else {
                EstadoApp.NoAutenticado
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EstadoApp.Cargando
        )

    init {
        // Limpia la sesión si superó los 90 días.
        viewModelScope.launch { authRepository.haySesionVigente() }
    }
}
