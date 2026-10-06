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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.ui.theme.Border
import com.adso.eggchecker.ui.theme.Brown
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.InputBackground
import com.adso.eggchecker.ui.theme.Placeholder
import com.adso.eggchecker.ui.theme.TextMuted

/** Cantidad máxima de dígitos aceptados en un teléfono. */
private const val MAX_DIGITOS_TELEFONO = 15

/** Deja solo los dígitos de un texto. */
fun soloDigitos(valor: String?): String = (valor ?: "").filter { it.isDigit() }

/**
 * Aplica el formato colombiano 3-3-4 solo para mostrar.
 * El valor persistido sigue siendo la cadena de dígitos.
 */
fun formatearTelefono(valor: String?): String {
    val digitos = soloDigitos(valor)
    return when {
        digitos.length <= 3 -> digitos
        digitos.length <= 6 ->
            "${digitos.substring(0, 3)} ${digitos.substring(3)}"
        else ->
            "${digitos.substring(0, 3)} ${digitos.substring(3, 6)} " +
                digitos.substring(6)
    }
}

/**
 * Campo de teléfono que solo acepta dígitos y muestra el formato 3-3-4.
 * Al llamador le entrega el valor sin espacios (solo dígitos).
 */
@Composable
fun CampoTelefono(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    marcador: String = "",
    error: String? = null,
    habilitado: Boolean = true
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
            color = InputBackground,
            border = BorderStroke(
                1.dp,
                if (error != null) ErrorRed else Border
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                if (soloDigitos(valor).isEmpty() && marcador.isNotEmpty()) {
                    Text(text = marcador, fontSize = 15.sp, color = Placeholder)
                }
                BasicTextField(
                    value = formatearTelefono(valor),
                    onValueChange = { texto ->
                        onValorChange(
                            soloDigitos(texto).take(MAX_DIGITOS_TELEFONO)
                        )
                    },
                    enabled = habilitado,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (habilitado) Dark else Placeholder
                    ),
                    cursorBrush = SolidColor(Brown),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),
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
        }
    }
}
