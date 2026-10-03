package com.adso.eggchecker.ui.shell.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.R
import com.adso.eggchecker.domain.model.Notificacion
import com.adso.eggchecker.ui.shell.NotificacionesUiState
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted
import com.adso.eggchecker.ui.theme.Yellow

private val RADIO_PANEL = RoundedCornerShape(16.dp)
private val RADIO_CARD = RoundedCornerShape(12.dp)
private val RADIO_BTN = RoundedCornerShape(8.dp)
private val COLOR_MENSAJE = Color(0xFF4B5563)
private val COLOR_LEIDA = Color(0xFFF9FAFB)

/**
 * Panel desplegable de notificaciones, fiel al `.ec-notif-panel` del web.
 *
 * @param estado Notificaciones, carga y error.
 */
@Composable
fun NotificacionesPanel(
    estado: NotificacionesUiState,
    onMarcarTodas: () -> Unit,
    onMarcarLeida: (Int) -> Unit,
    onEliminar: (Int) -> Unit,
    onVer: (Notificacion) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RADIO_PANEL,
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Notificaciones",
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Brown
                )
                Text(
                    text = "Marcar todas como leído",
                    color = if (estado.hayNoLeidas) Brown else Placeholder,
                    fontSize = 12.8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            enabled = estado.hayNoLeidas,
                            onClick = onMarcarTodas
                        )
                        .padding(4.dp)
                )
            }

            if (estado.cargando) {
                Text(
                    text = "Cargando…",
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (!estado.cargando && estado.error != null) {
                Text(
                    text = estado.error,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (!estado.cargando && estado.error == null &&
                estado.notificaciones.isEmpty()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notificaciones),
                        contentDescription = null,
                        tint = Placeholder,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "No tienes notificaciones",
                        fontSize = 14.sp,
                        color = TextMuted
                    )
                }
            }

            if (!estado.cargando && estado.error == null &&
                estado.notificaciones.isNotEmpty()
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    estado.notificaciones.forEach { aviso ->
                        NotificacionCard(
                            aviso = aviso,
                            onMarcarLeida = onMarcarLeida,
                            onEliminar = onEliminar,
                            onVer = onVer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificacionCard(
    aviso: Notificacion,
    onMarcarLeida: (Int) -> Unit,
    onEliminar: (Int) -> Unit,
    onVer: (Notificacion) -> Unit
) {
    Surface(
        shape = RADIO_CARD,
        color = if (aviso.leida) COLOR_LEIDA else Color.White,
        border = BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = aviso.titulo,
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.2.sp,
                    color = Dark,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEliminar(aviso.idNotificacion) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = Placeholder,
                        fontSize = 20.sp
                    )
                }
            }
            Text(
                text = aviso.mensaje,
                fontSize = 13.6.sp,
                color = COLOR_MENSAJE,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotonNotificacion(
                    texto = "Ver",
                    primario = false,
                    modifier = Modifier.weight(1f),
                    onClick = { onVer(aviso) }
                )
                if (aviso.leida) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Leída",
                            fontSize = 12.8.sp,
                            color = Placeholder
                        )
                    }
                } else {
                    BotonNotificacion(
                        texto = "Marcar como leído",
                        primario = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onMarcarLeida(aviso.idNotificacion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonNotificacion(
    texto: String,
    primario: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RADIO_BTN,
        color = if (primario) Brown else Color.White,
        border = if (primario) null else BorderStroke(1.dp, Border),
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RADIO_BTN)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = texto,
                color = if (primario) Yellow else Brown,
                fontSize = 12.8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
