package com.adso.eggchecker.ui.produccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.produccion.components.HistorialProduccion
import com.adso.eggchecker.ui.produccion.components.SelectorCamada
import com.adso.eggchecker.ui.produccion.components.SelectorFecha
import com.adso.eggchecker.ui.produccion.components.TipoHuevoCard
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

private val AVISO_FONDO = Color(0xFFFDC33B).copy(alpha = 0.2f)
private val AVISO_TEXTO = Color(0xFF92610A)

/** Pantalla de Producción Diaria (modo online, fiel al web móvil). */
@Composable
fun ProduccionScreen(viewModel: ProduccionViewModel) {
    val estado by viewModel.estado.collectAsState()

    val total = totalHuevos(estado.cantidades)
    val limite = estado.camadaSeleccionada?.cantidadActual ?: 0
    val esHoy = estado.fecha == hoyIso()
    val editable = edicionHabilitada(estado)
    val limiteAlcanzado = total >= limite
    val puedeGuardar = editable && total > 0 && total <= limite && !estado.guardando

    val registrosRecientes = estado.producciones
        .sortedByDescending { it.fechaRecoleccion }
        .take(3)
    val desde = fechaHaceDiasIso(6)
    val hoy = hoyIso()
    val enVentana = estado.producciones.filter {
        it.fechaRecoleccion >= desde && it.fechaRecoleccion <= hoy
    }
    val promedio7Dias = if (enVentana.isEmpty()) {
        0
    } else {
        enVentana.sumOf { it.totalHuevos } / enVentana.size
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Producción Diaria",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Gestión de recolección de huevos.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SuperficieTarjeta,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SelectorCamada(
                        camadas = estado.camadas,
                        seleccionada = estado.camadaSeleccionada,
                        habilitado = !estado.cargando,
                        onSeleccionar = viewModel::seleccionarCamada
                    )
                    SelectorFecha(
                        fecha = estado.fecha,
                        onCambiar = viewModel::cambiarFecha
                    )
                }
            }
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando producción…")
            }
            estado.camadas.isEmpty() -> item {
                CartelEstado(
                    texto = "No tienes camadas en etapa de producción " +
                        "(28 semanas o más)."
                )
            }
            else -> {
                if (!esHoy) {
                    item {
                        Aviso(
                            "Estás viendo una fecha anterior: los datos son " +
                                "de solo lectura. Selecciona la fecha de hoy " +
                                "para registrar."
                        )
                    }
                }

                items(TIPOS_HUEVO, key = { it.clave }) { tipo ->
                    TipoHuevoCard(
                        tipo = tipo,
                        valor = estado.cantidades[tipo.clave] ?: 0,
                        habilitado = editable,
                        limiteAlcanzado = limiteAlcanzado,
                        onIncrementar = viewModel::incrementar,
                        onDecrementar = viewModel::decrementar
                    )
                }

                if (editable && limiteAlcanzado) {
                    item {
                        Aviso(
                            "Alcanzaste la cantidad de aves actuales de la " +
                                "camada; no puedes registrar más huevos."
                        )
                    }
                }

                estado.error?.let { mensaje ->
                    item { CartelEstado(texto = mensaje, esError = true) }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Brown,
                            modifier = Modifier.weight(2f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total registrado",
                                    fontFamily = MaterialTheme.typography
                                        .titleLarge.fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Yellow
                                )
                                Text(
                                    text = if (estado.cargandoDia) {
                                        "…"
                                    } else {
                                        total.toString()
                                    },
                                    fontFamily = MaterialTheme.typography
                                        .titleLarge.fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Yellow
                                )
                            }
                        }
                        BotonApp(
                            texto = if (estado.guardando) {
                                "Guardando…"
                            } else {
                                "Guardar"
                            },
                            onClick = viewModel::guardar,
                            tipo = TipoBoton.AMARILLO,
                            cargando = estado.guardando,
                            habilitado = puedeGuardar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    HistorialProduccion(
                        registros = registrosRecientes,
                        promedio = promedio7Dias
                    )
                }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AVISO_FONDO
    ) {
        Text(
            text = texto,
            color = AVISO_TEXTO,
            fontSize = 13.6.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}
