package com.adso.eggchecker.ui.reportes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

import com.adso.eggchecker.ui.components.CartelEstado
import com.adso.eggchecker.ui.reportes.components.CuentasCard
import com.adso.eggchecker.ui.reportes.components.FiltroPeriodoCard
import com.adso.eggchecker.ui.reportes.components.ResumenRecogidaCard
import com.adso.eggchecker.ui.reportes.components.SaludBajasCard
import com.adso.eggchecker.ui.reportes.components.UsoComidaCard
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.TextMuted

/** Pantalla de Reportes (modo online, fiel al web). */
@Composable
fun ReportesScreen(viewModel: ReportesViewModel) {
    val estado by viewModel.estado.collectAsState()
    val context = LocalContext.current

    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            viewModel.descargarPdf()
        } else {
            viewModel.avisarPermisoDenegado()
        }
    }

    val descargar = {
        val necesitaPermiso = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        if (necesitaPermiso) {
            permisoLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.descargarPdf()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Reportes",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 30.sp,
                color = Brown
            )
            Text(
                text = "Rentabilidad, producción y salud de tu granja.",
                fontSize = 15.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            FiltroPeriodoCard(
                camadas = estado.camadas,
                camadaId = estado.camadaId,
                preset = estado.preset,
                desde = estado.desde,
                hasta = estado.hasta,
                descargando = estado.descargando,
                onCamada = viewModel::cambiarCamada,
                onPreset = viewModel::cambiarPreset,
                onDesde = viewModel::cambiarDesde,
                onHasta = viewModel::cambiarHasta,
                onDescargar = descargar
            )
        }

        when {
            estado.cargando -> item {
                CartelEstado(texto = "Cargando reporte…")
            }
            estado.error != null -> item {
                CartelEstado(texto = estado.error.orEmpty(), esError = true)
            }
            estado.reporte != null -> {
                val reporte = estado.reporte!!
                item {
                    CuentasCard(
                        ventas = reporte.ventas.ingresoTotal,
                        gastos = reporte.gastos.total,
                        ganancia = reporte.ganancia.valor
                    )
                }
                item {
                    SaludBajasCard(
                        gallinasPerdidas = reporte.salud.gallinasPerdidas,
                        causaPrincipal = reporte.salud.causaPrincipal,
                        vacunacionAlDia = reporte.salud.vacunacionAlDia
                    )
                }
                item {
                    ResumenRecogidaCard(
                        produccion = reporte.produccion,
                        camadaNombre = reporte.camada?.nombreCamada
                    )
                }
                item {
                    UsoComidaCard(
                        kgUsados = reporte.alimento.kgUsados,
                        promedioDiario = reporte.alimento.promedioDiario,
                        dias = diasDelRango(estado.desde, estado.hasta)
                    )
                }
            }
        }
    }
}
