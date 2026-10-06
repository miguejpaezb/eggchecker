package com.adso.eggchecker.ui.inventario.components

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

import com.adso.eggchecker.domain.model.Insumo
import com.adso.eggchecker.ui.components.BotonApp
import com.adso.eggchecker.ui.components.CampoTexto
import com.adso.eggchecker.ui.components.ModalApp
import com.adso.eggchecker.ui.components.TipoBoton
import com.adso.eggchecker.ui.theme.ErrorRed
import com.adso.eggchecker.ui.theme.TextMuted

/** Modal para editar nombre, unidad y stock mínimo de un insumo. */
@Composable
fun EditarInsumoModal(
    abierto: Boolean,
    insumo: Insumo?,
    onCerrar: () -> Unit,
    onGuardar: (
        idInsumo: Int,
        nombre: String,
        unidad: String,
        umbral: Double,
        onResultado: (Boolean) -> Unit
    ) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("") }
    var umbral by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(abierto, insumo) {
        if (abierto && insumo != null) {
            nombre = insumo.nombreInsumo
            unidad = insumo.unidadMedida
            umbral = insumo.umbralMinimo.toString()
            enviando = false
            error = null
        }
    }

    ModalApp(abierto = abierto, titulo = "Editar Insumo", onCerrar = onCerrar) {
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
            etiqueta = "Nombre del insumo"
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = unidad,
                onValorChange = { unidad = it },
                etiqueta = "Unidad de medida"
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            CampoTexto(
                valor = umbral,
                onValorChange = { umbral = it },
                etiqueta = "Stock mínimo",
                keyboardType = KeyboardType.Decimal,
                soloDecimal = true,
                hint = "El stock actual solo cambia con movimientos."
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
                    val objetivo = insumo ?: return@BotonApp
                    val umbralNumero = umbral.toDoubleOrNull()
                    when {
                        nombre.isBlank() -> error = "Ingresa el nombre del insumo"
                        unidad.isBlank() -> error = "Ingresa la unidad de medida"
                        umbralNumero == null || umbralNumero <= 0.0 ->
                            error = "El stock mínimo debe ser mayor que cero"
                        else -> {
                            enviando = true
                            onGuardar(
                                objetivo.idInsumo,
                                nombre.trim(),
                                unidad.trim(),
                                umbralNumero
                            ) { exito ->
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
