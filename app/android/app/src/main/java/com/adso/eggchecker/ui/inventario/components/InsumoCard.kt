package com.adso.eggchecker.ui.inventario.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.inventario.formatearCantidad
import com.adso.eggchecker.ui.inventario.nivelStock
import com.adso.eggchecker.ui.inventario.porcentajeBarra
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Green
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

private val ROJO = Color(0xFFE11D48)
private val GRIS = Color(0xFF9CA3AF)
private val BARRA_FONDO = Color(0xFFE5E7EB)
private val SUPERFICIE_ALERTA = Color(0xFFFCEDF0)
private val AVISO_TEXTO = Color(0xFF92610A)

/**
 * Tarjeta de un insumo con su barra de nivel de stock.
 *
 * @param categoria Nombre de la categoría del insumo.
 * @param acciones Menú de acciones anclado en la tarjeta.
 */
@Composable
fun InsumoCard(
    insumo: Insumo,
    categoria: String,
    acciones: @Composable () -> Unit
) {
    val nivel = nivelStock(insumo.stockActual, insumo.umbralMinimo)
    val porcentaje = porcentajeBarra(insumo.stockActual, insumo.umbralMinimo)
    val sinStock = insumo.stockActual <= 0.0
    val colorBarra = when (nivel) {
        "optimo" -> Green
        "bajo" -> Yellow
        "critico" -> ROJO
        else -> GRIS
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (sinStock) SUPERFICIE_ALERTA else SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insumo.nombreInsumo,
                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Brown
                    )
                    Text(
                        text = "$categoria - ${insumo.unidadMedida}",
                        fontSize = 13.6.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!insumo.activo) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Yellow.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = "Suspendido",
                                color = AVISO_TEXTO,
                                fontSize = 11.2.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
                    acciones()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stock: ${formatearCantidad(insumo.stockActual)} " +
                        insumo.unidadMedida,
                    fontSize = 13.6.sp,
                    color = TextMuted
                )
                Text(
                    text = "Mínimo: ${formatearCantidad(insumo.umbralMinimo)} " +
                        insumo.unidadMedida,
                    fontSize = 13.6.sp,
                    color = TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BARRA_FONDO)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(porcentaje / 100f)
                        .background(colorBarra)
                )
            }
        }
    }
}
