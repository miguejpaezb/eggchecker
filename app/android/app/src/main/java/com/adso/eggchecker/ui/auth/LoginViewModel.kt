package com.adso.eggchecker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.AuthRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Datos del formulario de inicio de sesión. */
data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

/** Maneja el formulario de login delegando en el repositorio. */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())

    /** Estado observable de la pantalla de login. */
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    /** Actualiza el correo escrito por el usuario. */
    fun onCorreoChange(valor: String) {
        _estado.update { it.copy(correo = valor, error = null) }
    }

    /** Actualiza la contraseña escrita por el usuario. */
    fun onContrasenaChange(valor: String) {
        _estado.update { it.copy(contrasena = valor, error = null) }
    }

    /** Envía las credenciales al backend. */
    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.correo.isBlank() || actual.contrasena.isBlank()) {
            _estado.update {
                it.copy(error = "Ingresa tu correo y contraseña")
            }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            authRepository
                .iniciarSesion(actual.correo.trim(), actual.contrasena)
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.message ?: "No se pudo iniciar sesión"
                        )
                    }
                }
            _estado.update { it.copy(cargando = false) }
        }
    }
}
