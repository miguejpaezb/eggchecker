package com.adso.eggchecker.ui.perfil.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.SuperficieTarjeta
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Ajustes de notificaciones exclusivos de la app (se guardan solo en el
 * dispositivo). El switch maestro deshabilita vibración y sonido.
 */
@Composable
fun AjustesNotificacionApp(
    notificacionesActivas: Boolean,
    vibracion: Boolean,
    sonidoPersonalizado: String?,
    onCambiarNotificaciones: (Boolean) -> Unit,
    onCambiarVibracion: (Boolean) -> Unit,
    onCambiarSonido: (String?) -> Unit,
    onReproducirSonido: () -> Unit
) {
    val selector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = resultado.data
                ?.getParcelableExtra<Uri>(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI
                )
            if (uri != null) {
                onCambiarSonido(uri.toString())
            }
        }
    }

    val abrirSelector = {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION
            )
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                "Elegir sonido de notificación"
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            sonidoPersonalizado?.let {
                putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Uri.parse(it)
                )
            }
        }
        selector.launch(intent)
    }

    TarjetaPerfil {
        SubtituloSeccion("Notificaciones del dispositivo")
        Text(
            text = "Ajustes solo de esta app; no se guardan en tu cuenta. " +
                "Si desinstalas la app o inicias sesión en otro teléfono, " +
                "vuelven al valor por defecto.",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FilaSwitch(
            titulo = "Notificaciones",
            descripcion = "Mostrar las alertas en la barra del teléfono. " +
                "Si lo desactivas, solo verás las alertas dentro de la app.",
            activo = notificacionesActivas,
            habilitado = true,
            onCambiar = onCambiarNotificaciones
        )
        FilaSwitch(
            titulo = "Vibración",
            descripcion = "Vibrar al recibir una alerta.",
            activo = vibracion,
            habilitado = notificacionesActivas,
            onCambiar = onCambiarVibracion
        )

        Text(
            text = "Sonido",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (notificacionesActivas) Dark else Placeholder,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OpcionSonido(
                texto = "Predeterminado",
                seleccionado = sonidoPersonalizado == null,
                habilitado = notificacionesActivas,
                modifier = Modifier.weight(1f)
            ) { onCambiarSonido(null) }
            OpcionSonido(
                texto = "Personalizado",
                seleccionado = sonidoPersonalizado != null,
                habilitado = notificacionesActivas,
                modifier = Modifier.weight(1f)
            ) { abrirSelector() }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Escuchar",
                onClick = onReproducirSonido,
                tipo = TipoBoton.GHOST,
                habilitado = notificacionesActivas
            )
            BotonApp(
                texto = "Elegir sonido",
                onClick = { abrirSelector() },
                tipo = TipoBoton.PRIMARIO,
                habilitado = notificacionesActivas,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun FilaSwitch(
    titulo: String,
    descripcion: String,
    activo: Boolean,
    habilitado: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (habilitado) Dark else Placeholder
            )
            Text(
                text = descripcion,
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        SwitchApp(
            activo = activo,
            onCambiar = onCambiar,
            habilitado = habilitado,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun OpcionSonido(
    texto: String,
    seleccionado: Boolean,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) {
            Brown.copy(alpha = 0.12f)
        } else {
            SuperficieTarjeta
        },
        border = BorderStroke(
            1.dp,
            if (seleccionado && habilitado) Brown else Border
        ),
        modifier = modifier.clickable(enabled = habilitado) { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = texto,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = if (seleccionado) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = when {
                    !habilitado -> Placeholder
                    seleccionado -> Brown
                    else -> TextMuted
                }
            )
        }
    }
}
