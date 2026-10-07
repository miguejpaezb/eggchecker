package com.adso.eggchecker.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.local.NotificacionEstadoStore
import com.adso.eggchecker.data.repository.AuthRepository
import com.adso.eggchecker.data.repository.PerfilRepository
import com.adso.eggchecker.domain.model.Perfil
import com.adso.eggchecker.notifications.NotificacionGestor
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Datos del formulario "Editar Perfil". */
data class DatosPerfilForm(
    val nombreCompleto: String,
    val correoElectronico: String,
    val telefono: String,
    val nombreGranja: String,
    val contrasenaActual: String
)

/** Estado de la pantalla de perfil. */
data class PerfilUiState(
    val perfil: Perfil? = null,
    val seccion: String = "editar",
    val cargando: Boolean = true,
    val error: String? = null,
    val notificacionesActivas: Boolean = true,
    val vibracion: Boolean = true,
    val sonidoPersonalizado: String? = null
)

/** Gestiona el perfil del usuario y las acciones de sus secciones. */
class PerfilViewModel(
    private val perfilRepository: PerfilRepository,
    private val authRepository: AuthRepository,
    private val mensajeManager: MensajeManager,
    private val notificacionEstadoStore: NotificacionEstadoStore,
    private val notificacionGestor: NotificacionGestor
) : ViewModel() {

    private val _estado = MutableStateFlow(PerfilUiState())

    /** Estado observable de la pantalla de perfil. */
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            notificacionEstadoStore.notificacionesFlow.collect { valor ->
                _estado.update { it.copy(notificacionesActivas = valor) }
            }
        }
        viewModelScope.launch {
            notificacionEstadoStore.vibracionFlow.collect { valor ->
                _estado.update { it.copy(vibracion = valor) }
            }
        }
        viewModelScope.launch {
            notificacionEstadoStore.sonidoFlow.collect { valor ->
                _estado.update { it.copy(sonidoPersonalizado = valor) }
            }
        }
    }

    /** Carga el perfil desde el backend. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            perfilRepository.obtenerPerfil()
                .onSuccess { perfil ->
                    _estado.update { it.copy(cargando = false, perfil = perfil) }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.message
                                ?: "No se pudo cargar el perfil"
                        )
                    }
                }
        }
    }

    /** Cambia la sección activa del perfil. */
    fun cambiarSeccion(seccion: String) {
        _estado.update { it.copy(seccion = seccion) }
    }

    /** Guarda los datos personales y de la granja. */
    fun guardarPerfil(
        datos: DatosPerfilForm,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            val correoActual = _estado.value.perfil
                ?.correoElectronico
                ?.lowercase()
                ?: ""
            val nuevoCorreo = datos.correoElectronico.trim().lowercase()
            val cambioCorreo = nuevoCorreo != correoActual

            perfilRepository.actualizarPerfil(
                nombreCompleto = datos.nombreCompleto.trim(),
                correoElectronico = nuevoCorreo,
                telefono = datos.telefono.trim().ifBlank { null },
                nombreGranja = datos.nombreGranja.trim().ifBlank { null },
                contrasenaActual = if (cambioCorreo) {
                    datos.contrasenaActual
                } else {
                    null
                }
            )
                .onSuccess { actualizado ->
                    if (cambioCorreo) {
                        mensajeManager.exito(
                            "Correo actualizado. Inicia sesión de nuevo"
                        )
                        authRepository.cerrarSesion()
                    } else {
                        mensajeManager.exito("Cambios guardados")
                        _estado.update { it.copy(perfil = actualizado) }
                        onExito()
                    }
                }
                .onFailure { error ->
                    mensajeManager.error(error.message ?: "No se pudo guardar")
                    onError()
                }
        }
    }

    /** Guarda las preferencias de alertas. */
    fun guardarNotificaciones(
        produccionBaja: Boolean,
        stockBajo: Boolean,
        vacunacion: Boolean,
        resumenSemanal: Boolean,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            perfilRepository.actualizarNotificaciones(
                produccionBaja = produccionBaja,
                stockBajo = stockBajo,
                vacunacion = vacunacion,
                resumenSemanal = resumenSemanal
            )
                .onSuccess { actualizado ->
                    mensajeManager.exito("Preferencias guardadas")
                    _estado.update { it.copy(perfil = actualizado) }
                    onExito()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudieron guardar"
                    )
                    onError()
                }
        }
    }

    /** Cambia la contraseña del usuario. */
    fun cambiarContrasena(
        contrasenaActual: String,
        contrasenaNueva: String,
        onExito: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            perfilRepository.cambiarContrasena(contrasenaActual, contrasenaNueva)
                .onSuccess { mensaje ->
                    mensajeManager.exito(mensaje)
                    onExito()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo cambiar la contraseña"
                    )
                    onError()
                }
        }
    }

    /** Activa o desactiva las notificaciones del dispositivo. */
    fun cambiarNotificaciones(activas: Boolean) {
        viewModelScope.launch {
            notificacionEstadoStore.guardarNotificaciones(activas)
            notificacionGestor.sincronizarAjustes()
            mensajeManager.exito(
                if (activas) {
                    "Notificaciones activadas"
                } else {
                    "Notificaciones desactivadas"
                }
            )
        }
    }

    /** Activa o desactiva la vibración. */
    fun cambiarVibracion(activa: Boolean) {
        viewModelScope.launch {
            notificacionEstadoStore.guardarVibracion(activa)
            notificacionGestor.sincronizarAjustes()
            mensajeManager.exito(
                if (activa) "Vibración activada" else "Vibración desactivada"
            )
        }
    }

    /** Cambia el sonido (null = predeterminado de la app). */
    fun cambiarSonido(uri: String?) {
        viewModelScope.launch {
            notificacionEstadoStore.guardarSonido(uri)
            notificacionGestor.sincronizarAjustes()
            mensajeManager.exito(
                if (uri == null) {
                    "Sonido predeterminado"
                } else {
                    "Sonido actualizado"
                }
            )
        }
    }

    /** Reproduce el sonido de notificación actual (vista previa). */
    fun reproducirSonido() {
        viewModelScope.launch { notificacionGestor.reproducirSonido() }
    }
}
