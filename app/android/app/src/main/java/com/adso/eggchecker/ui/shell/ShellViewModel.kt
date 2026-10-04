package com.adso.eggchecker.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.remote.ApiException
import com.adso.eggchecker.data.repository.AuthRepository
import com.adso.eggchecker.domain.model.Usuario

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Perfil del usuario y acciones del shell principal (topbar/drawer). */
class ShellViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Perfil del usuario leído desde Room. */
    val usuario: StateFlow<Usuario?> = authRepository.usuarioFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch { refrescar() }
    }

    /** Refresca el perfil desde el backend (pull-to-refresh). */
    suspend fun refrescar() {
        val resultado = authRepository.sincronizarPerfil()
        if ((resultado.exceptionOrNull() as? ApiException)?.codigo == 401) {
            authRepository.cerrarSesion()
        }
    }

    /** Cierra la sesión de forma manual. */
    fun cerrarSesion() {
        viewModelScope.launch { authRepository.cerrarSesion() }
    }
}
