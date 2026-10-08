package com.adso.eggchecker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Selector compacto tipo pastilla con etiqueta y valor desplegable.
 *
 * @param opciones Pares `valor to etiqueta`.
 */
@Composable
fun SelectorPill(
    etiqueta: String,
    valorTexto: String,
    opciones: List<Pair<String, String>>,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SuperficieTarjeta,
        border = BorderStroke(1.dp, Border),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { abierto = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = etiqueta,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "$valorTexto ▾",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brown
                )
            }
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false }
            ) {
                opciones.forEach { (valor, texto) ->
                    DropdownMenuItem(
                        text = { Text(texto) },
                        onClick = {
                            abierto = false
                            onSeleccionar(valor)
                        }
                    )
                }
            }
        }
    }
}
