package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.ui.components.CampoTexto

/** Sección para cambiar la contraseña del usuario. */
@Composable
fun SeguridadSeccion(
    onGuardar: (
        contrasenaActual: String,
        contrasenaNueva: String,
        onExito: () -> Unit,
        onError: () -> Unit
    ) -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    TarjetaPerfil {
        SubtituloSeccion("Seguridad de la Cuenta")
        if (error != null) AvisoError(error!!)
        if (exito) AvisoExito("Contraseña actualizada correctamente")

        CampoTexto(
            valor = actual,
            onValorChange = { actual = it },
            etiqueta = "Contraseña Actual",
            esContrasena = true
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            CampoTexto(
                valor = nueva,
                onValorChange = { nueva = it },
                etiqueta = "Nueva Contraseña",
                esContrasena = true,
                hint = "Mínimo 8 caracteres, con mayúscula, número y símbolo"
            )
        }
        Column(modifier = Modifier.padding(top = 12.dp)) {
            CampoTexto(
                valor = confirmar,
                onValorChange = { confirmar = it },
                etiqueta = "Confirmar Contraseña",
                esContrasena = true
            )
        }

        AccionesFormulario(
            textoGuardar = "Actualizar Contraseña",
            enviando = enviando,
            onCancelar = {
                actual = ""
                nueva = ""
                confirmar = ""
                error = null
                exito = false
            },
            onGuardar = {
                error = null
                exito = false
                when {
                    nueva != confirmar -> {
                        error = "Las contraseñas nuevas no coinciden"
                    }
                    nueva.length < 8 -> {
                        error = "La nueva contraseña debe tener al menos 8 caracteres"
                    }
                    actual.isBlank() -> {
                        error = "Ingresa tu contraseña actual"
                    }
                    else -> {
                        enviando = true
                        onGuardar(
                            actual,
                            nueva,
                            {
                                enviando = false
                                actual = ""
                                nueva = ""
                                confirmar = ""
                                exito = true
                            },
                            { enviando = false }
                        )
                    }
                }
            }
        )
    }
}
