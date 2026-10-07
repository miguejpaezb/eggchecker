package com.adso.eggchecker.ui.shell.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.R
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Yellow

/**
 * Barra superior marrón con el logo, menú, campanita y avatar.
 *
 * @param hayNoLeidas Muestra el punto amarillo en la campanita.
 */
@Composable
fun AppTopbar(
    hayNoLeidas: Boolean,
    onMenu: () -> Unit,
    onNotificaciones: () -> Unit,
    onPerfilClick: () -> Unit
) {
    Surface(
        color = Brown,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenu) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = "Abrir menú",
                    tint = Yellow
                )
            }

            Image(
                painter = painterResource(R.drawable.logo_eggchecker),
                contentDescription = "Isotipo EggChecker",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            Box {
                IconButton(onClick = onNotificaciones) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notificacion),
                        contentDescription = "Notificaciones",
                        tint = Yellow
                    )
                }
                if (hayNoLeidas) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Yellow)
                            .border(2.dp, Brown, CircleShape)
                    )
                }
            }

            IconButton(onClick = onPerfilClick) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_usuario),
                        contentDescription = "Perfil de usuario",
                        tint = Brown,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
