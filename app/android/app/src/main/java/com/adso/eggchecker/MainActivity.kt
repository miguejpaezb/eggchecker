package com.adso.eggchecker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

import com.adso.eggchecker.di.viewModelFactory
import com.adso.eggchecker.navigation.AuthNavHost
import com.adso.eggchecker.ui.EstadoApp
import com.adso.eggchecker.ui.MainViewModel
import com.adso.eggchecker.ui.SplashScreen
import com.adso.eggchecker.ui.mensajes.LocalMensajeManager
import com.adso.eggchecker.ui.mensajes.MensajeHost
import com.adso.eggchecker.ui.shell.ShellScreen
import com.adso.eggchecker.ui.shell.ShellViewModel
import com.adso.eggchecker.ui.theme.Cream
import com.adso.eggchecker.ui.theme.EggCheckerTheme

/** Actividad principal: elige entre autenticación o inicio según la sesión. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manejarIntento(intent)
        // Barras transparentes con íconos oscuros: la app es de fondo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        setContent {
            EggCheckerTheme {
                EggCheckerRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntento(intent)
    }

    /** Abre el panel de notificaciones si el intent lo pide. */
    private fun manejarIntento(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ABRIR_NOTIFICACIONES, false) == true) {
            (application as EggCheckerApp)
                .container
                .abrirNotificacionesBus
                .solicitar()
        }
    }

    companion object {
        /** Extra que indica abrir el panel de notificaciones al entrar. */
        const val EXTRA_ABRIR_NOTIFICACIONES = "abrir_notificaciones"
    }
}

@Composable
private fun EggCheckerRoot() {
    val container = (LocalContext.current.applicationContext as EggCheckerApp).container
    val factory = remember { container.viewModelFactory() }
    val mainViewModel: MainViewModel = viewModel(factory = factory)
    val estado by mainViewModel.estado.collectAsState()

    CompositionLocalProvider(
        LocalMensajeManager provides container.mensajeManager
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
        ) {
            when (estado) {
                EstadoApp.Cargando -> SplashScreen()
                EstadoApp.Autenticado -> {
                    val shellViewModel: ShellViewModel = viewModel(factory = factory)
                    ShellScreen(
                        viewModel = shellViewModel,
                        factory = factory,
                        refreshBus = container.refreshBus,
                        ventaPendienteBus = container.ventaPendienteBus,
                        abrirNotificacionesBus = container.abrirNotificacionesBus
                    )
                }
                EstadoApp.NoAutenticado -> AuthNavHost(factory = factory)
            }

            MensajeHost()
        }
    }
}
