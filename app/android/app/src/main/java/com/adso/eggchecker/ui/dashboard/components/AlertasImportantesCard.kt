package com.adso.eggchecker.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.AlertaDashboard
import com.adso.eggchecker.ui.reportes.formatoNumero
import com.adso.eggchecker.ui.theme.TextMuted

private val ROJO_TITULO = Color(0xFFE11D48)
private val ROJO_NOMBRE = Color(0xFFB91C1C)
private val ROJO_TEXTO = Color(0xFFDC2626)
private val ROJO_FONDO = Color(0xFFFEF2F2)
private val ROJO_BORDE = Color(0xFFFEE2E2)

// Alto máximo de la lista: tres tarjetas visibles, el resto se desliza.
private val ALTO_MAX_ALERTAS = 240.dp

/** Tarjeta "Alertas importantes": insumos bajo su umbral mínimo. */
@Composable
fun AlertasImportantesCard(
    alertas: List<AlertaDashboard>,
    onVerDetalle: (Int) -> Unit
) {
    TarjetaDashboard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notificacion),
                contentDescription = null,
                tint = ROJO_TITULO,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Alertas Importantes (${alertas.size})",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = ROJO_TITULO,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (alertas.isEmpty()) {
            Text(
                text = "No hay insumos bajo el umbral mínimo.",
                color = TextMuted
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = ALTO_MAX_ALERTAS)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                alertas.forEach { alerta ->
                    TarjetaAlerta(alerta = alerta, onVerDetalle = onVerDetalle)
                }
            }
        }
    }
}

@Composable
private fun TarjetaAlerta(
    alerta: AlertaDashboard,
    onVerDetalle: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ROJO_FONDO,
        border = BorderStroke(1.dp, ROJO_BORDE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notificacion),
                    contentDescription = null,
                    tint = ROJO_TITULO,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = alerta.nombreInsumo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ROJO_NOMBRE
                    )
                    Text(
                        text = "Quedan ${formatoNumero(alerta.stockActual, 2)} " +
                            "(Mínimo: ${formatoNumero(alerta.umbralMinimo, 2)}) · " +
                            alerta.categoria,
                        fontSize = 13.sp,
                        color = ROJO_TEXTO
                    )
                }
            }
            Text(
                text = "Detalles",
                color = ROJO_TEXTO,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onVerDetalle(alerta.idInsumo) }
            )
        }
    }
}
