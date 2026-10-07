package com.adso.eggchecker.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

import com.adso.eggchecker.ui.MainViewModel
import com.adso.eggchecker.ui.auth.LoginViewModel
import com.adso.eggchecker.ui.auth.RegisterViewModel
import com.adso.eggchecker.ui.camadas.CamadasViewModel
import com.adso.eggchecker.ui.clientes.ClientesViewModel
import com.adso.eggchecker.ui.inventario.InventarioViewModel
import com.adso.eggchecker.ui.produccion.ProduccionViewModel
import com.adso.eggchecker.ui.reportes.ReportesViewModel
import com.adso.eggchecker.ui.shell.NotificacionesViewModel
import com.adso.eggchecker.ui.shell.ShellViewModel
import com.adso.eggchecker.ui.ventas.VentasViewModel

/**
 * Fábrica de ViewModels del contenedor manual (sin Hilt).
 * Registra cada ViewModel con su repositorio correspondiente.
 */
fun AppContainer.viewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer { MainViewModel(authRepository) }
    initializer { LoginViewModel(authRepository) }
    initializer { RegisterViewModel(authRepository) }
    initializer { ShellViewModel(authRepository) }
    initializer { NotificacionesViewModel(notificacionRepository, mensajeManager) }
    initializer {
        CamadasViewModel(camadaRepository, refreshBus, mensajeManager)
    }
    initializer {
        ProduccionViewModel(
            camadaRepository = camadaRepository,
            produccionRepository = produccionRepository,
            refreshBus = refreshBus,
            mensajeManager = mensajeManager
        )
    }
    initializer {
        InventarioViewModel(
            insumoRepository = insumoRepository,
            categoriaRepository = categoriaRepository,
            refreshBus = refreshBus,
            mensajeManager = mensajeManager
        )
    }
    initializer {
        ClientesViewModel(
            clienteRepository = clienteRepository,
            refreshBus = refreshBus,
            mensajeManager = mensajeManager
        )
    }
    initializer {
        VentasViewModel(
            ventaRepository = ventaRepository,
            clienteRepository = clienteRepository,
            refreshBus = refreshBus,
            mensajeManager = mensajeManager,
            ventaPendienteBus = ventaPendienteBus
        )
    }
    initializer {
        ReportesViewModel(
            reporteRepository = reporteRepository,
            camadaRepository = camadaRepository,
            pdfStorage = pdfStorage,
            refreshBus = refreshBus,
            mensajeManager = mensajeManager
        )
    }
}
