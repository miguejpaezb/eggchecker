package com.adso.eggchecker.ui.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

/** Color de acento de una tarjeta KPI. */
enum class TonoKpi { AMARILLO, MARRON, VERDE, ROJO }

private val ROJO_ACENTO = Color(0xFFEF4444)
private val ROJO_VALOR = Color(0xFFDC2626)

/** Datos de una tarjeta KPI. */
data class Kpi(
    val titulo: String,
    val valor: String,
    val detalle: String,
    @DrawableRes val icono: Int,
    val tono: TonoKpi
)

/** Tarjeta de indicador (KPI) del dashboard. */
@Composable
fun KpiCard(kpi: Kpi, modifier: Modifier = Modifier) {
    val acento = when (kpi.tono) {
        TonoKpi.AMARILLO -> Yellow
        TonoKpi.MARRON -> Brown
        TonoKpi.VERDE -> Green
        TonoKpi.ROJO -> ROJO_ACENTO
    }
    val colorValor = if (kpi.tono == TonoKpi.ROJO) ROJO_VALOR else Dark
    val colorDetalle = if (kpi.tono == TonoKpi.ROJO) ROJO_ACENTO else Green

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kpi.titulo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
                Text(
                    text = kpi.valor,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colorValor,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = kpi.detalle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorDetalle,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(acento.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(kpi.icono),
                    contentDescription = null,
                    tint = acento,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
