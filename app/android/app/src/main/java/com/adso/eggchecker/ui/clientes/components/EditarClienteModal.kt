package com.adso.eggchecker.ui.clientes.components

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

import com.adso.eggchecker.domain.model.Cliente
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTelefono
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.ErrorRed

/** Modal para editar nombre, teléfono y dirección de un cliente. */
@Composable
fun EditarClienteModal(
    abierto: Boolean,
    cliente: Cliente?,
    onCerrar: () -> Unit,
    onGuardar: (
        idCliente: Int,
        nombre: String,
        telefono: String?,
        direccion: String?,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto, cliente) {
        if (abierto && cliente != null) {
            nombre = cliente.nombreCliente
            telefono = cliente.telefono.orEmpty()
            direccion = cliente.direccion.orEmpty()
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Editar Cliente", onCerrar = onCerrar) {
        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        CampoTexto(
            valor = nombre,
            onValorChange = { nombre = it },
            etiqueta = "Nombre del cliente"
        )

        Row(modifier = Modifier.padding(top = 16.dp)) {
            CampoTelefono(
                valor = telefono,
                onValorChange = { telefono = it },
                etiqueta = "Teléfono (opcional)",
                marcador = "Ej. 300 123 4567"
            )
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = direccion,
                onValorChange = { direccion = it },
                etiqueta = "Dirección (opcional)"
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
                texto = "Guardar cambios",
                onClick = {
                    error = null
                    val objetivo = cliente ?: return@BotonApp
                    if (nombre.isBlank()) {
                        error = "El nombre del cliente es obligatorio"
                    } else {
                        enviando = true
                        onGuardar(
                            objetivo.idCliente,
                            nombre.trim(),
                            telefono.ifBlank { null },
                            direccion.trim().ifBlank { null }
                        ) { exito ->
                            enviando = false
                            if (exito) onCerrar()
                        }
                    }
                },
                tipo = TipoBoton.PRIMARIO,
                cargando = enviando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
