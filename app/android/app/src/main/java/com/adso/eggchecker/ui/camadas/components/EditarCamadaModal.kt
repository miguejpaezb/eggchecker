package com.adso.eggchecker.ui.camadas.components

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

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.camadas.formatearFecha
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Modal para editar una camada: el nombre siempre y, dentro de las
 * primeras 24 horas, la cantidad inicial.
 */
@Composable
fun EditarCamadaModal(
    abierto: Boolean,
    camada: Camada?,
    onCerrar: () -> Unit,
    onGuardar: (
        idCamada: Int,
        nombre: String,
        cantidadInicial: Int?,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val puedeEditarInicial = camada?.puedeEditarInicial == true

    LaunchedEffect(abierto, camada) {
        if (abierto && camada != null) {
            nombre = camada.nombreCamada
            cantidad = camada.cantidadInicial.toString()
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Editar Camada", onCerrar = onCerrar) {
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
            etiqueta = "Nombre de la camada"
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Fecha de ingreso", color = TextMuted, fontSize = 14.sp)
            Text(
                text = formatearFecha(camada?.fechaIngreso),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = cantidad,
                onValorChange = { cantidad = it.filter(Char::isDigit) },
                etiqueta = "Cantidad inicial de aves",
                keyboardType = KeyboardType.Number,
                habilitado = puedeEditarInicial,
                hint = if (!puedeEditarInicial) {
                    "No se puede cambiar: la camada se registró hace más de 24 horas."
                } else {
                    null
                }
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
                    val objetivo = camada ?: return@BotonApp
                    if (nombre.isBlank()) {
                        error = "Ingresa el nombre de la camada"
                        return@BotonApp
                    }
                    val inicial: Int?
                    if (puedeEditarInicial) {
                        val numero = cantidad.toIntOrNull()
                        if (numero == null || numero <= 0) {
                            error = "La cantidad inicial debe ser mayor que cero"
                            return@BotonApp
                        }
                        inicial = numero
                    } else {
                        inicial = null
                    }
                    enviando = true
                    onGuardar(objetivo.idCamada, nombre.trim(), inicial) { exito ->
                        enviando = false
                        if (exito) onCerrar()
                    }
                },
                tipo = TipoBoton.PRIMARIO,
                cargando = enviando,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
