package com.adso.eggchecker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.adso.eggchecker.data.repository.DashboardRepository
import com.adso.eggchecker.data.sync.RefreshBus
import com.adso.eggchecker.domain.model.Dashboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla del dashboard. */
data class DashboardUiState(
    val datos: Dashboard? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

/** Carga los indicadores agregados del dashboard. */
class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val refreshBus: RefreshBus
) : ViewModel() {

    private val _estado = MutableStateFlow(DashboardUiState())

    /** Estado observable del dashboard. */
    val estado: StateFlow<DashboardUiState> = _estado.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            refreshBus.eventos.collect { cargar() }
        }
    }

    /** Recarga los indicadores desde el backend. */
    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            dashboardRepository.obtenerDashboard()
                .onSuccess { datos ->
                    _estado.update {
                        it.copy(cargando = false, datos = datos)
                    }
                }
                .onFailure { error ->
                    _estado.update {
                        it.copy(
                            cargando = false,
                            datos = null,
                            error = error.message
                                ?: "No se pudo cargar el dashboard"
                        )
                    }
                }
        }
    }
}
