package com.adso.eggchecker.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.NotificacionRepository
import com.adso.eggchecker.domain.model.Notificacion
import com.adso.eggchecker.notifications.NotificacionGestor
import com.adso.eggchecker.ui.mensajes.MensajeManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del panel de notificaciones. */
data class NotificacionesUiState(
    val notificaciones: List<Notificacion> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
) {
    /** Hay alguna notificación sin leer (para el punto de la campanita). */
    val hayNoLeidas: Boolean get() = notificaciones.any { !it.leida }
}

/** Carga y gestiona las notificaciones del usuario. */
class NotificacionesViewModel(
    private val notificacionRepository: NotificacionRepository,
    private val mensajeManager: MensajeManager,
    private val notificacionGestor: NotificacionGestor
) : ViewModel() {

    private val _estado = MutableStateFlow(NotificacionesUiState())

    /** Estado observable del panel de notificaciones. */
    val estado: StateFlow<NotificacionesUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    /** Recarga la lista desde el backend. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            notificacionRepository.listar()
                .onSuccess { lista ->
                    _estado.update {
                        it.copy(cargando = false, notificaciones = lista)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = error.message ?: "No se pudieron cargar"
                        )
                    }
                }
        }
    }

    /** Marca una notificación como leída. */
    fun marcarLeida(idNotificacion: Int) {
        viewModelScope.launch {
            notificacionRepository.marcarLeida(idNotificacion)
                .onSuccess {
                    notificacionGestor.quitarDescartada(idNotificacion)
                    cargar()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo marcar como leída"
                    )
                }
        }
    }

    /** Marca todas las notificaciones como leídas. */
    fun marcarTodas() {
        viewModelScope.launch {
            notificacionRepository.marcarTodas()
                .onSuccess {
                    notificacionGestor.cancelarTodas()
                    cargar()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudieron marcar como leídas"
                    )
                }
        }
    }

    /** Elimina una notificación. */
    fun eliminar(idNotificacion: Int) {
        viewModelScope.launch {
            notificacionRepository.eliminar(idNotificacion)
                .onSuccess {
                    notificacionGestor.quitarDescartada(idNotificacion)
                    cargar()
                }
                .onFailure { error ->
                    mensajeManager.error(
                        error.message ?: "No se pudo eliminar"
                    )
                }
        }
    }
}
