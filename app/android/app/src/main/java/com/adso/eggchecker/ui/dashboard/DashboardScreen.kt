package com.adso.eggchecker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.Dashboard
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.dashboard.components.AlertasImportantesCard
import com.adso.eggchecker.ui.dashboard.components.Kpi
import com.adso.eggchecker.ui.dashboard.components.KpiCarrusel
import com.adso.eggchecker.ui.dashboard.components.PedidosRecientesCard
import com.adso.eggchecker.ui.dashboard.components.ProduccionSemanalChart
import com.adso.eggchecker.ui.dashboard.components.ResumenSemanalCard
import com.adso.eggchecker.ui.dashboard.components.TarjetaDashboard
import com.adso.eggchecker.ui.dashboard.components.TonoKpi
import com.adso.eggchecker.ui.reportes.formatoNumero
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted

/** Pantalla del Dashboard (modo online, fiel al web móvil). */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    nombreUsuario: String,
    onNuevaProduccion: () -> Unit,
    onVerInsumo: (Int) -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Bienvenido de nuevo, $nombreUsuario.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            BotonApp(
                texto = "Nueva Producción",
                onClick = onNuevaProduccion,
                tipo = TipoBoton.PRIMARIO,
                icono = painterResource(R.drawable.ic_add),
                modifier = Modifier.fillMaxWidth()
            )
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando dashboard…")
            }
            estado.error != null -> item {
                Column {
                    CartelEstado(texto = estado.error.orEmpty(), esError = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    BotonApp(
                        texto = "Reintentar",
                        onClick = viewModel::cargar,
                        tipo = TipoBoton.PRIMARIO
                    )
                }
            }
            estado.datos != null -> {
                val datos = estado.datos!!
                item { KpiCarrusel(kpis = construirKpis(datos)) }
                item {
                    TarjetaDashboard(titulo = "Producción semanal") {
                        ProduccionSemanalChart(serie = datos.semana.serie)
                    }
                }
                item { ResumenSemanalCard(semana = datos.semana) }
                item {
                    AlertasImportantesCard(
                        alertas = datos.alertas,
                        onVerDetalle = onVerInsumo
                    )
                }
                item { PedidosRecientesCard(pedidos = datos.pedidosRecientes) }
            }
        }
    }
}

private fun construirKpis(datos: Dashboard): List<Kpi> = listOf(
    Kpi(
        titulo = "Producción Hoy",
        valor = formatoNumero(datos.produccionHoy.toDouble()),
        detalle = textoVariacion(datos.variacionProduccion),
        icono = R.drawable.ic_produccion,
        tono = TonoKpi.AMARILLO
    ),
    Kpi(
        titulo = "Aves activas",
        valor = formatoNumero(datos.avesActivas.toDouble()),
        detalle = "En total",
        icono = R.drawable.ic_camadas,
        tono = TonoKpi.MARRON
    ),
    Kpi(
        titulo = "Pendientes",
        valor = formatoNumero(datos.pedidosPendientes.toDouble()),
        detalle = "Pedidos por entregar",
        icono = R.drawable.ic_ventas,
        tono = TonoKpi.VERDE
    ),
    Kpi(
        titulo = "Alertas",
        valor = formatoNumero(datos.alertasCount.toDouble()),
        detalle = "Atención requerida",
        icono = R.drawable.ic_notificacion,
        tono = TonoKpi.ROJO
    )
)
