package com.adso.eggchecker.ui.perfil.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.adso.eggchecker.domain.model.Perfil
import com.adso.eggchecker.ui.components.CampoTelefono
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.perfil.DatosPerfilForm

/** Sección para editar los datos personales, la granja y el correo. */
@Composable
fun EditarPerfilSeccion(
    perfil: Perfil,
    onGuardar: (
        datos: DatosPerfilForm,
        onExito: () -> Unit,
        onError: () -> Unit
    ) -> Unit
) {
    var nombre by remember { mutableStateOf(perfil.nombreCompleto) }
    var correo by remember { mutableStateOf(perfil.correoElectronico) }
    var telefono by remember { mutableStateOf(perfil.telefono.orEmpty()) }
    var granja by remember { mutableStateOf(perfil.nombreGranja.orEmpty()) }
    var contrasena by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(perfil) {
        nombre = perfil.nombreCompleto
        correo = perfil.correoElectronico
        telefono = perfil.telefono.orEmpty()
        granja = perfil.nombreGranja.orEmpty()
        contrasena = ""
        error = null
        exito = false
    }

    val cambioCorreo = correo.trim().lowercase() !=
        perfil.correoElectronico.lowercase()

    val limpiar = {
        nombre = perfil.nombreCompleto
        correo = perfil.correoElectronico
        telefono = perfil.telefono.orEmpty()
        granja = perfil.nombreGranja.orEmpty()
        contrasena = ""
        error = null
        exito = false
    }

    TarjetaPerfil {
        if (error != null) AvisoError(error!!)
        if (exito) AvisoExito("Cambios guardados correctamente")

        SubtituloSeccion("Información Personal")
        CampoTexto(
            valor = nombre,
            onValorChange = { nombre = it },
            etiqueta = "Nombre Completo"
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            CampoTexto(
                valor = correo,
                onValorChange = { correo = it },
                etiqueta = "Correo electrónico",
                keyboardType = KeyboardType.Email
            )
        }
        if (cambioCorreo) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                CampoTexto(
                    valor = contrasena,
                    onValorChange = { contrasena = it },
                    etiqueta = "Contraseña actual (para cambiar el correo)",
                    esContrasena = true
                )
            }
        }
        Column(modifier = Modifier.padding(top = 12.dp)) {
            CampoTelefono(
                valor = telefono,
                onValorChange = { telefono = it },
                etiqueta = "Número de teléfono",
                marcador = "Ej. 300 123 4567"
            )
        }

        SubtituloSeccion("Datos de la granja")
        CampoTexto(
            valor = granja,
            onValorChange = { granja = it },
            etiqueta = "Nombre de la granja",
            marcador = "Ej. Granja La Esperanza"
        )

        AccionesFormulario(
            textoGuardar = "Guardar Cambios",
            enviando = enviando,
            onCancelar = limpiar,
            onGuardar = {
                error = null
                exito = false
                if (nombre.trim().isEmpty()) {
                    error = "El nombre completo es obligatorio"
                    return@AccionesFormulario
                }
                if (cambioCorreo && contrasena.isBlank()) {
                    error = "Ingresa tu contraseña actual para cambiar el correo"
                    return@AccionesFormulario
                }
                enviando = true
                onGuardar(
                    DatosPerfilForm(
                        nombreCompleto = nombre,
                        correoElectronico = correo,
                        telefono = telefono,
                        nombreGranja = granja,
                        contrasenaActual = contrasena
                    ),
                    {
                        enviando = false
                        contrasena = ""
                        exito = true
                    },
                    { enviando = false }
                )
            }
        )
    }
}
