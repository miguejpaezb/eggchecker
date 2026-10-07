package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.SuperficieTarjeta

/** Secciones de configuración del perfil. */
private val SECCIONES = listOf(
    "editar" to "Editar Perfil",
    "notificaciones" to "Notificaciones",
    "seguridad" to "Seguridad",
    "plan" to "Plan y Suscripción",
    "ayuda" to "Ayuda y Soporte"
)

/** Submenú de configuración del perfil. */
@Composable
fun PerfilMenuCard(
    activa: String,
    onSeleccionar: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SuperficieTarjeta,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Configuración",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Brown,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SECCIONES.forEachIndexed { indice, (id, etiqueta) ->
                val esActiva = id == activa
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeleccionar(id) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = etiqueta,
                        fontSize = 15.sp,
                        color = if (esActiva) Brown else Dark,
                        fontWeight = if (esActiva) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                    Text(
                        text = "›",
                        fontSize = 20.sp,
                        color = if (esActiva) Brown else Placeholder
                    )
                }
                if (indice != SECCIONES.lastIndex) {
                    HorizontalDivider(color = Color(0xFFEDEDED))
                }
            }
        }
    }
}
