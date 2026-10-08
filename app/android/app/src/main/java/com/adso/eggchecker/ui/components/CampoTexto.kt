package com.adso.eggchecker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.InputBackground
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Campo de texto de los formularios.
 *
 * @param hint Texto de ayuda bajo el campo.
 */
@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    marcador: String = "",
    error: String? = null,
    hint: String? = null,
    habilitado: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    soloDecimal: Boolean = false,
    esContrasena: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (habilitado) InputBackground else InputBackground,
            border = BorderStroke(
                1.dp,
                if (error != null) ErrorRed else Border
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                if (valor.isEmpty() && marcador.isNotEmpty()) {
                    Text(text = marcador, fontSize = 15.sp, color = Placeholder)
                }
                BasicTextField(
                    value = valor,
                    onValueChange = { texto ->
                        onValorChange(
                            if (soloDecimal) filtrarDecimal(texto) else texto
                        )
                    },
                    enabled = habilitado,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (habilitado) Dark else Placeholder
                    ),
                    cursorBrush = SolidColor(Brown),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (esContrasena) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )
        } else if (hint != null) {
            Text(
                text = hint,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )
        }
    }
}

/** Deja solo dígitos y un único punto decimal. */
private fun filtrarDecimal(texto: String): String {
    val limpio = texto.filter { it.isDigit() || it == '.' }
    val primerPunto = limpio.indexOf('.')
    if (primerPunto == -1) return limpio
    return limpio.substring(0, primerPunto + 1) +
        limpio.substring(primerPunto + 1).replace(".", "")
}
