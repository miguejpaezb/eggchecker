package com.adso.eggchecker.ui.clientes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed

/** Modal para eliminar un cliente confirmando la contraseña del usuario. */
@Composable
fun EliminarClienteModal(
    abierto: Boolean,
    cliente: Cliente?,
    onCerrar: () -> Unit,
    onEliminar: (
        contrasena: String,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var contrasena by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            contrasena = ""
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Eliminar cliente", onCerrar = onCerrar) {
        Text(
            text = "¿Eliminar \"${cliente?.nombreCliente.orEmpty()}\"? Esta acción " +
                "borra el cliente de la base de datos y no se puede revertir.",
            color = Dark,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Column {
            CampoTexto(
                valor = contrasena,
                onValorChange = { contrasena = it },
                etiqueta = "Contraseña de tu usuario",
                marcador = "Confirma tu contraseña",
                keyboardType = KeyboardType.Password,
                esContrasena = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            BotonApp(
                texto = "Cancelar",
                onClick = onCerrar,
                tipo = TipoBoton.GHOST
            )
            BotonApp(
                texto = "Eliminar cliente",
                onClick = {
                    error = null
                    if (contrasena.isBlank()) {
                        error = "Ingresa tu contraseña para confirmar"
                    } else {
                        enviando = true
                        onEliminar(contrasena) { exito ->
                            enviando = false
                            if (exito) onCerrar()
                        }
                    }
                },
                tipo = TipoBoton.PELIGRO,
                cargando = enviando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
