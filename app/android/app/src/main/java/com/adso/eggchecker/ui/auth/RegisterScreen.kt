package com.adso.eggchecker.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * Pantalla de registro de un avicultor nuevo.
 *
 * @param onRegistrado Callback con el correo cuando la cuenta se crea.
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistrado: (String) -> Unit,
    onIrALogin: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    AuthScaffold(
        contenido = {
            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.titleMedium,
                color = Brown,
                modifier = Modifier.fillMaxWidth()
            )
            estado.error?.let { AuthAlert(mensaje = it, esError = true) }

            AuthTextField(
                valor = estado.nombreCompleto,
                onValorChange = viewModel::onNombreChange,
                etiqueta = "Nombre Completo",
                icono = Icons.Filled.Person,
                marcador = "Juan Pérez",
                error = estado.errores["nombre"],
                imeAction = ImeAction.Next
            )
            AuthTextField(
                valor = estado.correo,
                onValorChange = viewModel::onCorreoChange,
                etiqueta = "Correo electrónico",
                icono = Icons.Filled.Email,
                marcador = "tucorreo@email.com",
                error = estado.errores["correo"],
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
            AuthTextField(
                valor = estado.contrasena,
                onValorChange = viewModel::onContrasenaChange,
                etiqueta = "Contraseña",
                icono = Icons.Filled.Lock,
                marcador = "••••••••",
                error = estado.errores["contrasena"],
                esContrasena = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
            AuthTextField(
                valor = estado.confirmar,
                onValorChange = viewModel::onConfirmarChange,
                etiqueta = "Confirmar contraseña",
                icono = Icons.Filled.Lock,
                marcador = "••••••••",
                error = estado.errores["confirmar"],
                esContrasena = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
            AuthButton(
                texto = if (estado.cargando) "Creando cuenta…" else "Crear cuenta",
                onClick = { viewModel.registrar(onRegistrado) },
                cargando = estado.cargando
            )
        },
        pie = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "¿Ya tienes cuenta? ")
                TextButton(onClick = onIrALogin) {
                    Text(
                        text = "Iniciar sesión",
                        color = Brown,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
