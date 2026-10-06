package com.adso.eggchecker.ui.mensajes

import androidx.compose.runtime.staticCompositionLocalOf

/** Provee el [MensajeManager] global a los hosts de toast. */
val LocalMensajeManager = staticCompositionLocalOf<MensajeManager> {
    error("MensajeManager no proporcionado")
}
