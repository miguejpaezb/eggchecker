package com.adso.eggchecker.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.model.MODULOS
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Yellow

/** Radio de 8dp del sidebar web (.ec-sidebar__link). */
private val RADIO_ITEM = RoundedCornerShape(8.dp)

/**
 * Menú lateral con la marca arriba y los módulos debajo, igual al web móvil.
 *
 * @param rutaActual Ruta activa para resaltar el ítem.
 * @param onSeleccionar Acción al elegir un módulo.
 */
@Composable
fun AppDrawer(
    rutaActual: String?,
    onSeleccionar: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Brown,
        drawerShape = RectangleShape,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.width(280.dp)
    ) {
        Spacer(
            modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_eggchecker),
                contentDescription = "Isotipo EggChecker",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "EggChecker",
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Yellow
            )
        }

        MODULOS.forEach { modulo ->
            val activo = rutaActual == modulo.ruta
            NavigationDrawerItem(
                label = {
                    Text(
                        text = modulo.nombre,
                        color = Yellow,
                        fontWeight = if (activo) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                icon = {
                    Icon(
                        painter = painterResource(modulo.icono),
                        contentDescription = null,
                        tint = Yellow,
                        modifier = Modifier.size(20.dp)
                    )
                },
                selected = activo,
                onClick = { onSeleccionar(modulo.ruta) },
                shape = RADIO_ITEM,
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Yellow.copy(alpha = 0.25f),
                    unselectedContainerColor = Color.Transparent,
                    selectedIconColor = Yellow,
                    unselectedIconColor = Yellow,
                    selectedTextColor = Yellow,
                    unselectedTextColor = Yellow
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}
