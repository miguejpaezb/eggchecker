package com.adso.eggchecker.ui.produccion.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.produccion.TipoHuevo
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.Yellow

private val VERDE_CLARO = Color(0xFF7A8D27)
private val ROSA = Color(0xFFE11D48)

/** Tarjeta de un tipo de huevo con su contador. */
@Composable
fun TipoHuevoCard(
    tipo: TipoHuevo,
    valor: Int,
    habilitado: Boolean,
    limiteAlcanzado: Boolean,
    onIncrementar: (String) -> Unit,
    onDecrementar: (String) -> Unit
) {
    val colorBorde = when (tipo.clave) {
        "aa" -> Yellow
        "a" -> VERDE_CLARO
        "b" -> Brown
        else -> ROSA
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(colorBorde)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tipo.titulo,
                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Brown
                    )
                    Text(
                        text = tipo.descripcion,
                        fontSize = 12.8.sp,
                        color = VERDE_CLARO,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BotonContador(
                        texto = "-",
                        habilitado = habilitado && valor > 0
                    ) { onDecrementar(tipo.clave) }
                    Text(
                        text = valor.toString(),
                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = Brown,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(48.dp)
                    )
                    BotonContador(
                        texto = "+",
                        habilitado = habilitado && !limiteAlcanzado
                    ) { onIncrementar(tipo.clave) }
                }
            }
        }
    }
}

@Composable
private fun BotonContador(
    texto: String,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Yellow,
        modifier = Modifier
            .size(32.dp)
            .alpha(if (habilitado) 1f else 0.45f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = habilitado, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = texto,
                color = Brown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}
