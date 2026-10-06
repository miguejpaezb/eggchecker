package com.adso.eggchecker.ui.mensajes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp

/**
 * Dibuja el mensaje flotante actual en la esquina superior derecha, con el
 * mismo zoom de los modales/popups pero naciendo de esa esquina.
 *
 * @param enModal true cuando se renderiza dentro de la ventana de un modal
 *   (queda por encima de su contenido). En la raíz se oculta mientras haya
 *   un modal abierto para no duplicar el mensaje.
 */
@Composable
fun MensajeHost(enModal: Boolean = false) {
    if (!enModal && ModalEstado.abiertos > 0) return

    val mensajeManager = LocalMensajeManager.current
    val actual by mensajeManager.actual.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, end = 12.dp)
        ) {
            AnimatedContent(
                targetState = actual,
                transitionSpec = {
                    (
                        fadeIn(tween(160)) + scaleIn(
                            initialScale = 0.8f,
                            transformOrigin = TransformOrigin(1f, 0f),
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        ) togetherWith (
                        fadeOut(tween(140)) + scaleOut(
                            targetScale = 0.9f,
                            transformOrigin = TransformOrigin(1f, 0f),
                            animationSpec = tween(140)
                        )
                        ) using SizeTransform(clip = false)
                },
                label = "mensajeToast"
            ) { mensaje ->
                if (mensaje != null) {
                    MensajeToast(
                        mensaje = mensaje,
                        onCerrar = { mensajeManager.descartar() }
                    )
                }
            }
        }
    }
}
