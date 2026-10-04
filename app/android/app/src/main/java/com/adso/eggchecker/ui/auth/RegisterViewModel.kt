package com.adso.eggchecker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.AuthRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val CORREO_VALIDO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Datos y errores del formulario de registro. */
data class RegisterUiState(
    val nombreCompleto: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmar: String = "",
    val errores: Map<String, String> = emptyMap(),
    val cargando: Boolean = false,
    val error: String? = null
)

/** Maneja el formulario de registro con las mismas validaciones del web. */
class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(RegisterUiState())

    /** Estado observable de la pantalla de registro. */
    val estado: StateFlow<RegisterUiState> = _estado.asStateFlow()

    /** Actualiza el nombre completo. */
    fun onNombreChange(valor: String) = actualizar { copy(nombreCompleto = valor) }

    /** Actualiza el correo electrónico. */
    fun onCorreoChange(valor: String) = actualizar { copy(correo = valor) }

    /** Actualiza la contraseña. */
    fun onContrasenaChange(valor: String) = actualizar { copy(contrasena = valor) }

    /** Actualiza la confirmación de contraseña. */
    fun onConfirmarChange(valor: String) = actualizar { copy(confirmar = valor) }

    private fun actualizar(transform: RegisterUiState.() -> RegisterUiState) {
        _estado.update { it.transform().copy(errores = emptyMap(), error = null) }
    }

    /**
     * Valida y envía el registro al backend.
     *
     * @param onExitoso Callback con el correo registrado para volver al login.
     */
    fun registrar(onExitoso: (String) -> Unit) {
        val actual = _estado.value
        val errores = validar(actual)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            authRepository
                .registrar(
                    actual.nombreCompleto.trim(),
                    actual.correo.trim(),
                    actual.contrasena
                )
                .onSuccess { onExitoso(actual.correo.trim()) }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.message ?: "No se pudo crear la cuenta"
                        )
                    }
                }
            _estado.update { it.copy(cargando = false) }
        }
    }

    /** Aplica las validaciones del RegisterForm.jsx del frontend. */
    private fun validar(estado: RegisterUiState): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        if (estado.nombreCompleto.trim().length < 2) {
            errores["nombre"] = "Ingresa tu nombre completo"
        }
        if (!CORREO_VALIDO.matches(estado.correo)) {
            errores["correo"] = "Ingresa un correo electrónico válido"
        }
        val contrasena = estado.contrasena
        when {
            contrasena.length < 8 ->
                errores["contrasena"] = "Debe tener al menos 8 caracteres"
            !contrasena.any { it.isUpperCase() } ->
                errores["contrasena"] = "Debe incluir al menos una mayúscula"
            !contrasena.any { it.isDigit() } ->
                errores["contrasena"] = "Debe incluir al menos un número"
            contrasena.none { !it.isLetterOrDigit() } ->
                errores["contrasena"] = "Debe incluir al menos un símbolo"
        }
        if (estado.confirmar != contrasena) {
            errores["confirmar"] = "Las contraseñas no coinciden"
        }
        return errores
    }
}
