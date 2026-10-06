package com.adso.eggchecker.ui.camadas.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.adso.eggchecker.domain.model.Camada
import com.adso.eggchecker.ui.camadas.formatearCantidad
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.Dark
import com.adso.eggchecker.ui.theme.ErrorRed

/**
 * Modal para registrar mortalidad, con confirmación previa.
 */
@Composable
fun MortalidadModal(
    abierto: Boolean,
    camada: Camada?,
    onCerrar: () -> Unit,
    onRegistrar: (
        idCamada: Int,
        cantidad: Int,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var confirmando by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto) {
        if (abierto) {
            cantidad = ""
            confirmando = false
            enviando = false
            error = null
        }
    }

    val cerrar = {
        confirmando = false
        onCerrar()
    }

    ModalApp(
        abierto = abierto,
        titulo = "Registrar Mortalidad",
        onCerrar = cerrar
    ) {
        error?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (confirmando && camada != null) {
            Text(
                text = "¿Confirmas registrar $cantidad aves muertas en " +
                    "${camada.nombreCamada}? Se descontarán de la cantidad actual.",
                color = Dark,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                BotonApp(
                    texto = "Volver",
                    onClick = { confirmando = false },
                    tipo = TipoBoton.GHOST
                )
                BotonApp(
                    texto = "Confirmar",
                    onClick = {
                        val numero = cantidad.toIntOrNull() ?: return@BotonApp
                        enviando = true
                        onRegistrar(camada.idCamada, numero) { exito ->
                            enviando = false
                            if (exito) {
                                onCerrar()
                            } else {
                                confirmando = false
                            }
                        }
                    },
                    tipo = TipoBoton.PELIGRO,
                    cargando = enviando,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        } else {
            CampoTexto(
                valor = cantidad,
                onValorChange = { cantidad = it.filter(Char::isDigit) },
                etiqueta = "Cantidad de aves muertas",
                marcador = "Ej. 3",
                keyboardType = KeyboardType.Number,
                hint = "Aves actuales: ${formatearCantidad(camada?.cantidadActual ?: 0)}"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                BotonApp(
                    texto = "Cancelar",
                    onClick = cerrar,
                    tipo = TipoBoton.GHOST
                )
                BotonApp(
                    texto = "Continuar",
                    onClick = {
                        error = null
                        val numero = cantidad.toIntOrNull()
                        when {
                            numero == null || numero <= 0 ->
                                error = "La cantidad debe ser mayor que cero"
                            camada != null && numero > camada.cantidadActual ->
                                error = "La cantidad no puede superar las aves actuales"
                            else -> confirmando = true
                        }
                    },
                    tipo = TipoBoton.PRIMARIO,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
