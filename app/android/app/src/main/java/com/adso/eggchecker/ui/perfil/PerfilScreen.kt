package com.adso.eggchecker.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.perfil.components.AyudaSeccion
import com.adso.eggchecker.ui.perfil.components.EditarPerfilSeccion
import com.adso.eggchecker.ui.perfil.components.NotificacionesSeccion
import com.adso.eggchecker.ui.perfil.components.PerfilHistoricoCard
import com.adso.eggchecker.ui.perfil.components.PerfilMenuCard
import com.adso.eggchecker.ui.perfil.components.PerfilUsuarioCard
import com.adso.eggchecker.ui.perfil.components.PlanSeccion
import com.adso.eggchecker.ui.perfil.components.SeguridadSeccion
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted

/** Pantalla de Perfil (modo online, fiel al web). */
@Composable
fun PerfilScreen(viewModel: PerfilViewModel) {
    val estado by viewModel.estado.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Gestiona tu cuenta, tu granja y tus preferencias.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando perfil…")
            }
            estado.error != null -> item {
                CartelEstado(texto = estado.error.orEmpty(), esError = true)
            }
            estado.perfil != null -> {
                val perfil = estado.perfil!!
                item {
                    PerfilUsuarioCard(
                        nombre = perfil.nombreCompleto,
                        correo = perfil.correoElectronico,
                        plan = perfil.planSuscripcion
                    )
                }
                item {
                    PerfilHistoricoCard(
                        totalHuevos = perfil.totalHuevosProducidos,
                        totalAves = perfil.totalAvesGestionadas,
                        fechaRegistro = perfil.fechaRegistro
                    )
                }
                item {
                    PerfilMenuCard(
                        activa = estado.seccion,
                        onSeleccionar = viewModel::cambiarSeccion
                    )
                }
                item {
                    when (estado.seccion) {
                        "notificaciones" -> NotificacionesSeccion(
                            perfil = perfil,
                            notificacionesActivas = estado.notificacionesActivas,
                            vibracion = estado.vibracion,
                            sonidoPersonalizado = estado.sonidoPersonalizado,
                            onGuardar = { a, b, c, d, onExito, onError ->
                                viewModel.guardarNotificaciones(
                                    a, b, c, d, onExito, onError
                                )
                            },
                            onCambiarNotificaciones =
                                viewModel::cambiarNotificaciones,
                            onCambiarVibracion = viewModel::cambiarVibracion,
                            onCambiarSonido = viewModel::cambiarSonido,
                            onReproducirSonido = viewModel::reproducirSonido
                        )
                        "seguridad" -> SeguridadSeccion(
                            onGuardar = { actual, nueva, onExito, onError ->
                                viewModel.cambiarContrasena(
                                    actual, nueva, onExito, onError
                                )
                            }
                        )
                        "plan" -> PlanSeccion(perfil = perfil)
                        "ayuda" -> AyudaSeccion()
                        else -> EditarPerfilSeccion(
                            perfil = perfil,
                            onGuardar = { datos, onExito, onError ->
                                viewModel.guardarPerfil(datos, onExito, onError)
                            }
                        )
                    }
                }
            }
        }
    }
}
