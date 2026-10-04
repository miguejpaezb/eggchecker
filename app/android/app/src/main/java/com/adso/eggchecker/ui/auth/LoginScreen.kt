package com.adso.eggchecker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.ui.components.AuthAlert
import com.adso.eggchecker.ui.components.AuthButton
import com.adso.eggchecker.ui.components.AuthScaffold
import com.adso.eggchecker.ui.components.AuthTextField
import com.adso.eggchecker.ui.theme.Brown

/**
 * Pantalla de inicio de sesión.
 *
 * @param correoInicial Correo rellenado tras un registro exitoso.
 * @param mensajeExito Aviso de cuenta creada.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    correoInicial: String,
    mensajeExito: String,
    onIrARegistro: () -> Unit,
    onIrARecuperar: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(correoInicial) {
        if (correoInicial.isNotBlank()) {
            viewModel.onCorreoChange(correoInicial)
        }
    }

    AuthScaffold(
        contenido = {
            Text(
                text = "Bienvenido",
                style = MaterialTheme.typography.titleMedium,
                color = Brown,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            if (mensajeExito.isNotBlank()) {
                AuthAlert(mensaje = mensajeExito, esError = false)
            }
            estado.error?.let { AuthAlert(mensaje = it, esError = true) }

            AuthTextField(
                valor = estado.correo,
                onValorChange = viewModel::onCorreoChange,
                etiqueta = "Correo electrónico",
                icono = Icons.Filled.Email,
                marcador = "tucorreo@email.com",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
            AuthTextField(
                valor = estado.contrasena,
                onValorChange = viewModel::onContrasenaChange,
                etiqueta = "Contraseña",
                icono = Icons.Filled.Lock,
                marcador = "••••••••",
                esContrasena = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
            AuthButton(
                texto = if (estado.cargando) "Ingresando…" else "Iniciar sesión",
                onClick = viewModel::iniciarSesion,
                cargando = estado.cargando
            )
            TextButton(
                onClick = onIrARecuperar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Olvidé mi contraseña",
                    color = Brown,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        pie = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "¿No tienes cuenta? ")
                TextButton(onClick = onIrARegistro) {
                    Text(
                        text = "Crear cuenta",
                        color = Brown,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
