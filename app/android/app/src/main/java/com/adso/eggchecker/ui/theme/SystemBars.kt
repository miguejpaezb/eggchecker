package com.adso.eggchecker.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

/**
 * Ajusta el color de los íconos de la barra de estado.
 *
 * @param oscuros true para íconos oscuros (fondo claro), false para claros
 *   (fondo marrón del topbar).
 */
@Composable
fun EstiloIconosBarraEstado(oscuros: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(oscuros) {
        val window = activity?.window
        val controller = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        controller?.isAppearanceLightStatusBars = oscuros
        onDispose {
            // Al salir del shell volvemos al estilo claro de las pantallas auth.
            controller?.isAppearanceLightStatusBars = true
        }
    }
}

private fun Context.findActivity(): Activity? {
    var contexto: Context? = this
    while (contexto is ContextWrapper) {
        if (contexto is Activity) return contexto
        contexto = contexto.baseContext
    }
    return null
}
