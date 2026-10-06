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

import com.adso.eggchecker.ui.camadas.fechaAyerIso
import com.adso.eggchecker.ui.camadas.fechaHoyIso
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.SelectorSegmentado
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/**
 * Modal para registrar una camada nueva.
 *
 * @param onCrear Crea la camada; devuelve true en éxito, false si falló
 *   (el error ya se mostró como toast).
 */
@Composable
fun NuevaCamadaModal(
    abierto: Boolean,
    onCerrar: () -> Unit,
    onCrear: (
        nombre: String,
        fechaIngreso: String,
        cantidadInicial: Int,
        estado: String,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    val hoy = fechaHoyIso()
    val ayer = fechaAyerIso()
    var nombre by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(hoy) }
    var cantidad by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("activa") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            nombre = ""
            fecha = hoy
            cantidad = ""
            estado = "activa"
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Nueva Camada", onCerrar = onCerrar) {
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
            etiqueta = "Nombre de la camada",
            marcador = "Ej. Lote A - Galpón 1"
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Fecha de ingreso", color = TextMuted, fontSize = 14.sp)
            Row(modifier = Modifier.padding(top = 6.dp)) {
                SelectorSegmentado(
                    opciones = listOf(hoy to "Hoy", ayer to "Ayer"),
                    valor = fecha,
                    onValorChange = { fecha = it }
                )
            }
            if (fecha == ayer) {
                Text(
                    text = "Estás registrando la camada con la fecha de ayer. " +
                        "Esta fecha no se podrá modificar más adelante.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = cantidad,
                onValorChange = { cantidad = it.filter(Char::isDigit) },
                etiqueta = "Cantidad inicial de aves",
                marcador = "Ej. 500",
                keyboardType = KeyboardType.Number
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Estado", color = TextMuted, fontSize = 14.sp)
            Row(modifier = Modifier.padding(top = 6.dp)) {
                SelectorSegmentado(
                    opciones = listOf("activa" to "Activa", "retirada" to "Retirada"),
                    valor = estado,
                    onValorChange = { estado = it }
                )
            }
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
                texto = "Registrar camada",
                onClick = {
                    error = null
                    val numero = cantidad.toIntOrNull()
                    when {
                        nombre.isBlank() ->
                            error = "Ingresa el nombre de la camada"
                        numero == null || numero <= 0 ->
                            error = "La cantidad inicial debe ser mayor que cero"
                        else -> {
                            enviando = true
                            onCrear(nombre.trim(), fecha, numero, estado) { exito ->
                                enviando = false
                                if (exito) onCerrar()
                            }
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
