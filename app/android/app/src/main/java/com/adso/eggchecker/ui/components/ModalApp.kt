package com.adso.eggchecker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.mensajes.MensajeHost
import com.adso.eggchecker.ui.mensajes.ModalEstado

/**
 * Diálogo centrado con el mismo zoom que los popups de la app.
 * El fondo se oscurece con fade suave y la tarjeta crece desde el centro.
 *
 * @param abierto Si el modal debe mostrarse.
 * @param titulo Título del modal.
 * @param onCerrar Acción al cerrar (botón ×, fondo o atrás).
 * @param contenido Campos/acciones del modal.
 */
@Composable
fun ModalApp(
    abierto: Boolean,
    titulo: String,
    onCerrar: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val visibilidad = remember { MutableTransitionState(false) }
    visibilidad.targetState = abierto

    if (visibilidad.currentState || visibilidad.targetState) {
        DisposableEffect(Unit) {
            ModalEstado.registrar()
            onDispose { ModalEstado.quitar() }
        }
        val interaccion = remember { MutableInteractionSource() }
        Dialog(
            onDismissRequest = onCerrar,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AnimatedVisibility(
                visibleState = visibilidad,
                enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180, easing = FastOutLinearInEasing))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = interaccion,
                                indication = null,
                                onClick = onCerrar
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val transicion = updateTransition(abierto, "zoomModal")
                        val escala by transicion.animateFloat(
                            transitionSpec = {
                                if (targetState) {
                                    spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                } else {
                                    tween(140)
                                }
                            },
                            label = "escalaModal"
                        ) { visible -> if (visible) 1f else 0.85f }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SuperficieTarjeta,
                            shadowElevation = 20.dp,
                            modifier = Modifier
                                .widthIn(max = 448.dp)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = escala
                                    scaleY = escala
                                    transformOrigin = TransformOrigin.Center
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = titulo,
                                        fontFamily = MaterialTheme.typography
                                            .titleLarge.fontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Brown,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(onClick = onCerrar),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "×",
                                            color = Placeholder,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                contenido()
                            }
                        }
                    }

                    MensajeHost(enModal = true)
                }
            }
        }
    }
}
