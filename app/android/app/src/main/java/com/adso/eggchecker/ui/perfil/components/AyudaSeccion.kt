package com.adso.eggchecker.ui.perfil.components

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.InputBackground
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted

// Correo de soporte y preguntas frecuentes.
private const val CORREO_SOPORTE = "ayuda@eggchecker.click"

private val PQR = listOf(
    "¿Cómo registro la producción diaria de una camada?" to
        "Ve al módulo Producción, selecciona la camada y las cantidades por " +
        "tipo de huevo (AA, A, B y No apto). El sistema guarda el total del día.",
    "El stock de un insumo aparece en negativo, ¿qué hago?" to
        "Revisa los movimientos registrados. El sistema no permite salidas " +
        "mayores al stock disponible; corrige el movimiento y vuelve a intentar.",
    "¿Cómo cambio el correo de mi cuenta?" to
        "En Perfil → Editar Perfil cambia el correo e ingresa tu contraseña " +
        "actual. Por seguridad, la sesión se cerrará y deberás iniciar de nuevo.",
    "¿Qué incluye el plan Premium?" to
        "Aves y clientes ilimitados, además del módulo de Inteligencia " +
        "Artificial. Consulta la sección Plan y Suscripción."
)

/** Sección de ayuda: envía un correo de soporte y muestra PQR frecuentes. */
@Composable
fun AyudaSeccion() {
    val context = LocalContext.current
    var asunto by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var abierta by remember { mutableStateOf<Int?>(null) }

    TarjetaPerfil {
        SubtituloSeccion("Ayuda y Soporte")

        CampoTexto(
            valor = asunto,
            onValorChange = { asunto = it },
            etiqueta = "Asunto",
            marcador = "Ej. Problema al registrar un pedido"
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "Mensaje",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = InputBackground,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    if (mensaje.isEmpty()) {
                        Text(
                            text = "Cuéntanos en qué podemos ayudarte…",
                            fontSize = 15.sp,
                            color = Placeholder
                        )
                    }
                    BasicTextField(
                        value = mensaje,
                        onValueChange = { mensaje = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Dark
                        ),
                        cursorBrush = SolidColor(Brown),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout
                .Arrangement.End
        ) {
            BotonApp(
                texto = "Enviar Correo",
                onClick = {
                    val enlace = Uri.parse(
                        "mailto:$CORREO_SOPORTE" +
                            "?subject=${Uri.encode(asunto)}" +
                            "&body=${Uri.encode(mensaje)}"
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, enlace)
                    )
                },
                tipo = TipoBoton.PRIMARIO
            )
        }

        SubtituloSeccion("Preguntas frecuentes (PQR)")
        PQR.forEachIndexed { indice, (pregunta, respuesta) ->
            val esAbierta = abierta == indice
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            abierta = if (esAbierta) null else indice
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pregunta,
                        color = Dark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (esAbierta) "⌄" else "›",
                        color = Placeholder,
                        fontSize = 18.sp
                    )
                }
                if (esAbierta) {
                    Text(
                        text = respuesta,
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}
