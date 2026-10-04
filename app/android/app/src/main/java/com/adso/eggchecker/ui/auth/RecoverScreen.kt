package com.adso.eggchecker.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

import com.adso.eggchecker.ui.components.AuthAlert
import com.adso.eggchecker.ui.components.AuthButton
import com.adso.eggchecker.ui.components.AuthScaffold
import com.adso.eggchecker.ui.components.AuthTextField
import com.adso.eggchecker.ui.theme.Brown

/**
 * Pantalla de recuperación de contraseña. Solo presentación: la
 * funcionalidad aún no está habilitada en el backend móvil.
 */
@Composable
fun RecoverScreen(onIrALogin: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf<String?>(null) }

    AuthScaffold(
        contenido = {
            Text(
                text = "Recuperar contraseña",
                style = MaterialTheme.typography.titleMedium,
                color = Brown,
                modifier = Modifier.fillMaxWidth()
            )
            aviso?.let { AuthAlert(mensaje = it, esError = true) }

            AuthTextField(
                valor = correo,
                onValorChange = { correo = it; aviso = null },
                etiqueta = "Correo electrónico",
                icono = Icons.Filled.Email,
                marcador = "tucorreo@email.com",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            )
            AuthButton(
                texto = "Enviar enlace",
                onClick = {
                    aviso = "La recuperación de contraseña aún no está habilitada."
                }
            )
        },
        pie = {
            TextButton(onClick = onIrALogin, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Volver a iniciar sesión",
                    color = Brown,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
