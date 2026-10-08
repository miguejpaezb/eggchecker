package com.adso.eggchecker.ui.ventas.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Pedido
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed

/** Modal para eliminar un pedido confirmando la contraseña. */
@Composable
fun EliminarPedidoModal(
    abierto: Boolean,
    pedido: Pedido?,
    onCerrar: () -> Unit,
    onEliminar: (contrasena: String, onResultado: (Boolean) -> Unit) -> Unit
) {
    var contrasena by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto, pedido) {
        if (abierto) {
            contrasena = ""
            enviando = false
            error = null
        }
    }

    ModalApp(
        abierto = abierto,
        titulo = "Eliminar Pedido",
        onCerrar = onCerrar
    ) {
        Text(
            text = "¿Eliminar el pedido de " +
                "\"${pedido?.clienteNombre.orEmpty()}\"? Esta acción no se " +
                "puede deshacer y se repondrá el stock.",
            color = Dark,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        CampoTexto(
            valor = contrasena,
            onValorChange = { contrasena = it },
            etiqueta = "Contraseña",
            marcador = "Ingresa tu contraseña",
            esContrasena = true,
            error = error
        )
        if (error == null) {
            Text(
                text = "Confirma con tu contraseña para eliminar.",
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
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
                texto = "Eliminar",
                onClick = {
                    if (contrasena.isBlank()) {
                        error = "Ingresa tu contraseña"
                        return@BotonApp
                    }
                    error = null
                    enviando = true
                    onEliminar(contrasena) { exito ->
                        enviando = false
                        if (exito) onCerrar()
                    }
                },
                tipo = TipoBoton.PELIGRO,
                cargando = enviando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
